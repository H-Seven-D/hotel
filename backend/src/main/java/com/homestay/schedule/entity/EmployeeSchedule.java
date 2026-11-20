package com.homestay.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("employee_schedule")
public class EmployeeSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long propertyId;
    private LocalDate workDate;
    private String shift;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String propertyName;
}
