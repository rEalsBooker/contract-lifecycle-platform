import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import ContractsView from '@/views/ContractsView.vue'
import ContractDetailView from '@/views/ContractDetailView.vue'
import TasksView from '@/views/TasksView.vue'
import LoginView from '@/views/LoginView.vue'
import WorkspaceView from '@/views/WorkspaceView.vue'
import ContractUploadView from '@/views/ContractUploadView.vue'
import FinanceRiskView from '@/views/FinanceRiskView.vue'
import OrganizationView from '@/views/OrganizationView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView },
    { path: '/workspaces', component: WorkspaceView },
    { path: '/dashboard', component: DashboardView },
    { path: '/contracts', component: ContractsView },
    { path: '/contracts/upload', component: ContractUploadView },
    { path: '/contracts/:id', component: ContractDetailView, props: true }
    ,{ path: '/tasks', component: TasksView }
    ,{ path: '/receivables', component: FinanceRiskView }
    ,{ path: '/risks', component: FinanceRiskView }
    ,{ path: '/organization', component: OrganizationView }
  ]
})

export default router
