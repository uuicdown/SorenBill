package com.soren.bill.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 银行名称 → 本地 PNG 图标映射
private val bankIconMap = mapOf(
    "中国工商银行" to com.soren.bill.R.drawable.bank_icbc,
    "中国建设银行" to com.soren.bill.R.drawable.bank_ccb,
    "中国农业银行" to com.soren.bill.R.drawable.bank_abc,
    "中国银行" to com.soren.bill.R.drawable.bank_boc,
    "交通银行" to com.soren.bill.R.drawable.bank_comm,
    "招商银行" to com.soren.bill.R.drawable.bank_cmb,
    "邮政储蓄银行" to com.soren.bill.R.drawable.bank_psbc,
    "浦发银行" to com.soren.bill.R.drawable.bank_spdb,
    "中信银行" to com.soren.bill.R.drawable.bank_citic,
    "光大银行" to com.soren.bill.R.drawable.bank_ceb,
    "民生银行" to com.soren.bill.R.drawable.bank_cmbc,
    "兴业银行" to com.soren.bill.R.drawable.bank_cib,
    "广发银行" to com.soren.bill.R.drawable.bank_gdb,
    "华夏银行" to com.soren.bill.R.drawable.bank_hxb,
    "平安银行" to com.soren.bill.R.drawable.bank_pingan,
    "北京银行" to com.soren.bill.R.drawable.bank_bj,
    "上海银行" to com.soren.bill.R.drawable.bank_sh,
    "江苏银行" to com.soren.bill.R.drawable.bank_js,
    "南京银行" to com.soren.bill.R.drawable.bank_nj,
    "宁波银行" to com.soren.bill.R.drawable.bank_nb,
    "杭州银行" to com.soren.bill.R.drawable.bank_hz,
    "浙商银行" to com.soren.bill.R.drawable.bank_cz
)

// 网贷/支付平台 → 本地 PNG 图标映射
private val paymentIconMap = mapOf(
    "微信" to com.soren.bill.R.drawable.ic_wechat,
    "支付宝" to com.soren.bill.R.drawable.ic_alipay,
    "京东白条" to com.soren.bill.R.drawable.ic_jd,
    "美团月付" to com.soren.bill.R.drawable.ic_meituan
)

@Composable
fun AccountIcon(type: String, name: String, size: Dp = 28.dp) {
    // 先查银行图标
    val bankRes = bankIconMap[name]
    if (bankRes != null) {
        Image(painter = painterResource(id = bankRes), contentDescription = name, modifier = Modifier.size(size))
        return
    }
    // 支付平台图标
    val payRes = paymentIconMap.entries.firstOrNull { name.contains(it.key) }?.value
    if (payRes != null) {
        Image(painter = painterResource(id = payRes), contentDescription = name, modifier = Modifier.size(size))
        return
    }
    // 无 PNG 图标 → 首字圆标降级
    val initial = when {
        name.contains("工商") -> "工"; name.contains("建设") -> "建"; name.contains("农业") -> "农"
        name.contains("交通") -> "交"; name.contains("招商") -> "招"; name.contains("邮政") -> "邮"
        name.contains("浦发") -> "浦"; name.contains("中信") -> "信"; name.contains("光大") -> "光"
        name.contains("民生") -> "民"; name.contains("兴业") -> "兴"; name.contains("广发") -> "广"
        name.contains("华夏") -> "华"; name.contains("平安") -> "平"; name.contains("北京") -> "京"
        name.contains("上海") -> "沪"; name.contains("花呗") -> "花"; name.contains("借呗") -> "借"
        name.contains("微粒") -> "微"; name.contains("度小满") -> "度"; name.contains("现金") -> "现"
        name.contains("江苏") -> "苏"; name.contains("南京") -> "宁"
        else -> name.take(1)
    }
    Box(Modifier.size(size).clip(CircleShape).background(accountBrandColor(type)), contentAlignment = Alignment.Center) {
        Text(initial, color = Color.White, fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Bold)
    }
}

fun accountBrandColor(type: String): Color = when (type) {
    "wechat" -> Color(0xFF07C160); "alipay" -> Color(0xFF1677FF)
    "bank_card" -> Color(0xFFE74C3C); "credit_card" -> Color(0xFF9B59B6)
    "loan" -> Color(0xFFE67E22); "cash" -> Color(0xFF27AE60)
    else -> Color(0xFF607D8B)
}

// === 分类图标映射 ===
import androidx.compose.material.icons.Icons as M3Icons
import androidx.compose.material.icons.filled.*

fun categoryIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector = when (name) {
    "餐饮" -> Restaurant; "交通" -> DirectionsBus; "购物" -> ShoppingCart
    "娱乐" -> Movie; "居住" -> Home; "医疗" -> LocalHospital
    "人情" -> Favorite; "教育" -> School; "通讯" -> Phone
    "服饰" -> Checkroom; "日用" -> CleaningServices; "数码" -> Devices
    "宠物" -> Pets; "运动" -> FitnessCenter; "旅行" -> Flight
    "美容" -> Face; "零食" -> Icecream; "水果" -> LocalFlorist
    "外卖" -> DeliveryDining; "工资" -> Paid; "奖金" -> EmojiEvents
    "兼职" -> Work; "理财" -> TrendingUp; "退款" -> MoneyOff
    "红包" -> CardGiftcard; "报销" -> Receipt; "房租收入" -> Apartment
    "转让" -> SwapHoriz; "余额调整" -> Tune; "其它" -> MoreHoriz
    else -> Circle
}
