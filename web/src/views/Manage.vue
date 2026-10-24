<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { api, session, tell, roles, money } from '../api'
const route = useRoute(), role = session.user.role.toLowerCase(), tab = route.path.split('/').pop(), data = ref({ records: [], total: 0 }), page = ref(1), query = ref(''), loading = ref(true), busy = ref(false), visible = ref(false), form = ref(), error = ref(''), editing = ref()
const model = ref({}), shop = ref({ name: '', description: '', open: true })
const title = computed(() => ({ users: '用户管理', shops: '店铺管理', dishes: role === 'merchant' ? '菜品管理' : '菜品概览', shop: '店铺设置' }[tab]))
const rules = {
  username: [{ required: true, pattern: /^[A-Za-z0-9_]{3,40}$/, message: '3–40 位字母、数字或下划线' }], password: [{ required: true, min: 6, max: 64, message: '密码为 6–64 位' }],
  realName: [{ required: true, message: '请输入姓名' }], phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入有效的 11 位手机号' }], role: [{ required: true, message: '请选择角色' }],
  name: [{ required: true, message: '请输入名称' }, { max: 80, message: '最多 80 字' }], description: [{ required: true, message: '请输入店铺简介' }, { max: 255, message: '最多 255 字' }], category: [{ required: true, message: '请输入分类' }, { max: 40, message: '最多 40 字' }], price: [{ required: true, type: 'number', min: 0.01, max: 9999.99, message: '价格须在 0.01–9999.99 元之间' }],
}
let controller
async function load(reset = false) {
  if (reset) page.value = 1
  controller?.abort(); controller = new AbortController(); const signal = controller.signal
  loading.value = true
  try { const result = await api(`/${role}/${tab}?page=${page.value}&query=${encodeURIComponent(query.value)}`, { signal }); if (!signal.aborted) { if (tab === 'shop') shop.value = { name: result.name, description: result.description, open: result.open }; else data.value = result } }
  catch (e) { if (e.name !== 'AbortError') tell(e.message, 'error') } finally { if (!signal.aborted) loading.value = false }
}
function open(row) { editing.value = row?.id; error.value = ''; model.value = tab === 'users' ? { username: '', password: '', realName: '', phone: '', role: 'MERCHANT' } : { name: row?.name || '', category: row?.category || '', price: Number(row?.price || 0.01) }; visible.value = true }
async function save() {
  if (busy.value || !await form.value.validate().catch(() => false)) return
  busy.value = true; error.value = ''
  try { await api(`/${role}/${tab}${editing.value ? '/' + editing.value : ''}`, { method: tab === 'shop' || editing.value ? 'PUT' : 'POST', body: tab === 'shop' ? shop.value : model.value }); visible.value = false; tell('已保存'); await load() }
  catch (e) { error.value = e.message } finally { busy.value = false }
}
async function toggle(row) {
  if (busy.value) return
  busy.value = true
  try {
    const path = tab === 'users' ? `/admin/users/${row.id}/enabled` : tab === 'shops' ? `/admin/shops/${row.id}/open` : `/merchant/dishes/${row.id}/toggle`
    await api(path, { method: tab === 'dishes' ? 'POST' : 'PUT', ...(tab === 'users' ? { body: { enabled: !row.enabled } } : tab === 'shops' ? { body: { open: !row.open } } : {}) }); tell('状态已更新'); await load()
  } catch (e) { tell(e.message, 'error') } finally { busy.value = false }
}
onMounted(load); onUnmounted(() => controller?.abort())
</script>
<template>
  <div class="page-heading"><div><span class="eyebrow">{{ role === 'merchant' ? 'YOUR KITCHEN, YOUR WAY' : 'CAMPUS OPERATIONS' }}</span><h1>{{ title }}</h1><p>{{ tab === 'dishes' ? '认真对待每道菜，也认真对待每份期待。' : '让校园服务，井井有条。' }}</p></div><el-button v-if="tab === 'users' || (tab === 'dishes' && role === 'merchant')" type="primary" @click="open()">{{ tab === 'users' ? '创建商家 / 骑手' : '添加菜品' }}</el-button></div>
  <section v-if="tab === 'shop'" class="panel narrow-panel"><h2>我的店铺</h2><el-alert v-if="error" :title="error" type="error" :closable="false" role="alert" /><el-form ref="form" :model="shop" :rules="rules" label-position="top" @submit.prevent="save"><el-form-item label="店铺名称" prop="name"><el-input v-model.trim="shop.name" maxlength="80" /></el-form-item><el-form-item label="店铺简介" prop="description"><el-input v-model.trim="shop.description" type="textarea" :rows="4" maxlength="255" show-word-limit /></el-form-item><el-form-item label="营业状态"><el-switch v-model="shop.open" active-text="营业中" inactive-text="休息中" aria-label="店铺营业状态" /></el-form-item><el-button native-type="submit" type="primary" :loading="busy || loading">保存店铺设置</el-button></el-form></section>
  <section v-else class="panel"><div class="section-heading"><h2>{{ title }} <small>{{ data.total }} 条</small></h2><form class="search" @submit.prevent="load(true)"><el-input v-model.trim="query" :prefix-icon="Search" placeholder="输入关键词搜索" aria-label="搜索管理列表" maxlength="80" clearable /><el-button native-type="submit" :loading="loading">搜索</el-button></form></div><div class="table-wrap"><el-table v-loading="loading" :data="data.records" empty-text="没有符合条件的数据" stripe>
    <template v-if="tab === 'users'"><el-table-column prop="username" label="用户名" min-width="140" /><el-table-column prop="realName" label="姓名" min-width="120" /><el-table-column label="角色" width="100"><template #default="{ row }">{{ roles[row.role] }}</template></el-table-column><el-table-column prop="phone" label="手机号" min-width="140" /><el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '已启用' : '已停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="110"><template #default="{ row }"><el-button v-if="row.role !== 'ADMIN'" :disabled="busy" text :type="row.enabled ? 'danger' : 'primary'" @click="toggle(row)">{{ row.enabled ? '停用' : '启用' }}</el-button><span v-else class="muted">受保护</span></template></el-table-column></template>
    <template v-else-if="tab === 'shops'"><el-table-column prop="name" label="店铺名称" min-width="160" /><el-table-column prop="description" label="简介" min-width="220" /><el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.open ? 'success' : 'info'">{{ row.open ? '营业中' : '休息中' }}</el-tag></template></el-table-column><el-table-column label="操作" width="110"><template #default="{ row }"><el-button text :disabled="busy" @click="toggle(row)">{{ row.open ? '暂停营业' : '开启营业' }}</el-button></template></el-table-column></template>
    <template v-else><el-table-column label="菜品" min-width="200"><template #default="{ row }"><div class="table-dish"><img :src="row.imagePath" :alt="row.name" loading="lazy"><b>{{ row.name }}</b></div></template></el-table-column><el-table-column v-if="role === 'admin'" prop="shopName" label="所属店铺" min-width="140" /><el-table-column prop="category" label="分类" width="100" /><el-table-column label="价格" width="110"><template #default="{ row }">¥{{ money(row.price) }}</template></el-table-column><el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.onSale ? 'success' : 'info'">{{ row.onSale ? '已上架' : '已下架' }}</el-tag></template></el-table-column><el-table-column v-if="role === 'merchant'" label="操作" width="160"><template #default="{ row }"><el-button text :disabled="busy" @click="open(row)">编辑</el-button><el-button text :disabled="busy" @click="toggle(row)">{{ row.onSale ? '下架' : '上架' }}</el-button></template></el-table-column></template>
  </el-table></div><el-pagination v-if="data.total > 12" v-model:current-page="page" :total="data.total" :page-size="12" layout="prev, pager, next" @current-change="load()" /></section>
  <el-dialog v-model="visible" :title="tab === 'users' ? '创建业务账号' : editing ? '编辑菜品' : '添加菜品'" width="480px" :close-on-click-modal="false" destroy-on-close><el-alert v-if="error" :title="error" type="error" :closable="false" role="alert" /><el-form ref="form" :model="model" :rules="rules" label-position="top" @submit.prevent="save"><template v-if="tab === 'users'"><el-form-item label="用户名" prop="username"><el-input v-model.trim="model.username" maxlength="40" autocomplete="off" /></el-form-item><el-form-item label="初始密码" prop="password"><el-input v-model="model.password" type="password" show-password autocomplete="new-password" maxlength="64" /></el-form-item><el-form-item label="姓名" prop="realName"><el-input v-model.trim="model.realName" maxlength="40" /></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model.trim="model.phone" inputmode="tel" maxlength="11" /></el-form-item><el-form-item label="角色" prop="role"><el-select v-model="model.role" aria-label="账号角色"><el-option label="商家" value="MERCHANT" /><el-option label="骑手" value="RIDER" /></el-select></el-form-item></template><template v-else><el-form-item label="菜品名称" prop="name"><el-input v-model.trim="model.name" maxlength="80" /></el-form-item><el-form-item label="分类" prop="category"><el-input v-model.trim="model.category" maxlength="40" placeholder="例如：米饭、面食、饮品" /></el-form-item><el-form-item label="价格（元）" prop="price"><el-input-number v-model="model.price" :precision="2" :step="0.5" :min="0.01" :max="9999.99" aria-label="菜品价格" /></el-form-item></template><el-button type="primary" native-type="submit" :loading="busy">保存</el-button></el-form></el-dialog>
</template>
