package com.contractguard.platform.contract;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.security.AuthPrincipal;
import com.contractguard.platform.storage.PrivateFileStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ContractService {
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private final ContractMapper contractMapper;
    private final PrivateFileStorage privateFileStorage;
    private final AuditService auditService;

    public ContractService(ContractMapper contractMapper, PrivateFileStorage privateFileStorage, AuditService auditService) {
        this.contractMapper = contractMapper;
        this.privateFileStorage = privateFileStorage;
        this.auditService = auditService;
    }

    @Transactional
    public CreateContractDraftResponse createDraft(AuthPrincipal principal, String contractNo, String name,
                                                   String counterpartyName, BigDecimal totalAmount, MultipartFile file) {
        requireWorkspace(principal);
        requireWriteRole(principal);
        validateFields(contractNo, name, counterpartyName, totalAmount);
        validateFile(file);
        if (contractMapper.existsByContractNo(principal.tenantId(), contractNo.trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONTRACT_NO_EXISTS", "合同编号在当前企业内已存在");
        }

        String storageKey = null;
        try {
            storageKey = privateFileStorage.store(principal.tenantId(), file);
            FileObjectRow storedFile = new FileObjectRow();
            String contentType = file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".pdf")
                    ? "application/pdf" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            contractMapper.insertFile(storedFile, principal.tenantId(), principal.membershipId(), storageKey,
                    safeFilename(file.getOriginalFilename()), contentType, file.getSize());

            ContractRow contract = new ContractRow();
            contractMapper.insertContract(contract, principal.tenantId(), principal.membershipId(), contractNo.trim(),
                    name.trim(), counterpartyName.trim(), totalAmount);
            ContractRow version = new ContractRow();
            contractMapper.insertVersion(version, principal.tenantId(), contract.getId(), storedFile.getId(), principal.membershipId());
            contractMapper.updateCurrentVersion(contract.getId(), principal.tenantId(), version.getId());
            auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(),
                    "CONTRACT_DRAFT_CREATED", "CONTRACT", contract.getId().toString());
            return new CreateContractDraftResponse(contract.getId(), contractNo.trim(), name.trim(), "DRAFT", totalAmount,
                    safeFilename(file.getOriginalFilename()));
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORE_FAILED", "合同文件保存失败，请重试");
        } catch (RuntimeException exception) {
            deleteQuietly(storageKey);
            throw exception;
        }
    }

    public List<ContractResponse> list(AuthPrincipal principal) {
        requireWorkspace(principal);
        return contractMapper.findVisible(principal.tenantId(), principal.membershipId(), hasGlobalRead(principal))
                .stream().map(ContractResponse::from).toList();
    }

    public DownloadableFile downloadSource(AuthPrincipal principal, Long contractId) {
        requireWorkspace(principal);
        ContractRow contract = contractMapper.findDetail(contractId, principal.tenantId());
        if (contract == null || !canRead(principal, contract)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CONTRACT_FORBIDDEN", "无权读取该合同或合同文件");
        }
        try {
            return new DownloadableFile(privateFileStorage.open(contract.getStorageKey()), contract.getOriginalFilename(), contract.getContentType());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_UNAVAILABLE", "合同文件不可读取，请联系管理员");
        }
    }

    public ContractDetailResponse detail(AuthPrincipal principal, Long contractId) {
        ContractRow contract = requireReadableContract(principal, contractId);
        return detailOf(principal, contract);
    }

    @Transactional
    public ContractDetailResponse prepareParse(AuthPrincipal principal, Long contractId) {
        ContractRow contract = requireWritableContract(principal, contractId);
        ParseJobRow latest = contractMapper.findLatestParseJob(principal.tenantId(), contractId);
        if (latest == null) {
            ParseJobRow job = new ParseJobRow();
            contractMapper.insertParseJob(job, principal.tenantId(), contractId, contract.getCurrentVersionId(), principal.membershipId());
            contractMapper.updateBusinessStatus(principal.tenantId(), contractId, "TERMS_REVIEW");
            auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(), "CONTRACT_PARSE_PREPARED", "CONTRACT", contractId.toString());
        }
        return detail(principal, contractId);
    }

    @Transactional
    public ContractDetailResponse createClause(AuthPrincipal principal, Long contractId, CreateClauseRequest request) {
        ContractRow contract = requireWritableContract(principal, contractId);
        if (request.sourcePageNo() < 1 || request.clauseType().length() > 32 || request.clauseTitle().length() > 255) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CLAUSE_FIELD_INVALID", "请填写有效的条款类型、标题和原文页码");
        }
        ParseJobRow job = contractMapper.findLatestParseJob(principal.tenantId(), contractId);
        if (job == null) throw new ApiException(HttpStatus.CONFLICT, "PARSE_PREPARATION_REQUIRED", "请先创建解析准备记录");
        ContractClauseRow clause = new ContractClauseRow();
        contractMapper.insertClause(clause, request, principal.tenantId(), contractId, contract.getCurrentVersionId(), job.getId(), principal.membershipId());
        auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(), "CONTRACT_CLAUSE_MANUALLY_ADDED", "CONTRACT_CLAUSE", clause.getId().toString());
        return detail(principal, contractId);
    }

    @Transactional
    public ContractDetailResponse confirmClause(AuthPrincipal principal, Long contractId, Long clauseId) {
        requireWritableContract(principal, contractId);
        if (contractMapper.confirmClause(principal.tenantId(), contractId, clauseId, principal.membershipId()) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CLAUSE_NOT_CONFIRMABLE", "条款不存在，或已被确认");
        }
        auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(), "CONTRACT_CLAUSE_CONFIRMED", "CONTRACT_CLAUSE", clauseId.toString());
        return detail(principal, contractId);
    }

    @Transactional
    public ContractDetailResponse updateClause(AuthPrincipal principal, Long contractId, Long clauseId, UpdateClauseRequest request) {
        requireWritableContract(principal, contractId);
        if (request.sourcePageNo() < 1 || request.clauseType().length() > 32 || request.clauseTitle().length() > 255) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CLAUSE_FIELD_INVALID", "请填写有效的条款类型、标题和原文页码");
        }
        if (contractMapper.updateClause(principal.tenantId(), contractId, clauseId, request) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CLAUSE_NOT_FOUND", "未找到可修改的条款");
        }
        auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(), "CONTRACT_CLAUSE_UPDATED", "CONTRACT_CLAUSE", clauseId.toString());
        return detail(principal, contractId);
    }

    @Transactional
    public ContractDetailResponse publishTerms(AuthPrincipal principal, Long contractId) {
        requireWritableContract(principal, contractId);
        if (contractMapper.countClauses(principal.tenantId(), contractId) == 0 || contractMapper.countUnconfirmedClauses(principal.tenantId(), contractId) > 0
                || contractMapper.countPendingAiFindings(principal.tenantId(),contractId)>0) {
            throw new ApiException(HttpStatus.CONFLICT, "TERMS_NOT_CONFIRMED", "至少需要一条已人工确认的关键条款，且不能存在待确认条款");
        }
        contractMapper.updateBusinessStatus(principal.tenantId(), contractId, "TERMS_CONFIRMED");
        auditService.record(principal.tenantId(), principal.userId(), principal.membershipId(), "CONTRACT_TERMS_PUBLISHED", "CONTRACT", contractId.toString());
        return detail(principal, contractId);
    }

    private ContractDetailResponse detailOf(AuthPrincipal principal, ContractRow contract) {
        return ContractDetailResponse.of(contract, contractMapper.findLatestParseJob(principal.tenantId(), contract.getId()),
                contractMapper.findClauses(principal.tenantId(), contract.getId()));
    }

    private ContractRow requireReadableContract(AuthPrincipal principal, Long contractId) {
        requireWorkspace(principal);
        ContractRow contract = contractMapper.findDetail(contractId, principal.tenantId());
        if (contract == null || !canRead(principal, contract)) throw new ApiException(HttpStatus.FORBIDDEN, "CONTRACT_FORBIDDEN", "无权访问该合同");
        return contract;
    }

    private ContractRow requireWritableContract(AuthPrincipal principal, Long contractId) {
        requireWriteRole(principal);
        return requireReadableContract(principal, contractId);
    }

    private boolean canRead(AuthPrincipal principal, ContractRow contract) {
        return hasGlobalRead(principal) || principal.membershipId().equals(contract.getOwnerMembershipId())
                || contractMapper.hasReadGrant(principal.tenantId(), contract.getId(), principal.membershipId())
                || contractMapper.hasTaskAccess(principal.tenantId(), contract.getId(), principal.membershipId());
    }

    private boolean hasGlobalRead(AuthPrincipal principal) {
        return principal.roleCodes().stream().anyMatch(role -> role.equals("ENTERPRISE_ADMIN") || role.equals("CONTRACT_OWNER")
                || role.equals("FINANCE") || role.equals("LEGAL"));
    }

    private void requireWriteRole(AuthPrincipal principal) {
        if (principal.roleCodes().stream().noneMatch(role -> role.equals("ENTERPRISE_ADMIN") || role.equals("CONTRACT_OWNER"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CONTRACT_WRITE_FORBIDDEN", "当前角色无权创建合同草稿");
        }
    }

    private void requireWorkspace(AuthPrincipal principal) {
        if (!principal.hasWorkspace()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "WORKSPACE_REQUIRED", "请先选择企业工作空间");
        }
    }

    private void validateFields(String contractNo, String name, String counterpartyName, BigDecimal totalAmount) {
        if (contractNo == null || contractNo.isBlank() || name == null || name.isBlank() || counterpartyName == null || counterpartyName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONTRACT_FIELD_REQUIRED", "合同编号、名称和相对方不能为空");
        }
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONTRACT_AMOUNT_INVALID", "合同总金额必须大于零");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED", "请上传合同文件");
        if (file.getSize() > MAX_FILE_SIZE) throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "合同文件不得超过 20MB");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".pdf") && !filename.endsWith(".docx")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TYPE_UNSUPPORTED", "当前仅支持 PDF 或 DOCX 合同文件");
        }
    }

    private String safeFilename(String filename) {
        return filename == null ? "contract" : filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void deleteQuietly(String storageKey) {
        if (storageKey == null) return;
        try { privateFileStorage.delete(storageKey); } catch (IOException ignored) { }
    }

    public record DownloadableFile(InputStream stream, String filename, String contentType) { }
}

