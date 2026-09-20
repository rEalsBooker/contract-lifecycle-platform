import { workspaceToken } from './auth'
export interface OrganizationMember{membershipId:number;username:string;displayName:string;status:string;roleCodes:string[]}
function headers(){const token=workspaceToken();if(!token)throw new Error('请先选择企业工作空间');return{Authorization:`Bearer ${token}`}}
async function errorMessage(r:Response){const b=await r.json().catch(()=>({message:'请求失败'}));return b.message??'请求失败'}
async function json<T>(r:Response){if(!r.ok)throw new Error(await errorMessage(r));return r.json() as Promise<T>}
export const organizationApi={
 members(){return fetch('/api/v1/organization/members',{headers:headers()}).then(json<OrganizationMember[]>)},
 create(value:{username:string;displayName:string;initialPassword:string;roleCodes:string[]}){return fetch('/api/v1/organization/members',{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify(value)}).then(json<OrganizationMember[]>)},
 roles(id:number,roleCodes:string[]){return fetch(`/api/v1/organization/members/${id}/roles`,{method:'PUT',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({roleCodes})}).then(json<OrganizationMember[]>)},
 status(id:number,value:string){return fetch(`/api/v1/organization/members/${id}/status?${new URLSearchParams({value})}`,{method:'POST',headers:headers()}).then(json<OrganizationMember[]>)},
 async grant(contractId:number,membershipId:number){const r=await fetch('/api/v1/organization/contract-grants',{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({contractId,membershipId})});if(!r.ok)throw new Error(await errorMessage(r))},
 async revoke(contractId:number,membershipId:number){const r=await fetch('/api/v1/organization/contract-grants',{method:'DELETE',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({contractId,membershipId})});if(!r.ok)throw new Error(await errorMessage(r))}
}
