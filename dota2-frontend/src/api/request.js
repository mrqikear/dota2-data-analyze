import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'
import router from '@/router'

const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use(config => {
  const t = getToken()
  if (t) config.headers.ssoToken = t
  return config
})

request.interceptors.response.use(
  res => {
    const d = res.data
    if (d.code !== '000000') { ElMessage.error(d.message || '请求失败'); return Promise.reject(new Error(d.message)) }
    return d
  },
  err => {
    if (err.response?.status === 401) { removeToken(); router.push('/login'); ElMessage.error('登录已过期') }
    else { ElMessage.error(err.message || '网络错误') }
    return Promise.reject(err)
  }
)
export default request
