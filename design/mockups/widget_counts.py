"""Макет «Виджет: без подложки внизу и число задач у каждого дня» (итерация 43, решения PM: 1Б, 2а).

1. Нижний градиент убран: кнопки «Все/Входящие», «Обновить» и «+» лежат прямо на ленте, задачи уходят под них.
2. У каждого дня справа — число задач серым (у «Сегодня» остаётся «1 из 4» с полоской).
Телефон 412×860 (dp 1:1). Запуск: python3 widget_counts.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CAL, P_INBOX
from tasks_polish import A, INK, T2, BG, SEL, LINE, OVD, INACT, PRI, row, timeline, phone, status

WALL = 'linear-gradient(165deg,#C5D8F0 0%,#BFD2EE 55%,#9FB8DE 100%)'
P_DL = 'M12 4v10M8 10l4 4 4-4M5 19h14'


def day(label, chip='', red=False, first=False, count=None, progress=None):
    """Подпись дня: слева «ПН, 02 ноя» и метка, справа — число задач (или «1 из 4» с полоской у «Сегодня»)."""
    c = OVD if red else INK
    ch = (f'<span style="font-size:12px;font-weight:700;color:{c if red else INK};background:#fff;border-radius:8px;padding:3px 8px;margin-left:8px;">{chip}</span>'
          if chip else '')
    right = ''
    if progress:
        done, total = progress
        right = (f'<span style="flex:1;height:4px;border-radius:4px;background:{LINE};margin:0 8px 0 8px;position:relative;">'
                 f'<span style="position:absolute;left:0;top:0;bottom:0;width:{100 * done // total}%;border-radius:4px;background:{A};"></span></span>'
                 f'<span style="font-size:12px;font-weight:600;color:{T2};padding-right:8px;">{done} из {total}</span>')
    elif count is not None:
        right = f'<span style="flex:1"></span><span style="font-size:12px;font-weight:600;color:{T2};padding-right:8px;">{count}</span>'
    return f'<div style="display:flex;align-items:center;padding:{2 if first else 14}px 0 0 12px;font-size:15px;font-weight:700;color:{c};">{label}{ch}{right}</div>'


def feed(counts):
    """Лента как в виджете на телефоне PM; counts — показывать ли число у дня."""
    n = (lambda k: k) if counts else (lambda k: None)
    r = day('ПТ, 30 окт', 'Сегодня', first=True, progress=(1, 4))  # у «Сегодня» прогресс есть и сейчас
    r += row('Финансы', '09:00', ring=PRI['high'], done=True, rem='now')
    r += row('Звонок маме', '12:00', ring=INACT, rep=True)
    r += row('Монтаж', '14:00', ring=OVD)
    r += row('Бритьё', '18:30', ring=INACT, rep=True, rem='early')
    r += day('ВС, 01 ноя', 'Завтра', count=n(3))
    r += row('Дзен', '10:00', ring=INACT, rep=True)
    r += row('Стирка', '14:30', ring=INACT, rep=True)
    r += row('Уборка', '16:00', ring=INACT, rep=True, rem='now')
    r += day('ПН, 02 ноя', count=n(2))
    r += row('Вес', '10:00', ring=INACT, rep=True, rem='now')
    r += row('Синк', '11:00', ring=PRI['mid'], rep=True)
    r += day('ВТ, 03 ноя', count=n(4))
    r += row('Дейлик', '11:00', ring=PRI['mid'], rep=True)
    r += row('Финансы', '14:00', ring=INACT, rep=True, rem='now')
    r += row('Регулярка', '17:00', ring=PRI['mid'], rep=True)
    r += row('Цветы', '23:00', ring=INACT, rep=True, rem='early')
    return timeline(r)


def buttons():
    sw = (f'<span style="height:44px;border-radius:16px;background:#fff;display:flex;align-items:center;padding:0 4px;gap:2px;">'
          f'<span style="width:40px;height:36px;border-radius:12px;background:{SEL};display:flex;align-items:center;justify-content:center;">{ico(P_CAL, A, 20, 2)}</span>'
          f'<span style="width:40px;height:36px;display:flex;align-items:center;justify-content:center;">{ico(P_INBOX, "#6B7280", 20, 2)}</span></span>')
    upd = (f'<span style="width:44px;height:44px;border-radius:15px;background:#fff;display:flex;align-items:center;justify-content:center;margin-right:8px;">'
           f'{ico(P_DL, "#6B7280", 22, 2)}</span>')
    plus = (f'<span style="width:50px;height:44px;border-radius:15px;background:{A};display:flex;align-items:center;justify-content:center;">'
            f'{ico("M12 5v14M5 12h14", "#fff", 22, 2.4)}</span>')
    return f'<div style="position:absolute;z-index:3;left:10px;right:10px;bottom:10px;display:flex;align-items:center;">{sw}<span style="flex:1"></span>{upd}{plus}</div>'


def screen(after):
    # Было: переход от прозрачного к фону высотой 70 под кнопками. Стало: перехода нет — только кнопки на ленте.
    fade = '' if after else f'<div style="position:absolute;z-index:2;left:0;right:0;bottom:0;height:70px;background:linear-gradient(rgba(246,247,246,0),{BG} 60%);"></div>'
    wg = (f'<div style="position:absolute;left:10px;right:10px;top:46px;height:598px;border-radius:30px;overflow:hidden;background:{BG};">'
          f'<div style="padding:12px 0 0 0;">{feed(after)}</div>{fade}{buttons()}</div>')
    apps = ''.join(f'<span style="width:62px;height:62px;border-radius:19px;background:{c};"></span>' for c in ('#F6D90A', '#2AA8E0', '#D6409F', '#3D6FE0'))
    return phone(WALL, status('#fff') + wg + f'<div style="position:absolute;left:0;right:0;bottom:52px;display:flex;justify-content:space-around;padding:0 18px;">{apps}</div>')


def legend():
    """Строки дней крупно: какие бывают и где число."""
    cases = [
        ('Сегодня — как было', day('ПТ, 30 окт', 'Сегодня', first=True, progress=(1, 4))),
        ('Завтра: метка и число', day('ВС, 01 ноя', 'Завтра', first=True, count=3)),
        ('Обычный день', day('ПН, 02 ноя', first=True, count=12)),
        ('Прошедший день — красным, число серое', day('ЧТ, 29 окт', 'Вчера', red=True, first=True, count=2)),
        ('Без даты', day('Без даты', first=True, count=5)),
    ]
    out = ''.join(f'<div style="margin-bottom:14px;"><div style="font-size:12px;color:{T2};font-weight:600;padding-left:12px;">{t}</div>{d}</div>' for t, d in cases)
    note = (f'<div style="font-size:13px;color:{T2};line-height:19px;padding:6px 12px 0;">Число — все задачи дня, считая выполненные и будущие повторы, '
            f'как плашки в ленте. Серый (text-secondary), 12 sp, полужирный; прижато к правому краю плашек.</div>')
    return (f'<div style="width:412px;border-radius:28px;background:{BG};padding:22px 0 18px;box-sizing:border-box;font-family:Manrope,sans-serif;'
            f'border:8px solid #DDE3DE;">{out}{note}</div>')


SCREENS = [
    ('1. Сейчас', 'Внизу полупрозрачная полоса под кнопками; у дней, кроме сегодняшнего, числа нет.', lambda: screen(False)),
    ('2. Стало', 'Полосы нет: кнопки лежат прямо на ленте, задачи уходят под них. У каждого дня справа — число задач.', lambda: screen(True)),
    ('3. Строки дней', 'Где стоит число и как выглядит у разных дней.', legend),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    cell = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    rows = f'<div style="display:flex;gap:30px;">{"".join(cell(*s) for s in SCREENS)}</div>'
    W, H = 3 * 428 + 2 * 30 + 80, 1130
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Виджет: без полосы внизу и число задач у каждого дня</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Решения PM: задачи уходят под кнопки (вариант «Б»); число — просто цифра (вариант «а»).</div>{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Виджет: число задач</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantW1.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    fresh = 'VariantW1.dc.html' not in c['boards']
    old = {k: v for k, v in c['boards'].items() if k != 'VariantW1.dc.html'}
    if fresh:  # повторный запуск не сдвигает прежние доски ещё раз
        for v in old.values():
            v['y'] += H + 120
    c['boards'] = {'VariantW1.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Виджет: число задач у дня, без полосы внизу (итерация 43)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Виджет: число задач у дня, без полосы внизу (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
