package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.contract.ContractMapper;
import com.contractguard.platform.contract.ContractRow;
import com.contractguard.platform.contract.ParseJobRow;
import com.contractguard.platform.notification.OutboxService;
import com.contractguard.platform.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractParseService {
    private final AiParseMapper mapper;
    private final ContractMapper contracts;
    private final ContractExtractionModel model;
    private final OutboxService outbox;
    private final AuditService audit;

    public ContractParseService(AiParseMapper mapper, ContractMapper contracts, ContractExtractionModel model,
                                OutboxService outbox, AuditService audit) {
        this.mapper = mapper; this.contracts = contracts; this.model = model; this.outbox = outbox; this.audit = audit;
    }

    @Transactional
    public ParseJobRow request(AuthPrincipal principal, Long contractId, CreateParseJobRequest request) {
        requireOwner(principal);
        ContractRow contract = contracts.findDetail(contractId, principal.tenantId());
        if (contract == null) throw new ApiException(HttpStatus.NOT_FOUND, "CONTRACT_NOT_FOUND", "合同不存在或不属于当前企业");
        ParseJobRow repeated = mapper.findByRequestKey(principal.tenantId(), contractId, request.requestKey());
        if (repeated != null) return repeated;
        if (!model.available()) throw new ApiException(HttpStatus.CONFLICT, "AI_MODEL_NOT_CONFIGURED", "AI 模型未配置，请继续使用人工审阅模式");
        if (mapper.hasActiveJob(principal.tenantId(), contractId)) throw new ApiException(HttpStatus.CONFLICT, "AI_PARSE_ALREADY_RUNNING", "该合同已有解析作业正在处理");
        ParseJobRow job = new ParseJobRow();
        mapper.insertQueued(job, principal.tenantId(), contractId, contract.getCurrentVersionId(), request.requestKey(),
                model.providerName(), model.modelName(), principal.membershipId());
        contracts.updateBusinessStatus(principal.tenantId(),contractId,"TERMS_REVIEW");
        outbox.enqueueEvent(principal.tenantId(), "CONTRACT_PARSE:" + job.getId(), "CONTRACT_PARSE_REQUESTED",
                "CONTRACT", contractId, new ContractParseEventPayload(principal.tenantId(), job.getId()));
        audit.record(principal.tenantId(), principal.userId(), principal.membershipId(), "CONTRACT_AI_PARSE_REQUESTED", "CONTRACT", contractId.toString());
        return mapper.findByRequestKey(principal.tenantId(), contractId, request.requestKey());
    }

    private void requireOwner(AuthPrincipal principal) {
        if (!principal.hasWorkspace()) throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_REQUIRED", "请先选择企业工作空间");
        if (principal.roleCodes().stream().noneMatch(role -> role.equals("ENTERPRISE_ADMIN") || role.equals("CONTRACT_OWNER"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AI_PARSE_FORBIDDEN", "当前角色无权发起合同解析");
        }
    }
}


