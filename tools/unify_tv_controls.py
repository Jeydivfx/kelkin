from pathlib import Path
import re
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
res = root / 'app/src/main/res'
selector = '''<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true"><shape><solid android:color="@color/yellow"/><corners android:radius="8dp"/></shape></item>
    <item android:state_pressed="true"><shape><solid android:color="@color/yellow"/><corners android:radius="8dp"/></shape></item>
    <item><shape><solid android:color="@color/btn_default_bg"/><corners android:radius="8dp"/></shape></item>
</selector>'''
for name in ['sl_button_high_contrast', 'bg_dialog_button', 'bg_login_primary', 'bg_login_secondary', 'selector_keyboard_key']:
    (res / f'drawable/{name}.xml').write_text(selector, encoding='utf-8')
(res / 'color/sl_text_high_contrast.xml').write_text('''<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true" android:color="@color/bg_black"/>
    <item android:state_pressed="true" android:color="@color/bg_black"/>
    <item android:color="@color/text_white"/>
</selector>''', encoding='utf-8')

def attr(tag, name, value):
    pattern = rf'\s{re.escape(name)}="[^"]*"'
    if re.search(pattern, tag):
        return re.sub(pattern, f' {name}="{value}"', tag)
    closing = '/>' if tag.endswith('/>') else '>'
    return tag[:-len(closing)] + f' {name}="{value}"' + closing

for p in (res / 'layout').glob('*.xml'):
    s = p.read_text(encoding='utf-8')
    def button(m):
        tag = m.group(0)
        for name, value in [('android:background', '@drawable/sl_button_high_contrast'),
                            ('android:backgroundTint', '@null'), ('android:textColor', '@color/sl_text_high_contrast'),
                            ('android:stateListAnimator', '@null')]:
            tag = attr(tag, name, value)
        if 'xmlns:app=' in s:
            tag = attr(tag, 'app:backgroundTint', '@null')
            if 'app:icon' in tag:
                tag = attr(tag, 'app:iconTint', '@color/sl_text_high_contrast')
        return tag
    s = re.sub(r'<(?:Button|androidx.appcompat.widget.AppCompatButton|com.google.android.material.button.MaterialButton)\b[^>]*>', button, s)
    if p.name == 'activity_login.xml':
        s = s.replace('android:textColor="#17202B"', 'android:textColor="@color/sl_text_high_contrast" android:duplicateParentState="true"')
        s = s.replace('android:background="@drawable/bg_login_primary"', 'android:background="@drawable/sl_button_high_contrast"')
    p.write_text(s, encoding='utf-8')

p = res / 'values/themes.xml'
s = p.read_text(encoding='utf-8').replace('<item name="android:textColor">@color/bg_black</item>', '<item name="android:textColor">@color/sl_text_high_contrast</item>')
s = s.replace('<item name="android:textColorPrimary">@color/text_white</item>', '''<item name="android:textColorPrimary">@color/text_white</item>
        <item name="android:textColorSecondary">@color/text_white</item>
        <item name="android:textColor">@color/text_white</item>
        <item name="textColorAlertDialogListItem">@color/text_white</item>''')
p.write_text(s, encoding='utf-8')

# Media3 controls and popup layouts deliberately retain library defaults.

for p in (res / 'layout').glob('*.xml'):
    ET.parse(p)
print('Control styles updated; all layout XML parsed successfully.')
