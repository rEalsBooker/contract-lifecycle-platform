package com.contractguard.platform.ai.agent;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.contract.ContractDetailResponse;
import com.contractguard.platform.contract.ContractService;
import com.contractguard.platform.fulfillment.TaskService;
import com.contractguard.platform.receivable.ReceivableService;
import com.contractguard.platform.risk.RiskService;
import com.contractguard.platform.security.AuthPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ContractAgentToolsTest {
    @Test
    void refusesModelGeneratedContractIdOutsideAuthorizedScope() {
        ContractService contracts = mock(ContractService.class);
        ContractAgentTools tools = tools(contracts, mock(TaskService.class), mock(ReceivableService.class), mock(RiskService.class));

        String result = tools.getContractInfo(999L);

        assertTrue(result.contains("FAILED"));
        verifyNoInteractions(contracts);
    }

    @Test
    void keepsFinanceAuthorizationInsideExistingService() {
        ContractService contracts = mock(ContractService.class);
        ReceivableService receivables = mock(ReceivableService.class);
        when(contracts.detail(any(), eq(10L))).thenReturn(contract());
        when(receivables.listByContract(any(), eq(10L))).thenThrow(
                new ApiException(HttpStatus.FORBIDDEN, "FINANCE_READ_FORBIDDEN", "无权查看财务数据"));
        ContractAgentTools tools = tools(contracts, mock(TaskService.class), receivables, mock(RiskService.class));

        String result = tools.getReceivableStatus(10L);

        assertTrue(result.contains("FINANCE_READ_FORBIDDEN"));
        assertTrue(result.contains("DENIED"));
    }

    private ContractAgentTools tools(ContractService contracts, TaskService tasks,
                                     ReceivableService receivables, RiskService risks) {
        AuthPrincipal principal = new AuthPrincipal(1L, 2L, 3L, List.of("PROJECT_OWNER"), "WORKSPACE");
        return new ContractAgentTools(principal, 10L, contracts, tasks, receivables, risks, new ObjectMapper());
    }

    private ContractDetailResponse contract() {
        return new ContractDetailResponse(10L, "C-10", "合同", "客户", java.math.BigDecimal.TEN,
                "ACTIVE", "UNARCHIVED", "contract.pdf", null, List.of());
    }
}


