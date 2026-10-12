# 当前系统架构

```mermaid
flowchart LR
    U[学生 / 商家 / 骑手 / 管理员] --> V[Thymeleaf 页面 / 原生 JS]
    V --> A[Spring Security / Session / CSRF]
    A --> C[Spring MVC 控制器]
    C --> S[下单 / 订单状态 / 账号 / 地址服务]
    S --> M[MyBatis / 条件更新 / 事务]
    M --> DB[(MySQL / 订单与地址快照 / 事件记录)]
```

图中节点对应当前仓库代码；虚线表示可选链路，不代表已完成外部生产验收。具体取舍见[负责模块与技术决策](PORTFOLIO.md)。
