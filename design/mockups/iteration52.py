"""Макеты итерации 52: новая логика «Входящих», разделы «Когда-нибудь» и «Без списка» (новый холст).

Решения PM (07.11.2026, после разбора справки Todoist, TickTick и SingularityApp и вопросов о привычках):
- «Входящие» — только неразобранное: задачи без даты и без списка. Дата или список — задача уходит сама.
- «Когда-нибудь» — задачи со списком, но без даты; строка на «Списках», внутри группы по спискам, «+» — новая задача без даты.
- «Без списка» — задачи без списка, но с датой; строка на «Списках», лента по дням.
- Счётчик неразобранных на вкладке «Входящие»; вкладка «Входящие» в виджете — по той же логике.

Запуск: python3 design/mockups/iteration52.py <папка project>   → canvas.json, Main.dc.html, Widget.dc.html
"""
import datetime
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import iteration49 as I  # noqa: E402

D, B, svg = I.D, I.B, I.svg
frow, day_header, mi, frame = D.frow, D.day_header, D.mi, D.frame
W_PHONE, H_PHONE = I.W_PHONE, I.H_PHONE

INBOX = ('var(--inbox-list)', 'ic_list_inbox')
WORK = ('var(--list-blue)', 'ic_list_work')
HOME = ('var(--list-orange)', 'ic_list_home')
SPORT = ('var(--list-teal)', 'ic_list_sport')


def nav_bar(sel, inbox_count=None):
    """Нижняя панель как в приложении; у «Входящих» — счётчик неразобранных (чёрный кружок с числом)."""
    bar = D.nav_bar(sel)
    if inbox_count is None:
        return bar
    badge = (f'<span style="position:absolute;top:-6px;right:-10px;min-width:18px;height:18px;padding:0 5px;box-sizing:border-box;border-radius:9px;'
             f'background:var(--accent);color:var(--on-accent);font-size:11px;line-height:18px;font-weight:800;text-align:center">{inbox_count}</span>')
    mail = mi('mail', 24)
    return bar.replace(mail, f'<span style="position:relative;display:flex">{mail}{badge}</span>', 1)


def row(title, list_=INBOX, left='', rep=False, rem=None, top=True, bottom=True, done=False):
    color, icon = list_
    return frow(left, 'var(--inactive)', title, rep=rep, rem=rem, top=top, bottom=bottom, done=done, list_color=color, list_icon=icon)


def top_bar(title, count=None):
    n = f'<span style="font-size:14px;font-weight:700;color:var(--text-secondary);padding-right:16px">{count}</span>' if count is not None else ''
    return (f'<div style="display:flex;align-items:center;padding:8px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("back")}</span>'
            f'<span style="flex:1;font-size:22px;line-height:28px;font-weight:700">{title}</span>{n}</div>')


FAB = (f'<span style="position:absolute;right:16px;bottom:16px;width:56px;height:56px;border-radius:16px;background:var(--accent);color:var(--on-accent);'
       f'display:flex;align-items:center;justify-content:center;{D.SHADOW}">{mi("add")}</span>')


# ------------------------------------------------------------------ экраны

def inbox_screen():
    rows = (row('Идея подарка маме', top=False) + row('Посмотреть «Дюну 3»') + row('Разобрать антресоль', bottom=False))
    hint = ('<div style="margin:12px 16px;font-size:13px;line-height:18px;color:var(--text-secondary)">Здесь — то, что ещё не разобрано: без даты и без списка. '
            'Поставьте дату или список — задача уйдёт сама.</div>')
    return frame(f'<div style="padding-top:8px">{rows}{hint}</div>{nav_bar(0, 3)}', H_PHONE)


def lists_screen():
    head = f'<div style="display:flex;align-items:center"><div style="flex:1">{D.screen_title("Списки")}</div>{D.btn_text("+ Папка")}{D.btn_text("+ Список")}</div>'
    line = lambda ic, t, n='', new=False: (f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary);{"background:var(--selected)" if new else ""}">{ic}'
                                           f'<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary)">{t}</span><span style="font-size:14px">{n}</span></div>')
    emoji_inbox = '<span class="list-sq" style="background:var(--inbox-list);width:24px;height:24px">' + svg('ic_list_inbox', 16) + '</span>'
    lsq = lambda c, ic: f'<span style="width:24px;height:24px;border-radius:var(--radius-md);background:{c};color:var(--on-list-square);display:flex;align-items:center;justify-content:center;flex-shrink:0">{svg(ic, 16)}</span>'
    proj = lambda name, c, ic, n: f'<div style="display:flex;align-items:center;padding:12px 16px">{lsq(c, ic)}<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px">{name}</span><span style="font-size:14px;color:var(--text-secondary)">{n}</span></div>'
    body = (head + line(svg('ic_no_date', 24), 'Когда-нибудь', 7, new=True) + line(emoji_inbox, 'Без списка', 2, new=True)
            + line(mi('check'), 'Выполненные') + D.WONT_ROW(3) + line(D.TRASH_ICON(24), 'Корзина', 5)
            + proj('Работа', *WORK, 12) + proj('Дом', *HOME, 6) + proj('Спорт', *SPORT, 3))
    return frame(f'<div style="padding-top:8px">{body}</div>{nav_bar(3, 3)}', H_PHONE)


def group(emoji_list, name, n):
    color, icon = emoji_list
    return (f'<div style="display:flex;align-items:center;gap:8px;padding:12px 16px 4px"><span class="list-sq" style="background:{color}">{svg(icon, 12)}</span>'
            f'<span style="font-size:14px;line-height:20px;font-weight:700">{name}</span><span style="font-size:12px;font-weight:700;color:var(--text-secondary)">{n}</span></div>')


def someday_screen():
    rows = (group(WORK, 'Работа', 3) + row('Обновить портфолио', WORK, top=False) + row('Прочитать «Вдохновлённые»', WORK) + row('Курс по аналитике', WORK, bottom=False)
            + group(HOME, 'Дом', 2) + row('Повесить полку', HOME, top=False) + row('Поменять фильтр воды', HOME, bottom=False)
            + group(SPORT, 'Спорт', 2) + row('Купить кроссовки', SPORT, top=False) + row('Записаться на падел-турнир', SPORT, bottom=False))
    return frame(f'{top_bar("Когда-нибудь", 7)}<div style="padding-top:0">{rows}</div>{FAB}', H_PHONE)


def nolist_screen():
    rows = (day_header('СБ, 07 ноя', 'Сегодня') + row('Купить подарок', left='19:00', top=False, bottom=False)
            + day_header('ПТ, 13 ноя') + row('Забрать посылку', left='', top=False, rem='at', bottom=False))
    return frame(f'{top_bar("Без списка", 2)}{rows}{FAB}', H_PHONE)


def notes():
    items = [
        ('Как задача переходит', 'Во «Входящих» поставили дату → в «Без списка» (и в «Все», «Неделю», виджет). Выбрали список → в «Когда-нибудь». '
         'Поставили и то и другое → в свой список. Убрали дату у задачи без списка → снова во «Входящих». Отдельной кнопки «разобрать» нет.'),
        ('Счётчик', 'На вкладке «Входящие» — число неразобранных (чёрный кружок). Ноль — кружка нет.'),
        ('«+» на экранах', '«Входящие» — новая задача без даты и без списка. «Когда-нибудь» — без даты, список выбирается сразу. «Без списка» — на сегодня, без списка.'),
        ('Повторяющиеся', 'Повтор всегда с датой — во «Входящие» и «Когда-нибудь» не попадает.'),
        ('Данные', 'Не меняются: «без списка» — служебный список «Входящие». Выгрузка и загрузка TickTick — как раньше.'),
    ]
    cards = ''.join(f'<div style="background:var(--surface);border-radius:var(--radius-lg);padding:14px 16px"><b>{t}.</b> {x}</div>' for t, x in items)
    return f'<div style="width:{W_PHONE}px;display:flex;flex-direction:column;gap:12px;font-size:14px;line-height:20px">{cards}</div>'


# ------------------------------------------------------------------ виджет

def widget_inbox():
    _, bottom = D.widget_body(sel='inbox')
    rows = row('Идея подарка маме', top=False) + row('Посмотреть «Дюну 3»') + row('Разобрать антресоль', bottom=False)
    return (f'<div style="position:relative;width:360px;height:420px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;border:1px solid var(--divider);padding:6px 3px 0">'
            f'{rows}{bottom}</div>')


def widget_before():
    _, bottom = D.widget_body(sel='inbox')
    rows = (row('Идея подарка маме', top=False) + row('Обновить портфолио', WORK) + row('Повесить полку', HOME) + row('Посмотреть «Дюну 3»')
            + row('Купить кроссовки', SPORT) + row('Курс по аналитике', WORK, bottom=False))
    return (f'<div style="position:relative;width:360px;height:420px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;border:1px solid var(--divider);padding:6px 3px 0">'
            f'{rows}{bottom}</div>')


def widget_body():
    def labelled(t, sub, inner):
        return f'<div><div style="font-size:17px;font-weight:700">{t}</div><div style="font-size:13px;color:var(--text-secondary);margin:2px 0 10px;width:360px">{sub}</div>{inner}</div>'
    return (f'<div style="display:flex;gap:48px;align-items:flex-start">'
            + labelled('Было', 'Все задачи без даты из всех списков вперемешку.', widget_before())
            + labelled('Стало', 'Только неразобранные — без даты и без списка. Вид строк не меняется.', widget_inbox())
            + '</div>')


# ------------------------------------------------------------------ сборка

BOARDS = [
    ('Main.dc.html', 'Входящие, Когда-нибудь, Без списка', 'Новая логика «Входящих» — итерация 52',
     '«Входящие» — только неразобранное. Задачи со списком без даты — в «Когда-нибудь», без списка с датой — в «Без списка». Переход — сам, по дате и списку.',
     [('1. «Входящие»', 'Только без даты и без списка. На вкладке — счётчик неразобранных.', inbox_screen),
      ('2. «Списки»', 'Две новые строки сверху (подсвечены только в макете): «Когда-нибудь» и «Без списка» с числом задач.', lists_screen),
      ('3. «Когда-нибудь»', 'Задачи со списком, но без даты — группами по спискам. «+» — новая задача без даты со списком.', someday_screen),
      ('4. «Без списка»', 'Задачи без списка, но с датой — лента по дням, как «Все». «+» — задача на сегодня.', nolist_screen)]),
]


# ------------------------------------------------------------------ 0.52.1: разные эмодзи и подпись служебного списка

PIN, THOUGHT, TRAY = '📌', '💭', '📥'


def emoji(e, size=16):
    return B.EMOJI_SPAN.format(px=size - 2, w=size, e=e)


def erow(left, title, e, top=True, bottom=True, rem=None):
    """Строка ленты с эмодзи [e] справа в плашке (у задачи без списка: 📥 без даты, 📌 с датой)."""
    html = frow(left, 'var(--inactive)', title, top=top, bottom=bottom, rem=rem, lsq=False)
    return html[:-len('</div></div>')] + f'<span style="margin-left:8px;display:flex">{emoji(e)}</span></div></div>'


def list_dialog(first, first_emoji):
    opt = lambda t, e, sel=False: D.option(t, sel, emoji(e))
    return D.dlg('Список', opt(first, first_emoji, True) + opt('Работа', '💼') + opt('Дом', '🏠') + opt('Финансы', '💰'))


def feed_with_parents():
    rows = (day_header('ВС, 08 ноя', 'Сегодня') + erow('14:30', 'Стирка', '🏠', top=False) + erow('16:00', 'Уборка', '🏠', rem='at')
            + erow(['18:00', '19:00'], 'Родители', PIN, bottom=False))
    return frame(f'<div style="padding-top:8px">{rows}</div>{nav_bar(1, 3)}', H_PHONE)


def dated_dialog_screen():
    return I.centered(feed_with_parents() + I.SCRIM, list_dialog('Без списка', PIN))


def undated_dialog_screen():
    return I.centered(inbox_screen() + I.SCRIM, list_dialog('Входящие', TRAY))


def lists_labels_screen():
    head = f'<div style="display:flex;align-items:center"><div style="flex:1">{D.screen_title("Списки")}</div>{D.btn_text("+ Папка")}{D.btn_text("+ Список")}</div>'
    line = lambda ic, t, n='': (f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary)">{ic}'
                                f'<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary)">{t}</span><span style="font-size:14px">{n}</span></div>')
    big = lambda e: emoji(e, 24)
    body = (head + line(big(THOUGHT), 'Когда-нибудь', 7) + line(big(PIN), 'Без списка', 2) + line(mi('check'), 'Выполненные')
            + D.WONT_ROW(3) + line(D.TRASH_ICON(24), 'Корзина', 5) + line(big('💼'), 'Работа', 12) + line(big('🏠'), 'Дом', 6))
    return frame(f'<div style="padding-top:8px">{body}</div>{nav_bar(3, 3)}', H_PHONE)


LABEL_BOARD = ('Labels.dc.html', 'Эмодзи и подпись (0.52.1)', 'Эмодзи и подпись служебного списка — 0.52.1',
               'У задачи без списка подпись зависит от даты: без даты — «📥 Входящие», с датой — «📌 Без списка». У «Когда-нибудь» — 💭.',
               [('1. Окно «Список» у задачи с датой', '«Родители» на вс 8 ноя 18:00: первый вариант — «📌 Без списка».', dated_dialog_screen),
                ('2. Окно «Список» у задачи без даты', 'Задача из «Входящих»: первый вариант — «📥 Входящие». Поставили дату — сменится на 📌.', undated_dialog_screen),
                ('3. «Списки»', '«💭 Когда-нибудь» и «📌 Без списка» — эмодзи вместо значков.', lists_labels_screen),
                ('4. Лента', 'У задачи без списка с датой справа 📌 (было 📥) — в ленте, «Неделе», виджете, поиске.', feed_with_parents)])


def build(out):
    os.makedirs(out, exist_ok=True)
    boards, order, y = {}, [], 0
    for name, board_title, title, lead, cols in BOARDS + [LABEL_BOARD]:
        Wd, H, body = I.board(title, lead, cols)
        Path(out, name).write_text(I.page(board_title, Wd, H, body), encoding='utf-8')
        boards[name] = {'x': 0, 'y': y, 'w': Wd, 'h': H, 'title': board_title}
        order.append(name)
        y += H + 120
    Wd, H = 1400, 720
    body = (f'<div style="font-size:28px;font-weight:800;letter-spacing:-.4px">Виджет и правила</div>'
            f'<div style="font-size:15px;color:var(--text-secondary);margin:6px 0 22px;line-height:21px">Вкладка «Входящие» в виджете — по той же логике. Справа — как задачи переходят между разделами.</div>'
            f'<div style="display:flex;gap:48px;align-items:flex-start">{widget_body()}{notes()}</div>')
    Path(out, 'Widget.dc.html').write_text(I.page('Виджет и правила', Wd, H, body), encoding='utf-8')
    boards['Widget.dc.html'] = {'x': 0, 'y': y, 'w': Wd, 'h': H, 'title': 'Виджет «Входящие» и правила'}
    order.append('Widget.dc.html')
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Итерация 52 — Входящие', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': boards, 'order': order,
              'notes': {'head': {'x': 0, 'y': -300, 'text': 'Входящие, Когда-нибудь, Без списка (итерация 52)', 'kind': 'title1', 'maxW': 1900}},
              'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', [(n, b['w'], b['h']) for n, b in boards.items()])


if __name__ == '__main__':
    build(sys.argv[1])
