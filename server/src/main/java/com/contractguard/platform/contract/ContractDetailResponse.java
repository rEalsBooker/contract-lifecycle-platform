package com.contractguard.platform.contract;

import java.math.BigDecimal;
import java.util.List;

public record ContractDetailResponse(Long id, String contractNo, String name, String counterpartyName, BigDecimal totalAmount,
                                     String businessStatus, String archiveStatus, String sourceFilename, ParseJobResponse parseJob,
                                     List<ClauseResponse> clauses) {
    static ContractDetailResponse of(ContractRow contract, ParseJobRow job, List<ContractClauseRow> clauses) {
        return new ContractDetailResponse(contract.getId(), contract.getContractNo(), contract.getName(), contract.getCounterpartyName(),
                contract.getTotalAmount(), contract.getBusinessStatus(), contract.getArchiveStatus(), contract.getOriginalFilename(),
                job == null ? null : ParseJobResponse.from(job), clauses.stream().map(ClauseResponse::from).toList());
    }

    record ParseJobResponse(Long id, String jobStatus, String executionMode, String failureMessage) {
        static ParseJobResponse from(ParseJobRow row) { return new ParseJobResponse(row.getId(), row.getJobStatus(), row.getExecutionMode(), row.getFailureMessage()); }
    }
    record ClauseResponse(Long id, String clauseType, String clauseTitle, String clauseContent, String sourceType,
                          Integer sourcePageNo, String sourceExcerpt, String confirmationStatus) {
        static ClauseResponse from(ContractClauseRow row) { return new ClauseResponse(row.getId(), row.getClauseType(), row.getClauseTitle(), row.getClauseContent(), row.getSourceType(), row.getSourcePageNo(), row.getSourceExcerpt(), row.getConfirmationStatus()); }
    }
}

