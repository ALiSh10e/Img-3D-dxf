# Image to 3D Extrusion

تطبيق Android أصلي مبني بـ Expo SDK 54 لتحويل الصور إلى نموذج بروز ثلاثي الأبعاد قابل للتصدير بصيغة DXF.

## ما تم تطويره

- اختيار صورة من المعرض أو التقاطها بالكاميرا.
- تحسين الصورة محليًا إلى عرض 960px قبل المعالجة لتقليل استهلاك الذاكرة.
- خط معالجة رؤية حاسوبية محلي: luminance sampling، contrast shaping، gradient/Sobel-like edge energy، وsmoothing اختياري.
- توليد شبكة ارتفاعات 3D من الصورة مع معاينة isometric مرئية.
- إعدادات عمق البروز، التباين، وتقوية الحواف مع presets جاهزة.
- تصدير DXF يحتوي على كيانات `3DFACE` قابلة للفتح في AutoCAD وFusion 360 وأدوات CNC التي تدعم DXF.
- مشاركة الملف مباشرة من Android وحفظ سجل آخر ملفات التصدير.
- لا يتم رفع الصور إلى خادم؛ المعالجة الأساسية تتم محليًا على الجهاز.

## التشغيل

```bash
pnpm install
pnpm start
```

لتشغيل Android عبر Expo:

```bash
pnpm android
```

للتحقق:

```bash
pnpm check
npx expo export --platform web --output-dir /tmp/img3d-web-export
```

## ملاحظة هندسية

توليد DXF الحالي يصدّر شبكة relief بصيغة 3DFACE. جودة النتيجة مرتبطة بجودة الصورة والإضاءة، ويمكن رفع الدقة من `buildHeightMap` عند استهداف أجهزة ذات ذاكرة أعلى. تم اختيار المعالجة المحلية لتجنب إرسال صور المستخدمين إلى خدمة خارجية ولتقديم تجربة مستقلة على Android.

## Android Native

يوجد الآن مشروع Android Native مستقل داخل `android-native/` مكتوب بلغة Kotlin وJetpack Compose. هذه النسخة هي المسار الموصى به لبناء APK/AAB حقيقي من Android Studio، وتضم معالجة Bitmap محلية، التقاط الكاميرا، اختيار الصور، معاينة Compose، وتصدير DXF عبر FileProvider.

```bash
cd android-native
./gradlew assembleDebug
```

افتح مجلد `android-native` في Android Studio إذا لم يكن Android SDK متاحًا في بيئة التشغيل الحالية.
