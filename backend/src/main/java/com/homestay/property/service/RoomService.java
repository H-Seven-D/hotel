package com.homestay.property.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.BusinessException;
import com.homestay.common.PageResponse;
import com.homestay.booking.entity.Booking;
import com.homestay.booking.mapper.BookingMapper;
import com.homestay.property.entity.Property;
import com.homestay.property.entity.Room;
import com.homestay.property.entity.RoomStatusLog;
import com.homestay.property.entity.RoomType;
import com.homestay.property.mapper.PropertyMapper;
import com.homestay.property.mapper.RoomMapper;
import com.homestay.property.mapper.RoomStatusLogMapper;
import com.homestay.property.mapper.RoomTypeMapper;
import com.homestay.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final Set<String> ROOM_STATUSES =
            Set.of("AVAILABLE", "RESERVED", "OCCUPIED", "CLEANING", "MAINTENANCE");

    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final PropertyMapper propertyMapper;
    private final RoomStatusLogMapper statusLogMapper;
    private final BookingMapper bookingMapper;

    public PageResponse<Room> page(long current, long size, Long propertyId,
                                   String status, String keyword) {
        LambdaQueryWrapper<Room> query = new LambdaQueryWrapper<Room>()
                .eq(propertyId != null, Room::getPropertyId, propertyId)
                .eq(StringUtils.hasText(status), Room::getStatus, status)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Room::getRoomNo, keyword)
                        .or()
                        .like(Room::getNotes, keyword))
                .orderByAsc(Room::getPropertyId)
                .orderByAsc(Room::getRoomNo);
        Page<Room> page = roomMapper.selectPage(Page.of(current, size), query);
        enrich(page.getRecords());
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<Room> options(Long propertyId) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(propertyId != null, Room::getPropertyId, propertyId)
                .ne(Room::getStatus, "MAINTENANCE")
                .orderByAsc(Room::getRoomNo));
        enrich(rooms);
        return rooms;
    }

    public Room create(RoomRequest request) {
        validateRequest(request, null);
        Room room = new Room();
        apply(room, request);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.insert(room);
        addStatusLog(room, null, room.getStatus(), null, "创建房间");
        return room;
    }

    public Room update(Long id, RoomRequest request) {
        Room room = requireRoom(id);
        validateRequest(request, id);
        apply(room, request);
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.updateById(room);
        return room;
    }

    @Transactional
    public void delete(Long id) {
        requireRoom(id);
        if (bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getRoomId, id)) > 0) {
            throw new BusinessException("该房间已有预订记录，不能删除");
        }
        roomMapper.deleteById(id);
    }

    @Transactional
    public Room changeStatus(Long id, String status, String notes, AuthUser operator) {
        if (!ROOM_STATUSES.contains(status)) {
            throw new BusinessException("房间状态不正确");
        }
        Room room = requireRoom(id);
        String before = room.getStatus();
        room.setStatus(status);
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.updateById(room);
        addStatusLog(room, before, status, operator, notes);
        return room;
    }

    public List<RoomStatusLog> statusLogs(Long roomId) {
        return statusLogMapper.selectList(new LambdaQueryWrapper<RoomStatusLog>()
                .eq(RoomStatusLog::getRoomId, roomId)
                .orderByDesc(RoomStatusLog::getCreatedAt)
                .last("LIMIT 20"));
    }

    public Room requireRoom(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }
        return room;
    }

    private void validateRequest(RoomRequest request, Long excludeId) {
        Property property = propertyMapper.selectById(request.propertyId());
        if (property == null) {
            throw new BusinessException("所属门店不存在");
        }
        RoomType roomType = roomTypeMapper.selectById(request.roomTypeId());
        if (roomType == null || !request.propertyId().equals(roomType.getPropertyId())) {
            throw new BusinessException("所选房型不属于该门店");
        }
        long duplicate = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getPropertyId, request.propertyId())
                .eq(Room::getRoomNo, request.roomNo())
                .ne(excludeId != null, Room::getId, excludeId));
        if (duplicate > 0) {
            throw new BusinessException("该门店已存在相同房号");
        }
        if (!ROOM_STATUSES.contains(request.status())) {
            throw new BusinessException("房间状态不正确");
        }
    }

    private void apply(Room room, RoomRequest request) {
        room.setPropertyId(request.propertyId());
        room.setRoomTypeId(request.roomTypeId());
        room.setRoomNo(request.roomNo());
        room.setFloor(request.floor());
        room.setStatus(request.status());
        room.setNotes(request.notes());
    }

    private void enrich(List<Room> rooms) {
        if (rooms.isEmpty()) {
            return;
        }
        Set<Long> propertyIds = rooms.stream().map(Room::getPropertyId).collect(Collectors.toSet());
        Set<Long> roomTypeIds = rooms.stream().map(Room::getRoomTypeId).collect(Collectors.toSet());
        Map<Long, Property> properties = propertyMapper.selectBatchIds(propertyIds).stream()
                .collect(Collectors.toMap(Property::getId, Function.identity()));
        Map<Long, RoomType> roomTypes = roomTypeMapper.selectBatchIds(roomTypeIds).stream()
                .collect(Collectors.toMap(RoomType::getId, Function.identity()));
        rooms.forEach(room -> {
            Property property = properties.get(room.getPropertyId());
            RoomType roomType = roomTypes.get(room.getRoomTypeId());
            room.setPropertyName(property == null ? "" : property.getName());
            room.setRoomTypeName(roomType == null ? "" : roomType.getName());
        });
    }

    private void addStatusLog(Room room, String before, String after, AuthUser operator, String notes) {
        RoomStatusLog log = new RoomStatusLog();
        log.setRoomId(room.getId());
        log.setRoomNo(room.getRoomNo());
        log.setBeforeStatus(before);
        log.setAfterStatus(after);
        log.setOperatorId(operator == null ? null : operator.id());
        log.setOperatorName(operator == null ? "系统" : operator.realName());
        log.setNotes(notes);
        log.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(log);
    }

    public record RoomRequest(
            @jakarta.validation.constraints.NotNull(message = "请选择所属门店") Long propertyId,
            @jakarta.validation.constraints.NotNull(message = "请选择房型") Long roomTypeId,
            @jakarta.validation.constraints.NotBlank(message = "请输入房号") String roomNo,
            Integer floor,
            @jakarta.validation.constraints.NotBlank(message = "请选择房间状态") String status,
            String notes
    ) {
    }

    public record StatusRequest(
            @jakarta.validation.constraints.NotBlank(message = "请选择房间状态") String status,
            String notes
    ) {
    }
}
