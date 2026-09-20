package com.contractguard.platform.fulfillment;

import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TaskMapper {
    @Select("SELECT id, clause_title AS title, clause_content AS task_description FROM contract_clauses WHERE tenant_id = #{tenantId} AND contract_id = #{contractId} AND confirmation_status = 'CONFIRMED' ORDER BY id")
    List<TaskRow> findConfirmedClausesForDraft(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId);

    @Insert("INSERT IGNORE INTO fulfillment_tasks(tenant_id, contract_id, source_clause_id, title, task_description, task_status, task_origin, created_by_membership_id) VALUES(#{tenantId}, #{contractId}, #{sourceClauseId}, #{title}, #{description}, 'DRAFT', 'CONFIRMED_CLAUSE_RULE', #{membershipId})")
    int insertDraft(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId, @Param("sourceClauseId") Long sourceClauseId,
                    @Param("title") String title, @Param("description") String description, @Param("membershipId") Long membershipId);

    @Select("SELECT t.id, t.contract_id, t.source_clause_id, c.contract_no, c.name AS contract_name, t.title, t.task_description, t.task_status, t.task_origin, t.due_date,t.contractual_due_date,t.internal_plan_date, t.assignee_membership_id, t.reviewer_membership_id FROM fulfillment_tasks t JOIN contracts c ON c.id = t.contract_id WHERE t.tenant_id = #{tenantId} AND t.contract_id = #{contractId} ORDER BY t.id DESC")
    List<TaskRow> findByContract(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId);

    @Select("SELECT t.id, t.contract_id, t.source_clause_id, c.contract_no, c.name AS contract_name, t.title, t.task_description, t.task_status, t.task_origin, t.due_date,t.contractual_due_date,t.internal_plan_date, t.assignee_membership_id, t.reviewer_membership_id FROM fulfillment_tasks t JOIN contracts c ON c.id = t.contract_id WHERE t.tenant_id = #{tenantId} AND (#{globalRead} = TRUE OR t.assignee_membership_id = #{membershipId} OR t.reviewer_membership_id = #{membershipId}) ORDER BY CASE WHEN t.internal_plan_date IS NULL THEN 1 ELSE 0 END, t.internal_plan_date, t.id DESC")
    List<TaskRow> findVisible(@Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId, @Param("globalRead") boolean globalRead);

    @Select("SELECT t.id, t.contract_id, t.source_clause_id, c.contract_no, c.name AS contract_name, t.title, t.task_description, t.task_status, t.task_origin, t.due_date,t.contractual_due_date,t.internal_plan_date, t.assignee_membership_id, t.reviewer_membership_id FROM fulfillment_tasks t JOIN contracts c ON c.id = t.contract_id WHERE t.tenant_id = #{tenantId} AND t.id = #{taskId}")
    TaskRow findById(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId);

    @Update("UPDATE fulfillment_tasks SET assignee_membership_id=#{membershipId}, reviewer_membership_id=#{reviewerId}, due_date=#{dueDate},contractual_due_date=#{dueDate},internal_plan_date=#{dueDate}, task_status='PENDING' WHERE tenant_id=#{tenantId} AND id=#{taskId} AND task_status='DRAFT'") int claim(@Param("tenantId") Long tenantId,@Param("taskId") Long taskId,@Param("membershipId") Long membershipId,@Param("reviewerId") Long reviewerId,@Param("dueDate") LocalDate dueDate);

    @Update("UPDATE fulfillment_tasks SET task_status = 'IN_PROGRESS' WHERE tenant_id = #{tenantId} AND id = #{taskId} AND assignee_membership_id = #{membershipId} AND task_status = 'PENDING'")
    int start(@Param("tenantId") Long tenantId, @Param("taskId") Long taskId, @Param("membershipId") Long membershipId);

    @Update("UPDATE fulfillment_tasks SET task_status='PENDING_REVIEW' WHERE tenant_id=#{tenantId} AND id=#{taskId} AND assignee_membership_id=#{membershipId} AND task_status='IN_PROGRESS'") int submit(@Param("tenantId")Long tenantId,@Param("taskId")Long taskId,@Param("membershipId")Long membershipId);
    @Update("UPDATE fulfillment_tasks SET task_status=#{status},completed_at=CASE WHEN #{status}='COMPLETED' THEN NOW(3) ELSE NULL END WHERE tenant_id=#{tenantId} AND id=#{taskId} AND reviewer_membership_id=#{reviewerId} AND task_status='PENDING_REVIEW'") int reviewed(@Param("tenantId")Long tenantId,@Param("taskId")Long taskId,@Param("reviewerId")Long reviewerId,@Param("status")String status);
    @Select("SELECT COUNT(*) FROM task_extension_requests WHERE tenant_id=#{tenantId} AND task_id=#{taskId} AND approval_status='PENDING'") int pendingExtension(@Param("tenantId")Long tenantId,@Param("taskId")Long taskId);
    @Insert("INSERT INTO task_extension_requests(tenant_id,task_id,applicant_membership_id,approver_membership_id,original_internal_plan_date,requested_internal_plan_date,reason,approval_status) VALUES(#{tenantId},#{task.id},#{applicantId},#{request.approverMembershipId},#{task.internalPlanDate},#{request.requestedInternalPlanDate},#{request.reason},'PENDING')") @Options(useGeneratedKeys=true,keyProperty="row.id",keyColumn="id") void insertExtension(@Param("row")TaskExtensionRow row,@Param("tenantId")Long tenantId,@Param("task")TaskRow task,@Param("applicantId")Long applicantId,@Param("request")CreateTaskExtensionRequest request);
    @Select("SELECT e.id,e.task_id,t.title task_title,e.applicant_membership_id,u.display_name applicant_name,e.approver_membership_id,e.original_internal_plan_date,e.requested_internal_plan_date,e.reason,e.approval_status,e.approval_note,e.created_at,e.reviewed_at FROM task_extension_requests e JOIN fulfillment_tasks t ON t.id=e.task_id JOIN memberships m ON m.id=e.applicant_membership_id JOIN app_users u ON u.id=m.user_id WHERE e.tenant_id=#{tenantId} AND (e.approver_membership_id=#{membershipId} OR e.applicant_membership_id=#{membershipId}) ORDER BY e.id DESC") List<TaskExtensionRow> findExtensions(@Param("tenantId")Long tenantId,@Param("membershipId")Long membershipId);
    @Select("SELECT e.id,e.task_id,t.title task_title,e.applicant_membership_id,e.approver_membership_id,e.original_internal_plan_date,e.requested_internal_plan_date,e.reason,e.approval_status FROM task_extension_requests e JOIN fulfillment_tasks t ON t.id=e.task_id WHERE e.tenant_id=#{tenantId} AND e.id=#{id} AND e.approver_membership_id=#{approverId} FOR UPDATE") TaskExtensionRow lockExtension(@Param("tenantId")Long tenantId,@Param("id")Long id,@Param("approverId")Long approverId);
    @Update("UPDATE task_extension_requests SET approval_status=#{status},approval_note=#{note},reviewed_at=NOW(3) WHERE tenant_id=#{tenantId} AND id=#{id} AND approval_status='PENDING'") int reviewExtension(@Param("tenantId")Long tenantId,@Param("id")Long id,@Param("status")String status,@Param("note")String note);
    @Update("UPDATE fulfillment_tasks SET internal_plan_date=#{date},due_date=#{date} WHERE tenant_id=#{tenantId} AND id=#{taskId} AND task_status NOT IN ('COMPLETED','CANCELLED')") int applyExtension(@Param("tenantId")Long tenantId,@Param("taskId")Long taskId,@Param("date")LocalDate date);
}

