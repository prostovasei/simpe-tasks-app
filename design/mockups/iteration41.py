"""Макеты итерации 41: дата и время в названии (бэклог №29) и подпись «Сегодня» слева от плашки.

Собирается из компонентов дизайн-системы (`design/system/`). Запуск: `python3 design/mockups/iteration41.py <файл.html>`.
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
OUT_FILE = sys.argv[1] if len(sys.argv) > 1 else 'iteration41.html'
from ds_page import B, D, tokens_css, CSS, note  # noqa: E402

svg, mi, frame, frow, day_header, bell = D.svg, D.mi, D.frame, D.frow, D.day_header, D.bell
CAL, CLOCK = svg('ic_widget_all', 22), svg('ic_time', 22)
CLR = f'<span class="clr" style="position:static">{B.X}</span>'
HL = 'background:var(--accent-soft);color:var(--accent);border-radius:4px;padding:0 2px'


def hl(text):
    return f'<span style="{HL}">{text}</span>'


def chip(text, icon=CAL):
    """Чип «поймали дату»: акцентная капсула со значком и крестиком «не разбирать»."""
    return (f'<div style="margin-top:8px"><span style="display:inline-flex;align-items:center;gap:6px;height:32px;padding:0 6px 0 10px;border-radius:16px;background:var(--accent-soft);'
            f'color:var(--accent);font-size:14px;line-height:20px;font-weight:700"><span style="display:flex">{icon.replace("22", "18")}</span>{text}'
            f'<span style="width:20px;height:20px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:color-mix(in srgb,var(--accent) 18%,transparent)">'
            f'<span style="width:12px;height:12px;display:flex">{B.X}</span></span></span></div>')


def sheet(title_html, chip_html='', fields=None, set_date=False, width=396, placeholder=False):
    """Шторка новой задачи: название, чип распознанного срока, поля даты и времени, панель инструментов."""
    handle = '<div style="display:flex;justify-content:center;padding:8px 0 4px"><span style="width:36px;height:4px;border-radius:var(--radius-sm);background:var(--divider)"></span></div>'
    color = 'var(--inactive)' if placeholder else 'var(--text-primary)'
    title = f'<div style="font-size:24px;line-height:32px;font-weight:800;margin-top:10px;color:{color}">{title_html}</div>'
    desc = '<div style="font-size:16px;line-height:24px;font-weight:500;color:var(--inactive);margin-top:4px">Описание</div>'
    if fields:
        fl = ''.join(f'<span class="field">{ic}<span>{t}</span>{CLR}</span>' for ic, t in fields)
    else:
        fl = f'<span class="field empty">{CAL}<span>Без даты и времени</span></span>'
    lsq = '<span class="list-sq" style="background:var(--inbox-list);width:25px;height:25px;border-radius:var(--radius-md)">' + svg('ic_list_inbox', 16) + '</span>'
    dim = '' if set_date else 'dim'
    tools = (B.tool(lsq) + B.tool(svg('ic_tool_priority', 25)) + B.tool(svg('ic_tool_checklist', 25)) + B.tool(svg('ic_tool_repeat', 25), dim)
             + B.tool(bell(25, 'at', 'var(--text-secondary)' if set_date else 'color-mix(in srgb,var(--inactive) 60%,transparent)'), dim))
    return (f'<div style="width:{width}px;border-radius:var(--radius-nav);background:var(--surface);padding:0 16px 14px;box-shadow:0 2px 10px color-mix(in srgb,var(--text-primary) 12%,transparent)">'
            f'{handle}{title}{desc}{chip_html}<div style="display:flex;align-items:center;gap:24px;height:54px;margin-top:8px">{fl}</div>'
            f'<div style="display:flex;gap:8px;align-items:center;margin-top:6px">{tools}<span style="flex:1"></span><span class="confirm">{B.CHECK}</span></div></div>')


def over_screen(card, h=380):
    """Шторка поверх затемнённого экрана."""
    scrim = '<div style="position:absolute;inset:0;background:var(--scrim)"></div>'
    return f'<div style="position:relative;width:412px;height:{h}px;flex-shrink:0;border-radius:28px;overflow:hidden;background:var(--background);border:1px solid var(--divider)">{scrim}<div style="position:absolute;left:8px;bottom:16px">{card}</div></div>'


def switch_row(title, hint, on):
    return (f'<div style="display:flex;align-items:center;gap:12px"><div style="flex:1"><div style="font-size:16px;line-height:24px;font-weight:600">{title}</div>'
            f'<div style="font-size:12px;line-height:16px;color:var(--text-secondary)">{hint}</div></div>{D.switch(on)}</div>')


def settings_card():
    t = '<div style="font-size:16px;line-height:24px;font-weight:600;padding-top:8px">'
    dv = '<div style="height:1px;background:var(--divider);margin:4px 0"></div>'
    body = ('<div style="padding:16px;display:flex;flex-direction:column;gap:12px"><div style="font-size:28px;line-height:36px;font-weight:700">Настройки</div>'
            f'{t}Ввод задач</div>{switch_row("Дата и время в названии", "«Созвон завтра 10:00» — срок ставится сам, фраза уходит из названия.", True)}{dv}'
            f'{t}Напоминания</div>{switch_row("Всплывающая полоска", "Напоминание — зелёной полоской снизу поверх любого приложения.", True)}'
            f'{switch_row("Точное время напоминаний", "Напоминание приходит секунда в секунду.", True)}'
            f'{switch_row("Утренняя сводка", "Каждый день в это время — уведомление с планом на сегодня.", False)}</div>')
    return frame(f'{body}{D.nav_bar(4)}', 560)


# ---------------------------------------------------------------- «Сегодн»

def day_col(lines, w, fs, color='var(--text-primary)', cut=False):
    inner = ''.join(f'<span>{x}</span>' for x in lines)
    clip = 'overflow:hidden;' if cut else ''
    return (f'<div style="width:{w}px;text-align:right;font-size:{fs}px;line-height:{fs + 4}px;font-weight:700;color:{color};display:flex;flex-direction:column;justify-content:center;{clip}white-space:nowrap">{inner}</div>')


def inbox_rows(w, fs, cut=False):
    data = [(['Сегодня', '12:30'], 'var(--ring-medium)', 'Позвонить врачу'), (['Завтра'], 'var(--inactive)', 'Купить продукты'),
            (['Вчера', '11:00'], 'var(--ring-low)', 'Отчёт за неделю'), (['16 окт'], 'var(--ring-high)', 'Записаться на стрижку')]
    out = []
    for i, (left, ring, title) in enumerate(data):
        row = frow(left, ring, title, top=i > 0, bottom=i < len(data) - 1)
        row = re.sub(r'width:\d+px;text-align:right;font-size:\d+px;line-height:\d+px', f'width:{w}px;text-align:right;font-size:{fs}px;line-height:{fs + 4}px', row, count=1)
        if cut:
            row = row.replace('flex-shrink:0">', 'flex-shrink:0;overflow:hidden;white-space:nowrap">', 1)
        out.append(row)
    return ''.join(out)


def inbox_screen(w, fs, title, cut=False):
    top = f'<div style="font-size:28px;line-height:36px;font-weight:700;padding:16px 16px 8px">{title}</div>'
    return frame(f'{top}<div>{inbox_rows(w, fs, cut)}</div>{D.nav_bar(0)}', 420)


def page():
    typing = over_screen(sheet(f'Созвон {hl("завтра в 10:00")}', chip('Завтра, 10:00'), [(CAL, 'пт, 30 окт'), (CLOCK, '10:00')], set_date=True))
    saved = frame(
        day_header('ПТ, 30 окт', 'Завтра') .replace('padding:12px 0 4px 12px', 'padding:20px 0 4px 12px')
        + frow('10:00', 'var(--inactive)', 'Созвон', top=False, bottom=False, lsq=True) + D.nav_bar(1), 260)
    off = over_screen(sheet('Созвон завтра в 10:00', '', None, set_date=False))
    sec1 = f'''
<section><div class="head"><span class="tag">1 · Ввод</span><h2>Фраза подсвечивается, под названием чип</h2>
<p>Пока человек печатает, распознанная фраза в названии подсвечивается акцентным цветом, под ним появляется чип с итоговым сроком, а поля даты и времени уже заполнены. Название очищается от фразы при сохранении.</p></div>
<div class="screens"><div class="screen"><div class="cap">Печатает «Созвон завтра в 10:00»</div>{typing}{note('Подсветка и чип появляются, когда фраза дописана до конца. Поля «дата» и «время» заполнены, повтор и уведомления доступны.')}</div>
<div class="screen"><div class="cap">Сохранил: название без фразы</div>{saved}{note('В списке: «Созвон», завтра, 10:00. Фраза «завтра в 10:00» из названия ушла.')}</div>
<div class="screen"><div class="cap">Нажал ✕ на чипе</div>{off}{note('✕ на чипе: подсветка и чип пропали, фраза остаётся в названии как обычный текст, срок не ставится. Для этой задачи фразы больше не разбираются.')}</div></div></section>'''

    c = lambda title, ph, chip_t, fields: (f'<div class="screen"><div class="cap">{ph}</div>' + over_screen(sheet(title, chip(chip_t), fields, set_date=True), 340) + '</div>')
    sec2 = f'''
<section><div class="head"><span class="tag">2 · Какой срок получится</span><h2>Время без даты, дата без времени, срок уже выбран</h2></div>
<div class="screens">
{c(f"Позвонить маме {hl('в 18:00')}", 'Только время, сейчас 19:00', 'Завтра, 18:00', [(CAL, 'пт, 30 окт'), (CLOCK, '18:00')])}
{c(f"Отчёт {hl('в пятницу')}", 'Только дата: весь день', 'пт, 30 окт', [(CAL, 'пт, 30 окт')])}
{c(f"Купить билеты {hl('15 ноября')}", 'Вкладка «Сегодня» уже поставила срок: побеждает название', '15 ноя', [(CAL, 'вс, 15 ноя')])}
</div></section>'''

    rec = [('«сегодня», «завтра», «послезавтра»', 'Созвон завтра', 'завтра'), ('день недели', 'Отчёт в пятницу · к понедельнику', 'ближайший такой день'),
           ('«через N дней / недель»', 'Позвонить через 3 дня', '+3 дня'), ('дата', 'Билеты 15 ноября · 15.11', '15 ноя'),
           ('время', 'Встреча в 10:00 · в 18 · в 9 утра', '10:00 · 18:00 · 09:00')]
    notrec = [('число без слов', 'Купить 5 яиц'), ('день недели без предлога', 'Вторник — фильм'), ('название месяца в тексте', 'Книга про март')]
    r1 = ''.join(f'<div><b>{a}</b><span>{b} → {d}</span></div>' for a, b, d in rec)
    r2 = ''.join(f'<div><b>{a}</b><span>{b} — не трогаем</span></div>' for a, b in notrec)
    sec3 = f'''
<section><div class="head"><span class="tag">3 · Что распознаём</span><h2>Только явные фразы</h2><p>Правило осторожное: лучше пропустить фразу, чем испортить название. Ошибку всегда можно отключить крестиком на чипе.</p></div>
<div class="rules">{r1}</div><div class="rules">{r2}</div></section>'''

    sec4 = f'''
<section><div class="head"><span class="tag">4 · Настройки</span><h2>Общий выключатель</h2><p>Выключатель «Дата и время в названии» в настройках, по умолчанию включён. Выключен — фразы нигде не разбираются, чипа нет.</p></div>
<div class="screens"><div class="screen">{settings_card()}</div></div></section>'''

    sec5 = f'''
<section><div class="head"><span class="tag">5 · Исправление</span><h2>Подпись «Сегодня» слева от плашки больше не обрезается</h2>
<p>Колонка слева (во «Входящих», списках, «Поиске», «Выполненных») была 48dp, и «Сегодня» упиралось в край. Теперь колонка 56dp и растёт вместе с размером шрифта в системе.</p></div>
<div class="screens"><div class="screen"><div class="cap">Было: колонка 48dp</div>{inbox_screen(48, 11, 'Входящие', cut=True)}{note('На телефоне при системном шрифте «Сегодня» упирается в край колонки и обрезается: «Сегодн». В макете браузера шрифт уже, поэтому влезает впритык.')}</div>
<div class="screen"><div class="cap">Стало: колонка 56dp</div>{inbox_screen(56, 11, 'Входящие')}{note('«Сегодня», «Вчера», «Завтра» и даты вроде «16 окт» помещаются. Плашка чуть уже, названия укорачиваются многоточием, как и раньше.')}</div></div></section>'''

    rules = '''
<section><div class="head"><h2>Правила</h2></div><div class="rules">
<div><b>Где</b><span>Шторка новой задачи и карточка существующей: везде, где вводится название. Импорт TickTick и виджет не затронуты.</span></div>
<div><b>Конфликт</b><span>Если срок уже выбран вручную, а в названии другая дата, побеждает название: чип показывает новый срок.</span></div>
<div><b>Отключение</b><span>✕ на чипе — для этой задачи. Выключатель в настройках — для всех.</span></div>
<div><b>Схема БД</b><span>Не меняется: разбор — только на вводе, сохраняются обычные поля срока.</span></div>
</div></section>'''
    return (f'<title>Макеты итерации 41</title><meta name="claude:viewport-fit-sides" content="cover">'
            f'<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">'
            f'<style>{tokens_css()}{B.COMMON}{CSS}</style><main>'
            f'<div class="head"><span class="tag">Макеты · итерация 41</span><h1>Дата и время в названии</h1>'
            f'<p>Распознавание срока в названии, чип, настройка и исправление подписи «Сегодня». В цветах дизайн-системы Simple Tasks. После вашего «ок» они пойдут в код.</p></div>{sec1}{sec2}{sec3}{sec4}{sec5}{rules}</main>')


Path(OUT_FILE).write_text(page(), encoding='utf-8')
print('ok', OUT_FILE)
