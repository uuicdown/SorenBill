# Draft: UI/UX 审计 — Soren 的账单

## Requirements (confirmed)
- 用户询问："前端ui是否需要优化？交互还有没有什么地方可以设计一下？"
- 目标：基于代码库现状（Jetpack Compose + Material3），给出可执行的 UI/交互改进清单，优先级与验收标准，和若干可直接落地的设计改动建议（包含 QA 验证步骤）。

## Technical Decisions
- 我将静默扫描 app/src/... 下所有 Compose Screen、Navigation、主题与 DesignSystem 文件以发现可改进点（交互流、剪枝步骤、默认行为、可访问性、国际化漏洞、图标/资源策略、电池/性能暗病）。
- 输出将分为：P0（立即可落地，影响用户感知）、P1（工程化改进，需小幅 DB/VM 支持）、P2（更大改动，如重设计或 DI 迁移）。

## Research Findings
- （占位）将由探索代理填充：屏幕清单、可疑交互模式、字符串硬编码、设计系统碎片、输入/键盘焦点问题、性能/重组成本估计。

## Open Questions
1. 你偏好哪种改进路径？（偏向小步增量改进 vs 大幅重设计） — 我会用问卷确认并在计划中默认选择“增量改进”。

用户选择：大幅重设计（complete UX redesign）。下文问题将决定重设计的范围与交付物（我会默认建议分阶段进行，但会按你的偏好准备全面方案）。

## Scope Boundaries
- INCLUDE: Compose 屏幕的交互流、默认值、输入体验、icon 与资源管理、无障碍检查、屏幕启动时间相关的可见优化建议。
- EXCLUDE: 完整视觉重设计的高保真稿、外包 UI 设计资源（可在后续单列）。
