<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import http from '../api/http'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const editingId = ref(null)
const rows = ref([])
const total = ref(0)

const query = reactive({
  current: 1,
  size: 10,
  keyword: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入客户手机号', trigger: 'blur' }]
}

function defaultForm() {
  return {
    name: '',
    phone: '',
    idCard: '',
    email: '',
    remark: ''
  }
}

async function load() {
  loading.value = true
  try {
    const response = await http.get('/customers', { params: query })
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
  Object.assign(query, { current: 1, size: 10, keyword: '' })
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    phone: row.phone,
    idCard: row.idCard,
    email: row.email,
    remark: row.remark
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await http.put(`/customers/${editingId.value}`, form)
      ElMessage.success('客户档案已更新')
    } else {
      await http.post('/customers', form)
      ElMessage.success('客户档案已创建')
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
    await ElMessageBox.confirm(`确定删除客户“${row.name}”吗？`, '删除客户', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await http.delete(`/customers/${row.id}`)
    ElMessage.success('客户档案已删除')
    load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="客户档案" description="集中维护入住客户信息，便于预订时快速选择和检索。">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增客户</el-button>
    </PageHeader>

    <section class="panel">
      <div class="panel__body">
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="按姓名、手机号或证件号搜索"
            :prefix-icon="Search"
            clearable
            style="width: 280px"
            @keyup.enter="search"
          />
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
          <div class="toolbar__spacer"></div>
          <span class="empty-note">共 {{ total }} 位客户</span>
        </div>

        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column prop="name" label="客户姓名" min-width="120">
            <template #default="{ row }"><strong>{{ row.name }}</strong></template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" min-width="135" />
          <el-table-column prop="idCard" label="证件号码" min-width="180" show-overflow-tooltip />
          <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
          <el-table-column prop="remark" label="偏好与备注" min-width="220" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="建档时间" min-width="170" />
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
            :page-sizes="[10, 20, 50]"
            @change="load"
          />
        </div>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑客户' : '新增客户'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="客户姓名" prop="name">
            <el-input v-model="form.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="证件号码">
            <el-input v-model="form.idCard" placeholder="身份证或其他证件号" />
          </el-form-item>
          <el-form-item label="电子邮箱">
            <el-input v-model="form.email" placeholder="name@example.com" />
          </el-form-item>
          <el-form-item class="span-2" label="偏好与备注">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="记录房型偏好、到店时间或特殊需求"
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
