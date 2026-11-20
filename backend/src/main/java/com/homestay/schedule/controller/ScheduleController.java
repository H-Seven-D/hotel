package com.homestay.schedule.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResponse;
import com.homestay.schedule.entity.EmployeeSchedule;
import com.homestay.schedule.service.ScheduleService;
import com.homestay.system.log.AuditAction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public ApiResponse<PageResponse<EmployeeSchedule>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(scheduleService.page(
                current, size, userId, propertyId, startDate, endDate));
    }

    @GetMapping("/employees")
    public ApiResponse<List<ScheduleService.EmployeeOption>> employees() {
        return ApiResponse.success(scheduleService.employeeOptions());
    }

    @PostMapping
    @AuditAction(module = "排班调度", action = "新增排班")
    public ApiResponse<EmployeeSchedule> create(@Valid @RequestBody ScheduleService.ScheduleRequest request) {
        return ApiResponse.success(scheduleService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "排班调度", action = "修改排班")
    public ApiResponse<EmployeeSchedule> update(@PathVariable Long id,
                                                @Valid @RequestBody ScheduleService.ScheduleRequest request) {
        return ApiResponse.success(scheduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "排班调度", action = "删除排班")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ApiResponse.success();
    }
}
