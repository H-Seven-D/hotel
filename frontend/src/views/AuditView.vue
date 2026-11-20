<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import http from '../api/http'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'

const activeTab = ref('sessions')
const sessionLoading = ref(false)
const operationLoading = ref(false)
const sessions = ref([])
const sessionTotal = ref(0)
const operations = ref([])
const operationTotal = ref(0)

const sessionQuery = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: ''
})

const operationQuery = reactive({
  current: 1,
  size: 10,
  keyword: '',
  module: '',
  result: ''
})

const modules = ['登录认证', '员工管理', '门店管理', '房型管理', '房间管理', '房间调度', '客户管理', '预订管理', '入住管理', '排班调度']

async function loadSessions() {
  sessionLoading.value = true
  try {
    const response = await http.get('/audit/sessions', { params: sessionQuery })
    sessions.value = response.data.records
    sessionTotal.value = response.data.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    sessionLoading.value = false
  }
}

async function loadOperations() {
  operationLoading.value = true
  try {
    const response = await http.get('/audit/operations', { params: operationQuery })
    operations.value = response.data.records
    operationTotal.value = response.data.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    operationLoading.value = false
  }
}

function searchSessions() {
  sessionQuery.current = 1
  loadSessions()
}

function resetSessions() {
  Object.assign(sessionQuery, { current: 1, size: 10, keyword: '', status: '' })
  loadSessions()
}

function searchOperations() {
  operationQuery.current = 1
  loadOperations()
}

function resetOperations() {
  Object.assign(operationQuery, { current: 1, size: 10, keyword: '', module: '', result: '' })
  loadOperations()
}

function duration(row) {
  const start = new Date(row.loginTime).getTime()
  const end = row.logoutTime ? new Date(row.logoutTime).getTime() : Date.now()
  const minutes = Math.max(Math.round((end - start) / 60000), 0)
  if (minutes < 60) return `${minutes} 分钟`
  return `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分钟`
}

onMounted(async () => {
  await Promise.all([loadSessions(), loadOperations()])
})
</script>

<template>
  <div>
    <PageHeader title="审计中心" description="查看员工登录上下线时间与系统关键操作记录。">
      <el-button :icon="Refresh" @click="activeTab === 'sessions' ? loadSessions() : loadOperations()">
        刷新记录
      </el-button>
    </PageHeader>

    <section class="panel">
      <div class="panel__body">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="登录会话" name="sessions">
            <div class="toolbar">
              <el-input
                v-model="sessionQuery.keyword"
                placeholder="按账号或员工姓名搜索"
                :prefix-icon="Search"
                clearable
                style="width: 250px"
                @keyup.enter="searchSessions"
              />
              <el-select v-model="sessionQuery.status" placeholder="全部状态" clearable style="width: 130px">
                <el-option label="在线" value="ONLINE" />
                <el-option label="已下线" value="OFFLINE" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="searchSessions">查询</el-button>
              <el-button :icon="Refresh" @click="resetSessions">重置</el-button>
            </div>

            <el-table v-loading="sessionLoading" :data="sessions" stripe>
              <el-table-column label="员工" min-width="140">
                <template #default="{ row }">
                  <div class="booking-customer">
                    <strong>{{ row.realName }}</strong>
                    <small>{{ row.username }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="loginTime" label="上线时间" min-width="170" />
              <el-table-column label="下线时间" min-width="170">
                <template #default="{ row }">{{ row.logoutTime || '当前仍在线' }}</template>
              </el-table-column>
              <el-table-column label="在线时长" min-width="130">
                <template #default="{ row }">{{ duration(row) }}</template>
              </el-table-column>
              <el-table-column prop="ipAddress" label="IP 地址" min-width="130" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }"><StatusTag kind="session" :value="row.onlineStatus" /></template>
              </el-table-column>
            </el-table>

            <div class="table-pagination">
              <el-pagination
                v-model:current-page="sessionQuery.current"
                v-model:page-size="sessionQuery.size"
                background
                layout="total, sizes, prev, pager, next"
                :total="sessionTotal"
                :page-sizes="[10, 20, 50]"
                @change="loadSessions"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="操作日志" name="operations">
            <div class="toolbar">
              <el-input
                v-model="operationQuery.keyword"
                placeholder="按员工、姓名或操作搜索"
                :prefix-icon="Search"
                clearable
                style="width: 250px"
                @keyup.enter="searchOperations"
              />
              <el-select v-model="operationQuery.module" placeholder="全部模块" clearable style="width: 150px">
                <el-option v-for="item in modules" :key="item" :label="item" :value="item" />
              </el-select>
              <el-select v-model="operationQuery.result" placeholder="全部结果" clearable style="width: 130px">
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAIL" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="searchOperations">查询</el-button>
              <el-button :icon="Refresh" @click="resetOperations">重置</el-button>
            </div>

            <el-table v-loading="operationLoading" :data="operations" stripe>
              <el-table-column prop="createdAt" label="操作时间" min-width="170" />
              <el-table-column label="操作员工" min-width="140">
                <template #default="{ row }">
                  <div class="booking-customer">
                    <strong>{{ row.realName }}</strong>
                    <small>{{ row.username }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="module" label="业务模块" min-width="120" />
              <el-table-column prop="action" label="操作内容" min-width="170" />
              <el-table-column label="请求" min-width="210">
                <template #default="{ row }">
                  <span class="muted">{{ row.requestMethod }} {{ row.requestUri }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="ipAddress" label="IP 地址" min-width="125" />
              <el-table-column label="结果" width="90">
                <template #default="{ row }"><StatusTag kind="result" :value="row.result" /></template>
              </el-table-column>
            </el-table>

            <div class="table-pagination">
              <el-pagination
                v-model:current-page="operationQuery.current"
                v-model:page-size="operationQuery.size"
                background
                layout="total, sizes, prev, pager, next"
                :total="operationTotal"
                :page-sizes="[10, 20, 50]"
                @change="loadOperations"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>
  </div>
</template>
