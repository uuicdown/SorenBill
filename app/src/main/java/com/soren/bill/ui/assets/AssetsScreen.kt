package com.soren.bill.ui.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soren.bill.data.entity.Account
import com.soren.bill.ui.theme.AccountIcon
import com.soren.bill.ui.theme.accountBrandColor
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.IncomeGreen
import com.soren.bill.ui.theme.SorenCardShape
import com.soren.bill.ui.theme.SorenDialogShape
import com.soren.bill.ui.theme.sorenShadow
import com.soren.bill.util.DateUtils

private val bankList = listOf(
    "中国工商银行","中国建设银行","中国农业银行","中国银行","交通银行","招商银行",
    "邮政储蓄银行","浦发银行","中信银行","光大银行","民生银行","兴业银行",
    "广发银行","华夏银行","平安银行","北京银行","上海银行","江苏银行",
    "南京银行","宁波银行","徽商银行","杭州银行","浙商银行","渤海银行","恒丰银行","其他银行"
)
private val creditList = listOf("花呗","借呗","京东白条","美团月付","微粒贷","度小满")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(viewModel: AssetsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AccountBalance?>(null) }
    var walletExp by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 标题 + 钱包切换
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("资产", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    // 钱包下拉
                    ExposedDropdownMenuBox(expanded = walletExp, onExpandedChange = { walletExp = it }) {
                        OutlinedTextField(
                            value = uiState.selectedWalletName,
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExp) },
                            modifier = Modifier.width(140.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenu(expanded = walletExp, onDismissRequest = { walletExp = false }) {
                            uiState.wallets.forEach { w ->
                                DropdownMenuItem(text = { Text(w.name) }, onClick = { viewModel.selectWallet(w.id); walletExp = false })
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { showAdd = true }) { Text("添加", fontSize = 14.sp) }
                }
            }
            item { NetAssetCard(uiState.netAsset, uiState.totalAsset, uiState.totalLiability) }
            uiState.groups.forEach { group ->
                if (group.accounts.isNotEmpty()) {
                    item { GroupHeader(group.label) }
                    items(group.accounts, key = { it.account.id }) { bal -> AccountCard(bal) { editing = bal } }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
        FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape, elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "添加") }
    }
    if (showAdd) AddDialog(onDismiss = { showAdd = false }, onConfirm = { n, t, l, d, b -> viewModel.addAccount(n, t, l, d, b); showAdd = false; snackMsg = "已添加「$n」" })
    if (editing != null) EditDialog(bal = editing!!, onDismiss = { editing = null },
        onSave = { viewModel.updateAccount(it); editing = null },
        onAdjust = { viewModel.adjustBalance(editing!!, it); editing = null },
        onDelete = { viewModel.deleteAccount(editing!!.account); editing = null })
}

@Composable
fun NetAssetCard(net: Double, total: Double, liability: Double) {
    Surface(shape = SorenCardShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth().sorenShadow()) {
        Column(Modifier.padding(18.dp)) {
            Text("净资产（元）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(DateUtils.formatAmount(net), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = if (net >= 0) IncomeGreen else ExpenseRed))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column { Text("总资产", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(DateUtils.formatAmount(total), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = IncomeGreen) }
                Column { Text("负资产", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(DateUtils.formatAmount(liability), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = ExpenseRed) }
            }
        }
    }
}

@Composable
fun GroupHeader(label: String) = Text(label, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun AccountCard(bal: AccountBalance, onClick: () -> Unit) {
    Surface(shape = SorenCardShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth().clickable { onClick() }.sorenShadow()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AccountIcon(bal.account.type, bal.account.name, 28.dp)
            Spacer(Modifier.width(10.dp))
            Text(bal.account.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(DateUtils.formatAmount(bal.balance), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (bal.balance >= 0) IncomeGreen else ExpenseRed))
        }
        if (bal.account.type == "credit_card" && bal.account.creditLimit > 0 && bal.account.paymentDueDay > 0) {
            val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
            val due = bal.account.paymentDueDay
            val days = if (due > today) due - today else due + java.util.Calendar.getInstance().getActualMaximum(java.util.Calendar.DAY_OF_MONTH) - today
            Text("可用 ${DateUtils.formatAmount(bal.account.creditLimit + bal.balance)} · 距还款 ${days}天", Modifier.padding(start = 52.dp, bottom = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EditDialog(bal: AccountBalance, onDismiss: () -> Unit, onSave: (Account) -> Unit, onAdjust: (Double) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(bal.account.name) }
    var limit by remember { mutableStateOf(if (bal.account.creditLimit > 0) bal.account.creditLimit.toLong().toString() else "") }
    var dueDay by remember { mutableStateOf(if (bal.account.paymentDueDay > 0) bal.account.paymentDueDay.toString() else "") }
    var newBal by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, shape = SorenDialogShape, title = { Text("编辑账户") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("当前余额", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatAmount(bal.balance), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (bal.balance >= 0) IncomeGreen else ExpenseRed))
                }
            }
            OutlinedTextField(value = newBal, onValueChange = { newBal = it }, label = { Text("修改为") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            HorizontalDivider()
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (bal.account.type == "credit_card") {
                OutlinedTextField(value = limit, onValueChange = { limit = it }, label = { Text("信用额度") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dueDay, onValueChange = { dueDay = it }, label = { Text("还款日") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }, confirmButton = {
        TextButton(onClick = {
            onSave(bal.account.copy(name = name.trim().ifBlank { bal.account.name }, creditLimit = limit.toDoubleOrNull() ?: bal.account.creditLimit, paymentDueDay = dueDay.toIntOrNull() ?: bal.account.paymentDueDay))
            newBal.toDoubleOrNull()?.let { onAdjust(it) }
        }) { Text("保存") }
    }, dismissButton = { Row { TextButton(onClick = onDelete) { Text("删除", color = ExpenseRed) }; TextButton(onClick = onDismiss) { Text("取消") } } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double, Int, Double) -> Unit) {
    var name by remember { mutableStateOf("") }; var cat by remember { mutableStateOf("online") }
    var onlineType by remember { mutableStateOf("wechat") }; var bankIdx by remember { mutableStateOf(0) }
    var creditIdx by remember { mutableStateOf(0) }; var bankExp by remember { mutableStateOf(false) }
    var creditExp by remember { mutableStateOf(false) }; var limit by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }; var bal by remember { mutableStateOf("") }; var isCredit by remember { mutableStateOf(false) }
    val cats = listOf("online" to "在线支付", "bank" to "网银", "credit" to "网贷/信用")
    AlertDialog(onDismissRequest = onDismiss, shape = SorenDialogShape, title = { Text("添加账户") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { cats.forEach { (v, l) -> FilterChip(selected = cat == v, onClick = { cat = v; isCredit = false }, label = { Text(l, fontSize = 12.sp) }, modifier = Modifier.weight(1f)) } }
            when (cat) {
                "online" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("wechat" to "微信", "alipay" to "支付宝").forEach { (v, l) -> FilterChip(selected = onlineType == v, onClick = { onlineType = v }, label = { Text(l) }, modifier = Modifier.weight(1f)) }
                }
                "credit" -> ExposedDropdownMenuBox(expanded = creditExp, onExpandedChange = { creditExp = it }) {
                    OutlinedTextField(value = creditList[creditIdx], onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = creditExp) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = creditExp, onDismissRequest = { creditExp = false }) { creditList.forEachIndexed { i, c -> DropdownMenuItem(text = { Text(c) }, onClick = { creditIdx = i; creditExp = false }) } }
                }
                "bank" -> {
                    ExposedDropdownMenuBox(expanded = bankExp, onExpandedChange = { bankExp = it }) {
                        OutlinedTextField(value = bankList[bankIdx], onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExp) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                        ExposedDropdownMenu(expanded = bankExp, onDismissRequest = { bankExp = false }) { bankList.forEachIndexed { i, b -> DropdownMenuItem(text = { Text(b) }, onClick = { bankIdx = i; bankExp = false }) } }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("信用卡", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.weight(1f)); Switch(checked = isCredit, onCheckedChange = { isCredit = it }) }
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bal, onValueChange = { bal = it }, label = { Text("当前余额（元）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            if ((cat == "bank" && isCredit) || cat == "credit") { OutlinedTextField(value = limit, onValueChange = { limit = it }, label = { Text("信用额度") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(value = due, onValueChange = { due = it }, label = { Text("还款日") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        }
    }, confirmButton = {
        TextButton(onClick = {
            val ft = when (cat) { "online" -> onlineType; "credit" -> "credit_card"; else -> if (isCredit) "credit_card" else "bank_card" }
            val fn = name.trim().ifBlank { when (cat) { "online" -> if (onlineType == "wechat") "微信" else "支付宝"; "credit" -> creditList[creditIdx]; else -> bankList[bankIdx] } }
            onConfirm(fn, ft, limit.toDoubleOrNull() ?: 0.0, due.toIntOrNull() ?: 0, bal.toDoubleOrNull() ?: 0.0)
        }) { Text("添加") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
