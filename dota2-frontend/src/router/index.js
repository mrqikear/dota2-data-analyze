import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '仪表盘' } },
      { path: 'user', name: 'User', component: () => import('@/views/user/index.vue'), meta: { title: '用户管理' } },
      { path: 'steamAccount', name: 'SteamAccount', component: () => import('@/views/steamAccount/index.vue'), meta: { title: 'Steam 账号' } },
      { path: 'match', name: 'Match', component: () => import('@/views/match/index.vue'), meta: { title: '比赛列表' } },
      { path: 'match/relatedMatches', name: 'RelatedMatches', component: () => import('@/views/match/relatedMatches/index.vue'), meta: { title: '关联比赛' } },
      { path: 'match/detail/:matchId', name: 'MatchDetail', component: () => import('@/views/matchDetail/index.vue'), meta: { title: '比赛详情' } },
      { path: 'analysis/heroOverview', name: 'HeroOverview', component: () => import('@/views/analysis/heroOverview/index.vue'), meta: { title: '英雄概览' } },
      { path: 'analysis/itemAnalysis', name: 'ItemAnalysis', component: () => import('@/views/analysis/itemAnalysis/index.vue'), meta: { title: '出装分析' } },
      { path: 'analysis/allHeroWinRate', name: 'AllHeroWinRate', component: () => import('@/views/analysis/allHeroWinRate/index.vue'), meta: { title: '全英雄胜率' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to, _, next) => {
  if (!to.meta.noAuth && !isLoggedIn()) next('/login')
  else next()
})
export default router
