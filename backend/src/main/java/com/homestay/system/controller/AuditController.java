package com.homestay.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.ApiResponse;
import com.homestay.common.PageResponse;
import com.homestay.system.entity.LoginSession;
import com.homestay.system.entity.OperationLog;
import com.homestay.system.mapper.LoginSessionMapper;
import com.homestay.system.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final LoginSessionMapper loginSessionMapper;
    private final OperationLogMapper operationLogMapper;

    @GetMapping("/sessions")
    public ApiResponse<PageResponse<LoginSession>> sessions(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<LoginSession> query = new LambdaQueryWrapper<LoginSession>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(LoginSession::getUsername, keyword)
                        .or()
                        .like(LoginSession::getRealName, keyword))
                .eq(StringUtils.hasText(status), LoginSession::getOnlineStatus, status)
                .orderByDesc(LoginSession::getLoginTime);
        Page<LoginSession> page = loginSessionMapper.selectPage(Page.of(current, size), query);
        return ApiResponse.success(new PageResponse<>(page.getRecords(), page.getTotal(),
                page.getCurrent(), page.getSize()));
    }

    @GetMapping("/operations")
    public ApiResponse<PageResponse<OperationLog>> operations(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String result) {
        LambdaQueryWrapper<OperationLog> query = new LambdaQueryWrapper<OperationLog>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(OperationLog::getUsername, keyword)
                        .or()
                        .like(OperationLog::getRealName, keyword)
                        .or()
                        .like(OperationLog::getAction, keyword))
                .eq(StringUtils.hasText(module), OperationLog::getModule, module)
                .eq(StringUtils.hasText(result), OperationLog::getResult, result)
                .orderByDesc(OperationLog::getCreatedAt);
        Page<OperationLog> page = operationLogMapper.selectPage(Page.of(current, size), query);
        return ApiResponse.success(new PageResponse<>(page.getRecords(), page.getTotal(),
                page.getCurrent(), page.getSize()));
    }
}
