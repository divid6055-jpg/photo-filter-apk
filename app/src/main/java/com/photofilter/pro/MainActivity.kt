package com.photofilter.pro

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.photofilter.pro.databinding.ActivityMainBinding
import com.photofilter.pro.utils.ImageUtils
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var cameraImageUri: Uri? = null

    private val TAG = "MainActivity"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val imageUri = data?.data ?: cameraImageUri
            cameraImageUri = null
            if (imageUri != null) {
                openEditor(imageUri)
            } else {
                showSnackbar(getString(R.string.msg_no_image))
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            showPermissionRationale(getString(R.string.msg_permission_required))
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openGallery()
        } else {
            showPermissionRationale(getString(R.string.msg_permission_required))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupClickListeners()

        // معالجة intent خارجي (فتح صورة عبر مشاركة)
        handleExternalIntent(intent)
    }

    private fun setupViews() {
        binding.featureFilters.ivFeatureIcon.setImageResource(R.drawable.ic_filter)
        binding.featureFilters.tvFeatureTitle.text = "أكثر من 18 فلتر احترافي"
        binding.featureFilters.tvFeatureDesc.text = "Vintage, Sepia, Cinematic, Lomo وغيرها"

        binding.featureAdjust.ivFeatureIcon.setImageResource(R.drawable.ic_adjust)
        binding.featureAdjust.tvFeatureTitle.text = "أدوات تعديل دقيقة"
        binding.featureAdjust.tvFeatureDesc.text = "السطوع، التباين، التشبع، الحدة والمزيد"

        binding.featureCrop.ivFeatureIcon.setImageResource(R.drawable.ic_crop)
        binding.featureCrop.tvFeatureTitle.text = "قص وتدوير وقلب"
        binding.featureCrop.tvFeatureDesc.text = "تحكم كامل في تكوين الصورة"
    }

    private fun setupClickListeners() {
        binding.btnOpenImage.setOnClickListener {
            checkStoragePermissionAndOpenGallery()
        }
        binding.cardUpload.setOnClickListener {
            checkStoragePermissionAndOpenGallery()
        }
        binding.btnOpenCamera.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
    }

    private fun handleExternalIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (sharedUri != null) {
                    openEditor(sharedUri)
                }
            }
            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data != null) {
                    openEditor(data)
                }
            }
        }
    }

    private fun checkStoragePermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            storagePermissionLauncher.launch(permission)
        }
    }

    private fun checkCameraPermissionAndOpen() {
        val permission = android.Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickImageLauncher.launch(Intent.createChooser(intent, getString(R.string.action_open_image)))
    }

    private fun openCamera() {
        val tempFile = ImageUtils.createTempImageFile(this) ?: run {
            showSnackbar("تعذّر إنشاء ملف الصورة")
            return
        }
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            tempFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            pickImageLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Camera launch failed", e)
            showSnackbar("تعذّر فتح الكاميرا")
        }
    }

    private fun openEditor(uri: Uri) {
        val intent = Intent(this, EditActivity::class.java).apply {
            putExtra(EditActivity.EXTRA_INPUT_URI, uri.toString())
        }
        startActivity(intent)
    }

    private fun showPermissionRationale(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("إذن مطلوب")
            .setMessage(message)
            .setPositiveButton("الإعدادات") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
