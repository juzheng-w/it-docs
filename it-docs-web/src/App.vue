<template>
  <!-- 登录页独立布局，无导航 -->
  <router-view v-if="isLogin" />

  <!-- 查阅端：深色顶栏 + 内容 -->
  <div v-else-if="!isManagement" class="portal-shell">
    <header class="portal-topbar">
      <div class="portal-topbar-inner">
        <div class="portal-brand" @click="go('/')">IT部知识共享服务平台</div>
        <form class="portal-search" @submit.prevent="submitSearch">
          <el-icon><Search /></el-icon>
          <input v-model="searchKeyword" type="search" placeholder="搜索报错原文 / 单据号 / 关键词" />
        </form>
        <span class="spacer"></span>
        <a v-if="user.logged" class="portal-link" @click="go('/console')">工作台</a>
        <a v-if="user.logged" class="portal-link" @click="go('/personal')">个人区域</a>
        <el-dropdown v-if="user.logged" trigger="click">
          <span class="portal-user">
            {{ user.displayName || user.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="go('/profile')"><el-icon><UserFilled /></el-icon>个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button v-else class="portal-login" type="button" @click="go('/login')">登录</button>
        <ThemeMenu class="portal-theme" />
      </div>
    </header>
    <router-view />
  </div>

  <!-- 管理端：左侧 200px 导航 + 内容区 -->
  <div v-else class="app-shell management-shell">
    <aside class="admin-sidebar" :class="{ open: navOpen }">
      <div class="admin-brand" @click="go('/console')">IT知识平台</div>
      <nav class="admin-menu" @click="navOpen = false">
        <a class="admin-link" :class="{ on: adminTab === 'home' }" @click="go('/console')">工作台首页</a>
        <a class="admin-link" :class="{ on: adminTab === 'qa' }" @click="openConsole('qa')">全部问题记录</a>
        <a class="admin-link" :class="{ on: adminTab === 'req' }" @click="openConsole('req')">全部业务文档</a>
        <a class="admin-link" :class="{ on: adminTab === 'tech' }" @click="openConsole('tech')">全部技术文档</a>
        <a class="admin-link" :class="{ on: adminTab === 'personal' }" @click="go('/personal')">个人区域</a>
        <div class="admin-menu-band"></div>
        <template v-if="user.isAdmin">
          <a class="admin-link" :class="{ on: adminTab === 'spaces' }" @click="go('/console/spaces')">空间与权限</a>
          <a class="admin-link" :class="{ on: adminTab === 'users' }" @click="go('/console/users')">成员角色</a>
          <a class="admin-link" :class="{ on: adminTab === 'meta' }" @click="go('/console/meta')">基础数据</a>
          <a class="admin-link" :class="{ on: adminTab === 'settings' }" @click="go('/settings')">系统设置</a>
        </template>
      </nav>
    </aside>
    <div v-if="navOpen" class="admin-backdrop" @click="navOpen = false"></div>

    <div class="admin-main">
      <header class="admin-header">
        <button class="admin-nav-toggle" type="button" aria-label="打开菜单" @click="navOpen = true">
          <el-icon><Menu /></el-icon>
        </button>
        <div class="admin-crumb">
          <a class="crumb-home" @click="go('/console')">首页</a>
          <span v-if="crumbTail" class="crumb-rest">/ {{ crumbTail }}</span>
        </div>
        <span class="spacer"></span>
        <ThemeMenu />
        <el-dropdown v-if="user.logged" trigger="click">
          <span class="admin-user">
            <span class="admin-avatar">{{ userInitial }}</span>
            <span class="admin-user-name">{{ user.username }}</span>
            <span class="admin-role">· {{ roleLabel(user.role) }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="go('/profile')"><el-icon><UserFilled /></el-icon>个人中心</el-dropdown-item>
              <el-dropdown-item @click="go('/')"><el-icon><Monitor /></el-icon>回到查阅端</el-dropdown-item>
              <el-dropdown-item divided @click="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button v-else size="small" @click="go('/login')">登录</el-button>
      </header>
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, UserFilled, SwitchButton, Menu, Search, Monitor } from '@element-plus/icons-vue'
import ThemeMenu from './components/ThemeMenu.vue'
import { roleLabel } from './constants/labels'
import { useUserStore } from './stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const isLogin = computed(() => route.path === '/login')
const isManagement = computed(() => ['/console', '/personal', '/profile', '/settings'].some(path => route.path.startsWith(path)))

const searchKeyword = ref(route.query.keyword || '')
watch(() => route.query.keyword, v => { searchKeyword.value = v || '' })

const navOpen = ref(false)

const userInitial = computed(() => (user.displayName || user.username || '?').slice(0, 1))

// 左侧菜单高亮：文档页按 docType，其余按页面
const adminTab = computed(() => {
  const p = route.path
  if (p.startsWith('/console/spaces')) return 'spaces'
  if (p.startsWith('/console/users')) return 'users'
  if (p.startsWith('/console/meta')) return 'meta'
  if (p.startsWith('/settings')) return 'settings'
  if (p.startsWith('/personal')) return 'personal'
  if (p.startsWith('/profile')) return ''
  if (p.replace(/\/$/, '') === '/console') {
    return ['qa', 'req', 'tech'].includes(route.query.docType) ? route.query.docType : 'home'
  }
  return 'home'
})

// 面包屑：与左侧菜单同一套叫法；「首页」单独拆出来做可点入口
const crumb = computed(() => {
  const p = route.path
  if (p.startsWith('/console/spaces')) return '首页 / 空间与权限'
  if (p.startsWith('/console/users')) return '首页 / 成员角色'
  if (p.startsWith('/console/meta')) return '首页 / 基础数据'
  if (p.startsWith('/settings')) return '首页 / 系统设置'
  if (p.startsWith('/personal')) return '首页 / 个人区域'
  if (p.startsWith('/profile')) return '首页 / 个人中心'
  if (p.startsWith('/console/edit')) return '首页 / 工作台 / 编辑内容'
  const type = route.query.docType
  const label = { qa: '问题记录', req: '业务文档', tech: '技术文档' }[type]
  if (route.query.system) return `首页 / 工作台 / ${route.query.system} / ${label || ''}`
  return label ? `首页 / 工作台 / 全部${label}` : '首页 / 工作台'
})
const crumbTail = computed(() => crumb.value.replace(/^首页\s*\/\s*/, ''))

function go(path) {
  navOpen.value = false
  // 比 fullPath 而不是 path：从 /console?docType=qa 回 /console 时路径相同，只比 path 会点不动
  if (router.resolve(path).fullPath !== route.fullPath) router.push(path)
}

function openConsole(type) {
  navOpen.value = false
  router.push({ path: '/console', query: { docType: type } })
}

function submitSearch() {
  const keyword = searchKeyword.value.trim()
  router.push({ path: '/', query: keyword ? { keyword } : {} })
}

function logout() {
  user.logout()
  router.push('/')
  location.reload()
}
</script>
