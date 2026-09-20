package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AiFindingRow {
    private Long id; private Long contractId; private Long contractVersionId; private Long parseJobId;
    private String findingType; private String title; private String content; private Integer sourcePageNo;
    private String sourceExcerpt; private BigDecimal confidence; private String reviewStatus; private String reviewNote;
    private LocalDateTime createdAt; private LocalDateTime reviewedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getContractId(){return contractId;} public void setContractId(Long v){contractId=v;}
    public Long getContractVersionId(){return contractVersionId;} public void setContractVersionId(Long v){contractVersionId=v;} public Long getParseJobId(){return parseJobId;} public void setParseJobId(Long v){parseJobId=v;}
    public String getFindingType(){return findingType;} public void setFindingType(String v){findingType=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;} public Integer getSourcePageNo(){return sourcePageNo;} public void setSourcePageNo(Integer v){sourcePageNo=v;}
    public String getSourceExcerpt(){return sourceExcerpt;} public void setSourceExcerpt(String v){sourceExcerpt=v;} public BigDecimal getConfidence(){return confidence;} public void setConfidence(BigDecimal v){confidence=v;}
    public String getReviewStatus(){return reviewStatus;} public void setReviewStatus(String v){reviewStatus=v;} public String getReviewNote(){return reviewNote;} public void setReviewNote(String v){reviewNote=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(LocalDateTime v){reviewedAt=v;}
}


