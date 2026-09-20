import { workspaceToken } from './auth'

export interface ReceivableItem { id:number;contractId:number;contractNo:string;contractName:string;counterpartyName:string;planName:string;planAmount:number;triggerStatus:string;dueDate:string|null;receivedAmount:number;invoicedAmount:number;balance:number;collectionStatus:string;invoiceStatus:string;dueStatus:string;overdueAmount:number }
export interface RiskItem { id:number;contractId:number;contractNo:string;contractName:string;ruleCode:string;objectType:string;objectId:number;severity:string;riskStatus:string;factSummary:string;handlerMembershipId:number|null;handlerName:string|null;handlingPlan:string|null;reviewDate:string|null;ignoredReason:string|null;createdAt:string;updatedAt:string }
function headers(json=false){const token=workspaceToken();if(!token)throw new Error('请先选择企业工作空间');return{Authorization:`Bearer ${token}`,...(json?{'Content-Type':'application/json'}:{})}}
async function response<T>(r:Response){if(!r.ok){const b=await r.json().catch(()=>({message:'请求失败'}));throw new Error(b.message??'请求失败')}return r.json() as Promise<T>}
export const financeApi={
 listReceivables:()=>fetch('/api/v1/receivables',{headers:headers()}).then(response<ReceivableItem[]>),
 createReceivable:(body:{contractId:number;planName:string;planAmount:number;triggerStatus:string;dueDate:string|null})=>fetch('/api/v1/receivables',{method:'POST',headers:headers(true),body:JSON.stringify(body)}).then(response<ReceivableItem>),
 addReceipt:(id:number,body:{amount:number;receiptDate:string;paymentMethod:string;payerName:string;referenceNo:string;note:string;requestKey:string})=>fetch(`/api/v1/receivables/${id}/receipts`,{method:'POST',headers:headers(true),body:JSON.stringify(body)}).then(response<ReceivableItem>),
 addInvoice:(id:number,body:{invoiceNo:string;amount:number;invoiceDate:string;invoiceType:string;invoiceTitle:string;requestKey:string})=>fetch(`/api/v1/receivables/${id}/invoices`,{method:'POST',headers:headers(true),body:JSON.stringify(body)}).then(response<ReceivableItem>),
 listRisks:()=>fetch('/api/v1/risks',{headers:headers()}).then(response<RiskItem[]>),
 acknowledge:(id:number)=>fetch(`/api/v1/risks/${id}/acknowledge`,{method:'POST',headers:headers()}).then(response<RiskItem>),
 startRisk:(id:number,handlerMembershipId:number,handlingPlan:string)=>fetch(`/api/v1/risks/${id}/start`,{method:'POST',headers:headers(true),body:JSON.stringify({handlerMembershipId,handlingPlan})}).then(response<RiskItem>),
 ignoreRisk:(id:number,reason:string,reviewDate:string)=>fetch(`/api/v1/risks/${id}/ignore`,{method:'POST',headers:headers(true),body:JSON.stringify({reason,reviewDate})}).then(response<RiskItem>),
 closeRisk:(id:number)=>fetch(`/api/v1/risks/${id}/close`,{method:'POST',headers:headers()}).then(response<RiskItem>)
}
