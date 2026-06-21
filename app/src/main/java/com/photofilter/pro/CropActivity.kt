package com.photofilter.pro

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.photofilter.pro.databinding.ActivityCropBinding
import com.photofilter.pro.utils.EditorState
import com.photofilter.pro.utils.ImageProcessor
import com.photofilter.pro.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * شاشة القص - تعرض الصورة (بعد تطبيق التحويل الحالي) مع إطار قابل للسحب.
 * عند الضغط على "تطبيق"، تحسب إحداثيات الإطار على الصورة الأصلية وتعيدها لـ EditActivity.
 */
class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropBinding
    private var sourceBitmap: Bitmap? = null
    private var editorState: EditorState = EditorState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // قراءة الـ intent
        val uriString = intent.getStringExtra(EXTRA_INPUT_URI)
        @Suppress("DEPRECATION")
        editorState = intent.getSerializableExtra(EXTRA_STATE) as? EditorState ?: EditorState()

        if (uriString == null) {
            finish()
            return
        }

        // تحميل الصورة وتطبيق التحويلات الحالية
        lifecycleScope.launch {
            val uri = android.net.Uri.parse(uriString)
            val loaded = withContext(Dispatchers.IO) {
                ImageUtils.loadBitmap(contentResolver, uri)
            }
            if (loaded == null) {
                Snackbar.make(binding.root, "تعذّر تحميل الصورة", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            val transformed = withContext(Dispatchers.IO) {
                if (editorState.rotationDegrees != 0 ||
                    editorState.flipHorizontal ||
                    editorState.flipVertical) {
                    ImageProcessor.applyTransform(
                        loaded,
                        editorState.rotationDegrees,
                        editorState.flipHorizontal,
                        editorState.flipVertical
                    )
                } else {
                    loaded
                }
            }
            sourceBitmap = transformed
            binding.ivImage.setImageBitmap(transformed)

            // بعد رسم الصورة، نمرّر منطقة عرضها للـ overlay
            binding.ivImage.post {
                updateOverlayRect()
            }

            setupAspectButtons()
            setupActions()
        }
    }

    /**
     * يحسب منطقة عرض الصورة (fitCenter) ويمرّرها للـ overlay.
     */
    private fun updateOverlayRect() {
        val bmp = sourceBitmap ?: return
        val viewW = binding.ivImage.width.toFloat()
        val viewH = binding.ivImage.height.toFloat()
        if (viewW <= 0 || viewH <= 0) return

        val bmpW = bmp.width.toFloat()
        val bmpH = bmp.height.toFloat()
        val scale = minOf(viewW / bmpW, viewH / bmpH)
        val dispW = bmpW * scale
        val dispH = bmpH * scale
        val left = (viewW - dispW) / 2f
        val top = (viewH - dispH) / 2f
        val rect = RectF(left, top, left + dispW, top + dispH)
        binding.cropOverlay.setImageDisplayRect(rect)
    }

    private val aspectButtons by lazy {
        listOf(
            Pair(binding.btnAspectFree, 0f),
            Pair(binding.btnAspect1_1, 1f),
            Pair(binding.btnAspect4_3, 4f / 3f),
            Pair(binding.btnAspect3_4, 3f / 4f),
            Pair(binding.btnAspect16_9, 16f / 9f),
            Pair(binding.btnAspect9_16, 9f / 16f)
        )
    }

    private fun setupAspectButtons() {
        aspectButtons.forEach { (btn, ratio) ->
            btn.setOnClickListener {
                binding.cropOverlay.setAspectRatio(ratio)
                // تحديث أنماط الأزرار
                aspectButtons.forEach { (b, _) ->
                    b.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(this, R.color.brand_dark_outline)
                    )
                }
                (btn as MaterialButton).backgroundTintList = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.brand_primary)
                )
            }
        }
    }

    private fun setupActions() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnApply.setOnClickListener {
            applyCrop()
        }
    }

    private fun applyCrop() {
        val bmp = sourceBitmap ?: run {
            finish()
            return
        }
        val viewRect = binding.cropOverlay.getCropRect()
        // تحويل من إحداثيات الـ view إلى إحداثيات الصورة الأصلية
        val viewW = binding.ivImage.width.toFloat()
        val viewH = binding.ivImage.height.toFloat()
        if (viewW <= 0 || viewH <= 0) {
            finish()
            return
        }
        val bmpW = bmp.width.toFloat()
        val bmpH = bmp.height.toFloat()
        val scale = minOf(viewW / bmpW, viewH / bmpH)
        val dispW = bmpW * scale
        val dispH = bmpH * scale
        val offsetX = (viewW - dispW) / 2f
        val offsetY = (viewH - dispH) / 2f

        // إحداثيات نسبية لمنطقة الصورة المعروضة
        val relLeft = (viewRect.left - offsetX).coerceIn(0f, dispW)
        val relTop = (viewRect.top - offsetY).coerceIn(0f, dispH)
        val relRight = (viewRect.right - offsetX).coerceIn(0f, dispW)
        val relBottom = (viewRect.bottom - offsetY).coerceIn(0f, dispH)

        // إحداثيات على الصورة الأصلية
        val bmpLeft = (relLeft / scale).toInt().coerceIn(0, bmp.width - 1)
        val bmpTop = (relTop / scale).toInt().coerceIn(0, bmp.height - 1)
        val bmpRight = (relRight / scale).toInt().coerceIn(1, bmp.width)
        val bmpBottom = (relBottom / scale).toInt().coerceIn(1, bmp.height)

        val cropRect = Rect(bmpLeft, bmpTop, bmpRight, bmpBottom)
        val resultIntent = Intent().apply {
            putExtra(RESULT_CROP_RECT, cropRect)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_INPUT_URI = "input_uri"
        const val EXTRA_STATE = "editor_state"
        const val RESULT_CROP_RECT = "crop_rect"
    }
}
