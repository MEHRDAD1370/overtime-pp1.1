# راه‌اندازی امضای ثابت APK نسخه ۲.۱۳

## فایل کلید اختصاصی
فایل `overtime-signing-v2.13.jks` جداگانه تحویل داده شده و داخل مخزن GitHub قرار ندهید. فایل رمزها را نیز در مخزن بارگذاری نکنید. کلید را در حداقل دو محل امن و آفلاین نگه دارید؛ گم‌شدن آن یعنی امکان به‌روزرسانی نسخه‌های امضاشده با همین کلید از بین می‌رود.

## چهار GitHub Actions Secrets
در مخزن بروید به **Settings → Secrets and variables → Actions → New repository secret** و این چهار مقدار را اضافه کنید:
- `KEYSTORE_BASE64`: محتوای Base64 فایل `.jks`
- `KEYSTORE_PASSWORD`: رمز keystore
- `KEY_ALIAS`: مقدار `overtime`
- `KEY_PASSWORD`: رمز کلید؛ در این پروژه با رمز keystore یکسان است

برای گرفتن Base64 در Windows PowerShell، در پوشه‌ای که فایل کلید را دانلود کرده‌اید اجرا کنید:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('.\overtime-signing-v2.13.jks')) | Set-Clipboard
```
سپس مقدار Clipboard را در Secret مربوط به `KEYSTORE_BASE64` بچسبانید. رمز را از فایل رمزهای جداگانه بردارید.

پس از تنظیم Secrets، به **Actions → Build Android APK → Run workflow** بروید. خروجی `overtime-panel-debug-apk` را از بخش Artifacts دانلود کنید. Workflow اگر Secrets کامل نباشند عمداً متوقف می‌شود تا APK با کلید موقت امضا نشود.

## هشدار حیاتی درباره نصب روی نسخه قدیمی
این پروژه با کلید اختصاصی تازه امضا می‌شود. اگر APK نصب‌شده قبلی با گواهی دیگری امضا شده باشد، اندروید اجازه نصب این نسخه روی آن را نمی‌دهد. حذف نسخه قبلی ممکن است داده‌های محلی آن را پاک کند؛ **تا وقتی گواهی APK نصب‌شده را با گواهی این کلید مقایسه نکرده‌اید، نسخه قدیمی را حذف نکنید.** کلید خصوصی قبلی از اثرانگشت گواهی قابل بازسازی نیست.

شناسه برنامه: `com.arianjalarang.overtime`  
versionCode: `14`  
versionName: `2.13.0`
