package com.homestay.property.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.BusinessException;
import com.homestay.common.PageResponse;
import com.homestay.property.entity.Room;
import com.homestay.property.entity.RoomType;
import com.homestay.property.mapper.RoomMapper;
import com.homestay.property.mapper.RoomTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final PropertyService propertyService;

    public PageResponse<RoomType> page(long current, long size, Long propertyId,
                                       String keyword, String status) {
        LambdaQueryWrapper<RoomType> query = new LambdaQueryWrapper<RoomType>()
                .eq(propertyId != null, RoomType::getPropertyId, propertyId)
                .like(StringUtils.hasText(keyword), RoomType::getName, keyword)
                .eq(StringUtils.hasText(status), RoomType::getStatus, status)
                .orderByDesc(RoomType::getCreatedAt);
        Page<RoomType> page = roomTypeMapper.selectPage(Page.of(current, size), query);
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public RoomType create(RoomTypeRequest request) {
        propertyService.requireProperty(request.propertyId());
        validate(request);
        RoomType roomType = new RoomType();
        apply(roomType, request);
        roomType.setCreatedAt(LocalDateTime.now());
        roomType.setUpdatedAt(LocalDateTime.now());
        roomTypeMapper.insert(roomType);
        return roomType;
    }

    public RoomType update(Long id, RoomTypeRequest request) {
        RoomType roomType = requireRoomType(id);
        propertyService.requireProperty(request.propertyId());
        validate(request);
        apply(roomType, request);
        roomType.setUpdatedAt(LocalDateTime.now());
        roomTypeMapper.updateById(roomType);
        return roomType;
    }

    @Transactional
    public void delete(Long id) {
        requireRoomType(id);
        if (roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getRoomTypeId, id)) > 0) {
            throw new BusinessException("该房型下仍有房间，不能直接删除");
        }
        roomTypeMapper.deleteById(id);
    }

    private RoomType requireRoomType(Long id) {
        RoomType roomType = roomTypeMapper.selectById(id);
        if (roomType == null) {
            throw new BusinessException("房型不存在");
        }
        return roomType;
    }

    private void validate(RoomTypeRequest request) {
        if (request.capacity() == null || request.capacity() < 1) {
            throw new BusinessException("入住人数至少为 1 人");
        }
        if (request.basePrice() == null || request.basePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("房型价格不能为负数");
        }
        if (!"ACTIVE".equals(request.status()) && !"INACTIVE".equals(request.status())) {
            throw new BusinessException("房型状态不正确");
        }
    }

    private void apply(RoomType roomType, RoomTypeRequest request) {
        roomType.setPropertyId(request.propertyId());
        roomType.setName(request.name());
        roomType.setCapacity(request.capacity());
        roomType.setBasePrice(request.basePrice());
        roomType.setDescription(request.description());
        roomType.setStatus(request.status());
    }

    public record RoomTypeRequest(
            @jakarta.validation.constraints.NotNull(message = "请选择所属门店") Long propertyId,
            @jakarta.validation.constraints.NotBlank(message = "请输入房型名称") String name,
            @jakarta.validation.constraints.NotNull(message = "请输入可入住人数") Integer capacity,
            @jakarta.validation.constraints.NotNull(message = "请输入基础价格") BigDecimal basePrice,
            String description,
            @jakarta.validation.constraints.NotBlank(message = "请选择房型状态") String status
    ) {
    }
}
