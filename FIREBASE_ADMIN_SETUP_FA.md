# ورود ادمین کلکین

کد ورود ادمین از Firebase Authentication با ایمیل و رمز استفاده می‌کند. ورود ادمین نشست جداگانه دارد و حساب گوگل بیننده را عوض نمی‌کند. داشتن حساب Authentication به‌تنهایی اجازهٔ مدیریت نمی‌دهد؛ نقش از `admins/<UID>` خوانده می‌شود.

## تنظیم پروژهٔ واقعی

1. در Firebase Console، بخش Authentication → Sign-in method، روش Email/Password را فعال کن.
2. در Authentication → Users → Add user، حساب ادمین را با ایمیل و رمز دلخواه بساز؛ UID آن را کپی کن.
3. در Realtime Database → Data، زیر `admins` یک فرزند با نام همان UID و مقدار boolean `true` بساز. مقدار رشته‌ای `"true"` پذیرفته نیست.
4. در Realtime Database → Rules، محتوای `firebase/database.rules.json` را قرار بده و Publish کن. این فایل در پروژه آماده شده؛ تغییر محلی آن به‌تنهایی Rules سرور را عوض نمی‌کند.
5. در اپ: تنظیمات → ورود مدیر. با ایمیل و رمز همان حساب وارد شو.

فعال‌سازی بیننده همچنان از `users/<UID>/isActive: true` در کنسول انجام می‌شود. کاربر عادی نمی‌تواند خودش را فعال یا ادمین کند. ادمین به اطلاعات اصلی کاتالوگ دسترسی نوشتن دارد؛ کلیدهای موجود فیلم و سریال هنگام ویرایش حفظ می‌شوند. شمارنده‌های `catalog_counters` خودکار ساخته می‌شوند.

حساب ادمین و Rules سرور از این محیط ساخته یا منتشر نشده‌اند؛ دسترسی مدیریتی پروژهٔ Firebase در ابزارهای این گفتگو موجود نیست. ورود واقعی و CRUD باید پس از این تنظیم با حساب خودت بررسی شوند.

## ساخت و ورود گوگل

SDK در `local.properties` تنظیم شده است. Java 21 همراه Android Studio قابل استفاده است. برای ورود گوگل نسخهٔ دیباگ، SHA-1 گواهی debug باید در Project settings → Android app ثبت و تنظیم Google sign-in فعال باشد؛ `google-services.json` باید با همان اپ مطابقت داشته باشد.

امضای release از متغیرهای محیطی `KELKIN_KEYSTORE_PATH`، `KELKIN_KEYSTORE_PASSWORD`، `KELKIN_KEY_ALIAS` و `KELKIN_KEY_PASSWORD` یا فایل محلی `keystore.properties` در ریشهٔ پروژه خوانده می‌شود. متغیرهای محیطی غیرخالی اولویت دارند. در فایل محلی، مقدارهای `storeFile`، `storePassword`، `keyAlias` و `keyPassword` را وارد کنید؛ برای مسیر ویندوز از `/` استفاده کنید. این فایل از Git مستثنا شده است و رمزها نباید ارسال یا منتشر شوند. APK دیباگ با کلید debug امضا می‌شود و ممکن است روی نسخهٔ release موجود قابل نصب به‌صورت update نباشد.

مراجع: [ورود ایمیل و رمز در Android](https://firebase.google.com/docs/auth/android/password-auth)، [شرط‌های امنیت Realtime Database](https://firebase.google.com/docs/database/security/rules-conditions).
