package com.homestay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_session")
public class LoginSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String onlineStatus;
    private String ipAddress;
    private String userAgent;
}
