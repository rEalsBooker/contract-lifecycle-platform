package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.contract.ContractMapper;
import com.contractguard.platform.contract.ContractRow;
import com.contractguard.platform.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiFindingService {
    private final AiParseMapper mapper; private final ContractMapper contracts; private final AuditService audit;
    public AiFindingService(AiParseMapper mapper, ContractMapper contracts, AuditService audit){this.mapper=mapper;this.contracts=contracts;this.audit=audit;}

    public List<AiFindingResponse> list(AuthPrincipal p,Long contractId){ requireReadable(p,contractId); return mapper.findFindings(p.tenantId(),contractId).stream().map(AiFindingResponse::from).toList(); }

    @Transactional
    public List<AiFindingResponse> update(AuthPrincipal p,Long contractId,Long findingId,UpdateAiFindingRequest request){
        requireWritable(p,contractId); validatePage(request.sourcePageNo());
        if(mapper.updateFinding(p.tenantId(),contractId,findingId,request)==0) throw conflict("AI_FINDING_NOT_EDITABLE","提取项不存在或已经处理");
        audit.record(p.tenantId(),p.userId(),p.membershipId(),"AI_FINDING_UPDATED","AI_FINDING",findingId.toString()); return list(p,contractId);
    }

    @Transactional
    public List<AiFindingResponse> confirm(AuthPrincipal p,Long contractId,Long findingId){
        ContractRow contract=requireWritable(p,contractId); AiFindingRow finding=mapper.lockFinding(p.tenantId(),contractId,findingId);
        if(finding==null||!"PENDING_CONFIRMATION".equals(finding.getReviewStatus())) throw conflict("AI_FINDING_NOT_CONFIRMABLE","提取项不存在或已经处理");
        if(!contract.getCurrentVersionId().equals(finding.getContractVersionId())) throw conflict("AI_FINDING_VERSION_STALE","提取项不属于当前合同版本，请重新解析");
        mapper.insertConfirmedClause(p.tenantId(),p.membershipId(),finding);
        mapper.reviewFinding(p.tenantId(),contractId,findingId,p.membershipId(),"CONFIRMED","人工确认并转为正式条款");
        audit.record(p.tenantId(),p.userId(),p.membershipId(),"AI_FINDING_CONFIRMED","AI_FINDING",findingId.toString()); return list(p,contractId);
    }

    @Transactional
    public List<AiFindingResponse> reject(AuthPrincipal p,Long contractId,Long findingId,String note){
        requireWritable(p,contractId); if(note==null||note.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST,"REJECT_REASON_REQUIRED","驳回原因不能为空");
        if(mapper.reviewFinding(p.tenantId(),contractId,findingId,p.membershipId(),"REJECTED",note.trim())==0) throw conflict("AI_FINDING_NOT_REJECTABLE","提取项不存在或已经处理");
        audit.record(p.tenantId(),p.userId(),p.membershipId(),"AI_FINDING_REJECTED","AI_FINDING",findingId.toString()); return list(p,contractId);
    }

    private ContractRow requireWritable(AuthPrincipal p,Long id){ if(p.roleCodes().stream().noneMatch(r->r.equals("ENTERPRISE_ADMIN")||r.equals("CONTRACT_OWNER")||r.equals("LEGAL"))) throw new ApiException(HttpStatus.FORBIDDEN,"AI_FINDING_WRITE_FORBIDDEN","当前角色无权审阅 AI 提取项"); return requireReadable(p,id); }
    private ContractRow requireReadable(AuthPrincipal p,Long id){ if(!p.hasWorkspace()) throw new ApiException(HttpStatus.FORBIDDEN,"WORKSPACE_REQUIRED","请先选择企业工作空间"); ContractRow c=contracts.findDetail(id,p.tenantId()); boolean global=p.roleCodes().stream().anyMatch(r->List.of("ENTERPRISE_ADMIN","CONTRACT_OWNER","FINANCE","LEGAL").contains(r)); if(c==null||(!global&&!p.membershipId().equals(c.getOwnerMembershipId())&&!contracts.hasReadGrant(p.tenantId(),id,p.membershipId())&&!contracts.hasTaskAccess(p.tenantId(),id,p.membershipId()))) throw new ApiException(HttpStatus.FORBIDDEN,"CONTRACT_FORBIDDEN","无权访问该合同"); return c; }
    private void validatePage(Integer page){if(page!=null&&page<1)throw new ApiException(HttpStatus.BAD_REQUEST,"SOURCE_PAGE_INVALID","来源页码必须大于零");}
    private ApiException conflict(String code,String message){return new ApiException(HttpStatus.CONFLICT,code,message);}
}


