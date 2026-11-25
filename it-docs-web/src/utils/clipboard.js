import { ElMessage } from 'element-plus'

/**
 * 复制文本到剪贴板：优先 Clipboard API（需 HTTPS/localhost），
 * 不支持或拒绝时降级 execCommand，最后再降级手动选中，保证到处可用。
 */
export async function copyText(text, okMsg = '已复制') {
  const s = String(text || '')
  if (!s) return false
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(s)
      ElMessage.success(okMsg)
      return true
    }
  } catch (e) {
    // 落到降级方案
  }
  try {
    const ta = document.createElement('textarea')
    ta.value = s
    ta.style.position = 'fixed'
    ta.style.left = '-9999px'
    document.body.appendChild(ta)
    ta.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(ta)
    if (ok) {
      ElMessage.success(okMsg)
      return true
    }
  } catch (e) {
    // ignore
  }
  ElMessage.warning('复制失败，请手动复制地址栏链接')
  window.prompt('手动复制链接：', s)
  return false
}

/** 当前域名下的文档永久链接 */
export function docLink(id) {
  return window.location.origin + '/doc/' + id
}
