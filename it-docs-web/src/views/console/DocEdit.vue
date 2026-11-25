<template>
  <div>
    <div class="page">
      <div class="page-toolbar">
        <el-link type="primary" :underline="false" @click="$router.back()">‹ 返回</el-link>
        <span class="page-title" style="margin:0">{{ isPersonal ? '个人草稿' : (doc.id ? '编辑' : '新建') }}</span>
        <span class="grow"></span>
        <el-button v-if="doc.id" plain @click="openVersions">历史版本</el-button>
        <el-button v-if="doc.id && !isPersonal" plain @click="openAcl">访问范围</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
      <div class="kb-card">
        <el-form label-width="90px">
          <el-row :gutter="12">
            <el-col :span="24"><el-form-item label="标题" required>
              <el-input v-model="doc.title" maxlength="200" />
            </el-form-item></el-col>
            <el-col :span="24"><el-form-item :label="doc.docType === 'qa' ? '一句话答案' : '摘要'">
              <el-input v-model="doc.summary" maxlength="500" show-word-limit
                        :placeholder="doc.docType === 'qa'
                          ? '一句话说清怎么解决，会展示在列表和阅读页顶部'
                          : '一句话概括这份文档，列表展示用'" />
            </el-form-item></el-col>
            <el-col :xs="24" :sm="12"><el-form-item label="类型">
              <el-select v-model="doc.docType" :disabled="!!doc.id" style="width:100%" placeholder="选择内容类型">
                <el-option label="问题记录" value="qa" />
                <el-option label="业务文档" value="req" />
                <el-option label="技术文档" value="tech" />
              </el-select>
            </el-form-item></el-col>
            <el-col :xs="24" :sm="12" v-if="!isPersonal"><el-form-item label="知识空间">
              <el-select v-model="doc.spaceId" style="width:100%" placeholder="选择知识空间">
                <el-option-group v-if="groupedSpaces.qa.length" label="问题记录">
                  <el-option v-for="s in groupedSpaces.qa" :key="s.id" :label="s.spaceName" :value="s.id" />
                </el-option-group>
                <el-option-group v-if="groupedSpaces.req.length" label="业务文档">
                  <el-option v-for="s in groupedSpaces.req" :key="s.id" :label="s.spaceName" :value="s.id" />
                </el-option-group>
                <el-option-group v-if="groupedSpaces.tech.length" label="技术文档">
                  <el-option v-for="s in groupedSpaces.tech" :key="s.id" :label="s.spaceName" :value="s.id" />
                </el-option-group>
                <el-option-group v-if="groupedSpaces.other.length" label="其他空间">
                  <el-option v-for="s in groupedSpaces.other" :key="s.id" :label="s.spaceName" :value="s.id" />
                </el-option-group>
              </el-select>
              <div class="field-hint">知识空间决定内容归属和访问范围；归属业务系统与问题分类用于描述内容本身。</div>
            </el-form-item></el-col>
          </el-row>

          <!-- qa 专属：system=工单归属系统，category=业务场景；下拉来自基础数据字典 -->
          <template v-if="doc.docType === 'qa'">
            <el-row :gutter="12">
              <el-col :xs="24" :sm="12" :md="8"><el-form-item label="归属系统">
                <el-select v-model="doc.system" style="width:100%" clearable filterable allow-create
                           default-first-option placeholder="选择或直接输入">
                  <el-option v-for="m in dict.BIZ_SYSTEM" :key="m.id" :label="m.name" :value="m.name">
                    <span class="opt-dot" :style="{ background: metaColor(m) }"></span>{{ m.name }}
                  </el-option>
                </el-select>
              </el-form-item></el-col>
              <el-col :xs="24" :sm="12" :md="8"><el-form-item label="问题分类">
                <el-select v-model="doc.category" style="width:100%" clearable filterable allow-create
                           default-first-option :placeholder="doc.system ? '选择或直接输入' : '先选归属系统，未绑定为通用'">
                  <el-option v-for="m in catOptions" :key="m.id" :label="m.name" :value="m.name">
                    <span class="opt-dot" :style="{ background: metaColor(m) }"></span>{{ m.name }}
                    <span v-if="!m.parentId" style="float:right;color:var(--kb-text-muted);font-size:12px">通用</span>
                  </el-option>
                </el-select>
              </el-form-item></el-col>
              <el-col :xs="24" :sm="12" :md="8"><el-form-item label="标签">
                <div class="tag-editor">
                  <el-tag v-for="t in tagList" :key="t" closable
                          class="tag-chip" @close="removeTag(t)">{{ t }}</el-tag>
                  <input v-model="tagDraft" class="tag-input" maxlength="20"
                         :placeholder="tagList.length ? '继续添加' : '输入后回车，如 高频'"
                         @keydown="onTagKey" @blur="commitTag" />
                </div>
              </el-form-item></el-col>
            </el-row>
            <el-form-item label="报错原文">
              <el-input v-model="qa.errorText" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="原因">
              <el-input v-model="qa.cause" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="处理步骤">
              <el-input v-model="stepsText" type="textarea" :rows="4" placeholder="一行一步骤" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="tipsText" type="textarea" :rows="2" placeholder="一行一条" />
            </el-form-item>
            <el-form-item label="截图">
              <div style="width:100%">
                <input type="file" accept="image/png,image/jpeg,image/webp,image/gif" multiple
                       ref="imgInput" @change="uploadImages" style="display:none" />
                <el-button size="small" @click="$refs.imgInput.click()">上传截图</el-button>
                <span style="color:var(--kb-text-faint);font-size:12px;margin-left:8px">原图整张保留，不裁剪</span>
                <div v-for="(img, i) in qa.images" :key="i"
                     style="display:flex;align-items:center;gap:10px;margin-top:8px">
                  <img :src="img.src" style="width:80px;height:50px;object-fit:cover;border:1px solid var(--kb-border);border-radius:4px" />
                  <el-input v-model="img.caption" placeholder="说明" size="small" style="flex:1" />
                  <el-button size="small" text type="danger" @click="qa.images.splice(i,1)">移除</el-button>
                </div>
              </div>
            </el-form-item>
          </template>

          <!-- 业务文档 / 技术文档：同样按业务系统归档，培训手册等用标签区分 -->
          <el-row v-else :gutter="12">
            <el-col :xs="24" :sm="12"><el-form-item label="归属系统">
              <el-select v-model="doc.system" style="width:100%" clearable filterable allow-create
                         default-first-option placeholder="选择或直接输入">
                <el-option v-for="m in dict.BIZ_SYSTEM" :key="m.id" :label="m.name" :value="m.name">
                  <span class="opt-dot" :style="{ background: metaColor(m) }"></span>{{ m.name }}
                </el-option>
              </el-select>
            </el-form-item></el-col>
            <el-col :xs="24" :sm="12"><el-form-item label="标签">
              <div class="tag-editor">
                <el-tag v-for="t in tagList" :key="t" closable
                        class="tag-chip" @close="removeTag(t)">{{ t }}</el-tag>
                <input v-model="tagDraft" class="tag-input" maxlength="20"
                       :placeholder="tagList.length ? '继续添加' : '输入后回车，如 培训手册'"
                       @keydown="onTagKey" @blur="commitTag" />
              </div>
            </el-form-item></el-col>
          </el-row>

          <el-form-item v-if="doc.docType !== 'qa'" label="正文">
            <div style="width:100%;border:1px solid var(--kb-border);border-radius:8px;overflow:hidden">
              <MdEditor v-model="doc.contentMd" :style="{ height: '520px' }"
                        :theme="isDark ? 'dark' : 'light'"
                        @onUploadImg="uploadMdImages" language="zh-CN" />
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
    <el-dialog v-model="aclVisible" title="单篇文档访问范围" width="560px" destroy-on-close>
      <p class="field-hint acl-hint">不设置名单时继承空间权限；添加名单后，仅名单中的用户或 LDAP 组可读。系统管理员可读并留有审计记录。</p>
      <div v-for="(item, index) in aclItems" :key="index" class="acl-item">
        <el-select v-model="item.subjectType" style="width:140px">
          <el-option label="指定用户" value="USER" />
          <el-option label="LDAP 用户组" value="LDAP_GROUP" />
        </el-select>
        <el-input v-model="item.subjectName" :placeholder="item.subjectType === 'USER' ? '用户名' : '组名或组 DN'" />
        <el-button text type="danger" @click="aclItems.splice(index, 1)">移除</el-button>
      </div>
      <el-button plain @click="aclItems.push({ subjectType: 'USER', subjectName: '' })">添加授权对象</el-button>
      <template #footer>
        <el-button @click="aclVisible = false">取消</el-button>
        <el-button type="primary" :loading="aclSaving" @click="saveAcl">保存访问范围</el-button>
      </template>
    </el-dialog>

    <!-- 历史版本：列表只拉元信息，选中后再取正文，避免一次加载全部内容 -->
    <el-drawer v-model="versionVisible" title="历史版本" size="min(760px, 96vw)" destroy-on-close>
      <div class="version-layout" v-loading="versionLoading">
        <div class="version-list">
          <button v-for="v in versions" :key="v.id" type="button" class="version-item"
                  :class="{ on: v.versionNo === selectedVersionNo }" @click="loadVersion(v)">
            <span class="version-no">v{{ v.versionNo }}<span v-if="isLegacyVersion(v)" class="version-legacy">旧</span></span>
            <span class="version-title">{{ v.title || '（无标题）' }}</span>
            <span class="version-meta">{{ v.operator || '—' }} · {{ formatTime(v.createdAt) }}</span>
          </button>
          <el-empty v-if="!versionLoading && !versions.length" description="还没有历史版本" :image-size="60" />
        </div>

        <div class="version-preview" v-if="versionDetail">
          <div class="version-preview-head">
            <div class="version-preview-title">
              <strong>{{ versionDetail.title || '（无标题）' }}</strong>
              <span>v{{ versionDetail.versionNo }} · {{ versionDetail.operator || '—' }} · {{ formatTime(versionDetail.createdAt) }}</span>
            </div>
            <el-button type="primary" size="small" :loading="restoring" @click="restoreVersion(versionDetail)">
              回滚到此版本
            </el-button>
          </div>
          <div class="version-meta-grid">
            <span v-if="versionDetail.summary" class="version-summary">一句话答案：{{ versionDetail.summary }}</span>
            <span>系统：{{ versionDetail.system || '—' }}</span>
            <span>分类：{{ versionDetail.category || '—' }}</span>
            <span v-if="versionDetail.owner">处理角色：{{ versionDetail.owner }}</span>
            <span v-if="versionDetail.tags">标签：{{ versionDetail.tags }}</span>
          </div>
          <el-alert v-if="isLegacyVersion(versionDetail)" type="info" :closable="false" show-icon
                    class="version-legacy-tip"
                    title="该版本是旧快照：只包含标题与正文，回滚不会改动摘要、系统、分类、处理角色和标签。" />
          <div class="version-content">
            <MdPreview :modelValue="versionDetail.contentMd || ''" :theme="isDark ? 'dark' : 'light'" />
            <pre v-if="versionDetail.contentJson" class="version-json">{{ prettyJson(versionDetail.contentJson) }}</pre>
          </div>
          <p class="field-hint">回滚会按该版本快照恢复标题、摘要、系统、分类、处理角色、标签与正文；当前内容会先存为新版本，未保存的修改会丢失。</p>
        </div>
        <el-empty v-else-if="!versionLoading" description="选择一个版本查看内容" :image-size="60" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdEditor, MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { isDark } from '../../utils/theme'
import api from '../../api'
import { useUserStore } from '../../stores/user'
import { fetchMeta, metaColor } from '../../api/meta'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const isPersonal = computed(() => route.path.startsWith('/personal'))
const saving = ref(false)
const imgInput = ref(null)
const aclVisible = ref(false)
const aclSaving = ref(false)
const aclItems = ref([])

// 历史版本
const versionVisible = ref(false)
const versionLoading = ref(false)
const versions = ref([])
const selectedVersionNo = ref(null)
const versionDetail = ref(null)
const restoring = ref(false)

// 表单数据（必须在下面的 computed/watch 之前声明：watch 的 getter 在 setup 阶段就会执行）
const doc = reactive({
  id: null, docType: 'tech', title: '', spaceId: null, summary: '',
  system: '', category: '', owner: '', contentMd: '', tags: ''
})
const qa = reactive({ errorText: '', cause: '', steps: [], tips: [], images: [] })
const stepsText = ref('')
const tipsText = ref('')

// 标签：doc.tags 存逗号字符串，编辑时按 chip 拆开
const MAX_TAGS = 10
const tagDraft = ref('')
const tagList = computed(() => (doc.tags || '').split(/[,，]/).map(s => s.trim()).filter(Boolean))

function commitTag() {
  const v = tagDraft.value.replace(/[,，]/g, '').trim()
  tagDraft.value = ''
  if (!v || tagList.value.includes(v)) return
  if (tagList.value.length >= MAX_TAGS) {
    ElMessage.warning('标签最多 ' + MAX_TAGS + ' 个')
    return
  }
  const next = [...tagList.value, v].join(',')
  // 后端 tags 字段是 VARCHAR(200)，超了会写库失败
  if (next.length > 200) {
    ElMessage.warning('标签总长度不能超过 200 字')
    return
  }
  doc.tags = next
}

function removeTag(t) {
  doc.tags = tagList.value.filter(x => x !== t).join(',')
}

function onTagKey(e) {
  if (e.key === 'Enter' || e.key === ',' || e.key === '，') {
    e.preventDefault()
    commitTag()
  } else if (e.key === 'Backspace' && !tagDraft.value && tagList.value.length) {
    removeTag(tagList.value[tagList.value.length - 1])
  }
}

// 字典：业务系统 / 问题分类（标签不入字典，直接写 doc.tags 字符串）
const dict = reactive({ BIZ_SYSTEM: [], CATEGORY: [] })

// 级联：选了系统 → 分类只出「该系统专属 + 通用」；没选系统 → 全部
const catOptions = computed(() => {
  const sys = dict.BIZ_SYSTEM.find(m => m.name === doc.system)
  if (!sys) return dict.CATEGORY
  return dict.CATEGORY.filter(c => !c.parentId || c.parentId === sys.id)
})

// 空间按内容类型分组：优先认标准空间编码，自定义空间按名称兜底
function spaceGroup(s) {
  const code = String(s.spaceCode || '').toUpperCase()
  if (code === 'QA') return 'qa'
  if (code === 'REQ') return 'req'
  if (code === 'TECH') return 'tech'
  const name = s.spaceName || ''
  if (/技术文档/.test(name)) return 'tech'
  if (/业务文档|培训|手册/.test(name)) return 'req'
  if (/问题|问答/.test(name)) return 'qa'
  return 'other'
}
const groupedSpaces = computed(() => {
  const out = { qa: [], req: [], tech: [], other: [] }
  user.spaces.forEach(s => out[spaceGroup(s)].push(s))
  return out
})

// 新建时切类型：空间跟着切到对应分组，避免问题记录存进技术文档空间
watch(() => doc.docType, t => {
  if (doc.id) return
  const current = user.spaces.find(s => s.id === doc.spaceId)
  if (current && spaceGroup(current) === t) return
  const preferred = groupedSpaces.value[t]
  if (preferred && preferred.length) doc.spaceId = preferred[0].id
})

// 换系统后当前分类不再可选时清空（防提交无效组合）
watch(() => doc.system, () => {
  if (doc.category && !catOptions.value.some(c => c.name === doc.category)) {
    doc.category = ''
  }
})

function parseLines(t) {
  return (t || '').split(/\r?\n/).map(s => s.trim()).filter(Boolean)
}

async function loadDoc(id) {
  const res = await api.get('/docs/' + id)
  const d = res.data
  Object.assign(doc, d)
  try {
    if (d.contentJson) {
      const content = JSON.parse(d.contentJson)
      delete content.sql // 兼容旧记录，但不再在表单或新保存内容中保留运维 SQL
      Object.assign(qa, content)
    }
  } catch (e) { /* 空 */ }
  if (!qa.images) qa.images = []
  stepsText.value = (qa.steps || []).join('\n')
  tipsText.value = (qa.tips || []).join('\n')
}

async function openAcl() {
  const res = await api.get('/docs/' + doc.id + '/acl')
  aclItems.value = (res.data || []).map(x => ({ subjectType: x.subjectType, subjectName: x.subjectName }))
  aclVisible.value = true
}

async function saveAcl() {
  const rows = aclItems.value.filter(x => x.subjectName && x.subjectName.trim())
  aclSaving.value = true
  try {
    await api.put('/docs/' + doc.id + '/acl', rows)
    ElMessage.success(rows.length ? '已限制为授权名单可读' : '已恢复继承空间权限')
    aclVisible.value = false
  } finally {
    aclSaving.value = false
  }
}

function formatTime(t) {
  return String(t || '').substring(0, 16).replace('T', ' ')
}

function prettyJson(s) {
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch (e) {
    return s
  }
}

async function openVersions() {
  versionVisible.value = true
  versionLoading.value = true
  versionDetail.value = null
  selectedVersionNo.value = null
  try {
    const res = await api.get('/docs/' + doc.id + '/versions')
    versions.value = res.data || []
    if (versions.value.length) {
      await loadVersion(versions.value[0])
    }
  } finally {
    versionLoading.value = false
  }
}

async function loadVersion(v) {
  selectedVersionNo.value = v.versionNo
  versionLoading.value = true
  try {
    const res = await api.get('/docs/' + doc.id + '/versions/' + v.versionNo)
    versionDetail.value = res.data
  } finally {
    versionLoading.value = false
  }
}

function isLegacyVersion(v) {
  return !!v && (v.snapshotV == null || v.snapshotV < 2)
}

async function restoreVersion(v) {
  const scope = isLegacyVersion(v)
    ? '标题与正文'
    : '标题、摘要、系统、分类、处理角色、标签与正文'
  try {
    await ElMessageBox.confirm(
      `回滚到 v${v.versionNo}？${scope}会恢复成该版本；当前内容会先保存为新版本，历史不会丢；未保存的修改会丢失。`,
      '确认回滚',
      { type: 'warning', confirmButtonText: '回滚', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  restoring.value = true
  try {
    await api.post('/docs/' + doc.id + '/versions/' + v.versionNo + '/restore')
    await loadDoc(doc.id)
    versionVisible.value = false
    ElMessage.success('已回滚到 v' + v.versionNo)
  } finally {
    restoring.value = false
  }
}

async function uploadFiles(files) {
  const out = []
  for (const f of files) {
    const fd = new FormData()
    fd.append('file', f)
    fd.append('bizType', 'IMAGE')
    if (doc.id) fd.append('docId', doc.id)
    const res = await api.post('/files', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    out.push(res.data)
  }
  return out
}

async function uploadImages(ev) {
  const files = Array.from(ev.target.files || [])
  if (!files.length) return
  const uploaded = await uploadFiles(files)
  uploaded.forEach(u => qa.images.push({ src: u.url, caption: u.name }))
  ev.target.value = ''
}

// md-editor-v3 粘贴/选择图片：上传后插入 Markdown 图片语法
async function uploadMdImages(files, callback) {
  const uploaded = await uploadFiles(files)
  callback(uploaded.map(u => ({ url: u.url, alt: u.name, title: u.name })))
}

async function save() {
  if (!doc.title || !doc.title.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  const body = { ...doc }
  // 标签就是文档上的一个字段：中英文逗号都认，去空去重后用英文逗号存
  body.tags = (doc.tags || '')
    .split(/[,，]/).map(s => s.trim()).filter(Boolean)
    .filter((s, i, a) => a.indexOf(s) === i)
    .join(',')
  if (doc.docType === 'qa') {
    qa.steps = parseLines(stepsText.value)
    qa.tips = parseLines(tipsText.value)
    const { errorText, cause, steps, tips, images } = qa
    body.contentJson = JSON.stringify({ errorText, cause, steps, tips, images })
    body.contentMd = qa.errorText || ''
  }
  // 保存目标：个人路径强制个人空间；否则用所选空间
  if (isPersonal.value) {
    body.spaceId = user.personalSpaceId
    if (!body.spaceId) {
      const me = await user.fetchMe(api)
      body.spaceId = me.personalSpace?.id
    }
  }
  saving.value = true
  try {
    if (body.id) {
      await api.put('/docs/' + body.id, body)
    } else {
      const res = await api.post('/docs', body)
      body.id = res.data.id
    }
    const fileIds = new Set()
    const content = [body.contentMd || '', body.contentJson || ''].join('\n')
    for (const match of content.matchAll(/\/api\/files\/(\d+)\//g)) fileIds.add(match[1])
    await Promise.all(Array.from(fileIds).map(id => api.patch('/files/' + id + '/doc/' + body.id)))
    ElMessage.success('已保存，实时生效')
    // 保存后回到原来那个系统的同一类列表
    const back = { docType: doc.docType === 'md' ? 'qa' : doc.docType }
    if (doc.system) back.system = doc.system
    router.push(isPersonal.value ? '/personal' : { path: '/console', query: back })
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!user.spaces.length || !user.personalSpaceId) {
    await user.fetchMe(api).catch(() => {})
  }
  const id = route.params.id
  // 工作台通过 ?docType= / ?system= 指定新建内容的类型和归属系统
  if (!id && ['qa', 'tech', 'req'].includes(route.query.docType)) {
    doc.docType = route.query.docType
  }
  if (!id && route.query.system) {
    doc.system = route.query.system
  }
  if (!isPersonal.value && user.spaces.length && !doc.spaceId) {
    const preferred = groupedSpaces.value[doc.docType]
    doc.spaceId = (preferred && preferred.length ? preferred[0] : user.spaces[0]).id
  }
  // 字典加载失败不阻塞编辑（下拉退化为手输）
  await Promise.all([
    fetchMeta('BIZ_SYSTEM').then(l => dict.BIZ_SYSTEM = l).catch(() => {}),
    fetchMeta('CATEGORY').then(l => dict.CATEGORY = l).catch(() => {})
  ])
  if (id) {
    await loadDoc(id)
    // 工作台「版本」按钮：进来直接打开历史版本抽屉
    if (route.query.versions === '1' && doc.id) openVersions()
  }
})
</script>

<style scoped>
.opt-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 8px;
  border: 1px solid var(--kb-dot-border);
}
.field-hint { margin-top: 5px; color: var(--kb-text-muted); font-size: 12px; line-height: 1.4; }
.tag-editor {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px;
  width: 100%; min-height: 32px; padding: 3px 8px;
  border: 1px solid var(--kb-border); border-radius: 6px; background: var(--kb-surface);
  transition: border-color .15s;
}
.tag-editor:focus-within { border-color: var(--kb-primary); }
.tag-chip { margin: 0; }
.tag-input {
  flex: 1 1 90px; min-width: 90px; height: 24px; padding: 0;
  border: 0; outline: 0; background: transparent; color: var(--kb-text); font-size: 13px;
}
.tag-input::placeholder { color: var(--kb-text-faint); }
.acl-hint { margin: 0 0 16px; }
.acl-item { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }

.version-layout { display: flex; gap: 16px; height: 100%; min-height: 420px; }
.version-list {
  flex: 0 0 210px; overflow-y: auto; padding-right: 4px;
  border-right: 1px solid var(--kb-border);
}
.version-item {
  display: flex; flex-direction: column; gap: 3px; width: 100%;
  margin-bottom: 6px; padding: 10px 12px; border: 1px solid transparent;
  border-radius: 8px; background: var(--kb-surface-2); color: var(--kb-text);
  text-align: left; font: inherit; cursor: pointer;
}
.version-item:hover { border-color: var(--kb-border-hover); }
.version-item.on { border-color: var(--kb-primary); background: var(--kb-primary-soft); }
.version-no { color: var(--kb-primary); font-size: 12px; font-weight: 700; }
.version-legacy {
  margin-left: 4px; padding: 0 4px; border-radius: 4px;
  background: var(--el-color-warning-light-9); color: var(--el-color-warning);
  font-size: 10px; font-weight: 500;
}
.version-title { font-size: 13px; font-weight: 600; line-height: 1.4; }
.version-meta { color: var(--kb-text-muted); font-size: 11px; }
.version-preview { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.version-preview-head {
  display: flex; justify-content: space-between; align-items: center; gap: 10px;
  margin-bottom: 10px; color: var(--kb-text-muted); font-size: 12px;
}
.version-preview-title { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.version-preview-title strong {
  color: var(--kb-text); font-size: 14px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.version-meta-grid {
  display: flex; flex-wrap: wrap; gap: 6px 14px;
  margin-bottom: 10px; color: var(--kb-text-2); font-size: 12px;
}
.version-meta-grid .version-summary { flex-basis: 100%; color: var(--kb-text); }
.version-legacy-tip { margin-bottom: 10px; }
.version-content {
  flex: 1; overflow-y: auto; padding: 12px 14px;
  border: 1px solid var(--kb-border); border-radius: 8px; background: var(--kb-surface);
}
.version-content :deep(.md-editor-preview) { font-size: 14px; line-height: 1.8; }
.version-json {
  margin: 12px 0 0; padding: 10px 12px; overflow: auto;
  border: 1px solid var(--kb-border); border-radius: 6px;
  background: var(--kb-code-bg); color: var(--kb-text-2);
  font: 12px/1.7 ui-monospace, SFMono-Regular, Consolas, monospace; white-space: pre-wrap;
}
@media (max-width: 768px) {
  .version-layout { flex-direction: column; }
  .version-list { flex: 0 0 auto; max-height: 180px; border-right: 0; border-bottom: 1px solid var(--kb-border); }
}
</style>
