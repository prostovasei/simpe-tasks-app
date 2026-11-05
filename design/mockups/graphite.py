"""Макет «Приоритеты графитом» (итерация 46, доска VariantG1).

Решения PM: приоритет без цвета (сейчас «очень цветасто»). В «Неделе» — вариант 2 «Контрастный графит» из рефа PM: фон строки
по уровню (высокий — почти чёрный с белым текстом, средний — средне-серый, низкий — светло-серый, обычный — белый). В «Все»,
«Входящих» и списках — вариант А: плашки белые, кружок приоритета — темнотой (высокий — чёрный и толще, средний — тёмно-серый,
низкий — светло-серый, обычный — бледный); выполненная — заливка тем же серым. Квадратики списков остаются цветными.
Тёмная тема — наоборот: высокий светлый. Просроченное время — красное (на тёмной строке — светло-красное).
Запуск: python3 graphite.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CAL, P_INBOX, P_CHECK, P_REP, P_BELL, P_FLAG
from tasks_polish import phone, status, P_GEAR, P_LISTI, P_SEARCH
import week_black as wb
import week_black2 as wb2
from week_black import LIGHT, DARK, P_WEEK, P_PLUS, WALL

# Кружки по уровню (вариант А) и строки «Недели» (вариант 2): светлая / тёмная тема.
RING = {
    'light': {'high': '#16171D', 'mid': '#5F626B', 'low': '#A3A6AD', 'none': '#C9CBCF'},
    'dark': {'high': '#F2F2F2', 'mid': '#B3B5BA', 'low': '#7A7D84', 'none': '#55575C'},
}
ROW = {  # фон, текст, кружок, время
    'light': {'high': ('#2E2F33', '#FFFFFF', '#FFFFFF', '#D8D9DC'), 'mid': ('#D0D1D4', '#16171D', '#16171D', '#4B4E56'),
              'low': ('#EAEBEC', '#16171D', '#A3A6AD', '#62656E'), 'none': ('#FFFFFF', '#16171D', '#C9CBCF', '#62656E')},
    'dark': {'high': ('#E4E5E7', '#16171D', '#16171D', '#4B4E56'), 'mid': ('#4A4C51', '#F2F2F2', '#F2F2F2', '#C9CBCF'),
             'low': ('#2E3034', '#ECECEC', '#7A7D84', '#A3A3A3'), 'none': ('#1E1E1E', '#ECECEC', '#55575C', '#A3A3A3')},
}
OVD_ON_DARK = '#FF8A80'
THEME = {'light': LIGHT, 'dark': DARK}


def week_task(theme):
    """Строка «Недели» варианта 2 (подменяет week_black.task)."""
    C = THEME[theme]

    def task(C_, title, time, pri, done, ghost=False, past=False, h=22, fs=13, ts=11, gap=3):
        p = {'mid': 'mid'}.get(pri, pri)
        bg, tc, ring, timec = ROW[theme]['none' if done else p]
        dark_row = (theme == 'light' and p == 'high' and not done) or (theme == 'dark' and p == 'mid' and not done)
        if done:
            tc, timec = C['INACT'], C['INACT']
            c = (f'<span style="width:13px;height:13px;border-radius:50%;background:{RING[theme]["none"]};flex-shrink:0;display:flex;align-items:center;justify-content:center;">'
                 f'{ico(P_CHECK, C["SURF"], 9, 3)}</span>')
        else:
            w = 2.5 if p == 'high' else 2
            c = f'<span style="width:13px;height:13px;border-radius:50%;border:{w}px solid {ring};box-sizing:border-box;flex-shrink:0;"></span>'
        if past and not done:
            timec = OVD_ON_DARK if dark_row else C['OVD']
        st = 'text-decoration:line-through;' if done else ''
        op = 'opacity:.5;' if ghost else ''
        t = f'<span style="font-size:{ts}px;font-weight:600;color:{timec};margin-left:6px;">{time}</span>' if time else ''
        return (f'<div style="height:{h}px;margin-top:{gap}px;border-radius:7px;background:{bg};display:flex;align-items:center;gap:6px;padding:0 6px;box-sizing:border-box;{op}">'
                f'{c}<span style="flex:1;min-width:0;font-size:{fs}px;font-weight:700;color:{tc};white-space:nowrap;overflow:hidden;text-overflow:ellipsis;{st}">{title}</span>{t}</div>')
    return task


def with_task(theme, fn):
    old = wb.task
    wb.task = week_task(theme)
    try:
        return fn()
    finally:
        wb.task = old


def ring_mark(theme, pri, done, size=16):
    r = RING[theme][pri]
    if done:
        return (f'<span style="width:{size}px;height:{size}px;border-radius:50%;background:{r};display:flex;align-items:center;justify-content:center;flex-shrink:0;">'
                f'{ico(P_CHECK, THEME[theme]["SURF"], size - 5, 3)}</span>')
    w = 2.5 if pri == 'high' else 2
    return f'<span style="width:{size}px;height:{size}px;border-radius:50%;border:{w}px solid {r};background:{THEME[theme]["BG"]};box-sizing:border-box;flex-shrink:0;"></span>'


def list_row(theme, title, time='', pri='none', done=False, over=False, rep=False, bell=False, lsq='#3D8BFD'):
    C = THEME[theme]
    icons = (ico(P_REP, C['NAVI'], 18, 1.8) if rep else '') + (ico(P_BELL, C['NAVI'], 18, 1.8) if bell else '')
    icons += f'<span style="width:18px;height:18px;border-radius:5px;background:{lsq};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(P_INBOX, "#fff", 12, 2)}</span>'
    st = 'text-decoration:line-through;' if done else ''
    op = 'opacity:.7;' if done else ''
    return (f'<div style="display:flex;align-items:center;height:40px;margin-top:6px;">'
            f'<span style="width:44px;text-align:right;font-size:12.5px;font-weight:600;color:{C["OVD"] if over else C["T2"]};padding-right:7px;box-sizing:border-box;">{time}</span>'
            f'<span style="width:22px;display:flex;justify-content:center;position:relative;z-index:1;">{ring_mark(theme, pri, done)}</span>'
            f'<span style="flex:1;height:40px;margin:0 8px 0 7px;border-radius:12px;background:{C["SURF"]};display:flex;align-items:center;gap:9px;padding:0 11px;box-sizing:border-box;{op}">'
            f'<span style="flex:1;font-size:15px;font-weight:600;color:{C["INK"]};{st}white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{title}</span>{icons}</span></div>')


def day(theme, label, chip='', red=False, first=False):
    C = THEME[theme]
    c = C['OVD'] if red else C['INK']
    ch = f'<span style="font-size:12px;font-weight:700;color:{c};background:{C["SURF"]};border-radius:8px;padding:3px 8px;margin-left:8px;">{chip}</span>' if chip else ''
    return f'<div style="display:flex;align-items:center;padding:{0 if first else 14}px 0 0 12px;font-size:15px;font-weight:700;color:{c};">{label}{ch}</div>'


def screen_all(theme):
    C = THEME[theme]
    rows = (day(theme, 'Вчера', 'Просрочено', red=True, first=True) + list_row(theme, 'Отправить отчёт', '18:00', 'high', over=True, lsq='#8E4EC6')
            + day(theme, 'Сегодня', '5 задач') + list_row(theme, 'Дейлик', '11:00', 'high', rep=True) + list_row(theme, 'Финансы', '14:00', 'mid', bell=True, lsq='#46A758')
            + list_row(theme, 'Регулярка', '17:00', 'low', rep=True) + list_row(theme, 'Купить продукты', '', 'none', lsq='#F76B15')
            + list_row(theme, 'Зарядка', '08:00', 'mid', done=True)
            + day(theme, 'Завтра') + list_row(theme, 'Spotify', '10:00', 'low') + list_row(theme, 'Созвон с командой', '12:00', 'none', bell=True))
    line = f'<span style="position:absolute;left:58px;top:20px;bottom:20px;width:1.5px;background:{C["LINE"]};"></span>'
    return phone(C['BG'], status(C['INK']) + '<div style="height:8px;"></div>' + f'<div style="position:relative;">{line}{rows}</div>' + wb.nav_app(C, 'all'))


def screen_week(theme):
    return with_task(theme, lambda: wb.screen_app(THEME[theme]))


def screen_widget(theme):
    return with_task(theme, lambda: wb2.screen(THEME[theme], True))


def detail():
    C = LIGHT
    box = lambda t, inner: (f'<div style="margin-bottom:14px;"><div style="font-size:12px;font-weight:700;color:{C["T2"]};margin-bottom:4px;">{t}</div>'
                            f'<div style="width:412px;background:{C["BG"]};border:1px solid {C["LINE"]};border-radius:14px;padding:10px;box-sizing:border-box;">{inner}</div></div>')
    names = [('high', 'Высокий'), ('mid', 'Средний'), ('low', 'Низкий'), ('none', 'Обычный')]

    def rings(theme):
        CC = THEME[theme]
        cells = ''.join(f'<span style="display:flex;flex-direction:column;align-items:center;gap:6px;font-size:11px;font-weight:600;color:{CC["T2"]};">'
                        f'<span style="display:flex;gap:6px;">{ring_mark(theme, p, False, 20)}{ring_mark(theme, p, True, 20)}</span>{n}</span>' for p, n in names)
        return f'<div style="display:flex;justify-content:space-around;background:{CC["BG"]};border-radius:10px;padding:10px 0;">{cells}</div>'

    def prio_buttons():
        b = lambda p, sel: (f'<span style="width:50px;height:50px;border-radius:12px;background:{RING["light"][p] + "29" if sel else C["SURF"]};border:1px solid {"transparent" if sel else C["LINE"]};'
                            f'display:flex;align-items:center;justify-content:center;">{ico(P_FLAG, RING["light"][p], 25, 2)}</span>')
        return f'<div style="display:flex;gap:10px;">{b("high", True)}{b("mid", False)}{b("low", False)}{b("none", False)}</div>'

    wk = with_task('light', lambda: ''.join(wb.task(C, n + ' приоритет', '14:30', p, False, h=30, fs=13) for p, n in names) + wb.task(C, 'Выполнена', '09:00', 'none', True, h=30)
                   + wb.task(C, 'Высокий, просрочено', '10:00', 'high', False, past=True, h=30))
    wkd = with_task('dark', lambda: ''.join(wb.task(DARK, n + ' приоритет', '14:30', p, False, h=30, fs=13) for p, n in names))
    return (box('Кружки в «Все» и списках (вариант А): светлая / тёмная тема, пустой и выполненный', rings('light') + '<div style="height:8px"></div>' + rings('dark'))
            + box('Строки «Недели» (вариант 2): светлая тема, выполненная, просрочка на тёмной строке', f'<div style="width:300px;">{wk}</div>')
            + box('Тёмная тема: наоборот — высокий светлый', f'<div style="width:300px;background:{DARK["BG"]};padding:6px;border-radius:10px;">{wkd}</div>')
            + box('Кнопка приоритета в окне задачи: выбран высокий', prio_buttons())
            + f'<div style="font-size:13px;color:{C["T2"]};line-height:19px;width:412px;">Красного, жёлтого и синего у приоритета больше нет. Квадратики списков справа на плашке '
              f'остаются цветными — это цвет списка. Просроченное время по-прежнему красное.</div>')


SCREENS = [
    ('1. «Все» (вариант А)', 'Плашки белые, приоритет — кружком: высокий чёрный и толще, средний тёмно-серый, низкий светлый, обычный бледный.', lambda: screen_all('light')),
    ('2. «Неделя» (вариант 2)', 'Фон строки по уровню: высокий — почти чёрный с белым текстом, средний — серый, низкий — светлый, обычный — белый.', lambda: screen_week('light')),
    ('3. Виджет «Неделя»', 'Те же строки, что в приложении; размеры — как сейчас (поправки 0.46.0).', lambda: screen_widget('light')),
    ('4. Тёмная тема', '«Все» в тёмной теме: высокий — белый кружок.', lambda: screen_all('dark')),
    ('5. Тёмная «Неделя»', 'Наоборот: высокий — светлая строка с тёмным текстом.', lambda: screen_week('dark')),
    ('6. Детали', 'Кружки, строки «Недели», кнопка приоритета.', detail),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:#16171D;margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:#62656E;line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    cell_ = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    rows = f'<div style="display:flex;gap:30px;">{"".join(cell_(*s) for s in SCREENS)}</div>'
    W, H = 6 * 428 + 5 * 30 + 80, 1180
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EFEFEF;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:#16171D;letter-spacing:-.5px;">Приоритеты графитом — итерация 46</div>'
            f'<div style="font-size:15px;color:#62656E;margin:6px 0 22px;line-height:21px;">Решения PM: без цвета («очень цветасто»). Списки — вариант А (кружок по уровню), '
            f'«Неделя» — вариант 2 «Контрастный графит» (фон строки по уровню). Тёмная тема — наоборот.</div>{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Приоритеты графитом</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantG1.dc.html'), 'w').write(html)
    cj = os.path.join(OUT, 'canvas.json')
    if os.path.exists(cj):
        c = json.load(open(cj))
        fresh = 'VariantG1.dc.html' not in c['boards']
        old = {k: v for k, v in c['boards'].items() if k != 'VariantG1.dc.html'}
        if fresh:
            for v in old.values():
                v['y'] += H + 120
        c['boards'] = {'VariantG1.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Приоритеты графитом (итерация 46)'}, **old}
        c['order'] = list(c['boards'])
        c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Приоритеты графитом (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
        json.dump(c, open(cj, 'w'), ensure_ascii=False, indent=2)
