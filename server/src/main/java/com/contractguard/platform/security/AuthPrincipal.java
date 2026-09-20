package com.contractguard.platform.security;

import java.util.List;

public record AuthPrincipal(Long userId, Long membershipId, Long tenantId, List<String> roleCodes, String tokenType) {
    public boolean hasWorkspace() {
        return membershipId != null && tenantId != null && "WORKSPACE".equals(tokenType);
    }
}

