// stores/settings.js
import { ref, reactive, watch } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'

export const useSettingsStore = defineStore('settings', () => {
  // 默认设置
  const defaultSettings = {
    language: 'zh-CN',
    fontSize: 14,
    animations: true,
    compactMode: false,
    sidebarWidth: 280,
    notifications: {
      email: {
        assignmentSubmit: true,
        studentQuestion: true,
        systemUpdate: false
      },
      inApp: {
        assignmentGraded: true,
        courseUpdate: true,
        deadlineReminder: true
      },
      push: {
        urgentMessage: true,
        liveSession: true
      }
    },
    security: {
      autoLogout: '30',
      twoFactorAuth: false,
      activityLog: true
    }
  }

  // 颜色方案
  const colorSchemes = [
    { name: 'blue', label: '蓝色主题', primary: '#409EFF', secondary: '#79bbff' },
    { name: 'green', label: '绿色主题', primary: '#67C23A', secondary: '#95d475' },
    { name: 'purple', label: '紫色主题', primary: '#8E44AD', secondary: '#b57edc' },
    { name: 'orange', label: '橙色主题', primary: '#E67E22', secondary: '#f5b87d' }
  ]

  // 响应式状态
  const isDarkMode = ref(false)
  const currentColorScheme = ref('blue')
  const settings = reactive(JSON.parse(JSON.stringify(defaultSettings)))

  // 获取当前颜色
  const currentColor = () => {
    return colorSchemes.find(scheme => scheme.name === currentColorScheme.value) || colorSchemes[0]
  }

  // 应用主题
  const applyTheme = (dark) => {
    const html = document.documentElement
    if (dark) {
      html.classList.add('dark-theme')
      html.classList.remove('light-theme')
      isDarkMode.value = true
    } else {
      html.classList.add('light-theme')
      html.classList.remove('dark-theme')
      isDarkMode.value = false
    }
    updateGlobalStyles()
  }

  // 应用颜色方案
  const applyColorScheme = (schemeName) => {
    const scheme = colorSchemes.find(s => s.name === schemeName)
    if (scheme) {
      currentColorScheme.value = schemeName
      updateGlobalStyles()
    }
  }

  // 应用字体大小
  const applyFontSize = (size) => {
    document.documentElement.style.fontSize = `${size}px`
    settings.fontSize = size
  }

  // 应用紧凑模式
  const applyCompactMode = (compact) => {
    const html = document.documentElement
    if (compact) {
      html.classList.add('compact-mode')
    } else {
      html.classList.remove('compact-mode')
    }
    settings.compactMode = compact
  }

  // 更新全局样式
  const updateGlobalStyles = () => {
    const scheme = currentColor()
    const root = document.documentElement

    // 设置颜色变量
    root.style.setProperty('--primary-color', scheme.primary)
    root.style.setProperty('--primary-light', scheme.secondary)

    // 设置主题相关变量
    if (isDarkMode.value) {
      root.style.setProperty('--bg-color', '#1a1a1a')
      root.style.setProperty('--text-color', '#ffffff')
      root.style.setProperty('--text-secondary', '#a0a0a0')
      root.style.setProperty('--card-bg', '#2d2d2d')
      root.style.setProperty('--border-color', '#404040')
      root.style.setProperty('--hover-bg', '#3d3d3d')
    } else {
      root.style.setProperty('--bg-color', '#f5f7fa')
      root.style.setProperty('--text-color', '#303133')
      root.style.setProperty('--text-secondary', '#909399')
      root.style.setProperty('--card-bg', '#ffffff')
      root.style.setProperty('--border-color', '#e4e7ed')
      root.style.setProperty('--hover-bg', '#f5f7fa')
    }
  }

  // 初始化设置
  const initializeSettings = () => {
    try {
      const savedSettings = localStorage.getItem('teacherSettings')
      const savedTheme = localStorage.getItem('themeMode')
      const savedColorScheme = localStorage.getItem('colorScheme')

      if (savedSettings) {
        const parsedSettings = JSON.parse(savedSettings)
        // 安全地合并设置
        Object.keys(parsedSettings).forEach(key => {
          if (key === 'notifications' && parsedSettings.notifications) {
            Object.assign(settings.notifications, parsedSettings.notifications)
          } else if (key === 'security' && parsedSettings.security) {
            Object.assign(settings.security, parsedSettings.security)
          } else if (parsedSettings[key] !== undefined) {
            settings[key] = parsedSettings[key]
          }
        })

        applyFontSize(settings.fontSize)
        applyCompactMode(settings.compactMode)
      }

      if (savedTheme) {
        applyTheme(savedTheme === 'dark')
      } else {
        applyTheme(false)
      }

      if (savedColorScheme) {
        applyColorScheme(savedColorScheme)
      } else {
        applyColorScheme('blue')
      }
    } catch (error) {
      console.error('初始化设置失败:', error)
      // 重置为默认设置
      resetToDefaultSettings(false)
    }
  }

  // 保存设置
  const saveSettings = () => {
    localStorage.setItem('teacherSettings', JSON.stringify(settings))
    localStorage.setItem('themeMode', isDarkMode.value ? 'dark' : 'light')
    localStorage.setItem('colorScheme', currentColorScheme.value)
    ElMessage.success('设置已保存成功')
  }

  // 重置到默认设置
  const resetToDefaultSettings = (showMessage = true) => {
    // 创建深拷贝的默认设置
    const newSettings = JSON.parse(JSON.stringify(defaultSettings))

    // 更新响应式对象
    Object.keys(newSettings).forEach(key => {
      if (key === 'notifications') {
        Object.assign(settings.notifications, newSettings.notifications)
      } else if (key === 'security') {
        Object.assign(settings.security, newSettings.security)
      } else {
        settings[key] = newSettings[key]
      }
    })

    // 应用默认主题和颜色
    applyTheme(false)
    applyColorScheme('blue')
    applyFontSize(defaultSettings.fontSize)
    applyCompactMode(defaultSettings.compactMode)

    // 保存到localStorage
    localStorage.setItem('teacherSettings', JSON.stringify(defaultSettings))
    localStorage.setItem('themeMode', 'light')
    localStorage.setItem('colorScheme', 'blue')

    if (showMessage) {
      ElMessage.success('已恢复默认设置')
    }
  }

  // 监听设置变化
  watch(() => settings.fontSize, (newSize) => {
    applyFontSize(newSize)
  })

  watch(() => settings.compactMode, (newValue) => {
    applyCompactMode(newValue)
  })

  return {
    isDarkMode,
    colorSchemes,
    currentColorScheme,
    settings,
    currentColor,
    applyTheme,
    applyColorScheme,
    applyFontSize,
    applyCompactMode,
    initializeSettings,
    saveSettings,
    resetToDefaultSettings
  }
})
