package com.contractguard.platform.identity;

import java.util.List;

public record WorkspaceResponse(Long membershipId, Long tenantId, String tenantName, List<String> roleCodes) { }

