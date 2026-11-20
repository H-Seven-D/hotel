package com.homestay.customer.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.PageResponse;
import com.homestay.customer.entity.Customer;
import com.homestay.customer.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ApiResponse<PageResponse<Customer>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(customerService.page(current, size, keyword));
    }

    @PostMapping
    @AuditAction(module = "客户管理", action = "新增客户")
    public ApiResponse<Customer> create(@Valid @RequestBody CustomerService.CustomerRequest request) {
        return ApiResponse.success(customerService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "客户管理", action = "修改客户")
    public ApiResponse<Customer> update(@PathVariable Long id,
                                        @Valid @RequestBody CustomerService.CustomerRequest request) {
        return ApiResponse.success(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "客户管理", action = "删除客户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.success();
    }
}
