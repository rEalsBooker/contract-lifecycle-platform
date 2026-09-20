package com.contractguard.platform.identity;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, String displayName) { }

