<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  Coin,
  House,
  PieChart,
  Refresh,
  Tickets
} from '@element-plus/icons-vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'
import TrendChart from '../components/TrendChart.vue'

const auth = useAuthStore()
const loading = ref(false)
const data = ref({
  summary: {},
  roomStatus: [],
  recentBookings: [],
  trend: []
})

const roomLabels = {
  AVAILABLE: '空闲',
  RESERVED: '已预订',
  OCCUPIED: '已入住',
  CLEANING: '清洁中',
  MAINTENANCE: '维护中'
}

const roomClass = {
  AVAILABLE: 'available',
  RESERVED: 'reserved',
  OCCUPIED: 'occupied',
  CLEANING: 'cleaning',
  MAINTENANCE: 'maintenance'
}

const totalRooms = computed(() => Number(data.value.summary.totalRooms || 0))
const chartData = computed(() => {
  const source = new Map(data.value.trend.map((item) => [String(item.day).slice(0, 10), item]))
  return Array.from({ length: 14 }).map((_, index) => {
    const date = new Date()
    date.setDate(date.getDate() - (13 - index))
    const key = [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0')
    ].join('-')
    const item = source.get(key)
    return {
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      count: Number(item?.count || 0),
      revenue: Number(item?.revenue || 0)
    }
  })
})

function formatMoney(value) {
  return Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

function percent(count) {
  if (!totalRooms.value) return 0
  return Math.round((Number(count) / totalRooms.value) * 100)
}

async function load() {
  loading.value = true
  try {
    const response = await http.get('/dashboard/overview')
    data.value = response.data
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <PageHeader title="运营总览" description="查看当前房态、今日接待与近期预订变化。">
      <el-button :icon="Refresh" @click="load">刷新数据</el-button>
    </PageHeader>

    <div class="dashboard-grid">
      <section class="stat-grid">
        <article class="stat-card">
          <div class="stat-card__top">
            <span class="stat-card__label">可用客房</span>
            <span class="stat-card__icon"><el-icon><House /></el-icon></span>
          </div>
          <div>
            <div class="stat-card__value">
              {{ data.summary.availableRooms || 0 }}<small>/ {{ totalRooms }} 间</small>
            </div>
            <div class="stat-card__meta">当前可直接安排的房间数量</div>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-card__top">
            <span class="stat-card__label">当前入住率</span>
            <span class="stat-card__icon"><el-icon><PieChart /></el-icon></span>
          </div>
          <div>
            <div class="stat-card__value">
              {{ data.summary.occupancyRate || 0 }}<small>%</small>
            </div>
            <div class="stat-card__meta">入住与已预订房间占用比例</div>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-card__top">
            <span class="stat-card__label">今日接待</span>
            <span class="stat-card__icon"><el-icon><Calendar /></el-icon></span>
          </div>
          <div>
            <div class="stat-card__value">
              {{ data.summary.todayCheckIns || 0 }}<small>人/单</small>
            </div>
            <div class="stat-card__meta">今日预抵 {{ data.summary.todayCheckIns || 0 }}，预离 {{ data.summary.todayCheckOuts || 0 }}</div>
          </div>
        </article>

        <article class="stat-card">
          <div class="stat-card__top">
            <span class="stat-card__label">本月预订金额</span>
            <span class="stat-card__icon"><el-icon><Coin /></el-icon></span>
          </div>
          <div>
            <div class="stat-card__value" style="font-size: 26px">
              ¥ {{ formatMoney(data.summary.monthRevenue) }}
            </div>
            <div class="stat-card__meta">
              <template v-if="auth.isAdmin">
                在岗员工 {{ data.summary.onlineEmployees || 0 }} / {{ data.summary.activeEmployees || 0 }}
              </template>
              <template v-else>统计口径为本月非取消预订</template>
            </div>
          </div>
        </article>
      </section>

      <section class="dashboard-main">
        <div class="panel">
          <div class="panel__header">
            <div>
              <h3>近两周预订趋势</h3>
              <p>预订数量与预订金额变化</p>
            </div>
            <el-icon style="font-size: 19px; color: #7a8496"><Tickets /></el-icon>
          </div>
          <div class="panel__body">
            <TrendChart :data="chartData" />
          </div>
        </div>

        <div class="panel">
          <div class="panel__header">
            <div>
              <h3>房态分布</h3>
              <p>共 {{ totalRooms }} 间房</p>
            </div>
          </div>
          <div class="panel__body">
            <div class="room-status-list">
              <div v-for="item in data.roomStatus" :key="item.status" class="room-status-item">
                <div class="room-status-item__line">
                  <span>
                    <i class="status-dot" :class="`status-dot--${roomClass[item.status]}`"></i>
                    {{ roomLabels[item.status] }}
                  </span>
                  <strong>{{ item.count }} 间</strong>
                </div>
                <div class="progress-track">
                  <span :style="{ width: `${percent(item.count)}%` }"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel__header">
          <div>
            <h3>最近预订</h3>
            <p>最新录入的预订与入住安排</p>
          </div>
          <router-link class="el-button el-button--primary is-link" to="/bookings">查看全部</router-link>
        </div>
        <div class="panel__body" style="padding-top: 8px">
          <el-table :data="data.recentBookings" stripe>
            <el-table-column prop="bookingNo" label="预订编号" min-width="150" />
            <el-table-column label="客户" min-width="150">
              <template #default="{ row }">
                <div class="booking-customer">
                  <span>{{ row.customerName }}</span>
                  <small>{{ row.customerPhone }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="propertyName" label="门店" min-width="150" />
            <el-table-column prop="roomNo" label="房号" width="90" />
            <el-table-column label="入住日期" min-width="120">
              <template #default="{ row }">{{ row.checkInDate }} 至 {{ row.checkOutDate }}</template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }">
                <span class="money">¥ {{ formatMoney(row.totalAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <StatusTag kind="booking" :value="row.status" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </div>
  </div>
</template>
