from pathlib import Path
import re
import zipfile

with zipfile.ZipFile('kelkin-before-fixes.zip') as backup:
    base = 'app/src/main/res/'
    layouts = ['fragment_home', 'fragment_movies', 'fragment_movie_detail', 'fragment_series',
               'fragment_series_details', 'fragment_tv', 'fragment_search', 'item_movie_card',
               'item_admin_list']
    series_now = Path(base + 'layout/fragment_series_details.xml').read_text(encoding='utf-8')
    continue_button = re.search(r'        <com.google.android.material.button.MaterialButton\s+android:id="@\+id/btnContinueSeries".*?/>', series_now, re.S).group()
    for name in layouts:
        path = base + 'layout/' + name + '.xml'
        text = backup.read(path).decode('utf-8-sig')
        text = text.replace('@drawable/sl_text_high_contrast', '@color/sl_text_high_contrast')
        if name == 'fragment_series_details':
            text = text.replace('        <HorizontalScrollView', continue_button + '\n\n        <HorizontalScrollView', 1)
            text = text.replace('android:text="@string/title_episodes"', 'android:id="@+id/episodeSectionTitle"\n            android:text="@string/title_episodes"', 1)
        if name in ('fragment_movies', 'fragment_series', 'fragment_tv'):
            text = text.replace('app:layout_constraintEnd_toEndOf="parent"', 'app:layout_constraintTop_toTopOf="parent"\n        app:layout_constraintEnd_toEndOf="parent"', 1)
        if name == 'fragment_search':
            text = text.replace('android:columnCount="9"', 'android:columnCount="10"')
        Path(path).write_text(text, encoding='utf-8')
    for name in ('bg_setting_selector', 'sl_button_high_contrast'):
        path = base + 'drawable/' + name + '.xml'
        Path(path).write_bytes(backup.read(path))
    original_sidebar = backup.read('app/src/main/java/com/example/kelkin/utils/SidebarComponent.kt').decode('utf-8-sig')
    print('Original sidebar sizes:', '\n'.join(line.strip() for line in original_sidebar.splitlines() if 'fontSize' in line or '.size(' in line))
    original_adapter = backup.read('app/src/main/java/com/example/kelkin/Adapter/MovieAdapter.kt').decode('utf-8-sig')
    print('Original poster fallback:', '\n'.join(line.strip() for line in original_adapter.splitlines() if '.placeholder(' in line or '.error(' in line))
