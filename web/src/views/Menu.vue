<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ShoppingBag, Plus, Search } from '@element-plus/icons-vue'
import { api, session, tell, money } from '../api'
const route = useRoute(), router = useRouter(), menu = ref(), cart = ref({ items: [], total: 0 }), page = ref(1), query = ref(''), busy = ref(false), loading = ref(true)
let controller
async function load(reset = false) {
  if (reset) page.value = 1
  controller?.abort(); controller = new AbortController(); const signal = controller.signal
  loading.value = true
  try { const result = await api(`/shops/${route.params.id}/menu?page=${page.value}&query=${encodeURIComponent(query.value)}`, { signal }); if (!signal.aborted) menu.value = result }
  catch (e) { if (e.name !== 'AbortError') tell(e.message, 'error') }
  finally { if (!signal.aborted) loading.value = false }
}
async function add(dish) {
  if (!session.user) return router.push({ path: '/login', query: { next: route.fullPath } })
  if (session.user.role !== 'STUDENT') return tell('请使用学生账号选餐', 'warning')
  if (busy.value) return
  busy.value = true
  try { cart.value = await api(`/student/cart/items/${dish.id}`, { method: 'PUT', body: { quantity: (cart.value.items.find(item => item.dishId === dish.id)?.quantity || 0) + 1 } }); tell(`已加入 ${dish.name}`) }
  catch (e) { tell(e.message, 'error') }
  finally { busy.value = false }
}
onMounted(async () => { await load(); if (session.user?.role === 'STUDENT') { try { cart.value = await api('/student/cart') } catch (e) { tell(e.message, 'error') } } })
onUnmounted(() => controller?.abort())
</script>
<template>
  <el-button text @click="router.push(session.user?.role === 'STUDENT' ? '/student/shops' : '/')">← 返回店铺列表</el-button>
  <template v-if="menu"><section class="menu-banner"><div><span class="eyebrow">CAMPUS KITCHEN</span><h1>{{ menu.shop.name }}</h1><p>{{ menu.shop.description }}</p><el-tag type="success" effect="plain">营业中 · 校内配送</el-tag></div><img :src="menu.shop.imagePath" :alt="menu.shop.name"></section>
    <div class="menu-layout"><div><div class="section-heading"><h2>店铺菜单 <small>{{ menu.dishes.total }} 道好味</small></h2><form class="search" @submit.prevent="load(true)"><el-input v-model.trim="query" placeholder="搜索菜品或分类" aria-label="搜索菜品或分类" :prefix-icon="Search" clearable maxlength="80" /><el-button native-type="submit" :loading="loading">搜索</el-button></form></div><el-skeleton v-if="loading" :rows="4" animated /><div v-else class="dish-grid"><article v-for="dish in menu.dishes.records" :key="dish.id" class="dish-card"><img :src="dish.imagePath" :alt="dish.name" loading="lazy"><div><small class="muted">{{ dish.category }}</small><h3>{{ dish.name }}</h3><div class="dish-bottom"><b class="price">¥{{ money(dish.price) }}</b><el-button :icon="Plus" type="primary" plain :loading="busy" :aria-label="`加入购物车：${dish.name}`" @click="add(dish)">加入</el-button></div></div></article></div><el-empty v-if="!loading && !menu.dishes.records.length" description="没有找到菜品" /><el-pagination v-if="menu.dishes.total > 12" v-model:current-page="page" :total="menu.dishes.total" :page-size="12" layout="prev, pager, next" @current-change="load()" /></div>
    <aside class="panel cart-summary"><el-icon class="large-icon"><ShoppingBag /></el-icon><h2>我的购物车</h2><p class="muted">一单一家店，趁热享用。</p><div v-for="item in cart.items" :key="item.dishId" class="summary-row"><span>{{ item.name }} <small>×{{ item.quantity }}</small></span><b>¥{{ money(item.subtotal) }}</b></div><p v-if="!cart.items.length" class="empty-note">还没想好？先挑一道喜欢的。</p><div class="summary-total"><span>合计</span><b>¥{{ money(cart.total) }}</b></div><el-button type="primary" class="full" @click="router.push('/student/cart')">前往购物车</el-button><small class="form-note">结算前可以选择校内收货地址。</small></aside></div>
  </template><el-skeleton v-else-if="loading" :rows="8" animated /><el-empty v-else description="店铺暂时不可访问，请返回列表" />
</template>
