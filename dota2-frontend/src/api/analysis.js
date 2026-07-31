import request from './request'

export const heroStats = (data) => request.post('/analysis/heroStats', data)

export const allHeroWinRate = (params) => request.get('/analysis/allHeroWinRate', { params })
