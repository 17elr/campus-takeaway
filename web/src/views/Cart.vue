<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api, tell, money } from '../api'
const router = useRouter(), cart = ref({ items: [], total: 0 }), addresses = ref([]), addressId = ref(), working = ref(false), loaded = ref(false)
async function load() {
  working.value = true
  try { const [basket, book] = await Promise.all([api('/student/cart'), api('/student/addresses')]); cart.value = basket; addresses.value = book.records; addressId.value = book.records.find(a => a.isDefault)?.id || book.records[0]?.id; loaded.value = true }
  catch (e) { tell(e.message, 'error') } finally { working.value = false }
}
async function change(item, quantity) {
  if (working.value || quantity === undefined) return
  working.value = true
  try { cart.value = await api(`/student/cart/items/${item.dishId}`, quantity ? { method: 'PUT', body: { quantity } } : { method: 'DELETE' }) }
  catch (e) { tell(e.message, 'error') } finally { working.value = false }
}
async function clear() {
  working.value = true
  try { cart.value = await api('/student/cart', { method: 'DELETE' }) } catch (e) { tell(e.message, 'error') } finally { working.value = false }
}
async function checkout() {
  if (working.value || !addressId.value) return
  working.value = true
  try { const order = await api('/student/orders', { method: 'POST', body: { checkoutKey: cart.value.checkoutKey, cartVersion: cart.value.version, addressId: addressId.value } }); tell('订单已创建，请在 15 分钟内完成模拟支付'); router.push(`/orders/${order.id}`) }
  catch (e) { tell(e.message, 'error') } finally { working.value = false }
}
onMounted(load)
</script>
<template>
  <div class="page-heading"><div><span class="eyebrow">YOUR CAMPUS TABLE</span><h1>购物车</h1><p>再看一眼，今天的美味都在这里。</p></div><el-button :loading="working" @click="load">刷新购物车</el-button></div>
  <el-skeleton v-if="!loaded && working" :rows="6" animated /><div v-else-if="cart.items.length" class="checkout-layout"><section class="panel"><div class="section-heading"><h2>已选餐品</h2><el-button text type="danger" :disabled="working" @click="clear">清空购物车</el-button></div><div v-for="item in cart.items" :key="item.dishId" class="cart-row"><img :src="item.imagePath" :alt="item.name"><div class="cart-item-copy"><h3>{{ item.name }}</h3><span class="price">¥{{ money(item.price) }}</span><small v-if="!item.onSale">该菜品已下架，请移除后重新结算</small></div><el-input-number :model-value="item.quantity" :min="1" :max="99" :disabled="working" :aria-label="`${item.name}数量`" @change="value => change(item, value)" /><b>¥{{ money(item.subtotal) }}</b><el-button text type="danger" :disabled="working" :aria-label="`移除${item.name}`" @click="change(item, 0)">移除</el-button></div></section>
    <section class="panel checkout-panel"><span class="eyebrow">ONE LAST STEP</span><h2>确认这份好味道</h2><el-form label-position="top"><el-form-item label="校内收货地址" :error="addresses.length ? '' : '请先添加一个校内地址'"><el-select v-model="addressId" placeholder="选择收货地址" :disabled="working" aria-label="校内收货地址"><el-option v-for="address in addresses" :key="address.id" :value="address.id" :label="`${address.building} ${address.room} · ${address.contactName}`" /></el-select></el-form-item></el-form><RouterLink to="/student/addresses">管理收货地址</RouterLink><div class="summary-total"><span>商品合计</span><b>¥{{ money(cart.total) }}</b></div><p class="form-note">成交价以提交时的菜品现价为准。创建订单后 15 分钟未支付将自动取消。</p><el-button type="primary" class="full" :loading="working" :disabled="!addressId" @click="checkout">提交订单</el-button><p class="form-note">本项目为模拟支付，不会扣除真实资金。</p></section></div>
  <el-empty v-else-if="loaded" description="购物车空空的，去发现一份好味道吧"><el-button type="primary" @click="router.push('/student/shops')">去选餐</el-button></el-empty>
</template>
