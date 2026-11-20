import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('homestay_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    if (response.data?.code && response.data.code !== 200) {
      return Promise.reject(new Error(response.data.message || '请求处理失败'))
    }
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('homestay_token')
      sessionStorage.removeItem('homestay_user')
      localStorage.removeItem('homestay_token')
      localStorage.removeItem('homestay_user')
      const redirect = encodeURIComponent(`${window.location.pathname}${window.location.search}`)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = `/login?redirect=${redirect}`
      }
    }
    const message = error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  }
)

export default http
