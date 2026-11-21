package com.inkwell.security;

import com.inkwell.config.AppProperties;
import com.inkwell.user.Role;
import com.inkwell.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public enum TokenType { ACCESS, REFRESH }

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(AppProperties props) {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("inkwell.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTtl = props.jwt().accessTtl();
        this.refreshTtl = props.jwt().refreshTtl();
    }

    public String issue(User user, TokenType type) {
        Instant now = Instant.now();
        Duration ttl = type == TokenType.ACCESS ? accessTtl : refreshTtl;
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("typ", type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Parses and validates a token of the expected type; throws JwtException otherwise. */
    public AuthUser parse(String token, TokenType expected) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!expected.name().equals(c.get("typ", String.class))) {
            throw new JwtException("Wrong token type");
        }
        return new AuthUser(Long.valueOf(c.getSubject()), c.get("username", String.class),
                Role.valueOf(c.get("role", String.class)));
    }
}
