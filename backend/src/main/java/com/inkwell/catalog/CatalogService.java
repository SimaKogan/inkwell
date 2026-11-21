package com.inkwell.catalog;

import com.inkwell.catalog.CatalogDtos.*;
import com.inkwell.common.ApiException;
import com.inkwell.common.ContentChangedEvent;
import com.inkwell.security.AuthUser;
import com.inkwell.user.UserDtos.UserBrief;
import com.inkwell.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private static final Set<String> SORTABLE = Set.of("title", "price", "createdAt", "publishedYear", "stock");

    private final CatalogItemRepository items;
    private final AuthorRepository authors;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public CatalogService(CatalogItemRepository items, AuthorRepository authors, UserRepository users,
                          ApplicationEventPublisher events) {
        this.items = items;
        this.authors = authors;
        this.users = users;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<ItemDto> list(String q, String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<CatalogItem> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (category != null && !category.isBlank()) {
                ps.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            if (minPrice != null) ps.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) ps.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(root.get("category")), like, '\\')));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return items.findAll(spec, safe(pageable)).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ItemDto get(Long id) {
        return toDto(items.findById(id).orElseThrow(() -> ApiException.notFound("Item")));
    }

    @Transactional(readOnly = true)
    public List<String> categories() { return items.findCategories(); }

    @Transactional(readOnly = true)
    public List<ItemDto> all() { return items.findAllByOrderByTitleAsc().stream().map(this::toDto).toList(); }

    @Transactional
    public ItemDto create(ItemRequest r, AuthUser me) {
        CatalogItem item = new CatalogItem();
        item.setOwner(users.getReferenceById(me.id()));
        apply(item, r);
        items.save(item);
        events.publishEvent(new ContentChangedEvent("catalog_item", item.getId()));
        return toDto(item);
    }

    @Transactional
    public ItemDto update(Long id, ItemRequest r, AuthUser me) {
        CatalogItem item = items.findById(id).orElseThrow(() -> ApiException.notFound("Item"));
        requireOwner(item, me);
        apply(item, r);
        item.setUpdatedAt(Instant.now());
        items.save(item);
        events.publishEvent(new ContentChangedEvent("catalog_item", item.getId()));
        return toDto(item);
    }

    @Transactional
    public void delete(Long id, AuthUser me) {
        CatalogItem item = items.findById(id).orElseThrow(() -> ApiException.notFound("Item"));
        requireOwner(item, me);
        items.delete(item);
    }

    private void requireOwner(CatalogItem item, AuthUser me) {
        if (!item.getOwner().getId().equals(me.id()) && !me.isAdmin()) throw ApiException.forbidden();
    }

    private void apply(CatalogItem item, ItemRequest r) {
        String isbn = r.isbn() == null || r.isbn().isBlank() ? null : r.isbn().trim().toUpperCase();
        if (isbn != null) {
            items.findByIsbn(isbn).filter(other -> !other.getId().equals(item.getId()))
                    .ifPresent(o -> { throw ApiException.conflict("An item with this ISBN already exists"); });
        }
        item.setTitle(r.title().trim());
        item.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        item.setCategory(r.category().trim());
        item.setIsbn(isbn);
        item.setPrice(r.price());
        item.setStock(r.stock() == null ? 0 : r.stock());
        item.setPublishedYear(r.publishedYear());
        Set<Author> resolved = new LinkedHashSet<>();
        if (r.authors() != null) {
            for (String name : r.authors()) {
                String n = name.trim();
                if (n.isEmpty()) continue;
                resolved.add(authors.findByNameIgnoreCase(n).orElseGet(() -> authors.save(new Author(n))));
            }
        }
        item.setAuthors(resolved);
    }

    private Pageable safe(Pageable p) {
        List<Sort.Order> orders = p.getSort().stream().filter(o -> SORTABLE.contains(o.getProperty())).toList();
        Sort sort = orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "createdAt") : Sort.by(orders);
        return PageRequest.of(p.getPageNumber(), Math.min(p.getPageSize(), 60), sort);
    }

    ItemDto toDto(CatalogItem c) {
        List<String> names = c.getAuthors().stream().map(Author::getName).sorted().toList();
        return new ItemDto(c.getId(), c.getTitle(), c.getDescription(), c.getCategory(), c.getIsbn(), c.getPrice(),
                c.getStock(), c.getPublishedYear(), names, UserBrief.of(c.getOwner()), c.getCreatedAt());
    }
}
