<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Calendar,
  Collection,
  DataAnalysis,
  Document,
  Expand,
  Fold,
  Grid,
  House,
  Menu,
  OfficeBuilding,
  Operation,
  SwitchButton,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const mobileOpen = ref(false)

const menuItems = computed(() => {
  const items = [
    { path: '/dashboard', label: '运营总览', icon: DataAnalysis },
    { path: '/properties', label: '门店管理', icon: OfficeBuilding },
    { path: '/rooms', label: '房型房间', icon: Grid },
    { path: '/bookings', label: '预订入住', icon: Calendar },
    { path: '/customers', label: '客户档案', icon: User },
    { path: '/schedules', label: '排班调度', icon: Operation }
  ]
  if (auth.isAdmin) {
    items.push(
      { path: '/employees', label: '员工管理', icon: UserFilled },
      { path: '/audit', label: '审计中心', icon: Document }
    )
  }
  return items
})

const currentTitle = computed(() => route.meta.title || '运营总览')
const currentDate = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

function navigate(path) {
  router.push(path)
  mobileOpen.value = false
}

async function handleLogout() {
  await ElMessageBox.confirm('退出后本次在线会话将结束，是否继续？', '退出登录', {
    confirmButtonText: '退出登录',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ 'sidebar--collapsed': collapsed }">
      <button class="brand" type="button" @click="navigate('/dashboard')">
        <span class="brand__mark"><el-icon><House /></el-icon></span>
        <span v-if="!collapsed" class="brand__text">
          <strong>yeu</strong>
          <small>民宿酒店预订系统</small>
        </span>
      </button>

      <nav class="sidebar__nav" aria-label="主导航">
        <button
          v-for="item in menuItems"
          :key="item.path"
          type="button"
          class="nav-item"
          :class="{ 'nav-item--active': route.path === item.path }"
          :title="collapsed ? item.label : undefined"
          @click="navigate(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span v-if="!collapsed">{{ item.label }}</span>
        </button>
      </nav>

      <div class="sidebar__footer">
        <button class="collapse-button" type="button" @click="collapsed = !collapsed">
          <el-icon><component :is="collapsed ? Expand : Fold" /></el-icon>
          <span v-if="!collapsed">收起侧栏</span>
        </button>
      </div>
    </aside>

    <el-drawer v-model="mobileOpen" direction="ltr" size="268px" :with-header="false">
      <div class="mobile-menu">
        <div class="brand brand--mobile">
          <span class="brand__mark"><el-icon><House /></el-icon></span>
          <span class="brand__text">
            <strong>yeu</strong>
            <small>民宿酒店预订系统</small>
          </span>
        </div>
        <nav class="sidebar__nav">
          <button
            v-for="item in menuItems"
            :key="item.path"
            type="button"
            class="nav-item"
            :class="{ 'nav-item--active': route.path === item.path }"
            @click="navigate(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </nav>
      </div>
    </el-drawer>

    <div class="app-main" :class="{ 'app-main--expanded': collapsed }">
      <header class="topbar">
        <div class="topbar__left">
          <el-tooltip content="打开导航">
            <el-button class="mobile-menu-button" :icon="Menu" circle @click="mobileOpen = true" />
          </el-tooltip>
          <div>
            <span class="eyebrow">工作台</span>
            <h2>{{ currentTitle }}</h2>
          </div>
        </div>
        <div class="topbar__right">
          <span class="topbar__date">{{ currentDate }}</span>
          <el-dropdown trigger="click">
            <button class="user-chip" type="button">
              <span class="user-chip__avatar">{{ auth.displayName.slice(0, 1) }}</span>
              <span class="user-chip__text">
                <strong>{{ auth.displayName }}</strong>
                <small>{{ auth.isAdmin ? '管理员' : '普通员工' }}</small>
              </span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :icon="Collection" disabled>
                  {{ auth.user?.username }}
                </el-dropdown-item>
                <el-dropdown-item :icon="SwitchButton" divided @click="handleLogout">
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>
