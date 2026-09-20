package com.contractguard.platform.fulfillment;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.contract.ContractMapper;
import com.contractguard.platform.notification.OutboxService;
import com.contractguard.platform.security.AuthPrincipal;
import com.contractguard.platform.storage.PrivateFileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvidenceServiceTest {
    private TaskMapper taskMapper;
    private EvidenceMapper evidenceMapper;
    private EvidenceService service;
    private AuthPrincipal reviewer;

    @BeforeEach
    void setUp() {
        taskMapper = mock(TaskMapper.class);
        evidenceMapper = mock(EvidenceMapper.class);
        service = new EvidenceService(taskMapper, evidenceMapper, mock(ContractMapper.class),
                mock(PrivateFileStorage.class), mock(AuditService.class), mock(OutboxService.class));
        reviewer = new AuthPrincipal(2L, 20L, 100L, List.of("PROJECT_OWNER"), "WORKSPACE");
    }

    @Test
    void rejectionRequiresReason() {
        when(evidenceMapper.taskForReview(100L, 7L, 20L)).thenReturn(30L);

        assertThatThrownBy(() -> service.review(reviewer, 7L, false, " "))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("REJECTION_NOTE_REQUIRED"));
    }

    @Test
    void approvedEvidenceCompletesTaskAndSubmissionAtomically() {
        when(evidenceMapper.taskForReview(100L, 7L, 20L)).thenReturn(30L);
        TaskRow task = new TaskRow();
        task.setAssigneeMembershipId(10L);
        task.setTitle("提交项目验收材料");
        when(taskMapper.findById(100L, 30L)).thenReturn(task);
        when(taskMapper.reviewed(100L, 30L, 20L, "COMPLETED")).thenReturn(1);
        when(evidenceMapper.review(100L, 7L, "APPROVED", "资料完整")).thenReturn(1);

        service.review(reviewer, 7L, true, "资料完整");

        verify(taskMapper).reviewed(100L, 30L, 20L, "COMPLETED");
        verify(evidenceMapper).review(100L, 7L, "APPROVED", "资料完整");
    }

    @Test
    void nonReviewerCannotReviewSubmission() {
        when(evidenceMapper.taskForReview(100L, 7L, 20L)).thenReturn(null);

        assertThatThrownBy(() -> service.review(reviewer, 7L, true, null))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("EVIDENCE_REVIEW_FORBIDDEN"));
    }
}

