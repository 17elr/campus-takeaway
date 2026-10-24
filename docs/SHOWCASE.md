# 界面图册

[返回项目首页](../README.md) · [文档中心](README.md)

以下 15 张图片来自 Edge 真实浏览器和独立演示数据库，页面宽度覆盖 375、768、1440 像素。账号、电话号码和订单均为演示数据。点击图片可查看原尺寸，[运行记录](screenshots/browser-results.json)对应学生下单、商家制作、骑手配送到确认收货的实际流程。

## 学生端

| 发现店铺 | 菜单与购物车 |
| --- | --- |
| [![发现校园店铺](screenshots/shops-desktop.png)](screenshots/shops-desktop.png) | [![店铺菜单与购物车](screenshots/student-menu.png)](screenshots/student-menu.png) |

| 核对餐品与收货地址 | 完成订单与事件时间线 |
| --- | --- |
| [![购物车结算](screenshots/student-cart.png)](screenshots/student-cart.png) | [![订单完成与时间线](screenshots/order-completed.png)](screenshots/order-completed.png) |

| 校内地址 | 账号资料与改密 |
| --- | --- |
| [![收货地址管理](screenshots/student-addresses.png)](screenshots/student-addresses.png) | [![账号资料](screenshots/account.png)](screenshots/account.png) |

## 商家端

[![商家订单工作台](screenshots/merchant-orders.png)](screenshots/merchant-orders.png)

| 菜品分类、现价与上下架 | 店铺名称、简介与营业状态 |
| --- | --- |
| [![菜品管理](screenshots/merchant-dishes.png)](screenshots/merchant-dishes.png) | [![店铺设置](screenshots/merchant-shop.png)](screenshots/merchant-shop.png) |

## 骑手与管理员

| 骑手 · 待配送大厅 | 管理员 · 用户管理 |
| --- | --- |
| [![骑手领取配送](screenshots/rider-available.png)](screenshots/rider-available.png) | [![管理员用户列表](screenshots/admin-users.png)](screenshots/admin-users.png) |

## 移动端与平板

| 375px · 学生发现页 | 375px · 骑手配送大厅 | 375px · 管理员列表 |
| --- | --- | --- |
| [![移动端发现页](screenshots/shops-mobile.png)](screenshots/shops-mobile.png) | [![移动端配送大厅](screenshots/rider-mobile.png)](screenshots/rider-mobile.png) | [![移动端用户管理](screenshots/admin-mobile.png)](screenshots/admin-mobile.png) |

<details>
<summary>展开 768px 平板布局</summary>

[![平板发现店铺](screenshots/shops-tablet.png)](screenshots/shops-tablet.png)

</details>

## 复现截图

截图未拼接或改写页面数据。运行 `scripts/browser-smoke.mjs` 可在隔离演示库重跑真实订单闭环并生成图片；环境、CDP 与命令说明见[验证报告](verification.md#截图与复现)。照片授权与出处保留在 [credits.html](../src/main/resources/static/images/credits.html)。
