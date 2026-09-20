package com.contractguard.platform.receivable;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableResponse(Long id, Long contractId, String contractNo, String contractName, String counterpartyName,
        String planName, BigDecimal planAmount, String triggerStatus, LocalDate dueDate, BigDecimal receivedAmount,
        BigDecimal invoicedAmount, BigDecimal balance, String collectionStatus, String invoiceStatus, String dueStatus,
        BigDecimal overdueAmount) {
    public static ReceivableResponse from(ReceivableRow row, LocalDate today) {
        BigDecimal received = value(row.getReceivedAmount()); BigDecimal invoiced = value(row.getInvoicedAmount());
        BigDecimal balance = row.getPlanAmount().subtract(received);
        String collection = received.signum()==0 ? "UNPAID" : balance.signum()==0 ? "PAID" : "PARTIALLY_PAID";
        String invoice = invoiced.signum()==0 ? "UNINVOICED" : invoiced.compareTo(row.getPlanAmount())>=0 ? "INVOICED" : "PARTIALLY_INVOICED";
        String due; BigDecimal overdue = BigDecimal.ZERO;
        if (balance.signum()==0) due="SETTLED";
        else if (!"TRIGGERED".equals(row.getTriggerStatus()) || row.getDueDate()==null) due="PENDING_TRIGGER";
        else if (row.getDueDate().isBefore(today)) { due="OVERDUE"; overdue=balance; }
        else if (row.getDueDate().isEqual(today)) due="DUE_TODAY"; else due="NOT_DUE";
        return new ReceivableResponse(row.getId(),row.getContractId(),row.getContractNo(),row.getContractName(),row.getCounterpartyName(),row.getPlanName(),row.getPlanAmount(),row.getTriggerStatus(),row.getDueDate(),received,invoiced,balance,collection,invoice,due,overdue);
    }
    private static BigDecimal value(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
}

