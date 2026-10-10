# بررسی کد کلکین — ۷ اکتبر ۲۰۲۶

> این گزارش مربوط به وضعیت اولیهٔ پروژه است. برای وضعیت اصلاحات و اعتبارسنجی جدید، `KELKIN_FIXES_FA.md` را بخوانید؛ شماره‌خط‌ها در گزارش اولیه بعد از اصلاحات تغییر کرده‌اند.

این گزارش بر اساس بررسی سورس Kotlin، layoutها، navigation، SQLite، Firebase، تنظیمات شبکه و Gradle تهیه شده است. اپ روی تلویزیون یا شبیه‌ساز اجرا نشده؛ موارد «ریسک» باید روی دستگاه تأیید شوند. «قطعی» یعنی شواهد در کد موجود است، نه اینکه همهٔ سناریوها روی دستگاه بازتولید شده‌اند. تغییر اصلاحی در سورس انجام نشده است.

P1: اصلاح زودهنگام برای امنیت، خرابی مسیر اصلی یا کرش. P2: مشکل قابل توجه کارکرد یا تجربهٔ کاربر. P3: اشکال فرعی یا نگهداری.

## محدودیت اعتبارسنجی

اجرای `gradlew.bat :app:lintDebug --offline` قبل از تحلیل Gradle با خطای `JAVA_HOME is not set` متوقف شد. Java 21 همراه Android Studio موجود است اما Java پروژه تنظیم نشده؛ نسخهٔ wrapper پروژه 9.3.1 در cache موجود نیست (9.4.1 موجود است). Android SDK در مسیر معمول بررسی‌شده پیدا نشد؛ adb در PATH نیست. `google-services.json`، Rules فایربیس، فایل تست و APK در این مجموعه پیدا نشد. بنابراین build، Lint، Rules سرور، لاگ runtime، صحت لینک‌های واقعی و ارزیابی بصری تأیید نشده‌اند. فایل تنظیم Firebase باید از پیکربندی معتبر توسعه‌دهنده تأمین شود؛ نبودش در این نسخهٔ پروژه به معنی خرابی نسخهٔ نصب‌شده نیست.

## یافته‌ها

### K-01 — رمز ادمین داخل APK است

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SettingsFragment.kt:108)

ورود به پنل فقط با مقایسهٔ نام و رمز ثابت در کلاینت انجام می‌شود و نقش ادمین از سرور بررسی نمی‌شود. امنیت نوشتن روی Firebase را بدون Rules نمی‌توان تأیید کرد.

**اصلاح پیشنهادی:** نقش ادمین را با Firebase Auth و Rules یا backend اعمال کنید؛ مخفی کردن پنل و تغییر رمز داخل APK کافی نیست.

### K-02 — کلید امضای انتشار همراه رمز در پروژه است

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/build.gradle.kts:38)

فایل app/kelkin.jks موجود است و رمزهای امضا در فایل Gradle نوشته شده‌اند. هر کس این مجموعه را داشته باشد به مواد لازم برای امضا دسترسی دارد.

**اصلاح پیشنهادی:** رمزها را از سورس خارج و keystore را جدا نگهداری کنید؛ اگر قبلاً در اختیار دیگران بوده، مسیر تعویض کلید انتشار را بررسی کنید.

### K-03 — کرش مسیر لوگوی جایگزین روی Android 6 و 7

**P1 · قطعی با محرک** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/SplashActivity.kt:204)

minSdk برابر 23 است ولی مسیر fallback از resources.getFont که API 26 است استفاده می‌کند. @RequiresApi دستگاه قدیمی را در زمان اجرا محافظت نمی‌کند.

**اصلاح پیشنهادی:** از ResourcesCompat.getFont استفاده کنید و خطای پخش انیمیشن را روی API 23 و 25 تست کنید.

### K-04 — شناسهٔ فیلم و سریال در کش و لیست شخصی تفکیک نشده

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/ViewModel/HomeViewModel.kt:36)

کش جزئیات، نشان‌شده‌ها و ادامه تماشا همگی فقط tmdb_id را کلید می‌کنند. دو عنوان از نوع movie و tv با شناسهٔ عددی برابر به هم برخورد می‌کنند؛ شناسهٔ خالی نیز همهٔ عنوان‌های بدون TMDB را یکی می‌کند.

**اصلاح پیشنهادی:** شناسهٔ ترکیبی نوع محتوا و شناسهٔ پایدار داخلی را در همهٔ این مسیرها استفاده کنید.

### K-05 — بازشدن سریال اشتباه از صفحهٔ خانه

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/HomeFragment.kt:62)

نوع محتوا با تطبیق tmdb_id یا id محلی حدس زده می‌شود. شناسه‌های فیلم و سریال مستقل‌اند؛ با هم‌پوشانی، حتی کارت فیلم به جزئیات سریال می‌رود.

**اصلاح پیشنهادی:** نوع محتوا را صریح ذخیره و ناوبری را بر اساس آن انجام دهید.

### K-06 — آدرس پوستر آرشیو فیلم خراب می‌شود

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Adapter/MovieGridAdapter.kt:72)

MoviesFragment آدرس کامل https://image.tmdb.org/t/p/w500/... می‌سازد؛ Adapter دوباره baseTMDB را جلو آن می‌گذارد. نتیجه شبیه .../w500/https://image.tmdb.org/... است.

**اصلاح پیشنهادی:** نرمال‌سازی URL را یک‌بار انجام دهید و لینک کامل را بدون پیشوند مجدد بپذیرید.

### K-07 — آدرس پوستر آرشیو سریال هم خراب می‌شود

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Adapter/SeriesGridAdapter.kt:69)

SeriesFragment مسیر را به لینک کامل تبدیل می‌کند و SeriesGridAdapter دوباره دامنه و مسیر را به آن می‌چسباند؛ پوسترها به حالت خطا می‌روند.

**اصلاح پیشنهادی:** همان سازندهٔ مشترک URL پوستر را برای فیلم، سریال، خانه و جستجو استفاده کنید.

### K-08 — Retry کانال زنده به URL اولین پخش برمی‌گردد

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:343)

Listener فقط هنگام ساخت اولین ExoPlayer اضافه می‌شود و url همان فراخوانی را capture می‌کند. پس از رفتن از کانال A به B، خطای B باعث تلاش مجدد برای A می‌شود؛ Retry معوق A هم می‌تواند پخش B را قطع کند.

**اصلاح پیشنهادی:** URL جاری را نگهداری و هر Retry قبلی را هنگام تعویض کانال یا سورس لغو کنید.

### K-09 — به‌روزرسانی هدرها کانال اولیه را دوباره پخش می‌کند

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:103)

currentVideoUrl فقط از Intent مقدار می‌گیرد و با انتخاب کانال یا سورس تغییر نمی‌کند. انتشار دوبارهٔ streamHeaders پخش را به لینک شروع Activity برمی‌گرداند.

**اصلاح پیشنهادی:** URL و کانال و سورس جاری را پیش از هر پخش با هم به‌روز کنید.

### K-10 — سورس ثبت‌شده در پنل به پلیر نمی‌رسد

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/ChannelListFragment.kt:177)

پنل در فیلد source رشته ذخیره می‌کند، اما HomeViewModel در سینک و fetchChannels فقط sources را می‌خواند. ویرایش هم ساختار موجود را با toString به متنی شبیه {name=...} تبدیل می‌کند، نه JSON معتبر.

**اصلاح پیشنهادی:** یک schema واحد برای sources تعیین کنید و داده‌های قبلی و فرم و پلیر را به آن منتقل کنید.

### K-11 — سریال با پاسخ دیرهنگام شبکه می‌تواند کرش کند

**P1 · قطعی با محرک** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SeriesDetailsFragment.kt:203)

درخواست در lifecycleScope خود Fragment اجرا می‌شود و isAdded را چک می‌کند؛ view می‌تواند نابود شده باشد ولی Fragment هنوز added باشد. updateUI آن‌وقت binding!! را می‌خواند.

**اصلاح پیشنهادی:** از viewLifecycleOwner.lifecycleScope استفاده کنید و اعتبار view را هنگام اعمال پاسخ کنترل کنید.

### K-12 — پخش زنده بدون دریافت هدرها راه خروج از انتظار ندارد

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:280)

اگر هدرها null باشند پلیر ساخته نمی‌شود. onCancelled در syncStreamHeaders خالی است و stream_config محلی هم خوانده نمی‌شود؛ کاربر روی دریافت تنظیمات می‌ماند.

**اصلاح پیشنهادی:** هدرهای کش‌شده یا پیش‌فرض، timeout، پیام خطا و دکمهٔ تلاش مجدد اضافه کنید.

### K-13 — User-Agent تنظیم‌شده از سرور استفاده نمی‌شود

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:307)

user_agent از Firebase خوانده می‌شود ولی درخواست‌ها همواره USER_AGENT_LIVE ثابت را می‌فرستند. اصلاح تنظیمات سرور اثری روی این هدر ندارد.

**اصلاح پیشنهادی:** مقدار معتبر پیکربندی را با fallback مشخص استفاده کنید.

### K-14 — تمام لینک‌های زنده به اجبار HLS فرض می‌شوند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:314)

برای هر لینک فقط HlsMediaSource ساخته می‌شود. کانال با MP4 یا DASH وارد مسیر نادرست می‌شود؛ پشتیبانی از آن‌ها در این مسیر وجود ندارد.

**اصلاح پیشنهادی:** نوع منبع را در مدل ذخیره کنید یا از انتخاب مناسب MediaSource بر اساس نوع استفاده کنید.

### K-15 — خطای پخش مستقیم مدیریت نشده

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:239)

Listener پلیر مستقیم فقط STATE_READY را می‌بیند؛ برای URL خراب، 403، شبکهٔ قطع یا فرمت نامعتبر پیام و retry قابل استفاده وجود ندارد.

**اصلاح پیشنهادی:** onPlayerError و وضعیت بارگذاری و retry و بازگشت واضح اضافه کنید.

### K-16 — تلاش مجدد زنده بی‌پایان و بدون کنترل کاربر است

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:343)

همهٔ خطاها هر سه ثانیه با همان لینک تکرار می‌شوند؛ سقف تلاش، backoff، تشخیص نوع خطا یا رفتن به سورس جایگزین وجود ندارد.

**اصلاح پیشنهادی:** Retry را محدود و قابل لغو کنید؛ خطاهای دائمی را به انتخاب سورس و اقدام کاربر تبدیل کنید.

### K-17 — پخش وب پیام خطای قابل استفاده ندارد

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:404)

WebViewClient فقط onPageFinished را پیاده‌سازی می‌کند. خطای شبکه یا HTTP و بارگذاری ناموفق ویدیو به وضعیت توضیح‌دار و retry تبدیل نمی‌شود.

**اصلاح پیشنهادی:** خطاهای صفحه و وضعیت آماده‌شدن ویدیو را به UI پلیر وصل کنید.

### K-18 — ویدیوی وب ممکن است در پس‌زمینه ادامه پیدا کند

**P2 · ریسک اجرایی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:136)

onPause و onStop فقط exoPlayer را متوقف می‌کنند؛ ویدیوی HTML و lifecycle خود WebView کنترل نمی‌شود.

**اصلاح پیشنهادی:** ویدیوی HTML را صریح pause کنید و lifecycle وب را مدیریت کنید؛ با Home و خاموش کردن صفحه روی TV تست کنید.

### K-19 — Play/Pause در وب کلیک روی مکان موس است

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:568)

دکمهٔ MEDIA_PLAY_PAUSE به video.play/pause وصل نیست؛ هر جایی نشانگر باشد همان جا کلیک می‌کند. می‌تواند روی تنظیمات، تبلیغ یا فضای خالی عمل کند.

**اصلاح پیشنهادی:** پخش و توقف را به عنصر ویدیو یا API پلتفرم وصل کنید و کلیدهای PLAY و PAUSE مستقل را هم پشتیبانی کنید.

### K-20 — مختصات کلیک موس نسبت به WebView اصلاح نشده

**P2 · قطعی با شرط چیدمان** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/VirtualMouseView.kt:113)

getLocationOnScreen مختصات صفحه می‌دهد ولی MotionEvent به WebView محلی ارسال می‌شود. اگر مبدأ WebView روی صفحه صفر نباشد، کلیک به جای غلط می‌خورد.

**اصلاح پیشنهادی:** موقعیت WebView روی صفحه را کم کنید؛ نشانگر و نقطهٔ کلیک را در دستگاه مختصات مشترک نگه دارید.

### K-21 — موس مجازی به رزولوشن وابسته است

**P2 · ریسک اجرایی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/VirtualMouseView.kt:22)

شروع روی 640,360 و سرعت 30 پیکسل ثابت است و پس از اندازه‌گیری یا تغییر ابعاد مرکزسازی نمی‌شود. سرعت حرکت و نقطهٔ شروع روی 720p، 1080p و 4K فرق می‌کند.

**اصلاح پیشنهادی:** موقعیت اولیه را از ابعاد view بگیرید و حرکت را با مقیاس مناسب و نگه‌داشتن کلید تنظیم کنید.

### K-22 — بازگشت به پلیر، Pause قبلی کاربر را نادیده می‌گیرد

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:681)

onResume بدون بررسی وضعیت قبلی همیشه play می‌زند. اگر کاربر Pause کرده و برنامه‌ای دیگر یا پنجرهٔ سیستم را باز کند، هنگام برگشت ویدیو خودکار راه می‌افتد.

**اصلاح پیشنهادی:** وضعیت playWhenReady قبلی را حفظ و فقط در صورت پخش بودن قبلی resume کنید.

### K-23 — پیشرفت تماشا فقط با Back ذخیره می‌شود

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:541)

ذخیرهٔ اصلی در مسیر Back است؛ Home، توقف Activity و بسته‌شدن پردازش checkpoint ندارند. بخشی از تماشای کاربر از دست می‌رود.

**اصلاح پیشنهادی:** Checkpoint دوره‌ای و ذخیره در زمان توقف را اضافه کنید؛ برای وب زمان ویدیو را نیز دوره‌ای بخوانید.

### K-24 — ادامهٔ سریال قسمت و فصل را حفظ نمی‌کند

**P1 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SeriesDetailsFragment.kt:193)

پلیر JSON سریال را با نام movie_json دریافت می‌کند و بعد آن را Movie می‌خواند. فصل و قسمت و URL قسمت ذخیره نمی‌شود، episodeTitle استفاده نمی‌شود و start_pos همیشه صفر است.

**اصلاح پیشنهادی:** مدل مستقل سابقهٔ قسمت با seriesId، seasonId، episodeId، URL و position بسازید و Resume را به همان قسمت وصل کنید.

### K-25 — دکمهٔ ادامهٔ فیلم پس از برگشت به‌روز نمی‌شود

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/MovieDetailFragment.kt:95)

savedProgress و listener دکمه فقط در onViewCreated تنظیم می‌شوند. ActivityResult سابقه را عوض می‌کند اما دکمه را بازسازی نمی‌کند؛ ممکن است غایب بماند یا زمان قدیمی را باز کند.

**اصلاح پیشنهادی:** continueWatchingList را observe کنید و هنگام کلیک موقعیت جاری را بخوانید.

### K-26 — فیلم تمام‌شده از ادامه تماشا حذف نمی‌شود

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:646)

هر موقعیت بالاتر از حد ذخیره در لیست می‌ماند؛ STATE_ENDED یا نزدیکی پایان به حذف عنوان وصل نیست.

**اصلاح پیشنهادی:** قاعدهٔ پایان تماشا تعریف کنید و عنوان پایان‌یافته را حذف یا completed کنید.

### K-27 — نشان‌شده‌های بعد از ده مورد قابل دسترسی نیستند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/HomeFragment.kt:226)

لیست ذخیره می‌شود اما صفحهٔ خانه فقط take(10) نشان می‌دهد و صفحه یا دکمهٔ نمایش همه وجود ندارد. نشان‌شده‌های قدیمی از دسترس UI خارج می‌شوند.

**اصلاح پیشنهادی:** صفحهٔ همهٔ نشان‌شده‌ها و همهٔ ادامه تماشا را اضافه کنید یا محدودیت نمایش را بردارید.

### K-28 — شناسهٔ واقعی فیلم در SQLite حفظ نمی‌شود

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/ViewModel/HomeViewModel.kt:198)

سینک فیلم جدول را پاک می‌کند و بدون فیلد id درج می‌کند؛ SQLite شناسهٔ تازه می‌دهد. مرتب‌سازی و تشخیص latest بر اساس این id، ترتیب درج snapshot را نمایش می‌دهد، نه id واقعی محتوا.

**اصلاح پیشنهادی:** شناسهٔ پایدار Firebase و زمان انتشار را صریح ذخیره کنید.

### K-29 — پوسترهای دادهٔ اصلی در سینک حذف می‌شوند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/ViewModel/HomeViewModel.kt:199)

در درج فیلم و سریال posterUrl کپی نمی‌شود؛ جدول قبلی هم پاک می‌شود. پوستر مستقل از TMDB حفظ نمی‌شود و پس از restart یا قطع TMDB کارت‌ها تصویر ندارند.

**اصلاح پیشنهادی:** تمام فیلدهای مدل را در سینک حفظ و metadata لازم را کش کنید.

### K-30 — کش محلی کانال‌ها در شروع بارگذاری نمی‌شود

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/ViewModel/HomeViewModel.kt:54)

channels در SQLite نوشته می‌شود ولی init آن را از SQLite نمی‌خواند و از emptyList شروع می‌کند. با شبکهٔ قطع یا Firebase در دسترس نبودن، دادهٔ قبلی نمایش داده نمی‌شود.

**اصلاح پیشنهادی:** loadChannelsFromLocalDatabase اضافه کنید و جداگانه وضعیت refresh را نشان دهید.

### K-31 — ارتقای دیتابیس همهٔ جدول‌ها را پاک می‌کند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/utils/DatabaseHelper.kt:32)

onUpgrade همهٔ جدول‌ها از جمله تنظیمات stream را DROP می‌کند و از نو می‌سازد. بعد از ارتقا بدون شبکه آرشیو و تنظیمات قبلی در دسترس نیست.

**اصلاح پیشنهادی:** Migration نسخه‌به‌نسخه و حفظ داده‌های موجود پیاده کنید.

### K-32 — Listenerهای Firebase پس از پایان عمرشان جدا نمی‌شوند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/ViewModel/HomeViewModel.kt:192)

Listenerها به صورت anonymous ثبت می‌شوند، handle آن‌ها نگهداری نمی‌شود و onCleared/removeEventListener وجود ندارد. Splash، Main و هر Player ViewModel جدا دارند؛ listener قدیمی می‌تواند باقی بماند.

**اصلاح پیشنهادی:** Reference و Listener را نگهداری و در onCleared جدا کنید؛ listenerهای پنل را نیز با عمر view هماهنگ کنید.

### K-33 — هر پلیر کل آرشیو را دوباره سینک می‌کند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:76)

HomeViewModel پلیر تمام سینک‌های فیلم، سریال، دسته و ... را راه می‌اندازد؛ syncStreamHeaders هم در init و دوباره در Activity ثبت می‌شود.

**اصلاح پیشنهادی:** Repository مشترک بسازید و ViewModel پلیر را محدود به داده‌های مورد نیازش کنید.

### K-34 — کش جزئیات از چند coroutine بدون هماهنگی تغییر می‌کند

**P2 · ریسک هم‌زمانی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/ViewModel/HomeViewModel.kt:394)

mutableMap مشترک از Dispatchers.IO تغییر می‌کند و همان شیء mutable با postValue به UI می‌رود؛ UI هم آن را می‌خواند. برای عملیات هم‌زمان ایمنی یا snapshot ثابت ندارید.

**اصلاح پیشنهادی:** کش را در یک context مالک نگه دارید یا هماهنگ کنید و نسخهٔ immutable منتشر کنید.

### K-35 — درخواست‌های جزئیات بسیار بیشتر از نیاز UI هستند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/HomeFragment.kt:128)

خانه برای تمام فیلم‌ها و سریال‌ها metadata درخواست می‌کند با اینکه فقط چند کارت نشان می‌دهد. در SeriesFragment هر پاسخ applyFilters را تکرار می‌کند و درخواست‌های هنوز نرسیده دوباره ایجاد می‌شوند؛ in-flight dedup ندارید.

**اصلاح پیشنهادی:** فقط آیتم‌های مورد نیاز را hydrate کنید، درخواست در حال اجرا را deduplicate و تعداد هم‌زمان را محدود کنید.

### K-36 — جستجو می‌تواند نتیجهٔ قدیمی را روی نتیجهٔ جدید بنویسد

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SearchFragment.kt:65)

هر تغییر متن coroutine تازه می‌سازد و قبلی لغو نمی‌شود؛ درخواست metadata برای نتایج هم پشت سر هم است. حتی پس از خالی کردن متن، کار قبلی می‌تواند کارت‌ها را دوباره برگرداند.

**اصلاح پیشنهادی:** debounce و cancel قبلی یا collectLatest اضافه کنید و ابتدا نتایج محلی را فوراً نشان دهید.

### K-37 — جستجوی SQLite روی thread اصلی اجرا می‌شود

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SearchFragment.kt:71)

searchMovies پیش از lifecycleScope.launch و در callback تایپ فراخوانی می‌شود. برای هر حرف کل جستجوی نام و توضیح روی UI اجرا می‌شود و در آرشیو بزرگ ریموت و تایپ کند می‌شوند.

**اصلاح پیشنهادی:** جستجو را به Dispatchers.IO منتقل و debounce کنید.

### K-38 — پوستر نتیجهٔ جستجو مسیر خام است

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SearchFragment.kt:81)

جستجو poster_path شبیه /abc.jpg را به MovieAdapter می‌دهد؛ آن Adapter مستقیماً Glide.load(finalUrl) می‌زند و دامنهٔ TMDB اضافه نمی‌کند.

**اصلاح پیشنهادی:** URL نهایی را با سازندهٔ مشترک تصاویر تهیه کنید.

### K-39 — زبان و کیفیت نتیجهٔ جستجو اشتباه خوانده می‌شوند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/utils/DatabaseHelper.kt:161)

برای language و quality از tmdbIdIndex استفاده شده، نه languageIndex و qualityIndex. کارت انتخاب‌شده metadata غلط دارد و جزئیات می‌تواند زبان و کیفیت را پنهان کند.

**اصلاح پیشنهادی:** دو index صحیح را استفاده کنید و یک ردیف با مقادیر متفاوت را بررسی کنید.

### K-40 — جستجوی آرشیو فقط فیلم را می‌گردد

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/utils/DatabaseHelper.kt:135)

جستجو از جدول movies می‌خواند و نتایج تنها مسیر جزئیات فیلم دارند؛ سریال و کانال پیدا نمی‌شوند.

**اصلاح پیشنهادی:** دامنهٔ جستجو را در UI روشن کنید یا نتایج دارای نوع برای فیلم، سریال و کانال بسازید.

### K-41 — کیبورد داخلی فاصله، عدد و حروف انگلیسی ندارد

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SearchFragment.kt:101)

فقط حروف فارسی و Backspace ساخته می‌شوند؛ addSpecialKey هیچ‌جا فراخوانی نمی‌شود. عنوان چندکلمه‌ای یا دارای عدد و انگلیسی را با کیبورد خود اپ نمی‌توان کامل نوشت. کد ردیف‌های ۱۲تایی هم با columnCount=10 سازگار نیست.

**اصلاح پیشنهادی:** Space، پاک‌کردن کامل، اعداد و تغییر زبان اضافه کنید و تعداد ستون‌ها و مسیر فوکوس را هماهنگ کنید.

### K-42 — بررسی فعال‌بودن حساب بعد از timeout اسپلش گیر می‌کند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/SplashActivity.kt:230)

isNavigated پیش از پاسخ checkUserActivationStatus true می‌شود؛ timeoutهای موجود دیگر عمل نمی‌کنند و برای بررسی حساب timeout مستقلی نیست. onCancelled نیز خطای شبکه را همان غیرفعال‌بودن حساب گزارش می‌کند.

**اصلاح پیشنهادی:** برای تأیید حساب timeout و وضعیت خطای جدا با retry بسازید؛ خطای شبکه را inactive تلقی نکنید.

### K-43 — مدیریت حساب و بازبینی فعال‌سازی ناقص است

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/LoginActivity.kt:78)

خروج یا تعویض حساب در اپ وجود ندارد. برای حساب منتظر فعال‌سازی هم refresh مستقل ندارید؛ کاربر باید دوباره مسیر ورود را برود. Main وضعیت فعال‌سازی را دوباره بررسی نمی‌کند.

**اصلاح پیشنهادی:** خروج و تعویض حساب و بررسی دوبارهٔ وضعیت را اضافه کنید و سیاست لغو دسترسی را در سرور نیز اجرا کنید.

### K-44 — انتخاب فصل با رسیدن metadata از نو ریست می‌شود

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SeriesDetailsFragment.kt:230)

بعد از دریافت backdrop، setupSeasonsButtonsAndEpisodes دوباره اجرا می‌شود؛ فصل اول انتخاب، Adapter تعویض و focus با delay به اولین دکمه برگردانده می‌شود. انتخاب کاربر قبل از رسیدن شبکه از دست می‌رود.

**اصلاح پیشنهادی:** فصل انتخاب‌شده و آیتم focused را state پایدار کنید؛ backdrop را بدون بازسازی navigation عوض کنید.

### K-45 — callback تأخیری تنظیمات به binding نابودشده دسترسی دارد

**P2 · قطعی با محرک** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SettingsFragment.kt:24)

Runnable بعد از ۳۰۰ms binding!! می‌خواند و در onDestroyView لغو نمی‌شود. خروج سریع از صفحه می‌تواند آن را پس از null شدن binding اجرا کند.

**اصلاح پیشنهادی:** callback را لغو کنید یا از lifecycleScope مخصوص view و binding معتبر استفاده کنید.

### K-46 — جابجایی منو history تکراری می‌سازد

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/MainActivity.kt:75)

تغییر بخش‌ها با navigate ساده انجام می‌شود؛ انتخاب Home نیز Home تازه روی stack می‌گذارد. Back از Home دیالوگ خروج باز می‌کند و مسیر history با انتظار کاربر هماهنگ نیست.

**اصلاح پیشنهادی:** برای بخش‌های اصلی سیاست popUpTo/saveState/restoreState و بازگشت مشخص تعریف کنید.

### K-47 — ورود به سایدبار فوکوس را به آیتم جاری می‌پراند

**P2 · ریسک ریموت** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/utils/SidebarComponent.kt:67)

هر بار بازشدن سایدبار selectedItem.requestFocus اجرا می‌شود؛ ورود روی یک آیتم دیگر ممکن است به آیتم بخش جاری منتقل شود. نمایش focus برای غیرآیتم پیش‌فرض تا پایان delay نیز suppressed است.

**اصلاح پیشنهادی:** فوکوس ورودی کاربر را حفظ کنید و رفتار عبور XML/Compose را روی D-pad تست کنید.

### K-48 — قفل لبهٔ ردیف‌ها بر اساس آیتم visible است

**P2 · ریسک ریموت** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/HomeFragment.kt:85)

قفل چپ و راست با firstVisible/lastVisible تعیین می‌شود، نه position آیتم دارای فوکوس؛ در ردیف کوتاه یا نمایش همهٔ کارت‌ها می‌تواند کلیدهای لازم برای خروج یا حرکت را ببلعد. رفتار وصول KeyEvent به parent باید روی دستگاه تست شود.

**اصلاح پیشنهادی:** مرزها را از آیتم focused و جهت واقعی RTL محاسبه کنید و راه رفتن به منو را صریح نگه دارید.

### K-49 — فصل انتخاب‌شده نشانهٔ پایدار ندارد

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SeriesDetailsFragment.kt:152)

دکمه‌ها فقط با کلیک لیست قسمت را تغییر می‌دهند؛ selected/checked یا رنگ پایدار برای فصل جاری تنظیم نمی‌شود. بعد از خروج فوکوس کاربر نمی‌بیند کدام فصل باز است.

**اصلاح پیشنهادی:** حالت انتخاب فصل را جدا از فوکوس نمایش دهید.

### K-50 — کنترل‌های ظاهراً قابل استفاده بدون عمل وجود دارند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/TvFragment.kt:58)

دکمهٔ علاقه‌مندی کانال callback خالی دارد. کارت بازیگر نیز clickable و focusable است اما CastAdapter هیچ click action تعریف نمی‌کند؛ کاربر OK می‌زند و اتفاقی نمی‌افتد.

**اصلاح پیشنهادی:** امکان را کامل کنید یا affordance و فوکوس آن را تا پیاده‌سازی حذف کنید.

### K-51 — پاک‌کردن کش روی UI و بدون تأیید نتیجه انجام می‌شود

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SettingsFragment.kt:75)

cacheDir.deleteRecursively روی thread اصلی است و نتیجهٔ Boolean آن بررسی نمی‌شود؛ پیام موفقیت حتی با شکست حذف نمایش داده می‌شود.

**اصلاح پیشنهادی:** حذف را در IO و با هماهنگی کش تصویر انجام دهید و نتیجهٔ واقعی را گزارش کنید.

### K-52 — آرشیو و جستجو حالت خالی و خطای شبکهٔ روشن ندارند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/MoviesFragment.kt:140)

بارگذاری محلی emptyList به‌عنوان پایان loading پذیرفته می‌شود؛ سپس خطای Firebase فقط log می‌شود. UI تفاوت هنوز بارگذاری نشده، دستهٔ خالی، نتیجهٔ صفر و قطع اینترنت را توضیح نمی‌دهد.

**اصلاح پیشنهادی:** stateهای loading/content/empty/error و retry مستقل از لیست داده اضافه کنید.

### K-53 — ساخت شناسه در پنل اتمیک نیست

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/MovieListFragment.kt:294)

شناسهٔ بعدی با خواندن بیشترین id و سپس نوشتن ساخته می‌شود؛ فیلم و کانال و سریال transaction تخصیص ندارند. دو ادمین یا دو ثبت نزدیک می‌توانند شناسه یا کلید یکسان تولید و داده را overwrite کنند.

**اصلاح پیشنهادی:** از push key و تخصیص اتمیک شناسه یا backend استفاده کنید.

### K-54 — اعتبارسنجی فرم‌ها و حفظ دادهٔ ویرایش ناقص است

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/MovieListFragment.kt:152)

نام و URL و TMDB و JSON بدون بررسی معتبر ذخیره می‌شوند و dialog قبل از نتیجهٔ عملیات بسته می‌شود. setValue در ویرایش فیلم/سریال کل رکورد را با فیلدهای محدود جایگزین می‌کند و metadata خارج فرم را حذف می‌کند؛ seasons قدیمی هم می‌تواند تغییر هم‌زمان را overwrite کند.

**اصلاح پیشنهادی:** پیش از بستن dialog اعتبارسنجی کنید؛ updateChildren برای فیلدهای تغییرکرده و محافظت تغییر هم‌زمان استفاده کنید.

### K-55 — مدیریت قسمت‌ها برای ریموت ناقص است

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/SeriesListFragment.kt:318)

دکمهٔ حذف قسمت TextView با click listener است ولی focusable تنظیم نشده. ذخیرهٔ قسمت نیز positive button استاندارد است؛ حتی هنگام ورودی ناقص dialog بسته می‌شود و به‌روزرسانی بعد از ذخیره روی dialog بسته‌شده انجام می‌شود.

**اصلاح پیشنهادی:** کنترل focusable با نشانگر فوکوس و تأیید حذف بسازید؛ Save را بعد از show با اعتبارسنجی و بستن در موفقیت مدیریت کنید.

### K-56 — قابلیت زیرنویس با API نادرست تنظیم شده

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:225)

setRendererDisabled اندیس renderer می‌گیرد، اما C.TRACK_TYPE_TEXT نوع ترک است. این دو یک مفهوم نیستند؛ مقدار استفاده‌شده تضمین نمی‌کند text renderer خاموش شود. همین تنظیم در هر STATE_READY دوباره اعمال می‌شود.

**اصلاح پیشنهادی:** برای نوع ترک از setTrackTypeDisabled(C.TRACK_TYPE_TEXT, ...) و سیاست انتخاب کاربر استفاده کنید؛ فعال‌ماندن زیرنویس بعد از buffering را تست کنید.

### K-57 — پلیر Audio Focus و MediaSession ندارد

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/PlayerActivity.kt:234)

در پلیرهای مستقیم/زنده تنظیم AudioAttributes با مدیریت focus یا MediaSession وجود ندارد؛ اسپلش مدیریت audio focus دارد. هماهنگی با صدا و کنترل‌های رسانهٔ سیستم ناقص است.

**اصلاح پیشنهادی:** مدیریت audio focus مناسب پخش و MediaSession و کنترل‌های ریموت رسانه را اضافه کنید.

### K-58 — کارت‌های خانه و جستجو نام عنوان ندارند

**P2 · قطعی؛ شدت بصری نیازمند TV** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/res/layout/item_movie_card.xml:1)

این layout فقط پوستر، امتیاز و progress دارد؛ MovieAdapter نیز هیچ نامی نمایش نمی‌دهد. با پوستر خطادار چند کارت یکسان و غیرقابل شناسایی می‌شوند.

**اصلاح پیشنهادی:** نام کوتاه و حالت focused با عنوان کامل اضافه کنید؛ fallback تصویر را به نام محتوا وصل کنید.

### K-59 — فونت‌های کوچک و عرض‌های ثابت روی TV نیاز به اصلاح دارند

**P2 · ریسک طراحی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/res/layout/fragment_movie_detail.xml:157)

متن‌های ۱۱ تا ۱۳sp در کارت‌ها و منو و عرض ثابت 700dp توضیح و 500dp جستجو و grid پنج‌ستونی وجود دارد. نام بلند، اندازهٔ فونت بالاتر و بازشدن sidebar می‌تواند خوانایی و جاگیری را خراب کند؛ بصری تأیید نشده.

**اصلاح پیشنهادی:** چیدمان را با عرض قابل استفاده تطبیق دهید و روی 720p/1080p/4K و فونت بزرگ با فاصلهٔ نشستن واقعی تست کنید.

### K-60 — برچسب مرتب‌سازی با رفتار واقعی نمی‌خواند

**P3 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Fragments/MoviesFragment.kt:96)

«تاریخ ساخت» در فیلم و سریال بر اساس id مرتب می‌شود، نه تاریخ ساخت. در TV متن اولیه «ترتیب الفبا» است ولی currentSort ابتدا default و خروجی بر اساس id است.

**اصلاح پیشنهادی:** عنوان را به «تازه اضافه‌شده» تغییر دهید یا واقعاً release date را استفاده کنید؛ متن اولیهٔ TV را با state هماهنگ کنید.

### K-61 — شبکه برای تمام دامنه‌ها HTTP و گواهی کاربر را مجاز می‌کند

**P2 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/res/xml/network_security_config.xml:3)

base-config برای همهٔ اتصال‌ها cleartextTrafficPermitted=true و certificates src=user دارد؛ محدودهٔ استثنا مشخص نیست.

**اصلاح پیشنهادی:** استثناهای لازم استریم را محدود به دامنه‌های مشخص کنید و برای باقی شبکه سیاست TLS معمول را نگه دارید.

### K-62 — ساختار build و وابستگی‌ها هزینهٔ اضافی دارند

**P3 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/build.gradle.kts:74)

Media3 و ExoPlayer قدیمی و libVLC با هم آمده‌اند؛ VLC در layout هست اما در PlayerActivity اجرا نمی‌شود. libVLC و OkHttp و datasource و BOMها هم چند بار اعلام شده‌اند. تکرار declaration لزوماً بستهٔ تکراری تولید نمی‌کند ولی نگهداری و سازگاری را مبهم می‌کند.

**اصلاح پیشنهادی:** پخش‌کننده‌های غیرلازم را حذف و versions و BOMها را یک‌دست کنید؛ اندازهٔ APK را پس از build مقایسه کنید.

### K-63 — Listener ناوبری با شیء اشتباه حذف می‌شود

**P3 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/MainActivity.kt:112)

در onDestroy یک lambda تازه به removeOnDestinationChangedListener داده شده؛ همان Listener ثبت‌شده نیست و حذف مورد نظر رخ نمی‌دهد.

**اصلاح پیشنهادی:** Listener را در property نگه دارید و همان instance را حذف کنید.

### K-64 — مقدار امتیاز ناموجود متن اشتباه دارد

**P3 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/Adapter/MovieGridAdapter.kt:61)

در MovieGridAdapter وقتی voteAverage صفر یا ناموجود است، «اس» نمایش داده می‌شود؛ در خانه «۰٫۰» و در سریال «--» نشان داده می‌شود. نبود داده با امتیاز واقعی صفر اشتباه گرفته می‌شود و fallbackها یک‌دست نیستند.

**اصلاح پیشنهادی:** برای نبود امتیاز یک حالت یکسان مانند «بدون امتیاز» یا حذف badge استفاده کنید.

### K-65 — ترجمهٔ ژانر Soap نادرست است

**P3 · قطعی** · [محل کد](C:/Users/javad/Downloads/Documents/kelkin-main/app/src/main/java/com/example/kelkin/object/TranslationHelper.kt:27)

ژانر Soap به «پرهزینه صابونی» ترجمه شده؛ این عبارت نام قابل فهم این ژانر برای کاربر فارسی‌زبان نیست.

**اصلاح پیشنهادی:** از عنوان متناسبی مثل «سریال ملودرام» استفاده کنید و ترجمهٔ ژانرها را بازبینی کنید.

## اعتبارسنجی انجام‌شده

تمام ۱۱۸ فایل XML منابع از نظر well-formed بودن parse شدند و خطای نحوی XML پیدا نشد؛ این بررسی جایگزین Android Lint یا build نیست. تبدیل آدرس پوستر با ورودی لینک کامل نیز جداگانه بررسی شد و همان لینک دارای پیشوند تکراری تولید شد. ایرادها از بررسی مسیرهای کد استخراج شده‌اند؛ هیچ تست روی دستگاه انجام نشده است.

## بررسی‌هایی که هنوز به دستگاه یا دسترسی سرور نیاز دارند

- صحت Rules برای خواندن آرشیو، isActive، نوشتن محتوا و نقش ادمین؛ دسترسی سرور آزمایش نشده است.
- ورود Google روی مدل‌های مختلف TV، حساب بدون Google Play Services، لغو ورود و حساب تازه فعال‌شده.
- مسیر فوکوس XML و Compose، ورود و خروج از sidebar، برگشت از جزئیات به کارت قبلی، ردیف کوتاه و grid خالی، popup و dialog با D-pad.
- باز و بسته‌شدن هر دو overlay در پلیر زنده و بازگشت فوکوس پس از انتخاب سورس؛ جهت بالا/پایین نباید overlay دوم ناخواسته باز کند.
- قطع شبکه هنگام پخش، 403/404، لینک منقضی، codec پشتیبانی‌نشده، MPEG-TS/MP4/HLS/DASH و تعویض سریع کانال.
- Resume در VK/OK وقتی video در iframe یا DOM دیرهنگام است؛ script فعلی فقط document.querySelector('video') را می‌خواند و timeout مستقل برای پایان callback ندارد.
- تبدیل URLهای VK/OK دارای query یا ساختارهای متفاوت؛ تشخیص فعلی contains دامنه را دقیق بررسی نمی‌کند و تبدیل OK query را از videoId جدا نمی‌کند.
- مصرف RAM و CPU و روانی روی TV کم‌حافظه با آرشیو بزرگ؛ backdropهای original و بازسازی همهٔ ردیف‌ها پس از هر metadata می‌توانند هزینه داشته باشند، اما اندازه‌گیری نشده است.
- نمایش بلندترین نام/توضیح، کنتراست روی پس‌زمینه‌های روشن، برش کارت بزرگ‌شده، فونت بزرگ و overscan.

## آزمایش‌های بازتولید پیشنهادی

1. آرشیو فیلم و سریال را باز کنید و URL نهایی Glide را ببینید: وجود دوبارهٔ `https://` داخل path نشان‌دهندهٔ K-06 و K-07 است.
2. کانال A را شروع، به B بروید و B را به لینک خطادار تغییر دهید؛ Retry نباید A را باز کند. سپس هدرهای stream را به‌روزرسانی کنید؛ باید B حفظ شود.
3. در پنل برای یک کانال سورس ثبت کنید؛ در پلیر بالا را بزنید و ببینید همان داده در لیست سورس هست.
4. قسمت دوم فصل دوم را ۳ دقیقه ببینید و برگردید؛ کارت ادامه تماشا باید همان قسمت را از همان زمان باز کند.
5. سریع «الف» سپس عبارت دیگر تایپ کنید و متن را پاک کنید؛ پاسخ کند قبلی نباید نتایج را برگرداند.
6. وارد جزئیات سریال شوید و قبل از پاسخ TMDB صفحه را ترک کنید؛ همچنین تنظیمات را سریع باز و بسته کنید. نباید binding null خوانده شود.
7. با Android API 23 و 25 خطای splash video را ایجاد کنید و fallback فونت را بررسی کنید.
8. دو رکورد فیلم/سریال با tmdb_id یکسان و دو رکورد بدون شناسه بسازید؛ کش، bookmark و navigation نباید به هم برخورد کنند.
9. بدون شبکه و با دادهٔ محلی موجود اپ را باز کنید؛ باید آرشیو قبلی، کانال‌ها و اقدام retry روشن نمایش داده شوند.
10. هنگام Pause، Home را بزنید و برگردید؛ Pause باید حفظ شود. هنگام Play همین کار را انجام دهید و سابقهٔ تماشا را بررسی کنید.

## مبنای API

- [Media3 Parameters.Builder](https://developer.android.com/reference/androidx/media3/exoplayer/trackselection/DefaultTrackSelector.Parameters.Builder): تفاوت rendererIndex و trackType برای K-56.
- [WebView lifecycle](https://developer.android.com/reference/android/webkit/WebView): چرخهٔ عمر WebView مستقل از توقف ExoPlayer در K-18 باید مدیریت شود.
- [چیدمان Android TV](https://developer.android.com/training/tv/playback/compose/layouts): فاصلهٔ دید، سادگی حرکت و مقیاس ابعاد در ارزیابی K-59.
