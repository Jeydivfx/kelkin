from pathlib import Path
import re

base = Path('app/src/main/res/layout')
home = base / 'fragment_home.xml'
text = home.read_text(encoding='utf-8')
text = text.replace('android:layout_height="450dp"', 'android:layout_height="330dp"')
text = text.replace('android:layout_marginTop="150dp"', 'android:layout_marginTop="44dp"')
text = text.replace('android:layout_width="500dp"', 'android:layout_width="0dp"')
text = text.replace('android:layout_marginTop="-60dp"', 'android:layout_marginTop="0dp"')
text = text.replace('android:textSize="11sp"', 'android:textSize="14sp"').replace('android:textSize="13sp"', 'android:textSize="16sp"')
text = text.replace('android:gravity="right"\n                app:layout_constraintTop', 'android:gravity="right"\n                app:layout_constraintWidth_percent="0.65"\n                app:layout_constraintTop', 1)
home.write_text(text, encoding='utf-8')

for name in ('fragment_movie_detail.xml', 'fragment_series_details.xml'):
    path = base / name
    text = path.read_text(encoding='utf-8')
    start = text.index('    <LinearLayout')
    end = text.rindex('    </LinearLayout>') + len('    </LinearLayout>')
    content = text[start:end]
    content = content.replace('android:layout_height="match_parent"', 'android:layout_height="wrap_content"', 1)
    content = content.replace('android:orientation="vertical"', 'android:orientation="vertical"\n        android:paddingTop="40dp"\n        android:paddingBottom="32dp"', 1)
    content = re.sub(r'\s*<Space\s.*?/>', '', content, count=1, flags=re.S)
    content = content.replace('android:layout_width="700dp"', 'android:layout_width="match_parent"')
    content = content.replace('android:textSize="40sp"', 'android:textSize="32sp"\n            android:maxLines="2"\n            android:ellipsize="end"')
    content = content.replace('android:textSize="12sp"', 'android:textSize="14sp"').replace('android:textSize="15sp"', 'android:textSize="16sp"')
    wrapper = '''    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:fillViewport="true"
        android:overScrollMode="never">
'''
    text = text[:start] + wrapper + content + '\n    </androidx.core.widget.NestedScrollView>' + text[end:]
    path.write_text(text, encoding='utf-8')
