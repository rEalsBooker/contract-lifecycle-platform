package com.contractguard.platform.receivable;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReceivableRow {
    private Long id; private Long contractId; private String contractNo; private String contractName; private String counterpartyName;
    private String planName; private BigDecimal planAmount; private String triggerStatus; private LocalDate dueDate;
    private BigDecimal receivedAmount; private BigDecimal invoicedAmount;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getContractId(){return contractId;} public void setContractId(Long v){contractId=v;}
    public String getContractNo(){return contractNo;} public void setContractNo(String v){contractNo=v;} public String getContractName(){return contractName;} public void setContractName(String v){contractName=v;}
    public String getCounterpartyName(){return counterpartyName;} public void setCounterpartyName(String v){counterpartyName=v;} public String getPlanName(){return planName;} public void setPlanName(String v){planName=v;}
    public BigDecimal getPlanAmount(){return planAmount;} public void setPlanAmount(BigDecimal v){planAmount=v;} public String getTriggerStatus(){return triggerStatus;} public void setTriggerStatus(String v){triggerStatus=v;}
    public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;} public BigDecimal getReceivedAmount(){return receivedAmount;} public void setReceivedAmount(BigDecimal v){receivedAmount=v;}
    public BigDecimal getInvoicedAmount(){return invoicedAmount;} public void setInvoicedAmount(BigDecimal v){invoicedAmount=v;}
}

