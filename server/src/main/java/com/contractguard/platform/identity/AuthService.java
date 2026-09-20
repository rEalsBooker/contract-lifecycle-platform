package com.contractguard.platform.identity;

import com.contractguard.platform.audit.AuditService;
import com.contractguard.platform.common.ApiException;
import com.contractguard.platform.security.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final AuditService auditService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService tokenService,
                       AuditService auditService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = userMapper.findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_LOGIN_FAILED", "账号或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "账号已停用，请联系企业管理员");
        }
        auditService.record(null, user.getId(), null, "LOGIN_SUCCEEDED", "USER", user.getId().toString());
        return new LoginResponse(tokenService.createIdentityToken(user.getId(), user.getUsername()),
                "Bearer", tokenService.expiresAt(), user.getDisplayName());
    }
}

