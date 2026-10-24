# 启动与开发

[返回项目首页](../README.md) · [文档中心](README.md)

## Docker Compose 体验

```sh
git clone https://github.com/17elr/campus-takeaway.git
cd campus-takeaway
cp .env.example .env
```

编辑 `.env`，将 `DB_PASSWORD` 和 `MYSQL_ROOT_PASSWORD` 替换为本地密码。`DEMO_DATA=true` 只在空用户表中建立演示账号和菜单；已有用户的库不会被重新灌入。

```sh
docker compose up --build -d
```

打开 [http://127.0.0.1:8080](http://127.0.0.1:8080)。示例账号见[项目首页](../README.md#quick-start)。MySQL 和 Redis 都使用命名卷保存数据，停止使用 `docker compose stop`，保留数据卷。

## 环境与端口

| 组件 | 本地要求 / 地址 | 容器内地址 |
| --- | --- | --- |
| JDK | Java 17 | 应用镜像内置 Java 17 |
| 构建 | Maven 3.9、Node 24、npm | 多阶段镜像自动构建 |
| 后端 / JAR | 127.0.0.1:8080 | app:8080 |
| Vue 开发 | 127.0.0.1:18081 | 开发时使用 |
| MySQL | 127.0.0.1:13317 / campus_takeaway | mysql:3306 |
| Redis | 127.0.0.1:16377 | redis:6379 |

Compose 的宿主映射只绑定 127.0.0.1，不修改已有 MySQL 服务。

| 环境变量 | 含义 |
| --- | --- |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 本地后端的数据库连接；默认用户名 takeaway |
| `MYSQL_ROOT_PASSWORD` | Compose 初始化 MySQL 的 root 密码 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接；本地默认 127.0.0.1:16377 |
| `DEMO_DATA` | 默认 false；显式开启且用户表为空时创建演示数据 |
| `COOKIE_SECURE` | HTTPS 部署设置 true |
| `CACHE_ENABLED` | 默认 true；false 可用于缓存基线对比 |
| `PORT` | 后端端口，默认 8080 |

`.env` 由 Compose 读取。直接运行 Maven/JAR 时，在当前终端设置环境变量；Spring Boot 不会自动读取该 `.env` 文件。

## 本地开发

先启动依赖：

```sh
docker compose up -d mysql redis
```

PowerShell 示例：

```powershell
$env:DB_USERNAME='takeaway'
$env:DB_PASSWORD='填写 .env 中的应用数据库密码'
$env:DEMO_DATA='true'
mvn spring-boot:run
```

另开终端：

```sh
cd web
npm ci
npm run dev
```

访问 Vue 开发地址 [http://127.0.0.1:18081](http://127.0.0.1:18081)。`/api` 和 `/images` 代理到 8080，开发和生产均同源携带 Cookie。修改后端端口时同步修改 [Vite 代理](../web/vite.config.js)。

## 构建一个 JAR

```sh
npm ci --prefix web
npm run build --prefix web
mvn verify
java -jar target/campus-takeaway-2.0.0.jar
```

Maven 从 `web/dist` 复制页面，不会自动运行 npm。首次构建必须先生成 Vue dist。`mvn verify` 默认运行单元测试；完整真实服务测试需按下节显式启用。

## 完整验证

准备独立 `campus_takeaway_test` 库。迁移测试额外创建并清理随机命名的 `campus_takeaway_migration_*` 库，隔离测试账号需具备 CREATE/DROP DATABASE 权限。不得将 TEST_DB_URL 指向业务库。

```powershell
$env:RUN_MYSQL_TESTS='true'
$env:TEST_DB_USERNAME='root'
$env:TEST_DB_PASSWORD='隔离测试库密码'
# TEST_DB_URL 默认 127.0.0.1:13317/campus_takeaway_test
# TEST_REDIS_PORT 默认 16377
mvn verify
npm test --prefix web
npm run build --prefix web
```

GitHub Actions 显式启用同一批 MySQL/Redis 集成检查并保存报告和 JAR。浏览器闭环与回退演练见[验证报告](verification.md)。

## 连接已有数据前

先备份原库与图片，将备份导入独立库演练。Flyway 对新库执行 V1 + V2；已有 V1 旧库应核对 schema 后显式建立基线，再执行 V2。旧 JVM Session 购物车需要重新选餐，已有订单、快照与事件保留。

切换步骤、DDL 失败处理与数据回退说明见[迁移手册](MIGRATION.md)。公网部署前关闭示例数据、更改全部示例密码，并在 HTTPS 下启用 Secure Cookie。
