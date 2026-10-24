// Run against the isolated demo database and an Edge/Chrome CDP endpoint, never a production account.
import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'
const base = process.env.BASE_URL || 'http://127.0.0.1:8080'
const cdp = process.env.CDP_URL || 'http://127.0.0.1:19333'
const output = process.env.SCREENSHOT_DIR || 'docs/screenshots'
const targets = await (await fetch(`${cdp}/json/list`)).json()
const page = targets.find(target => target.type === 'page')
assert.ok(page, 'Start an isolated browser with remote debugging first')
const socket = new WebSocket(page.webSocketDebuggerUrl)
await new Promise((resolve, reject) => { socket.addEventListener('open', resolve, { once: true }); socket.addEventListener('error', reject, { once: true }) })
let sequence = 0
const pending = new Map(), errors = [], checks = []
socket.addEventListener('message', event => {
  const message = JSON.parse(event.data)
  if (message.id) { const task = pending.get(message.id); pending.delete(message.id); message.error ? task?.reject(new Error(JSON.stringify(message.error))) : task?.resolve(message.result) }
  if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text + ': ' + (message.params.exceptionDetails.exception?.description || ''))
})
function send(method, params = {}) {
  const id = ++sequence
  return new Promise((resolve, reject) => { pending.set(id, { resolve, reject }); socket.send(JSON.stringify({ id, method, params })) })
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
  if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails))
  return result.result?.value
}
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))
async function wait(expression, label = expression) {
  const end = Date.now() + 15000
  while (Date.now() < end) { try { if (await evaluate(expression)) return } catch {} await delay(100) }
  throw new Error(`Timed out: ${label}; ${await evaluate('document.body.innerText')}`)
}
async function navigate(path, expression = 'document.querySelector("main h1")') { await send('Page.navigate', { url: base + path }); await wait(`location.pathname === ${JSON.stringify(path)} && document.readyState === 'complete' && (${expression})`) }
async function clickText(text, container = 'document') {
  await wait(`Array.from(${container}.querySelectorAll('button')).some(button => button.textContent.trim() === ${JSON.stringify(text)} && !button.disabled)`)
  await evaluate(`Array.from(${container}.querySelectorAll('button')).find(button => button.textContent.trim() === ${JSON.stringify(text)} && !button.disabled).click()`)
}
async function input(selector, value) { await evaluate(`(() => { const input=document.querySelector(${JSON.stringify(selector)}); Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,${JSON.stringify(value)}); input.dispatchEvent(new Event('input',{bubbles:true})); })()`) }
async function login(username, password, destination) {
  if (await evaluate(`Array.from(document.querySelectorAll('button')).some(button=>button.textContent.trim()==='退出')`)) { await clickText('退出'); await wait(`location.pathname === '/login'`) }
  else await navigate('/login')
  await input('input[name=username]', username); await input('input[name=password]', password); await clickText('登录')
  await wait(`location.pathname === ${JSON.stringify(destination)}`)
}
async function layout(label, width = 1440) {
  await send('Emulation.setDeviceMetricsOverride', { width, height: width < 600 ? 900 : 1000, deviceScaleFactor: 1, mobile: false })
  await delay(180)
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth + 1'), true, `${label}: horizontal overflow`)
  await wait(`Array.from(document.images).every(image => image.complete && image.naturalWidth > 0)`, `${label}: images`)
  const shot = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
  await writeFile(`${output}/${label}.png`, Buffer.from(shot.data, 'base64'))
  checks.push(`${label}: ${width}px, images and layout passed`)
}

try {
  await mkdir(output, { recursive: true }); await send('Page.enable'); await send('Runtime.enable')
  await navigate('/'); await wait('document.querySelectorAll(".shop-card").length > 0'); await layout('shops-desktop'); await layout('shops-mobile', 375); await layout('shops-tablet', 768)
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false })
  await navigate('/register'); await clickText('注册学生账号'); await wait('document.querySelectorAll(".el-form-item__error").length > 0'); checks.push('registration inline validation passed')
  await login('student', 'student123', '/student/shops'); await wait('document.querySelector(".shop-card")'); await evaluate('document.querySelector(".shop-card").click()'); await wait('document.querySelector(".dish-card")'); await layout('student-menu')
  await evaluate('document.querySelector(".dish-card button").click()'); await wait(`document.querySelector('.summary-row')`); await clickText('前往购物车'); await wait('document.querySelector(".cart-row")'); await layout('student-cart'); await clickText('提交订单'); await wait(`location.pathname.startsWith('/orders/') && Number(location.pathname.split('/').pop()) > 0 && document.querySelector('.status-banner')`)
  const orderPath = await evaluate('location.pathname'), orderId = orderPath.split('/').pop()
  await clickText('模拟支付'); await wait(`document.querySelector('.status-banner .eyebrow')?.textContent.trim() === '待接单'`)
  await login('merchant', 'merchant123', '/merchant/orders'); await wait('document.querySelector(".order-card")'); await layout('merchant-orders'); await navigate(orderPath); await clickText('接单制作'); await wait(`document.querySelector('.status-banner .eyebrow')?.textContent.trim() === '制作中'`); await clickText('制作完成'); await wait(`document.querySelector('.status-banner .eyebrow')?.textContent.trim() === '待配送'`)
  await login('rider', 'rider123', '/rider/available'); await wait('document.querySelector(".order-card")'); await layout('rider-available'); await layout('rider-mobile', 375)
  await clickText('领取配送', `document.querySelector('a[href="${orderPath}"]').closest('.order-card')`); await navigate('/rider/orders'); await wait(`document.querySelector('a[href="${orderPath}"]')`); await clickText('标记送达', `document.querySelector('a[href="${orderPath}"]').closest('.order-card')`); await wait(`document.querySelector('.order-card .el-tag')?.textContent.trim() === '待确认'`)
  await login('student', 'student123', '/student/shops'); await navigate(orderPath); await clickText('确认收货'); await wait(`document.querySelector('.status-banner .eyebrow')?.textContent.trim() === '已完成'`); await layout('order-completed', 1440); await clickText('再来一单'); await wait(`location.pathname === '/student/cart' && document.querySelector('.cart-row')`)
  await navigate('/student/addresses'); await wait('document.querySelector(".address-card")'); await layout('student-addresses'); await navigate('/account'); await layout('account')
  await login('admin', 'admin123', '/admin/users'); await wait('document.querySelectorAll(".el-table__row").length > 0'); await layout('admin-users'); await layout('admin-mobile', 375)
  for (const tab of ['shops', 'dishes', 'orders']) { await navigate(`/admin/${tab}`); await wait(tab === 'orders' ? 'document.querySelector(".order-card")' : 'document.querySelectorAll(".el-table__row").length > 0') }
  await login('merchant', 'merchant123', '/merchant/orders'); await navigate('/merchant/dishes'); await wait('document.querySelectorAll(".el-table__row").length > 0'); await layout('merchant-dishes', 1440); await navigate('/merchant/shop'); await layout('merchant-shop')
  assert.deepEqual(errors, [], 'browser runtime exceptions')
  const result = { date: new Date().toISOString(), browser: (await send('Browser.getVersion')).product, orderId, checks, flow: 'student -> simulated pay -> merchant accept/ready -> rider claim/deliver -> student confirm/reorder', runtimeErrors: errors }
  await writeFile(`${output}/browser-results.json`, JSON.stringify(result, null, 2) + '\n'); console.log(JSON.stringify(result, null, 2))
} finally { socket.close() }
