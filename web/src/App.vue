<script setup>
import { computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Shop, ShoppingBag, Tickets, Location, User, Setting, Bicycle, Bowl, Grid } from '@element-plus/icons-vue'
import { session, notice, logout, roles, tell } from './api'
const router = useRouter(), route = useRoute()
watch(() => session.user, (user, previous) => {
  if (!user && previous && (route.meta.auth || route.meta.role)) router.push('/login')
})
const nav = computed(() => ({
  STUDENT: [['/student/shops', '发现好店', Shop], ['/student/orders', '我的订单', Tickets], ['/student/cart', '购物车', ShoppingBag], ['/student/addresses', '收货地址', Location]],
  MERCHANT: [['/merchant/orders', '订单工作台', Tickets], ['/merchant/dishes', '菜品管理', Bowl], ['/merchant/shop', '店铺设置', Shop]],
  RIDER: [['/rider/available', '待配送大厅', Bicycle], ['/rider/orders', '我的配送', Tickets]],
  ADMIN: [['/admin/users', '用户管理', User], ['/admin/shops', '店铺管理', Shop], ['/admin/dishes', '菜品概览', Grid], ['/admin/orders', '订单概览', Tickets]],
}[session.user?.role] || [['/', '发现好店', Shop]]))
const current = computed(() => [...nav.value, ['/account', '账号设置']].find(item => item[0] === route.path)?.[1] || '校园好味道')
async function leave() { try { await logout(); router.push('/login') } catch (e) { tell(e.message, 'error') } }
</script>

<template>
  <a class="skip" href="#main">跳到主要内容</a>
  <aside class="sidebar">
    <RouterLink class="brand" to="/"><span class="brand-mark"><el-icon><Bowl /></el-icon></span><span>校园好味道<small>CAMPUS TAKEAWAY</small></span></RouterLink>
    <p class="nav-caption">{{ roles[session.user?.role] || '校园' }}服务</p>
    <nav aria-label="主要导航"><RouterLink v-for="[path, label, icon] in nav" :key="path" :to="path"><el-icon><component :is="icon" /></el-icon>{{ label }}</RouterLink></nav>
    <div class="sidebar-bottom"><RouterLink v-if="session.user" to="/account"><el-icon><Setting /></el-icon>账号设置</RouterLink><p>好好吃饭，好好生活。</p><small>仅校内配送 · 模拟支付</small></div>
  </aside>
  <div class="shell">
    <header class="topbar"><span class="breadcrumb">校园生活 <span>/</span> <b>{{ current }}</b></span><div class="account-nav"><template v-if="session.user"><span class="avatar">{{ session.user.realName.slice(0, 1) }}</span><RouterLink to="/account">{{ session.user.realName }} <small>{{ roles[session.user.role] }}</small></RouterLink><el-button text @click="leave">退出</el-button></template><template v-else><RouterLink to="/login">登录</RouterLink><el-button type="primary" @click="router.push('/register')">注册</el-button></template></div></header>
    <main id="main" tabindex="-1" class="content"><div class="announcement" role="status" aria-live="polite"><el-alert v-if="notice.message" :title="notice.message" :type="notice.kind" show-icon @close="notice.message = ''" /></div><RouterView :key="route.path" /></main>
    <footer>校园好味道 <span>让每一餐，都近一点。</span><a href="/images/credits.html" target="_blank" rel="noopener">图片来源</a></footer>
  </div>
</template>
