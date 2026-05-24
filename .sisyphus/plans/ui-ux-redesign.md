# 计划：完整 UI 重设计（设计 + 开发交付） — Soren 的账单

## TL;DR
> Summary: 按你要求的“完整 UX 重设计”（设计 + 开发交付），我准备一个 4 周的决策完备执行计划，覆盖主要用户路径：Add（记账）/Home（流水）/Calendar（日历）/Profile（我的）。计划交付高保真 Figma 原型、组件规范、以及逐项的实现任务（每项包含 Compose 实现要点、测试场景、PR 模板与验收命令）。
> Deliverables: Figma 高保真原型（关键屏幕）、组件库规范、实现级任务清单（含代码引用）、CI 可执行的 QA 脚本与证据路径。
> Effort: 中期 4 周（按你选择），并行化程度中等（Wave 1 基础与安全，Wave 2 实现与回归）
> Critical Path: 设计产出 → 开发实现（Add & Home）→ 迁移与回归测试 → 上线验收

## Context
- 用户要求：评估并进行完整 UI 重设计，交付设计 + 开发（你已选择“设计 + 开发交付”，验收由 指定设计师/PM）。
- 技术约束：Jetpack Compose + Material3, 手动 DI, Room DB；当前代码位置主要在 app/src/main/java/com/soren/bill/ui/** 与 theme/**。
- 已知问题（来自代码扫描）：Add 流双重金额输入、Home 行为导致误删、Calendar 固定宽度与微小文字、缺少 contentDescription、字符串散落在代码中、DesignSystem 的图标映射以字符串匹配（不可维护）。

## Work Objectives
- 在 4 周内完成：
  1) 可操作的高保真设计稿（Figma），覆盖 Add/Home/Calendar/Profile；
  2) 组件规范（颜色、Typography、Icon policy、Spacing、Token）；
  3) 决策完备的实现任务（每个任务含实现步骤、引用文件、测试脚本、PR 模板）；
  4) 所有 P0 改动通过自动化 QA（unit tests + instrumentation where specified）。

### Definition of Done (verifiable)
- Figma 原型发布并由指定设计师/PM 签收（签收证据：签收邮件/PR comment）
- 所有 P0 实现任务通过对应自动化 QA（见 Acceptance Criteria），证据存于 .sisyphus/evidence/
- strings.xml 骨架与 DesignSystem config 已提交并 PR 已创建

## Verification Strategy
- Test decision: 设计产出先验收（人工设计评审），实现采用 tests-after：ViewModel unit tests + instrumentation tests for navigation/critical flows + automated UI assertions where possible.
- QA policy: 每个实现任务必须包含：1 个快乐路径自动化验证命令 + 1 个失败/边界场景验证命令。所有证据保存到 .sisyphus/evidence/task-{N}-{slug}.*

## Execution Strategy
### Parallel Execution Waves (4-week schedule)
- Wave 1 (Week 1): Design sprint + Foundations (components, tokens, strings extraction)
  - D1. Figma: high-level flows & key screens (Add, Home, Calendar) — deliverables: prototype + interaction spec
  - F2. Design tokens & component spec (colors, typography, spacing, icon policy)
  - F3. Strings extraction plan & skeleton (strings.xml) and Accessibility checklist

- Wave 2 (Week 2): Implement core Add flow & Home safety improvements
  - I1. Implement consolidated amount input, inline validation, saving state (AddTransactionScreen + AddTransactionViewModel)
  - I2. Change Home row tap to Edit/Detail; implement swipe-to-delete with Undo snackbar (HomeScreen + HomeViewModel)

- Wave 3 (Week 3): Calendar responsiveness, DesignSystem refactor, icons & localization
  - C1. Replace fixed width math with BoxWithConstraints/LocalConfiguration, increase calendar typography, externalize holiday map
  - C2. Extract icon mapping to resource/config (JSON) and add category.icon_name migration (depends on DB work)

- Wave 4 (Week 4): QA, polish, docs, and handoff
  - Q1. Run all unit tests and connected instrumentation tests (migration tests separate plan) and generate evidence
  - Q2. Design review session with designated PM/designer; produce sign-off artifact
  - Q3. Prepare PRs per task with PR templates and rollback notes

### Dependency Matrix (high level)
- Strings & tokens (Wave1) → all UI tasks
- DB migration for icon_name → required for icon mapping rollout (blocks C2 UI fully)
- Add & Home changes are independent and can be implemented in parallel

## Detailed TODOs (决策完备任务列表)
> 每个任务都按 Prometheus 模板列出：实现要点、Must NOT、Agent profile、并行性、引用、验收、QA 场景、Commit 信息

- [ ] 1. DESIGN-01: Figma 高保真原型（关键屏：Add/Home/Calendar/Profile） — Wave1 (Week1)

  What to do:
  - 3 天内在 Figma 创建高保真交互原型，包含：Add 流（单输入 + 预览/模板）、Home 列表（编辑/查看/删除交互）、Calendar 月视图与日详情弹层、Profile 管理分类/账户界面。
  - 每个交互在原型中标注可点击区域、键盘行为、焦点顺序与 TalkBack 语义提示。

  Must NOT do: 只输出视觉稿，不包含交互说明或开发切图。

  Recommended Agent Profile:
  - Category: visual-engineering
  - Skills: Figma prototyping, Android Compose patterns

  Parallelization: NO (设计产出是实现的前提)

  References:
  - App screens: app/src/main/java/com/soren/bill/ui/add/AddTransactionScreen.kt
  - Home: app/src/main/java/com/soren/bill/ui/home/HomeScreen.kt
  - Calendar: app/src/main/java/com/soren/bill/ui/calendar/CalendarScreen.kt

  Acceptance Criteria:
  - [ ] Figma link posted in PR description and a design-review recording or comments from designer/PM (signed off)

  QA Scenarios:
  Scenario: Design review acceptance
    Tool: manual review by designated designer/PM
    Steps: Designer/PM reviews Figma prototype and verifies listed interactions; posts approval comment
    Expected: "Design accepted" comment exists in PR/issue; evidence saved to .sisyphus/evidence/design-signoff.txt

  Commit: YES | Message: "design: add Figma prototype for Add/Home/Calendar" | Files: (Figma link in PR body)

- [ ] 2. DEV-01: Add 流重构（单一金额输入 + inline validation + persisted draft） — Wave2 (Week2)

  What to do:
  - Replace dual amount presentation with single OutlinedTextField (leading currency prefix) in AddTransactionScreen.kt.
  - Add AddTransactionUiState fields: amountError:String?, isSaving:Boolean, draftId:String?; persist draft using DataStore keyed by user+walletId.
  - Disable Save button while isSaving; show inline error below field when amountError != null.

  Must NOT do: 同时改动 DB schema in this task; icon_name migration is separate.

  Recommended Agent Profile:
  - Category: visual-engineering + deep
  - Skills: Jetpack Compose, DataStore, ViewModel testing

  Parallelization: YES (can be done while Home changes proceed)

  References:
  - Add UI: app/src/main/java/com/soren/bill/ui/add/AddTransactionScreen.kt
  - Add VM: app/src/main/java/com/soren/bill/ui/add/AddTransactionViewModel.kt

  Acceptance Criteria (agent-executable):
  - [ ] Unit tests: ./gradlew testDebugUnitTest --tests "com.soren.bill.ui.add.AddTransactionViewModelTest.*" pass
  - [ ] Run static grep to confirm no duplicate amount UI remains: git grep -n "dual amount" || true (developer to ensure)

  QA Scenarios:
  Scenario: Happy path - save transaction
    Tool: unit test
    Steps: instantiate AddTransactionViewModel, set amount=100, select category/account, call save(); assert isSaving toggles and repository.saveTransaction called; assert no amountError
    Expected: test passes; evidence: .sisyphus/evidence/task-2-add-save.txt

  Scenario: Edge - invalid amount
    Tool: unit test
    Steps: set amount="abc"; call save(); assert amountError non-null and save not triggered
    Expected: test passes

  Commit: YES | Message: "feat(add): consolidate amount input, add draft persistence and validation" | Files: AddTransactionScreen.kt, AddTransactionViewModel.kt, tests

- [ ] 3. DEV-02: Home 行为改造（tap→edit, swipe-delete + undo） — Wave2 (Week2)

  What to do:
  - Change TxRow onClick in HomeScreen.kt to open EditTransactionScreen (route: "edit_transaction/{id}") instead of delete dialog.
  - Implement swipe-to-delete with confirm Snackbar. On swipe, mark pendingDelete in ViewModel and show Snackbar with Undo; if not undone within 5s, call repository.deleteTransaction.

  Must NOT do: Immediate hard delete without undo.

  Recommended Agent Profile:
  - Category: visual-engineering + quick
  - Skills: Compose gestures (swipe), ViewModel state, Snackbar usage

  Parallelization: YES

  References:
  - Home UI: app/src/main/java/com/soren/bill/ui/home/HomeScreen.kt
  - Home VM: app/src/main/java/com/soren/bill/ui/home/HomeViewModel.kt

  Acceptance Criteria:
  - [ ] Unit tests: ./gradlew testDebugUnitTest --tests "com.soren.bill.ui.home.HomeViewModelDeleteTest.*" pass
  - [ ] UI behavior: automated instrumentation test (if available) verifying swipe shows Snackbar and undo cancels deletion: ./gradlew connectedDebugAndroidTest --tests "com.soren.bill.ui.home.HomeScreenSwipeDeleteTest"

  QA Scenarios:
  Scenario: Happy path - swipe then timeout
    Tool: instrumentation test
    Steps: swipe item, do not press Undo; assert repository.deleteTransaction called after timeout
    Expected: test passes; evidence .sisyphus/evidence/task-3-home-delete.txt

  Scenario: Undo
    Tool: instrumentation test
    Steps: swipe item, press Undo; assert repository.deleteTransaction NOT called
    Expected: test passes

  Commit: YES | Message: "fix(home): change tap to edit, implement swipe-delete with undo" | Files: HomeScreen.kt, HomeViewModel.kt, tests

- [ ] 4. DEV-03: Calendar responsiveness & accessibility — Wave3 (Week3)

  What to do:
  - Replace hard-coded screenWidth math in CalendarScreen.kt with BoxWithConstraints or LocalConfiguration to compute cell sizes dynamically.
  - Increase day detail typography to 11–12sp and ensure scalable via LocalDensity/FontScale.
  - Replace inline small labels with day bottom sheet: tapping a day opens BottomSheet with the day's transactions.
  - Move holidayMap out of code to app/src/main/res/raw/holidays_2026.json and implement loader in CalendarViewModel.

  Must NOT do: Keep tiny font sizes (<10sp) for critical numeric data.

  Recommended Agent Profile:
  - Category: deep
  - Skills: Compose layout, performance optimization, resource management

  Parallelization: YES

  References:
  - Calendar UI: app/src/main/java/com/soren/bill/ui/calendar/CalendarScreen.kt
  - Calendar VM: app/src/main/java/com/soren/bill/ui/calendar/CalendarViewModel.kt

  Acceptance Criteria:
  - [ ] Unit tests for CalendarViewModel grouping: ./gradlew testDebugUnitTest --tests "com.soren.bill.ui.calendar.CalendarViewModelTest.*" pass
  - [ ] Instrumentation: open month, tap a day, assert bottom sheet displays correct transactions: ./gradlew connectedDebugAndroidTest --tests "com.soren.bill.ui.calendar.CalendarScreenDaySheetTest"

  QA Scenarios:
  Scenario: Responsive layout
    Tool: unit + instrumentation
    Steps: run on different emulator widths; ensure cell sizes adapt; run screenshot diff (optional)
    Expected: no overlap/truncation; evidence .sisyphus/evidence/task-4-calendar-responsive.txt

  Commit: YES | Message: "feat(calendar): responsive cells, larger typography, day bottom sheet, externalize holidays" | Files: CalendarScreen.kt, CalendarViewModel.kt, resources

- [ ] 5. DEV-04: DesignSystem & Icons externalization, strings extraction — Wave3 (Week3)

  What to do:
  - Extract all hard-coded UI strings into res/values/strings.xml. Provide English/zh-CN keys. Save skeleton in PR as strings.xml.
  - Move icon mapping to app/src/main/assets/icon_map.json keyed by category.type or category.id. Update DesignSystem.kt to consult icon_map first; fallback to previous mapping.
  - Add contentDescription for all interactive icons and attach semantics to rows.

  Must NOT do: Break existing behavior for users when icon_map lacks entries — always fallback.

  Recommended Agent Profile:
  - Category: quick + writing
  - Skills: Android localization, Compose semantics, resource handling

  Parallelization: YES

  References:
  - DesignSystem: app/src/main/java/com/soren/bill/ui/theme/DesignSystem.kt
  - Navigation: app/src/main/java/com/soren/bill/ui/navigation/AppNavigation.kt

  Acceptance Criteria:
  - [ ] grep -n "\u4e2d\u56fd\u6587" || true (no Chinese literals in code) — replace with stringResource
  - [ ] strings.xml skeleton present and referenced in UI
  - [ ] icon_map.json present and loaded by DesignSystem

  QA Scenarios:
  Scenario: Accessibility pass
    Tool: automated accessibility lint (if available) + unit tests for semantics
    Steps: run a11y checks; verify no missing contentDescription warnings for interactive elements
    Expected: warnings resolved; evidence .sisyphus/evidence/task-5-strings-icons.txt

  Commit: YES | Message: "chore(i18n): extract strings; feat(ui): externalize icon map and add semantics" | Files: strings.xml, icon_map.json, DesignSystem.kt

- [ ] 6. QA-01: End-to-end QA & Design sign-off — Wave4 (Week4)

  What to do:
  - Run unit tests: ./gradlew testDebugUnitTest
  - Run instrumentation tests (selected): ./gradlew connectedDebugAndroidTest --tests "com.soren.bill.ui.*"
  - Collate reports in .sisyphus/evidence/ and present to design/PM for sign-off. Capture design acceptance comment and CI links.

  Acceptance Criteria:
  - [ ] All unit tests pass
  - [ ] Selected instrumentation tests pass (migration tests are covered separately)
  - [ ] Design/PM posts sign-off comment in PR (evidence saved)

  Commit: NO (multiple PRs merged prior)

## Final Verification Wave (MANDATORY)
- F1. Plan Compliance Audit — Oracle: verify tasks, file references, and QA scripts exist
- F2. Code Quality Review — run static analysis, ktlint, and ast_grep checks for replacements
- F3. Real QA Runner — run unit tests + selected instrumentation tests; verify evidence files
- F4. Design Sign-off — designated designer/PM must post explicit acceptance in PR

## Commit Strategy
- 每个任务单独 PR，PR 模板必须包含：变更摘要、受影响文件、自动化 QA 命令与输出（或 CI 链接）、回滚步骤（若涉及 DB）。

## Success Criteria
- 在 4 周内完成 Figma 原型并实现 P0 改动（Add/Home/Calendar），所有自动化 QA 通过且设计师/PM 已签收。

Plan saved to: .sisyphus/plans/ui-ux-redesign.md

下一步：我可以（请选择一项）
- A. 发起 Momus 高精度审查（强烈推荐）
- B. 按计划开始派发任务并创建 PR（需要执行权限）
