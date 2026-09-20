package com.contractguard.platform.fulfillment;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.contract.ContractMapper;
import com.contractguard.platform.identity.MembershipMapper;
import com.contractguard.platform.notification.OutboxService;
import com.contractguard.platform.security.AuthPrincipal;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskServiceTest {
    @Test
    void assigneeCannotChooseSelfAsReviewer() {
        TaskMapper taskMapper = mock(TaskMapper.class);
        TaskRow task = new TaskRow();
        task.setId(3L);
        when(taskMapper.findById(100L, 3L)).thenReturn(task);
        TaskService service = new TaskService(taskMapper, mock(ContractMapper.class), mock(AuditService.class), mock(MembershipMapper.class), mock(OutboxService.class));
        AuthPrincipal principal = new AuthPrincipal(1L, 10L, 100L, List.of("ENTERPRISE_ADMIN"), "WORKSPACE");

        assertThatThrownBy(() -> service.claim(principal, 3L, new ClaimTaskRequest(LocalDate.now().plusDays(1), 10L)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TASK_REVIEWER_INVALID"));
    }
}

