package com.inkwell.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {
    private UserDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 32) @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "letters, digits, _ . - only") String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 80) String displayName) {}

    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UpdateProfileRequest(@NotBlank @Size(max = 80) String displayName, @Size(max = 500) String bio) {}

    public record UserBrief(Long id, String username, String displayName) {
        public static UserBrief of(User u) { return new UserBrief(u.getId(), u.getUsername(), u.getDisplayName()); }
    }

    public record UserDto(Long id, String username, String displayName, String bio, Role role, Instant createdAt) {
        public static UserDto of(User u) {
            return new UserDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getBio(), u.getRole(), u.getCreatedAt());
        }
    }

    public record ProfileDto(UserDto user, long followers, long following, long posts, boolean followedByMe) {}

    public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}
}
