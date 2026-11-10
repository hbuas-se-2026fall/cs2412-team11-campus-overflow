# ADR-002 模块与包结构

## 状态
已采纳（Lab 4，Sprint 2）

## 背景
[为什么现在要定包结构：27 个 Maven 模块、6 个限界上下文（identity / qa / reputation / discovery / shared-kernel / bootstrap），
多人并行开发，若无强约束会出现跨上下文直接依赖、领域层混入 Spring 注解等问题。]

## 决策
1. 每个限界上下文拆 5 个子模块：`-api` / `-domain` / `-application` / `-infrastructure` / `-interfaces`。
2. 依赖方向单向：interfaces → application → domain ← infrastructure；domain 不依赖任何框架。
3. 跨上下文只允许依赖对方的 `-api`（发布语言），禁止引用其 domain / application。
4. 表名前缀按上下文区分（`qa_` / `rep_` / `id_` / `disc_`）。
5. 上述约束由 ArchUnit 在 `co-bootstrap` 的 `ArchitectureTest` 中强制。

## 备选方案
- 方案 A：单一模块按包分层（controller/service/repository）——按技术切分，跨上下文边界靠自觉，多人协作易腐化，否决。
- 方案 B：按上下文分模块但不拆 api/domain——减少了模块数，但无法在编译期阻止领域层被基础设施反向依赖，否决。
- 方案 C：微服务拆分——超出课程与 Lab 4 范围，运维成本高，否决。

## 后果
- 正面：编译期即可挡住越界依赖；各上下文可独立演进与测试；domain 层纯 POJO，单测无需 Spring 上下文。
- 负面：模块数达 27，首次 `mvn install` 时间变长；新增一个用例要同时动 application 与 interfaces 两个模块。
- 风险控制：ArchUnit 失败即阻断 CI；`domain.coverage.minimum` 在 Lab 6 前保持 0。