<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
        <span style="font-weight:600">出装分析</span>
        <el-radio-group v-model="activeTab" size="small" @change="onTabChange">
          <el-radio-button value="starting">出门装</el-radio-button>
          <el-radio-button value="builds">出装路线</el-radio-button>
          <el-radio-button value="items">单件装备</el-radio-button>
          <el-radio-button value="contribution">胜率贡献</el-radio-button>
          <el-radio-button value="compare">全球对比</el-radio-button>
          <el-radio-button value="archetype">流派识别</el-radio-button>
        </el-radio-group>
      </div>
    </template>

    <!-- Shared Filters -->
    <div style="display:flex;gap:10px;flex-wrap:wrap;align-items:center;margin-bottom:16px">
      <el-select v-model="form.steamId" placeholder="Steam 账号" style="width:180px" size="small" clearable @change="onFilterChange">
        <el-option v-for="a in accounts" :key="a.steamId" :label="a.nickName" :value="a.steamId" />
      </el-select>
      <HeroPicker v-model="form.heroId" @update:modelValue="onFilterChange" />
      <template v-if="activeTab === 'builds'">
        <span style="font-size:12px;color:#909399">前</span>
        <el-input-number v-model="buildTopN" :min="3" :max="10" size="small" style="width:100px" controls-position="right" />
        <span style="font-size:12px;color:#909399">件</span>
      </template>
      <el-button type="primary" size="small" :loading="loading" @click="fetchData">查询</el-button>
      <el-tag v-if="totalGames > 0" type="info" size="small" effect="plain">
        基于 {{ totalGames }} 场次
      </el-tag>
    </div>

    <!-- ======== 4.1 出门装 ======== -->
    <template v-if="activeTab === 'starting'">
      <el-table v-if="startingData.length" :data="startingData" stripe border size="small" v-loading="loading">
        <el-table-column label="#" type="index" width="50" align="center" />
        <el-table-column label="出门装组合" min-width="300">
          <template #default="{ row }">
            <div style="display:flex;gap:4px;flex-wrap:wrap;align-items:center">
              <el-tooltip v-for="item in row.items" :key="item" :content="itemDisplayName(item)" placement="top">
                <el-image :src="itemIconUrl(item)" style="width:32px;height:32px;border-radius:3px;border:1px solid #e0e0e0" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="场次" width="80" align="center" prop="games" sortable><template #default="{ row }">{{ row.games }}</template></el-table-column>
        <el-table-column label="胜率" width="180" prop="winRate" sortable>
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px">
              <el-progress :percentage="row.winRate" :stroke-width="16" :color="winRateColor(row.winRate)" style="flex:1" :format="()=>''" />
              <span :style="{color:winRateColor(row.winRate),fontWeight:600,fontSize:13}">{{ row.winRate }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="胜/负" width="90" align="center">
          <template #default="{ row }"><span style="color:#67C23A">{{ row.wins }}</span> / <span style="color:#F56C6C">{{ row.games - row.wins }}</span></template>
        </el-table-column>
      </el-table>
      <el-empty v-else-if="!loading && form.steamId" description="暂无出门装数据" />
      <div v-else-if="!form.steamId" style="text-align:center;padding:60px 0;color:#909399">请选择 Steam 账号后点击「查询」</div>
    </template>

    <!-- ======== 4.2 出装路线 ======== -->
    <template v-if="activeTab === 'builds'">
      <template v-if="buildData.length">
        <div style="margin-bottom:12px;font-size:13px;color:#909399">前 {{ buildTopN }} 件非消耗品装备的出装路线，点击展开时间轴</div>
        <el-table :data="buildData" stripe border size="small" v-loading="loading">
        <el-table-column type="expand" width="40">
          <template #default="{ row }">
            <div style="padding:12px 24px">
              <div style="font-size:13px;color:#606266;margin-bottom:8px;font-weight:500">购买时间轴（按游戏内秒数）</div>
              <div style="display:flex;gap:8px;flex-wrap:wrap">
                <div v-for="(item, i) in row.items" :key="i"
                  style="display:flex;flex-direction:column;align-items:center;padding:8px 12px;border-radius:6px;background:#f5f7fa;min-width:64px">
                  <el-image :src="itemIconUrl(item)" style="width:32px;height:32px;border-radius:3px;border:1px solid #ddd" />
                  <span style="font-size:11px;color:#909399;margin-top:4px">{{ itemDisplayName(item) }}</span>
                  <span style="font-size:12px;color:#409EFF;font-weight:600;margin-top:2px">{{ formatTime(row.purchaseTimes[i]) }}</span>
                </div>
              </div>
              <div style="margin-top:8px;font-size:12px;color:#909399">平均首件购买时间: {{ formatTime(row.avgFirstTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="#" type="index" width="50" align="center" />
        <el-table-column label="出装顺序" min-width="320">
          <template #default="{ row }">
            <div style="display:flex;gap:3px;flex-wrap:wrap;align-items:center">
              <template v-for="(item, i) in row.items" :key="item">
                <el-tooltip :content="itemDisplayName(item)" placement="top">
                  <el-image :src="itemIconUrl(item)" style="width:28px;height:28px;border-radius:3px;border:1px solid #e0e0e0" />
                </el-tooltip>
                <span v-if="i < row.items.length - 1" style="color:#C0C4CC;font-size:12px">→</span>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="场次" width="70" align="center"><template #default="{ row }">{{ row.games }}</template></el-table-column>
        <el-table-column label="胜率" width="160" prop="winRate" sortable>
          <template #default="{ row }">
            <el-progress :percentage="row.winRate" :stroke-width="14" :color="winRateColor(row.winRate)" style="flex:1" :format="()=>row.winRate+'%'" />
          </template>
        </el-table-column>
        <el-table-column label="首件平均" width="100" align="center"><template #default="{ row }">{{ formatTime(row.avgFirstTime) }}</template></el-table-column>
      </el-table>
      </template>
      <el-empty v-else-if="!loading && form.steamId" description="暂无出装路线数据" />
    </template>

    <!-- ======== 4.3 单件装备 ======== -->
    <template v-if="activeTab === 'items'">
      <template v-if="itemStatsData.length">
        <el-table :data="itemStatsData" stripe border size="small" v-loading="loading">
          <el-table-column label="#" type="index" width="50" align="center" />
          <el-table-column label="装备" min-width="200">
            <template #default="{ row }">
              <div style="display:flex;align-items:center;gap:8px">
                <el-image :src="itemIdIconUrl(row.itemId)" style="width:32px;height:32px;border-radius:3px;border:1px solid #e0e0e0" />
                <span>{{ itemIdDisplayName(row.itemId) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="使用场次" width="90" align="center" prop="games" sortable><template #default="{ row }">{{ row.games }}</template></el-table-column>
          <el-table-column label="使用率" width="90" align="center">
            <template #default="{ row }">{{ totalGames > 0 ? (row.games / totalGames * 100).toFixed(1) : '-' }}%</template>
          </el-table-column>
          <el-table-column label="胜率" width="160" prop="winRate" sortable>
            <template #default="{ row }">
              <el-progress :percentage="row.winRate" :stroke-width="14" :color="winRateColor(row.winRate)" :format="()=>row.winRate+'%'" style="flex:1" />
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else-if="!loading && form.steamId" description="暂无装备数据" />
    </template>

    <!-- ======== 4.4 胜率贡献 ======== -->
    <template v-if="activeTab === 'contribution'">
      <template v-if="contributionData.length">
        <div style="margin-bottom:12px;font-size:13px;color:#909399">
          基准胜率: <strong>{{ contributionData[0]?.baselineWinRate || '-' }}%</strong>，差值 = 购买该装备的胜率 - 未购买的胜率
        </div>
        <el-table :data="contributionData" stripe border size="small" v-loading="loading" :default-sort="{ prop: 'delta', order: 'descending' }">
          <el-table-column label="#" type="index" width="50" align="center" />
          <el-table-column label="装备" min-width="180">
            <template #default="{ row }">
              <div style="display:flex;align-items:center;gap:8px">
                <el-image :src="itemIdIconUrl(row.itemId)" style="width:28px;height:28px;border-radius:3px;border:1px solid #e0e0e0" />
                <span>{{ itemIdDisplayName(row.itemId) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="使用场次" width="80" align="center"><template #default="{ row }">{{ row.games }}</template></el-table-column>
          <el-table-column label="有装备胜率" width="120" align="center"><template #default="{ row }">{{ row.withItemWinRate }}%</template></el-table-column>
          <el-table-column label="无装备胜率" width="120" align="center"><template #default="{ row }">{{ row.withoutItemWinRate }}%</template></el-table-column>
          <el-table-column label="差值" width="120" align="center" prop="delta" sortable>
            <template #default="{ row }">
              <el-tag :type="row.delta > 0 ? 'success' : row.delta < 0 ? 'danger' : 'info'" size="small" effect="dark">
                {{ row.delta > 0 ? '+' : '' }}{{ row.delta }}%
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else-if="!loading && form.steamId" description="暂无数据" />
    </template>

    <!-- ======== 4.5 个人 vs 全球对比 ======== -->
    <template v-if="activeTab === 'compare'">
      <div v-if="!form.heroId" style="text-align:center;padding:60px 0;color:#909399">全球对比需要选择英雄</div>
      <template v-else>
        <template v-if="compareData.length">
          <!-- 高亮差异卡片 -->
          <div v-if="highlights.length" style="margin-bottom:16px">
            <div style="font-weight:600;font-size:14px;margin-bottom:8px">📊 差异最大的装备</div>
            <el-row :gutter="8">
              <el-col v-for="h in highlights.slice(0, 6)" :key="h.itemId" :span="8" style="margin-bottom:8px">
                <el-card shadow="never" :body-style="{ padding: '10px 14px' }"
                  :style="{ borderLeft: '4px solid ' + (h.direction === 'more' ? '#67C23A' : '#F56C6C') }">
                  <div style="display:flex;align-items:center;gap:8px">
                    <el-image :src="itemIdIconUrl(h.itemId)" style="width:28px;height:28px;border-radius:3px;border:1px solid #e0e0e0" />
                    <div style="flex:1;font-size:13px">
                      <div>
                        <strong>{{ itemIdDisplayName(h.itemId) }}</strong>
                        <el-tag v-if="h.direction === 'more'" type="success" size="small" effect="dark" style="margin-left:6px">+{{ Math.abs(Math.round(h.diff)) }}%</el-tag>
                        <el-tag v-else type="danger" size="small" effect="dark" style="margin-left:6px">-{{ Math.abs(Math.round(h.diff)) }}%</el-tag>
                      </div>
                      <div style="color:#909399;font-size:12px;margin-top:2px">
                        <span v-if="h.direction === 'more'">你比全球玩家更爱出</span>
                        <span v-else>你比全球玩家出得更少</span>
                        <strong>{{ itemIdDisplayName(h.itemId) }}</strong>
                      </div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <!-- 对比表格 -->
          <div style="margin-bottom:8px;font-size:13px;color:#909399">
            英雄 <strong>{{ heroNameCn(form.heroId) }}</strong> — 个人共 {{ totalCompareGames }} 场 vs 全球 OpenDota 数据
          </div>
          <el-table :data="compareData" stripe border size="small" v-loading="loading"
            :default-sort="{ prop: 'diff', order: 'descending' }">
            <el-table-column label="装备" min-width="140">
              <template #default="{ row }">
                <div style="display:flex;align-items:center;gap:6px">
                  <el-image :src="itemIdIconUrl(row.itemId)" style="width:26px;height:26px;border-radius:3px;border:1px solid #e0e0e0" />
                  <span style="font-size:13px">{{ itemIdDisplayName(row.itemId) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="个人使用率" width="160" align="center">
              <template #default="{ row }">
                <div style="display:flex;align-items:center;gap:6px">
                  <el-progress :percentage="Math.min(row.personalPct, 100)" :stroke-width="14" color="#409EFF" style="flex:1" :format="()=>''" />
                  <span style="font-size:12px;font-weight:600;min-width:36px">{{ row.personalPct }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="全球使用率" width="160" align="center">
              <template #default="{ row }">
                <div style="display:flex;align-items:center;gap:6px">
                  <el-progress :percentage="Math.min(row.globalPct, 100)" :stroke-width="14" color="#909399" style="flex:1" :format="()=>''" />
                  <span style="font-size:12px;font-weight:600;min-width:36px">{{ row.globalPct }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="差异" width="100" align="center" prop="diff" sortable>
              <template #default="{ row }">
                <el-tag v-if="row.diff > 5" type="success" size="small" effect="dark">+{{ row.diff }}%</el-tag>
                <el-tag v-else-if="row.diff < -5" type="danger" size="small" effect="dark">{{ row.diff }}%</el-tag>
                <span v-else style="color:#909399;font-size:12px">{{ row.diff > 0 ? '+' : '' }}{{ row.diff }}%</span>
              </template>
            </el-table-column>
            <el-table-column label="个人场次" width="80" align="center"><template #default="{ row }">{{ row.personalGames }}</template></el-table-column>
            <el-table-column label="最佳阶段" width="80" align="center">
              <template #default="{ row }"><el-tag size="small" effect="plain">{{ phaseLabel(row.bestPhase) }}</el-tag></template>
            </el-table-column>
          </el-table>
        </template>
        <div v-if="!compareData.length && !loading" style="text-align:center;padding:60px 0;color:#909399">暂无全球对比数据</div>
      </template>
    </template>

    <!-- ======== 4.6 流派识别 ======== -->
    <template v-if="activeTab === 'archetype'">
      <template v-if="archetypeData.length">
        <el-table :data="archetypeData" stripe border size="small" v-loading="loading">
          <el-table-column label="流派" width="120">
            <template #default="{ row }">
              <el-tag :type="archetypeTagType(row.archetype)" size="large" effect="plain" style="font-size:14px">{{ row.archetype }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="场次" width="80" align="center" prop="games" sortable><template #default="{ row }">{{ row.games }}</template></el-table-column>
          <el-table-column label="占比" width="80" align="center"><template #default="{ row }">{{ totalArchetypeGames > 0 ? (row.games / totalArchetypeGames * 100).toFixed(1) : 0 }}%</template></el-table-column>
          <el-table-column label="胜率" width="160" sortable prop="winRate">
            <template #default="{ row }">
              <el-progress :percentage="row.winRate" :stroke-width="16" :color="winRateColor(row.winRate)" style="flex:1" :format="()=>row.winRate+'%'" />
            </template>
          </el-table-column>
          <el-table-column label="KDA" width="130" align="center">
            <template #default="{ row }">{{ row.avgKills }}/{{ row.avgDeaths }}/{{ row.avgAssists }} <span style="color:#909399;font-size:11px">({{ kda(row) }})</span></template>
          </el-table-column>
          <el-table-column label="GPM" width="70" align="center"><template #default="{ row }">{{ row.avgGpm }}</template></el-table-column>
          <el-table-column label="XPM" width="70" align="center"><template #default="{ row }">{{ row.avgXpm }}</template></el-table-column>
          <el-table-column label="英雄伤害" width="100" align="center"><template #default="{ row }">{{ formatNum(row.avgHeroDamage) }}</template></el-table-column>
          <el-table-column label="建筑伤害" width="100" align="center"><template #default="{ row }">{{ formatNum(row.avgTowerDamage) }}</template></el-table-column>
        </el-table>
      </template>
      <el-empty v-else-if="!loading && form.steamId" description="暂无流派数据" />
    </template>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAll } from '@/api/steamAccount'
import { startingItems, itemBuildRoutes, itemStats, itemWinContribution, itemCompareGlobal, itemArchetype } from '@/api/itemAnalysis'
import { itemIconUrl, itemDisplayName } from '@/utils/itemDisplay'
import HeroPicker from '@/components/HeroPicker.vue'
import { ITEM_NAMES } from '@/utils/itemMap'

// ---- State ----
const activeTab = ref('starting')
const loading = ref(false)
const accounts = ref([])
const buildTopN = ref(5)

// Data stores per tab
const startingData = ref([])
const buildData = ref([])
const itemStatsData = ref([])
const contributionData = ref([])
const compareData = ref([])
const compareResponse = ref(null)  // full API response with highlights
const archetypeData = ref([])

const form = reactive({ steamId: '', heroId: null })

// ---- Computed ----
const totalGames = computed(() => {
  if (activeTab.value === 'items') return itemStatsData.value.reduce((s, r) => s + r.games, 0)
  if (activeTab.value === 'contribution') return contributionData.value[0]?.games || 0
  return 0
})
const totalArchetypeGames = computed(() => archetypeData.value.reduce((s, r) => s + r.games, 0))
const highlights = computed(() => compareResponse.value?.highlights || [])
const totalCompareGames = computed(() => compareResponse.value?.totalGames || 0)

// ---- Hero data ----
const HERO_CN = { 1:'敌法师',2:'斧王',3:'痛苦之源',4:'嗜血狂魔',5:'水晶室女',6:'卓尔游侠',7:'撼地者',8:'主宰',9:'米拉娜',10:'变体精灵',11:'影魔',12:'幻影长矛手',13:'帕克',14:'帕吉',15:'雷泽',16:'沙王',17:'风暴之灵',18:'斯温',19:'小小',20:'复仇之魂',21:'风行者',22:'宙斯',23:'昆卡',25:'莉娜',26:'莱恩',27:'暗影萨满',28:'大鱼人',29:'潮汐猎人',30:'巫医',31:'巫妖',32:'力丸',33:'谜团',34:'修补匠',35:'狙击手',36:'死灵法师',37:'术士',38:'兽王',39:'痛苦女王',40:'剧毒术士',41:'虚空假面',42:'骷髅王',43:'死亡先知',44:'幻影刺客',45:'帕格纳',46:'圣堂刺客',47:'冥界亚龙',48:'月之骑士',49:'龙骑士',50:'戴泽',51:'发条技师',52:'拉席克',53:'先知',54:'噬魂鬼',55:'黑暗贤者',56:'克林克兹',57:'全能骑士',58:'魅惑魔女',59:'哈斯卡',60:'暗夜魔王',61:'育母蜘蛛',62:'赏金猎人',63:'编织者',64:'杰奇洛',65:'蝙蝠骑士',66:'陈',67:'幽鬼',68:'远古冰魄',69:'末日使者',70:'熊战士',71:'裂魂人',72:'矮人直升机',73:'炼金术师',74:'祈求者',75:'沉默术士',76:'黑曜毁灭者',77:'狼人',78:'酒仙',79:'暗影恶魔',80:'德鲁伊',81:'混沌骑士',82:'米波',83:'树精卫士',84:'食人魔法师',85:'不朽尸王',86:'拉比克',87:'干扰者',88:'司夜刺客',89:'娜迦海妖',90:'光之守卫',91:'艾欧',92:'维萨吉',93:'斯拉克',94:'美杜莎',95:'巨魔战将',96:'半人马',97:'马格纳斯',98:'伐木机',99:'钢背兽',100:'巨牙海民',101:'天怒法师',102:'亚巴顿',103:'上古巨神',104:'军团指挥官',105:'工程师',106:'灰烬之灵',107:'大地之灵',108:'孽主',109:'恐怖利刃',110:'凤凰',111:'神谕者',112:'寒冬飞龙',113:'天穹守望者',114:'齐天大圣',119:'邪影芳灵',120:'石鳞剑士',121:'天涯墨客',123:'森林之友',126:'虚无之灵',128:'电炎绝手',129:'玛尔斯',131:'戏命师',135:'破晓辰星',136:'玛西',137:'獸',138:'死亡射手',145:'凯兹',155:'拉戈' }
function heroNameCn(id) { return HERO_CN[id] || '英雄' + id }

// ---- Item helpers ----
/** Convert numeric item ID to icon URL */
function itemIdIconUrl(id) {
  if (!id || id === 0) return ''
  const name = ITEM_NAMES[id]
  if (!name) return ''
  return `/asset/item/${name}.png`
}
/** Convert item ID to display name (fallback: ID) */
function itemIdDisplayName(id) {
  if (!id || id === 0) return ''
  const name = ITEM_NAMES[id]
  if (!name) return '物品' + id
  return itemDisplayName(name)
}
/** Load item name from ID (for description lookup) */
function idToName(id) { return ITEM_NAMES[id] || '' }

function winRateColor(rate) { return rate >= 60 ? '#67C23A' : rate >= 50 ? '#E6A23C' : '#F56C6C' }
function formatTime(sec) {
  if (!sec && sec !== 0) return '-'
  const m = Math.floor(sec / 60); const s = sec % 60
  return m + ':' + String(s).padStart(2, '0')
}
function formatNum(n) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return Number(n).toLocaleString()
}
function phaseLabel(phase) {
  const map = { start:'开局', early:'前期', mid:'中期', late:'后期' }
  return map[phase] || phase
}
function kda(row) {
  const d = row.avgDeaths || 1
  return ((row.avgKills + row.avgAssists) / d).toFixed(1)
}
function archetypeTagType(name) {
  const map = { '物理核心': 'danger', '法系核心': 'primary', '防御/开团': 'success', '辅助装备': 'warning', '切入/机动': 'info' }
  return map[name] || ''
}

// ---- Lifecycle ----
onMounted(async () => {
  try { const r = await listAll(); accounts.value = r.data } catch {}
})

// ---- Data fetching ----
async function fetchData() {
  if (!form.steamId) { ElMessage.warning('请先选择 Steam 账号'); return }
  if (activeTab.value === 'compare' && !form.heroId) { ElMessage.warning('全球对比需要选择英雄'); return }

  loading.value = true
  try {
    const params = { steamId: form.steamId, heroId: form.heroId || null }
    switch (activeTab.value) {
      case 'starting': {
        const r = await startingItems(params)
        startingData.value = r.data || []
        break
      }
      case 'builds': {
        const r = await itemBuildRoutes({ ...params, topN: buildTopN.value })
        buildData.value = r.data || []
        break
      }
      case 'items': {
        const r = await itemStats(params)
        itemStatsData.value = r.data || []
        break
      }
      case 'contribution': {
        const r = await itemWinContribution(params)
        contributionData.value = r.data || []
        break
      }
      case 'compare': {
        const r = await itemCompareGlobal(params)
        compareResponse.value = r.data
        compareData.value = r.data?.items || []
        break
      }
      case 'archetype': {
        const r = await itemArchetype(params)
        archetypeData.value = r.data || []
        break
      }
    }
  } catch (e) {
    ElMessage.error('查询失败: ' + (e.response?.data?.message || e.message))
  } finally { loading.value = false }
}

function onFilterChange() {
  if (form.steamId) fetchData()
}
function onTabChange(tab) {
  if (form.steamId && tab !== 'compare') fetchData()
  if (tab === 'compare' && form.steamId && form.heroId) fetchData()
}
</script>
