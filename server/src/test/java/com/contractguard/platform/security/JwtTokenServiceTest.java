package com.contractguard.platform.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {
    @Test
    void createsAndParsesWorkspaceToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-must-have-more-than-thirty-two-bytes-2026");
        properties.setExpirationMinutes(30);
        JwtTokenService service = new JwtTokenService(properties);

        String token = service.createWorkspaceToken(7L, "demo", 9L, 11L, List.of("FINANCE"));
        AuthPrincipal principal = service.parse(token);

        assertThat(principal.userId()).isEqualTo(7L);
        assertThat(principal.membershipId()).isEqualTo(9L);
        assertThat(principal.tenantId()).isEqualTo(11L);
        assertThat(principal.roleCodes()).containsExactly("FINANCE");
        assertThat(principal.hasWorkspace()).isTrue();
    }
}

