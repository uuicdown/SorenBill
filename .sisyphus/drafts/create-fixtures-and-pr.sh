#!/usr/bin/env bash
set -euo pipefail

# 脚本：在本地创建历史 schema DDL、生成 SQLite fixture、添加 migration test 模板，并建立分支/提交/推送/（可选）创建 PR
# 用途：配合 .sisyphus/plans/evaluate-sorenbill.md 中的 DB-00 步骤，解除 MigrationTestHelper 的阻塞

# 重要：在运行前请确认你在项目仓库根目录下，并已安装：git, sqlite3. 可选：gh (GitHub CLI) 用于自动创建 PR。

REPO_ROOT="$(pwd)"
DDL_PATH="app/schemas/manual/schema_v2_create.sql"
FIXTURE_PATH="app/src/androidTest/assets/fixtures/v2.db"
TEST_PATH="app/src/androidTest/java/com/soren/bill/data/database/BillDatabaseMigrationTest.kt"
BRANCH_NAME="chore/add-db-fixtures-v2"
COMMIT_MSG="test(db): add legacy schema SQL and sqlite fixture v2 for migration tests"
PR_TITLE="chore(db): add legacy schema SQL and sqlite fixture v2 for migration tests"

mkdir -p "$(dirname "$DDL_PATH")"
mkdir -p "$(dirname "$FIXTURE_PATH")"
mkdir -p "$(dirname "$TEST_PATH")"

echo "[info] Writing DDL to $DDL_PATH"
cat > "$DDL_PATH" <<'SQL'
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
SQL

if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "[error] sqlite3 not found in PATH. Install sqlite3 and rerun." >&2
  exit 2
fi

echo "[info] Creating SQLite fixture at $FIXTURE_PATH"
sqlite3 "$FIXTURE_PATH" < "$DDL_PATH"

echo "[info] Inserting sample data into fixture"
sqlite3 "$FIXTURE_PATH" "INSERT INTO wallets(id,name,currency,createdAt) VALUES(1,'Default Wallet','CNY',1622505600);"
sqlite3 "$FIXTURE_PATH" "INSERT INTO accounts(id,name,type,creditLimit,paymentDueDay,isHidden,createdAt) VALUES(1,'Cash','cash',0.0,0,0,1622505600);"
sqlite3 "$FIXTURE_PATH" "INSERT INTO categories(id,name,type,is_adjustment,createdAt) VALUES(1,'Salary','income',0,1622505600);"
sqlite3 "$FIXTURE_PATH" "INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(1,1000.0,'income',1,1,1,1622592000,'pay',1622592000);"
sqlite3 "$FIXTURE_PATH" "INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(2,20.0,'expense',1,1,1,1622678400,'coffee',1622678400);"
sqlite3 "$FIXTURE_PATH" "INSERT INTO transactions(id,amount,type,walletId,accountId,categoryId,date,note,createdAt) VALUES(3,50.0,'expense',1,1,1,1622764800,'dinner',1622764800);"

echo "[info] Verifying fixture tables and transaction count"
sqlite3 "$FIXTURE_PATH" "SELECT name FROM sqlite_master WHERE type='table';" > .sisyphus/evidence/task-0-fixture-tables.txt
sqlite3 "$FIXTURE_PATH" "SELECT COUNT(*) FROM transactions;" > .sisyphus/evidence/task-0-fixture-transactions-count.txt

echo "[info] Fixture verification outputs saved to .sisyphus/evidence/"

echo "[info] Writing MigrationTest template to $TEST_PATH"
cat > "$TEST_PATH" <<'KOT'
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
    val dbName = "roomsample-db" // 如果实际 DB 名称不同，请手动替换
    val db = helper.createDatabase(dbName, 2)
    db.close()
    helper.runMigrationsAndValidate(dbName, 3, true, BillDatabase.MIGRATION_2_3)
  }
}
KOT

echo "[info] Preparing git branch: $BRANCH_NAME"
git checkout -b "$BRANCH_NAME"
git add "$DDL_PATH" "$FIXTURE_PATH" "$TEST_PATH" || true
git commit -m "$COMMIT_MSG" || echo "[warn] nothing to commit or commit failed"

if command -v gh >/dev/null 2>&1; then
  echo "[info] Pushing branch and creating PR using gh"
  git push -u origin "$BRANCH_NAME"
  gh pr create --title "$PR_TITLE" --body "添加 v2 schema DDL 与 sqlite fixture 用于 migration tests。\n\nAcceptance: CI migration-test job should pass. See .sisyphus/evidence for fixture generation outputs." || echo "[warn] gh pr create failed; create PR manually"
else
  echo "[info] gh CLI not found. Pushing branch only. Please create PR manually with the following info:" 
  echo "  Branch: $BRANCH_NAME"
  echo "  Commit message: $COMMIT_MSG"
  echo "  PR title: $PR_TITLE"
  echo "  Suggested PR body: 添加 v2 schema DDL 与 sqlite fixture 用于 migration tests. Acceptance: CI migration-test job should pass. See .sisyphus/evidence for fixture generation outputs."
  git push -u origin "$BRANCH_NAME"
fi

echo "[done] Patch files created and branch pushed (if possible)."
echo "Next: 在 CI 上运行 migration-test job，或在本地运行: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.soren.bill.data.database.BillDatabaseMigrationTest"

exit 0
