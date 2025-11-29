import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const authStore = useAuthStore()
const refreshClient = axios.create({
  baseURL: '/api/v1',
  timeout: 60000
})

let isRefreshing = false
const pendingQueue = []

const processQueue = (error, token = null) => {
  pendingQueue.forEach(({ resolve, reject, config }) => {
    if (error) {
      reject(error)
    } else {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
      resolve(service(config))
    }
  })
  pendingQueue.length = 0
}

// 创建一个 Axios 实例，所有请求将自动带上 /api 前缀
const service = axios.create({
  baseURL: '/api/v1', // 所有请求都将是 /api/v1/...
  timeout: 60000 // 60s
})

service.interceptors.request.use(
  (config) => {
    const token = authStore.getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 假设后端成功状态码为 200 或 202
    return response.data
  },
  async error => {
    let message = '请求失败，请稍后重试'
    if (error.response) {
      // 从后端响应中获取错误信息
      if (error.response.data && error.response.data.message) {
        message = error.response.data.message
      } else if (error.response.status === 404) {
        message = 'API 接口不存在'
      } else if (error.response.status === 401) {
        const originalRequest = error.config || {}
        if (originalRequest._retry) {
          authStore.clearSession()
          ElMessage.warning('请先登录')
          router.push({
            name: 'login',
            query: { redirect: router.currentRoute.value.fullPath || '/app' }
          })
          return Promise.reject(error)
        }

        const refreshToken = authStore.getRefreshToken()
        if (!refreshToken) {
          authStore.clearSession()
          ElMessage.warning('请先登录')
          router.push({
            name: 'login',
            query: { redirect: router.currentRoute.value.fullPath || '/app' }
          })
          return Promise.reject(error)
        }

        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            pendingQueue.push({
              resolve,
              reject,
              config: originalRequest
            })
          })
        }

        originalRequest._retry = true
        isRefreshing = true

        try {
          const { data: refreshResponse } = await refreshClient.post('/auth/refresh', { refreshToken })
          authStore.setSession(refreshResponse)
          processQueue(null, refreshResponse.accessToken)
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${refreshResponse.accessToken}`
          return service(originalRequest)
        } catch (refreshError) {
          processQueue(refreshError, null)
          authStore.clearSession()
          ElMessage.warning('请先登录')
          router.push({
            name: 'login',
            query: { redirect: router.currentRoute.value.fullPath || '/app' }
          })
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      } else if (error.response.status >= 500) {
        message = '服务器内部错误'
      }
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service