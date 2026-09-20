package com.contractguard.platform.identity;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.security.AuthPrincipal;
import com.contractguard.platform.security.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspaceService {
    private final MembershipMapper membershipMapper;
    private final UserMapper userMapper;
    private final JwtTokenService tokenService;
    private final AuditService auditService;

    public WorkspaceService(MembershipMapper membershipMapper, UserMapper userMapper, JwtTokenService tokenService,
                            AuditService auditService) {
        this.membershipMapper = membershipMapper;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.auditService = auditService;
    }

    public List<WorkspaceResponse> list(Long userId) {
        ensureActiveUser(userId);
        return membershipMapper.findActiveByUserId(userId).stream().map(this::toResponse).toList();
    }

    public WorkspaceTokenResponse switchWorkspace(Long membershipId, AuthPrincipal principal) {
        MembershipRow membership = membershipMapper.findActiveByIdAndUserId(membershipId, principal.userId());
        if (membership == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_FORBIDDEN", "无权进入该企业工作空间");
        }
        UserAccount user = ensureActiveUser(principal.userId());
        List<String> roles = membershipMapper.findRoleCodes(membership.getMembershipId());
        auditService.record(membership.getTenantId(), principal.userId(), membership.getMembershipId(),
                "WORKSPACE_SWITCHED", "TENANT", membership.getTenantId().toString());
        return new WorkspaceTokenResponse(
                tokenService.createWorkspaceToken(principal.userId(), user.getUsername(), membership.getMembershipId(), membership.getTenantId(), roles),
                "Bearer", tokenService.expiresAt(), membership.getMembershipId(), membership.getTenantId(), membership.getTenantName(), roles);
    }

    public WorkspaceResponse current(AuthPrincipal principal) {
        requireWorkspace(principal);
        ensureActiveUser(principal.userId());
        MembershipRow membership = membershipMapper.findActiveByIdAndUserId(principal.membershipId(), principal.userId());
        if (membership == null || !membership.getTenantId().equals(principal.tenantId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEMBERSHIP_INACTIVE", "成员关系已失效，请重新选择工作空间");
        }
        return toResponse(membership);
    }

    private WorkspaceResponse toResponse(MembershipRow membership) {
        return new WorkspaceResponse(membership.getMembershipId(), membership.getTenantId(), membership.getTenantName(),
                membershipMapper.findRoleCodes(membership.getMembershipId()));
    }

    private void requireWorkspace(AuthPrincipal principal) {
        if (!principal.hasWorkspace()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_REQUIRED", "请先选择企业工作空间");
        }
    }

    private UserAccount ensureActiveUser(Long userId) {
        UserAccount user = userMapper.findById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "账号已停用，请重新登录");
        }
        return user;
    }

}

