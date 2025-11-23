package com.aviscribe.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aviscribe.security.rate-limit")
public class RateLimitProperties {
    private AuthRateLimit auth = new AuthRateLimit();

    @Data
    public static class AuthRateLimit {
        private int maxRequests = 10;
        private long windowSeconds = 60;
    }
}
