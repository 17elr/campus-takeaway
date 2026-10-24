import { reactive } from 'vue'

export const session = reactive({ user: null, ready: false })
export const notice = reactive({ message: '', kind: 'success' })
let csrfToken = ''
export function tell(message, kind = 'success') { Object.assign(notice, { message, kind }) }

export async function csrf() {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
  const body = await response.json()
  if (!response.ok) throw Object.assign(new Error(body.message || '登录服务暂时不可用'), { status: response.status })
  csrfToken = body.token
}

export async function api(path, { method = 'GET', body, signal } = {}) {
  const write = method !== 'GET'
  if (write && !csrfToken) await csrf()
  const response = await fetch(`/api${path}`, {
    method, signal, credentials: 'same-origin',
    headers: { ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}), ...(write ? { 'X-CSRF-Token': csrfToken } : {}) },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  })
  const result = await response.json()
  if (!response.ok) {
    if (response.status === 401) session.user = null
    if (response.status === 403) csrfToken = ''
    throw Object.assign(new Error(result.message || '请求失败，请稍后重试'), { status: response.status, fields: result.fields || {} })
  }
  if (result.token) csrfToken = result.token
  return result
}

export async function initialize() {
  try { session.user = await api('/auth/me') }
  catch (error) { if (error.status !== 401) tell(error.message, 'error') }
  finally { session.ready = true }
}
export async function logout() {
  await api('/auth/logout', { method: 'POST' })
  session.user = null
  csrfToken = ''
}
export const money = value => Number(value || 0).toFixed(2)
export const labels = { UNPAID: '待支付', PENDING_ACCEPT: '待接单', MAKING: '制作中', READY: '待配送', DELIVERING: '配送中', WAIT_CONFIRM: '待确认', COMPLETED: '已完成', CANCELED: '已取消' }
export const roles = { STUDENT: '学生', MERCHANT: '商家', RIDER: '骑手', ADMIN: '管理员' }
export const landing = role => ({ STUDENT: '/student/shops', MERCHANT: '/merchant/orders', RIDER: '/rider/available', ADMIN: '/admin/users' }[role] || '/')
