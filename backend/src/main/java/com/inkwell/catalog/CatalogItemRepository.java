package com.inkwell.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long>, JpaSpecificationExecutor<CatalogItem> {

    @Override
    @EntityGraph(attributePaths = "owner")
    Page<CatalogItem> findAll(Specification<CatalogItem> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<CatalogItem> findById(Long id);

    @EntityGraph(attributePaths = "owner")
    List<CatalogItem> findAllByOrderByTitleAsc();

    Optional<CatalogItem> findByIsbn(String isbn);

    @Query("select distinct c.category from CatalogItem c order by c.category")
    List<String> findCategories();
}
