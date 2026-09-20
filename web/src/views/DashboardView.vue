<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { currentDisplayName } from '@/api/auth'
import { contractApi, type ContractItem, type PendingEvidence, type TaskItem } from '@/api/contracts'
import { financeApi, type ReceivableItem, type RiskItem } from '@/api/finance'

const router = useRouter()
const contracts = ref<ContractItem[]>([])
const tasks = ref<TaskItem[]>([])
const reviews = ref<PendingEvidence[]>([])
const receivables = ref<ReceivableItem[]>([])
const risks = ref<RiskItem[]>([])
const financeAvailable = ref(true)
const loading = ref(true)
const error = ref('')
const activeTasks = computed(() => tasks.value.filter(item => !['COMPLETED', 'CANCELLED'].includes(item.taskStatus)).length)
const completedTasks = computed(() => tasks.value.filter(item => item.taskStatus === 'COMPLETED').length)
const receivableBalance = computed(() => receivables.value.reduce((sum, item) => sum + Number(item.balance), 0))
const overdueBalance = computed(() => receivables.value.reduce((sum, item) => sum + Number(item.overdueAmount), 0))
const activeRisks = computed(() => risks.value.filter(item => !['CLOSED'].includes(item.riskStatus)).length)
const today = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
const money = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', maximumFractionDigits: 0 })

onMounted(async () => {
  try {
    ;[contracts.value, tasks.value, reviews.value] = await Promise.all([contractApi.list(), contractApi.listTasks(), contractApi.pendingEvidence()])
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '工作台数据加载失败' }
  try {
    ;[receivables.value, risks.value] = await Promise.all([financeApi.listReceivables(), financeApi.listRisks()])
  } catch { financeAvailable.value = false }
  finally { loading.value = false }
})
</script>

<template>
  <AppShell title="企业工作台" :subtitle="`数据更新至 ${today}`">
    <template #actions><button class="top-search" type="button" @click="router.push('/contracts')">⌕&nbsp; 搜索合同或任务</button><span class="role-chip">当前工作空间</span></template>
    <p v-if="error" class="feedback error">{{ error }}</p>
    <section class="welcome-banner"><div><h2>你好，{{ currentDisplayName() }}</h2><p>从合同条款确认开始，跟进每一个履约节点和审核事项。</p></div><div class="welcome-actions"><button class="white-button" @click="router.push('/contracts')">上传合同</button><button class="ghost-button" @click="router.push('/tasks')">处理任务</button></div></section>
    <section class="metric-grid" :class="{ loading }">
      <article class="metric-card"><div class="metric-head"><span>合同总数</span><b class="metric-icon indigo">▤</b></div><strong>{{ loading ? '—' : contracts.length }}</strong><p>当前授权范围内</p><button class="card-link" @click="router.push('/contracts')">查看合同 →</button></article>
      <article class="metric-card"><div class="metric-head"><span>进行中任务</span><b class="metric-icon blue">✓</b></div><strong>{{ loading ? '—' : activeTasks }}</strong><p>含草稿、执行中和待审核</p><button class="card-link" @click="router.push('/tasks')">进入任务 →</button></article>
      <article class="metric-card"><div class="metric-head"><span>应收余额</span><b class="metric-icon amber">¥</b></div><strong>{{ loading ? '—' : financeAvailable ? money.format(receivableBalance) : '无权限' }}</strong><p>{{ financeAvailable ? `其中逾期 ${money.format(overdueBalance)}` : '仅财务和企业管理员可见' }}</p><button class="card-link" :disabled="!financeAvailable" @click="router.push('/receivables')">查看回款 →</button></article>
      <article class="metric-card"><div class="metric-head"><span>未结案风险</span><b class="metric-icon red">!</b></div><strong>{{ loading ? '—' : financeAvailable ? activeRisks : '无权限' }}</strong><p>{{ financeAvailable ? '来自真实逾期规则扫描' : `待我审核 ${reviews.length} 项` }}</p><button class="card-link" :disabled="!financeAvailable" @click="router.push('/risks')">进入风险中心 →</button></article>
    </section>
    <section class="dashboard-columns">
      <article class="panel-card"><div class="panel-heading"><div><h3>当前履约概况</h3><p>统计来自真实任务数据，不生成模拟风险评分</p></div><button class="text-button" @click="router.push('/tasks')">查看全部</button></div><div class="health-summary"><div class="health-ring"><strong>{{ tasks.length ? Math.round(completedTasks / tasks.length * 100) : 0 }}%</strong><span>任务完成率</span></div><div class="health-bars"><p><span>已完成</span><strong>{{ completedTasks }}</strong></p><div><i :style="{ width: `${tasks.length ? completedTasks / tasks.length * 100 : 0}%` }"></i></div><p><span>未完成</span><strong>{{ activeTasks }}</strong></p><div><i class="amber-bar" :style="{ width: `${tasks.length ? activeTasks / tasks.length * 100 : 0}%` }"></i></div></div></div></article>
      <article class="panel-card upcoming-card"><div class="panel-heading"><div><h3>回款与风险摘要</h3><p>开票、收款、到期按独立状态统计</p></div><span class="status-badge" :class="financeAvailable && activeRisks ? 'overdue' : 'settled'">{{ financeAvailable ? (activeRisks ? '需要处置' : '暂无风险') : '无财务权限' }}</span></div><div class="milestone-note"><span>¥</span><div><strong>{{ financeAvailable ? `应收余额 ${money.format(receivableBalance)}` : '财务数据已按角色隔离' }}</strong><p v-if="financeAvailable">逾期余额 {{ money.format(overdueBalance) }}。登记回款后系统会重算余额，并推动对应风险进入待结案。</p><p v-else>当前账号仍可处理合同、履约任务和被指定的审核事项。</p></div></div></article>
    </section>
  </AppShell>
</template>
