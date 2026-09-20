package com.contractguard.platform.notification;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface NotificationMapper {
    @Select("""
            SELECT id,notification_type,title,content,object_type,object_id,route,read_at,created_at
            FROM notifications
            WHERE tenant_id=#{tenantId} AND recipient_membership_id=#{membershipId}
              AND (#{unreadOnly}=FALSE OR read_at IS NULL)
            ORDER BY created_at DESC,id DESC LIMIT 100
            """)
    List<NotificationRow> findInbox(@Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId,
                                    @Param("unreadOnly") boolean unreadOnly);

    @Select("SELECT COUNT(*) FROM notifications WHERE tenant_id=#{tenantId} AND recipient_membership_id=#{membershipId} AND read_at IS NULL")
    long countUnread(@Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId);

    @Update("UPDATE notifications SET read_at=COALESCE(read_at,NOW(3)) WHERE tenant_id=#{tenantId} AND recipient_membership_id=#{membershipId} AND id=#{id}")
    int markRead(@Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId, @Param("id") Long id);

    @Select("SELECT EXISTS(SELECT 1 FROM notifications WHERE tenant_id=#{tenantId} AND recipient_membership_id=#{membershipId} AND id=#{id})")
    boolean existsOwned(@Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId, @Param("id") Long id);

    @Update("UPDATE notifications SET read_at=NOW(3) WHERE tenant_id=#{tenantId} AND recipient_membership_id=#{membershipId} AND read_at IS NULL")
    int markAllRead(@Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId);

    @Insert("""
            INSERT IGNORE INTO notifications(tenant_id,recipient_membership_id,notification_type,title,content,
              object_type,object_id,route,dedup_key)
            VALUES(#{tenantId},#{recipientMembershipId},#{notificationType},#{title},#{content},
              #{objectType},#{objectId},#{route},#{dedupKey})
            """)
    int insert(@Param("tenantId") Long tenantId, @Param("recipientMembershipId") Long recipientMembershipId,
               @Param("notificationType") String notificationType, @Param("title") String title,
               @Param("content") String content, @Param("objectType") String objectType,
               @Param("objectId") Long objectId, @Param("route") String route, @Param("dedupKey") String dedupKey);

    @Select("""
            SELECT DISTINCT m.id FROM memberships m JOIN membership_roles mr ON mr.membership_id=m.id
            JOIN app_users u ON u.id=m.user_id
            WHERE m.tenant_id=#{tenantId} AND m.status='ACTIVE' AND u.status='ACTIVE'
              AND mr.role_code IN ('ENTERPRISE_ADMIN','CONTRACT_OWNER','FINANCE')
            """)
    List<Long> findFinancialRiskRecipients(Long tenantId);
}

