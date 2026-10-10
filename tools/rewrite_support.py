from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/com/example/kelkin'

def read(path):
    return (ROOT / path).read_text(encoding='utf-8-sig')

def write(path, text):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding='utf-8')

def edit(path, old, new, count=-1):
    text = read(path)
    if old not in text:
        raise ValueError(f'Missing edit anchor in {path}: {old[:100]}')
    write(path, text.replace(old, new, count))

def method(path, name, replacement):
    text = read(path)
    match = re.search(r'(?m)^    (?:private |override |suspend |internal )*fun '+re.escape(name)+r'\(', text)
    if not match:
        raise ValueError(f'Missing method {name}: {path}')
    start = match.start()
    # All replaced methods end at the class indentation; nested declarations are indented further.
    end = text.index('\n    }', match.end()) + len('\n    }')
    write(path, text[:start] + replacement.strip('\n') + text[end:])

def imports(path, imports):
    text = read(path)
    position = text.index('\n') + 1
    write(path, text[:position]+'\n'+''.join('import '+i+'\n' for i in imports if 'import '+i+'\n' not in text)+text[position:])
