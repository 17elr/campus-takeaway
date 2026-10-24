# 校园好味道 · 校园外卖系统

Java 17 + Spring Boot 3.5.16 + MyBatis 3.0.5 + MySQL 8 + Redis + Spring Session + Vue 3 + Element Plus。

2026-10-24 将原 Java Web / Thymeleaf 版升级为 Vue 前后端分离。保留学生、商家、骑手、管理员的订单闭环。支付为模拟，不会产生真实扣款。原版提交与图片来源保留在 Git 历史中。

![学生菜单](docs/screenshots/student-menu.png)

## 快速启动

需要 Docker Compose。复制 `.env.example` 为 `.env`，替换两个密码。首次演示使用 `DEMO_DATA=true`；空库会创建示例账号和菜单，已有用户的数据库不会被自动重新灌入示例数据。

```sh
cp .env.example .env
docker compose up --build -d
```

打开 http://127.0.0.1:8080 。MySQL/Redis 宿主端口分别为 **13317 / 16377**，只绑定 127.0.0.1。容器使用 mysql:3306 / redis:6379。命名卷保存数据；停止使用 `docker compose stop`，不要删除数据卷。

| 角色 | 示例用户名 | 示例密码 | 入口 |
| --- | --- | --- | --- |
| 学生 | student | student123 | 选餐、购物车、地址、模拟支付、订单 |
| 商家 | merchant | merchant123 | 接单、制作、菜品、店铺 |
| 骑手 | rider | rider123 | 待配送大厅、本人配送 |
| 管理员 | admin | admin123 | 用户、店铺、菜品、订单概览 |

这些账号仅在显式启用示例数据且用户表为空时建立；部署到公网前必须关闭示例数据并更改所有示例密码，HTTPS 设置 `COOKIE_SECURE=true`。

## 本地开发

需要 Java **17**、Maven 3.9、Node **24**、MySQL 8 和 Redis。可以只用 Compose 启动依赖：

```sh
docker compose up -d mysql redis
# 在当前终端设置 DB_USERNAME=takeaway、DB_PASSWORD 为 .env 中值
mvn spring-boot:run
# 另一个终端
cd web
npm ci
npm run dev
```

Vue 开发地址 http://127.0.0.1:18081 ，`/api` 和 `/images` 代理到 8080。开发与生产均同源携带 Cookie，无任意凭证 CORS。修改 Vite 代理端口时同步修改后端端口。

构建一个包含 Vue 的 JAR：

```sh
npm ci --prefix web
npm run build --prefix web
mvn verify
java -jar target/campus-takeaway-2.0.0.jar
```

`mvn verify` 默认只运行单元测试，MySQL 集成测试需显式启用；完整命令见下节。Maven 从 `web/dist` 拷贝页面，不会自动下载 Node 或运行 npm。Docker 构建执行前端检查，后端集成验证由 CI 使用真实服务完成。

## 业务与一致性

- 学生：注册/登录、店铺菜单、单店购物车、校内地址/默认地址、模拟支付、取消、确认收货、再来一单。
- 商家：自有店铺、营业状态、菜品分类/现价/上下架、接单和制作完成。
- 骑手：待配送大厅、条件抢单、本人配送、标记送达。
- 管理员：创建商家/骑手、用户启停、店铺启停、菜品和订单概览；没有任意改订单状态的接口。
- 四角色共用：资料、改密、角色导航、订单详情、事件时间线与错误反馈。
- 数据库购物车：用户行锁统一串行化修改与结算；版本/随机凭证和订单唯一键实现跨实例幂等。订单、明细、事件与清空购物车同事务。
- 登录：Redis 共享 Session，仅保存用户编号、认证版本与 CSRF 安全元数据。每次请求重新查 MySQL 中的启用状态与角色。旧 BCrypt 哈希兼容。
- 菜单：`takeaway:menu:v1:{shopId}` 缓存默认第一页 12 条；60–75 秒 TTL，空店铺 10 秒；其他分页/搜索直接执行数据库 LIMIT/OFFSET。改菜品/营业状态/停用商家后提交后失效。
- 结算价格始终重新查 MySQL。菜单缓存允许短暂旧展示；缓存不承担订单正确性。
- 未支付超时：创建时固定 15 分钟期限，每 30 秒批量条件取消，启动时补偿；事件原因 `PAYMENT_TIMEOUT`。
- 订单列表：先数据库过滤分页，再一次 IN 查询批量加载本页明细，消除逐订单查明细。
- 商家/骑手订单列表在可见时每 8 秒轮询，离开/隐藏终止请求，禁止重叠与过期响应覆盖。

## 完整验证

为专用库 `campus_takeaway_test` 准备 MySQL 权限。迁移测试额外创建并清理一个随机命名的 `campus_takeaway_migration_*` 库，账号需要 CREATE/DROP DATABASE 权限；这些权限只用于隔离测试。不能把 TEST_DB_URL 指向业务库。

PowerShell：

```powershell
$env:RUN_MYSQL_TESTS='true'
$env:TEST_DB_PASSWORD='你的隔离测试库密码'
# 默认 TEST_DB_URL 为 127.0.0.1:13317/campus_takeaway_test，TEST_DB_USERNAME=root
# 默认 TEST_REDIS_PORT=16377
mvn verify
npm test --prefix web
npm run build --prefix web
```

Linux CI 同样显式设置 `RUN_MYSQL_TESTS=true`，MySQL 8 + Redis 7 为真实服务。报告和 JAR 作为 Actions artifact 保存。浏览器脚本使用隔离演示库与浏览器，见 [验证记录](docs/verification.md)。

## 数据升级与回退

**不要直接把旧生产数据库接入新版试运行。** 本次按用户选择只升级代码，未连接或修改原项目数据库。先备份，再导入独立演练库，最后按编号迁移。旧 ID、BCrypt、订单、快照、金额、事件保留；旧 Session 购物车无法批量迁移，需要重新选餐。

Flyway 新库执行 V1 + V2；旧库在备份演练后显式设置 `FLYWAY_BASELINE_ON_MIGRATE=true`，记录 V1 基线并执行 V2，成功后关闭此开关。MySQL DDL 不具备完整事务回滚；务必先在副本验证。

- [迁移与回退手册](docs/MIGRATION.md)
- [接口与错误契约](docs/API.md)
- [数据字典](docs/DATA_DICTIONARY.md)
- [架构与状态图](docs/architecture.md)
- [真实测试与测量记录](docs/verification.md)
- [面试讲解](docs/INTERVIEW.md)
- [负责模块说明](docs/PORTFOLIO.md)

不接入真实支付、消息队列、地图、推荐算法或微服务。不存在真实运营规模或未经测量的 QPS 声明。
