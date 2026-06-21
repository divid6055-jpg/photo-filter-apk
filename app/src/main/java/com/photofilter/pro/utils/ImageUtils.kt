package com.photofilter.pro.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * أدوات تحميل وحفظ الصور.
 * تتعامل مع Uri، قراءة EXIF orientation، والحفظ في معرض الجهاز.
 */
object ImageUtils {

    private const val TAG = "ImageUtils"

    /**
     * يحمّل صورة من Uri مع معالجة EXIF orientation وتصغير الحجم إن لزم.
     * @param contentResolver
     * @param uri uri الصورة
     * @param maxDim الحد الأقصى للبُعد (0 = بدون حد)
     */
    fun loadBitmap(contentResolver: ContentResolver, uri: Uri, maxDim: Int = 4096): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from $uri")
                return null
            }

            // قراءة EXIF orientation
            val orientation = getExifOrientation(contentResolver, uri)
            val oriented = applyExifOrientation(bitmap, orientation)

            // تصغير الحجم إن لزم
            val sized = if (maxDim > 0) ImageProcessor.constrainSize(oriented, maxDim) else oriented
            sized
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap", e)
            null
        }
    }

    /**
     * يقرأ زاوية EXIF من Uri.
     */
    private fun getExifOrientation(contentResolver: ContentResolver, uri: Uri): Int {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return ExifInterface.ORIENTATION_NORMAL
            val exif = ExifInterface(inputStream)
            inputStream.close()
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * يطبّق دوران EXIF على الصورة.
     */
    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * يحفظ الصورة في معرض الجهاز (Pictures/PhotoFilterPro).
     * يستخدم MediaStore على Android 10+ وFile التقليدي قبلها.
     * @return Uri الصورة المحفوظة أو null عند الفشل.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, quality: Int = 95): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "PhotoFilter_$timestamp.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, bitmap, filename, quality)
        } else {
            saveViaFile(context, bitmap, filename, quality)
        }
    }

    private fun saveViaMediaStore(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        quality: Int
    ): Uri? {
        return try {
            val resolver = context.contentResolver
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoFilterPro")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return null
            val stream: OutputStream? = resolver.openOutputStream(uri)
            if (stream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.close()
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                resolver.delete(uri, null, null)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save via MediaStore", e)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveViaFile(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        quality: Int
    ): Uri? {
        return try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "PhotoFilterPro").apply { if (!exists()) mkdirs() }
            val file = File(appDir, filename)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }
            // إعلام المعرض
            val intent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(file)
            context.sendBroadcast(intent)
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save via file", e)
            null
        }
    }

    /**
     * ينسخ Uri إلى ملف مؤقت (مفيد للـ Camera).
     */
    fun copyUriToTempFile(context: Context, uri: Uri, prefix: String = "img"): File? {
        return try {
            val tempFile = File.createTempFile(prefix, ".jpg", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy uri to temp file", e)
            null
        }
    }

    /**
     * ينشئ ملف صورة مؤقت لاستخدامه مع الكاميرا.
     */
    fun createTempImageFile(context: Context): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            File.createTempFile("JPEG_${timestamp}_", ".jpg", context.cacheDir)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create temp image file", e)
            null
        }
    }

    /**
     * يحسب عامل التصغير المطلوب لصورة بدلالة maxDim.
     */
    fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val longest = maxOf(width, height)
        if (longest > maxDim) {
            while (longest / inSampleSize > maxDim) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
