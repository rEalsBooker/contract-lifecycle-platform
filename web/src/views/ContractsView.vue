<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { contractApi, type ContractItem } from '@/api/contracts'

const router = useRouter()
const contracts = ref<ContractItem[]>([])
const loading = ref(true)
const error = ref('')
const search = ref('')
const status = ref('ALL')
const statusOptions = computed(() => [
  { value: 'ALL', label: '全部', count: contracts.value.length },
  { value: 'DRAFT', label: '草稿', count: contracts.value.filter(item => item.businessStatus === 'DRAFT').length },
  { value: 'TERMS_CONFIRMED', label: '条款已确认', count: contracts.value.filter(item => item.businessStatus === 'TERMS_CONFIRMED').length }
])
const visibleContracts = computed(() => { const keyword = search.value.trim().toLowerCase(); return contracts.value.filter(item => (status.value === 'ALL' || item.businessStatus === status.value) && (!keyword || [item.contractNo, item.name, item.counterpartyName].some(value => value.toLowerCase().includes(keyword)))) })

async function load() { loading.value = true; error.value = ''; try { contracts.value = await contractApi.list() } catch (cause) { error.value = cause instanceof Error ? cause.message : '无法读取合同列表' } finally { loading.value = false } }
function resetFilters() { search.value = ''; status.value = 'ALL' }
const currency = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', maximumFractionDigits: 0 })
const statusLabel: Record<string, string> = { DRAFT: '草稿', TERMS_CONFIRMED: '条款已确认', ACTIVE: '履约中', COMPLETED: '已完成', TERMINATED: '已终止' }
onMounted(load)
</script>

<template>
  <AppShell title="合同管理" :subtitle="`共 ${contracts.length} 份合同（当前授权范围内）`">
    <template #actions><label class="top-search"><span>⌕</span><input v-model="search" placeholder="搜索合同名称、编号或相对方" /></label><button class="primary-button compact" @click="router.push('/contracts/upload')">＋ 上传合同</button></template>
    <p v-if="error" class="feedback error">{{ error }}</p>
    <section class="filter-card"><div class="status-tabs"><button v-for="item in statusOptions" :key="item.value" :class="{ active: status === item.value }" @click="status = item.value">{{ item.label }} <span>{{ item.count }}</span></button></div><div class="filter-row"><span>归档：全部</span><span>相对方：全部</span><span>负责人：全部</span><span>风险等级：待接入</span><button class="text-button" @click="resetFilters">↻ 重置筛选</button></div><p class="scope-note">导出、批量审核与列设置暂未纳入首版闭环</p></section>
    <section class="contract-table-card">
      <div v-if="loading" class="loading-state">正在读取合同…</div>
      <div v-else-if="contracts.length === 0" class="empty-state large-empty"><span>▤</span><h3>还没有合同</h3><p>上传脱敏合同文件，先确认基本信息，再进入人工条款审阅。</p><button class="primary-button" @click="router.push('/contracts/upload')">上传第一份合同</button></div>
      <div v-else-if="visibleContracts.length === 0" class="empty-state large-empty"><span>⌕</span><h3>没有符合条件的合同</h3><p>当前关键词或业务状态未匹配到结果。</p><button class="secondary-button" @click="resetFilters">重置筛选</button></div>
      <div v-else class="table-scroll"><table class="contract-table"><thead><tr><th>合同名称</th><th>相对方</th><th>合同总额</th><th>业务状态</th><th>解析 / 审阅</th><th>归档</th><th>操作</th></tr></thead><tbody><tr v-for="item in visibleContracts" :key="item.id"><td><button class="contract-name" @click="router.push(`/contracts/${item.id}`)"><strong>{{ item.name }}</strong><small>编号 {{ item.contractNo }} · {{ item.sourceFilename ?? '未上传原件' }}</small></button></td><td>{{ item.counterpartyName }}</td><td class="amount">{{ currency.format(item.totalAmount) }}</td><td><span class="status-badge" :class="item.businessStatus.toLowerCase()">{{ statusLabel[item.businessStatus] ?? item.businessStatus }}</span></td><td><span class="review-state">{{ item.businessStatus === 'DRAFT' ? '待人工确认' : '审阅完成' }}</span></td><td>{{ item.archiveStatus === 'ARCHIVED' ? '已归档' : '未归档' }}</td><td><div class="row-actions"><button class="text-button" @click="router.push(`/contracts/${item.id}`)">{{ item.businessStatus === 'DRAFT' ? '继续审阅' : '详情' }}</button><button class="text-button subtle" @click="contractApi.downloadSource(item)">下载</button></div></td></tr></tbody></table></div>
      <footer v-if="visibleContracts.length" class="table-footer">显示 1—{{ visibleContracts.length }} 条，共 {{ visibleContracts.length }} 条</footer>
    </section>
  </AppShell>
</template>
