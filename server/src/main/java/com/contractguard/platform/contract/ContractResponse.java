package com.contractguard.platform.contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContractResponse(Long id, String contractNo, String name, String counterpartyName, BigDecimal totalAmount,
                               String businessStatus, String archiveStatus, String sourceFilename, LocalDateTime updatedAt) {
    static ContractResponse from(ContractRow row) {
        return new ContractResponse(row.getId(), row.getContractNo(), row.getName(), row.getCounterpartyName(),
                row.getTotalAmount(), row.getBusinessStatus(), row.getArchiveStatus(), row.getOriginalFilename(), row.getUpdatedAt());
    }
}

