import { reactive, readonly } from 'vue'

const ACCESS_TOKEN_KEY = 'aviscribe_access_token'
const REFRESH_TOKEN_KEY = 'aviscribe_refresh_token'
const ACCESS_EXP_KEY = 'aviscribe_access_exp'
const REFRESH_EXP_KEY = 'aviscribe_refresh_exp'
const PROFILE_KEY = 'aviscribe_user_profile'

function loadProfile() {
  try {
    const raw = localStorage.getItem(PROFILE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (error) {
    console.error('Failed to parse profile from storage', error)
    return null
  }
}

const state = reactive({
  accessToken: localStorage.getItem(ACCESS_TOKEN_KEY) || '',
  refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY) || '',
  accessTokenExpiresAt: Number(localStorage.getItem(ACCESS_EXP_KEY) || 0),
  refreshTokenExpiresAt: Number(localStorage.getItem(REFRESH_EXP_KEY) || 0),
  profile: loadProfile()
})

const EXP_SKEW_MS = 5000

const isTimestampValid = (epochSeconds) => {
  if (!epochSeconds) return false
  return epochSeconds * 1000 > Date.now() + EXP_SKEW_MS
}

const hasValidAccessToken = () => {
  return Boolean(state.accessToken) && isTimestampValid(state.accessTokenExpiresAt)
}

const hasValidRefreshToken = () => {
  return Boolean(state.refreshToken) && isTimestampValid(state.refreshTokenExpiresAt)
}

function persistState() {
  if (state.accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, state.accessToken)
    localStorage.setItem(ACCESS_EXP_KEY, String(state.accessTokenExpiresAt || 0))
  } else {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(ACCESS_EXP_KEY)
  }

  if (state.refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, state.refreshToken)
    localStorage.setItem(REFRESH_EXP_KEY, String(state.refreshTokenExpiresAt || 0))
  } else {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(REFRESH_EXP_KEY)
  }

  if (state.profile) {
    localStorage.setItem(PROFILE_KEY, JSON.stringify(state.profile))
  } else {
    localStorage.removeItem(PROFILE_KEY)
  }
}

function setSession(payload) {
  state.accessToken = payload?.accessToken || ''
  state.refreshToken = payload?.refreshToken || ''
  state.accessTokenExpiresAt = payload?.accessTokenExpiresAt || 0
  state.refreshTokenExpiresAt = payload?.refreshTokenExpiresAt || 0
  state.profile = payload?.profile || null
  persistState()
}

function clearSession() {
  state.accessToken = ''
  state.refreshToken = ''
  state.accessTokenExpiresAt = 0
  state.refreshTokenExpiresAt = 0
  state.profile = null
  persistState()
}

function updateProfile(profile) {
  state.profile = profile
  persistState()
}

function isAuthenticated() {
  if (hasValidAccessToken() || hasValidRefreshToken()) {
    return true
  }
  clearSession()
  return false
}

function getAccessToken() {
  return hasValidAccessToken() ? state.accessToken : ''
}

function getRefreshToken() {
  if (hasValidRefreshToken()) {
    return state.refreshToken
  }
  clearSession()
  return ''
}

export function useAuthStore() {
  return {
    state: readonly(state),
    setSession,
    clearSession,
    updateProfile,
    isAuthenticated,
    getAccessToken,
    getRefreshToken
  }
}
