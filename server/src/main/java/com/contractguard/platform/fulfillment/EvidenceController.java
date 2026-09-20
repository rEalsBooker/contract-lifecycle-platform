package com.contractguard.platform.fulfillment;

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

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class EvidenceController {
    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping("/evidence-submissions/pending-review")
    public List<EvidenceReviewRow> pending(@AuthenticationPrincipal AuthPrincipal principal) {
        return evidenceService.pending(principal);
    }

    @PostMapping(value = "/tasks/{taskId}/evidence-submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> submit(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long taskId,
                                       @RequestParam MultipartFile file, @RequestParam(required = false) String note) {
        evidenceService.submit(principal, taskId, file, note);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/evidence-submissions/{evidenceId}/review")
    public ResponseEntity<Void> review(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long evidenceId,
                                       @RequestParam boolean approved, @RequestParam(required = false) String note) {
        evidenceService.review(principal, evidenceId, approved, note);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/evidence-submissions/{evidenceId}/file")
    public ResponseEntity<InputStreamResource> download(@AuthenticationPrincipal AuthPrincipal principal,
                                                         @PathVariable Long evidenceId) {
        EvidenceService.DownloadableEvidence file = evidenceService.download(principal, evidenceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(file.stream()));
    }
}

