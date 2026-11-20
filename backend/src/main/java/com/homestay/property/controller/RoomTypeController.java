package com.homestay.property.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResponse;
import com.homestay.property.entity.RoomType;
import com.homestay.property.service.RoomTypeService;
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

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping
    public ApiResponse<PageResponse<RoomType>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(roomTypeService.page(current, size, propertyId, keyword, status));
    }

    @PostMapping
    @AuditAction(module = "房型管理", action = "新增房型")
    public ApiResponse<RoomType> create(@Valid @RequestBody RoomTypeService.RoomTypeRequest request) {
        return ApiResponse.success(roomTypeService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "房型管理", action = "修改房型")
    public ApiResponse<RoomType> update(@PathVariable Long id,
                                        @Valid @RequestBody RoomTypeService.RoomTypeRequest request) {
        return ApiResponse.success(roomTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "房型管理", action = "删除房型")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ApiResponse.success();
    }
}
