package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/contracts/{contractId}/ai-findings")
public class AiFindingController {
    private final AiFindingService service; public AiFindingController(AiFindingService service){this.service=service;}
    @GetMapping public List<AiFindingResponse> list(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId){return service.list(p,contractId);}
    @PutMapping("/{findingId}") public List<AiFindingResponse> update(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId,@PathVariable Long findingId,@Valid @RequestBody UpdateAiFindingRequest request){return service.update(p,contractId,findingId,request);}
    @PostMapping("/{findingId}/confirm") public List<AiFindingResponse> confirm(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId,@PathVariable Long findingId){return service.confirm(p,contractId,findingId);}
    @PostMapping("/{findingId}/reject") public List<AiFindingResponse> reject(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId,@PathVariable Long findingId,@RequestParam String note){return service.reject(p,contractId,findingId,note);}
}


