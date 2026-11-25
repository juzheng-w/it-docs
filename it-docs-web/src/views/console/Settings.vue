<template>
  <div>
    <div class="page" style="max-width:720px">
      <h1 class="page-title">系统设置</h1>
      <div class="kb-card">
        <h3 style="margin:0 0 4px">LDAP 域认证</h3>
        <p style="margin:0 0 16px;color:var(--kb-text-muted);font-size:13px">
          保存后立即生效，无需重启。配置存数据库（it_config），环境变量仅作未配置时的兜底。
          <el-tag v-if="form.dbConfigured" size="small" type="success">界面配置</el-tag>
          <el-tag v-else size="small" type="info">未配置 / 环境变量默认值</el-tag>
        </p>

        <el-form label-width="130px" v-loading="loading">
          <el-form-item label="LDAP 地址" required>
            <el-input v-model="form.url" placeholder="ldap://ad.corp.com:389" />
          </el-form-item>
          <el-form-item label="Base DN" required>
            <el-input v-model="form.baseDn" placeholder="DC=corp,DC=com" />
          </el-form-item>
          <el-form-item label="用户 DN 模板">
            <el-input v-model="form.userDnPattern" placeholder="AD 留空；OpenLDAP：uid={0},ou=people,DC=corp,DC=com" />
            <div class="hint">{0} 会被替换为登录用户名。OpenLDAP（树形 DN）才填这个；Active Directory 留空，用下面的 UPN 模板</div>
          </el-form-item>
          <el-form-item label="UPN 模板">
            <el-input v-model="form.upnPattern" placeholder="{0}@corp.com" />
            <div class="hint">{0} 会被替换为登录用户名。Active Directory 填这个，如 {0}@tyeeli.com；两种模板登录时会依次尝试</div>
          </el-form-item>

          <el-divider content-position="left">查询账号（「从 AD 导入」功能用，登录不需要）</el-divider>
          <el-form-item label="查询账号">
            <el-input v-model="form.bindUser" placeholder="有域查询权限的账号，如 admin@tyeeli.com" />
            <div class="hint">成员管理「从 AD 导入」搜索域用户时用它 bind 查询；不配则只能手动新建域账号</div>
          </el-form-item>
          <el-form-item label="查询密码">
            <el-input v-model="form.bindPwd" type="password" show-password
                      :placeholder="form.bindPwdSet ? '已设置（留空 = 保持不变）' : '未设置'" />
          </el-form-item>

          <el-divider content-position="left">测试连接（可选，建议保存前先测）</el-divider>
          <el-form-item label="测试账号">
            <el-input v-model="form.testUser" placeholder="填一个真实域账号（可选）" style="width:46%" />
            <el-input v-model="form.testPwd" type="password" show-password placeholder="密码（可选）" style="width:46%;margin-left:8%" />
            <div class="hint">不填只测服务器连通性；填了会做真实登录验证（不落库）</div>
          </el-form-item>

          <el-form-item>
            <el-button @click="test" :loading="testing">测试连接</el-button>
            <el-button type="primary" @click="save" :loading="saving">保存</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="kb-card" style="font-size:13px;color:var(--kb-text-muted);line-height:1.9">
        <b>说明</b><br/>
        · 未配置时登录只走本地账号；配置后域用户首次登录自动建档（默认只读角色）<br/>
        · LDAP 服务器故障时本地管理员账号仍可登录（应急通道）<br/>
        · 每次修改都会记入操作日志（改前/改后值）
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const testing = ref(false)
const saving = ref(false)
const form = reactive({
  url: '', baseDn: '', userDnPattern: '', upnPattern: '',
  bindUser: '', bindPwd: '', bindPwdSet: false,
  testUser: '', testPwd: '',
  dbConfigured: false
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await api.get('/settings/ldap')
    Object.assign(form, res.data)
  } finally {
    loading.value = false
  }
})

async function test() {
  testing.value = true
  try {
    const res = await api.post('/settings/ldap/test', {
      url: form.url, baseDn: form.baseDn,
      userDnPattern: form.userDnPattern, upnPattern: form.upnPattern,
      testUser: form.testUser, testPwd: form.testPwd
    })
    ElMessage.success(res.data.message)
  } catch (e) {
    // 统一拦截器已提示错误信息
  } finally {
    testing.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await api.put('/settings/ldap', {
      url: form.url, baseDn: form.baseDn,
      userDnPattern: form.userDnPattern, upnPattern: form.upnPattern,
      bindUser: form.bindUser, bindPassword: form.bindPwd || undefined
    })
    ElMessage.success('已保存并生效')
    form.dbConfigured = true
    form.bindPwdSet = form.bindPwdSet || !!form.bindPwd
    form.bindPwd = ''
    form.testUser = ''
    form.testPwd = ''
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.hint { font-size: 12px; color: var(--kb-text-muted); line-height: 1.6; margin-top: 2px }
</style>
