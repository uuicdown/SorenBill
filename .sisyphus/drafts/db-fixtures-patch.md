# Draft: 可直接应用的 Patch 内容 — 添加历史 schema DDL 与 migration test 模板

说明：你选择让我生成完整 patch 内容。受限于本规划器不能直接修改源码外的文件，我把所有将要提交的文件内容、生成 fixture 的精确命令、以及完整的 git/PR 步骤打包成这个草稿。你或执行者可以按下列步骤：在本地新分支上创建文件、运行命令生成 fixture、提交并在 GitHub 上创建 PR。

一、目标文件与提交信息（全部精确）
- 分支名：chore/add-db-fixtures-v2
- 提交信息：test(db): add legacy schema SQL and sqlite fixture v2 for migration tests
- PR 标题：chore(db): add legacy schema SQL and sqlite fixture v2 for migration tests

二、要在仓库中新增的文件（请按路径与内容逐字创建）

1) app/schemas/manual/schema_v2_create.sql

文件内容（从第 1 行到末尾完整替换）：

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

2) app/src/androidTest/assets/fixtures/v2.db

说明：这是二进制 SQLite DB fixture。不要直接在文本文件中创建它，而是用下面的命令生成。

三、生成 fixture 与样例数据的精确命令（在项目根目录执行）

前置要求：在运行环境中已安装 sqlite3 命令行工具。

步骤：
1. 创建目录

  mkdir -p app/schemas/manual
  mkdir -p app/src/androidTest/assets/fixtures

2. 将上面的 schema_v2_create.sql 内容保存到 app/schemas/manual/schema_v2_create.sql

3. 用 sqlite3 创建空 DB 并导入 DDL

  sqlite3 app/src/androidTest/assets/fixtures/v2.db < app/schemas/manual/schema_v2_create.sql

4. 向 fixture 插入示例数据（逐条执行或用 heredoc）

  sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO wallets(id,name,currency,createdAt) VALUES(1,'Default Wallet','CNY',1622505600);"
  sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO accounts(id,name,type,creditLimit,paymentDueDay,isHidden,createdAt) VALUES(1,'Cash','cash',0.0,0,0,1622505600);"
  sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO categories(id,name,type,is_adjustment,createdAt) VALUES(1,'Salary','income',0,1622505600);"
  sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(1,1000.0,'income',1,1,1,1622592000,'pay',1622592000);"
  sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(2,20.0,'expense',1,1,1,1622678400,'coffee',1622678400);"
  sqlite3 app/src/androidTest/assets/fixtures/v2.db "INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(3,50.0,'expense',1,1,1,1622764800,'dinner',1622764800);"

5. 可选：检查 fixture 是否正常（列出表、计数）

  sqlite3 app/src/androidTest/assets/fixtures/v2.db "SELECT name FROM sqlite_master WHERE type='table';"
  sqlite3 app/src/androidTest/assets/fixtures/v2.db "SELECT COUNT(*) FROM transactions;"

把上述命令的输出保存到 .sisyphus/evidence/task-0-fixture-generation.txt 并附在 PR 描述里。

四、示例 MigrationTestHelper 测试文件（精确内容）

文件路径（建议）： app/src/androidTest/java/com/soren/bill/data/database/BillDatabaseMigrationTest.kt

文件内容（逐字创建）：

package com.soren.bill.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillDatabaseMigrationTest {

  @get:Rule
  val helper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    BillDatabase::class.java.canonicalName
  )

  @Test
  fun migrateV2ToV3_preservesData() {
    val dbName = "roomsample-db" // 替换为真实的 DB 名称，如果 BillDatabase 使用不同 name 请修改

    // 创建旧版 DB（version = 2）
    val db = helper.createDatabase(dbName, 2)
    // 可在此处插入/验证样例数据（fixture 方式也可）
    db.close()

    // 执行迁移并验证到 version 3（确保 BillDatabase.MIGRATION_2_3 在代码中存在）
    helper.runMigrationsAndValidate(dbName, 3, true, BillDatabase.MIGRATION_2_3)
  }
}

注意：如果 BillDatabase 在代码中使用了特定的 DB 名称或上下文，你需要将 dbName 替换为实际值；MigrationTestHelper 也可以使用复制 fixture 的方式来创建旧 DB（参见 Android 官方文档）。

五、完整 Git 提交流程（决策完备，复制执行）

1) 切分支并添加文件

  git checkout -b chore/add-db-fixtures-v2
  git add app/schemas/manual/schema_v2_create.sql app/src/androidTest/assets/fixtures/v2.db app/src/androidTest/java/com/soren/bill/data/database/BillDatabaseMigrationTest.kt

2) 提交并推送

  git commit -m "test(db): add legacy schema SQL and sqlite fixture v2 for migration tests"
  git push origin chore/add-db-fixtures-v2

3) 在 GitHub 上创建 PR

  标题: chore(db): add legacy schema SQL and sqlite fixture v2 for migration tests
  Body 模板（请完整粘贴）:

  - 变更说明：添加 v2 的 schema DDL 与 sqlite fixture，用于在 CI 中运行 Room MigrationTestHelper。此 PR 不更改运行时代码，仅用于测试。
  - 验收条件：
    1) CI 的 migration-test job 运行并通过：
       ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.soren.bill.data.database.BillDatabaseMigrationTest
    2) fixtures 位于 app/src/androidTest/assets/fixtures/v2.db，DDL 位于 app/schemas/manual/schema_v2_create.sql
  - 证据：在 PR 描述中粘贴 .sisyphus/evidence/task-0-fixture-generation.txt 的内容（包含 sqlite3 输出）或 CI job 链接。

4) Reviewer checklist（PR 模板）

  - [ ] 确认文件路径正确且无敏感信息
  - [ ] 本地执行 sqlite3 命令能重建 fixture（在 PR 评论中附上命令输出或 evidence 文件）
  - [ ] CI 有 migration-test job 并能成功运行（或 reviewer 在本地 emulator 上验证）

六、我会在 PR 创建后继续执行的任务清单（由我或 Sisyphus 执行代理接手）

- 触发 DB-01：运行 MigrationTestHelper 并记录结果到 .sisyphus/evidence/task-1-migration-report.txt
- 若测试通过，准备 DB-02 的 PR（移除 fallbackToDestructiveMigration() 的受控草案，包含灰度与回滚说明）

七、现在该你/我做什么

- 我可以把上述文件内容打包成一个可下载的 patch（.diff），或直接把创建 PR 的步骤输出为可复制命令（shell 脚本）。请回复：
  - 1: 生成可下载 patch (.diff)
  - 2: 生成 shell 脚本（在本地运行以创建文件、生成 fixture 并提交）

我会在你选择后立即生成对应 artifact（草稿中包含全部内容）。
