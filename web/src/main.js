import { createApp, h } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Home from './views/Home.vue'
import Auth from './views/Auth.vue'
import Menu from './views/Menu.vue'
import Cart from './views/Cart.vue'
import Orders from './views/Orders.vue'
import OrderDetail from './views/OrderDetail.vue'
import Addresses from './views/Addresses.vue'
import Account from './views/Account.vue'
import Manage from './views/Manage.vue'
import { initialize, session, landing } from './api'
import './style.css'

const routes = [
  { path: '/', component: Home }, { path: '/login', component: Auth }, { path: '/register', component: Auth },
  { path: '/student', redirect: '/student/shops' }, { path: '/student/shops', component: Home, meta: { role: 'STUDENT' } },
  { path: '/shops/:id', component: Menu }, { path: '/student/cart', component: Cart, meta: { role: 'STUDENT' } },
  { path: '/student/orders', component: Orders, meta: { role: 'STUDENT' } }, { path: '/student/addresses', component: Addresses, meta: { role: 'STUDENT' } },
  { path: '/orders/:id', component: OrderDetail, meta: { auth: true } }, { path: '/account', component: Account, meta: { auth: true } },
  ...['merchant', 'rider', 'admin'].flatMap(role => [
    { path: `/${role}`, redirect: landing(role.toUpperCase()) },
    { path: `/${role}/orders`, component: Orders, meta: { role: role.toUpperCase() } },
  ]),
  { path: '/rider/available', component: Orders, meta: { role: 'RIDER' } },
  ...['dishes', 'shop'].map(tab => ({ path: `/merchant/${tab}`, component: Manage, meta: { role: 'MERCHANT' } })),
  ...['users', 'shops', 'dishes'].map(tab => ({ path: `/admin/${tab}`, component: Manage, meta: { role: 'ADMIN' } })),
  { path: '/:pathMatch(.*)*', component: { render: () => h('section', { class: 'panel empty' }, [h('h1', '页面不存在'), h('a', { href: '/' }, '返回首页')]) } },
]
const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })
const initialized = initialize()
router.beforeEach(async to => {
  await initialized
  if ((to.meta.auth || to.meta.role) && !session.user) return { path: '/login', query: { next: to.fullPath } }
  if (to.meta.role && to.meta.role !== session.user?.role) return landing(session.user?.role)
})
createApp(App).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
