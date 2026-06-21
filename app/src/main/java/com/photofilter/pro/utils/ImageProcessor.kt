package com.photofilter.pro.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * محرك معالجة الصور - قلب التطبيق.
 *
 * يستخدم ColorMatrix للفلاتر الأساسية (سريعة جداً) ومعالجة البكسلات
 * اليدوية للفلاتر المعقدة (sketch, grain, vignette, HDR).
 *
 * كل الدوال thread-safe و لا تعدل البتدوماب الأصلي - تعيد بتدوماب جديد.
 */
object ImageProcessor {

    /** الحد الأقصى للبُعد لمنع OOM على الصور الكبيرة */
    private const val MAX_DIMENSION = 4096
    private const val PREVIEW_MAX_DIMENSION = 2048  // للمعاينة
    private const val THUMBNAIL_MAX_DIMENSION = 200  // للفلاتر

    // ===================== أدوات مساعدة عامة =====================

    /**
     * يصغّر الصورة إذا تجاوز بُعدها الأقصى MAX_DIMENSION للحفاظ على الذاكرة.
     */
    fun constrainSize(bitmap: Bitmap, maxDim: Int = MAX_DIMENSION): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = max(w, h)
        if (longest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / longest
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    /**
     * يصغّر الصورة لأبعاد المعاينة (للمعاينة الحية السريعة).
     * يجب استخدامه أثناء التحرير لتفادي OOM، واستخدام الصورة الأصلية للحفظ.
     */
    fun constrainToPreviewSize(bitmap: Bitmap): Bitmap {
        return constrainSize(bitmap, PREVIEW_MAX_DIMENSION)
    }

    /**
     * يصغّر الصورة لأبعاد Thumbnail (للقائمة الفلاتر).
     */
    fun constrainToThumbnailSize(bitmap: Bitmap): Bitmap {
        return constrainSize(bitmap, THUMBNAIL_MAX_DIMENSION)
    }

    /**
     * ينسخ الصورة بصيغة ARGB_8888 لضمان جودة المعالجة.
     */
    fun toEditable(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.ARGB_8888 && !bitmap.isRecycled) {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * يطبق ColorMatrix على الصورة ويعيد نسخة جديدة.
     */
    private fun applyColorMatrix(bitmap: Bitmap, matrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    /**
     * يطبق عدة ColorMatrix بالتتابع (دمج).
     */
    private fun applyColorMatrices(bitmap: Bitmap, matrices: List<ColorMatrix>): Bitmap {
        var current = bitmap
        for (m in matrices) {
            current = applyColorMatrix(current, m)
        }
        return current
    }

    // ===================== المعالجة حسب النوع =====================

    /**
     * التطبيق الرئيسي للفلتر على الصورة.
     */
    fun applyFilter(bitmap: Bitmap, filter: FilterType): Bitmap {
        if (filter == FilterType.NONE) return bitmap
        return when (filter) {
            FilterType.VINTAGE -> applyVintage(bitmap)
            FilterType.BLACK_WHITE -> applyBlackWhite(bitmap)
            FilterType.SEPIA -> applySepia(bitmap)
            FilterType.COOL -> applyCool(bitmap)
            FilterType.WARM -> applyWarm(bitmap)
            FilterType.VIVID -> applyVivid(bitmap)
            FilterType.FADE -> applyFade(bitmap)
            FilterType.NOIR -> applyNoir(bitmap)
            FilterType.CINEMATIC -> applyCinematic(bitmap)
            FilterType.DRAMA -> applyDrama(bitmap)
            FilterType.SILVER -> applySilver(bitmap)
            FilterType.LOMO -> applyLomo(bitmap)
            FilterType.POLAROID -> applyPolaroid(bitmap)
            FilterType.SUNRISE -> applySunrise(bitmap)
            FilterType.NIGHT -> applyNight(bitmap)
            FilterType.SKETCH -> applySketchFilter(bitmap)
            FilterType.INVERT -> applyInvert(bitmap)
            // فلاتر احترافية جديدة
            FilterType.HDR -> applyHDR(bitmap)
            FilterType.BLOOM -> applyBloom(bitmap)
            FilterType.AURORA -> applyAurora(bitmap)
            FilterType.MATTE -> applyMatte(bitmap)
            FilterType.EMERALD -> applyEmerald(bitmap)
            FilterType.CRIMSON -> applyCrimson(bitmap)
            FilterType.GOLDEN -> applyGolden(bitmap)
            FilterType.OCEAN -> applyOcean(bitmap)
            FilterType.PASTEL -> applyPastel(bitmap)
            FilterType.NEON -> applyNeon(bitmap)
            else -> bitmap
        }
    }

    // ===== الفلاتر الأساسية باستخدام ColorMatrix =====

    fun applyBlackWhite(bitmap: Bitmap): Bitmap {
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        return applyColorMatrix(bitmap, matrix)
    }

    fun applySepia(bitmap: Bitmap): Bitmap {
        val matrix = ColorMatrix().apply {
            setSaturation(0f)
            postConcat(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        // تركيب نغمات بنية على الصورة الرمادية
        val sepiaMatrix = ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        return applyColorMatrix(bitmap, sepiaMatrix)
    }

    fun applyVintage(bitmap: Bitmap): Bitmap {
        // إضافة دفء وتشبع مخفّض وتباين مرتفع قليلاً
        val cm = ColorMatrix()
        cm.setSaturation(0.85f)
        val contrastMatrix = ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, -15f,
            0f, 1.05f, 0f, 0f, -10f,
            0f, 0f, 0.9f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        ))
        val warmMatrix = ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, 10f,
            0f, 1.02f, 0f, 0f, 0f,
            0f, 0f, 0.85f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)
        cm.postConcat(warmMatrix)
        return applyColorMatrix(bitmap, cm)
    }

    fun applyCool(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            0.85f, 0f, 0f, 0f, 0f,
            0f, 0.95f, 0f, 0f, 5f,
            0f, 0f, 1.15f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(1.1f) }
        cm.postConcat(sat)
        return applyColorMatrix(bitmap, cm)
    }

    fun applyWarm(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            1.18f, 0f, 0f, 0f, 12f,
            0f, 1.05f, 0f, 0f, 5f,
            0f, 0f, 0.82f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        ))
        return applyColorMatrix(bitmap, cm)
    }

    fun applyVivid(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix().apply { setSaturation(1.5f) }
        val contrast = ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, -10f,
            0f, 1.1f, 0f, 0f, -10f,
            0f, 0f, 1.1f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrast)
        return applyColorMatrix(bitmap, cm)
    }

    fun applyFade(bitmap: Bitmap): Bitmap {
        // رفع الإضاءات وتقليل التباين
        val cm = ColorMatrix(floatArrayOf(
            0.9f, 0f, 0f, 0f, 25f,
            0f, 0.9f, 0f, 0f, 25f,
            0f, 0f, 0.9f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(0.8f) }
        cm.postConcat(sat)
        return applyColorMatrix(bitmap, cm)
    }

    fun applyNoir(bitmap: Bitmap): Bitmap {
        // أبيض وأسود بتباين عالٍ جداً
        val cm = ColorMatrix().apply { setSaturation(0f) }
        val contrast = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, -60f,
            0f, 1.5f, 0f, 0f, -60f,
            0f, 0f, 1.5f, 0f, -60f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrast)
        return applyColorMatrix(bitmap, cm)
    }

    fun applyCinematic(bitmap: Bitmap): Bitmap {
        // درجات teal-orange خفيفة + تباين
        val cm = ColorMatrix(floatArrayOf(
            1.08f, 0f, 0f, 0f, 5f,
            0f, 1.02f, 0f, 0f, 0f,
            0f, 0f, 0.95f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
        ))
        val contrast = ColorMatrix(floatArrayOf(
            1.15f, 0f, 0f, 0f, -15f,
            0f, 1.12f, 0f, 0f, -15f,
            0f, 0f, 1.1f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrast)
        return applyColorMatrix(bitmap, cm)
    }

    fun applyDrama(bitmap: Bitmap): Bitmap {
        // تباين عالٍ + تشبع مخفّض
        val cm = ColorMatrix().apply { setSaturation(0.85f) }
        val contrast = ColorMatrix(floatArrayOf(
            1.4f, 0f, 0f, 0f, -50f,
            0f, 1.4f, 0f, 0f, -50f,
            0f, 0f, 1.4f, 0f, -50f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrast)
        return applyColorMatrix(bitmap, cm)
    }

    fun applySilver(bitmap: Bitmap): Bitmap {
        // رمادي فضي بتباين خفيف
        val cm = ColorMatrix().apply { setSaturation(0.2f) }
        val contrast = ColorMatrix(floatArrayOf(
            1.15f, 0f, 0f, 0f, 10f,
            0f, 1.18f, 0f, 0f, 10f,
            0f, 0f, 1.2f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrast)
        return applyColorMatrix(bitmap, cm)
    }

    fun applyLomo(bitmap: Bitmap): Bitmap {
        // تشبع عالٍ + تباين عالٍ
        val cm = ColorMatrix().apply { setSaturation(1.35f) }
        val contrast = ColorMatrix(floatArrayOf(
            1.25f, 0f, 0f, 0f, -25f,
            0f, 1.25f, 0f, 0f, -25f,
            0f, 0f, 1.25f, 0f, -25f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrast)
        var result = applyColorMatrix(bitmap, cm)
        // إضافة vignette
        result = applyVignetteEffect(result, 0.45f)
        return result
    }

    fun applyPolaroid(bitmap: Bitmap): Bitmap {
        // ألوان باهتة دافئة مع ميل للأصفر
        val cm = ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, 15f,
            0f, 1.05f, 0f, 0f, 10f,
            0f, 0f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(0.9f) }
        cm.postConcat(sat)
        return applyColorMatrix(bitmap, cm)
    }

    fun applySunrise(bitmap: Bitmap): Bitmap {
        // دافئ جداً بألوان برتقالية
        val cm = ColorMatrix(floatArrayOf(
            1.25f, 0f, 0f, 0f, 20f,
            0f, 1.1f, 0f, 0f, 10f,
            0f, 0f, 0.75f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(1.2f) }
        cm.postConcat(sat)
        return applyColorMatrix(bitmap, cm)
    }

    fun applyNight(bitmap: Bitmap): Bitmap {
        // بارد داكن بألوان زرقاء
        val cm = ColorMatrix(floatArrayOf(
            0.8f, 0f, 0f, 0f, -5f,
            0f, 0.9f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(0.9f) }
        cm.postConcat(sat)
        return applyColorMatrix(bitmap, cm)
    }

    fun applyInvert(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        return applyColorMatrix(bitmap, cm)
    }

    // ===== الفلاتر الاحترافية الجديدة =====

    /**
     * HDR - تباين عالٍ مع إظهار التفاصيل في الإضاءات والظلال.
     * يجمع بين المعالجة المتعددة الطبقات.
     */
    fun applyHDR(bitmap: Bitmap): Bitmap {
        // 1. زيادة التباين العام
        val cm = ColorMatrix(floatArrayOf(
            1.25f, 0f, 0f, 0f, -25f,
            0f, 1.25f, 0f, 0f, -25f,
            0f, 0f, 1.25f, 0f, -25f,
            0f, 0f, 0f, 1f, 0f
        ))
        var result = applyColorMatrix(bitmap, cm)
        // 2. زيادة التشبع
        val sat = ColorMatrix().apply { setSaturation(1.35f) }
        result = applyColorMatrix(result, sat)
        // 3. تحسين الظلال
        result = applyHighlightsShadows(result, -10f, 30f)
        return result
    }

    /**
     * Bloom - توهج ناعم يبرز المناطق الساطعة.
     */
    fun applyBloom(bitmap: Bitmap): Bitmap {
        // 1. استخراج المناطق الساطعة
        val bright = extractBrightAreas(bitmap, 180)
        // 2. blur للتوهج الناعم
        val blurred = applySimpleBlur(bright, 8)
        bright.recycle()
        // 3. جمع الصورة الأصلية مع التوهج (screen blend)
        val w = bitmap.width
        val h = bitmap.height
        val orig = IntArray(w * h)
        val glow = IntArray(w * h)
        bitmap.getPixels(orig, 0, w, 0, 0, w, h)
        blurred.getPixels(glow, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        for (i in orig.indices) {
            val a = (orig[i] ushr 24) and 0xFF
            val oR = (orig[i] shr 16) and 0xFF
            val oG = (orig[i] shr 8) and 0xFF
            val oB = orig[i] and 0xFF
            val gR = (glow[i] shr 16) and 0xFF
            val gG = (glow[i] shr 8) and 0xFF
            val gB = glow[i] and 0xFF
            // Screen blend: 255 - ((255 - a) * (255 - b) / 255)
            val r = (255 - ((255 - oR) * (255 - gR) / 255)).coerceIn(0, 255)
            val g = (255 - ((255 - oG) * (255 - gG) / 255)).coerceIn(0, 255)
            val b = (255 - ((255 - oB) * (255 - gB) / 255)).coerceIn(0, 255)
            out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        blurred.recycle()
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * يستخرج المناطق الساطعة من الصورة (للاستخدام في Bloom).
     */
    private fun extractBrightAreas(bitmap: Bitmap, threshold: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF
            val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            if (luma > threshold) {
                // تكثيف الإضاءة
                val factor = (luma - threshold).coerceAtMost(75) / 75f * 2f
                val nr = (r * factor).coerceIn(0, 255).toInt()
                val ng = (g * factor).coerceIn(0, 255).toInt()
                val nb = (b * factor).coerceIn(0, 255).toInt()
                val a = (pixels[i] ushr 24) and 0xFF
                pixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            } else {
                pixels[i] = 0
            }
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * Aurora - ألوان الشفق القطبي (أخضر/بنفسجي/أزرق).
     */
    fun applyAurora(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            0.85f, 0f, 0.15f, 0f, 5f,
            0.05f, 1.1f, 0.1f, 0f, 10f,
            0.2f, 0.1f, 1.15f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        ))
        var result = applyColorMatrix(bitmap, cm)
        val sat = ColorMatrix().apply { setSaturation(1.15f) }
        result = applyColorMatrix(result, sat)
        return result
    }

    /**
     * Matte - مظهر سينمائي مسطّح بألوان باهتة.
     */
    fun applyMatte(bitmap: Bitmap): Bitmap {
        // رفع النقاط السوداء وخفض البيضاء لتأثير مسطّح
        val cm = ColorMatrix(floatArrayOf(
            0.9f, 0f, 0f, 0f, 15f,
            0f, 0.9f, 0f, 0f, 15f,
            0f, 0f, 0.9f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        ))
        var result = applyColorMatrix(bitmap, cm)
        val sat = ColorMatrix().apply { setSaturation(0.85f) }
        result = applyColorMatrix(result, sat)
        return result
    }

    /**
     * Emerald - درجات خضراء فيروزية.
     */
    fun applyEmerald(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            0.85f, 0f, 0f, 0f, 5f,
            0f, 1.15f, 0f, 0f, 10f,
            0f, 0.05f, 0.95f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(1.1f) }
        cm.postConcat(sat)
        return applyColorMatrix(bitmap, cm)
    }

    /**
     * Crimson - درجات حمراء قرمزية درامية.
     */
    fun applyCrimson(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            1.25f, 0f, 0f, 0f, 5f,
            0f, 0.85f, 0f, 0f, 0f,
            0f, 0f, 0.8f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        ))
        val contrast = ColorMatrix(floatArrayOf(
            1.15f, 0f, 0f, 0f, -15f,
            0f, 1.15f, 0f, 0f, -15f,
            0f, 0f, 1.15f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrast)
        return applyColorMatrix(bitmap, cm)
    }

    /**
     * Golden - ألوان ذهبية دافئة.
     */
    fun applyGolden(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 15f,
            0f, 1.1f, 0f, 0f, 8f,
            0f, 0f, 0.75f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(1.2f) }
        cm.postConcat(sat)
        return applyColorMatrix(bitmap, cm)
    }

    /**
     * Ocean - درجات زرقاء/فيروزية عميقة.
     */
    fun applyOcean(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            0.8f, 0f, 0f, 0f, -5f,
            0f, 0.95f, 0f, 0f, 5f,
            0f, 0.05f, 1.2f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(1.05f) }
        cm.postConcat(sat)
        return applyColorMatrix(bitmap, cm)
    }

    /**
     * Pastel - ألوان باستيل ناعمة.
     */
    fun applyPastel(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            0.95f, 0.05f, 0.05f, 0f, 20f,
            0.05f, 0.95f, 0.05f, 0f, 20f,
            0.05f, 0.05f, 0.95f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(0.8f) }
        cm.postConcat(sat)
        return applyColorMatrix(bitmap, cm)
    }

    /**
     * Neon - ألوان نيون زاهية في الليل.
     */
    fun applyNeon(bitmap: Bitmap): Bitmap {
        // تشبع عالٍ جداً + تباين عالٍ
        val cm = ColorMatrix().apply { setSaturation(2f) }
        val contrast = ColorMatrix(floatArrayOf(
            1.3f, 0f, 0f, 0f, -40f,
            0f, 1.3f, 0f, 0f, -40f,
            0f, 0f, 1.3f, 0f, -40f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrast)
        return applyColorMatrix(bitmap, cm)
    }

    // ===== الفلاتر المعقدة (معالجة بكسل ببكسل) =====

    /**
     * فلتر الرسم بالقلم - يحوّل الصورة إلى رسم تخطيطي.
     */
    fun applySketchFilter(bitmap: Bitmap): Bitmap {
        // 1. تحويل إلى تدرج رمادي
        val gray = applyBlackWhite(bitmap)
        // 2. عكس الصورة الرمادية
        val inverted = applyInvert(gray)
        // 3. تطبيق blur بسيط على المقلوبة (Dodge)
        val blurred = applySimpleBlur(inverted, 5)
        // 4. Color Dodge blend: result = base / (255 - mask) * 255
        val w = bitmap.width
        val h = bitmap.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val basePixels = IntArray(w * h)
        val maskPixels = IntArray(w * h)
        gray.getPixels(basePixels, 0, w, 0, 0, w, h)
        blurred.getPixels(maskPixels, 0, w, 0, 0, w, h)

        for (i in basePixels.indices) {
            val baseR = (basePixels[i] shr 16) and 0xFF
            val baseG = (basePixels[i] shr 8) and 0xFF
            val baseB = basePixels[i] and 0xFF
            val maskR = (maskPixels[i] shr 16) and 0xFF
            val maskG = (maskPixels[i] shr 8) and 0xFF
            val maskB = maskPixels[i] and 0xFF

            val r = if (maskR == 255) 255 else min(255, (baseR * 255) / (255 - maskR))
            val g = if (maskG == 255) 255 else min(255, (baseG * 255) / (255 - maskG))
            val b = if (maskB == 255) 255 else min(255, (baseB * 255) / (255 - maskB))

            val alpha = (basePixels[i] ushr 24) and 0xFF
            resultPixels(result, i, alpha, r, g, b)
        }
        if (gray != bitmap) gray.recycle()
        inverted.recycle()
        blurred.recycle()
        return result
    }

    private fun resultPixels(bitmap: Bitmap, i: Int, a: Int, r: Int, g: Int, b: Int) {
        val color = (a shl 24) or (r shl 16) or (g shl 8) or b
        // setPixel بطيء - نستخدم مصفوفة بدلاً منه عند الإمكان
        val x = i % bitmap.width
        val y = i / bitmap.width
        bitmap.setPixel(x, y, color)
    }

    /**
     * تطبيق blur بسيط (box blur) - أبطأ من Gaussian لكنه كافٍ للفلتر.
     */
    fun applySimpleBlur(bitmap: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        // blur أفقي ثم عمودي
        val tmp = IntArray(w * h)
        horizontalBlur(src, tmp, w, h, radius)
        verticalBlur(tmp, dst, w, h, radius)

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    private fun horizontalBlur(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val windowSize = 2 * r + 1
        for (y in 0 until h) {
            var sumR = 0; var sumG = 0; var sumB = 0; var sumA = 0
            // النافذة الأولى
            for (k in -r..r) {
                val xx = k.coerceIn(0, w - 1)
                val px = src[y * w + xx]
                sumA += (px ushr 24) and 0xFF
                sumR += (px shr 16) and 0xFF
                sumG += (px shr 8) and 0xFF
                sumB += px and 0xFF
            }
            for (x in 0 until w) {
                val a = sumA / windowSize
                val rr = sumR / windowSize
                val g = sumG / windowSize
                val b = sumB / windowSize
                dst[y * w + x] = (a shl 24) or (rr shl 16) or (g shl 8) or b
                // تحريك النافذة: أضف اليمين واحذف اليسار
                val xAdd = (x + r + 1).coerceIn(0, w - 1)
                val xSub = (x - r).coerceIn(0, w - 1)
                val pAdd = src[y * w + xAdd]
                val pSub = src[y * w + xSub]
                sumA += ((pAdd ushr 24) and 0xFF) - ((pSub ushr 24) and 0xFF)
                sumR += ((pAdd shr 16) and 0xFF) - ((pSub shr 16) and 0xFF)
                sumG += ((pAdd shr 8) and 0xFF) - ((pSub shr 8) and 0xFF)
                sumB += (pAdd and 0xFF) - (pSub and 0xFF)
            }
        }
    }

    private fun verticalBlur(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val windowSize = 2 * r + 1
        for (x in 0 until w) {
            var sumR = 0; var sumG = 0; var sumB = 0; var sumA = 0
            for (k in -r..r) {
                val yy = k.coerceIn(0, h - 1)
                val px = src[yy * w + x]
                sumA += (px ushr 24) and 0xFF
                sumR += (px shr 16) and 0xFF
                sumG += (px shr 8) and 0xFF
                sumB += px and 0xFF
            }
            for (y in 0 until h) {
                val a = sumA / windowSize
                val rr = sumR / windowSize
                val g = sumG / windowSize
                val b = sumB / windowSize
                dst[y * w + x] = (a shl 24) or (rr shl 16) or (g shl 8) or b
                val yAdd = (y + r + 1).coerceIn(0, h - 1)
                val ySub = (y - r).coerceIn(0, h - 1)
                val pAdd = src[yAdd * w + x]
                val pSub = src[ySub * w + x]
                sumA += ((pAdd ushr 24) and 0xFF) - ((pSub ushr 24) and 0xFF)
                sumR += ((pAdd shr 16) and 0xFF) - ((pSub shr 16) and 0xFF)
                sumG += ((pAdd shr 8) and 0xFF) - ((pSub shr 8) and 0xFF)
                sumB += (pAdd and 0xFF) - (pSub and 0xFF)
            }
        }
    }

    // ===================== أدوات التعديل =====================

    /**
     * تطبيق جميع إعدادات التعديل على الصورة.
     * الترتيب: exposure -> brightness -> contrast -> highlights/shadows
     *         -> saturation -> warmth -> sharpness -> vignette -> grain
     */
    fun applyAdjustments(bitmap: Bitmap, settings: AdjustSettings): Bitmap {
        if (settings.isDefault) return bitmap
        var result = bitmap

        // 1. Exposure + Brightness (إزاحة خطية ومضاعفة)
        if (settings.exposure != 0f || settings.brightness != 0f) {
            val exposureFactor = 1f + settings.exposure / 100f * 0.5f
            val brightnessAdd = settings.brightness * 1.5f
            val cm = ColorMatrix(floatArrayOf(
                exposureFactor, 0f, 0f, 0f, brightnessAdd,
                0f, exposureFactor, 0f, 0f, brightnessAdd,
                0f, 0f, exposureFactor, 0f, brightnessAdd,
                0f, 0f, 0f, 1f, 0f
            ))
            result = applyColorMatrix(result, cm)
        }

        // 2. Contrast
        if (settings.contrast != 0f) {
            val c = 1f + settings.contrast / 100f
            val t = 128f * (1 - c)
            val cm = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            ))
            result = applyColorMatrix(result, cm)
        }

        // 3. Highlights / Shadows (باستخدام gamma-like على القنوات)
        if (settings.highlights != 0f || settings.shadows != 0f) {
            result = applyHighlightsShadows(result, settings.highlights, settings.shadows)
        }

        // 4. Saturation
        if (settings.saturation != 0f) {
            val sat = 1f + settings.saturation / 100f
            val cm = ColorMatrix().apply { setSaturation(sat) }
            result = applyColorMatrix(result, cm)
        }

        // 5. Warmth (تعديل R و B فقط)
        if (settings.warmth != 0f) {
            val warmFactor = settings.warmth / 100f
            val rShift = warmFactor * 25f
            val bShift = -warmFactor * 25f
            val cm = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, rShift,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, bShift,
                0f, 0f, 0f, 1f, 0f
            ))
            result = applyColorMatrix(result, cm)
        }

        // 6. Sharpness (unsharp mask بسيط)
        if (settings.sharpness > 0f) {
            result = applySharpen(result, settings.sharpness / 100f)
        }

        // 7. Vignette
        if (settings.vignette > 0f) {
            result = applyVignetteEffect(result, settings.vignette / 100f)
        }

        // 8. Grain
        if (settings.grain > 0f) {
            result = applyGrain(result, settings.grain / 100f)
        }

        return result
    }

    /**
     * تعديل الإضاءات والظلال بمعالجة البكسلات.
     * Highlights: زيادة تجعل المناطق الفاتحة أكثر فاتحة، تخفيض تجعلها أقل فاتحة.
     * Shadows: زيادة تفتح المناطق الداكنة، تخفيض تعمّقها.
     */
    private fun applyHighlightsShadows(bitmap: Bitmap, highlights: Float, shadows: Float): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val hFactor = highlights / 100f * 0.5f
        val sFactor = shadows / 100f * 0.5f

        for (i in pixels.indices) {
            val a = (pixels[i] ushr 24) and 0xFF
            var r = (pixels[i] shr 16) and 0xFF
            var g = (pixels[i] shr 8) and 0xFF
            var b = pixels[i] and 0xFF

            r = adjustLumaChannel(r, hFactor, sFactor)
            g = adjustLumaChannel(g, hFactor, sFactor)
            b = adjustLumaChannel(b, hFactor, sFactor)

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun adjustLumaChannel(value: Int, highlights: Float, shadows: Float): Int {
        val v = value.toFloat() / 255f
        // عتبة: أقل من 0.5 = ظل، أكبر = إضاءة
        val isHighlight = v > 0.5f
        val norm = if (isHighlight) (v - 0.5f) * 2f else v * 2f
        val adj = if (isHighlight) highlights * norm else shadows * norm
        val newV = (v + adj).coerceIn(0f, 1f)
        return (newV * 255f).toInt()
    }

    /**
     * تطبيق زيادة الحدة باستخدام unsharp mask.
     * amount: 0..1
     */
    private fun applySharpen(bitmap: Bitmap, amount: Float): Bitmap {
        val blurred = applySimpleBlur(bitmap, 1)
        val w = bitmap.width
        val h = bitmap.height
        val orig = IntArray(w * h)
        val blur = IntArray(w * h)
        bitmap.getPixels(orig, 0, w, 0, 0, w, h)
        blurred.getPixels(blur, 0, w, 0, 0, w, h)

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val out = IntArray(w * h)
        val k = amount * 2.5f

        for (i in orig.indices) {
            val a = (orig[i] ushr 24) and 0xFF
            val oR = (orig[i] shr 16) and 0xFF
            val oG = (orig[i] shr 8) and 0xFF
            val oB = orig[i] and 0xFF
            val bR = (blur[i] shr 16) and 0xFF
            val bG = (blur[i] shr 8) and 0xFF
            val bB = blur[i] and 0xFF

            val r = (oR + k * (oR - bR)).coerceIn(0f, 255f).toInt()
            val g = (oG + k * (oG - bG)).coerceIn(0f, 255f).toInt()
            val b = (oB + k * (oB - bB)).coerceIn(0f, 255f).toInt()
            out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        result.setPixels(out, 0, w, 0, 0, w, h)
        blurred.recycle()
        return result
    }

    /**
     * تأثير تعتيم الحواف (Vignette) - يضع طبقة سوداء تتدرّج من الحواف.
     * intensity: 0..1
     */
    fun applyVignetteEffect(bitmap: Bitmap, intensity: Float): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // تدرّج شعاعي من الشفاف في المركز إلى الأسود على الحواف
        val cx = w / 2f
        val cy = h / 2f
        val radius = (max(w, h) / 2f) * 1.2f
        val shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(0x00000000, 0x00000000, (0xFF * intensity).toInt().coerceIn(0, 255) shl 24),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return result
    }

    /**
     * إضافة حبيبات فيلم (Grain) - ضوضاء عشوائية.
     * intensity: 0..1
     */
    fun applyGrain(bitmap: Bitmap, intensity: Float): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val random = Random(42) // ثابت للحصول على نفس النتيجة
        val amount = intensity * 60f

        for (i in pixels.indices) {
            val a = (pixels[i] ushr 24) and 0xFF
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF
            val noise = (random.nextFloat() - 0.5f) * amount
            val nr = (r + noise).coerceIn(0f, 255f).toInt()
            val ng = (g + noise).coerceIn(0f, 255f).toInt()
            val nb = (b + noise).coerceIn(0f, 255f).toInt()
            pixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    // ===================== أدوات التحويل =====================

    /**
     * تطبيق التحويل (تدوير + قلب).
     */
    fun applyTransform(bitmap: Bitmap, rotation: Int, flipH: Boolean, flipV: Boolean): Bitmap {
        if (rotation == 0 && !flipH && !flipV) return bitmap
        val matrix = Matrix()
        // 1. القلب أولاً
        if (flipH) matrix.preScale(-1f, 1f)
        if (flipV) matrix.preScale(1f, -1f)
        // 2. التدوير
        val normalizedRotation = ((rotation % 360) + 360) % 360
        if (normalizedRotation != 0) {
            matrix.postRotate(normalizedRotation.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * قص الصورة حسب الـ Rect المعطى.
     */
    fun cropBitmap(bitmap: Bitmap, rect: Rect): Bitmap {
        val safeRect = Rect(
            rect.left.coerceIn(0, bitmap.width - 1),
            rect.top.coerceIn(0, bitmap.height - 1),
            rect.right.coerceIn(1, bitmap.width),
            rect.bottom.coerceIn(1, bitmap.height)
        )
        return Bitmap.createBitmap(
            bitmap,
            safeRect.left,
            safeRect.top,
            safeRect.width(),
            safeRect.height()
        )
    }

    /**
     * المعالجة الكاملة - تطبيق الفلتر ثم التعديلات ثم التحويل.
     */
    fun processFull(bitmap: Bitmap, state: EditorState): Bitmap {
        var result = bitmap
        // 1. التحويل أولاً (يغيّر أبعاد الصورة)
        if (state.rotationDegrees != 0 || state.flipHorizontal || state.flipVertical) {
            result = applyTransform(result, state.rotationDegrees, state.flipHorizontal, state.flipVertical)
        }
        // 2. الفلتر
        if (state.filter != FilterType.NONE) {
            result = applyFilter(result, state.filter)
        }
        // 3. التعديلات
        if (!state.adjust.isDefault) {
            result = applyAdjustments(result, state.adjust)
        }
        return result
    }
}
