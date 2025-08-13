import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

const vuetify = createVuetify({
    components,
    directives,
    theme: {
        defaultTheme: 'light',
        themes: {
            light: {
                colors: {
                    primary: '#9d2c2a',
                    secondary: '#d4a664'
                }
            }
        }
    }
})

const app = createApp(App)
app.use(vuetify)
app.use(router)

router.onError((error) => {
    console.error('路由错误:', error)
})

app.config.errorHandler = (err) => {
    console.error('全局错误:', err)
}

app.mount('#app')