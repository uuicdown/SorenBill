package com.soren.bill.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.soren.bill.data.preferences.AppPreferences
import com.soren.bill.data.preferences.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val appPreferences: AppPreferences = koinInject()
    
    val themeMode by appPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val confirmBeforeSaving by appPreferences.confirmBeforeSaving.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("\u8bbe\u7f6e", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "\u8fd4\u56de") } }
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 主题设置
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Soren \u7684\u6837\u5b50", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = themeMode == ThemeMode.SYSTEM, onClick = { coroutineScope.launch { appPreferences.setThemeMode(ThemeMode.SYSTEM) } }, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)) { Text("\u548c\u624b\u673a\u4e00\u6837", style = MaterialTheme.typography.labelSmall) }
                        SegmentedButton(selected = themeMode == ThemeMode.LIGHT, onClick = { coroutineScope.launch { appPreferences.setThemeMode(ThemeMode.LIGHT) } }, shape = RoundedCornerShape(0.dp)) { Text("\u6e05\u723d\u767d\u5929", style = MaterialTheme.typography.labelSmall) }
                        SegmentedButton(selected = themeMode == ThemeMode.DARK, onClick = { coroutineScope.launch { appPreferences.setThemeMode(ThemeMode.DARK) } }, shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)) { Text("\u9759\u8c27\u591c\u665a", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }

            // 自动记账
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("\u8bb0\u8d26\u524d\u5148\u95ee\u6211\u4e00\u4e0b", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Soren \u53d1\u73b0\u65b0\u8d26\u5355\u65f6\u4f1a\u5f39\u7a97\u786e\u8ba4", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = confirmBeforeSaving, onCheckedChange = { coroutineScope.launch { appPreferences.setConfirmBeforeSaving(it) } })
                }
            }

            // 关于
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("\u5173\u4e8e", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Soren \u7684\u8d26\u5355 v1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("\u60a8\u7684\u79c1\u4eba\u865a\u62df\u7ba1\u5bb6 \u00b7 \u6bcf\u5929\u5e2e\u60a8\u8bb0\u5f55\u7ecf\u6d4e\u60c5\u51b5", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
