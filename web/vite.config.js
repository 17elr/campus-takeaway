import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue({ template: { transformAssetUrls: { includeAbsolute: false } } })],
  server: { proxy: { '/api': 'http://127.0.0.1:8080', '/images': 'http://127.0.0.1:8080' } },
  build: { rollupOptions: { output: { manualChunks: { 'element-plus': ['element-plus'] } } } },
})
