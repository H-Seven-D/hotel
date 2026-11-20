package com.homestay.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.BusinessException;
import com.homestay.common.PageResponse;
import com.homestay.property.entity.Property;
import com.homestay.property.mapper.PropertyMapper;
import com.homestay.schedule.entity.EmployeeSchedule;
import com.homestay.schedule.mapper.EmployeeScheduleMapper;
import com.homestay.system.entity.SysUser;
import com.homestay.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final Set<String> SHIFTS = Set.of("MORNING", "AFTERNOON", "NIGHT");
    private static final Set<String> STATUSES = Set.of("SCHEDULED", "COMPLETED", "LEAVE");

    private final EmployeeScheduleMapper scheduleMapper;
    private final SysUserMapper userMapper;
    private final PropertyMapper propertyMapper;

    public PageResponse<EmployeeSchedule> page(long current, long size, Long userId,
                                               Long propertyId, LocalDate startDate,
                                               LocalDate endDate) {
        LambdaQueryWrapper<EmployeeSchedule> query = new LambdaQueryWrapper<EmployeeSchedule>()
                .eq(userId != null, EmployeeSchedule::getUserId, userId)
                .eq(propertyId != null, EmployeeSchedule::getPropertyId, propertyId)
                .ge(startDate != null, EmployeeSchedule::getWorkDate, startDate)
                .le(endDate != null, EmployeeSchedule::getWorkDate, endDate)
                .orderByAsc(EmployeeSchedule::getWorkDate)
                .orderByAsc(EmployeeSchedule::getShift);
        Page<EmployeeSchedule> page = scheduleMapper.selectPage(Page.of(current, size), query);
        enrich(page.getRecords());
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<EmployeeOption> employeeOptions() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1)
                        .orderByAsc(SysUser::getRealName))
                .stream()
                .map(user -> new EmployeeOption(user.getId(), user.getRealName(), user.getRole()))
                .toList();
    }

    public EmployeeSchedule create(ScheduleRequest request) {
        validate(request, null);
        EmployeeSchedule schedule = new EmployeeSchedule();
        apply(schedule, request);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());
        scheduleMapper.insert(schedule);
        return schedule;
    }

    public EmployeeSchedule update(Long id, ScheduleRequest request) {
        EmployeeSchedule schedule = requireSchedule(id);
        validate(request, id);
        apply(schedule, request);
        schedule.setUpdatedAt(LocalDateTime.now());
        scheduleMapper.updateById(schedule);
        return schedule;
    }

    @Transactional
    public void delete(Long id) {
        requireSchedule(id);
        scheduleMapper.deleteById(id);
    }

    private void validate(ScheduleRequest request, Long excludeId) {
        SysUser user = userMapper.selectById(request.userId());
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException("排班员工不存在或已停用");
        }
        Property property = propertyMapper.selectById(request.propertyId());
        if (property == null) {
            throw new BusinessException("排班门店不存在");
        }
        if (!SHIFTS.contains(request.shift())) {
            throw new BusinessException("班次不正确");
        }
        if (!STATUSES.contains(request.status())) {
            throw new BusinessException("排班状态不正确");
        }
        long duplicate = scheduleMapper.selectCount(new LambdaQueryWrapper<EmployeeSchedule>()
                .eq(EmployeeSchedule::getUserId, request.userId())
                .eq(EmployeeSchedule::getWorkDate, request.workDate())
                .eq(EmployeeSchedule::getShift, request.shift())
                .ne(excludeId != null, EmployeeSchedule::getId, excludeId));
        if (duplicate > 0) {
            throw new BusinessException("该员工当天此班次已经排班");
        }
    }

    private EmployeeSchedule requireSchedule(Long id) {
        EmployeeSchedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException("排班记录不存在");
        }
        return schedule;
    }

    private void apply(EmployeeSchedule schedule, ScheduleRequest request) {
        schedule.setUserId(request.userId());
        schedule.setPropertyId(request.propertyId());
        schedule.setWorkDate(request.workDate());
        schedule.setShift(request.shift());
        schedule.setStatus(request.status());
        schedule.setRemark(request.remark());
    }

    private void enrich(List<EmployeeSchedule> schedules) {
        if (schedules.isEmpty()) {
            return;
        }
        Set<Long> userIds = schedules.stream().map(EmployeeSchedule::getUserId).collect(Collectors.toSet());
        Set<Long> propertyIds = schedules.stream()
                .map(EmployeeSchedule::getPropertyId).collect(Collectors.toSet());
        Map<Long, SysUser> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, Property> properties = propertyMapper.selectBatchIds(propertyIds).stream()
                .collect(Collectors.toMap(Property::getId, Function.identity()));
        schedules.forEach(schedule -> {
            SysUser user = users.get(schedule.getUserId());
            Property property = properties.get(schedule.getPropertyId());
            schedule.setUserName(user == null ? "" : user.getRealName());
            schedule.setPropertyName(property == null ? "" : property.getName());
        });
    }

    public record EmployeeOption(Long id, String realName, String role) {
    }

    public record ScheduleRequest(
            @jakarta.validation.constraints.NotNull(message = "请选择员工") Long userId,
            @jakarta.validation.constraints.NotNull(message = "请选择门店") Long propertyId,
            @jakarta.validation.constraints.NotNull(message = "请选择排班日期") LocalDate workDate,
            @jakarta.validation.constraints.NotBlank(message = "请选择班次") String shift,
            @jakarta.validation.constraints.NotBlank(message = "请选择排班状态") String status,
            String remark
    ) {
    }
}
