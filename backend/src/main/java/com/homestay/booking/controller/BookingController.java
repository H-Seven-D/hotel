package com.homestay.booking.controller;

import com.homestay.booking.entity.Booking;
import com.homestay.booking.service.BookingService;
import com.homestay.common.ApiResponse;
import com.homestay.common.PageResponse;
import com.homestay.security.SecurityUtils;
import com.homestay.system.log.AuditAction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ApiResponse<PageResponse<Booking>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(bookingService.page(
                current, size, keyword, status, propertyId, startDate, endDate));
    }

    @PostMapping
    @AuditAction(module = "预订管理", action = "新增预订")
    public ApiResponse<Booking> create(@Valid @RequestBody BookingService.BookingRequest request) {
        return ApiResponse.success(bookingService.create(request, SecurityUtils.currentUser()));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "预订管理", action = "修改预订")
    public ApiResponse<Booking> update(@PathVariable Long id,
                                       @Valid @RequestBody BookingService.BookingRequest request) {
        return ApiResponse.success(bookingService.update(id, request));
    }

    @PostMapping("/{id}/check-in")
    @AuditAction(module = "入住管理", action = "办理入住")
    public ApiResponse<Booking> checkIn(@PathVariable Long id) {
        return ApiResponse.success(bookingService.checkIn(id));
    }

    @PostMapping("/{id}/check-out")
    @AuditAction(module = "入住管理", action = "办理退房")
    public ApiResponse<Booking> checkOut(@PathVariable Long id) {
        return ApiResponse.success(bookingService.checkOut(id));
    }

    @PostMapping("/{id}/cancel")
    @AuditAction(module = "预订管理", action = "取消预订")
    public ApiResponse<Booking> cancel(@PathVariable Long id) {
        return ApiResponse.success(bookingService.cancel(id));
    }
}
