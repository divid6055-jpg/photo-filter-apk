package com.photofilter.pro

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.photofilter.pro.adapters.FilterAdapter
import com.photofilter.pro.databinding.ActivityEditBinding
import com.photofilter.pro.databinding.ItemSliderBinding
import com.photofilter.pro.utils.AdjustSettings
import com.photofilter.pro.utils.EditorState
import com.photofilter.pro.utils.FilterType
import com.photofilter.pro.utils.ImageProcessor
import com.photofilter.pro.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBinding

    private var sourceBitmap: Bitmap? = null
    private var editorState: EditorState = EditorState()
    private var compareMode: Boolean = false

    private var processingJob: Job? = null
    private var previewJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "تحرير الصورة"

        setupSliders()
        setupTabs()
        setupTransformButtons()
        setupCompareButton()

        loadImageFromIntent()
    }

    private fun loadImageFromIntent() {
        val uriString = intent.getStringExtra(EXTRA_INPUT_URI)
        if (uriString == null) {
            finish()
            return
        }
        val uri = Uri.parse(uriString)
        showProgress(true)
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ImageUtils.loadBitmap(contentResolver, uri)
            }
            showProgress(false)
            if (bitmap == null) {
                Snackbar.make(binding.root, "تعذّر تحميل الصورة", Snackbar.LENGTH_LONG).show()
                finish()
                return@launch
            }
            sourceBitmap = bitmap
            binding.ivPreview.setImageBitmap(bitmap)
            binding.beforeAfterView.setBeforeBitmap(bitmap)
            binding.beforeAfterView.setAfterBitmap(bitmap)
            setupFiltersList(bitmap)
        }
    }

    private fun setupFiltersList(bitmap: Bitmap) {
        val adapter = FilterAdapter(bitmap) { filter ->
            editorState = editorState.copy(filter = filter)
            schedulePreviewUpdate()
        }
        binding.rvFilters.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFilters.adapter = adapter
        adapter.setSelectedFilter(editorState.filter)
    }

    private fun setupSliders() {
        configureSlider(binding.sliderBrightness, getString(R.string.adjust_brightness), -100f, 100f, 0f)
        configureSlider(binding.sliderContrast, getString(R.string.adjust_contrast), -100f, 100f, 0f)
        configureSlider(binding.sliderSaturation, getString(R.string.adjust_saturation), -100f, 100f, 0f)
        configureSlider(binding.sliderWarmth, getString(R.string.adjust_warmth), -100f, 100f, 0f)
        configureSlider(binding.sliderExposure, getString(R.string.adjust_exposure), -100f, 100f, 0f)
        configureSlider(binding.sliderHighlights, getString(R.string.adjust_highlights), -100f, 100f, 0f)
        configureSlider(binding.sliderShadows, getString(R.string.adjust_shadows), -100f, 100f, 0f)
        configureSlider(binding.sliderSharpness, getString(R.string.adjust_sharpness), 0f, 100f, 0f)
        configureSlider(binding.sliderVignette, getString(R.string.adjust_vignette), 0f, 100f, 0f)
        configureSlider(binding.sliderGrain, getString(R.string.adjust_grain), 0f, 100f, 0f)
    }

    private fun configureSlider(
        sliderBinding: ItemSliderBinding,
        label: String,
        from: Float,
        to: Float,
        default: Float
    ) {
        sliderBinding.tvLabel.text = label
        sliderBinding.slider.valueFrom = from
        sliderBinding.slider.valueTo = to
        sliderBinding.slider.value = default
        sliderBinding.tvValue.text = default.toInt().toString()

        sliderBinding.slider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            sliderBinding.tvValue.text = value.toInt().toString()
            updateAdjustSettings()
            schedulePreviewUpdate()
        }
    }

    private fun updateAdjustSettings() {
        val settings = AdjustSettings(
            brightness = binding.sliderBrightness.slider.value,
            contrast = binding.sliderContrast.slider.value,
            saturation = binding.sliderSaturation.slider.value,
            warmth = binding.sliderWarmth.slider.value,
            exposure = binding.sliderExposure.slider.value,
            highlights = binding.sliderHighlights.slider.value,
            shadows = binding.sliderShadows.slider.value,
            sharpness = binding.sliderSharpness.slider.value,
            vignette = binding.sliderVignette.slider.value,
            grain = binding.sliderGrain.slider.value
        )
        editorState = editorState.copy(adjust = settings)
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_filters))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_adjust))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_transform))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.rvFilters.visibility = View.VISIBLE
                        binding.layoutAdjust.visibility = View.GONE
                        binding.layoutTransform.visibility = View.GONE
                    }
                    1 -> {
                        binding.rvFilters.visibility = View.GONE
                        binding.layoutAdjust.visibility = View.VISIBLE
                        binding.layoutTransform.visibility = View.GONE
                    }
                    2 -> {
                        binding.rvFilters.visibility = View.GONE
                        binding.layoutAdjust.visibility = View.GONE
                        binding.layoutTransform.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupTransformButtons() {
        binding.btnRotateLeft.setOnClickListener {
            editorState = editorState.copy(rotationDegrees = (editorState.rotationDegrees - 90 + 360) % 360)
            schedulePreviewUpdate()
        }
        binding.btnRotateRight.setOnClickListener {
            editorState = editorState.copy(rotationDegrees = (editorState.rotationDegrees + 90) % 360)
            schedulePreviewUpdate()
        }
        binding.btnFlipH.setOnClickListener {
            editorState = editorState.copy(flipHorizontal = !editorState.flipHorizontal)
            schedulePreviewUpdate()
        }
        binding.btnFlipV.setOnClickListener {
            editorState = editorState.copy(flipVertical = !editorState.flipVertical)
            schedulePreviewUpdate()
        }
        binding.btnCrop.setOnClickListener {
            val intent = Intent(this, CropActivity::class.java).apply {
                putExtra(CropActivity.EXTRA_INPUT_URI, this@EditActivity.intent.getStringExtra(EXTRA_INPUT_URI))
                putExtra(CropActivity.EXTRA_STATE, editorState)
            }
            startActivityForResult.launch(intent)
        }
    }

    private val startActivityForResult = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val cropRect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra(CropActivity.RESULT_CROP_RECT, android.graphics.Rect::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getParcelableExtra<android.graphics.Rect>(CropActivity.RESULT_CROP_RECT)
            }
            if (cropRect != null && sourceBitmap != null) {
                lifecycleScope.launch {
                    showProgress(true)
                    val cropped = withContext(Dispatchers.IO) {
                        ImageProcessor.cropBitmap(sourceBitmap!!, cropRect)
                    }
                    sourceBitmap = cropped
                    binding.ivPreview.setImageBitmap(cropped)
                    binding.beforeAfterView.setBeforeBitmap(cropped)
                    binding.beforeAfterView.setAfterBitmap(cropped)
                    (binding.rvFilters.adapter as? FilterAdapter)?.release()
                    setupFiltersList(cropped)
                    showProgress(false)
                }
            }
        }
    }

    private fun setupCompareButton() {
        binding.fabCompare.setOnClickListener {
            compareMode = !compareMode
            if (compareMode) {
                // إظهار before/after view
                binding.ivPreview.visibility = View.GONE
                binding.beforeAfterView.visibility = View.VISIBLE
                binding.tvModeLabel.text = "اسحب للمقارنة"
                binding.tvModeLabel.visibility = View.VISIBLE
                // تأكد من وجود afterBitmap محدّث
                updatePreviewForComparison()
            } else {
                binding.ivPreview.visibility = View.VISIBLE
                binding.beforeAfterView.visibility = View.GONE
                binding.tvModeLabel.text = "اضغط للمقارنة"
                binding.tvModeLabel.visibility = View.VISIBLE
            }
        }
    }

    private fun updatePreviewForComparison() {
        val src = sourceBitmap ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try { ImageProcessor.processFull(src, editorState) } catch (e: Exception) { null }
            }
            if (result != null) {
                binding.beforeAfterView.setAfterBitmap(result)
            }
        }
    }

    private fun schedulePreviewUpdate() {
        previewJob?.cancel()
        previewJob = lifecycleScope.launch {
            delay(120)  // تقليل التأخير للاستجابة الأسرع
            updatePreview()
        }
    }

    private fun updatePreview() {
        val src = sourceBitmap ?: return
        showProgress(true)
        processingJob?.cancel()
        processingJob = lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try { ImageProcessor.processFull(src, editorState) } catch (e: Exception) { null }
            }
            showProgress(false)
            if (result != null) {
                binding.ivPreview.setImageBitmap(result)
                if (compareMode) {
                    binding.beforeAfterView.setAfterBitmap(result)
                }
            }
        }
    }

    private fun showProgress(show: Boolean) {
        binding.progress.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_save -> { saveImage(); true }
            R.id.action_share -> { shareImage(); true }
            R.id.action_compare -> {
                binding.fabCompare.performClick()
                true
            }
            R.id.action_reset -> { resetAll(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun resetAll() {
        editorState = EditorState()
        binding.sliderBrightness.slider.value = 0f
        binding.sliderContrast.slider.value = 0f
        binding.sliderSaturation.slider.value = 0f
        binding.sliderWarmth.slider.value = 0f
        binding.sliderExposure.slider.value = 0f
        binding.sliderHighlights.slider.value = 0f
        binding.sliderShadows.slider.value = 0f
        binding.sliderSharpness.slider.value = 0f
        binding.sliderVignette.slider.value = 0f
        binding.sliderGrain.slider.value = 0f
        (binding.rvFilters.adapter as? FilterAdapter)?.setSelectedFilter(FilterType.NONE)
        sourceBitmap?.let {
            binding.ivPreview.setImageBitmap(it)
            binding.beforeAfterView.setAfterBitmap(it)
        }
        Snackbar.make(binding.root, "تمت إعادة التعيين", Snackbar.LENGTH_SHORT).show()
    }

    private fun saveImage() {
        val src = sourceBitmap ?: return
        showProgress(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try { ImageProcessor.processFull(src, editorState) } catch (e: Exception) { null }
            }
            if (result == null) {
                showProgress(false)
                Snackbar.make(binding.root, getString(R.string.msg_save_failed), Snackbar.LENGTH_LONG).show()
                return@launch
            }
            val uri = withContext(Dispatchers.IO) {
                ImageUtils.saveToGallery(this@EditActivity, result)
            }
            showProgress(false)
            if (uri != null) {
                Snackbar.make(binding.root, getString(R.string.msg_saved), Snackbar.LENGTH_LONG)
                    .setAction("عرض") {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "image/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(intent)
                    }.show()
            } else {
                Snackbar.make(binding.root, getString(R.string.msg_save_failed), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun shareImage() {
        val src = sourceBitmap ?: return
        showProgress(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try { ImageProcessor.processFull(src, editorState) } catch (e: Exception) { null }
            }
            if (result == null) {
                showProgress(false)
                return@launch
            }
            val uri = withContext(Dispatchers.IO) {
                ImageUtils.saveToGallery(this@EditActivity, result)
            }
            showProgress(false)
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (binding.rvFilters.adapter as? FilterAdapter)?.release()
        processingJob?.cancel()
        previewJob?.cancel()
    }

    companion object {
        const val EXTRA_INPUT_URI = "input_uri"
    }
}
