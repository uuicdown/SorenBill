package com.soren.bill.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soren.bill.ui.theme.SorenCardShape
import com.soren.bill.ui.theme.bounceClick
import com.soren.bill.ui.theme.sorenShadow

@Composable
fun ProfileScreen(
    onCategoryClick: () -> Unit,
    onWalletClick: () -> Unit,
    onAccountClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 头像区
        Box(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Text("Soren", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("\u60a8\u7684\u79c1\u4eba\u7ba1\u5bb6", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 第一组：分类、钱包、账户
        Surface(
            modifier = Modifier.fillMaxWidth().sorenShadow(),
            shape = SorenCardShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                MenuRow("\u5206\u7c7b\u7ba1\u7406", Icons.Default.Category, onCategoryClick)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MenuRow("\u94b1\u5305\u7ba1\u7406", Icons.Default.AccountBalanceWallet, onWalletClick)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MenuRow("\u8d26\u6237\u7ba1\u7406", Icons.Default.CreditCard, onAccountClick)
            }
        }

        // 第二组：设置
        Surface(
            modifier = Modifier.fillMaxWidth().sorenShadow(),
            shape = SorenCardShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                MenuRow("\u8bbe\u7f6e", Icons.Default.Settings, onSettingsClick)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MenuRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
