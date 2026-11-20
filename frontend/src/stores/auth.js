import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import http from '../api/http'

const TOKEN_KEY = 'homestay_token'
const USER_KEY = 'homestay_user'

function clearLegacyPersistentSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

clearLegacyPersistentSession()

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) || '')
  const user = ref(readUser())

  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const displayName = computed(() => user.value?.realName || user.value?.username || '员工')

  function readUser() {
    try {
      return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null')
    } catch {
      return null
    }
  }

  async function login(credentials) {
    const response = await http.post('/auth/login', credentials)
    token.value = response.data.token
    user.value = response.data.user
    sessionStorage.setItem(TOKEN_KEY, token.value)
    sessionStorage.setItem(USER_KEY, JSON.stringify(user.value))
    return user.value
  }

  async function logout() {
    try {
      if (token.value) {
        await http.post('/auth/logout')
      }
    } finally {
      clearSession()
    }
  }

  function clearSession() {
    token.value = ''
    user.value = null
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
    clearLegacyPersistentSession()
  }

  return {
    token,
    user,
    isAdmin,
    displayName,
    login,
    logout,
    clearSession
  }
})
