package com.contractguard.platform.fulfillment;

import java.time.LocalDate;
public record TaskResponse(Long id, Long contractId, Long sourceClauseId, String contractNo, String contractName,
                           String title, String taskDescription, String taskStatus, String taskOrigin,
                           LocalDate dueDate, LocalDate contractualDueDate, LocalDate internalPlanDate,
                           Long assigneeMembershipId, Long reviewerMembershipId) {
    public static TaskResponse from(TaskRow row) { return new TaskResponse(row.getId(), row.getContractId(), row.getSourceClauseId(), row.getContractNo(), row.getContractName(), row.getTitle(), row.getTaskDescription(), row.getTaskStatus(), row.getTaskOrigin(), row.getDueDate(), row.getContractualDueDate(), row.getInternalPlanDate(), row.getAssigneeMembershipId(), row.getReviewerMembershipId()); }
}

