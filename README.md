# 🎬 LuminaPlayer — راهنمای ساخت APK با GitHub Actions

## ✅ مرحله ۱: ریپازیتوری بساز

```
۱. GitHub.com رو باز کن
۲. روی آیکون + بالا سمت راست بزن
۳. New Repository بزن
۴. نام: LuminaPlayer
۵. Description: Video Player App
۶. Public بذار
۷. Create Repository بزن
```

---

## ✅ مرحله ۲: فایل‌ها رو آپلود کن

```
۱. روی "uploading an existing file" کلیک کن
۲. فایل‌ها رو Drag & Drop کن:
   - .github/workflows/build.yml
   - gradle/libs.versions.toml
   - settings.gradle.kts
   - build.gradle.kts
   - gradle.properties
   - app/build.gradle.kts
   - app/src/main/AndroidManifest.xml
   - app/src/main/java/com/example/luminaplayer/*.kt
۳. Commit Changes بزن
```

---

## ✅ مرحله ۳: GitHub Actions خودکار شروع میکنه

```
۱. تب Actions رو بزن (بالا سمت راست)
۲. "Build Android APK" رو میبینی
۳. اگه خودکار شروع نشد:
   - Run workflow بزن
   - دوباره Run workflow بزن
```

---

## ✅ مرحله ۴: APK رو دانلود کن

```
۱. صبر کن ۵-۱۰ دقیقه (تا سبز بشه ✅)
۲. روی آخرین Build کلیک کن
۳. Artifacts بخش پایین رو ببین
۴. "LuminaPlayer-APK" رو دانلود کن
۵. از فایل ZIP خارجش کن
۶. APK آماده‌ست! 🎉
```

---

## ⚠️ اگه ارور داد

```
۱. تب Actions → روی Build قرمز کلیک کن
۲. Log رو بخون
۳. اگه مشکل Gradle بود:
   - Settings → Actions → General
   - Workflow permissions → Read and write permissions → Save
```

---

## 📱 نصب APK روی گوشی

```
۱. APK رو به گوشی منتقل کن (USB یا WhatsApp)
۲. Settings → Security → Unknown sources → ON
۳. APK رو باز کن → Install بزن
۴. LuminaPlayer نصب شد! 🎉
```

---

## 📋 خلاصه

```
۱. GitHub.com → New Repository → LuminaPlayer
۲. فایل‌ها رو آپلود کن
۳. Actions تب → Run workflow بزن
۴. ۵-۱۰ دقیقه صبر کن
۵. APK رو دانلود کن
۶. روی گوشی نصب کن
```

---

**⚡ خلاصه:**
```
GitHub Actions خودکار APK میسازه!
فقط فایل‌ها رو آپلود کن و صبر کن!
```

**🎬 موفق باشی!** 🚀
