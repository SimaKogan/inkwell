package com.inkwell.search;

import com.inkwell.common.ApiException;
import com.inkwell.ml.MlClient;
import com.inkwell.search.SearchDtos.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Hybrid search: PostgreSQL full-text (prefix) + trigram similarity on titles + pgvector cosine distance,
 * merged with Reciprocal Rank Fusion. If the ML service is down it silently falls back to text-only search.
 */
@Service
public class SearchService {
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final double MAX_COSINE_DISTANCE = 0.65;
    private static final int PER_SOURCE = 50;

    private enum Source {
        CATALOG("catalog", "catalog_item", "t.title", "description"),
        POST("post", "post", "coalesce(nullif(t.title, ''), left(t.body, 80))", "body");

        final String type, table, titleExpr, bodyColumn;
        Source(String type, String table, String titleExpr, String bodyColumn) {
            this.type = type; this.table = table; this.titleExpr = titleExpr; this.bodyColumn = bodyColumn;
        }
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final MlClient ml;

    public SearchService(NamedParameterJdbcTemplate jdbc, MlClient ml) {
        this.jdbc = jdbc;
        this.ml = ml;
    }

    public SearchResponse search(String raw, String type, int limit) { return search(raw, type, limit, false); }

    /** exact = true searches the text as typed, skipping the keyboard-layout repair. */
    public SearchResponse search(String raw, String type, int limit, boolean exact) {
        String q = raw == null ? "" : raw.trim();
        if (q.length() > 200) q = q.substring(0, 200);
        int max = Math.max(1, Math.min(limit, 30));
        if (q.isEmpty()) return new SearchResponse(q, q, false, "unknown", false, List.of());

        String effective = exact ? q : chooseQuery(q);
        Optional<float[]> vec = ml.embed(effective);
        List<SearchHit> hits = new ArrayList<>();
        for (Source s : Source.values()) {
            if (type != null && !type.isBlank() && !s.type.equalsIgnoreCase(type)) continue;
            hits.addAll(run(s, effective, vec.orElse(null), max));
        }
        hits.sort(Comparator.comparingDouble(SearchHit::score).reversed());
        List<SearchHit> top = hits.size() > max ? hits.subList(0, max) : hits;
        return new SearchResponse(q, effective, !effective.equals(q), LanguageDetector.code(effective),
                vec.isPresent(), List.copyOf(top));
    }

    public AskResponse ask(String query) {
        SearchResponse found = search(query, null, 5);
        if (found.hits().isEmpty()) return new AskResponse(null, found.effectiveQuery(), List.of());
        List<Map<String, Object>> sources = new ArrayList<>();
        for (int i = 0; i < found.hits().size(); i++) {
            SearchHit h = found.hits().get(i);
            sources.add(Map.of("n", i + 1, "title", h.title(), "text", h.snippet() == null ? "" : h.snippet()));
        }
        String answer = ml.answer(found.effectiveQuery(), sources);
        if (answer == null || answer.isBlank()) throw new ApiException(HttpStatus.BAD_GATEWAY, "Empty answer from the model");
        return new AskResponse(answer, found.effectiveQuery(), found.hits());
    }

    /**
     * If the query finds nothing as typed but its keyboard-layout repair does, use the repaired text.
     * Data-driven on purpose: no guessing whether "dog" is English or mistyped Hebrew.
     */
    private String chooseQuery(String q) {
        if (ftsCount(q) > 0) return q;
        List<String> candidates = switch (LanguageDetector.script(q)) {
            case LATIN -> List.of(Layouts.toHebrew(q), Layouts.toRussian(q));
            case CYRILLIC -> List.of(Layouts.fromRussian(q));
            case HEBREW -> List.of(Layouts.fromHebrew(q));
            default -> List.<String>of();
        };
        return candidates.stream().filter(c -> !c.equals(q) && ftsCount(c) > 0).findFirst().orElse(q);
    }

    private int ftsCount(String q) {
        String tsq = tsQuery(q);
        if (tsq.isEmpty()) return 0;
        Integer n = jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM catalog_item WHERE tsv @@ to_tsquery('simple', :tsq))
                     + (SELECT count(*) FROM post WHERE tsv @@ to_tsquery('simple', :tsq))
                """, new MapSqlParameterSource("tsq", tsq), Integer.class);
        return n == null ? 0 : n;
    }

    /** Prefix tsquery built only from letters and digits, so user input can never inject query syntax. */
    static String tsQuery(String q) {
        List<String> tokens = new ArrayList<>();
        for (String t : NON_WORD.split(q.toLowerCase())) {
            if (!t.isEmpty()) tokens.add(t + ":*");
            if (tokens.size() == 8) break;
        }
        return String.join(" & ", tokens);
    }

    private List<SearchHit> run(Source s, String q, float[] vec, int limit) {
        String tsq = tsQuery(q);
        List<String> ctes = new ArrayList<>();
        List<String> unions = new ArrayList<>();
        MapSqlParameterSource p = new MapSqlParameterSource("limit", limit);

        if (!tsq.isEmpty()) {
            ctes.add("""
                    fts AS (SELECT id, ROW_NUMBER() OVER (ORDER BY score DESC) AS r FROM (
                        SELECT t.id, ts_rank_cd(t.tsv, to_tsquery('simple', :tsq)) AS score FROM {T} t
                        WHERE t.tsv @@ to_tsquery('simple', :tsq) ORDER BY score DESC LIMIT {N}) x)""");
            unions.add("SELECT id, r FROM fts");
            p.addValue("tsq", tsq);
        }
        if (q.length() >= 3) {
            ctes.add("""
                    trg AS (SELECT id, ROW_NUMBER() OVER (ORDER BY sim DESC) AS r FROM (
                        SELECT t.id, similarity(t.title, :q) AS sim FROM {T} t
                        WHERE t.title % :q ORDER BY sim DESC LIMIT {N}) x)""");
            unions.add("SELECT id, r FROM trg");
            p.addValue("q", q);
        }
        if (vec != null) {
            ctes.add("""
                    sem AS (SELECT id, ROW_NUMBER() OVER (ORDER BY dist) AS r FROM (
                        SELECT t.id, t.embedding <=> CAST(:vec AS vector) AS dist FROM {T} t
                        WHERE t.embedding IS NOT NULL ORDER BY t.embedding <=> CAST(:vec AS vector) LIMIT {N}) x
                        WHERE dist < :maxDist)""");
            unions.add("SELECT id, r FROM sem");
            p.addValue("vec", EmbeddingService.literal(vec));
            p.addValue("maxDist", MAX_COSINE_DISTANCE);
        }
        if (ctes.isEmpty()) return List.of();

        String sql = ("WITH " + String.join(",\n", ctes) + ",\n"
                + "scored AS (SELECT id, SUM(1.0 / (60 + r)) AS score FROM (" + String.join(" UNION ALL ", unions)
                + ") u GROUP BY id ORDER BY score DESC LIMIT :limit)\n"
                + "SELECT s.id, {TITLE} AS title, left(coalesce(t.{BODY}, ''), 300) AS snippet, s.score\n"
                + "FROM scored s JOIN {T} t ON t.id = s.id ORDER BY s.score DESC")
                .replace("{N}", String.valueOf(PER_SOURCE)).replace("{T}", s.table).replace("{TITLE}", s.titleExpr).replace("{BODY}", s.bodyColumn);

        return jdbc.query(sql, p, (rs, i) ->
                new SearchHit(s.type, rs.getLong("id"), rs.getString("title"), rs.getString("snippet"), rs.getDouble("score")));
    }
}
