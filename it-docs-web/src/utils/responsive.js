import { ref, onMounted, onBeforeUnmount } from 'vue'

// 管理端表格在窄屏只保留关键列，避免 fixed 操作列挤占内容。
export function useNarrowScreen(maxWidth = 768) {
  const isNarrow = ref(false)
  let query
  const update = () => { if (query) isNarrow.value = query.matches }

  onMounted(() => {
    query = window.matchMedia(`(max-width: ${maxWidth}px)`)
    update()
    query.addEventListener('change', update)
  })

  onBeforeUnmount(() => {
    if (query) query.removeEventListener('change', update)
  })

  return isNarrow
}
