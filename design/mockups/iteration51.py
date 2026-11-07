"""Макеты итерации 51: сетка окна даты, дата в нижней строке виджета, шторка дня поверх рабочего стола (новый холст).

Решения PM (07.11.2026, по скриншоту сетки субботы): пересекающиеся задачи — рядом, колонка делится поровну; в плашке от часа —
«19:30–21:30»; между плашками встык — зазор 2 dp. Виджет: между «✓» и «+» — «сб, 7 ноя», нажатие возвращает к сегодня на всех
вкладках («Неделя» — текущая неделя и месяц календарика, «Все» — к «Сегодня», «Входящие» — в начало). Нажатие на дату дня в
«Неделе» виджета — шторка дня поверх рабочего стола (список дня и «+»), приложение не открывается.

Запуск: python3 design/mockups/iteration51.py <папка project>   → canvas.json, Main.dc.html (сетка), Widget.dc.html, Day.dc.html
"""
import datetime
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import iteration49 as I  # noqa: E402
import week_tune as W  # noqa: E402

D, B, svg = I.D, I.B, I.svg
frow, day_header = D.frow, D.day_header
W_PHONE, H_PHONE = I.W_PHONE, I.H_PHONE

# ------------------------------------------------------------------ сетка окна даты

H0, H1, HH, GUT = 15, 24, 60, 30          # часы 15–24, час 60 px, колонка часов 30
CW = (W_PHONE - GUT) / 3                   # ширина колонки дня
GAP = 2                                    # зазор между плашками (решение PM)
DAYS = [('07', 'сб', True), ('08', 'вс', False), ('09', 'пн', False)]
NOW = 18 * 60 + 9

# (колонка, начало, конец, название, выполнена) — суббота со скриншота PM, вс — «Падел», пн — пример трёх пересечений.
SLOTS = [
    (0, 19 * 60 + 30, 21 * 60 + 30, 'Кальян', False),
    (0, 21 * 60 + 30, 23 * 60 + 30, 'Футбол', False),
    (0, 23 * 60, 23 * 60 + 30, 'Цветы', False),
    (1, 20 * 60, 21 * 60, 'Падел', False),
    (2, 18 * 60, 19 * 60 + 30, 'Ужин', False),
    (2, 18 * 60 + 30, 19 * 60 + 30, 'Звонок маме', False),
    (2, 19 * 60, 20 * 60, 'Сериал', False),
]


def hm(m):
    return f'{m // 60 % 24:02d}:{m % 60:02d}'


def lanes(slots):
    """Дорожки: пересекающиеся (не встык) задачи одного дня встают рядом; группа делит колонку поровну."""
    out = {}
    for day in {s[0] for s in slots}:
        items = sorted([s for s in slots if s[0] == day], key=lambda s: (s[1], -s[2]))
        group, end = [], -1
        def close(g):
            ends = []
            for s in g:
                lane = next((i for i, e in enumerate(ends) if e <= s[1]), len(ends))
                if lane == len(ends):
                    ends.append(s[2])
                else:
                    ends[lane] = s[2]
                out[s] = [lane, None]
            for s in g:
                out[s][1] = len(ends)
        for s in items:
            if group and s[1] >= end:
                close(group)
                group, end = [], -1
            group.append(s)
            end = max(end, s[2])
        if group:
            close(group)
    return out


def block(s, after, lane=0, count=1):
    day, a, b, title, done = s
    top = (a - H0 * 60) / 60 * HH
    h = (b - a) / 60 * HH
    long = b - a >= 60
    if after:
        inner = CW - 4
        w = (inner - (count - 1) * GAP) / count
        x = GUT + day * CW + 2 + lane * (w + GAP)
        h -= GAP
        # Не помещается в строку (узкая плашка) — конец переносится на вторую строку.
        when = f'{hm(a)}–<wbr>{hm(b)}' if long else ''
        bg = 'color-mix(in srgb,var(--divider) 70%,var(--surface))'
    else:
        w, x = CW - 4, GUT + day * CW + 2
        when = hm(a) if long else ''
        bg = 'color-mix(in srgb,var(--divider) 70%,transparent)'
    sub = f'<div style="font-size:11px;line-height:15px;overflow:hidden">{when}</div>' if when else ''
    return (f'<div style="position:absolute;left:{x:.1f}px;top:{top:.1f}px;width:{w:.1f}px;height:{h:.1f}px;border-radius:var(--radius-sm);background:{bg};'
            f'padding:2px 4px;box-sizing:border-box;overflow:hidden;color:var(--text-secondary)">'
            f'<div style="font-size:12px;line-height:16px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis{";text-decoration:line-through" if done else ""}">{title}</div>{sub}</div>')


def grid(after):
    head = ''.join(f'<span style="width:{CW:.1f}px;height:44px;display:flex;align-items:center;justify-content:center;gap:4px;'
                   f'background:{"var(--background)" if sel else "transparent"}"><b style="font-size:16px">{n}</b>'
                   f'<span style="font-size:12px;color:var(--text-secondary)">{wd}</span></span>' for n, wd, sel in DAYS)
    rows = ''.join(f'<div style="position:absolute;left:0;right:0;top:{(h - H0) * HH}px;height:{HH}px;border-top:1px solid var(--divider)">'
                   f'<span style="position:absolute;left:6px;top:3px;font-size:12px;color:var(--text-secondary)">{h}</span>'
                   f'<span style="position:absolute;left:{GUT}px;right:0;top:{HH / 2}px;border-top:1px solid color-mix(in srgb,var(--divider) 50%,transparent)"></span></div>'
                   for h in range(H0, H1))
    cols = ''.join(f'<span style="position:absolute;top:0;bottom:0;left:{GUT + i * CW:.1f}px;width:{CW:.1f}px;border-left:1px solid var(--divider);'
                   f'background:{"var(--background)" if DAYS[i][2] else "transparent"}"></span>' for i in range(3))
    now = f'<span style="position:absolute;left:{GUT}px;width:{CW:.1f}px;top:{(NOW - H0 * 60) / 60 * HH:.1f}px;height:2px;background:var(--accent)"></span>'
    order = SLOTS if after else [SLOTS[0], SLOTS[2], SLOTS[1]] + SLOTS[3:]
    lay = lanes(SLOTS)
    blocks = ''.join(block(s, after, *lay[s]) if after else block(s, False) for s in order)
    bar = B.window_bar().replace('26 окт – 08 ноя', '02 ноя – 15 ноя')
    body_h = (H1 - H0) * HH
    return (f'<div style="position:absolute;inset:0;background:var(--surface);border-radius:28px;overflow:hidden">'
            f'<div style="height:12px"></div><div style="display:flex;padding-left:{GUT}px;border-bottom:1px solid var(--divider)">{head}</div>'
            f'<div style="position:relative;height:{body_h}px;overflow:hidden">{cols}{rows}{blocks}{now}</div>'
            f'<div style="position:absolute;left:0;right:0;bottom:0;border-top:1px solid var(--divider);background:var(--surface)">{bar}</div></div>')


def grid_notes():
    items = [
        ('Рядом, а не поверх', '«Футбол» 21:30–23:30 и «Цветы» в 23:00 пересекаются — колонка делится пополам, обе плашки видны целиком и одного цвета. '
         'Три пересечения (пн: «Ужин», «Звонок маме», «Сериал») — по трети; в узкой плашке название обрезается «…».'),
        ('Подпись с концом', 'В плашке от часа и дольше под названием — «19:30–21:30», как в ленте «Все». В короткой — только название.'),
        ('Зазор 2 dp', '«Кальян» до 21:30 и «Футбол» с 21:30 идут встык — между ними тонкая щель, граница видна сразу. Встык — не пересечение: обе на всю ширину.'),
        ('Что не меняется', 'Высота плашки — длительность. Выполненная — бледная и зачёркнутая. Выбираемое время — чёрный блок поверх всего, как раньше. '
         'Блок «2 недели» и нижняя строка окна — без изменений.'),
    ]
    cards = ''.join(f'<div style="background:var(--surface);border-radius:var(--radius-lg);padding:14px 16px"><b>{t}.</b> {x}</div>' for t, x in items)
    return f'<div style="width:{W_PHONE}px;display:flex;flex-direction:column;gap:12px;font-size:14px;line-height:20px">{cards}</div>'


# ------------------------------------------------------------------ виджет: дата в нижней строке

W.TODAY = 7
W.WEEK[5] = ('сб', 7, [('Telegram', '10:30', W.ME, 0, 1), ('Кальян', '19:30', '🎮', 0, 0), ('Футбол', '21:30', W.WORK, 0, 0), ('Цветы', '23:00', W.HOME, 0, 1)])
DATE_FULL, DATE_SHORT = 'сб, 7 ноя', '7 ноя'


def bar(date=DATE_FULL, width=None):
    tab = lambda p, on: (f'<span style="width:50px;height:30px;border-radius:11px;background:{W.SOFT if on else "transparent"};display:flex;flex-shrink:0;'
                         f'align-items:center;justify-content:center;">{W.ico(p, W.INK if on else "#4A4D55", 22, 2)}</span>')
    check = (f'<svg width="24" height="20" viewBox="0 0 43 36" aria-hidden="true"><path d="M5 19L16 30L38 7" stroke="{W.INK}" '
             f'stroke-width="6" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>')
    pos = 'position:absolute;left:0;right:0;bottom:0;' if width is None else f'position:relative;width:{width}px;border-radius:0 0 16px 16px;'
    return (f'<div style="{pos}height:{W.BAR}px;border-top:1px solid {W.LINE};display:flex;align-items:center;padding:0 8px;gap:4px;background:{W.BG};font-family:{W.FONT}">'
            f'{tab(W.P_CAL, False)}{tab(W.P_INBOX, False)}{tab(W.P_WEEK, True)}<span style="flex:1"></span>'
            f'<span style="width:44px;height:32px;display:flex;align-items:center;justify-content:center;flex-shrink:0">{check}</span>'
            f'<span style="height:32px;padding:0 8px;border-radius:10px;display:flex;align-items:center;font-size:{W.sp(13)}px;font-weight:700;color:{W.INK};'
            f'white-space:nowrap;flex-shrink:0;outline:1.5px dashed #8E4EC6;outline-offset:-2px">{date}</span>'
            f'<span style="width:64px;height:32px;border-radius:12px;background:{W.INK};display:flex;align-items:center;justify-content:center;flex-shrink:0">'
            f'{W.ico(W.P_PLUS, "#FFFFFF", 20, 2.4)}</span></div>')


_calendar = W.calendar


def calendar(P, row_h):
    """Календарик как в week_tune.py, но сегодня — 7-е: кружок с 5-го (индекс 10) переезжает на 7-е."""
    html = _calendar(P, row_h)
    dark = f'background:{W.INK};border-radius:50%;'
    html = html.replace(dark, '', 1).replace('color:#FFFFFF;">5<', f'color:{W.INK};">5<', 1)
    marker = 'font-weight:700;color:' + W.INK + ';">7</span>'
    return html.replace(f'<span style="width:{P["cal_box"]}px;height:{P["cal_box"]}px;display:flex;align-items:center;justify-content:center;font-size:{W.sp(P["cal_fs"])}px;{marker}',
                        f'<span style="width:{P["cal_box"]}px;height:{P["cal_box"]}px;{dark}display:flex;align-items:center;justify-content:center;font-size:{W.sp(P["cal_fs"])}px;font-weight:700;color:#FFFFFF;">7</span>', 1)


W.bar = bar
W.calendar = calendar


def widget_phone():
    return W.phone(W.AFTER, 'C').replace('>14:12<', '>18:14<')


def bars_board():
    def labelled(t, sub, inner):
        return f'<div><div style="font-size:15px;font-weight:700">{t}</div><div style="font-size:13px;color:var(--text-secondary);margin:2px 0 8px">{sub}</div>{inner}</div>'
    wide = labelled('Обычный виджет (396 dp)', '«сб, 7 ноя» — между «✓» и «+».', bar(DATE_FULL, 396))
    narrow = labelled('Узкий виджет (до 340 dp)', 'Не помещается — без дня недели: «7 ноя».', bar(DATE_SHORT, 320))
    notes = [
        ('«Неделя»', 'Сразу текущая неделя (2–8 ноя) и месяц календарика, как будто выбрали «сегодня» в календарике.'),
        ('«Все»', 'Лента прокручивается к «СБ, 07 ноя · Сегодня» (просроченные остаются выше).'),
        ('«Входящие»', 'Список — в начало.'),
        ('Подпись', 'Дата меняется в полночь сама. Жирный, цвет основного текста, 13 sp, зона нажатия во всю высоту строки. Пунктир — только в макете.'),
    ]
    cards = ''.join(f'<div style="background:var(--surface);border-radius:var(--radius-lg);padding:12px 14px"><b>{t}.</b> {x}</div>' for t, x in notes)
    return (f'<div style="display:flex;flex-direction:column;gap:22px;width:430px">{wide}{narrow}'
            f'<div style="font-size:15px;font-weight:700;margin-top:4px">Нажатие на дату</div>'
            f'<div style="display:flex;flex-direction:column;gap:10px;font-size:14px;line-height:20px">{cards}</div></div>')


# ------------------------------------------------------------------ шторка дня поверх рабочего стола

def day_sheet():
    rows = (frow('10:30', 'var(--inactive)', 'Telegram', done=True, rep=True, top=False)
            + frow('19:30', 'var(--inactive)', 'Кальян', period=True, end='21:30')
            + frow('21:30', 'var(--inactive)', 'Футбол', period=True, end='23:30')
            + frow('23:00', 'var(--inactive)', 'Цветы', rep=True, bottom=False, rem=1))
    head = ('<div style="display:flex;align-items:center;gap:8px;padding:6px 16px 8px">'
            '<span style="font-size:22px;line-height:28px;font-weight:700">Сб, 7 ноя</span>'
            '<span style="font-size:11px;line-height:16px;font-weight:700;background:var(--background);border-radius:var(--radius-sm);padding:1px 8px">Сегодня</span>'
            '<span style="flex:1"></span><span style="font-size:14px;color:var(--text-secondary);font-weight:700">1 из 4</span></div>')
    add = (f'<div style="display:flex;justify-content:flex-end;padding:8px 12px 14px"><span style="height:48px;padding:0 18px;border-radius:var(--radius-xl);'
           f'background:var(--accent);color:var(--on-accent);display:flex;align-items:center;gap:6px;font-weight:700;font-size:14px">{D.mi("add", 22)}Задача на сб, 7 ноя</span></div>')
    sheet = (f'<div style="position:absolute;left:8px;right:8px;bottom:16px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden">'
             f'<div style="display:flex;justify-content:center;padding:8px 0 4px"><span style="width:40px;height:4px;border-radius:2px;background:var(--divider)"></span></div>'
             f'{head}<div style="padding:6px 0 6px 4px;background:var(--background)">{rows}</div>{add}</div>')
    behind = widget_phone()
    return (f'<div style="position:relative;width:428px;height:876px;flex-shrink:0">{behind}'
            f'<div style="position:absolute;inset:0;border-radius:52px;background:var(--scrim)"></div>'
            f'<div style="position:absolute;left:8px;top:8px;width:412px;height:860px">{sheet}</div></div>')


def day_notes():
    items = [
        ('Откуда', 'Нажатие на «СБ 7» в «Неделе» виджета. Приложение не открывается: поверх рабочего стола выезжает шторка, под ней затемнённый рабочий стол.'),
        ('Что внутри', 'Дата и «Сегодня» / «Завтра», «1 из 4» — выполнено из всех. Лента дня как в приложении: время слева, кружок или знак повтора, плашка с эмодзи списка. '
         'Будущие повторы в этот день — тоже (без кружка «выполнить», как в ленте).'),
        ('Действия', 'Кружок — выполнить и вернуть (остаётся серой до полуночи). Нажатие на задачу — её окно поверх, как из виджета. '
         '«Задача на сб, 7 ноя» — новая задача на этот день без времени. «Назад», смахивание вниз и нажатие на затемнение — закрыть.'),
        ('Много задач', 'Шторка растёт до ~80% экрана, дальше лента прокручивается внутри; кнопка «+» всегда видна снизу.'),
    ]
    cards = ''.join(f'<div style="background:var(--surface);border-radius:var(--radius-lg);padding:14px 16px"><b>{t}.</b> {x}</div>' for t, x in items)
    return f'<div style="width:440px;display:flex;flex-direction:column;gap:12px;font-size:14px;line-height:20px">{cards}</div>'


# ------------------------------------------------------------------ страницы

def page(title, Wd, H, body):
    html = I.page(title, Wd, H, body)
    return html.replace('family=Manrope:wght@400;500;600;700;800&amp;', 'family=Manrope:wght@400;500;600;700;800&amp;family=Roboto:wght@400;500;700&amp;')


def headline(title, lead):
    return (f'<div style="font-size:28px;font-weight:800;letter-spacing:-.4px">{title}</div>'
            f'<div style="font-size:15px;color:var(--text-secondary);margin:6px 0 22px;line-height:21px;max-width:1300px">{lead}</div>')


def col(t, sub, inner):
    return (f'<div><div style="font-size:17px;font-weight:700;margin-bottom:4px">{t}</div>'
            f'<div style="font-size:13px;color:var(--text-secondary);line-height:18px;min-height:40px;width:{W_PHONE}px;margin-bottom:10px">{sub}</div>{inner}</div>')


BOARDS = []


def build(out):
    boards, order, y = {}, [], 0

    def add(name, title, Wd, H, body):
        nonlocal y
        Path(out, name).write_text(page(title, Wd, H, body), encoding='utf-8')
        boards[name] = {'x': 0, 'y': y, 'w': Wd, 'h': H, 'title': title}
        order.append(name)
        y += H + 120

    phone_grid = lambda after: I.phone(grid(after))
    body = (headline('Сетка окна даты — итерация 51', 'Суббота со скриншота PM. Слева — как сейчас, справа — как будет: пересечения рядом, подпись «с–до», зазор 2 dp.')
            + '<div style="display:flex;gap:40px;align-items:flex-start">'
            + col('1. Было (0.50.1)', '«Цветы» просвечивают сквозь «Футбол», «Кальян» и «Футбол» сливаются, у плашек только начало.', phone_grid(False))
            + col('2. Стало', '«Футбол» и «Цветы» рядом, у длинных плашек «19:30–21:30», щель между задачами встык. Пн — три пересечения.', phone_grid(True))
            + f'<div style="padding-top:60px">{grid_notes()}</div></div>')
    add('Main.dc.html', 'Сетка окна даты', 3 * W_PHONE + 2 * 40 + 80, 36 + 40 + 52 + 22 + 64 + H_PHONE + 40, body)

    body = (headline('Виджет: дата в нижней строке', 'Между «✓» и «+» — сегодняшняя дата; нажатие возвращает к сегодня на любой вкладке.')
            + f'<div style="display:flex;gap:48px;align-items:flex-start">{widget_phone()}{bars_board()}</div>')
    add('Widget.dc.html', 'Дата в нижней строке виджета', 428 + 48 + 430 + 80, 36 + 40 + 52 + 876 + 40, body)

    body = (headline('Шторка дня поверх рабочего стола', 'Нажатие на дату дня в «Неделе» виджета — вместо открытия приложения.')
            + f'<div style="display:flex;gap:48px;align-items:flex-start">{day_sheet()}{day_notes()}</div>')
    add('Day.dc.html', 'Шторка дня', 428 + 48 + 440 + 80, 36 + 40 + 52 + 876 + 40, body)

    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Итерация 51 — сетка, дата и день в виджете', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': boards, 'order': order,
              'notes': {'head': {'x': 0, 'y': -300, 'text': 'Сетка окна даты, дата в виджете, шторка дня (итерация 51)', 'kind': 'title1', 'maxW': 1900}},
              'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', [(n, b['w'], b['h']) for n, b in boards.items()])


if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    build(out)
