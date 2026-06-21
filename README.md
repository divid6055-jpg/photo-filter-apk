# PhotoFilter Pro - تطبيق تعديل الصور الاحترافي

تطبيق أندرويد احترافي لتعديل الصور عبر مجموعة واسعة من الفلاتر والأدوات المتقدمة. بُني باستخدام Kotlin و Android SDK الحديث مع Material Design 3.

## المميزات الرئيسية

- **أكثر من 15 فلتر احترافي**: Vintage, Black & White, Sepia, Cool, Warm, Vivid, Fade, Noir, Cinematic, Drama, Silver, Lomo, Polaroid, Sunrise, Night, Sketch
- **أدوات تعديل متقدمة**: السطوع، التباين، التشبع، درجة الحرارة، الحدة، الشفافية، اللون الأخضر/الأحمر/الأزرق
- **أدوات تحويل**: القص، التدوير، القلب الأفقي والعمودي
- **معالجة الصور بالأداء العالي**: استخدام RenderScript و ColorMatrix لمعالجة سريعة
- **واجهة Material Design 3** عصرية وسهلة الاستخدام
- **حفظ بدقة عالية** مع إمكانية المشاركة المباشرة

## متطلبات التشغيل

- Android 6.0 (API 23) أو أحدث
- مساحة تخزين كافية للصور

## بناء التطبيق

### المتطلبات
- JDK 17
- Android SDK (compileSdk 34)
- Gradle 8.x

### البناء المحلي
```bash
./gradlew assembleDebug
```

### البناء عبر GitHub Actions
يتم بناء الـ APK تلقائياً عند كل push عبر GitHub Actions workflow. يمكنك تنزيل الـ APK من قسم Releases أو من Artifacts في Actions.

## التقنيات المستخدمة

- **Kotlin** - لغة التطوير الأساسية
- **AndroidX** - مكتبات الدعم الحديثة
- **Material Components** - تصميم Material Design 3
- **RenderScript/ColorMatrix** - معالجة الصور عالية الأداء
- **ViewBinding** - ربط الواجهات بأمان
- **Coroutines** - البرمجة غير المتزامنة

## بنية المشروع

```
app/
├── src/main/
│   ├── java/com/photofilter/pro/
│   │   ├── MainActivity.kt          # الشاشة الرئيسية
│   │   ├── FiltersActivity.kt       # شاشة الفلاتر
│   │   ├── EditActivity.kt          # شاشة أدوات التعديل
│   │   ├── utils/
│   │   │   ├── ImageProcessor.kt    # محرك معالجة الصور
│   │   │   └── FilterPresets.kt     # تعريفات الفلاتر
│   │   └── adapters/
│   │       └── FilterAdapter.kt     # محول قائمة الفلاتر
│   ├── res/
│   │   ├── layout/                  # تخطيطات الواجهة
│   │   ├── drawable/                # الأيقونات والصور
│   │   ├── values/                  # الألوان والنصوص
│   │   └── mipmap/                  # أيقونة التطبيق
│   └── AndroidManifest.xml
├── build.gradle.kts                 # تكوين الوحدة
build.gradle.kts                     # تكوين المشروع
settings.gradle.kts                  # إعدادات Gradle
.github/workflows/build.yml          # سير عمل بناء APK
```

## الترخيص

MIT License - حر للاستخدام والتعديل
