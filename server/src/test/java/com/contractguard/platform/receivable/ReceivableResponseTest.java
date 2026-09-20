package com.contractguard.platform.receivable;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReceivableResponseTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Test
    void overdueUsesRemainingBalanceInsteadOfPlanAmount() {
        ReceivableRow row = row("TRIGGERED", TODAY.minusDays(5), "120000", "50000", "120000");

        ReceivableResponse response = ReceivableResponse.from(row, TODAY);

        assertThat(response.dueStatus()).isEqualTo("OVERDUE");
        assertThat(response.collectionStatus()).isEqualTo("PARTIALLY_PAID");
        assertThat(response.invoiceStatus()).isEqualTo("INVOICED");
        assertThat(response.balance()).isEqualByComparingTo("70000");
        assertThat(response.overdueAmount()).isEqualByComparingTo("70000");
    }

    @Test
    void pendingTriggerContributesToBalanceButNeverCreatesOverdue() {
        ReceivableRow row = row("PENDING_TRIGGER", null, "90000", "0", "0");

        ReceivableResponse response = ReceivableResponse.from(row, TODAY);

        assertThat(response.dueStatus()).isEqualTo("PENDING_TRIGGER");
        assertThat(response.balance()).isEqualByComparingTo("90000");
        assertThat(response.overdueAmount()).isZero();
    }

    @Test
    void fullyCollectedPlanIsSettledEvenWhenOriginalDateHasPassed() {
        ReceivableRow row = row("TRIGGERED", TODAY.minusDays(30), "90000", "90000", "45000");

        ReceivableResponse response = ReceivableResponse.from(row, TODAY);

        assertThat(response.dueStatus()).isEqualTo("SETTLED");
        assertThat(response.collectionStatus()).isEqualTo("PAID");
        assertThat(response.invoiceStatus()).isEqualTo("PARTIALLY_INVOICED");
        assertThat(response.overdueAmount()).isZero();
    }

    private ReceivableRow row(String triggerStatus, LocalDate dueDate, String plan, String received, String invoiced) {
        ReceivableRow row = new ReceivableRow();
        row.setId(1L);
        row.setContractId(1L);
        row.setPlanAmount(new BigDecimal(plan));
        row.setTriggerStatus(triggerStatus);
        row.setDueDate(dueDate);
        row.setReceivedAmount(new BigDecimal(received));
        row.setInvoicedAmount(new BigDecimal(invoiced));
        return row;
    }
}

