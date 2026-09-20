package com.contractguard.platform.contract;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ContractMapper {
    @Insert("""
            INSERT INTO file_objects(tenant_id, uploader_membership_id, storage_key, original_filename, content_type, size_bytes, object_status)
            VALUES(#{tenantId}, #{membershipId}, #{storageKey}, #{filename}, #{contentType}, #{sizeBytes}, 'AVAILABLE')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "file.id", keyColumn = "id")
    void insertFile(@Param("file") FileObjectRow file, @Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId,
                    @Param("storageKey") String storageKey, @Param("filename") String filename,
                    @Param("contentType") String contentType, @Param("sizeBytes") Long sizeBytes);

    @Insert("""
            INSERT INTO contracts(tenant_id, owner_membership_id, contract_no, name, counterparty_name, total_amount, business_status, archive_status)
            VALUES(#{tenantId}, #{membershipId}, #{contractNo}, #{name}, #{counterpartyName}, #{totalAmount}, 'DRAFT', 'UNARCHIVED')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "contract.id", keyColumn = "id")
    void insertContract(@Param("contract") ContractRow contract, @Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId,
                        @Param("contractNo") String contractNo, @Param("name") String name,
                        @Param("counterpartyName") String counterpartyName, @Param("totalAmount") BigDecimal totalAmount);

    @Insert("""
            INSERT INTO contract_versions(tenant_id, contract_id, source_file_id, version_no, version_status, created_by_membership_id)
            VALUES(#{tenantId}, #{contractId}, #{fileId}, 1, 'DRAFT', #{membershipId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "version.id", keyColumn = "id")
    void insertVersion(@Param("version") ContractRow version, @Param("tenantId") Long tenantId, @Param("contractId") Long contractId,
                       @Param("fileId") Long fileId, @Param("membershipId") Long membershipId);

    @Update("UPDATE contracts SET current_version_id = #{versionId} WHERE id = #{contractId} AND tenant_id = #{tenantId}")
    void updateCurrentVersion(@Param("contractId") Long contractId, @Param("tenantId") Long tenantId, @Param("versionId") Long versionId);

    @Select("SELECT EXISTS(SELECT 1 FROM contracts WHERE tenant_id = #{tenantId} AND contract_no = #{contractNo})")
    boolean existsByContractNo(@Param("tenantId") Long tenantId, @Param("contractNo") String contractNo);

    @Select("""
            SELECT EXISTS(SELECT 1 FROM contract_grants
              WHERE tenant_id = #{tenantId} AND contract_id = #{contractId} AND membership_id = #{membershipId}
                AND permission_code = 'READ' AND (valid_until IS NULL OR valid_until > NOW(3)))
            """)
    boolean hasReadGrant(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId,
                         @Param("membershipId") Long membershipId);

    @Select("SELECT EXISTS(SELECT 1 FROM fulfillment_tasks WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND (assignee_membership_id=#{membershipId} OR reviewer_membership_id=#{membershipId}))")
    boolean hasTaskAccess(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId,@Param("membershipId")Long membershipId);

    @Select("""
            SELECT c.id, c.tenant_id, c.owner_membership_id, c.contract_no, c.name, c.counterparty_name, c.total_amount,
                   c.business_status, c.archive_status, c.current_version_id, c.updated_at
            FROM contracts c
            WHERE c.tenant_id = #{tenantId}
              AND (#{globalRead} = TRUE OR c.owner_membership_id = #{membershipId}
                   OR EXISTS (SELECT 1 FROM contract_grants g WHERE g.contract_id = c.id AND g.tenant_id = c.tenant_id
                              AND g.membership_id = #{membershipId} AND g.permission_code = 'READ'
                              AND (g.valid_until IS NULL OR g.valid_until > NOW(3)))
                   OR EXISTS (SELECT 1 FROM fulfillment_tasks ft WHERE ft.tenant_id=c.tenant_id AND ft.contract_id=c.id
                              AND (ft.assignee_membership_id=#{membershipId} OR ft.reviewer_membership_id=#{membershipId})))
            ORDER BY c.updated_at DESC
            """)
    List<ContractRow> findVisible(@Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId,
                                  @Param("globalRead") boolean globalRead);

    @Select("""
            SELECT c.id, c.tenant_id, c.owner_membership_id, c.contract_no, c.name, c.counterparty_name, c.total_amount,
                   c.business_status, c.archive_status, c.current_version_id, c.updated_at, f.id AS source_file_id,
                   f.storage_key, f.original_filename, f.content_type
            FROM contracts c
            JOIN contract_versions v ON v.id = c.current_version_id
            JOIN file_objects f ON f.id = v.source_file_id
            WHERE c.id = #{contractId} AND c.tenant_id = #{tenantId}
            """)
    ContractRow findDetail(@Param("contractId") Long contractId, @Param("tenantId") Long tenantId);

    @Insert("INSERT INTO contract_parse_jobs(tenant_id, contract_id, contract_version_id, job_status, execution_mode, created_by_membership_id) VALUES(#{tenantId}, #{contractId}, #{versionId}, 'WAITING_MANUAL_INPUT', 'MODEL_NOT_CONFIGURED', #{membershipId})")
    @Options(useGeneratedKeys = true, keyProperty = "job.id", keyColumn = "id")
    void insertParseJob(@Param("job") ParseJobRow job, @Param("tenantId") Long tenantId, @Param("contractId") Long contractId,
                        @Param("versionId") Long versionId, @Param("membershipId") Long membershipId);

    @Select("SELECT j.id,j.job_status,j.execution_mode,j.failure_message,j.created_at FROM contract_parse_jobs j JOIN contracts c ON c.id=j.contract_id AND c.current_version_id=j.contract_version_id WHERE j.tenant_id=#{tenantId} AND j.contract_id=#{contractId} ORDER BY j.id DESC LIMIT 1")
    ParseJobRow findLatestParseJob(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId);

    @Insert("INSERT INTO contract_clauses(tenant_id, contract_id, contract_version_id, parse_job_id, clause_type, clause_title, clause_content, source_type, source_page_no, source_excerpt, confirmation_status, created_by_membership_id) VALUES(#{tenantId}, #{contractId}, #{versionId}, #{jobId}, #{request.clauseType}, #{request.clauseTitle}, #{request.clauseContent}, 'MANUAL_ENTRY', #{request.sourcePageNo}, #{request.sourceExcerpt}, 'PENDING_CONFIRMATION', #{membershipId})")
    @Options(useGeneratedKeys = true, keyProperty = "clause.id", keyColumn = "id")
    void insertClause(@Param("clause") ContractClauseRow clause, @Param("request") CreateClauseRequest request, @Param("tenantId") Long tenantId,
                      @Param("contractId") Long contractId, @Param("versionId") Long versionId, @Param("jobId") Long jobId, @Param("membershipId") Long membershipId);

    @Select("SELECT cc.id,cc.clause_type,cc.clause_title,cc.clause_content,cc.source_type,cc.source_page_no,cc.source_excerpt,cc.confirmation_status,cc.confirmed_at FROM contract_clauses cc JOIN contracts c ON c.id=cc.contract_id AND c.current_version_id=cc.contract_version_id WHERE cc.tenant_id=#{tenantId} AND cc.contract_id=#{contractId} ORDER BY cc.id")
    List<ContractClauseRow> findClauses(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId);

    @Update("UPDATE contract_clauses SET confirmation_status = 'CONFIRMED', confirmed_by_membership_id = #{membershipId}, confirmed_at = NOW(3) WHERE id = #{clauseId} AND tenant_id = #{tenantId} AND contract_id = #{contractId} AND confirmation_status = 'PENDING_CONFIRMATION'")
    int confirmClause(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId, @Param("clauseId") Long clauseId, @Param("membershipId") Long membershipId);

    @Update("UPDATE contract_clauses SET clause_type = #{request.clauseType}, clause_title = #{request.clauseTitle}, clause_content = #{request.clauseContent}, source_page_no = #{request.sourcePageNo}, source_excerpt = #{request.sourceExcerpt}, confirmation_status = 'PENDING_CONFIRMATION', confirmed_by_membership_id = NULL, confirmed_at = NULL WHERE id = #{clauseId} AND tenant_id = #{tenantId} AND contract_id = #{contractId}")
    int updateClause(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId, @Param("clauseId") Long clauseId,
                     @Param("request") UpdateClauseRequest request);

    @Select("SELECT COUNT(*) FROM contract_clauses cc JOIN contracts c ON c.id=cc.contract_id AND c.current_version_id=cc.contract_version_id WHERE cc.tenant_id=#{tenantId} AND cc.contract_id=#{contractId} AND cc.confirmation_status<>'CONFIRMED'")
    int countUnconfirmedClauses(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId);

    @Select("SELECT COUNT(*) FROM contract_clauses cc JOIN contracts c ON c.id=cc.contract_id AND c.current_version_id=cc.contract_version_id WHERE cc.tenant_id=#{tenantId} AND cc.contract_id=#{contractId}")
    int countClauses(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId);

    @Select("SELECT COUNT(*) FROM ai_findings f JOIN contracts c ON c.id=f.contract_id AND c.current_version_id=f.contract_version_id WHERE f.tenant_id=#{tenantId} AND f.contract_id=#{contractId} AND f.review_status='PENDING_CONFIRMATION'")
    int countPendingAiFindings(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId);

    @Update("UPDATE contracts SET business_status = #{status} WHERE id = #{contractId} AND tenant_id = #{tenantId}")
    void updateBusinessStatus(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId, @Param("status") String status);
}

