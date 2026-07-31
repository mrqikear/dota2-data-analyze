import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  server: {
    host: '0.0.0.0',
    port: 5200,
    proxy: {
      '/api': {
        target: 'http://localhost:9601',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      },
      '/asset': {
        target: 'http://localhost:9601',
        changeOrigin: true
      }
    }
  }
})
