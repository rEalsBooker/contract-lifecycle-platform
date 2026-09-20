package com.contractguard.platform.identity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {
    private final BootstrapMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(BootstrapMapper mapper, PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (mapper.countUserByUsername("admin") == 0) { mapper.insertTenant("future-software", "远见软件"); mapper.insertUser("admin", passwordEncoder.encode("ChangeMe123!"), "周岚"); }
        long tenantId = mapper.findTenantIdByCode("future-software");
        long userId = mapper.findUserIdByUsername("admin");
        if (mapper.findMembershipIdNullable(tenantId, userId) == null) mapper.insertMembership(tenantId, userId);
        long membershipId = mapper.findMembershipId(tenantId, userId);
        mapper.insertRole(membershipId, RoleCode.ENTERPRISE_ADMIN.name());
        mapper.insertRole(membershipId, RoleCode.CONTRACT_OWNER.name());
        mapper.insertRole(membershipId, RoleCode.FINANCE.name());
        if (mapper.countUserByUsername("reviewer") == 0) mapper.insertUser("reviewer", passwordEncoder.encode("ChangeMe123!"), "赵宁（模拟审核人）");
        long reviewerUserId = mapper.findUserIdByUsername("reviewer"); if (mapper.findMembershipIdNullable(tenantId, reviewerUserId) == null) mapper.insertMembership(tenantId, reviewerUserId);
        mapper.insertRole(mapper.findMembershipId(tenantId, reviewerUserId), RoleCode.PROJECT_OWNER.name());
    }
}

