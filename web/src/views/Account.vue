<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, session, tell, roles } from '../api'
const router = useRouter(), profileForm = ref(), passwordForm = ref(), busy = ref(false), profileError = ref(''), passwordError = ref('')
const profile = ref({ realName: session.user.realName, phone: session.user.phone }), password = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })
const profileRules = { realName: [{ required: true, message: '请输入姓名' }, { max: 40, message: '姓名最多 40 字' }], phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入有效的 11 位手机号' }] }
const passwordRules = { currentPassword: [{ required: true, message: '请输入当前密码' }], newPassword: [{ required: true, min: 6, max: 64, message: '新密码为 6–64 位' }], confirmPassword: [{ required: true, message: '请再次输入新密码' }, { validator: (_, value, callback) => value === password.value.newPassword ? callback() : callback(new Error('两次新密码输入不一致')) }] }
async function save(changePassword = false) {
  if (busy.value || !await (changePassword ? passwordForm : profileForm).value.validate().catch(() => false)) return
  busy.value = true; profileError.value = ''; passwordError.value = ''
  try {
    if (changePassword) { await api('/account/password', { method: 'POST', body: password.value }); session.user = null; tell('密码已修改，请使用新密码登录'); router.push('/login') }
    else { await api('/account', { method: 'PUT', body: profile.value }); session.user = await api('/auth/me'); tell('个人资料已保存') }
  } catch (e) { if (changePassword) passwordError.value = e.message; else profileError.value = e.message }
  finally { busy.value = false }
}
</script>
<template>
  <div class="page-heading"><div><span class="eyebrow">YOUR CAMPUS IDENTITY</span><h1>账号设置</h1><p>管理个人资料，让每一次联系都准确。</p></div><el-tag>{{ roles[session.user?.role] }}</el-tag></div><div class="account-grid"><section class="panel"><h2>个人资料</h2><el-alert v-if="profileError" :title="profileError" type="error" :closable="false" role="alert" /><el-form ref="profileForm" :model="profile" :rules="profileRules" label-position="top" @submit.prevent="save()"><el-form-item label="用户名"><el-input :model-value="session.user?.username" disabled /></el-form-item><el-form-item label="姓名" prop="realName"><el-input v-model.trim="profile.realName" maxlength="40" autocomplete="name" /></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model.trim="profile.phone" maxlength="11" inputmode="tel" autocomplete="tel" /></el-form-item><el-button type="primary" native-type="submit" :loading="busy">保存资料</el-button></el-form></section><section class="panel"><h2>修改密码</h2><p class="form-note">修改后，所有设备上的旧登录将在下一次请求时失效。</p><el-alert v-if="passwordError" :title="passwordError" type="error" :closable="false" role="alert" /><el-form ref="passwordForm" :model="password" :rules="passwordRules" label-position="top" @submit.prevent="save(true)"><el-form-item label="当前密码" prop="currentPassword"><el-input v-model="password.currentPassword" type="password" show-password autocomplete="current-password" /></el-form-item><el-form-item label="新密码" prop="newPassword"><el-input v-model="password.newPassword" type="password" show-password autocomplete="new-password" maxlength="64" /></el-form-item><el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="password.confirmPassword" type="password" show-password autocomplete="new-password" maxlength="64" /></el-form-item><el-button type="primary" native-type="submit" :loading="busy">更新密码并重新登录</el-button></el-form></section></div>
</template>
