package com.homestay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String module;
    private String action;
    private String requestMethod;
    private String requestUri;
    private String ipAddress;
    private String detail;
    private String result;
    private LocalDateTime createdAt;
}
