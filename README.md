# 契约智控

企业合同履约与回款风险管理平台。项目采用模块化单体，围绕“合同条款转成可执行任务，并持续跟踪履约与回款风险”构建，不是普通合同文件管理系统。

## 当前进度

- 已完成登录、企业工作空间、JWT、Membership 角色权限与审计日志。
- 已完成合同草稿、PDF/DOCX 私有存储、受控下载、人工条款录入/确认/发布。
- 已完成履约任务领取、执行、私有凭证上传、指定不同成员审核、驳回重提和审核完成。
- 已完成应收计划、部分回款、已有发票登记、逾期计算、风险处置与工作台汇总。
- 已完成按企业与成员隔离的站内通知、已读状态，以及可重试、可去重的 Outbox 可靠投递基础。
- 已完成 Docker Compose 基础环境：MySQL、Redis、RabbitMQ、MinIO；RabbitMQ Outbox 和 MinIO 私有文件存储均可通过环境变量切换启用。
- 已完成 AI 解析作业、PDF/DOCX 文本提取、来源页码标记、不可变 AI 发现项、失败重试与 LangChain4j 模型适配边界。
- 已接入千问 OpenAI 兼容接口，完成 AI 发现项人工修改、确认/驳回。
- 已接入 Qdrant + LangChain4j Embedding：按合同版本切分 Chunk、建立向量索引、按 tenant/contract/version 过滤召回，并保留页码和原文依据。
- 已接入 LangChain4j `@Tool` 只读合同风险 Agent：按问题自主查询合同、履约任务、应收和风险，工具内部继续复用现有 Service 权限校验。
- 已完成组织与设置：成员创建/停用、预置角色、合同读取授权，并由后端统一校验租户和对象权限。
- 已完成任务内部计划延期申请/异人审批，合同约定日期不随延期审批修改。
- 已完成合同候选版本、异人变更确认、完成前置检查、终止、归档快照与解档。
- 已完成数据库 Outbox 本地派发和可切换 RabbitMQ 适配；本机未安装 RabbitMQ 时默认模式不依赖消息中间件。
- 未配置真实模型密钥时，页面明确显示人工审阅模式，不伪造 AI 结果，也不生成法律意见。

当前演示主线已覆盖：合同上传 → 人工条款确认 → 履约任务 → 凭证审核与通知 → 应收/回款 → 逾期风险与通知。

AI 解析默认使用千问兼容接口。通过环境变量配置 `APP_AI_API_KEY`；可选覆盖 `APP_AI_PROVIDER=qwen`、`APP_AI_MODEL_NAME=qwen3.8-flash` 和 `APP_AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1`。未配置密钥时，发起 AI 解析会被后端拒绝，人工审阅主线不受影响。密钥不要写入仓库。

## 目录

- `server`：Spring Boot + MyBatis 后端。
- `web`：Vue 3 + Vite 管理端。
- `docs`：需求、原型、范围和技术设计基线。

## 本地启动

1. 准备 MySQL 8，创建数据库 `contract_lifecycle`。
2. 复制 `server/.env.example` 的配置到本机环境变量，或直接调整 `server/src/main/resources/application-dev.yml` 中的本地数据库连接。
3. 后端：在 `server` 执行 `mvn spring-boot:run`。
4. 前端：在 `web` 执行 `npm install`，然后执行 `npm run dev`。

基础设施联调：先在项目根目录执行 `docker compose up -d`。当前机器已有本地 MySQL 占用 3306，因此 Compose 中的 MySQL 映射到 3307；现有开发数据库仍使用 3306。Compose 还包含 Redis、RabbitMQ、MinIO 和 Qdrant。启用 RabbitMQ、MinIO、Qdrant 时，后端环境变量设置为 `APP_MESSAGING_MODE=rabbit`、`APP_STORAGE_PROVIDER=minio`、`APP_AI_QDRANT_URL=http://localhost:6333`，RabbitMQ 使用 `SPRING_RABBITMQ_USERNAME=contract_app`、`SPRING_RABBITMQ_PASSWORD=contract_app_dev`。

Embedding 默认使用与千问兼容的 `text-embedding-v4`，与聊天模型共用 `APP_AI_API_KEY` 和 `APP_AI_BASE_URL`。可通过 `APP_AI_RAG_ENABLED=false` 或 `APP_AI_AGENT_ENABLED=false` 关闭对应能力；RAG 服务异常时会明确记录并按配置降级到合同内文本检索。

`dev` 配置会在空数据库中创建两个演示账号：

- `admin / ChangeMe123!`：企业管理员、合同负责人、财务人员。
- `reviewer / ChangeMe123!`：项目负责人，用于双账号凭证审核演示。

以上账号仅用于本地开发，不能用于任何公开环境。

## 验证

- 后端：在 `server` 执行 `mvn test`。
- 前端：在 `web` 执行 `npm run build`。
- 健康检查：`GET http://localhost:8080/api/v1/health`。
