"""Макет «Вид "Неделя"» (итерация 44): сетка из 7 дней и мини-календарь — в приложении и в виджете.

Решения PM: вход — новая кнопка в нижней панели приложения; неделя выбирается мини-календарём; в ячейке дня все задачи (выполненные зачёркнуты);
в виджете — третья вкладка «Неделя» рядом с «Все» и «Входящие».
Круг 2 (решения PM после проверки 0.44.0): ячейки одинаковой высоты, вся неделя на одном экране без прокрутки; нижняя панель плоская, на всю ширину, на
всём приложении, «+» остаётся в ней; если задач больше, чем помещается, в строке даты справа «+N», нажатие на строку даты открывает полный список дня;
мини-календарь — в последней ячейке снизу справа.
Телефон 412×860 (dp 1:1). Запуск: python3 week_view.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CAL, P_INBOX, P_CHECK
from tasks_polish import A, INK, T2, BG, SEL, LINE, OVD, INACT, PRI, phone, status, P_GEAR, P_LISTI, P_SEARCH, row, timeline

WALL = 'linear-gradient(165deg,#C5D8F0 0%,#BFD2EE 55%,#9FB8DE 100%)'
P_WEEK = 'M4 5h16v14H4zM9.3 5v14M14.7 5v14'  # значок «Неделя»: три колонки в рамке
P_PLUS = 'M12 5v14M5 12h14'
SOFT = '#D3ECDB'  # accent-soft

# Неделя 26 окт – 1 ноя 2026 (сегодня — пятница 30 окт). Задачи: (название, время, цвет кружка, выполнена).
DAYS = [
    ('пн', 26, [('Купить продукты', '10:00', PRI['low'], True), ('Тренировка', '', INACT, False)]),
    ('вт', 27, [('Оплатить интернет', '12:00', PRI['mid'], False), ('Позвонить маме', '', INACT, False)]),
    ('ср', 28, [('Встреча', '15:00', PRI['high'], False), ('Забрать заказ', '', INACT, False)]),
    ('чт', 29, [('Записаться к врачу', '09:30', INACT, True), ('Прогулка', '', INACT, False)]),
    ('пт', 30, [('Отчёт за неделю', '17:00', PRI['high'], False), ('Кино', '', INACT, False), ('Ужин с Аней', '20:00', INACT, False),
                ('Зарядка', '08:00', PRI['mid'], True), ('Позвонить врачу', '', INACT, False), ('Купить билеты', '', INACT, False)]),
    ('сб', 31, [('Уборка', '11:00', INACT, False), ('Купить подарок', '', INACT, False)]),
    ('вс', 1, [('Сходить на тренировку', '02:00', INACT, False), ('План на неделю', '', INACT, False)]),
]


def circle(color, done, size=14):
    if done:
        return (f'<span style="width:{size}px;height:{size}px;border-radius:50%;background:{color};flex-shrink:0;display:flex;align-items:center;justify-content:center;">'
                f'{ico(P_CHECK, "#fff", size - 4, 3)}</span>')
    return f'<span style="width:{size}px;height:{size}px;border-radius:50%;border:2px solid {color};box-sizing:border-box;flex-shrink:0;"></span>'


def task(title, time, color, done, h=32, fs=13, ts=11):
    st = 'text-decoration:line-through;' if done else ''
    op = 'opacity:.7;' if done else ''
    t = f'<span style="font-size:{ts}px;font-weight:600;color:{T2};margin-left:6px;">{time}</span>' if time else ''
    return (f'<div style="height:{h}px;margin-top:4px;border-radius:10px;background:#fff;display:flex;align-items:center;gap:7px;padding:0 8px;box-sizing:border-box;{op}">'
            f'{circle(color, done)}<span style="flex:1;min-width:0;font-size:{fs}px;font-weight:700;color:{INK};white-space:nowrap;overflow:hidden;text-overflow:ellipsis;{st}">{title}</span>{t}</div>')


def head(wd, num, today=False, hidden=0):
    """Строка даты: «пн 26» слева (сегодня — зелёная метка), справа «+N» — сколько задач не поместилось. Нажатие на строку открывает день."""
    plus = (f'<span style="margin-left:auto;font-size:12px;font-weight:700;color:{A};background:{SOFT};border-radius:999px;padding:1px 8px;">+{hidden}</span>' if hidden else '')
    if today:
        pill = f'<span style="font-size:12px;font-weight:700;color:#fff;background:{A};border-radius:999px;padding:2px 9px;margin-left:6px;">{num}</span>'
        return f'<div style="display:flex;align-items:center;height:26px;"><span style="font-size:14px;font-weight:800;color:{A};">{wd}</span>{pill}{plus}</div>'
    return (f'<div style="display:flex;align-items:center;height:26px;"><span style="font-size:14px;font-weight:800;color:{INK};">{wd}</span>'
            f'<span style="font-size:13px;font-weight:600;color:{T2};margin-left:6px;">{num}</span>{plus}</div>')


def cell(d, cap=None, h=None, fs=13, th=32):
    """Ячейка дня. h — фиксированная высота (ячейки недели одинаковые); cap — сколько задач помещается, остальное — «+N» в строке даты."""
    wd, num, tasks = d
    today = num == 30
    shown = tasks if cap is None else tasks[:cap]
    hidden = len(tasks) - len(shown)
    body = ''.join(task(*t, h=th, fs=fs) for t in shown)
    hh = f'height:{h}px;overflow:hidden;' if h else ''
    return (f'<div style="flex:1;min-width:0;{hh}box-sizing:border-box;padding:8px 6px 8px;border-right:1px solid {LINE};border-bottom:1px solid {LINE};">'
            f'{head(wd, num, today, hidden)}{body}</div>')


def mini_calendar(rows_h=24, fs=12, title_h=24, h=None):
    """Месяц (октябрь 2026): заголовок с ‹ ›, дни недели, шесть строк недель; выбранная неделя — полоса accent-soft, сегодня — зелёный кружок."""
    wk = ''.join(f'<span style="flex:1;text-align:center;font-size:10px;font-weight:600;color:{INACT};">{x}</span>' for x in ('пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'))
    weeks = [[28, 29, 30, 1, 2, 3, 4], [5, 6, 7, 8, 9, 10, 11], [12, 13, 14, 15, 16, 17, 18], [19, 20, 21, 22, 23, 24, 25], [26, 27, 28, 29, 30, 31, 1], [2, 3, 4, 5, 6, 7, 8]]
    out = ''
    for i, wkk in enumerate(weeks):
        band = f'background:{SOFT};border-radius:{rows_h // 2}px;' if i == 4 else ''
        cells = ''
        for j, n in enumerate(wkk):
            other = (i == 0 and j < 3) or (i == 4 and j == 6) or i == 5
            col = INACT if other else INK
            if i == 4 and n == 30:
                d = rows_h - 4
                cells += (f'<span style="flex:1;display:flex;justify-content:center;"><span style="width:{d}px;height:{d}px;border-radius:50%;background:{A};color:#fff;'
                          f'font-size:{fs}px;font-weight:700;display:flex;align-items:center;justify-content:center;">{n}</span></span>')
            else:
                cells += f'<span style="flex:1;text-align:center;font-size:{fs}px;font-weight:600;color:{col};">{n}</span>'
        out += f'<div style="display:flex;align-items:center;height:{rows_h}px;{band}">{cells}</div>'
    nav_btn = lambda p: f'<span style="width:24px;height:{title_h}px;display:flex;align-items:center;justify-content:center;">{ico(p, T2, 16, 2.2)}</span>'
    top = (f'<div style="display:flex;align-items:center;justify-content:space-between;height:{title_h}px;">{nav_btn("M15 6l-6 6 6 6")}'
           f'<span style="font-size:13px;font-weight:700;color:{INK};">октябрь 2026</span>{nav_btn(P_CHEV)}</div>')
    hh = f'height:{h}px;overflow:hidden;' if h else ''
    return (f'<div style="flex:1;min-width:0;{hh}box-sizing:border-box;padding:4px 6px;border-bottom:1px solid {LINE};">{top}'
            f'<div style="display:flex;height:14px;align-items:center;">{wk}</div>{out}</div>')


P_CHEV = 'M9 6l6 6-6 6'


def nav_flat(selected='week'):
    """Нижняя панель (решение PM): не парит, а лежит внизу на всю ширину — без капсулы и тени, сверху тонкая линия; «+» — в конце панели."""
    def t(path, key):
        c = A if key == selected else '#6B7280'
        return f'<span style="flex:1;height:56px;display:flex;align-items:center;justify-content:center;">{ico(path, c, 24, 2)}</span>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:56px;background:#fff;border-top:1px solid {LINE};display:flex;align-items:center;padding:0 8px 0 4px;z-index:2;">'
            f'{t(P_INBOX, "inbox")}{t(P_CAL, "all")}{t(P_WEEK, "week")}{t(P_LISTI, "lists")}{t(P_SEARCH, "search")}{t(P_GEAR, "settings")}'
            f'<span style="width:48px;height:40px;margin-left:4px;border-radius:14px;background:{A};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(P_PLUS, "#fff", 22, 2.4)}</span></div>')


def week_grid(row_h, cap, fs=13, th=32, cal=None):
    """Четыре строки одинаковой высоты: Пн–Вт, Ср–Чт, Пт–Сб, Вс и мини-календарь."""
    rows = ''
    for i in range(0, 6, 2):
        rows += f'<div style="display:flex;">{cell(DAYS[i], cap, row_h, fs, th)}{cell(DAYS[i + 1], cap, row_h, fs, th)}</div>'
    rows += f'<div style="display:flex;">{cell(DAYS[6], cap, row_h, fs, th)}{cal or mini_calendar(h=row_h)}</div>'
    return rows


def screen_app():
    # Экран 860 − строка состояния 34 − панель 56 = 770: четыре строки по 192. В ячейку помещается 4 плашки.
    inner = (status() + f'<div style="position:absolute;top:34px;left:0;right:0;bottom:56px;overflow:hidden;border-top:1px solid {LINE};">'
             f'{week_grid(row_h=192, cap=4)}</div>' + nav_flat('week'))
    return phone(BG, inner)


def screen_widget():
    P_TAB = lambda p, on: (f'<span style="width:52px;height:36px;border-radius:12px;background:{SEL if on else "#fff"};display:flex;align-items:center;justify-content:center;">'
                           f'{ico(p, A if on else "#6B7280", 22, 2)}</span>')
    sw = (f'<span style="height:44px;border-radius:16px;background:#fff;display:flex;align-items:center;padding:0 4px;gap:2px;">'
          f'{P_TAB(P_CAL, False)}{P_TAB(P_INBOX, False)}{P_TAB(P_WEEK, True)}</span>')
    check = f'<svg width="26" height="22" viewBox="0 0 43 36"><path d="M5 19L16 30L38 7" stroke="{A}" stroke-width="7" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>'
    bar = (f'<div style="position:absolute;z-index:3;left:10px;right:10px;bottom:4px;display:flex;align-items:center;">{sw}<span style="flex:1"></span>'
           f'<span style="width:56px;height:40px;border-radius:16px;background:#fff;display:flex;align-items:center;justify-content:center;margin-right:8px;">{check}</span>'
           f'<span style="width:64px;height:40px;border-radius:16px;background:{A};display:flex;align-items:center;justify-content:center;">{ico(P_PLUS, "#fff", 22, 2.4)}</span></div>')
    grid = week_grid(row_h=146, cap=3, fs=12, th=28, cal=mini_calendar(rows_h=17, fs=10, title_h=20, h=146))
    wg = (f'<div style="position:absolute;left:10px;right:10px;top:46px;height:598px;border-radius:30px;overflow:hidden;background:{BG};">'
          f'<div style="padding:6px 0 0 0;">{grid}</div>{bar}</div>')
    apps = ''.join(f'<span style="width:62px;height:62px;border-radius:19px;background:{c};"></span>' for c in ('#F6D90A', '#2AA8E0', '#D6409F', '#3D6FE0'))
    return phone(WALL, status('#fff') + wg + f'<div style="position:absolute;left:0;right:0;bottom:52px;display:flex;justify-content:space-around;padding:0 18px;">{apps}</div>')


def screen_day():
    """Полный список дня: открывается нажатием на строку даты в ячейке (вложенный экран с «Назад», без нижней панели)."""
    back = f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center;">{ico("M15 6l-6 6 6 6", INK, 24, 2.2)}</span>'
    chip = f'<span style="font-size:12px;font-weight:700;color:#fff;background:{A};border-radius:999px;padding:2px 9px;margin-left:8px;">Сегодня</span>'
    top = (f'<div style="display:flex;align-items:center;height:56px;padding-right:16px;">{back}<span style="font-size:20px;font-weight:800;color:{INK};">пт, 30 окт</span>{chip}'
           f'<span style="margin-left:auto;font-size:13px;font-weight:600;color:{T2};">6 задач</span></div>')
    rows = (row('Кино', kind='dot', ring=INACT) + row('Позвонить врачу', kind='dot', ring=INACT) + row('Купить билеты', kind='dot', ring=INACT)
            + row('Зарядка', '08:00', kind='dot', ring=PRI['mid'], done=True) + row('Отчёт за неделю', '17:00', kind='dot', ring=PRI['high']) + row('Ужин с Аней', '20:00', kind='dot', ring=INACT))
    return phone(BG, status() + top + f'<div style="padding-top:4px;">{timeline(rows)}</div>')


def spec():
    def box(title, inner, w=412):
        return f'<div style="margin-bottom:18px;"><div style="font-size:12px;font-weight:700;color:{T2};margin-bottom:4px;">{title}</div><div style="width:{w}px;background:{BG};border:1px solid {LINE};border-radius:14px;overflow:hidden;">{inner}</div></div>'
    empty = ('вт', 27, [])
    done_day = ('чт', 29, [('Записаться к врачу', '09:30', INACT, True), ('Позвонить маме', '', INACT, True)])
    long_t = ('пн', 26, [('Очень длинное название задачи, которое не помещается', '10:00', PRI['high'], False)])
    cells = (f'<div style="display:flex;">{cell(DAYS[0], h=118)}{cell(DAYS[4], cap=2, h=118)}</div>'
             f'<div style="display:flex;">{cell(empty, h=64)}{cell(long_t, h=64)}</div>')
    flat = f'<div style="position:relative;height:56px;">{nav_flat("week").replace("position:absolute;left:0;right:0;bottom:0;", "position:absolute;left:0;right:0;top:0;")}</div>'
    return (box('Ячейка дня: обычный, сегодня с «+N», пустой, длинное название', cells)
            + box('Выполненные: зачёркнуты, бледнее (70%), кружок залит', f'<div style="display:flex;">{cell(done_day, h=100)}<div style="flex:1;"></div></div>')
            + box('Нижняя панель: на всю ширину, без капсулы и тени, сверху линия; «+» в конце', flat)
            + f'<div style="font-size:13px;color:{T2};line-height:19px;width:412px;">Ячейки недели одинаковой высоты: четыре строки делят экран между строкой состояния и панелью. '
              f'В ячейку помещается столько плашек, сколько влезает по высоте; остальное — «+N» справа в строке даты. Нажатие на строку даты открывает полный список дня. '
              f'Панель меняется на всём приложении.</div>')


SCREENS = [
    ('1. Приложение: «Неделя»', 'Четыре строки одинаковой высоты, всё на одном экране, без прокрутки. Панель внизу плоская. В пятнице шесть задач: четыре видно и «+2».', screen_app),
    ('2. Виджет: «Неделя»', 'Те же ячейки одинаковой высоты, в дне до 3 задач и «+N». Мини-календарь в последней ячейке — мельче. Вторая часть итерации.', screen_widget),
    ('3. Полный список дня', 'Нажал на строку даты в ячейке (или на «+2») — открывается весь день, как на «Все»: «весь день», потом по времени. «Назад» возвращает на неделю.', screen_day),
    ('4. Ячейки и панель', 'Состояния ячейки, «+N» в строке даты и плоская нижняя панель.', spec),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:74px;width:428px;margin-bottom:10px;">{s}</div>'
    cell_ = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    rows = f'<div style="display:flex;gap:30px;">{"".join(cell_(*s) for s in SCREENS)}</div>'
    W, H = 4 * 428 + 3 * 30 + 80, 1230
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Вид «Неделя»: приложение и виджет — круг 2</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Решения PM: новая кнопка в нижней панели; неделя выбирается мини-календарём; в ячейке все задачи дня с выполненными; '
            f'в виджете — третья вкладка «Неделя». Круг 2: ячейки одинаковые и на одном экране, плоская панель, «+N» и полный список дня.</div>{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Вид «Неделя»</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantK1.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    fresh = 'VariantK1.dc.html' not in c['boards']
    old = {k: v for k, v in c['boards'].items() if k != 'VariantK1.dc.html'}
    if fresh:  # повторный запуск не сдвигает прежние доски ещё раз
        for v in old.values():
            v['y'] += H + 120
    c['boards'] = {'VariantK1.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Вид «Неделя»: приложение и виджет (итерация 44)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Вид «Неделя» (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
