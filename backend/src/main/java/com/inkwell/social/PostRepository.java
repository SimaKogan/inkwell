package com.inkwell.social;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @Override
    @EntityGraph(attributePaths = {"author", "catalogItem"})
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author", "catalogItem"})
    Optional<Post> findById(Long id);

    @EntityGraph(attributePaths = {"author", "catalogItem"})
    Page<Post> findByAuthorIdIn(Collection<Long> authorIds, Pageable pageable);

    long countByAuthorId(Long authorId);
}
