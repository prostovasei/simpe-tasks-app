"""Макеты итерации 55: новая нижняя панель, «Списки» как центр, «Неделя» и значки как в виджете (T-119…T-122, новый холст).

Решения PM (08.11.2026):
- Нижняя панель: слева раздел «Предстоящее / Неделя» (удержание значка — меню из двух пунктов, выбор запоминается),
  посередине «+», справа «Списки». Число неразобранных — на «Списках».
- «Входящие», «Поиск», «Настройки» — в «Списках»: поле поиска сверху, шестерёнка в шапке, «Входящие» первой строкой.
- «Неделя» в приложении — вид строк и подписей как в виджете (эмодзи слева от времени, дата капсом), размер под экран приложения.
- Значки панели и новых строк — контурные, как в нижней строке виджета.

Запуск: python3 design/mockups/iteration55.py <папка project>   → canvas.json, Main.dc.html, Lists.dc.html
"""
import datetime
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import iteration49 as I  # noqa: E402

D, B, svg, mi = I.D, I.B, I.svg, I.D.mi
H = I.H_PHONE

# Новые контурные значки (линия 2, круглые концы — как ic_widget_*): поиск и настройки.
SEARCH = ('<svg width="{s}" height="{s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" '
          'stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="6.5"/><path d="M20 20l-4.2-4.2"/></svg>')
GEAR = ('<svg width="{s}" height="{s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" '
        'stroke-linejoin="round" aria-hidden="true"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08'
        'a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 '
        '2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0'
        'l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38'
        'a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>')


def search_icon(s=24):
    return SEARCH.format(s=s)


def gear_icon(s=24):
    return GEAR.format(s=s)


ICON = {'upcoming': 'ic_widget_all', 'week': 'ic_tab_week', 'lists': 'ic_widget_list'}


def tab(icon, selected, badge=None, hint=False):
    """Вкладка как в нижней строке виджета: контурный значок, выбранная — на плашке `selected`. [hint] — точка «есть меню»."""
    bg = 'var(--selected)' if selected else 'transparent'
    color = 'var(--accent)' if selected else 'var(--text-secondary)'
    b = (f'<span style="position:absolute;top:-4px;right:6px;min-width:18px;height:18px;padding:0 5px;box-sizing:border-box;border-radius:9px;'
         f'background:var(--accent);color:var(--on-accent);font-size:11px;line-height:18px;font-weight:800;text-align:center">{badge}</span>') if badge else ''
    dot = ('<span style="position:absolute;bottom:3px;left:50%;margin-left:-2px;width:4px;height:4px;border-radius:2px;'
           f'background:{color};opacity:.6"></span>') if hint else ''
    return (f'<span style="flex:1;display:flex;justify-content:center"><span style="position:relative;width:72px;height:40px;border-radius:var(--radius-lg);'
            f'background:{bg};color:{color};display:flex;align-items:center;justify-content:center">{svg(icon, 24)}{b}{dot}</span></span>')


def nav3(mode='upcoming', lists=False, badge=50):
    """Новая панель: «Предстоящее / Неделя» · «+» · «Списки»."""
    left = tab(ICON[mode], not lists, hint=True)
    add = (f'<span style="width:72px;height:44px;border-radius:var(--radius-xl);background:var(--accent);color:var(--on-accent);display:flex;'
           f'align-items:center;justify-content:center;flex-shrink:0">{mi("add", 26)}</span>')
    right = tab(ICON['lists'], lists, badge=badge)
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:57px;background:var(--surface);border-top:1px solid var(--divider);'
            f'display:flex;align-items:center;padding:0 8px">{left}{add}{right}</div>')


def swap_nav(screen, old_sel, new_nav, old_badge=None):
    old = D.nav_bar(old_sel, old_badge) if old_badge else D.nav_bar(old_sel)
    assert old in screen, old_sel
    return screen.replace(old, new_nav)


def title_bar(text):
    return f'<div style="padding:16px 16px 4px;font-size:28px;line-height:36px;font-weight:700">{text}</div>'


# ------------------------------------------------------------------ доска 1: панель

def before():
    return D.all_screen().replace(D.nav_bar(1), D.nav_bar(1, 50))


def upcoming():
    s = D.all_screen()
    body = s.replace('<div style="padding-top:8px">', f'<div style="padding-top:0">{title_bar("Предстоящее")}', 1)
    return swap_nav(body, 1, nav3('upcoming'))


def hold_menu():
    def row(icon, text, sel):
        check = f'<span style="margin-left:auto;color:var(--accent);display:flex">{mi("check", 22)}</span>' if sel else ''
        return (f'<div style="min-height:48px;display:flex;align-items:center;gap:12px;padding:0 16px;font-size:16px;line-height:24px;font-weight:{700 if sel else 600};'
                f'background:{"var(--selected)" if sel else "transparent"}"><span style="display:flex;color:var(--text-secondary)">{svg(icon, 22)}</span>{text}{check}</div>')
    menu = (f'<div style="position:absolute;left:16px;bottom:72px;width:230px;border-radius:var(--radius-lg);background:var(--surface);padding:4px 0;'
            f'box-shadow:0 4px 16px color-mix(in srgb,var(--text-primary) 18%,transparent);border:1px solid var(--divider);overflow:hidden">'
            f'{row(ICON["upcoming"], "Предстоящее", True)}{row(ICON["week"], "Неделя", False)}</div>')
    return I.phone(upcoming() + menu)


def week_tab():
    return swap_nav(week_after_screen(), 2, nav3('week'))


# ------------------------------------------------------------------ «Неделя» как в виджете

def cell(wd, num, tasks, today=False, hidden=0, h=190):
    """Ячейка «Недели» по образцу виджета: дата капсом («ПН 9»), сегодня — жирным; размер под экран приложения."""
    more = f'<span style="margin-left:auto;font-size:11px;font-weight:800;color:var(--text-primary);background:var(--accent-soft);border-radius:12px;padding:0 7px;line-height:16px">+{hidden}</span>' if hidden else ''
    w = f'<span style="font-size:11px;letter-spacing:.02em;font-weight:{800 if today else 500};color:{"var(--text-primary)" if today else "var(--text-secondary)"}">{wd.upper()} {num}</span>'
    return (f'<div style="flex:1;min-width:0;height:{h}px;overflow:hidden;box-sizing:border-box;padding:4px 4px;border-right:1px solid var(--divider);border-bottom:1px solid var(--divider)">'
            f'<div style="display:flex;align-items:center;min-height:18px">{w}{more}</div>{"".join(tasks)}</div>')


def t(*a, e='💼', **k):
    return D.week_task(*a, h=24, fs=12, gap=3, emoji=e, **k)


def week_after_screen():
    h = 152
    r1 = (f'<div style="display:flex">{cell("пн", 9, [t("Вес", "10:00", rep=True, e="❤️"), t("Синк", "11:00", rep=True), t("S3", "11:30"), t("Виджет", "16:00")], today=True, hidden=2, h=h)}'
          f'{cell("вт", 10, [t("Дейлик", "11:00", rep=True), t("Финансы", "14:00", rep=True, e="💰"), t("Бритьё", "14:30", rep=True, e="🏠"), t("ИИ", "14:30", rep=True)], hidden=1, h=h)}</div>')
    r2 = (f'<div style="display:flex">{cell("ср", 11, [t("Налог", "10:30", "high", rep=True, e="💰"), t("Синк/Погашения", "14:00", rep=True), t("Оценка", "16:00", rep=True)], h=h)}'
          f'{cell("чт", 12, [t("Дейлик", "11:00", rep=True), t("Девопсы", "14:30", rep=True), t("ЖКУ", "15:00", rep=True, e="💰"), t("Показания", "15:30", rep=True, e="🏠")], hidden=1, h=h)}</div>')
    r3 = (f'<div style="display:flex">{cell("пт", 13, [t("ВПН", "10:00", rep=True, e="💰"), t("Финансы", "14:00", rep=True, e="💰")], h=h)}'
          f'{cell("сб", 14, [t("Okko", "10:00", rep=True, e="💰"), t("Ростелеком", "10:00", rep=True, e="💰"), t("Yota", "11:00", rep=True, e="💰"), t("Бритьё", "14:30", rep=True, e="🏠")], hidden=2, h=h)}</div>')
    r4 = f'<div style="display:flex">{cell("вс", 15, [t("Стирка", "14:30", rep=True, e="🏠"), t("Уборка", "16:00", rep=True, e="🏠")], h=h)}{D.week_calendar(h)}</div>'
    return D.frame(f'<div style="padding-top:2px">{r1}{r2}{r3}{r4}</div>{D.nav_bar(2)}', H)


def week_before():
    return D.week_screen().replace('height:700px', f'height:{H}px', 1)


# ------------------------------------------------------------------ доска 2: «Списки»

def lists_hub():
    head = (f'<div style="display:flex;align-items:center;padding:12px 4px 4px 16px"><span style="flex:1;font-size:28px;line-height:36px;font-weight:700">Списки</span>'
            f'{D.btn_text("+ Папка")}{D.btn_text("+ Список")}'
            f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{gear_icon(24)}</span></div>')
    search = (f'<div style="margin:4px 16px 8px;height:48px;border-radius:var(--radius-lg);background:var(--surface);display:flex;align-items:center;gap:12px;'
              f'padding:0 16px;color:var(--text-secondary)">{search_icon(22)}<span style="font-size:16px">Поиск задач</span></div>')
    big = lambda e: B.EMOJI_SPAN.format(px=22, w=24, e=e)
    line = lambda ic, txt, n='', bold=False: (f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary)">{ic}'
                                             f'<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary);font-weight:{700 if bold else 400}">{txt}</span>'
                                             f'<span style="font-size:14px;font-weight:{700 if bold else 400}">{n}</span></div>')
    rows = (line(big('📥'), 'Входящие', 50, bold=True) + line(big('💭'), 'Когда-нибудь', 7) + line(big('📌'), 'Без списка', 2)
            + line(mi('check'), 'Выполненные') + D.WONT_ROW(3) + line(D.TRASH_ICON(24), 'Корзина', 5) + line(big('💼'), 'Работа', 12) + line(big('🏠'), 'Дом', 6))
    return D.frame(f'{head}{search}{rows}{nav3("upcoming", lists=True)}', H)


def icons_panel():
    """Было / стало: значки панели — контурные, как в нижней строке виджета."""
    def pair(old, new, name):
        cell_ = lambda ic: f'<span style="width:56px;height:44px;border-radius:var(--radius-lg);background:var(--surface);display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{ic}</span>'
        return (f'<div style="display:flex;align-items:center;gap:16px;padding:8px 0"><span style="width:120px;font-size:15px;font-weight:600">{name}</span>'
                f'{cell_(old)}<span style="color:var(--text-secondary)">→</span>{cell_(new)}</div>')
    rows = (pair(mi('range', 24), svg(ICON['upcoming'], 24), 'Предстоящее') + pair(svg('ic_tab_week', 24), svg(ICON['week'], 24), 'Неделя')
            + pair(mi('list', 24), svg(ICON['lists'], 24), 'Списки') + pair(mi('mail', 24), B.EMOJI_SPAN.format(px=20, w=24, e='📥'), 'Входящие')
            + pair(mi('search', 24), search_icon(24), 'Поиск') + pair(mi('settings', 24), gear_icon(24), 'Настройки'))
    head = ('<div style="font-size:13px;line-height:18px;color:var(--text-secondary);margin-bottom:8px">Слева — сейчас в приложении (залитые Material), '
            'справа — контурные, как в нижней строке виджета: линия 2, круглые концы.</div>')
    return D.frame(f'<div style="padding:24px 20px">{head}{rows}</div>', H)


# ------------------------------------------------------------------ сборка

BOARDS = [
    ('Main.dc.html', 'Нижняя панель', 'Нижняя панель: три места — итерация 55',
     'Слева «Предстоящее / Неделя», посередине «+», справа «Списки». Удержание левого значка — меню «Предстоящее / Неделя», выбор запоминается; '
     'точка под значком подсказывает, что меню есть. Число неразобранных — на «Списках».',
     [('1. Было', 'Шесть вкладок и «+» справа; число неразобранных на «Входящих».', before),
      ('2. Предстоящее', 'Лента «Все» называется «Предстоящее». Панель: календарь · «+» · списки (с числом неразобранных).', upcoming),
      ('3. Удержали значок', 'Меню над значком: «Предстоящее» и «Неделя», выбранный — с галочкой.', hold_menu),
      ('4. Неделя', 'Выбрали «Неделю» — значок в панели сменился на колонки недели.', week_tab)]),
    ('Lists.dc.html', '«Списки», «Неделя», значки', '«Списки», «Неделя» и значки — итерация 55',
     '«Входящие», «Поиск», «Настройки» переехали в «Списки». «Неделя» — строки и подписи как в виджете. Значки панели — контурные, как в виджете.',
     [('1. «Списки»', 'Шестерёнка «Настройки» в шапке, поле «Поиск задач» сверху, «📥 Входящие» первой строкой с числом.', lists_hub),
      ('2. «Неделя» — было', 'Сейчас: без эмодзи, дата строчными «пн 26», строки крупнее.', week_before),
      ('3. «Неделя» — стало', 'Как в виджете: эмодзи слева от времени, дата капсом «ПН 9», сегодня жирным; строки мельче, чем сейчас, но крупнее виджета.', week_tab),
      ('4. Значки', 'Значки панели и новых строк «Списков» — контурные, как в виджете.', icons_panel)]),
]


def build(out):
    os.makedirs(out, exist_ok=True)
    boards, order, y = {}, [], 0
    for name, board_title, title, lead, cols in BOARDS:
        Wd, Hh, body = I.board(title, lead, cols)
        Path(out, name).write_text(I.page(board_title, Wd, Hh, body), encoding='utf-8')
        boards[name] = {'x': 0, 'y': y, 'w': Wd, 'h': Hh, 'title': board_title}
        order.append(name)
        y += Hh + 120
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Итерация 55 — Нижняя панель', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': boards, 'order': order,
              'notes': {'head': {'x': 0, 'y': -300, 'text': 'Нижняя панель, «Списки», «Неделя» и значки (итерация 55)', 'kind': 'title1', 'maxW': 1900}},
              'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', [(n, b['w'], b['h']) for n, b in boards.items()])


if __name__ == '__main__':
    build(sys.argv[1])
