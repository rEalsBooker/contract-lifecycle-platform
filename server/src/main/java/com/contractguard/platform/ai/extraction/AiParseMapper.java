package com.contractguard.platform.ai.extraction;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import com.contractguard.platform.contract.ParseJobRow;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AiParseMapper {
    @Select("SELECT EXISTS(SELECT 1 FROM contract_parse_jobs WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND job_status IN ('QUEUED','PROCESSING','RETRY_WAIT'))")
    boolean hasActiveJob(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId);

    @Select("SELECT id,job_status,execution_mode,failure_message,created_at FROM contract_parse_jobs WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND request_key=#{requestKey}")
    ParseJobRow findByRequestKey(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId,
                                 @Param("requestKey") String requestKey);

    @Insert("""
            INSERT INTO contract_parse_jobs(tenant_id,contract_id,contract_version_id,job_status,execution_mode,
              request_key,provider_name,model_name,prompt_version,created_by_membership_id)
            VALUES(#{tenantId},#{contractId},#{versionId},'QUEUED','AI_MODEL',#{requestKey},#{provider},#{model},'contract-extract-v1',#{membershipId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "job.id", keyColumn = "id")
    void insertQueued(@Param("job") ParseJobRow job, @Param("tenantId") Long tenantId,
                      @Param("contractId") Long contractId, @Param("versionId") Long versionId,
                      @Param("requestKey") String requestKey, @Param("provider") String provider,
                      @Param("model") String model, @Param("membershipId") Long membershipId);

    @Update("""
            UPDATE contract_parse_jobs SET job_status='PROCESSING',attempt_count=attempt_count+1,
              started_at=COALESCE(started_at,NOW(3)),failure_message=NULL
            WHERE tenant_id=#{tenantId} AND id=#{jobId} AND job_status IN ('QUEUED','RETRY_WAIT') AND attempt_count<3
            """)
    int claim(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId);

    @Select("""
            SELECT j.id,j.tenant_id,j.contract_id,j.contract_version_id,j.created_by_membership_id,j.attempt_count,
              f.id AS source_file_id,f.storage_key,f.content_type
            FROM contract_parse_jobs j JOIN contract_versions v ON v.id=j.contract_version_id AND v.tenant_id=j.tenant_id
            JOIN file_objects f ON f.id=v.source_file_id AND f.tenant_id=j.tenant_id
            WHERE j.tenant_id=#{tenantId} AND j.id=#{jobId}
            """)
    ParseJobExecutionRow findExecution(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId);

    @Insert("""
            INSERT INTO ai_findings(tenant_id,contract_id,contract_version_id,parse_job_id,finding_type,title,content,
              source_page_no,source_excerpt,confidence,raw_json)
            VALUES(#{row.tenantId},#{row.contractId},#{row.contractVersionId},#{row.id},#{finding.findingType},
              #{finding.title},#{finding.content},#{finding.sourcePageNo},#{finding.sourceExcerpt},#{finding.confidence},CAST(#{rawJson} AS JSON))
            """)
    void insertFinding(@Param("row") ParseJobExecutionRow row, @Param("finding") ExtractedFinding finding,
                       @Param("rawJson") String rawJson);

    @Update("UPDATE contract_parse_jobs SET job_status='SUCCEEDED',finished_at=NOW(3),failure_message=NULL WHERE tenant_id=#{tenantId} AND id=#{jobId} AND job_status='PROCESSING'")
    int succeed(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId);

    @Update("""
            UPDATE contract_parse_jobs SET job_status=#{status},failure_message=#{message},
              finished_at=CASE WHEN #{status}='FAILED' THEN NOW(3) ELSE NULL END
            WHERE tenant_id=#{tenantId} AND id=#{jobId} AND job_status='PROCESSING'
            """)
    int fail(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId,
             @Param("status") String status, @Param("message") String message);

    @Select("SELECT id,contract_id,contract_version_id,parse_job_id,finding_type,title,content,source_page_no,source_excerpt,confidence,review_status,review_note,created_at,reviewed_at FROM ai_findings WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} ORDER BY id")
    List<AiFindingRow> findFindings(@Param("tenantId") Long tenantId,@Param("contractId") Long contractId);

    @Select("SELECT id,contract_id,contract_version_id,parse_job_id,finding_type,title,content,source_page_no,source_excerpt,confidence,review_status,review_note,created_at,reviewed_at FROM ai_findings WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND id=#{findingId} FOR UPDATE")
    AiFindingRow lockFinding(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId,@Param("findingId")Long findingId);

    @Update("UPDATE ai_findings SET finding_type=#{request.findingType},title=#{request.title},content=#{request.content},source_page_no=#{request.sourcePageNo},source_excerpt=#{request.sourceExcerpt},review_status='PENDING_CONFIRMATION',reviewed_by_membership_id=NULL,reviewed_at=NULL,review_note=NULL WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND id=#{findingId} AND review_status='PENDING_CONFIRMATION'")
    int updateFinding(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId,@Param("findingId")Long findingId,@Param("request")UpdateAiFindingRequest request);

    @Insert("INSERT INTO contract_clauses(tenant_id,contract_id,contract_version_id,parse_job_id,clause_type,clause_title,clause_content,source_type,source_page_no,source_excerpt,confidence,confirmation_status,confirmed_by_membership_id,confirmed_at,created_by_membership_id) VALUES(#{tenantId},#{finding.contractId},#{finding.contractVersionId},#{finding.parseJobId},#{finding.findingType},#{finding.title},#{finding.content},'AI_EXTRACTION',#{finding.sourcePageNo},#{finding.sourceExcerpt},#{finding.confidence},'CONFIRMED',#{membershipId},NOW(3),#{membershipId})")
    void insertConfirmedClause(@Param("tenantId")Long tenantId,@Param("membershipId")Long membershipId,@Param("finding")AiFindingRow finding);

    @Update("UPDATE ai_findings SET review_status=#{status},reviewed_by_membership_id=#{membershipId},review_note=#{note},reviewed_at=NOW(3) WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND id=#{findingId} AND review_status='PENDING_CONFIRMATION'")
    int reviewFinding(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId,@Param("findingId")Long findingId,@Param("membershipId")Long membershipId,@Param("status")String status,@Param("note")String note);

    @Select("SELECT COUNT(*) FROM fulfillment_tasks WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND task_status NOT IN ('COMPLETED','CANCELLED')")
    long countActiveTasks(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId);
    @Select("SELECT COALESCE(SUM(p.plan_amount-(SELECT COALESCE(SUM(r.amount),0) FROM receipt_records r WHERE r.tenant_id=p.tenant_id AND r.receivable_plan_id=p.id AND r.record_status='ACTIVE')),0) FROM receivable_plans p WHERE p.tenant_id=#{tenantId} AND p.contract_id=#{contractId}")
    BigDecimal receivableBalance(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId);
    @Select("SELECT COUNT(*) FROM risk_cases WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND risk_status NOT IN ('CLOSED','IGNORED')")
    long countActiveRisks(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId);
    @Insert("INSERT INTO ai_question_logs(tenant_id,contract_id,contract_version_id,membership_id,question,answer_text,source_json,tool_context_json,model_name) VALUES(#{tenantId},#{contractId},#{versionId},#{membershipId},#{question},#{answer},CAST(#{sourcesJson} AS JSON),CAST(#{toolsJson} AS JSON),#{modelName})")
    void insertQuestionLog(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId,@Param("versionId")Long versionId,@Param("membershipId")Long membershipId,@Param("question")String question,@Param("answer")String answer,@Param("sourcesJson")String sourcesJson,@Param("toolsJson")String toolsJson,@Param("modelName")String modelName);
}


