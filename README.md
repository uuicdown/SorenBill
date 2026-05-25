# Soren 的账单

> *"我是 Soren，您的私人虚拟管家。每天帮您记录经济情况，为您做出合适的储蓄规划。"*

SorenBill 是一款极简、优雅的 Android 个人记账应用。它不仅能手动快速记账，还能通过无障碍服务**自动识别微信/支付宝支付成功页面**，帮您无感记录每一笔花销。

---

## 功能一览

| 模块 | 说明 |
|---|---|
| 📊 **流水** | 按月展示收入/花销/结余，点击条目查看完整详情 |
| 📅 **日历** | 月视图 + 每日支出预览，一目了然 |
| 💰 **资产** | 多钱包管理、账户分组、净资产统计 |
| 📈 **统计** | 分类饼图 + 占比分析，了解钱花在哪 |
| 🤵 **Soren 管家** | 深色/浅色模式切换 + 无障碍自动记账开关 |
| 🤖 **自动记账** | 后台监听微信/支付宝支付成功页，自动提取金额/商户/订单号，智能匹配分类后弹窗确认或静默入账 |

---

## 技术栈

| 层级 | 技术 |
|---|---|
| 语言 | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room + KSP |
| DI | Koin |
| 本地存储 | DataStore (Preferences) |
| 架构 | MVVM + Repository |
| 自动记账 | AccessibilityService + 自定义节点解析引擎 |

---

## 快速开始

```bash
# 克隆仓库
git clone https://github.com/uuicdown/SorenBill.git

# 用 Android Studio 打开项目，Sync Gradle 后即可 Run
./gradlew assembleDebug
```

---

## 项目结构

```
app/src/main/java/com/soren/bill/
├── BillApplication.kt          # Koin 启动入口
├── MainActivity.kt             # 单 Activity + 全局确认弹窗
├── data/
│   ├── entity/                 # Room 实体 (Wallet, Account, Category, Transaction)
│   ├── dao/                    # DAO 接口
│   ├── database/               # BillDatabase + Migration
│   ├── repository/             # BillRepository (数据仓库)
│   └── preferences/            # AppPreferences (DataStore 主题/记账设置)
├── di/
│   └── AppModule.kt            # Koin 依赖注入模块
├── ui/
│   ├── theme/                  # Color.kt, Theme.kt, DesignSystem
│   ├── navigation/             # AppNavigation (底部导航 + 路由)
│   ├── home/                   # 首页：月度汇总 + 流水列表
│   ├── calendar/               # 日历页
│   ├── assets/                 # 资产页
│   ├── stats/                  # 统计页
│   ├── profile/                # Soren 页（设置 + CRUD 管理）
│   └── add/                    # 记账页
├── service/
│   ├── AutoAccountingAccessibilityService.kt  # 无障碍服务主控
│   ├── PaymentScreenParser.kt                 # 支付页面节点解析引擎
│   ├── MerchantCategoryMapper.kt              # 商户→分类映射规则
│   └── PendingTransactionManager.kt           # 服务→UI 通信管道
└── util/
    └── DateUtils.kt            # 日期/金额格式化工具
```

---

## 自动记账原理

```
用户扫码付款 → 微信/支付宝弹出"支付成功"
        ↓
AccessibilityService 捕获 TYPE_WINDOW_CONTENT_CHANGED
        ↓
PaymentScreenParser BFS 遍历节点树 → 提取金额/商户/订单号
        ↓
MerchantCategoryMapper 匹配分类（如"星巴克"→餐饮）
        ↓
   ┌──────────────┬──────────────────┐
   │ 确认模式(ON) │  静默模式(OFF)    │
   │ 发送通知     │  直接写入 Room    │
   │ → 用户弹窗确认│                  │
   └──────────────┴──────────────────┘
```

---

## 设计理念

- **极简扁平化** (Flat Design)：0 阴影、0 投影，靠留白和色彩区分层级
- **Soren 管家人格**：所有 UI 文案以温柔的私人管家口吻呈现，不是冷冰冰的工具
- **双主题**：清爽白天 / 静谧夜晚，支持跟随系统自动切换

---

## License

MIT
