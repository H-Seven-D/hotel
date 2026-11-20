package com.homestay.property.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_status_log")
public class RoomStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String roomNo;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private String notes;
    private LocalDateTime createdAt;
}
