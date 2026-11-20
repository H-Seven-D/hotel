package com.homestay.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homestay.common.ApiResponse;
import com.homestay.common.BusinessException;
import com.homestay.security.AuthUser;
import com.homestay.security.JwtService;
import com.homestay.security.SecurityUtils;
import com.homestay.system.entity.LoginSession;
import com.homestay.system.entity.SysUser;
import com.homestay.system.mapper.LoginSessionMapper;
import com.homestay.system.mapper.SysUserMapper;
import com.homestay.system.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final LoginSessionMapper loginSessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest servletRequest) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())
                .last("LIMIT 1"));
        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(401, "账号或密码不正确，或账号已停用");
        }

        LoginSession session = new LoginSession();
        session.setUserId(user.getId());
        session.setUsername(user.getUsername());
        session.setRealName(user.getRealName());
        session.setLoginTime(LocalDateTime.now());
        session.setOnlineStatus("ONLINE");
        session.setIpAddress(clientIp(servletRequest));
        session.setUserAgent(servletRequest.getHeader("User-Agent"));
        loginSessionMapper.insert(session);

        AuthUser authUser = new AuthUser(
                user.getId(), user.getUsername(), user.getRealName(), user.getRole(), session.getId());
        auditService.record(authUser, "登录认证", "登录系统",
                servletRequest.getMethod(), servletRequest.getRequestURI(),
                session.getIpAddress(), "SUCCESS");
        return ApiResponse.success(new LoginResponse(jwtService.generateToken(authUser), authUser));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest servletRequest) {
        AuthUser user = SecurityUtils.currentUser();
        LoginSession session = loginSessionMapper.selectById(user.sessionId());
        if (session != null) {
            session.setOnlineStatus("OFFLINE");
            session.setLogoutTime(LocalDateTime.now());
            loginSessionMapper.updateById(session);
        }
        auditService.record(user, "登录认证", "退出系统",
                servletRequest.getMethod(), servletRequest.getRequestURI(),
                clientIp(servletRequest), "SUCCESS");
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<AuthUser> me() {
        return ApiResponse.success(SecurityUtils.currentUser());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record LoginRequest(
            @NotBlank(message = "请输入登录账号") String username,
            @NotBlank(message = "请输入登录密码") String password
    ) {
    }

    public record LoginResponse(String token, AuthUser user) {
    }
}
