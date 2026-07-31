<template>
  <div>
    <el-button size="small" style="margin-bottom:16px" @click="$router.back()">&larr; 返回列表</el-button>
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:600">比赛详情 #{{ matchId }}</span>
          <el-button v-if="!detail" type="primary" size="small" :loading="fetching" @click="handleFetch">从 OpenDota 获取</el-button>
          <el-button v-if="detail && detail.syncStatus !== 2" type="warning" size="small" :loading="requesting" @click="handleRequestParse">请求解析</el-button>
          <el-tag v-if="detail && detail.syncStatus === 2" type="success" size="small">已同步</el-tag>
          <el-tag v-else-if="detail && detail.syncStatus === -1" type="danger" size="small">同步失败</el-tag>
        </div>
      </template>

      <!-- No detail yet -->
      <el-empty v-if="!detail && !loading" description="暂无比赛数据">
        <template #description>
          <span>暂无比赛数据</span>
          <p style="color:#909399;font-size:13px;margin-top:8px">点击游戏详情或 OpenDota 获取按钮获取</p>
        </template>
      </el-empty>

      <!-- Detail loaded -->
      <template v-if="detail && detail.syncStatus === 2">
        <!-- Match Info -->
        <el-descriptions title="比赛信息" :column="3" border style="margin-bottom:20px">
          <el-descriptions-item label="比赛ID">{{ detail.matchId }}</el-descriptions-item>
          <el-descriptions-item label="比赛时间">{{ formatUnix(startTime) }}</el-descriptions-item>
          <el-descriptions-item label="时长">{{ formatDuration(detail.duration) }}</el-descriptions-item>
          <el-descriptions-item label="天辉胜">
            <el-tag :type="detail.radiantWin ? 'success' : 'danger'" size="small">
              {{ detail.radiantWin ? '胜' : '负' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="游戏模式">{{ gameModeLabel(detail.gameMode) }}</el-descriptions-item>
          <el-descriptions-item label="房间类型">{{ lobbyTypeLabel(detail.lobbyType) }}</el-descriptions-item>
          <el-descriptions-item label="解析状态">
            <el-tag v-if="detail.rawJson && detail.rawJson.includes('damage_inflictor')" type="success" size="small">完整解析</el-tag>
            <el-tag v-else-if="detail.rawJson" type="warning" size="small">基础数据</el-tag>
            <el-tag v-else type="info" size="small">未获取</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="一血时间">{{ detail.firstBloodTime ? detail.firstBloodTime + 's' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="天辉击杀">{{ detail.radiantScore }}</el-descriptions-item>
          <el-descriptions-item label="夜魇击杀">{{ detail.direScore }}</el-descriptions-item>
          <el-descriptions-item label="总击杀">{{ (detail.radiantScore || 0) + (detail.direScore || 0) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.radiantName" label="天辉队名">{{ detail.radiantName }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.direName" label="夜魇队名">{{ detail.direName }}</el-descriptions-item>
        </el-descriptions>

        <!-- Players Table -->
        <div style="margin-bottom:12px;font-weight:600;font-size:15px">参赛玩家</div>
        <div style="overflow-x:auto">
        <el-table :data="players" stripe border style="width:100%" size="small">
          <el-table-column type="expand" label=" " width="50" align="center">
            <template #default="{ row }">
              <div style="padding:12px 24px">
                <div style="font-size:13px;color:#606266;margin-bottom:8px">技能加点顺序（每级升级的技能）：</div>
                <div style="display:flex;flex-wrap:wrap;gap:2px">
                  <div v-for="(aid, idx) in row.abilityUpgrades" :key="idx"
                    :title="'Lv' + (idx + 1) + ': ' + abilityDName(aid)"
                    style="display:flex;flex-direction:column;align-items:center;width:42px;padding:4px 2px;
                      border-radius:4px;background:#f5f7fa;"
                    :class="{ 'ability-talent': isTalent(aid) }">
                    <el-image v-if="abilityIcon(aid)"
                      :src="abilityIcon(aid)"
                      style="width:26px;height:26px;border-radius:3px"
                      :style="{ border: isTalent(aid) ? '2px solid #E6A23C' : '2px solid #DCDFE6' }" />
                    <span v-else style="width:26px;height:26px;border-radius:3px;border:2px solid #DCDFE6;
                      display:flex;align-items:center;justify-content:center;font-size:10px;color:#909399">?</span>
                    <span style="font-size:10px;color:#909399;margin-top:2px">Lv{{ idx + 1 }}</span>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="#" width="40" align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="玩家" min-width="160">
            <template #default="{ row }">
              <div style="font-size:12px">
                <div v-if="row.nickName" style="font-weight:500;color:#303133">{{ row.nickName }}</div>
                <div style="color:#909399;font-size:11px">{{ row.steamId }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="阵营" width="60" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isRadiant ? 'success' : 'danger'" size="small">{{ row.isRadiant ? '天辉' : '夜魇' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="英雄" width="90" align="center">
            <template #default="{ row }">
              <div style="position:relative;display:inline-block;padding-top:4px">
                <el-image v-if="row.heroId" :src="heroIcon(row.heroId)" style="width:32px;height:32px;border-radius:4px" />
                <div v-if="mvpInfo && row.steamId === mvpInfo.steamId" style="position:absolute;top:0;left:50%;transform:translateX(-50%);background:#E6A23C;color:#fff;font-size:9px;padding:0 4px;border-radius:3px;font-weight:700;line-height:14px;white-space:nowrap">MVP</div>
                <div v-if="fmvpInfo && row.steamId === fmvpInfo.steamId" style="position:absolute;top:0;left:50%;transform:translateX(-50%);background:#909399;color:#fff;font-size:9px;padding:0 4px;border-radius:3px;font-weight:700;line-height:14px;white-space:nowrap">FMVP</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="KDA" width="130" align="center">
            <template #default="{ row }">
              <span :style="{color: row.kills >= row.deaths ? '#67C23A' : '#F56C6C'}">{{ row.kills }}</span>
              / <span style="color:#909399">{{ row.deaths }}</span>
              / <span :style="{color: row.assists > 5 ? '#67C23A' : '#909399'}">{{ row.assists }}</span>
            </template>
          </el-table-column>
          <el-table-column label="GPM" width="60" align="center">
            <template #default="{ row }">{{ row.goldPerMin }}</template>
          </el-table-column>
          <el-table-column label="XPM" width="60" align="center">
            <template #default="{ row }">{{ row.xpPerMin }}</template>
          </el-table-column>
          <el-table-column label="等级" width="50" align="center">
            <template #default="{ row }">{{ row.level }}</template>
          </el-table-column>
          <el-table-column label="正补/反补" width="90" align="center">
            <template #default="{ row }">{{ row.lastHits }} / {{ row.denies }}</template>
          </el-table-column>
          <el-table-column label="伤害" width="80" align="center">
            <template #default="{ row }">{{ formatNum(row.heroDamage) }}</template>
          </el-table-column>
          <el-table-column label="承伤" width="80" align="center">
            <template #default="{ row }">{{ formatNum(row.damageTaken || row.damage_taken || 0) }}</template>
          </el-table-column>
          <el-table-column label="推塔" width="70" align="center">
            <template #default="{ row }">{{ formatNum(row.towerDamage) }}</template>
          </el-table-column>
          <el-table-column label="治疗" width="70" align="center">
            <template #default="{ row }">{{ formatNum(row.heroHealing) }}</template>
          </el-table-column>
          <el-table-column label="金钱" width="80" align="center">
            <template #default="{ row }">{{ formatNum(row.gold) }}</template>
          </el-table-column>
          <el-table-column label="插眼" width="60" align="center"><template #default="{ row }">{{ row.obs_placed || 0 }}</template></el-table-column>
          <el-table-column label="排眼" width="60" align="center"><template #default="{ row }">{{ row.observer_kills || 0 }}</template></el-table-column>
          <el-table-column label="真眼" width="60" align="center"><template #default="{ row }">{{ row.sen_placed || 0 }}</template></el-table-column>
          <el-table-column label="反真" width="60" align="center"><template #default="{ row }">{{ row.sentry_kills || 0 }}</template></el-table-column>
          <el-table-column label="TP" width="50" align="center"><template #default="{ row }">{{ row.tp_count || 0 }}</template></el-table-column>
          <el-table-column label="伤害源" width="60" align="center" fixed="right">
            <template #default="{ row }">
              <el-popover placement="left" :width="320" trigger="click">
                <template #reference>
                  <el-button size="small" circle type="warning" plain style="font-size:10px">详</el-button>
                </template>
                <div style="padding:4px;max-height:400px;overflow-y:auto">
                  <div style="font-weight:600;font-size:14px;margin-bottom:8px">伤害来源分析</div>
                  <div v-if="row.damageInflictor && row.damageInflictor.length">
                    <div style="font-size:12px;font-weight:500;color:#E6A23C;margin-bottom:4px">┃ 造成伤害 ({{ totalDamage(row.damageInflictor) }})</div>
                    <div v-if="row.damageInflictor.some(d=>d.dmgType)" style="display:flex;gap:8px;margin-bottom:6px;font-size:11px">
                      <span style="color:#67C23A">■ 物理 {{ sumByType(row.damageInflictor,1) }}</span>
                      <span style="color:#409EFF">■ 魔法 {{ sumByType(row.damageInflictor,2) }}</span>
                      <span style="color:#E6A23C">■ 纯粹 {{ sumByType(row.damageInflictor,3) }}</span>
                    </div>
                    <div v-for="d in row.damageInflictor.slice(0,20)" :key="d.name"
                      style="display:flex;align-items:center;justify-content:space-between;font-size:12px;padding:2px 4px;border-bottom:1px solid #f0f0f0">
                      <div style="display:flex;align-items:center;gap:4px;flex:1;min-width:0">
                        <el-image v-if="d.icon" :src="d.icon" style="width:18px;height:18px;border-radius:2px;flex-shrink:0">
                          <template #error><span style="width:18px;height:18px;display:inline-block;background:#f0f0f0;border-radius:2px"></span></template>
                        </el-image>
                        <span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ d.display }}</span>
                      </div>
                      <span style="font-weight:600;min-width:60px;text-align:right;flex-shrink:0">{{ formatNum(d.damage) }}</span>
                    </div>
                  </div>
                  <div v-if="row.damageInflictorReceived && row.damageInflictorReceived.length" style="margin-top:10px">
                    <div style="font-size:12px;font-weight:500;color:#F56C6C;margin-bottom:4px">┃ 受到伤害 ({{ totalDamage(row.damageInflictorReceived) }})</div>
                    <div v-for="d in row.damageInflictorReceived.slice(0,20)" :key="d.name"
                      style="display:flex;align-items:center;justify-content:space-between;font-size:12px;padding:2px 4px;border-bottom:1px solid #f0f0f0">
                      <div style="display:flex;align-items:center;gap:4px;flex:1;min-width:0">
                        <el-image v-if="d.icon" :src="d.icon" style="width:18px;height:18px;border-radius:2px;flex-shrink:0">
                          <template #error><span style="width:18px;height:18px;display:inline-block;background:#f0f0f0;border-radius:2px"></span></template>
                        </el-image>
                        <span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ d.display }}</span>
                      </div>
                      <span style="font-weight:600;min-width:60px;text-align:right;flex-shrink:0">{{ formatNum(d.damage) }}</span>
                    </div>
                  </div>
                  <div v-if="!row.damageInflictor && !row.damageInflictorReceived" style="color:#909399;font-size:12px;padding:8px 0">暂无解析数据，等待 OpenDota 解析完成后自动显示</div>
                </div>
              </el-popover>
            </template>
          </el-table-column>
          <el-table-column label="装备" min-width="200">
            <template #default="{ row }">
              <div style="display:flex;gap:3px;flex-wrap:wrap">
                <el-tooltip v-for="(item, i) in row.items" :key="i" :content="itemName(item)">
                  <el-image v-if="item" :src="itemIcon(item)" style="width:28px;height:28px;border-radius:3px;border:1px solid #ddd" />
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="背包" min-width="90">
            <template #default="{ row }">
              <div style="display:flex;gap:2px">
                <el-tooltip v-for="(item, i) in row.backpack" :key="i" :content="itemName(item)">
                  <el-image v-if="item" :src="itemIcon(item)" style="width:20px;height:20px;border-radius:2px;border:1px solid #ddd" />
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
        </div>

        <!-- Picks & Bans -->
        <div v-if="picksBans.length" style="margin-top:20px">
          <div style="margin-bottom:12px;font-weight:600;font-size:15px">选人 & Ban</div>
          <el-table :data="picksBans" stripe border style="width:100%" size="small">
            <el-table-column label="顺序" width="50" align="center">
              <template #default="{ row }">{{ row.order }}</template>
            </el-table-column>
            <el-table-column label="阵营" width="60" align="center">
              <template #default="{ row }">
                <el-tag :type="row.team === 0 ? 'success' : 'danger'" size="small">{{ row.team === 0 ? '天辉' : '夜魇' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="英雄" width="60" align="center">
              <template #default="{ row }">
                <el-image v-if="row.heroId" :src="heroIcon(row.heroId)" style="width:28px;height:28px;border-radius:4px" />
              </template>
            </el-table-column>
            <el-table-column label="英雄名" width="120">
              <template #default="{ row }">{{ row.heroName || heroName(row.heroId) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="60" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isPick ? 'primary' : 'info'" size="small">{{ row.isPick ? '选' : 'Ban' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- Raw JSON -->
        <el-collapse style="margin-top:16px">
          <el-collapse-item title="原始 JSON（OpenDota 全量数据）" name="raw">
            <pre style="max-height:500px;overflow:auto;background:#f5f7fa;padding:12px;border-radius:4px;font-size:12px;white-space:pre-wrap">{{ formatJson(detail.rawJson) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMatchDetail, fetchMatchDetail, requestParse } from '@/api/matchDetail'
import { heroName } from '@/utils/heroes'
import { GAME_MODE_LABELS, LOBBY_TYPE_LABELS } from '@/utils/matchTypes'
import { ITEM_NAMES } from '@/utils/itemMap'
import { ABILITY_IDS } from '@/utils/abilityIds'
import dayjs from 'dayjs'

// Build lookup: ability internal name → { display, icon }
const ABILITY_LOOKUP = {}
for (const [id, val] of Object.entries(ABILITY_IDS)) {
  if (val.n && val.n.startsWith('item_')) continue // skip items
  if (val.n) ABILITY_LOOKUP[val.n] = val
}
// Build lookup: item internal name → icon
const ITEM_LOOKUP = {}
for (const [id, name] of Object.entries(ITEM_NAMES)) {
  ITEM_LOOKUP[name] = { icon: name }
}

// Damage source display names
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
  'abyssal_blade': '深渊之刃',
  'sange_and_yasha': '双刀',
  'skull_basher': '碎颅锤',
  'crimson_guard': '赤红甲',
  'pipe': '笛子',
  'aev_disk': '永恒之盘',
  'heart': '龙心',
  'assault_cuirass': '强袭',
  'desolator': '黯灭',
  'satanic': '撒旦',
  'monkey_king_bar': '大炮',
  'butterfly': '蝴蝶',
  'daedalus': '金箍棒',
  'silver_edge': '大隐刀',
  'divine_rapier': '圣剑',
  'skadi': '冰眼',
  'diffusal_blade': '散失',
  'ethereal_blade': '虚灵刀',
  'dagon': '红杖',
  'spirit_vessel': '大骨灰',
  'solar_crest': '大勋章',
  'vladmir': '祭品',
  'helm_of_the_dominator': '支配',
  'bottle': '魔瓶',
  'soul_ring': '魂戒',
  // Most common hero abilities from damage_inflictor
  'ogre_magi_fireblast': '火焰爆轰',
  'ogre_magi_ignite': '引燃',
  'pudge_rot': '腐烂',
  'pudge_dismember': '肢解',
  'pudge_meat_hook': '肉钩',
  'zuus_lightning_bolt': '雷击',
  'zuus_arc_lightning': '弧形闪电',
  'zuus_thundergods_wrath': '雷神之怒',
  'zuus_static_field': '静电场',
  'skeleton_king_vampiric_spirit': '吸血灵魂',
  'skeleton_king_hellfire_blast': '冥火爆击',
  'centaur_stampede': '马蹄践踏',
  'queenofpain_shadow_strike': '暗影突袭',
  'queenofpain_sonic_wave': '超声冲击波',
  'queenofpain_scream_of_pain': '痛苦尖叫',
  'ember_spirit_activate_fire_remnant': '激活火余烬',
  'ember_spirit_searing_chains': '锁链',
  'ember_spirit_flame_guard': '火焰护盾',
  'ember_spirit_sleight_of_fist': '无影拳',
  'rubick_fade_bolt': '弱化能流',
  'lion_finger_of_death': '死亡之指',
  'lion_impale': '穿刺',
  'juggernaut_omni_slash': '无敌斩',
  'juggernaut_blade_fury': '剑刃风暴',
  'nevermore_shadowraze1': '毁灭阴影(近)',
  'nevermore_shadowraze2': '毁灭阴影(中)',
  'nevermore_shadowraze3': '毁灭阴影(远)',
  'nevermore_requiem': '魂之挽歌',
  'axe_battle_hunger': '战斗饥渴',
  'axe_counter_helix': '反击螺旋',
  'axe_culling_blade': '淘汰之刃',
  'sniper_shrapnel': '榴霰弹',
  'sniper_assassinate': '暗杀',
  'earthshaker_fissure': '沟壑',
  'skywrath_mage_arcane_bolt': '奥术箭',
  'ancient_apparition_ice_blast': '冰晶爆轰',
  'furion_wrath_of_nature': '自然之怒',
  'furion_sprout': '发芽',
  'tusk_snowball': '雪球',
  'tusk_ice_shards': '冰碎片',
  'faceless_void_time_lock': '时间锁定',
  'faceless_void_time_dilation': '时间膨胀',
  'crystal_maiden_freezing_field': '极寒领域',
  'crystal_maiden_frostbite': '冰霜禁制',
  'crystal_maiden_crystal_nova': '冰晶新星',
  'rubick_telekinesis': '隔空取物',
  'rubick_fade_bolt': '弱化能流',
  'cloak_of_flames': '火焰斗篷',
  'chipped_vest': '碎裂背心',
  'overwhelming_blink': '回响跳刀',
  'searing_signet': '炽烈指环',
  'conjurers_catalyst': '咒术师催化石',
  'stormcrafter': '风暴工匠',
  'hydras_breath': '九头蛇吐息',
  'orb_of_venom': '毒球',
  'bfury': '狂战斧',
  'cyclone': '吹风',
  'urn_of_shadows': '骨灰盒',
  'bloodthorn': '血棘',
  'spirit_vessel': '大骨灰',
  'dagon_5': '红杖5级',
  'phylactery': '护符',
  'immolation': '辉耀灼烧',
  'crippling_crossbow': '致残弩炮',
}

// Damage type lookup: ability name => dmg_type (1=phys, 2=magic, 3=pure)
let damageTypeLookup = null
async function ensureDamageTypeLookup() {
  if (damageTypeLookup) return
  try {
    const r = await fetch('/api/constants/abilities')
    const d = await r.json()
    if (d.code === '000000' && d.data) {
      const raw = JSON.parse(d.data)
      damageTypeLookup = {}
      for (const [key, val] of Object.entries(raw)) {
        if (val.dmg_type && val.dmg_type !== '0') {
          damageTypeLookup[key] = Number(val.dmg_type)
        }
      }
    }
  } catch {}
}

function getDamageType(name) {
  if (name === 'null') return 1
  if (name === 'undefined') return 0
  if (ITEM_LOOKUP[name]) {
    const itemDmg = { 'radiance':2,'urn_shard':2,'blade_mail':1,'lotus_orb':2,
      'mjollnir':2,'maelstrom':2,'shivas_guard':2,'blood_grenade':2,
      'immolation':2,'cloak_of_flames':2,'chipped_vest':1,'orb_of_venom':2,
      'spirit_vessel':2,'urn_of_shadows':2,'bfury':1,'cyclone':2,
      'overwhelming_blink':2,'searing_signet':2,'stormcrafter':2,
      'hydras_breath':2,'crippling_crossbow':1 }
    return itemDmg[name] || 0
  }
  if (damageTypeLookup && damageTypeLookup[name]) return damageTypeLookup[name]
  return 0
}

function sumByType(list, type) {
  const t = list.filter(d => d.dmgType === type).reduce((s, d) => s + d.damage, 0)
  if (t >= 10000) return (t / 10000).toFixed(1) + '万'
  return t.toLocaleString()
}

function parseDamageMap(dmgObj) {
  if (!dmgObj || typeof dmgObj !== 'object') return []
  return Object.entries(dmgObj)
    .map(([k, v]) => {
      let display = DAMAGE_NAMES[k]
      let icon = null
      let type = 'unknown'
      if (k === 'null') { display = '普通攻击'; type = 'attack'; icon = '/asset/item/blades_of_attack.png' }
      else if (k === 'undefined') { display = '未知来源'; type = 'unknown'; icon = null }
      // Check items
      else if (ITEM_LOOKUP[k]) { icon = `/asset/item/${k}.png`; type = 'item' }
      // Check abilities (use icon + English name as fallback)
      else if (ABILITY_LOOKUP[k]) {
        const a = ABILITY_LOOKUP[k]
        if (!display) display = a.d
        if (a.i) icon = `/asset/ability/${a.i}.png`
        type = 'ability'
      }
      // Fallback: format key name (if no Chinese name and no English name)
      if (!display) display = formatKeyName(k)
      const dmgType = getDamageType(k)
      const dmgTypeLabel = ['', '物理', '魔法', '纯粹'][dmgType] || ''
      const dmgTypeColor = ['', '#67C23A', '#409EFF', '#E6A23C'][dmgType] || ''
      return {
        name: k, display, icon, type, dmgType, dmgTypeLabel, dmgTypeColor,
        damage: typeof v === 'number' ? v : (typeof v === 'object' ? Object.values(v).reduce((a,b) => a+b, 0) : 0)
      }
    })
    .filter(d => d.damage > 0)
    .sort((a, b) => b.damage - a.damage)
}

function formatKeyName(k) {
  if (k === 'null') return '普通攻击'
  return k.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

// Parse raw JSON from OpenDota to extract players, picks/bans
function parseMatchJson(rawJson) {
  if (!rawJson) return { players: [], picksBans: [] }
  try {
    const data = typeof rawJson === 'string' ? JSON.parse(rawJson) : rawJson
    const players = (data.players || []).map(p => ({
      steamId: p.account_id ? String(BigInt(76561197960265728) + BigInt(p.account_id)) : '',
      accountId: p.account_id || 0,
      personaname: p.personaname || '',
      isRadiant: (p.player_slot || 0) < 128,
      heroId: p.hero_id,
      kills: p.kills || 0,
      deaths: p.deaths || 0,
      assists: p.assists || 0,
      goldPerMin: p.gold_per_min || 0,
      xpPerMin: p.xp_per_min || 0,
      level: p.level || 1,
      lastHits: p.last_hits || 0,
      denies: p.denies || 0,
      heroDamage: p.hero_damage || 0,
      damageTaken: typeof p.damage_taken === 'object' && p.damage_taken ? Object.values(p.damage_taken).reduce((a,b) => a + (typeof b === 'number' ? b : 0), 0) : (p.damage_taken || 0),
      towerDamage: p.tower_damage || 0,
      heroHealing: p.hero_healing || 0,
      gold: p.gold || 0,
      lane: p.lane || '',
      laneRole: p.lane_role || 0,
      items: [p.item_0, p.item_1, p.item_2, p.item_3, p.item_4, p.item_5],
      backpack: [p.backpack_0, p.backpack_1, p.backpack_2],
      obs_placed: typeof p.obs === 'object' && p.obs ? Object.keys(p.obs).length : (p.obs_placed || p.observers_placed || 0),
      observer_kills: p.observer_kills || 0,
      sen_placed: typeof p.sen === 'object' && p.sen ? Object.keys(p.sen).length : (p.sen_placed || 0),
      sentry_kills: p.sentry_kills || 0,
      tp_count: p.purchase_tpscroll || 0,
      abilityUpgrades: p.ability_upgrades_arr || [],
      // Damage breakdown (from fully parsed matches)
      damageInflictor: p.damage_inflictor ? parseDamageMap(p.damage_inflictor) : null,
      damageInflictorReceived: p.damage_inflictor_received ? parseDamageMap(p.damage_inflictor_received) : null,
      itemUses: p.item_uses || null,
    }))
    const picksBans = (data.picks_bans || []).map(pb => ({
      order: pb.order || 0,
      team: pb.team || 0,
      heroId: pb.hero_id,
      isPick: pb.is_pick,
      heroName: '',
    }))
    return { players, picksBans }
  } catch (e) {
    return { players: [], picksBans: [] }
  }
}

const route = useRoute()
const matchId = ref(route.params.matchId)
const loading = ref(false)
const fetching = ref(false)
const requesting = ref(false)
const detail = ref(null)
const players = ref([])
const picksBans = ref([])
const mvpInfo = ref(null)
const fmvpInfo = ref(null)
const startTime = ref(0)

onMounted(async () => {
  await ensureDamageTypeLookup() // Preload damage type data
  loading.value = true
  try {
    const r = await getMatchDetail(matchId.value)
    detail.value = r.data
    if (detail.value && detail.value.detail && detail.value.detail.rawJson) {
      const detailObj = detail.value.detail
      const playerInfos = (detail.value.players || []).reduce((map, p) => {
        map[p.steamId] = p
        return map
      }, {})
      mvpInfo.value = r.data.mvp || null
      fmvpInfo.value = r.data.fmvp || null
      startTime.value = r.data.startTime || 0
      const parsed = parseMatchJson(detailObj.rawJson)
      // Merge backend player info (nickName, steamId) with rawJson parsed data
      players.value = parsed.players.map(p => ({
        ...p,
        nickName: playerInfos[p.steamId]?.nickName || p.personaname || '',
      }))
      picksBans.value = parsed.picksBans
      detail.value = detailObj
    } else if (detail.value && detail.value.rawJson) {
      // old response format (direct entity)
      const parsed = parseMatchJson(detail.value.rawJson)
      players.value = parsed.players
      picksBans.value = parsed.picksBans
    }
  } catch (e) {
    ElMessage.error('获取失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
})

async function handleRequestParse() {
  requesting.value = true
  try {
    await requestParse(matchId.value)
    ElMessage.success('已请求 OpenDota 解析，请 3-5 分钟后刷新查看')
  } catch (e) {
    ElMessage.error('请求失败: ' + (e.response?.data?.message || e.message))
  } finally {
    requesting.value = false
  }
}

async function handleFetch() {
  fetching.value = true
  try {
    await fetchMatchDetail(matchId.value)
    ElMessage.success('数据已获取，请稍后刷新页面查看')
    setTimeout(() => {
      window.location.reload()
    }, 3000)
  } catch (e) {
    ElMessage.error('获取失败: ' + (e.response?.data?.message || e.message))
  } finally {
    fetching.value = false
  }
}

function formatUnix(ts) {
  if (!ts) return '-'
  return dayjs.unix(ts).format('YYYY-MM-DD HH:mm')
}

function formatDuration(sec) {
  if (!sec) return '-'
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return m + ':' + String(s).padStart(2, '0')
}

function totalDamage(list) {
  if (!list || !list.length) return '0'
  const total = list.reduce((s, d) => s + d.damage, 0)
  if (total >= 10000) return (total / 10000).toFixed(1) + '万'
  return total.toLocaleString()
}

function formatNum(n) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

function formatJson(json) {
  if (!json) return ''
  try {
    const obj = typeof json === 'string' ? JSON.parse(json) : json
    return JSON.stringify(obj, null, 2)
  } catch {
    return json
  }
}

function gameModeLabel(mode) {
  return GAME_MODE_LABELS[mode] || '未知(' + mode + ')'
}

function lobbyTypeLabel(type) {
  return LOBBY_TYPE_LABELS[type] || '未知(' + type + ')'
}

function heroIcon(id) {
  const name = heroName(id)
  if (name) {
    return '/asset/hero/' + name + '.png'
  }
  return ''
}

function itemIcon(id) {
  if (!id) return ''
  return '/asset/item/' + getItemName(id) + '.png'
}

function itemName(id) {
  if (!id) return ''
  return getItemName(id)
}

function getItemName(id) {
  return ITEM_NAMES[id] || ''
}

function abilityDName(id) {
  const a = ABILITY_IDS[id]
  if (!a) return '未知技能(' + id + ')'
  return a.d || a.n
}

function abilityIcon(id) {
  const a = ABILITY_IDS[id]
  if (!a || !a.i) return ''
  return '/asset/ability/' + a.i + '.png'
}

function isTalent(id) {
  const a = ABILITY_IDS[id]
  return a && a.n && a.n.startsWith('special_bonus')
}
</script>

<style scoped>
.ability-talent {
  background: #fdf6ec !important;
}
</style>
