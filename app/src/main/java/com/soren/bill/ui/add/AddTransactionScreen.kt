package com.soren.bill.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onNavigateBack()
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记一笔") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TypeSelector(
                selectedType = uiState.type,
                onTypeChange = { viewModel.setType(it) }
            )

            AmountInput(
                amount = uiState.amount,
                onAmountChange = { viewModel.setAmount(it) },
                type = uiState.type
            )

            SectionLabel("分类")
            CategorySelector(
                categories = if (uiState.type == "expense") uiState.expenseCategories else uiState.incomeCategories,
                selectedCategory = uiState.selectedCategory,
                onCategoryClick = { viewModel.setCategory(it) }
            )

            SectionLabel("支付账户")
            AccountSelector(
                accounts = uiState.accounts,
                selectedAccount = uiState.selectedAccount,
                onAccountClick = { viewModel.setAccount(it) }
            )

            SectionLabel("钱包")
            WalletSelector(
                wallets = uiState.wallets,
                selectedWallet = uiState.selectedWallet,
                onWalletClick = { viewModel.setWallet(it) }
            )

            SectionLabel("日期时间")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(dateFormat.format(Date(uiState.date)), fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(timeFormat.format(Date(uiState.date)), fontSize = 13.sp)
                }
            }

            SectionLabel("备注（可选）")
            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.setNote(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("比如：和朋友吃饭") },
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.amount.toDoubleOrNull()?.let { it > 0 } == true
                        && uiState.selectedCategory != null
                        && uiState.selectedAccount != null
                        && uiState.selectedWallet != null
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val oldCal = Calendar.getInstance().apply { timeInMillis = uiState.date }
                        val newCal = Calendar.getInstance().apply { timeInMillis = millis }
                        newCal.set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                        newCal.set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
                        viewModel.setDate(newCal.timeInMillis)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.date }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance().apply { timeInMillis = uiState.date }
                    newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    newCal.set(Calendar.MINUTE, timePickerState.minute)
                    viewModel.setDate(newCal.timeInMillis)
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun TypeSelector(selectedType: String, onTypeChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = selectedType == "expense",
            onClick = { onTypeChange("expense") },
            label = { Text("支出") },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ExpenseRed.copy(alpha = 0.15f)
            )
        )
        FilterChip(
            selected = selectedType == "income",
            onClick = { onTypeChange("income") },
            label = { Text("收入") },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = IncomeGreen.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun AmountInput(amount: String, onAmountChange: (String) -> Unit, type: String) {
    val color = if (type == "expense") ExpenseRed else IncomeGreen

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (amount.isEmpty()) "¥0" else "¥$amount",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                color = color
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("输入金额", textAlign = TextAlign.Center) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center,
                fontSize = 24.sp
            )
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategoryClick: (Category) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories, key = { it.id }) { category ->
            val selected = selectedCategory?.id == category.id
            FilterChip(
                selected = selected,
                onClick = { onCategoryClick(category) },
                label = { Text(category.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun AccountSelector(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountClick: (Account) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(accounts, key = { it.id }) { account ->
            val selected = selectedAccount?.id == account.id
            FilterChip(
                selected = selected,
                onClick = { onAccountClick(account) },
                label = { Text(account.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun WalletSelector(
    wallets: List<Wallet>,
    selectedWallet: Wallet?,
    onWalletClick: (Wallet) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(wallets, key = { it.id }) { wallet ->
            val selected = selectedWallet?.id == wallet.id
            FilterChip(
                selected = selected,
                onClick = { onWalletClick(wallet) },
                label = { Text(wallet.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
