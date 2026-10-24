<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { api, session, tell, money, labels } from '../api'
import { createPoller } from '../polling'
const route = useRoute(), page = ref(1), status = ref(''), query = ref(''), data = ref({ records: [], total: 0 }), loading = ref(true), working = ref(false)
const role = session.user.role, available = computed(() => route.path === '/rider/available')
const poller = createPoller(async signal => {
  loading.value = true
  try { const result = await api(`/orders?page=${page.value}&status=${status.value}&scope=${available.value ? 'available' : 'mine'}&query=${encodeURIComponent(query.value)}`, { signal }); if (!signal.aborted) data.value = result }
  finally { if (!signal.aborted) loading.value = false }
}, { periodic: ['MERCHANT', 'RIDER'].includes(role), onError: e => { loading.value = false; tell(e.message, 'error') } })
function filter() { page.value = 1; poller.refresh() }
function visibility() { if (document.hidden) poller.pause(); else poller.start() }
async function action(order, action) {
  if (working.value) return
  working.value = true
  try { await api(`/orders/${order.id}/${action}`, { method: 'POST' }); tell(action === 'claim' ? '接单成功，请前往店铺取餐' : '订单已更新') }
  catch (e) { tell(e.status === 409 && action === 'claim' ? '订单已被领取或状态已变化，列表已刷新' : e.message, 'error') }
  finally { working.value = false; poller.refresh() }
}
onMounted(() => { poller.start(); document.addEventListener('visibilitychange', visibility) })
onUnmounted(() => { poller.pause(); document.removeEventListener('visibilitychange', visibility) })
</script>
<template>
  <div class="page-heading"><div><span class="eyebrow">{{ role === 'RIDER' ? 'ON THE WAY, WITH CARE' : 'EVERY ORDER, EVERY MOMENT' }}</span><h1>{{ available ? '待配送大厅' : role === 'MERCHANT' ? '订单工作台' : role === 'ADMIN' ? '订单概览' : role === 'RIDER' ? '我的配送' : '我的订单' }}</h1><p>{{ available ? '接一份热乎，送一份安心。' : '每一份订单的进度，都清清楚楚。' }} <small v-if="['MERCHANT', 'RIDER'].includes(role)">页面可见时每 8 秒刷新</small></p></div><el-button :loading="loading" @click="poller.refresh">刷新列表</el-button></div>
  <div class="panel filter-bar"><el-select v-model="status" aria-label="筛选订单状态" placeholder="全部状态" clearable @change="filter"><el-option label="全部状态" value="" /><el-option v-for="(label, value) in labels" :key="value" :label="label" :value="value" /></el-select><form class="search" @submit.prevent="filter"><el-input v-model.trim="query" aria-label="搜索订单号或店铺" placeholder="搜索订单号或店铺" :prefix-icon="Search" clearable maxlength="80" /><el-button native-type="submit">搜索</el-button></form><small class="muted">共 {{ data.total }} 笔订单</small></div>
  <el-skeleton v-if="loading && !data.records.length" :rows="6" animated /><div class="order-list"><article v-for="order in data.records" :key="order.id" class="panel order-card"><div class="order-top"><RouterLink :to="`/orders/${order.id}`"><h2>{{ order.shopName }}</h2></RouterLink><el-tag :type="order.status === 'COMPLETED' ? 'success' : order.status === 'CANCELED' ? 'info' : 'warning'" effect="plain">{{ labels[order.status] }}</el-tag></div><div class="order-lines"><span v-for="item in order.items" :key="item.id">{{ item.dishName }} <small>×{{ item.quantity }}</small></span></div><div class="order-meta"><span>{{ order.orderNo }}</span><span>{{ order.createdAt?.replace('T', ' ') }}</span></div><div class="order-bottom"><b class="price">¥{{ money(order.amount) }}</b><div class="actions"><el-button @click="$router.push(`/orders/${order.id}`)">订单详情</el-button><el-button v-if="role === 'MERCHANT' && order.status === 'PENDING_ACCEPT'" type="primary" :loading="working" @click="action(order, 'accept')">接单制作</el-button><el-button v-if="role === 'MERCHANT' && order.status === 'MAKING'" type="primary" :loading="working" @click="action(order, 'ready')">制作完成</el-button><el-button v-if="role === 'RIDER' && available && order.status === 'READY'" type="primary" :loading="working" @click="action(order, 'claim')">领取配送</el-button><el-button v-if="role === 'RIDER' && order.status === 'DELIVERING' && order.riderId === session.user?.id" type="primary" :loading="working" @click="action(order, 'delivered')">标记送达</el-button></div></div></article></div>
  <el-empty v-if="!loading && !data.records.length" :description="available ? '暂时没有待配送订单，稍后再来看看' : '暂时没有符合条件的订单'" /><el-pagination v-if="data.total > 12" v-model:current-page="page" :total="data.total" :page-size="12" layout="prev, pager, next" @current-change="poller.refresh" />
</template>
