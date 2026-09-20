package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.contract.ContractMapper;
import com.contractguard.platform.contract.ContractRow;
import com.contractguard.platform.notification.OutboxService;
import com.contractguard.platform.security.AuthPrincipal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContractParseServiceTest {
    @Test
    void refusesAiJobWithoutConfiguredModelAndDoesNotCreateFakeResults() {
        AiParseMapper parseMapper = mock(AiParseMapper.class);
        ContractMapper contracts = mock(ContractMapper.class);
        ContractExtractionModel model = mock(ContractExtractionModel.class);
        OutboxService outbox = mock(OutboxService.class);
        AuditService audit = mock(AuditService.class);
        ContractRow contract = new ContractRow();
        contract.setId(33L);
        contract.setTenantId(100L);
        contract.setCurrentVersionId(44L);
        when(contracts.findDetail(33L, 100L)).thenReturn(contract);
        when(model.available()).thenReturn(false);

        ContractParseService service = new ContractParseService(parseMapper, contracts, model, outbox, audit);
        AuthPrincipal principal = new AuthPrincipal(10L, 20L, 100L, List.of("ENTERPRISE_ADMIN"), "WORKSPACE");
        ApiException exception = assertThrows(ApiException.class,
                () -> service.request(principal, 33L, new CreateParseJobRequest("request-1")));

        assertEquals("AI_MODEL_NOT_CONFIGURED", exception.getCode());
        verify(parseMapper, never()).insertQueued(any(), any(), any(), any(), any(), any(), any(), any());
        verifyNoInteractions(outbox, audit);
    }
}


