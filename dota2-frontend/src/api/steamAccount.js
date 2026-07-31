import request from './request'
export const pageAccounts = data => request.post('/steamAccount/page', data)
export const listAll = () => request.get('/steamAccount/listAll')
export const syncPlayerInfo = steamId => request.get('/steamAccount/sync/' + steamId)
export const addAccount = data => request.post('/steamAccount/addAccount', data)
export const deleteAccount = data => request.post('/steamAccount/deleteAccount', data)
