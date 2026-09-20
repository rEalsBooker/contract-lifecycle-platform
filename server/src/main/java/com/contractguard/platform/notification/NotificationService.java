package com.contractguard.platform.notification;

import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationMapper mapper;

    public NotificationService(NotificationMapper mapper) { this.mapper = mapper; }

    public List<NotificationResponse> list(AuthPrincipal principal, boolean unreadOnly) {
        requireWorkspace(principal);
        return mapper.findInbox(principal.tenantId(), principal.membershipId(), unreadOnly).stream()
                .map(NotificationResponse::from).toList();
    }

    public long unreadCount(AuthPrincipal principal) {
        requireWorkspace(principal);
        return mapper.countUnread(principal.tenantId(), principal.membershipId());
    }

    public void markRead(AuthPrincipal principal, Long id) {
        requireWorkspace(principal);
        if (mapper.markRead(principal.tenantId(), principal.membershipId(), id) == 0
                && !mapper.existsOwned(principal.tenantId(), principal.membershipId(), id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "通知不存在或不属于当前成员");
        }
    }

    public void markAllRead(AuthPrincipal principal) {
        requireWorkspace(principal);
        mapper.markAllRead(principal.tenantId(), principal.membershipId());
    }

    private void requireWorkspace(AuthPrincipal principal) {
        if (!principal.hasWorkspace()) throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_REQUIRED", "请先选择企业工作空间");
    }
}

