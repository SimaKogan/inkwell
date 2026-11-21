package com.inkwell.user;

import com.inkwell.common.ApiException;
import com.inkwell.security.AuthUser;
import com.inkwell.security.JwtService;
import com.inkwell.security.JwtService.TokenType;
import com.inkwell.user.UserDtos.*;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByUsernameIgnoreCase(req.username())) throw ApiException.conflict("Username is already taken");
        if (users.existsByEmailIgnoreCase(req.email())) throw ApiException.conflict("Email is already registered");
        User u = new User();
        u.setUsername(req.username());
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setDisplayName(req.displayName() == null || req.displayName().isBlank() ? req.username() : req.displayName().trim());
        users.save(u);
        return tokens(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User u = users.findByUsernameIgnoreCase(req.identifier())
                .or(() -> users.findByEmailIgnoreCase(req.identifier()))
                .filter(found -> encoder.matches(req.password(), found.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong username or password"));
        return tokens(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest req) {
        AuthUser claims;
        try {
            claims = jwt.parse(req.refreshToken(), TokenType.REFRESH);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired, please sign in again");
        }
        User u = users.findById(claims.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired, please sign in again"));
        return tokens(u);
    }

    private AuthResponse tokens(User u) {
        return new AuthResponse(jwt.issue(u, TokenType.ACCESS), jwt.issue(u, TokenType.REFRESH), UserDto.of(u));
    }
}
