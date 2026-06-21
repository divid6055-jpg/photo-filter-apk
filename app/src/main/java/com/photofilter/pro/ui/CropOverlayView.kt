package com.photofilter.pro.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * View مخصص لرسم إطار القص على الصورة.
 * يدعم:
 * - سحب الإطار من أي مكان داخل المربع
 * - سحب المقابض (المقابض الـ 8) لتغيير الحجم
 * - تثبيت نسبة العرض للارتفاع عند ضبط نسبة معينة
 * - شبكة قاعدة الأثلاث لمساعدة التكوين
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** منطقة القص الحالية بإحداثيات الـ view */
    private var cropRect: RectF = RectF()
    private val handleSize = 32f
    private val touchPadding = 24f

    /** نسبة العرض للارتفاع (0 = حر) */
    private var aspectRatio: Float = 0f

    // منطقة الصورة المعروضة (fitCenter) لإعادة حساب الإحداثيات
    private var imageDisplayRect: RectF = RectF()

    // رسم
    private val dimPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val gridPaint = Paint().apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    // حالة السحب
    private enum class DragMode { NONE, MOVE, RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR,
        RESIZE_T, RESIZE_B, RESIZE_L, RESIZE_R }
    private var dragMode = DragMode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            // تهيئة cropRect ليغطّي 80% من الـ view
            val margin = min(w, h) * 0.1f
            cropRect = RectF(margin, margin, w - margin, h - margin)
            if (aspectRatio > 0) applyAspectRatio()
            invalidate()
        }
    }

    /**
     * يحدّث منطقة الصورة المعروضة لاستخدامها لاحقاً في تحويل الإحداثيات.
     */
    fun setImageDisplayRect(rect: RectF) {
        imageDisplayRect = RectF(rect)
        // كرّب cropRect داخل منطقة الصورة
        cropRect.intersect(imageDisplayRect)
        invalidate()
    }

    /**
     * يضبط نسبة العرض للارتفاع. 0 = حر.
     */
    fun setAspectRatio(ratio: Float) {
        aspectRatio = ratio
        if (ratio > 0 && width > 0 && height > 0) {
            applyAspectRatio()
            invalidate()
        }
    }

    private fun applyAspectRatio() {
        val cx = cropRect.centerX()
        val cy = cropRect.centerY()
        val currentW = cropRect.width()
        val currentH = cropRect.height()
        // نأخذ الأكبر من الحالي ونعدّل الآخر
        if (currentW / currentH > aspectRatio) {
            val newH = currentW / aspectRatio
            cropRect = RectF(
                cropRect.left,
                cy - newH / 2,
                cropRect.right,
                cy + newH / 2
            )
        } else {
            val newW = currentH * aspectRatio
            cropRect = RectF(
                cx - newW / 2,
                cropRect.top,
                cx + newW / 2,
                cropRect.bottom
            )
        }
        // قيود الـ view
        if (cropRect.width() > width) {
            val scale = width / cropRect.width()
            cropRect = RectF(
                0f,
                cropRect.top,
                width.toFloat(),
                cropRect.top + cropRect.height() * scale
            )
        }
        if (cropRect.height() > height) {
            val scale = height / cropRect.height()
            cropRect = RectF(
                cropRect.left,
                0f,
                cropRect.left + cropRect.width() * scale,
                height.toFloat()
            )
        }
    }

    /**
     * يرجع Rect القص بإحداثيات الـ view.
     */
    fun getCropRect(): Rect {
        return Rect(
            cropRect.left.toInt().coerceIn(0, width),
            cropRect.top.toInt().coerceIn(0, height),
            cropRect.right.toInt().coerceIn(0, width),
            cropRect.bottom.toInt().coerceIn(0, height)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. تعتيم خارج الإطار (4 قطع)
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, dimPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, dimPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, dimPaint)

        // 2. شبكة الأثلاث
        val thirdW = cropRect.width() / 3
        val thirdH = cropRect.height() / 3
        canvas.drawLine(cropRect.left + thirdW, cropRect.top, cropRect.left + thirdW, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left + 2 * thirdW, cropRect.top, cropRect.left + 2 * thirdW, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + thirdH, cropRect.right, cropRect.top + thirdH, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + 2 * thirdH, cropRect.right, cropRect.top + 2 * thirdH, gridPaint)

        // 3. الإطار
        canvas.drawRect(cropRect, borderPaint)

        // 4. المقابض (الزوايا + المنتصفات)
        drawHandles(canvas)
    }

    private fun drawHandles(canvas: Canvas) {
        val r = handleSize / 2
        // الزوايا
        canvas.drawCircle(cropRect.left, cropRect.top, r, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.top, r, handlePaint)
        canvas.drawCircle(cropRect.left, cropRect.bottom, r, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.bottom, r, handlePaint)
        // المنتصفات
        canvas.drawCircle(cropRect.centerX(), cropRect.top, r * 0.8f, handlePaint)
        canvas.drawCircle(cropRect.centerX(), cropRect.bottom, r * 0.8f, handlePaint)
        canvas.drawCircle(cropRect.left, cropRect.centerY(), r * 0.8f, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.centerY(), r * 0.8f, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                dragMode = detectDragMode(event.x, event.y)
                return dragMode != DragMode.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragMode == DragMode.NONE) return false
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y
                handleDrag(dx, dy)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragMode = DragMode.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun detectDragMode(x: Float, y: Float): DragMode {
        val tlDist = dist(x, y, cropRect.left, cropRect.top)
        val trDist = dist(x, y, cropRect.right, cropRect.top)
        val blDist = dist(x, y, cropRect.left, cropRect.bottom)
        val brDist = dist(x, y, cropRect.right, cropRect.bottom)
        val handleTouch = handleSize + touchPadding
        // أولوية للزوايا
        if (tlDist < handleTouch) return DragMode.RESIZE_TL
        if (trDist < handleTouch) return DragMode.RESIZE_TR
        if (blDist < handleTouch) return DragMode.RESIZE_BL
        if (brDist < handleTouch) return DragMode.RESIZE_BR
        // ثم المنتصفات
        if (abs(y - cropRect.top) < touchPadding && abs(x - cropRect.centerX()) < cropRect.width() / 2) {
            return DragMode.RESIZE_T
        }
        if (abs(y - cropRect.bottom) < touchPadding && abs(x - cropRect.centerX()) < cropRect.width() / 2) {
            return DragMode.RESIZE_B
        }
        if (abs(x - cropRect.left) < touchPadding && abs(y - cropRect.centerY()) < cropRect.height() / 2) {
            return DragMode.RESIZE_L
        }
        if (abs(x - cropRect.right) < touchPadding && abs(y - cropRect.centerY()) < cropRect.height() / 2) {
            return DragMode.RESIZE_R
        }
        // وأخيراً النقل داخل الإطار
        if (cropRect.contains(x, y)) return DragMode.MOVE
        return DragMode.NONE
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return kotlin.math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
    }

    private fun handleDrag(dx: Float, dy: Float) {
        when (dragMode) {
            DragMode.MOVE -> {
                var newLeft = cropRect.left + dx
                var newTop = cropRect.top + dy
                var newRight = cropRect.right + dx
                var newBottom = cropRect.bottom + dy
                // قيود الـ view
                if (newLeft < 0) {
                    val adjust = -newLeft; newLeft += adjust; newRight += adjust
                }
                if (newTop < 0) {
                    val adjust = -newTop; newTop += adjust; newBottom += adjust
                }
                if (newRight > width) {
                    val adjust = newRight - width; newLeft -= adjust; newRight -= adjust
                }
                if (newBottom > height) {
                    val adjust = newBottom - height; newTop -= adjust; newBottom -= adjust
                }
                cropRect = RectF(newLeft, newTop, newRight, newBottom)
            }
            DragMode.RESIZE_TL -> resizeTopLeft(dx, dy)
            DragMode.RESIZE_TR -> resizeTopRight(dx, dy)
            DragMode.RESIZE_BL -> resizeBottomLeft(dx, dy)
            DragMode.RESIZE_BR -> resizeBottomRight(dx, dy)
            DragMode.RESIZE_T -> resizeTop(dy)
            DragMode.RESIZE_B -> resizeBottom(dy)
            DragMode.RESIZE_L -> resizeLeft(dx)
            DragMode.RESIZE_R -> resizeRight(dx)
            else -> {}
        }
        // تأكيد عدم الخروج عن الـ view
        cropRect.left = cropRect.left.coerceIn(0f, width.toFloat())
        cropRect.top = cropRect.top.coerceIn(0f, height.toFloat())
        cropRect.right = cropRect.right.coerceIn(0f, width.toFloat())
        cropRect.bottom = cropRect.bottom.coerceIn(0f, height.toFloat())
        // تأكيد حجم أدنى
        val minSize = 60f
        if (cropRect.width() < minSize) {
            cropRect.right = cropRect.left + minSize
        }
        if (cropRect.height() < minSize) {
            cropRect.bottom = cropRect.top + minSize
        }
    }

    private fun resizeTopLeft(dx: Float, dy: Float) {
        var newLeft = cropRect.left + dx
        var newTop = cropRect.top + dy
        if (aspectRatio > 0) {
            // حافظ على النسبة: اعتمد على dy واحسب dx
            val newH = cropRect.bottom - newTop
            val newW = newH * aspectRatio
            newLeft = cropRect.right - newW
        }
        cropRect.left = newLeft
        cropRect.top = newTop
    }
    private fun resizeTopRight(dx: Float, dy: Float) {
        var newRight = cropRect.right + dx
        var newTop = cropRect.top + dy
        if (aspectRatio > 0) {
            val newH = cropRect.bottom - newTop
            val newW = newH * aspectRatio
            newRight = cropRect.left + newW
        }
        cropRect.right = newRight
        cropRect.top = newTop
    }
    private fun resizeBottomLeft(dx: Float, dy: Float) {
        var newLeft = cropRect.left + dx
        var newBottom = cropRect.bottom + dy
        if (aspectRatio > 0) {
            val newH = newBottom - cropRect.top
            val newW = newH * aspectRatio
            newLeft = cropRect.right - newW
        }
        cropRect.left = newLeft
        cropRect.bottom = newBottom
    }
    private fun resizeBottomRight(dx: Float, dy: Float) {
        var newRight = cropRect.right + dx
        var newBottom = cropRect.bottom + dy
        if (aspectRatio > 0) {
            val newH = newBottom - cropRect.top
            val newW = newH * aspectRatio
            newRight = cropRect.left + newW
        }
        cropRect.right = newRight
        cropRect.bottom = newBottom
    }
    private fun resizeTop(dy: Float) {
        cropRect.top = (cropRect.top + dy).coerceAtMost(cropRect.bottom - 60f)
        if (aspectRatio > 0) {
            val newH = cropRect.bottom - cropRect.top
            val newW = newH * aspectRatio
            val cx = cropRect.centerX()
            cropRect.left = cx - newW / 2
            cropRect.right = cx + newW / 2
        }
    }
    private fun resizeBottom(dy: Float) {
        cropRect.bottom = (cropRect.bottom + dy).coerceAtLeast(cropRect.top + 60f)
        if (aspectRatio > 0) {
            val newH = cropRect.bottom - cropRect.top
            val newW = newH * aspectRatio
            val cx = cropRect.centerX()
            cropRect.left = cx - newW / 2
            cropRect.right = cx + newW / 2
        }
    }
    private fun resizeLeft(dx: Float) {
        cropRect.left = (cropRect.left + dx).coerceAtMost(cropRect.right - 60f)
        if (aspectRatio > 0) {
            val newW = cropRect.right - cropRect.left
            val newH = newW / aspectRatio
            val cy = cropRect.centerY()
            cropRect.top = cy - newH / 2
            cropRect.bottom = cy + newH / 2
        }
    }
    private fun resizeRight(dx: Float) {
        cropRect.right = (cropRect.right + dx).coerceAtLeast(cropRect.left + 60f)
        if (aspectRatio > 0) {
            val newW = cropRect.right - cropRect.left
            val newH = newW / aspectRatio
            val cy = cropRect.centerY()
            cropRect.top = cy - newH / 2
            cropRect.bottom = cy + newH / 2
        }
    }
}
