from rewrite_support import *
j='app/src/main/java/com/example/kelkin/'
p='app/src/main/res/layout/activity_player.xml'
text=read(p)
text=text.replace('    android:background="@color/bg_black">','    android:background="@color/bg_black"\n    android:focusable="true"\n    android:focusableInTouchMode="true">',1)
panel='''
    <LinearLayout
        android:id="@+id/playerStatusPanel"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:padding="24dp"
        android:orientation="vertical"
        android:gravity="center"
        android:background="@drawable/bg_source_panel"
        android:visibility="gone"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent">
        <ProgressBar
            android:id="@+id/playerStatusProgress"
            android:layout_width="40dp"
            android:layout_height="40dp" />
        <TextView
            android:id="@+id/playerStatusText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:maxWidth="600dp"
            android:gravity="center"
            android:textColor="@color/text_white"
            android:textSize="18sp"
            android:padding="16dp" />
        <LinearLayout
            android:id="@+id/playerStatusActions"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layoutDirection="rtl">
            <Button
                android:id="@+id/btnPlayerRetry"
                android:layout_width="wrap_content"
                android:layout_height="52dp"
                android:background="@drawable/sl_button_high_contrast"
                android:textColor="@drawable/sl_text_high_contrast"
                android:text="تلاش دوباره" />
            <Button
                android:id="@+id/btnPlayerSources"
                android:layout_width="wrap_content"
                android:layout_height="52dp"
                android:background="@drawable/sl_button_high_contrast"
                android:textColor="@drawable/sl_text_high_contrast"
                android:layout_marginHorizontal="12dp"
                android:text="سورس دیگر" />
            <Button
                android:id="@+id/btnPlayerBack"
                android:layout_width="wrap_content"
                android:layout_height="52dp"
                android:background="@drawable/sl_button_high_contrast"
                android:textColor="@drawable/sl_text_high_contrast"
                android:text="بازگشت" />
        </LinearLayout>
    </LinearLayout>
'''
text=text.replace('</androidx.constraintlayout.widget.ConstraintLayout>',panel+'</androidx.constraintlayout.widget.ConstraintLayout>',1) if False else text
pos=text.rfind('</androidx.constraintlayout.widget.ConstraintLayout>')
text=text[:pos]+panel+text[pos:]
write(p,text)
p=j+'VirtualMouseView.kt'
edit(p,'private val speed = 30f','private val speed get() = (width * 0.018f).coerceAtLeast(8f)')
edit(p,'PointF(640f, 360f)','PointF(0f, 0f)')
edit(p,'            val currentPlatform = activity?.intent?.getStringExtra("video_url") // یک روش کمکی برای گرفتن وضعیت پلتفرم جاری اکتیویتی','')
edit(p,'        val maxX = width.toFloat() - pointer.width','        val maxX = (width.toFloat() - pointer.width).coerceAtLeast(0f)')
edit(p,'        val maxY = height.toFloat() - pointer.height','        val maxY = (height.toFloat() - pointer.height).coerceAtLeast(0f)')
edit(p,'        val clickX = location[0].toFloat() + (pointer.width / 2)\n        val clickY = location[1].toFloat() + (pointer.height / 2)','''        val webLocation = IntArray(2)
        targetWebView.getLocationOnScreen(webLocation)
        val clickX = location[0].toFloat() - webLocation[0] + pointer.width / 2f
        val clickY = location[1].toFloat() - webLocation[1] + pointer.height / 2f''')
edit(p,'    override fun onDetachedFromWindow()', '''    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        currentPos.set(w / 2f, h / 2f)
        pointer.translationX = currentPos.x; pointer.translationY = currentPos.y
    }

    override fun onDetachedFromWindow()''')
p=j+'PlayerActivity.kt'
edit(p,'"Kelkin/${BuildConfig.VERSION_NAME}"','"Kelkin/1.80"')
edit(p,'    private fun showSources() {','    private fun showSources() {\n        hideStatus()')

p=j+'Adapter/ChannelAdapter.kt'
edit(p,'        holder.itemView.setOnClickListener { onChannelClick(channel) }','''        holder.itemView.setOnClickListener { onChannelClick(channel) }
        holder.itemView.setOnLongClickListener {
            val prefs = it.context.getSharedPreferences("kelkin_prefs", 0)
            val favorites = prefs.getStringSet("favorite_channels", emptySet()).orEmpty().toMutableSet()
            val added = favorites.add(channel.id.toString())
            if (!added) favorites.remove(channel.id.toString())
            prefs.edit().putStringSet("favorite_channels", favorites).apply()
            android.widget.Toast.makeText(it.context, if (added) "کانال نشان شد" else "کانال از نشان‌شده‌ها حذف شد", android.widget.Toast.LENGTH_SHORT).show()
            true
        }''')

write('app/src/main/res/xml/network_security_config.xml','''<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors><certificates src="system" /></trust-anchors>
    </base-config>
    <!-- Add only known legacy stream hosts here; do not enable HTTP for the entire app. -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">aparatchi.com</domain>
    </domain-config>
</network-security-config>
''')

# Show a persistent selected-season border independently from white focus.
p='app/src/main/res/drawable/sl_button_high_contrast.xml'
text=read(p)
idx=text.rfind('    <item>')
if idx>=0:
    text=text[:idx]+'''    <item android:state_selected="true">
        <shape android:shape="rectangle">
            <solid android:color="#303030" />
            <stroke android:width="2dp" android:color="@color/yellow" />
            <corners android:radius="6dp" />
        </shape>
    </item>
'''+text[idx:]
write(p,text)
