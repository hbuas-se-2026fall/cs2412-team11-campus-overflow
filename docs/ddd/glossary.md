# CampusOverflow 统一语言术语表 v1.0（Ubiquitous Language Glossary）

> Lab 3 Step 4 产出物　｜　第 11 组　｜　共 **46** 条术语（中英对照）
> 使用约定：代码、接口、文档、看板卡片、会议记录一律使用本表的英文标识或中文名，禁止同义混用。

## 一、建模方法术语（20 条）

| 编号 | 中文 | English | 定义（团队统一口径） | 所属上下文 |
|---|---|---|---|---|
| T-01 | 领域事件 | Domain Event | 领域中已发生的、业务上有意义的事实，一律用过去时命名 | 全局 |
| T-02 | 命令 | Command | 触发领域事件的意图动作，用动词原形命名，必须指明发起角色 | 全局 |
| T-03 | 角色 | Actor | 发起命令的人或系统，包括用户角色与定时任务 | 全局 |
| T-04 | 聚合 | Aggregate | 一组必须作为一个整体保持一致性的对象集合 | 全局 |
| T-05 | 聚合根 | Aggregate Root | 聚合的唯一入口实体，外部只能通过它访问聚合内部 | 全局 |
| T-06 | 实体 | Entity | 有独立标识、生命周期内属性可变的领域对象 | 全局 |
| T-07 | 值对象 | Value Object | 无独立标识、不可变、以属性相等判断相同的领域对象 | 全局 |
| T-08 | 不变式 | Invariant | 聚合在任何时刻都必须成立的业务规则 | 全局 |
| T-09 | 限界上下文 | Bounded Context | 一个模型边界清晰的业务能力单元，内部有独立的统一语言 | 全局 |
| T-10 | 上下文映射 | Context Map | 描述多个限界上下文之间关系的全局视图 | 全局 |
| T-11 | 共享内核 | Shared Kernel | 多个上下文共同拥有、一起演进的模型子集（本项目的术语表与公共值对象） | 全局 |
| T-12 | 防腐层 | Anti-Corruption Layer (ACL) | 隔离外部系统模型、防止其概念污染领域模型的转换层 | 全局 |
| T-13 | 客户-供应商关系 | Customer-Supplier | 上游上下文提供服务或事件，下游上下文消费并按需提出需求 | 全局 |
| T-14 | 伙伴关系 | Partnership | 两个上下文成败绑定、需协同演进与联合发布 | Q&A ↔ Reputation |
| T-15 | 读模型 | Read Model | 为展示与查询优化的只读投影，不承载业务规则 | Discovery / 各上下文 |
| T-16 | 领域专家 | Domain Expert | 提供业务规则与真实场景的人，本次由教师或助教扮演 | 全局 |
| T-17 | 统一语言 | Ubiquitous Language | 团队与领域专家共同确认、贯穿文档与代码的同一套表述 | 全局 |
| T-18 | 热点 | Hot Spot | 事件风暴中暴露的冲突、未知或未定义规则所在处 | 全局 |
| T-19 | 事件风暴 | Event Storming | 以领域事件为主线、用彩色便利贴快速建模的协作方法 | 全局 |
| T-20 | 最终一致性 | Eventual Consistency | 跨聚合的数据经过事件传播后达到一致，允许短暂不一致 | Q&A / Reputation / Notification |

## 二、角色术语（6 条）

| 编号 | 中文 | English | 定义（团队统一口径） | 所属上下文 |
|---|---|---|---|---|
| T-21 | 访客 | Visitor | 未登录用户，只能浏览、检索与查看，不能发布任何内容 | Identity / Discovery |
| T-22 | 注册用户 | Registered User | 已注册且邮箱已验证的用户，可发布问题、回答与评论 | Identity |
| T-23 | 提问者 | Asker | 某条问题的作者，唯一有权采纳该问题下回答的角色 | Q&A |
| T-24 | 回答者 | Answerer | 某条回答的作者，可编辑或删除自己的回答 | Q&A |
| T-25 | 版主（教师） | Moderator | 具备内容治理权限的角色，可删除内容、扣减声誉分 | Q&A / Reputation |
| T-26 | 定时任务 | Scheduler | 非人为触发的系统角色，承担邮件发送、通知、声誉与徽章结算 | 全局 |

## 三、领域对象术语（20 条）

| 编号 | 中文 | English | 定义（团队统一口径） | 所属上下文 |
|---|---|---|---|---|
| T-27 | 问题 | Question | 一次求助帖，含标题、正文、标签与状态，是 Q&A 的聚合根 | Q&A |
| T-28 | 回答 | Answer | 挂在某条问题下的答复，属于问题聚合内部实体，不能独立存在 | Q&A |
| T-29 | 投票 | Vote | 用户对某条回答的赞成/反对表达，值对象，一人一答仅一条 | Q&A |
| T-30 | 采纳 | Acceptance | 提问者把某条回答标记为最佳答案的动作 | Q&A |
| T-31 | 标签 | Tag | 问题的分类标记，1–5 个，用于检索与聚合展示 | Q&A / Discovery |
| T-32 | 评论 | Comment | 挂在问题或回答下的补充说明，独立聚合 | Q&A |
| T-33 | 通知 | Notification | 面向单一接收者的站内消息，含已读状态 | Notification |
| T-34 | 举报 | Report | 用户对某条内容提交的违规报告，独立聚合 | Q&A |
| T-35 | 声誉分 | Reputation Score | 用户贡献度的累计得分，不得小于 0 | Reputation |
| T-36 | 声誉档案 | Reputation Profile | 每个用户唯一的声誉聚合根，含得分与全部加减分流水 | Reputation |
| T-37 | 徽章 | Badge | 达成条件后授予用户的荣誉标识，同类型只授予一次 | Reputation |
| T-38 | 用户档案 | User Profile | 用户的昵称、头像、简介等可编辑资料 | Identity |
| T-39 | 邮箱验证 | Email Verification | 通过邮件链接确认邮箱归属的流程 | Identity |
| T-40 | 第三方账号绑定 | OAuth Binding | 把外部身份提供商的账号与本站用户唯一绑定的关系 | Identity |
| T-41 | 内容 | Content | 问题、回答、评论的统称，举报与治理的作用对象 | 全局 |
| T-42 | 问题列表 | Question List | 问题摘要的集合视图，含回答数、投票数与状态 | Discovery |
| T-43 | 检索 | Search | 按关键词与标签查找问题的只读行为 | Discovery |
| T-44 | 个人主页 | User Home | 展示某用户声誉分、徽章与贡献统计的视图 | Discovery |
| T-45 | 待处置举报 | Pending Report | 尚未被版主处理完毕的举报条目 | Q&A |
| T-46 | 未读计数 | Unread Count | 某用户当前未读通知的条数 | Notification |

## 四、状态与规则术语（补充说明）

| 术语 | 取值 / 规则 | 说明 |
|---|---|---|
| 问题状态 Question Status | `Open` / `Solved` / `Deleted` | 采纳后自动 `Open → Solved`，不可回退；`Deleted` 为软删除终态 |
| 已解决 Solved | 布尔状态 | 由「采纳回答」或「标记解决」触发，与采纳互斥一致 |
| 软删除 Soft Delete | — | 数据保留、不再进入列表，不可物理删除后重建同标识 |
| 幂等 Idempotency | — | 同一来源事件重复投递只生效一次（通知、声誉加分） |
| 乐观锁 Optimistic Locking | version 字段 | 保护聚合并发修改，冲突方重试而非覆盖 |
| 领域事件发布 Domain Event Publishing | — | 聚合状态变更成功后发布事件，事件是跨上下文唯一通信方式 |

## 五、禁用与易混表述（团队约定）

| 不要这样写 | 统一改为 | 原因 |
|---|---|---|
| 帖子 / Post | 问题 Question | 「帖子」无法区分问题与回答 |
| 点赞 / Like | 投票 Vote（up / down） | 本项目投票有方向，不是单向点赞 |
| 最佳答案 / Best Answer | 已采纳回答 Accepted Answer | 与事件 `Answer Accepted` 对齐 |
| 用户操作完成 | 具体领域事件（如 `Answer Posted`） | 事件粒度过粗，无法建模 |
| 积分 / Point | 声誉分 Reputation Score | 避免与 Story Point 混淆 |
| 黑名单 / Ban | 版主处置 Moderator Action（待补） | 处置流程尚未建模，见 Hot Spot HS-4 |
