<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { api, session, tell } from '../api'
const data = ref({ records: [], buildings: [] }), visible = ref(false), form = ref(), working = ref(false), error = ref(''), editing = ref()
const model = ref({ building: '', room: '', contactName: '', phone: '' })
const rules = { building: [{ required: true, message: '请选择校内楼栋' }], room: [{ required: true, message: '请输入房间或收货点' }, { max: 40, message: '最多 40 字' }], contactName: [{ required: true, message: '请输入联系人' }, { max: 40, message: '最多 40 字' }], phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入有效的 11 位手机号' }] }
async function load() { try { data.value = await api('/student/addresses') } catch (e) { tell(e.message, 'error') } }
function open(address) { editing.value = address?.id; model.value = address ? { building: address.building, room: address.room, contactName: address.contactName, phone: address.phone } : { building: '', room: '', contactName: session.user.realName, phone: session.user.phone }; error.value = ''; visible.value = true }
async function save() {
  if (working.value || !await form.value.validate().catch(() => false)) return
  working.value = true
  try { await api(`/student/addresses${editing.value ? '/' + editing.value : ''}`, { method: editing.value ? 'PUT' : 'POST', body: model.value }); visible.value = false; tell('收货地址已保存'); await load() }
  catch (e) { error.value = e.message } finally { working.value = false }
}
async function action(address, remove = false) {
  if (working.value) return
  if (remove && !await ElMessageBox.confirm('确定删除这个收货地址吗？历史订单地址快照将保留。', '删除地址', { confirmButtonText: '删除', cancelButtonText: '保留' }).catch(() => false)) return
  working.value = true
  try { await api(`/student/addresses/${address.id}${remove ? '' : '/default'}`, { method: remove ? 'DELETE' : 'POST' }); await load(); tell('地址已更新') }
  catch (e) { tell(e.message, 'error') } finally { working.value = false }
}
onMounted(load)
</script>
<template>
  <div class="page-heading"><div><span class="eyebrow">A LITTLE CLOSER TO YOU</span><h1>收货地址</h1><p>校园里，总有一处等着热乎的饭。</p></div><el-button type="primary" @click="open()">添加地址</el-button></div><div class="address-grid"><article v-for="address in data.records" :key="address.id" class="panel address-card"><div class="section-heading"><h2>{{ address.building }}</h2><el-tag v-if="address.isDefault" effect="plain">默认地址</el-tag></div><p>{{ address.room }}</p><p class="muted">{{ address.contactName }} · {{ address.phone }}</p><div class="actions"><el-button @click="open(address)">编辑</el-button><el-button v-if="!address.isDefault" :disabled="working" @click="action(address)">设为默认</el-button><el-button text type="danger" :disabled="working" @click="action(address, true)">删除</el-button></div></article></div><el-empty v-if="!data.records.length" description="还没有校内收货地址，添加一个吧" />
  <el-dialog v-model="visible" :title="editing ? '编辑收货地址' : '添加收货地址'" width="480px" :close-on-click-modal="false" destroy-on-close><el-alert v-if="error" :title="error" type="error" :closable="false" role="alert" /><el-form ref="form" :model="model" :rules="rules" label-position="top" @submit.prevent="save"><el-form-item label="校内楼栋" prop="building"><el-select v-model="model.building" aria-label="校内楼栋"><el-option v-for="building in data.buildings" :key="building" :label="building" :value="building" /></el-select></el-form-item><el-form-item label="房间或收货点" prop="room"><el-input v-model.trim="model.room" maxlength="40" placeholder="例如：302 室 / 楼下取餐点" /></el-form-item><el-form-item label="联系人" prop="contactName"><el-input v-model.trim="model.contactName" maxlength="40" /></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model.trim="model.phone" inputmode="tel" maxlength="11" /></el-form-item><el-button type="primary" native-type="submit" :loading="working">保存地址</el-button></el-form></el-dialog>
</template>
