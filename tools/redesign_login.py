from pathlib import Path

root = Path(__file__).resolve().parents[1]
res = root / 'app/src/main/res'
def write(path, text):
    (res / path).write_text(text, encoding='utf-8')

write('drawable/bg_login_canvas.xml', '''<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:angle="0" android:startColor="#101923" android:centerColor="#11151C" android:endColor="#1D1920" />
</shape>''')
write('drawable/bg_login_panel.xml', '''<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#202630" /><corners android:radius="24dp" />
    <stroke android:width="1dp" android:color="#35404D" />
</shape>''')
for name, normal, focus in [('primary', '#F2E3C5', '#FFFFFF'), ('secondary', '#28323F', '#43566A')]:
    write(f'drawable/bg_login_{name}.xml', f'''<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_enabled="false"><shape><solid android:color="#39414B"/><corners android:radius="12dp"/></shape></item>
    <item android:state_focused="true"><shape><solid android:color="{focus}"/><corners android:radius="12dp"/><stroke android:width="3dp" android:color="#E7BA73"/></shape></item>
    <item><shape><solid android:color="{normal}"/><corners android:radius="12dp"/></shape></item>
</selector>''')
write('layout/activity_login.xml', '''<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent" android:background="@drawable/bg_login_canvas" android:padding="40dp">
    <LinearLayout android:layout_width="780dp" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical" android:layoutDirection="ltr"
        app:layout_constraintStart_toStartOf="parent" app:layout_constraintEnd_toEndOf="parent" app:layout_constraintTop_toTopOf="parent" app:layout_constraintBottom_toBottomOf="parent">
        <LinearLayout android:layout_width="340dp" android:layout_height="wrap_content" android:orientation="vertical" android:background="@drawable/bg_login_panel" android:padding="28dp" android:gravity="center_horizontal" android:layoutDirection="rtl">
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/login_panel_title" android:fontFamily="@font/vazir_bold" android:textColor="#FFFFFF" android:textSize="24sp" android:gravity="right" />
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:layout_marginBottom="24dp" android:text="@string/login_panel_subtitle" android:fontFamily="@font/vazir_regular" android:textColor="#B9C3CE" android:textSize="14sp" android:gravity="right" />
            <LinearLayout android:id="@+id/materialCardGoogle" android:layout_width="match_parent" android:layout_height="54dp" android:orientation="horizontal" android:gravity="center" android:layoutDirection="ltr" android:background="@drawable/bg_login_primary" android:clickable="true" android:focusable="true" android:nextFocusDown="@id/btnRetryActivation">
                <ImageView android:id="@+id/icGoogle" android:layout_width="22dp" android:layout_height="22dp" android:src="@drawable/ic_google" android:importantForAccessibility="no" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:text="@string/login_with_google" android:fontFamily="@font/vazir_bold" android:textColor="#17202B" android:textSize="16sp" />
            </LinearLayout>
            <Button android:id="@+id/btnRetryActivation" android:layout_width="match_parent" android:layout_height="50dp" android:layout_marginTop="12dp" android:background="@drawable/bg_login_secondary" android:backgroundTint="@null" android:stateListAnimator="@null" android:text="@string/login_retry_activation" android:textAllCaps="false" android:textColor="#FFFFFF" android:fontFamily="@font/vazir_regular" android:textSize="14sp" android:nextFocusUp="@id/materialCardGoogle" />
            <TextView android:id="@+id/txtStatus" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:layout_marginTop="16dp" android:gravity="center" android:fontFamily="@font/vazir_regular" android:textColor="#CCD5DF" android:textSize="12sp" android:accessibilityLiveRegion="polite" android:text="@string/login_status_hint" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:layout_marginStart="56dp" android:orientation="vertical" android:gravity="right" android:layoutDirection="rtl">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="KELKIN" android:fontFamily="@font/bebas_neue_rgular" android:textSize="28sp" android:letterSpacing="0.18" android:textColor="#E7BA73" />
            <TextView android:id="@+id/txtTitle" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="14dp" android:text="@string/login_brand_title" android:fontFamily="@font/vazir_bold" android:textColor="#FFFFFF" android:textSize="38sp" android:gravity="right" />
            <TextView android:id="@+id/txtSubtitle" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="16dp" android:text="@string/login_brand_subtitle" android:fontFamily="@font/vazir_regular" android:textColor="#B9C3CE" android:textSize="17sp" android:lineSpacingExtra="6dp" android:gravity="right" />
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="28dp" android:text="@string/login_brand_categories" android:fontFamily="@font/vazir_regular" android:textColor="#E7BA73" android:textSize="14sp" android:gravity="right" />
        </LinearLayout>
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>''')
write('values/login_strings.xml', '''<resources>
    <string name="login_panel_title">خوش آمدید</string>
    <string name="login_panel_subtitle">با حساب گوگل وارد کلکین شوید.</string>
    <string name="login_retry_activation">بررسی دوبارهٔ فعال‌سازی</string>
    <string name="login_status_hint">پس از ورود، وضعیت فعال‌سازی حساب بررسی می‌شود.</string>
    <string name="login_brand_title">پنجره‌ای به\nدنیای تماشا</string>
    <string name="login_brand_subtitle">داستان بعدی شما از اینجا شروع می‌شود.</string>
    <string name="login_brand_categories">فیلم   •   سریال   •   تلویزیون زنده</string>
    <string name="intro_skip_hint">برای رد کردن معرفی، OK را بزنید</string>
</resources>''')

p = root / 'app/src/main/java/com/example/kelkin/LoginActivity.kt'
s = p.read_text(encoding='utf-8').replace('        signIn.setBackgroundResource(R.drawable.bg_setting_selector)\n', '')
a = s.index('        retry = Button(this).apply {')
b = s.index('        signIn.requestFocus()', a)
s = s[:a] + '''        retry = findViewById(R.id.btnRetryActivation)
        retry.setOnClickListener { if (!busy) lifecycleScope.launch { checkActivation() } }
''' + s[b:]
p.write_text(s, encoding='utf-8')

p = root / 'app/src/main/java/com/example/kelkin/SplashActivity.kt'
s = p.read_text(encoding='utf-8')
a = s.index('    override fun onKeyDown(')
b = s.index('    override fun onPause()', a)
s = s[:a] + '''    // Intercept before PlayerView or another focused child consumes the remote OK.
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val confirm = event.keyCode in intArrayOf(
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER,
            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
            android.view.KeyEvent.KEYCODE_BUTTON_A
        )
        if (confirm) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                navigateToMain()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

''' + s[b:]
p.write_text(s, encoding='utf-8')
p = res / 'layout/activity_splash.xml'
s = p.read_text(encoding='utf-8').replace('</androidx.constraintlayout.widget.ConstraintLayout>', '''    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_marginBottom="14dp" android:text="@string/intro_skip_hint"
        android:textColor="#CCCCCC" android:textSize="12sp" android:fontFamily="@font/vazir_regular"
        app:layout_constraintBottom_toTopOf="@id/txtVersion"
        app:layout_constraintStart_toStartOf="parent" app:layout_constraintEnd_toEndOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>''')
p.write_text(s, encoding='utf-8')
