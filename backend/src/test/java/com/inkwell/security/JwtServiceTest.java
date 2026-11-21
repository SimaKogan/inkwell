package com.inkwell.security;

import static org.junit.jupiter.api.Assertions.*;

import com.inkwell.config.AppProperties;
import com.inkwell.user.Role;
import com.inkwell.user.User;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static JwtService service(Duration access) {
        return new JwtService(new AppProperties(
                new AppProperties.Jwt("0123456789abcdef0123456789abcdef-secret", access, Duration.ofDays(1)),
                null, null, null));
    }

    private static User user() {
        User u = new User();
        u.setId(7L);
        u.setUsername("maya");
        u.setRole(Role.ADMIN);
        return u;
    }

    @Test void roundTrip() {
        JwtService jwt = service(Duration.ofMinutes(5));
        AuthUser parsed = jwt.parse(jwt.issue(user(), JwtService.TokenType.ACCESS), JwtService.TokenType.ACCESS);
        assertEquals(7L, parsed.id());
        assertEquals("maya", parsed.username());
        assertTrue(parsed.isAdmin());
    }

    @Test void refreshTokenCannotBeUsedAsAccessToken() {
        JwtService jwt = service(Duration.ofMinutes(5));
        String refresh = jwt.issue(user(), JwtService.TokenType.REFRESH);
        assertThrows(JwtException.class, () -> jwt.parse(refresh, JwtService.TokenType.ACCESS));
    }

    @Test void expiredTokenIsRejected() {
        JwtService jwt = service(Duration.ofSeconds(-5));
        String token = jwt.issue(user(), JwtService.TokenType.ACCESS);
        assertThrows(JwtException.class, () -> jwt.parse(token, JwtService.TokenType.ACCESS));
    }

    @Test void tamperedTokenIsRejected() {
        JwtService jwt = service(Duration.ofMinutes(5));
        String token = jwt.issue(user(), JwtService.TokenType.ACCESS);
        String broken = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");
        assertThrows(JwtException.class, () -> jwt.parse(broken, JwtService.TokenType.ACCESS));
    }

    @Test void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtService(new AppProperties(
                new AppProperties.Jwt("short", Duration.ofMinutes(1), Duration.ofDays(1)), null, null, null)));
    }
}
