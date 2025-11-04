import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: {
    proxy: {
      '/api': { target: 'http://39.96.172.21:9999', changeOrigin: true },
      '/media': { target: 'http://39.96.172.21:9999', changeOrigin: true },
      '/uploads': { target: 'http://39.96.172.21:9999', changeOrigin: true },
    },
  },
})


