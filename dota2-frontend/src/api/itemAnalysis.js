import request from './request'

/**
 * 4.1 出门装组合
 * @param {Object} params - { steamId, heroId? }
 */
export function startingItems(params) {
  return request.post('/itemAnalysis/startingItems', params)
}

/**
 * 4.2 大件出装路线
 * @param {Object} params - { steamId, heroId?, topN }
 */
export function itemBuildRoutes(params) {
  const { topN, ...body } = params
  return request.post('/itemAnalysis/buildRoutes', body, { params: { topN } })
}

/**
 * 4.3 单件装备分析
 * @param {Object} params - { steamId, heroId? }
 */
export function itemStats(params) {
  return request.post('/itemAnalysis/itemStats', params)
}

/**
 * 4.4 装备胜率贡献
 * @param {Object} params - { steamId, heroId? }
 */
export function itemWinContribution(params) {
  return request.post('/itemAnalysis/winContribution', params)
}

/**
 * 4.5 个人 vs 全球对比
 * @param {Object} params - { steamId, heroId }
 */
export function itemCompareGlobal(params) {
  return request.post('/itemAnalysis/compareGlobal', params)
}

/**
 * 4.6 流派识别
 * @param {Object} params - { steamId, heroId? }
 */
export function itemArchetype(params) {
  return request.post('/itemAnalysis/archetype', params)
}
