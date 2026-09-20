package com.contractguard.platform.identity;

import java.time.Instant;
import java.util.List;

public record WorkspaceTokenResponse(String accessToken, String tokenType, Instant expiresAt, Long membershipId,
                                     Long tenantId, String tenantName, List<String> roleCodes) { }

