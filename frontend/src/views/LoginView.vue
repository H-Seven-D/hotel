<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'Admin@123'
})

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    router.replace(String(route.query.redirect || '/dashboard'))
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-intro">
      <div class="login-intro__brand">
        <span class="brand__mark brand__mark--large"><el-icon><House /></el-icon></span>
        <div>
          <strong>yeu</strong>
          <span>民宿酒店预订管理系统</span>
        </div>
      </div>
      <div class="login-intro__copy">
        <span class="eyebrow">HOSPITALITY OPERATIONS</span>
        <h1>让每一次入住安排<br />清晰、有序、可追溯。</h1>
        <p>统一管理门店、房态、预订、入住退房与员工排班，管理员可查看完整的在线和操作记录。</p>
        <div class="login-feature-row">
          <span>实时房态</span>
          <span>预订调度</span>
          <span>审计追踪</span>
        </div>
      </div>
      <div class="login-intro__footer">毕业设计项目 · 2026</div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="login-card__heading">
          <span class="eyebrow">账号登录</span>
          <h2>进入管理工作台</h2>
          <p>使用管理员或普通员工账号登录系统。</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="form.username" :prefix-icon="User" size="large" placeholder="请输入账号" />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
            />
          </el-form-item>
          <el-button class="login-submit" type="primary" size="large" :loading="loading" @click="submit">
            登录系统
          </el-button>
        </el-form>

        <div class="login-card__hint">
          <span>演示管理员：admin / Admin@123</span>
          <span>演示员工：employee / Staff@123</span>
        </div>
      </div>
    </section>
  </div>
</template>
