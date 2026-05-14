package com.soren.bill.ui.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.IncomeGreen
import com.soren.bill.util.DateUtils
import java.util.Calendar

@Composable
fun AssetsScreen(viewModel: AssetsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加账户")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("资产", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("添加账户", fontSize = 14.sp)
                    }
                }
            }

            item {
                NetAssetCard(
                    netAsset = uiState.netAsset,
                    totalAsset = uiState.totalAsset,
                    totalLiability = uiState.totalLiability
                )
            }

            val bankCards = uiState.groups.find { it.label == "储蓄卡" }
            if (bankCards != null && bankCards.accounts.isNotEmpty()) {
                item { GroupHeader(label = bankCards.label, total = bankCards.totalBalance) }
                items(bankCards.accounts, key = { it.account.id }) { AssetAccountItem(it) }
            }

            val creditCards = uiState.groups.find { it.label == "信用卡" }
            if (creditCards != null && creditCards.accounts.isNotEmpty()) {
                item { GroupHeader(label = creditCards.label, total = creditCards.totalBalance) }
                items(creditCards.accounts, key = { it.account.id }) { CreditCardItem(it) }
            }

            val payments = uiState.groups.find { it.label == "网络支付账户" }
            if (payments != null && payments.accounts.isNotEmpty()) {
                item { GroupHeader(label = payments.label, total = payments.totalBalance) }
                items(payments.accounts, key = { it.account.id }) { AssetAccountItem(it) }
            }

            val payables = uiState.groups.find { it.label == "应付账户" }
            if (payables != null && payables.accounts.isNotEmpty()) {
                item { GroupHeader(label = payables.label, total = payables.totalBalance) }
                items(payables.accounts, key = { it.account.id }) { AssetAccountItem(it) }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, creditLimit, paymentDueDay ->
                viewModel.addAccount(name, type, creditLimit, paymentDueDay)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun NetAssetCard(netAsset: Double, totalAsset: Double, totalLiability: Double) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Text(
                "净资产（元）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                DateUtils.formatAmount(netAsset),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (netAsset >= 0) IncomeGreen else ExpenseRed
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(
                        "总资产",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        DateUtils.formatAmount(totalAsset),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = IncomeGreen
                    )
                }
                Column {
                    Text(
                        "负资产",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        DateUtils.formatAmount(totalLiability),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
fun GroupHeader(label: String, total: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            DateUtils.formatAmount(total),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = if (total >= 0) IncomeGreen else ExpenseRed
            )
        )
    }
}

@Composable
fun AssetAccountItem(item: AccountBalance) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                item.account.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                DateUtils.formatAmount(item.balance),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (item.balance >= 0) IncomeGreen else ExpenseRed
                )
            )
        }
    }
}

@Composable
fun CreditCardItem(item: AccountBalance) {
    val cal = Calendar.getInstance()
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    val dueDay = item.account.paymentDueDay
    val daysUntilDue = if (dueDay > 0) {
        if (dueDay > dayOfMonth) dueDay - dayOfMonth
        else dueDay + cal.getActualMaximum(Calendar.DAY_OF_MONTH) - dayOfMonth
    } else 0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    item.account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    DateUtils.formatAmount(item.balance),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (item.balance >= 0) IncomeGreen else ExpenseRed
                    )
                )
            }
            if (item.account.creditLimit > 0 && dueDay > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val available = item.account.creditLimit + item.balance
                Text(
                    "可用：${DateUtils.formatAmount(available)}  距还款日还有${daysUntilDue}天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (daysUntilDue <= 18) {
                    Text(
                        "今日起可免息${daysUntilDue}天",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, creditLimit: Double, paymentDueDay: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("wechat") }
    var creditLimit by remember { mutableStateOf("") }
    var paymentDueDay by remember { mutableStateOf("") }
    val types = listOf(
        "wechat" to "微信",
        "alipay" to "支付宝",
        "bank_card" to "储蓄卡",
        "credit_card" to "信用卡",
        "loan" to "应付账户",
        "cash" to "现金"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加账户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("账户名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                if (selectedType == "credit_card") {
                    OutlinedTextField(
                        value = creditLimit,
                        onValueChange = { creditLimit = it },
                        placeholder = { Text("信用额度") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentDueDay,
                        onValueChange = { paymentDueDay = it },
                        placeholder = { Text("每月还款日（1-28）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            selectedType,
                            creditLimit.toDoubleOrNull() ?: 0.0,
                            paymentDueDay.toIntOrNull() ?: 0
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
