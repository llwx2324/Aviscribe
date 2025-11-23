package com.aviscribe.security;

import com.aviscribe.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final JwtProperties properties;
    private final Key signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        if (!StringUtils.hasText(properties.getSecret()) || properties.getSecret().length() < 32) {
            throw new IllegalArgumentException("JWT secret 长度必须>=32");
        }
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair generateTokenPair(User user) {
        String accessToken = buildToken(user, properties.getAccessExpMinutes(), ChronoUnit.MINUTES, TYPE_ACCESS);
        String refreshToken = buildToken(user, properties.getRefreshExpDays(), ChronoUnit.DAYS, TYPE_REFRESH);
        long accessExp = Instant.now().plus(properties.getAccessExpMinutes(), ChronoUnit.MINUTES).getEpochSecond();
        long refreshExp = Instant.now().plus(properties.getRefreshExpDays(), ChronoUnit.DAYS).getEpochSecond();
        return new TokenPair(accessToken, refreshToken, accessExp, refreshExp);
    }

    private String buildToken(User user, long ttl, ChronoUnit unit, String type) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(ttl, unit);
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ROLE, user.getRole());
        claims.put(CLAIM_TYPE, type);
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expireAt))
                .addClaims(claims)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equalsIgnoreCase(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equalsIgnoreCase(getTokenType(token));
    }

    public Long extractUserId(String token) {
        Claims claims = parseClaims(token).getBody();
        return Long.parseLong(claims.getSubject());
    }

    private String getTokenType(String token) {
        Claims claims = parseClaims(token).getBody();
        return claims.get(CLAIM_TYPE, String.class);
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
    }

    public record TokenPair(String accessToken, String refreshToken, long accessExp, long refreshExp) {}
}
