<template>
  <div class="page console-page">
    <!-- 工作台首页：按业务系统组织内容 -->
    <template v-if="!docType">
      <div class="console-head">
        <div>
          <h1 class="console-title">工作台</h1>
          <p class="console-sub">按业务系统查看问题记录、业务文档和技术文档；点开系统即可维护。</p>
        </div>
        <div class="console-head-actions">
          <el-dropdown trigger="click" @command="newDoc">
            <el-button type="primary">
              新建内容<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="qa">问题记录</el-dropdown-item>
                <el-dropdown-item command="req">业务文档</el-dropdown-item>
                <el-dropdown-item command="tech">技术文档</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-if="canWrite" @click="importVisible = true">批量导入</el-button>
        </div>
      </div>

      <div class="stat-row">
        <div class="stat-card blue"><span>问题记录</span><b>{{ counts.qa }}</b></div>
        <div class="stat-card orange"><span>业务文档</span><b>{{ counts.req }}</b></div>
        <div class="stat-card green"><span>技术文档</span><b>{{ counts.tech }}</b></div>
        <div class="stat-card pink"><span>个人草稿</span><b>{{ counts.personal }}</b></div>
      </div>

      <div class="section-head">
        <h2 class="section-title">业务系统</h2>
        <span class="section-hint">共 {{ systems.length }} 个系统 · 点击进入</span>
      </div>

      <div v-loading="homeLoading" class="system-grid">
        <button v-for="m in systems" :key="m.id" type="button" class="system-card"
                @click="openSystem(m.name)">
          <span class="system-bar" :style="{ background: metaColor(m) }"></span>
          <span class="system-name">{{ m.name }}</span>
          <span class="system-counts">
            <span class="sc"><em>问题记录</em><b>{{ sysCount(m.name).qa }}</b></span>
            <span class="sc"><em>业务文档</em><b>{{ sysCount(m.name).req }}</b></span>
            <span class="sc"><em>技术文档</em><b>{{ sysCount(m.name).tech }}</b></span>
          </span>
        </button>
        <div v-if="!homeLoading && !systems.length" class="system-empty">
          还没有业务系统，先到「基础数据」里添加
        </div>
      </div>

      <div class="home-links">
        <a @click="goList('qa')">全部问题记录 ›</a>
        <a @click="goList('req')">全部业务文档 ›</a>
        <a @click="goList('tech')">全部技术文档 ›</a>
      </div>
    </template>

    <!-- 某个系统 / 某一类内容的列表 -->
    <template v-else>
      <div class="list-head">
        <button type="button" class="back-link" @click="goHome">‹ 工作台</button>
        <h1 class="console-title">{{ system || '全部' + typeLabel(docType) }}</h1>
        <span class="spacer"></span>
        <el-button type="primary" @click="newDoc(docType)">新建{{ typeLabel(docType) }}</el-button>
      </div>

      <div class="type-tabs">
        <button v-for="t in typeTabs" :key="t.value" type="button" class="type-tab"
                :class="{ on: docType === t.value }" @click="switchType(t.value)">
          {{ t.label }}<span class="type-count">{{ t.count }}</span>
        </button>
      </div>

      <div class="kb-card table-card">
        <div class="table-toolbar">
          <el-select :model-value="system || ''" class="console-select" @change="switchSystem">
            <el-option label="全部系统" value="" />
            <el-option v-for="m in systems" :key="m.id" :label="m.name" :value="m.name" />
          </el-select>
          <el-input v-model="keyword" placeholder="搜索标题或关键词" clearable class="console-search"
                    @keyup.enter="load(1)" @clear="load(1)">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button @click="load(1)">搜索</el-button>
          <span class="spacer"></span>
          <el-button v-if="canWrite" text :loading="exporting" @click="exportCsv">导出 CSV</el-button>
        </div>

        <el-table :data="list" v-loading="loading" class="content-table">
          <el-table-column v-if="!isNarrow" label="编号" width="170">
            <template #default="{ row }"><span class="doc-no">{{ row.docNo || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="标题" min-width="260">
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push('/console/edit/' + row.id)">{{ row.title }}</el-link>
              <el-tag v-if="row.status === 'DISABLED'" size="small" type="warning" effect="plain"
                      style="margin-left:8px">已下架</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="归属 · 空间" min-width="180">
            <template #default="{ row }">
              <div class="cell-strong">{{ row.system || docTypeLabel(row.docType) }}<template v-if="row.category"> · {{ row.category }}</template></div>
              <div class="cell-sub">{{ spaceName(row.spaceId) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="更新人 · 时间" width="170">
            <template #default="{ row }">
              <div class="cell-strong">{{ row.updatedBy || '—' }}</div>
              <div class="cell-sub">{{ (row.updatedAt || '').substring(0, 16) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" :width="isNarrow ? 232 : 272" :fixed="isNarrow ? false : 'right'">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="$router.push('/console/edit/' + row.id)">编辑</el-button>
              <el-button size="small" text @click="$router.push('/console/edit/' + row.id + '?versions=1')">版本</el-button>
              <el-button v-if="!isNarrow" size="small" text @click="$router.push('/doc/' + row.id)">预览</el-button>
              <el-popconfirm v-if="row.status === 'DISABLED'"
                             title="恢复上架？查阅端立即可见" @confirm="setStatus(row, 'ACTIVE')">
                <template #reference><el-button size="small" text type="success">恢复</el-button></template>
              </el-popconfirm>
              <el-popconfirm v-else
                             title="下架后查阅端不可见（数据保留，可随时恢复）。确定？" @confirm="setStatus(row, 'DISABLED')">
                <template #reference><el-button size="small" text type="warning">下架</el-button></template>
              </el-popconfirm>
              <el-popconfirm title="删除后会从列表移除，工作台不能直接恢复；管理员可从操作日志追溯。建议优先下架。确认删除？" @confirm="del(row)">
                <template #reference><el-button size="small" text type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination v-if="total > 20" layout="prev, pager, next, total" :total="total"
                       :page-size="20" :current-page="page" @current-change="load"
                       class="console-pagination" />
      </div>
    </template>

    <!-- 批量导入 CSV -->
    <el-dialog v-model="importVisible" title="批量导入文档" width="520px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
                title="按表头名匹配列（顺序可任意）；支持 UTF-8 / GBK，Excel 另存为 CSV 即可直接上传" />
      <el-form label-width="110px">
        <el-form-item label="目标空间" required>
          <el-select v-model="importSpaceId" style="width:100%" placeholder="行内 spaceId 为空时用这个">
            <el-option v-for="s in user.spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="CSV 文件" required>
          <input type="file" accept=".csv,text/csv" @change="onPickFile" />
          <span v-if="importFile" style="margin-left:8px;font-size:12px;color:var(--kb-text-muted)">{{ importFile.name }}</span>
        </el-form-item>
        <el-form-item label="重复处理">
          <el-checkbox v-model="updateExisting">按编号 / 同空间同标题更新已有文档（不勾则跳过）</el-checkbox>
        </el-form-item>
      </el-form>
      <div style="text-align:right;margin-top:-6px">
        <el-button text type="primary" @click="downloadTemplate">下载导入模板</el-button>
      </div>

      <div v-if="importResult" class="import-result">
        <p style="margin:0 0 6px">
          共 <b>{{ importResult.total }}</b> 行：
          新建 <b>{{ importResult.created }}</b> ·
          更新 <b>{{ importResult.updated }}</b> ·
          跳过 <b>{{ importResult.skipped }}</b>
          <span v-if="importResult.errors && importResult.errors.length" style="color:var(--kb-orange)">
            · 失败 {{ importResult.errors.length }} 条
          </span>
        </p>
        <div v-for="(e, i) in importResult.errors" :key="i" style="font-size:12px;color:var(--kb-orange)">{{ e }}</div>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile || !importSpaceId" @click="doImport">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Search } from '@element-plus/icons-vue'
import api from '../../api'
import { fetchMeta, metaColor } from '../../api/meta'
import { docTypeLabel } from '../../constants/labels'
import { useUserStore } from '../../stores/user'
import { downloadBlob } from '../../utils/download'
import { useNarrowScreen } from '../../utils/responsive'

const isNarrow = useNarrowScreen()
const route = useRoute()
const router = useRouter()
const user = useUserStore()

// ?docType= 有值 = 列表视图；没有 = 工作台首页（系统卡片）
const docType = computed(() => {
  const t = route.query.docType
  return ['qa', 'req', 'tech'].includes(t) ? t : null
})
const system = computed(() => route.query.system || null)

const systems = ref([])
const keyword = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const homeLoading = ref(false)
const exporting = ref(false)

const counts = reactive({ qa: 0, req: 0, tech: 0, personal: 0 })
const tabCounts = reactive({ qa: 0, req: 0, tech: 0 })
const sysCounts = reactive({})

const canWrite = computed(() => ['ADMIN', 'EDITOR'].includes(user.role))
const typeTabs = computed(() => [
  { value: 'qa', label: '问题记录', count: tabCounts.qa },
  { value: 'req', label: '业务文档', count: tabCounts.req },
  { value: 'tech', label: '技术文档', count: tabCounts.tech }
])

function typeLabel(t) {
  return docTypeLabel(t)
}

function sysCount(name) {
  return sysCounts[name] || { qa: 0, req: 0, tech: 0 }
}

function spaceName(id) {
  const s = user.spaces.find(x => x.id === id)
  return s ? s.spaceName : '—'
}

// ---------------- 导航 ----------------
function goHome() {
  router.push('/console')
}

function openSystem(name) {
  router.push({ path: '/console', query: { system: name, docType: 'qa' } })
}

function goList(type) {
  router.replace({ path: '/console', query: { docType: type } })
}

function switchType(type) {
  router.replace({
    path: '/console',
    query: system.value ? { docType: type, system: system.value } : { docType: type }
  })
}

function switchSystem(name) {
  const q = { docType: docType.value }
  if (name) q.system = name
  router.replace({ path: '/console', query: q })
}

function newDoc(type) {
  const query = { docType: type }
  if (system.value) query.system = system.value
  router.push({ path: '/console/edit', query })
}

// ---------------- 导入导出 ----------------
const importVisible = ref(false)
const importing = ref(false)
const importFile = ref(null)
const importSpaceId = ref(null)
const updateExisting = ref(false)
const importResult = ref(null)

function csvName(prefix) {
  const d = new Date()
  const day = d.getFullYear() + String(d.getMonth() + 1).padStart(2, '0') + String(d.getDate()).padStart(2, '0')
  return prefix + day + '.csv'
}

async function exportCsv() {
  exporting.value = true
  try {
    // 沿用当前筛选条件；responseType=blob 时拦截器直接返回 Blob
    const blob = await api.get('/docs/export', {
      params: {
        keyword: keyword.value || undefined,
        docType: docType.value || undefined,
        system: system.value || undefined
      },
      responseType: 'blob'
    })
    downloadBlob(blob, csvName('it-docs-'))
    ElMessage.success('已导出')
  } finally {
    exporting.value = false
  }
}

async function downloadTemplate() {
  const blob = await api.get('/docs/export/template', { responseType: 'blob' })
  downloadBlob(blob, csvName('it-docs-template-'))
}

function onPickFile(ev) {
  importFile.value = (ev.target.files || [])[0] || null
  importResult.value = null
}

async function doImport() {
  if (!importFile.value || !importSpaceId.value) return
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', importFile.value)
    fd.append('spaceId', importSpaceId.value)
    fd.append('updateExisting', updateExisting.value)
    const res = await api.post('/docs/import', fd)
    importResult.value = res.data
    ElMessage.success('导入完成')
    if (docType.value) {
      load(1)
      loadTabCounts()
    } else {
      loadHome()
    }
  } finally {
    importing.value = false
  }
}

// ---------------- 计数 ----------------
async function countOf(params) {
  try {
    const res = await api.get('/docs', { params: { ...params, includeDisabled: true, page: 1, size: 1 } })
    return res.data.total || 0
  } catch {
    return 0
  }
}

async function loadHome() {
  homeLoading.value = true
  try {
    const [qa, req, tech, personal] = await Promise.all([
      countOf({ docType: 'qa' }),
      countOf({ docType: 'req' }),
      countOf({ docType: 'tech' }),
      user.personalSpaceId ? countOf({ spaceId: user.personalSpaceId }) : Promise.resolve(0)
    ])
    Object.assign(counts, { qa, req, tech, personal })
    await Promise.all(systems.value.map(async m => {
      const [a, b, c] = await Promise.all([
        countOf({ docType: 'qa', system: m.name }),
        countOf({ docType: 'req', system: m.name }),
        countOf({ docType: 'tech', system: m.name })
      ])
      sysCounts[m.name] = { qa: a, req: b, tech: c }
    }))
  } finally {
    homeLoading.value = false
  }
}

async function loadTabCounts() {
  const sys = system.value || undefined
  const [qa, req, tech] = await Promise.all([
    countOf({ docType: 'qa', system: sys }),
    countOf({ docType: 'req', system: sys }),
    countOf({ docType: 'tech', system: sys })
  ])
  Object.assign(tabCounts, { qa, req, tech })
}

async function loadSystems() {
  try {
    systems.value = await fetchMeta('BIZ_SYSTEM')
  } catch {
    systems.value = []
  }
}

// ---------------- 列表 ----------------
async function load(p = 1) {
  page.value = p
  loading.value = true
  try {
    const res = await api.get('/docs', {
      params: {
        keyword: keyword.value || undefined,
        docType: docType.value || undefined,
        system: system.value || undefined,
        // 工作台要能看到已下架的，才能恢复
        includeDisabled: true,
        page: p, size: 20
      }
    })
    list.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function setStatus(row, status) {
  await api.patch('/docs/' + row.id + '/status', { status })
  ElMessage.success(status === 'DISABLED' ? '已下架，查阅端不再显示' : '已恢复')
  load(page.value)
  loadTabCounts()
}

async function del(row) {
  await api.delete('/docs/' + row.id)
  ElMessage.success('已删除')
  load(page.value)
  loadTabCounts()
}

async function ensureUser() {
  if (!user.spaces.length || !user.personalSpaceId) {
    await user.fetchMe(api).catch(() => {})
  }
  if (user.spaces.length) {
    importSpaceId.value = user.spaces[0].id
  }
}

onMounted(async () => {
  await Promise.all([ensureUser(), loadSystems()])
  if (!docType.value && system.value) {
    // 只带系统、没带类型：按问题记录打开该系统
    router.replace({ path: '/console', query: { system: system.value, docType: 'qa' } })
    return
  }
  if (docType.value) {
    load(1)
    loadTabCounts()
  } else {
    loadHome()
  }
})

watch(() => [route.query.docType, route.query.system].join('|'), () => {
  if (!docType.value) {
    if (system.value) {
      router.replace({ path: '/console', query: { system: system.value, docType: 'qa' } })
      return
    }
    loadHome()
    return
  }
  load(1)
  loadTabCounts()
})
</script>

<style scoped>
.console-page { padding-top: 20px; }
.console-head, .list-head {
  display: flex; align-items: flex-end; flex-wrap: wrap; gap: 12px;
  margin-bottom: 16px;
}
.console-head-actions { display: flex; gap: 10px; margin-left: auto; }
.console-title { margin: 0; font-size: 20px; font-weight: 600; color: var(--kb-text); }
.list-head { align-items: center; }
.console-sub { margin: 5px 0 0; font-size: 13px; color: var(--kb-text-muted); }
.back-link {
  border: 0; background: transparent; padding: 0; margin-right: 2px;
  color: var(--kb-text-muted); font: inherit; font-size: 13px; cursor: pointer;
}
.back-link:hover { color: var(--kb-primary); }
.spacer { flex: 1; }

.stat-row {
  display: grid; grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px; margin-bottom: 22px;
}
.stat-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-radius: 4px;
}
.stat-card span { font-size: 14px; color: var(--kb-text-2); }
.stat-card b { font-size: 26px; font-weight: 600; line-height: 1.2; }
.stat-card.blue { background: var(--kb-primary-soft); }
.stat-card.blue b { color: var(--kb-primary); }
.stat-card.green { background: var(--kb-green-bg); }
.stat-card.green b { color: var(--kb-green); }
.stat-card.orange { background: var(--kb-orange-bg); }
.stat-card.orange b { color: var(--kb-orange); }
.stat-card.pink { background: color-mix(in srgb, #f56c8c 10%, var(--kb-surface)); }
.stat-card.pink b { color: #e05a7e; }

/* 业务系统卡片 */
.section-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px; }
.section-title { margin: 0; font-size: 15px; font-weight: 600; color: var(--kb-text); }
.section-hint { font-size: 12px; color: var(--kb-text-muted); }
.system-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 14px; min-height: 90px;
}
.system-card {
  position: relative; display: flex; flex-direction: column; gap: 12px;
  padding: 16px 18px 14px 20px; overflow: hidden;
  background: var(--kb-surface); border: 1px solid var(--kb-border); border-radius: 8px;
  text-align: left; font: inherit; cursor: pointer;
  transition: border-color .15s, box-shadow .15s;
}
.system-card:hover { border-color: var(--kb-primary); box-shadow: 0 2px 10px var(--kb-shadow); }
.system-bar { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; }
.system-name { font-size: 16px; font-weight: 600; color: var(--kb-text); }
.system-card:hover .system-name { color: var(--kb-primary); }
.system-counts { display: flex; gap: 18px; }
.sc { display: flex; flex-direction: column; gap: 1px; }
.sc em { font-style: normal; font-size: 11.5px; color: var(--kb-text-muted); }
.sc b { font-size: 18px; font-weight: 600; color: var(--kb-text-2); line-height: 1.25; }
.system-empty {
  grid-column: 1 / -1; padding: 28px 0; text-align: center;
  color: var(--kb-text-muted); font-size: 13px;
}
.home-links { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 18px; }
.home-links a { color: var(--kb-text-2); font-size: 13px; cursor: pointer; }
.home-links a:hover { color: var(--kb-primary); }

/* 列表视图的类型 tab */
.type-tabs {
  display: flex; gap: 2px; margin-bottom: 12px;
  border-bottom: 1px solid var(--kb-border);
}
.type-tab {
  position: relative; display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 14px; border: 0; background: transparent;
  color: var(--kb-text-2); font: inherit; font-size: 14px; cursor: pointer;
}
.type-tab:hover { color: var(--kb-primary); }
.type-tab.on { color: var(--kb-primary); font-weight: 600; }
.type-tab.on::after {
  content: ''; position: absolute; left: 12px; right: 12px; bottom: -1px;
  height: 2px; background: var(--kb-primary);
}
.type-count { font-size: 12px; font-weight: 400; color: var(--kb-text-faint); }
.type-tab.on .type-count { color: var(--kb-primary); }

.table-card { padding: 14px 16px 16px; }
.table-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; }
.console-search { width: 300px; }
.console-select { width: 150px; }
.content-table :deep(th.el-table__cell) { font-size: 13px; }
.content-table :deep(.el-link) { font-size: 14px; }
/* 操作列 5 个文字按钮：默认 12px 间距 + 换行空白会把「删除」挤到第二行 */
.content-table :deep(td:last-child .cell) { display: flex; align-items: center; gap: 2px; }
.content-table :deep(td:last-child .el-button + .el-button) { margin-left: 0; }
.doc-no { color: var(--kb-text-muted); font-size: 13px; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
.cell-strong { color: var(--kb-text-2); font-size: 13px; line-height: 1.5; }
.cell-sub { color: var(--kb-text-muted); font-size: 12px; line-height: 1.5; }
.console-pagination { margin-top: 16px; justify-content: flex-end; }
.import-result {
  margin-top: 10px; padding: 10px 12px; border-radius: 4px;
  background: var(--kb-code-bg); border: 1px solid var(--kb-border); font-size: 13px;
}

@media (max-width: 1100px) {
  .stat-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 768px) {
  .console-head { align-items: flex-start; }
  .console-head-actions { width: 100%; margin-left: 0; }
  .console-head-actions > * { flex: 1; }
  .console-head-actions :deep(.el-button) { width: 100%; }
  .stat-row { gap: 10px; }
  .stat-card { padding: 12px 14px; }
  .stat-card span { font-size: 13px; }
  .stat-card b { font-size: 21px; }
  .system-grid { grid-template-columns: 1fr; }
  .system-counts { gap: 14px; }
  .table-toolbar .spacer { display: none; }
  .console-search { width: 100%; }
  .console-select { width: 100%; }
  .type-tabs { overflow-x: auto; scrollbar-width: none; }
  .type-tabs::-webkit-scrollbar { display: none; }
  .type-tab { flex: 0 0 auto; padding: 10px 12px; font-size: 13px; }
}
</style>
