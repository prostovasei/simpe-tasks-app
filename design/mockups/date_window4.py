"""Макет «Итоговый вид шторки и окна даты» (итерация 34, макет до кода).

Решения PM после круга 3 и своих набросков:
- в шторке вместо одной полоски — два поля: дата «чт, 29 окт ✕» и время «17:00–18:00 ✕» (без даты — «Без даты и времени»);
- повтор и уведомления вернулись в панель (список, приоритет, чек-лист, повтор, уведомления, галочка); доступны, если есть дата;
- корзины в панели нет: у созданной задачи меню «⋮» — «Удалить» и «создано …»;
- круг «выполнено» слева от названия — с зелёной заливкой;
- окно даты: внизу «←» (отмена), «без даты», «‹ 26 окт – 8 ноя ›», «Календарь» (значок), галочка; «без времени», повтора и уведомлений в окне нет.
Запуск: python3 date_window4.py <папка с canvas.json>
"""
import os, sys, json, datetime as dt
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CAL, P_REP, P_FLAG, P_LIST, P_CHECK, P_CLOCK
from tasks_polish import A, INK, T2, BG, SEL, INACT, OVD, phone
from date_window import (LINE, PH, SHADOW, P_NODATE, P_TRASH, feed, handle, sq, list_btn, confirm, sheet, desc, check_row, clear_dot)
from date_window2 import (TODAY, DAYS, MONTHS, FIRST, NEXT, P_MONTH, week_rows, bell)
from date_window3 import DIM_ICON, HOUR, calendar_dialog

P_BACK = 'M19 12H5M11 6l-6 6 6 6'
P_DOTS = 'M12 5h.01M12 12h.01M12 19h.01'


# ---------- Шторка ----------

def done_circle(done=False):
    """Круг «выполнено» слева от названия: у невыполненной — зелёный контур и светлая заливка, у выполненной — сплошной зелёный с галочкой."""
    if done:
        return (f'<span style="width:26px;height:26px;border-radius:50%;background:{A};display:flex;align-items:center;justify-content:center;flex-shrink:0;">'
                f'{ico(P_CHECK, "#fff", 16, 3)}</span>')
    return f'<span style="width:26px;height:26px;border-radius:50%;border:2.5px solid {A};background:{SEL};box-sizing:border-box;flex-shrink:0;"></span>'


def dots_btn():
    return (f'<span style="width:36px;height:36px;display:flex;align-items:center;justify-content:center;flex-shrink:0;margin-right:-6px;">'
            f'{ico(P_DOTS, T2, 26, 3.4)}</span>')


def menu_card(created='чт, 01 окт 2026'):
    return (f'<div style="position:absolute;right:-4px;top:40px;width:236px;background:#fff;border-radius:16px;border:1px solid {LINE};box-shadow:0 8px 28px rgba(0,0,0,.16);z-index:20;overflow:hidden;">'
            f'<div style="display:flex;align-items:center;gap:14px;padding:16px 18px;font-size:17px;font-weight:600;color:{OVD};">{ico(P_TRASH, OVD, 22, 1.9)}Удалить</div>'
            f'<div style="height:1px;background:{LINE};margin:0 14px;"></div>'
            f'<div style="padding:14px 18px 16px;font-size:14px;color:{T2};">создано {created}</div></div>')


def title4(text=None, created=False, prio=None, done=False, menu=False):
    """Название: у созданной задачи слева круг «выполнено», справа приоритет и «⋮» (в нём «Удалить» и дата создания)."""
    if not created:
        return f'<div style="font-size:22px;font-weight:800;color:{PH if text is None else INK};margin-top:14px;">{text or "Купить билеты на концерт"}</div>'
    badge = (f'<span style="background:#FFD9D9;color:{OVD};font-weight:700;font-size:13px;padding:5px 8px;border-radius:8px;">{prio}</span>' if prio else '')
    return (f'<div style="position:relative;display:flex;align-items:center;gap:12px;margin-top:14px;">{done_circle(done)}'
            f'<span style="font-size:22px;font-weight:800;color:{INK};flex:1;">{text}</span>{badge}{dots_btn()}{menu_card() if menu else ""}</div>')


def date_row(date=None, time=None):
    """Два поля вместо полоски. Без даты — серая подпись без крестика; дата без времени — только поле даты."""
    if date is None:
        return (f'<div style="display:flex;align-items:center;gap:10px;margin-top:18px;height:36px;">{ico(P_CAL, T2, 22, 1.9)}'
                f'<span style="font-size:19px;font-weight:700;color:{T2};">Без даты и времени</span></div>')
    parts = (f'<span style="display:flex;align-items:center;gap:8px;">{ico(P_CAL, A, 22, 1.9)}'
             f'<span style="font-size:19px;font-weight:700;color:{A};">{date}</span>{clear_dot()}</span>')
    if time:
        parts += (f'<span style="display:flex;align-items:center;gap:8px;">{ico(P_CLOCK, A, 22, 1.9)}'
                  f'<span style="font-size:19px;font-weight:700;color:{A};">{time}</span>{clear_dot()}</span>')
    return f'<div style="display:flex;align-items:center;gap:22px;margin-top:18px;height:36px;">{parts}</div>'


def bell_btn(state, kind='at', size=50):
    """Колокольчик в панели: dim — нет даты (бледный), on — доступен, sel — выбран (kind: at / early / multi)."""
    bg, br, tint = '#fff', LINE, T2
    if state == 'sel':
        bg, br, tint = SEL, SEL, A
    elif state == 'dim':
        tint = DIM_ICON
    glyph = bell(round(size * .5), tint=tint, kind=kind if state == 'sel' else 'at', variant='D', bg=bg)
    return (f'<span style="width:{size}px;height:{size}px;border-radius:{round(size * .32)}px;border:1px solid {br};background:{bg};box-sizing:border-box;'
            f'display:flex;align-items:center;justify-content:center;flex-shrink:0;">{glyph}</span>')


def toolbar4(has_date, rep=False, rem=None, flag='on', chk='on', size=50):
    """Панель: список, приоритет, чек-лист, повтор, уведомления, справа галочка. Без даты повтор и уведомления бледные."""
    rep_state = 'dim' if not has_date else ('sel' if rep else 'on')
    rem_state = 'dim' if not has_date else ('sel' if rem else 'on')
    tools = (list_btn(size) + sq(P_FLAG, flag, size) + sq(P_LIST, chk, size) + sq(P_REP, rep_state, size) + bell_btn(rem_state, rem or 'at', size))
    return f'<div style="display:flex;gap:8px;align-items:center;margin-top:16px;">{tools}<span style="flex:1"></span>{confirm(size)}</div>'


# ---------- Окно даты ----------

def range_label(first):
    last = first + dt.timedelta(days=13)
    return f'{first.day} {MONTHS[first.month - 1]} – {last.day} {MONTHS[last.month - 1]}'


def bottom_row(first, prev_enabled):
    """«←» (отмена), «без даты», «‹ 26 окт – 8 ноя ›», «Календарь» (значок), галочка."""
    chev = lambda path, on: ico(path, INK if on else '#C3CBC6', 22, 2.4)
    mid = (f'<span style="flex:1;display:flex;align-items:center;justify-content:center;gap:4px;min-width:0;">{chev("M15 6l-6 6 6 6", prev_enabled)}'
           f'<span style="font-size:14px;font-weight:700;color:{INK};white-space:nowrap;">{range_label(first)}</span>{chev("M9 6l6 6-6 6", True)}</span>')
    cal = (f'<span style="width:48px;height:48px;border-radius:15px;border:1px solid {LINE};background:#fff;box-sizing:border-box;display:flex;align-items:center;justify-content:center;flex-shrink:0;">'
           f'{ico(P_MONTH, A, 24, 1.9)}</span>')
    return (f'<div style="display:flex;gap:8px;padding:10px 14px 16px;align-items:center;">{sq(P_BACK, "on", 48)}{sq(P_NODATE, "on", 48)}{mid}{cal}{confirm(56)}</div>')


def window(first, selected, grid_first, prev_enabled, sel_col, chip, hour, length=1.0, now=True):
    """Окно на всю высоту: сетка 10:00–24:00 с линией «сейчас», блок «2 недели», внизу ряд кнопок."""
    gut, cw = 26, (412 - 26) / 3
    cols = [grid_first + dt.timedelta(days=i) for i in range(3)]
    names = [f'{d.day} {DAYS[d.weekday()]}' for d in cols]
    head = ''.join(
        f'<span style="width:{cw}px;height:36px;display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:{800 if i == sel_col else 600};'
        f'color:{A if i == sel_col else INK};background:{SEL if i == sel_col else "transparent"};">{t}</span>' for i, t in enumerate(names))
    hours = ''.join(
        f'<div style="position:absolute;left:0;right:0;top:{(h - 10) * HOUR}px;height:{HOUR}px;border-top:1px solid {LINE};"><span style="position:absolute;left:4px;top:2px;font-size:12px;color:{T2};">{h:02d}</span></div>'
        for h in range(10, 24))
    end = f'<div style="position:absolute;left:0;right:0;top:{14 * HOUR}px;border-top:1px solid {LINE};"><span style="position:absolute;left:4px;top:-14px;font-size:12px;color:{T2};">24</span></div>'
    vlines = ''.join(f'<span style="position:absolute;left:{gut + i * cw}px;top:0;bottom:0;width:1px;background:{LINE};"></span>' for i in range(1, 3))
    sel_bg = f'<span style="position:absolute;left:{gut + sel_col * cw}px;width:{cw}px;top:0;bottom:0;background:{SEL};opacity:.7;"></span>'
    top = (hour - 10) * HOUR
    blk = (f'<div style="position:absolute;left:{gut + sel_col * cw + 2}px;width:{cw - 4}px;top:{top}px;height:{max(24, length * HOUR - 2)}px;border-radius:5px;background:{A};color:#fff;font-weight:800;font-size:13px;'
           f'display:flex;align-items:center;justify-content:center;">{chip}</div>')
    # Линия «сейчас» (14:40) — только в колонке сегодняшнего дня.
    nowline = ''
    if now and grid_first <= TODAY < grid_first + dt.timedelta(days=3):
        col = (TODAY - grid_first).days
        nowline = f'<span style="position:absolute;left:{gut + col * cw}px;width:{cw}px;top:{(14 + 40 / 60 - 10) * HOUR}px;height:2px;background:{A};"></span>'
    grid = f'<div style="position:relative;height:{14 * HOUR}px;overflow:hidden;">{sel_bg}{hours}{end}{vlines}{nowline}{blk}</div>'
    return (f'<div style="position:absolute;left:0;right:0;top:34px;bottom:0;background:#fff;border-radius:22px 22px 0 0;{SHADOW}overflow:hidden;z-index:10;">{handle()}'
            f'<div style="display:flex;padding-left:{gut}px;margin-top:4px;">{head}</div>{grid}'
            f'<div style="position:absolute;left:0;right:0;bottom:82px;border-top:1px solid {LINE};background:#fff;padding:8px 14px 4px;">'
            f'{week_rows(first, selected, grid_first)}</div>'
            f'<div style="position:absolute;left:0;right:0;bottom:0;background:#fff;">{bottom_row(first, prev_enabled)}</div></div>')


# ---------- Экраны ----------

DESC = '2 билета в партер, оплатить до пятницы'


def s_new_empty():
    return phone('#EEF1F5', feed() + sheet(title4() + desc(DESC) + date_row() + toolbar4(False)))


def s_new_date():
    return phone('#EEF1F5', feed() + sheet(title4() + desc(DESC) + date_row('чт, 29 окт') + toolbar4(True)))


def s_new_all():
    return phone('#EEF1F5', feed() + sheet(title4() + desc(DESC) + date_row('чт, 29 окт', '17:00–18:00') + toolbar4(True, rep=True, rem='multi')))


def s_detail(menu=False):
    body = (title4('Подготовить отчёт', created=True, prio='P1', menu=menu) + desc('Квартальный отчёт для руководства') + check_row('Собрать данные', True) + check_row('Сверстать таблицу')
            + date_row('пт, 30 окт', '10:00–11:30') + toolbar4(True, rep=True, rem='early', flag='sel', chk='sel'))
    return phone('#EEF1F5', feed() + sheet(body))


def s_window():
    return phone('#EEF1F5', feed() + window(FIRST, TODAY, TODAY, False, 0, chip='17:00–18:00', hour=17))


def s_window_far():
    d = dt.date(2026, 11, 12)
    return phone('#EEF1F5', feed() + window(NEXT, d, d - dt.timedelta(days=1), True, 1, chip='15:00', hour=15, length=0.5))


def s_calendar():
    return phone('#EEF1F5', feed() + window(FIRST, TODAY, TODAY, False, 0, chip='17:00–18:00', hour=17) + calendar_dialog(TODAY))


def legend():
    def row(label, html):
        return f'<div style="margin-top:12px;"><div style="font-size:12px;font-weight:700;color:{T2};margin-bottom:4px;">{label}</div><div style="width:392px;">{html}</div></div>'
    circles = (f'<div style="display:flex;gap:18px;align-items:center;margin-top:6px;"><span style="display:flex;align-items:center;gap:8px;">{done_circle(False)}<span style="font-size:13px;color:{T2};">не выполнена</span></span>'
               f'<span style="display:flex;align-items:center;gap:8px;">{done_circle(True)}<span style="font-size:13px;color:{T2};">выполнена</span></span></div>')
    rows = [
        ('Без даты', date_row()),
        ('Только дата', date_row('чт, 29 окт')),
        ('Дата и время', date_row('чт, 29 окт', '17:00')),
        ('Период', date_row('чт, 29 окт', '17:00–18:00')),
        ('Круг «выполнено»', circles),
        ('Колокольчик в панели: нет даты · доступен · в срок · заранее · несколько',
         f'<div style="display:flex;gap:8px;">{bell_btn("dim")}{bell_btn("on")}{bell_btn("sel", "at")}{bell_btn("sel", "early")}{bell_btn("sel", "multi")}</div>'),
    ]
    li = ''.join(row(a, b) for a, b in rows)
    return f'<div style="font-family:Manrope,sans-serif;"><div style="font-size:18px;font-weight:700;color:{INK};">Состояния</div>{li}</div>'


def notes():
    items = [
        ('Дата и время', 'два отдельных поля. Нажатие на дату открывает окно даты (как сейчас). ✕ у даты убирает дату вместе со временем, ✕ у времени — только время, день остаётся. Без даты — серая подпись «Без даты и времени», без крестика.'),
        ('Повтор и уведомления', 'в панели, как раньше. Есть дата (в том числе без времени) — доступны; нет даты — бледные, нажатие показывает «Сначала выберите день». В окне даты их нет.'),
        ('Корзина', 'из панели ушла. У созданной задачи справа от названия «⋮»: «Удалить» (красным) и «создано …». У новой задачи «⋮» нет.'),
        ('Круг «выполнено»', 'слева от названия созданной задачи: невыполненная — зелёный контур с светлой заливкой, выполненная — сплошной зелёный с галочкой.'),
        ('Окно даты, низ', '«←» — отменить изменения и вернуться в шторку; «без даты»; «‹ 26 окт – 8 ноя ›» — листание по 2 недели (на первой странице «‹» бледная); «Календарь» — значок, окно на 2 месяца; ✓ — применить.'),
        ('Окно даты, верх', 'сетка 10:00–24:00, линия «сейчас» в колонке сегодня, зелёный блок — время этой задачи. День в блоке «2 недели» выбирается сразу.'),
        ('Убрали', '«без времени» в окне (время убирается ✕ у времени или крестиком на блоке в сетке), повтор и уведомления в окне, верхний ряд над блоком со стрелками и «Календарь».'),
    ]
    li = ''.join(f'<div style="display:flex;gap:10px;margin-top:10px;font-size:13.5px;line-height:19px;color:{INK};"><b style="color:{A};min-width:18px;">{i + 1}</b><span><b>{a}:</b> {b}</span></div>' for i, (a, b) in enumerate(items))
    return (f'<div style="width:428px;font-family:Manrope,sans-serif;"><div style="font-size:18px;font-weight:700;color:{INK};">Как это работает</div>{li}</div>')


SCREENS = [
    ('1. Новая задача: без даты', 'Подпись «Без даты и времени». Повтор и уведомления бледные, нажатие — подсказка «Сначала выберите день».', s_new_empty),
    ('2. Новая задача: только дата', 'Выбрана дата без времени: поле времени нет, повтор и уведомления уже доступны.', s_new_date),
    ('3. Новая задача: всё выбрано', 'Дата, время-период, повтор и несколько уведомлений (колокольчик с точкой).', s_new_all),
    ('4. Созданная задача', 'Круг слева, приоритет и «⋮» справа; корзины в панели нет, галочка справа.', s_detail),
    ('5. Созданная: меню «⋮»', '«Удалить» и «создано …». Больше пунктов пока нет.', lambda: s_detail(menu=True)),
    ('6. Окно даты', 'Время 10:00–24:00, линия «сейчас». Внизу «←», «без даты», листание недель, «Календарь», галочка.', s_window),
    ('7. Окно даты: другая страница', 'Листнули на 9–22 ноября, выбран 12 ноября, 15:00; «‹» доступна.', s_window_far),
    ('8. Календарь на 2 месяца', 'День выбирается сразу по нажатию, окно закрывается; кнопка «Закрыть».', s_calendar),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    cell = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    cells = [cell(*s) for s in SCREENS] + [f'<div>{legend()}</div>', f'<div>{notes()}</div>']
    rows = ''.join(f'<div style="display:flex;gap:30px;margin-bottom:34px;">{"".join(cells[i:i + 4])}</div>' for i in (0, 4, 8))
    W, H = 4 * 428 + 3 * 30 + 80, 2900
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Шторка задачи и окно даты — итоговый макет (итерация 34)</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Решения PM: два поля даты и времени, повтор и уведомления в панели (доступны при выбранной дате), «⋮» вместо корзины, круг «выполнено» с зелёной заливкой, новый низ окна даты.</div>{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Шторка и окно даты, итог</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantC4.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    fresh = 'VariantC4.dc.html' not in c['boards']
    old = {k: v for k, v in c['boards'].items() if k != 'VariantC4.dc.html'}
    if fresh:  # повторный запуск не сдвигает прежние доски ещё раз
        for v in old.values():
            v['y'] += H + 120
    c['boards'] = {'VariantC4.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Шторка и окно даты — итог (итерация 34)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Шторка и окно даты — итоговый макет (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
