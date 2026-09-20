# 契约智控——技术设计基线

> 版本：v0.1；日期：2026-09-17；阶段：迭代开发；状态：已完成合同、履约审核、应收风险、站内通知、Outbox、RabbitMQ 可切换适配及 AI 解析作业基础；真实模型密钥与解析结果审阅转换待接入。  
> 本文以 `requirements-v0.1.md` 与 `project-scope.md` 为准。Calicat 原型用于页面和字段参考；其交互连线因额度不足暂缓，不改变本文的业务约束。

## 1. 目标与架构决策

第一版采用**模块化单体**：一个 Spring Boot 服务、一套 MySQL 数据库、一个 Vue 3 管理端。模块之间通过清晰的领域边界和应用事件协作，不拆 Spring Cloud 微服务。

| 组件 | 第一版职责 | 不承担的职责 |
| --- | --- | --- |
| Spring Boot + MyBatis | 身份、租户、业务规则、权限、审计、REST API | 分布式服务治理 |
| MySQL | 交易事实、状态、审计、待发送事件、幂等键 | 大文件存储、向量检索 |
| MinIO | 合同与凭证私有对象存储 | 公开链接下载 |
| Redis | 验证码、登录会话、限流、短期权限缓存 | 回款余额等财务事实 |
| RabbitMQ | 解析、索引、通知的异步派发 | 业务事实的唯一来源 |
| LangChain4j | 合同结构化解析、合同内问答、摘要草稿 | 发布任务、登记回款、法律意见 |

部署先使用 Docker Compose 管理 MySQL、Redis、RabbitMQ、MinIO 与单体服务。Spring Cloud、WebSocket、OCR、银行/税务接口不进入 MVP。

## 2. 后端模块边界

```text
identity      租户、用户、成员关系、角色、合同授权
contract      合同、版本、条款、解析、确认、归档、变更
fulfillment   节点、任务、凭证批次、审核、内部延期
receivable    应收计划、回款流水、发票、作废更正
risk          风险规则、风险实例、处置、通知
file          MinIO 对象、受控下载、归属校验
ai            解析作业、来源定位、RAG、只读 Tools、AI 草稿
audit         审计日志、待发送事件、幂等记录
```

模块只能通过应用服务调用；Controller 不直接写 Mapper。所有写操作由应用服务完成权限、状态、版本与幂等校验后，再开启数据库事务。

## 3. 租户、身份与权限

### 3.1 请求上下文

登录令牌只识别 `userId`。用户选择工作空间后，服务端从有效 Membership 获取 `tenantId` 与 `membershipId`，写入请求上下文；前端传入的租户标识只作选择请求，不作为可信数据范围。

每个查询和写入都遵循：

```text
有效成员关系 → 预置角色动作权限 → 对象归属 tenant_id → 合同授权范围 → 当前状态允许
```

`platform_operator` 仅可管理租户开通/停用，不默认拥有企业合同、文件或财务读取权限。普通成员仅取得本人任务必要上下文，不自动读取合同全文或财务数据。

### 3.2 权限实现

- 预置角色：`ENTERPRISE_ADMIN`、`CONTRACT_OWNER`、`FINANCE`、`PROJECT_OWNER`、`LEGAL`、`MEMBER`、`PLATFORM_OPERATOR`。
- 动作权限以服务端枚举实现，例如 `CONTRACT_READ`、`CONTRACT_EDIT`、`RECEIVABLE_WRITE`、`EVIDENCE_REVIEW`、`RISK_CLOSE`。
- `contract_grant` 限定合同级范围；企业管理员可在本租户全量管理。财务字段另加 `FINANCE_READ/WRITE` 判断。
- 审核接口要求 `submitter_membership_id != reviewer_membership_id`；延期和版本确认同理。
- 撤权或停用后清理对应 Redis 会话/权限缓存；异步任务发布结果、受控下载、AI Tools 仍需重新校验。

## 4. 数据模型

所有业务表均包含 `id`、`tenant_id`、`created_at`、`created_by`、`updated_at`、`updated_by`、`version`；禁止用物理删除替代业务作废。金额使用 `decimal(18,2)`，时间统一 `Asia/Shanghai`。

| 领域 | 核心表 | 关键字段与约束 |
| --- | --- | --- |
| 身份 | `tenant`、`user`、`membership`、`membership_role`、`contract_grant` | `membership(tenant_id,user_id)` 唯一；授权人与合同必须同租户 |
| 合同 | `contract`、`contract_version`、`contract_archive_snapshot` | 合同业务、解析、审阅、归档为独立状态；仅一个生效版本 |
| 解析与条款 | `file_object`、`parse_job`、`ai_finding`、`contract_clause`、`source_locator` | 原始模型结果不可覆盖；`source_locator` 保存 PDF 页码或 DOCX 段落及原文片段 |
| 履约 | `fulfillment_node`、`fulfillment_task`、`evidence_submission`、`evidence_file_ref`、`review_record`、`extension_request` | 节点/任务草稿发布后写正式记录；同任务仅一个待审批延期 |
| 财务 | `receivable_plan`、`receipt_record`、`invoice_record`、`void_record` | 有效回款累计不超过应收；同租户有效发票号唯一；作废生成新事实记录 |
| 风险与通知 | `risk_case`、`risk_round`、`risk_action`、`notification` | 活动风险去重键唯一；关闭前校验根因状态 |
| 可靠性 | `audit_log`、`idempotency_record`、`outbox_event` | 重要写入记录前后值；发布/回款/作废等写接口有持久化幂等键 |

建议索引：

- 每张业务表建立 `(tenant_id, id)` 与常用列表索引，例如 `contract(tenant_id, business_status, updated_at)`。
- `receivable_plan(tenant_id, due_date, collection_status)` 用于逾期扫描。
- `fulfillment_task(tenant_id, assignee_membership_id, status, internal_plan_date)` 用于个人待办。
- `risk_case(tenant_id, active_dedup_key)` 对活动记录唯一；关闭后释放或按轮次重开。
- `idempotency_record(tenant_id, operation, request_key)` 唯一。

## 5. 状态与关键规则

### 5.1 独立状态

| 对象 | 状态 |
| --- | --- |
| 合同业务 | `DRAFT`、`ACTIVE`、`COMPLETED`、`TERMINATED` |
| 解析作业 | `QUEUED`、`PROCESSING`、`SUCCEEDED`、`FAILED`、`CANCELED` |
| 审阅 | `NOT_STARTED`、`IN_REVIEW`、`REVIEWED` |
| 归档 | `UNARCHIVED`、`ARCHIVED` |
| 节点 | `PENDING_TRIGGER`、`PENDING`、`IN_PROGRESS`、`COMPLETED`、`CANCELED` |
| 任务 | `PENDING`、`IN_PROGRESS`、`PENDING_REVIEW`、`REJECTED`、`COMPLETED`、`CANCELED` |
| 应收收款 | `UNPAID`、`PARTIALLY_PAID`、`PAID`；到期另计算 `PENDING_TRIGGER/NORMAL/DUE_SOON/OVERDUE` |
| 风险 | `NEW`、`ACKNOWLEDGED`、`IN_PROGRESS`、`PENDING_CLOSE`、`CLOSED`、`IGNORED` |

不设置“已暂停”。解析失败不改变合同 `ACTIVE` 的生效版本、节点、任务或应收。内部延期只更新内部计划日，不改变合同约定日和已产生的逾期。

### 5.2 发布与财务事务

- 条款/节点/任务草稿可保存；未全部处置提取项、缺负责人、复杂日期未确认时禁止发布。
- 发布在单一事务中创建节点、任务与应收，并以草稿 ID 作为幂等范围；失败不留半套数据。
- 条件收款节点在客户验收事件经凭证审核确认前为 `PENDING_TRIGGER`，不计算到期或逾期。
- 回款登记验证金额大于零且不超过当前有效余额；并发时对 `receivable_plan` 行加锁。
- 作废不删除原流水，写作废事实、重算余额、创建风险重评事件；不模拟真实退款。

## 6. API 轮廓

统一前缀 `/api/v1`，写请求必须带 `Idempotency-Key`；冲突返回明确业务错误，不使用通用 `updateStatus` 接口。

| 模块 | 主要接口示例 |
| --- | --- |
| 身份 | `POST /auth/login`、`GET /workspaces`、`POST /workspaces/{id}/switch`、`GET /members`、`POST /members`、`POST /contract-grants` |
| 合同 | `GET/POST /contracts`、`GET /contracts/{id}`、`POST /contracts/{id}/versions`、`POST /contracts/{id}/archive`、`POST /contracts/{id}/complete-check` |
| 解析与审阅 | `POST /contracts/{id}/parse-jobs`、`GET /parse-jobs/{id}`、`POST /findings/{id}/confirm`、`POST /findings/{id}/reject`、`POST /review-drafts/{id}/publish` |
| 履约 | `GET /tasks`、`POST /tasks/{id}/evidence-submissions`、`POST /evidence-submissions/{id}/review`、`POST /tasks/{id}/extensions`、`POST /extensions/{id}/approve` |
| 财务 | `GET /receivables`、`POST /receivables/{id}/receipts`、`POST /receipts/{id}/void`、`POST /receivables/{id}/invoices` |
| 风险与 AI | `GET /risks`、`POST /risks/{id}/acknowledge`、`POST /risks/{id}/actions`、`POST /contracts/{id}/ai/questions` |

错误码至少区分：`AUTH_REQUIRED`、`MEMBERSHIP_INACTIVE`、`FORBIDDEN`、`OBJECT_NOT_FOUND`、`INVALID_STATE`、`CONFLICT_VERSION`、`IDEMPOTENCY_CONFLICT`、`AMOUNT_EXCEEDS_BALANCE`、`SELF_REVIEW_FORBIDDEN`、`AI_RESULT_UNAVAILABLE`。

## 7. 文件与异步处理

### 7.1 文件

仅接收文本型 PDF/DOCX 合同与 PNG/JPEG 凭证；单文件 20MB。上传顺序：服务端创建待上传记录 → MinIO 私有对象 → 校验成功后标记可用。下载通过受控接口重新鉴权并签发短时 URL；不将对象 URL 直接写入前端数据。

### 7.2 RabbitMQ 与 Outbox

数据库事务提交时写 `outbox_event`，后台可靠投递 RabbitMQ。消费者按事件 ID 幂等处理。

| 事件 | 消费动作 | 重试原则 |
| --- | --- | --- |
| `CONTRACT_PARSE_REQUESTED` | 提取文本、调用模型、保存 ParseJob/AiFinding | 网络/限流最多自动重试 2 次 |
| `CONTRACT_INDEX_REQUESTED` | 按已授权可用版本生成合同检索索引 | 失败可手动重建 |
| `TASK_OR_RISK_CHANGED` | 创建站内通知 | 同对象同接收人每日提醒去重 |
| `RECEIVABLE_CHANGED` | 重算到期与风险 | 幂等重放 |

模型 120 秒超时；取消后的迟到响应不得写入可见结果。格式、文件可读性、权限错误不自动重试，允许人工录入继续业务链路。

## 8. AI 与 RAG

开发阶段不伪造 AI 解析结果。未配置真实模型时，系统返回“人工审阅模式”，并允许人工录入条款继续业务闭环；最终 MVP 验收再接入实际模型解析与合同内 RAG。

- 结构化输出至少包含字段名、值、置信度、来源定位、模型版本、提示版本、生成时间和人工处置状态。
- RAG 只检索当前用户有权访问的当前合同/版本；查询前先做会员、合同、财务字段校验。
- 实时金额、任务、风险通过只读 Tools 查询数据库，不以向量摘要代替事实。
- AI 只返回辅助分析、风险提示或待审阅草稿；不得发布节点、修改合同、登记回款，也不得生成法律意见。
- 无来源、索引未就绪、权限不足、超时均返回可解释状态；权限不足不泄露金额或受限对象名称。

## 9. 实施与测试顺序

1. 建立数据库迁移、基础模块、请求上下文、租户与审计框架。
2. 实现成员/角色/合同授权、私有文件上传与受控下载，并完成跨租户测试。
3. 实现合同、版本、人工条款录入、节点任务草稿发布；先不接 AI。
4. 实现任务凭证、不同人审核、延期审批与通知。
5. 实现应收、部分回款、发票、作废和风险规则。
6. 接入 RabbitMQ + Outbox，完成解析作业和通知可靠性。
7. 接入 LangChain4j 的结构化解析、来源定位、合同内问答。
8. Vue 3 按稳定接口分模块联调，补充 E2E 主流程与异常测试。

每阶段至少覆盖 `requirements-v0.1.md` 的 AC01—AC16 对应项；优先跑 AC01、AC03、AC05、AC06、AC07、AC08、AC09、AC11、AC12、AC15。

## 10. 进入编码前的确认项

- 确认 MVP 以企业版、软件外包合同、收款侧为唯一演示主线；律所完整工作台继续延期。
- 确认多租户使用真实 `tenant_id + membership + contract_grant`，不以演示账号切换角色代替后端鉴权。
- 确认最终演示含实际模型解析与合同 RAG；开发早期离线模拟必须可见标识。
- Calicat 交互连线后续补齐，不作为当前技术设计的前置阻塞；代码联调阶段以本文件的接口与状态机为可执行流程依据。
