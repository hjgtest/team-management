import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)

app.use(router)
app.use(ElementPlus)

app.mount('#app')

// 静默处理 ResizeObserver 循环限制错误
window.addEventListener('error', (e) => {
  if (e.message === 'ResizeObserver loop limit exceeded' || 
      e.message === 'ResizeObserver loop completed with undelivered notifications.') {
    e.stopImmediatePropagation()
  }
})
