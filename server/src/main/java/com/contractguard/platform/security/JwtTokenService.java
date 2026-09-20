package com.contractguard.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {
    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    public String createIdentityToken(Long userId, String username) {
        return createToken(userId, username, null, null, List.of(), "IDENTITY");
    }

    public String createWorkspaceToken(Long userId, String username, Long membershipId, Long tenantId, List<String> roles) {
        return createToken(userId, username, membershipId, tenantId, roles, "WORKSPACE");
    }

    public AuthPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
        Object rawRoles = claims.get("roles");
        List<String> roles = rawRoles instanceof Collection<?> collection
                ? collection.stream().map(Object::toString).toList()
                : List.of();
        return new AuthPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("membershipId", Long.class),
                claims.get("tenantId", Long.class),
                roles,
                claims.get("type", String.class)
        );
    }

    public Instant expiresAt() {
        return Instant.now().plusSeconds(properties.getExpirationMinutes() * 60);
    }

    private String createToken(Long userId, String username, Long membershipId, Long tenantId, List<String> roles, String type) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", type)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getExpirationMinutes() * 60)))
                .signWith(signingKey());
        if (membershipId != null) builder.claim("membershipId", membershipId);
        if (tenantId != null) builder.claim("tenantId", tenantId);
        return builder.compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}

