package com.homestay.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homestay.booking.entity.Booking;
import com.homestay.booking.mapper.BookingMapper;
import com.homestay.booking.service.BookingService;
import com.homestay.property.entity.Property;
import com.homestay.property.entity.Room;
import com.homestay.property.mapper.PropertyMapper;
import com.homestay.property.mapper.RoomMapper;
import com.homestay.system.entity.LoginSession;
import com.homestay.system.entity.SysUser;
import com.homestay.system.mapper.LoginSessionMapper;
import com.homestay.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PropertyMapper propertyMapper;
    private final RoomMapper roomMapper;
    private final BookingMapper bookingMapper;
    private final SysUserMapper userMapper;
    private final LoginSessionMapper loginSessionMapper;
    private final BookingService bookingService;

    public Map<String, Object> overview() {
        long activeProperties = propertyMapper.selectCount(new LambdaQueryWrapper<Property>()
                .eq(Property::getStatus, "ACTIVE"));
        long totalRooms = roomMapper.selectCount(null);
        long availableRooms = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getStatus, "AVAILABLE"));
        long occupiedRooms = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getStatus, "OCCUPIED"));
        long reservedRooms = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getStatus, "RESERVED"));
        long activeEmployees = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1));
        long onlineEmployees = loginSessionMapper.selectCount(new LambdaQueryWrapper<LoginSession>()
                .eq(LoginSession::getOnlineStatus, "ONLINE"));
        long todayCheckIns = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getCheckInDate, LocalDate.now())
                .in(Booking::getStatus, List.of("CONFIRMED", "CHECKED_IN")));
        long todayCheckOuts = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getCheckOutDate, LocalDate.now())
                .in(Booking::getStatus, List.of("CHECKED_IN", "COMPLETED")));
        BigDecimal monthRevenue = monthRevenue();

        BigDecimal occupancyRate = totalRooms == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(occupiedRooms + reservedRooms)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("activeProperties", activeProperties);
        summary.put("totalRooms", totalRooms);
        summary.put("availableRooms", availableRooms);
        summary.put("todayCheckIns", todayCheckIns);
        summary.put("todayCheckOuts", todayCheckOuts);
        summary.put("activeEmployees", activeEmployees);
        summary.put("onlineEmployees", onlineEmployees);
        summary.put("monthRevenue", monthRevenue);
        summary.put("occupancyRate", occupancyRate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("roomStatus", roomStatus());
        result.put("recentBookings", bookingService.page(1, 6, null, null, null, null, null).records());
        result.put("trend", bookingTrend());
        return result;
    }

    private BigDecimal monthRevenue() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        QueryWrapper<Booking> query = new QueryWrapper<Booking>()
                .select("COALESCE(SUM(total_amount), 0) AS total")
                .ge("created_at", monthStart)
                .ne("status", "CANCELLED");
        List<Object> values = bookingMapper.selectObjs(query);
        if (values.isEmpty() || values.get(0) == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(values.get(0).toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> roomStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String status : List.of("AVAILABLE", "RESERVED", "OCCUPIED", "CLEANING", "MAINTENANCE")) {
            long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                    .eq(Room::getStatus, status));
            result.add(Map.of("status", status, "count", count));
        }
        return result;
    }

    private List<Map<String, Object>> bookingTrend() {
        LocalDate start = LocalDate.now().minusDays(13);
        QueryWrapper<Booking> query = new QueryWrapper<Booking>()
                .select("DATE(created_at) AS day", "COUNT(*) AS booking_count",
                        "COALESCE(SUM(total_amount), 0) AS revenue")
                .ge("created_at", start.atStartOfDay())
                .groupBy("DATE(created_at)")
                .orderByAsc("DATE(created_at)");
        List<Map<String, Object>> rows = bookingMapper.selectMaps(query);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", String.valueOf(row.get("day")));
            item.put("count", row.get("booking_count"));
            item.put("revenue", row.get("revenue"));
            result.add(item);
        }
        return result;
    }
}
