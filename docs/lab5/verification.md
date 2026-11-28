# Lab 5 前端验证记录

## 环境

- 操作系统：Windows
- Node.js：v24.22.1
- pnpm：12.14.0
- 后端：Spring Boot 本地启动，端口 8080
- 数据库：Docker `co-mysql` / `co-redis`

## 验证结果

| 项目 | 命令 | 结果 |
|---|---|---|
| 类型检查 | `pnpm typecheck` | 通过（无输出） |
| 代码规范 | `pnpm lint` | 通过（无输出） |
| 开发服务器 | `pnpm dev` | 启动成功，http://localhost:5173/ 可访问 |
| 首页可访问 | 浏览器打开 `/questions` | 显示「全部问题」页面，无报错 |
| 后端接口 | `GET /api/v1/questions` | 返回 JSON，状态 200 |

## 结论

Lab 5 提交物第 1、2 条在本机验证通过：前端工程可通过 `pnpm dev` 启动并访问首页，
`pnpm lint` 与 `pnpm typecheck` 全绿。

验证人：向宇航（SM）
验证时间：2026-11-28