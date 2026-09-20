package com.contractguard.platform.fulfillment;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.contract.ContractMapper;
import com.contractguard.platform.contract.FileObjectRow;
import com.contractguard.platform.notification.OutboxService;
import com.contractguard.platform.security.AuthPrincipal;
import com.contractguard.platform.storage.PrivateFileStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
public class EvidenceService {
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private final TaskMapper taskMapper;
    private final EvidenceMapper evidenceMapper;
    private final ContractMapper contractMapper;
    private final PrivateFileStorage storage;
    private final AuditService auditService;
    private final OutboxService outboxService;

    public EvidenceService(TaskMapper taskMapper, EvidenceMapper evidenceMapper, ContractMapper contractMapper,
                           PrivateFileStorage storage, AuditService auditService, OutboxService outboxService) {
        this.taskMapper = taskMapper;
        this.evidenceMapper = evidenceMapper;
        this.contractMapper = contractMapper;
        this.storage = storage;
        this.auditService = auditService;
        this.outboxService = outboxService;
    }

    @Transactional
    public void submit(AuthPrincipal principal, Long taskId, MultipartFile file, String note) {
        requireWorkspace(principal);
        TaskRow task = taskMapper.findById(principal.tenantId(), taskId);
        if (task == null || !principal.membershipId().equals(task.getAssigneeMembershipId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TASK_FORBIDDEN", "无权访问该履约任务");
        }
        if (!"IN_PROGRESS".equals(task.getTaskStatus()) || task.getReviewerMembershipId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "EVIDENCE_NOT_ALLOWED", "仅执行中的负责人可提交已指定审核人的凭证");
        }
        if (principal.membershipId().equals(task.getReviewerMembershipId())) {
            throw new ApiException(HttpStatus.CONFLICT, "SELF_REVIEW_FORBIDDEN", "执行人与审核人不能是同一成员");
        }
        validateFile(file);

        String storageKey = null;
        try {
            storageKey = storage.store(principal.tenantId(), file);
            FileObjectRow storedFile = new FileObjectRow();
            contractMapper.insertFile(storedFile, principal.tenantId(), principal.membershipId(), storageKey,
                    safeFilename(file.getOriginalFilename()), contentType(file), file.getSize());
            if (taskMapper.submit(principal.tenantId(), taskId, principal.membershipId()) == 0) {
                throw new ApiException(HttpStatus.CONFLICT, "TASK_STATUS_CONFLICT", "任务状态已变化，请刷新后重试");
            }
            evidenceMapper.insert(principal.tenantId(), taskId, storedFile.getId(), principal.membershipId(),
                    task.getReviewerMembershipId(), normalizeNote(note));
            outboxService.enqueueNotification(principal.tenantId(), task.getReviewerMembershipId(),
                    "EVIDENCE_PENDING_REVIEW", "有新的履约凭证待审核", task.getTitle(),
                    "TASK", taskId, "/tasks?taskId=" + taskId,
                    "EVIDENCE_SUBMITTED:TASK:" + taskId + ":FILE:" + storedFile.getId());
            auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(),
                    "EVIDENCE_SUBMITTED", "TASK", taskId.toString());
        } catch (IOException exception) {
            deleteQuietly(storageKey);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "EVIDENCE_STORE_FAILED", "凭证保存失败，请重试");
        } catch (RuntimeException exception) {
            deleteQuietly(storageKey);
            throw exception;
        }
    }

    public List<EvidenceReviewRow> pending(AuthPrincipal principal) {
        requireWorkspace(principal);
        return evidenceMapper.pending(principal.tenantId(), principal.membershipId());
    }

    @Transactional
    public void review(AuthPrincipal principal, Long evidenceId, boolean approved, String note) {
        requireWorkspace(principal);
        Long taskId = evidenceMapper.taskForReview(principal.tenantId(), evidenceId, principal.membershipId());
        if (taskId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EVIDENCE_REVIEW_FORBIDDEN", "无权审核该凭证，或凭证已处理");
        }
        if (!approved && (note == null || note.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REJECTION_NOTE_REQUIRED", "驳回凭证必须填写原因");
        }
        TaskRow task = taskMapper.findById(principal.tenantId(), taskId);
        if (task == null || task.getAssigneeMembershipId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "TASK_STATUS_CONFLICT", "关联任务已不可用，请刷新后重试");
        }
        String nextTaskStatus = approved ? "COMPLETED" : "IN_PROGRESS";
        if (taskMapper.reviewed(principal.tenantId(), taskId, principal.membershipId(), nextTaskStatus) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "TASK_STATUS_CONFLICT", "任务状态已变化，请刷新后重试");
        }
        if (evidenceMapper.review(principal.tenantId(), evidenceId, approved ? "APPROVED" : "REJECTED", normalizeNote(note)) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "EVIDENCE_REVIEWED", "凭证已被处理，请刷新后查看结果");
        }
        auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(),
                approved ? "EVIDENCE_APPROVED" : "EVIDENCE_REJECTED", "TASK", taskId.toString());
        outboxService.enqueueNotification(principal.tenantId(), task.getAssigneeMembershipId(),
                approved ? "EVIDENCE_APPROVED" : "EVIDENCE_REJECTED",
                approved ? "履约凭证审核通过" : "履约凭证被驳回",
                approved ? task.getTitle() + " 已完成" : task.getTitle() + "：" + normalizeNote(note),
                "TASK", taskId, "/tasks?taskId=" + taskId,
                "EVIDENCE_REVIEWED:" + evidenceId + ":" + (approved ? "APPROVED" : "REJECTED"));
    }

    public DownloadableEvidence download(AuthPrincipal principal, Long evidenceId) {
        requireWorkspace(principal);
        EvidenceFileRow file = evidenceMapper.findAccessibleFile(principal.tenantId(), evidenceId, principal.membershipId());
        if (file == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EVIDENCE_FILE_FORBIDDEN", "无权读取该凭证文件");
        }
        try {
            return new DownloadableEvidence(storage.open(file.getStorageKey()), file.getOriginalFilename(), file.getContentType());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EVIDENCE_FILE_UNAVAILABLE", "凭证文件不可读取，请联系管理员");
        }
    }

    private void requireWorkspace(AuthPrincipal principal) {
        if (!principal.hasWorkspace()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_REQUIRED", "请先选择企业工作空间");
        }
    }

    private void validateFile(MultipartFile file) {
        String filename = file == null || file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EVIDENCE_FILE_REQUIRED", "请选择凭证文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EVIDENCE_FILE_TOO_LARGE", "凭证文件不得超过 20MB");
        }
        if (!(filename.endsWith(".pdf") || filename.endsWith(".docx") || filename.endsWith(".png")
                || filename.endsWith(".jpg") || filename.endsWith(".jpeg"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EVIDENCE_FILE_TYPE_UNSUPPORTED", "凭证仅支持 PDF、DOCX、PNG 或 JPEG");
        }
    }

    private String contentType(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".pdf")) return "application/pdf";
        if (filename.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (filename.endsWith(".png")) return "image/png";
        return "image/jpeg";
    }

    private String safeFilename(String filename) {
        return filename == null ? "evidence" : filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) return null;
        String trimmed = note.trim();
        if (trimmed.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EVIDENCE_NOTE_TOO_LONG", "说明不得超过 1000 个字符");
        }
        return trimmed;
    }

    private void deleteQuietly(String storageKey) {
        if (storageKey == null) return;
        try { storage.delete(storageKey); } catch (IOException ignored) { }
    }

    public record DownloadableEvidence(InputStream stream, String filename, String contentType) { }
}

