package com.soren.bill.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Account
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.ui.theme.ExpenseRed
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountViewModel(private val repository: BillRepository) : ViewModel() {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()
    init { viewModelScope.launch { repository.getAllAccounts().collect { _accounts.value = it } } }
    fun add(name: String) { viewModelScope.launch { repository.insertAccount(Account(name = name, type = "other")) } }
    fun delete(account: Account) { viewModelScope.launch { repository.deleteAccount(account.id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(viewModel: AccountViewModel, onBack: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("\u8d26\u6237\u7ba1\u7406", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "\u8fd4\u56de") } })
        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
            items(accounts) { account ->
                var showDel by remember { mutableStateOf(false) }
                val icon = when (account.type) { "wechat" -> Icons.AutoMirrored.Filled.Chat; "alipay" -> Icons.Default.ShoppingCart; "bank_card" -> Icons.Default.CreditCard; "credit_card" -> Icons.Default.CreditCard; "cash" -> Icons.Default.LocalAtm; else -> Icons.Default.AccountBalance }
                Row(Modifier.fillMaxWidth().clickable { showDel = true }.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(account.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                }
                if (showDel) AlertDialog(onDismissRequest = { showDel = false }, title = { Text("\u5220\u9664\u8d26\u6237") }, text = { Text("\u786e\u5b9a\u5220\u9664\u300c${account.name}\u300d\u5417\uff1f") },
                    confirmButton = { TextButton(onClick = { viewModel.delete(account); showDel = false }) { Text("\u5220\u9664", color = ExpenseRed) } },
                    dismissButton = { TextButton(onClick = { showDel = false }) { Text("\u53d6\u6d88") } })
            }
            item {
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showDialog = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(0.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("\u65b0\u5efa\u8d26\u6237") }
            }
        }
    }
    if (showDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text("\u65b0\u5efa\u8d26\u6237") },
            text = { OutlinedTextField(text, { text = it }, placeholder = { Text("\u8d26\u6237\u540d\u79f0") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (text.isNotBlank()) { viewModel.add(text.trim()); showDialog = false } }) { Text("\u6dfb\u52a0") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("\u53d6\u6d88") } })
    }
}
