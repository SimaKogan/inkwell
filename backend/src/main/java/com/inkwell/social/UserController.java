package com.inkwell.social;

import com.inkwell.common.ApiException;
import com.inkwell.security.AuthUser;
import com.inkwell.social.SocialDtos.FollowResult;
import com.inkwell.user.User;
import com.inkwell.user.UserDtos.*;
import com.inkwell.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    private final FollowRepository follows;
    private final PostRepository posts;

    public UserController(UserRepository users, FollowRepository follows, PostRepository posts) {
        this.users = users;
        this.follows = follows;
        this.posts = posts;
    }

    @GetMapping("/{username}")
    @Transactional(readOnly = true)
    public ProfileDto profile(@PathVariable String username, @AuthenticationPrincipal AuthUser me) {
        User u = users.findByUsernameIgnoreCase(username).orElseThrow(() -> ApiException.notFound("User"));
        boolean followed = me != null && follows.existsByFollowerIdAndFolloweeId(me.id(), u.getId());
        return new ProfileDto(UserDto.of(u), follows.countByFolloweeId(u.getId()), follows.countByFollowerId(u.getId()),
                posts.countByAuthorId(u.getId()), followed);
    }

    @PutMapping("/me")
    @Transactional
    public UserDto update(@Valid @RequestBody UpdateProfileRequest req, @AuthenticationPrincipal AuthUser me) {
        User u = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));
        u.setDisplayName(req.displayName().trim());
        u.setBio(req.bio() == null || req.bio().isBlank() ? null : req.bio().trim());
        return UserDto.of(u);
    }

    @PostMapping("/{username}/follow")
    @Transactional
    public FollowResult toggleFollow(@PathVariable String username, @AuthenticationPrincipal AuthUser me) {
        User target = users.findByUsernameIgnoreCase(username).orElseThrow(() -> ApiException.notFound("User"));
        if (target.getId().equals(me.id())) throw ApiException.badRequest("You cannot follow yourself");
        var existing = follows.findByFollowerIdAndFolloweeId(me.id(), target.getId());
        boolean following;
        if (existing.isPresent()) {
            follows.delete(existing.get());
            following = false;
        } else {
            Follow f = new Follow();
            f.setFollower(users.getReferenceById(me.id()));
            f.setFollowee(target);
            follows.save(f);
            following = true;
        }
        follows.flush();
        return new FollowResult(following, follows.countByFolloweeId(target.getId()));
    }
}
