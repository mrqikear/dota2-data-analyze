<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="(item, i) in cards" :key="i">
        <el-card shadow="never" :body-style="{ padding: '20px' }">
          <div style="display:flex;align-items:center;gap:16px">
            <div :style="{ width:'48px', height:'48px', borderRadius:'8px', background:item.bg, display:'flex', alignItems:'center', justifyContent:'center' }">
              <el-icon :size="24" color="#fff"><component :is="item.icon" /></el-icon>
            </div>
            <div>
              <div style="font-size:22px;font-weight:700;color:#303133">{{ item.value }}</div>
              <div style="font-size:13px;color:#909399;margin-top:2px">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header><span style="font-weight:600">快速入口</span></template>
          <el-row :gutter="16">
            <el-col :span="12" v-for="(entry, i) in entries" :key="i">
              <el-card shadow="never" :body-style="{ padding: '16px', cursor: 'pointer' }" @click="$router.push(entry.path)" style="margin-bottom:12px">
                <div style="display:flex;align-items:center;gap:12px">
                  <el-icon :size="28" :color="entry.color"><component :is="entry.icon" /></el-icon>
                  <div>
                    <div style="font-size:14px;font-weight:500">{{ entry.label }}</div>
                    <div style="font-size:12px;color:#909399;margin-top:2px">{{ entry.desc }}</div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span style="font-weight:600">系统信息</span></template>
          <div v-for="(info, i) in sysInfo" :key="i" style="display:flex;justify-content:space-between;padding:6px 0;font-size:13px">
            <span style="color:#909399">{{ info.label }}</span>
            <span style="color:#303133">{{ info.value }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { User, Connection, Trophy, TrendCharts } from '@element-plus/icons-vue'

const cards = [
  { label: '系统用户', value: '-', icon: User, bg: '#409EFF' },
  { label: 'Steam 账号', value: '-', icon: Connection, bg: '#67C23A' },
  { label: '已分析比赛', value: '-', icon: Trophy, bg: '#E6A23C' },
  { label: '监控英雄', value: '-', icon: TrendCharts, bg: '#F56C6C' }
]
const entries = [
  { label: '用户管理', desc: '管理系统用户账号', path: '/user', icon: User, color: '#409EFF' },
  { label: 'Steam 账号', desc: '管理绑定的 Steam 账号', path: '/steamAccount', icon: Connection, color: '#67C23A' }
]
import { getUser } from '@/utils/auth'
const sysInfo = [
  { label: '当前用户', value: getUser()?.nickName || '-' },
  { label: '系统版本', value: 'v1.0.0' },
  { label: '后端端口', value: '9601' },
  { label: '数据库', value: 'PostgreSQL 16' }
]
</script>
