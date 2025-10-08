import type { Router } from 'vue-router'
import { useUserStore } from '@/store/user'
import { i18n } from '@/locales'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 配置 NProgress
NProgress.configure({ showSpinner: false, speed: 500 })

// 不需要登录的白名单
const whiteList = ['/login', '/register', '/forget']

export function setupRouterGuards(router: Router) {
  // 前置守卫 - 页面跳转前执行
  router.beforeEach((to, from, next) => {
    // 开始进度条
    NProgress.start()

    // 设置页面标题
    const baseTitle = import.meta.env.VITE_APP_TITLE || 'Connect Ai-智能物联网管理平台'
    if (to.meta.title) {
      // 如果是翻译键，则进行翻译
      const title = to.meta.title.startsWith('router.') 
        ? i18n.global.t(to.meta.title)
        : to.meta.title
      document.title = `${title} - ${baseTitle}`
    } else {
      document.title = baseTitle
    }

    const userStore = useUserStore()
    const hasToken = !!userStore.userInfo
    const isAdmin = String(userStore.userInfo?.isAdmin) === '1'

    // 1. 未登录处理
    if (!hasToken) {
      if (whiteList.includes(to.path)) {
        // 在白名单中，直接访问
        next()
      } else {
        // 不在白名单中，跳转到登录页
        next(`/login?redirect=${to.path}`)
        NProgress.done()
      }
      return
    }

    // 2. 已登录处理
    if (to.path === '/login') {
      // 如果已登录，访问登录页则跳转到首页
      next({ path: '/dashboard' })
      NProgress.done()
      return
    }

    // 3. 权限检查
    if (to.meta.requiresAuth) {
      // 检查是否需要管理员权限
      if (to.meta.isAdmin && !isAdmin) {
        console.warn(`用户无权限访问: ${to.path}`)
        next('/403')
        NProgress.done()
        return
      }
    }

    // 4. 放行
    next()
  })

  // 后置守卫 - 页面跳转后执行
  router.afterEach(() => {
    // 结束进度条
    NProgress.done()
  })

  // 错误处理
  router.onError((error) => {
    console.error('路由错误:', error)
    NProgress.done()
  })
}

// 使用示例：
// 在 main.ts 中：
// import { setupRouterGuards } from './router/guards'
// setupRouterGuards(router)
