<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Edit,
  Files,
  Plus,
  Refresh,
  Search,
  SetUp
} from '@element-plus/icons-vue'
import http from '../api/http'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'

const activeTab = ref('rooms')
const loading = ref(false)
const saving = ref(false)
const roomDialog = ref(false)
const typeDialog = ref(false)
const statusDialog = ref(false)
const logDrawer = ref(false)
const editingRoomId = ref(null)
const editingTypeId = ref(null)
const selectedRoom = ref(null)
const roomFormRef = ref()
const typeFormRef = ref()
const statusFormRef = ref()

const rooms = ref([])
const roomTotal = ref(0)
const roomTypes = ref([])
const properties = ref([])
const statusLogs = ref([])

const roomQuery = reactive({
  current: 1,
  size: 10,
  propertyId: null,
  status: '',
  keyword: ''
})

const typeQuery = reactive({
  propertyId: null,
  keyword: ''
})

const roomForm = reactive(defaultRoom())
const typeForm = reactive(defaultRoomType())
const statusForm = reactive({ status: 'AVAILABLE', notes: '' })

const roomRules = {
  propertyId: [{ required: true, message: '请选择所属门店', trigger: 'change' }],
  roomTypeId: [{ required: true, message: '请选择房型', trigger: 'change' }],
  roomNo: [{ required: true, message: '请输入房号', trigger: 'blur' }],
  status: [{ required: true, message: '请选择房间状态', trigger: 'change' }]
}

const typeRules = {
  propertyId: [{ required: true, message: '请选择所属门店', trigger: 'change' }],
  name: [{ required: true, message: '请输入房型名称', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入可入住人数', trigger: 'blur' }],
  basePrice: [{ required: true, message: '请输入基础价格', trigger: 'blur' }],
  status: [{ required: true, message: '请选择房型状态', trigger: 'change' }]
}

const filteredRoomTypes = computed(() => {
  if (!roomForm.propertyId) return roomTypes.value
  return roomTypes.value.filter((item) => item.propertyId === roomForm.propertyId)
})

const roomStatuses = [
  { label: '空闲', value: 'AVAILABLE' },
  { label: '已预订', value: 'RESERVED' },
  { label: '已入住', value: 'OCCUPIED' },
  { label: '清洁中', value: 'CLEANING' },
  { label: '维护中', value: 'MAINTENANCE' }
]

function defaultRoom() {
  return {
    propertyId: null,
    roomTypeId: null,
    roomNo: '',
    floor: 1,
    status: 'AVAILABLE',
    notes: ''
  }
}

function defaultRoomType() {
  return {
    propertyId: null,
    name: '',
    capacity: 2,
    basePrice: 0,
    description: '',
    status: 'ACTIVE'
  }
}

async function loadOptions() {
  try {
    const [propertyResponse, typeResponse] = await Promise.all([
      http.get('/properties', { params: { current: 1, size: 100 } }),
      http.get('/room-types', { params: { current: 1, size: 100 } })
    ])
    properties.value = propertyResponse.data.records
    roomTypes.value = typeResponse.data.records
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadRooms() {
  loading.value = true
  try {
    const response = await http.get('/rooms', { params: roomQuery })
    rooms.value = response.data.records
    roomTotal.value = response.data.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function loadTypes() {
  loading.value = true
  try {
    const response = await http.get('/room-types', {
      params: { ...typeQuery, current: 1, size: 100 }
    })
    roomTypes.value = response.data.records
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function searchRooms() {
  roomQuery.current = 1
  loadRooms()
}

function resetRooms() {
  Object.assign(roomQuery, { current: 1, size: 10, propertyId: null, status: '', keyword: '' })
  loadRooms()
}

function searchTypes() {
  loadTypes()
}

function openCreateRoom() {
  editingRoomId.value = null
  Object.assign(roomForm, defaultRoom())
  roomDialog.value = true
}

function openEditRoom(row) {
  editingRoomId.value = row.id
  Object.assign(roomForm, {
    propertyId: row.propertyId,
    roomTypeId: row.roomTypeId,
    roomNo: row.roomNo,
    floor: row.floor,
    status: row.status,
    notes: row.notes
  })
  roomDialog.value = true
}

async function submitRoom() {
  await roomFormRef.value.validate()
  saving.value = true
  try {
    if (editingRoomId.value) {
      await http.put(`/rooms/${editingRoomId.value}`, roomForm)
      ElMessage.success('房间信息已更新')
    } else {
      await http.post('/rooms', roomForm)
      ElMessage.success('房间已创建')
    }
    roomDialog.value = false
    await Promise.all([loadRooms(), loadOptions()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function removeRoom(row) {
  try {
    await ElMessageBox.confirm(`确定删除房间 ${row.roomNo} 吗？`, '删除房间', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await http.delete(`/rooms/${row.id}`)
    ElMessage.success('房间已删除')
    loadRooms()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message)
  }
}

function openStatus(row) {
  selectedRoom.value = row
  Object.assign(statusForm, { status: row.status, notes: '' })
  statusDialog.value = true
}

async function submitStatus() {
  await statusFormRef.value.validate()
  saving.value = true
  try {
    await http.put(`/rooms/${selectedRoom.value.id}/status`, statusForm)
    ElMessage.success('房间状态已更新')
    statusDialog.value = false
    loadRooms()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function openLogs(row) {
  selectedRoom.value = row
  logDrawer.value = true
  try {
    const response = await http.get(`/rooms/${row.id}/status-logs`)
    statusLogs.value = response.data
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openCreateType() {
  editingTypeId.value = null
  Object.assign(typeForm, defaultRoomType())
  typeDialog.value = true
}

function openEditType(row) {
  editingTypeId.value = row.id
  Object.assign(typeForm, {
    propertyId: row.propertyId,
    name: row.name,
    capacity: row.capacity,
    basePrice: Number(row.basePrice),
    description: row.description,
    status: row.status
  })
  typeDialog.value = true
}

async function submitType() {
  await typeFormRef.value.validate()
  saving.value = true
  try {
    if (editingTypeId.value) {
      await http.put(`/room-types/${editingTypeId.value}`, typeForm)
      ElMessage.success('房型信息已更新')
    } else {
      await http.post('/room-types', typeForm)
      ElMessage.success('房型已创建')
    }
    typeDialog.value = false
    await Promise.all([loadTypes(), loadOptions()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function removeType(row) {
  try {
    await ElMessageBox.confirm(`确定删除房型“${row.name}”吗？`, '删除房型', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await http.delete(`/room-types/${row.id}`)
    ElMessage.success('房型已删除')
    await Promise.all([loadTypes(), loadOptions()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message)
  }
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

onMounted(async () => {
  await loadOptions()
  await loadRooms()
})
</script>

<template>
  <div>
    <PageHeader title="房型房间" description="维护门店房型、房间编号和实时房态，支持清洁与维护调度。">
      <el-button
        type="primary"
        :icon="Plus"
        @click="activeTab === 'rooms' ? openCreateRoom() : openCreateType()"
      >
        {{ activeTab === 'rooms' ? '新增房间' : '新增房型' }}
      </el-button>
    </PageHeader>

    <section class="panel">
      <div class="panel__body">
        <el-tabs v-model="activeTab" class="room-tabs">
          <el-tab-pane label="房间清单" name="rooms">
            <div class="toolbar">
              <el-input
                v-model="roomQuery.keyword"
                placeholder="按房号或备注搜索"
                :prefix-icon="Search"
                clearable
                style="width: 220px"
                @keyup.enter="searchRooms"
              />
              <el-select v-model="roomQuery.propertyId" placeholder="全部门店" clearable style="width: 170px">
                <el-option v-for="item in properties" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
              <el-select v-model="roomQuery.status" placeholder="全部房态" clearable style="width: 140px">
                <el-option v-for="item in roomStatuses" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="searchRooms">查询</el-button>
              <el-button :icon="Refresh" @click="resetRooms">重置</el-button>
            </div>

            <el-table v-loading="loading" :data="rooms" stripe>
              <el-table-column prop="roomNo" label="房号" width="100">
                <template #default="{ row }"><strong>{{ row.roomNo }}</strong></template>
              </el-table-column>
              <el-table-column prop="propertyName" label="所属门店" min-width="160" />
              <el-table-column prop="roomTypeName" label="房型" min-width="140" />
              <el-table-column prop="floor" label="楼层" width="80" />
              <el-table-column label="房态" width="105">
                <template #default="{ row }">
                  <StatusTag kind="room" :value="row.status" />
                </template>
              </el-table-column>
              <el-table-column prop="notes" label="备注" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="250" fixed="right">
                <template #default="{ row }">
                  <div class="inline-actions">
                    <el-button link type="primary" :icon="SetUp" @click="openStatus(row)">调度</el-button>
                    <el-button link type="primary" :icon="Files" @click="openLogs(row)">记录</el-button>
                    <el-button link type="primary" :icon="Edit" @click="openEditRoom(row)">编辑</el-button>
                    <el-button link type="danger" :icon="Delete" @click="removeRoom(row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="table-pagination">
              <el-pagination
                v-model:current-page="roomQuery.current"
                v-model:page-size="roomQuery.size"
                background
                layout="total, sizes, prev, pager, next"
                :total="roomTotal"
                :page-sizes="[10, 20, 50]"
                @change="loadRooms"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="房型设置" name="types">
            <div class="toolbar">
              <el-input
                v-model="typeQuery.keyword"
                placeholder="按房型名称搜索"
                :prefix-icon="Search"
                clearable
                style="width: 220px"
                @keyup.enter="searchTypes"
              />
              <el-select v-model="typeQuery.propertyId" placeholder="全部门店" clearable style="width: 170px">
                <el-option v-for="item in properties" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="searchTypes">查询</el-button>
            </div>

            <el-table v-loading="loading" :data="roomTypes" stripe>
              <el-table-column prop="name" label="房型名称" min-width="160" />
              <el-table-column label="所属门店" min-width="170">
                <template #default="{ row }">
                  {{ properties.find((item) => item.id === row.propertyId)?.name || '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="capacity" label="可住人数" width="100">
                <template #default="{ row }">{{ row.capacity }} 人</template>
              </el-table-column>
              <el-table-column label="基础价格" width="130">
                <template #default="{ row }"><span class="money">¥ {{ formatMoney(row.basePrice) }}</span></template>
              </el-table-column>
              <el-table-column prop="description" label="房型说明" min-width="230" show-overflow-tooltip />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <StatusTag kind="property" :value="row.status" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <div class="inline-actions">
                    <el-button link type="primary" :icon="Edit" @click="openEditType(row)">编辑</el-button>
                    <el-button link type="danger" :icon="Delete" @click="removeType(row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>

    <el-dialog v-model="roomDialog" :title="editingRoomId ? '编辑房间' : '新增房间'" width="620px" destroy-on-close>
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="所属门店" prop="propertyId">
            <el-select
              v-model="roomForm.propertyId"
              style="width: 100%"
              @change="roomForm.roomTypeId = null"
            >
              <el-option v-for="item in properties" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="房型" prop="roomTypeId">
            <el-select v-model="roomForm.roomTypeId" style="width: 100%">
              <el-option v-for="item in filteredRoomTypes" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="房号" prop="roomNo">
            <el-input v-model="roomForm.roomNo" placeholder="例如：A101" />
          </el-form-item>
          <el-form-item label="楼层">
            <el-input-number v-model="roomForm.floor" :min="1" :max="99" style="width: 100%" />
          </el-form-item>
          <el-form-item label="房间状态" prop="status">
            <el-select v-model="roomForm.status" style="width: 100%">
              <el-option v-for="item in roomStatuses" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item class="span-2" label="备注">
            <el-input v-model="roomForm.notes" type="textarea" :rows="3" placeholder="例如：靠近庭院、无烟房" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roomDialog = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitRoom">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="typeDialog" :title="editingTypeId ? '编辑房型' : '新增房型'" width="620px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="所属门店" prop="propertyId">
            <el-select v-model="typeForm.propertyId" style="width: 100%">
              <el-option v-for="item in properties" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="房型名称" prop="name">
            <el-input v-model="typeForm.name" placeholder="例如：山景大床房" />
          </el-form-item>
          <el-form-item label="可住人数" prop="capacity">
            <el-input-number v-model="typeForm.capacity" :min="1" :max="20" style="width: 100%" />
          </el-form-item>
          <el-form-item label="基础价格（元/晚）" prop="basePrice">
            <el-input-number v-model="typeForm.basePrice" :min="0" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="房型状态" prop="status">
            <el-select v-model="typeForm.status" style="width: 100%">
              <el-option label="启用" value="ACTIVE" />
              <el-option label="停用" value="INACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item class="span-2" label="房型说明">
            <el-input v-model="typeForm.description" type="textarea" :rows="3" placeholder="描述床型、景观和配套服务" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="typeDialog = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitType">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="statusDialog" title="房间调度" width="480px" destroy-on-close>
      <div class="empty-note" style="margin-bottom: 16px">
        {{ selectedRoom?.propertyName }} · {{ selectedRoom?.roomNo }} · {{ selectedRoom?.roomTypeName }}
      </div>
      <el-form ref="statusFormRef" :model="statusForm" label-position="top">
        <el-form-item label="目标房态">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option v-for="item in roomStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="调度说明">
          <el-input v-model="statusForm.notes" type="textarea" :rows="3" placeholder="例如：完成客房清洁，恢复可售" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="statusDialog = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitStatus">确认调度</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="logDrawer" title="房态变更记录" size="min(520px, 92vw)">
      <el-timeline v-if="statusLogs.length">
        <el-timeline-item
          v-for="item in statusLogs"
          :key="item.id"
          :timestamp="item.createdAt"
          placement="top"
        >
          <div style="font-weight: 600">{{ item.beforeStatus || '创建' }} → {{ item.afterStatus }}</div>
          <div class="muted" style="margin-top: 5px">{{ item.operatorName }} · {{ item.notes || '无备注' }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无房态记录" />
    </el-drawer>
  </div>
</template>
