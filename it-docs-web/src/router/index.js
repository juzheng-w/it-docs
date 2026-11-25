import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/portal/Home.vue') },
  { path: '/login', name: 'login', component: () => import('../views/auth/Login.vue') },
  { path: '/doc/:id', name: 'doc', component: () => import('../views/portal/DocRead.vue') },
  // 工作台（需登录；权限由后端二次校验）
  { path: '/console', name: 'console', component: () => import('../views/console/Console.vue') },
  { path: '/console/users', name: 'users', component: () => import('../views/console/Users.vue') },
  { path: '/console/meta', name: 'meta', component: () => import('../views/console/Meta.vue') },
  { path: '/console/spaces', name: 'spaces', component: () => import('../views/console/Spaces.vue') },
  { path: '/console/edit/:id?', name: 'edit', component: () => import('../views/console/DocEdit.vue') },
  { path: '/settings', name: 'settings', component: () => import('../views/console/Settings.vue') },
  { path: '/profile', name: 'profile', component: () => import('../views/console/Profile.vue') },
  { path: '/personal', name: 'personal', component: () => import('../views/console/Personal.vue') },
  { path: '/personal/edit/:id?', name: 'personal-edit', component: () => import('../views/console/DocEdit.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes })

// 需要登录的路径守卫（体验层；安全由后端 JWT 校验兜底）
router.beforeEach(to => {
  const needLogin = ['/console', '/personal', '/settings', '/profile'].some(p => to.path.startsWith(p))
  const logged = !!localStorage.getItem('kb_token')
  if (needLogin && !logged) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 系统设置 / 成员管理 / 基础数据仅管理员（体验层；后端 ADMIN 二次校验兜底）
  if (['/settings', '/console/users', '/console/meta', '/console/spaces'].includes(to.path) && localStorage.getItem('kb_role') !== 'ADMIN') {
    return { path: '/console' }
  }
})

export default router
