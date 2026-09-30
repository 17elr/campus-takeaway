# 系统架构

## 总体架构

```mermaid
flowchart LR
    U[浏览器\n学生 / 商家 / 骑手 / 管理员]
    W[Spring Boot Web\n内嵌 Tomcat]
    S[Spring Security\n登录 / 会话 / 角色权限 / CSRF]
    C[Spring MVC Controllers\n页面与表单请求]
    B[业务服务层\nAccountService / AddressService / OrderService]
    M[MyBatis Mapper\nAppMapper]
    D[(MySQL 8\nInnoDB)]
    V[Thymeleaf + HTML/CSS/原生 JS\n响应式页面]

    U --> W
    W --> S
    S --> C
    C --> B
    B --> M
    M --> D
    C --> V
    V --> U
```

项目采用单体 Java Web 架构，不拆分微服务，不依赖 Redis、消息队列、地图或真实支付服务。

## 模块划分

| 模块 | 主要代码 | 职责 |
| --- | --- | --- |
| 页面层 | `src/main/resources/templates/`、`static/` | 服务端渲染页面、样式、交互和表单 |
| 控制器层 | `controller/` | 路由、角色入口、参数接收和页面跳转 |
| 业务层 | `service/` | 下单、支付、状态流转、地址和账户规则 |
| 安全层 | `config/SecurityConfig.java`、`ActiveAccountFilter.java` | 登录、密码编码、角色授权、CSRF、停用账号失效 |
| 数据访问层 | `mapper/AppMapper.java` | MyBatis SQL 查询、插入、更新和条件抢单 |
| 领域模型 | `domain/` | 用户、店铺、菜品、购物车、订单和状态对象 |
| 数据库层 | `src/main/resources/db/schema.sql` | 表结构、外键、状态和数量约束 |

## 核心业务流

```mermaid
sequenceDiagram
    participant Student as 学生
    participant App as Spring Boot 应用
    participant Merchant as 商家
    participant Rider as 骑手
    participant DB as MySQL

    Student->>App: 浏览菜品、加入购物车、提交订单
    App->>DB: 校验店铺/菜品并按现价计算金额
    App->>DB: 保存订单、明细和地址快照
    Student->>App: 模拟支付
    App->>DB: UNPAID -> PENDING_ACCEPT
    Merchant->>App: 接单并标记制作完成
    App->>DB: PENDING_ACCEPT -> MAKING -> READY
    Rider->>App: 条件抢单并标记送达
    App->>DB: READY -> DELIVERING -> WAIT_CONFIRM
    Student->>App: 确认收货
    App->>DB: WAIT_CONFIRM -> COMPLETED
```

## 数据关系

```mermaid
erDiagram
    USERS ||--o| SHOPS : owns
    USERS ||--o{ ADDRESSES : has
    USERS ||--o{ ORDERS : places
    USERS ||--o{ ORDERS : delivers
    USERS ||--o{ ORDER_EVENTS : operates
    SHOPS ||--o{ DISHES : contains
    SHOPS ||--o{ ORDERS : receives
    ORDERS ||--|{ ORDER_ITEMS : contains
    ORDERS ||--o{ ORDER_EVENTS : records
    DISHES ||--o{ ORDER_ITEMS : snapshots

    USERS {
        bigint id PK
        varchar username UK
        varchar role
        boolean enabled
    }
    SHOPS {
        bigint id PK
        bigint owner_id FK
        boolean is_open
    }
    DISHES {
        bigint id PK
        bigint shop_id FK
        decimal price
        boolean on_sale
    }
    ADDRESSES {
        bigint id PK
        bigint user_id FK
        boolean is_default
    }
    ORDERS {
        bigint id PK
        bigint student_id FK
        bigint shop_id FK
        bigint rider_id FK
        decimal amount
        varchar status
    }
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint dish_id FK
        decimal unit_price
        int quantity
    }
    ORDER_EVENTS {
        bigint id PK
        bigint order_id FK
        bigint operator_id FK
        varchar to_status
    }
```

## 设计要点

- 订单金额由服务端重新计算，不信任浏览器提交的金额。
- 订单明细保存菜品名称、单价和地址快照，避免历史订单随资料修改变化。
- 订单写入和状态变更使用事务；骑手抢单使用带状态条件的更新，保证同一订单只被一人接取。
- 页面支付为模拟支付，不连接真实支付渠道。
