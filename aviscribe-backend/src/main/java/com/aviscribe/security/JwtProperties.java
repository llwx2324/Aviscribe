package com.aviscribe.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aviscribe.security.jwt")
public class JwtProperties {
    private String secret;
    private long accessExpMinutes;
    private long refreshExpDays;
}
