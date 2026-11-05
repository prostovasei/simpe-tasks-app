"""Макет «Неделя по рефу PM + чёрный акцент» (итерация 45, доска VariantK2).

Решения PM (интервью): акцент чёрный во всём приложении и виджете (в тёмной теме — белый); фон и разделители нейтрально-серые, без зелени,
иконка тоже; строки «Недели» компактные (в ячейке ~6 задач), фон строки — бледный цвет приоритета, без приоритета — белый;
выполненные — серые и зачёркнутые, серая галочка; сегодня — число в чёрном кружке; «+N» — серая плашка; в мини-календаре нет строки
месяца со стрелками — месяц листается сам, когда выбираешь неделю соседнего месяца; нижняя панель виджета как на рефе
(выбранная вкладка на сером, галочка без плашки, «+» чёрный). Новый вид строк — только в «Неделе».
Телефон 412×860 (dp 1:1). Запуск: python3 week_black.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CAL, P_INBOX, P_CHECK, P_REP, P_BELL
import tasks_polish as tp
from tasks_polish import phone, status, P_GEAR, P_LISTI, P_SEARCH

P_WEEK = 'M4 5h16v14H4zM9.3 5v14M14.7 5v14'
P_PLUS = 'M12 5v14M5 12h14'
WALL = 'linear-gradient(165deg,#C5D8F0 0%,#BFD2EE 55%,#9FB8DE 100%)'

# Новые цвета (светлая / тёмная). Приоритеты-кружки прежние.
LIGHT = dict(A='#16171D', ON_A='#FFFFFF', INK='#16171D', T2='#62656E', BG='#F5F5F5', SURF='#FFFFFF', LINE='#E2E2E2', INACT='#A9A9A9',
             SEL='#E8E8E8', SOFT='#EAEAEA', OVD='#C62828', DONE='#BDBDBD', NAVI='#6B6E76',
             TINT={'high': '#FDECEC', 'mid': '#FDF4E3', 'low': '#EAF2FC', 'none': '#FFFFFF'},
             PRI={'none': '#A9A9A9', 'low': '#2E7DD7', 'mid': '#E08A00', 'high': '#C62828'})
DARK = dict(A='#F2F2F2', ON_A='#16171D', INK='#ECECEC', T2='#A3A3A3', BG='#121212', SURF='#1E1E1E', LINE='#2E2E2E', INACT='#636363',
            SEL='#2C2C2C', SOFT='#2E2E2E', OVD='#FF6B6B', DONE='#5A5A5A', NAVI='#9A9A9A',
            TINT={'high': '#3A2222', 'mid': '#3A2F1C', 'low': '#1D2A3B', 'none': '#1E1E1E'},
            PRI={'none': '#636363', 'low': '#6AA8FF', 'mid': '#FFB74D', 'high': '#FF6B6B'})

# Неделя 2–8 ноября 2026 (сегодня — вторник 3), как на рефе PM. (название, время, приоритет, выполнена, повтор-призрак)
DAYS = [
    ('пн', 2, [('Вес', '10:00', 'none', False), ('Синк', '11:00', 'mid', False)]),
    ('вт', 3, [('Дейлик', '11:00', 'mid', False), ('Финансы', '14:00', 'none', False), ('Регулярка', '17:00', 'mid', False)]),
    ('ср', 4, [('Spotify', '10:00', 'none', False), ('Дейлик/Погашения', '12:00', 'high', False), ('Синк/Погашения', '14:00', 'mid', False)]),
    ('чт', 5, [('Дейлик', '11:00', 'mid', False, True), ('Девопсы', '14:30', 'low', False, True)]),
    ('пт', 6, [('Финансы', '14:00', 'none', True)]),
    ('сб', 7, [('Telegram', '10:30', 'none', False), ('Зарплата', '11:30', 'none', False), ('Подарки', '12:00', 'none', False),
               ('Купить продукты', '13:00', 'mid', False), ('Тренировка', '16:00', 'low', False), ('План на неделю', '19:00', 'none', False),
               ('Стрижка', '20:00', 'none', False)]),
    ('вс', 8, [('Стирка', '14:30', 'none', True), ('Уборка', '16:00', 'none', True)]),
]
TODAY = 3
PAST = (2,)  # понедельник прошёл: невыполненное время — красное


def circle(C, pri, done, size=14):
    if done:
        return (f'<span style="width:{size}px;height:{size}px;border-radius:50%;background:{C["DONE"]};flex-shrink:0;display:flex;align-items:center;justify-content:center;">'
                f'{ico(P_CHECK, C["SURF"], size - 4, 3)}</span>')
    return f'<span style="width:{size}px;height:{size}px;border-radius:50%;border:2px solid {C["PRI"][pri]};box-sizing:border-box;flex-shrink:0;"></span>'


def task(C, title, time, pri, done, ghost=False, past=False, h=22, fs=13, ts=11, gap=3):
    """Компактная строка: фон — бледный цвет приоритета (без приоритета — белый); выполненная — серая и зачёркнутая; призрак повтора — 50%."""
    bg = C['TINT']['none'] if done else C['TINT'][pri]
    tc = C['INACT'] if done else C['INK']
    st = 'text-decoration:line-through;' if done else ''
    op = 'opacity:.5;' if ghost else ''
    timec = C['OVD'] if (past and not done) else (C['INACT'] if done else C['T2'])
    t = f'<span style="font-size:{ts}px;font-weight:600;color:{timec};margin-left:6px;">{time}</span>' if time else ''
    return (f'<div style="height:{h}px;margin-top:{gap}px;border-radius:7px;background:{bg};display:flex;align-items:center;gap:6px;padding:0 6px;box-sizing:border-box;{op}">'
            f'{circle(C, pri, done, 13)}<span style="flex:1;min-width:0;font-size:{fs}px;font-weight:700;color:{tc};white-space:nowrap;overflow:hidden;text-overflow:ellipsis;{st}">{title}</span>{t}</div>')


def head(C, wd, num, hidden=0, hh=24):
    plus = (f'<span style="margin-left:auto;font-size:11.5px;font-weight:800;color:{C["INK"]};background:{C["SOFT"]};border-radius:999px;padding:1px 8px;">+{hidden}</span>' if hidden else '')
    if num == TODAY:
        n = (f'<span style="width:22px;height:22px;border-radius:50%;background:{C["A"]};color:{C["ON_A"]};font-size:12px;font-weight:800;margin-left:6px;'
             f'display:flex;align-items:center;justify-content:center;">{num}</span>')
    else:
        n = f'<span style="font-size:13px;font-weight:600;color:{C["T2"]};margin-left:6px;">{num}</span>'
    return f'<div style="display:flex;align-items:center;height:{hh}px;"><span style="font-size:14px;font-weight:800;color:{C["INK"]};">{wd}</span>{n}{plus}</div>'


def cell(C, d, ch, cap, **kw):
    wd, num, tasks = d
    shown, hidden = tasks[:cap], len(tasks) - min(cap, len(tasks))
    body = ''.join(task(C, t[0], t[1], t[2], t[3], ghost=len(t) > 4 and t[4], past=num in PAST, **kw) for t in shown)
    return (f'<div style="flex:1;min-width:0;height:{ch}px;overflow:hidden;box-sizing:border-box;padding:6px 6px;border-right:1px solid {C["LINE"]};border-bottom:1px solid {C["LINE"]};">'
            f'{head(C, wd, num, hidden)}{body}</div>')


def mini_calendar(C, h, rows_h=22, fs=12):
    """Без строки месяца: дни недели и шесть недель. Выбранная неделя — серая полоса, сегодня — чёрный кружок, чужой месяц — бледный."""
    wk = ''.join(f'<span style="flex:1;text-align:center;font-size:10px;font-weight:600;color:{C["INACT"]};">{x}</span>' for x in ('пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'))
    weeks = [[26, 27, 28, 29, 30, 31, 1], [2, 3, 4, 5, 6, 7, 8], [9, 10, 11, 12, 13, 14, 15], [16, 17, 18, 19, 20, 21, 22], [23, 24, 25, 26, 27, 28, 29], [30, 1, 2, 3, 4, 5, 6]]
    out = ''
    for i, w in enumerate(weeks):
        band = f'background:{C["SEL"]};border-radius:{rows_h // 2}px;' if i == 1 else ''
        cells = ''
        for j, n in enumerate(w):
            other = (i == 0 and j < 6) or (i == 5 and j > 0)
            if i == 1 and n == TODAY:
                d = rows_h - 2
                cells += (f'<span style="flex:1;display:flex;justify-content:center;"><span style="width:{d}px;height:{d}px;border-radius:50%;background:{C["A"]};color:{C["ON_A"]};'
                          f'font-size:{fs}px;font-weight:800;display:flex;align-items:center;justify-content:center;">{n}</span></span>')
            else:
                cells += f'<span style="flex:1;text-align:center;font-size:{fs}px;font-weight:600;color:{C["INACT"] if other else C["INK"]};">{n}</span>'
        out += f'<div style="display:flex;align-items:center;height:{rows_h}px;{band}">{cells}</div>'
    return (f'<div style="flex:1;min-width:0;height:{h}px;overflow:hidden;box-sizing:border-box;padding:6px 6px;border-bottom:1px solid {C["LINE"]};display:flex;flex-direction:column;justify-content:space-between;">'
            f'<div style="display:flex;height:16px;align-items:center;">{wk}</div>{out}</div>')


def grid(C, row_h, cap, cal_rows=22, **kw):
    rows = ''
    for i in range(0, 6, 2):
        rows += f'<div style="display:flex;">{cell(C, DAYS[i], row_h, cap, **kw)}{cell(C, DAYS[i + 1], row_h, cap, **kw)}</div>'
    rows += f'<div style="display:flex;">{cell(C, DAYS[6], row_h, cap, **kw)}{mini_calendar(C, row_h, cal_rows)}</div>'
    return rows


def nav_app(C, selected='week'):
    def t(path, key):
        c = C['A'] if key == selected else C['NAVI']
        return f'<span style="flex:1;height:56px;display:flex;align-items:center;justify-content:center;">{ico(path, c, 24, 2)}</span>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:56px;background:{C["SURF"]};border-top:1px solid {C["LINE"]};display:flex;align-items:center;padding:0 8px 0 4px;z-index:2;">'
            f'{t(P_INBOX, "inbox")}{t(P_CAL, "all")}{t(P_WEEK, "week")}{t(P_LISTI, "lists")}{t(P_SEARCH, "search")}{t(P_GEAR, "settings")}'
            f'<span style="width:48px;height:40px;margin-left:4px;border-radius:14px;background:{C["A"]};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(P_PLUS, C["ON_A"], 22, 2.4)}</span></div>')


def screen_app(C):
    # 860 − 34 − 56 = 770: четыре строки по 192 → в ячейке 6 строк (22 + 3).
    inner = (status(C['INK']) + f'<div style="position:absolute;top:34px;left:0;right:0;bottom:56px;overflow:hidden;border-top:1px solid {C["LINE"]};">'
             f'{grid(C, 192, 6, cal_rows=26)}</div>' + nav_app(C))
    return phone(C['BG'], inner)


def widget_bar(C):
    tab = lambda p, on: (f'<span style="width:52px;height:40px;border-radius:12px;background:{C["SEL"] if on else "transparent"};display:flex;align-items:center;justify-content:center;">'
                         f'{ico(p, C["INK"] if on else C["NAVI"], 22, 2)}</span>')
    check = f'<svg width="24" height="20" viewBox="0 0 43 36"><path d="M5 19L16 30L38 7" stroke="{C["INK"]}" stroke-width="6" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:52px;background:{C["BG"]};border-top:1px solid {C["LINE"]};display:flex;align-items:center;padding:0 8px;gap:4px;">'
            f'{tab(P_CAL, False)}{tab(P_INBOX, False)}{tab(P_WEEK, True)}<span style="flex:1"></span>'
            f'<span style="width:52px;height:40px;display:flex;align-items:center;justify-content:center;margin-right:6px;">{check}</span>'
            f'<span style="width:72px;height:40px;border-radius:12px;background:{C["A"]};display:flex;align-items:center;justify-content:center;">{ico(P_PLUS, C["ON_A"], 22, 2.4)}</span></div>')


def screen_widget(C):
    # Виджет 680: (680 − 52) / 4 = 157 → строка даты 24 + 5 строк по 20+2; шестая задача — в «+N».
    wg = (f'<div style="position:absolute;left:10px;right:10px;top:60px;height:680px;border-radius:26px;overflow:hidden;background:{C["BG"]};">'
          f'{grid(C, 157, 5, cal_rows=19, h=20, fs=12, ts=10.5, gap=2)}{widget_bar(C)}</div>')
    apps = ''.join(f'<span style="width:62px;height:62px;border-radius:19px;background:{c};"></span>' for c in ('#F6D90A', '#2AA8E0', '#D6409F', '#3D6FE0'))
    return phone(WALL, status('#fff') + wg + f'<div style="position:absolute;left:0;right:0;bottom:26px;display:flex;justify-content:space-around;padding:0 18px;">{apps}</div>')


def screen_all(C):
    """Обычный экран «Все» — строки прежние (белые плашки), меняются только акцент, фон и разделители."""
    tp.A, tp.INK, tp.T2, tp.BG, tp.INACT, tp.LINE, tp.SEL = C['A'], C['INK'], C['T2'], C['BG'], C['INACT'], C['LINE'], C['SEL']
    PRI = C['PRI']
    rows = (tp.day('Сегодня', '3 задачи', first=True)
            + tp.row('Дейлик', '11:00', ring=PRI['mid'], rep=True) + tp.row('Финансы', '14:00', ring=PRI['none'], rem='at')
            + tp.row('Регулярка', '17:00', ring=PRI['mid'], done=True)
            + tp.day('Завтра') + tp.row('Spotify', '10:00') + tp.row('Дейлик/Погашения', '12:00', ring=PRI['high']))
    top = '<div style="height:8px;"></div>'
    return phone(C['BG'], status(C['INK']) + top + tp.timeline(rows) + nav_app(C, 'all'))


def spec():
    C = LIGHT
    sw = lambda name, old, new: (f'<div style="display:flex;align-items:center;gap:8px;height:30px;font-size:12.5px;color:{C["T2"]};">'
                                 f'<span style="width:22px;height:22px;border-radius:6px;background:{old};border:1px solid #0001;"></span>→'
                                 f'<span style="width:22px;height:22px;border-radius:6px;background:{new};border:1px solid #0001;"></span>'
                                 f'<span style="color:{C["INK"]};font-weight:600;">{name}</span></div>')
    pal = (sw('Акцент (тёмная тема — белый)', '#15803D', '#16171D') + sw('Подсветка выбранного', '#E4F2E8', '#E8E8E8')
           + sw('Фон', '#F6F7F6', '#F5F5F5') + sw('Разделители', '#D5E0D8', '#E2E2E2') + sw('Неактивное', '#A8B3AB', '#A9A9A9'))
    rows = ''.join(task(C, *a) for a in [('Высокий приоритет', '12:00', 'high', False), ('Средний', '11:00', 'mid', False), ('Низкий', '16:00', 'low', False),
                                         ('Без приоритета', '10:30', 'none', False), ('Выполнена', '14:30', 'none', True)])
    rows += task(C, 'Повтор в будущем', '11:00', 'mid', False, ghost=True) + task(C, 'Прошлый день, не сделано', '10:00', 'none', False, past=True)
    heads = head(C, 'вт', 3) + head(C, 'сб', 7, hidden=1) + head(C, 'пн', 2)
    box = lambda title, inner, pad=10: (f'<div style="margin-bottom:16px;"><div style="font-size:12px;font-weight:700;color:{C["T2"]};margin-bottom:4px;">{title}</div>'
                                        f'<div style="width:412px;background:{C["BG"]};border:1px solid {C["LINE"]};border-radius:14px;padding:{pad}px;box-sizing:border-box;">{inner}</div></div>')
    return (box('Цвета: было → стало', pal)
            + box('Строки «Недели»: фон по приоритету, выполненная, повтор, просрочка', f'<div style="width:220px;">{rows}</div>')
            + box('Строка даты: сегодня, «+N», обычный день', f'<div style="width:220px;">{heads}</div>')
            + f'<div style="font-size:13px;color:{C["T2"]};line-height:19px;width:412px;">Месяц в календаре листается сам: нажал на бледную неделю соседнего месяца — она выбирается, '
              f'и календарь показывает её месяц. Строки месяца и стрелок больше нет. Новый вид строк — только в «Неделе», в остальных списках белые плашки остаются.</div>')


SCREENS = [
    ('1. Приложение: «Неделя»', 'Строки ниже, в ячейке 6 задач. Фон строки — бледный цвет приоритета. Сегодня — число в чёрном кружке, «+1» серой плашкой.', lambda: screen_app(LIGHT)),
    ('2. Виджет: «Неделя»', 'Те же строки чуть мельче. Календарь без строки месяца. Нижняя панель: выбранная вкладка на сером, галочка без плашки, «+» чёрный.', lambda: screen_widget(LIGHT)),
    ('3. Тёмная тема', 'Акцент в тёмной теме — белый: кружок «сегодня», «+», выбранное. Фон нейтральный тёмно-серый, без зелени.', lambda: screen_app(DARK)),
    ('4. Остальные экраны', 'Пример «Все»: плашки прежние. Зелёный стал чёрным везде: «+», выбранная вкладка, кнопки, переключатели, выбранные дни. Фон серый.', lambda: screen_all(LIGHT)),
    ('5. Детали', 'Цвета до и после, состояния строк, строка даты.', spec),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:#16171D;margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:#62656E;line-height:18px;height:74px;width:428px;margin-bottom:10px;">{s}</div>'
    cell_ = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    rows = f'<div style="display:flex;gap:30px;">{"".join(cell_(*s) for s in SCREENS)}</div>'
    W, H = 5 * 428 + 4 * 30 + 80, 1230
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EFEFEF;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:#16171D;letter-spacing:-.5px;">«Неделя» по рефу и чёрный акцент — итерация 45</div>'
            f'<div style="font-size:15px;color:#62656E;margin:6px 0 22px;line-height:21px;">Решения PM: чёрный акцент везде (в тёмной теме белый), фон без зелени; в «Неделе» компактные строки '
            f'с фоном по приоритету, выполненные серые и зачёркнутые, сегодня — чёрный кружок, «+N» серый, календарь без строки месяца, панель виджета как на рефе.</div>{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Неделя и чёрный акцент</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantK2.dc.html'), 'w').write(html)
    cj = os.path.join(OUT, 'canvas.json')
    if os.path.exists(cj):
        c = json.load(open(cj))
        fresh = 'VariantK2.dc.html' not in c['boards']
        old = {k: v for k, v in c['boards'].items() if k != 'VariantK2.dc.html'}
        if fresh:
            for v in old.values():
                v['y'] += H + 120
        c['boards'] = {'VariantK2.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': '«Неделя» по рефу и чёрный акцент (итерация 45)'}, **old}
        c['order'] = list(c['boards'])
        c['notes']['head'] = {'x': 0, 'y': -300, 'text': '«Неделя» по рефу и чёрный акцент (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
        json.dump(c, open(cj, 'w'), ensure_ascii=False, indent=2)
