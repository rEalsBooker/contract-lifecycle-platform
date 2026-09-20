package com.contractguard.platform.risk;

import com.contractguard.platform.receivable.ReceivableMapper;
import com.contractguard.platform.receivable.ReceivableRow;
import com.contractguard.platform.notification.OutboxService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskEvaluationServiceTest {
    @Test
    void createsOneDeduplicatedRiskForOverdueBalance() {
        RiskMapper riskMapper = mock(RiskMapper.class);
        OutboxService outbox = mock(OutboxService.class);
        RiskEvaluationService service = new RiskEvaluationService(riskMapper, mock(ReceivableMapper.class), outbox);
        ReceivableRow row = plan(LocalDate.now().minusDays(3), "10000", "2500");
        RiskRow risk = new RiskRow();
        risk.setId(9L);
        risk.setContractNo("C001");
        risk.setRiskStatus("NEW");
        when(riskMapper.findOverdueByObject(8L, 3L)).thenReturn(risk);

        service.evaluate(8L, row);

        verify(riskMapper).upsertOverdue(eq(8L), eq(2L), eq(3L), eq("RECEIVABLE_OVERDUE:3"), contains("7500"));
        verify(outbox).enqueueOverdueRisk(eq(8L), eq(9L), eq(2L), eq("C001"), contains("7500"));
        verify(riskMapper, never()).markPendingClose(8L, 3L);
    }

    @Test
    void settledPlanMovesExistingRiskToPendingClose() {
        RiskMapper riskMapper = mock(RiskMapper.class);
        RiskEvaluationService service = new RiskEvaluationService(riskMapper, mock(ReceivableMapper.class), mock(OutboxService.class));
        ReceivableRow row = plan(LocalDate.now().minusDays(3), "10000", "10000");

        service.evaluate(8L, row);

        verify(riskMapper).markPendingClose(8L, 3L);
        verify(riskMapper, never()).upsertOverdue(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private ReceivableRow plan(LocalDate dueDate, String amount, String received) {
        ReceivableRow row = new ReceivableRow();
        row.setId(3L);
        row.setContractId(2L);
        row.setPlanAmount(new BigDecimal(amount));
        row.setReceivedAmount(new BigDecimal(received));
        row.setInvoicedAmount(BigDecimal.ZERO);
        row.setTriggerStatus("TRIGGERED");
        row.setDueDate(dueDate);
        return row;
    }
}

