# 数据字典与索引

V1 为原表结构，V2 为增量迁移。`flyway_schema_history` 记录迁移编号、校验和及结果；应用不再每次启动执行 schema.sql。

| 表 | 关键字段 / 约束 |
| --- | --- |
| users | id、唯一 username、BCrypt password_hash、real_name/phone、role 四值、enabled、auth_version 默认 0 |
| shops | 唯一 owner_id 关联商家；name/description/is_open |
| dishes | shop_id、category/name、DECIMAL(10,2) price、on_sale、image_path |
| addresses | user_id、白名单 building、room/contact_name/phone、is_default；用户行锁保证默认地址唯一 |
| cart | id、唯一 user_id、可空 shop_id、version、checkout_key(UUID)、updated_at |
| cart_item | 复合主键 cart_id+dish_id；quantity 数据库 CHECK 1–99；外键关联 cart、dishes |
| orders | 唯一 order_no；student_id/shop_id/rider_id；金额与地址快照；固定 status；各阶段时间 |
| orders 新增 | request_key、checkout_cart_version、checkout_address_id、request_hash(SHA-256)、expires_at；历史订单幂等字段可空 |
| order_items | order_id/dish_id、下单时 dish_name/unit_price/quantity/subtotal 快照 |
| order_events | order_id/from_status/to_status/operator_id/created_at；reason 可为 PAYMENT_TIMEOUT |

订单新增 `(student_id,request_key)` 唯一约束，MySQL 允许历史 NULL；`(status,expires_at,id)` 支持超时扫描，`(student_id,id)`、`(shop_id,id)`、`(rider_id,id)` 支持角色列表。外键与原约束保留。

所有金额使用 BigDecimal/DECIMAL，成交价从数据库读取；请求不接受客户端 amount。request_hash 针对规范化的 checkoutKey、十进制 cartVersion、addressId 计算，不依赖重放时已被清空的购物车。

Session namespace 为 `takeaway:session`；属性仅用户 ID、authVersion、CSRF token，无购物车对象、密码或自报角色。菜单 key 为 `takeaway:menu:v1:{shopId}`，缓存明确 Menu/ShopInfo/PageResult DTO；订单、地址、权限和最终成交金额均不缓存。
