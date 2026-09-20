package com.contractguard.platform.contract;
import com.contractguard.platform.security.AuthPrincipal; import jakarta.validation.Valid; import org.springframework.http.MediaType; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.util.List;
@RestController @RequestMapping("/api/v1/contracts/{contractId}") public class ContractLifecycleController {private final ContractLifecycleService service;public ContractLifecycleController(ContractLifecycleService service){this.service=service;}
 @GetMapping("/versions")public List<ContractVersionRow> versions(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId){return service.versions(p,contractId);}
 @PostMapping(value="/versions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)public List<ContractVersionRow> upload(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId,@RequestParam MultipartFile file,@RequestParam String changeSummary,@RequestParam Long reviewerMembershipId){return service.uploadVersion(p,contractId,file,changeSummary,reviewerMembershipId);}
 @PostMapping("/version-change-requests/{requestId}/review")public List<ContractVersionRow> review(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId,@PathVariable Long requestId,@Valid @RequestBody ReviewVersionRequest request){return service.reviewVersion(p,contractId,requestId,request);}
 @GetMapping("/completion-check")public ContractCompletionCheck check(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId){return service.check(p,contractId);}
 @PostMapping("/complete")public ContractDetailResponse complete(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId){return service.complete(p,contractId);}
 @PostMapping("/terminate")public ContractDetailResponse terminate(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId,@RequestParam String reason){return service.terminate(p,contractId,reason);}
 @PostMapping("/archive")public ContractDetailResponse archive(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId,@RequestParam String reason){return service.archive(p,contractId,reason);}
 @PostMapping("/unarchive")public ContractDetailResponse unarchive(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId,@RequestParam String reason){return service.unarchive(p,contractId,reason);}
}

