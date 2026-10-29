"""Макеты итерации 40: массовое редактирование (бэклог №37).

Собирается из компонентов дизайн-системы (`design/system/`). Запуск: `python3 design/mockups/iteration40.py <файл.html>`.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
OUT_FILE = sys.argv[1] if len(sys.argv) > 1 else 'iteration40.html'
from ds_page import B, D, tokens_css, CSS, note  # noqa: E402

svg, mi, frame, frow, day_header, bell = D.svg, D.mi, D.frame, D.frow, D.day_header, D.bell
SHADOW = D.SHADOW


def sel_row(left, color, title, selected=False, done=False, ghost=False, rep=False, rem=None, top=True, bottom=True, list_color='var(--inbox-list)', list_icon='ic_list_inbox', sub=None):
    """Строка шкалы в режиме выбора: кружок → зелёная галочка, плашка подсвечена; будущий повтор бледный и не выбирается."""
    lines = left if isinstance(left, list) else [left]
    wide = len(lines) > 1 or (lines and len(lines[0]) > 6)
    h = 58 if sub else 46
    tcol = 'var(--text-secondary)' if done or ghost else 'var(--text-primary)'
    t = (f'<div style="width:{48 if wide else 38}px;text-align:right;font-size:{11 if wide else 12}px;line-height:{15 if wide else 16}px;font-weight:700;color:{tcol};display:flex;flex-direction:column;justify-content:center;flex-shrink:0">'
         + ''.join(f'<span>{x}</span>' for x in lines) + '</div>')
    if selected:
        node = f'<span style="width:20px;height:20px;border-radius:50%;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("check", 14)}</span>'
    else:
        node = B.node(color, done, 20)
    rail = (f'<div style="width:26px;display:flex;flex-direction:column;align-items:center;flex-shrink:0"><span style="width:1px;flex:1;background:{"var(--divider)" if top else "transparent"}"></span>'
            f'{node}<span style="width:1px;flex:1;background:{"var(--divider)" if bottom else "transparent"}"></span></div>')
    icons = (f'<span style="color:var(--text-secondary);margin-left:4px;display:flex">{svg("ic_tool_repeat", 18)}</span>' if rep else '') + (f'<span style="margin-left:4px;display:flex">{bell(18, rem)}</span>' if rem else '')
    lsq = f'<span class="list-sq" style="background:{list_color};margin-left:8px">{svg(list_icon, 12)}</span>'
    subh = f'<div style="font-size:12px;line-height:14px;font-weight:500;color:var(--text-secondary)">{sub}</div>' if sub else ''
    bg = 'var(--selected)' if selected else 'var(--surface)'
    ring = 'box-shadow:inset 0 0 0 2px var(--accent);' if selected else ''
    plaque = (f'<div style="flex:1;min-width:0;margin:3px 0;border-radius:var(--radius-md);background:{bg};{ring}display:flex;align-items:center;padding:0 12px;opacity:{.5 if ghost else (.7 if done else 1)}">'
              f'<div style="flex:1;min-width:0"><div style="font-size:14px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;{"text-decoration:line-through;color:var(--text-secondary)" if done else ""}">{title}</div>{subh}</div>{icons}{lsq}</div>')
    return f'<div style="display:flex;height:{h}px;padding-right:8px">{t}{rail}{plaque}</div>'


def select_bar(count, all_label='Выбрать все'):
    return (f'<div style="position:absolute;left:0;right:0;top:0;height:60px;background:var(--surface);border-bottom:1px solid var(--divider);display:flex;align-items:center;padding:8px 4px 0 4px;z-index:2">'
            f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center;color:var(--text-primary)">{mi("close", 24)}</span>'
            f'<span style="flex:1;font-size:20px;line-height:28px;font-weight:700">Выбрано: {count}</span>'
            f'<span style="padding:10px 12px;font-size:14px;line-height:20px;font-weight:700;color:var(--accent)">{all_label}</span></div>')


ACTIONS = [('ic_node_check', 'Выполнить'), ('ic_popup_tomorrow', 'На завтра'), ('ic_widget_all', 'На другой день'), ('ic_list_inbox', 'В список'),
           ('ic_tool_priority', 'Приоритет'), ('ic_snack_restore', 'Вернуть'), ('ic_tool_delete', 'Удалить')]


def ribbon(actions=ACTIONS, dim=(), danger=('Удалить', 'Удалить навсегда')):
    btn = lambda ic, t: (f'<span style="flex-shrink:0;min-width:76px;padding:0 8px;box-sizing:border-box;height:56px;border-radius:var(--radius-lg);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;'
                         f'color:{"var(--danger)" if t in danger else "var(--text-primary)"};opacity:{.35 if t in dim else 1}">{svg(ic, 24)}<span style="font-size:11px;line-height:14px;font-weight:700;white-space:nowrap">{t}</span></span>')
    fade = '<span style="position:absolute;right:0;top:0;bottom:0;width:36px;background:linear-gradient(to right,transparent,var(--surface));border-radius:0 var(--radius-nav) var(--radius-nav) 0"></span>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;padding:12px"><div style="position:relative;height:64px;border-radius:var(--radius-nav);background:var(--surface);{SHADOW};overflow:hidden;'
            f'display:flex;align-items:center;gap:2px;padding:0 6px">{"".join(btn(i, t) for i, t in actions)}{fade}</div></div>')


def all_selected():
    rows = (day_header('СР, 28 окт', 'Вчера', True).replace('padding:12px 0 4px 12px', 'padding:70px 0 4px 12px')
            + sel_row('11:00', 'var(--inactive)', 'Отчёт за неделю', selected=True, top=False, bottom=False)
            + day_header('ЧТ, 29 окт', 'Сегодня')
            + sel_row('09:00', 'var(--ring-high)', 'Зарядка', done=True, top=False)
            + sel_row('12:30', 'var(--ring-medium)', 'Созвон с командой', selected=True, rep=True, rem='at')
            + sel_row('18:00', 'var(--ring-low)', 'Оплатить интернет', selected=True, rem='early', bottom=False)
            + day_header('ПТ, 30 окт', 'Завтра')
            + sel_row('10:00', 'var(--inactive)', 'Созвон с командой', ghost=True, rep=True, top=False, bottom=False)
            + sel_row('15:00', 'var(--ring-low)', 'Купить билеты', top=False, bottom=False))
    return frame(f'{select_bar(3)}<div>{rows}</div>{ribbon(dim=("Вернуть",))}', 680)


def flat_selected(done_view=False):
    if done_view:
        rows = (sel_row(['Сегодня', '08:00'], 'var(--ring-high)', 'Зарядка', selected=True, done=True, top=False)
                + sel_row(['Вчера', '11:00'], 'var(--ring-medium)', 'Отчёт за неделю', selected=True, done=True)
                + sel_row(['27 окт'], 'var(--inactive)', 'Купить продукты', done=True, bottom=False))
        bar = select_bar(2)
        rb = ribbon(dim=('Выполнить', 'На завтра', 'На другой день'))
    else:
        rows = (sel_row(['Сегодня', '12:30'], 'var(--ring-medium)', 'Позвонить врачу', selected=True, top=False)
                + sel_row(['16 окт'], 'var(--inactive)', 'Купить продукты', rep=True)
                + sel_row(['18 окт', '10:00'], 'var(--ring-low)', 'Записаться на стрижку', selected=True)
                + sel_row(['—'], 'var(--ring-high)', 'Отчёт за квартал', bottom=False))
        bar = select_bar(2)
        rb = ribbon(dim=('Вернуть',))
    return frame(f'{bar}<div style="padding-top:68px">{rows}</div>{rb}', 680)


def trash_selected():
    rows = (sel_row(['Сегодня', '12:30'], 'var(--ring-medium)', 'Позвонить врачу', selected=True, top=False, sub='удалена 28 окт')
            + sel_row(['16 окт'], 'var(--inactive)', 'Купить продукты', sub='удалена 27 окт')
            + sel_row(['18 окт', '10:00'], 'var(--ring-low)', 'Записаться на стрижку', selected=True, sub='удалена 27 окт', bottom=False))
    return frame(f'{select_bar(2)}<div style="padding-top:68px">{rows}</div>{ribbon([("ic_snack_restore", "Восстановить"), ("ic_tool_delete", "Удалить навсегда")])}', 680)


def normal_with_hint():
    glow = lambda row: row.replace('margin:3px 0;', 'margin:3px 0;box-shadow:0 0 0 3px color-mix(in srgb,var(--accent) 40%,transparent);', 1)
    rows = (day_header('ЧТ, 29 окт', 'Сегодня').replace('padding:12px 0 4px 12px', 'padding:20px 0 4px 12px')
            + frow('09:00', 'var(--ring-high)', 'Зарядка', done=True, top=False)
            + glow(frow('12:30', 'var(--ring-medium)', 'Созвон с командой', rep=True, rem='at'))
            + frow('18:00', 'var(--ring-low)', 'Оплатить интернет', rem='early', bottom=False))
    hint = '<div style="padding:10px 0 0 60px"><span style="font-size:12px;line-height:16px;font-weight:700;color:var(--accent);background:var(--selected);border-radius:var(--radius-md);padding:4px 10px">долгое нажатие на плашку</span></div>'
    return frame(f'<div>{rows}{hint}</div>{D.nav_bar(1)}', 680)


def page():
    mode = f'''
<section><div class="head"><span class="tag">1 · Включение и выбор</span><h2>Долгое нажатие — режим выбора</h2>
<p>Долгое нажатие на плашку включает режим: задача сразу выбрана. Дальше каждое нажатие на плашку или кружок отмечает и снимает. Вверху «Выбрано: N» и «Выбрать все», внизу вместо панели вкладок лента действий.</p></div>
<div class="screens"><div class="screen"><div class="cap">Обычный экран «Все»</div>{normal_with_hint()}{note('Долгое нажатие на плашку. Обычное нажатие, как раньше, открывает задачу.')}</div>
<div class="screen"><div class="cap">Режим выбора на «Все»</div>{all_selected()}{note('Выбранные: зелёная галочка в кружке и рамка на плашке. Выполненная «Зарядка» не выбрана, будущий повтор (бледный) выбирать нельзя. «Вернуть» бледное: среди выбранных нет выполненных. Лента прокручивается пальцем.')}</div></div></section>'''
    flat = f'''
<section><div class="head"><span class="tag">2 · Другие экраны</span><h2>То же во «Входящих», списках, «Поиске» и «Выполненных»</h2>
<p>На «Выполненных» и в «Поиске» среди выбранных бывают выполненные: «Вернуть» работает, а «Выполнить», «На завтра» и «На другой день» бледные.</p></div>
<div class="screens"><div class="screen"><div class="cap">Входящие</div>{flat_selected(False)}</div>
<div class="screen"><div class="cap">Выполненные</div>{flat_selected(True)}</div>
<div class="screen"><div class="cap">Корзина</div>{trash_selected()}{note('В корзине своя лента: «Восстановить» и «Удалить навсегда». Удалить навсегда спрашивает подтверждение.')}</div></div></section>'''
    snacks = f'''
<section><div class="head"><span class="tag">3 · «Отмена»</span><h2>Подсказка после каждого действия</h2>
<p>Режим закрывается, внизу подсказка. «Отмена» возвращает всё, как было: сроки, список, приоритеты, выполнение, удаление.</p></div>
<div class="screens"><div class="screen" style="gap:8px">{D.snackbar('Выполнено: 3', 'ic_node_check')}{D.snackbar('Перенесено на завтра: 3', 'ic_popup_tomorrow')}{D.snackbar('Перенесено на 3 нояб: 3', 'ic_widget_all')}{D.snackbar('Перенесено в «Работа»: 3', 'ic_list_inbox')}
{D.snackbar('Приоритет задан: 3', 'ic_tool_priority')}{D.snackbar('Возвращено в активные: 2', 'ic_snack_restore')}{D.snackbar('Удалено: 3', 'ic_tool_delete')}</div></div></section>'''
    rules = '''
<section><div class="head"><h2>Правила</h2></div><div class="rules">
<div><b>Выбор</b><span>Долгое нажатие включает, нажатие отмечает и снимает. «Выбрать все» выбирает всё выбираемое на экране, кроме будущих повторов. «✕» и «Назад» закрывают режим.</span></div>
<div><b>Действия</b><span>Выполнить (повторяющиеся едут на следующий раз), на завтра, на другой день (календарь, время то же), в список, приоритет, вернуть, удалить. Неподходящие бледные.</span></div>
<div><b>После</b><span>Режим закрывается, подтверждений нет, подсказка «Отмена». Исключение: «Удалить навсегда» в корзине.</span></div>
<div><b>Не затрагивает</b><span>Виджет: долгого нажатия там нет.</span></div>
</div></section>'''
    return (f'<title>Макеты итерации 40</title><meta name="claude:viewport-fit-sides" content="cover">'
            f'<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">'
            f'<style>{tokens_css()}{CSS}</style><main>'
            f'<div class="head"><span class="tag">Макеты · итерация 40</span><h1>Массовое редактирование</h1>'
            f'<p>Режим выбора, лента действий и подсказки в цветах дизайн-системы Simple Tasks. После вашего «ок» они пойдут в код.</p></div>{mode}{flat}{snacks}{rules}</main>')


Path(OUT_FILE).write_text(page(), encoding='utf-8')
print('ok', OUT_FILE)
