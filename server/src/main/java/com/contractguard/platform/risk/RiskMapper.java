package com.contractguard.platform.risk;
import org.apache.ibatis.annotations.*; import java.time.LocalDate; import java.util.List;
@Mapper
public interface RiskMapper {
 @Insert("""
 INSERT INTO risk_cases(tenant_id,contract_id,rule_code,object_type,object_id,dedup_key,severity,risk_status,fact_summary)
 VALUES(#{tenantId},#{contractId},'RECEIVABLE_OVERDUE','RECEIVABLE',#{objectId},#{dedupKey},'HIGH','NEW',#{fact})
 ON DUPLICATE KEY UPDATE fact_summary=VALUES(fact_summary),severity='HIGH',updated_at=NOW(3),risk_status=CASE WHEN risk_status IN ('CLOSED','PENDING_CLOSE') OR (risk_status='IGNORED' AND review_date<=CURDATE()) THEN 'NEW' ELSE risk_status END
 """)
 void upsertOverdue(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId,@Param("objectId")Long objectId,@Param("dedupKey")String dedupKey,@Param("fact")String fact);
 @Update("UPDATE risk_cases SET risk_status='PENDING_CLOSE',updated_at=NOW(3) WHERE tenant_id=#{tenantId} AND rule_code='RECEIVABLE_OVERDUE' AND object_id=#{objectId} AND risk_status NOT IN ('CLOSED','PENDING_CLOSE')") int markPendingClose(@Param("tenantId")Long tenantId,@Param("objectId")Long objectId);
 @Select("""
 SELECT r.id,r.contract_id,c.contract_no,c.name AS contract_name,r.rule_code,r.object_type,r.object_id,r.severity,r.risk_status,r.fact_summary,r.handler_membership_id,u.display_name AS handler_name,r.handling_plan,r.review_date,r.ignored_reason,r.created_at,r.updated_at FROM risk_cases r JOIN contracts c ON c.id=r.contract_id LEFT JOIN memberships m ON m.id=r.handler_membership_id LEFT JOIN app_users u ON u.id=m.user_id WHERE r.tenant_id=#{tenantId} ORDER BY CASE r.severity WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END,r.updated_at DESC
 """) List<RiskRow> findAll(Long tenantId);
 @Select("""
 SELECT r.id,r.contract_id,r.rule_code,r.object_type,r.object_id,r.severity,r.risk_status,r.fact_summary,
 r.handler_membership_id,u.display_name AS handler_name,r.handling_plan,r.review_date,r.ignored_reason,r.created_at,r.updated_at
 FROM risk_cases r LEFT JOIN memberships m ON m.id=r.handler_membership_id AND m.tenant_id=r.tenant_id
 LEFT JOIN app_users u ON u.id=m.user_id WHERE r.tenant_id=#{tenantId} AND r.id=#{id}
 """) RiskRow findById(@Param("tenantId")Long tenantId,@Param("id")Long id);
 @Select("""
 SELECT r.id,r.contract_id,c.contract_no,c.name AS contract_name,r.rule_code,r.object_type,r.object_id,r.severity,
 r.risk_status,r.fact_summary,r.handler_membership_id,r.handling_plan,r.review_date,r.ignored_reason,r.created_at,r.updated_at
 FROM risk_cases r JOIN contracts c ON c.id=r.contract_id AND c.tenant_id=r.tenant_id
 WHERE r.tenant_id=#{tenantId} AND r.rule_code='RECEIVABLE_OVERDUE' AND r.object_id=#{objectId}
 """) RiskRow findOverdueByObject(@Param("tenantId")Long tenantId,@Param("objectId")Long objectId);
 @Update("UPDATE risk_cases SET risk_status='ACKNOWLEDGED' WHERE tenant_id=#{tenantId} AND id=#{id} AND risk_status='NEW'") int acknowledge(@Param("tenantId")Long tenantId,@Param("id")Long id);
 @Update("UPDATE risk_cases SET risk_status='IN_PROGRESS',handler_membership_id=#{handlerId},handling_plan=#{plan},ignored_reason=NULL,review_date=NULL WHERE tenant_id=#{tenantId} AND id=#{id} AND risk_status IN ('NEW','ACKNOWLEDGED','IN_PROGRESS')") int start(@Param("tenantId")Long tenantId,@Param("id")Long id,@Param("handlerId")Long handlerId,@Param("plan")String plan);
 @Update("UPDATE risk_cases SET risk_status='IGNORED',ignored_reason=#{reason},review_date=#{reviewDate} WHERE tenant_id=#{tenantId} AND id=#{id} AND risk_status IN ('NEW','ACKNOWLEDGED','IN_PROGRESS','IGNORED')") int ignore(@Param("tenantId")Long tenantId,@Param("id")Long id,@Param("reason")String reason,@Param("reviewDate")LocalDate reviewDate);
 @Update("UPDATE risk_cases SET risk_status='CLOSED' WHERE tenant_id=#{tenantId} AND id=#{id} AND risk_status='PENDING_CLOSE'") int close(@Param("tenantId")Long tenantId,@Param("id")Long id);
}

