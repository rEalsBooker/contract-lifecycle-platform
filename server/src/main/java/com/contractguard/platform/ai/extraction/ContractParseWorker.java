package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.notification.OutboxService;
import com.contractguard.platform.storage.PrivateFileStorage;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class ContractParseWorker {
    private final AiParseMapper mapper;
    private final AiParsePersistence persistence;
    private final ContractExtractionModel model;
    private final ContractTextExtractor extractor;
    private final PrivateFileStorage storage;
    private final OutboxService outbox;
    private final ContractRagService rag;

    public ContractParseWorker(AiParseMapper mapper, AiParsePersistence persistence, ContractExtractionModel model,
                               ContractTextExtractor extractor, PrivateFileStorage storage, OutboxService outbox,
                               ContractRagService rag) {
        this.mapper = mapper; this.persistence = persistence; this.model = model; this.extractor = extractor;
        this.storage = storage; this.outbox = outbox; this.rag = rag;
    }

    public void execute(ContractParseEventPayload event) {
        if (mapper.claim(event.tenantId(), event.parseJobId()) == 0) return;
        ParseJobExecutionRow row = mapper.findExecution(event.tenantId(), event.parseJobId());
        if (row == null) throw new IllegalStateException("解析作业或合同文件不存在");
        try (InputStream stream = storage.open(row.getStorageKey())) {
            if (!model.available()) throw new IllegalStateException("AI 模型未配置，已保留人工审阅模式");
            String text = extractor.extract(stream, row.getContentType());
            ContractExtractionResult result = model.extract(text);
            persistence.saveSuccess(row, result);
            boolean indexed = rag.rebuildIndex(row, text);
            outbox.enqueueNotification(row.getTenantId(), row.getCreatedByMembershipId(), "AI_PARSE_SUCCEEDED",
                    "合同 AI 解析完成", "解析结果必须人工核对和确认后才能进入履约流程。"
                            + (indexed ? "合同向量索引已更新。" : "向量索引暂不可用，问答将明确降级。"),
                    "CONTRACT", row.getContractId(), "/contracts/" + row.getContractId(),
                    "AI_PARSE_SUCCEEDED:" + row.getId());
        } catch (Exception exception) {
            String message = safeMessage(exception);
            persistence.saveFailure(row, message);
            if (row.getAttemptCount() >= 3) {
                outbox.enqueueNotification(row.getTenantId(), row.getCreatedByMembershipId(), "AI_PARSE_FAILED",
                        "合同 AI 解析失败", message + "；可以继续人工审阅。", "CONTRACT", row.getContractId(),
                        "/contracts/" + row.getContractId(), "AI_PARSE_FAILED:" + row.getId());
                return;
            }
            throw new IllegalStateException(message, exception);
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage() == null ? "解析失败" : exception.getMessage();
        return message.substring(0, Math.min(message.length(), 500));
    }
}


