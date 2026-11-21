package com.inkwell.search;

import com.inkwell.common.ApiException;
import com.inkwell.search.SearchDtos.*;
import com.inkwell.security.AuthUser;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService search;
    private final EmbeddingService embeddings;

    public SearchController(SearchService search, EmbeddingService embeddings) {
        this.search = search;
        this.embeddings = embeddings;
    }

    @GetMapping
    public SearchResponse search(@RequestParam String q,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(defaultValue = "12") int limit) {
        return search.search(q, type, limit);
    }

    /** Grounded answer over the top hits (requires a signed-in user: it costs LLM tokens). */
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest req) { return search.ask(req.query()); }

    @PostMapping("/reindex")
    public Map<String, Integer> reindex(@AuthenticationPrincipal AuthUser me) {
        if (!me.isAdmin()) throw ApiException.forbidden();
        return Map.of("indexed", embeddings.reindexMissing());
    }
}
