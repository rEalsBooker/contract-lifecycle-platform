package com.contractguard.platform.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditMapper auditMapper;

    public AuditService(AuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    public void record(Long tenantId, Long userId, Long membershipId, String action, String objectType, String objectId) {
        auditMapper.insert(tenantId, userId, membershipId, action, objectType, objectId, "SUCCESS", "{}");
    }
}

