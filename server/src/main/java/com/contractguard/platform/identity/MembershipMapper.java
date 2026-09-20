package com.contractguard.platform.identity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MembershipMapper {
    @Select("""
            SELECT m.id AS membership_id, t.id AS tenant_id, t.name AS tenant_name, m.status AS membership_status
            FROM memberships m JOIN tenants t ON t.id = m.tenant_id
            WHERE m.user_id = #{userId} AND m.status = 'ACTIVE' AND t.status = 'ACTIVE'
            ORDER BY t.name
            """)
    List<MembershipRow> findActiveByUserId(Long userId);

    @Select("""
            SELECT m.id AS membership_id, t.id AS tenant_id, t.name AS tenant_name, m.status AS membership_status
            FROM memberships m JOIN tenants t ON t.id = m.tenant_id
            WHERE m.id = #{membershipId} AND m.user_id = #{userId}
              AND m.status = 'ACTIVE' AND t.status = 'ACTIVE'
            """)
    MembershipRow findActiveByIdAndUserId(Long membershipId, Long userId);

    @Select("SELECT role_code FROM membership_roles WHERE membership_id = #{membershipId} ORDER BY role_code")
    List<String> findRoleCodes(Long membershipId);

    @Select("SELECT m.id AS membership_id, u.display_name FROM memberships m JOIN app_users u ON u.id = m.user_id WHERE m.tenant_id = #{tenantId} AND m.status = 'ACTIVE' AND u.status = 'ACTIVE' ORDER BY u.display_name")
    List<MemberOption> findActiveOptions(Long tenantId);
    @Select("SELECT EXISTS(SELECT 1 FROM memberships WHERE tenant_id = #{tenantId} AND id = #{membershipId} AND status = 'ACTIVE')") boolean existsActive(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId, @org.apache.ibatis.annotations.Param("membershipId") Long membershipId);
}

