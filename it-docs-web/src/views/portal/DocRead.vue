<template>
  <div>
    <div class="page" v-loading="loading">
      <!-- 面包屑 + 操作 -->
      <div class="read-toolbar">
        <div class="crumb">
          <a @click="$router.push('/')">知识库</a><span class="crumb-sep">/</span>
          <span>{{ doc.docNo || '文档详情' }}</span>
        </div>
        <span class="grow"></span>
        <el-button size="small" @click="copyLink">复制链接</el-button>
        <el-button size="small" @click="doPrint">打印</el-button>
        <el-button v-if="user.logged && canEdit" size="small" type="primary"
                    @click="$router.push('/console/edit/' + doc.id)">编辑</el-button>
      </div>

      <!-- 下架 / 不存在：给个明确说法，不要空白 -->
      <div v-if="loadError" class="kb-card" style="padding:56px 0">
        <el-empty :description="loadError" :image-size="90">
          <el-button size="small" @click="$router.push('/')">返回知识库</el-button>
        </el-empty>
      </div>

      <div class="read-layout" v-else>
        <!-- 正文 -->
        <main class="kb-card read-main">
          <h1 class="doc-title">{{ doc.title }}</h1>
          <div class="doc-sub">
            <span>{{ docTypeLabel(doc.docType) }}</span>
            <span v-if="doc.docNo">{{ doc.docNo }}</span>
            <span v-if="doc.system">{{ doc.system }}</span>
            <span v-if="doc.category">{{ doc.category }}</span>
            <span v-if="doc.updatedBy">更新人 {{ doc.updatedBy }}</span>
            <span>{{ (doc.updatedAt || '').substring(0, 10) }}</span>
          </div>

          <div class="chips" v-if="tagList.length">
            <span v-for="t in tagList" :key="t" class="chip" :style="tagChipStyle(t)"># {{ t }}</span>
          </div>

          <div v-if="doc.summary" class="answer-callout">
            <span class="answer-label">{{ doc.docType === 'qa' ? '一句话答案' : '摘要' }}</span>
            <p>{{ doc.summary }}</p>
          </div>

          <el-divider />

          <!-- qa 问答卡：结构化区 -->
          <template v-if="doc.docType === 'qa' && qa">
            <section v-if="qa.errorText" class="qa-sec">
              <div class="sec-head">
                <b>报错原文</b>
                <el-button size="small" text @click="copyText(qa.errorText, '已复制')">复制</el-button>
              </div>
              <pre>{{ qa.errorText }}</pre>
            </section>
            <section v-if="qa.cause" class="qa-sec">
              <div class="sec-head"><b>原因</b></div>
              <div class="sec-body">{{ qa.cause }}</div>
            </section>
            <section v-if="qa.steps && qa.steps.length" class="qa-sec">
              <div class="sec-head"><b>处理步骤</b></div>
              <ol class="step-list">
                <li v-for="(s, i) in qa.steps" :key="i">
                  <span class="step-n">{{ i + 1 }}</span><span>{{ s }}</span>
                </li>
              </ol>
            </section>
            <section v-if="qa.tips && qa.tips.length" class="qa-sec warn">
              <div class="sec-head"><b>备注</b></div>
              <ul class="tip-list"><li v-for="(t, i) in qa.tips" :key="i">{{ t }}</li></ul>
            </section>
            <section v-if="qa.images && qa.images.length" class="qa-sec">
              <div class="sec-head"><b>截图</b></div>
              <div class="screenshots">
                <figure v-for="(img, i) in qa.images" :key="i" class="shot">
                  <el-image :src="img.src" :preview-src-list="qa.images.map(x => x.src)"
                            :initial-index="i" fit="contain" preview-teleported>
                    <template #error><div class="image-error">图片无法加载</div></template>
                  </el-image>
                  <figcaption v-if="img.caption" class="shot-cap">{{ img.caption }}</figcaption>
                </figure>
              </div>
            </section>
          </template>

          <!-- Markdown 正文 -->
          <MdPreview :modelValue="doc.contentMd || ''" class="doc-body" :editorId="previewId"
                     :theme="isDark ? 'dark' : 'light'" />
        </main>

        <!-- 侧栏：只留目录，正文区不被信息卡挤压 -->
        <aside class="read-side">
          <div v-if="hasToc" class="kb-card side-card toc-card">
            <h4>目录</h4>
            <MdCatalog :editorId="previewId" :scrollElement="scrollElement"
                       :scrollElementOffsetTop="80" :theme="isDark ? 'dark' : 'light'" />
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { MdPreview, MdCatalog } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { isDark } from '../../utils/theme'
import api from '../../api'
import { docTypeLabel } from '../../constants/labels'
import { useUserStore } from '../../stores/user'
import { tagColor } from '../../api/meta'
import { copyText, docLink } from '../../utils/clipboard'

const route = useRoute()
const user = useUserStore()
const doc = ref({})
const loading = ref(false)
const loadError = ref('')

// 目录锚点：编辑器 id 必须与 MdCatalog 一致；页面滚动容器是 documentElement
const previewId = 'doc-preview-' + Math.random().toString(36).slice(2, 9)
const scrollElement = typeof document !== 'undefined' ? document.documentElement : undefined
const hasToc = computed(() => doc.value.docType !== 'qa' && /^#{1,6}\s/m.test(doc.value.contentMd || ''))

const tagList = computed(() => (doc.value.tags || '').split(/[,，]/).map(s => s.trim()).filter(Boolean))

// 标签不入字典：颜色按名称哈希稳定生成
function tagChipStyle(name) {
  const c = tagColor(name)
  return { background: c, color: '#fff', borderColor: c }
}

const qa = computed(() => {
  try { return doc.value.contentJson ? JSON.parse(doc.value.contentJson) : null } catch (e) { return null }
})

// 编辑入口只做体验层显隐：无权限时后端也会拒绝
const canEdit = computed(() => ['ADMIN', 'EDITOR'].includes(user.role))

function copyLink() {
  copyText(docLink(route.params.id), '链接已复制，可直接发给同事')
}

function doPrint() {
  window.print()
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await api.get('/docs/' + route.params.id)
    doc.value = res.data
  } catch (e) {
    // 已下架 / 已删除 / 无权限：后端给的是 404 带原因
    loadError.value = (e && e.response && e.response.data && e.response.data.msg) || '内容不存在或已下架'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.read-toolbar { display: flex; align-items: center; gap: 8px; max-width: 1200px; margin: 0 auto 12px; flex-wrap: wrap; }
.read-toolbar .grow { flex: 1; }
.crumb { display: flex; gap: 8px; align-items: center; color: var(--kb-text-muted); font-size: 13px; }
.crumb a { cursor: pointer; color: var(--kb-primary); }

/* 正文控制在约 900px 内，行长 40~50 字，长文阅读不扫读串行 */
.read-layout { display: grid; grid-template-columns: minmax(0, 1fr) 260px; gap: 14px; align-items: start; max-width: 1200px; margin: 0 auto; }
.read-main { padding: 24px 28px; min-width: 0; }
.read-side { min-width: 0; }

.doc-title { margin: 0 0 10px; font-size: 28px; line-height: 1.4; font-weight: 700; color: var(--kb-text); }
.read-main :deep(.md-editor-preview) { font-size: 16px; line-height: 1.9; }
.read-main :deep(.md-editor-preview p), .read-main :deep(.md-editor-preview li) { font-size: 16px; line-height: 1.9; }
.read-main :deep(.md-editor-preview h1), .read-main :deep(.md-editor-preview h2), .read-main :deep(.md-editor-preview h3) { line-height: 1.5; }
.doc-sub { display: flex; align-items: center; flex-wrap: wrap; color: var(--kb-text-muted); font-size: 13px; }
.doc-sub span + span::before { content: '·'; margin: 0 8px; color: var(--kb-text-faint); }

.chips { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 12px; }
.chip { font-size: 11px; padding: 2px 10px; border-radius: 10px; background: var(--kb-primary-soft); color: var(--kb-primary); border: 1px solid transparent; }

.answer-callout {
  margin-top: 16px; padding: 12px 15px;
  border: 1px solid var(--kb-border-hover); border-left: 4px solid var(--kb-primary);
  border-radius: 0 8px 8px 0; background: var(--kb-primary-soft);
}
.answer-label {
  display: inline-block; margin-bottom: 5px;
  color: var(--kb-primary); font-size: 12px; font-weight: 750; letter-spacing: .04em;
}
.answer-callout p { margin: 0; color: var(--kb-text); font-size: 17px; line-height: 1.75; font-weight: 600; }

.qa-sec {
  border-left: 3px solid var(--kb-primary); background: var(--kb-primary-tint);
  border-radius: 0 8px 8px 0; padding: 12px 14px; margin-bottom: 12px;
}
.qa-sec.accent { border-left-color: var(--kb-green); background: var(--kb-green-tint); }
.qa-sec.warn { border-left-color: var(--kb-orange); background: var(--kb-orange-tint); }
.sec-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.sec-head b { color: var(--kb-text); font-size: 15px; }
.qa-sec pre {
  background: var(--kb-surface); border: 1px solid var(--kb-border); padding: 12px; border-radius: 8px;
  overflow: auto; font-size: 14px; line-height: 1.8; white-space: pre-wrap; margin: 0;
}
.sec-body { font-size: 16px; line-height: 1.85; }
.step-list, .tip-list { margin: 4px 0 0; padding: 0; list-style: none; }
.step-list li { display: flex; gap: 10px; align-items: flex-start; padding: 7px 0; font-size: 16px; line-height: 1.8; }
.step-n {
  flex-shrink: 0; width: 20px; height: 20px; border-radius: 50%; background: var(--kb-primary);
  color: var(--kb-on-primary); font-size: 11px; display: inline-flex; align-items: center; justify-content: center;
}
.tip-list li { padding: 4px 0 4px 16px; position: relative; font-size: 15px; line-height: 1.75; }
.tip-list li::before { content: "•"; position: absolute; left: 2px; color: var(--kb-orange); }
.screenshots { display: flex; flex-wrap: wrap; gap: 10px; }
.shot { margin: 0; }
.shot :deep(.el-image) {
  width: min(260px, 100%); height: 160px;
  border: 1px solid var(--kb-border); border-radius: 7px; background: var(--kb-surface-2);
}
.image-error { display: grid; place-items: center; width: 100%; height: 100%; color: var(--kb-text-muted); font-size: 12px; }
.shot-cap { color: var(--kb-text-muted); font-size: 12px; margin-top: 4px; }

.side-card { padding: 14px 16px; }
.side-card h4 { margin: 0 0 10px; font-size: 14px; color: var(--kb-text); }
.toc-card { position: sticky; top: 84px; }
.toc-card :deep(.md-editor-catalog) { max-height: calc(100vh - 220px); overflow-y: auto; font-size: 12px; }
.toc-card :deep(.md-editor-catalog-link) { padding: 3px 0; }

@media (max-width: 992px) {
  .read-layout { grid-template-columns: 1fr; }
  .read-side { display: none; }
  .read-main { padding: 18px; }
  .doc-title { font-size: 22px; }
}

@media print {
  .read-toolbar, .read-side { display: none !important; }
  .read-layout { display: block; }
  .kb-card { border: none; box-shadow: none; }
}
</style>
