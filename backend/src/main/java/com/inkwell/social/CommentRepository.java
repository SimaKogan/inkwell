package com.inkwell.social;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findTop500ByPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);

    @Query("select c.post.id, count(c) from Comment c where c.post.id in :ids group by c.post.id")
    List<Object[]> countByPostIds(@Param("ids") Collection<Long> ids);
}
