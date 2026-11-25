import api from './index'

// 字典接口 + 前端缓存：编辑表单/详情页/列表页共用，改字典后 invalidateMeta() 失效
const cache = new Map()

export async function fetchMeta(type) {
  if (cache.has(type)) return cache.get(type)
  const res = await api.get('/meta', { params: type ? { type } : {} })
  const list = res.data || []
  cache.set(type, list)
  return list
}

export function invalidateMeta() {
  cache.clear()
}

// 字典项颜色（空色给默认灰）
export function metaColor(item) {
  return (item && item.color) || '#909399'
}

// 标签不是字典（直接存文档 tags 字段），颜色按名称哈希稳定生成：同一个标签全站同色
const TAG_COLORS = [
  '#4E79C7', '#2FA48A', '#C9843A', '#8A63C4',
  '#C0555F', '#3E9BC4', '#6D8B3F', '#A85A9E'
]

export function tagColor(name) {
  const s = String(name || '')
  let h = 0
  for (let i = 0; i < s.length; i++) {
    h = (h * 31 + s.charCodeAt(i)) >>> 0
  }
  return TAG_COLORS[h % TAG_COLORS.length]
}

export const META_TYPES = [
  { type: 'BIZ_SYSTEM', label: '业务系统' },
  { type: 'CATEGORY', label: '问题分类' }
]
