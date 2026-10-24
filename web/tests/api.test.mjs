import { test } from 'node:test'
import assert from 'node:assert/strict'

test('writes use same-origin cookies and rotate CSRF after login', async () => {
  const { api } = await import('../src/api.js?rotation')
  const original = globalThis.fetch, calls = []
  globalThis.fetch = async (path, options) => {
    calls.push({ path, options })
    return Response.json(path === '/api/auth/csrf' ? { token: 'anonymous' } : path === '/api/auth/login' ? { token: 'authenticated' } : { message: 'ok' })
  }
  try {
    await api('/auth/login', { method: 'POST', body: { username: 'student', password: 'test123' } })
    await api('/auth/logout', { method: 'POST' })
    assert.equal(calls[1].options.headers['X-CSRF-Token'], 'anonymous')
    assert.equal(calls[2].options.headers['X-CSRF-Token'], 'authenticated')
    assert.equal(calls[2].options.credentials, 'same-origin')
    assert.equal(calls[1].options.headers['Content-Type'], 'application/json')
  } finally { globalThis.fetch = original }
})

test('401 clears identity; conflicts and outages retain their error status', async () => {
  const { api, session } = await import('../src/api.js?errors')
  const original = globalThis.fetch
  try {
    for (const status of [409, 503, 401]) {
      session.user = { id: 1 }
      globalThis.fetch = async () => Response.json({ message: '请重试' }, { status })
      await assert.rejects(api('/orders'), error => error.status === status && error.message === '请重试')
      assert.equal(session.user === null, status === 401)
    }
  } finally { globalThis.fetch = original }
})
