"""Общий каркас страниц макетов: токены дизайн-системы (светлая и тёмная темы) и базовые стили.

Подключается из макетов итераций (`iteration39.py`…): `from ds_page import tokens_css, CSS, note`.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'system'))
_argv = sys.argv
sys.argv = [sys.argv[0], '/tmp/ds_scratch']  # build_design_system читает путь вывода из argv
import build_design_system as B  # noqa: E402
import ds_more as D  # noqa: E402
sys.argv = _argv


def tokens_css():
    t = B.build_tokens()
    light, dark = [], []
    for c in t['color']['tokens']:
        light.append(f"--{c['name']}:{c['value']['light']}")
        dark.append(f"--{c['name']}:{c['value']['dark']}")
    common = []
    for group in ('spacing', 'radius', 'size'):
        for s in t[group]['tokens']:
            common.append(f"--{s['name']}:{s['value']}")
    common.append('--font-sans:"Manrope",system-ui,-apple-system,"Segoe UI",sans-serif')
    return ':root{' + ';'.join(light + common) + ';--accent-soft:color-mix(in srgb,var(--accent) 18%,transparent)}' \
        '@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){' + ';'.join(dark) + ';color-scheme:dark}}' \
        ':root[data-theme="dark"]{' + ';'.join(dark) + ';color-scheme:dark}'


CSS = '''
*{box-sizing:border-box}
body{margin:0;background:var(--background);color:var(--text-primary);font-family:var(--font-sans);font-size:14px;line-height:20px;
 padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px));padding-block:28px 56px}
main{max-width:1000px;margin:0 auto;display:flex;flex-direction:column;gap:48px}
h1{font-size:28px;line-height:36px;font-weight:800;margin:0 0 8px}
h2{font-size:22px;line-height:28px;font-weight:700;margin:0}
p{margin:0;color:var(--text-secondary);font-size:15px;line-height:22px;max-width:64ch}
.tag{display:inline-block;font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:var(--accent);background:var(--selected);border-radius:4px;padding:2px 8px;align-self:flex-start}
section{display:flex;flex-direction:column;gap:18px}
.head{display:flex;flex-direction:column;gap:6px}
.screens{display:flex;flex-wrap:wrap;gap:28px;align-items:flex-start}
.screen{display:flex;flex-direction:column;gap:10px;max-width:100%}
.cap{font-size:12px;font-weight:700;letter-spacing:.03em;color:var(--text-secondary);text-transform:uppercase}
.note{width:412px;max-width:100%;font-size:14px;line-height:20px;color:var(--text-primary);background:var(--selected);border-radius:var(--radius-lg);padding:10px 14px}
.rules{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px}
.rules div{background:var(--surface);border:1px solid var(--divider);border-radius:var(--radius-lg);padding:14px 16px;display:flex;flex-direction:column;gap:4px}
.rules b{font-weight:700}.rules span{color:var(--text-secondary)}
.scroll{overflow-x:auto;max-width:100%}
a{color:var(--accent);font-weight:600}
'''


def note(t):
    return f'<div class="note">{t}</div>'
