package com.soren.bill.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soren.bill.data.entity.Account
import com.soren.bill.data.entity.Category
import com.soren.bill.data.entity.Wallet
import com.soren.bill.ui.theme.categoryIcon
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.IncomeGreen
import com.soren.bill.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: AddTransactionViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saved) { if (uiState.saved) onNavigateBack() }

    val df = remember { SimpleDateFormat("MM月dd日", Locale.getDefault()) }
    val tf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("记一笔", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 类型
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = uiState.type == "expense", onClick = { viewModel.setType("expense") },
                    label = { Text("支出") }, modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ExpenseRed.copy(alpha = 0.12f)))
                FilterChip(selected = uiState.type == "income", onClick = { viewModel.setType("income") },
                    label = { Text("收入") }, modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IncomeGreen.copy(alpha = 0.12f)))
            }

            // 金额
            val color = if (uiState.type == "expense") ExpenseRed else IncomeGreen
            Text(if (uiState.amount.isEmpty()) "¥0" else "¥${uiState.amount}",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 42.sp, color = color))
            OutlinedTextField(value = uiState.amount, onValueChange = { viewModel.setAmount(it) },
                modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = { Text("输入金额") }, singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center))

            // 分类
            SectionLabel("分类")
            CategoryRow(
                cats = if (uiState.type == "expense") uiState.expenseCategories else uiState.incomeCategories,
                sel = uiState.selectedCategory, onClick = { viewModel.setCategory(it) })

            // 账户
            SectionLabel("支付账户")
            AccountRow(acts = uiState.accounts, sel = uiState.selectedAccount, onClick = { viewModel.setAccount(it) })

            // 钱包
            SectionLabel("钱包")
            WalletRow(wals = uiState.wallets, sel = uiState.selectedWallet, onClick = { viewModel.setWallet(it) })

            // 日期时间
            SectionLabel("日期")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                    Text(df.format(Date(uiState.date)), fontSize = 13.sp)
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                    Text(tf.format(Date(uiState.date)), fontSize = 13.sp)
                }
            }

            // 备注
            SectionLabel("备注")
            OutlinedTextField(value = uiState.note, onValueChange = { viewModel.setNote(it) },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("添加备注...") }, singleLine = true)

            Spacer(Modifier.height(8.dp))

            // 保存按钮
            Button(onClick = { viewModel.save() }, Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.amount.toDoubleOrNull()?.let { it > 0 } == true
                        && uiState.selectedCategory != null && uiState.selectedAccount != null && uiState.selectedWallet != null) {
                Text("保存", fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // Date picker
    if (showDate) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = uiState.date)
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = { dps.selectedDateMillis?.let { viewModel.setDate(it) }; showDate = false }) { Text("确定") }
        }, dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } }) { DatePicker(state = dps) }
    }

    // Time picker
    if (showTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.date }
        val tps = rememberTimePickerState(initialHour = cal.get(Calendar.HOUR_OF_DAY), initialMinute = cal.get(Calendar.MINUTE), is24Hour = true)
        AlertDialog(onDismissRequest = { showTime = false }, title = { Text("选择时间") },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = tps) } },
            confirmButton = {
                TextButton(onClick = {
                    cal.set(Calendar.HOUR_OF_DAY, tps.hour); cal.set(Calendar.MINUTE, tps.minute)
                    viewModel.setDate(cal.timeInMillis); showTime = false
                }) { Text("确定") }
            }, dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消") } })
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
}

@Composable
fun CategoryRow(cats: List<Category>, sel: Category?, onClick: (Category) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(cats, key = { it.id }) { c ->
            FilterChip(selected = sel?.id == c.id, onClick = { onClick(c) },
                label = { Text(c.name) },
                leadingIcon = { Icon(categoryIcon(c.name), null, Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
        }
    }
}

@Composable
fun AccountRow(acts: List<Account>, sel: Account?, onClick: (Account) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(acts, key = { it.id }) { a ->
            FilterChip(selected = sel?.id == a.id, onClick = { onClick(a) },
                label = { Text(a.name) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
        }
    }
}

@Composable
fun WalletRow(wals: List<Wallet>, sel: Wallet?, onClick: (Wallet) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(wals, key = { it.id }) { w ->
            FilterChip(selected = sel?.id == w.id, onClick = { onClick(w) },
                label = { Text(w.name) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
        }
    }
}
