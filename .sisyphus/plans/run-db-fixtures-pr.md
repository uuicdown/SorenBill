# 计划：在仓库中创建历史 schema DDL 与 SQLite fixture 并提交 PR

## TL;DR
> Summary: 运行已生成的脚本 .sisyphus/drafts/create-fixtures-and-pr.sh，在本地生成 app/schemas/manual/schema_v2_create.sql、app/src/androidTest/assets/fixtures/v2.db 与示例 migration test，并在新分支 chore/add-db-fixtures-v2 提交并推送，若安装 gh 则自动创建 PR。此任务完全自动化且可回滚。
> Effort: 快速（约 5–20 分钟）
> Critical Path: 本任务完成后，后续会运行 MigrationTestHelper（DB-01）进行迁移测试。

## Context
- 目的：解除 Momus 阻塞，提供旧版 schema / fixture 以便执行自动化迁移测试（MigrationTestHelper）。

## Core Objective
- 生成并提交历史 schema DDL 与 SQLite fixture（v2），创建 PR，并在 PR 描述中包含生成证据文件路径（.sisyphus/evidence/）。

## Preconditions (必备环境与权限)
- 在运行机器上已安装：git、sqlite3。若要自动创建 PR：gh (GitHub CLI) 可选但推荐。
- 运行者对仓库有写入权限并能推送新分支到远程（git push 权限）。
- 当前工作树必须干净（没有未提交改动）。

## Execution Steps (决策完备，逐行命令)
在仓库根目录执行下列命令（绝对复制）：

1) 验证工作区与工具

  git status --porcelain
  if [ -n "$(git status --porcelain)" ]; then
    echo "工作区不干净，请先 stash/commit 本地改动" >&2; exit 1
  fi
  command -v sqlite3 >/dev/null 2>&1 || (echo "请安装 sqlite3" >&2; exit 2)

2) 给脚本可执行权限并运行（脚本会创建文件、生成 fixture、提交并推送）

  chmod +x .sisyphus/drafts/create-fixtures-and-pr.sh
  ./.sisyphus/drafts/create-fixtures-and-pr.sh

注意：若未安装 gh，脚本会推送分支但不会自动创建 PR。脚本会把验证输出保存到 .sisyphus/evidence/。

3) 验证分支与提交

  git rev-parse --abbrev-ref HEAD
  # 确认当前分支为 chore/add-db-fixtures-v2
  git ls-files app/schemas/manual/schema_v2_create.sql app/src/androidTest/assets/fixtures/v2.db app/src/androidTest/java/com/soren/bill/data/database/BillDatabaseMigrationTest.kt

4) （可选）若 gh 可用：确认 PR 已创建

  gh pr view --json number,title,url,body --jq '. | {number,title,url}' || echo "PR 未创建或 gh 不可用"

## Acceptance Criteria (验收标准，自动化可验证)
- [ ] 文件存在：git ls-files 返回以下三个路径：
  - app/schemas/manual/schema_v2_create.sql
  - app/src/androidTest/assets/fixtures/v2.db
  - app/src/androidTest/java/com/soren/bill/data/database/BillDatabaseMigrationTest.kt
- [ ] .sisyphus/evidence/task-0-fixture-tables.txt 存在且包含表名（wallets, accounts, categories, transactions）
- [ ] .sisyphus/evidence/task-0-fixture-transactions-count.txt 存在且值为 3
- [ ] 新分支已推送到远程（git ls-remote --heads origin chore/add-db-fixtures-v2 返回非空）
- [ ] PR 已创建（若 gh 可用）：gh pr view 返回 PR url

## QA Scenarios (必须自动执行，包含失败场景)
Scenario: Happy path
  Tool: Bash + git + sqlite3 + (gh optional)
  Steps:
    1. 运行脚本（见上）
    2. 检查证据文件：cat .sisyphus/evidence/task-0-fixture-transactions-count.txt -> 应输出 3
    3. 确认分支与文件存在（git ls-files ...）
    4. 若 gh 可用，gh pr view 输出 PR URL
  Expected: 所有断言成立。

Scenario: Failure - sqlite3 不存在
  Tool: Bash
  Steps: 在不安装 sqlite3 的环境运行脚本
  Expected: 脚本退出并输出错误 'sqlite3 not found'，并不修改仓库。

Scenario: Failure - 无推送权限
  Tool: Bash
  Steps: 运行脚本但 git push 权限被拒绝
  Expected: 脚本会尝试 git push 并报错，工作分支保留在本地；保存 .sisyphus/evidence/push-error.txt 包含错误信息（执行者需将该文件上传到 PR 或反馈）。

## Rollback / Cleanup
- 若需要回滚本次提交：
  git push origin --delete chore/add-db-fixtures-v2 || true
  git checkout main && git reset --hard origin/main

## Agent Profile (推荐)
- Category: quick + git-master
- Skills: git, sqlite3, Android instrumentation testing, GitHub CLI (optional)

## Next Steps after Success
- 执行 DB-01：在 CI 或本地运行迁移测试（./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.soren.bill.data.database.BillDatabaseMigrationTest），并将结果保存到 .sisyphus/evidence/task-1-migration-report.txt。若测试通过，继续 DB-02（受控移除 fallbackToDestructiveMigration）。

Plan saved to: .sisyphus/plans/run-db-fixtures-pr.md
