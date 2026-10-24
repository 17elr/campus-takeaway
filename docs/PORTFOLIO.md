# 负责模块与技术取舍

本次升级范围是前后端分离与订单一致性，不是仅替换 README 技术栈。2026-10-24 在原 Java Web 基础上新增 Vue/Redis，保留原四角色功能。AI 辅助开发与排错的贡献说明沿用原项目，不把工具生成代码描述为独立手写。

| 模块 | 代码入口 | 可以讲解的实际机制 |
| --- | --- | --- |
| 四角色交互 | web/src/views、controller | Vue 路由、表单、分页与固定订单动作 |
| 登录 | auth、config/SecurityConfig | Session ID 轮换、CSRF、可信当前用户、认证版本；仅 BCrypt 使用密码库 |
| 购物车/结算 | cart、service/OrderService | 数据库用户锁、版本、唯一键、请求指纹与事务 |
| 菜单缓存 | cache/MenuCache、CatalogService | Redis TTL、负缓存、提交后失效、故障回源 |
| 超时与配送 | OrderExpiry、OrderService | 条件转换、事件回滚、抢单单赢家 |
| 验证 | src/test、scripts/browser-smoke.mjs | 真实 MySQL/Redis、双应用实例、浏览器闭环 |

不使用支付网关、MQ、地图和微服务。没有真实营收、订单规模或臆造性能数据。当前能力与限制见 [verification.md](verification.md)，讲解问答见 [INTERVIEW.md](INTERVIEW.md)。
