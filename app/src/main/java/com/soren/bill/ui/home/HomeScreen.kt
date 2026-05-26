package com.soren.bill.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soren.bill.util.DateUtils
import com.soren.bill.ui.theme.SorenCardShape
import com.soren.bill.ui.theme.bounceClick
import com.soren.bill.ui.theme.categoryIcon
import com.soren.bill.ui.theme.sorenShadow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onAddClick: () -> Unit, onStatsClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTx by remember { mutableStateOf<com.soren.bill.data.entity.Transaction?>(null) }
    var selectedDayDate by remember { mutableStateOf(0L) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .sorenShadow(),
                shape = SorenCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                HomeMonthHeader(
                    timestamp = uiState.currentMonthTimestamp,
                    onSwitchMonth = { viewModel.switchMonth(it) },
                    onStatsClick = onStatsClick,
                    onDaySelected = { selectedDayDate = it }
                )
            }
            SummaryCard(income = uiState.monthlyIncome, expense = uiState.monthlyExpense)

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (selectedDayDate > 0) {
                DayTransactionList(selectedDayDate, uiState) { selectedTx = it }
            } else if (uiState.transactions.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(bottom = 72.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("\u8fd9\u4e2a\u6708\u8fd8\u6ca1\u5f00\u59cb\u8bb0\u8d26\u5462\uff5e", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Soren \u5728\u7b49\u60a8\u7684\u7b2c\u4e00\u7b14\u8bb0\u5f55", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(uiState.transactions, key = { it.id }) { tx ->
                        TxRow(tx, uiState.categoryMap[tx.categoryId] ?: "\u672a\u77e5") { selectedTx = tx }
                    }
                }
            }
        }
        FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape, elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "\u8bb0\u4e00\u7b14") }

        selectedTx?.let { tx ->
            TransactionDetailDialog(
                tx = tx,
                categoryName = uiState.categoryMap[tx.categoryId] ?: "\u672a\u77e5",
                walletName = uiState.wallets.find { it.id == tx.walletId }?.name ?: "\u672a\u77e5\u94b1\u5305",
                accountName = uiState.accounts.find { it.id == tx.accountId }?.name ?: "\u672a\u77e5\u8d26\u6237",
                onDismiss = { selectedTx = null },
                onDelete = { viewModel.deleteTransaction(tx); selectedTx = null }
            )
        }
    }
}

@Composable
private fun DayTransactionList(
    selectedDayDate: Long,
    uiState: HomeUiState,
    onTxClick: (com.soren.bill.data.entity.Transaction) -> Unit
) {
    val dayStart = Calendar.getInstance().apply { timeInMillis = selectedDayDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
    val dayEnd = dayStart + 24L * 60 * 60 * 1000 - 1
    val dayTxs = uiState.transactions.filter { it.date in dayStart..dayEnd }
    val dateLabel = remember(selectedDayDate) { SimpleDateFormat("M\u6708d\u65e5 EEEE", Locale.CHINA).format(Date(selectedDayDate)) }

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("📅 $dateLabel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { /* \u901a\u8fc7 Header \u7684 onSwitchMonth \u56de\u5230\u5f53\u6708 */ }) {
            Text("\u6536\u8d77", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (dayTxs.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\u8fd9\u5929\u6ca1\u6709\u8bb0\u5f55\u5462\uff5e", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            items(dayTxs, key = { it.id }) { tx ->
                TxRow(tx, uiState.categoryMap[tx.categoryId] ?: "\u672a\u77e5") { onTxClick(tx) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMonthHeader(timestamp: Long, onSwitchMonth: (Long) -> Unit, onStatsClick: () -> Unit, onDaySelected: (Long) -> Unit) {
    val cal = Calendar.getInstance()
    val tc = Calendar.getInstance().apply { timeInMillis = timestamp }
    val isThisMonth = tc.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && tc.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
    var showPicker by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { Calendar.getInstance().apply { timeInMillis = timestamp; add(Calendar.MONTH, -1) }.let { onSwitchMonth(it.timeInMillis) } }) {
            Icon(Icons.Default.ChevronLeft, "\u4e0a\u4e2a\u6708", tint = MaterialTheme.colorScheme.primary)
        }
        Text(DateUtils.formatMonth(timestamp), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = { showPicker = true }) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text("${cal.get(Calendar.DAY_OF_MONTH)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = onStatsClick) { Icon(Icons.Default.PieChart, "\u7edf\u8ba1", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        IconButton(onClick = { Calendar.getInstance().apply { timeInMillis = timestamp; add(Calendar.MONTH, 1) }.let { onSwitchMonth(it.timeInMillis) } }, enabled = !isThisMonth) {
            Icon(Icons.Default.ChevronRight, "\u4e0b\u4e2a\u6708", tint = if (isThisMonth) Color.Transparent else MaterialTheme.colorScheme.primary)
        }
    }

    if (showPicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton(onClick = { dps.selectedDateMillis?.let { onDaySelected(it); onSwitchMonth(it) }; showPicker = false }) { Text("\u8df3\u8f6c") }
        }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text("\u53d6\u6d88") } }) {
            DatePicker(state = dps)
        }
    }
}

@Composable
fun SummaryCard(income: Double, expense: Double) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).sorenShadow(), shape = SorenCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SumItem("\u5165\u8d26", income, com.soren.bill.ui.theme.IncomeGreen, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
            SumItem("\u82b1\u9500", expense, com.soren.bill.ui.theme.ExpenseRed, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
            SumItem("\u5269\u4f59", income - expense, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        }
    }
}

@Composable
fun SumItem(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(DateUtils.formatAmount(amount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color))
    }
}

@Composable
fun TxRow(tx: com.soren.bill.data.entity.Transaction, categoryName: String, onClick: () -> Unit) {
    val isExpense = tx.type == "expense"
    val color = if (isExpense) com.soren.bill.ui.theme.ExpenseRed else com.soren.bill.ui.theme.IncomeGreen
    val bgColor = color.copy(alpha = 0.12f)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .sorenShadow()
            .clip(SorenCardShape)
            .background(MaterialTheme.colorScheme.surface)
            .bounceClick { onClick() }
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(categoryName), null, Modifier.size(24.dp), tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(DateUtils.formatDisplayDate(tx.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!tx.note.isNullOrBlank()) { Text(" \u00b7 ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(tx.note, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                }
            }
            Text(if (isExpense) "-${DateUtils.formatAmount(tx.amount)}" else "+${DateUtils.formatAmount(tx.amount)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = color))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailDialog(tx: com.soren.bill.data.entity.Transaction, categoryName: String, walletName: String, accountName: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var showDel by remember { mutableStateOf(false) }
    val isExpense = tx.type == "expense"
    val color = if (isExpense) com.soren.bill.ui.theme.ExpenseRed else com.soren.bill.ui.theme.IncomeGreen
    val bgColor = color.copy(alpha = 0.12f)
    val timeFormat = remember { SimpleDateFormat("yyyy\u5e74MM\u6708dd\u65e5 HH:mm", Locale.CHINA) }
    val timeStr = remember(tx.date) { timeFormat.format(Date(tx.date)) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) { Icon(categoryIcon(categoryName), null, Modifier.size(32.dp), tint = color) }
            Spacer(Modifier.height(16.dp))
            Text(categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(if (isExpense) "-${DateUtils.formatAmount(tx.amount)}" else "+${DateUtils.formatAmount(tx.amount)}", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = color))
            Spacer(Modifier.height(32.dp))
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                DetailField("\u8bb0\u5f55\u65f6\u95f4", timeStr)
                DetailField("\u6240\u5c5e\u94b1\u5305", walletName)
                DetailField("\u652f\u4ed8\u8d26\u6237", accountName)
                if (!tx.note.isNullOrBlank()) DetailField("\u5907\u6ce8\u4fe1\u606f", tx.note)
            }
            Spacer(Modifier.height(32.dp))
            TextButton(onClick = { showDel = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.textButtonColors(contentColor = com.soren.bill.ui.theme.ExpenseRed)) {
                Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("\u5220\u9664\u6b64\u8bb0\u5f55", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (showDel) AlertDialog(onDismissRequest = { showDel = false }, title = { Text("\u5220\u9664\u7ed9\u5c5e") }, text = { Text("\u786e\u5b9a\u8981\u5220\u6389\u8fd9\u6761\u8bb0\u5f55\u5417\uff1fSoren \u4f1a\u6709\u70b9\u5fc3\u75bc\u5462") },
        confirmButton = { TextButton(onClick = { onDelete(); showDel = false }) { Text("\u5220\u9664", color = com.soren.bill.ui.theme.ExpenseRed) } },
        dismissButton = { TextButton(onClick = { showDel = false }) { Text("\u53d6\u6d88") } })
}

@Composable
private fun DetailField(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}