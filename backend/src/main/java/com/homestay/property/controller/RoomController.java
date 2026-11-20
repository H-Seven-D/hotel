package com.homestay.property.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResponse;
import com.homestay.property.entity.Room;
import com.homestay.property.entity.RoomStatusLog;
import com.homestay.property.service.RoomService;
import com.homestay.security.SecurityUtils;
import com.homestay.system.log.AuditAction;
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

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ApiResponse<PageResponse<Room>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(roomService.page(current, size, propertyId, status, keyword));
    }

    @GetMapping("/options")
    public ApiResponse<List<Room>> options(@RequestParam(required = false) Long propertyId) {
        return ApiResponse.success(roomService.options(propertyId));
    }

    @PostMapping
    @AuditAction(module = "房间管理", action = "新增房间")
    public ApiResponse<Room> create(@Valid @RequestBody RoomService.RoomRequest request) {
        return ApiResponse.success(roomService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "房间管理", action = "修改房间")
    public ApiResponse<Room> update(@PathVariable Long id,
                                    @Valid @RequestBody RoomService.RoomRequest request) {
        return ApiResponse.success(roomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "房间管理", action = "删除房间")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    @AuditAction(module = "房间调度", action = "变更房间状态")
    public ApiResponse<Room> changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody RoomService.StatusRequest request) {
        return ApiResponse.success(roomService.changeStatus(
                id, request.status(), request.notes(), SecurityUtils.currentUser()));
    }

    @GetMapping("/{id}/status-logs")
    public ApiResponse<List<RoomStatusLog>> statusLogs(@PathVariable Long id) {
        return ApiResponse.success(roomService.statusLogs(id));
    }
}
