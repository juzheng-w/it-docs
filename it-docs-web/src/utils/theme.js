/**
 * 界面主题：外观模式（浅色 / 深色 / 跟随系统）+ 主色预设。
 *
 * 实现要点：
 * - 所有颜色都落在 CSS 变量上，样式文件只引用变量，不写死色值；
 * - html.dark 触发 Element Plus 官方暗色变量表（theme-chalk/dark/css-vars.css）；
 * - 主色梯度（--el-color-primary-light-N / dark-2）按当前明暗动态生成，
 *   浅色模式混白、深色模式混黑，与 Element Plus 官方规则一致；
 * - 偏好存 localStorage，跟随系统时监听 prefers-color-scheme 实时切换。
 */
import { ref } from 'vue'

const MODE_KEY = 'kb_theme_mode'
const ACCENT_KEY = 'kb_theme_accent'

export const THEME_MODES = [
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
  { value: 'auto', label: '跟随系统' }
]

export const THEME_ACCENTS = [
  { value: 'blue', label: '科技蓝', color: '#409eff' },
  { value: 'indigo', label: '靛紫', color: '#6d5efc' },
  { value: 'teal', label: '青绿', color: '#0f9d8f' },
  { value: 'orange', label: '暖橙', color: '#e8730c' }
]

/** 当前是否暗色（供 md-editor-v3 等第三方组件绑定 theme 用） */
export const isDark = ref(false)

let mql = null
let mqlHandler = null

export function getTheme() {
  let mode = localStorage.getItem(MODE_KEY)
  if (!THEME_MODES.some(m => m.value === mode)) mode = 'light'
  let accent = localStorage.getItem(ACCENT_KEY)
  if (!THEME_ACCENTS.some(a => a.value === accent)) accent = 'blue'
  return { mode, accent }
}

function systemDark() {
  return typeof window.matchMedia === 'function'
    && window.matchMedia('(prefers-color-scheme: dark)').matches
}

function resolveDark(mode) {
  if (mode === 'dark') return true
  if (mode === 'auto') return systemDark()
  return false
}

function rgb(hex) {
  const h = hex.replace('#', '')
  return [parseInt(h.substring(0, 2), 16), parseInt(h.substring(2, 4), 16), parseInt(h.substring(4, 6), 16)]
}

/** 把 base 按 ratio 向 target（白或黑）混合，返回 #rrggbb */
function mix(base, target, ratio) {
  const a = rgb(base), b = rgb(target)
  const r = Math.min(1, Math.max(0, ratio))
  const parts = a.map((v, i) => Math.round(v + (b[i] - v) * r).toString(16).padStart(2, '0'))
  return '#' + parts.join('')
}

/** 解绑上一次的系统主题监听 */
function unbindSystem() {
  if (mql && mqlHandler) {
    if (mql.removeEventListener) mql.removeEventListener('change', mqlHandler)
    else if (mql.removeListener) mql.removeListener(mqlHandler)
  }
  mql = null
  mqlHandler = null
}

/** 应用主题，返回 { mode, accent, dark } */
export function applyTheme() {
  const { mode, accent } = getTheme()
  const root = document.documentElement
  const dark = resolveDark(mode)
  root.classList.toggle('dark', dark)
  root.style.colorScheme = dark ? 'dark' : 'light'
  isDark.value = dark

  const base = (THEME_ACCENTS.find(a => a.value === accent) || THEME_ACCENTS[0]).color
  // Element Plus 主色梯度：浅色 light-N 混白，深色 light-N 混黑、dark-2 混白
  const lightTarget = dark ? '#000000' : '#ffffff'
  const lightRatio = dark ? 0.65 : 1
  for (const n of [3, 5, 7, 8, 9]) {
    root.style.setProperty('--el-color-primary-light-' + n, mix(base, lightTarget, (n / 10) * lightRatio))
  }
  root.style.setProperty('--el-color-primary', base)
  root.style.setProperty('--el-color-primary-dark-2', mix(base, dark ? '#ffffff' : '#000000', 0.2))

  // 自定义组件变量（styles.css 与各页面 scoped 样式引用）
  root.style.setProperty('--kb-primary', base)
  root.style.setProperty('--kb-primary-2', mix(base, '#ffffff', dark ? 0.1 : 0.3))
  root.style.setProperty('--kb-primary-soft', dark ? mix(base, '#000000', 0.72) : mix(base, '#ffffff', 0.91))
  root.style.setProperty('--kb-primary-tint', dark ? mix(base, '#000000', 0.84) : mix(base, '#ffffff', 0.96))

  // 跟随系统：只在 auto 模式挂监听，切换出去立即解绑
  unbindSystem()
  if (mode === 'auto' && typeof window.matchMedia === 'function') {
    mql = window.matchMedia('(prefers-color-scheme: dark)')
    mqlHandler = () => applyTheme()
    if (mql.addEventListener) mql.addEventListener('change', mqlHandler)
    else if (mql.addListener) mql.addListener(mqlHandler)
  }
  return { mode, accent, dark }
}

export function setThemeMode(mode) {
  localStorage.setItem(MODE_KEY, mode)
  return applyTheme()
}

export function setThemeAccent(accent) {
  localStorage.setItem(ACCENT_KEY, accent)
  return applyTheme()
}

/** 入口最早调用一次；index.html 的内联脚本已提前设置 html.dark 防闪白 */
export function initTheme() {
  return applyTheme()
}
