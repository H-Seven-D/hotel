package com.homestay.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.BusinessException;
import com.homestay.common.PageResponse;
import com.homestay.security.AuthUser;
import com.homestay.system.entity.SysUser;
import com.homestay.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<SysUser> page(long current, long size, String keyword, String role, Integer status) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getRealName, keyword)
                        .or()
                        .like(SysUser::getPhone, keyword))
                .eq(StringUtils.hasText(role), SysUser::getRole, role)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> page = userMapper.selectPage(Page.of(current, size), query);
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public SysUser create(CreateUserRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())) > 0) {
            throw new BusinessException("登录账号已存在");
        }
        validateRole(request.role());
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    @Transactional
    public SysUser update(Long id, UpdateUserRequest request, AuthUser operator) {
        SysUser user = requireUser(id);
        validateRole(request.role());
        if (id.equals(operator.id()) && request.status() == 0) {
            throw new BusinessException("不能停用当前登录账号");
        }
        if ("ADMIN".equals(user.getRole())
                && (!"ADMIN".equals(request.role()) || request.status() == 0)
                && countActiveAdmins() <= 1) {
            throw new BusinessException("系统必须至少保留一个可用管理员");
        }
        user.setRealName(request.realName());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setStatus(request.status());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    @Transactional
    public void resetPassword(Long id, String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            throw new BusinessException("新密码至少需要 8 个字符");
        }
        SysUser user = requireUser(id);
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id, AuthUser operator) {
        SysUser user = requireUser(id);
        if (id.equals(operator.id())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        if ("ADMIN".equals(user.getRole()) && countActiveAdmins() <= 1) {
            throw new BusinessException("系统必须至少保留一个管理员");
        }
        userMapper.deleteById(id);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("员工账号不存在");
        }
        return user;
    }

    private long countActiveAdmins() {
        return userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "ADMIN")
                .eq(SysUser::getStatus, 1));
    }

    private void validateRole(String role) {
        if (!"ADMIN".equals(role) && !"EMPLOYEE".equals(role)) {
            throw new BusinessException("员工角色不正确");
        }
    }

    public record CreateUserRequest(
            @jakarta.validation.constraints.NotBlank(message = "请输入登录账号") String username,
            @jakarta.validation.constraints.Size(min = 8, message = "密码至少需要 8 个字符") String password,
            @jakarta.validation.constraints.NotBlank(message = "请输入员工姓名") String realName,
            String phone,
            @jakarta.validation.constraints.NotBlank(message = "请选择员工角色") String role
    ) {
    }

    public record UpdateUserRequest(
            @jakarta.validation.constraints.NotBlank(message = "请输入员工姓名") String realName,
            String phone,
            @jakarta.validation.constraints.NotBlank(message = "请选择员工角色") String role,
            @jakarta.validation.constraints.NotNull(message = "请选择账号状态") Integer status
    ) {
    }

    public record ResetPasswordRequest(
            @jakarta.validation.constraints.Size(min = 8, message = "新密码至少需要 8 个字符") String password
    ) {
    }
}
