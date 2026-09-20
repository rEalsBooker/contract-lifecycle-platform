import { workspaceToken } from './auth'

export interface ContractItem {
  id: number
  contractNo: string
  name: string
  counterpartyName: string
  totalAmount: number
  businessStatus: string
  archiveStatus: string
  sourceFilename: string | null
  updatedAt: string
}

export interface ClauseItem { id: number; clauseType: string; clauseTitle: string; clauseContent: string; sourceType: string; sourcePageNo: number; sourceExcerpt: string | null; confirmationStatus: string }
export interface TaskItem { id: number; contractId: number; sourceClauseId: number; contractNo: string; contractName: string; title: string; taskDescription: string | null; taskStatus: string; taskOrigin: string; dueDate: string | null; contractualDueDate:string|null; internalPlanDate:string|null; assigneeMembershipId: number | null; reviewerMembershipId: number | null }
export interface ContractDetail extends ContractItem { parseJob: { id: number; jobStatus: string; executionMode: string; failureMessage: string | null } | null; clauses: ClauseItem[] }
export interface ClauseForm { clauseType: string; clauseTitle: string; clauseContent: string; sourcePageNo: number; sourceExcerpt: string }
export interface PendingEvidence { id:number; taskId:number; taskTitle:string; submitNote:string|null; submitterName:string; originalFilename:string; submittedAt:string }
export interface MemberOption { membershipId:number; displayName:string }
export interface ContractVersion { id:number; contractId:number; versionNo:number; versionStatus:string; originalFilename:string; changeRequestId:number|null; requesterMembershipId:number|null; reviewerMembershipId:number|null; changeSummary:string|null; requestStatus:string|null; reviewNote:string|null; createdAt:string }
export interface CompletionCheck { unfinishedTaskCount:number; pendingReviewCount:number; activeRiskCount:number; receivableBalance:number; completable:boolean }
export interface TaskExtension { id:number;taskId:number;taskTitle:string;applicantMembershipId:number;applicantName:string;approverMembershipId:number;originalInternalPlanDate:string;requestedInternalPlanDate:string;reason:string;approvalStatus:string;approvalNote:string|null;createdAt:string }

function headers() {
  const token = workspaceToken()
  if (!token) throw new Error('请先选择企业工作空间')
  return { Authorization: `Bearer ${token}` }
}

async function errorMessage(response: Response) {
  const body = await response.json().catch(() => ({ message: '请求失败' }))
  return body.message ?? '请求失败'
}

export const contractApi = {
  async list() {
    const response = await fetch('/api/v1/contracts', { headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<ContractItem[]>
  },
  async createDraft(form: { contractNo: string; name: string; counterpartyName: string; totalAmount: string; file: File }) {
    const data = new FormData()
    data.append('contractNo', form.contractNo)
    data.append('name', form.name)
    data.append('counterpartyName', form.counterpartyName)
    data.append('totalAmount', form.totalAmount)
    data.append('file', form.file)
    const response = await fetch('/api/v1/contracts/drafts', { method: 'POST', headers: headers(), body: data })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<ContractItem>
  },
  async detail(contractId: number) {
    const response = await fetch(`/api/v1/contracts/${contractId}`, { headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<ContractDetail>
  },
  async prepareParse(contractId: number) {
    const response = await fetch(`/api/v1/contracts/${contractId}/parse-preparations`, { method: 'POST', headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<ContractDetail>
  },
  async createClause(contractId: number, clause: ClauseForm) {
    const response = await fetch(`/api/v1/contracts/${contractId}/clauses`, { method: 'POST', headers: { ...headers(), 'Content-Type': 'application/json' }, body: JSON.stringify(clause) })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<ContractDetail>
  },
  async updateClause(contractId: number, clauseId: number, clause: ClauseForm) {
    const response = await fetch(`/api/v1/contracts/${contractId}/clauses/${clauseId}`, { method: 'PUT', headers: { ...headers(), 'Content-Type': 'application/json' }, body: JSON.stringify(clause) })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<ContractDetail>
  },
  async confirmClause(contractId: number, clauseId: number) {
    const response = await fetch(`/api/v1/contracts/${contractId}/clauses/${clauseId}/confirm`, { method: 'POST', headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<ContractDetail>
  },
  async publishTerms(contractId: number) {
    const response = await fetch(`/api/v1/contracts/${contractId}/terms/publish`, { method: 'POST', headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<ContractDetail>
  },
  async listTasks(contractId?: number) {
    const suffix = contractId ? `?contractId=${contractId}` : ''
    const response = await fetch(`/api/v1/tasks${suffix}`, { headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<TaskItem[]>
  },
  async generateTaskDrafts(contractId: number) {
    const response = await fetch(`/api/v1/contracts/${contractId}/task-drafts`, { method: 'POST', headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<TaskItem[]>
  },
  async claimTask(taskId: number, dueDate: string, reviewerMembershipId: number) {
    const response = await fetch(`/api/v1/tasks/${taskId}/claim`, { method: 'POST', headers: { ...headers(), 'Content-Type': 'application/json' }, body: JSON.stringify({ dueDate, reviewerMembershipId }) })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<TaskItem>
  },
  async startTask(taskId: number) {
    const response = await fetch(`/api/v1/tasks/${taskId}/start`, { method: 'POST', headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<TaskItem>
  },
  async activeMembers() {
    const response=await fetch('/api/v1/members/active-options',{headers:headers()})
    if(!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<MemberOption[]>
  },
  async submitEvidence(taskId:number,file:File,note:string) {
    const data=new FormData(); data.append('file',file); data.append('note',note)
    const response=await fetch(`/api/v1/tasks/${taskId}/evidence-submissions`,{method:'POST',headers:headers(),body:data})
    if(!response.ok) throw new Error(await errorMessage(response))
  },
  async pendingEvidence() {
    const response=await fetch('/api/v1/evidence-submissions/pending-review',{headers:headers()})
    if(!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<PendingEvidence[]>
  },
  async reviewEvidence(id:number,approved:boolean,note:string) {
    const params=new URLSearchParams({approved:String(approved),note})
    const response=await fetch(`/api/v1/evidence-submissions/${id}/review?${params}`,{method:'POST',headers:headers()})
    if(!response.ok) throw new Error(await errorMessage(response))
  },
  async downloadEvidence(item:PendingEvidence) {
    const response=await fetch(`/api/v1/evidence-submissions/${item.id}/file`,{headers:headers()})
    if(!response.ok) throw new Error(await errorMessage(response))
    const blob=await response.blob(); const url=URL.createObjectURL(blob); const anchor=document.createElement('a')
    anchor.href=url; anchor.download=item.originalFilename||'evidence'; anchor.click(); URL.revokeObjectURL(url)
  },
  async downloadSource(contract: ContractItem) {
    const response = await fetch(`/api/v1/contracts/${contract.id}/source-file`, { headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = contract.sourceFilename ?? 'contract'
    anchor.click()
    URL.revokeObjectURL(url)
  },
  async versions(contractId:number){const response=await fetch(`/api/v1/contracts/${contractId}/versions`,{headers:headers()});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<ContractVersion[]>},
  async uploadVersion(contractId:number,file:File,changeSummary:string,reviewerMembershipId:number){const data=new FormData();data.append('file',file);data.append('changeSummary',changeSummary);data.append('reviewerMembershipId',String(reviewerMembershipId));const response=await fetch(`/api/v1/contracts/${contractId}/versions`,{method:'POST',headers:headers(),body:data});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<ContractVersion[]>},
  async reviewVersion(contractId:number,requestId:number,approved:boolean,note:string){const response=await fetch(`/api/v1/contracts/${contractId}/version-change-requests/${requestId}/review`,{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({approved,note})});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<ContractVersion[]>},
  async completionCheck(contractId:number){const response=await fetch(`/api/v1/contracts/${contractId}/completion-check`,{headers:headers()});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<CompletionCheck>},
  async lifecycle(contractId:number,action:'complete'|'terminate'|'archive'|'unarchive',reason=''){const suffix=reason?`?${new URLSearchParams({reason})}`:'';const response=await fetch(`/api/v1/contracts/${contractId}/${action}${suffix}`,{method:'POST',headers:headers()});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<ContractDetail>},
  async extensions(){const response=await fetch('/api/v1/task-extensions',{headers:headers()});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<TaskExtension[]>},
  async requestExtension(taskId:number,requestedInternalPlanDate:string,reason:string,approverMembershipId:number){const response=await fetch(`/api/v1/tasks/${taskId}/extensions`,{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({requestedInternalPlanDate,reason,approverMembershipId})});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<TaskExtension[]>},
  async reviewExtension(id:number,approved:boolean,note:string){const response=await fetch(`/api/v1/task-extensions/${id}/review`,{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({approved,note})});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<TaskExtension[]>}
}
