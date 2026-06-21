package com.photofilter.pro.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * View لعرض مقارنة قبل/بعد بسلايدر قابل للسحب.
 * الصورة الأصلية على اليسار، المعالجة على اليمين،
 * ويوجد مقبض سحب عمودي للتحكم في موضع المقارنة.
 */
class BeforeAfterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var beforeBitmap: Bitmap? = null
    private var afterBitmap: Bitmap? = null
    private var sliderPosition: Float = 0.5f  // 0..1
    private var isDragging = false

    // رسم
    private val dividerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val handleBgPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val handleIconPaint = Paint().apply {
        color = Color.parseColor("#FF6B35")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
        isFakeBoldText = true
    }
    private val labelBgPaint = Paint().apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val srcRect = Rect()
    private val dstRect = RectF()
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val handleTouchRadius = 60f

    /** يضبط الصورة الأصلية (قبل) */
    fun setBeforeBitmap(bitmap: Bitmap?) {
        beforeBitmap = bitmap
        invalidate()
    }

    /** يضبط الصورة المعالجة (بعد) */
    fun setAfterBitmap(bitmap: Bitmap?) {
        afterBitmap = bitmap
        invalidate()
    }

    /** يضبط موضع السلايدر (0..1) */
    fun setSliderPosition(position: Float) {
        sliderPosition = position.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val before = beforeBitmap ?: return
        val after = afterBitmap ?: before

        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW <= 0 || viewH <= 0) return

        // حساب منطقة عرض الصورة (fitCenter)
        val scale = min(viewW / before.width, viewH / before.height)
        val dispW = before.width * scale
        val dispH = before.height * scale
        val offsetX = (viewW - dispW) / 2f
        val offsetY = (viewH - dispH) / 2f

        // 1. رسم الصورة الأصلية (before) كاملة
        srcRect.set(0, 0, before.width, before.height)
        dstRect.set(offsetX, offsetY, offsetX + dispW, offsetY + dispH)
        canvas.drawBitmap(before, srcRect, dstRect, null)

        // 2. رسم الصورة بعد (after) فقط على يمين السلايدر
        val dividerX = offsetX + dispW * sliderPosition

        // نحفظ الـ canvas ونقصّ المنطقة على يمين السلايدر
        canvas.save()
        canvas.clipRect(dividerX, 0f, viewW, viewH)
        if (after !== before) {
            canvas.drawBitmap(after, srcRect, dstRect, null)
        } else {
            canvas.drawBitmap(before, srcRect, dstRect, null)
        }
        canvas.restore()

        // 3. رسم خط الفاصل
        canvas.drawLine(dividerX, offsetY, dividerX, offsetY + dispH, dividerPaint)

        // 4. رسم مقبض السحب (دائرة بيضاء مع سهمين)
        val handleCenterY = viewH / 2f
        canvas.drawCircle(dividerX, handleCenterY, 36f, handleBgPaint)
        // سهم يسار
        canvas.drawLine(dividerX - 12f, handleCenterY, dividerX - 4f, handleCenterY - 8f, handleIconPaint)
        canvas.drawLine(dividerX - 12f, handleCenterY, dividerX - 4f, handleCenterY + 8f, handleIconPaint)
        // سهم يمين
        canvas.drawLine(dividerX + 12f, handleCenterY, dividerX + 4f, handleCenterY - 8f, handleIconPaint)
        canvas.drawLine(dividerX + 12f, handleCenterY, dividerX + 4f, handleCenterY + 8f, handleIconPaint)

        // 5. رسم تسميات "قبل" و "بعد"
        drawLabel(canvas, "قبل", offsetX + 16f, offsetY + 16f, 16f)
        drawLabel(canvas, "بعد", offsetX + dispW - 80f, offsetY + 16f, 16f)
    }

    private fun drawLabel(canvas: Canvas, text: String, x: Float, y: Float, padding: Float) {
        val textWidth = labelPaint.measureText(text)
        val textHeight = labelPaint.textSize
        val rectF = RectF(x, y, x + textWidth + padding * 2, y + textHeight + padding)
        canvas.drawRoundRect(rectF, 12f, 12f, labelBgPaint)
        canvas.drawText(text, x + padding, y + textHeight - 4f, labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val viewW = width.toFloat()
                if (viewW <= 0) return false
                val dividerX = viewW * sliderPosition
                val dx = abs(event.x - dividerX)
                val dy = abs(event.y - height / 2f)
                // قبول اللمس لو كان قرب المقبض أو قرب الخط الفاصل
                if (dx < handleTouchRadius * 2 || (dx < 100f && event.y > 0 && event.y < height)) {
                    isDragging = true
                    updateSliderFromTouch(event.x)
                    return true
                }
                // أو قبول أي لمس داخل الـ view لتفعيل السلايدر
                isDragging = true
                updateSliderFromTouch(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateSliderFromTouch(event.x)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateSliderFromTouch(touchX: Float) {
        val viewW = width.toFloat()
        if (viewW <= 0) return
        // احسب منطقة الصورة المعروضة
        val before = beforeBitmap ?: return
        val scale = min(viewW / before.width, height.toFloat() / before.height)
        val dispW = before.width * scale
        val offsetX = (viewW - dispW) / 2f
        // السماح بالسحب فقط داخل منطقة الصورة
        val relX = (touchX - offsetX) / dispW
        setSliderPosition(relX)
    }
}
