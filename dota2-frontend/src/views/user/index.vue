<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:600">用户列表</span>
        <el-button type="primary" size="small" @click="openDialog()">+ 新增用户</el-button>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe border style="width:100%">
      <el-table-column type="index" label="#" width="56" align="center" />
      <el-table-column prop="userName" label="用户名" min-width="100" />
      <el-table-column prop="nickName" label="昵称" min-width="100" />
      <el-table-column prop="email" label="邮箱" min-width="150" />
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column prop="status" label="状态" width="70" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">{{ row.status === 0 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="150" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createdTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
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

  <el-dialog v-model="visible" :title="isEdit ? '编辑用户' : '新增用户'" width="460px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="75px">
      <el-form-item label="用户名" prop="userName">
        <el-input v-model="form.userName" :disabled="isEdit" />
      </el-form-item>
      <el-form-item label="密码" prop="password" v-if="!isEdit">
        <el-input v-model="form.password" type="password" show-password />
      </el-form-item>
      <el-form-item label="昵称" prop="nickName"><el-input v-model="form.nickName" /></el-form-item>
      <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" /></el-form-item>
      <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="状态" v-if="isEdit">
        <el-radio-group v-model="form.status">
          <el-radio :value="0">启用</el-radio>
          <el-radio :value="1">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageUsers, addUser, editUser, deleteUser } from '@/api/user'
import dayjs from 'dayjs'

const loading = ref(false); const saving = ref(false)
const list = ref([]); const total = ref(0)
const query = reactive({ page: 1, size: 100 })
const visible = ref(false); const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ userName: '', password: '', nickName: '', email: '', phone: '', status: 0 })
const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

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
  try { const r = await pageUsers({ ...query }); list.value = r.data.list; total.value = r.data.total }
  finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row
  Object.assign(form, { userName: '', password: '', nickName: '', email: '', phone: '', status: 0 })
  if (row) { form.userName = row.userName; form.nickName = row.nickName; form.email = row.email || ''; form.phone = row.phone || ''; form.status = row.status; form.id = row.id }
  visible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return; saving.value = true
  try {
    if (isEdit.value) { await editUser({ id: form.id, nickName: form.nickName, email: form.email, phone: form.phone, status: form.status }); ElMessage.success('编辑成功') }
    else { await addUser({ userName: form.userName, password: form.password, nickName: form.nickName, email: form.email, phone: form.phone }); ElMessage.success('新增成功') }
    visible.value = false; fetchData()
  } finally { saving.value = false }
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除用户「' + row.userName + '」？', '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
  await deleteUser({ ids: [row.id] }); ElMessage.success('删除成功'); fetchData()
}
</script>
