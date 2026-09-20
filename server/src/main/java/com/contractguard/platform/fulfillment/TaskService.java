package com.contractguard.platform.fulfillment;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.contract.ContractMapper;
import com.contractguard.platform.contract.ContractRow;
import com.contractguard.platform.identity.MembershipMapper;
import com.contractguard.platform.security.AuthPrincipal;
import com.contractguard.platform.notification.OutboxService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {
    private final TaskMapper taskMapper; private final ContractMapper contractMapper; private final AuditService auditService; private final MembershipMapper membershipMapper; private final OutboxService outbox;
    public TaskService(TaskMapper taskMapper, ContractMapper contractMapper, AuditService auditService, MembershipMapper membershipMapper,OutboxService outbox) { this.taskMapper = taskMapper; this.contractMapper = contractMapper; this.auditService = auditService; this.membershipMapper = membershipMapper; this.outbox=outbox; }

    public List<TaskResponse> list(AuthPrincipal principal, Long contractId) {
        requireWorkspace(principal);
        if (contractId != null) requireReadableContract(principal, contractId);
        List<TaskRow> tasks = contractId == null ? taskMapper.findVisible(principal.tenantId(), principal.membershipId(), hasGlobalRead(principal)) : taskMapper.findByContract(principal.tenantId(), contractId);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public List<TaskResponse> generateDrafts(AuthPrincipal principal, Long contractId) {
        ContractRow contract = requireContractOwner(principal, contractId);
        if (!"TERMS_CONFIRMED".equals(contract.getBusinessStatus())) throw new ApiException(HttpStatus.CONFLICT, "TERMS_NOT_PUBLISHED", "请先发布全部人工确认的条款，再生成履约任务草稿");
        int created = 0;
        for (TaskRow clause : taskMapper.findConfirmedClausesForDraft(principal.tenantId(), contractId)) {
            created += taskMapper.insertDraft(principal.tenantId(), contractId, clause.getId(), "跟进：" + clause.getTitle(), clause.getTaskDescription(), principal.membershipId());
        }
        auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(), "FULFILLMENT_TASK_DRAFTS_GENERATED", "CONTRACT", contractId.toString());
        return list(principal, contractId);
    }

    @Transactional
    public TaskResponse claim(AuthPrincipal principal, Long taskId, ClaimTaskRequest request) {
        requireWorkspace(principal); if (request.dueDate().isBefore(LocalDate.now())) throw new ApiException(HttpStatus.BAD_REQUEST, "TASK_DUE_DATE_INVALID", "截止日期不能早于今天");
        TaskRow task = requireVisibleTask(principal, taskId);
        if(principal.membershipId().equals(request.reviewerMembershipId()) || !membershipMapper.existsActive(principal.tenantId(),request.reviewerMembershipId())) throw new ApiException(HttpStatus.BAD_REQUEST,"TASK_REVIEWER_INVALID","审核人必须是同企业的其他有效成员");
        if (taskMapper.claim(principal.tenantId(), taskId, principal.membershipId(), request.reviewerMembershipId(), request.dueDate()) == 0) throw new ApiException(HttpStatus.CONFLICT, "TASK_NOT_CLAIMABLE", "该任务不是可领取的草稿状态");
        auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(), "FULFILLMENT_TASK_CLAIMED", "TASK", taskId.toString());
        return TaskResponse.from(taskMapper.findById(principal.tenantId(), taskId));
    }

    @Transactional
    public TaskResponse start(AuthPrincipal principal, Long taskId) { return transition(principal, taskId, "start"); }

    public List<TaskExtensionRow> extensions(AuthPrincipal principal){requireWorkspace(principal);return taskMapper.findExtensions(principal.tenantId(),principal.membershipId());}

    @Transactional
    public List<TaskExtensionRow> requestExtension(AuthPrincipal principal,Long taskId,CreateTaskExtensionRequest request){
        requireWorkspace(principal);TaskRow task=requireVisibleTask(principal,taskId);
        if(!principal.membershipId().equals(task.getAssigneeMembershipId()))throw new ApiException(HttpStatus.FORBIDDEN,"EXTENSION_APPLICANT_FORBIDDEN","只有任务负责人可以申请延期");
        if(List.of("COMPLETED","CANCELLED").contains(task.getTaskStatus())||task.getInternalPlanDate()==null)throw new ApiException(HttpStatus.CONFLICT,"EXTENSION_STATE_INVALID","当前任务不能申请延期");
        if(!request.requestedInternalPlanDate().isAfter(task.getInternalPlanDate()))throw new ApiException(HttpStatus.BAD_REQUEST,"EXTENSION_DATE_INVALID","新内部计划日必须晚于原计划日");
        if(principal.membershipId().equals(request.approverMembershipId())||!membershipMapper.existsActive(principal.tenantId(),request.approverMembershipId()))throw new ApiException(HttpStatus.BAD_REQUEST,"EXTENSION_APPROVER_INVALID","审批人必须是同企业的其他有效成员");
        if(taskMapper.pendingExtension(principal.tenantId(),taskId)>0)throw new ApiException(HttpStatus.CONFLICT,"EXTENSION_ALREADY_PENDING","该任务已有待审批延期");
        TaskExtensionRow row=new TaskExtensionRow();taskMapper.insertExtension(row,principal.tenantId(),task,principal.membershipId(),request);
        outbox.enqueueNotification(principal.tenantId(),request.approverMembershipId(),"TASK_EXTENSION_PENDING","任务延期待审批",task.getTitle(),"TASK",taskId,"/tasks?taskId="+taskId,"EXTENSION:"+row.getId()+":PENDING");
        auditService.record(principal.tenantId(),principal.userId(),principal.membershipId(),"TASK_EXTENSION_REQUESTED","TASK_EXTENSION",row.getId().toString());return extensions(principal);
    }

    @Transactional
    public List<TaskExtensionRow> reviewExtension(AuthPrincipal principal,Long extensionId,ReviewTaskExtensionRequest request){
        requireWorkspace(principal);TaskExtensionRow row=taskMapper.lockExtension(principal.tenantId(),extensionId,principal.membershipId());
        if(row==null||!"PENDING".equals(row.getApprovalStatus()))throw new ApiException(HttpStatus.CONFLICT,"EXTENSION_NOT_REVIEWABLE","延期申请不存在或已处理");
        if(!request.approved()&&(request.note()==null||request.note().isBlank()))throw new ApiException(HttpStatus.BAD_REQUEST,"REVIEW_NOTE_REQUIRED","驳回必须填写原因");
        String status=request.approved()?"APPROVED":"REJECTED";if(request.approved()&&taskMapper.applyExtension(principal.tenantId(),row.getTaskId(),row.getRequestedInternalPlanDate())==0)throw new ApiException(HttpStatus.CONFLICT,"TASK_STATE_CHANGED","任务状态已变化，不能批准延期");
        String resultNote=request.note()==null||request.note().isBlank()?(request.approved()?"内部计划日期已更新":"延期申请未通过"):request.note().trim();
        taskMapper.reviewExtension(principal.tenantId(),extensionId,status,resultNote);outbox.enqueueNotification(principal.tenantId(),row.getApplicantMembershipId(),"TASK_EXTENSION_"+status,"任务延期"+(request.approved()?"已批准":"已驳回"),resultNote,"TASK",row.getTaskId(),"/tasks?taskId="+row.getTaskId(),"EXTENSION:"+extensionId+":"+status);
        auditService.record(principal.tenantId(),principal.userId(),principal.membershipId(),"TASK_EXTENSION_"+status,"TASK_EXTENSION",extensionId.toString());return extensions(principal);
    }
    private TaskResponse transition(AuthPrincipal principal, Long taskId, String action) {
        requireWorkspace(principal); TaskRow task = requireVisibleTask(principal, taskId);
        int changed = taskMapper.start(principal.tenantId(), taskId, principal.membershipId());
        if (changed == 0) throw new ApiException(HttpStatus.CONFLICT, "TASK_STATUS_NOT_ALLOWED", "当前任务状态不允许此操作，或您不是负责人");
        auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(), "FULFILLMENT_TASK_" + action.toUpperCase(), "TASK", taskId.toString());
        return TaskResponse.from(taskMapper.findById(principal.tenantId(), taskId));
    }

    private TaskRow requireVisibleTask(AuthPrincipal principal, Long taskId) { TaskRow task = taskMapper.findById(principal.tenantId(), taskId); if (task == null || (!hasGlobalRead(principal) && !principal.membershipId().equals(task.getAssigneeMembershipId())&&!principal.membershipId().equals(task.getReviewerMembershipId()))) throw new ApiException(HttpStatus.FORBIDDEN, "TASK_FORBIDDEN", "无权访问该履约任务"); return task; }
    private ContractRow requireContractOwner(AuthPrincipal principal, Long contractId) { requireWriteRole(principal); return requireReadableContract(principal, contractId); }
    private ContractRow requireReadableContract(AuthPrincipal principal, Long contractId) { requireWorkspace(principal); ContractRow contract = contractMapper.findDetail(contractId, principal.tenantId()); if (contract == null || !(hasGlobalRead(principal) || principal.membershipId().equals(contract.getOwnerMembershipId()) || contractMapper.hasReadGrant(principal.tenantId(), contractId, principal.membershipId()) || contractMapper.hasTaskAccess(principal.tenantId(), contractId, principal.membershipId()))) throw new ApiException(HttpStatus.FORBIDDEN, "CONTRACT_FORBIDDEN", "无权访问该合同"); return contract; }
    private boolean hasGlobalRead(AuthPrincipal principal) { return principal.roleCodes().stream().anyMatch(r -> r.equals("ENTERPRISE_ADMIN") || r.equals("CONTRACT_OWNER")); }
    private void requireWriteRole(AuthPrincipal principal) { if (principal.roleCodes().stream().noneMatch(r -> r.equals("ENTERPRISE_ADMIN") || r.equals("CONTRACT_OWNER"))) throw new ApiException(HttpStatus.FORBIDDEN, "TASK_WRITE_FORBIDDEN", "当前角色无权生成履约任务草稿"); }
    private void requireWorkspace(AuthPrincipal principal) { if (!principal.hasWorkspace()) throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_REQUIRED", "请先选择企业工作空间"); }
}

