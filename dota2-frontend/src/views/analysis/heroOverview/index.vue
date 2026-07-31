<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
        <span style="font-weight:600">英雄使用总览</span>
        <div style="display:flex;gap:10px;flex-wrap:wrap;align-items:center">

          <!-- Account filter -->
          <el-select v-model="form.steamId" placeholder="Steam 账号" style="width:180px" size="small" clearable>
            <el-option v-for="a in accounts" :key="a.steamId" :label="a.nickName" :value="a.steamId" />
          </el-select>

          <!-- Match type -->
          <el-select v-model="form.matchType" placeholder="游戏模式" style="width:130px" size="small" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="普通匹配" :value="2" />
            <el-option label="天梯" :value="3" />
            <el-option label="加速模式" :value="4" />
          </el-select>

          <!-- Date range -->
          <el-select v-model="datePreset" placeholder="日期范围" style="width:130px" size="small" clearable @change="onDateChange">
            <el-option label="最近 1 个月" :value="1" />
            <el-option label="最近 3 个月" :value="3" />
            <el-option label="最近 6 个月" :value="6" />
            <el-option label="自定义" :value="0" />
          </el-select>
          <template v-if="datePreset === 0">
            <el-date-picker v-model="customRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束"
              size="small" style="width:220px" value-format="x" />
          </template>

          <!-- Min matches -->
          <span style="font-size:12px;color:#909399;white-space:nowrap">最少</span>
          <el-input-number v-model="form.minMatches" :min="1" :max="200" size="small" style="width:90px" controls-position="right" />

          <el-button type="primary" size="small" :loading="loading" @click="fetchData">查询</el-button>
        </div>
      </div>
    </template>

    <!-- Hero stats table -->
    <el-table :data="list" v-loading="loading" stripe border style="width:100%" size="small"
      @sort-change="handleSortChange" empty-text="请选择 Steam 账号后查询">

      <el-table-column label="#" type="index" width="40" align="center" />

      <el-table-column label="英雄" width="80" align="center" fixed>
        <template #default="{ row }">
          <el-image v-if="row.heroId" :src="heroIconUrl(row.heroId)" style="width:36px;height:36px;border-radius:4px" />
        </template>
      </el-table-column>

      <el-table-column label="英雄名" width="90">
        <template #default="{ row }">{{ heroNameCn(row.heroId) }}</template>
      </el-table-column>

      <el-table-column label="场次" width="70" align="center" sortable="custom" prop="games">
        <template #default="{ row }">{{ row.games }}</template>
      </el-table-column>

      <el-table-column label="胜率" width="80" align="center" sortable="custom" prop="winRate">
        <template #default="{ row }">
          <el-tag :type="row.winRate >= 50 ? 'success' : 'danger'" size="small">
            {{ row.winRate }}%
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="KDA" width="100" align="center">
        <template #default="{ row }">
          {{ row.avgKills }}/{{ row.avgDeaths }}/{{ row.avgAssists }}
          <span style="color:#909399;font-size:11px;margin-left:4px">({{ row.avgKda }})</span>
        </template>
      </el-table-column>

      <el-table-column label="GPM" width="60" align="center">
        <template #default="{ row }">{{ row.avgGoldPerMin }}</template>
      </el-table-column>

      <el-table-column label="XPM" width="60" align="center">
        <template #default="{ row }">{{ row.avgXpPerMin }}</template>
      </el-table-column>

      <!-- Recent 20 -->
      <el-table-column label="近 20 场胜率" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.recentGames20 > 0" :style="{color: row.recentWinRate20 >= 50 ? '#67C23A' : '#F56C6C'}">
            {{ row.recentWinRate20 }}%
            <span v-if="row.games >= 20" style="margin-left:2px">
              <el-icon v-if="row.recentWinRate20 > row.winRate" color="#67C23A"><Top /></el-icon>
              <el-icon v-else-if="row.recentWinRate20 < row.winRate" color="#F56C6C"><Bottom /></el-icon>
              <el-icon v-else color="#909399"><Minus /></el-icon>
            </span>
          </span>
          <span v-else style="color:#909399">-</span>
        </template>
      </el-table-column>

      <el-table-column label="近 20 场 KDA" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.recentGames20 > 0">{{ row.recentKda20 }}</span>
          <span v-else style="color:#909399">-</span>
        </template>
      </el-table-column>

      <!-- Recent 50 -->
      <el-table-column label="近 50 场胜率" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.recentGames50 > 0" :style="{color: row.recentWinRate50 >= 50 ? '#67C23A' : '#F56C6C'}">
            {{ row.recentWinRate50 }}%
            <span v-if="row.games >= 50" style="margin-left:2px">
              <el-icon v-if="row.recentWinRate50 > row.winRate" color="#67C23A"><Top /></el-icon>
              <el-icon v-else-if="row.recentWinRate50 < row.winRate" color="#F56C6C"><Bottom /></el-icon>
              <el-icon v-else color="#909399"><Minus /></el-icon>
            </span>
          </span>
          <span v-else style="color:#909399">-</span>
        </template>
      </el-table-column>

      <el-table-column label="近 50 场 KDA" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.recentGames50 > 0">{{ row.recentKda50 }}</span>
          <span v-else style="color:#909399">-</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-dropdown trigger="click" @command="(cmd) => handleAction(cmd, row)">
            <el-button size="small" type="primary" plain>
              查看 <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="viewMatches">
                  比赛明细
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;color:#909399;font-size:12px">
      共 {{ list.length }} 个英雄
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { heroStats } from '@/api/analysis'
import { listAll } from '@/api/steamAccount'
import { Top, Bottom, Minus, ArrowDown } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
const router = useRouter()

const loading = ref(false)
const list = ref([])
const accounts = ref([])
const datePreset = ref(null)
const customRange = ref(null)

const form = reactive({
  steamId: '',
  matchType: null,
  startTime: null,
  endTime: null,
  minMatches: 1,
  sortField: 'games',
  sortOrder: 'DESC'
})

// ——— Hero name data ———
const HERO_CN_NAMES = {
  1:'敌法师',2:'斧王',3:'痛苦之源',4:'嗜血狂魔',5:'水晶室女',
  6:'卓尔游侠',7:'撼地者',8:'主宰',9:'米拉娜',10:'变体精灵',
  11:'影魔',12:'幻影长矛手',13:'帕克',14:'帕吉',15:'雷泽',
  16:'沙王',17:'风暴之灵',18:'斯温',19:'小小',20:'复仇之魂',
  21:'风行者',22:'宙斯',23:'昆卡',25:'莉娜',26:'莱恩',
  27:'暗影萨满',28:'大鱼人',29:'潮汐猎人',30:'巫医',31:'巫妖',
  32:'力丸',33:'谜团',34:'修补匠',35:'狙击手',36:'死灵法师',
  37:'术士',38:'兽王',39:'痛苦女王',40:'剧毒术士',41:'虚空假面',
  42:'骷髅王',43:'死亡先知',44:'幻影刺客',45:'帕格纳',46:'圣堂刺客',
  47:'冥界亚龙',48:'月之骑士',49:'龙骑士',50:'戴泽',
  51:'发条技师',52:'拉席克',53:'先知',54:'噬魂鬼',55:'黑暗贤者',
  56:'克林克兹',57:'全能骑士',58:'魅惑魔女',59:'哈斯卡',60:'暗夜魔王',
  61:'育母蜘蛛',62:'赏金猎人',63:'编织者',64:'杰奇洛',65:'蝙蝠骑士',
  66:'陈',67:'幽鬼',68:'远古冰魄',69:'末日使者',70:'熊战士',
  71:'裂魂人',72:'矮人直升机',73:'炼金术师',74:'祈求者',75:'沉默术士',
  76:'黑曜毁灭者',77:'狼人',78:'酒仙',79:'暗影恶魔',80:'德鲁伊',
  81:'混沌骑士',82:'米波',83:'树精卫士',84:'食人魔法师',85:'不朽尸王',
  86:'拉比克',87:'干扰者',88:'司夜刺客',89:'娜迦海妖',90:'光之守卫',
  91:'艾欧',92:'维萨吉',93:'斯拉克',94:'美杜莎',95:'巨魔战将',
  96:'半人马',97:'马格纳斯',98:'伐木机',99:'钢背兽',100:'巨牙海民',
  101:'天怒法师',102:'亚巴顿',103:'上古巨神',104:'军团指挥官',105:'工程师',
  106:'灰烬之灵',107:'大地之灵',108:'孽主',109:'恐怖利刃',110:'凤凰',
  111:'神谕者',112:'寒冬飞龙',113:'天穹守望者',114:'齐天大圣',119:'邪影芳灵',
  120:'石鳞剑士',121:'天涯墨客',123:'森林之友',126:'虚无之灵',128:'电炎绝手',
  129:'玛尔斯',131:'戏命师',135:'破晓辰星',136:'玛西',137:'獸',138:'死亡射手',
  145:'凯兹',155:'拉戈'
}

function heroNameCn(id) {
  return HERO_CN_NAMES[id] || '英雄' + id
}

function heroIconUrl(id) {
  const EN_NAMES = {
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
  const name = EN_NAMES[id] || ''
  return name ? '/asset/hero/' + name + '.png' : ''
}

onMounted(async () => {
  try {
    const r = await listAll()
    accounts.value = r.data
  } catch {}
})

function onDateChange(val) {
  if (val && val > 0) {
    form.startTime = dayjs().subtract(val, 'month').unix()
    form.endTime = dayjs().unix()
    customRange.value = null
  } else {
    form.startTime = null
    form.endTime = null
  }
}

function handleSortChange({ prop, order }) {
  if (!order) return
  form.sortField = prop === 'winRate' ? 'winRate' : 'games'
  form.sortOrder = order === 'ascending' ? 'ASC' : 'DESC'
  fetchData()
}

async function fetchData() {
  if (!form.steamId) {
    ElMessage.warning('请先选择 Steam 账号')
    return
  }

  // Handle custom date range
  if (datePreset.value === 0 && customRange.value) {
    form.startTime = Math.floor(customRange.value[0] / 1000)
    form.endTime = Math.floor(customRange.value[1] / 1000)
  } else if (datePreset.value === 0 && !customRange.value) {
    form.startTime = null
    form.endTime = null
  }

  loading.value = true
  try {
    const r = await heroStats({ ...form })
    list.value = r.data
  } catch (e) {
    ElMessage.error('查询失败: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

function handleAction(cmd, row) {
  if (cmd === 'viewMatches') {
    const q = { heroId: row.heroId }
    if (form.steamId) q.steamId = form.steamId
    // 英雄概览的 matchType 是自定义分类，需要映射为 Dota game_mode 值
    // 2=普通匹配(含多种模式) → 不传 gameMode
    // 3=天梯 → 22(天梯全英雄选择)
    // 4=加速模式 → 23
    if (form.matchType === 3) {
      q.gameMode = 22
    } else if (form.matchType === 4) {
      q.gameMode = 23
    }
    // matchType=2(普通匹配) 包含多种 game_mode，不传 gameMode 筛选
    router.push({ path: '/match', query: q })
  }
}
</script>
