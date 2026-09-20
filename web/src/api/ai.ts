import { workspaceToken } from './auth'

export interface AiStatus {
  configured: boolean
  provider: string
  modelName: string
  mode: 'AI_ASSISTED_REVIEW' | 'MANUAL_REVIEW'
  boundary: string
}
export interface AiFinding { id:number; findingType:string; title:string; content:string; sourcePageNo:number|null; sourceExcerpt:string|null; confidence:number|null; reviewStatus:string; reviewNote:string|null }
export interface RagSource { contractId:number; contractVersionId:number; fileId:number; pageNo:number|null; chunkIndex:number; sourceText:string; score:number }
export interface ContractAnswer { answer:string; sources:string[]; ragSources:RagSource[]; toolsUsed:string[]; retrievalMode:string; modelName:string; boundary:string; warning:string|null }

function headers() {
  const token = workspaceToken()
  if (!token) throw new Error('请先选择企业工作空间')
  return { Authorization: `Bearer ${token}` }
}

async function errorMessage(response: Response) {
  const body = await response.json().catch(() => ({ message: '请求失败' }))
  return body.message ?? '请求失败'
}

export const aiApi = {
  async status() {
    const response = await fetch('/api/v1/ai/status', { headers: headers() })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<AiStatus>
  },
  async requestParse(contractId: number) {
    const requestKey = crypto.randomUUID()
    const response = await fetch(`/api/v1/contracts/${contractId}/parse-jobs`, {
      method: 'POST',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ requestKey })
    })
    if (!response.ok) throw new Error(await errorMessage(response))
    return response.json() as Promise<{ id: number; jobStatus: string; executionMode: string }>
  },
  async findings(contractId:number){const response=await fetch(`/api/v1/contracts/${contractId}/ai-findings`,{headers:headers()});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<AiFinding[]>},
  async updateFinding(contractId:number,id:number,value:Pick<AiFinding,'findingType'|'title'|'content'|'sourcePageNo'|'sourceExcerpt'>){const response=await fetch(`/api/v1/contracts/${contractId}/ai-findings/${id}`,{method:'PUT',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify(value)});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<AiFinding[]>},
  async confirmFinding(contractId:number,id:number){const response=await fetch(`/api/v1/contracts/${contractId}/ai-findings/${id}/confirm`,{method:'POST',headers:headers()});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<AiFinding[]>},
  async rejectFinding(contractId:number,id:number,note:string){const response=await fetch(`/api/v1/contracts/${contractId}/ai-findings/${id}/reject?${new URLSearchParams({note})}`,{method:'POST',headers:headers()});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<AiFinding[]>},
  async ask(contractId:number,question:string){const response=await fetch(`/api/v1/contracts/${contractId}/ai/questions`,{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({question})});if(!response.ok)throw new Error(await errorMessage(response));return response.json() as Promise<ContractAnswer>}
}
