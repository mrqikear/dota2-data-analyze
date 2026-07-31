<template>
  <div>
    <!-- Trigger: show selected hero or placeholder -->
    <el-popover placement="bottom-start" :width="480" trigger="click" :visible="popoverVisible" @hide="popoverVisible = false">
      <template #reference>
        <div class="hero-picker-trigger" @click="popoverVisible = !popoverVisible">
          <template v-if="modelValue">
            <el-image :src="heroIconUrl(modelValue)" style="width:32px;height:32px;border-radius:4px" />
            <span style="margin-left:6px;font-size:13px">{{ heroName(modelValue) }}</span>
            <el-icon style="margin-left:4px;color:#909399;font-size:12px"><ArrowDown /></el-icon>
          </template>
          <template v-else>
            <span style="color:#909399;font-size:13px">选择英雄</span>
            <el-icon style="margin-left:4px;color:#909399;font-size:12px"><ArrowDown /></el-icon>
          </template>
        </div>
      </template>

      <!-- Popover content: search + hero grid -->
      <div style="padding:4px 0">
        <el-input
          v-model="searchText"
          placeholder="搜索英雄名称..."
          size="small"
          clearable
          style="margin-bottom:8px"
          :prefix-icon="Search"
        />
        <div style="max-height:360px;overflow-y:auto;display:flex;flex-wrap:wrap;gap:4px">
          <div
            v-for="h in filteredHeroes"
            :key="h.id"
            class="hero-grid-item"
            :class="{ selected: modelValue === h.id }"
            @click="selectHero(h.id)"
          >
            <el-image :src="heroIconUrl(h.id)" style="width:36px;height:36px;border-radius:4px" />
            <span class="hero-grid-name">{{ h.name }}</span>
          </div>
        </div>
        <div v-if="modelValue" style="margin-top:8px;text-align:center;border-top:1px solid #eee;padding-top:8px">
          <el-button size="small" type="info" plain @click="clearHero">清除选择</el-button>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ArrowDown, Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue'])

const popoverVisible = ref(false)
const searchText = ref('')

const HERO_LIST = [
  { id:1,name:'敌法师'},{id:2,name:'斧王'},{id:3,name:'痛苦之源'},{id:4,name:'嗜血狂魔'},{id:5,name:'水晶室女'},
  {id:6,name:'卓尔游侠'},{id:7,name:'撼地者'},{id:8,name:'主宰'},{id:9,name:'米拉娜'},{id:10,name:'变体精灵'},
  {id:11,name:'影魔'},{id:12,name:'幻影长矛手'},{id:13,name:'帕克'},{id:14,name:'帕吉'},{id:15,name:'雷泽'},
  {id:16,name:'沙王'},{id:17,name:'风暴之灵'},{id:18,name:'斯温'},{id:19,name:'小小'},{id:20,name:'复仇之魂'},
  {id:21,name:'风行者'},{id:22,name:'宙斯'},{id:23,name:'昆卡'},{id:25,name:'莉娜'},{id:26,name:'莱恩'},
  {id:27,name:'暗影萨满'},{id:28,name:'大鱼人'},{id:29,name:'潮汐猎人'},{id:30,name:'巫医'},{id:31,name:'巫妖'},
  {id:32,name:'力丸'},{id:33,name:'谜团'},{id:34,name:'修补匠'},{id:35,name:'狙击手'},{id:36,name:'死灵法师'},
  {id:37,name:'术士'},{id:38,name:'兽王'},{id:39,name:'痛苦女王'},{id:40,name:'剧毒术士'},{id:41,name:'虚空假面'},
  {id:42,name:'骷髅王'},{id:43,name:'死亡先知'},{id:44,name:'幻影刺客'},{id:45,name:'帕格纳'},{id:46,name:'圣堂刺客'},
  {id:47,name:'冥界亚龙'},{id:48,name:'月之骑士'},{id:49,name:'龙骑士'},{id:50,name:'戴泽'},
  {id:51,name:'发条技师'},{id:52,name:'拉席克'},{id:53,name:'先知'},{id:54,name:'噬魂鬼'},{id:55,name:'黑暗贤者'},
  {id:56,name:'克林克兹'},{id:57,name:'全能骑士'},{id:58,name:'魅惑魔女'},{id:59,name:'哈斯卡'},{id:60,name:'暗夜魔王'},
  {id:61,name:'育母蜘蛛'},{id:62,name:'赏金猎人'},{id:63,name:'编织者'},{id:64,name:'杰奇洛'},{id:65,name:'蝙蝠骑士'},
  {id:66,name:'陈'},{id:67,name:'幽鬼'},{id:68,name:'远古冰魄'},{id:69,name:'末日使者'},{id:70,name:'熊战士'},
  {id:71,name:'裂魂人'},{id:72,name:'矮人直升机'},{id:73,name:'炼金术师'},{id:74,name:'祈求者'},{id:75,name:'沉默术士'},
  {id:76,name:'黑曜毁灭者'},{id:77,name:'狼人'},{id:78,name:'酒仙'},{id:79,name:'暗影恶魔'},{id:80,name:'德鲁伊'},
  {id:81,name:'混沌骑士'},{id:82,name:'米波'},{id:83,name:'树精卫士'},{id:84,name:'食人魔法师'},{id:85,name:'不朽尸王'},
  {id:86,name:'拉比克'},{id:87,name:'干扰者'},{id:88,name:'司夜刺客'},{id:89,name:'娜迦海妖'},{id:90,name:'光之守卫'},
  {id:91,name:'艾欧'},{id:92,name:'维萨吉'},{id:93,name:'斯拉克'},{id:94,name:'美杜莎'},{id:95,name:'巨魔战将'},
  {id:96,name:'半人马'},{id:97,name:'马格纳斯'},{id:98,name:'伐木机'},{id:99,name:'钢背兽'},{id:100,name:'巨牙海民'},
  {id:101,name:'天怒法师'},{id:102,name:'亚巴顿'},{id:103,name:'上古巨神'},{id:104,name:'军团指挥官'},{id:105,name:'工程师'},
  {id:106,name:'灰烬之灵'},{id:107,name:'大地之灵'},{id:108,name:'孽主'},{id:109,name:'恐怖利刃'},{id:110,name:'凤凰'},
  {id:111,name:'神谕者'},{id:112,name:'寒冬飞龙'},{id:113,name:'天穹守望者'},{id:114,name:'齐天大圣'},
  {id:119,name:'邪影芳灵'},{id:120,name:'石鳞剑士'},{id:121,name:'天涯墨客'},{id:123,name:'森林之友'},
  {id:126,name:'虚无之灵'},{id:128,name:'电炎绝手'},{id:129,name:'玛尔斯'},{id:131,name:'戏命师'},
  {id:135,name:'破晓辰星'},{id:136,name:'玛西'},{id:137,name:'獸'},{id:138,name:'死亡射手'},
  {id:145,name:'凯兹'},{id:155,name:'拉戈'}
]

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

function heroIconUrl(id) {
  const en = HERO_EN[id]
  return en ? `/asset/hero/${en}.png` : ''
}

function heroName(id) {
  const h = HERO_LIST.find(x => x.id === id)
  return h ? h.name : '英雄' + id
}

const filteredHeroes = computed(() => {
  if (!searchText.value) return HERO_LIST
  const q = searchText.value.toLowerCase()
  return HERO_LIST.filter(h => h.name.includes(q) || String(h.id).includes(q))
})

function selectHero(id) {
  emit('update:modelValue', id)
  popoverVisible.value = false
  searchText.value = ''
}

function clearHero() {
  emit('update:modelValue', null)
  popoverVisible.value = false
  searchText.value = ''
}
</script>

<style scoped>
.hero-picker-trigger {
  display:inline-flex;align-items:center;padding:4px 10px;
  border:1px solid #dcdfe6;border-radius:4px;cursor:pointer;
  min-width:120px;height:32px;background:#fff;
  transition:border-color .2s;
}
.hero-picker-trigger:hover { border-color:#409EFF; }
.hero-grid-item {
  display:flex;flex-direction:column;align-items:center;
  width:64px;padding:4px 2px;border-radius:6px;cursor:pointer;
  transition:background .15s;
}
.hero-grid-item:hover { background:#f0f5ff; }
.hero-grid-item.selected { background:#e6f7ff; outline:2px solid #409EFF; outline-offset:-2px; }
.hero-grid-name {
  font-size:11px;color:#606266;margin-top:2px;
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:60px;
}
</style>
