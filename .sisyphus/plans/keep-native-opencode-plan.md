# 保留 OpenCode 原生 Plan/Build 模式（同时保留 oh-my-openagent 插件）

## TL;DR
> **目标**：在不卸载插件的前提下，恢复并长期保留 OpenCode 的原生 Plan/Build（Tab 切换）行为。优先采用“无代码”方案：通过插件配置与运行时进程级覆盖（OPENCODE_CONFIG_CONTENT）实现；仅在配置无效时做只读级别的插件代码定位与最小修补建议。
> **交付物**：
- 已决配置片段（JSON），用于写入：%USERPROFILE%\\.config\\opencode\\oh-my-opencode.json
- 两个 PowerShell 启动脚本（并可放入 profile）：start-opencode-with-omo.ps1、start-opencode-vanilla.ps1
- 完整验证脚本（自动化断言，生成日志文件）
> **估时**：短（15-45 分钟）——配置 + 验证。若需定位插件代码并申请修补：中（1-3 小时）。

## Context
用户在安装 oh-my-openagent 插件后发现 OpenCode 的原生 Plan/Build（Tab 切换）不再可用。已确认：
- 插件已在 %USERPROFILE%\\.config\\opencode\\opencode.json 中启用（"plugin": ["oh-my-openagent@latest"]）。
- 插件自身配置文件 %USERPROFILE%\\.config\\opencode\\oh-my-openagent.jsonc 中未发现直接的 modes/keybind 配置。
- 社区文档（参考）指出插件支持通过 sisyphus_agent 配置保留原生 agent：

```json
{
  "sisyphus_agent": {
    "default_builder_enabled": true,
    "replace_plan": false
  }
}
```

参考文档：https://www.opencodecn.com/docs/best-practices/oh-my-opencode-keep-native-agents
插件源码证据（判断逻辑）：https://github.com/code-yeongyu/oh-my-openagent/blob/.../agent-config-handler.ts

## 工作目标
- 在用户级配置中添加/合并 sisyphus_agent 设置，令插件不替换原生 Plan 行为。
- 提供可回滚、不可破坏的 PowerShell wrapper，用于：
  - 启动 "plugin-enabled + preserve native" 会话（用于日常使用）
  - 启动 "vanilla"（临时禁用插件并验证原生行为）会话（用于排查）
- 自动化验证：通过日志断言判断 Plan/Build 是否恢复（无需人工视觉确认）。

## 必要的决策（已做）
- 决定采用“无代码优先”策略：先尝试配置合并 + 运行时覆盖（OPENCODE_CONFIG_CONTENT）。
- 如果配置层无法生效，使用只读代码定位并提出最小化修补建议（不直接改插件源）。

## 验证策略（自动化）
- 测试框架：PowerShell 脚本 + 日志文件（.\opencode_output.log、.\opencode_output_disabled.log、.\test_opencode_env_dump.txt）
- 断言规则（自动）：启动日志包含关键词 "Plan" 或 "Build" 或 plugin 输出中能看到 sisyphus/builder 已加载的记录。

## 执行策略（波次）
- 并行：此方案主要串行、短任务（改配置 → 重启验证 → wrapper 制作 → 验证）。

### 任务清单（决策完备：每一步都含准确命令、接受条件、回滚指令）

- [ ] 1. 备份当前配置（必须，回滚点）

  What to do:
  - 创建备份目录并复制现有配置文件（如果存在）到备份目录。

  Commands (PowerShell, 逐行可复制)：
  ```powershell
  $cfgDir = Join-Path $env:USERPROFILE ".config\opencode"
  $backupDir = Join-Path $cfgDir "backup-keep-native-$(Get-Date -Format yyyyMMddHHmmss)"
  New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
  # 备份主配置与插件配置（若存在）
  foreach ($f in @('opencode.json','opencode.json.tmp','oh-my-openagent.jsonc','oh-my-opencode.json')) {
    $p = Join-Path $cfgDir $f
    if (Test-Path $p) { Copy-Item -Path $p -Destination $backupDir -Force }
  }
  Write-Output "BACKUP_DIR=$backupDir"
  ```

  Acceptance Criteria:
  - 备份目录存在且包含被复制的配置文件。

  Rollback:
  - 若需恢复，复制备份文件回原位置：
  ```powershell
  Copy-Item -Path (Join-Path $backupDir '*') -Destination $cfgDir -Force
  ```

- [ ] 2. 在用户级 oh-my-opencode 配置中合并 sisyphus_agent（最小持久修复）

  What to do:
  - 将以下 JSON 片段合并到 %USERPROFILE%\\.config\\opencode\\oh-my-opencode.json（不存在则新建）。

  JSON to ensure (exact):
  ```json
  {
    "sisyphus_agent": {
      "default_builder_enabled": true,
      "replace_plan": false
    }
  }
  ```

  Commands (PowerShell, 合并逻辑，非破坏)：
  ```powershell
  $cfgFile = Join-Path $env:USERPROFILE ".config\opencode\oh-my-opencode.json"
  if (-not (Test-Path $cfgFile)) {
    New-Item -ItemType File -Path $cfgFile -Force | Out-Null
    '{"sisyphus_agent": {"default_builder_enabled": true, "replace_plan": false}}' | Set-Content -Path $cfgFile -Encoding UTF8
  } else {
    $json = Get-Content $cfgFile -Raw | ConvertFrom-Json
    if (-not $json.sisyphus_agent) { $json | Add-Member -MemberType NoteProperty -Name sisyphus_agent -Value @{} }
    $json.sisyphus_agent.default_builder_enabled = $true
    $json.sisyphus_agent.replace_plan = $false
    $json | ConvertTo-Json -Depth 10 | Set-Content -Path $cfgFile -Encoding UTF8
  }
  Write-Output "WROTE: $cfgFile"
  ```

  Acceptance Criteria:
  - 文件 %USERPROFILE%\\.config\\opencode\\oh-my-opencode.json 存在且其 JSON 包含 sisyphus_agent.default_builder_enabled = true 且 replace_plan = false。

  Rollback:
  - 用步骤 1 的备份恢复该文件。

- [ ] 3. 创建 PowerShell wrapper（建议放在 %USERPROFILE%\\.config\\opencode\\scripts 或直接写入 PowerShell profile）

  What to do:
  - 在 %USERPROFILE%\\.config\\opencode\\scripts 下创建两个脚本：start-opencode-with-omo.ps1 与 start-opencode-vanilla.ps1（内容见下），并将目录加入 PATH（可选）或将函数写入 PowerShell profile。

  Files & exact content (copy-paste ready):

  File: %USERPROFILE%\\.config\\opencode\\scripts\\start-opencode-with-omo.ps1
  ```powershell
  param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Args)

  $cfg = @'
  {
    "sisyphus_agent": {
      "default_builder_enabled": true,
      "replace_plan": false
    }
  }
  '@

  $env:OPENCODE_CONFIG_CONTENT = $cfg
  # 启动 opencode 并把输出写入日志
  & opencode @Args *> "$PWD\opencode_output.log" 2>&1
  Remove-Item Env:OPENCODE_CONFIG_CONTENT -ErrorAction SilentlyContinue
  ```

  File: %USERPROFILE%\\.config\\opencode\\scripts\\start-opencode-vanilla.ps1
  ```powershell
  param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Args)

  # 读取磁盘配置并在进程级移除 oh-my-openagent 插件条目（不改磁盘）
  $cfgFile = Join-Path $env:USERPROFILE ".config\opencode\opencode.json"
  if (-not (Test-Path $cfgFile)) { Write-Error "找不到 $cfgFile"; exit 2 }
  $cfg = Get-Content $cfgFile -Raw | ConvertFrom-Json
  if ($cfg.plugin) { $cfg.plugin = $cfg.plugin | Where-Object { $_ -notmatch "^oh-my-openagent(@.*)?$" } }
  $env:OPENCODE_CONFIG_CONTENT = ($cfg | ConvertTo-Json -Depth 10)
  & opencode @Args *> "$PWD\opencode_output_disabled.log" 2>&1
  Remove-Item Env:OPENCODE_CONFIG_CONTENT -ErrorAction SilentlyContinue
  ```

  Acceptance Criteria:
  - 两个脚本文件存在且可执行（Test-Path 返回 true）；执行 start-opencode-with-omo.ps1 后生成 opencode_output.log；执行 start-opencode-vanilla.ps1 后生成 opencode_output_disabled.log。

  Rollback:
  - 删除脚本或从 PATH 中移除脚本目录。

- [ ] 4. 自动化验证（按 Metis 的 Acceptance tests A/B/C）

  What to do:
  - 运行以下三项自动测试来断言覆盖生效与 TUI 恢复情况。

  Test A — 验证环境变量注入
  ```powershell
  $json = '{"sisyphus_agent": {"default_builder_enabled": true, "replace_plan": false}}'
  $env:OPENCODE_CONFIG_CONTENT = $json
  Write-Output $env:OPENCODE_CONFIG_CONTENT | Out-File -FilePath .\test_opencode_env_dump.txt -Encoding utf8
  if (-not ((Get-Content -Raw .\test_opencode_env_dump.txt) -match 'sisyphus_agent')) { Write-Error 'ENV_NOT_SET'; exit 2 } else { Write-Output 'ENV_OK'; exit 0 }
  ```

  Test B — 启动 plugin-enabled 并检查日志
  ```powershell
  # 运行 start-opencode-with-omo.ps1（脚本会写 opencode_output.log）
  & "$env:USERPROFILE\.config\opencode\scripts\start-opencode-with-omo.ps1"
  Start-Sleep -Seconds 3
  if (-not (Test-Path .\opencode_output.log)) { Write-Error 'NO_OUTPUT_LOG'; exit 3 }
  $content = Get-Content -Raw .\opencode_output.log
  if ($content -match 'Plan' -or $content -match 'Build' -or $content -match 'sisyphus') { Write-Output 'TUI_POSSIBLE'; exit 0 } else { Write-Error 'TUI_NOT_DETECTED'; exit 4 }
  ```

  Test C — 启动 vanilla（插件临时禁用）并检查日志对比
  ```powershell
  & "$env:USERPROFILE\.config\opencode\scripts\start-opencode-vanilla.ps1"
  Start-Sleep -Seconds 3
  if (-not (Test-Path .\opencode_output_disabled.log)) { Write-Error 'NO_OUTPUT_LOG_DISABLED'; exit 6 }
  $contentDisabled = Get-Content -Raw .\opencode_output_disabled.log
  if ($contentDisabled -match 'Plan' -or $contentDisabled -match 'Build') { Write-Output 'TUI_PRESENT_WITHOUT_PLUGIN'; exit 0 } else { Write-Error 'TUI_MISSING_WITHOUT_PLUGIN'; exit 5 }
  ```

  Interpretation:
  - 若 Test B 成功且 Test C 失败 → 插件正在替换/拦截原生 Plan，需更深入定位插件代码或请求 upstream 改进（进入任务 5）。
  - 若 Test B 与 Test C 都成功 → 插件已保留原生 Plan（配置成功），结束。

- [ ] 5. （后备）只读定位插件拦截点并提出修复建议

  When to do:
  - 仅在步骤 2-4 未能恢复原生 Plan/Build 时触发。

  What to do (read-only):
  - 在插件安装目录（通常在 %USERPROFILE%\\.config\\opencode\\node_modules\\ 或全局 node_modules）搜索下列关键字：
    - replace_plan, default_builder_enabled, registerMode, replacePlan, keybind, keypress, onKey, intercept, TUI
  - Windows PowerShell example:
  ```powershell
  $pluginDir = Join-Path $env:USERPROFILE ".config\opencode\node_modules\@opencode-ai\oh-my-openagent"
  if (-not (Test-Path $pluginDir)) { Write-Error "插件目录未找到： $pluginDir"; exit 10 }
  Get-ChildItem -Path $pluginDir -Recurse -Include *.js,*.ts,*.mjs | Select-String -Pattern 'replace_plan|default_builder_enabled|replacePlan|registerMode|keybind|keypress|onKey|intercept' -List | Out-File .\plugin_search_results.txt -Encoding utf8
  Write-Output "SEARCH_RESULTS=plugin_search_results.txt"
  ```

  Acceptance Criteria:
  - plugin_search_results.txt 非空，包含至少一处可能与 modes/plan 替换相关的代码位置。

  Next (proposal):
  - 基于匹配结果，准备两套输出：
    1) 精确的只读补丁建议（修改点、代码片段、建议的配置键）供用户或维护者应用；
    2) 若用户授权，可生成 PR / issue 模板提交给插件维护者请求添加配置开关（e.g., "preserve_native_modes": true）。

- [ ] 6. 回滚与清理（始终可执行）

  Commands:
  ```powershell
  # 恢复备份
  Copy-Item -Path (Join-Path $backupDir '*') -Destination (Join-Path $env:USERPROFILE '.config\opencode') -Force
  # 删除脚本
  Remove-Item -Path (Join-Path $env:USERPROFILE '.config\opencode\scripts') -Recurse -Force
  # 清理日志
  Remove-Item -Path .\opencode_output*.log -ErrorAction SilentlyContinue
  ```

  Acceptance Criteria:
  - 原配置文件被恢复且 opencode 行为回到安装前状态。

## 依赖与参考（证据）
- 社区文档（中文）：https://www.opencodecn.com/docs/best-practices/oh-my-opencode-keep-native-agents
- 插件源码（判断逻辑）：https://github.com/code-yeongyu/oh-my-openagent/blob/de7f1d887db6f274ae67075a360cd05d45f59179/src/plugin-handlers/agent-config-handler.ts
- 社区 issue（OPENCODE_CONFIG_CONTENT wrapper）：https://github.com/code-yeongyu/oh-my-openagent/issues/673

## 最终验证波（必须由用户明确批准才能完成计划）
- F1. Plan 合规审计（检查：所有文件路径、命令与 JSON 片段准确无误） — 执行者：Prometheus/Oracle
- F2. 配置变更与脚本 QA（执行 acceptance tests A/B/C） — 执行者：Sisyphus-Junior
- F3. 手动 TUI 验证（可选，用户做视觉确认） — 执行者：人工 QA 或 Playwright（若能脚本化）
- F4. Scope 一致性检查（确保未修改其他 opencode 插件或项目级配置） — 执行者：Metis

## 成功标准
- 最终可在不卸载 oh-my-openagent 的情况下：
  1) 通过 start-opencode-with-omo.ps1 启动时，Plan/Build（Tab 切换）行为与原生 OpenCode 一致；或
  2) 至少可以通过 start-opencode-vanilla.ps1 恢复原生行为并确认插件确实是替换源（以便决定下一步）。

## 决策需要用户反馈（如果有）
- 是否要把脚本写入你的 PowerShell profile（$PROFILE）以便每次可直接调用？（yes/no，默认：no）
- 是否授权我在失败时进行只读插件代码定位（任务 5）并返回可复制的修补建议？（yes/no，默认：yes）

## 用户已决事项
- 用户选择：把启动脚本写入全局 PowerShell profile（$PROFILE），使脚本在所有 PowerShell 会话中可用。脚本目录仍为 %USERPROFILE%\\.config\\opencode\\scripts（默认）。
- 用户授权：在配置方法失败时执行只读插件代码定位并返回可复制的修补建议（同意）。

---
Plan 文件已保存： .sisyphus/plans/keep-native-opencode-plan.md
