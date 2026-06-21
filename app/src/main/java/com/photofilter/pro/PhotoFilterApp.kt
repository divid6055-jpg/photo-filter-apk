package com.photofilter.pro

import android.app.Application
import com.google.android.material.color.DynamicColors

/**
 * فئة التطبيق الأساسية - تهيئة عامة للتطبيق.
 */
class PhotoFilterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // تطبيق Dynamic Colors على Android 12+ (اختياري - يحترم ثيم العلامة)
        // DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
