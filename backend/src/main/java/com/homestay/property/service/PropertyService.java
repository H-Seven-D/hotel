package com.homestay.property.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.BusinessException;
import com.homestay.common.PageResponse;
import com.homestay.property.entity.Property;
import com.homestay.property.entity.Room;
import com.homestay.property.entity.RoomType;
import com.homestay.property.mapper.PropertyMapper;
import com.homestay.property.mapper.RoomMapper;
import com.homestay.property.mapper.RoomTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyMapper propertyMapper;
    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;

    public PageResponse<Property> page(long current, long size, String keyword,
                                       String type, String status) {
        LambdaQueryWrapper<Property> query = new LambdaQueryWrapper<Property>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Property::getName, keyword)
                        .or()
                        .like(Property::getAddress, keyword))
                .eq(StringUtils.hasText(type), Property::getType, type)
                .eq(StringUtils.hasText(status), Property::getStatus, status)
                .orderByDesc(Property::getCreatedAt);
        Page<Property> page = propertyMapper.selectPage(Page.of(current, size), query);
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public Property create(PropertyRequest request) {
        validate(request.type(), request.status());
        Property property = new Property();
        apply(property, request);
        property.setCreatedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());
        propertyMapper.insert(property);
        return property;
    }

    public Property update(Long id, PropertyRequest request) {
        Property property = requireProperty(id);
        validate(request.type(), request.status());
        apply(property, request);
        property.setUpdatedAt(LocalDateTime.now());
        propertyMapper.updateById(property);
        return property;
    }

    @Transactional
    public void delete(Long id) {
        requireProperty(id);
        if (roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getPropertyId, id)) > 0
                || roomTypeMapper.selectCount(new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getPropertyId, id)) > 0) {
            throw new BusinessException("该门店仍关联房型或房间，请先清理房型房间");
        }
        propertyMapper.deleteById(id);
    }

    public Property requireProperty(Long id) {
        Property property = propertyMapper.selectById(id);
        if (property == null) {
            throw new BusinessException("民宿或酒店不存在");
        }
        return property;
    }

    private void apply(Property property, PropertyRequest request) {
        property.setName(request.name());
        property.setType(request.type());
        property.setAddress(request.address());
        property.setPhone(request.phone());
        property.setDescription(request.description());
        property.setStatus(request.status());
    }

    private void validate(String type, String status) {
        if (!"HOTEL".equals(type) && !"HOMESTAY".equals(type)) {
            throw new BusinessException("门店类型不正确");
        }
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new BusinessException("营业状态不正确");
        }
    }

    public record PropertyRequest(
            @jakarta.validation.constraints.NotBlank(message = "请输入门店名称") String name,
            @jakarta.validation.constraints.NotBlank(message = "请选择门店类型") String type,
            @jakarta.validation.constraints.NotBlank(message = "请输入门店地址") String address,
            String phone,
            String description,
            @jakarta.validation.constraints.NotBlank(message = "请选择营业状态") String status
    ) {
    }
}
