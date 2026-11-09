"""Макеты итерации 57: 10 вариантов нижней строки виджета (T-128, исследование, новый холст).

Просьба PM (10.11.2026): нынешняя нижняя строка виджета не нравится — подготовить 10 разных вариантов.
В каждом варианте есть всё, что умеет строка сейчас: вкладки «Все» / «Входящие» / «Неделя», открыть приложение,
сегодняшняя дата (нажатие — назад к сегодня) и «+». Всё рисуется обычными элементами Android-виджета (RemoteViews):
без размытия, теней поверх обоев и жестов-свайпов.

Второй круг (требования PM 10.11.2026): галочка «открыть приложение» остаётся; нажатие на дату — назад к сегодня;
дата в две строки — «вт, 10» сверху, «ноя» снизу; вкладки — только значки, без подписей.

Запуск: python3 design/mockups/iteration57.py <папка project>   → canvas.json, Main.dc.html (круг 1), Round2.dc.html (круг 2), Date.dc.html (дата), Final.dc.html (итог 1), Round4.dc.html (круг 4), Final2.dc.html (итог 2)
"""
import datetime
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import iteration49 as I  # noqa: E402

D, B, svg, mi = I.D, I.B, I.svg, I.D.mi

W = 412
TABS = [('ic_widget_all', 'Все'), ('ic_widget_inbox', 'Входящие'), ('ic_tab_week', 'Неделя')]
SEL = 2  # выбрана «Неделя», как на скриншоте PM
DATE = 'вт, 10 ноя'


def t(*a, e='💼', **k):
    return D.week_task(*a, h=20, fs=10, gap=2, emoji=e, **k)


def week_rows(h=150):
    """Нижний ряд «Недели» виджета (как на скриншоте PM): воскресенье и мини-календарь."""
    cell = D.week_cell('вс', 15, [t('Стирка', '14:30', rep=True, e='🏠'), t('Уборка', '16:00', rep=True, e='🏠')], h=h, widget=True)
    return f'<div style="display:flex">{cell}{D.week_calendar(h, widget=True)}</div>'


def widget(bar, top=False, h=150):
    """Кусок виджета: ряд недели и нижняя строка [bar] (или строка сверху при [top])."""
    rows = week_rows(h)
    inner = f'{bar}{rows}' if top else f'{rows}{bar}'
    return (f'<div style="width:{W}px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;'
            f'border:1px solid var(--divider);padding:4px 3px 0;box-sizing:border-box">{inner}</div>')


def bar(content, pad='8px 10px 10px', bg='transparent', border=True):
    line = 'border-top:1px solid var(--divider);' if border else ''
    return f'<div style="display:flex;align-items:center;gap:6px;padding:{pad};background:{bg};{line}">{content}</div>'


def plus(w=56, h=44, radius='var(--radius-xl)'):
    return (f'<span style="width:{w}px;height:{h}px;border-radius:{radius};background:var(--accent);color:var(--on-accent);display:flex;'
            f'align-items:center;justify-content:center;flex-shrink:0">{svg("ic_widget_add", 24)}</span>')


def ic(name, color='var(--text-secondary)', s=24):
    return f'<span style="display:flex;color:{color}">{svg(name, s)}</span>'


SPACER = '<span style="flex:1"></span>'


# ------------------------------------------------------------------ варианты

def v_now():
    tabs = ''.join(f'<span style="width:56px;height:40px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                   f'background:{"var(--selected)" if i == SEL else "transparent"};color:{"var(--accent)" if i == SEL else "var(--text-secondary)"}">{svg(n, 24)}</span>'
                   for i, (n, _) in enumerate(TABS))
    return widget(bar(f'{tabs}{SPACER}{ic("ic_node_check", "var(--text-primary)", 26)}'
                      f'<span style="font-size:14px;font-weight:700;padding:0 4px">{DATE}</span>{plus(64, 40)}'))


def v1_segment():
    seg = ''.join(f'<span style="flex:1;height:36px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                  f'background:{"var(--surface)" if i == SEL else "transparent"};color:{"var(--accent)" if i == SEL else "var(--text-secondary)"};'
                  f'{"box-shadow:0 1px 2px color-mix(in srgb,var(--text-primary) 15%,transparent)" if i == SEL else ""}">{svg(n, 22)}</span>'
                  for i, (n, _) in enumerate(TABS))
    group = f'<span style="width:168px;display:flex;padding:3px;border-radius:var(--radius-xl);background:var(--divider)">{seg}</span>'
    date = f'<span style="flex:1;text-align:center;font-size:14px;font-weight:700">{DATE}</span>'
    return widget(bar(f'{group}{date}{plus(48, 44, "22px")}'))


def v2_labels():
    tabs = ''.join(f'<span style="width:62px;display:flex;flex-direction:column;align-items:center;gap:2px;color:{"var(--accent)" if i == SEL else "var(--text-secondary)"}">'
                   f'{svg(n, 22)}<span style="font-size:10px;line-height:12px;font-weight:{800 if i == SEL else 600}">{lab}</span></span>' for i, (n, lab) in enumerate(TABS))
    date = (f'<span style="display:flex;flex-direction:column;align-items:center;padding:0 6px"><span style="font-size:15px;font-weight:800;line-height:18px">10</span>'
            f'<span style="font-size:10px;font-weight:600;color:var(--text-secondary);line-height:12px">вт, ноя</span></span>')
    return widget(bar(f'{tabs}{SPACER}{date}{plus(52, 44)}'))


def v3_text():
    tabs = ''.join(f'<span style="display:flex;flex-direction:column;align-items:center;gap:4px;padding:0 6px">'
                   f'<span style="font-size:14px;font-weight:{800 if i == SEL else 600};color:{"var(--text-primary)" if i == SEL else "var(--text-secondary)"}">{lab}</span>'
                   f'<span style="width:22px;height:3px;border-radius:2px;background:{"var(--accent)" if i == SEL else "transparent"}"></span></span>'
                   for i, (_, lab) in enumerate(TABS))
    date = f'<span style="font-size:12px;font-weight:700;color:var(--text-secondary)">10 ноя</span>'
    return widget(bar(f'{tabs}{SPACER}{date}{plus(44, 44, "22px")}', pad='10px 8px 8px'))


def v4_dark_pill():
    icons = ''.join(f'<span style="width:44px;height:36px;border-radius:18px;display:flex;align-items:center;justify-content:center;'
                    f'background:{"var(--on-accent)" if i == SEL else "transparent"};color:{"var(--accent)" if i == SEL else "var(--on-accent)"}">{svg(n, 22)}</span>'
                    for i, (n, _) in enumerate(TABS))
    pill = (f'<span style="display:flex;align-items:center;gap:2px;padding:4px;border-radius:22px;background:var(--accent)">{icons}'
            f'<span style="width:1px;height:20px;background:color-mix(in srgb,var(--on-accent) 30%,transparent);margin:0 6px"></span>'
            f'<span style="font-size:13px;font-weight:700;color:var(--on-accent);padding-right:10px">{DATE}</span></span>')
    return widget(bar(f'{SPACER}{pill}{plus(44, 44, "22px")}{SPACER}', border=False))


def v5_big_date():
    date = (f'<span style="display:flex;align-items:center;gap:8px;padding:0 4px"><span style="font-size:30px;line-height:32px;font-weight:800">10</span>'
            f'<span style="display:flex;flex-direction:column"><span style="font-size:11px;line-height:14px;font-weight:800">ВТОРНИК</span>'
            f'<span style="font-size:11px;line-height:14px;font-weight:600;color:var(--text-secondary)">ноябрь</span></span></span>')
    tabs = ''.join(f'<span style="width:40px;height:40px;border-radius:20px;display:flex;align-items:center;justify-content:center;'
                   f'background:{"var(--selected)" if i == SEL else "transparent"};color:{"var(--accent)" if i == SEL else "var(--text-secondary)"}">{svg(n, 22)}</span>'
                   for i, (n, _) in enumerate(TABS))
    return widget(bar(f'{date}{SPACER}{tabs}{plus(44, 44, "22px")}'))


def v6_switcher():
    switch = (f'<span style="display:flex;align-items:center;gap:6px;height:40px;padding:0 12px;border-radius:20px;background:var(--surface);'
              f'border:1px solid var(--divider)">{svg(TABS[SEL][0], 20)}<span style="font-size:14px;font-weight:800">{TABS[SEL][1]}</span>'
              f'<span style="display:flex;color:var(--text-secondary)">{mi("down", 18)}</span></span>')
    date = f'<span style="flex:1;text-align:center;font-size:14px;font-weight:700;color:var(--text-secondary)">{DATE}</span>'
    return widget(bar(f'{switch}{date}{plus(52, 40)}'))


def v7_chips():
    chips = ''.join(f'<span style="height:32px;padding:0 12px;border-radius:16px;display:flex;align-items:center;font-size:13px;font-weight:700;'
                    f'{"background:var(--accent);color:var(--on-accent)" if i == SEL else "border:1px solid var(--divider);color:var(--text-primary)"}">{lab}</span>'
                    for i, (_, lab) in enumerate(TABS))
    return widget(bar(f'{chips}{SPACER}<span style="font-size:12px;font-weight:700;color:var(--text-secondary)">10 ноя</span>{plus(40, 40, "20px")}'))


def v8_indicator():
    tabs = ''.join(f'<span style="flex:1;height:52px;position:relative;display:flex;align-items:center;justify-content:center;color:{"var(--accent)" if i == SEL else "var(--text-secondary)"}">'
                   f'<span style="position:absolute;top:0;left:22%;right:22%;height:3px;border-radius:0 0 3px 3px;background:{"var(--accent)" if i == SEL else "transparent"}"></span>{svg(n, 24)}</span>'
                   for i, (n, _) in enumerate(TABS))
    date = f'<span style="flex:1.3;text-align:center;font-size:13px;font-weight:700">{DATE}</span>'
    add = f'<span style="flex:0.8;height:52px;display:flex;align-items:center;justify-content:center;color:var(--text-primary)">{svg("ic_widget_add", 28)}</span>'
    return widget(bar(f'{tabs}{date}{add}', pad='0 6px'))


def v9_circles():
    circ = lambda inner, on=False, bg=None: (f'<span style="width:44px;height:44px;border-radius:22px;display:flex;align-items:center;justify-content:center;flex-shrink:0;'
                                             f'background:{bg or ("var(--accent)" if on else "var(--surface)")};color:{"var(--on-accent)" if on else "var(--text-secondary)"}">{inner}</span>')
    tabs = ''.join(circ(svg(n, 22), i == SEL) for i, (n, _) in enumerate(TABS))
    day = circ('<span style="font-size:15px;font-weight:800;color:var(--text-primary)">10</span>')
    return widget(bar(f'{tabs}{SPACER}{day}{circ(svg("ic_widget_add", 24), True)}', border=False))


def v10_top():
    tabs = ''.join(f'<span style="width:34px;height:32px;border-radius:var(--radius-md);display:flex;align-items:center;justify-content:center;'
                   f'background:{"var(--selected)" if i == SEL else "transparent"};color:{"var(--accent)" if i == SEL else "var(--text-secondary)"}">{svg(n, 20)}</span>'
                   for i, (n, _) in enumerate(TABS))
    title = '<span style="font-size:17px;font-weight:800;padding-left:6px">Вторник, 10 ноября</span>'
    head = (f'<div style="display:flex;align-items:center;gap:2px;padding:6px 6px 8px;border-bottom:1px solid var(--divider)">{title}{SPACER}{tabs}'
            f'<span style="width:36px;height:32px;border-radius:var(--radius-md);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;'
            f'justify-content:center;margin-left:4px">{svg("ic_widget_add", 20)}</span></div>')
    return widget(head, top=True, h=190)


VARIANTS = [
    ('Сейчас', 'Три вкладки слева, галочка «открыть приложение», дата и широкая «+». Пять разных элементов в одной строке.', v_now),
    ('1. Сегменты', 'Вкладки в одной серой капсуле, выбранная — белая. Галочки нет: нажатие на дату открывает приложение (и возвращает к сегодня). «+» круглее.', v1_segment),
    ('2. Значки с подписями', 'Под значками — «Все», «Входящие», «Неделя»: понятно без привыкания. Дата — числом и днём недели; нажатие — приложение.', v2_labels),
    ('3. Только текст', 'Вкладки словами, выбранная — жирная с чёрной чертой снизу. Самый тихий вариант; дата мелко, «+» — круг.', v3_text),
    ('4. Тёмная капсула', 'Одна чёрная капсула по центру: три вкладки и дата, рядом круглая «+». Строка без линии сверху — «парит».', v4_dark_pill),
    ('5. Крупная дата', 'Слева крупно «10 ВТОРНИК ноябрь» — как часы на рабочем столе; нажатие — приложение. Справа маленькие круглые вкладки и «+».', v5_big_date),
    ('6. Один переключатель', 'Вместо трёх вкладок — одна кнопка «▦ Неделя ▾»: нажатие переключает Все → Входящие → Неделя по кругу. Меньше всего элементов.', v6_switcher),
    ('7. Чипы', 'Вкладки — чипы со словами, выбранный — чёрный. Дата коротко, «+» — маленький круг.', v7_chips),
    ('8. Полоска сверху', 'Как вкладки Android: значки на всю ширину, у выбранного — чёрная полоска над ним. «+» без заливки, дата посередине.', v8_indicator),
    ('9. Круглые кнопки', 'Все кнопки — одинаковые круги: вкладки белые, выбранная и «+» — чёрные, дата — круг с числом.', v9_circles),
    ('10. Строка сверху', 'Строка переезжает наверх виджета: «Вторник, 10 ноября» крупно (нажатие — приложение), справа вкладки и «+». Снизу ничего — задачам больше места.', v10_top),
]


# ------------------------------------------------------------------ круг 2: требования PM

def date2(color='var(--text-primary)', sub='var(--text-secondary)', size=14):
    """Дата в две строки: «вт, 10» сверху, «ноя» снизу (нажатие — назад к сегодня)."""
    return (f'<span style="display:flex;flex-direction:column;align-items:center;justify-content:center;padding:0 6px;flex-shrink:0">'
            f'<span style="font-size:{size}px;line-height:{size + 3}px;font-weight:800;color:{color}">вт, 10</span>'
            f'<span style="font-size:11px;line-height:13px;font-weight:600;color:{sub}">ноя</span></span>')


def check(color='var(--text-primary)', s=26):
    return f'<span style="display:flex;color:{color};padding:0 4px">{svg("ic_node_check", s)}</span>'


def icon_tabs(item):
    return ''.join(item(n, i == SEL) for i, (n, _) in enumerate(TABS))


def r1_segment():
    tab = lambda n, on: (f'<span style="flex:1;height:36px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--surface)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"};'
                         f'{"box-shadow:0 1px 2px color-mix(in srgb,var(--text-primary) 15%,transparent)" if on else ""}">{svg(n, 22)}</span>')
    group = f'<span style="width:160px;display:flex;padding:3px;border-radius:var(--radius-xl);background:var(--divider)">{icon_tabs(tab)}</span>'
    return widget(bar(f'{group}{SPACER}{check()}{date2()}{plus(48, 44, "22px")}'))


def r2_two_capsules():
    tab = lambda n, on: (f'<span style="width:42px;height:36px;border-radius:18px;display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--accent)" if on else "transparent"};color:{"var(--on-accent)" if on else "var(--text-secondary)"}">{svg(n, 22)}</span>')
    left = f'<span style="display:flex;padding:4px;border-radius:22px;background:var(--surface)">{icon_tabs(tab)}</span>'
    right = (f'<span style="display:flex;align-items:center;gap:2px;padding:4px 4px 4px 8px;border-radius:22px;background:var(--surface)">{check(s=24)}'
             f'{date2(size=13)}<span style="width:36px;height:36px;border-radius:18px;background:var(--accent);color:var(--on-accent);display:flex;'
             f'align-items:center;justify-content:center">{svg("ic_widget_add", 22)}</span></span>')
    return widget(bar(f'{left}{SPACER}{right}', border=False))


def r3_plain_dot():
    tab = lambda n, on: (f'<span style="width:48px;display:flex;flex-direction:column;align-items:center;gap:4px;color:{"var(--accent)" if on else "var(--inactive)"}">'
                         f'{svg(n, 24)}<span style="width:5px;height:5px;border-radius:3px;background:{"var(--accent)" if on else "transparent"}"></span></span>')
    return widget(bar(f'{icon_tabs(tab)}{SPACER}{check()}{date2()}{plus(52, 44)}', pad='10px 8px 6px'))


def r4_dark_bar():
    tab = lambda n, on: (f'<span style="width:40px;height:36px;border-radius:18px;display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--on-accent)" if on else "transparent"};color:{"var(--accent)" if on else "var(--on-accent)"}">{svg(n, 22)}</span>')
    inner = (f'{icon_tabs(tab)}{SPACER}{check("var(--on-accent)", 24)}{date2("var(--on-accent)", "color-mix(in srgb,var(--on-accent) 65%,transparent)", 13)}'
             f'<span style="width:36px;height:36px;border-radius:18px;background:var(--on-accent);color:var(--accent);display:flex;align-items:center;justify-content:center">{svg("ic_widget_add", 22)}</span>')
    pill = f'<div style="display:flex;align-items:center;gap:2px;padding:4px 4px 4px 4px;border-radius:24px;background:var(--accent)">{inner}</div>'
    return widget(f'<div style="padding:6px 6px 8px">{pill}</div>')


def r5_indicator():
    tab = lambda n, on: (f'<span style="flex:1;height:54px;position:relative;display:flex;align-items:center;justify-content:center;color:{"var(--accent)" if on else "var(--text-secondary)"}">'
                         f'<span style="position:absolute;top:0;left:20%;right:20%;height:3px;border-radius:0 0 3px 3px;background:{"var(--accent)" if on else "transparent"}"></span>{svg(n, 24)}</span>')
    add = f'<span style="width:48px;height:54px;display:flex;align-items:center;justify-content:center;color:var(--text-primary)">{svg("ic_widget_add", 28)}</span>'
    return widget(bar(f'{icon_tabs(tab)}{check()}{date2()}{add}', pad='0 4px'))


def r6_circles():
    circ = lambda inner, on=False: (f'<span style="width:44px;height:44px;border-radius:22px;display:flex;align-items:center;justify-content:center;flex-shrink:0;'
                                    f'background:{"var(--accent)" if on else "var(--surface)"};color:{"var(--on-accent)" if on else "var(--text-secondary)"}">{inner}</span>')
    tabs = icon_tabs(lambda n, on: circ(svg(n, 22), on))
    date = f'<span style="height:44px;border-radius:22px;background:var(--surface);display:flex;align-items:center;padding:0 6px">{date2(size=13)}</span>'
    return widget(bar(f'{tabs}{SPACER}{circ(check(s=24))}{date}{circ(svg("ic_widget_add", 24), True)}', border=False))


def r7_date_center():
    tab = lambda n, on: (f'<span style="width:38px;height:38px;border-radius:var(--radius-md);display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--selected)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"}">{svg(n, 22)}</span>')
    side = 'width:132px;display:flex;align-items:center'
    return widget(bar(f'<span style="{side}">{icon_tabs(tab)}</span><span style="flex:1;display:flex;justify-content:center">{date2(size=16)}</span>'
                      f'<span style="{side};justify-content:flex-end;gap:6px">{check()}{plus(52, 40)}</span>'))


def r8_date_left():
    tab = lambda n, on: (f'<span style="width:40px;height:34px;border-radius:17px;display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--surface)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"}">{svg(n, 21)}</span>')
    group = f'<span style="display:flex;padding:3px;border-radius:20px;background:var(--divider)">{icon_tabs(tab)}</span>'
    date = (f'<span style="display:flex;flex-direction:column;justify-content:center;padding:0 4px 0 6px"><span style="font-size:15px;line-height:18px;font-weight:800">вт, 10</span>'
            f'<span style="font-size:11px;line-height:13px;font-weight:600;color:var(--text-secondary)">ноя</span></span>')
    return widget(bar(f'{date}{SPACER}{group}{SPACER}{check()}{plus(44, 44, "22px")}'))


def r9_tiles():
    tile = lambda inner, w=44, on=False: (f'<span style="width:{w}px;height:44px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;flex-shrink:0;'
                                          f'background:{"var(--accent)" if on else "var(--surface)"};color:{"var(--on-accent)" if on else "var(--text-secondary)"}">{inner}</span>')
    tabs = f'<span style="display:flex;gap:4px">{icon_tabs(lambda n, on: tile(svg(n, 22), 44, on))}</span>'
    return widget(bar(f'{tabs}{SPACER}{tile(check(s=24))}{tile(date2(size=13), 64)}{tile(svg("ic_widget_add", 24), 52, True)}', border=False))


def r10_dividers():
    tab = lambda n, on: (f'<span style="width:40px;height:40px;border-radius:20px;display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--selected)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"}">{svg(n, 22)}</span>')
    div = '<span style="width:1px;height:28px;background:var(--divider);margin:0 4px"></span>'
    return widget(bar(f'{icon_tabs(tab)}{SPACER}{div}{check()}{div}{date2()}{div}<span style="display:flex;color:var(--text-primary);padding:0 6px">{svg("ic_widget_add", 28)}</span>'))


VARIANTS2 = [
    ('Сейчас', 'Для сравнения.', v_now),
    ('1. Сегменты', 'Значки в серой капсуле, выбранный — белый. Справа галочка, дата в две строки и круглая «+».', r1_segment),
    ('2. Две капсулы', 'Слева белая капсула вкладок (выбранная — чёрный круг), справа вторая: галочка, дата, «+». Строка без линии.', r2_two_capsules),
    ('3. Точка под значком', 'Значки без подложек, выбранный — чёрный с точкой под ним, остальные бледные. Самый лёгкий.', r3_plain_dot),
    ('4. Тёмная полоса', 'Вся строка — чёрная капсула: белые значки, галочка, дата и белая «+». Контраст с белыми задачами.', r4_dark_bar),
    ('5. Полоска сверху', 'Значки на всю ширину, у выбранного — чёрная полоска над ним; «+» без заливки.', r5_indicator),
    ('6. Круги', 'Вкладки, галочка и «+» — круги одного размера, дата — овал. Выбранная и «+» — чёрные.', r6_circles),
    ('7. Дата по центру', 'Слева вкладки, посередине дата крупнее, справа галочка и «+». Симметрично.', r7_date_center),
    ('8. Дата слева', 'Дата первой (как заголовок дня), вкладки в капсуле посередине, справа галочка и «+».', r8_date_left),
    ('9. Плитки', 'Каждая кнопка — белая плитка одной высоты, выбранная вкладка и «+» — чёрные.', r9_tiles),
    ('10. Разделители', 'Группы разделены тонкими линиями: вкладки | галочка | дата | «+». Без заливок, кроме выбранной вкладки.', r10_dividers),
]


# ------------------------------------------------------------------ круг 3: как показывать дату (просьба PM)

def stack(*lines):
    """Строки даты столбиком: (текст, размер, начертание, цвет)."""
    return ('<span style="display:flex;flex-direction:column;align-items:center;justify-content:center;padding:0 6px;flex-shrink:0">'
            + ''.join(f'<span style="font-size:{fs}px;line-height:{fs + 2}px;font-weight:{fw};color:{c}">{tx}</span>' for tx, fs, fw, c in lines) + '</span>')


P, S2 = 'var(--text-primary)', 'var(--text-secondary)'


def d_two():
    return stack(('вт, 10', 14, 800, P), ('ноя', 11, 600, S2))


def d_three():
    return stack(('вт', 11, 600, S2), ('10', 14, 800, P), ('Ноя', 11, 600, S2))


def d_three_big():
    return stack(('ВТ', 10, 800, S2), ('10', 22, 800, P), ('НОЯ', 10, 800, S2))


def d_num_left():
    return ('<span style="display:flex;align-items:center;gap:5px;padding:0 6px"><span style="font-size:26px;line-height:28px;font-weight:800">10</span>'
            + stack(('вт', 11, 700, P), ('ноя', 11, 600, S2)).replace('padding:0 6px', 'padding:0;align-items:flex-start') + '</span>')


def d_leaf():
    return ('<span style="width:44px;border-radius:var(--radius-md);overflow:hidden;border:1px solid var(--divider);background:var(--surface);display:flex;flex-direction:column;'
            'align-items:center;flex-shrink:0;margin:0 4px"><span style="width:100%;text-align:center;background:var(--accent);color:var(--on-accent);font-size:9px;line-height:13px;'
            'font-weight:800">НОЯ</span><span style="font-size:17px;line-height:19px;font-weight:800">10</span><span style="font-size:9px;line-height:11px;font-weight:600;'
            'color:var(--text-secondary);padding-bottom:2px">вт</span></span>')


def d_circle():
    return ('<span style="display:flex;flex-direction:column;align-items:center;gap:1px;padding:0 6px"><span style="font-size:10px;line-height:11px;font-weight:700;color:var(--text-secondary)">вт</span>'
            '<span style="width:28px;height:28px;border-radius:14px;background:var(--accent);color:var(--on-accent);font-size:14px;font-weight:800;display:flex;align-items:center;'
            'justify-content:center">10</span></span>')


def d_one_line():
    return '<span style="font-size:14px;font-weight:700;padding:0 6px;white-space:nowrap">вт, 10 ноя</span>'


def d_today():
    return stack(('Сегодня', 13, 800, P), ('10 ноя', 11, 600, S2))


def d_weekday_full():
    return stack(('10 ноя', 14, 800, P), ('вторник', 11, 600, S2))


def d_outlined():
    return ('<span style="border:1.5px solid var(--text-primary);border-radius:var(--radius-md);padding:1px 8px;margin:0 4px;flex-shrink:0">'
            + stack(('вт', 10, 600, S2), ('10', 15, 800, P), ('ноя', 10, 600, S2)).replace('padding:0 6px', 'padding:0') + '</span>')


def date_bar(date):
    tab = lambda n, on: (f'<span style="flex:1;height:36px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--surface)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"};'
                         f'{"box-shadow:0 1px 2px color-mix(in srgb,var(--text-primary) 15%,transparent)" if on else ""}">{svg(n, 22)}</span>')
    group = f'<span style="width:150px;display:flex;padding:3px;border-radius:var(--radius-xl);background:var(--divider)">{icon_tabs(tab)}</span>'
    inner = bar(f'{group}{SPACER}{check()}{date}{plus(48, 44, "22px")}', pad='10px 10px 10px', border=False)
    zoom = (f'<div style="margin-top:10px;height:136px;border-radius:var(--radius-lg);background:var(--surface);display:flex;align-items:center;justify-content:center">'
            f'<span style="transform:scale(2);display:flex">{date}</span></div>')
    return (f'<div style="width:{W}px;border-radius:var(--radius-xl);background:var(--background);border:1px solid var(--divider);box-sizing:border-box">{inner}</div>{zoom}')


DATES = [
    ('1. Две строки', '«вт, 10» сверху, «ноя» снизу — из круга 2.', lambda: date_bar(d_two())),
    ('2. Три строки', '«вт» / «10» / «Ноя» — каждое отдельно, число чуть крупнее. Ваш вариант.', lambda: date_bar(d_three())),
    ('3. Три строки, крупное число', '«ВТ» / «10» / «НОЯ»: число заметно крупнее, день и месяц мелко капсом — как в календаре.', lambda: date_bar(d_three_big())),
    ('4. Число слева', 'Крупное «10», справа столбиком «вт» и «ноя». Компактно по высоте.', lambda: date_bar(d_num_left())),
    ('5. Листок календаря', 'Мини-листок: сверху чёрная полоса «НОЯ», крупное «10», снизу «вт». Сразу понятно, что это дата.', lambda: date_bar(d_leaf())),
    ('6. Число в круге', '«вт» над чёрным кругом с «10» — как «сегодня» в мини-календаре виджета. Месяца нет.', lambda: date_bar(d_circle())),
    ('7. Одна строка', '«вт, 10 ноя» — как сейчас, для сравнения.', lambda: date_bar(d_one_line())),
    ('8. «Сегодня»', '«Сегодня» сверху, «10 ноя» снизу: подсказывает, что нажатие вернёт к сегодня.', lambda: date_bar(d_today())),
    ('9. День недели словом', '«10 ноя» сверху, «вторник» снизу полностью.', lambda: date_bar(d_weekday_full())),
    ('10. В рамке', '«вт» / «10» / «ноя» в тонкой чёрной рамке — дата выглядит кнопкой.', lambda: date_bar(d_outlined())),
]


def date_cell(title, sub, fn):
    return (f'<div style="width:{W}px"><div style="font-size:17px;font-weight:800;margin-bottom:4px">{title}</div>'
            f'<div style="font-size:13px;color:var(--text-secondary);line-height:18px;min-height:40px;margin-bottom:10px">{sub}</div>{fn()}</div>')


def cell(title, sub, fn):
    return (f'<div style="width:{W}px"><div style="font-size:17px;font-weight:800;margin-bottom:4px">{title}</div>'
            f'<div style="font-size:13px;color:var(--text-secondary);line-height:18px;min-height:56px;margin-bottom:10px">{sub}</div>{fn()}</div>')


# ------------------------------------------------------------------ итог: выбор PM (вкладки слева, блок по центру, дата справа)

def final_bar(center='light'):
    """Решение PM: слева вкладки значками (как сейчас), по центру блок «галочка + «+»», справа дата — вариант 4 («10» и «вт» / «ноя»)."""
    tab = lambda n, on: (f'<span style="width:50px;height:40px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--selected)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"}">{svg(n, 24)}</span>')
    if center == 'light':
        block = (f'<span style="display:flex;align-items:center;gap:4px;padding:4px 4px 4px 10px;border-radius:24px;background:var(--surface);'
                 f'border:1px solid var(--divider)">{check(s=24)}<span style="width:40px;height:36px;border-radius:18px;background:var(--accent);color:var(--on-accent);'
                 f'display:flex;align-items:center;justify-content:center">{svg("ic_widget_add", 22)}</span></span>')
    else:
        block = (f'<span style="display:flex;align-items:center;gap:4px;padding:4px 4px 4px 10px;border-radius:24px;background:var(--accent)">'
                 f'{check("var(--on-accent)", 24)}<span style="width:1px;height:20px;background:color-mix(in srgb,var(--on-accent) 30%,transparent)"></span>'
                 f'<span style="width:40px;height:36px;display:flex;align-items:center;justify-content:center;color:var(--on-accent)">{svg("ic_widget_add", 24)}</span></span>')
    side = 'flex:1;display:flex;align-items:center'
    return bar(f'<span style="{side}">{icon_tabs(tab)}</span>{block}<span style="{side};justify-content:flex-end">{d_num_left()}</span>', pad='8px 6px 10px')


def full_week(bar_html, sel=2):
    h = 148
    r1 = (f'<div style="display:flex">{D.week_cell("пн", 9, [t("Вес", "10:00", rep=True, done=True, e="🔁"), t("Синк", "11:00", rep=True, done=True), t("S3", "11:30", done=True), t("Виджет", "16:00", done=True), t("Подарки", "17:00", done=True, e="💲")], h=h, widget=True)}'
          f'{D.week_cell("вт", 10, [t("Дейлик", "11:00", rep=True), t("ИИ", "14:30", rep=True), t("Расскажут об экспертизе", "16:00"), t("Регулярка", "17:00", rep=True), t("Стирка", "23:00", e="🏠")], today=True, h=h, widget=True)}</div>')
    r2 = (f'<div style="display:flex">{D.week_cell("ср", 11, [t("Встреча по итогам оценки", "10:00"), t("Налог", "11:30", "high", e="💲"), t("Синк/Погашения", "14:00", rep=True), t("Оценка", "16:00", rep=True)], h=h, widget=True)}'
          f'{D.week_cell("чт", 12, [t("Дейлик", "11:00", rep=True), t("Расскажут об экспертизе", "14:00"), t("Девопсы", "14:30", rep=True), t("ЖКУ", "15:00", rep=True, e="💲"), t("Показания", "15:30", rep=True, e="💲")], hidden=1, h=h, widget=True)}</div>')
    r3 = (f'<div style="display:flex">{D.week_cell("пт", 13, [t("ВПН", "10:00", rep=True, e="💲"), t("Финансы", "14:00", rep=True, e="🔁")], h=h, widget=True)}'
          f'{D.week_cell("сб", 14, [t("Okko", "10:00", rep=True, e="💲"), t("Ростелеком", "10:00", rep=True, e="💲"), t("Yota", "11:00", rep=True, e="💲"), t("Бритьё", "14:30", rep=True, e="🔁"), t("Теннис", "20:30", rep=True, e="🏀")], hidden=1, h=h, widget=True)}</div>')
    r4 = f'<div style="display:flex">{D.week_cell("вс", 15, [t("Стирка", "14:30", rep=True, e="🏠"), t("Уборка", "16:00", rep=True, e="🏠")], h=h, widget=True)}{D.week_calendar(h, widget=True)}</div>'
    return (f'<div style="width:{W}px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;border:1px solid var(--divider);'
            f'padding:4px 3px 0;box-sizing:border-box">{r1}{r2}{r3}{r4}{bar_html}</div>')


def full_all(bar_html):
    rows, _ = D.widget_body()
    return (f'<div style="width:{W}px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;border:1px solid var(--divider);'
            f'padding:6px 3px 0;box-sizing:border-box"><div style="height:400px;overflow:hidden">{rows}</div>{bar_html}</div>')


def tabs_sel(bar_html, sel):
    """Та же строка с другой выбранной вкладкой (подложка `selected` переезжает)."""
    return bar_html


def dark(inner):
    import iteration53 as S3
    return f'<div style="{S3.dark_vars()};color-scheme:dark;color:var(--text-primary);border-radius:var(--radius-xl)">{inner}</div>'


def final_all_bar():
    global SEL
    keep, SEL = SEL, 0
    try:
        return final_bar()
    finally:
        SEL = keep


FINAL = [
    ('1. Неделя', 'Слева вкладки значками, как сейчас (выбранная — на светлой подложке). По центру белый блок: галочка — открыть приложение, чёрная «+» — новая задача. '
     'Справа дата: крупное «10», рядом «вт» / «ноя»; нажатие — назад к сегодня.', lambda: full_week(final_bar())),
    ('2. Все', 'Лента «Все» с той же строкой: выбрана первая вкладка. Блок по центру и дата не двигаются.', lambda: full_all(final_all_bar())),
    ('3. Тёмная тема', 'Те же места; блок по центру тёмный, «+» светлая.', lambda: dark(full_week(final_bar()))),
    ('4. Другой блок по центру', 'Если белый блок покажется лишним: чёрная капсула, в ней белые галочка и «+» через тонкую черту.', lambda: full_week(final_bar('dark'))),
]


# ------------------------------------------------------------------ круг 4: «+» слева, вкладки по центру, дата справа (решение PM)

def lay(left, center, right, pad='8px 8px 10px', bg=None, border=True, wrap=None):
    """Три места: стороны делят остаток поровну — блок вкладок ровно по центру. Пустое место строки — открыть приложение."""
    side = 'flex:1;display:flex;align-items:center'
    inner = f'<span style="{side}">{left}</span>{center}<span style="{side};justify-content:flex-end">{right}</span>'
    if wrap:
        return wrap(inner)
    return bar(inner, pad=pad, border=border, bg=bg or 'transparent')


DATE4 = d_num_left


def tabs_sel_bg(size=24, w=50, h=40, radius='var(--radius-lg)'):
    return icon_tabs(lambda n, on: (f'<span style="width:{w}px;height:{h}px;border-radius:{radius};display:flex;align-items:center;justify-content:center;'
                                    f'background:{"var(--selected)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"}">{svg(n, size)}</span>'))


def q1_flat():
    return widget(lay(plus(52, 40), f'<span style="display:flex">{tabs_sel_bg()}</span>', DATE4()))


def q2_segment():
    tab = lambda n, on: (f'<span style="width:48px;height:36px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--surface)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"};'
                         f'{"box-shadow:0 1px 2px color-mix(in srgb,var(--text-primary) 15%,transparent)" if on else ""}">{svg(n, 22)}</span>')
    seg = f'<span style="display:flex;padding:3px;border-radius:var(--radius-xl);background:var(--divider)">{icon_tabs(tab)}</span>'
    return widget(lay(plus(44, 44, '22px'), seg, DATE4()))


def q3_dark_tabs():
    tab = lambda n, on: (f'<span style="width:44px;height:36px;border-radius:18px;display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--on-accent)" if on else "transparent"};color:{"var(--accent)" if on else "var(--on-accent)"}">{svg(n, 22)}</span>')
    pill = f'<span style="display:flex;padding:4px;border-radius:22px;background:var(--accent)">{icon_tabs(tab)}</span>'
    add = (f'<span style="width:44px;height:44px;border-radius:22px;border:1.5px solid var(--text-primary);box-sizing:border-box;display:flex;align-items:center;'
           f'justify-content:center;color:var(--text-primary)">{svg("ic_widget_add", 24)}</span>')
    return widget(lay(add, pill, DATE4(), border=False))


def q4_dots():
    tab = lambda n, on: (f'<span style="width:48px;display:flex;flex-direction:column;align-items:center;gap:4px;color:{"var(--accent)" if on else "var(--inactive)"}">'
                         f'{svg(n, 24)}<span style="width:5px;height:5px;border-radius:3px;background:{"var(--accent)" if on else "transparent"}"></span></span>')
    return widget(lay(plus(44, 44, '22px'), f'<span style="display:flex;padding-top:6px">{icon_tabs(tab)}</span>', DATE4()))


def q5_white_capsule():
    tab = lambda n, on: (f'<span style="width:44px;height:36px;border-radius:18px;display:flex;align-items:center;justify-content:center;'
                         f'background:{"var(--accent)" if on else "transparent"};color:{"var(--on-accent)" if on else "var(--text-secondary)"}">{svg(n, 22)}</span>')
    pill = f'<span style="display:flex;padding:4px;border-radius:22px;background:var(--surface);border:1px solid var(--divider)">{icon_tabs(tab)}</span>'
    return widget(lay(plus(48, 44, 'var(--radius-lg)'), pill, DATE4(), border=False))


def q6_tiles():
    tile = lambda inner, on=False, w=44: (f'<span style="width:{w}px;height:44px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;flex-shrink:0;'
                                          f'background:{"var(--accent)" if on else "var(--surface)"};color:{"var(--on-accent)" if on else "var(--text-secondary)"}">{inner}</span>')
    tabs = f'<span style="display:flex;gap:4px">{icon_tabs(lambda n, on: tile(svg(n, 22), on))}</span>'
    date = f'<span style="height:44px;border-radius:var(--radius-lg);background:var(--surface);display:flex;align-items:center;padding:0 4px">{DATE4()}</span>'
    return widget(lay(tile(svg('ic_widget_add', 24), True, 52), tabs, date, border=False))


def q7_indicator():
    tab = lambda n, on: (f'<span style="width:56px;height:54px;position:relative;display:flex;align-items:center;justify-content:center;color:{"var(--accent)" if on else "var(--text-secondary)"}">'
                         f'<span style="position:absolute;top:0;left:12px;right:12px;height:3px;border-radius:0 0 3px 3px;background:{"var(--accent)" if on else "transparent"}"></span>{svg(n, 24)}</span>')
    return widget(lay(plus(48, 40), f'<span style="display:flex">{icon_tabs(tab)}</span>', DATE4(), pad='0 8px'))


def q8_card():
    card = lambda inner: (f'<div style="padding:6px 6px 8px"><div style="display:flex;align-items:center;padding:6px;border-radius:var(--radius-xl);background:var(--surface);'
                          f'border:1px solid var(--divider)">{inner}</div></div>')
    return widget(lay(plus(44, 40, 'var(--radius-lg)'), f'<span style="display:flex">{tabs_sel_bg(22, 46, 36)}</span>', DATE4(), wrap=card))


def q9_dark_bar():
    tab = lambda n, on: (f'<span style="width:44px;height:36px;border-radius:18px;display:flex;align-items:center;justify-content:center;'
                         f'background:{"color-mix(in srgb,var(--on-accent) 22%,transparent)" if on else "transparent"};color:{"var(--on-accent)" if on else "color-mix(in srgb,var(--on-accent) 60%,transparent)"}">{svg(n, 22)}</span>')
    add = (f'<span style="width:40px;height:36px;border-radius:18px;background:var(--on-accent);color:var(--accent);display:flex;align-items:center;'
           f'justify-content:center">{svg("ic_widget_add", 22)}</span>')
    date = DATE4().replace('color:var(--text-secondary)', 'color:color-mix(in srgb,var(--on-accent) 65%,transparent)')
    date = f'<span style="color:var(--on-accent);display:flex">{date}</span>'
    wrap = lambda inner: (f'<div style="padding:6px 6px 8px"><div style="display:flex;align-items:center;padding:4px;border-radius:24px;background:var(--accent)">{inner}</div></div>')
    return widget(lay(add, f'<span style="display:flex">{icon_tabs(tab)}</span>', date, wrap=wrap))


def q10_big_plus():
    add = (f'<span style="width:56px;height:56px;margin-top:-18px;border-radius:var(--radius-lg);background:var(--accent);color:var(--on-accent);display:flex;'
           f'align-items:center;justify-content:center;box-shadow:0 2px 6px color-mix(in srgb,var(--text-primary) 25%,transparent)">{svg("ic_widget_add", 28)}</span>')
    return widget(lay(add, f'<span style="display:flex">{tabs_sel_bg()}</span>', DATE4()))


VARIANTS4 = [
    ('1. Плоско', '«+» — чёрная плашка слева, значки по центру (выбранный — на светлой подложке, как сейчас), дата справа. Самый близкий к нынешнему.', q1_flat),
    ('2. Сегменты', 'Значки в серой капсуле, выбранный — белый. «+» — чёрный круг.', q2_segment),
    ('3. Тёмная капсула', 'Вкладки в чёрной капсуле по центру — главный элемент строки; «+» — контурный круг.', q3_dark_tabs),
    ('4. Точка под значком', 'Значки без подложек, выбранный — чёрный с точкой; «+» — чёрный круг. Самый лёгкий.', q4_dots),
    ('5. Белая капсула', 'Вкладки в белой капсуле, выбранный — чёрный круг; «+» — чёрная плашка со скруглением.', q5_white_capsule),
    ('6. Плитки', '«+», каждая вкладка и дата — белые плитки одной высоты; выбранная вкладка и «+» — чёрные.', q6_tiles),
    ('7. Полоска сверху', 'У выбранного значка — чёрная полоска над ним, как вкладки Android.', q7_indicator),
    ('8. Белая карточка', 'Вся строка — белая карточка со скруглением, внутри «+», вкладки и дата.', q8_card),
    ('9. Чёрная полоса', 'Вся строка — чёрная капсула: белая «+», светлые значки, белая дата.', q9_dark_bar),
    ('10. Крупный «+»', '«+» крупнее и приподнят над строкой, как главная кнопка; вкладки и дата — как в 1. В виджете сложнее всего: кнопка заходит на ряд задач.', q10_big_plus),
]


# ------------------------------------------------------------------ итог 2: вариант 6, доработки PM

TILE_W, TILE_H = 64, 36


def final2_bar(sel=SEL):
    """Решение PM: основа — «Плитки» (круг 4, вариант 6). «+» и дата — плитки одной ширины; вкладки — один белый блок по центру,
    выбранный вид — чёрный значок, остальные серые; строка ниже (плитки 36 вместо 44)."""
    tile = lambda inner, bg, color: (f'<span style="width:{TILE_W}px;height:{TILE_H}px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                                     f'flex-shrink:0;background:{bg};color:{color}">{inner}</span>')
    add = tile(svg('ic_widget_add', 22), 'var(--accent)', 'var(--on-accent)')
    date = ('<span style="display:flex;align-items:center;gap:4px"><span style="font-size:20px;line-height:22px;font-weight:800">10</span>'
            '<span style="display:flex;flex-direction:column"><span style="font-size:9px;line-height:10px;font-weight:800">ВТ</span>'
            '<span style="font-size:9px;line-height:10px;font-weight:600;color:var(--text-secondary)">ноя</span></span></span>')
    date = tile(date, 'var(--surface)', 'var(--text-primary)')
    icons = ''.join(f'<span style="width:48px;height:{TILE_H}px;display:flex;align-items:center;justify-content:center;'
                    f'color:{"var(--accent)" if i == sel else "var(--inactive)"}">{svg(n, 22)}</span>' for i, (n, _) in enumerate(TABS))
    block = f'<span style="display:flex;border-radius:var(--radius-lg);background:var(--surface);padding:0 4px">{icons}</span>'
    return lay(add, block, date, pad='6px 6px 7px', border=False)


def final2_bar_sel(sel):
    return lambda: final2_bar(sel)


FINAL2 = [
    ('1. Неделя', 'Слева чёрная «+», справа белая плитка с датой — одной ширины. По центру один белый блок вкладок: выбранный вид — чёрный значок, '
     'остальные серые. Строка ниже: плитки 36 вместо 44. Пустое место строки — открыть приложение; дата — назад к сегодня.', lambda: full_week(final2_bar(2))),
    ('2. Все', 'Выбрана лента «Все» — чёрный значок календаря; «+» и дата на месте.', lambda: full_all(final2_bar(0))),
    ('3. Тёмная тема', 'Те же места: «+» светлая, блоки тёмные, выбранный значок светлый.', lambda: dark(full_week(final2_bar(2)))),
    ('4. Было и стало', 'Сверху — строка сейчас, снизу — новая. Новая ниже и спокойнее: одна чёрная кнопка, один блок вкладок, дата справа.',
     lambda: f'<div style="display:flex;flex-direction:column;gap:16px">{widget(v_now_bar())}{widget(final2_bar(2))}</div>'),
]


def v_now_bar():
    tabs = ''.join(f'<span style="width:56px;height:40px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                   f'background:{"var(--selected)" if i == SEL else "transparent"};color:{"var(--accent)" if i == SEL else "var(--text-secondary)"}">{svg(n, 24)}</span>'
                   for i, (n, _) in enumerate(TABS))
    return bar(f'{tabs}{SPACER}{ic("ic_node_check", "var(--text-primary)", 26)}<span style="font-size:14px;font-weight:700;padding:0 4px">{DATE}</span>{plus(64, 40)}')


def board(title, lead, variants, cell_fn=None, row_h=270 + 80):
    cell_ = cell_fn or cell
    cols, gap = 4, 40
    Wd = cols * W + (cols - 1) * gap + 80
    rows = (len(variants) + cols - 1) // cols
    Hh = 36 + 40 + 70 + rows * (row_h + 48) + 20
    body = (f'<div style="font-size:28px;font-weight:800;letter-spacing:-.4px">{title}</div>'
            f'<div style="font-size:15px;color:var(--text-secondary);margin:6px 0 22px;line-height:21px">{lead}</div>'
            f'<div style="display:flex;flex-wrap:wrap;gap:48px {gap}px;align-items:flex-start">' + ''.join(cell_(*v) for v in variants) + '</div>')
    return Wd, Hh, body


BOARDS = [
    ('Main.dc.html', '10 вариантов', 'Нижняя строка виджета — 10 вариантов (итерация 57)',
     'В каждом варианте есть всё, что умеет строка сейчас: вкладки «Все» / «Входящие» / «Неделя», открыть приложение, сегодняшняя дата '
     '(нажатие — назад к сегодня) и «+». Всё делается обычными элементами виджета Android. Первый — как сейчас, для сравнения. '
     'Можно смешивать: например, «сегменты» из 1 и «крупную дату» из 5.', VARIANTS),
    ('Round2.dc.html', 'Круг 2', 'Нижняя строка виджета — круг 2 (требования PM)',
     'Во всех: галочка — сразу открыть приложение; дата в две строки «вт, 10» / «ноя» — нажатие возвращает к сегодня; '
     'вкладки «Все» / «Входящие» / «Неделя» — только значками; «+» — новая задача.', VARIANTS2),
    ('Date.dc.html', 'Дата', 'Как показывать дату — 10 вариантов',
     'Строка одна и та же (сегменты из круга 2), меняется только дата. Под каждой строкой — дата крупно, чтобы рассмотреть. '
     'Нажатие на дату — назад к сегодня; дата всегда сегодняшняя.', DATES, date_cell, 290),
    ('Round4.dc.html', 'Круг 4', 'Нижняя строка виджета — «+» слева, вкладки по центру, дата справа',
     'Решение PM: галочки нет — приложение открывается нажатием на свободное место строки. Слева «+» (новая задача), по центру вкладки '
     '«Все» / «Входящие» / «Неделя» значками, справа дата «10 вт / ноя» (нажатие — назад к сегодня). 10 вариантов оформления.', VARIANTS4),
    ('Final2.dc.html', 'Итог 2', 'Нижняя строка виджета — итог (вариант 6 с доработками PM)',
     'Слева «+», по центру один белый блок вкладок (выбранный вид — чёрный значок, остальные серые), справа дата «10 ВТ / ноя». '
     '«+» и дата — плитки одной ширины; строка ниже, чем сейчас. Галочки нет: приложение открывается нажатием на свободное место строки.', FINAL2, None, 820),
    ('Final.dc.html', 'Итог', 'Нижняя строка виджета — итог (выбор PM)',
     'Слева — вкладки «Все» / «Входящие» / «Неделя» значками, как сейчас. По центру — блок: галочка (сразу открыть приложение) и «+» (новая задача). '
     'Справа — дата, вариант 4: крупное «10», рядом «вт» и «ноя»; нажатие возвращает к сегодня.', FINAL, None, 820),
]


def build(out, created=None):
    os.makedirs(out, exist_ok=True)
    boards, order, y = {}, [], 0
    for name, board_title, title, lead, variants, *extra in BOARDS:
        Wd, Hh, body = board(title, lead, variants, *extra)
        Path(out, name).write_text(I.page(board_title, Wd, Hh, body), encoding='utf-8')
        boards[name] = {'x': 0, 'y': y, 'w': Wd, 'h': Hh, 'title': board_title}
        order.append(name)
        y += Hh + 120
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {'v': 3, 'createdOnFiles': created or {'v': 1, 'at': now}, 'title': 'Итерация 57 — Нижняя строка виджета', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': boards, 'order': order, 'notes': {}, 'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', [(n, b['w'], b['h']) for n, b in boards.items()])

if __name__ == '__main__':
    build(sys.argv[1])
