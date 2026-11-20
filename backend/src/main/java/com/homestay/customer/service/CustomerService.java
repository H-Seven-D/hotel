package com.homestay.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.booking.entity.Booking;
import com.homestay.booking.mapper.BookingMapper;
import com.homestay.common.BusinessException;
import com.homestay.common.PageResponse;
import com.homestay.customer.entity.Customer;
import com.homestay.customer.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final BookingMapper bookingMapper;

    public PageResponse<Customer> page(long current, long size, String keyword) {
        LambdaQueryWrapper<Customer> query = new LambdaQueryWrapper<Customer>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Customer::getName, keyword)
                        .or()
                        .like(Customer::getPhone, keyword)
                        .or()
                        .like(Customer::getIdCard, keyword))
                .orderByDesc(Customer::getCreatedAt);
        Page<Customer> page = customerMapper.selectPage(Page.of(current, size), query);
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public Customer create(CustomerRequest request) {
        checkPhone(request.phone(), null);
        Customer customer = new Customer();
        apply(customer, request);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customerMapper.insert(customer);
        return customer;
    }

    public Customer update(Long id, CustomerRequest request) {
        Customer customer = requireCustomer(id);
        checkPhone(request.phone(), id);
        apply(customer, request);
        customer.setUpdatedAt(LocalDateTime.now());
        customerMapper.updateById(customer);
        return customer;
    }

    public void delete(Long id) {
        requireCustomer(id);
        if (bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getCustomerId, id)) > 0) {
            throw new BusinessException("该客户已有预订记录，不能删除");
        }
        customerMapper.deleteById(id);
    }

    public Customer requireCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        return customer;
    }

    private void checkPhone(String phone, Long excludeId) {
        LambdaQueryWrapper<Customer> query = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone)
                .ne(excludeId != null, Customer::getId, excludeId);
        if (customerMapper.selectCount(query) > 0) {
            throw new BusinessException("该手机号已登记客户");
        }
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setIdCard(request.idCard());
        customer.setEmail(request.email());
        customer.setRemark(request.remark());
    }

    public record CustomerRequest(
            @jakarta.validation.constraints.NotBlank(message = "请输入客户姓名") String name,
            @jakarta.validation.constraints.NotBlank(message = "请输入客户手机号") String phone,
            String idCard,
            String email,
            String remark
    ) {
    }
}
