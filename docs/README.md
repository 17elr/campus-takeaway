# 文档中心

[项目首页](../README.md) · [界面预览](SHOWCASE.md) · [快速开始](QUICKSTART.md)

按你现在想做的事选择入口：

| 目标 | 阅读入口 | 可以找到什么 |
| --- | --- | --- |
| 先运行起来 | [启动与开发](QUICKSTART.md) | Compose、开发环境、端口、环境变量和 JAR 构建 |
| 先看项目效果 | [界面图册](SHOWCASE.md) | 四角色页面、购物车、时间线与移动端截图 |
| 理解系统怎么工作 | [架构与状态图](architecture.md) | 模块职责、订单状态、事务边界与缓存取舍 |
| 接前端或查接口 | [JSON API 契约](API.md) | 请求、分页、CSRF、角色归属与错误码 |
| 看数据库设计 | [数据字典](DATA_DICTIONARY.md) | 表、约束、索引、幂等字段与命名空间 |
| 核对真实验证 | [测试与测量记录](verification.md) | 36 项 Java、4 项前端、浏览器、SQL 计数与边界 |
| 迁移或回退数据 | [迁移手册](MIGRATION.md) | 备份、V1/V2、旧密码/订单保留与回退演练 |
| 准备讲解项目 | [面试讲解](INTERVIEW.md) · [模块说明](PORTFOLIO.md) | 幂等、锁、Session、缓存一致性与个人贡献边界 |
| 查看版本变化 | [版本记录](../CHANGELOG.md) | Java Web 到 Vue / Redis 的升级与提交入口 |

## 可核对的记录

- [Java 测试结果](test-results.json)：36 项通过，0 失败、0 跳过。
- [浏览器结果](screenshots/browser-results.json)：17 条检查与完整订单闭环。
- [回退结果](rollback-results.json)：旧 JAR 读取前后，9 张业务表摘要一致。
- [GitHub Actions](https://github.com/17elr/campus-takeaway/actions/workflows/ci.yml)：真实 MySQL/Redis、Vue 和 Docker 构建。

## 截图与资源来源

当前 Vue 截图在 [screenshots](screenshots/SOURCES.md)，原 Java Web 截图在 [images](images/SOURCES.md) 作为历史存档。餐食照片来源保留在 [图片说明](../src/main/resources/static/images/credits.html)。仓库封面的制作方式与展示参考见 [assets](assets/README.md)。
