<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
        <span style="font-weight:600">全英雄胜率（OpenDota 全球数据）</span>
        <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap">
          <el-radio-group v-model="modeFilter" size="small" style="margin-right:4px">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="pub">普通+天梯</el-radio-button>
            <el-radio-button value="turbo">加速模式</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="days" size="small" @change="fetchData">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="3">近3天</el-radio-button>
            <el-radio-button :value="5">近5天</el-radio-button>
            <el-radio-button :value="7">近7天</el-radio-button>
          </el-radio-group>
          <el-button type="primary" size="small" :loading="loading" @click="fetchData">刷新</el-button>
        </div>
      </div>
    </template>

    <!-- Stats bar -->
    <div style="margin-bottom:12px;font-size:13px;color:#909399;display:flex;gap:16px;flex-wrap:wrap">
      <span>共 <strong>{{ mergedList.length }}</strong> 个英雄</span>
      <el-tag v-if="days" type="info" size="small" effect="plain">最近 {{ days }} 天</el-tag>
    </div>

    <!-- Sort tabs -->
    <div style="margin-bottom:12px">
      <el-radio-group v-model="sortBy" size="small" @change="doSort">
        <el-radio-button value="games">按出场</el-radio-button>
        <el-radio-button value="winRateDesc">胜率↓</el-radio-button>
        <el-radio-button value="winRateAsc">胜率↑</el-radio-button>
        <el-radio-button value="name">按名称</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Hero grid -->
    <div v-loading="loading" style="display:flex;flex-wrap:wrap;gap:6px">
      <div v-for="(h, idx) in sortedList" :key="h.heroId" class="hero-card">
        <div class="hero-rank">{{ idx + 1 }}</div>
        <el-image :src="heroIconUrl(h.heroId)" style="width:36px;height:36px;border-radius:4px" />
        <div class="hero-info">
          <div class="hero-name">{{ h.heroName }}</div>
          <div v-if="modeFilter==='all'" style="display:flex;gap:10px;margin-top:2px">
            <div style="flex:1">
              <div style="font-size:10px;color:#909399">普通+天梯</div>
              <div class="hero-winrate" :style="{ color: winRateColor(h.pubWinRate) }">{{ h.pubWinRate }}%</div>
              <div class="hero-bar"><div class="hero-bar-fill" :style="{ width: h.pubWinRate + '%', background: winRateColor(h.pubWinRate) }" /></div>
              <div class="hero-games">{{ formatNum(h.pubGames) }}场</div>
            </div>
            <div style="flex:1">
              <div style="font-size:10px;color:#E6A23C">加速模式</div>
              <div style="display:flex;align-items:center;gap:4px">
                <div class="hero-winrate" :style="{ color: winRateColor(h.turboWinRate) }">{{ h.turboWinRate }}%</div>
                <span v-if="modeFilter==='all' && h.pubGames && h.turboGames"
                  :style="{ fontSize:'9px', fontWeight:700, color: h.turboWinRate > h.pubWinRate ? '#67C23A' : '#F56C6C' }">
                  {{ h.turboWinRate > h.pubWinRate ? '▲' : '▼' }}{{ Math.abs(Math.round((h.turboWinRate - h.pubWinRate)*10)/10) }}
                </span>
              </div>
              <div class="hero-bar"><div class="hero-bar-fill" :style="{ width: h.turboWinRate + '%', background: winRateColor(h.turboWinRate) }" /></div>
              <div class="hero-games">{{ formatNum(h.turboGames) }}场</div>
            </div>
          </div>
          <div v-else style="margin-top:2px">
            <div v-if="modeFilter==='pub'" style="flex:1">
              <div class="hero-winrate" :style="{ color: winRateColor(h.pubWinRate) }">{{ h.pubWinRate }}%</div>
              <div class="hero-bar"><div class="hero-bar-fill" :style="{ width: h.pubWinRate + '%', background: winRateColor(h.pubWinRate) }" /></div>
              <div class="hero-games">{{ formatNum(h.pubGames) }}场</div>
            </div>
            <div v-if="modeFilter==='turbo'" style="flex:1">
              <div class="hero-winrate" :style="{ color: winRateColor(h.turboWinRate) }">{{ h.turboWinRate }}%</div>
              <div class="hero-bar"><div class="hero-bar-fill" :style="{ width: h.turboWinRate + '%', background: winRateColor(h.turboWinRate) }" /></div>
              <div class="hero-games">{{ formatNum(h.turboGames) }}场</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !mergedList.length" description="暂无数据" />
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { allHeroWinRate } from '@/api/analysis'

const loading = ref(false)
const pubList = ref([])
const turboList = ref([])
const days = ref(null)
const sortBy = ref('games')
const modeFilter = ref('all')

const HERO_EN = {
  1:'antimage',2:'axe',3:'bane',4:'bloodseeker',5:'crystal_maiden',
  6:'drow_ranger',7:'earthshaker',8:'juggernaut',9:'mirana',10:'morphling',
  11:'nevermore',12:'phantom_lancer',13:'puck',14:'pudge',15:'razor',
  16:'sand_king',17:'storm_spirit',18:'sven',19:'tiny',20:'vengefulspirit',
  21:'windrunner',22:'zuus',23:'kunkka',25:'lina',26:'lion',
  27:'shadow_shaman',28:'slardar',29:'tidehunter',30:'witch_doctor',31:'lich',
  32:'riki',33:'enigma',34:'tinker',35:'sniper',36:'necrolyte',
  37:'warlock',38:'beastmaster',39:'queenofpain',40:'venomancer',41:'faceless_void',
  42:'skeleton_king',43:'death_prophet',44:'phantom_assassin',45:'pugna',46:'templar_assassin',
  47:'viper',48:'luna',49:'dragon_knight',50:'dazzle',51:'rattletrap',
  52:'leshrac',53:'furion',54:'life_stealer',55:'dark_seer',56:'clinkz',
  57:'omniknight',58:'enchantress',59:'huskar',60:'night_stalker',61:'broodmother',
  62:'bounty_hunter',63:'weaver',64:'jakiro',65:'batrider',66:'chen',
  67:'spectre',68:'ancient_apparition',69:'doom_bringer',70:'ursa',71:'spirit_breaker',
  72:'gyrocopter',73:'alchemist',74:'invoker',75:'silencer',76:'obsidian_destroyer',
  77:'lycan',78:'brewmaster',79:'shadow_demon',80:'lone_druid',81:'chaos_knight',
  82:'meepo',83:'treant',84:'ogre_magi',85:'undying',86:'rubick',
  87:'disruptor',88:'nyx_assassin',89:'naga_siren',90:'keeper_of_the_light',91:'wisp',
  92:'visage',93:'slark',94:'medusa',95:'troll_warlord',96:'centaur',
  97:'magnataur',98:'shredder',99:'bristleback',100:'tusk',101:'skywrath_mage',
  102:'abaddon',103:'elder_titan',104:'legion_commander',105:'techies',106:'ember_spirit',
  107:'earth_spirit',108:'abyssal_underlord',109:'terrorblade',110:'phoenix',111:'oracle',
  112:'winter_wyvern',113:'arc_warden',114:'monkey_king',119:'dark_willow',120:'pangolier',
  121:'grimstroke',123:'hoodwink',126:'void_spirit',128:'snapfire',129:'mars',
  131:'ringmaster',135:'dawnbreaker',136:'marci',137:'primal_beast',138:'muerta',
  145:'kez',155:'largo'
}

// Merge pub and turbo data by heroId
const mergedList = computed(() => {
  const map = new Map()
  for (const h of pubList.value) {
    map.set(h.heroId, { ...h, pubGames: h.games, pubWinRate: h.winRate, turboGames: 0, turboWinRate: 0 })
  }
  for (const h of turboList.value) {
    const existing = map.get(h.heroId)
    if (existing) {
      existing.turboGames = h.games
      existing.turboWinRate = h.winRate
    } else {
      map.set(h.heroId, { heroId: h.heroId, heroName: h.heroName, pubGames: 0, pubWinRate: 0, turboGames: h.games, turboWinRate: h.winRate })
    }
  }
  return Array.from(map.values())
})

const totalGames = computed(() => mergedList.value.reduce((s, h) => s + Number(h.pubGames) + Number(h.turboGames), 0))

const sortedList = computed(() => {
  const arr = [...mergedList.value]
  const isPub = modeFilter.value === 'pub'
  const isTurbo = modeFilter.value === 'turbo'
  switch (sortBy.value) {
    case 'winRateDesc':
      if (isPub) arr.sort((a, b) => b.pubWinRate - a.pubWinRate)
      else arr.sort((a, b) => b.turboWinRate - a.turboWinRate)
      break
    case 'winRateAsc':
      if (isPub) arr.sort((a, b) => a.pubWinRate - b.pubWinRate)
      else arr.sort((a, b) => a.turboWinRate - b.turboWinRate)
      break
    case 'name': arr.sort((a, b) => a.heroName.localeCompare(b.heroName)); break
    default:
      if (isPub) arr.sort((a, b) => b.pubGames - a.pubGames)
      else if (isTurbo) arr.sort((a, b) => b.turboGames - a.turboGames)
      else arr.sort((a, b) => (b.pubGames + b.turboGames) - (a.pubGames + a.turboGames))
      break
  }
  return arr
})

function heroIconUrl(id) { const e = HERO_EN[id]; return e ? `/asset/hero/${e}.png` : '' }
function winRateColor(r) { return r >= 55 ? '#67C23A' : r >= 48 ? '#E6A23C' : '#F56C6C' }
function formatNum(n) { if (!n) return '0'; if (n >= 10000) return (n / 10000).toFixed(2) + '万'; return String(n) }
function doSort() {}

async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (days.value) params.days = days.value
    const r = await allHeroWinRate(params)
    pubList.value = r.data?.pub || []
    turboList.value = r.data?.turbo || []
  } catch (e) {
    ElMessage.error('获取失败: ' + (e.message || e))
  } finally { loading.value = false }
}

onMounted(() => fetchData())
</script>

<style scoped>
.hero-card {
  display:flex;align-items:center;gap:6px;
  padding:5px 8px;border-radius:6px;
  border:1px solid #ebeef5;background:#fff;
  width:calc(20% - 6px);min-width:200px;
  transition:all .15s;cursor:default;
}
.hero-card:hover { border-color:#409EFF;box-shadow:0 2px 8px rgba(64,158,255,.15); }
.hero-rank { font-size:11px;font-weight:700;color:#C0C4CC;min-width:18px;text-align:center; }
.hero-info { flex:1;min-width:0; }
.hero-name { font-size:12px;font-weight:500;color:#303133; }
.hero-winrate { font-size:12px;font-weight:700;margin:1px 0; }
.hero-bar { height:3px;background:#f0f0f0;border-radius:2px;overflow:hidden; }
.hero-bar-fill { height:100%;border-radius:2px;transition:width .3s; }
.hero-games { font-size:9px;color:#909399; }
</style>
