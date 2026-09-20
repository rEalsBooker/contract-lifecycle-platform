<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { contractApi } from '@/api/contracts'

const router = useRouter()
const saving = ref(false)
const error = ref('')
const selectedFile = ref<File | null>(null)
const form = reactive({ contractNo: '', name: '', counterpartyName: '', totalAmount: '' })

function chooseFile(event: Event) {
  error.value = ''
  const file = (event.target as HTMLInputElement).files?.[0] ?? null
  if (!file) { selectedFile.value = null; return }
  const allowed = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  if (!allowed.includes(file.type) && !/\.(pdf|docx)$/i.test(file.name)) { error.value = '格式不支持，请上传文本型 PDF 或 DOCX'; return }
  if (file.size > 20 * 1024 * 1024) { error.value = '文件超过 20MB，请压缩或拆分后重新上传'; return }
  selectedFile.value = file
}

async function submit(startReview: boolean) {
  if (!selectedFile.value) { error.value = '请选择合同原件'; return }
  saving.value = true; error.value = ''
  try {
    const created = await contractApi.createDraft({ ...form, file: selectedFile.value })
    if (startReview) await contractApi.prepareParse(created.id)
    await router.push(startReview ? `/contracts/${created.id}?tab=clauses` : '/contracts')
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '合同保存失败' }
  finally { saving.value = false }
}
</script>

<template>
  <AppShell title="上传合同" subtitle="上传合同并进入结构化审阅流程">
    <template #actions><button class="secondary-button compact" @click="router.push('/contracts')">← 返回列表</button></template>
    <p v-if="error" class="feedback error">{{ error }}</p>
    <div class="upload-page-grid">
      <section class="upload-main-card">
        <div class="section-title"><h2>合同文件</h2><p>仅支持文本型 PDF / DOCX，单文件上限 20MB；扫描件暂不支持 OCR。</p></div>
        <label class="large-file-drop"><input type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" @change="chooseFile" /><span class="upload-cloud">↑</span><strong>{{ selectedFile?.name ?? '拖拽文件到此处，或点击选择文件' }}</strong><small v-if="selectedFile">{{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB · 等待保存 · 私有存储</small><small v-else>支持 .pdf、.docx，系统按内容与扩展名共同校验</small></label>
        <div class="upload-rules"><span>格式不支持：请上传文本型 PDF / DOCX</span><span>超过 20MB：请压缩或拆分后再上传</span><span>上传中断：保留表单，可重新选择文件</span></div>

        <div class="section-title form-section-title"><h2>合同基础信息</h2><p>标 * 的为必填字段，缺失时无法开始审阅。</p></div>
        <form class="contract-base-form" @submit.prevent="submit(true)">
          <label>合同编号 <em>*</em><input v-model.trim="form.contractNo" required placeholder="例如 C001" /></label>
          <label class="span-2">合同名称 <em>*</em><input v-model.trim="form.name" required placeholder="请输入合同名称" /></label>
          <label>相对方（甲方 / 客户） <em>*</em><input v-model.trim="form.counterpartyName" required placeholder="请输入企业名称" /></label>
          <label>合同总额（人民币） <em>*</em><input v-model="form.totalAmount" required type="number" min="0.01" step="0.01" placeholder="0.00" /></label>
          <div class="form-actions span-2"><button type="button" class="text-button" @click="router.push('/contracts')">取消</button><button type="button" class="secondary-button" :disabled="saving" @click="submit(false)">保存草稿</button><button class="primary-button" :disabled="saving">{{ saving ? '保存中…' : '保存并开始审阅' }}</button></div>
        </form>
      </section>
      <aside class="privacy-card"><h3>文件与隐私归属</h3><ul><li>文件保存在私有存储，通过受控入口重新鉴权后下载</li><li>合同原文件不可公开访问</li><li>仅当前企业授权范围内的成员可访问</li><li>第一版只使用脱敏模拟合同数据</li></ul><div class="manual-mode-note"><strong>当前为人工审阅模式</strong><p>尚未配置 AI 模型，不会伪造合同解析结果，也不会输出法律意见。</p></div></aside>
    </div>
  </AppShell>
</template>
