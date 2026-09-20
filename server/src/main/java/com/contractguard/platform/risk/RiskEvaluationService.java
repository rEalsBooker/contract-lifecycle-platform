package com.contractguard.platform.risk;
import com.contractguard.platform.notification.OutboxService; import com.contractguard.platform.receivable.*; import org.springframework.stereotype.Service; import java.time.LocalDate;
@Service
public class RiskEvaluationService { private final RiskMapper risks; private final ReceivableMapper receivables; private final OutboxService outbox; public RiskEvaluationService(RiskMapper risks,ReceivableMapper receivables,OutboxService outbox){this.risks=risks;this.receivables=receivables;this.outbox=outbox;}
 public void evaluate(Long tenantId,ReceivableRow row){if(row==null)return;ReceivableResponse value=ReceivableResponse.from(row,LocalDate.now());if("OVERDUE".equals(value.dueStatus())){String fact="到期 "+value.dueDate()+"，余额 ¥"+value.balance().stripTrailingZeros().toPlainString()+" 未结清";risks.upsertOverdue(tenantId,value.contractId(),value.id(),"RECEIVABLE_OVERDUE:"+value.id(),fact);RiskRow risk=risks.findOverdueByObject(tenantId,value.id());if(risk!=null&&"NEW".equals(risk.getRiskStatus()))outbox.enqueueOverdueRisk(tenantId,risk.getId(),value.contractId(),risk.getContractNo(),fact);}else risks.markPendingClose(tenantId,value.id());}
 public void scan(Long tenantId){for(ReceivableRow row:receivables.findAll(tenantId))evaluate(tenantId,row);}
}

