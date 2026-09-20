package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AiFindingResponse(Long id, String findingType, String title, String content, Integer sourcePageNo,
                                String sourceExcerpt, BigDecimal confidence, String reviewStatus,
                                String reviewNote, LocalDateTime createdAt, LocalDateTime reviewedAt) {
    static AiFindingResponse from(AiFindingRow row) { return new AiFindingResponse(row.getId(), row.getFindingType(), row.getTitle(),
            row.getContent(), row.getSourcePageNo(), row.getSourceExcerpt(), row.getConfidence(), row.getReviewStatus(),
            row.getReviewNote(), row.getCreatedAt(), row.getReviewedAt()); }
}


