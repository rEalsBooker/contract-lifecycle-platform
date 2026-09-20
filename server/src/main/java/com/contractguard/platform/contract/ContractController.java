package com.contractguard.platform.contract;

import com.contractguard.platform.security.AuthPrincipal;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {
    private final ContractService contractService;
    public ContractController(ContractService contractService) { this.contractService = contractService; }

    @GetMapping
    public List<ContractResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return contractService.list(principal);
    }

    @GetMapping("/{contractId}")
    public ContractDetailResponse detail(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contractId) {
        return contractService.detail(principal, contractId);
    }

    @PostMapping("/{contractId}/parse-preparations")
    public ContractDetailResponse prepareParse(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contractId) {
        return contractService.prepareParse(principal, contractId);
    }

    @PostMapping("/{contractId}/clauses")
    public ContractDetailResponse createClause(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contractId,
                                               @Valid @org.springframework.web.bind.annotation.RequestBody CreateClauseRequest request) {
        return contractService.createClause(principal, contractId, request);
    }

    @PostMapping("/{contractId}/clauses/{clauseId}/confirm")
    public ContractDetailResponse confirmClause(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contractId,
                                                @PathVariable Long clauseId) {
        return contractService.confirmClause(principal, contractId, clauseId);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{contractId}/clauses/{clauseId}")
    public ContractDetailResponse updateClause(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contractId,
                                               @PathVariable Long clauseId, @Valid @org.springframework.web.bind.annotation.RequestBody UpdateClauseRequest request) {
        return contractService.updateClause(principal, contractId, clauseId, request);
    }

    @PostMapping("/{contractId}/terms/publish")
    public ContractDetailResponse publishTerms(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contractId) {
        return contractService.publishTerms(principal, contractId);
    }

    @PostMapping(value = "/drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CreateContractDraftResponse createDraft(@AuthenticationPrincipal AuthPrincipal principal,
                                                    @RequestParam String contractNo, @RequestParam String name,
                                                    @RequestParam String counterpartyName, @RequestParam BigDecimal totalAmount,
                                                    @RequestParam MultipartFile file) {
        return contractService.createDraft(principal, contractNo, name, counterpartyName, totalAmount, file);
    }

    @GetMapping("/{contractId}/source-file")
    public ResponseEntity<InputStreamResource> downloadSource(@AuthenticationPrincipal AuthPrincipal principal,
                                                               @PathVariable Long contractId) {
        ContractService.DownloadableFile file = contractService.downloadSource(principal, contractId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(file.stream()));
    }
}

