"""Макеты итерации 39: «Корзина» (бэклог №35).

Собирается из компонентов дизайн-системы (`design/system/`). Запуск: `python3 design/mockups/iteration39.py <файл.html>`.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
OUT_FILE = sys.argv[1] if len(sys.argv) > 1 else 'iteration39.html'
from ds_page import B, D, tokens_css, CSS, note  # noqa: E402

svg, mi, frame, nav_bar, frow = D.svg, D.mi, D.frame, D.nav_bar, D.frow
TRASH = lambda size=22: svg('ic_tool_delete', size)


def sub(title, when):
    return f'{title}<div style="font-size:12px;line-height:14px;font-weight:500;color:var(--text-secondary)">удалена {when}</div>'


def trash_rows():
    rows = [
        (['Сегодня', '12:30'], 'var(--ring-medium)', 'Позвонить врачу', '28 окт', {'rem': 'early'}),
        (['16 окт'], 'var(--inactive)', 'Купить продукты', '27 окт', {'rep': True}),
        (['18 окт', '10:00'], 'var(--ring-low)', 'Записаться на стрижку', '27 окт', {}),
        (['—'], 'var(--ring-high)', 'Отчёт за квартал', '24 окт', {'list_color': 'var(--list-blue)', 'list_icon': 'ic_list_work'}),
        (['—'], 'var(--inactive)', 'Заказать оборудование', '12 окт', {'list_color': 'var(--list-blue)', 'list_icon': 'ic_list_work'}),
    ]
    out = []
    for i, (left, ring, title, when, kw) in enumerate(rows):
        row = frow(left, ring, sub(title, when), top=i > 0, bottom=i < len(rows) - 1, **kw).replace('height:46px', 'height:58px')
        out.append(row)
    return ''.join(out)


def trash_header(count=5):
    btn = lambda t, c: f'<span style="padding:10px 12px;font-size:14px;line-height:20px;font-weight:700;color:{c}">{t}</span>'
    return (f'<div style="display:flex;align-items:center;padding:8px 4px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("back")}</span>'
            f'<span style="flex:1;font-size:22px;line-height:28px;font-weight:700">Корзина</span></div>'
            f'<div style="display:flex;align-items:center;padding:4px 4px 4px 16px"><span style="flex:1;font-size:12px;line-height:16px;font-weight:700;letter-spacing:.04em;color:var(--text-secondary)">ЗАДАЧ: {count}</span>'
            f'{btn("Восстановить все", "var(--accent)")}{btn("Удалить все", "var(--danger)")}</div>')


def trash_screen():
    return frame(f'{trash_header()}<div style="padding-top:4px">{trash_rows()}</div>', 680)


def empty_trash():
    msg = ('<div style="position:absolute;left:0;right:0;top:260px;text-align:center;padding:0 32px"><div style="font-size:16px;line-height:24px;font-weight:800">Корзина пуста</div>'
           '<div style="font-size:14px;line-height:20px;color:var(--text-secondary);padding-top:4px">Удалённые задачи появятся здесь и будут лежать, пока вы их не вернёте или не сотрёте</div></div>')
    top = ('<div style="display:flex;align-items:center;padding:8px 4px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">' + mi('back') + '</span>'
           '<span style="flex:1;font-size:22px;line-height:28px;font-weight:700">Корзина</span></div>')
    return frame(f'{top}{msg}', 680)


def lists_with_trash():
    lsq = lambda c, ic: f'<span style="width:24px;height:24px;border-radius:var(--radius-md);background:{c};color:var(--on-list-square);display:flex;align-items:center;justify-content:center;flex-shrink:0">{svg(ic, 16)}</span>'
    proj = lambda name, c, ic, n: f'<div style="display:flex;align-items:center;padding:12px 16px">{lsq(c, ic)}<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px">{name}</span><span style="font-size:14px;color:var(--text-secondary)">{n}</span></div>'
    head = f'<div style="display:flex;align-items:center"><div style="flex:1">{D.screen_title("Списки")}</div>{D.btn_text("+ Папка")}{D.btn_text("+ Список")}</div>'
    row = lambda ic, t, n='': (f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary)">{ic}<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary)">{t}</span>'
                              f'<span style="font-size:14px">{n}</span></div>')
    body = (head + row(mi('check'), 'Выполненные') + row(TRASH(24), 'Корзина', 5) + proj('Работа', 'var(--list-blue)', 'ic_list_work', 12)
            + proj('Спорт', 'var(--list-teal)', 'ic_list_sport', '') + proj('Учёба', 'var(--list-purple)', 'ic_list_study', 4))
    return frame(f'<div style="padding-top:8px">{body}</div>{nav_bar(2)}', 680)


def readonly_task(menu=True):
    card = B.sheet_card(True, width=396)
    card = card.replace('<span class="confirm">' + B.CHECK + '</span>', '')
    card = card.replace('class="tool sel"', 'class="tool dim"').replace('class="tool pri"', 'class="tool pri dim"')
    card = card.replace(f'<span class="clr" style="position:static">{B.X}</span>', '').replace(f'<span class="clr">{B.X}</span>', '')
    banner = (f'<div style="margin:0 0 8px;border-radius:var(--radius-lg);background:var(--selected);color:var(--text-primary);display:flex;align-items:center;gap:10px;padding:10px 14px;font-size:14px;line-height:20px;font-weight:600">'
              f'<span style="display:flex;color:var(--text-secondary)">{TRASH(20)}</span><span style="flex:1">В корзине · удалена 28 окт</span></div>')
    popup = ''
    if menu:
        item = lambda t, c='var(--text-primary)': f'<div style="min-height:48px;display:flex;align-items:center;padding:0 20px;font-size:16px;line-height:24px;font-weight:600;color:{c}">{t}</div>'
        popup = ('<div style="position:absolute;right:20px;top:516px;width:240px;border-radius:var(--radius-lg);background:var(--surface);'
                 'box-shadow:0 4px 16px color-mix(in srgb,var(--text-primary) 18%,transparent);border:1px solid var(--divider);overflow:hidden">'
                 + item('Восстановить') + '<div style="height:1px;background:var(--divider)"></div>' + item('Удалить навсегда', 'var(--danger)') + '</div>')
    scrim = '<div style="position:absolute;inset:0;background:var(--scrim)"></div>'
    base = trash_screen()
    return (f'<div style="position:relative;width:412px;height:680px;flex-shrink:0">{base}{scrim}'
            f'<div style="position:absolute;left:8px;right:8px;bottom:16px">{banner}{card}</div>{popup}</div>')


def danger_dialog(title, text, ok):
    d = D.dlg(title, f'<div style="padding:0 24px 8px;font-size:14px;line-height:20px;color:var(--text-secondary)">{text}</div>', ('Отмена', ok))
    return d.replace('background:var(--accent);color:var(--on-accent)', 'background:var(--danger);color:var(--on-accent)')


def page():
    entry = f'''
<section><div class="head"><span class="tag">1 · Вход</span><h2>Строка «Корзина» на экране «Списки»</h2>
<p>Рядом с «Выполненные»: значок корзины, название и число задач внутри. Пока корзина пуста, число не показывается.</p></div>
<div class="screens"><div class="screen"><div class="cap">Списки</div>{lists_with_trash()}</div>
<div class="screen"><div class="cap">Раздел «Корзина»</div>{trash_screen()}{note('Плашки как в обычном списке: слева срок, справа список. Мелкая подпись «удалена 28 окт». Новые удалённые сверху. Вверху «Восстановить все» и «Удалить все».')}</div></div></section>'''
    task = f'''
<section><div class="head"><span class="tag">2 · Задача в корзине</span><h2>Открывается как есть, но править нельзя</h2>
<p>Нажатие на плашку открывает ту же шторку задачи. Сверху плашка «В корзине · удалена…». Кнопки панели бледные, крестиков у полей и галочки «Готово» нет, текст не правится. В «⋮» два пункта.</p></div>
<div class="screens"><div class="screen"><div class="cap">«⋮» открыто</div>{readonly_task(True)}</div>
<div class="screen"><div class="cap">Пустая корзина</div>{empty_trash()}</div></div></section>'''
    dialogs = f'''
<section><div class="head"><span class="tag">3 · Подтверждения и подсказки</span><h2>Спрашиваем только про «навсегда»</h2>
<p>«Удалить навсегда» у одной задачи и «Удалить все» просят подтверждение: кнопка красная. «Восстановить» и «Восстановить все» без вопросов, но с подсказкой «Отмена».</p></div>
<div class="screens"><div class="screen"><div class="cap">Одна задача</div>{danger_dialog('Удалить навсегда?', 'Задача «Позвонить врачу» и её подзадачи будут стёрты. Вернуть их будет нельзя.', 'Удалить')}</div>
<div class="screen"><div class="cap">Все задачи</div>{danger_dialog('Удалить все навсегда?', 'В корзине 5 задач. Все они будут стёрты, вернуть их будет нельзя.', 'Удалить все')}</div>
<div class="screen"><div class="cap">Подсказки</div><div style="display:flex;flex-direction:column;gap:8px">{D.snackbar('Задача восстановлена', 'ic_snack_restore')}{D.snackbar('Восстановлено: 5', 'ic_snack_restore')}</div>{note('Если списка уже нет, задача возвращается во «Входящие». Срок и повтор те же, подзадачи и чек-листы возвращаются вместе.')}</div></div></section>'''
    rules = '''
<section><div class="head"><h2>Правила</h2></div><div class="rules">
<div><b>Что попадает</b><span>Удалённые задачи с подзадачами и чек-листами; в корзине видна родительская задача. Задачи удалённого списка тоже.</span></div>
<div><b>Сколько лежит</b><span>Пока вы не вернёте или не сотрёте сами. Само не удаляется.</span></div>
<div><b>Куда возвращается</b><span>В прежний список, иначе во «Входящие». Срок, повтор, напоминания те же.</span></div>
<div><b>Чего в корзине нет</b><span>Поиска, виджета, напоминаний, выгрузки TickTick и резервной копии.</span></div>
</div></section>'''
    return (f'<title>Макеты итерации 39</title><meta name="claude:viewport-fit-sides" content="cover">'
            f'<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">'
            f'<style>{tokens_css()}{CSS}</style><main>'
            f'<div class="head"><span class="tag">Макеты · итерация 39</span><h1>Корзина</h1>'
            f'<p>Вход, раздел, задача в корзине, подтверждения и подсказки в цветах дизайн-системы Simple Tasks. После вашего «ок» они пойдут в код.</p></div>{entry}{task}{dialogs}{rules}</main>')


Path(OUT_FILE).write_text(page(), encoding='utf-8')
print('ok', OUT_FILE)
