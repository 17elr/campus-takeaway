<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { api, session, tell, money, labels } from '../api'
const route = useRoute(), router = useRouter(), detail = ref(), working = ref(false), clock = ref(Date.now())
let timer, controller = new AbortController()
const order = computed(() => detail.value?.order)
const countdown = computed(() => { const seconds = Math.max(0, Math.floor((new Date(order.value?.expiresAt).getTime() - clock.value) / 1000)); return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}` })
const role = session.user.role
async function load() { try { detail.value = await api(`/orders/${route.params.id}`, { signal: controller.signal }) } catch (e) { if (e.name !== 'AbortError') tell(e.message, 'error') } }
async function action(action) {
  if (working.value) return
  if (action === 'cancel' && !await ElMessageBox.confirm('确定取消这份订单吗？', '取消订单', { confirmButtonText: '确定取消', cancelButtonText: '继续等待' }).catch(() => false)) return
  working.value = true
  try {
    if (action === 'reorder') { await api(`/student/orders/${order.value.id}/reorder`, { method: 'POST' }); tell('已按当前菜品重新选餐，请核对现价'); router.push('/student/cart') }
    else { await api(`/orders/${order.value.id}/${action}`, { method: 'POST' }); tell(action === 'pay' ? '模拟支付成功，未扣除真实资金' : '订单已更新'); await load() }
  } catch (e) { tell(e.message, 'error'); await load() }
  finally { working.value = false }
}
onMounted(() => { load(); timer = setInterval(() => { clock.value = Date.now() }, 1000) })
onUnmounted(() => { clearInterval(timer); controller.abort() })
</script>
<template>
  <el-button text @click="router.back()">← 返回</el-button><template v-if="order"><div class="page-heading"><div><span class="eyebrow">ORDER DETAILS</span><h1>订单详情</h1><p>{{ order.orderNo }}</p></div><el-button @click="load">刷新进度</el-button></div>
    <section class="status-banner"><div><span class="eyebrow">{{ labels[order.status] }}</span><h2>{{ { UNPAID: '美味已备好，等你确认。', PENDING_ACCEPT: '店铺正在等待接单。', MAKING: '厨房里，烟火正忙。', READY: '餐已做好，等待骑手。', DELIVERING: '热乎的好味道，正在路上。', WAIT_CONFIRM: '餐已送达，记得趁热享用。', COMPLETED: '这一餐，也值得好好回味。', CANCELED: '订单已取消，下次再见。' }[order.status] }}</h2><p v-if="order.status === 'UNPAID' && order.expiresAt">剩余模拟支付时间 {{ countdown }}；超时由服务器自动取消。</p><p v-else>如有进度变化，点击刷新即可查看。</p></div><b class="status-price">¥{{ money(order.amount) }}</b></section>
    <div class="detail-layout"><section class="panel"><h2>{{ order.shopName }}</h2><div v-for="item in order.items" :key="item.id" class="detail-item"><div><b>{{ item.dishName }}</b><small>¥{{ money(item.unitPrice) }} × {{ item.quantity }}</small></div><b>¥{{ money(item.subtotal) }}</b></div><div class="summary-total"><span>订单金额</span><b>¥{{ money(order.amount) }}</b></div><h3>收货信息</h3><p>{{ order.addressSnapshot }}</p><p class="form-note">收货信息与商品价格保存为下单时快照。</p><div class="actions"><template v-if="role === 'STUDENT'"><el-button v-if="order.status === 'UNPAID'" type="primary" :loading="working" @click="action('pay')">模拟支付</el-button><el-button v-if="['UNPAID', 'PENDING_ACCEPT'].includes(order.status)" :loading="working" @click="action('cancel')">取消订单</el-button><el-button v-if="order.status === 'WAIT_CONFIRM'" type="primary" :loading="working" @click="action('confirm')">确认收货</el-button><el-button v-if="['COMPLETED', 'CANCELED'].includes(order.status)" type="primary" :loading="working" @click="action('reorder')">再来一单</el-button></template><template v-if="role === 'MERCHANT'"><el-button v-if="order.status === 'PENDING_ACCEPT'" type="primary" :loading="working" @click="action('accept')">接单制作</el-button><el-button v-if="order.status === 'MAKING'" type="primary" :loading="working" @click="action('ready')">制作完成</el-button></template><template v-if="role === 'RIDER'"><el-button v-if="order.status === 'READY' && !order.riderId" type="primary" :loading="working" @click="action('claim')">领取配送</el-button><el-button v-if="order.status === 'DELIVERING' && order.riderId === session.user?.id" type="primary" :loading="working" @click="action('delivered')">标记送达</el-button></template></div><el-alert class="payment-note" title="本订单仅支持模拟支付，不会产生真实扣款或退款。" type="info" :closable="false" /></section><aside class="panel"><h2>订单时间线</h2><el-timeline><el-timeline-item v-for="(event, index) in detail.events" :key="index" :timestamp="event.createdAt?.replace('T', ' ')" placement="top"><b>{{ labels[event.toStatus] }}</b><p>{{ event.reason === 'PAYMENT_TIMEOUT' ? '15 分钟未支付，系统自动取消' : event.operatorName }}</p></el-timeline-item></el-timeline></aside></div>
  </template><el-empty v-else description="正在读取订单；若无权限，请返回订单列表" />
</template>
