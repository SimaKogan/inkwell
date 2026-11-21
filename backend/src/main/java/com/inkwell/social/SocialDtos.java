package com.inkwell.social;

import com.inkwell.catalog.CatalogDtos.ItemBrief;
import com.inkwell.user.UserDtos.UserBrief;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class SocialDtos {
    private SocialDtos() {}

    public record PostRequest(@Size(max = 200) String title,
                              @NotBlank @Size(max = 10000) String body,
                              Long catalogItemId,
                              PostSource source) {}

    public record PostDto(Long id, String title, String body, PostSource source, Instant createdAt,
                          UserBrief author, ItemBrief item, long likeCount, long commentCount, boolean likedByMe) {}

    public record CommentRequest(@NotBlank @Size(max = 4000) String body, Long parentId) {}

    public record CommentDto(Long id, Long postId, Long parentId, String body, Instant createdAt, UserBrief author) {}

    public record LikeResult(boolean liked, long likeCount) {}

    public record FollowResult(boolean following, long followers) {}
}
