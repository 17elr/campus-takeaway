# 版本记录

[项目首页](README.md) · [文档中心](docs/README.md)

## 仓库展示更新 · 2026-10-24

- 统一项目封面、构建徽章、首页导航与四角色截图展示。
- 新增启动手册、文档中心与完整界面图册，整理已有验证结果。
- 更新仓库简介与技术标签，保留原截图、提交和资源来源。

## 2.0.0 · 2026-10-24

从 Java Web / Thymeleaf 版升级为 Vue 3 + Element Plus 与 JSON API，保留四角色订单闭环。

- Redis 共享 Session、独立认证/CSRF 过滤器，保留 BCrypt。
- 数据库购物车、版本与结算凭证、事务和用户行锁，支持跨实例幂等结算。
- 公共菜单缓存、提交后失效、故障回源与数据库现价结算。
- SQL 分页与批量明细、未支付超时取消、启动补偿及商家/骑手页面轮询。
- Flyway V1/V2、Compose、多阶段 Docker 构建、真实服务 CI、迁移与回退文档。

[升级 PR #2](https://github.com/17elr/campus-takeaway/pull/2) · [合并提交](https://github.com/17elr/campus-takeaway/commit/3a229298af4d8b6f93312b490700087c4b8714d4) · [验证记录](docs/verification.md)

## Java Web 基线 · 2026-10-12

Spring Boot / MyBatis / MySQL / Thymeleaf 与 Spring Security，学生、商家、骑手、管理员使用服务端页面。保留订单快照、事务、条件抢单、图片与专用 MySQL 测试库。

[基线提交](https://github.com/17elr/campus-takeaway/commit/695c93e6aa4887d26156fc3b015e1a25f6d731e5) · [历史截图来源](docs/images/SOURCES.md)
