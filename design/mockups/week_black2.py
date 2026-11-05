"""Макет «Виджет: поправки 0.46.0» (доска VariantK3, итерация 45).

Решения PM после проверки build193: нижняя строка виджета ниже; строка даты в ячейке — шрифт меньше и меньше отступы по вертикали;
сегодняшний день без кружка — просто жирнее остальных; у остальных дней жирность ниже. Только виджет: приложение не меняется.
Запуск: python3 week_black2.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
import week_black as wb
from week_black import LIGHT, DARK, ico, P_CAL, P_INBOX, P_WEEK, P_PLUS, WALL, phone, status

BAR_OLD, BAR_NEW = 48, 40


def head_new(C, wd, num, hidden=0, hh=18):
    """Строка даты (стало): 12 px, высота 18. Сегодня — день недели и число жирные (800) основного цвета; остальные дни — обычные (500 / 400)."""
    plus = (f'<span style="margin-left:auto;font-size:10.5px;font-weight:700;color:{C["INK"]};background:{C["SOFT"]};border-radius:999px;padding:0 7px;line-height:15px;">+{hidden}</span>' if hidden else '')
    today = num == wb.TODAY
    w = (f'<span style="font-size:12px;font-weight:{800 if today else 500};color:{C["INK"]};">{wd}</span>'
         f'<span style="font-size:12px;font-weight:{800 if today else 400};color:{C["INK"] if today else C["T2"]};margin-left:5px;">{num}</span>')
    return f'<div style="display:flex;align-items:center;height:{hh}px;">{w}{plus}</div>'


def widget_bar(C, h, tab_h, add_h, icon=22):
    tab = lambda p, on: (f'<span style="width:50px;height:{tab_h}px;border-radius:11px;background:{C["SEL"] if on else "transparent"};display:flex;align-items:center;justify-content:center;">'
                         f'{ico(p, C["INK"] if on else C["NAVI"], icon, 2)}</span>')
    check = f'<svg width="{icon + 2}" height="{icon - 2}" viewBox="0 0 43 36"><path d="M5 19L16 30L38 7" stroke="{C["INK"]}" stroke-width="6" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:{h}px;background:{C["BG"]};border-top:1px solid {C["LINE"]};display:flex;align-items:center;padding:0 8px;gap:4px;">'
            f'{tab(P_CAL, False)}{tab(P_INBOX, False)}{tab(P_WEEK, True)}<span style="flex:1"></span>'
            f'<span style="width:50px;height:{add_h}px;display:flex;align-items:center;justify-content:center;margin-right:6px;">{check}</span>'
            f'<span style="width:64px;height:{add_h}px;border-radius:12px;background:{C["A"]};display:flex;align-items:center;justify-content:center;">{ico(P_PLUS, C["ON_A"], icon - 2, 2.4)}</span></div>')


def screen(C, new):
    H = 680
    bar = BAR_NEW if new else BAR_OLD
    row = (H - 6 - bar - 1) // 4
    old_head = wb.head
    if new:
        wb.head = head_new
    try:
        grid = wb.grid(C, row, 6, cal_rows=19, h=20, fs=12, ts=10.5, gap=2)
    finally:
        wb.head = old_head
    if new:
        grid = grid.replace('padding:6px 6px;border-right', 'padding:3px 6px;border-right')
    b = widget_bar(C, bar + 1, 30, 32) if new else widget_bar(C, bar + 1, 36, 40, 24)
    wg = (f'<div style="position:absolute;left:10px;right:10px;top:60px;height:{H}px;border-radius:26px;overflow:hidden;background:{C["BG"]};">'
          f'<div style="padding-top:6px;">{grid}</div>{b}</div>')
    apps = ''.join(f'<span style="width:62px;height:62px;border-radius:19px;background:{c};"></span>' for c in ('#F6D90A', '#2AA8E0', '#D6409F', '#3D6FE0'))
    return phone(WALL, status('#fff') + wg + f'<div style="position:absolute;left:0;right:0;bottom:26px;display:flex;justify-content:space-around;padding:0 18px;">{apps}</div>')


def detail():
    C = LIGHT
    box = lambda t, inner: (f'<div style="margin-bottom:16px;"><div style="font-size:12px;font-weight:700;color:{C["T2"]};margin-bottom:4px;">{t}</div>'
                            f'<div style="width:412px;background:{C["BG"]};border:1px solid {C["LINE"]};border-radius:14px;padding:10px;box-sizing:border-box;">{inner}</div></div>')
    old = f'<div style="width:200px;">{wb.head(C, "чт", 5)}{wb.head(C, "сб", 7, hidden=1)}{wb.head(C, "пн", 2)}</div>'
    new = f'<div style="width:200px;">{head_new(C, "чт", 5)}{head_new(C, "сб", 7, hidden=1)}{head_new(C, "пн", 2)}</div>'
    bars = (f'<div style="position:relative;height:{BAR_OLD + 1}px;">{widget_bar(C, BAR_OLD + 1, 36, 40, 24)}</div><div style="height:10px"></div>'
            f'<div style="position:relative;height:{BAR_NEW + 1}px;">{widget_bar(C, BAR_NEW + 1, 30, 32)}</div>')
    return (box('Строка даты: было (сегодня в кружке, 14 px, высота 22)', old) + box('Стало: 12 px, высота 18; сегодня — жирное, без кружка; остальные — обычные', new)
            + box(f'Нижняя строка: было {BAR_OLD} → стало {BAR_NEW}', bars)
            + f'<div style="font-size:13px;color:{C["T2"]};line-height:19px;width:412px;">Освободившаяся высота уходит строкам задач: в ячейке по-прежнему до 6, строки чуть выше. '
              f'Нижняя строка одна на все вкладки виджета — на «Все» и «Входящие» тоже станет ниже. В приложении ничего не меняется.</div>')


SCREENS = [
    ('1. Было (build193)', 'Сегодня — число в чёрном кружке, строка даты 14 px, нижняя строка 48.', lambda: screen(LIGHT, False)),
    ('2. Стало', 'Строка даты меньше и ниже, сегодня просто жирнее, остальные дни — обычным шрифтом. Нижняя строка 40.', lambda: screen(LIGHT, True)),
    ('3. Стало — тёмная тема', 'То же в тёмной теме.', lambda: screen(DARK, True)),
    ('4. Детали', 'Строка даты и нижняя строка до и после.', detail),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:#16171D;margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:#62656E;line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    cell_ = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    rows = f'<div style="display:flex;gap:30px;">{"".join(cell_(*s) for s in SCREENS)}</div>'
    W, H = 4 * 428 + 3 * 30 + 80, 1180
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EFEFEF;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:#16171D;letter-spacing:-.5px;">Виджет «Неделя»: поправки после проверки — итерация 45</div>'
            f'<div style="font-size:15px;color:#62656E;margin:6px 0 22px;line-height:21px;">Решения PM: нижняя строка ниже; строка даты — шрифт меньше, меньше отступы; '
            f'сегодня без кружка, просто жирнее; остальные дни — тоньше.</div>{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Виджет: поправки</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantK3.dc.html'), 'w').write(html)
    cj = os.path.join(OUT, 'canvas.json')
    if os.path.exists(cj):
        c = json.load(open(cj))
        fresh = 'VariantK3.dc.html' not in c['boards']
        old = {k: v for k, v in c['boards'].items() if k != 'VariantK3.dc.html'}
        if fresh:
            for v in old.values():
                v['y'] += H + 120
        c['boards'] = {'VariantK3.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Виджет «Неделя»: поправки (итерация 45)'}, **old}
        c['order'] = list(c['boards'])
        c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Виджет «Неделя»: поправки (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
        json.dump(c, open(cj, 'w'), ensure_ascii=False, indent=2)
