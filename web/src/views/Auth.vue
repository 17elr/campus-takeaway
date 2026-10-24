<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, session, tell, landing } from '../api'
const route = useRoute(), router = useRouter(), register = computed(() => route.path === '/register')
const form = ref(), busy = ref(false), error = ref(''), fields = ref({})
const model = ref({ username: '', password: '', realName: '', phone: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { pattern: /^[A-Za-z0-9_]{3,40}$/, message: '3–40 位字母、数字或下划线', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 64, message: '密码为 6–64 位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }, { max: 40, message: '姓名最多 40 字', trigger: 'blur' }],
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入有效的 11 位手机号', trigger: 'blur' }],
}
async function submit() {
  if (busy.value || !await form.value.validate().catch(() => false)) return
  busy.value = true; error.value = ''; fields.value = {}
  try {
    if (register.value) {
      await api('/auth/register', { method: 'POST', body: model.value }); tell('注册成功，欢迎加入校园好味道'); router.push('/login')
    } else {
      const result = await api('/auth/login', { method: 'POST', body: { username: model.value.username, password: model.value.password } })
      session.user = result.user
      const next = typeof route.query.next === 'string' && route.query.next.startsWith('/') && !route.query.next.startsWith('//') ? route.query.next : landing(result.user.role)
      router.push(next)
    }
  } catch (e) { error.value = e.message; fields.value = e.fields || {} }
  finally { busy.value = false }
}
</script>
<template>
  <section class="auth-layout"><div class="auth-story"><img :src="register ? '/images/register-food.webp' : '/images/login-food.webp'" alt="校园里的日常美食"><div><span class="eyebrow">GOOD FOOD, GOOD CAMPUS LIFE</span><h1>把生活的忙碌，<br>留给一顿好饭。</h1><p>你熟悉的校园味道，一直在这里。</p></div></div><div class="auth-form"><span class="eyebrow">{{ register ? 'JOIN OUR CAMPUS TABLE' : 'WELCOME BACK' }}</span><h1>{{ register ? '开启你的校园食光' : '欢迎回来' }}</h1><p class="muted">{{ register ? '创建学生账号，发现校园里的好味道。' : '登录账号，继续今天的校园生活。' }}</p><el-alert v-if="error" :title="error" type="error" :closable="false" role="alert" show-icon />
    <el-form ref="form" :model="model" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名" prop="username" :error="fields.username"><el-input v-model.trim="model.username" name="username" autocomplete="username" placeholder="请输入用户名" maxlength="40" /></el-form-item>
      <el-form-item label="密码" prop="password" :error="fields.password"><el-input v-model="model.password" name="password" type="password" show-password :autocomplete="register ? 'new-password' : 'current-password'" placeholder="请输入密码" maxlength="64" /></el-form-item>
      <template v-if="register"><el-form-item label="姓名" prop="realName" :error="fields.realName"><el-input v-model.trim="model.realName" name="realName" autocomplete="name" maxlength="40" /></el-form-item><el-form-item label="手机号" prop="phone" :error="fields.phone"><el-input v-model.trim="model.phone" name="phone" autocomplete="tel" inputmode="tel" maxlength="11" /></el-form-item></template>
      <el-button class="full" type="primary" native-type="submit" :loading="busy">{{ register ? '注册学生账号' : '登录' }}</el-button>
    </el-form><p class="auth-switch">{{ register ? '已有账号？' : '还没有账号？' }} <RouterLink :to="register ? '/login' : '/register'">{{ register ? '去登录' : '注册学生账号' }}</RouterLink></p><p class="form-note">学生、商家、骑手与管理员使用同一登录入口。<br>商家及骑手账号由管理员创建。</p></div></section>
</template>
