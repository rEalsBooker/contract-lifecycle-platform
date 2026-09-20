package com.contractguard.platform.contract;

import java.time.LocalDateTime;

public class ParseJobRow {
    private Long id;
    private String jobStatus;
    private String executionMode;
    private String failureMessage;
    private LocalDateTime createdAt;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getJobStatus() { return jobStatus; } public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }
    public String getExecutionMode() { return executionMode; } public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public String getFailureMessage() { return failureMessage; } public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

