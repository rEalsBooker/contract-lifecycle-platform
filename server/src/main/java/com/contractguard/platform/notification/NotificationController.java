package com.contractguard.platform.notification;

import com.contractguard.platform.security.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
                                           @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return service.list(principal, unreadOnly);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        return Map.of("count", service.unreadCount(principal));
    }

    @PostMapping("/{id}/read")
    public void markRead(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        service.markRead(principal, id);
    }

    @PostMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal AuthPrincipal principal) {
        service.markAllRead(principal);
    }
}

