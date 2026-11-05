# CampusOverflow 事件风暴 · 聚合识别清单（Aggregate Inventory）

> Lab 3 Step 3 产出物　｜　第 11 组　｜　依据 `process-modeling.md` 的 27 条命令-事件聚类
> 一致性边界原则：一个聚合内的所有不变式必须在单次事务内被保护；跨聚合一律通过领域事件最终一致。

## 一、聚合总览（紫色卡）

| 编号 | 聚合名 | 根实体 Root Entity | 聚合内实体 / 值对象 | 覆盖事件数 |
|---|---|---|---|---|
| A-01 | User | `User` | Value Object：`Email`、`DisplayName`、`OAuthBinding` | 6 |
| A-02 | Question | `Question` | Entity：`Answer`；Value Object：`Vote`、`Tag`、`QuestionStatus` | 10 |
| A-03 | Comment | `Comment` | Value Object：`CommentTarget` | 2 |
| A-04 | Notification | `Notification` | Value Object：`Recipient`、`ReadStatus` | 2 |
| A-05 | Report | `Report` | Value Object：`ReportedContent`、`ReportReason` | 1 |
| A-06 | Reputation | `ReputationProfile` | Entity：`ReputationRecord`；Value Object：`Score` | 2 |
| A-07 | Badge | `BadgeGrant` | Value Object：`BadgeType` | 1 |
| A-08 | —（无聚合） | 浏览类只读查询 | 由读模型承担，不产生领域事件 | 3 |

合计覆盖 6 + 10 + 2 + 2 + 1 + 2 + 1 + 3 = **27** 个事件，与 Big Picture 全量对齐。

> 限界上下文的正式边界与上下文间关系在 Step 4 划定，本清单只回答「哪些东西必须一起变」。

---

## 二、逐聚合：根实体、命令、事件、关键不变式

### A-01　User（用户）

- **根实体**：`User`（以 `UserId` 唯一标识）
- **命令**：Register Account、Verify Email、Update Profile、Login、Link OAuth Account
- **事件**：User Registered、Email Verification Sent、Email Verified、Profile Updated、User Logged In、OAuth Account Linked
- **关键不变式**：
  1. 邮箱在全局唯一，同一邮箱只能对应一个 `User`。
  2. `EmailVerified == false` 的账号不允许发布问题/回答/评论（只能浏览）。
  3. 同一 `OAuthProvider + OAuthSubject` 只能绑定到一个 `User`。
  4. `DisplayName` 非空、长度 2–30，且不可与已存在昵称重复。

### A-02　Question（问题，含 Answer 实体与 Vote 值对象）

- **根实体**：`Question`
- **聚合内实体**：`Answer`（属于某个 `Question`，随根实体一起加载与保存）
- **值对象**：`Vote`（userId + 方向 up/down）、`Tag`、`QuestionStatus`（Open / Solved / Deleted）
- **命令**：Publish Question、Edit Question、Add Tags、Delete Question、Post Answer、Edit Answer、Vote Answer、Accept Answer、Mark Question Solved、Delete Answer
- **事件**：Question Published、Question Edited、Question Tagged、Question Deleted、Answer Posted、Answer Edited、Answer Voted、Answer Accepted、Question Marked Solved、Answer Deleted
- **关键不变式**：
  1. `Title` 非空、长度 5–200；`Body` 非空、长度 ≥ 10。
  2. 标签数量 1–5 个，标签不超过 6 个字。
  3. **同一 `Question` 下最多只有一个 `Answer` 处于 `Accepted` 状态**（对应 Hot Spot HS-1：并发采纳必须在聚合根内串行化，采纳动作幂等）。
  4. 采纳某个 `Answer` 后，`Question.Status` 必须同时变为 `Solved`，且不可回退为 `Open`。
  5. 同一用户对同一 `Answer` 只能存在一条 `Vote`，重复投票视为改票而非新增。
  6. `Question.Status == Deleted` 时，其下所有 `Answer` 不可再被编辑、投票、采纳。
  7. 不允许「作者给自己的回答投票/采纳自己的回答」以外的自利操作被计入声誉（规则交由 A-06 判定，本聚合只发布事件）。

### A-03　Comment（评论）

- **根实体**：`Comment`
- **值对象**：`CommentTarget`（targetType = Question / Answer + targetId）
- **命令**：Post Comment、Delete Comment
- **事件**：Comment Posted、Comment Deleted
- **关键不变式**：
  1. 评论必须挂在一个未被删除的 `CommentTarget` 上。
  2. `Content` 非空、长度 ≤ 500。
  3. 删除为软删除，记录保留但不再进入任何列表。

### A-04　Notification（通知）

- **根实体**：`Notification`
- **值对象**：`Recipient`、`ReadStatus`
- **命令**：Notify User、Read Notification
- **事件**：Notification Sent、Notification Read
- **关键不变式**：
  1. 每条通知有且只有一个接收者。
  2. `ReadStatus` 只能由 `Unread → Read` 单向变化，不可回退。
  3. 同一业务事件（如 `Answer Accepted` 的 `eventId`）对同一接收者只生成一条通知（幂等，对应 HS-3 的重试风险）。
  4. 通知落库成功即视为「已发送」，外部推送失败不影响本聚合状态（推送由 Notification Service 重试）。

### A-05　Report（举报）

- **根实体**：`Report`
- **值对象**：`ReportedContent`、`ReportReason`（Spam / Abuse / OffTopic / Other）
- **命令**：Report Content、（缺失：处置命令 — 对应 Hot Spot HS-4）
- **事件**：Content Reported
- **关键不变式**：
  1. 同一举报人对同一内容只能有一条 `Pending` 状态的举报。
  2. `ReportReason` 必填，`Other` 必须附说明。
  3. 举报本身不修改被举报内容的任何状态（处置属于后续流程）。

### A-06　Reputation（声誉）

- **根实体**：`ReputationProfile`（每用户唯一）
- **聚合内实体**：`ReputationRecord`（每次加/扣分流水）
- **值对象**：`Score`
- **命令**：Award Reputation、Deduct Reputation
- **事件**：Reputation Increased、Reputation Decreased
- **关键不变式**：
  1. `Score` 不得小于 0，扣分下限截断为 0。
  2. 同一来源事件（`sourceEventId`）对同一用户只计一次（幂等）。
  3. 每条 `ReputationRecord` 必须记录来源事件与原因，不可无来源地变更分数。
  4. 加分规则未定义前，规则常量集中在聚合内部，不散落到监听器（对应 Hot Spot HS-2）。

### A-07　Badge（徽章）

- **根实体**：`BadgeGrant`（userId + badgeType）
- **值对象**：`BadgeType`
- **命令**：Award Badge
- **事件**：Badge Awarded
- **关键不变式**：
  1. 同一 `BadgeType` 对同一用户只能授予一次。
  2. 授予必须有可追溯的触发条件（由定时任务扫描 A-06 的 `Score` 或 A-02 的事件）。

---

## 三、一致性边界说明（哪些必须强一致、哪些允许最终一致）

| 关系 | 一致性要求 | 实现方式 |
|---|---|---|
| Question ↔ Answer ↔ Vote | **强一致**（同一聚合内） | 单事务加载 `Question` 聚合根，聚合方法内校验不变式 |
| Question 采纳 → Question 状态 Solved | **强一致**（同一聚合内） | `Question.acceptAnswer(answerId)` 一个方法内同时改两个状态 |
| Question / Answer → Reputation | **最终一致** | 聚合发布 `Answer Accepted` 等事件，A-06 异步订阅 |
| Question / Answer / Comment → Notification | **最终一致** | 事件驱动，A-04 幂等落库后交外部服务推送 |
| Report → 内容处置 | **最终一致**（当前为断点） | HS-4 待补处置命令与事件 |
| 浏览统计、回答数、投票数展示 | **最终一致** | 由读模型（投影）维护，不回写聚合 |

---

## 四、读模型清单（Read Model）

| 编号 | 读模型 | 支撑的命令 / 页面 | 主要字段 | 数据来源 |
|---|---|---|---|---|
| RM-01 | 问题列表视图 | Browse Question List | 标题、标签、回答数、投票数、状态、作者、更新时间 | Question 聚合投影 |
| RM-02 | 问题检索视图 | Search Questions | 标题/正文关键词、标签、排序分 | Question 聚合 + 检索索引 |
| RM-03 | 问题详情视图 | View Question | 问题全文、回答列表（含采纳标记）、评论数 | Question + Comment 聚合投影 |
| RM-04 | 用户主页视图 | 成长阶段展示 | 昵称、声誉分、徽章列表、提问/回答数 | User + Reputation + Badge 投影 |
| RM-05 | 未读通知计数视图 | 顶部未读红点 | userId、未读条数 | Notification 聚合投影 |
| RM-06 | 待处置举报列表 | 版主后台 | 被举报内容、举报原因、举报时间、状态 | Report 聚合投影 |

---

## 五、与 Hot Spot 的闭环

| Hot Spot | 聚合层处理方式 |
|---|---|
| HS-1 并发采纳冲突 | 收敛在 A-02 聚合根，`acceptAnswer` 在聚合内保证「唯一采纳 + 幂等」，由数据库乐观锁（version）兜底 |
| HS-2 加分规则未定义 | 收敛在 A-06 聚合内部，规则常量与阈值不落到监听器，待规则评审后只改聚合 |
| HS-3 依赖外部邮件/推送服务 | A-04 只保证落库与幂等，外部调用失败交由重试与降级，不进入聚合事务 |
| HS-4 举报后无处置事件 | 识别为缺失聚合行为，A-05 暂只记录举报，处置命令（Dismiss / Remove Content）列为待补 |
