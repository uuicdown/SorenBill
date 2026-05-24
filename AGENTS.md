# Soren的账单 — AI 开发维护文档

> 本文件面向 AI 助手，帮助快速理解项目结构、架构约定和开发规范。
> 最后更新：2026-05-24

---

## 一、项目速览

| 项目 | 值 |
|---|---|
| 应用名称 | Soren的账单 |
| 包名 | `com.soren.bill` |
| 最低 SDK | 26 (Android 8.0) |
| 目标 SDK | 35 |
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room 2.7.1 + KSP |
| 架构 | MVVM + Repository |
| 构建 | AGP 9.2.1, Gradle 8.13 |

---

## 二、目录结构与职责

```
SorenBill/
├── AGENTS.md                           # ← 本文档
├── CODE_REVIEW.md                      # Code Review 记录
├── PROJECT_STATUS.md                   # 项目状态与规划
├── build.gradle.kts                    # 项目级构建
├── settings.gradle.kts                 # Gradle 设置（阿里云镜像）
├── gradle.properties                   # Gradle 配置
└── app/
    ├── build.gradle.kts                # 模块级构建
    ├── schemas/                        # Room schema 导出目录（v3+）
    └── src/
        ├── main/java/com/soren/bill/
        │   ├── BillApplication.kt      # Application，手动 DI 入口
        │   ├── MainActivity.kt         # 入口 Activity
        │   ├── data/
        │   │   ├── entity/             # Room 实体：Wallet, Account, Category, Transaction
        │   │   ├── dao/                # DAO 接口
        │   │   ├── database/           # BillDatabase（Room 数据库类）
        │   │   └── repository/         # BillRepository（数据仓库）
        │   ├── ui/
        │   │   ├── theme/              # Color, Type, Theme, DesignSystem（设计规范）
        │   │   ├── navigation/         # AppNavigation（底部导航 + 路由）
        │   │   ├── home/               # 首页：月度汇总 + 流水列表
        │   │   ├── calendar/           # 日历页：月视图 + 日支出预览
        │   │   ├── assets/             # 资产页：净资产 + 账户分组管理
        │   │   ├── stats/              # 统计页：饼图 + 分类占比
        │   │   ├── profile/            # 我的页：钱包/账户/分类 CRUD 管理
        │   │   └── add/                # 记账页：金额优先输入 + 日期时间选择
        │   └── util/DateUtils.kt       # 日期工具类
        ├── androidTest/                # Instrumentation 测试（Room DAO）
        └── test/                       # 单元测试（ViewModel）
```

---

## 三、关键架构决策

### 3.1 MVVM + Repository

```
Composable → ViewModel(StateFlow) → BillRepository → DAO(Room)
```

- 每个 Screen 对应一个 ViewModel，每个 ViewModel 有对应的 Factory 类
- ViewModel 通过 `viewModel(factory = ...)` 创建
- 状态通过 `MutableStateFlow<UiState>` + `collectAsState()` 响应式更新
- Repository 是纯透传层（目前无业务逻辑），使用方法注入 4 个 DAO

### 3.2 手动 DI（无框架）

`BillApplication` 中用 `by lazy` 创建 database 和 repository 的单例：
```kotlin
class BillApplication : Application() {
    val database by lazy { BillDatabase.getInstance(this) }
    val repository by lazy { BillRepository(...) }
}
```
`MainActivity` 通过 `(application as BillApplication).repository` 传给 `AppNavigation`。

### 3.3 数据库版本管理

| 版本 | 变更内容 |
|---|---|
| 1 | 初始版本 |
| 2 | 结构变更（历史，无 schema 记录） |
| 3 | 新增 `categories.is_adjustment` 列 |

Migration 策略：
- 有正常 Migration（当前：`MIGRATION_2_3`）
- `fallbackToDestructiveMigration()` 保留作为兜底（开发阶段可接受，上架前应移除）

### 3.4 余额调整的设计

"余额调整"和"初始余额"通过 `Category.isAdjustment = true` 标记。统计和汇总时过滤这些类的交易，**不再依赖 note 文本匹配**。

跨版本兼容：
- 新建数据库 → `seedDefaults()` 创建时 `isAdjustment = true`
- 从 v2 升级 → Migration 添加 `DEFAULT 0`，种子数据更新逻辑不自动修正存量（用户可在"我的"页面手动修改分类）

---

## 四、常用开发命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（混淆已启用）
./gradlew assembleRelease

# 运行 Instrumentation 测试（需设备/模拟器）
./gradlew connectedAndroidTest

# 运行单元测试
./gradlew testDebugUnitTest

# Clean 项目
./gradlew clean
```

---

## 五、代码模式与约定

### 5.1 ViewModel 模板

```kotlin
data class XxxUiState(
    val field1: List<Something> = emptyList(),
    val isLoading: Boolean = true
)

class XxxViewModel(private val repository: BillRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(XxxUiState())
    val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()

    // Factory 类
    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return XxxViewModel(repository) as T
        }
    }
}
```

### 5.2 数据流模式

- ViewModel 初始化时在 `viewModelScope.launch` 中 `collect` 数据流
- 多个 Flow 合并使用 `combine`（避免多个独立 collector 引发的重复计算）
- 避免在 ViewModel 中使用 `.first()` 读取 Flow（破坏响应式，改用 collect）

### 5.3 命名约定

| 类别 | 规则 | 示例 |
|---|---|---|
| Package | 全小写 | `com.soren.bill.ui.home` |
| Entity | 单数名词 | `Transaction`, `Category` |
| DAO | 接口名 + Dao | `TransactionDao` |
| ViewModel | ViewModel 后缀 | `HomeViewModel` |
| Screen | Compose + Screen 后缀 | `HomeScreen` |
| UI State | UiState 后缀 | `HomeUiState` |
| Route | 小写字符串 | `"home"`, `"add_transaction"` |

### 5.4 Compose 约定

- Screen Composables 接收 ViewModel 参数（而非 State）
- 用 `by viewModel.uiState.collectAsState()` 收集状态
- 业务逻辑在 ViewModel 中（CalendarScreen 中的日期计算已在 CalendarViewModel 中）
- 设计规范集中在 `DesignSystem.kt`

---

## 六、数据模型

### Wallet
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| name | String | 钱包名称 |
| currency | String | 币种，默认 CNY |
| createdAt | Long | 创建时间戳 |

### Account
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| name | String | 账户名称 |
| type | String | 类型：wechat/alipay/bank_card/credit_card/loan/cash/other |
| creditLimit | Double | 信用额度 |
| paymentDueDay | Int | 还款日 |
| isHidden | Boolean | 是否隐藏 |
| createdAt | Long | 创建时间戳 |

### Category
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| name | String | 分类名称 |
| type | String | income/expense |
| isAdjustment | Boolean | 是否为余额调整类 |
| createdAt | Long | 创建时间戳 |

### Transaction
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| amount | Double | 金额 |
| type | String | income/expense |
| walletId | Long | FK → wallets |
| accountId | Long | FK → accounts |
| categoryId | Long | FK → categories |
| date | Long | 交易时间戳 |
| note | String? | 备注 |
| createdAt | Long | 创建时间戳 |

---

## 七、注意事项与陷阱

### ⚠️ 不要做的事

1. **不要在 note 字段上做业务判断** — 用户可能输入任意文本。用 category 的 `isAdjustment` 字段替代
2. **不要在 Composable 中做日期计算** — 移到 ViewModel
3. **不要在 Repository 中加业务逻辑** — 目前是透传层，如果加业务逻辑应在 ViewModel 中，或等后续重构
4. **不要用 `first()` 读 Flow** — 会阻塞协程，破坏响应式，改用 `collect`
5. **不要改 string 类型** — `Transaction.type`、`Account.type`、`Category.type` 使用 `String` 而非枚举，任何修改需同步更新所有 `when` 分支

### ⚠️ 需要注意的

- 数据库 Migration 需要写 `app/schemas/` 目录下的 schema JSON 导出。如果 schema 目录不在版本控制中，新设备构建会失败。`exportSchema = true` 需要对应目录存在
- 所有 ViewModel 依赖都是手动注入的，没加 DI 框架。新增 ViewModel 需要同步创建 Factory 并在 `AppNavigation` 中注册
- 日历页的节假日数据 (`holidayMap`) 是硬编码的 2026 年数据，每年需要更新
- 图标资源使用本地 PNG（银行/支付平台图标），Material Icons 通过 `material-icons-extended` 全量导入

---

## 八、后续开发方向

| Phase | 内容 |
|---|---|
| 1 (当前) | 核心功能完整，可上架 |
| 2 | 自动记账（AccessibilityService + NotificationListener）、商户→分类规则表 |
| 3 | 多币种、云端备份、预算管理、桌面小组件 |

### 待完成的优化项

- [ ] 移除 `fallbackToDestructiveMigration()` 兜底（为稳定版本写全 Migration）
- [ ] `Category` 实体加 `iconName` 字段（替代 `DesignSystem.categoryIcon()` 硬编码映射）
- [ ] 中文 UI 字符串抽取到 `strings.xml`
- [ ] 缩小 `material-icons-extended` 引用
- [ ] 补充更多测试覆盖

---

## 九、数据库迁移指南

### 添加新 Migration 的步骤

1. 在 Entity 中添加新字段
2. 递增 `@Database(version = N+1)`
3. 写 `Migration(N, N+1)` 执行 `ALTER TABLE`
4. 在 `buildDatabase()` 中注册新 Migration

### 示例

```kotlin
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN is_adjustment INTEGER NOT NULL DEFAULT 0")
    }
}
```

**注意**：`exportSchema = true` 会在编译时生成 schema JSON 到 `app/schemas/`，请在版本控制中包含这些文件，以便后续写 Migration 时参考。
