<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { authApi, type Workspace } from '@/api/auth'
import { contractApi, type MemberOption, type PendingEvidence, type TaskItem, type TaskExtension } from '@/api/contracts'

const workspace = ref<Workspace | null>(null)
const route = useRoute()
const contractFilter = computed(() => Number(route.query.contractId) || undefined)
const tasks = ref<TaskItem[]>([])
const pendingEvidence = ref<PendingEvidence[]>([])
const members = ref<MemberOption[]>([])
const extensions = ref<TaskExtension[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const message = ref('')
const search = ref('')
const status = ref('ALL')
const selectedTask = ref<TaskItem | null>(null)
const dueDates = reactive<Record<number, string>>({})
const reviewerIds = reactive<Record<number, number | undefined>>({})
const files = reactive<Record<number, File | null>>({})
const submitNotes = reactive<Record<number, string>>({})
const reviewNotes = reactive<Record<number, string>>({})
const extensionDates=reactive<Record<number,string>>({});const extensionReasons=reactive<Record<number,string>>({});const extensionApprovers=reactive<Record<number,number|undefined>>({});const extensionReviewNotes=reactive<Record<number,string>>({})
const reviewerOptions = computed(() => members.value.filter(item => item.membershipId !== workspace.value?.membershipId))
const statusTabs = computed(() => [{ value: 'ALL', label: '全部', count: tasks.value.length }, ...['DRAFT', 'PENDING', 'IN_PROGRESS', 'PENDING_REVIEW', 'COMPLETED'].map(value => ({ value, label: statusLabel[value], count: tasks.value.filter(item => item.taskStatus === value).length }))])
const visibleTasks = computed(() => { const keyword = search.value.trim().toLowerCase(); return tasks.value.filter(item => (status.value === 'ALL' || item.taskStatus === status.value) && (!keyword || [item.title, item.contractNo, item.contractName].some(value => value.toLowerCase().includes(keyword)))) })
const selectedEvidence = computed(() => selectedTask.value ? pendingEvidence.value.find(item => item.taskId === selectedTask.value?.id) ?? null : null)
const statusLabel: Record<string, string> = { DRAFT: '待领取', PENDING: '待开始', IN_PROGRESS: '进行中', PENDING_REVIEW: '待审核', COMPLETED: '已完成', CANCELLED: '已取消' }

async function load() {
  loading.value = true; error.value = ''
  try { const [currentWorkspace, taskList, reviewList, memberList,extensionList] = await Promise.all([authApi.currentWorkspace(), contractApi.listTasks(contractFilter.value), contractApi.pendingEvidence(), contractApi.activeMembers(),contractApi.extensions()]); workspace.value = currentWorkspace; tasks.value = taskList; pendingEvidence.value = reviewList; members.value = memberList;extensions.value=extensionList; const requestedTaskId=Number(route.query.taskId)||selectedTask.value?.id; selectedTask.value=requestedTaskId?taskList.find(item=>item.id===requestedTaskId)??null:null }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '履约任务加载失败' }
  finally { loading.value = false }
}
async function perform(action: () => Promise<unknown>, successMessage: string) { saving.value = true; error.value = ''; message.value = ''; try { await action(); message.value = successMessage; await load() } catch (cause) { error.value = cause instanceof Error ? cause.message : '操作失败' } finally { saving.value = false } }
function memberName(id: number | null) { return members.value.find(item => item.membershipId === id)?.displayName ?? '未分配' }
function selectFile(taskId: number, event: Event) { files[taskId] = (event.target as HTMLInputElement).files?.[0] ?? null }
function claim(task: TaskItem) { const dueDate = dueDates[task.id]; const reviewerId = reviewerIds[task.id]; if (!dueDate || !reviewerId) { error.value = '请选择内部计划日和有效审核人'; return } perform(() => contractApi.claimTask(task.id, dueDate, reviewerId), '任务已领取并指定独立审核人。') }
function submitEvidence(task: TaskItem) { const file = files[task.id]; if (!file) { error.value = '请选择履约凭证文件'; return } perform(() => contractApi.submitEvidence(task.id, file, submitNotes[task.id] ?? ''), '凭证已提交并冻结，等待指定审核人处理。') }
function review(item: PendingEvidence, approved: boolean) { const note = reviewNotes[item.id] ?? ''; if (!approved && !note.trim()) { error.value = '驳回必须填写原因'; return } perform(() => contractApi.reviewEvidence(item.id, approved, note), approved ? '审核通过，任务已完成。' : '凭证已驳回，任务退回进行中。') }
function openTask(task: TaskItem) { selectedTask.value = task; error.value = ''; message.value = '' }
function requestExtension(task:TaskItem){const date=extensionDates[task.id],reason=extensionReasons[task.id],approver=extensionApprovers[task.id];if(!date||!reason?.trim()||!approver){error.value='请填写新计划日、原因和审批人';return}perform(()=>contractApi.requestExtension(task.id,date,reason,approver),'延期申请已提交。')}
function reviewExtension(item:TaskExtension,approved:boolean){const note=extensionReviewNotes[item.id]??'';if(!approved&&!note.trim()){error.value='驳回必须填写原因';return}perform(()=>contractApi.reviewExtension(item.id,approved,note),approved?'延期已批准。':'延期已驳回。')}
function taskExtensions(id:number){return extensions.value.filter(e=>e.taskId===id)}
onMounted(load)
</script>

<template>
  <AppShell title="履约任务" subtitle="主状态、执行人和指定审核人分开管理">
    <template #actions><label class="top-search"><span>⌕</span><input v-model="search" placeholder="搜索任务或合同" /></label><button class="secondary-button compact" @click="load">↻ 刷新</button></template>
    <p v-if="error" class="feedback error">{{ error }}</p><p v-if="message" class="feedback success">{{ message }}</p>
    <section class="filter-card task-filter"><div class="status-tabs"><button v-for="item in statusTabs" :key="item.value" :class="{ active: status === item.value }" @click="status = item.value">{{ item.label }} <span>{{ item.count }}</span></button></div><div class="filter-row"><span>合同：全部</span><span>负责人：全部</span><span>时间标签：全部</span><p class="task-rule-note">提交人不能审核本人任务；待审核材料不可覆盖</p></div></section>
    <section class="contract-table-card">
      <div v-if="loading" class="loading-state">正在加载任务与审核待办…</div>
      <div v-else-if="tasks.length === 0" class="empty-state large-empty"><span>✓</span><h3>暂无履约任务</h3><p>请先在合同详情完成人工条款确认并生成任务草稿。</p></div>
      <div v-else-if="visibleTasks.length === 0" class="empty-state large-empty"><span>⌕</span><h3>没有符合条件的任务</h3><p>请更换状态或搜索关键词。</p><button class="secondary-button" @click="status = 'ALL'; search = ''">重置筛选</button></div>
      <div v-else class="table-scroll"><table class="contract-table task-table"><thead><tr><th>任务名称</th><th>关联合同</th><th>主状态</th><th>负责人</th><th>指定审核人</th><th>内部计划日</th><th>操作</th></tr></thead><tbody><tr v-for="task in visibleTasks" :key="task.id"><td><button class="contract-name" @click="openTask(task)"><strong>{{ task.title }}</strong><small>{{ task.taskDescription || '由人工确认条款生成' }}</small></button></td><td><strong>{{ task.contractNo }}</strong><small class="table-subline">{{ task.contractName }}</small></td><td><span class="status-badge" :class="task.taskStatus.toLowerCase()">{{ statusLabel[task.taskStatus] ?? task.taskStatus }}</span></td><td>{{ memberName(task.assigneeMembershipId) }}</td><td>{{ memberName(task.reviewerMembershipId) }}</td><td>{{ task.dueDate ?? '待设定' }}</td><td><button class="text-button" @click="openTask(task)">{{ task.taskStatus === 'PENDING_REVIEW' && task.reviewerMembershipId === workspace?.membershipId ? '审核' : task.taskStatus === 'IN_PROGRESS' ? '提交凭证' : task.taskStatus === 'DRAFT' ? '领取' : '查看' }}</button></td></tr></tbody></table></div>
      <footer v-if="visibleTasks.length" class="table-footer">待我审核 {{ pendingEvidence.length }} 项 · 共 {{ visibleTasks.length }} 条</footer>
    </section>

    <div v-if="selectedTask" class="drawer-backdrop" @click.self="selectedTask = null"><aside class="task-detail-drawer"><div class="drawer-heading"><div><h2>{{ selectedTask.title }}</h2><p>{{ selectedTask.contractNo }} {{ selectedTask.contractName }}</p></div><button class="icon-button" @click="selectedTask = null">×</button></div><div class="task-drawer-status"><span class="status-badge" :class="selectedTask.taskStatus.toLowerCase()">{{ statusLabel[selectedTask.taskStatus] }}</span><span>任务 #{{ selectedTask.id }}</span></div><dl class="task-meta"><div><dt>内部计划日</dt><dd>{{ selectedTask.dueDate ?? '待设定' }}</dd></div><div><dt>负责人</dt><dd>{{ memberName(selectedTask.assigneeMembershipId) }}</dd></div><div><dt>指定审核人</dt><dd>{{ memberName(selectedTask.reviewerMembershipId) }}</dd></div><div><dt>任务来源</dt><dd>{{ selectedTask.taskOrigin }}</dd></div></dl><section class="task-description"><h3>任务说明</h3><p>{{ selectedTask.taskDescription || '该任务由已人工确认的合同条款生成。' }}</p></section>
      <section v-if="selectedTask.taskStatus === 'DRAFT'" class="drawer-operation"><h3>领取并分配审核人</h3><p>审核人必须是同一企业的有效成员，且不能与执行人相同。</p><label>内部计划日 <em>*</em><input v-model="dueDates[selectedTask.id]" type="date" /></label><label>指定审核人 <em>*</em><select v-model="reviewerIds[selectedTask.id]"><option :value="undefined">请选择审核人</option><option v-for="member in reviewerOptions" :key="member.membershipId" :value="member.membershipId">{{ member.displayName }}</option></select></label><button class="primary-button full-button" :disabled="saving" @click="claim(selectedTask)">领取任务</button></section>
      <section v-else-if="selectedTask.taskStatus === 'PENDING' && selectedTask.assigneeMembershipId === workspace?.membershipId" class="drawer-operation"><h3>开始执行</h3><p>开始后任务进入进行中状态，可以上传履约凭证。</p><button class="primary-button full-button" :disabled="saving" @click="perform(() => contractApi.startTask(selectedTask!.id), '任务已开始执行。')">开始执行任务</button></section>
      <section v-else-if="selectedTask.taskStatus === 'IN_PROGRESS' && selectedTask.assigneeMembershipId === workspace?.membershipId" class="drawer-operation"><h3>提交履约凭证</h3><p>提交后本轮材料冻结，等待指定审核人处理；被驳回后可以重新提交。</p><label>凭证文件 <em>*</em><span class="file-drop"><input type="file" accept=".pdf,.docx,.png,.jpg,.jpeg" @change="selectFile(selectedTask!.id, $event)" /><b>{{ files[selectedTask.id]?.name ?? '选择 PDF、DOCX 或图片' }}</b><small>通过私有存储保存，仅授权成员可下载</small></span></label><label>提交说明<input v-model="submitNotes[selectedTask.id]" placeholder="说明交付或验收情况（选填）" /></label><button class="primary-button full-button" :disabled="saving" @click="submitEvidence(selectedTask)">提交审核</button></section>
      <section v-else-if="selectedTask.taskStatus === 'PENDING_REVIEW'" class="drawer-operation"><h3>本轮凭证（已冻结）</h3><template v-if="selectedEvidence"><div class="evidence-file"><span>文</span><div><strong>{{ selectedEvidence.originalFilename }}</strong><small>{{ selectedEvidence.submitterName }} 提交 · {{ selectedEvidence.submittedAt }}</small></div><button class="text-button" @click="contractApi.downloadEvidence(selectedEvidence)">受控下载</button></div><p class="submit-note">{{ selectedEvidence.submitNote || '提交人未填写说明' }}</p><template v-if="selectedTask.reviewerMembershipId === workspace?.membershipId"><label>审核意见<input v-model="reviewNotes[selectedEvidence.id]" placeholder="驳回时必须填写原因" /></label><div class="review-buttons"><button class="secondary-button danger" :disabled="saving" @click="review(selectedEvidence, false)">驳回并说明</button><button class="primary-button" :disabled="saving" @click="review(selectedEvidence, true)">审核通过</button></div></template><p v-else class="manual-mode-note">只有指定审核人可以处理本轮凭证。</p></template><p v-else class="manual-mode-note">凭证已提交；当前账号不是指定审核人，因此不能读取本轮材料。</p></section>
      <section v-else-if="selectedTask.taskStatus === 'COMPLETED'" class="drawer-operation completed-operation"><span>✓</span><h3>任务已完成</h3><p>指定审核人已确认本轮履约凭证。</p></section>
      <section v-else class="drawer-operation"><p>当前账号没有此任务的可执行操作。</p></section>
      <section v-if="selectedTask.assigneeMembershipId===workspace?.membershipId&&!['COMPLETED','CANCELLED'].includes(selectedTask.taskStatus)" class="drawer-operation"><h3>申请调整内部计划</h3><p>合同约定日保持不变，批准后只更新内部计划日。</p><label>新内部计划日<input v-model="extensionDates[selectedTask.id]" type="date"></label><label>延期原因<input v-model="extensionReasons[selectedTask.id]" placeholder="说明原因和影响"></label><label>审批人<select v-model="extensionApprovers[selectedTask.id]"><option :value="undefined">请选择</option><option v-for="m in reviewerOptions" :key="m.membershipId" :value="m.membershipId">{{m.displayName}}</option></select></label><button class="secondary-button full-button" @click="requestExtension(selectedTask)">提交延期申请</button></section>
      <section v-if="taskExtensions(selectedTask.id).length" class="drawer-operation"><h3>延期记录</h3><article v-for="e in taskExtensions(selectedTask.id)" :key="e.id" class="extension-item"><strong>{{e.originalInternalPlanDate}} → {{e.requestedInternalPlanDate}}</strong><span class="status-badge" :class="e.approvalStatus==='APPROVED'?'settled':'draft'">{{e.approvalStatus}}</span><p>{{e.reason}}</p><template v-if="e.approvalStatus==='PENDING'&&e.approverMembershipId===workspace?.membershipId"><input v-model="extensionReviewNotes[e.id]" placeholder="审批意见"><div class="review-buttons"><button class="secondary-button danger" @click="reviewExtension(e,false)">驳回</button><button class="primary-button" @click="reviewExtension(e,true)">批准</button></div></template></article></section>
    </aside></div>
  </AppShell>
</template>
