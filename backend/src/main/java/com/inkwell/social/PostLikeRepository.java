package com.inkwell.social;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    @Query("select l.post.id, count(l) from PostLike l where l.post.id in :ids group by l.post.id")
    List<Object[]> countByPostIds(@Param("ids") Collection<Long> ids);

    @Query("select l.post.id from PostLike l where l.user.id = :uid and l.post.id in :ids")
    List<Long> likedPostIds(@Param("uid") Long userId, @Param("ids") Collection<Long> ids);
}
