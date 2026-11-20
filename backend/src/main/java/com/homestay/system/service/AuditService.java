package com.homestay.system.service;

import com.homestay.security.AuthUser;
import com.homestay.system.entity.OperationLog;
import com.homestay.system.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final OperationLogMapper operationLogMapper;

    public void record(AuthUser user, String module, String action,
                       String method, String uri, String ip, String result) {
        OperationLog log = new OperationLog();
        if (user != null) {
            log.setUserId(user.id());
            log.setUsername(user.username());
            log.setRealName(user.realName());
        } else {
            log.setUsername("anonymous");
            log.setRealName("匿名用户");
        }
        log.setModule(module);
        log.setAction(action);
        log.setRequestMethod(method);
        log.setRequestUri(uri);
        log.setIpAddress(ip);
        log.setDetail(method + " " + uri);
        log.setResult(result);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
