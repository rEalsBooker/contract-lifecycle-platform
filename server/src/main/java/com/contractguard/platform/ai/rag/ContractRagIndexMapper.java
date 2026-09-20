package com.contractguard.platform.ai.rag;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.ai.assistant.*;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContractRagIndexMapper {
    @Insert("""
            INSERT INTO contract_vector_indexes(tenant_id,contract_id,contract_version_id,index_status,chunk_count,embedding_model,last_error,indexed_at)
            VALUES(#{tenantId},#{contractId},#{versionId},#{status},#{chunkCount},#{model},#{error},
              CASE WHEN #{status}='READY' THEN NOW(3) ELSE NULL END)
            ON DUPLICATE KEY UPDATE index_status=VALUES(index_status),chunk_count=VALUES(chunk_count),
              embedding_model=VALUES(embedding_model),last_error=VALUES(last_error),indexed_at=VALUES(indexed_at)
            """)
    void upsert(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId,
                @Param("versionId") Long versionId, @Param("status") String status,
                @Param("chunkCount") int chunkCount, @Param("model") String model,
                @Param("error") String error);

    @Select("""
            SELECT EXISTS(SELECT 1 FROM contract_vector_indexes
              WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND contract_version_id=#{versionId}
                AND index_status='READY')
            """)
    boolean ready(@Param("tenantId") Long tenantId, @Param("contractId") Long contractId,
                  @Param("versionId") Long versionId);
}


