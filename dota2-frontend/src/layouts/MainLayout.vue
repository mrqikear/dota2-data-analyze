<template>
  <el-container style="height:100vh">
    <!-- Sidebar -->
    <el-aside width="220px" class="dark-sidebar" style="background:var(--sidebar-bg);overflow:hidden">
      <div class="sidebar-logo">Dota2 Analyzer</div>
      <el-menu
        :default-active="activeMenu"
        background-color="#1d2228"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon><span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/steamAccount">
          <el-icon><Connection /></el-icon><span>Steam 账号</span>
        </el-menu-item>
        <el-sub-menu index="match">
          <template #title>
            <el-icon><TrophyBase /></el-icon><span>比赛记录</span>
          </template>
          <el-menu-item index="/match">比赛列表</el-menu-item>
          <el-menu-item index="/match/relatedMatches">关联比赛</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="analysis">
          <template #title>
            <el-icon><Histogram /></el-icon><span>数据分析</span>
          </template>
          <el-menu-item index="/analysis/heroOverview">英雄概览</el-menu-item>
          <el-menu-item index="/analysis/itemAnalysis">出装分析</el-menu-item>
          <el-menu-item index="/analysis/allHeroWinRate">全英雄胜率</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- Header -->
      <el-header class="dark-header" style="display:flex;align-items:center;justify-content:space-between;height:var(--header-height);padding:0 20px">
        <span style="font-size:15px;font-weight:500;color:rgba(255,255,255,.85)">{{ route.meta?.title }}</span>
        <el-dropdown trigger="click" @command="handleCommand">
          <span style="cursor:pointer;display:flex;align-items:center;gap:8px;color:rgba(255,255,255,.85)">
            <el-avatar :size="28" icon="UserFilled" style="background:#5d7092" />
            {{ user?.nickName || user?.userName }}
          </span>
          <template #dropdown>
            <el-dropdown-menu><el-dropdown-item command="logout">退出登录</el-dropdown-item></el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- Main -->
      <el-main style="background:var(--main-bg);padding:20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUser, removeToken } from '@/utils/auth'
import { DataAnalysis, User, Connection, TrophyBase, UserFilled, Histogram } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const user = getUser()
const activeMenu = computed(() => route.path)

function handleCommand(cmd) {
  if (cmd === 'logout') { removeToken(); router.push('/login') }
}
</script>

<style scoped>
.sidebar-logo {
  height: var(--header-height); line-height: var(--header-height); text-align: center;
  color: #fff; font-size: 16px; font-weight: 600; letter-spacing: 1px;
  border-bottom: 1px solid rgba(255,255,255,.08);
}
</style>
