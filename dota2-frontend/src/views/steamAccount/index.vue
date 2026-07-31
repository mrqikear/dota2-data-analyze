<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
        <span style="font-weight:600">Steam 账号列表</span>
        <div style="display:flex;align-items:center;gap:8px">
          <span style="font-size:12px;color:#909399">同步日期范围</span>
          <el-date-picker
            v-model="syncDateRange" type="daterange"
            range-separator="至" start-placeholder="开始" end-placeholder="结束"
            size="small" style="width:220px"
            value-format="YYYY-MM-DD"
          />
          <el-button type="primary" size="small" @click="openAddDialog">+ 新增账号</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe border style="width:100%">
      <el-table-column type="index" label="#" width="56" align="center" />
      <el-table-column prop="nickName" label="昵称" min-width="140" />
      <el-table-column prop="steamId" label="Steam ID" min-width="150" />
      <el-table-column label="头像" width="72" align="center">
        <template #default="{ row }">
          <el-avatar :size="40" :src="row.avatar" icon="UserFilled" />
        </template>
      </el-table-column>
      <el-table-column label="资料页" min-width="200">
        <template #default="{ row }">
          <a :href="'https://steamcommunity.com/profiles/' + row.steamId" target="_blank" rel="noopener noreferrer" style="color:#409EFF;text-decoration:none;font-size:13px;cursor:pointer" @click.stop>steamcommunity.com/profiles/{{ row.steamId }}</a>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="150" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createdTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right" align="center">
        <template #default="{ row, $index }">
          <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap">
            <el-button size="small" text type="primary" :loading="syncingMatchId === row.id" :disabled="syncingMatchId !== null" @click="handleSyncMatch(row)">同步</el-button>
            <el-button size="small" text type="success" :loading="syncingTurboId === row.id" :disabled="syncingTurboId !== null" @click="handleSyncTurbo(row)">加速同步</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;text-align:right;display:flex;justify-content:flex-end;align-items:center;gap:12px">
      <div style="display:flex;align-items:center;gap:4px;font-size:13px;color:#606266">
        每页
        <el-input v-model.number="query.size" type="number" :min="1" :max="2000" style="width:80px" size="small" @change="onSizeChange" />
        条
      </div>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total" layout="prev, pager, next, total" small
        @current-change="fetchData"
      />
    </div>
  </el-card>

  <el-dialog v-model="visible" title="新增 Steam 账号" width="480px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="Steam ID" prop="steamId">
        <el-input v-model="form.steamId" placeholder="64位数字ID，如 76561197972495328" />
      </el-form-item>
    </el-form>
    <div v-if="syncedInfo" style="background:#f5f7fa;border-radius:4px;padding:16px;margin-bottom:16px;display:flex;align-items:center;gap:12px">
      <el-avatar :size="48" :src="syncedInfo.avatar" icon="UserFilled" />
      <div>
        <div style="font-weight:500;font-size:14px">{{ syncedInfo.nickName }}</div>
        <div style="font-size:12px;color:#909399;margin-top:4px">Steam ID: {{ syncedInfo.steamId }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="syncing" @click="handleSyncAndAdd">同步并添加</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageAccounts, syncPlayerInfo, addAccount, deleteAccount } from '@/api/steamAccount'
import { syncMatches, syncTurboDate } from '@/api/match'
import dayjs from 'dayjs'

const loading = ref(false)
const list = ref([]); const total = ref(0)
const query = reactive({ page: 1, size: 100 })
const visible = ref(false)
const formRef = ref(null)
const form = reactive({ steamId: '' })
const rules = { steamId: [{ required: true, message: '请输入 Steam ID', trigger: 'blur' }] }
const syncing = ref(false)
const syncedInfo = ref(null)

const syncDateRange = ref(null)
const syncingMatchId = ref(null)
const syncingTurboId = ref(null)

const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'

onMounted(fetchData)

function onSizeChange(val) {
  let v = Number(val)
  if (v < 1) v = 1
  if (v > 2000) {
    v = 2000
    ElMessage.warning('每页最多显示 2000 条')
  }
  query.size = v
  query.page = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try { const r = await pageAccounts({ ...query }); list.value = r.data.list; total.value = r.data.total }
  finally { loading.value = false }
}

function openAddDialog() {
  form.steamId = ''
  syncedInfo.value = null
  visible.value = true
}

async function handleSyncAndAdd() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  syncing.value = true
  try {
    const syncRes = await syncPlayerInfo(form.steamId.trim())
    syncedInfo.value = syncRes.data
    await addAccount({ steamId: form.steamId.trim() })
    ElMessage.success('新增成功！已同步昵称: ' + syncRes.data.nickName)
    visible.value = false
    fetchData()
  } catch (e) {
  } finally {
    syncing.value = false
  }
}

async function handleSyncMatch(row) {
  syncingMatchId.value = row.id
  try {
    const range = syncDateRange.value
    const minDate = range ? range[0] : null
    const maxDate = range ? range[1] : null
    await syncMatches(row.steamId, null, minDate, maxDate)
    ElMessage.success('同步任务已启动，后台正在拉取所有比赛记录...')
    setTimeout(() => {
      syncingMatchId.value = null
      fetchData()
      window.dispatchEvent(new CustomEvent('match-synced'))
    }, 2000)
  } catch (e) {
    syncingMatchId.value = null
  }
}

async function handleSyncTurbo(row) {
  syncingTurboId.value = row.id
  try {
    const range = syncDateRange.value
    let minTime = null, maxTime = null
    if (range) {
      minTime = Math.floor(new Date(range[0]).getTime() / 1000)
      maxTime = Math.floor(new Date(range[1]).getTime() / 1000) + 86399
    }
    await syncTurboDate(row.steamId, minTime, maxTime)
    ElMessage.success('加速模式同步任务已启动')
    setTimeout(() => { syncingTurboId.value = null; fetchData() }, 2000)
  } catch (e) {
    syncingTurboId.value = null
    ElMessage.error('同步失败: ' + (e.response?.data?.message || e.message))
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除 Steam 账号「' + row.nickName + '」？', '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
  await deleteAccount({ ids: [row.id] })
  ElMessage.success('删除成功')
  fetchData()
}
</script>
