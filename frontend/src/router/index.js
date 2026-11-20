import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/dashboard'
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
          meta: { title: '运营总览' }
        },
        {
          path: 'properties',
          name: 'properties',
          component: () => import('../views/PropertiesView.vue'),
          meta: { title: '门店管理' }
        },
        {
          path: 'rooms',
          name: 'rooms',
          component: () => import('../views/RoomsView.vue'),
          meta: { title: '房型房间' }
        },
        {
          path: 'bookings',
          name: 'bookings',
          component: () => import('../views/BookingsView.vue'),
          meta: { title: '预订入住' }
        },
        {
          path: 'customers',
          name: 'customers',
          component: () => import('../views/CustomersView.vue'),
          meta: { title: '客户档案' }
        },
        {
          path: 'schedules',
          name: 'schedules',
          component: () => import('../views/SchedulesView.vue'),
          meta: { title: '排班调度' }
        },
        {
          path: 'employees',
          name: 'employees',
          component: () => import('../views/EmployeesView.vue'),
          meta: { title: '员工管理', adminOnly: true }
        },
        {
          path: 'audit',
          name: 'audit',
          component: () => import('../views/AuditView.vue'),
          meta: { title: '审计中心', adminOnly: true }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  document.title = `${to.meta.title || '管理系统'} · yeu`
  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.token) {
    return { name: 'dashboard' }
  }
  if (to.meta.adminOnly && !auth.isAdmin) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
