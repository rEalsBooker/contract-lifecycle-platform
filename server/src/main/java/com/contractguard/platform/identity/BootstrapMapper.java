package com.contractguard.platform.identity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BootstrapMapper {
    @Select("SELECT COUNT(*) FROM app_users WHERE username = #{username}")
    long countUserByUsername(@Param("username") String username);

    @Insert("INSERT INTO tenants(code, name, status) VALUES(#{code}, #{name}, 'ACTIVE')")
    void insertTenant(@Param("code") String code, @Param("name") String name);

    @Select("SELECT id FROM tenants WHERE code = #{code}")
    long findTenantIdByCode(@Param("code") String code);

    @Insert("""
            INSERT INTO app_users(username, password_hash, display_name, status)
            VALUES(#{username}, #{passwordHash}, #{displayName}, 'ACTIVE')
            """)
    void insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash,
                    @Param("displayName") String displayName);

    @Select("SELECT id FROM app_users WHERE username = #{username}")
    long findUserIdByUsername(@Param("username") String username);

    @Insert("INSERT INTO memberships(tenant_id, user_id, status) VALUES(#{tenantId}, #{userId}, 'ACTIVE')")
    void insertMembership(@Param("tenantId") long tenantId, @Param("userId") long userId);

    @Select("SELECT id FROM memberships WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    long findMembershipId(@Param("tenantId") long tenantId, @Param("userId") long userId);
    @Select("SELECT id FROM memberships WHERE tenant_id = #{tenantId} AND user_id = #{userId}") Long findMembershipIdNullable(@Param("tenantId") long tenantId, @Param("userId") long userId);

    @Insert("INSERT IGNORE INTO membership_roles(membership_id, role_code) VALUES(#{membershipId}, #{roleCode})")
    void insertRole(@Param("membershipId") long membershipId, @Param("roleCode") String roleCode);
}

