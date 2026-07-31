<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="login-brand">
          <div class="brand-icon">
            <el-icon :size="40" color="#409EFF"><DataAnalysis /></el-icon>
          </div>
          <div class="brand-title">Dota2 Data Analyzer</div>
          <div class="brand-subtitle">数据分析 · 英雄统计 · 出装推荐</div>
        </div>
      </div>
      <div class="login-divider"></div>
      <div class="login-right">
        <div class="login-form-wrapper">
          <div class="form-header">
            <div class="form-title">系统登录</div>
            <div class="form-desc">WELCOME TO LOGIN</div>
          </div>
          <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-dark-input">
            <el-form-item prop="userName">
              <el-input v-model="form.userName" placeholder="用户名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" show-password>
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item style="margin-top:36px">
              <el-button type="primary" :loading="loading" style="width:100%;border-radius:0;height:40px" @click="handleLogin">登 录</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/user'
import { setToken, setUser } from '@/utils/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ userName: 'admin', password: 'admin123' })
const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await login(form)
    setToken(res.data.token)
    setUser(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: #1a1a2e;
  position: relative; overflow: hidden;
}
.login-page::before {
  content: ''; position: absolute; inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' xmlns='http://www.w3.org/2000/svg'%3E%3Cdefs%3E%3Cpattern id='g' patternUnits='userSpaceOnUse' width='60' height='60'%3E%3Ccircle cx='30' cy='30' r='0.5' fill='rgba(255,255,255,0.06)'/%3E%3C/pattern%3E%3C/defs%3E%3Crect width='100%25' height='100%25' fill='url(%23g)'/%3E%3C/svg%3E");
  background-size: 60px 60px;
}
.login-container {
  position: relative; z-index: 1;
  display: flex; width: 800px; min-height: 440px;
  background: rgba(255,255,255,.06); backdrop-filter: blur(20px);
  border-radius: 8px; border: 1px solid rgba(255,255,255,.08);
}
.login-left {
  flex: 1; display: flex; align-items: center; justify-content: center;
  padding: 40px;
}
.login-brand { text-align: center; }
.brand-icon { margin-bottom: 16px; }
.brand-title { font-size: 22px; font-weight: 700; color: #fff; margin-bottom: 8px; letter-spacing: 2px; }
.brand-subtitle { font-size: 13px; color: rgba(255,255,255,.5); letter-spacing: 1px; }
.login-divider {
  width: 1px; align-self: stretch;
  background: linear-gradient(to bottom, transparent, rgba(255,255,255,.1), transparent);
}
.login-right {
  width: 360px; display: flex; align-items: center; justify-content: center;
  padding: 40px;
}
.login-form-wrapper { width: 100%; }
.form-header { margin-bottom: 32px; }
.form-title { font-size: 24px; font-weight: 700; color: #fff; line-height: 1.2; }
.form-desc { font-size: 12px; color: rgba(255,255,255,.4); margin-top: 6px; letter-spacing: 1px; }
</style>
