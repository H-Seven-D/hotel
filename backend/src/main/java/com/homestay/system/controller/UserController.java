package com.homestay.system.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResponse;
import com.homestay.security.AuthUser;
import com.homestay.security.SecurityUtils;
import com.homestay.system.entity.SysUser;
import com.homestay.system.log.AuditAction;
import com.homestay.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResponse<SysUser>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(userService.page(current, size, keyword, role, status));
    }

    @PostMapping
    @AuditAction(module = "员工管理", action = "新增员工")
    public ApiResponse<SysUser> create(@Valid @RequestBody UserService.CreateUserRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "员工管理", action = "修改员工")
    public ApiResponse<SysUser> update(@PathVariable Long id,
                                       @Valid @RequestBody UserService.UpdateUserRequest request) {
        return ApiResponse.success(userService.update(id, request, SecurityUtils.currentUser()));
    }

    @PutMapping("/{id}/password")
    @AuditAction(module = "员工管理", action = "重置员工密码")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody UserService.ResetPasswordRequest request) {
        userService.resetPassword(id, request.password());
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "员工管理", action = "删除员工")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AuthUser operator = SecurityUtils.currentUser();
        userService.delete(id, operator);
        return ApiResponse.success();
    }
}
