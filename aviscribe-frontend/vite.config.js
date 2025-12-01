import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // 配置代理，将所有 /api/* 请求转发到后端
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false, // 如果后端是http，设置为false
        // rewrite: (path) => path.replace(/^\/api/, '/api') // 后端context-path已经是/api，不需要rewrite
      }
    }
  }
})