# CampusOverflow 事件风暴 · Process Modeling（命令-角色-外部系统）

> Lab 3 Step 2 产出物　｜　第 11 组　｜　与 `event-storming-bigpicture.png` 的 27 个领域事件逐条对应
> 便利贴颜色约定：**蓝色 = 命令 Command（谁触发）**　**黄色 = 角色 Actor**　**粉色 = 外部系统 External System**

## 一、颜色与命名规则

| 卡片 | 颜色 | 命名规则 | 示例 |
|---|---|---|---|
| 领域事件 | 橙色 | 英文过去时 + 中文 | `Question Published` 问题已发布 |
| 命令 | 蓝色 | 动词原形 + 宾语（英文祈使式 + 中文） | `Publish Question` 发布问题 |
| 角色 | 黄色 | 谁发起该命令 | 访客 / 注册用户 / 提问者 / 回答者 / 教师（版主）/ 定时任务 |
| 外部系统 | 粉色 | 系统边界之外的服务 | Email Service / OAuth Provider / Notification Service |

## 二、命令-角色-外部系统全表（27 条，与 Big Picture 一一对应）

### 阶段 1　注册

| # | 领域事件（橙） | 命令（蓝） | 角色（黄） | 外部系统（粉） |
|---|---|---|---|---|
| 1 | User Registered　用户已注册 | Register Account　注册账号 | 访客 Visitor | OAuth Provider（第三方登录时） |
| 2 | Email Verification Sent　验证邮件已发送 | Send Verification Email　发送验证邮件 | 定时任务 Scheduler（注册后自动触发） | Email Service |
| 3 | Email Verified　邮箱已验证 | Verify Email　验证邮箱 | 注册用户 Registered User | Email Service（校验链接来源） |
| 4 | Profile Updated　资料已更新 | Update Profile　更新资料 | 注册用户 Registered User | — |
| 5 | User Logged In　用户已登录 | Login　登录 | 注册用户 Registered User | OAuth Provider（第三方登录时） |
| 6 | OAuth Account Linked　OAuth 账号已绑定 | Link OAuth Account　绑定 OAuth 账号 | 注册用户 Registered User | OAuth Provider |

### 阶段 2　浏览

| # | 领域事件（橙） | 命令（蓝） | 角色（黄） | 外部系统（粉） |
|---|---|---|---|---|
| 7 | Question List Viewed　问题列表已浏览 | Browse Question List　浏览问题列表 | 访客 / 注册用户 | — |
| 8 | Question Searched　问题已检索 | Search Questions　检索问题 | 访客 / 注册用户 | — |
| 9 | Question Viewed　问题详情已浏览 | View Question　查看问题详情 | 访客 / 注册用户 | — |

### 阶段 3　提问

| # | 领域事件（橙） | 命令（蓝） | 角色（黄） | 外部系统（粉） |
|---|---|---|---|---|
| 10 | Question Published　问题已发布 | Publish Question　发布问题 | 注册用户（提问者 Asker） | — |
| 11 | Question Edited　问题已编辑 | Edit Question　编辑问题 | 提问者 / 教师（版主 Moderator） | — |
| 12 | Question Tagged　问题已打标签 | Add Tags　添加标签 | 提问者 Asker | — |
| 13 | Question Deleted　问题已删除 | Delete Question　删除问题 | 提问者 / 教师（版主 Moderator） | — |

### 阶段 4　回答

| # | 领域事件（橙） | 命令（蓝） | 角色（黄） | 外部系统（粉） |
|---|---|---|---|---|
| 14 | Answer Posted　回答已发布 | Post Answer　发布回答 | 注册用户（回答者 Answerer） | — |
| 15 | Answer Edited　回答已编辑 | Edit Answer　编辑回答 | 回答者 Answerer | — |
| 16 | Answer Voted　回答已投票 | Vote Answer　投票回答 | 注册用户（含提问者） | — |
| 17 | Answer Accepted　回答已被采纳 | Accept Answer　采纳回答 | 提问者 Asker | — |
| 18 | Question Marked Solved　问题已标记解决 | Mark Question Solved　标记问题已解决 | 提问者 Asker | — |
| 19 | Answer Deleted　回答已删除 | Delete Answer　删除回答 | 回答者 / 教师（版主 Moderator） | — |

### 阶段 5　互动

| # | 领域事件（橙） | 命令（蓝） | 角色（黄） | 外部系统（粉） |
|---|---|---|---|---|
| 20 | Comment Posted　评论已发布 | Post Comment　发布评论 | 注册用户 Registered User | — |
| 21 | Comment Deleted　评论已删除 | Delete Comment　删除评论 | 评论者 / 教师（版主 Moderator） | — |
| 22 | Notification Sent　通知已发送 | Notify User　发送通知 | 定时任务 Scheduler（事件监听后异步触发） | Notification Service、Email Service |
| 23 | Notification Read　通知已读 | Read Notification　阅读通知 | 注册用户 Registered User | Notification Service |
| 24 | Content Reported　内容已举报 | Report Content　举报内容 | 注册用户 Registered User | — |

### 阶段 6　成长

| # | 领域事件（橙） | 命令（蓝） | 角色（黄） | 外部系统（粉） |
|---|---|---|---|---|
| 25 | Reputation Increased　声誉分已增加 | Award Reputation　加声誉分 | 定时任务 Scheduler（监听 Answer Accepted / Answer Voted） | — |
| 26 | Reputation Decreased　声誉分已扣减 | Deduct Reputation　扣声誉分 | 教师（版主 Moderator）/ 定时任务 Scheduler | — |
| 27 | Badge Awarded　徽章已授予 | Award Badge　授予徽章 | 定时任务 Scheduler（规则扫描） | — |

## 三、角色清单（黄色卡，共 6 类）

| 角色 | 英文 | 触发的命令 |
|---|---|---|
| 访客 | Visitor | Register Account、Browse Question List、Search Questions、View Question |
| 注册用户 | Registered User | Verify Email、Update Profile、Login、Link OAuth Account、Browse/Search/View、Vote Answer、Post Comment、Report Content、Read Notification |
| 提问者 | Asker | Publish Question、Add Tags、Edit Question、Delete Question、Accept Answer、Mark Question Solved |
| 回答者 | Answerer | Post Answer、Edit Answer、Delete Answer |
| 教师（版主） | Moderator | Edit/Delete Question、Delete Answer、Delete Comment、Deduct Reputation |
| 定时任务 | Scheduler | Send Verification Email、Notify User、Award Reputation、Deduct Reputation、Award Badge |

## 四、外部系统清单（粉色卡，共 3 个）

| 外部系统 | 英文 | 关联事件 | 系统边界说明 |
|---|---|---|---|
| 邮件服务 | Email Service | Email Verification Sent、Email Verified、Notification Sent | 系统外，只通过异步消息调用，失败需重试 |
| 第三方登录提供商 | OAuth Provider | User Registered、User Logged In、OAuth Account Linked | 系统外，通过 ACL（防腐层）转换用户信息 |
| 通知推送服务 | Notification Service | Notification Sent、Notification Read | 系统外，站内通知由本系统落库，推送由该服务负责 |

## 五、与 Hot Spot 的对应（延续 Step 1）

| Hot Spot | 关联事件 | Process Modeling 补充说明 |
|---|---|---|
| HS-1 | Answer Accepted | 命令 `Accept Answer` 的发起角色只有「提问者」，但并发采纳需在命令侧做唯一性约束 |
| HS-2 | Reputation Increased | 命令 `Award Reputation` 由定时任务触发，加分规则尚未定义，规则须单独评审 |
| HS-3 | Notification Sent | 命令 `Notify User` 依赖 External `Notification Service`，需明确失败重试与降级策略 |
| HS-4 | Content Reported | 命令 `Report Content` 之后没有对应的处置事件与命令，流程断点 |
