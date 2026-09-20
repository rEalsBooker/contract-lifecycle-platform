package com.contractguard.platform.contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ContractRow {
    private Long id;
    private Long tenantId;
    private Long ownerMembershipId;
    private String contractNo;
    private String name;
    private String counterpartyName;
    private BigDecimal totalAmount;
    private String businessStatus;
    private String archiveStatus;
    private Long currentVersionId;
    private Long sourceFileId;
    private String originalFilename;
    private String contentType;
    private String storageKey;
    private LocalDateTime updatedAt;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; } public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getOwnerMembershipId() { return ownerMembershipId; } public void setOwnerMembershipId(Long ownerMembershipId) { this.ownerMembershipId = ownerMembershipId; }
    public String getContractNo() { return contractNo; } public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCounterpartyName() { return counterpartyName; } public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public BigDecimal getTotalAmount() { return totalAmount; } public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getBusinessStatus() { return businessStatus; } public void setBusinessStatus(String businessStatus) { this.businessStatus = businessStatus; }
    public String getArchiveStatus() { return archiveStatus; } public void setArchiveStatus(String archiveStatus) { this.archiveStatus = archiveStatus; }
    public Long getCurrentVersionId() { return currentVersionId; } public void setCurrentVersionId(Long currentVersionId) { this.currentVersionId = currentVersionId; }
    public Long getSourceFileId() { return sourceFileId; } public void setSourceFileId(Long sourceFileId) { this.sourceFileId = sourceFileId; }
    public String getOriginalFilename() { return originalFilename; } public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getContentType() { return contentType; } public void setContentType(String contentType) { this.contentType = contentType; }
    public String getStorageKey() { return storageKey; } public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

