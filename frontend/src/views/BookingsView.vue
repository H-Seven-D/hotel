<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  CloseBold,
  Edit,
  Plus,
  Refresh,
  Search,
  SwitchButton
} from '@element-plus/icons-vue'
import http from '../api/http'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const editingId = ref(null)
const rows = ref([])
const total = ref(0)
const properties = ref([])
const customers = ref([])
const roomOptions = ref([])

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: '',
  propertyId: null,
  startDate: null,
  endDate: null
})

const form = reactive(defaultForm())

const rules = {
  propertyId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择房间', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  guestCount: [{ required: true, message: '请输入入住人数', trigger: 'blur' }],
  dateRange: [{ required: true, message: '请选择入住和退房日期', trigger: 'change' }]
}

const statusOptions = [
  { label: '待确认', value: 'PENDING' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已入住', value: 'CHECKED_IN' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const dialogTitle = computed(() => (editingId.value ? '编辑预订' : '新增预订'))
const nights = computed(() => {
  if (!form.dateRange?.length) return 0
  const diff = new Date(form.dateRange[1]) - new Date(form.dateRange[0])
  return Math.max(Math.round(diff / 86400000), 0)
})

function defaultForm() {
  const start = new Date()
  start.setDate(start.getDate() + 1)
  const end = new Date(start)
  end.setDate(end.getDate() + 2)
  return {
    propertyId: null,
    roomId: null,
    customerId: null,
    dateRange: [dateString(start), dateString(end)],
    guestCount: 1,
    remark: ''
  }
}

function dateString(date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0')
  ].join('-')
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

async function load() {
  loading.value = true
  try {
    const response = await http.get('/bookings', { params: query })
    rows.value = response.data.records
    total.value = response.data.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [propertyResponse, customerResponse] = await Promise.all([
      http.get('/properties', { params: { current: 1, size: 100, status: 'ACTIVE' } }),
      http.get('/customers', { params: { current: 1, size: 100 } })
    ])
    properties.value = propertyResponse.data.records
    customers.value = customerResponse.data.records
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadRooms(propertyId) {
  if (!propertyId) {
    roomOptions.value = []
    return
  }
  try {
    const response = await http.get('/rooms/options', { params: { propertyId } })
    roomOptions.value = response.data
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  Object.assign(query, {
    current: 1,
    size: 10,
    keyword: '',
    status: '',
    propertyId: null,
    startDate: null,
    endDate: null
  })
  load()
}

async function openCreate() {
  editingId.value = null
  Object.assign(form, defaultForm())
  form.propertyId = properties.value[0]?.id || null
  await loadRooms(form.propertyId)
  dialogVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    propertyId: row.propertyId,
    roomId: row.roomId,
    customerId: row.customerId,
    dateRange: [row.checkInDate, row.checkOutDate],
    guestCount: row.guestCount,
    remark: row.remark
  })
  await loadRooms(form.propertyId)
  dialogVisible.value = true
}

async function changeProperty() {
  form.roomId = null
  await loadRooms(form.propertyId)
}

async function submit() {
  await formRef.value.validate()
  if (nights.value < 1) {
    ElMessage.warning('退房日期必须晚于入住日期')
    return
  }
  saving.value = true
  try {
    const payload = {
      propertyId: form.propertyId,
      roomId: form.roomId,
      customerId: form.customerId,
      checkInDate: form.dateRange[0],
      checkOutDate: form.dateRange[1],
      guestCount: form.guestCount,
      remark: form.remark
    }
    if (editingId.value) {
      await http.put(`/bookings/${editingId.value}`, payload)
      ElMessage.success('预订信息已更新')
    } else {
      await http.post('/bookings', payload)
      ElMessage.success('预订已创建')
    }
    dialogVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function changeStatus(row, action, message) {
  try {
    await ElMessageBox.confirm(message, action, {
      confirmButtonText: '确认办理',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const endpoint = action === '办理入住' ? 'check-in' : action === '办理退房' ? 'check-out' : 'cancel'
    await http.post(`/bookings/${row.id}/${endpoint}`)
    ElMessage.success(`${action}成功`)
    load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message)
  }
}

onMounted(async () => {
  await loadOptions()
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="预订入住" description="完成房间预订、入住办理、退房和订单取消。">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增预订</el-button>
    </PageHeader>

    <section class="panel">
      <div class="panel__body">
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="按预订编号或备注搜索"
            :prefix-icon="Search"
            clearable
            style="width: 220px"
            @keyup.enter="search"
          />
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="query.propertyId" placeholder="全部门店" clearable style="width: 170px">
            <el-option v-for="item in properties" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-date-picker
            v-model="query.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="入住起始日"
            style="width: 145px"
          />
          <el-date-picker
            v-model="query.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="退房截止日"
            style="width: 145px"
          />
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </div>

        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column prop="bookingNo" label="预订编号" min-width="155">
            <template #default="{ row }"><strong>{{ row.bookingNo }}</strong></template>
          </el-table-column>
          <el-table-column label="客户" min-width="145">
            <template #default="{ row }">
              <div class="booking-customer">
                <span>{{ row.customerName }}</span>
                <small>{{ row.customerPhone }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="propertyName" label="门店" min-width="150" />
          <el-table-column prop="roomNo" label="房号" width="85" />
          <el-table-column label="入住区间" min-width="205">
            <template #default="{ row }">{{ row.checkInDate }} 至 {{ row.checkOutDate }}</template>
          </el-table-column>
          <el-table-column prop="guestCount" label="人数" width="75" />
          <el-table-column label="金额" width="115">
            <template #default="{ row }"><span class="money">¥ {{ formatMoney(row.totalAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><StatusTag kind="booking" :value="row.status" /></template>
          </el-table-column>
          <el-table-column label="操作" width="290" fixed="right">
            <template #default="{ row }">
              <div class="inline-actions">
                <el-button
                  v-if="row.status === 'CONFIRMED'"
                  link
                  type="success"
                  :icon="CircleCheck"
                  @click="changeStatus(row, '办理入住', `确定为房间 ${row.roomNo} 办理入住吗？`)"
                >
                  入住
                </el-button>
                <el-button
                  v-if="row.status === 'CHECKED_IN'"
                  link
                  type="warning"
                  :icon="SwitchButton"
                  @click="changeStatus(row, '办理退房', `退房后房间将转为清洁状态，确认继续吗？`)"
                >
                  退房
                </el-button>
                <el-button
                  v-if="['PENDING', 'CONFIRMED'].includes(row.status)"
                  link
                  type="primary"
                  :icon="Edit"
                  @click="openEdit(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="['PENDING', 'CONFIRMED'].includes(row.status)"
                  link
                  type="danger"
                  :icon="CloseBold"
                  @click="changeStatus(row, '取消预订', '取消后将释放当前预订占用，确认继续吗？')"
                >
                  取消
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="query.current"
            v-model:page-size="query.size"
            background
            layout="total, sizes, prev, pager, next"
            :total="total"
            :page-sizes="[10, 20, 50]"
            @change="load"
          />
        </div>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="所属门店" prop="propertyId">
            <el-select v-model="form.propertyId" style="width: 100%" @change="changeProperty">
              <el-option v-for="item in properties" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="房间" prop="roomId">
            <el-select v-model="form.roomId" style="width: 100%" placeholder="选择房间">
              <el-option
                v-for="item in roomOptions"
                :key="item.id"
                :label="`${item.roomNo} · ${item.roomTypeName}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="客户" prop="customerId">
            <el-select v-model="form.customerId" filterable style="width: 100%" placeholder="选择已登记客户">
              <el-option
                v-for="item in customers"
                :key="item.id"
                :label="`${item.name} · ${item.phone}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="入住人数" prop="guestCount">
            <el-input-number v-model="form.guestCount" :min="1" :max="20" style="width: 100%" />
          </el-form-item>
          <el-form-item class="span-2" label="入住日期" prop="dateRange">
            <el-date-picker
              v-model="form.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="入住日期"
              end-placeholder="退房日期"
              :disabled-date="(date) => date.getTime() < Date.now() - 86400000"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item class="span-2" label="预订备注">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="例如：预计晚间到店、需要儿童用品"
            />
          </el-form-item>
        </div>
        <div v-if="nights" class="empty-note">
          共 {{ nights }} 晚。最终金额由后端根据房型价格和日期自动计算。
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存预订</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
