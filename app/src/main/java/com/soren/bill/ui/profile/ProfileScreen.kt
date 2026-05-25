package com.soren.bill.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.categoryIcon
import com.soren.bill.data.preferences.ThemeMode
import com.soren.bill.data.preferences.AppPreferences
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryTypeForAdd by remember { mutableStateOf("expense") }

    val appPreferences: AppPreferences = koinInject()
    val themeMode by appPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val confirmBeforeSaving by appPreferences.confirmBeforeSaving.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("主题设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            SegmentedButton(
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { coroutineScope.launch { appPreferences.setThemeMode(ThemeMode.SYSTEM) } },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            ) { Text("\u548c\u624b\u673a\u4e00\u6837", style = MaterialTheme.typography.labelMedium) }
            SegmentedButton(
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { coroutineScope.launch { appPreferences.setThemeMode(ThemeMode.LIGHT) } },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            ) { Text("浅色模式", style = MaterialTheme.typography.labelMedium) }
            SegmentedButton(
                selected = themeMode == ThemeMode.DARK,
                onClick = { coroutineScope.launch { appPreferences.setThemeMode(ThemeMode.DARK) } },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) { Text("深色模式", style = MaterialTheme.typography.labelMedium) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("\u8bb0\u8d26\u524d\u5148\u95ee\u6211\u4e00\u4e0b", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "开启后，识别到账单时将弹窗让你确认后再保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = confirmBeforeSaving,
                onCheckedChange = { checked ->
                    coroutineScope.launch { appPreferences.setConfirmBeforeSaving(checked) }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = "钱包", onAdd = { showAddWalletDialog = true })
        uiState.wallets.forEach { wallet ->
            ManageableItem(
                name = wallet.name,
                subtitle = wallet.currency,
                icon = Icons.Default.AccountBalanceWallet,
                onDelete = { viewModel.deleteWallet(wallet) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = "支付账户", onAdd = { showAddAccountDialog = true })
        uiState.accounts.forEach { account ->
            val icon = when (account.type) {
                "wechat" -> Icons.Default.Chat
                "alipay" -> Icons.Default.ShoppingCart
                "bank_card" -> Icons.Default.CreditCard
                "credit_card" -> Icons.Default.CreditCard
                "cash" -> Icons.Default.LocalAtm
                else -> Icons.Default.AccountBalance
            }
            ManageableItem(
                name = account.name,
                icon = icon,
                onDelete = { viewModel.deleteAccount(account) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = "支出分类", onAdd = {
            categoryTypeForAdd = "expense"
            showAddCategoryDialog = true
        })
        uiState.expenseCategories.forEach { category ->
            ManageableItem(
                name = category.name,
                icon = categoryIcon(category.name),
                onDelete = { viewModel.deleteCategory(category) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = "收入分类", onAdd = {
            categoryTypeForAdd = "income"
            showAddCategoryDialog = true
        })
        uiState.incomeCategories.forEach { category ->
            ManageableItem(
                name = category.name,
                icon = categoryIcon(category.name),
                onDelete = { viewModel.deleteCategory(category) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showAddWalletDialog) {
        AddItemDialog(
            title = "添加钱包",
            placeholder = "钱包名称",
            onConfirm = { name ->
                viewModel.addWallet(name)
                showAddWalletDialog = false
            },
            onDismiss = { showAddWalletDialog = false }
        )
    }

    if (showAddAccountDialog) {
        AddItemDialog(
            title = "添加支付账户",
            placeholder = "账户名称",
            onConfirm = { name ->
                viewModel.addAccount(name, "other")
                showAddAccountDialog = false
            },
            onDismiss = { showAddAccountDialog = false }
        )
    }

    if (showAddCategoryDialog) {
        AddItemDialog(
            title = if (categoryTypeForAdd == "expense") "添加支出分类" else "添加收入分类",
            placeholder = "分类名称",
            onConfirm = { name ->
                viewModel.addCategory(name, categoryTypeForAdd)
                showAddCategoryDialog = false
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }
}

@Composable
fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = "添加")
        }
    }
}

@Composable
fun ManageableItem(
    name: String,
    icon: ImageVector,
    subtitle: String? = null,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDelete = true }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除") },
            text = { Text("确定要删除「$name」吗？相关记录也会一并删除。") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text("删除", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun AddItemDialog(
    title: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

