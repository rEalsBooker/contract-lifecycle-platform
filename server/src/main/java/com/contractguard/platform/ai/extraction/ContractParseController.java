package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.contract.ParseJobRow;
import com.contractguard.platform.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contracts/{contractId}/parse-jobs")
public class ContractParseController {
    private final ContractParseService service;
    public ContractParseController(ContractParseService service) { this.service = service; }

    @PostMapping
    public ParseJobRow request(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contractId,
                               @Valid @RequestBody CreateParseJobRequest request) {
        return service.request(principal, contractId, request);
    }
}


