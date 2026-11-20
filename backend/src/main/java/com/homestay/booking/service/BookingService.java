package com.homestay.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.booking.entity.Booking;
import com.homestay.booking.mapper.BookingMapper;
import com.homestay.common.BusinessException;
import com.homestay.common.PageResponse;
import com.homestay.customer.entity.Customer;
import com.homestay.customer.mapper.CustomerMapper;
import com.homestay.property.entity.Property;
import com.homestay.property.entity.Room;
import com.homestay.property.entity.RoomType;
import com.homestay.property.mapper.PropertyMapper;
import com.homestay.property.mapper.RoomMapper;
import com.homestay.property.mapper.RoomTypeMapper;
import com.homestay.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Set<String> ACTIVE_STATUSES =
            Set.of("PENDING", "CONFIRMED", "CHECKED_IN");

    private final BookingMapper bookingMapper;
    private final PropertyMapper propertyMapper;
    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final CustomerMapper customerMapper;

    public PageResponse<Booking> page(long current, long size, String keyword, String status,
                                      Long propertyId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Booking> query = new LambdaQueryWrapper<Booking>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Booking::getBookingNo, keyword)
                        .or()
                        .like(Booking::getRemark, keyword))
                .eq(StringUtils.hasText(status), Booking::getStatus, status)
                .eq(propertyId != null, Booking::getPropertyId, propertyId)
                .ge(startDate != null, Booking::getCheckInDate, startDate)
                .le(endDate != null, Booking::getCheckOutDate, endDate)
                .orderByDesc(Booking::getCreatedAt);
        Page<Booking> page = bookingMapper.selectPage(Page.of(current, size), query);
        enrich(page.getRecords());
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public Booking create(BookingRequest request, AuthUser operator) {
        ValidatedBooking validated = validateRequest(request, null);
        Booking booking = new Booking();
        apply(booking, request);
        booking.setBookingNo(generateBookingNo());
        booking.setTotalAmount(calculateAmount(validated.roomType(), request));
        booking.setStatus("CONFIRMED");
        booking.setCreatedBy(operator.id());
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.insert(booking);
        if ("AVAILABLE".equals(validated.room().getStatus())) {
            validated.room().setStatus("RESERVED");
            validated.room().setUpdatedAt(LocalDateTime.now());
            roomMapper.updateById(validated.room());
        }
        return booking;
    }

    @Transactional
    public Booking update(Long id, BookingRequest request) {
        Booking booking = requireBooking(id);
        if (!"PENDING".equals(booking.getStatus()) && !"CONFIRMED".equals(booking.getStatus())) {
            throw new BusinessException("当前预订状态不允许修改");
        }
        ValidatedBooking validated = validateRequest(request, id);
        apply(booking, request);
        booking.setTotalAmount(calculateAmount(validated.roomType(), request));
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        return booking;
    }

    @Transactional
    public Booking checkIn(Long id) {
        Booking booking = requireBooking(id);
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new BusinessException("只有已确认的预订可以办理入住");
        }
        booking.setStatus("CHECKED_IN");
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        Room room = roomMapper.selectById(booking.getRoomId());
        if (room != null) {
            room.setStatus("OCCUPIED");
            room.setUpdatedAt(LocalDateTime.now());
            roomMapper.updateById(room);
        }
        return booking;
    }

    @Transactional
    public Booking checkOut(Long id) {
        Booking booking = requireBooking(id);
        if (!"CHECKED_IN".equals(booking.getStatus())) {
            throw new BusinessException("只有已入住的预订可以办理退房");
        }
        booking.setStatus("COMPLETED");
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        Room room = roomMapper.selectById(booking.getRoomId());
        if (room != null) {
            room.setStatus("CLEANING");
            room.setUpdatedAt(LocalDateTime.now());
            roomMapper.updateById(room);
        }
        return booking;
    }

    @Transactional
    public Booking cancel(Long id) {
        Booking booking = requireBooking(id);
        if (!"PENDING".equals(booking.getStatus()) && !"CONFIRMED".equals(booking.getStatus())) {
            throw new BusinessException("当前预订状态不允许取消");
        }
        booking.setStatus("CANCELLED");
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        Room room = roomMapper.selectById(booking.getRoomId());
        if (room != null && "RESERVED".equals(room.getStatus())) {
            room.setStatus("AVAILABLE");
            room.setUpdatedAt(LocalDateTime.now());
            roomMapper.updateById(room);
        }
        return booking;
    }

    public Booking requireBooking(Long id) {
        Booking booking = bookingMapper.selectById(id);
        if (booking == null) {
            throw new BusinessException("预订记录不存在");
        }
        return booking;
    }

    private ValidatedBooking validateRequest(BookingRequest request, Long excludeId) {
        if (request.checkInDate() == null || request.checkOutDate() == null) {
            throw new BusinessException("请选择入住和退房日期");
        }
        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw new BusinessException("退房日期必须晚于入住日期");
        }
        if (request.checkInDate().isBefore(LocalDate.now())) {
            throw new BusinessException("入住日期不能早于今天");
        }
        Property property = propertyMapper.selectById(request.propertyId());
        if (property == null || !"ACTIVE".equals(property.getStatus())) {
            throw new BusinessException("所选门店不可预订");
        }
        Room room = roomMapper.selectById(request.roomId());
        if (room == null || !request.propertyId().equals(room.getPropertyId())) {
            throw new BusinessException("所选房间不属于该门店");
        }
        if ("MAINTENANCE".equals(room.getStatus())) {
            throw new BusinessException("该房间正在维护，不能预订");
        }
        RoomType roomType = roomTypeMapper.selectById(room.getRoomTypeId());
        if (roomType == null) {
            throw new BusinessException("房间房型信息不存在");
        }
        if (request.guestCount() == null || request.guestCount() < 1
                || request.guestCount() > roomType.getCapacity()) {
            throw new BusinessException("入住人数超过房型可容纳人数");
        }
        Customer customer = customerMapper.selectById(request.customerId());
        if (customer == null) {
            throw new BusinessException("请选择已登记客户");
        }
        long overlap = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getRoomId, request.roomId())
                .in(Booking::getStatus, ACTIVE_STATUSES)
                .ne(excludeId != null, Booking::getId, excludeId)
                .lt(Booking::getCheckInDate, request.checkOutDate())
                .gt(Booking::getCheckOutDate, request.checkInDate()));
        if (overlap > 0) {
            throw new BusinessException("该房间在所选日期内已被预订");
        }
        return new ValidatedBooking(property, room, roomType, customer);
    }

    private BigDecimal calculateAmount(RoomType roomType, BookingRequest request) {
        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        return roomType.getBasePrice().multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void apply(Booking booking, BookingRequest request) {
        booking.setPropertyId(request.propertyId());
        booking.setRoomId(request.roomId());
        booking.setCustomerId(request.customerId());
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setGuestCount(request.guestCount());
        booking.setRemark(request.remark());
    }

    private String generateBookingNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "BK" + date + random;
    }

    private void enrich(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return;
        }
        Set<Long> propertyIds = bookings.stream().map(Booking::getPropertyId).collect(Collectors.toSet());
        Set<Long> roomIds = bookings.stream().map(Booking::getRoomId).collect(Collectors.toSet());
        Set<Long> customerIds = bookings.stream().map(Booking::getCustomerId).collect(Collectors.toSet());
        Map<Long, Property> properties = propertyMapper.selectBatchIds(propertyIds).stream()
                .collect(Collectors.toMap(Property::getId, Function.identity()));
        Map<Long, Room> rooms = roomMapper.selectBatchIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, Function.identity()));
        Map<Long, Customer> customers = customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        bookings.forEach(booking -> {
            Property property = properties.get(booking.getPropertyId());
            Room room = rooms.get(booking.getRoomId());
            Customer customer = customers.get(booking.getCustomerId());
            booking.setPropertyName(property == null ? "" : property.getName());
            booking.setRoomNo(room == null ? "" : room.getRoomNo());
            booking.setCustomerName(customer == null ? "" : customer.getName());
            booking.setCustomerPhone(customer == null ? "" : customer.getPhone());
        });
    }

    private record ValidatedBooking(Property property, Room room, RoomType roomType, Customer customer) {
    }

    public record BookingRequest(
            @jakarta.validation.constraints.NotNull(message = "请选择门店") Long propertyId,
            @jakarta.validation.constraints.NotNull(message = "请选择房间") Long roomId,
            @jakarta.validation.constraints.NotNull(message = "请选择客户") Long customerId,
            @jakarta.validation.constraints.NotNull(message = "请选择入住日期") LocalDate checkInDate,
            @jakarta.validation.constraints.NotNull(message = "请选择退房日期") LocalDate checkOutDate,
            @jakarta.validation.constraints.NotNull(message = "请输入入住人数") Integer guestCount,
            String remark
    ) {
    }
}
