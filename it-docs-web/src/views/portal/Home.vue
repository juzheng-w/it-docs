<template>
  <div class="home">
    <section class="hero">
      <div class="hero-inner">
        <h1>IT部知识共享服务平台</h1>
        <p class="hero-sub">报错原文、原因与处理步骤，按业务系统沉淀，搜到就能照着做。</p>
        <div class="hero-stats">
          <div v-for="s in heroStats" :key="s.label" class="hero-stat">
            <span>{{ s.label }}</span>
            <b>{{ s.value }}</b>
          </div>
        </div>
      </div>
    </section>

    <div class="home-body">
      <div v-if="drawer" class="facet-backdrop" @click="drawer = false"></div>

      <!-- 手机端：两级筛选做成横向 chip，一键切换，不必每次都开抽屉 -->
      <div class="mobile-chips">
        <div class="chip-row">
          <button type="button" class="chip" :class="{ on: !sysFilter }" @click="selectSystem(null)">全部</button>
          <button v-for="m in sysFacets" :key="'s' + m.id" type="button"
                  class="chip" :class="{ on: sysFilter === m.name }" @click="selectSystem(m.name)">
            {{ m.name }}<span class="chip-n">{{ m.count }}</span>
          </button>
        </div>
        <div class="chip-row">
          <button type="button" class="chip cat" :class="{ on: !catFilter }" @click="selectCategory(null)">分类：全部</button>
          <button v-for="m in catFacets" :key="'c' + m.id" type="button"
                  class="chip cat" :class="{ on: catFilter === m.name }" @click="selectCategory(m.name)">
            {{ m.name }}
          </button>
        </div>
      </div>

      <aside class="facet-panel" :class="{ open: drawer }">
        <div class="facet-head">
          <strong>筛选</strong>
          <button type="button" @click="drawer = false">关闭</button>
        </div>

        <div class="facet-block">
          <h3>① 业务系统</h3>
          <button type="button" class="facet-item" :class="{ on: !sysFilter }" @click="selectSystem(null)">
            <span class="facet-name">全部系统</span>
            <span class="facet-count">{{ currentTotal }}</span>
          </button>
          <button v-for="m in sysFacets" :key="m.id" type="button"
                  class="facet-item" :class="{ on: sysFilter === m.name }"
                  @click="selectSystem(m.name)">
            <span class="facet-name">{{ m.name }}</span>
            <span class="facet-count">{{ m.count }}</span>
          </button>
        </div>

        <div class="facet-block">
          <h3>② 问题分类</h3>
          <button type="button" class="facet-item" :class="{ on: !catFilter }" @click="selectCategory(null)">
            <span class="facet-name">全部</span>
            <span class="facet-count">{{ currentTotal }}</span>
          </button>
          <button v-for="m in catFacets" :key="m.id" type="button"
                  class="facet-item" :class="{ on: catFilter === m.name }"
                  @click="selectCategory(m.name)">
            <span class="facet-name">{{ m.name }}</span>
            <span class="facet-count">{{ m.count }}</span>
          </button>
        </div>

        <button v-if="hasFilter" type="button" class="facet-reset" @click="resetFilters">重置筛选</button>
      </aside>

      <main class="home-main">
        <div class="result-head">
          <div class="result-crumb">
            <template v-if="keyword">
              <span class="crumb-key">搜索「{{ keyword }}」</span>
            </template>
            <template v-else>
              <span class="crumb-label">系统</span><b>{{ sysFilter || '全部' }}</b>
              <span class="crumb-sep">›</span>
              <span class="crumb-label">分类</span><b>{{ catFilter || '全部' }}</b>
            </template>
            <span class="result-total">共 {{ total }} 条</span>
          </div>
          <button class="facet-toggle" type="button" @click="drawer = true">
            <el-icon><Filter /></el-icon>筛选
            <span v-if="activeFilterCount" class="facet-badge">{{ activeFilterCount }}</span>
          </button>
        </div>

        <div v-loading="loading" class="doc-list">
          <article v-for="d in list" :key="d.id" class="doc-card" @click="open(d)">
            <div class="doc-head">
              <span v-if="d.system" class="pill system">{{ d.system }}</span>
              <span v-if="d.category" class="pill">{{ d.category }}</span>
              <h3>{{ d.title }}</h3>
            </div>
            <p v-if="d.summary" class="doc-one-line"><em>一句话：</em>{{ d.summary }}</p>
            <div class="doc-foot">
              <span>{{ d.docNo || 'DOC-' + d.id }}</span>
              <span v-if="d.owner">处理人 {{ d.owner }}</span>
              <span>{{ (d.updatedAt || '').substring(0, 10) }}</span>
              <button class="copy-link" type="button" title="复制链接，可直接发到群里"
                      @click.stop="copyText(docLink(d.id), '链接已复制，可直接发到群里')">
                <el-icon><Link /></el-icon>复制链接
              </button>
            </div>
          </article>

          <el-empty v-if="!loading && loadError" description="服务暂时不可用，请稍后重试">
            <el-button type="primary" plain @click="load(page)">重试</el-button>
          </el-empty>
          <el-empty v-else-if="!loading && list.length === 0" description="没有找到匹配内容，试试其他关键词或清空筛选">
            <el-button v-if="hasFilter" size="small" @click="resetFilters">清空筛选</el-button>
          </el-empty>
        </div>

        <el-pagination v-if="total > 20" layout="prev, pager, next, total" :total="total"
                       :page-size="20" :current-page="page" class="result-pagination"
                       @current-change="load" />
      </main>

      <aside class="home-rail">
        <section v-if="showRecent" class="rail-card">
          <h3>最近更新</h3>
          <ul v-if="recent.length" class="rail-list">
            <li v-for="d in recent" :key="d.id" @click="open(d)">
              <span class="rail-title">{{ d.title }}</span>
              <span class="rail-meta">
                {{ d.system || '未归属系统' }} · {{ (d.updatedAt || '').substring(5, 10) }}
              </span>
            </li>
          </ul>
          <p v-else class="rail-empty">还没有更新记录</p>
        </section>

        <section class="rail-card">
          <template v-if="user.logged">
            <h3>快速开始</h3>
            <button type="button" class="rail-action primary" @click="router.push('/personal/edit')">写一条问题记录</button>
            <button type="button" class="rail-action" @click="router.push('/personal')">我的个人区域</button>
            <button v-if="canWrite" type="button" class="rail-action" @click="router.push('/console')">进入工作台</button>
          </template>
          <!-- 未登录不再放第二个「登录」按钮，顶栏已有入口 -->
          <template v-else>
            <h3>如何贡献</h3>
            <p class="rail-tip">登录后可以把自己遇到的问题记下来，发布后沉淀进公共知识库。</p>
            <ol class="rail-steps">
              <li>写一条问题记录：报错原文 + 原因 + 处理步骤</li>
              <li>个人区域里保存草稿，随时继续</li>
              <li>管理员审核后发布，所有人可搜到</li>
            </ol>
          </template>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Filter, Link } from '@element-plus/icons-vue'
import api from '../../api'
import { fetchMeta } from '../../api/meta'
import { copyText, docLink } from '../../utils/clipboard'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

// 查阅端只公开问题记录；业务文档和技术文档登录后在「工作台」查看
const docType = 'qa'

const keyword = ref(route.query.keyword || '')
const sysFilter = ref(null)
const catFilter = ref(null)
const list = ref([])
const recent = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const loadError = ref(false)
const drawer = ref(false)

const dict = reactive({ BIZ_SYSTEM: [], CATEGORY: [] })
const counts = reactive({ qa: 0 })
const sysCounts = reactive({})
const catCounts = reactive({})

const canWrite = computed(() => ['ADMIN', 'EDITOR'].includes(user.role))
const currentTotal = computed(() => counts.qa)
const hasFilter = computed(() => !!sysFilter.value || !!catFilter.value)
const activeFilterCount = computed(() => [sysFilter.value, catFilter.value].filter(Boolean).length)

// 列表本身就不超过一屏时，「最近更新」只是把同一批卡再排一遍
const showRecent = computed(() => hasFilter.value || !!keyword.value || total.value > 6)

const heroStats = computed(() => [
  { label: '问题记录', value: counts.qa },
  { label: '收录系统', value: dict.BIZ_SYSTEM.length }
])

// 分类随所选系统收窄：绑定系统的分类只在该系统下出现，未绑定的对全部系统通用
const catOptions = computed(() => {
  const sys = dict.BIZ_SYSTEM.find(m => m.name === sysFilter.value)
  return sys ? dict.CATEGORY.filter(c => !c.parentId || c.parentId === sys.id) : dict.CATEGORY
})

// 计数为 0 的项（还没收录内容的系统/分类）默认收起，面板才不会被 0 撑长
function withCounts(items, counter) {
  const rows = items.map(m => ({ ...m, count: counter[m.name] || 0 }))
  const any = rows.some(r => r.count > 0)
  return (any ? rows.filter(r => r.count > 0) : rows).sort((a, b) => b.count - a.count)
}
const sysFacets = computed(() => withCounts(dict.BIZ_SYSTEM, sysCounts))
const catFacets = computed(() => withCounts(catOptions.value, catCounts))

function open(d) {
  router.push('/doc/' + d.id)
}

function selectSystem(name) {
  sysFilter.value = name
  if (catFilter.value && !catOptions.value.some(c => c.name === catFilter.value)) catFilter.value = null
  drawer.value = false
  load(1)
}

function selectCategory(name) {
  catFilter.value = name
  drawer.value = false
  load(1)
}

function resetFilters() {
  sysFilter.value = null
  catFilter.value = null
  drawer.value = false
  load(1)
  loadFacetCounts()
}

async function countOf(params) {
  try {
    const res = await api.get('/docs', { params: { ...params, page: 1, size: 1 } })
    return res.data.total || 0
  } catch {
    return 0
  }
}

// 没有统计接口，用 size=1 的列表请求拿 total；字典不大，并行成本可接受
async function loadCounts() {
  counts.qa = await countOf({ docType })
}

// 左侧分面计数同样只统计问题记录
async function loadFacetCounts() {
  if (dict.BIZ_SYSTEM.length + dict.CATEGORY.length > 24) return
  await Promise.all([
    ...dict.BIZ_SYSTEM.map(async m => { sysCounts[m.name] = await countOf({ docType, system: m.name }) }),
    ...dict.CATEGORY.map(async m => { catCounts[m.name] = await countOf({ docType, category: m.name }) })
  ])
}

async function loadRecent() {
  try {
    const res = await api.get('/docs', { params: { docType, page: 1, size: 6 } })
    recent.value = res.data.list || []
  } catch {
    recent.value = []
  }
}

async function load(p = 1) {
  page.value = p
  loading.value = true
  loadError.value = false
  try {
    const res = await api.get('/docs', { params: {
      keyword: keyword.value || undefined,
      docType,
      system: sysFilter.value || undefined,
      category: catFilter.value || undefined,
      page: p, size: 20
    } })
    list.value = res.data.list || []
    total.value = res.data.total || 0
  } catch {
    list.value = []
    total.value = 0
    loadError.value = true
  } finally {
    loading.value = false
  }
}

watch(() => route.query.keyword, v => {
  keyword.value = v || ''
  load(1)
})

onMounted(async () => {
  await Promise.all([
    fetchMeta('BIZ_SYSTEM').then(l => { dict.BIZ_SYSTEM = l }).catch(() => {}),
    fetchMeta('CATEGORY').then(l => { dict.CATEGORY = l }).catch(() => {}),
    load(1),
    loadRecent()
  ])
  loadCounts()
  loadFacetCounts()
})
</script>

<style scoped>
.home { color: var(--kb-text); }

/* 蓝色头图：标题与关键数字居中，和设计稿一致 */
.hero { background: var(--kb-primary); color: #fff; }
.hero-inner { max-width: 1440px; margin: 0 auto; padding: 34px 24px 30px; text-align: center; }
.hero h1 { margin: 0; font-size: 26px; font-weight: 600; line-height: 1.4; letter-spacing: .02em; }
.hero-sub { margin: 10px 0 0; font-size: 14px; color: rgba(255, 255, 255, .84); }
.hero-stats { display: flex; justify-content: center; flex-wrap: wrap; gap: 72px; margin-top: 26px; }
.hero-stat { display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 68px; }
.hero-stat span { font-size: 13px; color: rgba(255, 255, 255, .84); }
.hero-stat b { font-size: 30px; font-weight: 600; line-height: 1.2; }

/* 主体：左筛选 + 中列表 + 右最近更新 */
.home-body {
  display: grid; grid-template-columns: 232px minmax(0, 1fr) 280px;
  gap: 20px; align-items: start;
  max-width: 1440px; margin: 0 auto; padding: 20px 24px 56px;
}
.facet-panel {
  position: sticky; top: 76px;
  padding: 4px 0 12px;
  background: var(--kb-surface); border: 1px solid var(--kb-border); border-radius: 4px;
}
.facet-head { display: none; }
.facet-block { padding: 12px 14px 6px; }
.facet-block + .facet-block { border-top: 1px solid var(--kb-border); }
.facet-block h3 {
  margin: 0 0 6px; font-size: 13px; font-weight: 600; color: var(--kb-text-2);
}
.facet-item {
  display: flex; align-items: center; gap: 8px; width: 100%;
  padding: 7px 8px; border: 0; border-radius: 2px;
  background: transparent; color: var(--kb-text-2);
  font: inherit; font-size: 13px; text-align: left; cursor: pointer;
}
.facet-item:hover { background: var(--kb-surface-2); color: var(--kb-primary); }
.facet-item.on { background: var(--kb-primary-soft); color: var(--kb-primary); font-weight: 600; }
.facet-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.facet-count { flex: 0 0 auto; color: var(--kb-text-faint); font-size: 12px; }
.facet-item.on .facet-count { color: var(--kb-primary); }
.facet-reset {
  display: block; width: calc(100% - 28px); margin: 12px 14px 0;
  padding: 6px 0; border: 1px solid var(--kb-border); border-radius: 4px;
  background: var(--kb-surface); color: var(--kb-text-2); font: inherit; font-size: 13px; cursor: pointer;
}
.facet-reset:hover { color: var(--kb-primary); border-color: var(--kb-primary); }
.facet-toggle, .facet-backdrop, .mobile-chips { display: none; }

.home-main { min-width: 0; }
.result-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; padding: 0 2px; min-height: 22px; }
.result-crumb { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--kb-text-muted); }
.result-crumb b { color: var(--kb-text); font-weight: 600; }
.crumb-sep { color: var(--kb-text-faint); }
.result-total { margin-left: 8px; color: var(--kb-text-muted); }
.crumb-key { color: var(--kb-text-2); }

.doc-list { min-height: 240px; }
.doc-card {
  margin-bottom: 10px; padding: 14px 18px;
  background: var(--kb-surface); border: 1px solid var(--kb-border); border-radius: 4px;
  cursor: pointer; transition: border-color .15s, box-shadow .15s;
}
.doc-card:hover { border-color: var(--kb-primary); box-shadow: 0 2px 10px var(--kb-shadow); }
.doc-head { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.doc-head h3 { margin: 0; font-size: 15px; font-weight: 600; color: var(--kb-text); line-height: 1.5; }
.doc-card:hover .doc-head h3 { color: var(--kb-primary); }
.pill {
  flex: 0 0 auto; padding: 1px 7px; border-radius: 3px;
  background: var(--kb-surface-2); color: var(--kb-text-muted);
  font-size: 11px; line-height: 18px; white-space: nowrap;
}
.pill.system { background: var(--kb-primary-soft); color: var(--kb-primary); font-weight: 600; }
.doc-one-line {
  display: -webkit-box; overflow: hidden; margin: 8px 0 0;
  color: var(--kb-text-2); font-size: 13px; line-height: 1.65;
  -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.doc-one-line em { color: var(--kb-text-muted); font-style: normal; }
.doc-foot {
  display: flex; flex-wrap: wrap; gap: 16px;
  margin-top: 8px; color: var(--kb-text-muted); font-size: 12px;
}
.doc-foot span + span::before { content: '·'; margin-right: 16px; color: var(--kb-text-faint); }
.copy-link {
  display: inline-flex; align-items: center; gap: 4px;
  margin-left: auto; padding: 0;
  border: 0; background: none; cursor: pointer;
  color: var(--kb-text-faint); font: inherit; font-size: 12px;
  transition: color .15s;
}
.copy-link:hover { color: var(--kb-primary); }
.result-pagination { justify-content: center; margin-top: 18px; }

/* 右栏：最近更新 + 快速开始 */
.home-rail { position: sticky; top: 76px; display: flex; flex-direction: column; gap: 12px; }
.rail-card {
  padding: 14px 16px 16px;
  background: var(--kb-surface); border: 1px solid var(--kb-border); border-radius: 8px;
}
.rail-card h3 { margin: 0 0 10px; font-size: 14px; font-weight: 600; color: var(--kb-text); }
.rail-list { margin: 0; padding: 0; list-style: none; }
.rail-list li { padding: 8px 0; cursor: pointer; border-top: 1px solid var(--kb-border); }
.rail-list li:first-child { border-top: 0; }
.rail-list li:hover .rail-title { color: var(--kb-primary); }
.rail-title {
  display: -webkit-box; overflow: hidden; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  color: var(--kb-text-2); font-size: 13px; line-height: 1.55;
}
.rail-meta { display: block; margin-top: 3px; color: var(--kb-text-faint); font-size: 12px; }
.rail-empty { margin: 0; color: var(--kb-text-muted); font-size: 13px; }
.rail-tip { margin: 0 0 10px; color: var(--kb-text-muted); font-size: 12.5px; line-height: 1.65; }
.rail-steps { margin: 0; padding-left: 18px; color: var(--kb-text-muted); font-size: 12.5px; line-height: 1.6; }
.rail-steps li { margin-top: 6px; }
.rail-steps li::marker { color: var(--kb-text-faint); }
.rail-action {
  display: block; width: 100%; margin-top: 8px; padding: 8px 0;
  border: 1px solid var(--kb-border); border-radius: 4px;
  background: var(--kb-surface); color: var(--kb-text-2);
  font: inherit; font-size: 13px; cursor: pointer; transition: border-color .15s, color .15s;
}
.rail-action:hover { color: var(--kb-primary); border-color: var(--kb-primary); }
.rail-action.primary { background: var(--kb-primary); border-color: var(--kb-primary); color: #fff; }
.rail-action.primary:hover { background: #66b1ff; border-color: #66b1ff; color: #fff; }

/* 宽度不够时先收右栏，内容区优先 */
@media (max-width: 1280px) {
  .home-body { grid-template-columns: 232px minmax(0, 1fr); }
  .home-rail { display: none; }
}

@media (max-width: 900px) {
  .hero-inner { padding: 24px 16px 22px; }
  .hero h1 { font-size: 19px; padding: 0 8px; }
  .hero-sub { font-size: 12.5px; margin-top: 8px; }
  .hero-stats { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-top: 18px; }
  .hero-stat { min-width: 0; }
  .hero-stat b { font-size: 22px; }
  .hero-stat span { font-size: 11.5px; white-space: nowrap; }
  .home-body { display: block; padding: 14px 14px 40px; }
  .mobile-chips { display: block; margin-bottom: 10px; }
  .chip-row { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 6px; scrollbar-width: none; }
  .chip-row::-webkit-scrollbar { display: none; }
  .chip {
    flex: 0 0 auto; padding: 5px 12px; border: 1px solid var(--kb-border); border-radius: 14px;
    background: var(--kb-surface); color: var(--kb-text-2); font: inherit; font-size: 12.5px;
    white-space: nowrap; cursor: pointer;
  }
  .chip.on { background: var(--kb-primary); border-color: var(--kb-primary); color: #fff; }
  .chip.cat.on { background: var(--kb-primary-soft); border-color: var(--kb-primary); color: var(--kb-primary); }
  .chip-n { margin-left: 5px; opacity: .72; }
  .doc-foot { gap: 10px; }
  .doc-foot span + span::before { display: none; }
  .facet-toggle {
    display: inline-flex; align-items: center; gap: 6px;
    flex: 0 0 auto; padding: 6px 12px;
    border: 1px solid var(--kb-border); border-radius: 4px;
    background: var(--kb-surface); color: var(--kb-text-2); font: inherit; font-size: 13px; cursor: pointer;
  }
  .facet-badge {
    min-width: 16px; height: 16px; padding: 0 4px; border-radius: 8px;
    background: var(--kb-primary); color: #fff; font-size: 10px; line-height: 16px; text-align: center;
  }
  .facet-backdrop { display: block; position: fixed; inset: 0; z-index: 39; background: rgba(0, 0, 0, .4); }
  .facet-panel {
    position: fixed; z-index: 40; top: 0; right: 0; bottom: 0;
    width: min(320px, 88vw); overflow-y: auto;
    border: 0; border-radius: 0;
    transform: translateX(105%); transition: transform .2s ease;
  }
  .facet-panel.open { transform: none; }
  .facet-head {
    display: flex; align-items: center; justify-content: space-between;
    padding: 14px 16px; border-bottom: 1px solid var(--kb-border); font-size: 15px;
  }
  .facet-head button { border: 0; background: transparent; color: var(--kb-primary); font: inherit; font-size: 13px; cursor: pointer; }
  .doc-card { padding: 12px 14px; }
  .doc-head h3 { font-size: 14px; }
  .doc-one-line { font-size: 12.5px; }
}
</style>
