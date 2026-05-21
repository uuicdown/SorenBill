# Soren的账单 — Code Review

> 审查日期：2026-05-21  
> 范围：全项目

---

## 一、总体评价

**B+** — 功能完整，架构清晰，可上架。主要问题是代码冗余和少量技术债。

---

## 二、严重问题（建议修）

### 2.1 TransactionDao 重复方法

```kotlin
// 两个完全相同的方法，KSP 生成两份实现
fun getAll(): Flow<List<Transaction>>
fun getAllTransactions(): Flow<List<Transaction>>
```

**建议**：删掉 `getAll()`，只保留 `getAllTransactions()`。

### 2.2 `fallbackToDestructiveMigration` 风险

```kotlin
.fallbackToDestructiveMigration()
```

每次改 Entity 字段都会清空用户数据。上架后必须替换为正常 Migration。

**建议**：下个版本写 `Migration(2, 3) { }` 替代。

### 2.3 构建文件缺 `kotlin-android` 插件声明

```kotlin
plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")       // ← 有
    id("org.jetbrains.kotlin.plugin.compose")  // ← 有
    // 缺: id("org.jetbrains.kotlin.android")
}
```

`build.gradle.kts` 项目级声明了但 app 级漏了。碰巧 KSP 会隐式引入 Kotlin 插件所以能编译，但不规范。

**建议**：补上 `id("org.jetbrains.kotlin.android")`。

### 2.4 垃圾文件混入仓库

```
git
main
```

项目根目录有两个无扩展名文件，是之前 git 操作误创建的。

**建议**：`rm git main` 后提交。

---

## 三、架构问题（建议优化）

### 3.1 Repository 是纯透传

`BillRepository` 对所有 DAO 方法做了 1:1 转发，没有业务逻辑。

**建议**：删除 Repository，让 ViewModel 直接依赖 DAO；或者在 Repository 里合并逻辑（如 `getAccountBalances` 一次返回计算好的余额列表）。

### 3.2 ViewModel 用 `first()` 读 Flow

多处调用了 `repository.getXxx().first()`，会阻塞协程直到 Flow emit 第一个值。

**建议**：用 `combine` 或 `flatMapLatest` 保持响应式，避免手动拉取。

### 3.3 余额调整通过 note 字段识别

```kotlin
tx.note != "手动调整余额" && tx.note != "初始余额"
```

脆弱：用户可能自己写"手动调整余额"作为备注。

**建议**：给 Category 表加 `isAdjustment: Boolean` 字段，按 categoryId 过滤。

---

## 四、代码质量

### 4.1 好的实践 ✅

- MVVM 分层清晰
- StateFlow + collectAsState 响应式 UI
- 每个 ViewModel 有 Factory 类
- 设计规范集中在 DesignSystem.kt
- 27 个银行 PNG 图标本地化

### 4.2 待改进

| 问题 | 位置 | 建议 |
|---|---|---|
| Category 实体无 icon 字段 | `Category.kt` | 加 `iconName: String`，不再用外部映射 |
| `CalendarUiState.selectedDayTransactions` 未使用 | `CalendarViewModel` | 删除 |
| 日历日期详情过滤逻辑在 UI 层 | `CalendarScreen.kt` | 移到 ViewModel |
| `AssetsViewModel.updateState` 每次遍历所有日 | `AssetsViewModel` | 缓存 `dailyExpenses` |
| 多个 screen 使用完整限定类名 | `HomeScreen.kt` | 改为 import |
| `ProfileScreen` 未被导航引用 | `AppNavigation` | 确认是否仍需保留 |
| wallet/WalletScreen.kt 残留 | `ui/wallet/` | 删除 |

---

## 五、性能

| 问题 | 严重度 | 说明 |
|---|---|---|
| 日历每天循环 31 次创建 Calendar 对象 | 低 | 可优化为 1 次创建+复用 |
| AssetsViewModel 独立 collect accounts 和 transactions | 中 | 两个 Flow 各自触发会导致同月数据计算两次 |
| `material-icons-extended` 体积大 | 低 | 打包后增加 ~2MB，可后续优化为只用需要的图标 |

---

## 六、测试

**当前状态**：无任何自动化测试。

**建议**：
- 至少写 3 个 Room DAO 测试（插入、查询、删除）
- 写 2 个 ViewModel 测试（添加交易后汇总更新、月份切换）
- CI 用 GitHub Actions 跑 `./gradlew test`

---

## 七、优先级排序

| 优先级 | 任务 | 工时 |
|---|---|---|
| 🔴 P0 | 加回 `kotlin-android` 插件声明 | 1min |
| 🔴 P0 | 删除根目录 `git` `main` 垃圾文件 | 1min |
| 🟡 P1 | 删除 `TransactionDao.getAll()` 重复方法 | 2min |
| 🟡 P1 | 删除 `WalletScreen.kt` 残留 | 1min |
| 🟢 P2 | 移除 Repository 透传层 | 20min |
| 🟢 P2 | Category 加 `isAdjustment` 字段 | 30min |
| 🟢 P3 | 写 DAO 测试 | 1h |
| 🟢 P3 | `fallbackToDestructiveMigration` → 正常 Migration | 30min |

---

## 八、总结

**功能上**：四 tab 完整，记账流程闭环，27 个图标本地化，分类带 Material 图标。可以直接用了。

**技术上**：最大风险是 `fallbackToDestructiveMigration` 会在升级时丢数据。其余都是小修小补。代码组织有冗余但没有硬伤。
