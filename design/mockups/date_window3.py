"""Макет «Окно даты и времени на весь экран + повтор и уведомления в полоске» (итерация 33, круг 3).

Решения PM: значок «несколько уведомлений» — вариант Г (точка); в календаре день выбирается сразу, без «Готово»;
время в окне всегда с 10:00 до 24:00; окно на всю высоту; кнопки повтора и уведомлений переехали из панели шторки
в полоску даты, а в окне даты появились внизу.
Запуск: python3 date_window3.py <папка с canvas.json>
"""
import os, sys, json, datetime as dt
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CAL, P_REP, P_BELL, P_FLAG, P_LIST
from tasks_polish import A, INK, T2, BG, SEL, INACT, OVD, phone
from date_window import (LINE, PH, SHADOW, P_NODATE, P_NOTIME, P_TRASH, feed, handle, sq, list_btn, confirm, sheet, title, desc, check_row, clear_dot)
from date_window2 import (TODAY, DAYS, MONTHS, FIRST, NEXT, W7, block_head, week_rows, month_grid, bell, monday)

DIM_ICON = '#C3CBC6'
HOUR = 37.5  # высота часа: 10:00–24:00 (14 часов) помещаются на экран


def strip_btn(icon_html, selected, size=40):
    """Кнопка внутри полоски: выбранная — белая плашка, иначе просто значок."""
    bg = '#fff' if selected else 'transparent'
    return f'<span style="width:{size}px;height:{size}px;border-radius:12px;background:{bg};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{icon_html}</span>'


def strip(text=None, repeat='off', rem='off'):
    """Единая полоска: слева дата и время (нажать — окно даты), справа повтор и уведомления.
    repeat: off | on; rem: off | at | early | multi (точка, вариант Г). Без даты повтор и уведомления бледные."""
    has = text is not None
    rep_icon = ico(P_REP, A if repeat == 'on' else (T2 if has else DIM_ICON), 22, 1.9)
    if not has:
        bell_icon = bell(22, tint=DIM_ICON, kind='at', variant='D')
    else:
        kind = {'off': 'at', 'at': 'at', 'early': 'early', 'multi': 'multi'}[rem]
        bell_icon = bell(22, tint=(A if rem != 'off' else T2), kind=kind, variant='D', bg='#fff' if rem != 'off' else SEL)
    rem_sel = rem != 'off' and has
    if has:
        left = (f'<span style="display:flex;align-items:center;gap:12px;flex:1;min-width:0;">{ico(P_CAL, A, 22, 1.9)}'
                f'<span style="font-size:16px;font-weight:700;color:{A};flex:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{text}</span>{clear_dot()}</span>')
        bg, br = SEL, SEL
    else:
        left = (f'<span style="display:flex;align-items:center;gap:12px;flex:1;">{ico(P_CAL, INK, 22, 1.9)}'
                f'<span style="font-size:16px;font-weight:600;color:{PH};">Дата и время</span></span>')
        bg, br = '#fff', LINE
    right = f'{strip_btn(rep_icon, repeat == "on" and has)}{strip_btn(bell_icon, rem_sel)}'
    div = f'<span style="width:1px;height:28px;background:{"#C9E3D2" if has else LINE};margin:0 6px 0 10px;flex-shrink:0;"></span>'
    return (f'<div style="height:54px;border-radius:16px;background:{bg};border:1px solid {br};box-sizing:border-box;display:flex;align-items:center;padding:0 7px 0 16px;gap:0;">'
            f'{left}{div}{right}</div>')


def toolbar3(delete=False, flag='on', chk='on', size=50):
    """Панель шторки без повтора и уведомлений: список, приоритет, чек-лист; справа корзина (у созданной) и галочка."""
    tools = list_btn(size) + sq(P_FLAG, flag, size) + sq(P_LIST, chk, size)
    right = (sq(P_TRASH, 'danger', size) if delete else '') + confirm(size)
    return f'<div style="display:flex;gap:8px;align-items:center;margin-top:14px;">{tools}<span style="flex:1"></span>{right}</div>'


# ---------- Окно даты и времени на весь экран ----------

def window(first, selected, grid_first, prev_enabled, sel_col, chip='11:30', hour=11.5, rep='off', rem='at'):
    """Окно на всю высоту под строкой статуса: сетка 10:00–24:00, блок «2 недели», внизу «без даты», «без времени»,
    повтор, уведомления и галочка."""
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
    blk = (f'<div style="position:absolute;left:{gut + sel_col * cw + 2}px;width:{cw - 4}px;top:{top}px;height:24px;border-radius:5px;background:{A};color:#fff;font-weight:800;font-size:13px;'
           f'display:flex;align-items:center;justify-content:center;">{chip}</div>')
    # Подсказка, что выше есть раннее время: бледная стрелка вверх у верхнего края сетки.
    up = (f'<div style="position:absolute;left:0;right:0;top:0;height:18px;background:linear-gradient(#fff,rgba(255,255,255,0));display:flex;justify-content:center;">'
          f'{ico("M6 14l6-6 6 6", PH, 14, 2.2)}</div>')
    grid = f'<div style="position:relative;height:{14 * HOUR}px;overflow:hidden;">{sel_bg}{hours}{end}{vlines}{blk}{up}</div>'
    rep_btn = sq(P_REP, 'sel' if rep == 'on' else 'on', 48)
    kind = {'off': 'at', 'at': 'at', 'early': 'early', 'multi': 'multi'}[rem]
    rem_btn = (f'<span style="width:48px;height:48px;border-radius:15px;border:1px solid {SEL if rem != "off" else LINE};background:{SEL if rem != "off" else "#fff"};box-sizing:border-box;'
               f'display:flex;align-items:center;justify-content:center;flex-shrink:0;">{bell(24, tint=(A if rem != "off" else T2), kind=kind, variant="D", bg=(SEL if rem != "off" else "#fff"))}</span>')
    btns = (f'<div style="display:flex;gap:8px;padding:10px 14px 16px;align-items:center;">{sq(P_NODATE, "on", 48)}{sq(P_NOTIME, "on", 48)}'
            f'<span style="width:1px;height:28px;background:{LINE};margin:0 2px;"></span>{rep_btn}{rem_btn}<span style="flex:1"></span>{confirm(56)}</div>')
    return (f'<div style="position:absolute;left:0;right:0;top:34px;bottom:0;background:#fff;border-radius:22px 22px 0 0;{SHADOW}overflow:hidden;z-index:10;">{handle()}'
            f'<div style="display:flex;padding-left:{gut}px;margin-top:4px;">{head}</div>{grid}'
            f'<div style="position:absolute;left:0;right:0;bottom:82px;border-top:1px solid {LINE};background:#fff;padding:8px 14px 4px;">'
            f'{block_head(first, prev_enabled)}{week_rows(first, selected, grid_first if selected is not None else None)}</div>'
            f'<div style="position:absolute;left:0;right:0;bottom:0;background:#fff;">{btns}</div></div>')


def calendar_dialog(selected):
    """Календарь на 2 месяца: день выбирается сразу по нажатию, окно закрывается — кнопка только «Закрыть»."""
    cell = 47.4
    head = ''.join(f'<span style="width:{cell}px;text-align:center;font-size:11px;font-weight:700;color:{PH};">{d}</span>' for d in DAYS)
    btn = f'<span style="flex:1;height:50px;border-radius:16px;border:1px solid {LINE};box-sizing:border-box;display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:700;color:{INK};">Закрыть</span>'
    return ('<div style="position:absolute;inset:0;background:rgba(21,22,31,.42);z-index:20;"></div>'
            '<div style="position:absolute;inset:34px 0 0;z-index:21;display:flex;align-items:center;justify-content:center;">'
            f'<div style="width:364px;border-radius:28px;background:#fff;{SHADOW}overflow:hidden;">'
            f'<div style="padding:22px 16px 6px;"><div style="font-size:21px;font-weight:700;color:{INK};padding:0 6px;">Календарь</div>'
            f'<div style="font-size:14px;color:{T2};padding:2px 6px 8px;">Нажмите на день — окно закроется</div>'
            f'<div style="display:flex;">{head}</div>{month_grid(2026, 10, selected)}{month_grid(2026, 11, selected)}</div>'
            f'<div style="display:flex;padding:14px 22px 22px;border-top:1px solid {LINE};">{btn}</div></div></div>')


def callout(text, top):
    return (f'<div style="position:absolute;left:16px;right:16px;top:{top}px;z-index:30;font-size:12.5px;line-height:17px;color:#fff;'
            f'background:rgba(21,22,31,.86);border-radius:12px;padding:8px 12px;">{text}</div>')


# ---------- Экраны ----------

def s_new_empty():
    return phone('#EEF1F5', feed() + sheet(title() + desc('2 билета в партер, оплатить до пятницы') + f'<div style="margin-top:16px;">{strip()}</div>' + toolbar3()))


def s_new_all():
    return phone('#EEF1F5', feed() + sheet(title() + desc('2 билета в партер, оплатить до пятницы')
                                          + f'<div style="margin-top:16px;">{strip("чт, 29 окт · 11:30", "on", "multi")}</div>' + toolbar3()))


def s_detail():
    body = (title('Подготовить отчёт', True) + desc('Квартальный отчёт для руководства') + check_row('Собрать данные', True) + check_row('Сверстать таблицу')
            + f'<div style="margin-top:16px;">{strip("пт, 30 окт · 10:00–11:30", "on", "early")}</div>' + toolbar3(delete=True, flag='sel', chk='sel'))
    return phone('#EEF1F5', feed() + sheet(body))


def s_window():
    return phone('#EEF1F5', feed() + window(FIRST, TODAY, TODAY, False, 0, rep='on', rem='multi'))


def s_calendar():
    return phone('#EEF1F5', feed() + window(FIRST, TODAY, TODAY, False, 0) + calendar_dialog(TODAY))


def s_window_far():
    d = dt.date(2026, 11, 12)
    return phone('#EEF1F5', feed() + window(NEXT, d, d - dt.timedelta(days=1), True, 1, chip='15:00', hour=15, rem='at'))


def legend():
    rows = [
        ('Без даты', strip()),
        ('Дата без времени', strip('чт, 29 окт', 'off', 'at')),
        ('Дата и время', strip('чт, 29 окт · 11:30', 'off', 'at')),
        ('Период', strip('чт, 29 окт · 11:30–13:00', 'off', 'at')),
        ('Повтор и уведомление «заранее»', strip('чт, 29 окт · 11:30', 'on', 'early')),
        ('Несколько уведомлений (точка)', strip('чт, 29 окт · 11:30', 'on', 'multi')),
    ]
    li = ''.join(f'<div style="margin-top:12px;"><div style="font-size:12px;font-weight:700;color:{T2};margin-bottom:4px;">{a}</div><div style="width:392px;">{b}</div></div>' for a, b in rows)
    return f'<div style="font-family:Manrope,sans-serif;"><div style="font-size:18px;font-weight:700;color:{INK};">Полоска: состояния</div>{li}</div>'


def notes():
    items = [
        ('Время', 'окно всегда открывается с 10:00, видно 10:00–24:00 сразу; раньше 10:00 — прокрутить вверх (бледная стрелка сверху). Час ≈ 37 dp вместо 44, чтобы всё поместилось.'),
        ('Окно на весь экран', 'от строки статуса до низа, без полосы сверху.'),
        ('Повтор и уведомления', 'в окне даты — внизу рядом с «без даты» и «без времени». В шторке задачи их в панели больше нет: они справа в полоске даты (повтор — значок, уведомления — колокольчик).'),
        ('Колокольчик', 'в момент срока — без знаков; заранее — стрелка назад; несколько — стрелка и зелёная точка справа внизу.'),
        ('Календарь', 'день выбирается сразу по нажатию, окно закрывается, сетка переезжает к дню. Кнопка одна — «Закрыть».'),
        ('Без даты', 'повтор и уведомления в полоске бледные; нажатие — подсказка «Сначала выберите день», как сейчас.'),
    ]
    li = ''.join(f'<div style="display:flex;gap:10px;margin-top:11px;font-size:13.5px;line-height:19px;color:{INK};"><b style="color:{A};min-width:18px;">{i + 1}</b><span><b>{a}:</b> {b}</span></div>' for i, (a, b) in enumerate(items))
    return (f'<div style="width:428px;font-family:Manrope,sans-serif;"><div style="font-size:18px;font-weight:700;color:{INK};">Как это работает</div>{li}'
            f'<div style="font-size:18px;font-weight:700;color:{INK};margin-top:22px;">Что решить</div>'
            f'<div style="font-size:13.5px;line-height:19px;color:{INK};margin-top:8px;"><b>Время раньше 10:00:</b> «всегда с 10:00» — задача на 07:30 откроется на 10:00, и её видно только после прокрутки. '
            f'Или открывать с 10:00, но если время выбрано раньше — с него?</div></div>')


SCREENS = [
    ('1. Новая задача: без даты', 'Повтор и уведомления теперь в полоске даты справа (бледные, пока нет даты). В панели — список, приоритет, чек-лист.', s_new_empty),
    ('2. Новая задача: всё выбрано', 'Дата и время, повтор (зелёный значок на белой плашке) и колокольчик с точкой — уведомлений несколько.', s_new_all),
    ('3. Созданная задача', 'То же в созданной: период, повтор, колокольчик со стрелкой «заранее»; в панели корзина и галочка.', s_detail),
    ('4. Окно даты на весь экран', 'От строки статуса до низа. Время 10:00–24:00, блок «2 недели», внизу «без даты», «без времени», повтор, уведомления, галочка.', s_window),
    ('5. Календарь на 2 месяца', 'День выбирается сразу по нажатию, «Готово» нет — одна кнопка «Закрыть».', s_calendar),
    ('6. Другая страница', 'Листнули на 9–22 ноября и выбрали 12 ноября, 15:00: сетка у выбранного дня, время тоже с 10:00.', s_window_far),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    cell = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    cells = [cell(*s) for s in SCREENS] + [f'<div>{legend()}</div>', f'<div>{notes()}</div>']
    rows = ''.join(f'<div style="display:flex;gap:30px;margin-bottom:34px;">{"".join(cells[i:i + 4])}</div>' for i in (0, 4))
    W, H = 4 * 428 + 3 * 30 + 80, 2130
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Окно даты на весь экран, повтор и уведомления в полоске — круг 3</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Решения PM: колокольчик «несколько» — точка (Г); календарь без «Готово»; время всегда 10:00–24:00; окно на всю высоту; повтор и уведомления — в полоске даты.</div>{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Окно даты, круг 3</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantC3.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    fresh = 'VariantC3.dc.html' not in c['boards']
    old = {k: v for k, v in c['boards'].items() if k != 'VariantC3.dc.html'}
    if fresh:  # повторный запуск не сдвигает прежние доски ещё раз
        for v in old.values():
            v['y'] += H + 120
    c['boards'] = {'VariantC3.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Окно даты на весь экран, полоска (итерация 33, круг 3)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Окно даты и полоска, круг 3 (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
