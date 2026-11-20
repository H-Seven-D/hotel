<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search } from '@element-plus/icons-vue'
import http from '../api/http'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const passwordVisible = ref(false)
const formRef = ref()
const passwordRef = ref()
const editingId = ref(null)
const passwordUser = ref(null)
const rows = ref([])
const total = ref(0)

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  role: '',
  status: null
})

const form = reactive(defaultForm())
const passwordForm = reactive({ password: '' })

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ min: 8, message: '密码至少需要 8 个字符', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择员工角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择账号状态', trigger: 'change' }]
}

const passwordRules = {
  password: [{ required: true, min: 8, message: '新密码至少需要 8 个字符', trigger: 'blur' }]
}

function defaultForm() {
  return {
    username: '',
    password: 'Staff@123',
    realName: '',
    phone: '',
    role: 'EMPLOYEE',
    status: 1
  }
}

async function load() {
  loading.value = true
  try {
    const response = await http.get('/users', { params: query })
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
  Object.assign(query, { current: 1, size: 10, keyword: '', role: '', status: null })
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
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone,
    role: row.role,
    status: row.status
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await http.put(`/users/${editingId.value}`, {
        realName: form.realName,
        phone: form.phone,
        role: form.role,
        status: form.status
      })
      ElMessage.success('员工信息已更新')
    } else {
      await http.post('/users', form)
      ElMessage.success('员工账号已创建')
    }
    dialogVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

function openPassword(row) {
  passwordUser.value = row
  passwordForm.password = ''
  passwordVisible.value = true
}

async function resetPassword() {
  await passwordRef.value.validate()
  saving.value = true
  try {
    await http.put(`/users/${passwordUser.value.id}/password`, passwordForm)
    ElMessage.success('员工密码已重置')
    passwordVisible.value = false
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除员工账号“${row.realName}（${row.username}）”吗？`,
      '删除员工',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await http.delete(`/users/${row.id}`)
    ElMessage.success('员工账号已删除')
    load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="员工管理" description="仅管理员可新增、停用、删除员工账号并重置登录密码。">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增员工</el-button>
    </PageHeader>

    <section class="panel">
      <div class="panel__body">
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="按账号、姓名或手机号搜索"
            :prefix-icon="Search"
            clearable
            style="width: 260px"
            @keyup.enter="search"
          />
          <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 140px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通员工" value="EMPLOYEE" />
          </el-select>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </div>

        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column prop="realName" label="员工姓名" min-width="130">
            <template #default="{ row }">
              <div class="booking-customer">
                <strong>{{ row.realName }}</strong>
                <small>{{ row.username }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" min-width="135" />
          <el-table-column label="角色" width="110">
            <template #default="{ row }"><StatusTag kind="role" :value="row.role" /></template>
          </el-table-column>
          <el-table-column label="账号状态" width="105">
            <template #default="{ row }"><StatusTag kind="account" :value="row.status" /></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="170" />
          <el-table-column prop="updatedAt" label="最后更新" min-width="170" />
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <div class="inline-actions">
                <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
                <el-button link type="warning" :icon="Key" @click="openPassword(row)">重置密码</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑员工' : '新增员工'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="form.username" :disabled="Boolean(editingId)" placeholder="用于登录系统" />
          </el-form-item>
          <el-form-item v-if="!editingId" label="初始密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="至少 8 个字符" />
          </el-form-item>
          <el-form-item label="员工姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="员工角色" prop="role">
            <el-select v-model="form.role" style="width: 100%">
              <el-option label="普通员工" value="EMPLOYEE" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </el-form-item>
          <el-form-item label="账号状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
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

    <el-dialog v-model="passwordVisible" title="重置员工密码" width="460px" destroy-on-close>
      <div class="empty-note" style="margin-bottom: 16px">
        正在为 {{ passwordUser?.realName }}（{{ passwordUser?.username }}）设置新密码。
      </div>
      <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="至少 8 个字符" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="passwordVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="resetPassword">确认重置</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
