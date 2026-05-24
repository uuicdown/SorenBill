# 计划：Execute-All — 提交当前变更、生成 DB fixtures、并启动 UI 重设计与自动记账实现

## TL;DR
> 我将生成一份决策完备的执行清单，指导如何在安全、可验证的前提下提交当前本地变更、运行 fixtures 生成脚本并创建 PR；同时我会立刻派发两个后台 agent 做 UI 设计（visual-engineering）和竞品设计研究（librarian）。

## Quick decision summary (我代为决策并执行)
- 处理未提交改动方式：提交为 WIP（git add -A && git commit -m "WIP: save local changes before fixture script"）。理由：保留本地修改为最小风险路径，并保持工作树干净以运行 fixture 脚本。
- 生成 fixtures：运行 .sisyphus/drafts/create-fixtures-and-pr.sh（会创建 DDL、fixture DB、migration test、branch、commit 并推送；如安装 gh 会尝试创建 PR）。
- UI 设计：并行派发两个 agent：visual-engineering（生成 Figma 高保真原型与交互说明）、librarian（收集竞品 UI/交互样例与设计要点）。

## Step-by-step (决策完备命令)
Run these commands at the repository root, in order. Copy-paste exactly.

1) Commit current local changes (conservative, non-destructive)

  git add -A
  git commit -m "WIP: save local changes before fixture script"

  # If you prefer stash instead of commit, use:
  # git stash push -m "WIP before fixture script"

2) Confirm working tree is clean

  git status --porcelain
  # Expect: no output

3) Ensure prerequisites

  sqlite3 --version || echo "Install sqlite3 and re-run"
  # Optional but recommended for auto-PR:
  gh --version || echo "Install GitHub CLI (gh) to auto-create PR"

4) Run the fixture & PR script

  bash .sisyphus/drafts/create-fixtures-and-pr.sh

  # The script will:
  # - create app/schemas/manual/schema_v2_create.sql
  # - create app/src/androidTest/assets/fixtures/v2.db
  # - create app/src/androidTest/java/com/soren/bill/data/database/BillDatabaseMigrationTest.kt
  # - write evidence to .sisyphus/evidence/*
  # - create branch chore/add-db-fixtures-v2, commit and push, and attempt gh pr create

5) Verify evidence and PR

  cat .sisyphus/evidence/task-0-fixture-tables.txt
  cat .sisyphus/evidence/task-0-fixture-transactions-count.txt
  git branch --show-current
  git ls-remote --heads origin chore/add-db-fixtures-v2

  # If gh didn't create PR, open:
  # https://github.com/<your-org>/<your-repo>/pull/new/chore/add-db-fixtures-v2

## Post-submit actions (automated by agents once PR exists)
- DB-01: Run MigrationTestHelper (androidTest) in CI; gate PR on connectedAndroidTest migration tests.
- UI-01: Start UI redesign implementation tasks (Dev PRs) after design acceptance.
- AUTO-01: Start auto-bookkeeping engineering in an opt-in Suggestion-only mode (parser + NotificationListener + Accessibility scaffolding behind explicit opt-in UI + offline-only processing by default).

## Agents I will dispatch now (background)
- Visual design agent (category: visual-engineering) — deliverable: Figma hi-fi prototype for Add/Home/Calendar/Profile + component spec + interaction notes (3 business days). Output: Figma link + exported spec JSON + task list for implementers.
- Research agent (subagent_type: librarian) — deliverable: competitor UX audit (随手记/挖财/网易有钱) listing 15 UX patterns to borrow + screenshots & copy notes (2 business days).

I will dispatch them now and collect results under .sisyphus/evidence/ui-design-*.json and .sisyphus/evidence/competitor-research.md.

## Safety & Rollback
- If migration tests fail, do NOT merge PR; revert the branch and investigate. Rollback commands:

  git push origin --delete chore/add-db-fixtures-v2 || true
  git checkout main && git reset --hard origin/main

## Acceptance Criteria for overall step
- PR created: branch chore/add-db-fixtures-v2 pushed and PR exists (if gh available)
- Evidence files exist and show expected tables + 3 sample transactions
- Visual design agent produced Figma link and component spec

Plan saved to: .sisyphus/plans/execute-all.md
