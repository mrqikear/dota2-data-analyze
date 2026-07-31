<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:600">比赛记录列表</span>
        <div>
          <el-select v-model="gameModeFilter" placeholder="模式筛选" clearable style="width:130px;margin-right:10px" @change="onFilterChange">
            <el-option label="全英雄选择" :value="1" />
            <el-option label="队长模式" :value="2" />
            <el-option label="随机征召" :value="3" />
            <el-option label="单一征召" :value="4" />
            <el-option label="全随机" :value="5" />
            <el-option label="天梯全英雄选择" :value="22" />
            <el-option label="加速模式" :value="23" />
          </el-select>
          <el-select v-model="parsedFilter" placeholder="解析状态" clearable style="width:120px;margin-right:10px" @change="onFilterChange">
            <el-option label="已解析" :value="true" />
            <el-option label="未解析" :value="false" />
          </el-select>
          <el-select v-model="steamIdFilter" placeholder="筛选Steam 账号" clearable style="width:200px;margin-right:10px" @change="onFilterChange">
            <el-option v-for="a in accounts" :key="a.steamId" :label="a.nickName + ' (' + a.steamId + ')'" :value="a.steamId" />
          </el-select>
          <el-tag v-if="heroIdFilter" closable size="small" style="margin-right:10px" @close="heroIdFilter=null;onFilterChange()">
            英雄: {{ HERO_CN[heroIdFilter] || ('ID '+heroIdFilter) }}
          </el-tag>
          <el-button type="primary" size="small" @click="fetchData">刷新</el-button>
        </div>
      </div>
    </template>

    <!-- Player Stats Summary -->
    <div v-if="playerStatsData" style="margin-bottom:12px;padding:10px 14px;border-radius:6px;border:1px solid #ebeef5;background:#fafafa">
      <div style="font-weight:600;font-size:14px;margin-bottom:6px">📊 用户总统计</div>
      <div style="display:flex;gap:20px;flex-wrap:wrap;font-size:13px">
        <span>总场次<strong>{{ playerStatsData.total }}</strong></span>
        <span style="color:#67C23A">胜<strong>{{ playerStatsData.wins }}</strong></span>
        <span style="color:#F56C6C">负<strong>{{ playerStatsData.losses }}</strong></span>
        <span>胜率 <strong :style="{color: playerStatsData.winRate >= 50 ? '#67C23A' : '#F56C6C'}">{{ playerStatsData.winRate }}%</strong></span>
        <span style="color:#E6A23C">🏆 MVP <strong>{{ playerStatsData.mvp || 0 }}</strong></span>
        <span style="color:#909399">🥈 FMVP <strong>{{ playerStatsData.fmvp || 0 }}</strong></span>
      </div>
      <div v-if="playerStatsData.byMode && playerStatsData.byMode.length" style="display:flex;gap:20px;flex-wrap:wrap;font-size:12px;margin-top:4px;color:#909399">
        <span v-for="m in playerStatsData.byMode" :key="m.game_mode">
          {{ m.game_mode === 'turbo' ? '加速' : '普通天梯' }}: {{ m.total }} 场 胜率{{ m.winRate }}%
        </span>
      </div>
    </div>

    <!-- Stats bar -->
    <div v-if="list.length > 0" style="margin-bottom:12px;font-size:13px;display:flex;gap:16px;flex-wrap:wrap;align-items:center">
      <span>共<strong>{{ list.length }}</strong>场</span>
      <span style="color:#67C23A">胜<strong>{{ winCount }}</strong></span>
      <span style="color:#F56C6C">负<strong>{{ loseCount }}</strong></span>
      <span>胜率 <strong :style="{ color: winRate >= 50 ? '#67C23A' : '#F56C6C' }">{{ winRate }}%</strong></span>
      <el-button size="small" text @click="showHeroStats = !showHeroStats">
        {{ showHeroStats ? '收起' : '展开' }}英雄统计
      </el-button>
    </div>

    <!-- Hero stats table (from backend API, includes MVP/FMVP per hero) -->
    <el-table v-if="showHeroStats && playerStatsData && playerStatsData.heroes && playerStatsData.heroes.length" :data="playerStatsData.heroes" stripe border size="small" style="margin-bottom:12px">
      <el-table-column label="英雄" width="60" align="center">
        <template #default="{ row }"><el-image :src="heroIconUrl(row.hero_id)" style="width:28px;height:28px;border-radius:4px" /></template>
      </el-table-column>
      <el-table-column label="名称" width="90"><template #default="{ row }">{{ heroName(row.hero_id) }}</template></el-table-column>
      <el-table-column label="场次" width="60" align="center" prop="games" sortable />
      <el-table-column label="胜" width="50" align="center" prop="wins" sortable>
        <template #default="{ row }"><span style="color:#67C23A">{{ row.wins }}</span></template>
      </el-table-column>
      <el-table-column label="负" width="50" align="center" prop="losses" sortable>
        <template #default="{ row }"><span style="color:#F56C6C">{{ row.wins ? row.games - row.wins : row.games }}</span></template>
      </el-table-column>
      <el-table-column label="胜率" width="70" align="center" prop="winRate" sortable>
        <template #default="{ row }"><span :style="{ color: row.winRate >= 50 ? '#67C23A' : '#F56C6C', fontWeight:700 }">{{ row.winRate }}%</span></template>
      </el-table-column>
      <el-table-column label="KDA" width="110" align="center">
        <template #default="{ row }">{{ row.avg_kills }}/{{ row.avg_deaths }}/{{ row.avg_assists }}</template>
      </el-table-column>
      <el-table-column label="MVP" width="60" align="center" prop="mvp" sortable>
        <template #default="{ row }"><span style="color:#E6A23C;font-weight:700">{{ row.mvp }}</span></template>
      </el-table-column>
      <el-table-column label="FMVP" width="60" align="center" prop="fmvp" sortable>
        <template #default="{ row }"><span style="color:#909399;font-weight:700">{{ row.fmvp }}</span></template>
      </el-table-column>
    </el-table>

    <el-table :data="list" v-loading="loading" stripe border style="width:100%" @sort-change="handleSortChange">
      <el-table-column type="index" label="#" width="50" align="center" />
      <el-table-column label="用户" min-width="160">
        <template #default="{ row }">
          {{ row.nickName || '-' }}
          <span style="color:#909399;font-size:12px">({{ row.steamId }})</span>
        </template>
      </el-table-column>
      <el-table-column label="英雄" width="80" align="center">
        <template #default="{ row }">
          <div style="position:relative;display:inline-block">
          <el-popover v-if="row.parsed" placement="right" :width="280" trigger="hover">
            <template #reference>
              <el-image :src="'/asset/hero/' + heroName(row.heroId) + '.png'" style="width:36px;height:36px;border-radius:4px;cursor:pointer" />
            </template>
            <div v-if="damageCache[row.matchId+'_'+row.steamId]" style="padding:4px">
              <div style="font-weight:600;font-size:13px;margin-bottom:6px">伤害分析</div>
              <div v-if="damageCache[row.matchId+'_'+row.steamId].inflictor.some(d=>d.dmgType>0)" style="display:flex;gap:6px;margin-bottom:4px;font-size:10px">
                <span style="color:#67C23A">■ 物理 {{ sumByTypeL(damageCache[row.matchId+'_'+row.steamId].inflictor,1) }}</span>
                <span style="color:#409EFF">■ 魔法 {{ sumByTypeL(damageCache[row.matchId+'_'+row.steamId].inflictor,2) }}</span>
                <span style="color:#E6A23C">■ 纯粹 {{ sumByTypeL(damageCache[row.matchId+'_'+row.steamId].inflictor,3) }}</span>
              </div>
              <div style="font-size:12px;margin-bottom:4px">
                <span style="color:#E6A23C">造成伤害: <strong>{{ damageCache[row.matchId+'_'+row.steamId].inflictor.length ? totalDamage(damageCache[row.matchId+'_'+row.steamId].inflictor) : formatDamageNum(damageCache[row.matchId+'_'+row.steamId].heroDamage) }}</strong></span>
              </div>
              <div v-if="damageCache[row.matchId+'_'+row.steamId].inflictor.length" style="max-height:200px;overflow-y:auto">
                <div v-for="d in damageCache[row.matchId+'_'+row.steamId].inflictor.slice(0,10)" :key="d.name"
                  style="display:flex;align-items:center;gap:4px;font-size:11px;padding:1px 0">
                  <el-image v-if="d.icon" :src="d.icon" style="width:14px;height:14px;flex-shrink:0" />
                  <span style="flex:1">{{ d.display }}</span>
                  <span style="font-weight:600">{{ formatDamageNum(d.damage) }}</span>
                </div>
              </div>
              <div v-if="damageCache[row.matchId+'_'+row.steamId].received.some(d=>d.dmgType>0)" style="display:flex;gap:6px;margin-bottom:4px;font-size:10px">
                <span style="color:#67C23A">■ 物理 {{ sumByTypeL(damageCache[row.matchId+'_'+row.steamId].received,1) }}</span>
                <span style="color:#409EFF">■ 魔法 {{ sumByTypeL(damageCache[row.matchId+'_'+row.steamId].received,2) }}</span>
                <span style="color:#E6A23C">■ 纯粹 {{ sumByTypeL(damageCache[row.matchId+'_'+row.steamId].received,3) }}</span>
              </div>
              <div style="font-size:12px;margin:6px 0 4px">
                <span style="color:#F56C6C">承受伤害: <strong>{{ damageCache[row.matchId+'_'+row.steamId].received.length ? totalDamage(damageCache[row.matchId+'_'+row.steamId].received) : formatDamageNum(damageCache[row.matchId+'_'+row.steamId].damageTaken) }}</strong></span>
              </div>
              <div v-if="damageCache[row.matchId+'_'+row.steamId].received.length" style="max-height:200px;overflow-y:auto">
                <div v-for="d in damageCache[row.matchId+'_'+row.steamId].received.slice(0,10)" :key="d.name"
                  style="display:flex;align-items:center;gap:4px;font-size:11px;padding:1px 0">
                  <el-image v-if="d.icon" :src="d.icon" style="width:14px;height:14px;flex-shrink:0" />
                  <span style="flex:1">{{ d.display }}</span>
                  <span style="font-weight:600">{{ formatDamageNum(d.damage) }}</span>
                </div>
              </div>
            </div>
            <div v-else style="padding:8px;text-align:center;font-size:12px;color:#909399">加载中..</div>
            <span v-if="row.mvpSteamId === row.steamId" style="position:absolute;top:-4px;right:-4px;background:#E6A23C;color:#fff;font-size:8px;padding:0 3px;border-radius:2px;font-weight:700;line-height:12px">MVP</span>
            <span v-if="row.fmvpSteamId === row.steamId" style="position:absolute;top:-4px;right:-4px;background:#909399;color:#fff;font-size:8px;padding:0 3px;border-radius:2px;font-weight:700;line-height:12px">F</span>
          </el-popover>
          <div v-else-if="row.heroId" style="position:relative;display:inline-block">
            <el-image :src="'/asset/hero/' + heroName(row.heroId) + '.png'" style="width:36px;height:36px;border-radius:4px" />
            <span v-if="row.mvpSteamId === row.steamId" style="position:absolute;top:-4px;right:-4px;background:#E6A23C;color:#fff;font-size:8px;padding:0 3px;border-radius:2px;font-weight:700;line-height:12px">MVP</span>
            <span v-if="row.fmvpSteamId === row.steamId" style="position:absolute;top:-4px;right:-4px;background:#909399;color:#fff;font-size:8px;padding:0 3px;border-radius:2px;font-weight:700;line-height:12px">F</span>
          </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="KDA" width="140" align="center">
        <template #default="{ row }">
          <span>{{ row.kills }} / {{ row.deaths }} / {{ row.assists }}</span>
          <span style="margin-left:8px;color:#909399;font-size:12px">({{ kdaRatio(row) }})</span>
        </template>
      </el-table-column>
      <el-table-column label="胜负" width="60" align="center">
        <template #default="{ row }">
          <el-tag :type="row.win ? 'success' : 'danger'" size="small">{{ row.win ? '胜' : '负' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="比赛类型" width="130" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ matchTypeLabel(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="解析" width="60" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.parsed" type="success" size="small">已解析</el-tag>
          <el-tag v-else type="info" size="small">未解析</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="比赛ID" width="110" align="center">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" style="cursor:pointer" @click="goToDetail(row.matchId)">{{ row.matchId }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="时长" width="80" align="center">
        <template #default="{ row }">
          {{ formatDuration(row.duration) }}
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="150" align="center" sortable="custom" prop="startTime">
        <template #default="{ row }">
          {{ formatUnix(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column label="GPM" width="70" align="center">
        <template #default="{ row }">{{ row.goldPerMin > 0 ? row.goldPerMin : '-' }}</template>
      </el-table-column>
      <el-table-column label="XPM" width="70" align="center">
        <template #default="{ row }">{{ row.xpPerMin > 0 ? row.xpPerMin : '-' }}</template>
      </el-table-column>
      <el-table-column label="补刀" width="70" align="center">
        <template #default="{ row }">{{ row.lastHits > 0 ? row.lastHits : '-' }}</template>
      </el-table-column>
      <el-table-column label="反补" width="70" align="center">
        <template #default="{ row }">{{ row.denies > 0 ? row.denies : '-' }}</template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div style="margin-top:16px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
      <div style="font-size:13px;color:#909399">
        共<strong style="color:#303133">{{ total }}</strong> 条记录，第<strong style="color:#303133">{{ query.page }}</strong> / <strong style="color:#303133">{{ maxPage }}</strong> 页      </div>
      <div style="display:flex;align-items:center;gap:6px">
        <el-button size="small" :disabled="query.page <= 1" @click="goPage(query.page - 1)">上一页</el-button>
        <template v-for="p in visiblePages" :key="p">
          <el-button v-if="p === '...'" size="small" disabled style="border:none;background:none">…</el-button>
          <el-button v-else size="small" :type="p === query.page ? 'primary' : 'default'" @click="goPage(p)">{{ p }}</el-button>
        </template>
        <el-button size="small" :disabled="query.page >= maxPage" @click="goPage(query.page + 1)">下一页</el-button>
        <span style="margin-left:4px;font-size:12px;color:#909399">每页</span>
        <el-select v-model.number="query.size" size="small" style="width:80px" @change="onSizeChange">
          <el-option :value="20" label="20" />
          <el-option :value="50" label="50" />
          <el-option :value="100" label="100" />
          <el-option :value="200" label="200" />
        </el-select>
        <span style="font-size:12px;color:#909399">条</span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
import { useRoute } from 'vue-router'
const route = useRoute()
import { pageMatches, playerStats } from '@/api/match'
import { getMatchDamage } from '@/api/matchDetail'
import { ITEM_NAMES } from '@/utils/itemMap'
import { ABILITY_IDS } from '@/utils/abilityIds'

import { listAll } from '@/api/steamAccount'
import dayjs from 'dayjs'

const loading = ref(false)
const list = ref([]); const total = ref(0)
const damageCache = ref({})
const playerStatsData = ref(null)
const query = reactive({ page: 1, size: 20 })
const accounts = ref([])
const steamIdFilter = ref('')
const gameModeFilter = ref(null)
const parsedFilter = ref(null)
const heroIdFilter = ref(null)
const showHeroStats = ref(false)

// Compute win/loss stats from displayed list
const winCount = computed(() => list.value.filter(r => r.win).length)
const loseCount = computed(() => list.value.filter(r => !r.win).length)
const winRate = computed(() => {
  const t = winCount.value + loseCount.value
  return t > 0 ? Math.round(100 * winCount.value / t) : 0
})

// Compute per-hero stats from displayed list
const heroStatsList = computed(() => {
  const map = {}
  for (const r of list.value) {
    if (!map[r.heroId]) map[r.heroId] = { heroId: r.heroId, games: 0, wins: 0, losses: 0, kills: 0, deaths: 0, assists: 0 }
    map[r.heroId].games++
    if (r.win) { map[r.heroId].wins++ } else { map[r.heroId].losses++ }
    map[r.heroId].kills += (r.kills || 0)
    map[r.heroId].deaths += (r.deaths || 0)
    map[r.heroId].assists += (r.assists || 0)
  }
  return Object.values(map).map(h => ({
    ...h,
    winRate: h.games > 0 ? Math.round(100 * h.wins / h.games) : 0,
    avgKills: h.games > 0 ? (h.kills / h.games).toFixed(1) : '0.0',
    avgDeaths: h.games > 0 ? (h.deaths / h.games).toFixed(1) : '0.0',
    avgAssists: h.games > 0 ? (h.assists / h.games).toFixed(1) : '0.0',
  })).sort((a, b) => b.games - a.games)
})

const HERO_CN = {
  1:'敌法师',2:'斧王',3:'痛苦之源',4:'嗜血狂魔',5:'水晶室女',
  6:'卓尔游侠',7:'撼地者',8:'主宰',9:'米拉娜',10:'变体精灵',
  11:'影魔',12:'幻影长矛手',13:'帕克',14:'帕吉',15:'雷泽',
  16:'沙王',17:'风暴之灵',18:'斯温',19:'小小',20:'复仇之魂',
  21:'风行者',22:'宙斯',23:'昆卡',25:'莉娜',26:'莱恩',
  27:'暗影萨满',28:'大鱼人',29:'潮汐猎人',30:'巫医',31:'巫妖',
  32:'力丸',33:'谜团',34:'修补匠',35:'狙击手',36:'死灵法师',
  37:'术士',38:'兽王',39:'痛苦女王',40:'剧毒术士',41:'虚空假面',
  42:'骷髅王',43:'死亡先知',44:'幻影刺客',45:'帕格纳',46:'圣堂刺客',
  47:'冥界亚龙',48:'月之骑士',49:'龙骑士',50:'戴泽',51:'发条技师',
  52:'拉席克',53:'先知',54:'噬魂鬼',55:'黑暗贤者',56:'克林克兹',
  57:'全能骑士',58:'魅惑魔女',59:'哈斯卡',60:'暗夜魔王',61:'育母蜘蛛',
  62:'赏金猎人',63:'编织者',64:'杰奇洛',65:'蝙蝠骑士',66:'陈',
  67:'幽鬼',68:'远古冰魄',69:'末日使者',70:'熊战士',71:'裂魂人',
  72:'矮人直升机',73:'炼金术师',74:'祈求者',75:'沉默术士',76:'黑曜毁灭者',
  77:'狼人',78:'酒仙',79:'暗影恶魔',80:'德鲁伊',81:'混沌骑士',
  82:'米波',83:'树精卫士',84:'食人魔法师',85:'不朽尸王',86:'拉比克',
  87:'干扰者',88:'司夜刺客',89:'娜迦海妖',90:'光之守卫',91:'艾欧',
  92:'维萨吉',93:'斯拉克',94:'美杜莎',95:'巨魔战将',96:'半人马',
  97:'马格纳斯',98:'伐木机',99:'钢背兽',100:'巨牙海民',101:'天怒法师',
  102:'亚巴顿',103:'上古巨神',104:'军团指挥官',105:'工程师',106:'灰烬之灵',
  107:'大地之灵',108:'孽主',109:'恐怖利刃',110:'凤凰',111:'神谕者',
  112:'寒冬飞龙',113:'天穹守望者',114:'齐天大圣',119:'邪影芳灵',120:'石鳞剑士',
  121:'天涯墨客',123:'森林之友',126:'虚无之灵',128:'电炎绝手',129:'玛尔斯',
  131:'戏命师',135:'破晓辰星',136:'玛西',137:'獸',138:'死亡射手',
  145:'凯兹',155:'拉戈'
}

const HERO_NAMES = {
  1: 'antimage', 2: 'axe', 3: 'bane', 4: 'bloodseeker', 5: 'crystal_maiden',
  6: 'drow_ranger', 7: 'earthshaker', 8: 'juggernaut', 9: 'mirana', 10: 'morphling',
  11: 'nevermore', 12: 'phantom_lancer', 13: 'puck', 14: 'pudge', 15: 'razor',
  16: 'sand_king', 17: 'storm_spirit', 18: 'sven', 19: 'tiny', 20: 'vengefulspirit',
  21: 'windrunner', 22: 'zuus', 23: 'kunkka', 25: 'lina', 26: 'lion',
  27: 'shadow_shaman', 28: 'slardar', 29: 'tidehunter', 30: 'witch_doctor', 31: 'lich',
  32: 'riki', 33: 'enigma', 34: 'tinker', 35: 'sniper', 36: 'necrolyte',
  37: 'warlock', 38: 'beastmaster', 39: 'queenofpain', 40: 'venomancer', 41: 'faceless_void',
  42: 'skeleton_king', 43: 'death_prophet', 44: 'phantom_assassin', 45: 'pugna', 46: 'templar_assassin',
  47: 'viper', 48: 'luna', 49: 'dragon_knight', 50: 'dazzle', 51: 'rattletrap',
  52: 'leshrac', 53: 'furion', 54: 'life_stealer', 55: 'dark_seer', 56: 'clinkz',
  57: 'omniknight', 58: 'enchantress', 59: 'huskar', 60: 'night_stalker', 61: 'broodmother',
  62: 'bounty_hunter', 63: 'weaver', 64: 'jakiro', 65: 'batrider', 66: 'chen',
  67: 'spectre', 68: 'ancient_apparition', 69: 'doom_bringer', 70: 'ursa', 71: 'spirit_breaker',
  72: 'gyrocopter', 73: 'alchemist', 74: 'invoker', 75: 'silencer', 76: 'obsidian_destroyer',
  77: 'lycan', 78: 'brewmaster', 79: 'shadow_demon', 80: 'lone_druid', 81: 'chaos_knight',
  82: 'meepo', 83: 'treant', 84: 'ogre_magi', 85: 'undying', 86: 'rubick',
  87: 'disruptor', 88: 'nyx_assassin', 89: 'naga_siren', 90: 'keeper_of_the_light', 91: 'wisp',
  92: 'visage', 93: 'slark', 94: 'medusa', 95: 'troll_warlord', 96: 'centaur',
  97: 'magnataur', 98: 'shredder', 99: 'bristleback', 100: 'tusk', 101: 'skywrath_mage',
  102: 'abaddon', 103: 'elder_titan', 104: 'legion_commander', 105: 'techies', 106: 'ember_spirit',
  107: 'earth_spirit', 108: 'abyssal_underlord', 109: 'terrorblade', 110: 'phoenix', 111: 'oracle',
  112: 'winter_wyvern', 113: 'arc_warden', 114: 'monkey_king', 119: 'dark_willow', 120: 'pangolier',
  121: 'grimstroke', 123: 'hoodwink', 126: 'void_spirit', 128: 'snapfire', 129: 'mars',
  131: 'ringmaster', 135: 'dawnbreaker', 136: 'marci', 137: 'primal_beast', 138: 'muerta',
  145: 'kez', 155: 'largo'
}
const heroName = (id) => HERO_NAMES[id] || 'default'
const heroIconUrl = (id) => `/asset/hero/${heroName(id)}.png`

const GAME_MODE_LABELS = {
  1: '全英雄选择',
  2: '队长模式',
  3: '随机征召',
  4: '单一征召',
  5: '全随机',
  22: '全英雄选择',
  23: '加速模式'
}

const LOBBY_PREFIX = {
  7: '天梯',
  9: '勇士联赛'
}

const matchTypeLabel = (row) => {
  const prefix = LOBBY_PREFIX[row.lobbyType] || ''
  const mode = GAME_MODE_LABELS[row.gameMode]
  if (!mode) return row.gameMode ? prefix + '模式' + row.gameMode : '-'
  return prefix + mode
}

const kdaRatio = (r) => {
  const d = r.deaths || 1
  return ((r.kills + r.assists) / d).toFixed(1)
}
const formatDuration = (s) => s ? Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0') : '-'
const formatUnix = (ts) => ts ? dayjs.unix(ts).format('YYYY-MM-DD HH:mm') : '-'

let dmgTypeLookupM = null
async function ensureDmgTypeM() {
  if (dmgTypeLookupM) return
  dmgTypeLookupM = {}
  try {
    const r = await fetch('/api/constants/abilities')
    const d = await r.json()
    if (d.code === '000000' && d.data) {
      const raw = JSON.parse(d.data)
      for (const [key, val] of Object.entries(raw)) {
        if (val.dmg_type && val.dmg_type !== '0') dmgTypeLookupM[key] = Number(val.dmg_type)
      }
      // Re-parse all cached entries with updated dmg types
      for (const [ck, entry] of Object.entries(damageCache.value)) {
        if (!entry.inflictor && !entry.received) continue
        const orig = damageCache.value[ck]
        orig.inflictor = orig.inflictor.map(d => ({...d, dmgType: getDmgTypeL(d.name)}))
        orig.received = orig.received.map(d => ({...d, dmgType: getDmgTypeL(d.name)}))
      }
    }
  } catch {}
}
const itemDmgM = {'radiance':2,'urn_shard':2,'blade_mail':1,'lotus_orb':2,'mjollnir':2,'maelstrom':2,'shivas_guard':2,'blood_grenade':2,'immolation':2,'cloak_of_flames':2,'chipped_vest':1,'orb_of_venom':2,'spirit_vessel':2,'urn_of_shadows':2,'bfury':1,'cyclone':2,'overwhelming_blink':2,'searing_signet':2,'stormcrafter':2,'hydras_breath':2,'crippling_crossbow':1}
function sumByTypeL(list,type){const t=list.filter(d=>d.dmgType===type).reduce((s,d)=>s+d.damage,0);return t>=10000?(t/10000).toFixed(1)+'万':t.toLocaleString()}
function getDmgTypeL(name){
  if(name==='null')return 1
  if(!name||name==='undefined')return 0
  if(itemDmgM[name]!==undefined)return itemDmgM[name]
  if(dmgTypeLookupM&&dmgTypeLookupM[name])return dmgTypeLookupM[name]
  return 0
}

async function loadDamage(row) {
  const ck = row.matchId+'_'+row.steamId
  if (damageCache.value[ck]) return
  // Set fallback cache immediately using match_player data (heroDamage)
  damageCache.value[ck] = {
    inflictor: [],
    received: [],
    heroDamage: row.heroDamage || 0,
    damageTaken: row.damageTaken || 0,

  }
  try {
    if (!dmgTypeLookupM) await ensureDmgTypeM()
    const r = await getMatchDamage(row.matchId)
    if (r.data?.parsed) {
      const players = r.data.players || []
      // Find matching player by steamId, or use the first player as fallback
      let playerData = players.find(p => p.steamId === row.steamId)
      if (!playerData) playerData = players.find(p => row.steamId.endsWith(p.steamId.slice(-8)))
      if (!playerData && players.length > 0) playerData = players[0]
      if (playerData) {
        const di = playerData.damageInflictor || {}
        const dir = playerData.damageInflictorReceived || {}
        let inf = [], rec = [], parseErr = ''
        try { inf = parseDamageList(di); rec = parseDamageList(dir) } catch(e) { parseErr = e.message }
        damageCache.value[ck] = {
          inflictor: inf,
          received: rec,
          heroDamage: playerData.heroDamage || 0,
          damageTaken: playerData.damageTaken || 0,

        }
      }
    }
  } catch {}
}

// Build lookup tables (same as matchDetail page)
const ABILITY_LOOKUP = {}
for (const [id, val] of Object.entries(ABILITY_IDS)) {
  if (val.n && val.n.startsWith('item_')) continue
  if (val.n) ABILITY_LOOKUP[val.n] = val
}
const ITEM_LOOKUP = {}
for (const [id, name] of Object.entries(ITEM_NAMES)) {
  ITEM_LOOKUP[name] = { icon: name }
}

const DAMAGE_NAMES = {
  'null': '普通攻击',
  'radiance': '辉耀',
  'urn_shard': '大骨灰',
  'blade_mail': '刃甲',
  'lotus_orb': '莲花',
  'mjollnir': '大雷锤',
  'maelstrom': '小雷锤',
  'shivas_guard': '冰甲',
  'blood_grenade': '血棘手雷',
  'immolation': '辉耀灼烧',
  'cloak_of_flames': '火焰斗篷',
  'chipped_vest': '碎裂背心',
  'overwhelming_blink': '回响跳刀',
  'orb_of_venom': '毒球',
  'bfury': '狂战斧',
  'cyclone': '吹风',
  'urn_of_shadows': '骨灰盒',
  'spirit_vessel': '大骨灰',
  'soul_ring': '魂戒',
}

function parseDamageList(dmgObj) {
  if (!dmgObj) return []
  return Object.entries(dmgObj)
    .map(([k, v]) => {
      let display = DAMAGE_NAMES[k]
      let icon = null
      if (k === 'null') { display = '普通攻击'; icon = '/asset/item/blades_of_attack.png' }
      else if (k === 'undefined') { display = '未知来源'; icon = null }
      else if (ITEM_LOOKUP[k]) { icon = `/asset/item/${k}.png` }
      else if (ABILITY_LOOKUP[k]) {
        const a = ABILITY_LOOKUP[k]
        if (!display) display = a.d || k
        if (a.i) icon = `/asset/ability/${a.i}.png`
      }
      if (!display) display = k.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
      return { name: k, display, icon, damage: Number(v) || 0 }
    })
    .filter(d => d.damage > 0)
    .sort((a, b) => b.damage - a.damage)
}

function totalDamage(list) {
  const t = list.reduce((s, d) => s + d.damage, 0)
  if (t >= 10000) return (t / 10000).toFixed(1) + '万'
  return t.toLocaleString()
}

function formatDamageNum(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

function goToDetail(matchId) {
  router.push('/match/detail/' + matchId)
}

onMounted(async () => {
  try {
    const r = await listAll(); accounts.value = r.data
  } catch {}
  // Read heroId & steamId & gameMode from URL query params
  const q = route.query
  if (q.heroId) heroIdFilter.value = Number(q.heroId)
  if (q.steamId) steamIdFilter.value = q.steamId
  if (q.gameMode) gameModeFilter.value = Number(q.gameMode)
  fetchData()
  window.addEventListener('match-synced', fetchData)
})

// Watch for route query changes (e.g. navigating from hero overview)
watch(() => route.query, (q) => {
  let changed = false
  if (q.heroId) {
    heroIdFilter.value = Number(q.heroId)
    changed = true
  } else if (heroIdFilter.value && !q.heroId) {
    heroIdFilter.value = null
    changed = true
  }
  if (q.gameMode) {
    gameModeFilter.value = Number(q.gameMode)
    changed = true
  } else if (gameModeFilter.value && !q.gameMode) {
    gameModeFilter.value = null
    changed = true
  }
  if (changed) { query.page = 1; fetchData() }
})

onBeforeUnmount(() => {
  window.removeEventListener('match-synced', fetchData)
})

function handleSortChange({ prop, order }) {
  if (order) {
    query.sortField = prop
    query.sortOrder = order === 'ascending' ? 'ASC' : 'DESC'
  } else {
    delete query.sortField
    delete query.sortOrder
  }
  query.page = 1
  fetchData()
}

function onSizeChange(val) {
  query.size = Number(val)
  query.page = 1
  fetchData()
}

const maxPage = computed(() => Math.max(1, Math.ceil(total.value / query.size)))

const visiblePages = computed(() => {
  const pages = []
  const totalPages = maxPage.value
  const current = query.page
  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) pages.push(i)
  } else {
    pages.push(1)
    if (current > 3) pages.push('...')
    for (let i = Math.max(2, current - 1); i <= Math.min(totalPages - 1, current + 1); i++) pages.push(i)
    if (current < totalPages - 2) pages.push('...')
    pages.push(totalPages)
  }
  return pages
})

function onFilterChange() {
  query.page = 1
  fetchData()
}

function goPage(p) {
  query.page = p
  fetchData()
}

async function fetchStats() {
  if (!steamIdFilter.value) { playerStatsData.value = null; return }
  try {
    const params = {}
    if (gameModeFilter.value) params.gameMode = gameModeFilter.value
    const r = await playerStats(steamIdFilter.value, params)
    playerStatsData.value = r.data
  } catch {}
}

async function fetchData() {
  fetchStats()
  loading.value = true
  try {
    const params = { ...query }
    if (steamIdFilter.value) params.steamId = steamIdFilter.value
    if (gameModeFilter.value) params.gameMode = gameModeFilter.value
    if (heroIdFilter.value) params.heroId = heroIdFilter.value
    if (parsedFilter.value !== null) params.parsed = parsedFilter.value
    const r = await pageMatches(params)
    list.value = r.data.list
    total.value = r.data.total
    // Prefetch damage data for parsed matches
    for (const row of list.value) {
      if (row.parsed) loadDamage(row)
    }
  } finally { loading.value = false }
}
</script>
