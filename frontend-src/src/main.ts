import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { initializeAuth } from './lib/auth'

const app = createApp(App)

app.use(router)

initializeAuth().finally(() => {
  app.mount('#app')
})
