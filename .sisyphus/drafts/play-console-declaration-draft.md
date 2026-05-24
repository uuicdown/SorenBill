# Draft: Play Console Permissions Declaration 与提交材料草稿

目的：为自动记账（使用 AccessibilityService + NotificationListener）准备可直接复制粘贴到 Play Console Permissions Declaration 表单的逐项答案、演示视频清单与必备材料清单。此稿为决策完备文本；你可直接复制到 Play Console 的相应输入框并上传演示视频与证据。

注意：以下文本假定自动记账功能遵循「本地处理、仅在用户明确同意并开启权限后才运行、默认以“建议/草稿”模式输出、不自动上传」的实现策略。若你希望改成云端/上传模型，请先告知，表单文本需调整。

一、表单填报（逐字段建议）
- Field: "Why does your app need to use the Accessibility Service API?"
  - 建议答案（中文，可直接翻译或保留中文）：
    "本应用使用 AccessibilityService 以读取用户同意授权的屏幕内容，用于在设备本机自动识别来自短信/通知/屏幕的交易信息（例如金额、商家、交易类型），并生成‘建议账目’供用户确认。该功能仅用于提高记账便利性；所有解析与识别均在本设备上完成，除非用户明确开启云备份，否则不会上传任何解析文本或敏感信息。"

- Field: "Do you collect and/or share personal or sensitive user data using the accessibility capabilities?"
  - 建议答案：Yes
  - 说明（供补充文本填写）：
    "我们会读取用户授权范围内的通知标题与正文、以及在用户启用时读取当前窗口的可见文本（仅用于解析交易相关数据）。收集的数据类型包括：通知标题/正文、交易金额、商家名称、交易时间戳。默认不上传、不共享；仅在用户显式启用云同步时才会上传，上传前会对要上传的字段做最小化与脱敏处理（例如仅上传商户名首字母与金额区间），并在隐私政策中明示。"

- Field: "Do you require the isAccessibilityTool attribute?"
  - 建议答案：No（本功能非为残障辅助而开发，务必如实选择）。

- Field: "Provide a detailed description of the specific data accessed or collected via Accessibility APIs"
  - 建议答案（逐点列出）：
    1. 通知元数据：应用包名、通知标题、通知正文、时间戳（仅用户已授权的通知来源）。
    2. 屏幕可见文本（仅在用户启用“屏幕读取”并在交互期间允许的情况下）用于解析收据/交易信息。
    3. 解析结果（在本地生成的建议账目：金额、商家、分类、日期），这些解析结果用于在应用内展示给用户并可选择保存为正式交易。仅当用户开启云备份时，解析结果的极小化字段可能会上传。

- Field: "Explain how the data will be used and/or shared"
  - 建议答案：
    "用途：在本设备本地解析通知或屏幕文本以自动识别交易并生成账目建议，用户可在应用内审阅并确认保存。共享：默认不共享；若用户在设置中显式启用云备份，才会把经脱敏/最小化处理的数据上传至用户指定云端存储。不会与广告商或第三方数据分析机构共享。"

- Field: "Does your app require a demo video?"
  - 建议答案：Yes（Play Console 很可能要求 demo，尤其使用 AccessibilityService）

- Field: "Provide steps to reproduce the usage of Accessibility APIs in the demo video"
  - 建议答案（可直接复制到表单）：见下方“演示视频逐字脚本”草稿（请把演示视频上传为 MP4，并在表单中粘贴视频链接）。

二、必须上传的证据与材料清单（提交包内请包含）
1. 演示视频（MP4，720p/1080p） — 文件名示例：sorenbill_accessibility_demo_v1.mp4
   - 必须包含：应用内醒目披露页面、用户点击“我同意并启用”、系统设置页面（Notification access / Accessibility）用户手动开启权限、回到应用显示“权限已启用”、示例通知到达并在应用内生成“建议账目”、用户确认建议账目保存为正式交易。
2. 醒目披露页面文本（在 PR/表单内粘贴） — 参见 drafts/play-console-disclosure.txt（下面也已给出多种文案）。
3. 隐私政策链接（App Store / privacy URL）并在其中明确列出 Accessibility/Notification 数据流与上传控制点。
4. Data Safety 表单的预填写信息（列出收集的数据类别及是否上传）——示例已在本草稿末尾给出。

三、演示视频逐字脚本（决策完备，逐秒说明、必须逐字朗读）
-- 总时长建议：60–120 秒（分段，务必包含醒目披露与用户主动同意场景）

片段 0 — 引导与开场（0:00–0:05）
  - 画面：App 启动画面（Soren 的账单 logo）
  - 朗读（旁白/字幕）：“下面演示 Soren 的账单如何在本机自动检测通知并生成记账建议。所有识别均在设备本地完成。”

片段 1 — 醒目披露页面（0:05–0:20）
  - 画面：打开 App → 导航到 Onboarding 权限页面（或首次启动自动弹出醒目披露）
  - 视觉要求：Disclosure 页面完整可见，文字清晰（不要把文本截断）。
  - 逐字朗读文本（必须逐字朗读并同时在屏幕上显示）：
    标题："需要通知与屏幕访问权限以启用自动记账"
    主体："为了在本机自动识别收据与交易信息，Soren 的账单需要读取你选择的通知（标题与正文）和在你启用时的屏幕内容。我们只在本设备上处理这些信息，用于生成记账建议；除非你显式开启云备份，否则不会上传任何内容。要继续，请点击“我同意并启用”。"
  - 交互：示范点击按钮 “我同意并启用”

片段 2 — 系统权限引导（0:20–0:40）
  - 画面：App 自动跳转到系统设置页面（Notification access / Accessibility Setting），显示如何开启（录屏时需放慢操作以便审查者看清）
  - 逐字朗读/提示文字（屏幕下方字幕即可）：“请在系统设置中为 Soren 的账单启用通知访问与无障碍服务。您可以随时在系统设置中撤销该权限。”
  - 交互：示范用户开启权限（点击开关），并返回 App

片段 3 — 权限已启用状态与示例通知（0:40–0:55）
  - 画面：App 返回并显示“权限已启用”的状态提示（Show toast/snackbar 或状态页）
  - 朗读/字幕："权限已启用 — 我们将仅在本地处理通知内容，用于生成记账建议。"
  - 交互：用另一台设备或模拟器发一条示例通知（例如：支付宝通知：“收到转账100.00元”）并录制通知在系统栏出现

片段 4 — 应用内展示建议账目（0:55–1:10）
  - 画面：App 自动收到通知并在“建议”或“待确认”面板出现一条建议账目条目，显示解析出的金额、商家、时间与分类建议
  - 朗读/字幕："系统检测到一条交易通知，已为您生成记账建议。请确认或编辑后保存。"
  - 交互：示范用户点击“确认保存”（或“编辑并保存”），并展示保存成功的反馈（如 Snackbar 或跳转到流水）

片段 5 — 撤销/设置与结尾（1:10–1:20）
  - 画面：展示设置页中关于自动记账的开关（可关闭），并演示如何撤销通知访问或无障碍权限（引导到系统设置）
  - 朗读/字幕："用户可随时在应用设置或系统设置中撤销权限或关闭自动记账功能。"
  - 画面结束：显示隐私政策链接与应用名，字幕："Soren 的账单 — 隐私第一。"

视频录制注意事项（决策完备）
- 格式：MP4，H.264，720p 或 1080p；音频：清晰旁白（推荐使用麦克风）；文件名： sorenbill_accessibility_demo_v1.mp4
- 必须在视频描述/Play Console 表单中填入：设备型号、Android 版本、录制人（开发者）姓名与邮件
- 建议同时上传一个 README.txt（与视频同目录）说明录制环境与每段时间点（timestamp）对应的操作

四、Data Safety / Privacy snippet（预填写建议，供 Data safety 表单使用）
- 我们收集的数据类型：Notification content (title, body), On-screen text (only when enabled), Transaction parsing results (amount, merchant, inferred category), Device identifiers only for crash reporting (optional)
- 是否上传：默认 No（除非用户显式启用云备份）
- 是否用于广告：No
- 是否用于 personalization：Yes (仅用于改善用户体验的本地建议)

五、审查清单（提交前请确认）
1. 醒目披露页面在 App 内独立存在并能被录屏完整显示（不要只在隐私政策或网页中放置）。
2. 视频中有显式的用户 Affirmative action（点击“我同意并启用”）。
3. Play Console Permissions Declaration 表单与演示视频中陈述一致（同一套文字）。
4. 隐私政策链接可访问并包含相同描述。  
5. Data safety 表单已按上面片段填写。

六、我会生成的交付物（接下来我会把它们写为草稿文件）
- .sisyphus/drafts/play-console-declaration-draft.md （本文件）
- .sisyphus/drafts/play-console-demo-video-script.md （逐字演示视频脚本，亦会生成以便直接朗读）

现在我已经准备好把逐字演示视频脚本写入 .sisyphus/drafts/play-console-demo-video-script.md 并生成表单的最终填报文本（英文与中文两套）。是否需要我同时生成英文版的表单草稿？（推荐：Yes，Play Console 的审查多人使用英文，建议同时提交中/英文本）
