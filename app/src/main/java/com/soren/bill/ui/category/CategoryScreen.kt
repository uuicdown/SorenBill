package com.soren.bill.ui.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Category
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.categoryIcon
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryViewModel(private val repository: BillRepository) : ViewModel() {
    private val _expenseCategories = MutableStateFlow<List<Category>>(emptyList())
    val expenseCategories: StateFlow<List<Category>> = _expenseCategories.asStateFlow()

    private val _incomeCategories = MutableStateFlow<List<Category>>(emptyList())
    val incomeCategories: StateFlow<List<Category>> = _incomeCategories.asStateFlow()

    init {
        viewModelScope.launch { repository.getCategoriesByType("expense").collect { _expenseCategories.value = it } }
        viewModelScope.launch { repository.getCategoriesByType("income").collect { _incomeCategories.value = it } }
    }

    fun addCategory(name: String, type: String) { viewModelScope.launch { repository.insertCategory(Category(name = name, type = type)) } }
    fun deleteCategory(category: Category) { viewModelScope.launch { repository.deleteCategory(category.id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(viewModel: CategoryViewModel, onBack: () -> Unit) {
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var dialogType by remember { mutableStateOf("expense") }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("\u5206\u7c7b\u7ba1\u7406", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "\u8fd4\u56de") } })
        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
            item { SectionTitle("\u82b1\u9500\u5206\u7c7b", showDialog, dialogType) { dialogType = "expense"; showDialog = true } }
            items(expenseCategories) { cat -> CategoryRow(cat) { viewModel.deleteCategory(cat) } }
            item { Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp)) }
            item { SectionTitle("\u5165\u8d26\u5206\u7c7b", showDialog, dialogType) { dialogType = "income"; showDialog = true } }
            items(incomeCategories) { cat -> CategoryRow(cat) { viewModel.deleteCategory(cat) } }
        }
    }

    if (showDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text(if (dialogType == "expense") "\u65b0\u5efa\u82b1\u9500\u5206\u7c7b" else "\u65b0\u5efa\u5165\u8d26\u5206\u7c7b") },
            text = { OutlinedTextField(text, { text = it }, placeholder = { Text("\u5206\u7c7b\u540d\u79f0") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (text.isNotBlank()) { viewModel.addCategory(text.trim(), dialogType); showDialog = false } }) { Text("\u6dfb\u52a0") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("\u53d6\u6d88") } })
    }
}

@Composable
private fun SectionTitle(title: String, showDialog: Boolean, dialogType: String, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "\u6dfb\u52a0") }
    }
}

@Composable
private fun CategoryRow(cat: Category, onDelete: () -> Unit) {
    var showDel by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { showDel = true }.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(categoryIcon(cat.name), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(cat.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
    if (showDel) AlertDialog(onDismissRequest = { showDel = false }, title = { Text("\u5220\u9664\u5206\u7c7b") }, text = { Text("\u786e\u5b9a\u5220\u9664\u300c${cat.name}\u300d\u5417\uff1f") },
        confirmButton = { TextButton(onClick = { onDelete(); showDel = false }) { Text("\u5220\u9664", color = ExpenseRed) } },
        dismissButton = { TextButton(onClick = { showDel = false }) { Text("\u53d6\u6d88") } })
}
