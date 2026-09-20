package com.contractguard.platform.ai.agent;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.contract.ContractDetailResponse;
import com.contractguard.platform.contract.ContractService;
import com.contractguard.platform.fulfillment.TaskResponse;
import com.contractguard.platform.fulfillment.TaskService;
import com.contractguard.platform.receivable.ReceivableService;
import com.contractguard.platform.risk.RiskService;
import com.contractguard.platform.security.AuthPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ContractAgentTools {
    private final AuthPrincipal principal;
    private final Long authorizedContractId;
    private final ContractService contracts;
    private final TaskService tasks;
    private final ReceivableService receivables;
    private final RiskService risks;
    private final ObjectMapper json;
    private final List<Map<String, Object>> trace = new ArrayList<>();

    public ContractAgentTools(AuthPrincipal principal, Long authorizedContractId, ContractService contracts,
                              TaskService tasks, ReceivableService receivables, RiskService risks, ObjectMapper json) {
        this.principal = principal; this.authorizedContractId = authorizedContractId; this.contracts = contracts;
        this.tasks = tasks; this.receivables = receivables; this.risks = risks; this.json = json;
    }

    @Tool("读取当前授权合同的基本信息和业务状态。仅用于查询，不修改合同。")
    public String getContractInfo(@P("必须是当前问答对应的合同ID") Long contractId) {
        return execute("getContractInfo", contractId, () -> {
            ContractDetailResponse value = contracts.detail(principal, contractId);
            return Map.of("id", value.id(), "contractNo", value.contractNo(), "name", value.name(),
                    "counterpartyName", value.counterpartyName(), "totalAmount", value.totalAmount(),
                    "businessStatus", value.businessStatus(), "archiveStatus", value.archiveStatus());
        });
    }

    @Tool("查询当前授权合同尚未完成的履约任务、负责人和日期。仅用于查询。")
    public String getPendingTasks(@P("必须是当前问答对应的合同ID") Long contractId) {
        return execute("getPendingTasks", contractId, () -> tasks.list(principal, contractId).stream()
                .filter(task -> !List.of("COMPLETED", "CANCELLED").contains(task.taskStatus()))
                .map(this::taskView).toList());
    }

    @Tool("查询当前授权合同的应收、已收、余额、开票和逾期状态。需要财务查看权限。仅用于查询。")
    public String getReceivableStatus(@P("必须是当前问答对应的合同ID") Long contractId) {
        return execute("getReceivableStatus", contractId, () -> receivables.listByContract(principal, contractId));
    }

    @Tool("查询当前授权合同已经由规则识别的风险事件和处置状态。仅用于查询。")
    public String getRiskEvents(@P("必须是当前问答对应的合同ID") Long contractId) {
        return execute("getRiskEvents", contractId, () -> risks.listByContract(principal, contractId));
    }

    public List<Map<String, Object>> trace() { return List.copyOf(trace); }

    private Object taskView(TaskResponse value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", value.id()); result.put("title", value.title()); result.put("status", value.taskStatus());
        result.put("contractualDueDate", value.contractualDueDate()); result.put("internalPlanDate", value.internalPlanDate());
        result.put("assigneeMembershipId", value.assigneeMembershipId()); result.put("reviewerMembershipId", value.reviewerMembershipId());
        return result;
    }

    private String execute(String toolName, Long contractId, Supplier<Object> action) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("tool", toolName); item.put("contractId", contractId);
        try {
            if (contractId == null || !authorizedContractId.equals(contractId))
                throw new IllegalArgumentException("工具只能查询当前问答合同");
            contracts.detail(principal, contractId);
            Object value = action.get();
            item.put("status", "SUCCESS"); item.put("result", value); trace.add(item);
            return json.writeValueAsString(Map.of("status", "SUCCESS", "data", value));
        } catch (ApiException exception) {
            item.put("status", "DENIED"); item.put("errorCode", exception.getCode()); trace.add(item);
            return jsonValue(Map.of("status", "DENIED", "errorCode", exception.getCode(), "message", exception.getMessage()));
        } catch (Exception exception) {
            item.put("status", "FAILED"); item.put("message", safe(exception.getMessage())); trace.add(item);
            return jsonValue(Map.of("status", "FAILED", "message", safe(exception.getMessage())));
        }
    }

    private String jsonValue(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception exception) { return "{\"status\":\"FAILED\",\"message\":\"工具结果序列化失败\"}"; }
    }

    private String safe(String value) { return value == null || value.isBlank() ? "工具调用失败" : value; }
}


