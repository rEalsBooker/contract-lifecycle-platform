package com.contractguard.platform.audit;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditMapper {
    @Insert("""
            INSERT INTO audit_logs(tenant_id, actor_user_id, actor_membership_id, action, object_type, object_id, result, detail_json)
            VALUES(#{tenantId}, #{actorUserId}, #{actorMembershipId}, #{action}, #{objectType}, #{objectId}, #{result}, CAST(#{detailJson} AS JSON))
            """)
    void insert(@Param("tenantId") Long tenantId, @Param("actorUserId") Long actorUserId,
                @Param("actorMembershipId") Long actorMembershipId, @Param("action") String action,
                @Param("objectType") String objectType, @Param("objectId") String objectId,
                @Param("result") String result, @Param("detailJson") String detailJson);
}

