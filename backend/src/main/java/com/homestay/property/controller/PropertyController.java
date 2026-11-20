package com.homestay.property.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResponse;
import com.homestay.property.entity.Property;
import com.homestay.property.service.PropertyService;
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
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public ApiResponse<PageResponse<Property>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(propertyService.page(current, size, keyword, type, status));
    }

    @PostMapping
    @AuditAction(module = "门店管理", action = "新增门店")
    public ApiResponse<Property> create(@Valid @RequestBody PropertyService.PropertyRequest request) {
        return ApiResponse.success(propertyService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "门店管理", action = "修改门店")
    public ApiResponse<Property> update(@PathVariable Long id,
                                        @Valid @RequestBody PropertyService.PropertyRequest request) {
        return ApiResponse.success(propertyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "门店管理", action = "删除门店")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        propertyService.delete(id);
        return ApiResponse.success();
    }
}
