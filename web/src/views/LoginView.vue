<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'

const router = useRouter()
const username = ref(localStorage.getItem('contract-remembered-account') ?? 'admin')
const password = ref('ChangeMe123!')
const remember = ref(true)
const submitting = ref(false)
const error = ref('')

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    await authApi.login(username.value, password.value)
    if (remember.value) localStorage.setItem('contract-remembered-account', username.value)
    else localStorage.removeItem('contract-remembered-account')
    await router.push('/workspaces')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-layout login-page">
    <section class="brand-panel">
      <div class="brand-lockup"><span class="brand-mark light">契</span><span><strong>契约智控</strong><small>Contract Guard</small></span></div>
      <div class="brand-message">
        <h1>合同履约与回款<br />全流程可控</h1>
        <p>从条款确认到收款跟进，让每个节点都有责任人、有依据、有记录。</p>
        <ul class="value-list">
          <li><span>拆</span>条款拆解为可执行的履约节点</li>
          <li><span>款</span>开票、收款、到期独立跟踪</li>
          <li><span>险</span>规则驱动的风险早发现</li>
          <li><span>溯</span>合同履约全链路可追溯</li>
        </ul>
      </div>
      <p class="demo-note">当前为脱敏模拟数据环境 · AI 仅提供辅助分析，不构成法律意见</p>
    </section>
    <section class="login-panel">
      <div class="login-card">
        <h2>登录企业工作空间</h2>
        <p class="muted">使用企业分配的账号登录契约智控</p>
        <form @submit.prevent="submit">
          <label>账号<span class="input-wrap"><span>人</span><input v-model.trim="username" autocomplete="username" required /></span></label>
          <label>密码<span class="input-wrap"><span>锁</span><input v-model="password" type="password" autocomplete="current-password" required /></span></label>
          <div class="form-options"><label class="check-label"><input v-model="remember" type="checkbox" />记住账号</label><span>本地演示环境</span></div>
          <p v-if="error" class="feedback error">{{ error }}</p>
          <button class="primary-button login-button" :disabled="submitting">{{ submitting ? '登录中…' : '登录' }}</button>
        </form>
        <div class="demo-account"><strong>演示账号</strong><span>执行人：admin</span><span>审核人：reviewer</span></div>
        <p class="login-footnote">企业账号由管理员统一开通，暂不支持自主注册</p>
      </div>
    </section>
  </main>
</template>
