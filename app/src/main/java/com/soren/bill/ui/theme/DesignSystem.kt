package com.soren.bill.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons as M3Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
private fun GradientFallbackIcon(type: String, name: String, size: Dp) {
    val (colorStart, colorEnd) = accountGradientColors(type)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(colorStart, colorEnd))),
        contentAlignment = Alignment.Center
    ) {
        val initial = if (name.isNotEmpty()) name.take(1) else "?"
        Text(initial, color = Color.White, fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AccountIcon(type: String, name: String, size: Dp = 28.dp) {
    val bankCode = when {
        name.contains("工商") -> "ICBC"
        name.contains("建设") -> "CCB"
        name.contains("农业") -> "ABC"
        name.contains("中国银行") -> "BOC"
        name.contains("交通") -> "COMM"
        name.contains("招商") -> "CMB"
        name.contains("邮政") -> "PSBC"
        name.contains("浦发") -> "SPDB"
        name.contains("中信") -> "CITIC"
        name.contains("光大") -> "CEB"
        name.contains("民生") -> "CMBC"
        name.contains("兴业") -> "CIB"
        name.contains("广发") -> "GDB"
        name.contains("华夏") -> "HXB"
        name.contains("平安") -> "SPABANK"
        name.contains("北京银行") -> "BJBANK"
        name.contains("上海银行") -> "SHBANK"
        name.contains("微信") -> "WECHAT"
        name.contains("支付宝") -> "ALIPAY"
        else -> null
    }

    if (bankCode != null && bankCode != "WECHAT" && bankCode != "ALIPAY") {
        SubcomposeAsyncImage(
            model = "https://apimg.alipay.com/combo.png?d=cashier&t=$bankCode",
            contentDescription = name,
            modifier = Modifier.size(size).clip(CircleShape).background(Color.White),
            contentScale = ContentScale.Fit,
            error = { GradientFallbackIcon(type, name, size) }
        )
        return
    }

    if (bankCode == "ALIPAY") {
        SubcomposeAsyncImage(
            model = "https://gw.alipayobjects.com/zos/rmsportal/qHwSmyhKkXqXzzkARTkM.png",
            contentDescription = name,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = { GradientFallbackIcon(type, name, size) }
        )
        return
    }

    if (bankCode == "WECHAT") {
        SubcomposeAsyncImage(
            model = "https://res.wx.qq.com/a/wx_fed/assets/res/NTI4MWU5.ico",
            contentDescription = name,
            modifier = Modifier.size(size).clip(CircleShape).background(Color.White),
            contentScale = ContentScale.Crop,
            error = { GradientFallbackIcon(type, name, size) }
        )
        return
    }

    GradientFallbackIcon(type, name, size)
}

fun accountBrandColor(type: String): Color = accountGradientColors(type).first

fun accountGradientColors(type: String): Pair<Color, Color> = when (type) {
    "wechat" -> Pair(Color(0xFF2ECA71), Color(0xFF07C160))
    "alipay" -> Pair(Color(0xFF4293FF), Color(0xFF1677FF))
    "bank_card" -> Pair(Color(0xFFE74C3C), Color(0xFFC0392B))
    "credit_card" -> Pair(Color(0xFF9B59B6), Color(0xFF8E44AD))
    "loan" -> Pair(Color(0xFFF39C12), Color(0xFFD35400))
    "cash" -> Pair(Color(0xFF2ECC71), Color(0xFF27AE60))
    else -> Pair(Color(0xFF78909C), Color(0xFF546E7A))
}

// === 分类图标映射 ===

fun categoryIcon(name: String): ImageVector = when (name) {
    "餐饮" -> M3Icons.Filled.Restaurant; "交通" -> M3Icons.Filled.DirectionsBus; "购物" -> M3Icons.Filled.ShoppingCart
    "娱乐" -> M3Icons.Filled.Movie; "居住" -> M3Icons.Filled.Home; "医疗" -> M3Icons.Filled.LocalHospital
    "恋爱" -> M3Icons.Filled.Favorite; "教育" -> M3Icons.Filled.School; "通讯" -> M3Icons.Filled.Phone
    "服饰" -> M3Icons.Filled.Checkroom; "家政" -> M3Icons.Filled.CleaningServices; "数码" -> M3Icons.Filled.Devices
    "宠物" -> M3Icons.Filled.Pets; "运动" -> M3Icons.Filled.FitnessCenter; "旅行" -> M3Icons.Filled.Flight
    "美容" -> M3Icons.Filled.Face; "零食" -> M3Icons.Filled.Icecream; "水果" -> M3Icons.Filled.LocalFlorist
    "外卖" -> M3Icons.Filled.DeliveryDining; "工资" -> M3Icons.Filled.Paid; "奖金" -> M3Icons.Filled.EmojiEvents
    "兼职" -> M3Icons.Filled.Work; "理财" -> M3Icons.AutoMirrored.Filled.TrendingUp; "退款" -> M3Icons.Filled.MoneyOff
    "红包" -> M3Icons.Filled.CardGiftcard; "报销" -> M3Icons.Filled.Receipt; "二手交易" -> M3Icons.Filled.Apartment
    "转让" -> M3Icons.Filled.SwapHoriz; "余额调整" -> M3Icons.Filled.Tune; "其它" -> M3Icons.Filled.MoreHoriz
    else -> M3Icons.Filled.Circle
}

// === Soren (Premium x iOS) Design Elements ===

val CardCornerRadius = 20.dp
val DialogCornerRadius = 24.dp

val SorenCardShape = RoundedCornerShape(CardCornerRadius)
val SorenDialogShape = RoundedCornerShape(DialogCornerRadius)

fun Modifier.sorenShadow(
    color: Color = Color(0x14000000), // 8% black
    blurRadius: Dp = 16.dp,
    offsetY: Dp = 8.dp
): Modifier = this.shadow(
    elevation = blurRadius,
    shape = SorenCardShape,
    spotColor = color,
    ambientColor = color
)

fun Modifier.bounceClick(
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "bounceScale"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Disable default ripple for iOS feel
            onClick = onClick
        )
}