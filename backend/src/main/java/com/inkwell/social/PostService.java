package com.inkwell.social;

import com.inkwell.catalog.CatalogDtos.ItemBrief;
import com.inkwell.catalog.CatalogItemRepository;
import com.inkwell.common.ApiException;
import com.inkwell.common.ContentChangedEvent;
import com.inkwell.security.AuthUser;
import com.inkwell.social.SocialDtos.*;
import com.inkwell.user.UserDtos.UserBrief;
import com.inkwell.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository posts;
    private final CommentRepository comments;
    private final PostLikeRepository likes;
    private final FollowRepository follows;
    private final UserRepository users;
    private final CatalogItemRepository items;
    private final ApplicationEventPublisher events;

    public PostService(PostRepository posts, CommentRepository comments, PostLikeRepository likes,
                       FollowRepository follows, UserRepository users, CatalogItemRepository items,
                       ApplicationEventPublisher events) {
        this.posts = posts;
        this.comments = comments;
        this.likes = likes;
        this.follows = follows;
        this.users = users;
        this.items = items;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<PostDto> list(Long authorId, Long itemId, Pageable pageable, Long meId) {
        Specification<Post> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (authorId != null) ps.add(cb.equal(root.get("author").get("id"), authorId));
            if (itemId != null) ps.add(cb.equal(root.get("catalogItem").get("id"), itemId));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return toPage(posts.findAll(spec, newest(pageable)), meId);
    }

    @Transactional(readOnly = true)
    public Page<PostDto> feed(Long meId, Pageable pageable) {
        List<Long> authors = new ArrayList<>(follows.followeeIds(meId));
        authors.add(meId);
        return toPage(posts.findByAuthorIdIn(authors, newest(pageable)), meId);
    }

    @Transactional(readOnly = true)
    public PostDto get(Long id, Long meId) {
        Post p = posts.findById(id).orElseThrow(() -> ApiException.notFound("Post"));
        return toDtos(List.of(p), meId).get(0);
    }

    @Transactional
    public PostDto create(PostRequest r, AuthUser me) {
        Post p = new Post();
        p.setAuthor(users.getReferenceById(me.id()));
        p.setTitle(r.title() == null || r.title().isBlank() ? null : r.title().trim());
        p.setBody(r.body().trim());
        p.setSource(r.source() == null ? PostSource.TYPED : r.source());
        if (r.catalogItemId() != null) {
            p.setCatalogItem(items.findById(r.catalogItemId()).orElseThrow(() -> ApiException.notFound("Item")));
        }
        posts.save(p);
        events.publishEvent(new ContentChangedEvent("post", p.getId()));
        return get(p.getId(), me.id());
    }

    @Transactional
    public void delete(Long id, AuthUser me) {
        Post p = posts.findById(id).orElseThrow(() -> ApiException.notFound("Post"));
        if (!p.getAuthor().getId().equals(me.id()) && !me.isAdmin()) throw ApiException.forbidden();
        posts.delete(p);
    }

    @Transactional
    public LikeResult toggleLike(Long postId, AuthUser me) {
        if (!posts.existsById(postId)) throw ApiException.notFound("Post");
        Optional<PostLike> existing = likes.findByPostIdAndUserId(postId, me.id());
        boolean liked;
        if (existing.isPresent()) {
            likes.delete(existing.get());
            liked = false;
        } else {
            PostLike l = new PostLike();
            l.setPost(posts.getReferenceById(postId));
            l.setUser(users.getReferenceById(me.id()));
            likes.save(l);
            liked = true;
        }
        likes.flush();
        return new LikeResult(liked, likes.countByPostId(postId));
    }

    @Transactional(readOnly = true)
    public List<CommentDto> comments(Long postId) {
        if (!posts.existsById(postId)) throw ApiException.notFound("Post");
        return comments.findTop500ByPostIdOrderByCreatedAtAsc(postId).stream().map(this::toDto).toList();
    }

    @Transactional
    public CommentDto addComment(Long postId, CommentRequest r, AuthUser me) {
        Post post = posts.findById(postId).orElseThrow(() -> ApiException.notFound("Post"));
        Comment c = new Comment();
        c.setPost(post);
        c.setAuthor(users.getReferenceById(me.id()));
        c.setBody(r.body().trim());
        if (r.parentId() != null) {
            Comment parent = comments.findById(r.parentId()).orElseThrow(() -> ApiException.notFound("Parent comment"));
            if (!parent.getPost().getId().equals(postId)) throw ApiException.badRequest("Parent comment belongs to another post");
            c.setParent(parent);
        }
        comments.saveAndFlush(c);
        return new CommentDto(c.getId(), postId, r.parentId(), c.getBody(), c.getCreatedAt(),
                new UserBrief(me.id(), me.username(), users.getReferenceById(me.id()).getDisplayName()));
    }

    @Transactional
    public void deleteComment(Long commentId, AuthUser me) {
        Comment c = comments.findById(commentId).orElseThrow(() -> ApiException.notFound("Comment"));
        boolean allowed = c.getAuthor().getId().equals(me.id())
                || c.getPost().getAuthor().getId().equals(me.id()) || me.isAdmin();
        if (!allowed) throw ApiException.forbidden();
        comments.delete(c);
    }

    private Pageable newest(Pageable p) {
        return PageRequest.of(p.getPageNumber(), Math.min(p.getPageSize(), 50), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Page<PostDto> toPage(Page<Post> page, Long meId) {
        return new PageImpl<>(toDtos(page.getContent(), meId), page.getPageable(), page.getTotalElements());
    }

    private List<PostDto> toDtos(List<Post> list, Long meId) {
        if (list.isEmpty()) return List.of();
        List<Long> ids = list.stream().map(Post::getId).toList();
        Map<Long, Long> likeCounts = asMap(likes.countByPostIds(ids));
        Map<Long, Long> commentCounts = asMap(comments.countByPostIds(ids));
        Set<Long> liked = meId == null ? Set.of() : new HashSet<>(likes.likedPostIds(meId, ids));
        return list.stream().map(p -> new PostDto(p.getId(), p.getTitle(), p.getBody(), p.getSource(), p.getCreatedAt(),
                UserBrief.of(p.getAuthor()),
                p.getCatalogItem() == null ? null : new ItemBrief(p.getCatalogItem().getId(), p.getCatalogItem().getTitle()),
                likeCounts.getOrDefault(p.getId(), 0L), commentCounts.getOrDefault(p.getId(), 0L),
                liked.contains(p.getId()))).toList();
    }

    private CommentDto toDto(Comment c) {
        return new CommentDto(c.getId(), c.getPost().getId(), c.getParent() == null ? null : c.getParent().getId(),
                c.getBody(), c.getCreatedAt(), UserBrief.of(c.getAuthor()));
    }

    private static Map<Long, Long> asMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }
}
