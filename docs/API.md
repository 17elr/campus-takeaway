# JSON API 契约

所有业务路径以 `/api` 开头；成功返回 JSON 和 200，创建订单重放也返回 200 和原订单。写接口不以 302 冒充成功。请求 Content-Type 为 application/json；未知 JSON 字段、浮点数数量等非法输入返回 400。

| 接口 | 角色 / 行为 |
| --- | --- |
| GET /api/auth/csrf | 公开；创建匿名 Redis Session，返回 `{token}` |
| POST /api/auth/login | 公开但需要 CSRF；username/password，成功更换会话编号和 CSRF token，返回 user/token |
| POST /api/auth/register | 公开但需要 CSRF；username/password/realName/phone，只创建学生 |
| GET /api/auth/me | 已登录；可信当前用户，无密码字段 |
| POST /api/auth/logout | 已登录 + CSRF；使会话失效 |
| GET /api/shops | 公开；只返回正在营业且商家启用的店铺，支持 query/page/size |
| GET /api/shops/{id}/menu | 公开；`{shop,dishes:{records,total,page,size}}`，query 同时匹配名称/分类 |
| GET /api/student/cart | 学生；shopId/version/checkoutKey/items/total |
| PUT /api/student/cart/items/{dishId} | 学生；`{quantity:1..99}` 设定绝对数量 |
| DELETE /api/student/cart/items/{dishId} | 学生；移除一项 |
| DELETE /api/student/cart | 学生；清空，版本和凭证均更新 |
| POST /api/student/orders | 学生；仅 checkoutKey/cartVersion/addressId，原订单重放或事务创建 |
| POST /api/student/orders/{id}/reorder | 本人学生订单；当前菜品全部可购买后替换购物车 |
| GET /api/student/addresses | 学生；records 和允许的 buildings |
| POST /api/student/addresses | 学生；building/room/contactName/phone |
| PUT /api/student/addresses/{id} | 本人；编辑上述字段 |
| DELETE /api/student/addresses/{id} | 本人；删除并补选默认地址 |
| POST /api/student/addresses/{id}/default | 本人；设置唯一默认地址 |
| GET /api/orders | 已登录；按可信角色限定归属，再过滤分页；scope=mine/available，available 仅影响骑手 |
| GET /api/orders/{id} | 角色 + 归属；order/events/paymentMode=SIMULATED |
| POST /api/orders/{id}/pay、cancel、confirm | 本人学生订单；固定状态动作，pay 为模拟 |
| POST /api/orders/{id}/accept、ready | 自有店铺商家 |
| POST /api/orders/{id}/claim、delivered | 骑手；claim 仅 READY 未领取，delivered 仅本人 |
| GET /api/account、PUT /api/account | 共用；读取本人/更新 realName、phone |
| POST /api/account/password | currentPassword/newPassword/confirmPassword；BCrypt 校验，递增版本并注销当前 Session |
| GET /api/merchant/shop、PUT /api/merchant/shop | 自有店铺；name/description/open |
| GET /api/merchant/dishes | 自有菜品分页 |
| POST /api/merchant/dishes、PUT /api/merchant/dishes/{id} | category/name/price |
| POST /api/merchant/dishes/{id}/toggle | 自有菜品上下架 |
| GET /api/admin/users、shops、dishes | 管理员；query/page/size 分页 |
| POST /api/admin/users | 仅允许 role=MERCHANT/RIDER；username/password/realName/phone/role |
| PUT /api/admin/users/{id}/enabled | `{enabled}`；禁止停用管理员；递增 authVersion |
| PUT /api/admin/shops/{id}/open | `{open}`；营业状态维护 |

分页统一 `records,total,page,size`，page 为 1 起，size 为 1–100；稳定排序固定为店铺 id 升序、其他列表 id 降序，不接受客户端任意 SQL 排序。先数据库筛选、再 LIMIT/OFFSET。订单本页明细只执行一次 IN 查询。

所有写请求都需要 `X-CSRF-Token`。登录后须使用新 token，Cookie 由浏览器同源携带。X-User-Id/X-User-Role 不参与认证。Cookie 为 HttpOnly / SameSite=Lax，HTTPS 用 COOKIE_SECURE=true。

错误体为 `{message,fields?}`：400 输入错误、401 会话失效、403 角色/归属/CSRF 不允许、404 资源不存在、409 状态/版本/同凭证参数冲突、503 服务不可用。字段校验错误附 fields，前端保留输入。Redis Session 失败关闭；公共菜单缓存失败可回源。

前端路由 fallback 明确枚举，不吞 `/api` 和图片 404。`/student/cart` 等刷新返回 Vue，未知页面路径和不存在图片仍为真实 404。
