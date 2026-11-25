// 系统内置枚举的中文名称：全站唯一出处，改文案只改这一处。
// 业务系统 / 问题分类属于可维护的业务字典，走「基础数据」页面（/console/meta），不在这里。
// 这些值参与接口与鉴权判断，code 不能改，只改展示名称。

export const ROLE_LABELS = {
  ADMIN: '管理员',
  EDITOR: '编辑',
  VIEWER: '只读'
}

export const ROLE_TAG_TYPES = {
  ADMIN: 'danger',
  EDITOR: 'primary',
  VIEWER: 'info'
}

export const DOC_TYPE_LABELS = {
  qa: '问题记录',
  req: '业务文档',
  tech: '技术文档',
  md: '旧版 Markdown'
}

export const VISIBILITY_LABELS = {
  PUBLIC: '公开',
  INTERNAL: '内部',
  RESTRICTED: '名单'
}

export const AUTH_SOURCE_LABELS = {
  LDAP: '域账号',
  LOCAL: '本地账号'
}

export const SPACE_TYPE_LABELS = {
  PUBLIC: '公共空间',
  PERSONAL: '个人空间'
}

export const STATUS_LABELS = {
  ACTIVE: '启用',
  DISABLED: '停用'
}

export function optionsOf(map) {
  return Object.entries(map).map(([value, label]) => ({ value, label }))
}

export const ROLE_OPTIONS = optionsOf(ROLE_LABELS)
export const DOC_TYPE_OPTIONS = optionsOf(DOC_TYPE_LABELS)
export const AUTH_SOURCE_OPTIONS = optionsOf(AUTH_SOURCE_LABELS)

export const labelOf = (map, code, fallback = '—') => map[code] || fallback
export const roleLabel = code => labelOf(ROLE_LABELS, code)
export const roleTagType = code => ROLE_TAG_TYPES[code] || 'info'
export const docTypeLabel = code => labelOf(DOC_TYPE_LABELS, code, '文档')
export const visibilityLabel = code => labelOf(VISIBILITY_LABELS, code, '内部')
export const authSourceLabel = code => labelOf(AUTH_SOURCE_LABELS, code, '本地账号')
export const spaceTypeLabel = code => labelOf(SPACE_TYPE_LABELS, code, '公共空间')
export const statusLabel = code => labelOf(STATUS_LABELS, code, '停用')
