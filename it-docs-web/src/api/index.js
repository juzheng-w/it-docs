import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

// 请求带 JWT
http.interceptors.request.use(cfg => {
  const token = localStorage.getItem('kb_token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

// 401 跳登录；错误统一提示
http.interceptors.response.use(
  resp => {
    const d = resp.data
    if (d && typeof d.code === 'number' && d.code !== 0) {
      ElMessage.error(d.msg || '请求失败')
      return Promise.reject(d)
    }
    return d
  },
  err => {
    const status = err.response?.status
    const msg = err.response?.data?.msg
    if (status === 401) {
      localStorage.removeItem('kb_token')
      localStorage.removeItem('kb_user')
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
    } else if (status === 403 && msg && msg.includes('修改初始密码')) {
      // 后端在强制改密期间拦下除改密外的所有接口，这里把用户送到改密页
      ElMessage.warning(msg)
      if (router.currentRoute.value.path !== '/profile') {
        router.push('/profile')
      }
    } else {
      ElMessage.error(msg || '服务异常，请稍后重试')
    }
    return Promise.reject(err)
  }
)

export default http
