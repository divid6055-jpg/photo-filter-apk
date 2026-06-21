package com.photofilter.pro

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.photofilter.pro.databinding.ActivitySplashBinding

/**
 * شاشة البداية الاحترافية - تعرض شعار التطبيق مع animations جذابة.
 * تستمر 1.8 ثانية ثم تنتقل لـ MainActivity.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playAnimations()

        // الانتقال للشاشة الرئيسية بعد 1.8 ثانية
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1800)
    }

    private fun playAnimations() {
        // 1. ظهور تدريجي للشعار مع تكبير
        val logoAnim = AnimationSet(true).apply {
            interpolator = AccelerateDecelerateInterpolator()
            addAnimation(ScaleAnimation(0.5f, 1f, 0.5f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f).apply {
                duration = 800
            })
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 800 })
        }
        binding.logoContainer.startAnimation(logoAnim)

        // 2. توهج خلف الشعار
        val glowAnim = AnimationSet(true).apply {
            interpolator = AccelerateDecelerateInterpolator()
            addAnimation(ScaleAnimation(0.3f, 1.2f, 0.3f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f).apply {
                duration = 1200
                startOffset = 200
            })
            addAnimation(AlphaAnimation(0f, 0.4f).apply {
                duration = 1200
                startOffset = 200
            })
        }
        binding.glowRing.startAnimation(glowAnim)

        // 3. ظهور اسم التطبيق (بعد الشعار)
        val nameAnim = AlphaAnimation(0f, 1f).apply {
            duration = 500
            startOffset = 600
        }
        binding.tvAppName.startAnimation(nameAnim)

        // 4. ظهور الوصف
        val subtitleAnim = AlphaAnimation(0f, 1f).apply {
            duration = 500
            startOffset = 800
        }
        binding.tvSubtitle.startAnimation(subtitleAnim)

        // 5. ظهور مؤشر التحميل
        val progressAnim = AlphaAnimation(0f, 1f).apply {
            duration = 400
            startOffset = 1000
        }
        binding.progress.startAnimation(progressAnim)
        binding.tvLoading.startAnimation(progressAnim)

        // 6. ظهور العناصر الزخرفية
        val decoAnim = AlphaAnimation(0f, 1f).apply {
            duration = 1000
            startOffset = 200
        }
        binding.decorationCircle1.startAnimation(decoAnim)
        binding.decorationCircle2.startAnimation(decoAnim)
        binding.decorationCircle3.startAnimation(decoAnim)

        // 7. الإصدار في النهاية
        val versionAnim = AlphaAnimation(0f, 1f).apply {
            duration = 400
            startOffset = 1400
        }
        binding.tvVersion.startAnimation(versionAnim)
    }
}
