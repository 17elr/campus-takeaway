<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ArrowRight, Location } from '@element-plus/icons-vue'
import { api, tell } from '../api'
const router = useRouter(), query = ref(''), page = ref(1), data = ref({ records: [], total: 0 }), loading = ref(true)
let controller
async function load(reset = false) {
  if (reset) page.value = 1
  controller?.abort(); controller = new AbortController(); const signal = controller.signal
  loading.value = true
  try { const result = await api(`/shops?page=${page.value}&query=${encodeURIComponent(query.value)}`, { signal }); if (!signal.aborted) data.value = result }
  catch (e) { if (e.name !== 'AbortError') tell(e.message, 'error') }
  finally { if (!signal.aborted) loading.value = false }
}
function filter(value) { query.value = value; load(true) }
onMounted(load); onUnmounted(() => controller?.abort())
</script>
<template>
  <section class="hero"><div class="hero-copy"><span class="eyebrow"><el-icon><Location /></el-icon> 校园食光 · 美味就在身边</span><h1>今天，也要好好吃饭。</h1><p>从熟悉的食堂，到喜欢的小店。<br>把热腾腾的好味道，送到你身边。</p><a class="hero-link" href="#shops">发现校园好店 <el-icon><ArrowRight /></el-icon></a></div><img src="/images/student-meal.webp" alt="热腾腾的校园套餐" class="hero-image" fetchpriority="high"><span class="hero-note">一日三餐 · 每一餐都认真</span></section>
  <div id="shops" class="section-heading"><div><span class="eyebrow">FIND YOUR NEXT FAVORITE</span><h2>校园好店 <small>{{ data.total }} 家正在营业</small></h2></div><form class="search" role="search" @submit.prevent="load(true)"><el-input v-model.trim="query" aria-label="搜索店铺或美食" placeholder="搜索店铺、食堂或美食" :prefix-icon="Search" clearable maxlength="80" /><el-button native-type="submit" :loading="loading">搜索</el-button></form></div>
  <div class="chips" aria-label="美食筛选"><el-button v-for="category in ['', '米饭', '面', '饮品']" :key="category" :type="query === category ? 'primary' : 'default'" round @click="filter(category)">{{ category || '全部店铺' }}</el-button></div>
  <el-skeleton v-if="loading" :rows="5" animated />
  <div v-else-if="data.records.length" class="shop-grid"><RouterLink v-for="shop in data.records" :key="shop.id" :to="`/shops/${shop.id}`" class="shop-card"><div class="shop-photo"><img :src="shop.imagePath" :alt="shop.name + '的餐食'" loading="lazy"><span class="open-pill">营业中</span></div><div class="shop-info"><h3>{{ shop.name }} <el-icon><ArrowRight /></el-icon></h3><p>{{ shop.description }}</p><div class="card-foot"><span>校内配送</span><span>查看菜单</span></div></div></RouterLink></div>
  <el-empty v-else description="没有找到店铺，换个关键词试试" />
  <el-pagination v-if="data.total > 12" v-model:current-page="page" :total="data.total" :page-size="12" layout="prev, pager, next" @current-change="load()" />
  <div class="service-strip"><div><b>校内好店</b><span>发现身边的日常美味</span></div><div><b>送到楼下</b><span>校内楼栋，取餐更方便</span></div><div><b>进度可查</b><span>从下单到送达，每一步清楚</span></div></div>
</template>
