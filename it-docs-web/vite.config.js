import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    // 监听所有网卡：同事用 http://<本机IP>:5173 也能访问（默认只监听 127.0.0.1）
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      // 开发期代理到本地后端
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  },
  build: {
    chunkSizeWarningLimit: 1500
  }
})
