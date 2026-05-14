# Stabilize Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the Android app source to a buildable UTF-8 state and fix the highest-value reactive data issues.

**Architecture:** Keep the existing MVVM + Room + Compose structure. First repair corrupted literals and swallowed code blocks, then add lightweight DAO Flow support so asset/stat screens refresh when transactions change.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Kotlin Flow, PowerShell static verification.

---

### Task 1: Add Source Health Verification

**Files:**
- Create: `scripts/verify-source.ps1`

- [ ] Add a static check that scans source files for obvious encoding-corruption syntax patterns.
- [ ] Run `powershell -ExecutionPolicy Bypass -File scripts/verify-source.ps1` and confirm it fails before source repair.

### Task 2: Repair UTF-8 Text And Syntax Blockers

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/soren/bill/data/database/BillDatabase.kt`
- Modify: `app/src/main/java/com/soren/bill/util/DateUtils.kt`
- Modify: `app/src/main/java/com/soren/bill/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/soren/bill/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/soren/bill/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/soren/bill/ui/add/AddTransactionScreen.kt`
- Modify: `app/src/main/java/com/soren/bill/ui/assets/AssetsScreen.kt`
- Modify: `app/src/main/java/com/soren/bill/ui/assets/AssetsViewModel.kt`
- Modify: `app/src/main/java/com/soren/bill/ui/calendar/CalendarScreen.kt`
- Modify: `app/src/main/java/com/soren/bill/ui/stats/StatsScreen.kt`

- [ ] Replace corrupted Chinese labels with normal UTF-8 text.
- [ ] Move any `if` or `viewModelScope.launch` accidentally swallowed by comments onto executable lines.
- [ ] Run the source health verification again.

### Task 3: Make Account Balances Reactive

**Files:**
- Modify: `app/src/main/java/com/soren/bill/data/dao/TransactionDao.kt`
- Modify: `app/src/main/java/com/soren/bill/data/repository/BillRepository.kt`
- Modify: `app/src/main/java/com/soren/bill/ui/assets/AssetsViewModel.kt`

- [ ] Add a transaction Flow that emits whenever transactions change.
- [ ] Recompute account balances from the latest accounts and transactions in memory.
- [ ] Keep the existing account grouping UI contract.

### Task 4: Make Stats Reactive

**Files:**
- Modify: `app/src/main/java/com/soren/bill/ui/stats/StatsViewModel.kt`

- [ ] Combine category flows with the monthly transaction flow.
- [ ] Recompute totals and category breakdowns whenever transactions/categories change.
- [ ] Preserve the current `StatsUiState` shape.

### Task 5: Verify

**Commands:**
- `powershell -ExecutionPolicy Bypass -File scripts/verify-source.ps1`
- `gradle :app:compileDebugKotlin` if a Gradle executable is available.

- [ ] Report exact verification results and any environment blocker.
