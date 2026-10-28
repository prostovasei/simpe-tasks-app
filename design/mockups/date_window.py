"""Макет «Единое окно даты и времени» (итерация 33): вместо плиток дней и значка месяца — одна кнопка-поле в шторке задачи,
по нажатию окно с сеткой на 3 дня и блоком «2 недели» внизу. Месяц нигде не открывается.

Телефон 412×860 (dp 1:1). «Сегодня» — чт, 29 окт 2026 (как на скринах PM).
Запуск: python3 date_window.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CHECK, P_CAL, P_REP, P_BELL, P_FLAG, P_LIST, P_INBOX, P_CLOCK
from tasks_polish import A, INK, T2, BG, SEL, INACT, OVD, PRI, phone, day, row, timeline
from sheet_dialogs import tool, list_sq

LINE = '#E3E7E4'
PH = '#A3ABA7'
SHADOW = 'box-shadow:0 -6px 24px rgba(0,0,0,.1);'
P_NODATE = P_CAL + 'M3 21L21 3'
P_NOTIME = P_CLOCK + 'M3 21L21 3'
P_TRASH = 'M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13M10 11v6M14 11v6'
P_PLUS = 'M12 5v14M5 12h14'


def status(c=INK):
    return (f'<div style="height:34px;display:flex;align-items:center;justify-content:space-between;padding:0 24px;font-size:13px;font-weight:700;color:{c};">'
            f'<span>00:47</span><span style="letter-spacing:1px;">●●● ▮</span></div>')


def feed():
    """Список под шторкой (затемнён): сегодня чт, 29 окт."""
    r = (day('СР, 28 окт', 'Вчера', red=True, first=True) + row('Плппш', ring=OVD, rem='now', overdue=True)
         + row('Тест', '12:00', ring=OVD, rem='early', overdue=True) + day('ЧТ, 29 окт', 'Сегодня') + row('Купить продукты', ring=PRI['low']))
    return status() + f'<div style="padding:8px 0 0;">{timeline(r)}</div><div style="position:absolute;inset:0;background:rgba(21,22,31,.42);z-index:5;"></div>'


def handle():
    return '<div style="display:flex;justify-content:center;padding-top:8px;"><span style="width:36px;height:4px;border-radius:2px;background:#D5DAD7;"></span></div>'


def clear_dot():
    return (f'<span style="width:22px;height:22px;border-radius:50%;background:{INACT};display:flex;align-items:center;justify-content:center;flex-shrink:0;">'
            f'{ico("M6 6l12 12M18 6L6 18", "#fff", 12, 2.6)}</span>')


def date_field(text=None):
    """Одна кнопка вместо плиток дней: пусто — подсказка, выбрано — дата и время зелёным и крестик «убрать дату»."""
    if text is None:
        return (f'<div style="height:52px;border-radius:16px;border:1px solid {LINE};background:#fff;box-sizing:border-box;display:flex;align-items:center;gap:12px;padding:0 16px;">'
                f'{ico(P_CAL, INK, 22, 1.9)}<span style="font-size:16px;font-weight:600;color:{PH};flex:1;">Дата и время</span></div>')
    return (f'<div style="height:52px;border-radius:16px;background:{SEL};box-sizing:border-box;display:flex;align-items:center;gap:12px;padding:0 14px 0 16px;">'
            f'{ico(P_CAL, A, 22, 1.9)}<span style="font-size:16px;font-weight:700;color:{A};flex:1;">{text}</span>{clear_dot()}</div>')


def sq(path, state='on', size=50, icon=None):
    """Квадратная кнопка панели; state: on — доступна, sel — выбрана (зелёная иконка), dim — бледная."""
    bg, br, c = '#fff', LINE, T2
    if state == 'sel':
        bg, br, c = SEL, SEL, A
    elif state == 'dim':
        c = '#C3CBC6'
    elif state == 'danger':
        c = OVD
    isz = icon or round(size * .5)
    return (f'<span style="width:{size}px;height:{size}px;border-radius:{round(size * .32)}px;border:1px solid {br};background:{bg};box-sizing:border-box;'
            f'display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(path, c, isz, 1.9)}</span>')


def list_btn(size=50):
    return (f'<span style="width:{size}px;height:{size}px;border-radius:{round(size * .32)}px;border:1px solid {LINE};display:flex;align-items:center;justify-content:center;'
            f'box-sizing:border-box;flex-shrink:0;">{list_sq(size=round(size * .5))}</span>')


def confirm(size=50):
    return (f'<span style="width:{size}px;height:{size}px;border-radius:{round(size * .32)}px;background:{A};display:flex;align-items:center;justify-content:center;flex-shrink:0;">'
            f'{ico(P_CHECK, "#fff", round(size * .5), 2.6)}</span>')


def toolbar(states=('on', 'on', 'on', 'on'), size=50, extra_left='', delete=False):
    rep, bell, flag, chk = states
    tools = extra_left + list_btn(size) + sq(P_REP, rep, size) + sq(P_BELL, bell, size) + sq(P_FLAG, flag, size) + sq(P_LIST, chk, size)
    right = (sq(P_TRASH, 'danger', size) if delete else '') + confirm(size)
    return f'<div style="display:flex;gap:8px;align-items:center;margin-top:14px;">{tools}<span style="flex:1"></span>{right}</div>'


def sheet(body, bottom=0):
    return (f'<div style="position:absolute;left:0;right:0;bottom:{bottom}px;background:#fff;border-radius:28px 28px 0 0;padding:0 16px 24px;{SHADOW}z-index:10;">{handle()}{body}</div>')


def title(text=None, done_circle=False):
    if done_circle:
        return (f'<div style="display:flex;align-items:center;gap:10px;margin-top:14px;"><span style="width:22px;height:22px;border-radius:50%;border:2px solid {T2};box-sizing:border-box;"></span>'
                f'<span style="font-size:22px;font-weight:800;color:{INK};flex:1;">{text}</span><span style="background:#FFD9D9;color:{OVD};font-weight:700;font-size:13px;padding:5px 8px;border-radius:8px;">P1</span></div>')
    return f'<div style="font-size:22px;font-weight:800;color:{PH if text is None else INK};margin-top:14px;">{text or "Купить билеты на концерт"}</div>'


def desc(text):
    return f'<div style="font-size:16px;color:{T2 if text else PH};margin-top:8px;font-weight:500;">{text or "Описание"}</div>'


def check_row(t, done=False):
    box = (f'<span style="width:20px;height:20px;border-radius:5px;border:2px solid {A if done else T2};background:{A if done else "transparent"};box-sizing:border-box;display:flex;align-items:center;justify-content:center;">'
           f'{ico(P_CHECK, "#fff", 13, 3) if done else ""}</span>')
    return (f'<div style="display:flex;align-items:center;gap:12px;margin-top:12px;">{box}<span style="font-size:15px;color:{T2 if done else INK};{"text-decoration:line-through;" if done else ""}">{t}</span></div>')


# ---------- Окно даты и времени ----------

DAYS = ['пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс']


def week_block(selected, visible, today=(0, 3), rows=None):
    """Блок «2 недели»: ряды пн–вс, текущая и следующая неделя. Прошедшие дни бледные, сегодня — кольцо, выбранный — зелёный,
    три дня, что видны в сетке, — под светлой подложкой. selected/visible — (ряд, колонка)."""
    cells = [[(26, 'окт'), (27, 'окт'), (28, 'окт'), (29, 'окт'), (30, 'окт'), (31, 'окт'), (1, 'нояб')],
             [(2, 'нояб'), (3, 'нояб'), (4, 'нояб'), (5, 'нояб'), (6, 'нояб'), (7, 'нояб'), (8, 'нояб')]]
    W = 54.857
    head = ''.join(f'<span style="width:{W}px;text-align:center;font-size:11px;font-weight:700;color:{PH};">{d}</span>' for d in DAYS)
    out = f'<div style="display:flex;">{head}</div>'
    for ri, rw in enumerate(cells):
        band = ''
        if visible[0] == ri:
            band = (f'<span style="position:absolute;left:{visible[1] * W}px;width:{3 * W}px;top:2px;bottom:2px;border-radius:20px;background:{SEL};"></span>')
        cs = ''
        for ci, (d, m) in enumerate(rw):
            past = ri == 0 and ci < 3
            sel = (ri, ci) == selected
            tod = (ri, ci) == today
            c, fw = (INK, 600)
            if past:
                c = '#C3CBC6'
            if sel:
                c, fw = ('#fff', 800)
            bg = f'background:{A};' if sel else ''
            ring = f'box-shadow:inset 0 0 0 1.5px {A};' if tod and not sel else ''
            lab = f'<span style="position:absolute;bottom:0;left:0;right:0;text-align:center;font-size:9px;font-weight:700;color:{A};">{m}</span>' if d == 1 else ''
            cs += (f'<span style="position:relative;width:{W}px;height:40px;display:flex;align-items:center;justify-content:center;z-index:1;">'
                   f'<span style="width:34px;height:34px;border-radius:50%;{bg}{ring}display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:{fw};color:{c};">{d}</span>{lab}</span>')
        out += f'<div style="position:relative;display:flex;">{band}{cs}</div>'
    return out


def strip_block(selected=0):
    """Вариант Б: 14 дней одной лентой от сегодня (прокручивается вбок)."""
    days = [('чт', 29), ('пт', 30), ('сб', 31), ('вс', 1), ('пн', 2), ('вт', 3), ('ср', 4), ('чт', 5)]
    cs = ''
    for i, (w, d) in enumerate(days):
        sel = i == selected
        bg = f'background:{A};' if sel else ''
        c = '#fff' if sel else INK
        cs += (f'<span style="width:52px;height:62px;border-radius:16px;{bg}border:1px solid {A if sel else LINE};box-sizing:border-box;display:flex;flex-direction:column;align-items:center;justify-content:center;flex-shrink:0;">'
               f'<span style="font-size:11px;font-weight:700;color:{c if sel else PH};">{w}</span><span style="font-size:18px;font-weight:800;color:{c};">{d}</span></span>')
    return f'<div style="display:flex;gap:6px;overflow:hidden;">{cs}</div>'


def window(selected_day, block_html, block_label, hour=7.5, cols=('29 чт', '30 пт', '31 сб'), sel_col=0, chip='07:30'):
    """Окно даты и времени: сетка на 3 дня (как сейчас) + блок дней + кнопки «без даты», «без времени», «готово»."""
    gut, cw = 26, (412 - 26) / 3
    head = ''.join(
        f'<span style="width:{cw}px;height:38px;display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:{800 if i == sel_col else 600};'
        f'color:{A if i == sel_col else INK};background:{SEL if i == sel_col else "transparent"};">{t}</span>' for i, t in enumerate(cols))
    hours = ''.join(f'<div style="position:absolute;left:0;right:0;top:{h * 44}px;height:44px;border-top:1px solid {LINE};"><span style="position:absolute;left:4px;top:2px;font-size:12px;color:{T2};">{h:02d}</span></div>' for h in range(12))
    sel_bg = f'<span style="position:absolute;left:{gut + sel_col * cw}px;width:{cw}px;top:0;bottom:0;background:{SEL};opacity:.7;"></span>'
    vlines = ''.join(f'<span style="position:absolute;left:{gut + i * cw}px;top:0;bottom:0;width:1px;background:{LINE};"></span>' for i in range(1, 3))
    block = (f'<div style="position:absolute;left:{gut + sel_col * cw + 2}px;width:{cw - 4}px;top:{hour * 44}px;height:24px;border-radius:5px;background:{A};color:#fff;font-weight:800;font-size:13px;'
             f'display:flex;align-items:center;justify-content:center;">{chip}</div>'
             f'<span style="position:absolute;left:{gut + (sel_col + 1) * cw - 18}px;top:{hour * 44 - 10}px;width:20px;height:20px;border-radius:50%;background:#fff;border:1.5px solid {A};display:flex;align-items:center;justify-content:center;">{ico("M6 6l12 12M18 6L6 18", A, 11, 2.6)}</span>')
    grid = f'<div style="position:relative;height:{12 * 44}px;overflow:hidden;">{sel_bg}{hours}{vlines}{block}</div>'
    btns = (f'<div style="display:flex;gap:8px;padding:10px 14px 18px;align-items:center;">{sq(P_NODATE)}{sq(P_NOTIME)}<span style="flex:1"></span>{confirm(56)}</div>')
    return (f'<div style="position:absolute;left:0;right:0;top:50px;bottom:0;background:#fff;border-radius:28px 28px 0 0;{SHADOW}overflow:hidden;z-index:10;">{handle()}'
            f'<div style="display:flex;padding-left:{gut}px;margin-top:6px;">{head}</div>{grid}'
            f'<div style="position:absolute;left:0;right:0;bottom:84px;border-top:1px solid {LINE};background:#fff;padding:8px 14px 6px;">'
            f'<div style="font-size:11px;font-weight:700;color:{T2};margin:0 0 4px 2px;">{block_label}</div>{block_html}</div>'
            f'<div style="position:absolute;left:0;right:0;bottom:0;background:#fff;">{btns}</div></div>')


def callout(text, top, left=16, right=16):
    return (f'<div style="position:absolute;left:{left}px;right:{right}px;top:{top}px;z-index:30;font-size:12.5px;line-height:17px;color:#fff;'
            f'background:rgba(21,22,31,.86);border-radius:12px;padding:8px 12px;">{text}</div>')


# ---------- Экраны ----------

def s_new_empty():
    return phone('#EEF1F5', feed() + sheet(title() + desc('2 билета в партер, оплатить до пятницы') + f'<div style="margin-top:16px;">{date_field()}</div>' + toolbar()))


def s_new_set():
    return phone('#EEF1F5', feed() + sheet(title() + desc('2 билета в партер, оплатить до пятницы') + f'<div style="margin-top:16px;">{date_field("чт, 29 окт · 07:30")}</div>' + toolbar(('on', 'sel', 'on', 'on'))))


def s_detail():
    body = (title('Подготовить отчёт', True) + desc('Квартальный отчёт для руководства') + check_row('Собрать данные', True) + check_row('Сверстать таблицу')
            + f'<div style="margin-top:16px;">{date_field("пт, 30 окт · 10:00–11:30")}</div>' + toolbar(('sel', 'sel', 'sel', 'sel'), size=45, delete=True))
    return phone('#EEF1F5', feed() + sheet(body))


def s_icon_only():
    body = (title('Подготовить отчёт', True) + desc('Квартальный отчёт для руководства') + check_row('Собрать данные', True) + check_row('Сверстать таблицу')
            + toolbar(('sel', 'sel', 'sel', 'sel'), size=38, extra_left=sq(P_CAL, 'sel', 38), delete=True))
    return phone('#EEF1F5', feed() + sheet(body) + callout('Только иконка: дату не видно, а в панели 8 кнопок по 38 dp вместо 45–50.', 520))


def s_window():
    return phone('#EEF1F5', feed() + window(0, week_block((0, 3), (0, 3)), 'ЭТА НЕДЕЛЯ И СЛЕДУЮЩАЯ'))


def s_window_far():
    return phone('#EEF1F5', feed() + window(0, week_block((1, 2), (1, 1)), 'ЭТА НЕДЕЛЯ И СЛЕДУЮЩАЯ', cols=('3 вт', '4 ср', '5 чт'), sel_col=1, chip='07:30'))


def s_window_strip():
    return phone('#EEF1F5', feed() + window(0, strip_block(0), 'БЛИЖАЙШИЕ 14 ДНЕЙ (ЛЕНТА ЛИСТАЕТСЯ ВБОК)'))


def notes():
    items = [
        ('Одна кнопка', 'вместо ряда плиток дней и значка месяца: поле «Дата и время». Пусто — подсказка, выбрано — «чт, 29 окт · 07:30» зелёным и крестик «убрать дату».'),
        ('Окно', 'то же, что сейчас открывается по плитке: сетка на 3 дня со временем, кнопки «без даты», «без времени» и зелёная галочка.'),
        ('Блок «2 недели»', 'внизу окна: текущая и следующая неделя, ряды пн–вс. Прошедшие дни бледные и недоступны, сегодня — кольцо, выбранный — зелёный, три дня из сетки — под светлой подложкой.'),
        ('Нажал день в блоке', 'сетка сразу переезжает к нему, выбранное время остаётся. Дата без времени — нажать день и «без времени».'),
        ('Месяц', 'нигде не открывается. Дальше двух недель — листать сетку вбок, как сейчас.'),
        ('Когда уехали за две недели', 'блок показывает две недели с недели выбранного дня (выбранный в первом ряду). Пока выбран день внутри текущих двух недель — блок на месте и не прыгает.'),
    ]
    li = ''.join(f'<div style="display:flex;gap:10px;margin-top:11px;font-size:13.5px;line-height:19px;color:{INK};"><b style="color:{A};min-width:18px;">{i + 1}</b><span><b>{a}:</b> {b}</span></div>' for i, (a, b) in enumerate(items))
    return (f'<div style="width:428px;font-family:Manrope,sans-serif;"><div style="font-size:18px;font-weight:700;color:{INK};">Как это работает</div>{li}'
            f'<div style="font-size:18px;font-weight:700;color:{INK};margin-top:24px;">Что решить</div>'
            f'<div style="font-size:13.5px;line-height:19px;color:{INK};margin-top:8px;"><b>Кнопка:</b> А — поле в шторке (рекомендую) или Б — иконка в панели.<br>'
            f'<b>Блок:</b> А — две недели рядами (рекомендую) или Б — лента на 14 дней.</div></div>')


SCREENS = [
    ('1. Новая задача: пусто (кнопка А)', 'Вместо плиток — одно поле «Дата и время» во всю ширину. Ниже панель как была, без значка календаря.', s_new_empty),
    ('2. Новая задача: дата выбрана', 'Дата и время зелёным, справа крестик «убрать дату». Нажатие на поле открывает окно даты и времени.', s_new_set),
    ('3. Созданная задача (кнопка А)', 'Тот же вид у созданной задачи: поле с датой и периодом, панель из 7 кнопок по 45 dp, как сейчас.', s_detail),
    ('4. Вариант Б: только иконка в панели', 'Иконка календаря первой в панели. Компактнее по высоте, но дата не видна, а кнопки уже: 8 по 38 dp.', s_icon_only),
    ('5. Окно даты и времени (блок А)', 'Сетка на 3 дня со временем и внизу блок «2 недели». Выбрано чт, 29 окт, 07:30. Три видимых дня подсвечены в блоке.', s_window),
    ('6. Выбрали день в блоке', 'Нажали 4 ноября в блоке: сетка переехала к нему (3, 4, 5 ноя), время 07:30 осталось, подложка сместилась.', s_window_far),
    ('7. Вариант блока Б: лента', 'Один ряд из 14 дней, листается вбок: меньше места, но недели не видно целиком.', s_window_strip),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    cell = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    cells = [cell(*s) for s in SCREENS] + [f'<div>{notes()}</div>']
    rows = ''.join(f'<div style="display:flex;gap:30px;margin-bottom:34px;">{"".join(cells[i:i + 4])}</div>' for i in (0, 4))
    W, H = 4 * 428 + 3 * 30 + 80, 2130
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Единое окно даты и времени</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">В шторке задачи одна кнопка вместо плиток дней и значка месяца. '
            f'По нажатию — окно с сеткой на 3 дня и блоком «2 недели» внизу. Месяц больше нигде не открывается.</div>{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Единое окно даты и времени</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantC1.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    fresh = 'VariantC1.dc.html' not in c['boards']
    old = {k: v for k, v in c['boards'].items() if k != 'VariantC1.dc.html'}
    if fresh:  # повторный запуск не сдвигает прежние доски ещё раз
        for v in old.values():
            v['y'] += H + 120
    c['boards'] = {'VariantC1.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Единое окно даты и времени (итерация 33)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Единое окно даты и времени (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
