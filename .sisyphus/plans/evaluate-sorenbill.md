# 计划：评估并优化 “Soren 的账单” 项目（优先：用户可感知改进）

## TL;DR
> Summary: 针对你现有仓库（SorenBill），我准备一份决策完备的执行计划：优先实施用户可感知的 UX/记账流程改进，同时并行完成高优先级的数据库迁移安全工程（移除 fallbackToDestructiveMigration 的前置工作）；次轮包含图标字段、seed 幂等与类型安全改造；可选后续迁移到 Hilt。
> Deliverables: 优先级任务清单 + 每项的实现步骤、引用文件、自动化 QA 场景、迁移 SQL 模板、CI 命令；Final Verification Wave 报告模板。
> Effort: Medium（并行两条主线：UX 改进 与 DB 安全），估算 2-4 周工程量（含测试与 CI 配置）。
> Parallel: YES - 2 waves (Wave 1: 安全与基础；Wave 2: UX 与改进)
> Critical Path: 迁移测试 → 从 Release 移除 fallbackToDestructiveMigration → 灰度发布

## Context
### Original Request
- 用户："目前这个项目怎么样，有哪里可用优化，和市面上的竞品比怎么样"。已确认竞品范围：国内主流记账 App（随手记、挖财、网易有钱）。优先级：用户可感知改进（UX/功能/流畅度）。

### Interview Summary
- 我已静默扫描仓库（见 .sisyphus/drafts/evaluate-sorenbill.md 与扫描结果）。关键发现：MVVM+Repository、手动 DI (BillApplication)、Room database with fallbackToDestructiveMigration()（高风险）、ViewModel 使用 StateFlow、DesignSystem 图标硬编码、字符串作为 domain types。Metis 已审查并建议先做用户可见改进并同时硬化 DB 迁移。你选择了所有 Metis 列出的默认决策（见下“Key Decisions”）。

### Metis Review (gaps addressed)
- 分类：将工作分为 P0/P1/P2；P0 包含 UX 快速改进与 DB 迁移测试；明确必须在 Release 中移除 destructive fallback 之前完成迁移测试和灰度；建议 Hilt 延后为 P2。

## Work Objectives
### Core Objective
- 在不改用户数据风险的前提下，优先上线能显著提升用户体验的改进（减少步骤、记住上次选择、常用模板、图标改进），并同时完成数据库迁移安全化工作，确保可在 Release 中安全移除 fallbackToDestructiveMigration()。

### Deliverables
- 一套可执行 PR 列表与变更说明（含 patch 模板与精确 migration SQL）
- 对应的自动化 QA（gradle 命令、instrumentation tests、migration tests）和 CI job 片段
- 竞品对比摘要（国内主流产品差异、可借鉴 UX 列表）

### Definition of Done (verifiable)
- 所有 P0 任务通过其对应的 agent-executable QA 场景（见每项 Acceptance Criteria）
- 在 release 分支中，grep 不应返回 fallbackToDestructiveMigration()（除非在 dev/debug 明确保留）
- Migration tests（connectedAndroidTest）在 CI 上通过并产生报告
- Final Verification Wave（F1-F4）全部通过并由你显式批准

### Must Have
- Migration 自动化测试覆盖（每个已发布的 DB 版本 → 当前版本）
- UX 改进的单元测试与 ViewModel 行为测试
- 所有变更包含具体 QA 场景与证据路径

### Must NOT Have
- 不在未验证迁移链下删除 fallbackToDestructiveMigration()
- 不自动修改存在用户内容（除非是系统内置项并有明确回退）

## Verification Strategy
- Test decision: tests-after（先实现，再用自动化测试验证），包含 unit tests + instrumentation migration tests（connectedAndroidTest）。
- QA policy: 每个任务必须包含一个快乐路径自动化脚本和一个失败/边界场景脚本。
- Evidence: 所有证据写入 .sisyphus/evidence/ 目录（示例：.sisyphus/evidence/task-1-migration-report.txt）。

## Execution Strategy
### Parallel Execution Waves
Wave 1 (Foundation, parallelizable)
- 1.1 DB Migration Safety: 收集 schemas、编写 Migration tests、预备迁移 SQL、CI job（P0）
- 1.2 Flow & ViewModel Stability: 消除 .first() / 多个 collectors，合并流、补充 unit tests（P0）

Wave 2 (User-visible features)
- 2.1 Add 流程改进：记住上次账户/分类、常用模板、减少点击（P0）
- 2.2 Icon 改造：在 Category 表新增 icon_name 字段（nullable）并提供 picker（P1）
- 2.3 Seed 幂等与 domain wrapper（P1）

Optional Wave 3 (P2)
- 3.1 小步迁移到 Hilt（可选，P2）

### Dependency Matrix (高层)
- DB Migration tests → Remove fallback → Gray release → Full release
- Flow fixes & ViewModel stability can run in parallel with DB work
- Icon field addition requires a migration (blocks icon picker UI full rollout until migration merged)

### Agent Dispatch Summary
- Wave 1: 3 tasks (DB migration, Flow fixes, CI job) — HIGH priority
- Wave 2: 4 tasks (Add flow UX, iconName migration+UI, seed refactor, domain wrappers)

## TODOs (任务清单)
- 请注意：每个实现任务都包含“实现 + 测试”。

- [ ] 1. DB-01: 迁移链审计与 MigrationTestHelper 套件（P0）

Note: Momus 报告阻塞性缺失 —— 仓库中缺少 Room 导出的 schema JSON（app/schemas/ 为空），这会阻塞 MigrationTestHelper 的自动化流程。为消除阻塞，下面新增前置任务：

- [ ] 0. DB-00: 补齐/生成历史 schema 工件（阻塞项 — 必须先完成）

  What to do:
  - 检查 app/schemas/ 是否包含 schema JSON（命令: git ls-files app/schemas/**）。
  - 若目录为空（常见情形），采用本计划推荐的替代方案：为每个历史 DB 版本生成两类工件并加入版本控制：
    1) SQL DDL 脚本：路径格式 app/schemas/manual/schema_v{N}_create.sql。每个脚本包含完整的 CREATE TABLE 与索引语句，以精确重建旧版本 schema（示例模板见下）。
    2) 预构建 SQLite DB fixture：路径格式 app/src/androidTest/assets/fixtures/v{N}.db（二进制 sqlite DB）。这些 fixture 用于快速在 MigrationTestHelper 中创建旧版 DB 或在 CI 中直接复制到 emulator。

  - 生成流程（决策完备步骤）:
    1. 确认需要支持的历史版本（例如 v1, v2）。如果不确定，运行: git tag --list 或查看 Release/CHANGELOG；把发现写入 .sisyphus/evidence/schema-versions.txt。
    2. 为每个旧版本手动编写 SQL DDL（或从历史分支/commit 重建并导出），文件位置: app/schemas/manual/schema_v{N}_create.sql。示例（Category 表 v2）:
       ```sql
       CREATE TABLE IF NOT EXISTS `categories` (
         `id` INTEGER NOT NULL PRIMARY KEY,
         `name` TEXT NOT NULL,
         `type` TEXT NOT NULL,
         `createdAt` INTEGER NOT NULL
       );
       ```
    3. 在支持 sqlite3 的环境下生成 fixture DB（本地命令示例，CI 可使用类似容器）：
       - mkdir -p app/src/androidTest/assets/fixtures
       - sqlite3 app/src/androidTest/assets/fixtures/v{N}.db < app/schemas/manual/schema_v{N}_create.sql
    4. 在 instrumentation test 中使用 MigrationTestHelper.createDatabase 或 helper.createDatabaseFromSql 来打开该 fixture 并插入示例数据（或直接用 fixture 已含示例数据）。示例测试代码片段（放在 app/src/androidTest/.../DBFixturesHelper.kt）将在 DB-01 任务中引用。

  Must NOT do: 试图在没有记录/备份的情况下直接运行生产迁移流水线或在 release 分支删除 fallbackToDestructiveMigration()。

  Acceptance Criteria:
  - [ ] app/schemas/manual/ 中包含对应历史版本的 SQL DDL 文件
  - [ ] app/src/androidTest/assets/fixtures/ 中包含对应版本的 .db fixture 文件
  - [ ] 本地运行示例命令能生成 fixture（或测试能在 CI 中直接使用 fixture）并能被 MigrationTestHelper 打开用于后续迁移测试

  QA Scenarios:
  Scenario: Happy path - create fixture and open with MigrationTestHelper
    Tool: Bash + connectedAndroidTest
    Steps:
      1. 执行: git ls-files app/schemas/** 并保存输出到 .sisyphus/evidence/schema-list.txt
      2. 若为空，执行 sqlite3 命令生成 fixture: sqlite3 app/src/androidTest/assets/fixtures/v2.db < app/schemas/manual/schema_v2_create.sql
      3. 运行 migration test: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.soren.bill.data.database.BillDatabaseMigrationTest
    Expected: MigrationTestHelper 能打开 fixture 并运行断言；测试通过并生成报告 .sisyphus/evidence/task-0-fixture-generation.txt

  Commit: YES | Message: "test(db): add legacy schema SQL and db fixtures for migration tests" | Files: app/schemas/manual/schema_v2_create.sql, app/src/androidTest/assets/fixtures/v2.db

  ---

  Implementation-ready templates & commands（你已同意选项 B：我方添加 SQL DDL 与 fixture）

  1) DDL 模板（保存为 app/schemas/manual/schema_v2_create.sql）

  +-- schema_v2_create.sql
  +CREATE TABLE IF NOT EXISTS `wallets` (
  +  `id` INTEGER NOT NULL PRIMARY KEY,
  +  `name` TEXT NOT NULL,
  +  `currency` TEXT NOT NULL,
  +  `createdAt` INTEGER NOT NULL
  +);
  +
  +CREATE TABLE IF NOT EXISTS `accounts` (
  +  `id` INTEGER NOT NULL PRIMARY KEY,
  +  `name` TEXT NOT NULL,
  +  `type` TEXT NOT NULL,
  +  `creditLimit` REAL NOT NULL DEFAULT 0.0,
  +  `paymentDueDay` INTEGER NOT NULL DEFAULT 0,
  +  `isHidden` INTEGER NOT NULL DEFAULT 0,
  +  `createdAt` INTEGER NOT NULL
  +);
  +
  +CREATE TABLE IF NOT EXISTS `categories` (
  +  `id` INTEGER NOT NULL PRIMARY KEY,
  +  `name` TEXT NOT NULL,
  +  `type` TEXT NOT NULL,
  +  `is_adjustment` INTEGER NOT NULL DEFAULT 0,
  +  `createdAt` INTEGER NOT NULL
  +);
  +
  +CREATE TABLE IF NOT EXISTS `transactions` (
  +  `id` INTEGER NOT NULL PRIMARY KEY,
  +  `amount` REAL NOT NULL,
  +  `type` TEXT NOT NULL,
  +  `walletId` INTEGER NOT NULL,
  +  `accountId` INTEGER NOT NULL,
  +  `categoryId` INTEGER NOT NULL,
  +  `date` INTEGER NOT NULL,
  +  `note` TEXT,
  +  `createdAt` INTEGER NOT NULL
  +);

  2) 生成 SQLite fixture 的具体命令（在项目根目录执行）

  +# 创建 fixtures 目录
  +mkdir -p app/src/androidTest/assets/fixtures
  +
  +# 用 sqlite3 从 DDL 创建空的 v2.db
  +sqlite3 app/src/androidTest/assets/fixtures/v2.db < app/schemas/manual/schema_v2_create.sql
  +
  +# 向 fixture 插入样例数据
  +sqlite3 app/src/androidTest/assets/fixtures/v2.db <<'SQL'
  +INSERT INTO wallets(id,name,currency,createdAt) VALUES(1,'Default Wallet','CNY',1622505600);
  +INSERT INTO accounts(id,name,type,creditLimit,paymentDueDay,isHidden,createdAt) VALUES(1,'Cash','cash',0.0,0,0,1622505600);
  +INSERT INTO categories(id,name,type,is_adjustment,createdAt) VALUES(1,'Salary','income',0,1622505600);
  +INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(1,1000.0,'income',1,1,1,1622592000,'pay',1622592000);
  +INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(2,20.0,'expense',1,1,1,1622678400,'coffee',1622678400);
  +INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(3,50.0,'expense',1,1,1,1622764800,'dinner',1622764800);
  +SQL

  3) 示例 MigrationTestHelper 测试片段（保存为 app/src/androidTest/java/com/soren/bill/data/database/BillDatabaseMigrationTest.kt）

  +// 文件: BillDatabaseMigrationTest.kt
  +package com.soren.bill.data.database
  +
  +import androidx.room.testing.MigrationTestHelper
  +import androidx.test.ext.junit.runners.AndroidJUnit4
  +import androidx.test.platform.app.InstrumentationRegistry
  +import org.junit.Rule
  +import org.junit.Test
  +import org.junit.runner.RunWith
  +import java.io.File
  +
  +@RunWith(AndroidJUnit4::class)
  +class BillDatabaseMigrationTest {
  +  @get:Rule
  +  val helper: MigrationTestHelper = MigrationTestHelper(
  +    InstrumentationRegistry.getInstrumentation(),
  +    BillDatabase::class.java.canonicalName
  +  )
  +
  +  @Test
  +  fun migrateV2ToV3_preservesData() {
  +    val dbName = "roomsample-db"
  +    // 创建旧版 DB
  +    val db = helper.createDatabase(dbName, 2)
  +    // 可在此处插入/验证示例数据
  +    db.close()
  +
  +    // 运行迁移并验证
  +    helper.runMigrationsAndValidate(dbName, 3, true, BillDatabase.MIGRATION_2_3)
  +  }
  +}

  4) CI job 示例（迁移测试阶段）

  +- name: migration-test
  +  run: |
  +    ./gradlew assembleDebug
  +    # 启动 emulator 的步骤由 CI 平台决定（需保证 emulator 可用）
  +    ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.soren.bill.data.database.BillDatabaseMigrationTest

  5) 证据路径
  +- 迁移测试报告: app/build/reports/androidTests/connected/index.html
  +- fixture 生成日志: .sisyphus/evidence/task-0-fixture-generation.txt



  What to do: 
  - 列出 app/schemas/ 下的所有 schema JSON 并确认对应的 DB 版本（script: git ls-files app/schemas/**）。
  - 为每一对（旧版 → 目标版）编写 Migration SQL（以 MIGRATION_2_3 为示例），并编写 instrumentation test 类 com.soren.bill.data.database.BillDatabaseMigrationTest，使用 androidx.room.testing.MigrationTestHelper 自动化验证迁移（创建旧 schema DB、插入示例数据、执行 migrate、断言数据与结构）。
  Must NOT do: 直接删除 fallbackToDestructiveMigration() 于 release 分支。

  Recommended Agent Profile:
  - Category: unspecified-high - Reason: 需要设备/模拟器与测试工程经验
  - Skills: [Android instrumentation testing, Room migrations, Gradle CI] - 必要用于编写 MigrationTestHelper 用例与 CI job

  Parallelization: Can Parallel: YES (migration tests for different source versions run in parallel) | Wave 1 | Blocks: Remove fallback task
  Blocked By: availability of schema JSONs for old versions

  References:
  - Pattern: app/src/main/java/com/soren/bill/data/database/BillDatabase.kt - 包含 MIGRATION_2_3 和 fallbackToDestructiveMigration()（必须参考）
  - Schema dir: app/schemas/ - 用于生成 MigrationTestHelper 的旧 schema
  - AGENTS.md - 迁移指南示例和政策

  Acceptance Criteria:
  - [ ] 在本地运行命令并通过：
    - ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.soren.bill.data.database.BillDatabaseMigrationTest
    - Expected: Gradle 退出码 0；控制台含 "BUILD SUCCESSFUL" 且测试报告显示无 FAILED tests；证据文件保存 .sisyphus/evidence/task-1-migration-report.txt

  QA Scenarios:
  Scenario: Happy path - migrate v2 → v3
    Tool: connectedAndroidTest
    Steps:
      1. 在 emulator 上创建 v2 schema 的 DB 并插入 3 条示例交易和 3 条分类（含 is_adjustment=0 的旧数据）。
      2. 在测试中运行项目 MIGRATION_2_3。
      3. 断言 categories 表存在 is_adjustment 列且默认值为 0，交易仍然存在且 count = 3。
    Expected: 所有断言成立。证据: .sisyphus/evidence/task-1-migration-report.txt

  Scenario: Edge case - 部分表缺失
    Tool: connectedAndroidTest
    Steps:
      1. 创建一个缺失 categories 表的旧 DB（模拟损坏/异常旧版）。
      2. 运行 migration，并捕获 migration 异常/失败信息。
    Expected: Migration 抛出可识别异常，测试捕获并输出清晰日志；且测试失败时保留原始 DB 副本到 .sisyphus/evidence/

  Commit: YES | Message: "test(db): add migration tests v2->v3" | Files: app/src/androidTest/java/com/soren/bill/data/database/BillDatabaseMigrationTest.kt

- [ ] 2. DB-02: 移除 fallbackToDestructiveMigration 的受控流程（P0）

  What to do:
  - 在 release 分支创建 PR 但不要直接合并。PR 包含：修改 BillDatabase builder，移除 fallbackToDestructiveMigration()；同时引用 MigrationTestHelper 测试结果，并在 PR 描述中刻录迁移测试通过日志。
  - 在合并前执行灰度发布策略（阶段性 rollout）。
  Must NOT do: 直接在主分支推送且未完成迁移测试。

  Recommended Agent Profile:
  - Category: quick (code change) + unspecified-high for release gating
  - Skills: [Gradle, Android packaging, release/rollout processes]

  Parallelization: NO (must await Migration tests) | Wave 1 | Blocks: Full release
  Blocked By: Task DB-01

  References:
  - app/src/main/java/com/soren/bill/data/database/BillDatabase.kt: builder 调用位置
  - AGENTS.md: 有关 fallback 的说明

  Acceptance Criteria:
  - [ ] 在 release 分支执行: git grep -n "fallbackToDestructiveMigration" && expect no matches
  - [ ] PR 描述中包含 Migration tests 报告并显示通过

  QA Scenario: Verify removal
    Tool: Bash
    Steps:
      1. git checkout release && git pull
      2. git grep -n "fallbackToDestructiveMigration" -n app/ || true
    Expected: 无匹配；证据: .sisyphus/evidence/task-2-remove-fallback.txt

  Commit: YES | Message: "chore(db): remove fallbackToDestructiveMigration after migration tests" | Files: app/src/main/java/com/soren/bill/data/database/BillDatabase.kt

- [ ] 3. VM-01: Flow & ViewModel Stability（P0）

  What to do:
  - 在所有 ViewModel 中审查对 repository flows 的收集点（优先 HomeViewModel、AddTransactionViewModel、CalendarViewModel）。将多重独立 collectors 合并为 combine/zip 或 StateFlow + stateIn 模式以减少重复执行与竞态。
  - 查找并替换 .first() 的使用（若存在）并将其替换为适当的 collect/firstOrNull 按需策略。
  Must NOT do: 引入 breaking change 到 public repository API（除非同时更新 factories 并运行测试）。

  Recommended Agent Profile:
  - Category: deep - Reason: 需要理解 reactive patterns 与微妙行为
  - Skills: [Kotlin coroutines, Flow, StateFlow, ViewModel testing]

  Parallelization: YES (不同 ViewModel 的重构可并行) | Wave 1
  Blocked By: none

  References:
  - app/src/main/java/com/soren/bill/ui/home/HomeViewModel.kt
  - app/src/main/java/com/soren/bill/ui/add/AddTransactionViewModel.kt

  Acceptance Criteria:
  - [ ] 本地运行: ./gradlew testDebugUnitTest --tests "com.soren.bill.ui.home.HomeViewModelFlowTest.*" 并通过（0 failed）

  QA Scenarios:
  Scenario: Happy path - combined flows update UiState correctly
    Tool: unit test
    Steps: 编写 HomeViewModelFlowTest 提供 fake repository flows，触发 emissions，断言 UiState 最终包含合并后的汇总数据
    Expected: 断言通过；证据 .sisyphus/evidence/task-3-homeflow-test.txt

  Scenario: Edge case - upstream flow throws
    Tool: unit test
    Steps: 模拟 repository flow 抛出异常，断言 ViewModel 能捕获并设置 uiState.error 字段
    Expected: 断言通过

  Commit: YES | Message: "refactor(vm): consolidate flows in HomeViewModel" | Files: src files changed

- [ ] 4. UX-01: Add 流程改进（记住常用/最近选择，减少点击）（P0）

  What to do:
  - 在 AddTransactionViewModel 中新增 lastUsedAccountId 与 lastUsedCategoryId 的 persisted key（使用 DataStore 或 Room 中的 settings 表，建议使用 DataStore）。
  - 在 AddTransactionScreen 启动时优先选择 lastUsed 值并提供快速模板（常用金额/分类组合）。
  - 增加“最近”快捷行列表（最多 5 项），并支持一键填充。
  Must NOT do: 在没有用户测试的情况下改变默认排序或删除现有选项。

  Recommended Agent Profile:
  - Category: visual-engineering + unspecified-high for DataStore integration
  - Skills: [Jetpack Compose, DataStore, ViewModel testing]

  Parallelization: YES (UI + ViewModel + small storage change) | Wave 2
  Blocked By: VM-01 optional

  References:
  - app/src/main/java/com/soren/bill/ui/add/AddTransactionViewModel.kt
  - app/src/main/java/com/soren/bill/ui/add/AddTransactionScreen.kt

  Acceptance Criteria:
  - [ ] Unit test: ./gradlew testDebugUnitTest --tests "com.soren.bill.ui.add.AddTransactionViewModelTest.remembersLastUsed*" 通过
  - [ ] Manual-free evidence: 自动化 UI test not required now; unit tests sufficient for logic

  QA Scenarios:
  Scenario: Happy path - remember last used
    Tool: unit test
    Steps: simulate saving transaction with accountId=5/categoryId=7; new instance of ViewModel should expose lastUsedAccountId=5 and AddScreen should default to those values
    Expected: test passes; evidence .sisyphus/evidence/task-4-remember-last.txt

  Scenario: Edge case - no last used exists
    Tool: unit test
    Steps: ensure ViewModel returns fallback (first account/category)
    Expected: test passes

  Commit: YES | Message: "feat(ui): remember last used account/category in Add screen" | Files: AddTransactionViewModel.kt, AddTransactionScreen.kt

- [ ] 5. ICON-01: Category.icon_name 字段与 picker（P1）

  What to do:
  - 在 Category entity 增加 nullable 字段 icon_name（字符串）。增加数据库 Migration SQL: ALTER TABLE categories ADD COLUMN icon_name TEXT;
  - 在 UI 中提供 icon picker，且保持 DesignSystem 中的旧映射作为回退（若 icon_name 为 null 则使用旧逻辑）。
  - 为现有系统类别（seed defaults）在创建时填充 icon_name（仅新装）。
  Must NOT do: 自动改写用户自定义分类的 icon_name 值

  Recommended Agent Profile:
  - Category: unspecified-high
  - Skills: [Room Migration, Jetpack Compose, Resource management]

  Parallelization: NO (requires migration) | Wave 2 | Blocked By: DB-01

  References:
  - app/src/main/java/com/soren/bill/data/entity/Category.kt
  - app/src/main/java/com/soren/bill/ui/theme/DesignSystem.kt

  Acceptance Criteria:
  - [ ] Migration test verifying column exists and default behavior (use old mapping if null)
  - [ ] Unit test for UI fallback mapping

  QA Scenarios:
  Scenario: Happy path - new icon_name used
    Tool: unit + instrumentation test
    Steps: migrate DB, create category with icon_name='bank_icon', open category list, assert compose shows bank_icon
    Expected: passes; evidence .sisyphus/evidence/task-5-icon-migration.txt

  Commit: YES | Message: "feat(db+ui): add icon_name to Category and icon picker" | Files: Category.kt, migration, UI files

- [ ] 6. SEED-01: seedDefaults 幂等性改造（P1）

  What to do:
  - 改造 seedDefaults() 使其可幂等（按种子版本号或检查 system_flag 字段避免重复插入）。
  - 将 seedDefaults 的调用时机明确为 DB onCreate，仅影响新装用户。
  Must NOT do: 在 migration 阶段对用户自定义分类做删除/修改

  Recommended Agent Profile:
  - Category: quick
  - Skills: [Room, Kotlin, DB seeding patterns]

  Parallelization: YES | Wave 2

  Acceptance Criteria:
  - [ ] Instrumentation test seedRunsOnce() 通过（见上面描述命令）

  Commit: YES | Message: "fix(db): make seedDefaults idempotent" | Files: seed implementation

- [ ] 7. DOMAIN-01: 字符串类型域改造（短期 wrapper，P1）

  What to do:
  - 在 domain 层添加 value objects (TransactionType, AccountType) 并提供解析/序列化函数，不立即改 DB schema，仅在代码层提供类型安全包装以减少错误比较。
  - 更新关键分支点（统计、汇总）使用新 wrapper
  Must NOT do: 立即替换所有数据库字段名或移除现有字符串兼容性

  Acceptance Criteria:
  - [ ] 单元测试: ./gradlew testDebugUnitTest --tests "com.soren.bill.domain.DomainTypeParsingTest.*" 通过

  Commit: YES | Message: "refactor(domain): add type wrappers for Transaction/Account types" | Files: new domain files

- [ ] 8. Optional: HILT-01 小步迁移计划（P2）

  What to do:
  - 设计分阶段 Hilt 迁移：先把 Application 提供的单例转换为 Hilt modules（保留 BillApplication 的兼容工厂），并在单个屏幕（例如 Home）完成从 Factory 到 Hilt 注入的验证。
  - 提交单独 PR 并包含 lsp_find_references 验证与 ast_grep_replace(dryRun=true) 预览。
  Must NOT do: 在未完成 DB 安全工作前大范围改动 DI

  Acceptance Criteria:
  - [ ] Home screen 能在 Hilt 注入下启动且所有 unit tests 通过

  Commit: YES/NO (team decision) | Message: "chore(di): begin Hilt migration for Home screen" | Files: DI modules

## Final Verification Wave (MANDATORY)
> 在所有实现任务完成后，四个审查子任务并行运行，必须全部通过并由你手动确认 "OK" 才算最终完成。

- [ ] F1. Plan Compliance Audit — Oracle (自动化检查：文件/引用/QA 脚本存在性)
- [ ] F2. Code Quality Review — 自动化 lint + static analysis + ast_grep checks
- [ ] F3. Real QA Runner — 运行所有 unit tests + connectedAndroidTest migration tests（若选择 instrumentation）
- [ ] F4. Scope Fidelity Check — 对照任务清单，验证无额外未记录变更

## Commit Strategy
- 小步提交，每个任务单独 PR，PR 模板必须包含：变更摘要、受影响文件、QA 运行命令与结果（或 CI job 链接）、回滚步骤（若涉及 DB）。

## Success Criteria
- 所有 P0 任务完工并通过自动化 QA；release 分支不包含 destructive fallback；Add 流程改进在用户可感知指标上产生正向影响（建议：留存提升/转化率监测，非本次计划直接包含）。

## Key Decisions (based on your defaults)
- 已确认：已发布 DB 版本状态由自动检测（计划包含 schema 扫描步骤）
- 已确认：Release 中暂不立即移除 fallbackToDestructiveMigration()，先在 dev/debug 保留；仅在迁移测试通过后移除并灰度发布
- 已确认：测试策略必须包含 instrumentation（connectedAndroidTest）以验证迁移
- 已确认：Migration 回滚策略采用自动导出/备份 DB + 分阶段发布（灰度）
- 已确认：允许在 Repository 层做必要非破坏性改动（可添加迁移 helper）

## Auto-Resolved / Defaults Applied
- 我已将竞品对比范围限定为国内主流（随手记/挖财/网易有钱）并在后续分析中提供 UX 差距矩阵（单独产出）。

## Decisions Needed
- 无（你已接受所有建议的默认决策）。

Plan saved to: .sisyphus/plans/evaluate-sorenbill.md

下一步:
- 我已准备好按该计划细化每个任务到可执行 PR。如果需要更高准确度审查（Momus），请选择“高精度审查”；否则选择“开始执行”。

--

## DB-00-PR: 创建 PR 并加入 schema DDL 与 fixture（你选择：我来创建 PR 模板）

目的：将历史 schema DDL 与 SQLite fixture 添加到仓库以便在 CI 中运行 MigrationTestHelper。这是解除 Momus 阻塞的必要步骤。

决策完备的 PR 创建步骤（完全可复制）：

1) 新分支（在仓库根目录执行）

  - git checkout -b chore/add-db-fixtures-v2

2) 新增文件（基准路径与内容，精确到字节）

  - 文件: app/schemas/manual/schema_v2_create.sql
    内容（完全替换为下列行）：
    CREATE TABLE IF NOT EXISTS `wallets` (
      `id` INTEGER NOT NULL PRIMARY KEY,
      `name` TEXT NOT NULL,
      `currency` TEXT NOT NULL,
      `createdAt` INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS `accounts` (
      `id` INTEGER NOT NULL PRIMARY KEY,
      `name` TEXT NOT NULL,
      `type` TEXT NOT NULL,
      `creditLimit` REAL NOT NULL DEFAULT 0.0,
      `paymentDueDay` INTEGER NOT NULL DEFAULT 0,
      `isHidden` INTEGER NOT NULL DEFAULT 0,
      `createdAt` INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS `categories` (
      `id` INTEGER NOT NULL PRIMARY KEY,
      `name` TEXT NOT NULL,
      `type` TEXT NOT NULL,
      `is_adjustment` INTEGER NOT NULL DEFAULT 0,
      `createdAt` INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS `transactions` (
      `id` INTEGER NOT NULL PRIMARY KEY,
      `amount` REAL NOT NULL,
      `type` TEXT NOT NULL,
      `walletId` INTEGER NOT NULL,
      `accountId` INTEGER NOT NULL,
      `categoryId` INTEGER NOT NULL,
      `date` INTEGER NOT NULL,
      `note` TEXT,
      `createdAt` INTEGER NOT NULL
    );

  - 文件: app/src/androidTest/assets/fixtures/v2.db
    说明：二进制 SQLite 文件。生成命令见下一步（不要手工编辑二进制文件）。

3) 在本地/CI 生成 fixture（在项目根运行）

  - 确保已安装 sqlite3 在 PATH 中。
  - mkdir -p app/src/androidTest/assets/fixtures
  - sqlite3 app/src/androidTest/assets/fixtures/v2.db < app/schemas/manual/schema_v2_create.sql
  - sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO wallets(id,name,currency,createdAt) VALUES(1,'Default Wallet','CNY',1622505600);"
  - sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO accounts(id,name,type,creditLimit,paymentDueDay,isHidden,createdAt) VALUES(1,'Cash','cash',0.0,0,0,1622505600);"
  - sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO categories(id,name,type,is_adjustment,createdAt) VALUES(1,'Salary','income',0,1622505600);"
  - sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(1,1000.0,'income',1,1,1,1622592000,'pay',1622592000);"
  - sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(2,20.0,'expense',1,1,1,1622678400,'coffee',1622678400);"
  - sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(3,50.0,'expense',1,1,1,1622764800,'dinner',1622764800);"

4) 添加示例 migration test 文件（可选，由实现者复核）

  - 建议路径: app/src/androidTest/java/com/soren/bill/data/database/BillDatabaseMigrationTest.kt
  - 我在计划中已提供示例片段；请将其复制到上述路径并根据项目实际 DB 名称调整 createDatabase 参数（示例使用 roomsample-db 作为占位）。

5) Git 提交与 PR（完整命令）

  - git add app/schemas/manual/schema_v2_create.sql app/src/androidTest/assets/fixtures/v2.db
  - git commit -m "test(db): add legacy schema SQL and sqlite fixture v2 for migration tests"
  - git push origin chore/add-db-fixtures-v2
  - 在 GitHub 上创建 PR：
    Title: chore(db): add legacy schema SQL and sqlite fixture v2 for migration tests
    Body (模板):
      - 变更说明：添加 v2 的 schema DDL 与 sqlite fixture，用于在 CI 中运行 Room MigrationTestHelper。此 PR 不更改产品运行时代码。
      - 验收条件：
        1) CI 的 migration-test job 运行并通过（./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.soren.bill.data.database.BillDatabaseMigrationTest）
        2) Fixtures 位于 app/src/androidTest/assets/fixtures/v2.db，DDL 位于 app/schemas/manual/schema_v2_create.sql
      - 证据：在 PR 描述里粘贴 .sisyphus/evidence/task-0-fixture-generation.txt 的内容（包含 sqlite3 命令输出和路径），并附上 CI job 链接。

6) Reviewer checklist（PR 模板必填项）

  - [ ] 确认文件路径正确且无敏感信息
  - [ ] 本地执行 sqlite3 命令能重建 fixture（执行者在 PR 评论中附上命令输出或 .sisyphus/evidence 文件）
  - [ ] CI 有 migration-test job 并能成功运行（或 reviewer 在本地 emulator 上验证）

7) 合并后步骤（由 release 管理员按计划执行）

  - 等待 DB-01（迁移测试）通过并生成报告
  - 按计划在 release 分支提出移除 fallbackToDestructiveMigration() 的 PR（DB-02），PR 必须引用 DB-01 测试报告并说明灰度/回滚策略

Acceptance Criteria for this PR:

- [ ] PR 创建并包含指定文件
- [ ] CI migration-test job 成功（或 reviewer 本地验证）并在 PR 中上传测试报告路径
- [ ] PR 描述包含证据文件的内容或 CI 链接

Recommended Agent Profile: category: quick + git-master; skills: [git, sqlite3, Android instrumentation testing]

我现在已准备好生成一个可下载的 patch 文件（包含上述文件与命令），或把这份 PR 指南复制到你指定的 issue/PR 页面。请选择：

- A: 生成完整 patch 内容（供你直接应用）
- B: 仅把 PR 指南文本格式化为 GitHub PR body 并打开给你复制

回复 A 或 B。  
