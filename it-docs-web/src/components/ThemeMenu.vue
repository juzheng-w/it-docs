<template>
  <el-popover placement="bottom-end" :width="238" trigger="click" popper-class="theme-popper">
    <template #reference>
      <span class="icon-chip" title="外观设置">
        <el-icon><Brush /></el-icon>
      </span>
    </template>
    <div class="theme-panel">
      <div class="theme-group">
        <div class="theme-label">外观模式</div>
        <div class="theme-mode-row">
          <button v-for="m in THEME_MODES" :key="m.value" type="button"
                  class="theme-mode-btn" :class="{ on: themeMode === m.value }"
                  @click="chooseMode(m.value)">
            <el-icon><component :is="MODE_ICONS[m.value]" /></el-icon>
            {{ m.label }}
          </button>
        </div>
      </div>
      <div class="theme-group">
        <div class="theme-label">主题色</div>
        <div class="theme-accent-row">
          <span v-for="a in THEME_ACCENTS" :key="a.value"
                class="theme-accent" :class="{ on: themeAccent === a.value }"
                :style="{ background: a.color }" :title="a.label"
                @click="chooseAccent(a.value)"></span>
          <span class="theme-accent-name">{{ accentLabel }}</span>
        </div>
      </div>
      <p class="theme-tip">选择立即生效，保存在本机浏览器；「跟随系统」会随系统深浅色自动切换。</p>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Brush, Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import { THEME_MODES, THEME_ACCENTS, getTheme, setThemeMode, setThemeAccent } from '../utils/theme'

const MODE_ICONS = { light: Sunny, dark: Moon, auto: Monitor }
const initialTheme = getTheme()
const themeMode = ref(initialTheme.mode)
const themeAccent = ref(initialTheme.accent)
const accentLabel = computed(() => (THEME_ACCENTS.find(x => x.value === themeAccent.value) || {}).label || '')

function chooseMode(mode) {
  setThemeMode(mode)
  themeMode.value = mode
}

function chooseAccent(accent) {
  setThemeAccent(accent)
  themeAccent.value = accent
}
</script>
