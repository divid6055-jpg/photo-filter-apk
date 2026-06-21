package com.photofilter.pro.utils

import java.io.Serializable

/**
 * تعريفات الفلاتر المتاحة في التطبيق
 * كل فلتر له اسم العرض (المترجم) ومعرف فريد
 */
enum class FilterType(val id: String) {
    NONE("none"),
    VINTAGE("vintage"),
    BLACK_WHITE("bw"),
    SEPIA("sepia"),
    COOL("cool"),
    WARM("warm"),
    VIVID("vivid"),
    FADE("fade"),
    NOIR("noir"),
    CINEMATIC("cinematic"),
    DRAMA("drama"),
    SILVER("silver"),
    LOMO("lomo"),
    POLAROID("polaroid"),
    SUNRISE("sunrise"),
    NIGHT("night"),
    SKETCH("sketch"),
    INVERT("invert"),
    // فلاتر احترافية جديدة
    HDR("hdr"),
    BLOOM("bloom"),
    AURORA("aurora"),
    MATTE("matte"),
    EMERALD("emerald"),
    CRIMSON("crimson"),
    GOLDEN("golden"),
    OCEAN("ocean"),
    PASTEL("pastel"),
    NEON("neon");

    companion object {
        fun fromId(id: String): FilterType = entries.find { it.id == id } ?: NONE
    }
}

/**
 * إعدادات التعديل اليدوية. القيم الافتراضية تمثل "لا تغيير".
 * النطاقات مصممة لتكون بديهية للمستخدم (مئوية أو نسبية).
 */
data class AdjustSettings(
    val brightness: Float = 0f,        // -100 .. 100   (0 = لا تغيير)
    val contrast: Float = 0f,          // -100 .. 100
    val saturation: Float = 0f,        // -100 .. 100
    val warmth: Float = 0f,            // -100 .. 100   (سالب = بارد، موجب = دافئ)
    val sharpness: Float = 0f,         // 0 .. 100
    val exposure: Float = 0f,          // -100 .. 100
    val highlights: Float = 0f,        // -100 .. 100
    val shadows: Float = 0f,           // -100 .. 100
    val vignette: Float = 0f,          // 0 .. 100
    val grain: Float = 0f              // 0 .. 100
) : Serializable {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f && warmth == 0f &&
                sharpness == 0f && exposure == 0f && highlights == 0f && shadows == 0f &&
                vignette == 0f && grain == 0f

    companion object {
        val DEFAULT = AdjustSettings()
    }
}

/**
 * حالة المحرر الكاملة - تجمع الفلتر المختار وإعدادات التعديل وحالة التحويل.
 */
data class EditorState(
    val filter: FilterType = FilterType.NONE,
    val adjust: AdjustSettings = AdjustSettings.DEFAULT,
    val rotationDegrees: Int = 0,        // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
) : Serializable {
    val isDefault: Boolean
        get() = filter == FilterType.NONE && adjust.isDefault &&
                rotationDegrees == 0 && !flipHorizontal && !flipVertical
}
