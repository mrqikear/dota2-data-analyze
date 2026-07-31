<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:600">关联比赛</span>
        <el-radio-group v-model="relationType" size="small">
          <el-radio-button value="teammate">队友关联</el-radio-button>
          <el-radio-button value="opponent">对手关联</el-radio-button>
          <el-radio-button value="solo">非关联(单排)</el-radio-button>
        </el-radio-group>
      </div>
    </template>

    <!-- Account multi-select -->
    <div style="margin-bottom:12px">
      <el-checkbox-group v-model="selectedIds" size="small">
        <el-checkbox v-for="a in accounts" :key="a.steamId" :label="a.steamId" border>
          <div style="display:flex;align-items:center;gap:6px">
            <el-image v-if="a.avatar" :src="a.avatar" style="width:20px;height:20px;border-radius:50%" />
            <span>{{ a.nickName }}</span>
            <span style="color:#909399;font-size:11px">({{ a.steamId.slice(-6) }})</span>
          </div>
        </el-checkbox>
      </el-checkbox-group>
    </div>

    <div style="margin-bottom:12px;display:flex;gap:10px;align-items:center">
      <el-button type="primary" :loading="loading" @click="fetchData" :disabled="relationType==='teammate' && selectedIds.length<2">
        查询
      </el-button>
      <el-input v-if="relationType==='opponent'" v-model="opponentId" placeholder="输入对手的 Steam ID" size="small" style="width:320px" clearable />

      <span style="color:#909399;font-size:12px">{{ hintText }}</span>
    </div>

    <!-- Summary table -->
    <!-- Solo mode: show overall stats from backend -->
    <div v-if="relationType==='solo' && soloStatsData" style="margin-bottom:16px;padding:10px 14px;border-radius:6px;border:1px solid #ebeef5;background:#fafafa">
      <div style="font-weight:600;font-size:14px;margin-bottom:6px">🏆 整体统计</div>
      <div style="display:flex;gap:20px;flex-wrap:wrap;font-size:13px">
        <span>总场次 <strong>{{ soloStatsData.total }}</strong></span>
        <span style="color:#67C23A">胜 <strong>{{ soloStatsData.wins }}</strong></span>
        <span style="color:#F56C6C">负 <strong>{{ soloStatsData.losses }}</strong></span>
        <span>胜率 <strong :style="{color: soloStatsData.winRate >= 50 ? '#67C23A' : '#F56C6C'}">{{ soloStatsData.winRate }}%</strong></span>
        <span style="color:#E6A23C">🏆 MVP <strong>{{ soloStatsData.mvp || 0 }}</strong></span>
        <span style="color:#909399">🥈 FMVP <strong>{{ soloStatsData.fmvp || 0 }}</strong></span>
      </div>
    </div>

    <!-- Non-solo: show page-level summary from backend -->
    <div v-if="relationType!=='solo' && summaryList.length > 0" style="margin-bottom:16px">
      <div style="font-weight:600;font-size:14px;margin-bottom:8px">🏆 关联账号 MVP/FMVP 统计</div>
      <el-table :data="summaryList" stripe border size="small">
        <el-table-column type="expand" width="40" align="center">
          <template #default="{ row }">
            <div style="padding:8px 16px">
              <div style="font-weight:600;font-size:13px;margin-bottom:6px">各英雄 MVP/FMVP 明细</div>
              <div v-if="row.heroes && row.heroes.length" style="display:flex;flex-wrap:wrap;gap:8px">
                <div v-for="h in row.heroes" :key="h.heroId" style="display:flex;align-items:center;gap:6px;padding:4px 10px;border-radius:6px;background:#f5f7fa;border:1px solid #ebeef5">
                  <el-image :src="heroIcon(h.heroId)" style="width:28px;height:28px;border-radius:4px" />
                  <div style="font-size:12px">
                    <div style="font-weight:500">{{ heroName(h.heroId) }}</div>
                    <div style="display:flex;gap:6px;color:#909399">
                      <span style="color:#E6A23C">MVP {{ h.mvp }}</span>
                      <span style="color:#909399">FMVP {{ h.fmvp }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else style="color:#909399;font-size:12px">暂无明细数据</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="#" type="index" width="50" align="center" />
        <el-table-column label="账号" min-width="120">
          <template #default="{ row }">{{ row.nickName || row.steamId.slice(-6) }}</template>
        </el-table-column>
        <el-table-column label="MVP" width="80" align="center" prop="mvp" sortable>
          <template #default="{ row }"><span style="color:#E6A23C;font-weight:700">{{ row.mvp }}次</span></template>
        </el-table-column>
        <el-table-column label="FMVP" width="80" align="center" prop="fmvp" sortable>
          <template #default="{ row }"><span style="color:#909399;font-weight:700">{{ row.fmvp }}次</span></template>
        </el-table-column>
        <el-table-column label="合计" width="80" align="center" prop="total" sortable>
          <template #default="{ row }"><strong>{{ row.total }}次</strong></template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Filters (always visible when data exists) -->
    <div v-if="rawList.length > 0" style="margin-bottom:12px;font-size:13px;display:flex;gap:16px;flex-wrap:wrap;align-items:center">
      <span>共 <strong v-if="relationType==='solo'">{{ soloTotal }}</strong><strong v-else>{{ rawList.length }}</strong> 场<template v-if="filteredList.length < rawList.length">（筛选后 <strong>{{ filteredList.length }}</strong> 场）</template></span>
      <span v-if="relationType!=='solo'" style="color:#67C23A">胜 <strong>{{ stats.wins }}</strong></span>
      <span v-if="relationType!=='solo'" style="color:#F56C6C">负 <strong>{{ stats.losses }}</strong></span>
      <span v-if="relationType!=='solo'" style="color:#E6A23C">混合 <strong>{{ stats.mixed }}</strong></span>
      <span v-if="relationType!=='solo'">组排胜率 <strong :style="{color: stats.winRate >= 50 ? '#67C23A' : '#F56C6C'}">{{ stats.winRate }}%</strong></span>
      <el-select v-model="modeFilter" placeholder="模式" size="small" style="width:100px" clearable @change="onModeFilterChange">
        <el-option label="全部模式" value="" />
        <el-option v-for="(v,k) in MODES" :key="k" :label="v" :value="k" />
      </el-select>
      <el-select v-model="winFilter" placeholder="胜负" size="small" style="width:100px" clearable>
        <el-option label="全部" value="" />
        <el-option label="胜场" value="win" />
        <el-option label="负场" value="lose" />
        <el-option label="混合" value="mixed" />
      </el-select>
    </div>

    <!-- Results table -->
    <template v-if="filteredList.length > 0">
      <el-table :data="filteredList" stripe border size="small" v-loading="loading">
        <el-table-column label="比赛ID" width="110" align="center">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="$router.push('/match/detail/' + row.matchId)">{{ row.matchId }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="130" align="center">
          <template #default="{ row }">
            <div style="display:flex;flex-direction:column;align-items:center;gap:2px">
              <el-tag :type="resultTag(row).type" size="small" effect="dark">{{ resultTag(row).label }}</el-tag>
              <div style="display:flex;gap:4px;font-size:10px;white-space:nowrap">
                <span v-if="row.mvp && row.mvp.steamId" style="color:#E6A23C;font-weight:700">MVP{{ row.mvp.score ? '('+row.mvp.score+')' : '' }}</span>
                <span v-if="row.fmvp && row.fmvp.steamId" style="color:#909399;font-weight:700">FMVP{{ row.fmvp.score ? '('+row.fmvp.score+')' : '' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="模式" width="80" align="center">
          <template #default="{ row }">{{ modeLabel(row.gameMode) }}</template>
        </el-table-column>
        <el-table-column label="时长" width="70" align="center">
          <template #default="{ row }">{{ fmtDuration(row.duration) }}</template>
        </el-table-column>
        <el-table-column label="参赛账号" min-width="280">
          <template #default="{ row }">
            <div style="display:flex;gap:6px;flex-wrap:wrap">
              <div v-for="p in trackedPlayers(row)" :key="p.steamId"
                style="display:flex;align-items:center;gap:3px;padding:2px 6px;border-radius:4px;background:#f0f5ff">
                <div style="position:relative">
                  <el-image :src="heroIcon(p.heroId)" style="width:20px;height:20px;border-radius:3px" />
                  <span v-if="row.mvp && row.mvp.steamId === p.steamId" style="position:absolute;top:-6px;right:-6px;background:#E6A23C;color:#fff;font-size:8px;padding:0 3px;border-radius:2px;font-weight:700;line-height:12px">M</span>
                  <span v-if="row.fmvp && row.fmvp.steamId === p.steamId" style="position:absolute;top:-6px;right:-6px;background:#909399;color:#fff;font-size:8px;padding:0 3px;border-radius:2px;font-weight:700;line-height:12px">F</span>
                </div>
                <span style="font-size:11px;font-weight:500">{{ p.nickName || p.steamId.slice(-6) }}</span>
                <el-tag :type="p.win ? 'success' : 'danger'" size="small" effect="dark">{{ p.win ? '胜' : '负' }}</el-tag>
                <span style="font-size:10px;color:#909399">{{ p.kills }}/{{ p.deaths }}/{{ p.assists }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="全部玩家" min-width="100">
          <template #default="{ row }">
            <el-popover placement="left" :width="320" trigger="hover">
              <template #reference>
                <el-tag size="small" effect="plain" style="cursor:pointer">{{ row.players.length }}人</el-tag>
              </template>
              <div style="display:flex;flex-direction:column;gap:4px">
                <div v-for="p in row.players" :key="p.steamId"
                  style="display:flex;align-items:center;gap:6px;padding:2px 0;font-size:12px"
                  :style="{ background: p.isTracked ? '#f0f5ff' : 'transparent' }">
                  <el-image :src="heroIcon(p.heroId)" style="width:20px;height:20px;border-radius:3px" />
                  <span style="flex:1">{{ p.nickName || p.steamId.slice(-6) }}</span>
                  <span style="color:#909399;min-width:50px">{{ p.kills }}/{{ p.deaths }}/{{ p.assists }}</span>
                  <el-tag v-if="p.isTracked" size="small" type="primary" effect="dark">跟踪</el-tag>
                </div>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="140" align="center">
          <template #default="{ row }">{{ fmtTime(row.startTime) }}</template>
        </el-table-column>
      </el-table>
    </template>

    <el-empty v-if="!loading && queried && !rawList.length" description="未找到关联比赛" />

    <!-- Pagination for solo mode -->
    <div v-if="relationType==='solo' && soloTotal > rawList.length" style="margin-top:12px;display:flex;justify-content:center;align-items:center;gap:8px;font-size:13px">
      <el-button size="small" :disabled="soloPage <= 1" @click="goSoloPage(soloPage - 1)">上一页</el-button>
      <span style="color:#909399">第 {{ soloPage }} / {{ maxSoloPage }} 页</span>
      <el-button size="small" :disabled="soloPage >= maxSoloPage" @click="goSoloPage(soloPage + 1)">下一页</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAll } from '@/api/steamAccount'
import { relatedMatches } from '@/api/match'
import dayjs from 'dayjs'

const accounts = ref([])
const selectedIds = ref([])
const relationType = ref('teammate')
const opponentId = ref('')
const rawList = ref([])
const backendSummary = ref([])
const loading = ref(false)
const queried = ref(false)
const winFilter = ref('')
const modeFilter = ref('')
const soloPage = ref(1)
const soloTotal = ref(0)
const soloStatsData = ref(null)

const MODES = { 1:'全英雄',2:'队长',3:'随机',4:'单一',5:'全随机',22:'天梯',23:'加速' }
const HEROES = {
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

// ---- Computed stats ----
const stats = computed(() => {
  const total = filteredList.value.length
  let wins = 0, losses = 0, mixed = 0
  for (const m of filteredList.value) {
    const tracked = (m.players || []).filter(p => p.isTracked)
    const allWin = tracked.every(p => p.win)
    const allLose = tracked.every(p => !p.win)
    if (allWin) wins++
    else if (allLose) losses++
    else mixed++
  }
  return {
    total, wins, losses, mixed,
    winRate: total > 0 ? Math.round(100.0 * wins / total * 10) / 10 : 0
  }
})

const summaryList = computed(() => {
  const map = {} // steamId → { steamId, nickName, mvp, fmvp, heroes: {heroId: {mvp, fmvp}} }
  const nickMap = {} // steamId → nickName
  for (const m of filteredList.value) {
    for (const p of (m.players || []).filter(p => p.isTracked)) {
      if (!map[p.steamId]) map[p.steamId] = { steamId: p.steamId, mvp: 0, fmvp: 0, heroes: {} }
      if (p.nickName) nickMap[p.steamId] = p.nickName
      // Count MVP/FMVP from match-level mvp/fmvp steamId
      if (m.mvp && m.mvp.steamId === p.steamId) { map[p.steamId].mvp++; addHero(map[p.steamId].heroes, p.heroId, 'mvp') }
      if (m.fmvp && m.fmvp.steamId === p.steamId) { map[p.steamId].fmvp++; addHero(map[p.steamId].heroes, p.heroId, 'fmvp') }
    }
  }
  const list = Object.values(map).map(s => ({
    ...s,
    heroes: Object.values(s.heroes),
    nickName: nickMap[s.steamId] || '',
    total: s.mvp + s.fmvp
  }))
  list.sort((a, b) => b.total - a.total)
  return list
})

function addHero(heroes, heroId, type) {
  if (!heroes[heroId]) heroes[heroId] = { heroId, mvp: 0, fmvp: 0 }
  heroes[heroId][type]++
}

const filteredList = computed(() => {
  return rawList.value.filter(m => {
    // Mode filter
    if (modeFilter.value && Number(m.gameMode) !== Number(modeFilter.value)) return false
    // Win filter
    if (winFilter.value) {
      const tracked = (m.players || []).filter(p => p.isTracked)
      const allWin = tracked.every(p => p.win)
      const allLose = tracked.every(p => !p.win)
      if (winFilter.value === 'win' && !allWin) return false
      if (winFilter.value === 'lose' && !allLose) return false
      if (winFilter.value === 'mixed' && (allWin || allLose)) return false
    }
    return true
  })
})

// ---- Helpers ----
function resultTag(row) {
  const tracked = (row.players || []).filter(p => p.isTracked)
  const allWin = tracked.every(p => p.win)
  const allLose = tracked.every(p => !p.win)
  if (allWin) return { type: 'success', label: '胜' }
  if (allLose) return { type: 'danger', label: '负' }
  return { type: 'warning', label: '混' }
}
function trackedPlayers(row) { return (row.players || []).filter(p => p.isTracked) }
const HERO_CN = {1:'敌法师',2:'斧王',3:'痛苦之源',4:'嗜血狂魔',5:'水晶室女',6:'卓尔游侠',7:'撼地者',8:'主宰',9:'米拉娜',10:'变体精灵',11:'影魔',12:'幻影长矛手',13:'帕克',14:'帕吉',15:'雷泽',16:'沙王',17:'风暴之灵',18:'斯温',19:'小小',20:'复仇之魂',21:'风行者',22:'宙斯',23:'昆卡',25:'莉娜',26:'莱恩',27:'暗影萨满',28:'大鱼人',29:'潮汐猎人',30:'巫医',31:'巫妖',32:'力丸',33:'谜团',34:'修补匠',35:'狙击手',36:'死灵法师',37:'术士',38:'兽王',39:'痛苦女王',40:'剧毒术士',41:'虚空假面',42:'骷髅王',43:'死亡先知',44:'幻影刺客',45:'帕格纳',46:'圣堂刺客',47:'冥界亚龙',48:'月之骑士',49:'龙骑士',50:'戴泽',51:'发条技师',52:'拉席克',53:'先知',54:'噬魂鬼',55:'黑暗贤者',56:'克林克兹',57:'全能骑士',58:'魅惑魔女',59:'哈斯卡',60:'暗夜魔王',61:'育母蜘蛛',62:'赏金猎人',63:'编织者',64:'杰奇洛',65:'蝙蝠骑士',66:'陈',67:'幽鬼',68:'远古冰魄',69:'末日使者',70:'熊战士',71:'裂魂人',72:'矮人直升机',73:'炼金术师',74:'祈求者',75:'沉默术士',76:'黑曜毁灭者',77:'狼人',78:'酒仙',79:'暗影恶魔',80:'德鲁伊',81:'混沌骑士',82:'米波',83:'树精卫士',84:'食人魔法师',85:'不朽尸王',86:'拉比克',87:'干扰者',88:'司夜刺客',89:'娜迦海妖',90:'光之守卫',91:'艾欧',92:'维萨吉',93:'斯拉克',94:'美杜莎',95:'巨魔战将',96:'半人马',97:'马格纳斯',98:'伐木机',99:'钢背兽',100:'巨牙海民',101:'天怒法师',102:'亚巴顿',103:'上古巨神',104:'军团指挥官',105:'工程师',106:'灰烬之灵',107:'大地之灵',108:'孽主',109:'恐怖利刃',110:'凤凰',111:'神谕者',112:'寒冬飞龙',113:'天穹守望者',114:'齐天大圣',119:'邪影芳灵',120:'石鳞剑士',121:'天涯墨客',123:'森林之友',126:'虚无之灵',128:'电炎绝手',129:'玛尔斯',131:'戏命师',135:'破晓辰星',136:'玛西',137:'獸',138:'死亡射手',145:'凯兹',155:'拉戈'}
function heroIcon(id) { const e = HEROES[id]; return e ? `/asset/hero/${e}.png` : '' }
function heroName(id) { return HERO_CN[id] || ('英雄' + id) }
function modeLabel(m) { return MODES[m] || ('模式' + m) }
function fmtDuration(s) { if (!s) return '-'; return Math.floor(s/60) + ':' + String(s%60).padStart(2,'0') }
function fmtTime(ts) { return ts ? dayjs.unix(ts).format('YYYY-MM-DD HH:mm') : '-' }

const hintText = computed(() => {
  if (relationType.value === 'teammate') return selectedIds.value.length < 2 ? '队友关联需要至少选 2 个账号' : `已选 ${selectedIds.value.length} 个账号`
  if (relationType.value === 'opponent') return opponentId.value ? '已选 ' + selectedIds.value.length + ' 个账号对战 ' + opponentId.value.slice(-6) : '对手关联需要输入对手的 Steam ID'
  if (relationType.value === 'solo') return selectedIds.value.length >= 1 ? `查询 ${selectedIds.value[0].slice(-6)} 的单排记录` : '单排请勾选一个账号'
  return ''
})

async function fetchData() {
  if (relationType.value === 'teammate' && selectedIds.value.length < 2) { ElMessage.warning('队友关联需要至少选择 2 个账号'); return }
  if (relationType.value === 'opponent' && selectedIds.value.length < 1) { ElMessage.warning('对手关联请至少选择 1 个账号'); return }
  if (relationType.value === 'solo' && selectedIds.value.length < 1) { ElMessage.warning('单排模式请勾选至少 1 个账号'); return }
  loading.value = true; queried.value = true
  try {
    const body = { steamIds: selectedIds.value, relationType: relationType.value }
    if (relationType.value === 'solo') { body.steamId = selectedIds.value[0]; body.page = soloPage.value; body.size = 20; if (modeFilter.value) body.gameMode = Number(modeFilter.value) }
    if (relationType.value === 'opponent') { if (!opponentId.value) { ElMessage.warning('请输入对手的 Steam ID'); loading.value=false; return }; body.opponentId = opponentId.value }
    const r = await relatedMatches(body)
    rawList.value = r.data?.matches || []
    backendSummary.value = r.data?.summary || []
    if (relationType.value === 'solo') {
      soloTotal.value = r.data?.total || 0
      soloStatsData.value = r.data?.soloStats || null
    }
  } catch (e) {
    ElMessage.error('查询失败: ' + (e.response?.data?.message || e.message))
  } finally { loading.value = false }
}

const maxSoloPage = computed(() => Math.max(1, Math.ceil(soloTotal.value / 20)))

function goSoloPage(p) {
  soloPage.value = p
  fetchData()
}

function onModeFilterChange() {
  if (relationType.value === 'solo') soloPage.value = 1
  fetchData()
}

onMounted(async () => {
  try {
    const r = await listAll()
    accounts.value = r.data
    if (r.data.length >= 2) selectedIds.value = [r.data[0].steamId, r.data[1].steamId]
  } catch {}
})
</script>
