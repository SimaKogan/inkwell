package com.inkwell.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inkwell")
public record AppProperties(Jwt jwt, Ml ml, Cors cors, Seed seed) {
    public record Jwt(String secret, Duration accessTtl, Duration refreshTtl) {}
    public record Ml(String baseUrl, Duration timeout) {}
    public record Cors(List<String> allowedOrigins) {}
    public record Seed(boolean enabled) {}
}
