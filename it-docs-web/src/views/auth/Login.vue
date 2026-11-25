<template>
  <main class="login-page">
    <!-- 左：品牌介绍，窄屏隐藏 -->
    <aside class="login-brand">
      <div class="brand-grid" aria-hidden="true"></div>
      <div class="brand-glow" aria-hidden="true"></div>
      <div class="brand-inner">
        <div class="brand-logo">
          <span class="brand-mark">IT</span>
          <span class="brand-logo-text">内部知识平台</span>
        </div>

        <h1 class="brand-title">IT知识共享服务平台</h1>
        <p class="brand-desc">
          报错原文、原因与处理步骤，按业务系统沉淀；<br />
          业务文档与技术文档登录后统一查看。
        </p>

        <ul class="brand-points">
          <li>
            <span class="pt-icon"><el-icon><Search /></el-icon></span>
            <div><b>问题记录</b><em>搜报错、搜单据号，直接照着处理步骤做</em></div>
          </li>
          <li>
            <span class="pt-icon"><el-icon><Collection /></el-icon></span>
            <div><b>业务文档</b><em>业务流程、操作手册与培训手册</em></div>
          </li>
          <li>
            <span class="pt-icon"><el-icon><Reading /></el-icon></span>
            <div><b>技术文档</b><em>部署、配置与排查记录</em></div>
          </li>
        </ul>

        <p class="brand-foot">未登录也可以浏览公开的问题记录</p>
      </div>
    </aside>

    <!-- 右：登录表单 -->
    <section class="login-main">
      <div class="login-top"><ThemeMenu /></div>

      <div class="login-box">
        <div class="login-mobile-brand">
          <span class="brand-mark">IT</span>
          <span>IT知识共享服务平台</span>
        </div>

        <h2 class="login-title">登录</h2>
        <p class="login-subtitle">登录后可维护内容、查看业务与技术文档</p>

        <div class="login-tabs" role="tablist">
          <button v-for="t in CHANNELS" :key="t.value" type="button" role="tab"
                  class="login-tab" :class="{ on: channel === t.value }"
                  :aria-selected="channel === t.value"
                  @click="channel = t.value">{{ t.label }}</button>
        </div>

        <el-form class="login-form" @submit.prevent="doLogin">
          <el-input v-model="username" class="login-input" size="large"
                    :placeholder="activeChannel.placeholder" autocomplete="username" autofocus>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
          <el-input v-model="password" class="login-input" size="large" type="password" show-password
                    :placeholder="activeChannel.passwordPlaceholder" autocomplete="current-password"
                    @keyup.enter="doLogin">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
          <el-button native-type="submit" type="primary" size="large" class="login-submit" :loading="loading">
            登录<el-icon v-if="!loading" class="login-submit-arrow"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <p class="login-hint">
          <el-icon class="login-hint-icon"><InfoFilled /></el-icon>
          <span>{{ activeChannel.hint }}</span>
        </p>

        <div class="login-split"></div>
        <button class="guest-link" type="button" @click="router.push('/')">先浏览公开知识 →</button>
      </div>

      <p class="login-foot">IT 部 · 内部系统，账号问题请联系管理员</p>
    </section>
  </main>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Collection, InfoFilled, Lock, Reading, Search, User } from '@element-plus/icons-vue'
import ThemeMenu from '../../components/ThemeMenu.vue'
import api from '../../api'
import { useUserStore } from '../../stores/user'

const CHANNELS = [
  {
    value: 'ldap',
    label: '域账号',
    placeholder: '域账号，如 wang.jz',
    passwordPlaceholder: '域密码',
    hint: '首次登录自动开通平台账号，默认只读，管理员可提权'
  },
  {
    value: 'local',
    label: '本地账号',
    placeholder: '本地账号，如 admin',
    passwordPlaceholder: '密码',
    hint: '本地账号由管理员创建，忘记密码请联系管理员重置'
  }
]

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const channel = ref('ldap')
const username = ref('')
const password = ref('')
const loading = ref(false)

// 后端按账号来源自动识别域账号 / 本地账号，Tab 只影响提示文案
const activeChannel = computed(() => CHANNELS.find(c => c.value === channel.value) || CHANNELS[0])

async function doLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await api.post('/auth/login', { username: username.value.trim(), password: password.value })
    user.setLogin(res.data)
    await user.fetchMe(api)
    if (res.data.mustChangePwd) {
      ElMessage.warning('首次登录请先修改初始密码')
      router.replace('/profile')
    } else {
      router.push(route.query.redirect || '/console')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  min-height: 100vh;
  background: var(--kb-bg);
}

/* ── 左侧品牌区 ── */
.login-brand {
  position: relative; overflow: hidden;
  display: flex; align-items: center;
  padding: 64px 72px;
  background: linear-gradient(158deg, #34465c 0%, #2b3a4d 46%, #1f2b3a 100%);
  color: #fff;
}
.brand-grid {
  position: absolute; inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, .16) 1px, transparent 1px);
  background-size: 24px 24px;
  mask-image: radial-gradient(120% 90% at 18% 22%, #000 0%, transparent 78%);
  -webkit-mask-image: radial-gradient(120% 90% at 18% 22%, #000 0%, transparent 78%);
  opacity: .5;
}
.brand-glow {
  position: absolute; width: 520px; height: 520px; right: -180px; bottom: -220px;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--kb-primary) 46%, transparent) 0%, transparent 68%);
  filter: blur(10px);
}
.brand-inner { position: relative; max-width: 520px; }

.brand-logo { display: flex; align-items: center; gap: 10px; margin-bottom: 40px; }
.brand-mark {
  display: inline-grid; place-items: center;
  width: 34px; height: 34px; border-radius: 8px;
  background: var(--kb-primary); color: #fff;
  font-size: 13px; font-weight: 700; letter-spacing: .04em;
}
.brand-logo-text { color: rgba(255, 255, 255, .72); font-size: 13px; letter-spacing: .08em; }

.brand-title { margin: 0; font-size: 32px; font-weight: 600; letter-spacing: -.01em; line-height: 1.3; }
.brand-desc {
  margin: 16px 0 0;
  color: rgba(255, 255, 255, .66); font-size: 14px; line-height: 1.9;
}

.brand-points { display: grid; gap: 18px; margin: 40px 0 0; padding: 0; list-style: none; }
.brand-points li { display: flex; align-items: flex-start; gap: 12px; }
.pt-icon {
  flex: 0 0 auto; display: inline-grid; place-items: center;
  width: 30px; height: 30px; margin-top: 1px; border-radius: 8px;
  background: rgba(255, 255, 255, .09); color: rgba(255, 255, 255, .86); font-size: 15px;
}
.brand-points b { display: block; font-size: 14px; font-weight: 600; color: rgba(255, 255, 255, .94); }
.brand-points em { display: block; margin-top: 3px; font-style: normal; font-size: 12.5px; color: rgba(255, 255, 255, .55); }
.brand-foot {
  margin: 48px 0 0; padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, .1);
  color: rgba(255, 255, 255, .5); font-size: 12px;
}

/* ── 右侧表单区 ── */
.login-main {
  position: relative;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 48px 32px;
}
.login-top { position: absolute; top: 24px; right: 24px; }
.login-box { width: min(100%, 372px); }

.login-mobile-brand { display: none; align-items: center; gap: 10px; margin-bottom: 28px; font-size: 15px; font-weight: 600; color: var(--kb-text); }

.login-title { margin: 0; font-size: 26px; font-weight: 600; color: var(--kb-text); letter-spacing: -.01em; }
.login-subtitle { margin: 8px 0 0; font-size: 13px; color: var(--kb-text-muted); line-height: 1.6; }

.login-tabs {
  display: grid; grid-template-columns: 1fr 1fr;
  margin: 30px 0 22px;
  border-bottom: 1px solid var(--kb-border);
}
.login-tab {
  position: relative; padding: 0 0 11px;
  border: 0; background: none; color: var(--kb-text-muted);
  font: inherit; font-size: 14px; cursor: pointer;
  transition: color .15s;
}
.login-tab:hover { color: var(--kb-primary); }
.login-tab.on { color: var(--kb-primary); font-weight: 600; }
.login-tab.on::after {
  content: ''; position: absolute; left: 0; right: 0; bottom: -1px;
  height: 2px; border-radius: 1px; background: var(--kb-primary);
}

.login-form { display: grid; gap: 14px; }
.login-form :deep(.el-input__wrapper) { min-height: 46px; padding: 0 14px; border-radius: 4px; }
.login-form :deep(.el-input__inner) { font-size: 14px; }
.login-form :deep(.el-input__prefix) { color: var(--kb-text-faint); margin-right: 4px; }

.login-submit {
  width: 100%; min-height: 46px; margin-top: 6px;
  font-size: 15px; letter-spacing: .04em;
}
.login-submit-arrow { margin-left: 6px; }

.login-hint {
  display: flex; align-items: flex-start; gap: 6px;
  margin: 14px 0 0;
  color: var(--kb-text-muted); font-size: 12px; line-height: 1.7;
}
.login-hint-icon { flex: 0 0 auto; margin-top: 3px; font-size: 13px; color: var(--kb-text-faint); }

.login-split { margin: 20px 0 0; height: 1px; background: var(--kb-border); }
.guest-link {
  display: block; width: 100%; margin-top: 12px; padding: 0;
  border: 0; background: none;
  color: var(--kb-primary); font: inherit; font-size: 13px; text-align: center; cursor: pointer;
}
.guest-link:hover { text-decoration: underline; }

.login-foot {
  position: absolute; bottom: 24px; left: 0; right: 0;
  margin: 0; text-align: center;
  color: var(--kb-text-faint); font-size: 11.5px;
}

/* ── 窄屏：收掉品牌区 ── */
@media (max-width: 1000px) {
  .login-page { grid-template-columns: minmax(0, 1fr); }
  .login-brand { display: none; }
  .login-mobile-brand { display: flex; }
  .login-main { padding: 72px 24px 64px; }
}
@media (max-width: 480px) {
  .login-main { padding: 64px 20px 56px; }
  .login-title { font-size: 23px; }
  .login-top { top: 16px; right: 16px; }
  .login-foot { bottom: 16px; }
}
</style>
