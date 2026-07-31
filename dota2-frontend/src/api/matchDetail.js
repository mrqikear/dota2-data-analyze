import request from './request'

export const getMatchDetail = matchId => request.get('/match/detail/' + matchId)
export const fetchMatchDetail = matchId => request.post('/match/detail/fetch/' + matchId)
export const requestParse = matchId => request.post('/match/detail/request/' + matchId)
export const getMatchDamage = matchId => request.get('/match/detail/damage/' + matchId)
