<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: [String, Number],
    default: ''
  },
  kind: {
    type: String,
    default: 'default'
  }
})

const maps = {
  property: {
    ACTIVE: ['营业中', 'success'],
    INACTIVE: ['已停用', 'info']
  },
  propertyType: {
    HOTEL: ['酒店', 'primary'],
    HOMESTAY: ['民宿', 'warning']
  },
  room: {
    AVAILABLE: ['空闲', 'success'],
    RESERVED: ['已预订', 'primary'],
    OCCUPIED: ['已入住', 'warning'],
    CLEANING: ['清洁中', 'info'],
    MAINTENANCE: ['维护中', 'danger']
  },
  booking: {
    PENDING: ['待确认', 'info'],
    CONFIRMED: ['已确认', 'primary'],
    CHECKED_IN: ['已入住', 'warning'],
    COMPLETED: ['已完成', 'success'],
    CANCELLED: ['已取消', 'info']
  },
  schedule: {
    SCHEDULED: ['已排班', 'primary'],
    COMPLETED: ['已完成', 'success'],
    LEAVE: ['请假', 'warning']
  },
  session: {
    ONLINE: ['在线', 'success'],
    OFFLINE: ['已下线', 'info']
  },
  role: {
    ADMIN: ['管理员', 'danger'],
    EMPLOYEE: ['普通员工', 'primary']
  },
  account: {
    1: ['启用', 'success'],
    0: ['停用', 'info']
  },
  result: {
    SUCCESS: ['成功', 'success'],
    FAIL: ['失败', 'danger']
  }
}

const display = computed(() => {
  const entry = maps[props.kind]?.[props.value]
  return entry || [String(props.value ?? '-'), 'info']
})
</script>

<template>
  <el-tag :type="display[1]" effect="light" round>{{ display[0] }}</el-tag>
</template>
