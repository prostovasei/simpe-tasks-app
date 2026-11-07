"""Макеты итерации 53: «Отложить» вместо «На завтра» в уведомлении и на полоске (T-84, новый холст).

Решения PM (07.11.2026, вариант Б):
- «Отложить» переносит срок самой задачи, а не только напоминание; «На завтра» входит в «Отложить».
- Кнопки уведомления и полоски: «Выполнено», «Не буду делать», «Отложить». «Отложить» открывает выбор, когда.
- Задача со временем: через 1 час, через 3 часа, вечером (19:00), завтра, выбрать дату и время…
- Задача на весь день: завтра, послезавтра, в субботу, через неделю, выбрать дату…
- Новый срок считается от старого срока задачи; если он уже прошёл — от текущего момента. Период едет целиком.
- Повторяющаяся — переносится только этот раз, серия на своём расписании.

Запуск: python3 design/mockups/iteration53.py <папка project>   → canvas.json, Strip.dc.html, Notification.dc.html
"""
import datetime
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import iteration49 as I  # noqa: E402

D, svg, mi = I.D, I.svg, I.D.mi
W_PHONE, H_PHONE = I.W_PHONE, I.H_PHONE
SHADOW = 'box-shadow:0 8px 28px color-mix(in srgb,var(--text-primary) 22%,transparent)'
ROUND = 'color-mix(in srgb,var(--on-accent) 18%,transparent)'


# ------------------------------------------------------------------ фон: чужое приложение (полоска — поверх любого экрана)

def other_app():
    """Условный мессенджер: шапка и серые «сообщения» — показывает, что полоска поверх чужого приложения."""
    bubble = lambda w, right=False: (f'<div style="display:flex;justify-content:{"flex-end" if right else "flex-start"};padding:4px 16px">'
                                     f'<span style="width:{w}px;height:36px;border-radius:18px;background:{"var(--selected)" if right else "var(--surface)"}"></span></div>')
    head = ('<div style="height:64px;display:flex;align-items:center;gap:12px;padding:8px 16px 0;background:var(--surface);border-bottom:1px solid var(--divider)">'
            '<span style="width:36px;height:36px;border-radius:50%;background:var(--divider)"></span>'
            '<div><div style="width:120px;height:12px;border-radius:6px;background:var(--divider)"></div>'
            '<div style="width:70px;height:10px;border-radius:5px;background:var(--divider);margin-top:6px;opacity:.6"></div></div></div>')
    body = head + '<div style="padding-top:12px">' + ''.join(bubble(w, r) for w, r in [(220, False), (160, True), (260, False), (120, True), (200, False), (180, True), (240, False)]) + '</div>'
    return f'<div style="position:absolute;inset:0;background:var(--background)">{body}</div>'


# ------------------------------------------------------------------ полоска

def round_btn(icon, kind='ghost'):
    bg = {'ghost': f'background:{ROUND}', 'white': 'background:var(--surface);color:var(--accent)',
          'pressed': 'background:var(--surface);color:var(--accent);box-shadow:0 0 0 3px color-mix(in srgb,var(--on-accent) 45%,transparent)'}[kind]
    return f'<span style="width:48px;height:48px;border-radius:50%;{bg};display:flex;align-items:center;justify-content:center;flex-shrink:0">{icon}</span>'


def strip(title, subtitle, pressed=False):
    """Полоска 0.53: «Не буду делать», «Отложить» (часы), «Выполнено». Значок часов прежний — смысл «позже»."""
    return (f'<div style="height:68px;border-radius:34px;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;gap:10px;padding:0 10px 0 16px">'
            f'{svg("ic_tool_reminder", 22)}<div style="flex:1;min-width:0"><div style="font-size:16px;line-height:24px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{title}</div>'
            f'<div style="font-size:11px;line-height:16px;opacity:.8">{subtitle}</div></div>'
            f'{round_btn(I.wont(22))}{round_btn(svg("ic_popup_tomorrow", 22), "pressed" if pressed else "ghost")}{round_btn(svg("ic_node_check", 22), "white")}</div>')


def undo_strip(text):
    return (f'<div style="height:68px;border-radius:34px;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;gap:12px;padding:0 8px 0 16px;position:relative;overflow:hidden">'
            f'{svg("ic_popup_tomorrow", 22)}<span style="flex:1;font-size:16px;line-height:24px;font-weight:700">{text}</span>'
            f'<span style="font-size:14px;font-weight:700;color:var(--accent);background:var(--surface);border-radius:20px;padding:8px 16px">Отменить</span>'
            f'<span style="position:absolute;left:0;bottom:0;height:3px;width:55%;background:color-mix(in srgb,var(--on-accent) 60%,transparent)"></span></div>')


def menu_row(label, when, last=False, icon=None):
    ic = f'<span style="display:flex;color:var(--text-secondary)">{icon}</span>' if icon else ''
    return (f'<div style="min-height:48px;display:flex;align-items:center;gap:12px;padding:0 20px;{"" if last else "border-bottom:1px solid var(--divider)"}">'
            f'{ic}<span style="flex:1;font-size:16px;line-height:24px;font-weight:600">{label}</span>'
            f'<span style="font-size:14px;line-height:20px;font-weight:600;color:var(--text-secondary)">{when}</span></div>')


TIMED = [('Через 1 час', '18:00'), ('Через 3 часа', '20:00'), ('Вечером', '19:00'), ('Завтра', 'чт, 12 ноя, 17:00')]
ALL_DAY = [('Завтра', 'чт, 12 ноя'), ('Послезавтра', 'пт, 13 ноя'), ('В субботу', 'сб, 14 ноя'), ('Через неделю', 'ср, 18 ноя')]
LATE = [('Через 1 час', '21:30'), ('Через 3 часа', '23:30'), ('Завтра', 'чт, 12 ноя, 18:00')]


def menu(rows, pick='Выбрать дату и время…', title=None, width=388):
    head = (f'<div style="padding:14px 20px 6px;font-size:13px;line-height:18px;font-weight:700;color:var(--text-secondary);letter-spacing:.2px">{title}</div>' if title else '')
    body = ''.join(menu_row(l, w) for l, w in rows) + menu_row(pick, '', last=True, icon=svg('ic_widget_all', 20))
    return f'<div style="width:{width}px;border-radius:var(--radius-xl);background:var(--surface);overflow:hidden;{SHADOW}">{head}{body}</div>'


def at_bottom(inner, gap=12):
    return f'<div style="position:absolute;left:12px;right:12px;bottom:{gap + 12}px;display:flex;flex-direction:column;gap:8px">{inner}</div>'


def strip_before():
    old = strip('Созвон с командой', 'Сегодня, 17:00 · Работа')
    return I.phone(other_app() + at_bottom(old))


def strip_menu():
    return I.phone(other_app() + at_bottom(menu(TIMED, title='ОТЛОЖИТЬ «СОЗВОН С КОМАНДОЙ»') + strip('Созвон с командой', 'Сегодня, 17:00 · Работа', pressed=True)))


def strip_done():
    return I.phone(other_app() + at_bottom(undo_strip('Отложено на 18:00')))


def strip_all_day():
    return I.phone(other_app() + at_bottom(menu(ALL_DAY, pick='Выбрать дату…', title='ОТЛОЖИТЬ «ОПЛАТИТЬ ИНТЕРНЕТ»')
                                           + strip('Оплатить интернет', 'Сегодня · Дом', pressed=True)))


# ------------------------------------------------------------------ уведомление

def notif_btn(label, hl=False):
    style = 'background:var(--selected);border-radius:18px' if hl else ''
    return f'<span style="min-height:36px;padding:0 12px;display:flex;align-items:center;font-size:14px;font-weight:700;color:var(--accent);{style}">{label}</span>'


def notification(hl=False):
    return ('<div style="border-radius:var(--radius-xl);background:var(--surface);padding:12px 16px;border:1px solid var(--divider);' + SHADOW + '">'
            '<div style="display:flex;align-items:center;gap:8px;font-size:12px;line-height:16px;color:var(--text-secondary)"><span style="color:var(--accent);display:flex">'
            + svg('ic_notification', 16) + '</span>Simple Tasks · Напоминания · сейчас</div>'
            '<div style="font-size:16px;line-height:24px;font-weight:700;padding-top:4px">Созвон с командой</div>'
            '<div style="font-size:14px;line-height:20px;color:var(--text-secondary)">Сегодня, 17:00 · Работа</div>'
            f'<div style="display:flex;gap:4px;padding-top:8px;margin-left:-12px">{notif_btn("Выполнено")}{notif_btn("Не буду делать")}{notif_btn("Отложить", hl)}</div></div>')


def shade():
    """Шторка уведомлений поверх чужого приложения."""
    clock = '<div style="padding:28px 24px 12px;font-size:40px;line-height:44px;font-weight:300">16:58</div>'
    return f'<div style="position:absolute;inset:0;background:color-mix(in srgb,var(--background) 92%,var(--text-primary))">{clock}<div style="padding:0 12px">{notification(True)}</div></div>'


def notif_shade():
    return I.phone(other_app() + shade())


def window(rows, title, sub):
    """Окно «Отложить» поверх экрана (из уведомления): заголовок, задача, варианты, «Отмена». Выбор срабатывает сразу."""
    head = (f'<div style="padding:22px 24px 10px"><div style="font-size:22px;line-height:28px;font-weight:700">{title}</div>'
            f'<div style="font-size:14px;line-height:20px;color:var(--text-secondary);margin-top:4px">{sub}</div></div>')
    body = ''.join(menu_row(l, w) for l, w in rows) + menu_row('Выбрать дату и время…', '', last=True, icon=svg('ic_widget_all', 20))
    cancel = ('<div style="padding:12px 16px 16px"><span style="min-height:50px;border-radius:var(--radius-lg);border:1px solid var(--divider);display:flex;align-items:center;'
              'justify-content:center;font-weight:700">Отмена</span></div>')
    return f'<div style="width:356px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden">{head}{body}{cancel}</div>'


def notif_window():
    return I.centered(other_app(), window(TIMED, 'Отложить', 'Созвон с командой · сегодня, 17:00'))


def notif_done():
    snack = D.snackbar('Отложено на сегодня, 18:00', 'ic_popup_tomorrow')
    return I.phone(other_app() + f'<div style="position:absolute;left:16px;right:16px;bottom:28px;display:flex;justify-content:center">{snack}</div>')


def notif_late():
    """Срок уже прошёл: задача на 18:00, «Отложить» нажали в 20:30 — считаем от сейчас."""
    return I.centered(other_app(), window(LATE, 'Отложить', 'Позвонить маме · сегодня, 18:00 (прошло)'))


# ------------------------------------------------------------------ сборка

BOARDS = [
    ('Strip.dc.html', 'Полоска', 'Полоска напоминания: «Отложить» — итерация 53',
     'Часы на полоске теперь «Отложить»: открывают выбор над полоской. Новый срок — у самой задачи, справа видно, каким он станет. После выбора — «Отложено на … · Отменить».',
     [('1. Было', 'Часы — «на завтра в то же время», сразу без выбора.', strip_before),
      ('2. Нажали «Отложить»', 'Над полоской — варианты для задачи со временем; справа новый срок. Нажатие на вариант сразу переносит задачу.', strip_menu),
      ('3. После выбора', 'Полоска сменяется на «Отложено на 18:00 · Отменить» (1,5 с), потом уезжает.', strip_done),
      ('4. Задача на весь день', 'Только дни, задача остаётся без времени. «Выбрать дату…» — окно даты поверх экрана.', strip_all_day)]),
    ('Notification.dc.html', 'Уведомление', 'Уведомление: «Отложить» — итерация 53',
     'В уведомлении три кнопки: «Выполнено», «Не буду делать», «Отложить». Меню в самом уведомлении Android не показывает — «Отложить» открывает окно поверх экрана.',
     [('1. Уведомление', '«На завтра» убрано — теперь это вариант «Завтра» внутри «Отложить».', notif_shade),
      ('2. Окно «Отложить»', 'Поверх того, что на экране. Вариант срабатывает сразу; «Отмена» — ничего не меняет.', notif_window),
      ('3. После выбора', 'Подсказка «Отложено на … · Отмена» ~4 с, потом окно закрывается само.', notif_done),
      ('4. Срок уже прошёл', 'Задача на 18:00, нажали в 20:30: от старого срока вышло бы 19:00 (прошло) — считаем от сейчас. «Вечером» скрыт: 19:00 уже позади.', notif_late)]),
]


def build(out):
    os.makedirs(out, exist_ok=True)
    boards, order, y = {}, [], 0
    for name, board_title, title, lead, cols in BOARDS:
        Wd, H, body = I.board(title, lead, cols)
        Path(out, name).write_text(I.page(board_title, Wd, H, body), encoding='utf-8')
        boards[name] = {'x': 0, 'y': y, 'w': Wd, 'h': H, 'title': board_title}
        order.append(name)
        y += H + 120
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Итерация 53 — Отложить', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': boards, 'order': order,
              'notes': {'head': {'x': 0, 'y': -300, 'text': '«Отложить» в уведомлении и на полоске (итерация 53, T-84)', 'kind': 'title1', 'maxW': 1900}},
              'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', [(n, b['w'], b['h']) for n, b in boards.items()])


if __name__ == '__main__':
    build(sys.argv[1])
