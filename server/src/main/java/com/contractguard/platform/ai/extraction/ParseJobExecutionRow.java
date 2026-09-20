package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

public class ParseJobExecutionRow {
    private Long id;
    private Long tenantId;
    private Long contractId;
    private Long contractVersionId;
    private Long createdByMembershipId;
    private Long sourceFileId;
    private String storageKey;
    private String contentType;
    private int attemptCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public Long getContractVersionId() { return contractVersionId; }
    public void setContractVersionId(Long contractVersionId) { this.contractVersionId = contractVersionId; }
    public Long getCreatedByMembershipId() { return createdByMembershipId; }
    public void setCreatedByMembershipId(Long createdByMembershipId) { this.createdByMembershipId = createdByMembershipId; }
    public Long getSourceFileId() { return sourceFileId; }
    public void setSourceFileId(Long sourceFileId) { this.sourceFileId = sourceFileId; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
}


