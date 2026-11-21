package com.inkwell.catalog;

import com.inkwell.catalog.CatalogDtos.*;
import com.inkwell.security.AuthUser;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;
    private final ReportService reports;
    private final AuthorRepository authors;

    public CatalogController(CatalogService catalog, ReportService reports, AuthorRepository authors) {
        this.catalog = catalog;
        this.reports = reports;
        this.authors = authors;
    }

    @GetMapping("/catalog")
    public Page<ItemDto> list(@RequestParam(required = false) String q,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) BigDecimal minPrice,
                              @RequestParam(required = false) BigDecimal maxPrice,
                              @PageableDefault(size = 12) Pageable pageable) {
        return catalog.list(q, category, minPrice, maxPrice, pageable);
    }

    @GetMapping("/catalog/categories")
    public List<String> categories() { return catalog.categories(); }

    @GetMapping("/catalog/report")
    public ResponseEntity<String> report(@RequestParam(defaultValue = "html") String format) {
        if ("csv".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inkwell-catalog.csv\"")
                    .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                    .body(reports.csv());
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(reports.html());
    }

    @GetMapping("/catalog/{id}")
    public ItemDto get(@PathVariable Long id) { return catalog.get(id); }

    @PostMapping("/catalog")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@Valid @RequestBody ItemRequest req, @AuthenticationPrincipal AuthUser me) {
        return catalog.create(req, me);
    }

    @PutMapping("/catalog/{id}")
    public ItemDto update(@PathVariable Long id, @Valid @RequestBody ItemRequest req, @AuthenticationPrincipal AuthUser me) {
        return catalog.update(id, req, me);
    }

    @DeleteMapping("/catalog/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { catalog.delete(id, me); }

    @GetMapping("/authors")
    public List<String> authorNames(@RequestParam(defaultValue = "") String q) {
        return authors.findByNameContainingIgnoreCaseOrderByName(q.trim(), Pageable.ofSize(20))
                .stream().map(Author::getName).toList();
    }
}
