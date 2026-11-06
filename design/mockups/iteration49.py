"""Макеты итерации 49: «Не буду делать» и удаление повторяющейся задачи (новый холст).

Решения PM (06.11.2026): удаление не переписываем — у любого раза повтора «Удалить» удаляет всю серию, но сначала окно
«Удалить серию / Не буду делать / Отмена»; обычные задачи удаляются как раньше, без окна. «Не буду делать» — для всех задач:
у обычной закрывает задачу, у повторяющейся — этот раз (серия идёт дальше); у будущего раза — как в TickTick, окно «Пропустить все /
Выполнить все» для незакрытых повторов перед ним. Кнопка — в «⋮» окна задачи, в режиме выбора, в уведомлении и полоске.
Задача сразу исчезает из ленты, внизу «Отмена»; дальше — в отдельном списке «Не буду делать» (вход на «Списках»), откуда её можно
вернуть в работу или удалить.

Собирается из компонентов дизайн-системы (`design/system/`). Запуск: python3 design/mockups/iteration49.py <папка project>
"""
import datetime
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ds_page import B, D, tokens_css  # noqa: E402

svg, mi, frame, frow, day_header, nav_bar, dlg = D.svg, D.mi, D.frame, D.frow, D.day_header, D.nav_bar, D.dlg
W_PHONE, H_PHONE = 412, 680

# Значок «Не буду делать» (новый, ic_wont_do): круг с крестиком — как круг «выполнено», только вместо галочки ✕.
WONT = ('<svg width="{s}" height="{s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">'
        '<circle cx="12" cy="12" r="9"/><path d="M9 9l6 6M15 9l-6 6"/></svg>')


def wont(size=22):
    return WONT.format(s=size)


SCRIM = '<div style="position:absolute;inset:0;border-radius:28px;background:var(--scrim)"></div>'


def phone(inner):
    return f'<div style="position:relative;width:{W_PHONE}px;height:{H_PHONE}px;flex-shrink:0;border-radius:28px;overflow:hidden">{inner}</div>'


# ------------------------------------------------------------------ лента «Все» (фон)

def feed(rows):
    return frame(f'<div style="padding-top:8px">{rows}</div>{nav_bar(1)}', H_PHONE)


def feed_before():
    return feed(day_header('ПТ, 6 НОЯ', 'Сегодня') + frow('22:00', 'var(--inactive)', 'Теннис', top=False, bottom=False)
                + day_header('ВС, 8 НОЯ') + frow('14:30', 'var(--inactive)', 'Стирка', rep=True, top=False) + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, bottom=False)
                + day_header('ВС, 15 НОЯ') + frow('14:30', 'var(--inactive)', 'Стирка', rep=True, top=False) + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, bottom=False))


def feed_after_wont():
    """После «Не буду делать» на вс 8: раз пропал, серия на вс 15."""
    return feed(day_header('ПТ, 6 НОЯ', 'Сегодня') + frow('22:00', 'var(--inactive)', 'Теннис', top=False, bottom=False)
                + day_header('ВС, 8 НОЯ') + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, top=False, bottom=False)
                + day_header('ВС, 15 НОЯ') + frow('14:30', 'var(--inactive)', 'Стирка', rep=True, top=False) + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, bottom=False)
                + day_header('ВС, 22 НОЯ') + frow('14:30', 'var(--inactive)', 'Стирка', rep=True, top=False, bottom=False))


def feed_after_future():
    """После «Не буду делать» на вс 22 + «Пропустить все»: вс 8 и вс 15 пропали, серия с вс 29."""
    return feed(day_header('ПТ, 6 НОЯ', 'Сегодня') + frow('22:00', 'var(--inactive)', 'Теннис', top=False, bottom=False)
                + day_header('ВС, 8 НОЯ') + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, top=False, bottom=False)
                + day_header('ВС, 15 НОЯ') + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, top=False, bottom=False)
                + day_header('ВС, 29 НОЯ') + frow('14:30', 'var(--inactive)', 'Стирка', rep=True, top=False, bottom=False))


# ------------------------------------------------------------------ окно задачи и «⋮»

def task_card(date='вс, 8 ноя', banner=''):
    card = B.sheet_card(True, width=396)
    card = (card.replace('Подготовить отчёт', 'Стирка').replace('Квартальный отчёт для руководства', 'Постельное и полотенца')
            .replace('пт, 30 окт', date).replace('10:00–11:30', '14:30'))
    rep = f'<span style="color:var(--inactive);display:flex;flex-shrink:0">{svg("ic_repeat_round", 24)}</span>'
    card = card.replace('<span style="width:22px;height:22px;border-radius:50%;border:2px solid var(--ring-high);flex-shrink:0"></span>', rep)
    card = card.replace('class="tool pri" style="background:color-mix(in srgb,var(--ring-high) 16%,var(--surface));color:var(--ring-high)"', 'class="tool pri"')
    return f'<div style="position:absolute;left:8px;right:8px;bottom:16px">{banner}{card}</div>'


def menu(items, top=430):
    """Выпадающее меню «⋮» (DropdownMenu): строки 48, значок слева, черта, подпись «создано …»."""
    def item(icon, text, color='var(--text-primary)', new=False):
        mark = '<span style="margin-left:auto;font-size:10px;font-weight:800;letter-spacing:.04em;color:var(--on-accent);background:var(--accent);border-radius:4px;padding:1px 6px">НОВОЕ</span>' if new else ''
        return (f'<div style="min-height:48px;display:flex;align-items:center;gap:12px;padding:0 16px;font-size:16px;line-height:24px;font-weight:600;color:{color}">'
                f'<span style="display:flex">{icon}</span>{text}{mark}</div>')
    rows = ''.join(item(*i) if isinstance(i, tuple) else i for i in items)
    return (f'<div style="position:absolute;right:20px;top:{top}px;width:260px;border-radius:var(--radius-lg);background:var(--surface);padding:4px 0;'
            f'box-shadow:0 4px 16px color-mix(in srgb,var(--text-primary) 18%,transparent);border:1px solid var(--divider);overflow:hidden">{rows}</div>')


DIVIDER = '<div style="height:1px;background:var(--divider);margin:4px 12px"></div>'
CREATED = '<div style="padding:8px 16px;font-size:12px;line-height:16px;color:var(--text-secondary)">создано пн, 2 ноя 2026</div>'
M_WONT = (wont(22), 'Не буду делать', 'var(--text-primary)', True)
M_DELETE = (svg('ic_tool_delete', 22), 'Удалить', 'var(--danger)')


def task_menu_screen():
    return phone(feed_before() + SCRIM + task_card() + menu([M_WONT, M_DELETE, DIVIDER, CREATED], top=496))


def plain_menu_screen():
    """Обычная (не повторяющаяся) задача: то же меню; «Удалить» — сразу, без окна."""
    card = task_card().replace('Стирка', 'Купить продукты').replace('Постельное и полотенца', 'Молоко, хлеб, яйца').replace(svg('ic_repeat_round', 24), '<span style="width:22px;height:22px;border-radius:50%;border:2px solid var(--inactive);display:block"></span>')
    return phone(feed_before() + SCRIM + card + menu([M_WONT, M_DELETE, DIVIDER, CREATED], top=496))


# ------------------------------------------------------------------ окна

def stacked(title, text, buttons, width=356):
    """Окно по центру (SheetDialog) с кнопками столбиком: (текст, вид) — 'danger' красная, 'primary' чёрная, 'outline', 'text'."""
    def b(t, kind):
        style = {
            'danger': 'background:var(--danger);color:var(--on-accent)',
            'primary': 'background:var(--accent);color:var(--on-accent)',
            'outline': 'border:1px solid var(--divider);color:var(--text-primary)',
            'text': 'color:var(--text-primary)',
        }[kind]
        icon = f'<span style="display:flex;margin-right:8px">{wont(20)}</span>' if t == 'Не буду делать' else ''
        return (f'<span style="min-height:50px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:700;padding:0 12px;{style}">{icon}{t}</span>')
    return (f'<div style="width:{width}px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden">'
            f'<div style="padding:24px 24px 8px"><div style="font-size:22px;line-height:28px;font-weight:700">{title}</div>'
            f'<div style="font-size:14px;line-height:20px;color:var(--text-secondary);margin-top:8px">{text}</div></div>'
            f'<div style="display:flex;flex-direction:column;gap:8px;padding:16px">{"".join(b(*x) for x in buttons)}</div></div>')


def centered(base, dialog):
    return phone(base + SCRIM + f'<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center">{dialog}</div>')


def delete_series_dialog():
    return stacked('Удалить серию повторов?',
                   'Задача «Стирка» повторяется. Удалятся все её будущие повторы, выполненные останутся в истории.<br><br>'
                   'Чтобы убрать только этот повтор, нажмите «Не буду делать».',
                   [('Удалить серию', 'danger'), ('Не буду делать', 'outline'), ('Отмена', 'text')])


def unclosed_dialog():
    return stacked('Есть незакрытые повторы',
                   'Перед вс, 22 ноя остались повторы «Стирки»: вс, 8 ноя и вс, 15 ноя. Что с ними сделать?',
                   [('Пропустить все', 'primary'), ('Выполнить все', 'outline'), ('Отмена', 'text')])


def bulk_dialog():
    return stacked('Удалить выбранные задачи?',
                   'Среди выбранных 2 повторяющиеся — их серии удалятся полностью, выполненные повторы останутся в истории.<br><br>'
                   'Чтобы убрать только текущие повторы, нажмите «Не буду делать».',
                   [('Удалить', 'danger'), ('Не буду делать', 'outline'), ('Отмена', 'text')])


# ------------------------------------------------------------------ подсказка «Отмена»

def snack(text, icon):
    return (f'<div style="position:absolute;left:16px;right:16px;bottom:84px;min-height:56px;border-radius:var(--radius-xl);background:color-mix(in srgb,var(--surface) 88%,transparent);'
            f'border:1px solid var(--divider);display:flex;align-items:center;padding:0 4px 0 16px;gap:12px">'
            f'<span style="display:flex;color:var(--text-secondary)">{icon}</span>'
            f'<span style="flex:1;font-size:14px;line-height:20px;font-weight:600;padding:8px 0">{text}</span>'
            f'<span style="min-height:40px;padding:0 12px;display:flex;align-items:center;font-size:14px;font-weight:700;color:var(--accent)">Отмена</span></div>')


def after_wont_screen():
    return phone(feed_after_wont() + snack('Не буду делать · вс, 8 ноя', wont(22)))


def after_delete_screen():
    rows = (day_header('ПТ, 6 НОЯ', 'Сегодня') + frow('22:00', 'var(--inactive)', 'Теннис', top=False, bottom=False)
            + day_header('ВС, 8 НОЯ') + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, top=False, bottom=False)
            + day_header('ВС, 15 НОЯ') + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, top=False, bottom=False))
    return phone(feed(rows) + snack('Задача удалена', svg('ic_tool_delete', 22)))


def after_future_screen():
    return phone(feed_after_future() + snack('Не буду делать · вс, 22 ноя · пропущено: 2', wont(22)))


# ------------------------------------------------------------------ будущий раз

def future_banner():
    return (f'<div style="margin:0 0 8px;border-radius:var(--radius-lg);background:var(--selected);display:flex;align-items:center;gap:10px;padding:10px 14px;'
            f'font-size:14px;line-height:20px;font-weight:600"><span style="display:flex;color:var(--text-secondary)">{svg("ic_tool_repeat", 20)}</span>'
            f'<span style="flex:1">Будущий повтор · ближайший — вс, 8 ноя</span></div>')


def future_menu_screen():
    base = feed_before()
    return phone(base + SCRIM + task_card('вс, 22 ноя', future_banner()) + menu([M_WONT, M_DELETE, DIVIDER, CREATED], top=496))


# ------------------------------------------------------------------ режим выбора, уведомление, полоска

def selection_screen():
    rows = (day_header('ПТ, 6 НОЯ', 'Сегодня').replace('padding:12px 0 4px 12px', 'padding:70px 0 4px 12px')
            + D.sel_row('22:00', 'var(--inactive)', 'Теннис', selected=True, top=False, bottom=False)
            + day_header('ВС, 8 НОЯ')
            + D.sel_row('14:30', 'var(--inactive)', 'Стирка', selected=True, rep=True, top=False)
            + D.sel_row('16:00', 'var(--inactive)', 'Уборка', selected=True, rep=True, bottom=False)
            + day_header('ВС, 15 НОЯ')
            + D.sel_row('14:30', 'var(--inactive)', 'Стирка', ghost=True, rep=True, top=False, bottom=False))
    actions = [('ic_node_check', 'Выполнить'), ('WONT', 'Не буду делать'), ('ic_popup_tomorrow', 'На завтра'), ('ic_widget_all', 'На другой день'),
               ('ic_list_inbox', 'В список'), ('ic_tool_priority', 'Приоритет'), ('ic_snack_restore', 'Вернуть'), ('ic_tool_delete', 'Удалить')]
    rib = D.ribbon([(a if a != 'WONT' else 'ic_node_check', t) for a, t in actions], dim=('Вернуть',))
    rib = rib.replace(svg('ic_node_check', 24) + '<span style="font-size:11px;line-height:14px;font-weight:700;white-space:nowrap">Не буду делать', wont(24) + '<span style="font-size:11px;line-height:14px;font-weight:700;white-space:nowrap">Не буду делать')
    return frame(f'{D.select_bar(3)}<div>{rows}</div>{rib}', H_PHONE)


def selection_dialog_screen():
    return centered(selection_screen(), bulk_dialog())


def notification_card():
    btn = lambda t: f'<span style="min-height:36px;padding:0 12px;display:flex;align-items:center;font-size:14px;font-weight:600;color:var(--accent)">{t}</span>'
    return (f'<div style="border-radius:var(--radius-xl);background:var(--surface);padding:12px 16px;border:1px solid var(--divider)">'
            f'<div style="display:flex;align-items:center;gap:8px;font-size:12px;line-height:16px;color:var(--text-secondary)"><span style="color:var(--accent);display:flex">{svg("ic_notification", 16)}</span>Simple Tasks · Напоминания · сейчас</div>'
            f'<div style="font-size:16px;line-height:24px;font-weight:700;padding-top:4px">Стирка</div>'
            f'<div style="font-size:14px;line-height:20px;color:var(--text-secondary)">Сегодня, 14:30 · Дом</div>'
            f'<div style="display:flex;gap:0;padding-top:8px;margin-left:-12px">{btn("Выполнить")}{btn("Не буду делать")}{btn("На завтра")}</div></div>')


def strip_card():
    round_btn = lambda inner, bg, fg: f'<span style="width:48px;height:48px;border-radius:50%;background:{bg};color:{fg};display:flex;align-items:center;justify-content:center;flex-shrink:0">{inner}</span>'
    soft = 'color-mix(in srgb,var(--on-accent) 18%,transparent)'
    return (f'<div style="height:68px;border-radius:34px;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;gap:8px;padding:0 8px 0 16px">{svg("ic_tool_reminder", 22)}'
            f'<div style="flex:1;min-width:0"><div style="font-size:16px;line-height:24px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">Стирка</div>'
            f'<div style="font-size:11px;line-height:16px;opacity:.8">Сегодня, 14:30 · Дом</div></div>'
            f'{round_btn(wont(22), soft, "var(--on-accent)")}{round_btn(svg("ic_popup_tomorrow", 22), soft, "var(--on-accent)")}'
            f'{round_btn(svg("ic_node_check", 22), "var(--surface)", "var(--accent)")}</div>')


def reminders_screen():
    wall = 'linear-gradient(165deg,#C5D8F0 0%,#BFD2EE 55%,#9FB8DE 100%)'
    lab = lambda t: f'<div style="font-size:12px;font-weight:700;color:#3B4252;margin:0 4px 6px">{t}</div>'
    return (f'<div style="width:{W_PHONE}px;height:{H_PHONE}px;border-radius:28px;background:{wall};padding:20px 12px;display:flex;flex-direction:column;gap:18px;flex-shrink:0">'
            f'<div>{lab("Уведомление: кнопка между «Выполнить» и «На завтра»")}{notification_card()}</div>'
            f'<div style="margin-top:auto">{lab("Полоска поверх приложений: новая круглая кнопка слева")}{strip_card()}</div></div>')


# ------------------------------------------------------------------ список «Не буду делать»

def lists_screen():
    head = f'<div style="display:flex;align-items:center"><div style="flex:1">{D.screen_title("Списки")}</div>{D.btn_text("+ Папка")}{D.btn_text("+ Список")}</div>'
    row = lambda ic, t, n='', new=False: (f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary);{"background:var(--selected);" if new else ""}">{ic}'
                                         f'<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary)">{t}</span><span style="font-size:14px">{n}</span></div>')
    proj = lambda e, name, n: (f'<div style="display:flex;align-items:center;padding:12px 16px"><span style="width:24px;text-align:center;font-size:18px">{e}</span>'
                               f'<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px">{name}</span><span style="font-size:14px;color:var(--text-secondary)">{n}</span></div>')
    body = (head + row(mi('check'), 'Выполненные') + row(wont(24), 'Не буду делать', 3, True) + row(svg('ic_tool_delete', 24), 'Корзина', 5)
            + proj('📥', 'Входящие', 9) + proj('🏠', 'Дом', 7) + proj('💼', 'Работа', 12) + proj('🎾', 'Спорт', 4))
    return frame(f'<div style="padding-top:8px">{body}</div>{nav_bar(3)}', H_PHONE)


def wont_row(left, title, sub, top=True, bottom=True, rep=False):
    lines = left if isinstance(left, list) else [left]
    t = ('<div style="width:56px;text-align:right;font-size:11px;line-height:15px;font-weight:700;color:var(--text-secondary);display:flex;flex-direction:column;justify-content:center;flex-shrink:0">'
         + ''.join(f'<span>{x}</span>' for x in lines) + '</div>')
    node = f'<span style="color:var(--inactive);display:flex">{wont(20)}</span>'
    rail = (f'<div style="width:26px;display:flex;flex-direction:column;align-items:center;flex-shrink:0"><span style="width:1px;flex:1;background:{"var(--divider)" if top else "transparent"}"></span>'
            f'{node}<span style="width:1px;flex:1;background:{"var(--divider)" if bottom else "transparent"}"></span></div>')
    badge = f'<span style="display:flex;color:var(--inactive);margin-left:6px">{svg("ic_tool_repeat", 16)}</span>' if rep else ''
    plaque = (f'<div style="flex:1;min-width:0;margin:3px 0;border-radius:var(--radius-md);background:var(--surface);display:flex;align-items:center;padding:0 12px;opacity:.85">'
              f'<div style="flex:1;min-width:0"><div style="font-size:14px;font-weight:700;color:var(--text-secondary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{title}</div>'
              f'<div style="font-size:12px;line-height:14px;font-weight:500;color:var(--text-secondary)">{sub}</div></div>{badge}</div>')
    return f'<div style="display:flex;height:58px;padding-right:8px">{t}{rail}{plaque}</div>'


def wont_screen():
    top = (f'<div style="display:flex;align-items:center;padding:8px 4px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("back")}</span>'
           f'<span style="font-size:22px;line-height:28px;font-weight:700">Не буду делать</span></div>')
    rows = (day_header('Сегодня') + wont_row(['вс, 8 ноя', '14:30'], 'Стирка', 'Дом · повтор', top=False, rep=True)
            + wont_row(['—'], 'Записаться к стоматологу', 'Входящие', bottom=False)
            + day_header('Вчера') + wont_row(['чт, 5 ноя', '10:00'], 'Пробежка', 'Спорт · повтор', top=False, bottom=False, rep=True))
    return frame(f'{top}{rows}', H_PHONE)


def wont_task_screen():
    banner = (f'<div style="margin:0 0 8px;border-radius:var(--radius-lg);background:var(--selected);display:flex;align-items:center;gap:10px;padding:10px 14px;'
              f'font-size:14px;line-height:20px;font-weight:600"><span style="display:flex;color:var(--text-secondary)">{wont(20)}</span>'
              f'<span style="flex:1">Не буду делать · отмечено пт, 6 ноя</span></div>')
    card = task_card(banner=banner)
    card = card.replace('class="tool sel"', 'class="tool dim"').replace('class="tool pri"', 'class="tool pri dim"').replace('<span class="confirm">' + B.CHECK + '</span>', '')
    card = card.replace(svg('ic_repeat_round', 24), f'<span style="color:var(--inactive);display:flex">{wont(24)}</span>')
    card = card.replace(f'<span class="clr" style="position:static">{B.X}</span>', '').replace(f'<span class="clr">{B.X}</span>', '')
    m = menu([(svg('ic_snack_restore', 22), 'Вернуть в работу'), DIVIDER, M_DELETE], top=496)
    return phone(wont_screen() + SCRIM + card + m)


# ------------------------------------------------------------------ доски

def cap(t, sub):
    return (f'<div style="font-size:17px;font-weight:700;margin-bottom:4px">{t}</div>'
            f'<div style="font-size:13px;color:var(--text-secondary);line-height:18px;min-height:54px;width:{W_PHONE}px;margin-bottom:10px">{sub}</div>')


def board(title, lead, cols):
    gap = 40
    W = len(cols) * W_PHONE + (len(cols) - 1) * gap + 80
    H = 36 + 40 + 52 + 22 + 78 + H_PHONE + 40
    body = (f'<div style="font-size:28px;font-weight:800;letter-spacing:-.4px">{title}</div>'
            f'<div style="font-size:15px;color:var(--text-secondary);margin:6px 0 22px;line-height:21px;max-width:{W - 80}px">{lead}</div>'
            f'<div style="display:flex;gap:{gap}px;align-items:flex-start">' + ''.join(f'<div>{cap(t, s)}{f()}</div>' for t, s, f in cols) + '</div>')
    return W, H, body


def page(title, W, H, body):
    html = f'''<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<title>{title}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&amp;family=Noto+Color+Emoji&amp;display=swap" rel="stylesheet">
<style>{tokens_css()}{B.COMMON}body{{margin:0;padding:0}}</style>
</helmet>
<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EFEFEF;padding:36px 40px;font-family:var(--font-sans);color:var(--text-primary)">
{body}
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script>
</body>
</html>'''
    return B.emoji_lists(html)


BOARDS = [
    ('Main.dc.html', 'Удаление повторяющейся задачи', 'Удаление и «Не буду делать» — итерация 49',
     'У повторяющейся задачи «Удалить» удаляет всю серию, но сначала спрашивает. Убрать один раз — «Не буду делать». Обычная задача удаляется сразу, как раньше.',
     [('1. Новый пункт в «⋮»', '«Не буду делать» — первым, над «Удалить». Есть у всех задач: в приложении и в окне поверх рабочего стола из виджета.', task_menu_screen),
      ('2. «Удалить» у повторяющейся', 'Окно по центру: «Удалить серию» (красная), «Не буду делать», «Отмена». Выполненные повторы остаются в истории.', lambda: centered(feed_before() + SCRIM + task_card(), delete_series_dialog())),
      ('3. «Удалить серию»', 'Как сейчас: серия в Корзину, внизу «Задача удалена · Отмена». Будущих повторов «Стирки» больше нет.', after_delete_screen),
      ('4. «Не буду делать»', 'Раз вс 8 пропал сразу, серия на вс 15. Внизу «Не буду делать · вс, 8 ноя · Отмена». Раз — в списке «Не буду делать».', after_wont_screen)]),
    ('Future.dc.html', 'Будущий повтор', 'Будущий повтор — как в TickTick',
     'Нажатие на будущий повтор открывает этот раз со своей датой. Закрыть его можно, только решив, что делать с незакрытыми повторами перед ним.',
     [('1. Будущий повтор открыт', 'Дата — вс, 22 ноя; сверху подсказка «Будущий повтор · ближайший — вс, 8 ноя». В «⋮» те же пункты.', future_menu_screen),
      ('2. «Не буду делать» на вс 22', 'Окно: «Пропустить все» — вс 8 и вс 15 исчезают; «Выполнить все» — становятся выполненными. «Отмена» — ничего не меняется.', lambda: centered(feed_before() + SCRIM + task_card('вс, 22 ноя', future_banner()), unclosed_dialog())),
      ('3. После «Пропустить все»', 'Серия продолжается с вс 29. Внизу подсказка с «Отменой» — она возвращает все три раза.', after_future_screen)]),
    ('Elsewhere.dc.html', 'Режим выбора, уведомление, полоска', '«Не буду делать» в режиме выбора и в напоминаниях',
     'В ленте действий — вторым, после «Выполнить». Если среди выбранных есть повторяющиеся, «Удалить» спрашивает один раз за все.',
     [('1. Режим выбора', 'Новое действие «Не буду делать». Будущие повторы, как и раньше, не выбираются.', selection_screen),
      ('2. «Удалить» с повторяющимися', 'Одно окно на все выбранные. «Не буду делать» закрывает текущие повторы, обычные задачи — целиком.', selection_dialog_screen),
      ('3. Уведомление и полоска', 'В уведомлении — кнопка текстом; в полоске — круглая кнопка со значком ✕ слева от «На завтра».', reminders_screen)]),
    ('List.dc.html', 'Список «Не буду делать»', 'Список «Не буду делать»',
     'Вход — на экране «Списки», между «Выполненными» и «Корзиной». Здесь же видны задачи со статусом Won\'t Do из TickTick.',
     [('1. «Списки»', 'Новая строка со значком ✕ в круге и числом задач.', lists_screen),
      ('2. Экран списка', 'Как «Выполненные»: по дням отметки, серые, вместо кружка — ✕. Под названием — список и «повтор» у раза серии.', wont_screen),
      ('3. Задача из списка', 'Только чтение. В «⋮»: «Вернуть в работу» (раз повтора вернётся обычной задачей) и «Удалить» (в Корзину).', wont_task_screen)]),
]


if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    boards, order, x, y = {}, [], 0, 0
    for name, board_title, title, lead, cols in BOARDS:
        W, H, body = board(title, lead, cols)
        Path(out, name).write_text(page(board_title, W, H, body), encoding='utf-8')
        boards[name] = {'x': 0, 'y': y, 'w': W, 'h': H, 'title': board_title}
        order.append(name)
        y += H + 120
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Итерация 49 — «Не буду делать»', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': boards, 'order': order,
              'notes': {'head': {'x': 0, 'y': -300, 'text': '«Не буду делать» и удаление повторов (итерация 49)', 'kind': 'title1', 'maxW': 1900}},
              'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', [(n, b['w'], b['h']) for n, b in boards.items()])
