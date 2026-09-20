package com.contractguard.platform.contract;

import java.time.LocalDateTime;

public class ContractClauseRow {
    private Long id;
    private String clauseType;
    private String clauseTitle;
    private String clauseContent;
    private String sourceType;
    private Integer sourcePageNo;
    private String sourceExcerpt;
    private String confirmationStatus;
    private LocalDateTime confirmedAt;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getClauseType() { return clauseType; } public void setClauseType(String clauseType) { this.clauseType = clauseType; }
    public String getClauseTitle() { return clauseTitle; } public void setClauseTitle(String clauseTitle) { this.clauseTitle = clauseTitle; }
    public String getClauseContent() { return clauseContent; } public void setClauseContent(String clauseContent) { this.clauseContent = clauseContent; }
    public String getSourceType() { return sourceType; } public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Integer getSourcePageNo() { return sourcePageNo; } public void setSourcePageNo(Integer sourcePageNo) { this.sourcePageNo = sourcePageNo; }
    public String getSourceExcerpt() { return sourceExcerpt; } public void setSourceExcerpt(String sourceExcerpt) { this.sourceExcerpt = sourceExcerpt; }
    public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String confirmationStatus) { this.confirmationStatus = confirmationStatus; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; } public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}

