# 负责模块与关键技术决策

## 个人负责范围

本人主导需求、系统设计、Java 服务、页面、数据库、测试与交付文档，使用 AI 辅助编码和排错；贡献方式经本人确认。

| 负责模块 | 代码入口 | 讲解重点 |
| --- | --- | --- |
| 四角色业务闭环 | [controller](../src/main/java/cn/campus/takeaway/controller/)、[templates](../src/main/resources/templates/) | 学生下单、商家制作、骑手配送、管理员维护 |
| 订单一致性 | [OrderService](../src/main/java/cn/campus/takeaway/service/OrderService.java) | 金额重算、快照、下单令牌、事务与条件抢单 |
| 账号与地址 | [service](../src/main/java/cn/campus/takeaway/service/)、[config](../src/main/java/cn/campus/takeaway/config/) | 角色隔离、密码存储、CSRF、本人数据校验 |
| 验证与交付 | [测试](../src/test/java/cn/campus/takeaway/)、[数据库](../src/main/resources/db/)、[脚本](../scripts/) | 独立测试库、回滚检查、可执行 JAR |

## 技术决策

| 决策 | 原因与代价 |
| --- | --- |
| 单体 Spring Boot + Thymeleaf | 四类角色共用一个 JAR，便于课程/本地演示；不宣称微服务架构。 |
| 服务端 BigDecimal 重算订单金额 | 不信任浏览器金额，按当前可售菜品计价。 |
| 商品价格与地址快照 | 历史订单不随改价或地址编辑变化。 |
| 事务 + 条件 UPDATE | 原子提交订单与事件，抢单以状态条件避免重复接取。 |
| Session + Spring Security + CSRF | 用现有安全框架实现网页认证，保留表单写操作防护。 |
| 专用 MySQL 测试库 | 测试前校验库名，防止回归检查清理业务数据。 |

## 演示与验证

- [架构与业务流程](architecture.md)、[首页架构图](architecture-overview.md)。
- [演示截图来源](images/SOURCES.md)：复用既有本地验收图片，页面中的支付与订单均为演示。
- [已有验证记录](verification.md)、[GitHub Actions](../.github/workflows/ci.yml)：CI 显式开启 MySQL 集成测试。

## 简历表述

主导 Spring Boot 校园订餐与配送系统开发，贯通学生、商家、骑手、管理员四角色流程；通过服务端金额重算、商品与地址快照、事务和条件更新，保障订单计价与状态流转，防止重复提交和重复抢单；基于 Spring Security 实现角色授权及 CSRF 防护。

本项目没有真实支付、地图定位、Redis、MQ 或微服务；不将演示数据写成真实运营成果。
