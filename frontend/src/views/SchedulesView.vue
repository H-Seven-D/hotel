<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
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
const employees = ref([])
const properties = ref([])

const today = new Date()
const weekEnd = new Date()
weekEnd.setDate(weekEnd.getDate() + 6)

const query = reactive({
  current: 1,
  size: 20,
  userId: null,
  propertyId: null,
  startDate: dateString(today),
  endDate: dateString(weekEnd)
})

const form = reactive(defaultForm())

const rules = {
  userId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  propertyId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择排班日期', trigger: 'change' }],
  shift: [{ required: true, message: '请选择班次', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const shiftLabels = {
  MORNING: '早班',
  AFTERNOON: '午班',
  NIGHT: '夜班'
}

function dateString(date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0')
  ].join('-')
}

function defaultForm() {
  return {
    userId: null,
    propertyId: null,
    workDate: dateString(new Date()),
    shift: 'MORNING',
    status: 'SCHEDULED',
    remark: ''
  }
}

async function loadOptions() {
  try {
    const [employeeResponse, propertyResponse] = await Promise.all([
      http.get('/schedules/employees'),
      http.get('/properties', { params: { current: 1, size: 100, status: 'ACTIVE' } })
    ])
    employees.value = employeeResponse.data
    properties.value = propertyResponse.data.records
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function load() {
  loading.value = true
  try {
    const response = await http.get('/schedules', { params: query })
    rows.value = response.data.records
    total.value = response.data.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  Object.assign(query, {
    current: 1,
    size: 20,
    userId: null,
    propertyId: null,
    startDate: dateString(today),
    endDate: dateString(weekEnd)
  })
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, defaultForm())
  form.userId = employees.value[0]?.id || null
  form.propertyId = properties.value[0]?.id || null
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    userId: row.userId,
    propertyId: row.propertyId,
    workDate: row.workDate,
    shift: row.shift,
    status: row.status,
    remark: row.remark
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await http.put(`/schedules/${editingId.value}`, form)
      ElMessage.success('排班已更新')
    } else {
      await http.post('/schedules', form)
      ElMessage.success('排班已创建')
    }
    dialogVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除 ${row.userName} 在 ${row.workDate} 的${shiftLabels[row.shift]}吗？`,
      '删除排班',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await http.delete(`/schedules/${row.id}`)
    ElMessage.success('排班已删除')
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
    <PageHeader title="排班调度" description="按日期、门店和班次安排员工，确保前台与客房工作有序衔接。">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增排班</el-button>
    </PageHeader>

    <section class="panel">
      <div class="panel__body">
        <div class="toolbar">
          <el-select v-model="query.userId" placeholder="全部员工" clearable style="width: 150px">
            <el-option v-for="item in employees" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
          <el-select v-model="query.propertyId" placeholder="全部门店" clearable style="width: 170px">
            <el-option v-for="item in properties" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-date-picker
            v-model="query.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
            style="width: 145px"
          />
          <el-date-picker
            v-model="query.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
            style="width: 145px"
          />
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </div>

        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column prop="workDate" label="排班日期" min-width="125">
            <template #default="{ row }"><strong>{{ row.workDate }}</strong></template>
          </el-table-column>
          <el-table-column prop="userName" label="员工" min-width="120" />
          <el-table-column prop="propertyName" label="工作门店" min-width="170" />
          <el-table-column label="班次" width="100">
            <template #default="{ row }">
              <el-tag effect="plain">{{ shiftLabels[row.shift] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="105">
            <template #default="{ row }"><StatusTag kind="schedule" :value="row.status" /></template>
          </el-table-column>
          <el-table-column prop="remark" label="工作说明" min-width="230" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <div class="inline-actions">
                <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
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
            :page-sizes="[20, 50, 100]"
            @change="load"
          />
        </div>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑排班' : '新增排班'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="员工" prop="userId">
            <el-select v-model="form.userId" filterable style="width: 100%">
              <el-option
                v-for="item in employees"
                :key="item.id"
                :label="`${item.realName} · ${item.role === 'ADMIN' ? '管理员' : '普通员工'}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="工作门店" prop="propertyId">
            <el-select v-model="form.propertyId" style="width: 100%">
              <el-option v-for="item in properties" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="排班日期" prop="workDate">
            <el-date-picker v-model="form.workDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="班次" prop="shift">
            <el-select v-model="form.shift" style="width: 100%">
              <el-option label="早班" value="MORNING" />
              <el-option label="午班" value="AFTERNOON" />
              <el-option label="夜班" value="NIGHT" />
            </el-select>
          </el-form-item>
          <el-form-item label="排班状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="已排班" value="SCHEDULED" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="请假" value="LEAVE" />
            </el-select>
          </el-form-item>
          <el-form-item class="span-2" label="工作说明">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="例如：负责前台接待和入住办理"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
