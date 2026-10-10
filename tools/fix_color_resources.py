from pathlib import Path
src = Path('app/src/main/res/drawable/sl_text_high_contrast.xml')
dst = Path('app/src/main/res/color/sl_text_high_contrast.xml')
dst.parent.mkdir(exist_ok=True)
dst.write_text(src.read_text(encoding='utf-8'), encoding='utf-8')
src.unlink()
for folder in ('app/src/main/java', 'app/src/main/res'):
    for path in Path(folder).rglob('*'):
        if path.suffix not in ('.kt', '.xml'): continue
        original = path.read_text(encoding='utf-8')
        changed = original.replace('R.drawable.sl_text_high_contrast', 'R.color.sl_text_high_contrast').replace('@drawable/sl_text_high_contrast', '@color/sl_text_high_contrast')
        if changed != original: path.write_text(changed, encoding='utf-8')
for name in ('fragment_movies.xml', 'fragment_series.xml', 'fragment_tv.xml'):
    path = Path('app/src/main/res/layout') / name
    text = path.read_text(encoding='utf-8')
    # The top filter/header row originally had only horizontal constraints.
    text = text.replace('app:layout_constraintEnd_toEndOf="parent"', 'app:layout_constraintTop_toTopOf="parent"\n        app:layout_constraintEnd_toEndOf="parent"', 1)
    path.write_text(text, encoding='utf-8')
Path('local.properties').write_text('sdk.dir=C\\:/Users/javad/AppData/Local/Android/Sdk\n', encoding='utf-8')
