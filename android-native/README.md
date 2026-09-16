# Image to 3D Extrusion — Android Native

هذا المجلد هو النسخة الأصلية لتطبيق Android مكتوبة بلغة **Kotlin** باستخدام **Jetpack Compose**، مستقلة عن نسخة Expo السابقة.

## المزايا الأصلية

- واجهة Android أصلية بملف `MainActivity.kt` وJetpack Compose.
- استيراد الصور عبر Android Storage Access Framework باستخدام `ACTION_OPEN_DOCUMENT` مع دعم JPG وPNG وWebP وحفظ صلاحية القراءة.
- التقاط الصور عبر Android Camera Activity مع طلب إذن وقت التشغيل.
- معالجة `Bitmap` محليًا: luminance، contrast shaping، gradient edge energy، وsmoothing.
- معاينة isometric لخريطة الارتفاعات باستخدام Compose Canvas.
- إنشاء DXF حقيقي بكيانات `3DFACE` ومشاركته عبر Android `FileProvider`.
- عدم رفع الصور إلى أي خدمة خارجية.

## البناء

يتطلب Android Studio أو Android SDK مع JDK 17 وGradle 8.9 أو أحدث متوافق مع Android Gradle Plugin 8.7.3.

```bash
cd android-native
./gradlew assembleDebug
```

سيكون الناتج في:

```text
app/build/outputs/apk/debug/app-debug.apk
```

افتح مجلد `android-native` مباشرة في Android Studio ثم شغّل التطبيق على جهاز Android أو محاكي.
