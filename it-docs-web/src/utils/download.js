/** 触发浏览器下载（后端返回 CSV Blob 或文本） */
export function downloadBlob(data, filename) {
  const blob = data instanceof Blob
    ? data
    : new Blob([data], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
