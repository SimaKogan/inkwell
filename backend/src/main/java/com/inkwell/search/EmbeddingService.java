package com.inkwell.search;

import com.inkwell.common.ContentChangedEvent;
import com.inkwell.ml.MlClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps the pgvector column in sync. Embeddings are computed after commit, off the request thread;
 * a scheduled sweep catches rows that were saved while the ML service was down.
 */
@Service
public class EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    // Table names come from this fixed map only, never from user input.
    private static final Map<String, String> TEXT_SQL = Map.of(
            "catalog_item", "SELECT title || E'\\n' || coalesce(description, '') || E'\\n' || category FROM catalog_item WHERE id = ?",
            "post", "SELECT coalesce(title, '') || E'\\n' || body FROM post WHERE id = ?");

    private final JdbcTemplate jdbc;
    private final MlClient ml;

    public EmbeddingService(JdbcTemplate jdbc, MlClient ml) {
        this.jdbc = jdbc;
        this.ml = ml;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChange(ContentChangedEvent e) { index(e.table(), e.id()); }

    public boolean index(String table, long id) {
        String sql = TEXT_SQL.get(table);
        if (sql == null) throw new IllegalArgumentException("Unknown table " + table);
        List<String> rows = jdbc.query(sql, (rs, i) -> rs.getString(1), id);
        if (rows.isEmpty()) return true; // deleted meanwhile
        Optional<float[]> vec = ml.embed(rows.get(0));
        if (vec.isEmpty()) return false;
        jdbc.update("UPDATE " + table + " SET embedding = CAST(? AS vector) WHERE id = ?", literal(vec.get()), id);
        return true;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 45_000)
    public int reindexMissing() {
        int done = 0;
        for (String table : TEXT_SQL.keySet()) {
            List<Long> ids = jdbc.queryForList("SELECT id FROM " + table + " WHERE embedding IS NULL ORDER BY id LIMIT 100", Long.class);
            for (Long id : ids) {
                if (!index(table, id)) {
                    log.info("ML service unavailable, will retry embeddings later");
                    return done;
                }
                done++;
            }
        }
        if (done > 0) log.info("Indexed {} rows", done);
        return done;
    }

    public static String literal(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
