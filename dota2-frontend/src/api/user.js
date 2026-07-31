import request from './request'
export const login = data => request.post('/user/login', data)
export const getCurrentUser = () => request.get('/user/current')
export const pageUsers = data => request.post('/user/page', data)
export const addUser = data => request.post('/user/addUser', data)
export const editUser = data => request.post('/user/editUser', data)
export const deleteUser = data => request.post('/user/deleteUser', data)
