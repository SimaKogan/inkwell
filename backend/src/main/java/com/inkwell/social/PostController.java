package com.inkwell.social;

import com.inkwell.security.AuthUser;
import com.inkwell.social.SocialDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PostController {
    private final PostService service;

    public PostController(PostService service) { this.service = service; }

    private static Long id(AuthUser me) { return me == null ? null : me.id(); }

    @GetMapping("/posts")
    public Page<PostDto> list(@RequestParam(required = false) Long authorId,
                              @RequestParam(required = false) Long itemId,
                              @PageableDefault(size = 10) Pageable pageable,
                              @AuthenticationPrincipal AuthUser me) {
        return service.list(authorId, itemId, pageable, id(me));
    }

    @GetMapping("/posts/feed")
    public Page<PostDto> feed(@PageableDefault(size = 10) Pageable pageable, @AuthenticationPrincipal AuthUser me) {
        return service.feed(me.id(), pageable);
    }

    @GetMapping("/posts/{id}")
    public PostDto get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.get(id, id(me)); }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto create(@Valid @RequestBody PostRequest req, @AuthenticationPrincipal AuthUser me) {
        return service.create(req, me);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { service.delete(id, me); }

    @PostMapping("/posts/{id}/like")
    public LikeResult like(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.toggleLike(id, me); }

    @GetMapping("/posts/{id}/comments")
    public List<CommentDto> comments(@PathVariable Long id) { return service.comments(id); }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest req,
                                 @AuthenticationPrincipal AuthUser me) {
        return service.addComment(id, req, me);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { service.deleteComment(id, me); }
}
