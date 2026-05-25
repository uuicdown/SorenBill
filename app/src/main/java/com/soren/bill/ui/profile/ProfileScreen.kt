package com.soren.bill.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    onCategoryClick: () -> Unit,
    onWalletClick: () -> Unit,
    onAccountClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 头像区
        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Text("Soren", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("\u60a8\u7684\u79c1\u4eba\u7ba1\u5bb6", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 菜单项
        MenuCard("\u5206\u7c7b\u7ba1\u7406", "\u7ba1\u7406\u82b1\u9500\u548c\u5165\u8d26\u5206\u7c7b", Icons.Default.Category, onCategoryClick)
        MenuCard("\u94b1\u5305\u7ba1\u7406", "\u7ba1\u7406\u60a8\u7684\u94b1\u5305", Icons.Default.AccountBalanceWallet, onWalletClick)
        MenuCard("\u8d26\u6237\u7ba1\u7406", "\u7ba1\u7406\u652f\u4ed8\u8d26\u6237", Icons.Default.CreditCard, onAccountClick)
        MenuCard("\u8bbe\u7f6e", "Soren \u7684\u6837\u5b50\u3001\u8bb0\u8d26\u504f\u597d", Icons.Default.Settings, onSettingsClick)
    }
}

@Composable
private fun MenuCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
