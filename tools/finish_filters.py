from pathlib import Path
for name in ('fragment_movies.xml', 'fragment_series.xml', 'fragment_tv.xml'):
    path = Path('app/src/main/res/layout') / name
    text = path.read_text(encoding='utf-8')
    text = text.replace('android:textSize="13sp"', 'android:textSize="16sp"')
    text = text.replace('android:textColor="@color/text_white"', 'android:textColor="@color/sl_text_high_contrast"')
    text = text.replace('app:iconTint="@color/text_white"', 'app:iconTint="@color/sl_text_high_contrast"')
    text = text.replace('style="@style/Widget.MaterialComponents.Button.OutlinedButton"', 'style="@style/Widget.MaterialComponents.Button.OutlinedButton"\n            app:backgroundTint="@color/tv_filter_background"\n            android:minHeight="48dp"')
    path.write_text(text, encoding='utf-8')
