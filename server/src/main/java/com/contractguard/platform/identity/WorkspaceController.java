package com.contractguard.platform.identity;

import com.contractguard.platform.security.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/workspaces")
    public List<WorkspaceResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return workspaceService.list(principal.userId());
    }

    @PostMapping("/workspaces/{membershipId}/switch")
    public WorkspaceTokenResponse switchWorkspace(@PathVariable Long membershipId,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return workspaceService.switchWorkspace(membershipId, principal);
    }

    @GetMapping("/me")
    public WorkspaceResponse current(@AuthenticationPrincipal AuthPrincipal principal) {
        return workspaceService.current(principal);
    }
}

