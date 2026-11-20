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
const rows = ref([])
const total = ref(0)
const editingId = ref(null)

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  type: '',
  status: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择门店类型', trigger: 'change' }],
  address: [{ required: true, message: '请输入门店地址', trigger: 'blur' }],
  status: [{ required: true, message: '请选择营业状态', trigger: 'change' }]
}

function defaultForm() {
  return {
    name: '',
    type: 'HOMESTAY',
    address: '',
    phone: '',
    description: '',
    status: 'ACTIVE'
  }
}

async function load() {
  loading.value = true
  try {
    const response = await http.get('/properties', { params: query })
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
  Object.assign(query, { current: 1, size: 10, keyword: '', type: '', status: '' })
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
    type: row.type,
    address: row.address,
    phone: row.phone,
    description: row.description,
    status: row.status
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await http.put(`/properties/${editingId.value}`, form)
      ElMessage.success('门店信息已更新')
    } else {
      await http.post('/properties', form)
      ElMessage.success('门店已创建')
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
      `确定删除“${row.name}”吗？有关联房型或房间时系统会阻止删除。`,
      '删除门店',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await http.delete(`/properties/${row.id}`)
    ElMessage.success('门店已删除')
    load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message)
    }
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="门店管理" description="维护民宿与酒店的基础资料和营业状态。">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增门店</el-button>
    </PageHeader>

    <section class="panel">
      <div class="panel__body">
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="按名称或地址搜索"
            :prefix-icon="Search"
            clearable
            style="width: 240px"
            @keyup.enter="search"
          />
          <el-select v-model="query.type" placeholder="门店类型" clearable style="width: 140px">
            <el-option label="民宿" value="HOMESTAY" />
            <el-option label="酒店" value="HOTEL" />
          </el-select>
          <el-select v-model="query.status" placeholder="营业状态" clearable style="width: 140px">
            <el-option label="营业中" value="ACTIVE" />
            <el-option label="已停用" value="INACTIVE" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </div>

        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column prop="name" label="门店名称" min-width="170" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <StatusTag kind="propertyType" :value="row.type" />
            </template>
          </el-table-column>
          <el-table-column prop="address" label="门店地址" min-width="230" show-overflow-tooltip />
          <el-table-column prop="phone" label="联系电话" min-width="140" />
          <el-table-column prop="description" label="简介" min-width="230" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <StatusTag kind="property" :value="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
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

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑门店' : '新增门店'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="门店名称" prop="name">
            <el-input v-model="form.name" placeholder="例如：云栖山居民宿" />
          </el-form-item>
          <el-form-item label="门店类型" prop="type">
            <el-select v-model="form.type" style="width: 100%">
              <el-option label="民宿" value="HOMESTAY" />
              <el-option label="酒店" value="HOTEL" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="前台或预订电话" />
          </el-form-item>
          <el-form-item label="营业状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="营业中" value="ACTIVE" />
              <el-option label="已停用" value="INACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item class="span-2" label="门店地址" prop="address">
            <el-input v-model="form.address" placeholder="请输入详细地址" />
          </el-form-item>
          <el-form-item class="span-2" label="门店简介">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="简要描述门店特色与位置"
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
