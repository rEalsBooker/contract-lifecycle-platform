package com.contractguard.platform.organization;
import org.apache.ibatis.annotations.*; import java.util.List;
@Mapper public interface OrganizationMapper {
 @Select("SELECT m.id membership_id,m.user_id,u.username,u.display_name,m.status membership_status,GROUP_CONCAT(mr.role_code ORDER BY mr.role_code) role_codes FROM memberships m JOIN app_users u ON u.id=m.user_id LEFT JOIN membership_roles mr ON mr.membership_id=m.id WHERE m.tenant_id=#{tenantId} GROUP BY m.id,m.user_id,u.username,u.display_name,m.status ORDER BY u.display_name") List<OrganizationMemberRow> members(Long tenantId);
 @Select("SELECT COUNT(*) FROM app_users WHERE username=#{username}") int usernameExists(String username);
 @Insert("INSERT INTO app_users(username,password_hash,display_name,status) VALUES(#{row.username},#{passwordHash},#{row.displayName},'ACTIVE')") @Options(useGeneratedKeys=true,keyProperty="row.userId",keyColumn="id") void insertUser(@Param("row")OrganizationMemberRow row,@Param("passwordHash")String passwordHash);
 @Insert("INSERT INTO memberships(tenant_id,user_id,status) VALUES(#{tenantId},#{row.userId},'ACTIVE')") @Options(useGeneratedKeys=true,keyProperty="row.membershipId",keyColumn="id") void insertMembership(@Param("tenantId")Long tenantId,@Param("row")OrganizationMemberRow row);
 @Insert("INSERT INTO membership_roles(membership_id,role_code) VALUES(#{membershipId},#{role})") void insertRole(@Param("membershipId")Long membershipId,@Param("role")String role);
 @Delete("DELETE FROM membership_roles WHERE membership_id=#{membershipId} AND EXISTS(SELECT 1 FROM memberships m WHERE m.id=#{membershipId} AND m.tenant_id=#{tenantId})") int deleteRoles(@Param("tenantId")Long tenantId,@Param("membershipId")Long membershipId);
 @Select("SELECT EXISTS(SELECT 1 FROM memberships WHERE id=#{membershipId} AND tenant_id=#{tenantId})") boolean exists(@Param("tenantId")Long tenantId,@Param("membershipId")Long membershipId);
 @Select("SELECT COUNT(*) FROM fulfillment_tasks WHERE tenant_id=#{tenantId} AND assignee_membership_id=#{membershipId} AND task_status NOT IN ('COMPLETED','CANCELLED')") int unfinishedTasks(@Param("tenantId")Long tenantId,@Param("membershipId")Long membershipId);
 @Update("UPDATE memberships SET status=#{status} WHERE tenant_id=#{tenantId} AND id=#{membershipId}") int updateStatus(@Param("tenantId")Long tenantId,@Param("membershipId")Long membershipId,@Param("status")String status);
 @Select("SELECT COUNT(*) FROM contracts WHERE tenant_id=#{tenantId} AND id=#{contractId}") int contractExists(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId);
 @Insert("INSERT INTO contract_grants(tenant_id,contract_id,membership_id,permission_code) VALUES(#{tenantId},#{contractId},#{membershipId},'READ') ON DUPLICATE KEY UPDATE valid_until=NULL") void grant(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId,@Param("membershipId")Long membershipId);
 @Delete("DELETE FROM contract_grants WHERE tenant_id=#{tenantId} AND contract_id=#{contractId} AND membership_id=#{membershipId} AND permission_code='READ'") int revoke(@Param("tenantId")Long tenantId,@Param("contractId")Long contractId,@Param("membershipId")Long membershipId);
}

