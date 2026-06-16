// 存储/读取/清除 token 和用户信息

const TOKEN_KEY = 'team_token'
const USER_KEY = 'team_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUser() {
  const str = localStorage.getItem(USER_KEY)
  return str ? JSON.parse(str) : null
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeUser() {
  localStorage.removeItem(USER_KEY)
}

export function isAdmin() {
  const user = getUser()
  return user && user.userType === 0
}

export function canEdit() {
  return isAdmin()
}
