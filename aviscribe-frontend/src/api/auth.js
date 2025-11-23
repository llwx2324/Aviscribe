import request from './request'

export function registerUser(payload) {
  return request({
    url: '/auth/register',
    method: 'post',
    data: payload
  })
}

export function loginUser(payload) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: payload
  })
}

export function refreshToken(refreshToken) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    data: { refreshToken }
  })
}

export function fetchProfile() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

export function updateProfile(payload) {
  return request({
    url: '/auth/profile',
    method: 'post',
    data: payload
  })
}

export function changePassword(payload) {
  return request({
    url: '/auth/change-password',
    method: 'post',
    data: payload
  })
}
