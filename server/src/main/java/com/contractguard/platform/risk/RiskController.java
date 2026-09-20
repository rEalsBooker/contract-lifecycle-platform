package com.contractguard.platform.risk;
import com.contractguard.platform.security.AuthPrincipal;import jakarta.validation.Valid;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/v1/risks") public class RiskController {private final RiskService service;public RiskController(RiskService s){service=s;}
 @GetMapping public List<RiskResponse> list(@AuthenticationPrincipal AuthPrincipal p){return service.list(p);}
 @PostMapping("/{id}/acknowledge") public RiskResponse acknowledge(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long id){return service.acknowledge(p,id);}
 @PostMapping("/{id}/start") public RiskResponse start(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long id,@Valid @RequestBody StartRiskRequest r){return service.start(p,id,r);}
 @PostMapping("/{id}/ignore") public RiskResponse ignore(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long id,@Valid @RequestBody IgnoreRiskRequest r){return service.ignore(p,id,r);}
 @PostMapping("/{id}/close") public RiskResponse close(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long id){return service.close(p,id);}
}

