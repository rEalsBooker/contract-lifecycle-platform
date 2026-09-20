package com.contractguard.platform.identity;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/members")
public class MemberDirectoryController {
 private final MembershipMapper mapper; public MemberDirectoryController(MembershipMapper mapper) { this.mapper = mapper; }
 @GetMapping("/active-options") public List<MemberOption> active(@AuthenticationPrincipal AuthPrincipal p) { if (!p.hasWorkspace()) throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_REQUIRED", "请先选择企业工作空间"); return mapper.findActiveOptions(p.tenantId()); }
}

