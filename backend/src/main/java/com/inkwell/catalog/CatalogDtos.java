package com.inkwell.catalog;

import com.inkwell.user.UserDtos.UserBrief;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CatalogDtos {
    private CatalogDtos() {}

    public record ItemRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5000) String description,
            @NotBlank @Size(max = 80) String category,
            @Pattern(regexp = "^[0-9Xx-]{10,20}$", message = "ISBN: 10–20 digits or dashes") String isbn,
            @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
            @Min(0) Integer stock,
            @Min(0) @Max(2100) Integer publishedYear,
            @Size(max = 10) List<@NotBlank @Size(max = 160) String> authors) {}

    public record ItemDto(Long id, String title, String description, String category, String isbn,
                          BigDecimal price, int stock, Integer publishedYear, List<String> authors,
                          UserBrief owner, Instant createdAt) {}

    public record ItemBrief(Long id, String title) {}
}
