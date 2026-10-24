# 2026-10-24 Vue / Redis 升级验证

## 实际环境与结果

本地：Windows，Microsoft Java 17.0.18，MySQL 8.0.44，独立 Redis 5.0.14，Node 24.15.0，Edge 155.0.4283.62。Compose/CI 使用 MySQL 8.0 与 Redis 7.4；不把本地 Redis 5 的结果说成已测试 Redis 7。

| 检查 | 结果 |
| --- | --- |
| `RUN_MYSQL_TESTS=true mvn verify` | 36 项，0 失败、0 错误、0 跳过：9 单元 + 26 真实服务集成 + 1 独立库迁移 |
| `npm test --prefix web` | 4 项，全部通过；Fetch Cookie/CSRF/错误处理与轮询并发/中止 |
| `npm run build --prefix web` | 通过，Vue dist 可打包进 JAR |
| Edge 真实浏览器 | 完整订单闭环与四角色页面，16 条检查，15 张截图；脚本异常 0 |
| 旧 JAR 隔离回退读取 | CSRF 登录、新订单可见，9 张业务表读取前后摘要一致 |

机器可读结果：[Java 测试](test-results.json)、[浏览器](screenshots/browser-results.json)、[回退](rollback-results.json)。GitHub Actions 在 push/PR 时运行同一批 Java 测试、Vue 检查、Compose 校验和 Docker 构建，实际状态以仓库 Actions 为准。

## 覆盖的业务与故障

保留原订单闭环、服务端金额、快照、角色/归属、默认地址、账号资料/改密、图片、示例菜单可重复导入与再来一单等测试语义；不通过删除失败用例完成迁移。

- 两个真实 Tomcat 应用实例，独立端口、同一 MySQL/Redis。跨实例 Cookie 共享登录；同 key 并发结算仅 1 单，同 key 改版本/地址 409，响应重放返回旧订单且不清新购物车。
- 真实 HTTP 登录的 Session ID/CSRF 轮换、四角色授权、伪造用户头无效、注册不能提权、CSRF 缺失拒绝、停用/改密使旧 Session 失效。
- 数量 1/99/0/100/负值/浮点值，跨店、休息店铺、下架菜品、别人的地址与订单；失败不清购物车。
- 购物车修改与结算并发；条件抢单仅一赢家；MySQL trigger 注入事件失败，验证订单/明细/状态/cart 回滚。
- 15 分钟超时条件更新、重复扫描不重复事件、真实新应用启动补偿、超时与支付竞争、超时事件失败回滚。
- 真实 Redis 菜单缓存命中、改价/上下架/停业/停用商家提交后失效、负缓存；Redis 不可达时公共菜单查 MySQL，匿名/已登录会话入口均 503。
- 数据库筛选后分页，总数、末页、稳定顺序、一次 IN 明细批量查询；不输出密码哈希。
- UTF-8 显式导入示例 SQL，修复 Windows 默认编码导致的中文店名乱码。
- 真实浏览器：学生下单→模拟支付→商家接单/制作完成→骑手领取/送达→学生确认/再来一单；并检查地址、账号、商家菜品/设置、管理员各概览。

## 可以复核的缓存/SQL测量

相同测试库、同店铺、默认第一页，连续两次 HTTP 菜单请求：开启缓存触发 **1 次菜单分页 SQL**；关闭缓存连续读取触发 **2 次**。集成测试用真实 Redis 存取、Mapper spy 计数，两种配置分别核对；菜单 DTO 的 TTL 为 60–75 秒。订单每页主表 1 次 + count 1 次 + IN 明细 1 次，已验证不调用逐单明细方法。

以上是查询次数的定向验证；未进行吞吐、命中率长期采样、p95 或生产压测，不提供编造的 QPS/加速百分比。Vite 提示 Element Plus vendor chunk 较大（约 1 MB 原始、330 KB gzip），当前采用整库注册以保持实现简单，尚未做按组件拆包。

## 截图与复现

新截图位于 [screenshots](screenshots/)，由同一脚本生成，视口 375 / 768 / 1440。检查全部已加载图片 naturalWidth、页面无横向溢出、注册字段旁错误以及浏览器运行时异常。表格和移动导航容器可内部滚动。

启动独立演示实例（DEMO_DATA=true、全新演示库），再启动独立 Edge 浏览器并开启本地 CDP 端口：

```sh
# 浏览器示例参数：--headless=new --remote-debugging-port=19333 --user-data-dir=一个独立临时目录
node scripts/browser-smoke.mjs
```

默认 BASE_URL=http://127.0.0.1:8080，CDP_URL=http://127.0.0.1:19333；可用同名环境变量覆盖。脚本只用演示账号，会产生模拟订单，应只指向隔离演示库。使用浏览器 CDP，不依赖 Playwright 或外部 API Key。

## 尚未执行的范围

原业务库迁移和正式环境切换按用户选择未执行；原数据未改动。Docker 本机不可用，容器构建与 Redis 7 校验交由 GitHub Actions，不能把未完成的远端构建写成本地已通过。未接真实支付、未做生产负载测试。历史 `docs/images` 是原版存档，不作为本次 Vue 截图。
