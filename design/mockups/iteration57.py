"""Макеты итерации 57: 10 вариантов нижней строки виджета (T-128, исследование, новый холст).

Просьба PM (10.11.2026): нынешняя нижняя строка виджета не нравится — подготовить 10 разных вариантов.
В каждом варианте есть всё, что умеет строка сейчас: вкладки «Все» / «Входящие» / «Неделя», открыть приложение,
сегодняшняя дата (нажатие — назад к сегодня) и «+». Всё рисуется обычными элементами Android-виджета (RemoteViews):
без размытия, теней поверх обоев и жестов-свайпов.

Запуск: python3 design/mockups/iteration57.py <папка project>   → canvas.json, Main.dc.html
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


def cell(title, sub, fn):
    return (f'<div style="width:{W}px"><div style="font-size:17px;font-weight:800;margin-bottom:4px">{title}</div>'
            f'<div style="font-size:13px;color:var(--text-secondary);line-height:18px;min-height:56px;margin-bottom:10px">{sub}</div>{fn()}</div>')


def build(out):
    os.makedirs(out, exist_ok=True)
    cols, gap = 4, 40
    Wd = cols * W + (cols - 1) * gap + 80
    rows = (len(VARIANTS) + cols - 1) // cols
    Hh = 36 + 40 + 70 + rows * (80 + 270 + 48) + 20
    body = ('<div style="font-size:28px;font-weight:800;letter-spacing:-.4px">Нижняя строка виджета — 10 вариантов (итерация 57)</div>'
            '<div style="font-size:15px;color:var(--text-secondary);margin:6px 0 22px;line-height:21px">В каждом варианте есть всё, что умеет строка сейчас: '
            'вкладки «Все» / «Входящие» / «Неделя», открыть приложение, сегодняшняя дата (нажатие — назад к сегодня) и «+». Всё делается обычными '
            'элементами виджета Android. Первый — как сейчас, для сравнения. Можно смешивать: например, «сегменты» из 1 и «крупную дату» из 5.</div>'
            f'<div style="display:flex;flex-wrap:wrap;gap:48px {gap}px;align-items:flex-start">' + ''.join(cell(*v) for v in VARIANTS) + '</div>')
    Path(out, 'Main.dc.html').write_text(I.page('10 вариантов', Wd, Hh, body), encoding='utf-8')
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Итерация 57 — Нижняя строка виджета', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': {'Main.dc.html': {'x': 0, 'y': 0, 'w': Wd, 'h': Hh, 'title': '10 вариантов'}}, 'order': ['Main.dc.html'],
              'notes': {}, 'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', Wd, Hh)


if __name__ == '__main__':
    build(sys.argv[1])
