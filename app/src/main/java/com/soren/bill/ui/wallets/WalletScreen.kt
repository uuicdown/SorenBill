package com.soren.bill.ui.wallets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Wallet
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.ui.theme.ExpenseRed
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WalletViewModel(private val repository: BillRepository) : ViewModel() {
    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: StateFlow<List<Wallet>> = _wallets.asStateFlow()
    init { viewModelScope.launch { repository.getAllWallets().collect { _wallets.value = it } } }
    fun add(name: String) { viewModelScope.launch { repository.insertWallet(Wallet(name = name, currency = "CNY")) } }
    fun delete(wallet: Wallet) { viewModelScope.launch { repository.deleteWallet(wallet.id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: WalletViewModel, onBack: () -> Unit) {
    val wallets by viewModel.wallets.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("\u94b1\u5305\u7ba1\u7406", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "\u8fd4\u56de") } })
        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
            items(wallets) { wallet ->
                var showDel by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().clickable { showDel = true }.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(wallet.name, style = MaterialTheme.typography.bodyLarge); Text(wallet.currency, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (showDel) AlertDialog(onDismissRequest = { showDel = false }, title = { Text("\u5220\u9664\u94b1\u5305") }, text = { Text("\u786e\u5b9a\u5220\u9664\u300c${wallet.name}\u300d\u5417\uff1f\u76f8\u5173\u8bb0\u5f55\u4e5f\u4f1a\u4e00\u8d77\u5220\u9664\u3002") },
                    confirmButton = { TextButton(onClick = { viewModel.delete(wallet); showDel = false }) { Text("\u5220\u9664", color = ExpenseRed) } },
                    dismissButton = { TextButton(onClick = { showDel = false }) { Text("\u53d6\u6d88") } })
            }
            item {
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showDialog = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(0.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("\u65b0\u5efa\u94b1\u5305") }
            }
        }
    }
    if (showDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text("\u65b0\u5efa\u94b1\u5305") },
            text = { OutlinedTextField(text, { text = it }, placeholder = { Text("\u94b1\u5305\u540d\u79f0") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (text.isNotBlank()) { viewModel.add(text.trim()); showDialog = false } }) { Text("\u6dfb\u52a0") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("\u53d6\u6d88") } })
    }
}
