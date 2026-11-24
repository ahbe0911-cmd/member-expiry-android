# مدیریت تاریخ انقضای اعضا — نسخه 1.0.0 (فاز ۵)

سورس تجمعی پنج فاز. شناسه ثابت `ir.memberexpiry.manager`، Room schema=1، فایل `members.db`، حداقل اندروید ۸. هیچ داده نمونه‌ای وارد دیتابیس نمی‌شود. ساخت و نصب واقعی APK هنوز در محیط تهیه این بسته تأیید نشده است.

## امکانات و سیاست‌های نسخه نهایی

- رابط فارسی RTL، تم زمردی و حالت روشن/تیره، فونت وزیرمتن، داشبورد چهارگانه، نمودار ۱۲ ماه، جستجوی سراسری، کپی کد ملی، افزودن/اصلاح، تمدید و Undo؛ بخش آمار و تاریخچه، اعلان WorkManager و ثبت‌نام HTTPS با Custom Tabs. موتور شمسی حسابی ۳۳ ساله فقط سال‌های ۱۳۰۴ تا ۱۴۶۸ را پشتیبانی می‌کند. تاریخ امروز تابع منطقه زمانی دستگاه است؛ تاریخ تمدید پیش‌فرض یک سال شمسی از دیرترِ امروز و انقضای قبلی است. تاریخ واقعی انقضا برای انتقال معوقات تغییر نمی‌کند.
- در خانه، کارت «مدیریت فایل و پشتیبان»: ورود XLSX/CSV، پشتیبان کامل JSON و بازیابی آن. انتخاب فایل از Storage Access Framework است؛ مجوز دسترسی عمومی به حافظه لازم نیست. برنامه برای فایل‌ها به شبکه نیاز ندارد؛ اگر کاربر ارائه‌دهنده فضای ابری را در انتخاب‌گر انتخاب کند، ممکن است خود آن ارائه‌دهنده به اینترنت نیاز داشته باشد. ثبت‌نام سایت نیز به اینترنت نیاز دارد.
- XLSX: انتخاب شیت، Shared Strings و inline strings، سلول عددی، تاریخ سریالی اکسل ۱۹۰۰ و ۱۹۰۴، تاریخ شمسی یا میلادی با سال چهاررقمی. فایل‌های XLS قدیمی، XLSX رمزدار، XML غیر UTF-8 و برخی گونه‌های غیرمعمول OOXML پشتیبانی نمی‌شوند. فرمول اجرا نمی‌شود؛ فقط مقدار ذخیره‌شده در فایل خوانده می‌شود. حداکثر ۵۰ شیت، مجموع ۵۰٬۰۰۱ ردیف، ۱۲۸ ستون، ۳۲٬۷۶۸ نویسه هر سلول؛ ورودی حداکثر ۳۲ MiB و داده بازشده ZIP حداکثر ۶۴ MiB. این محدودیت‌ها برای کنترل مصرف حافظه‌اند، نه تضمین اجرای حداکثر حجم روی همه گوشی‌ها.
- CSV: UTF-8 (با یا بدون BOM) یا UTF-16 دارای BOM؛ جداکننده کاما، نقطه‌ویرگول یا Tab، نقل‌قول و سلول چندخطی. خروجی UTF-8 BOM است. متن‌هایی که با نویسه‌های فرمول‌ساز شروع می‌شوند در CSV با آپاستروف محافظت می‌شوند. Excel ممکن است در هنگام باز کردن مستقیم CSV صفرهای ابتدایی کد ملی را حذف کند؛ خروجی XLSX کد ملی را به صورت رشته نگه می‌دارد و گزینه مناسب‌تر است. کدهای کوتاه عددی هنگام ورود با صفر از چپ کامل می‌شوند؛ اعتبارسنجی رقم کنترل کد ملی انجام نمی‌شود.
- تشخیص عنوان‌ها از ۳۰ ردیف اول (فارسی و انگلیسی). کد ملی، نام، نام خانوادگی و تاریخ انقضا ضروری، نام پدر و وضعیت اختیاری. نام‌های غیرشناخته‌شده باید در فایل مبدأ به عنوان استاندارد تبدیل شوند؛ نگاشت دستی ستون‌ها در این نسخه نیست. پیش‌نمایش تعداد معتبر/نامعتبر، حداکثر ۲۰ پیام خطا و ۳ نمونه؛ حذف ردیف نامعتبر نیازمند تأیید است.
- ادغام اکسل: تطبیق کد ملی، به‌روزرسانی اطلاعات موجود یا درج رکورد جدید؛ اگر در فایل یک کد تکرار شود، آخرین ردیف معتبر اعمال می‌شود. اگر کد در بانک بیش از یک بار موجود باشد، ادغام آن فایل به صورت اتمیک متوقف می‌شود. افزودن همه: شناسه مستقل حتی برای کد ملی تکراری. جایگزینی: حذف جدول اعضا و درج ردیف‌های معتبر با تأیید صریح؛ جدول تاریخچه حفظ می‌شود اما Undo عملیات عضو حذف‌شده ممکن نیست. ورود داده تاریخی به عنوان تمدید یا عضویت جدید امروز ثبت نمی‌شود. ثبت دستی عضو و تمدید واقعی همچنان در آمار ثبت می‌شوند.
- دکمه Excel و CSV در فهرست اعضا، صفحه عملیات و تاریخچه انقضا؛ خروجی بر اساس داده‌ها و فیلتر فعال در لحظه انتخاب تهیه می‌شود. خروجی تاریخچه یا عملیات، قالب گزارش است نه فایل ورود اعضا. XLSX واقعی با سلول‌های متنی، بدون ماکرو یا فرمول تولید می‌شود. خروجی CSV برای حفظ متن در Excel ممکن است نیاز به Import با نوع ستون Text داشته باشد.
- JSON شامل هر دو جدول با شناسه، تاریخ‌ها، وضعیت، شماره بازبینی و همه تاریخچه (حتی عملیات Undo) است. قالب نسخه‌دار با محدودیت ۳۲ MiB، ۵۰٬۰۰۰ عضو و ۲۰۰٬۰۰۰ سابقه. ادغام بر اساس شناسه یکتا؛ رکوردهای یکسان تکرار نمی‌شوند و شناسه یکسان با محتوای متفاوت باعث توقف کل تراکنش می‌شود. شناسه متفاوت با کد ملی یکسان مستقل می‌ماند. بازیابی جایگزین هر دو جدول را پس از تأیید پاک و بازیابی می‌کند. JSON رمزگذاری نشده و باید در محل امن نگهداری شود. پشتیبان تنظیمات دستگاه/مجوز اعلان و مرورگر را شامل نمی‌شود. بازیابی و ورود تراکنشی‌اند؛ ساخت فایل خروجی پیش از نوشتن انجام می‌شود، ولی قطع برق یا خطای ارائه‌دهنده می‌تواند فایل ناقص باقی بگذارد.
- WorkManager تقریباً هر ۲۴ ساعت بررسی می‌کند؛ زمان دقیق، اجرای پس از Force Stop و مجوز اعلان تضمین نمی‌شوند. SSL دور زده نمی‌شود. فرم بازِ قبلی یا انتخاب فایل در صورت کشته شدن فرایند ممکن است از دست برود؛ تراکنش Room داده نیمه‌کاره ثبت نمی‌کند.

## فونت

Gradle نسخه ثابت `v33.003` از مخزن رسمی Vazirmatn را در مرحله build دریافت و دو وزن Regular/Bold را به عنوان منابع Android (`R.font.vazirmatn_regular` و `R.font.vazirmatn_bold`) و متن مجوز OFL را به عنوان `R.raw.vazirmatn_license` بسته‌بندی می‌کند. هیچ فونتی در زمان اجرای اپ دانلود نمی‌شود. اولین build یا build پس از پاک کردن خروجی‌ها برای این منابع به شبکه نیاز دارد؛ اگر GitHub raw در دسترس نباشد، ساخت با خطا متوقف می‌شود و فونت دیگری به جای آن استفاده نمی‌شود. آدرس‌ها و بررسی ابتدایی فرمت در `app/build.gradle.kts` قرار دارند. مرجع: https://github.com/rastikerdar/vazirmatn ، https://developer.android.com/build/extend-agp .

## نسخه‌های ساخت

JDK 17، Gradle 8.13، AGP 8.11.1، Kotlin 2.2.21، KSP 2.2.21-2.0.4، Compose BOM 2025.05.01، Room 2.8.4، WorkManager 2.10.1، AndroidX Browser 1.8.0. minSdk=26، targetSdk=36 و compileSdk=36. نسخه برنامه `1.0.0` و versionCode=5.

## اجرا و GitHub Actions

1. همه فایل‌ها و پوشه‌های مخفی از جمله `.github` را داخل ریشه مخزن جایگزین کنید. Wrapper قبلی را نگه دارید.
2. Commit و Push روی main/master یا اجرای دستی workflow `Build APK` از تب Actions. گردش‌کار lint، تست JVM، Debug APK، کامپایل APK تست اندروید، Release APK و بررسی امضا را انجام می‌دهد. برای اجرای واقعی تست‌های دستگاه، از Run workflow گزینه `run_device_tests` را فعال کنید (پیش‌فرض اجرای دستی: فعال). پس از build، job جداگانه emulator روی API 35 تست‌ها را اجرا و گزارش را در Artifact ذخیره می‌کند. Push عادی فقط build و تست‌های JVM را اجرا می‌کند. مرجع راه‌اندازی شبیه‌ساز: https://github.com/ReactiveCircus/android-emulator-runner .
3. از قسمت Artifacts فایل `MemberExpiry-Debug` را دانلود و استخراج کنید. `MemberExpiry-Release` در صورت تنظیم کلید امضا قابل نصب است. `Gradle-Wrapper-8.13` نیز به عنوان artifact تولید می‌شود. نصب روی نسخه موجود فقط در صورت یکسان بودن امضا امکان‌پذیر است. کلید Debug در runner موقت ممکن است با اجرای قبلی متفاوت باشد؛ برای اطلاعات واقعی از کلید ثابت استفاده کنید و نسخه نصب‌شده دارای داده را حذف نکنید.
4. برای Release امضاشده، Secrets با نام‌های `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` را در مخزن خصوصی تعریف کنید؛ کلید خصوصی را commit نکنید.
5. برای ساخت محلی، Android SDK و JDK 17 نصب شده باشند. با Wrapper موجود: `./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` (ویندوز: `gradlew.bat`). برای تست روی دستگاه یا emulator: `./gradlew :app:connectedDebugAndroidTest`.

## بررسی دستی نصب (ترجیحاً دستگاه یا پروفایل آزمایشی)

- ایجاد یک عضو با تاریخ دیروز، یکی امروز و یکی در آینده. بررسی شمارش چهار کارت، صف معوقات، نمودار و مرکز اعلان. ثبت رکورد با صفر ابتدای کد ملی و کپی آن را کنترل کنید.
- جستجوی همان کد با ارقام فارسی و ASCII و جستجوی نام با ی/ي و ک/ك.
- تمدید یکی از معوقات: حذف از فهرست معوقات و افزایش آمار تمدید؛ تاریخ انقضای قبلی در تاریخچه ماه خود باقی بماند. بازگردانی عملیات و بررسی شمارش‌ها.
- اصلاح تاریخ یا نام: بدون افزایش شمارش تمدید؛ بازگردانی عملیات قدیمی پس از اصلاح باید رد شود.
- ماه و سال نمودار، فیلترهای تاریخچه و آمار و حالت خالی را بررسی کنید. تاریخ اشتباه و کد ملی ناقص یا تکراری باید با پیام فارسی رد شوند و فرم باز بماند.
- حالت پرواز، تغییر تم دستگاه، چرخش نمایشگر، فونت بزرگ، بازگشت از سامانه سامان و باز کردن اعلان سیستم را بررسی کنید. صفحه انتخابی حفظ می‌شود؛ فرم بازِ در حال ویرایش در این نسخه ممکن است پس از بازآفرینی Activity بسته شود.

## کنترل کیفیت این بسته

- پنج آزمون JVM موتور CSV/XLSX با Kotlin مستقل (رفت‌وبرگشت CSV/XLSX، تاریخ و ردیف نامعتبر، محافظت فرمول و CSV خراب، رد DTD) موفق شدند. تست‌ها در سورس موجودند.
- خواندن فایل Excel تولیدشده با openpyxl و بازکردن خروجی XLSX با openpyxl موفق بود؛ کد ملی صفرآغاز در سلول متن حفظ شد.
- بررسی نوع‌ها و کامپایل مستقل لایه تاریخ، داده، انتقال و ViewModel با بدل‌های API اندروید/Room/Lifecycle/JSON موفق بود؛ این آزمون اجرای JSON یا Android واقعی نیست.
- تست‌های دستگاه برای Room و بازیابی JSON در پروژه موجودند، ولی در این محیط اجرا نشده‌اند. Android SDK/Gradle موجود نیست؛ بیلد واقعی Compose و KSP، نصب APK، عملکرد SAF، دریافت فونت و اعلان روی گوشی باید در CI یا دستگاه بررسی شود. این بسته تضمین کامپایل ۱۰۰٪ نیست.
- برای آزمایش روی گوشی: ابتدا از داده‌های فعلی پشتیبان بگیرید. یک فایل با عنوان‌های استاندارد بسازید، یک ردیف نامعتبر وارد کنید و پیش‌نمایش را کنترل کنید. ادغام/افزودن/جایگزینی و خروجی هر صفحه را روی بانک آزمایشی اجرا کنید. پشتیبان JSON را پس از تمدید و Undo ذخیره کنید و در بانک آزمایشی بازیابی کامل کنید. اجرای مجدد همان پشتیبان در حالت ادغام نباید رکورد تکراری ایجاد کند.

## مسیر دقیق فایل‌ها

تمام فایل‌ها در ZIP موجودند؛ مسیرها نسبت به ریشه مخزن هستند.

- `.gitattributes`
- `.github/workflows/build-apk.yml`
- `.gitignore`
- `README.md`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/schemas/.gitkeep`
- `app/src/androidTest/java/ir/memberexpiry/manager/data/RepositoryDeviceTest.kt`
- `app/src/androidTest/java/ir/memberexpiry/manager/data/TransferDeviceTest.kt`
- `app/src/androidTest/java/ir/memberexpiry/manager/ui/screens/MemberFormTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/ir/memberexpiry/manager/MainActivity.kt`
- `app/src/main/java/ir/memberexpiry/manager/MemberExpiryApplication.kt`
- `app/src/main/java/ir/memberexpiry/manager/core/browser/RegistrationLauncher.kt`
- `app/src/main/java/ir/memberexpiry/manager/core/date/DateProvider.kt`
- `app/src/main/java/ir/memberexpiry/manager/core/date/ExcelDates.kt`
- `app/src/main/java/ir/memberexpiry/manager/core/date/JalaliDate.kt`
- `app/src/main/java/ir/memberexpiry/manager/core/date/PersianText.kt`
- `app/src/main/java/ir/memberexpiry/manager/core/transfer/BackupCodec.kt`
- `app/src/main/java/ir/memberexpiry/manager/core/transfer/Reports.kt`
- `app/src/main/java/ir/memberexpiry/manager/core/transfer/Tabular.kt`
- `app/src/main/java/ir/memberexpiry/manager/core/transfer/XlsxCodec.kt`
- `app/src/main/java/ir/memberexpiry/manager/data/local/ActivityLogDao.kt`
- `app/src/main/java/ir/memberexpiry/manager/data/local/ActivityLogEntity.kt`
- `app/src/main/java/ir/memberexpiry/manager/data/local/MemberDao.kt`
- `app/src/main/java/ir/memberexpiry/manager/data/local/MemberDatabase.kt`
- `app/src/main/java/ir/memberexpiry/manager/data/local/MemberEntity.kt`
- `app/src/main/java/ir/memberexpiry/manager/data/local/Projections.kt`
- `app/src/main/java/ir/memberexpiry/manager/data/repository/MemberInput.kt`
- `app/src/main/java/ir/memberexpiry/manager/data/repository/MemberRepository.kt`
- `app/src/main/java/ir/memberexpiry/manager/notifications/ExpiryNotifier.kt`
- `app/src/main/java/ir/memberexpiry/manager/notifications/ExpiryScheduler.kt`
- `app/src/main/java/ir/memberexpiry/manager/notifications/ExpirySummary.kt`
- `app/src/main/java/ir/memberexpiry/manager/notifications/ExpiryWorker.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/screens/DashboardScreen.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/screens/MemberApp.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/screens/MemberForms.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/screens/NotificationsScreen.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/screens/StatisticsScreens.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/screens/TransferDialogs.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/screens/UiComponents.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/theme/Theme.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/viewmodel/MemberViewModelFactory.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/viewmodel/MembersViewModel.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/viewmodel/NotificationCenterViewModel.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/viewmodel/StatisticsViewModel.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/viewmodel/TransferViewModel.kt`
- `app/src/main/java/ir/memberexpiry/manager/ui/viewmodel/UiState.kt`
- `app/src/main/res/drawable/ic_expiry_notification.xml`
- `app/src/main/res/drawable/ic_launcher.xml`
- `app/src/main/res/drawable/ic_ui_bell.xml`
- `app/src/main/res/drawable/ic_ui_close.xml`
- `app/src/main/res/drawable/ic_ui_copy.xml`
- `app/src/main/res/drawable/ic_ui_external.xml`
- `app/src/main/res/drawable/ic_ui_history.xml`
- `app/src/main/res/drawable/ic_ui_home.xml`
- `app/src/main/res/drawable/ic_ui_people.xml`
- `app/src/main/res/drawable/ic_ui_plus.xml`
- `app/src/main/res/drawable/ic_ui_search.xml`
- `app/src/main/res/drawable/ic_ui_stats.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/test/java/ir/memberexpiry/manager/core/date/DateEngineTest.kt`
- `app/src/test/java/ir/memberexpiry/manager/core/transfer/TransferCodecTest.kt`
- `app/src/test/java/ir/memberexpiry/manager/notifications/ExpiryPolicyTest.kt`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `settings.gradle.kts`
