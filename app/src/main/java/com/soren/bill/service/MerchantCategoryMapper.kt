package com.soren.bill.service

/**
 * 商户名 → 记账分类的自动映射规则表。
 * 覆盖微信/支付宝常见生活场景的商户关键词。
 */
object MerchantCategoryMapper {

    data class CategorySuggestion(
        val targetCategoryName: String, // 对应的分类名称（如"餐饮"）
        val confidence: Float           // 匹配置信度 0-1
    )

    // 分类关键词规则：key = 带 match 后缀，确保不会直接相等
    private val rules: Map<String, String> = mapOf(
        // 餐饮
        "麦当劳" to "餐饮", "肯德基" to "餐饮", "汉堡王" to "餐饮", "必胜客" to "餐饮",
        "星巴克" to "餐饮", "瑞幸" to "餐饮", "喜茶" to "餐饮", "奈雪" to "餐饮",
        "海底捞" to "餐饮", "太二" to "餐饮", "西贝" to "餐饮", "呷哺" to "餐饮",
        "美团外卖" to "餐饮", "饿了么" to "餐饮", "DQ" to "餐饮", "蜜雪冰城" to "餐饮",
        "餐厅" to "餐饮", "饭店" to "餐饮", "小吃" to "餐饮", "奶茶" to "餐饮",
        "咖啡" to "餐饮", "烘焙" to "餐饮", "面馆" to "餐饮", "料理" to "餐饮",

        // 交通
        "滴滴" to "交通", "曹操出行" to "交通", "T3出行" to "交通", "花小猪" to "交通",
        "高德打车" to "交通", "中石化" to "交通", "中石油" to "交通", "壳牌" to "交通",
        "公交" to "交通", "地铁" to "交通", "高铁" to "交通", "火车票" to "交通",
        "机票" to "交通", "航班" to "交通", "ETC" to "交通", "停车" to "交通",
        "加油" to "交通", "充电" to "交通",

        // 购物
        "京东" to "购物", "淘宝" to "购物", "天猫" to "购物", "拼多多" to "购物",
        "抖音商城" to "购物", "唯品会" to "购物", "得物" to "购物", "闲鱼" to "购物",
        "超市" to "购物", "便利店" to "购物", "罗森" to "购物", "7-ELEVEN" to "购物",
        "屈臣氏" to "购物", "名创优品" to "购物", "山姆" to "购物", "Costco" to "购物",

        // 娱乐
        "猫眼" to "娱乐", "淘票票" to "娱乐", "万达影城" to "娱乐", "KTV" to "娱乐",
        "网易云" to "娱乐", "QQ音乐" to "娱乐", "哔哩哔哩" to "娱乐", "B站" to "娱乐",
        "王者荣耀" to "娱乐", "原神" to "娱乐", "Steam" to "娱乐", "游戏" to "娱乐",

        // 通讯
        "中国移动" to "通讯", "中国联通" to "通讯", "中国电信" to "通讯",
        "话费" to "通讯", "流量" to "通讯",

        // 居住
        "物业" to "居住", "水电" to "居住", "燃气" to "居住", "房租" to "居住",
        "自如" to "居住", "贝壳" to "居住", "链家" to "居住",

        // 医疗
        "医院" to "医疗", "药房" to "医疗", "诊所" to "医疗", "体检" to "医疗",

        // 教育
        "学费" to "教育", "培训" to "教育", "书店" to "教育", "得到" to "教育",
        "知乎" to "教育",

        // 其他生活
        "顺丰" to "生活", "圆通" to "生活", "中通" to "生活", "申通" to "生活",
        "快递" to "生活", "理发" to "生活", "洗衣" to "生活",
    )

    /**
     * 根据商户名称匹配分类建议
     */
    fun classify(merchant: String?): CategorySuggestion? {
        if (merchant.isNullOrBlank()) return null

        // 精确匹配
        for ((keyword, category) in rules) {
            if (merchant.length >= 2 && keyword.length >= 2) {
                // 双向包含匹配
                if (merchant.contains(keyword) || keyword.contains(merchant)) {
                    return CategorySuggestion(targetCategoryName = category, confidence = 0.9f)
                }
            }
        }

        // 模糊匹配（包含任一关键词）
        for ((keyword, category) in rules) {
            if (merchant.contains(keyword)) {
                return CategorySuggestion(targetCategoryName = category, confidence = 0.7f)
            }
        }

        return null
    }
}
