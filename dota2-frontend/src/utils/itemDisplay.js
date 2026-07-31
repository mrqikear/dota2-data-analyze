// Reverse map: item internal name → { id, displayName? }
// Auto-generated from ITEM_NAMES
import { ITEM_NAMES } from './itemMap'

const _reverseMap = {}
for (const [id, name] of Object.entries(ITEM_NAMES)) {
  _reverseMap[name] = { id: Number(id) }
}

/**
 * Get item info by internal name (as used in purchase_log).
 * @param {string} internalName e.g. "branches", "magic_stick"
 * @returns {{ id: number } | null}
 */
export function getItemInfo(internalName) {
  return _reverseMap[internalName] || null
}

/**
 * Get item icon URL by internal name.
 */
export function itemIconUrl(internalName) {
  if (!internalName) return ''
  return `/asset/item/${internalName}.png`
}

/**
 * Localized item names (Chinese). Only commonly-known items for now.
 */
const ITEM_CN = {
  branches: '铁树枝干',
  magic_stick: '魔棒',
  magic_wand: '魔杖',
  gauntlets: '力量手套',
  slippers: '敏捷便鞋',
  mantle: '智力斗篷',
  circlet: '圆环',
  ring_of_protection: '守护指环',
  ring_of_regen: '回复戒指',
  sobi_mask: '贤者面罩',
  boots: '速度之靴',
  quelling_blade: '补刀斧',
  stout_shield: '圆盾',
  orb_of_venom: '毒球',
  blight_stone: '枯萎之石',
  wind_lace: '风灵之纹',
  crown: '王冠',
  fluffy_hat: '绒毛帽',
  belt_of_strength: '力量腰带',
  boots_of_elves: '精灵布带',
  robe: '法师长袍',
  blade_of_alacrity: '欢欣之刃',
  ogre_axe: '食人魔之斧',
  staff_of_wizardry: '魔力法杖',
  mithril_hammer: '秘银锤',
  broadsword: '阔剑',
  claymore: '大剑',
  chainmail: '锁子甲',
  helm_of_iron_will: '铁意头盔',
  javelin: '标枪',
  gloves: '加速手套',
  ring_of_health: '治疗指环',
  void_stone: '虚无宝石',
  energy_booster: '能量之球',
  point_booster: '精气之球',
  vitality_booster: '活力之球',
  ultimate_orb: '极限法球',
  hyperstone: '振奋宝石',
  demon_edge: '恶魔刀锋',
  eagle: '鹰歌弓',
  reaver: '掠夺者之斧',
  relic: '圣者遗物',
  mystic_staff: '神秘法杖',
  aghanims_shard: '阿哈利姆魔晶',
  blood_grenade: '血棘之 grenade',
  faerie_fire: '仙灵之火',
  enchanted_mango: '芒果',
  tpscroll: '回城卷轴',
  smoke_of_deceit: '诡计之雾',
  ward_observer: '侦查守卫',
  ward_sentry: '真视守卫',
  dust: '显影之尘',
  clarity: '净化药水',
  flask: '治疗药膏',
  tango: '吃树',
  bottle: '魔瓶',
  cheese: '奶酪',
  diadem: '头冠',
  blitz_knuckles: '雷火拳套',
  voodoo_mask: '巫毒面具',
  cornucopia: '聚宝盆',
  orb_of_corrosion: '腐蚀之球',
  falcon_blade: '猎鹰战刃',
  mage_slayer: '法师克星',
  echo_sabre: '回音战刃',
  phylactery: '护符',
  disperser: '清莲宝珠'
}

export function itemDisplayName(internalName) {
  return ITEM_CN[internalName] || internalName.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}
