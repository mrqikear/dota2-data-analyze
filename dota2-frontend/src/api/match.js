import request from './request'
export const syncMatches = (steamId, days, minDate, maxDate) => {
  let url = '/match/sync/' + steamId
  const params = []
  if (days) params.push('days=' + days)
  if (minDate) params.push('minDate=' + minDate)
  if (maxDate) params.push('maxDate=' + maxDate)
  if (params.length) url += '?' + params.join('&')
  return request.get(url)
}
export const syncTurboDate = (steamId, minTime, maxTime) => {
  let url = '/match/syncTurboDate/' + steamId
  const params = []
  if (minTime) params.push('minTime=' + minTime)
  if (maxTime) params.push('maxTime=' + maxTime)
  if (params.length) url += '?' + params.join('&')
  return request.get(url)
}
export const pageMatches = data => request.post('/match/page', data)
export const relatedMatches = (steamIds) => request.post('/match/relatedMatches', steamIds)
export const playerStats = (steamId, params) => request.get('/match/playerStats/' + steamId, { params })
