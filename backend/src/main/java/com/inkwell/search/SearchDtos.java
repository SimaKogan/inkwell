package com.inkwell.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class SearchDtos {
    private SearchDtos() {}

    public record SearchHit(String type, long id, String title, String snippet, double score) {}

    /** layoutCorrected = the query was re-typed from a wrong keyboard layout; semantic = vector search was used. */
    public record SearchResponse(String query, String effectiveQuery, boolean layoutCorrected, String language,
                                 boolean semantic, List<SearchHit> hits) {}

    public record AskRequest(@NotBlank @Size(max = 500) String query) {}

    public record AskResponse(String answer, String effectiveQuery, List<SearchHit> sources) {}
}
