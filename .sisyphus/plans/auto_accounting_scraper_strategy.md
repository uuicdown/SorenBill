# WeChat/Alipay Auto Accounting Scraper Strategy

Based on analysis of open-source auto-accounting apps (like AutoAccounting, Qianji_auto, ZhiWeiJZ, and various AccessibilityService scripts on GitHub/Gitee), here is the synthesized strategy for extracting payment data (merchant, amount, order number) from WeChat and Alipay robustly.

## 1. Trigger & Window Identification
- **Service Type**: `AccessibilityService`
- **Events**: Listen to `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`.
- **Packages**: `com.tencent.mm` (WeChat) and `com.eg.android.AlipayGphone` (Alipay).
- **Target Screens**: 
  - Payment Success screens
  - Bill Details screens

## 2. Extraction Strategy & Version Robustness
### The Problem with Hardcoded View IDs
Almost all modern and robust open-source scrapers **avoid** using `viewIdResourceName` (e.g., `com.tencent.mm:id/aq7`).
- **Why**: WeChat and Alipay heavily obfuscate their View IDs, and these IDs change with almost every minor app update. Relying on them leads to a high maintenance burden.

### The Robust Approach: Text-Based & Structure-Based Matching
The industry standard for no-root accessibility parsing is to use **Text Matching** combined with **Relative Node Traversal**.

#### A. Identifying the Screen
Look for unique text markers to confirm we are on a relevant screen:
- **WeChat**: "支付成功", "微信支付", "账单详情"
- **Alipay**: "支付成功", "账单详情", "交易详情"

#### B. Extracting the Amount
- **Anchor**: Use the currency symbol `"￥"` or `"¥"` as an anchor node (`findAccessibilityNodeInfosByText("¥")`).
- **Target**: Once the anchor is found, the amount is usually:
  1. The immediate next sibling node.
  2. If the text itself is `"¥15.50"`, extract using Regex: `¥?([0-9]+(\.[0-9]{1,2})?)`.
- **Validation**: Ensure the extracted string parses to a valid `Double`.

#### C. Extracting Merchant & Order Number
Use Key-Value pair heuristics. In bill details, data is usually presented in a row/list structure:
- **Keys**: 
  - Merchant: "收款方", "商户名称", "交易对方"
  - Order Number: "订单号", "交易单号", "商户单号"
- **Traversal Strategy**:
  1. Find the Key node using `findAccessibilityNodeInfosByText("收款方")`.
  2. Look for the Value node. The value is typically:
     - The sibling node within the same parent (e.g., `node.getParent().getChild(1)` if key is child `0`).
     - Or the next linearly focusable node in the screen reader traversal order.
     - Sometimes the Key and Value are merged in one string: `"收款方：XX超市"`, requiring a split by `":"` or `"："`.

## 3. Dealing with Edge Cases
- **Delayed Loading**: The Accessibility event may fire before the network request completes and the UI is fully populated. Scrapers usually implement a small delay or retry loop (e.g., check every 500ms for up to 3 seconds) until the target nodes appear.
- **Multiple "¥" Symbols**: There might be discounts, balances, or original prices shown. The actual paid amount is usually the largest text size or the first valid amount in the main header layout.
- **Root/Xposed Alternative**: For maximum stability, apps like AutoAccounting provide an Xposed/Shizuku module mode that hooks directly into local SQLite databases or WebView data structures, completely bypassing UI flakiness. However, for a standard app without root, the Accessibility Text + Sibling Traversal is the most robust method.

## Conclusion
To avoid breaking during WeChat/Alipay updates:
1. **Never use hardcoded `viewIdResourceName`**.
2. Query nodes by text: `findAccessibilityNodeInfosByText`.
3. Locate Values by finding their relative position to constant Keys (Siblings of "收款方", "订单号", "￥").
4. Fallback to Regex extraction on merged text blocks.