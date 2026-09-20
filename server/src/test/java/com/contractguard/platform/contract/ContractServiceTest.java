package com.contractguard.platform.contract;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.security.AuthPrincipal;
import com.contractguard.platform.storage.PrivateFileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ContractServiceTest {
    @Test
    void refusesContractFromAnotherTenantBeforeOpeningPrivateFile() {
        ContractMapper contracts = mock(ContractMapper.class);
        PrivateFileStorage storage = mock(PrivateFileStorage.class);
        ContractService service = new ContractService(contracts, storage, mock(AuditService.class));
        AuthPrincipal principal = new AuthPrincipal(10L, 20L, 100L,
                List.of("PROJECT_OWNER"), "WORKSPACE");

        when(contracts.findDetail(7L, 100L)).thenReturn(null);

        assertThatThrownBy(() -> service.detail(principal, 7L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
                                .isEqualTo("CONTRACT_FORBIDDEN"));
        verifyNoInteractions(storage);
    }

    @Test
    void refusesPrivateFileDownloadWithoutOwnerGrantOrTaskAccess() {
        ContractMapper contracts = mock(ContractMapper.class);
        PrivateFileStorage storage = mock(PrivateFileStorage.class);
        ContractService service = new ContractService(contracts, storage, mock(AuditService.class));
        AuthPrincipal principal = new AuthPrincipal(10L, 20L, 100L,
                List.of("PROJECT_OWNER"), "WORKSPACE");
        ContractRow contract = new ContractRow();
        contract.setId(7L);
        contract.setTenantId(100L);
        contract.setOwnerMembershipId(999L);

        when(contracts.findDetail(7L, 100L)).thenReturn(contract);
        when(contracts.hasReadGrant(100L, 7L, 20L)).thenReturn(false);
        when(contracts.hasTaskAccess(100L, 7L, 20L)).thenReturn(false);

        assertThatThrownBy(() -> service.downloadSource(principal, 7L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
                                .isEqualTo("CONTRACT_FORBIDDEN"));
        verifyNoInteractions(storage);
    }
}
