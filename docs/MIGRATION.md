# 升级、数据保留与回退

## 本次边界

按用户选择，本次只升级代码，未读取/修改原项目数据库。Windows 上另启独立 MySQL 8.0.44（127.0.0.1:13317）和 Redis（16377），测试、演示、回退副本均使用独立库；未修改原 MySQL 服务的配置。源码/图片/原未提交修改和 Git bundle 另有工作区外备份，未加入 GitHub。

## 正式切换前

1. 停止业务写入，记录旧代码提交、MySQL 版本、schema、各表行数与订单金额/快照/事件摘要；备份数据库、图片目录及旧 JAR。备份中包含个人资料和密码哈希，放在受限目录，不提交仓库。
2. `mysqldump --single-transaction --routines --triggers --hex-blob --default-character-set=utf8mb4 --result-file=before.sql campus_takeaway`。使用环境 MYSQL_PWD/安全配置提供凭据，不把密码写进命令或仓库。备份后记录 SHA-256。
3. 在独立库导入备份，绝不覆盖原库；对照 V1 字段核对真实 schema。与基线不同的库应先调整迁移并重新演练，不盲目开启 baseline。
4. 新库由 Flyway 执行 V1 + V2。已有 V1 旧库只在演练后显式使用 `FLYWAY_BASELINE_ON_MIGRATE=true`，把现存原结构记为 V1，再执行 V2。成功后关闭开关，检查 flyway_schema_history。
5. 对照迁移前后的用户 ID/BCrypt、订单、商品/地址快照、金额和事件；查看升级字段和索引，不给历史订单伪造 requestKey。原 UNPAID 订单按原 created_at 补 15 分钟 expires_at，启动补偿可能立即取消已超时订单，须在演练中确认。
6. Redis 切换到专有 takeaway: 命名空间，用户需重新登录。旧 JVM Session 购物车不能可靠批量导出，**需要重新选餐**；已有订单保留。不能把这部分购物车称为已迁移。
7. 在副本运行测试与四角色闭环，再停写备份、升级实际目标并核对。HTTPS 设置 COOKIE_SECURE=true。本次没有执行原业务库切换。

V2 的 ALTER 为 MySQL DDL，不能保证整个脚本失败时自动回滚。部分失败时保留现场并在新副本恢复备份定位，不对原库反复执行、不盲目 Flyway repair、不删除历史表。

## 回退原则

代码回退优先连接**保留全部新版数据的 V2 schema 副本**。旧代码查询/写入原字段，V2 新增列可空或有默认值，额外 cart 表不影响旧查询。先在隔离副本验证旧 JAR，再在停写窗口决定是否切换；不要直接恢复旧时刻备份覆盖新版订单。

切换后必须再完整导出 users/shops/dishes/addresses/orders/order_items/order_events/cart/cart_item 与 Flyway 元数据，保留新的用户、订单、快照、事件、幂等字段和购物车，并备份图片。这是回退的最新数据基线，不能只保留切换前备份。

旧版不认识数据库购物车，因此旧 UI 无法展示新版待结算 cart；保留 cart/cart_item 导出及原 V2 数据库。由用户重新选餐，恢复新版时可继续读取保留下来的数据库 cart。旧版新建订单没有新版幂等/超时字段；再次升级前核对并补齐新产生的未支付订单期限，不能宣称旧版仍有新版跨实例结算保证。

不执行破坏性降级 SQL、不删除 V2 列/表，不把数据库恢复操作与代码回退混为一谈。

## 已做的隔离演练

- `MigrationTest` 创建随机命名的独立库，执行 V1、植入旧 ID/BCrypt/订单/明细/事件，再执行 V2；逐表按旧列比对内容与行数，确认历史 requestKey=NULL、authVersion=0，旧密码仍匹配。最后仅清理该临时库。
- 浏览器在新建演示库产生新版订单并完成收货、再来一单。对演示库做完整 mysqldump 并导入 `campus_takeaway_rollback_test`。
- 从原交付包提取既有 1.0.0 JAR，在随机本地端口启动，连接上述副本。通过旧版 CSRF 表单登录并进入订单页，能读取新版已完成订单。
- `scripts/rollback-smoke.py` 在旧版读取前后对 9 张业务表计算行数和 SHA-256；全部一致，含 2 个订单、2 行明细、8 个事件和 1 个数据库购物车/明细。结果见 [rollback-results.json](rollback-results.json)。这证明该演示数据副本的兼容读取，不代表原业务库已经完成切换。

复现旧 JAR 检查（先准备指定副本与旧 JAR 服务，禁止指向原业务库）：

```powershell
$env:LEGACY_URL='http://127.0.0.1:旧JAR随机端口'
$env:REHEARSAL_DB_PASSWORD='隔离实例密码'
python scripts/rollback-smoke.py
```
