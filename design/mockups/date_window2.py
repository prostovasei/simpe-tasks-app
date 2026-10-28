"""Макет «Окно даты и времени: листание недель, календарь на 2 месяца, колокольчик «несколько»» (итерация 33, круг 2).

Решения PM: кнопка-поле и блок «2 недели» — вариант А. Добавлено: стрелки над блоком (следующие/прошлые 2 недели),
кнопка «Календарь» (окно на ближайшие 2 месяца), значок «несколько уведомлений» у колокольчика (4 варианта).
Запуск: python3 date_window2.py <папка с canvas.json>
"""
import os, sys, json, datetime as dt
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CAL, P_BELL
from tasks_polish import A, INK, T2, BG, SEL, INACT, phone
from date_window import (LINE, PH, SHADOW, P_NODATE, P_NOTIME, feed, handle, sq, confirm, window as _unused)  # noqa: F401

TODAY = dt.date(2026, 10, 29)
MONTHS = ['янв', 'фев', 'мар', 'апр', 'мая', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек']
MONTHS_FULL = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь']
DAYS = ['пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс']
W7 = 54.857
P_MONTH = 'M4 6h16v14H4zM4 10h16M9 3v4M15 3v4M8 14h.01M12 14h.01M16 14h.01M8 17h.01M12 17h.01'


def monday(d):
    return d - dt.timedelta(days=d.weekday())


def short(d):
    return f'{d.day} {MONTHS[d.month - 1]}'


def round_btn(path, enabled=True):
    c = INK if enabled else '#C3CBC6'
    return (f'<span style="width:34px;height:34px;border-radius:50%;border:1px solid {LINE};background:#fff;box-sizing:border-box;display:flex;align-items:center;justify-content:center;flex-shrink:0;">'
            f'{ico(path, c, 18, 2.2)}</span>')


def block_head(first, prev_enabled):
    last = first + dt.timedelta(days=13)
    label = f'{short(first)} – {short(last)}'
    cal = (f'<span style="height:34px;padding:0 12px;border-radius:17px;background:{SEL};display:flex;align-items:center;gap:6px;font-size:13px;font-weight:700;color:{A};flex-shrink:0;">'
           f'{ico(P_MONTH, A, 17, 1.9)}Календарь</span>')
    return (f'<div style="display:flex;align-items:center;gap:6px;margin-bottom:6px;">{round_btn("M15 6l-6 6 6 6", prev_enabled)}'
            f'<span style="font-size:13px;font-weight:700;color:{INK};min-width:104px;text-align:center;">{label}</span>{round_btn("M9 6l6 6-6 6")}<span style="flex:1"></span>{cal}</div>')


def week_rows(first, selected=None, band_from=None):
    """Две недели с понедельника [first]: прошедшие — бледные, сегодня — кольцо, выбранный — зелёный, три дня сетки — подложка."""
    head = ''.join(f'<span style="width:{W7}px;text-align:center;font-size:11px;font-weight:700;color:{PH};">{d}</span>' for d in DAYS)
    out = f'<div style="display:flex;">{head}</div>'
    for r in range(2):
        band = ''
        if band_from is not None:
            col = (band_from - (first + dt.timedelta(days=7 * r))).days
            if 0 <= col <= 4:
                band = f'<span style="position:absolute;left:{col * W7}px;width:{3 * W7}px;top:2px;bottom:2px;border-radius:20px;background:{SEL};"></span>'
        cs = ''
        for c in range(7):
            d = first + dt.timedelta(days=7 * r + c)
            past, sel, tod = d < TODAY, d == selected, d == TODAY
            color, fw = ('#C3CBC6' if past else INK), 600
            if sel:
                color, fw = '#fff', 800
            bg = f'background:{A};' if sel else ''
            ring = f'box-shadow:inset 0 0 0 1.5px {A};' if tod and not sel else ''
            lab = (f'<span style="position:absolute;bottom:0;left:0;right:0;text-align:center;font-size:9px;font-weight:700;color:{A};">{MONTHS[d.month - 1]}</span>' if d.day == 1 else '')
            cs += (f'<span style="position:relative;width:{W7}px;height:40px;display:flex;align-items:center;justify-content:center;z-index:1;">'
                   f'<span style="width:34px;height:34px;border-radius:50%;{bg}{ring}display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:{fw};color:{color};">{d.day}</span>{lab}</span>')
        out += f'<div style="position:relative;display:flex;">{band}{cs}</div>'
    return out


def date_window(first, selected, grid_first, prev_enabled, sel_col, chip='07:30', hour=7.5):
    """Окно: сетка на 3 дня (с [grid_first]) + блок «2 недели» со стрелками и кнопкой «Календарь» + кнопки внизу."""
    gut, cw = 26, (412 - 26) / 3
    cols = [grid_first + dt.timedelta(days=i) for i in range(3)]
    names = [f'{d.day} {DAYS[d.weekday()]}' for d in cols]
    head = ''.join(
        f'<span style="width:{cw}px;height:38px;display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:{800 if i == sel_col else 600};'
        f'color:{A if i == sel_col else INK};background:{SEL if i == sel_col else "transparent"};">{t}</span>' for i, t in enumerate(names))
    hours = ''.join(f'<div style="position:absolute;left:0;right:0;top:{h * 44}px;height:44px;border-top:1px solid {LINE};"><span style="position:absolute;left:4px;top:2px;font-size:12px;color:{T2};">{h:02d}</span></div>' for h in range(12))
    vlines = ''.join(f'<span style="position:absolute;left:{gut + i * cw}px;top:0;bottom:0;width:1px;background:{LINE};"></span>' for i in range(1, 3))
    sel_bg = f'<span style="position:absolute;left:{gut + sel_col * cw}px;width:{cw}px;top:0;bottom:0;background:{SEL};opacity:.7;"></span>'
    block = (f'<div style="position:absolute;left:{gut + sel_col * cw + 2}px;width:{cw - 4}px;top:{hour * 44}px;height:24px;border-radius:5px;background:{A};color:#fff;font-weight:800;font-size:13px;'
             f'display:flex;align-items:center;justify-content:center;">{chip}</div>')
    grid = f'<div style="position:relative;height:{12 * 44}px;overflow:hidden;">{sel_bg}{hours}{vlines}{block}</div>'
    btns = f'<div style="display:flex;gap:8px;padding:10px 14px 18px;align-items:center;">{sq(P_NODATE)}{sq(P_NOTIME)}<span style="flex:1"></span>{confirm(56)}</div>'
    return (f'<div style="position:absolute;left:0;right:0;top:50px;bottom:0;background:#fff;border-radius:28px 28px 0 0;{SHADOW}overflow:hidden;z-index:10;">{handle()}'
            f'<div style="display:flex;padding-left:{gut}px;margin-top:6px;">{head}</div>{grid}'
            f'<div style="position:absolute;left:0;right:0;bottom:84px;border-top:1px solid {LINE};background:#fff;padding:10px 14px 6px;">'
            f'{block_head(first, prev_enabled)}{week_rows(first, selected, grid_first if selected is not None else None)}</div>'
            f'<div style="position:absolute;left:0;right:0;bottom:0;background:#fff;">{btns}</div></div>')


def month_grid(year, month, selected, cell=47.4):
    first = dt.date(year, month, 1)
    start = first - dt.timedelta(days=first.weekday())
    rows = ''
    d = start
    while d.month == month or d <= first or (d - dt.timedelta(days=1)).month == month and d.weekday() != 0:
        if d > first and d.month != month and d.weekday() == 0:
            break
        cs = ''
        for _ in range(7):
            if d.month != month:
                cs += f'<span style="width:{cell}px;height:38px;"></span>'
            else:
                past, sel, tod = d < TODAY, d == selected, d == TODAY
                color, fw = ('#C3CBC6' if past else INK), 600
                if sel:
                    color, fw = '#fff', 800
                bg = f'background:{A};' if sel else ''
                ring = f'box-shadow:inset 0 0 0 1.5px {A};' if tod and not sel else ''
                cs += (f'<span style="width:{cell}px;height:38px;display:flex;align-items:center;justify-content:center;">'
                       f'<span style="width:34px;height:34px;border-radius:50%;{bg}{ring}display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:{fw};color:{color};">{d.day}</span></span>')
            d += dt.timedelta(days=1)
        rows += f'<div style="display:flex;">{cs}</div>'
    return f'<div style="font-size:15px;font-weight:800;color:{INK};margin:10px 0 2px;">{MONTHS_FULL[month - 1]} {year}</div>{rows}'


def calendar_dialog(selected):
    cell = 47.4
    head = ''.join(f'<span style="width:{cell}px;text-align:center;font-size:11px;font-weight:700;color:{PH};">{d}</span>' for d in DAYS)
    btns = (f'<span style="flex:1;height:50px;border-radius:16px;border:1px solid {LINE};box-sizing:border-box;display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:700;color:{INK};">Отмена</span>'
            f'<span style="flex:1;height:50px;border-radius:16px;background:{A};display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:700;color:#fff;">Готово</span>')
    return ('<div style="position:absolute;inset:0;background:rgba(21,22,31,.42);z-index:20;"></div>'
            '<div style="position:absolute;inset:34px 0 0;z-index:21;display:flex;align-items:center;justify-content:center;">'
            f'<div style="width:364px;border-radius:28px;background:#fff;{SHADOW}overflow:hidden;">'
            f'<div style="padding:22px 16px 6px;"><div style="font-size:21px;font-weight:700;color:{INK};padding:0 6px;">Календарь</div>'
            f'<div style="font-size:14px;color:{T2};padding:2px 6px 8px;">Ближайшие 2 месяца</div>'
            f'<div style="display:flex;">{head}</div>{month_grid(2026, 10, selected)}{month_grid(2026, 11, selected)}</div>'
            f'<div style="display:flex;gap:10px;padding:14px 22px 22px;border-top:1px solid {LINE};">{btns}</div></div></div>')


def callout(text, top):
    return (f'<div style="position:absolute;left:16px;right:16px;top:{top}px;z-index:30;font-size:12.5px;line-height:17px;color:#fff;'
            f'background:rgba(21,22,31,.86);border-radius:12px;padding:8px 12px;">{text}</div>')


# ---------- Колокольчик: «в срок», «заранее», «несколько» ----------

_mask = [0]
BELL = 'M6,10a6,6 0 0 1 12,0c0,5 2,7 2,7H4c0,0 2,-2 2,-7 M10,20.5a2,2 0 0 0 4,0'
ARROW = 'M15.4,2.4L13.2,4.6L15.4,6.8 M13.6,4.6H18.8a2.9,2.9 0 0 1 0,5.8H17.6'


def bell(size, tint='#6B7280', mark=A, kind='at', variant='A', bg='#fff'):
    """Колокольчик 1:1 с приложением (стрелка «заранее» справа вверху, вокруг неё вырез) + значок «несколько» по варианту.
    kind: at — в срок, early — заранее (одно), multi — несколько; variant: А плюс, Б двойной колокольчик, В штрихи, Г точка."""
    _mask[0] += 1
    mid = f'm{_mask[0]}'
    early = kind in ('early', 'multi')
    multi = kind == 'multi'
    cut = ''
    if early:
        cut += f'<path d="{ARROW}" stroke="#000" stroke-width="4.6" stroke-linecap="round" stroke-linejoin="round" fill="none"/><rect x="13.6" y="4.6" width="8.1" height="5.8" fill="#000"/>'
    if multi and variant == 'A':
        cut += '<circle cx="19" cy="19" r="6.4" fill="#000"/>'
    if multi and variant == 'C':
        cut += '<rect x="13.8" y="14.6" width="9.6" height="9.2" rx="2" fill="#000"/>'
    if multi and variant == 'D':
        cut += '<circle cx="19.2" cy="19.2" r="5" fill="#000"/>'
    out = f'<defs><mask id="{mid}"><rect width="24" height="24" fill="#fff"/>{cut}</mask></defs>'
    if multi and variant == 'B':
        out += f'<g transform="translate(-3.4 1.6)" opacity=".42"><path d="{BELL}" fill="none" stroke="{tint}" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/></g>'
        out += f'<path d="{BELL}" fill="none" stroke="{bg}" stroke-width="4.8" stroke-linecap="round" stroke-linejoin="round"/>'
    out += f'<g mask="url(#{mid})"><path d="{BELL}" fill="none" stroke="{tint}" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/></g>'
    if early:
        out += f'<path d="{ARROW}" fill="none" stroke="{mark}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
    if multi and variant == 'A':
        out += f'<circle cx="19" cy="19" r="4.3" fill="{mark}"/><path d="M19 16.9v4.2M16.9 19h4.2" stroke="#fff" stroke-width="1.6" stroke-linecap="round"/>'
    if multi and variant == 'C':
        out += f'<path d="M15.6 17h5.6M15.6 19.6h5.6M15.6 22.2h5.6" stroke="{mark}" stroke-width="1.7" stroke-linecap="round"/>'
    if multi and variant == 'D':
        out += f'<circle cx="19.2" cy="19.2" r="3.1" fill="{mark}"/>'
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none">{out}</svg>'


def list_row(title, icon_html):
    """Плашка задачи в ленте с колокольчиком и значком списка."""
    return (f'<span style="width:230px;height:40px;border-radius:12px;background:#fff;border:1px solid {LINE};box-sizing:border-box;display:flex;align-items:center;gap:9px;padding:0 11px;">'
            f'<span style="flex:1;font-size:15px;font-weight:600;color:{INK};">{title}</span>{icon_html}'
            f'<span style="width:18px;height:18px;border-radius:5px;background:#3D8BFD;display:flex;align-items:center;justify-content:center;">{ico("M3 13h5l2 3h4l2-3h5M5 5h14l2 8v6H3v-6z", "#fff", 12, 2)}</span></span>')


def tool_btn(icon_html, selected=True):
    bg, br = (SEL, SEL) if selected else ('#fff', LINE)
    return (f'<span style="width:50px;height:50px;border-radius:16px;background:{bg};border:1px solid {br};box-sizing:border-box;display:flex;align-items:center;justify-content:center;flex-shrink:0;">{icon_html}</span>')


KINDS = [('at', 'Один, в момент срока'), ('early', 'Один, заранее'), ('multi', 'Несколько')]
VARIANTS = [
    ('A', 'А. Плюс', 'Маленький зелёный кружок с «+» справа внизу: «есть ещё». Понятнее всего, но мелкий на 18 dp.'),
    ('B', 'Б. Двойной колокольчик', 'Второй колокольчик бледно виден за первым. Без новых знаков, читается как «стопка».'),
    ('C', 'В. Штрихи', 'Три коротких штриха справа внизу, как «список». Нейтрально, но тоньше всех.'),
    ('D', 'Г. Точка', 'Зелёная точка справа внизу. Проще всех, но может читаться как «новое».'),
]


def bell_panel():
    kind_head = ''.join(f'<div style="width:430px;font-size:13px;font-weight:700;color:{T2};">{t}</div>' for _, t in KINDS)
    rows = ''
    for v, name, desc in VARIANTS:
        cells = ''
        for k, _ in KINDS:
            r18 = list_row('Тест', bell(18, kind=k, variant=v))
            tb = tool_btn(bell(25, tint=A, kind=k, variant=v, bg=SEL))
            big = f'<span style="width:64px;height:64px;border-radius:16px;background:#fff;border:1px solid {LINE};display:flex;align-items:center;justify-content:center;">{bell(48, kind=k, variant=v)}</span>'
            cells += f'<div style="width:430px;display:flex;align-items:center;gap:14px;">{r18}{tb}{big}</div>'
        rows += (f'<div style="display:flex;align-items:center;padding:16px 0;border-top:1px solid {LINE};"><div style="width:320px;padding-right:20px;">'
                 f'<div style="font-size:16px;font-weight:700;color:{INK};">{name}</div><div style="font-size:13px;line-height:18px;color:{T2};margin-top:3px;">{desc}</div></div>{cells}</div>')
    note = (f'<div style="font-size:13.5px;line-height:19px;color:{T2};margin-top:14px;">«Заранее» — стрелка назад справа вверху (как сейчас). «Несколько» — когда у задачи настроено два и больше уведомлений; '
            f'всегда идёт вместе со стрелкой, потому что два в момент срока быть не могут. Один размер на плашке ленты и виджета, кнопка панели и крупно.</div>')
    return (f'<div style="background:#fff;border-radius:24px;padding:22px 28px 18px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:20px;font-weight:700;color:{INK};">Колокольчик: несколько уведомлений</div>'
            f'<div style="display:flex;margin:14px 0 8px 320px;">{kind_head}</div>{rows}{note}</div>')


# ---------- Экраны ----------

FIRST = monday(TODAY)  # пн, 26 окт
NEXT = FIRST + dt.timedelta(days=14)  # пн, 9 ноя


def s_window():
    return phone('#EEF1F5', feed() + date_window(FIRST, TODAY, TODAY, False, 0))


def s_next_page():
    return phone('#EEF1F5', feed() + date_window(NEXT, None, TODAY, True, 0)
                 + callout('Нажали «›»: блок листает на следующие 2 недели. «‹» на первой странице бледная (в прошлое не пускаем).', 330))


def s_picked_far():
    d = dt.date(2026, 11, 12)
    return phone('#EEF1F5', feed() + date_window(NEXT, d, d - dt.timedelta(days=1), True, 1))


def s_calendar():
    return phone('#EEF1F5', feed() + date_window(FIRST, TODAY, TODAY, False, 0) + calendar_dialog(TODAY))


SCREENS = [
    ('1. Окно: стрелки и «Календарь»', 'Над блоком: «‹ 26 окт – 8 нояб ›» и кнопка «Календарь». «‹» бледная: в прошлое листать нельзя.', s_window),
    ('2. Листаем на 2 недели вперёд', '«›» показывает 9–22 ноября. Выбранный день (29 окт) остаётся в сетке, в блоке его нет — подсветки нет.', s_next_page),
    ('3. Выбрали 12 ноября', 'Нажали день: сетка переехала к нему (11, 12, 13 ноя), время 07:30 осталось, в блоке подсвечены те же три дня.', s_picked_far),
    ('4. «Календарь»: 2 месяца', 'Окно по центру в едином виде: октябрь и ноябрь, прошедшие дни бледные. Выбрал день → «Готово» → сетка у этого дня.', s_calendar),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    phones = ''.join(f'<div>{lab(t)}{sub(d)}{f()}</div>' for t, d, f in SCREENS)
    W, H = 4 * 428 + 3 * 30 + 80, 1700
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Окно даты и времени и колокольчик — круг 2</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Кнопка-поле и блок «2 недели» — вариант А (решение PM). Добавлены стрелки, календарь на 2 месяца и значок «несколько уведомлений».</div>'
            f'<div style="display:flex;gap:30px;margin-bottom:34px;">{phones}</div>{bell_panel()}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Окно даты и колокольчик</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantC2.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    fresh = 'VariantC2.dc.html' not in c['boards']
    old = {k: v for k, v in c['boards'].items() if k != 'VariantC2.dc.html'}
    if fresh:  # повторный запуск не сдвигает прежние доски ещё раз
        for v in old.values():
            v['y'] += H + 120
    c['boards'] = {'VariantC2.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Окно даты и колокольчик — круг 2 (итерация 33)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Окно даты и колокольчик, круг 2 (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
