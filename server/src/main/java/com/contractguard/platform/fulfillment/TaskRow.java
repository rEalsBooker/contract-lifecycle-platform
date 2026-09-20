package com.contractguard.platform.fulfillment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskRow {
    private Long id;
    private Long contractId;
    private Long sourceClauseId;
    private String contractNo;
    private String contractName;
    private String title;
    private String taskDescription;
    private String taskStatus;
    private String taskOrigin;
    private LocalDate dueDate;
    private LocalDate contractualDueDate;
    private LocalDate internalPlanDate;
    private Long assigneeMembershipId;
    private Long reviewerMembershipId;
    private LocalDateTime completedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getContractId() { return contractId; } public void setContractId(Long contractId) { this.contractId = contractId; }
    public Long getSourceClauseId() { return sourceClauseId; } public void setSourceClauseId(Long sourceClauseId) { this.sourceClauseId = sourceClauseId; }
    public String getContractNo() { return contractNo; } public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public String getContractName() { return contractName; } public void setContractName(String contractName) { this.contractName = contractName; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getTaskDescription() { return taskDescription; } public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
    public String getTaskStatus() { return taskStatus; } public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }
    public String getTaskOrigin() { return taskOrigin; } public void setTaskOrigin(String taskOrigin) { this.taskOrigin = taskOrigin; }
    public LocalDate getDueDate() { return dueDate; } public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getContractualDueDate(){return contractualDueDate;} public void setContractualDueDate(LocalDate v){contractualDueDate=v;}
    public LocalDate getInternalPlanDate(){return internalPlanDate;} public void setInternalPlanDate(LocalDate v){internalPlanDate=v;}
    public Long getAssigneeMembershipId() { return assigneeMembershipId; } public void setAssigneeMembershipId(Long assigneeMembershipId) { this.assigneeMembershipId = assigneeMembershipId; }
    public Long getReviewerMembershipId() { return reviewerMembershipId; } public void setReviewerMembershipId(Long reviewerMembershipId) { this.reviewerMembershipId = reviewerMembershipId; }
    public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}

