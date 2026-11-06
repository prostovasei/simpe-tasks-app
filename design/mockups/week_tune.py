"""Макет «Виджет „Неделя": поправки» (итерация 48, новый холст).

Запрос PM (06.11.2026, по скриншоту виджета): отступ справа от времени в плашке — минимальный; эмодзи списка у задачи — два
варианта (перед названием и после); день недели капсом; дата и день недели мельче; не-сегодняшний день не жирный; меньше отступы
сверху и снизу от даты, у верхнего ряда — ещё меньше; название задачи мельче; даты в календарике крупнее.
Решения PM: эмодзи показываем у всех задач, в том числе у «Входящих»; если эмодзи уже есть в названии — дубль остаётся.
Только виджет, вкладка «Неделя»: приложение не меняется.

Размеры — в dp, шрифты — в sp × FS (масштаб шрифта телефона PM, чтобы макет был похож на скриншот). Цвета — из кода
(ColorTokens, WidgetCheckTokens, WeekRowTokens, res/values/widget_rows.xml).
Запуск: python3 week_tune.py <папка project>   → canvas.json, Main.dc.html, Detail.dc.html
"""
import os, sys, json, datetime
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from sheetlib import ico, P_CAL, P_INBOX

P_WEEK = 'M4 5h16v14H4zM9.3 5v14M14.7 5v14'
P_PLUS = 'M12 5v14M5 12h14'
FS = 1.1  # масштаб шрифта: sp → px макета

# Цвета виджета (светлая тема) — из кода.
INK, T2, BG, SURF, LINE, INACT = '#16171D', '#62656E', '#F5F5F5', '#FFFFFF', '#E2E2E2', '#A9A9A9'
HIGH_BG, HIGH_RING, SOFT, BAND = '#F1E8FA', '#8E4EC6', '#EBEBEB', '#E4E4E4'
WALL = '#C5D8F0'
FONT = "'Roboto',sans-serif"
EMO_FONT = "'Noto Color Emoji',sans-serif"
FONTS_LINK = ('<link rel="preconnect" href="https://fonts.googleapis.com">'
              '<link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&amp;family=Noto+Color+Emoji&amp;display=swap" rel="stylesheet">')

# Было (0.47.0, код) и стало (предложение). Всё в dp / sp.
BEFORE = dict(top=6, pad_t=2, head_h=18, head_fs=12, upper=False, other_w=500, first=2, gap=2, title_fs=12, time_end=8,
              cal_fs=9, cal_box=14)
AFTER = dict(top=2, pad_t=1, head_h=None, head_fs=10, upper=True, other_w=400, first=2, gap=2, title_fs=11, time_end=2,
             cal_fs=11, cal_box=18)

# Списки и задачи — как на скриншоте PM (неделя 2–8 ноября 2026, сегодня пт 6).
WORK, MONEY, ME, SPORT, HOME, INBOX = '💼', '💰', '👤', '🎾', '🏠', '📥'
TODAY = 6
WEEK = [
    ('пн', 2, [('Вес', '10:00', ME, 0, 0), ('Синк', '11:00', WORK, 1, 0)]),
    ('вт', 3, [('Дейлик', '11:00', WORK, 1, 1), ('Финансы', '14:00', MONEY, 0, 1), ('Регулярка', '17:00', WORK, 1, 1)]),
    ('ср', 4, [('Spotify', '10:00', MONEY, 0, 0), ('Дейлик/Погашения', '12:00', WORK, 1, 0), ('Синк/Погашения', '14:00', WORK, 1, 0)]),
    ('чт', 5, [('Девопсы', '14:30', WORK, 0, 1)]),
    ('пт', 6, [('🌐Купить молоко в Ашане', '18:30', INBOX, 0, 0), ('Теннис', '22:00', SPORT, 0, 0)]),
    ('сб', 7, [('Telegram', '10:30', ME, 0, 1), ('Зарплата', '11:30', MONEY, 0, 1), ('Подарки', '12:00', ME, 0, 1),
               ('Бритье', '14:30', ME, 0, 1), ('Теннис', '20:30', SPORT, 0, 1), ('Цветы', '23:00', HOME, 0, 1)]),
    ('вс', 8, [('Стирка', '14:30', HOME, 0, 1), ('Уборка', '16:00', HOME, 0, 1), ('Падел', '20:00', SPORT, 0, 0)]),
]
CAP = 6
WIDGET_W, WIDGET_H, BAR = 396, 640, 40


def sp(v):
    return round(v * FS, 2)


def emo(e, size):
    return f'<span style="font-family:{EMO_FONT};font-size:{size}px;line-height:1;font-weight:400;">{e}</span>'


def repeat_icon(color, size=13):
    """Знак повтора (как ic_repeat_round): круг из двух половин со стрелками посередине боковых сторон."""
    c, g, r, k = size / 2, 1.2, size / 2 - 2, 1.6
    L, R = c - r, c + r
    d = (f'M{L} {c - g}A{r} {r} 0 0 1 {R} {c - g}M{R - k} {c - g - k}L{R} {c - g}L{R + k} {c - g - k}'
         f'M{R} {c + g}A{r} {r} 0 0 1 {L} {c + g}M{L - k} {c + g + k}L{L} {c + g}L{L + k} {c + g + k}')
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 {size} {size}" fill="none" stroke="{color}" stroke-width="1.6" '
            f'stroke-linecap="round" stroke-linejoin="round" style="overflow:visible" aria-hidden="true"><path d="{d}"/></svg>')


def ring(color, size=13):
    return f'<span style="width:{size - 3}px;height:{size - 3}px;border-radius:50%;border:1.5px solid {color};display:block;"></span>'


def slot(task, P, mode, h):
    """Строка задачи. mode: None — без эмодзи (было), 'A' — эмодзи перед названием, 'B' — после."""
    title, time, e, high, rep = task
    color = HIGH_RING if high else INACT
    mark = repeat_icon(color) if rep else ring(color)
    fs = sp(P['title_fs'])
    if mode == 'A':
        text = f'{emo(e, fs)} {title}'
    elif mode == 'B':
        text = f'{title} {emo(e, fs)}'
    else:
        text = title
    return (f'<div style="height:{h}px;margin-top:{P["gap"]}px;border-radius:8px;background:{HIGH_BG if high else SURF};'
            f'display:flex;align-items:center;min-width:0;">'
            f'<span style="width:18px;height:100%;display:flex;align-items:center;justify-content:center;flex-shrink:0;">{mark}</span>'
            f'<span style="flex:1;min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:{fs}px;font-weight:700;color:{INK};">{text}</span>'
            f'<span style="margin-left:4px;margin-right:{P["time_end"]}px;font-size:{sp(10)}px;color:{T2};flex-shrink:0;">{time}</span></div>')


def head(wd, num, P):
    today = num == TODAY
    w = 700 if today else P['other_w']
    label = wd.upper() if P['upper'] else wd
    fs = sp(P['head_fs'])
    hh = f'height:{P["head_h"]}px;' if P['head_h'] else f'line-height:{round(fs * 1.17, 1)}px;'
    return (f'<div style="display:flex;align-items:center;{hh}font-size:{fs}px;">'
            f'<span style="font-weight:{w};color:{INK if today else T2};">{label}</span>'
            f'<span style="font-weight:{w};color:{INK if today else T2};margin-left:4px;">{num}</span></div>')


def cell(day, P, mode, row_h, top_row, right):
    wd, num, tasks = day
    head_px = P['head_h'] or round(sp(P['head_fs']) * 1.17, 1)
    slot_h = (row_h - P['pad_t'] - 2 - head_px - CAP * P['gap']) / CAP
    slots = ''.join(slot(t, P, mode, slot_h) for t in tasks)
    return (f'<div style="flex:1;min-width:0;height:{row_h}px;box-sizing:border-box;padding:{P["pad_t"]}px 4px 2px 4px;overflow:hidden;'
            f'{"border-right:1px solid " + LINE + ";" if right else ""}border-bottom:1px solid {LINE};">'
            f'{head(wd, num, P)}{slots}</div>')


def calendar(P, row_h):
    days = [26, 27, 28, 29, 30, 31] + list(range(1, 31)) + [1, 2, 3, 4, 5, 6]
    out_month = lambda i: i < 6 or i >= 36
    fs, box = sp(P['cal_fs']), P['cal_box']
    lab = ''.join(f'<span style="flex:1;text-align:center;font-size:{sp(9)}px;color:{T2};">{w}</span>' for w in ['пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'])
    rows = ''
    rh = (row_h - 4 - 12) / 6
    for r in range(6):
        cells = ''
        for c in range(7):
            i = r * 7 + c
            n = days[i]
            today = i == 10
            col = '#FFFFFF' if today else (INACT if out_month(i) else INK)
            bg = f'background:{INK};border-radius:50%;' if today else ''
            cells += (f'<span style="flex:1;display:flex;align-items:center;justify-content:center;">'
                      f'<span style="width:{box}px;height:{box}px;{bg}display:flex;align-items:center;justify-content:center;'
                      f'font-size:{fs}px;font-weight:{700 if not out_month(i) else 400};color:{col};">{n}</span></span>')
        band = f'background:{BAND};border-radius:{rh / 2}px;' if r == 1 else ''
        rows += f'<div style="height:{rh}px;display:flex;{band}">{cells}</div>'
    return (f'<div style="flex:1;min-width:0;height:{row_h}px;box-sizing:border-box;padding:2px 4px;border-bottom:1px solid {LINE};">'
            f'<div style="height:12px;display:flex;align-items:center;">{lab}</div>{rows}</div>')


def bar():
    tab = lambda p, on: (f'<span style="width:50px;height:30px;border-radius:11px;background:{SOFT if on else "transparent"};display:flex;'
                         f'align-items:center;justify-content:center;">{ico(p, INK if on else "#4A4D55", 22, 2)}</span>')
    check = (f'<svg width="24" height="20" viewBox="0 0 43 36" aria-hidden="true"><path d="M5 19L16 30L38 7" stroke="{INK}" '
             f'stroke-width="6" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>')
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:{BAR}px;border-top:1px solid {LINE};display:flex;'
            f'align-items:center;padding:0 8px;gap:4px;background:{BG};">'
            f'{tab(P_CAL, False)}{tab(P_INBOX, False)}{tab(P_WEEK, True)}<span style="flex:1"></span>'
            f'<span style="width:50px;height:32px;display:flex;align-items:center;justify-content:center;margin-right:6px;">{check}</span>'
            f'<span style="width:64px;height:32px;border-radius:12px;background:{INK};display:flex;align-items:center;justify-content:center;">'
            f'{ico(P_PLUS, "#FFFFFF", 20, 2.4)}</span></div>')


def widget(P, mode):
    row_h = (WIDGET_H - P['top'] - BAR) / 4
    rows = ''
    for r in range(4):
        left = WEEK[r * 2]
        right = WEEK[r * 2 + 1] if r * 2 + 1 < len(WEEK) else None
        rc = cell(right, P, mode, row_h, r == 0, False) if right else calendar(P, row_h)
        rows += f'<div style="display:flex;">{cell(left, P, mode, row_h, r == 0, True)}{rc}</div>'
    return (f'<div style="position:relative;width:{WIDGET_W}px;height:{WIDGET_H}px;border-radius:16px;overflow:hidden;background:{BG};'
            f'font-family:{FONT};"><div style="padding:{P["top"]}px 0 0 3px;">{rows}</div>{bar()}</div>')


def phone(P, mode):
    apps = ''.join(f'<span style="width:62px;height:62px;border-radius:19px;background:{c};"></span>'
                   for c in ('#1F2329', '#2AA8E0', '#D6409F', '#3D6FE0'))
    return (f'<div style="position:relative;width:412px;height:860px;border-radius:44px;overflow:hidden;background:{WALL};'
            f'border:8px solid #111;box-sizing:content-box;font-family:{FONT};">'
            f'<div style="height:34px;display:flex;align-items:center;justify-content:space-between;padding:0 26px;font-size:14px;'
            f'font-weight:500;color:#FFFFFF;"><span>14:12</span><span>47%</span></div>'
            f'<div style="position:absolute;left:8px;top:46px;">{widget(P, mode)}</div>'
            f'<div style="position:absolute;left:0;right:0;bottom:44px;display:flex;justify-content:space-around;padding:0 18px;">{apps}</div></div>')


def page(title, W, H, body):
    return f'''<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<title>{title}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
{FONTS_LINK}
<style>body{{margin:0}}</style>
</helmet>
<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EFEFEF;padding:36px 40px;font-family:{FONT};color:{INK};">
{body}
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script>
</body>
</html>'''


def column(label, sub, inner):
    return (f'<div style="width:428px;"><div style="font-size:18px;font-weight:700;margin-bottom:4px;">{label}</div>'
            f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;margin-bottom:10px;">{sub}</div>{inner}</div>')


def main_board():
    cols = [
        ('1. Было (0.47.0)', 'Как сейчас на телефоне: дата 12 и средней жирности, строка даты 18 dp, название 12, время в 8 dp от края, '
                             'календарик 9.', phone(BEFORE, None)),
        ('2. Вариант А — эмодзи перед названием', 'Все поправки + эмодзи списка в начале строки. У длинного названия эмодзи '
                                                  'всегда видно, обрезается текст.', phone(AFTER, 'A')),
        ('3. Вариант Б — эмодзи после названия', 'Все поправки + эмодзи сразу после названия. У длинного названия эмодзи '
                                                 'уходит под «…» вместе с концом текста.', phone(AFTER, 'B')),
    ]
    W, H = 3 * 428 + 2 * 30 + 80, 1130
    body = (f'<div style="font-size:28px;font-weight:700;letter-spacing:-.4px;">Виджет «Неделя»: поправки — итерация 48</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Время у края плашки · день недели капсом · '
            f'дата мельче, не-сегодня обычным шрифтом · меньше отступов вокруг даты · название мельче · календарик крупнее · '
            f'эмодзи списка у каждой задачи (в том числе «Входящие» 📥).</div>'
            f'<div style="display:flex;gap:30px;">{"".join(column(*c) for c in cols)}</div>')
    return W, H, page('Виджет «Неделя»: было и варианты', W, H, body)


def zoom_cells(P, mode, days, row_h=150):
    w = WIDGET_W / 2
    cells = ''.join(f'<div style="width:{w}px;display:flex;">{cell(WEEK[i], P, mode, row_h, i < 2, False)}</div>' for i in days)
    return (f'<div style="width:{len(days) * w}px;transform:scale(1.6);transform-origin:0 0;">'
            f'<div style="display:flex;background:{BG};border-radius:12px;overflow:hidden;padding-top:{P["top"]}px;">{cells}</div></div>')


def detail_board():
    W, H = 1400, 1000
    box = lambda t, inner, h: (f'<div style="margin-bottom:26px;"><div style="font-size:14px;font-weight:700;color:{T2};margin-bottom:8px;">{t}</div>'
                               f'<div style="height:{h}px;">{inner}</div></div>')
    rows = [
        ('1', 'Отступ справа от времени', '8 dp', '2 dp'),
        ('2', 'Эмодзи списка', 'нет', 'А — перед названием · Б — после'),
        ('3', 'День недели', 'чт 5', 'ЧТ 5'),
        ('4', 'Шрифт даты и дня недели', '12', '10'),
        ('5', 'Не-сегодняшний день', 'средний', 'обычный (сегодня — жирный)'),
        ('6', 'Отступ над датой / строка даты', '2 dp / 18 dp', '1 dp / по высоте текста (~13 dp)'),
        ('7', 'Верхний ряд: от края виджета до даты', '6 + 2 dp', '2 + 1 dp'),
        ('8', 'Шрифт названия задачи', '12', '11'),
        ('9', 'Даты в календарике', '9, кружок «сегодня» 14 dp', '11, кружок 18 dp'),
    ]
    td = 'padding:9px 12px;border-bottom:1px solid #DDDDDD;font-size:14px;'
    table = (f'<table style="border-collapse:collapse;width:560px;background:#FFFFFF;border-radius:12px;overflow:hidden;">'
             f'<tr><th style="{td}text-align:left;color:{T2};">№</th><th style="{td}text-align:left;color:{T2};">Что</th>'
             f'<th style="{td}text-align:left;color:{T2};">Было</th><th style="{td}text-align:left;color:{T2};">Стало</th></tr>'
             + ''.join(f'<tr><td style="{td}color:{T2};">{a}</td><td style="{td}">{b}</td><td style="{td}color:{T2};">{c}</td>'
                       f'<td style="{td}font-weight:700;">{d}</td></tr>' for a, b, c, d in rows)
             + '</table>')
    left = (box('Верхний ряд крупно — было', zoom_cells(BEFORE, None, [0, 1]), 250)
            + box('Стало, вариант А', zoom_cells(AFTER, 'A', [0, 1]), 250)
            + box('Стало, вариант Б', zoom_cells(AFTER, 'B', [0, 1]), 250))
    right = (f'{table}<div style="font-size:13px;color:{T2};line-height:19px;width:560px;margin-top:16px;">'
             f'Эмодзи — у всех задач, включая «Входящие» (📥). Если эмодзи уже есть в названии (🌐Купить молоко) — показываем оба, '
             f'без отдельной логики. Строк в ячейке по-прежнему до 6; освободившаяся высота уходит строкам задач. '
             f'Тёмная тема — те же размеры. В приложении ничего не меняется.</div>')
    body = (f'<div style="font-size:28px;font-weight:700;letter-spacing:-.4px;margin-bottom:22px;">Детали: было и стало</div>'
            f'<div style="display:flex;gap:60px;"><div style="width:660px;">{left}</div><div>{right}</div></div>')
    return W, H, page('Виджет «Неделя»: детали', W, H, body)


if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    W1, H1, main = main_board()
    W2, H2, det = detail_board()
    open(os.path.join(out, 'Main.dc.html'), 'w').write(main)
    open(os.path.join(out, 'Detail.dc.html'), 'w').write(det)
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {
        'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Виджет «Неделя» — итерация 48',
        'launch': {'view': 'canvas'}, 'pages': [],
        'boards': {
            'Main.dc.html': {'x': 0, 'y': 0, 'w': W1, 'h': H1, 'title': 'Было · вариант А · вариант Б'},
            'Detail.dc.html': {'x': W1 + 80, 'y': 0, 'w': W2, 'h': H2, 'title': 'Детали: было и стало'},
        },
        'order': ['Main.dc.html', 'Detail.dc.html'],
        'notes': {'head': {'x': 0, 'y': -300, 'text': 'Виджет «Неделя»: поправки (итерация 48)', 'kind': 'title1', 'maxW': W1 + 80 + W2}},
        'designSystems': [],
    }
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
