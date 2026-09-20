package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiParsePersistence {
    private final AiParseMapper mapper;

    public AiParsePersistence(AiParseMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void saveSuccess(ParseJobExecutionRow row, ContractExtractionResult result) {
        for (ExtractedFinding finding : result.findings()) mapper.insertFinding(row, finding, result.rawJson());
        if (mapper.succeed(row.getTenantId(), row.getId()) == 0) throw new IllegalStateException("解析作业状态已变化");
    }

    @Transactional
    public void saveFailure(ParseJobExecutionRow row, String message) {
        mapper.fail(row.getTenantId(), row.getId(), row.getAttemptCount() < 3 ? "RETRY_WAIT" : "FAILED", message);
    }
}


