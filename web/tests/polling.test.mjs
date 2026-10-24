import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createPoller } from '../src/polling.js'

test('polling does not overlap and aborts when paused', async () => {
  let calls = 0, signal
  const poller = createPoller(s => { calls++; signal = s; return new Promise(resolve => s.addEventListener('abort', resolve, { once: true })) }, { interval: 5, visible: () => true })
  poller.start()
  await new Promise(resolve => setTimeout(resolve, 30))
  assert.equal(calls, 1)
  poller.pause()
  assert.equal(signal.aborted, true)
})
test('hidden pages do not load; manual refresh cancels stale work', async () => {
  let visible = false, calls = 0, oldSignal
  const poller = createPoller(s => { calls++; oldSignal ||= s; return new Promise(resolve => s.addEventListener('abort', resolve, { once: true })) }, { periodic: false, visible: () => visible })
  poller.start(); assert.equal(calls, 0)
  visible = true; poller.refresh(); assert.equal(calls, 1)
  poller.refresh(); assert.equal(oldSignal.aborted, true); assert.equal(calls, 2)
  poller.pause()
})
