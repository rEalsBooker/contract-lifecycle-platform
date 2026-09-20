package com.contractguard.platform.fulfillment;

import com.contractguard.platform.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TaskController {
    private final TaskService taskService;
    public TaskController(TaskService taskService) { this.taskService = taskService; }
    @GetMapping("/tasks") public List<TaskResponse> list(@AuthenticationPrincipal AuthPrincipal principal, @RequestParam(required = false) Long contractId) { return taskService.list(principal, contractId); }
    @PostMapping("/contracts/{contractId}/task-drafts") public List<TaskResponse> generate(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contractId) { return taskService.generateDrafts(principal, contractId); }
    @PostMapping("/tasks/{taskId}/claim") public TaskResponse claim(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long taskId, @Valid @RequestBody ClaimTaskRequest request) { return taskService.claim(principal, taskId, request); }
    @PostMapping("/tasks/{taskId}/start") public TaskResponse start(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long taskId) { return taskService.start(principal, taskId); }
    @GetMapping("/task-extensions") public List<TaskExtensionRow> extensions(@AuthenticationPrincipal AuthPrincipal principal){return taskService.extensions(principal);}
    @PostMapping("/tasks/{taskId}/extensions") public List<TaskExtensionRow> requestExtension(@AuthenticationPrincipal AuthPrincipal principal,@PathVariable Long taskId,@Valid @RequestBody CreateTaskExtensionRequest request){return taskService.requestExtension(principal,taskId,request);}
    @PostMapping("/task-extensions/{extensionId}/review") public List<TaskExtensionRow> reviewExtension(@AuthenticationPrincipal AuthPrincipal principal,@PathVariable Long extensionId,@Valid @RequestBody ReviewTaskExtensionRequest request){return taskService.reviewExtension(principal,extensionId,request);}
}

