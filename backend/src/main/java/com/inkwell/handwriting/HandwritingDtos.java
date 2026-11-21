package com.inkwell.handwriting;

import com.inkwell.ml.MlClient.SheetReport;
import jakarta.validation.constraints.*;
import java.util.List;

public final class HandwritingDtos {
    private HandwritingDtos() {}

    public record RenderRequest(
            @NotBlank @Size(max = 2000) String text,
            @Pattern(regexp = "ruled|plain|transparent") String paper,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String inkColor,
            @Min(30) @Max(140) Integer size,
            @Min(600) @Max(2400) Integer width,
            Long seed) {}

    /** image is a data: URL (PNG) ready for an <img> tag; missing = characters the profile has no glyph for. */
    public record RenderResult(String image, int width, int height, String missing) {}

    public record ProfileCreated(ProfileDto profile, List<SheetReport> sheets) {}

    public record RecognizeResult(String text, String language, String engine) {}
}
