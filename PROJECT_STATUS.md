# Soren的账单 — 项目总览

> 最后更新：2026-05-13  
> 状态：Phase 1 开发中，构建系统已对齐，待验证编译

---

## 一、核心需求

一款 Android 个人记账应用，模仿参考图的多 tab 布局，核心目标：**看清钱的流向**。

| # | 决策 | 结论 |
|---|---|---|
| 1 | 使用者 | 个人自用 |
| 2 | 核心目的 | 记录收支，不做预算 |
| 3 | 收支 | 双栏都记（收入 + 支出） |
| 4 | 字段 | 金额 + 收支类型 + 分类 + 日期（精确到分钟） + 备注(可选) + 支付账户 |
| 5 | 支付账户 | 自由增删，预置微信/支付宝/银行卡/信用卡/现金 |
| 6 | 分类 | 收支各一套，自由增删 |
| 7 | 钱包 | 钱包 ≠ 支付账户（独立维度），自由创建，币种字段预留（Phase 1 仅 CNY） |
| 8 | 首页 | 月份标题 + 三栏汇总（收入/支出/结余） + 空状态 + 流水列表 |
| 9 | 快捷记账 | 金额优先输入，记住上次选择，日期时间可修改 |
| 10 | 导航 | 四 tab：主页 · 日历 · 资产 · 统计 |
| 11 | 存储 | 纯本地 Room，后续加云端备份 |
| 12 | 资产页 | 净资产卡片 + 分组（储蓄卡/信用卡/网络支付/应付）+ 信用卡还款日 |
| 13 | 日历页 | 月视图日历网格 + 今日高亮 + 日预算标注 |
| 14 | 统计页 | 大数字 + 饼图 + 折线图 |
| 15 | 筛选 | 高级多条件（日期/分类/类型/账户） |
| 16 | 编辑删除 | 支持 |
| 17 | 主题 | 简约现代，Phase 1 唯一主题 |
| 18 | app 名 | Soren的账单 |
| 19 | 周期记账 | Phase 1 不做 |

---

## 二、技术栈

| 层面 | 选型 |
|---|---|
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room 2.7.1 + KSP |
| 架构 | MVVM + Repository |
| 构建 | AGP 9.2.1, Gradle 8.13 |
| 最低 SDK | 26 (Android 8.0) |

---

## 三、项目结构

```
SorenBill/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    └── src/main/java/com/soren/bill/
        ├── BillApplication.kt
        ├── MainActivity.kt
        ├── data/
        │   ├── entity/
        │   │   ├── Wallet.kt
        │   │   ├── Account.kt        (+ creditLimit, paymentDueDay, isHidden)
        │   │   ├── Category.kt
        │   │   └── Transaction.kt
        │   ├── dao/
        │   │   ├── WalletDao.kt
        │   │   ├── AccountDao.kt
        │   │   ├── CategoryDao.kt
        │   │   └── TransactionDao.kt  (+ getAccountIncome/Expense)
        │   ├── database/BillDatabase.kt (v2, seed defaults)
        │   └── repository/BillRepository.kt
        ├── ui/
        │   ├── theme/  (Color / Type / Theme — 简约现代)
        │   ├── navigation/AppNavigation.kt  (4 tab)
        │   ├── home/    (HomeScreen + HomeViewModel)
        │   ├── calendar/ (CalendarScreen)
        │   ├── assets/  (AssetsScreen + AssetsViewModel)
        │   ├── stats/   (StatsScreen + StatsViewModel)
        │   ├── profile/ (ProfileScreen + ProfileViewModel — 分类/账户管理)
        │   └── add/     (AddTransactionScreen + AddTransactionViewModel)
        └── util/DateUtils.kt
```

---

## 四、已完成进度

### 数据层
- [x] 四个实体：Wallet, Account, Category, Transaction
- [x] 四个 DAO（含高级筛选、账户余额聚合查询）
- [x] Room Database v2 + 预设种子数据 + destructive migration
- [x] BillRepository 统一数据访问

### UI 层
- [x] 简约现代主题（Color / Type / Theme）
- [x] **主页**：月份标题 + 日期图标 + 三栏汇总 + 空状态 + 流水列表（显示分类+备注）
- [x] **日历页**：月视图日历网格 + 左右翻月 + 日预算标注
- [x] **资产页**：净资产卡片 + 四组账户（储蓄卡/信用卡含还款日/网络支付/应付）
- [x] **统计页**：大数字 + Canvas 饼图
- [x] **记账页**：金额优先输入 + 收支切换 + 分类选择 + 支付账户 + 钱包 + DatePicker + TimePicker + 备注
- [x] **我的页**：钱包/支付账户/分类的 CRUD 管理
- [x] 四 tab 底部导航

### 构建系统
- [x] AGP 9.2.1 + Kotlin 2.2.10 + KSP 2.2.10-2.0.1
- [x] Room 2.7.1 + Compose BOM 2025.05.00
- [x] Gradle 镜像（腾讯云）
- [x] 所有 `@Delete` 替换为 `@Query("DELETE FROM ...")` 规避 KSP 兼容 bug

---

## 五、待办项

### Phase 1 — 编译 & 运行
- [ ] **验证编译通过**：AS Clean + Rebuild，确认 Room kapt 生成代码无报错
- [ ] **真机/模拟器运行**：四个 tab 可点击切换，首页显示空状态
- [ ] **添加一笔记录**：验证记账页保存后首页列表+汇总刷新
- [ ] **日历页数据联动**：日历日显示该日支出金额

### Phase 1 — 完善
- [ ] 高级筛选页面（多条件：日期范围/分类/类型/账户）
- [ ] 统计页折线图（月趋势）
- [ ] CSV 导出
- [ ] 每笔记录显示所属账户图标

### Phase 2 — 自动化
- [ ] 自动记账（AccessibilityService + NotificationListener）
- [ ] 商户→分类规则表
- [ ] 付款成功页悬浮窗审核

### Phase 3 — 高级
- [ ] 多币种支持（汇率换算）
- [ ] 云端备份（Git 仓库自动推送）
- [ ] 预算管理
- [ ] 桌面小组件

---

## 六、已安装 Skill

| Skill | 用途 |
|---|---|
| grill-me | 决策树盘问，厘清需求 |
| handoff | 会话交接文档，跨 session 连续工作 |
| caveman | 极简通讯模式，token 压缩 ~75% |
| diagnose | 六阶段结构调试方法论 |

---

## 七、下一步

1. AS 里 **Clean Project → Rebuild Project**，确认编译通过
2. 连接设备或启动模拟器，Run
3. 测试四个 tab 切换、添加记录、删除记录
4. 根据实际运行效果微调 UI 细节
