"""Остальные компоненты и экраны дизайн-системы Simple Tasks (итерация 35): окна, элементы списков, виджет, полоска напоминания, экраны.

Подключается из `build_design_system.py`. Размеры и тексты — из Kotlin-кода и `strings.xml` (ссылки в README каждого компонента).
"""
import build_design_system as B
from build_design_system import doc, svg, bell, X, CHECK, tool, LIST_NAMES

# --------------------------------------------------------------- стандартные значки Material (androidx.compose.material.icons)

MAT = {
    'search': 'M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
    'settings': 'M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z',
    'mail': 'M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 14H4V8l8 5 8-5v10zm-8-7L4 6h16l-8 5z',
    'range': 'M9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm2-7h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11z',
    'list': 'M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z',
    'add': 'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z',
    'check': 'M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z',
    'close': 'M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',
    'more': 'M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z',
    'back': 'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
    'down': 'M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z',
    'right': 'M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z',
    'warn': 'M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z',
    'lock': 'M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z',
}


def mi(name, size=24, extra=''):
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" {extra}><path d="{MAT[name]}"/></svg>'


SHADOW = 'box-shadow:0 2px 10px color-mix(in srgb,var(--text-primary) 14%,transparent)'
FONT = 'font-family:var(--font-sans)'


def btn_text(label, color='var(--accent)'):
    return f'<span style="min-height:40px;padding:0 12px;display:inline-flex;align-items:center;font-size:14px;font-weight:600;color:{color}">{label}</span>'


# --------------------------------------------------------------- общие куски

def option(title, sel=False, leading='', sub=None, hint=None, multi=False):
    """Вариант в окне (DialogOption): подсветка и галочка справа у одного из многих, квадрат-галочка слева у нескольких."""
    box = ''
    if multi:
        box = (f'<span style="width:18px;height:18px;border-radius:var(--radius-sm);border:2px solid {"var(--accent)" if sel else "var(--inactive)"};background:{"var(--accent)" if sel else "transparent"};'
               f'color:var(--on-accent);display:flex;align-items:center;justify-content:center;flex-shrink:0">{mi("check", 14) if sel else ""}</span>')
    right = f'<span style="color:var(--accent);display:flex">{mi("check", 24)}</span>' if sel and not multi else ''
    return (f'<div style="min-height:52px;display:flex;align-items:center;gap:12px;padding:4px 24px;background:{"var(--selected)" if sel and not multi else "transparent"}">{box}{leading}'
            f'<div style="flex:1;min-width:0"><div style="font-size:16px;line-height:24px;font-weight:{700 if sel else 600}">{title}</div>'
            f'{f"<div style=font-size:12px;line-height:16px;color:var(--text-secondary)>{sub}</div>" if sub else ""}</div>'
            f'{f"<span style=font-size:12px;font-weight:600;color:var(--text-secondary)>{hint}</span>" if hint else ""}{right}</div>')


def dlg(title, body, buttons=('Отмена', 'Готово'), subtitle=None, icon='', width=340):
    """Окно по центру (SheetDialog): заголовок, содержимое, две кнопки (вторая — зелёная; None — только «Закрыть»)."""
    def b(t, primary):
        return (f'<span style="flex:1;min-height:50px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;font-weight:700;padding:0 8px;text-align:center;'
                f'{"background:var(--accent);color:var(--on-accent)" if primary else "border:1px solid var(--divider);color:var(--text-primary)"}">{t}</span>')
    btns = b(buttons[0], False) + (b(buttons[1], True) if len(buttons) > 1 and buttons[1] else '')
    sub = f'<div style="font-size:14px;line-height:20px;color:var(--text-secondary)">{subtitle}</div>' if subtitle else ''
    ic = f'<div style="margin-bottom:12px">{icon}</div>' if icon else ''
    return (f'<div style="width:{width}px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden;flex-shrink:0">'
            f'<div style="padding:24px 24px 4px">{ic}<div style="font-size:22px;line-height:28px;font-weight:700">{title}</div>{sub}</div>'
            f'<div style="padding:4px 0">{body}</div><div style="height:1px;background:var(--divider)"></div><div style="display:flex;gap:12px;padding:16px">{btns}</div></div>')


def badge(kind='ok'):
    """Круглый значок над заголовком окна обновления: зелёный или красный."""
    if kind == 'err':
        return f'<span style="width:48px;height:48px;border-radius:50%;background:color-mix(in srgb,var(--danger) 12%,transparent);color:var(--danger);display:flex;align-items:center;justify-content:center">{mi("warn", 26)}</span>'
    inner = {'ok': mi('check', 26), 'update': svg('ic_widget_update', 26), 'lock': mi('lock', 26)}[kind]
    return f'<span style="width:48px;height:48px;border-radius:50%;background:var(--selected);color:var(--accent);display:flex;align-items:center;justify-content:center">{inner}</span>'


def progress(pct=None, pad='12px 24px'):
    inner = f'width:{pct}%' if pct is not None else 'width:30%;margin-left:0'
    return f'<div style="padding:{pad}"><div style="height:4px;border-radius:2px;background:var(--divider)"><div style="height:4px;border-radius:2px;background:var(--accent);{inner}"></div></div></div>'


def text_row(t, strong=False, small=False, pad='4px 24px'):
    if strong:
        return f'<div style="padding:{pad};font-size:16px;line-height:24px;font-weight:700">{t}</div>'
    if small:
        return f'<div style="padding:{pad};font-size:12px;line-height:16px;color:var(--text-secondary)">{t}</div>'
    return f'<div style="padding:{pad};font-size:16px;line-height:24px;color:var(--text-secondary)">{t}</div>'


def frame(body, h=640, w=412):
    return f'<div style="position:relative;width:{w}px;height:{h}px;border-radius:28px;background:var(--background);overflow:hidden;border:1px solid var(--divider);flex-shrink:0">{body}</div>'


def nav_bar(sel):
    names = ['mail', 'range', 'list', 'search', 'settings']
    tabs = ''.join(f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center;color:{"var(--accent)" if i == sel else "var(--text-secondary)"}">{mi(n, 24)}</span>' for i, n in enumerate(names))
    add = f'<span style="width:52px;height:52px;border-radius:var(--radius-xl);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("add", 24)}</span>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;padding:12px"><div style="height:64px;border-radius:var(--radius-nav);background:var(--surface);{SHADOW};'
            f'display:flex;align-items:center;justify-content:space-between;padding:0 4px 0 12px">{tabs}{add}</div></div>')


def day_header(text, relative=None, overdue=False, pad='12px 0 4px 12px'):
    color = 'var(--overdue)' if overdue else 'var(--text-primary)'
    rel = f'<span style="font-size:11px;line-height:16px;font-weight:700;color:{color};background:var(--surface);border-radius:var(--radius-sm);padding:1px 8px">{relative}</span>' if relative else ''
    return f'<div style="display:flex;align-items:center;gap:8px;padding:{pad}"><span style="font-size:14px;line-height:20px;font-weight:700;color:{color}">{text}</span>{rel}</div>'


def section_header(text, color='var(--text-secondary)'):
    return f'<div style="padding:16px 16px 4px;font-size:12px;line-height:16px;font-weight:700;letter-spacing:.04em;color:{color}">{text.upper()}</div>'


def screen_title(text):
    return f'<div style="padding:12px 16px;font-size:28px;line-height:36px;font-weight:700">{text}</div>'


def frow(left, color, title, done=False, over=False, period=False, rep=False, rem=None, lsq=True, end=None, top=True, bottom=True, list_color='var(--inbox-list)', list_icon='ic_list_inbox'):
    """Строка шкалы (TimelineTaskRow). left — 1–2 строки слева: [«Сегодня», «10:00»] в плоских списках или «10:00»."""
    lines = left if isinstance(left, list) else [left]
    wide = len(lines) > 1 or (lines and len(lines[0]) > 6)
    h = 64 if period else 46
    tcol = 'var(--overdue)' if over else ('var(--text-secondary)' if done else 'var(--text-primary)')
    fs = 11 if wide else 12
    t = (f'<div style="width:{48 if wide else 38}px;text-align:right;font-size:{fs}px;line-height:{fs + 4}px;font-weight:700;color:{tcol};display:flex;flex-direction:column;justify-content:center;flex-shrink:0">'
         + ''.join(f'<span>{x}</span>' for x in lines) + (f'<span style="color:{"var(--overdue)" if over else "var(--text-secondary)"}">{end}</span>' if end else '') + '</div>')
    nh = 58 if period else 20
    rail = (f'<div style="width:26px;display:flex;flex-direction:column;align-items:center;flex-shrink:0"><span style="width:1px;flex:1;background:{"var(--divider)" if top else "transparent"}"></span>'
            f'{B.node(color, done, nh)}<span style="width:1px;flex:1;background:{"var(--divider)" if bottom else "transparent"}"></span></div>')
    icons = (f'<span style="color:var(--text-secondary);margin-left:4px;display:flex">{svg("ic_tool_repeat", 18)}</span>' if rep else '') + (f'<span style="margin-left:4px;display:flex">{bell(18, rem)}</span>' if rem else '')
    lsqh = f'<span class="list-sq" style="background:{list_color};margin-left:8px">{svg(list_icon, 12)}</span>' if lsq else ''
    plaque = (f'<div style="flex:1;min-width:0;margin:3px 0;border-radius:var(--radius-md);background:var(--surface);display:flex;align-items:center;padding:0 12px;opacity:{.7 if done else 1}">'
              f'<span style="flex:1;min-width:0;font-size:14px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;{"text-decoration:line-through;color:var(--text-secondary)" if done else ""}">{title}</span>{icons}{lsqh}</div>')
    return f'<div style="display:flex;height:{h}px;padding-right:8px">{t}{rail}{plaque}</div>'


def square_check(ring, done=False):
    if done:
        return f'<span style="width:18px;height:18px;border-radius:var(--radius-sm);background:var(--accent);border:2px solid var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("check", 14)}</span>'
    return f'<span style="width:18px;height:18px;border-radius:var(--radius-sm);background:var(--surface);border:2px solid {ring};display:block"></span>'


def old_row(title, ring='var(--inactive)', done=False, sub=None, trailing=None, rep=False, rem=None, mark=None, list_color='var(--inbox-list)'):
    """Плашка TaskRow (поиск, «Выполненные»): квадратная галочка с рамкой цвета приоритета, квадратик списка, название, значки, время справа."""
    t = ''.join(f'<span>{x}</span>' for x in (trailing or []))
    hl = lambda s: s if not mark else s.replace(mark, f'<span style="background:var(--accent-soft);border-radius:2px">{mark}</span>')
    icons = (f'<span style="color:var(--text-secondary);margin-left:4px;display:flex">{svg("ic_tool_repeat", 14)}</span>' if rep else '') + (f'<span style="margin-left:4px;display:flex">{bell(18, rem)}</span>' if rem else '')
    return (f'<div style="padding:0 4px"><div style="display:flex;align-items:center;gap:0;border-radius:var(--radius-md);background:var(--surface);padding:2px 12px 2px 4px;min-height:44px">'
            f'<span style="width:40px;height:40px;display:flex;align-items:center;justify-content:center;flex-shrink:0">{square_check(ring, done)}</span>'
            f'<span class="list-sq" style="background:{list_color};margin-right:8px">{svg("ic_list_inbox", 12)}</span>'
            f'<div style="flex:1;min-width:0"><div style="font-size:14px;line-height:20px;font-weight:600;{"text-decoration:line-through;color:var(--text-secondary)" if done else ""}">{hl(title)}</div>'
            f'{f"<div style=font-size:12px;line-height:16px;color:var(--text-secondary)>{hl(sub)}</div>" if sub else ""}</div>{icons}'
            f'<div style="margin-left:8px;display:flex;flex-direction:column;align-items:center;font-size:12px;line-height:12px;font-weight:700;color:var(--text-primary)">{t}</div></div></div>')


def material_button(label, kind='filled', color=None):
    if kind == 'filled':
        return f'<span style="height:40px;padding:0 24px;border-radius:20px;background:var(--accent);color:var(--on-accent);display:inline-flex;align-items:center;font-size:14px;font-weight:600">{label}</span>'
    c = color or 'var(--accent)'
    return f'<span style="height:40px;padding:0 24px;border-radius:20px;border:1px solid var(--divider);color:{c};display:inline-flex;align-items:center;font-size:14px;font-weight:600">{label}</span>'


def switch(on=True):
    track = 'var(--accent)' if on else 'var(--surface)'
    border = 'var(--accent)' if on else 'var(--inactive)'
    thumb = 'var(--on-accent)' if on else 'var(--inactive)'
    left = 'calc(100% - 28px)' if on else '4px'
    size = 24 if on else 16
    top = 4 if on else 8
    return (f'<span style="position:relative;display:inline-block;width:52px;height:32px;border-radius:16px;background:{track};border:2px solid {border};flex-shrink:0">'
            f'<span style="position:absolute;top:{top - 2}px;left:{left if on else "6px"};width:{size}px;height:{size}px;border-radius:50%;background:{thumb}"></span></span>')


def chip(label, sel=False):
    if sel:
        return f'<span style="height:32px;padding:0 12px;border-radius:var(--radius-md);background:var(--accent);color:var(--on-accent);display:inline-flex;align-items:center;font-size:14px;font-weight:500">{label}</span>'
    return f'<span style="height:32px;padding:0 12px;border-radius:var(--radius-md);background:var(--surface);border:1px solid var(--divider);display:inline-flex;align-items:center;font-size:14px;font-weight:500">{label}</span>'


# --------------------------------------------------------------- превью компонентов


def p_update_dialog():
    d = lambda *a, **k: dlg(*a, **k)
    items = [
        d('Обновление', text_row('Проверяю наличие обновления…') + progress(None), ('Закрыть',)),
        d('Обновлений нет', text_row('Установлена последняя версия') + text_row('0.34.0 (сборка 138)', strong=True), ('Закрыть',), icon=badge('ok')),
        d('Доступно обновление', text_row('Новая версия') + text_row('0.35.0 (сборка 140)', strong=True) + text_row('Сейчас: 0.34.0 (сборка 138) · около 16 МБ', small=True, pad='4px 24px 8px'), ('Закрыть', 'Загрузить'), icon=badge('update')),
        d('Загружаю обновление', text_row('Версия 0.35.0') + progress(45) + f'<div style="display:flex;justify-content:space-between;padding:0 24px">{text_row("7,2 из 16 МБ", small=True, pad="0")}{text_row("45%", small=True, pad="0")}</div>', ('Отмена',)),
        d('Обновление загружено', text_row('Установить версию 0.35.0 сейчас? Приложение перезапустится, задачи и настройки сохранятся.'), ('Не сейчас', 'Обновить'), icon=badge('ok')),
        d('Не удалось проверить', text_row('Проверьте интернет и попробуйте ещё раз.'), ('Закрыть', 'Повторить'), icon=badge('err')),
        d('Нужно разрешение', text_row('Чтобы установить обновление, разрешите приложению установку. Это один раз.'), ('Закрыть', 'Разрешить'), icon=badge('lock')),
        d('Устанавливаю обновление', text_row('Подтвердите установку в окне Android'), ('Закрыть',)),
    ]
    return doc('Окна', 1260, 'Окно обновления (кнопка на виджете): 8 состояний', '<div class="row" style="align-items:flex-start;gap:24px">' + ''.join(items) + '</div>')


def p_list_priority_dialogs():
    lsq = lambda c, ic: f'<span class="list-sq" style="background:{c}">{svg(ic, 12)}</span>'
    lst = dlg('Список', option('Входящие', False, lsq('var(--inbox-list)', 'ic_list_inbox')) + option('Работа', True, lsq('var(--list-blue)', 'ic_list_work')) + option('Дом', False, lsq('var(--list-green)', 'ic_list_home')) + option('Покупки', False, lsq('var(--list-orange)', 'ic_list_shopping')))
    flag = lambda c: f'<span style="color:{c};display:flex">{svg("ic_tool_priority", 24)}</span>'
    pri = dlg('Приоритет', option('P1 Высокий', False, flag('var(--ring-high)')) + option('P2 Средний', True, flag('var(--ring-medium)')) + option('P3 Низкий', False, flag('var(--ring-low)')) + option('Без приоритета', False, flag('var(--inactive)')))
    return doc('Окна', 380, 'Окна «Список» и «Приоритет»: цвета как у кружков задач', f'<div class="row" style="align-items:flex-start;gap:24px">{lst}{pri}</div>')


def stepper(label, val):
    return (f'<div style="display:flex;align-items:center;padding:0 24px;min-height:48px"><span style="flex:1;font-size:16px;line-height:24px">{label}</span>'
            f'<span style="width:48px;text-align:center;font-size:22px;color:var(--accent);font-weight:600">−</span><span style="font-size:16px;font-weight:700;min-width:28px;text-align:center">{val}</span>'
            f'<span style="width:48px;text-align:center;font-size:22px;color:var(--accent);font-weight:600">+</span></div>')


def p_custom_dialogs():
    rem = dlg('Вручную', stepper('Дней до срока', '00') + stepper('Часов', '02') + stepper('Минут', '00') + '<div style="padding:8px 24px;font-size:14px;line-height:20px;font-weight:700;color:var(--accent)">Сработает: чт, 29 окт, 15:00</div>', subtitle='За сколько до срока напомнить')
    days = ''.join(chip(d, d in ('вт', 'чт')) for d in ['пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'])
    rep = dlg('Произвольно', '<div style="padding:0 24px;display:flex;flex-direction:column;gap:12px">'
              f'<div style="display:flex;gap:8px;flex-wrap:wrap">{chip("День")}{chip("Неделя", True)}{chip("Месяц")}{chip("Год")}</div>'
              f'<div style="display:flex;align-items:center;gap:12px"><span style="font-size:16px">Каждые</span>{material_button("−", "outlined", "var(--text-primary)").replace("padding:0 24px", "padding:0 16px")}<span style="font-weight:700">2</span>{material_button("+", "outlined", "var(--text-primary)").replace("padding:0 24px", "padding:0 16px")}</div>'
              f'<div style="display:flex;gap:4px;flex-wrap:wrap">{days}</div><div style="font-weight:700;color:var(--accent)">Каждые 2 недели: вт, чт</div></div>')
    return doc('Окна', 560, 'Окна «Вручную» (уведомление) и «Произвольно» (повтор)', f'<div class="row" style="align-items:flex-start;gap:24px">{rem}{rep}</div>')


def alert(title, body, buttons):
    btn = ''.join(btn_text(t, c) for t, c in buttons)
    return (f'<div style="width:300px;border-radius:28px;background:var(--surface);padding:24px;flex-shrink:0"><div style="font-size:24px;line-height:32px;font-weight:400">{title}</div>'
            f'<div style="padding-top:16px;font-size:14px;line-height:20px;color:var(--text-secondary)">{body}</div><div style="display:flex;justify-content:flex-end;gap:8px;padding-top:24px">{btn}</div></div>')


def p_alert_dialogs():
    field = f'<div style="height:56px;border-radius:4px;border:2px solid var(--accent);display:flex;align-items:center;padding:0 16px;font-size:16px;color:var(--text-primary)">Работа</div>'
    folder = f'<div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:12px">{chip("Без папки", True)}{chip("Дом")}{chip("Учёба")}</div>'
    name = alert('Новый список', field + folder, [('Отмена', 'var(--accent)'), ('Создать', 'var(--accent)')])
    delete = alert('Удалить список «Работа»?', 'Задачи и чек-листы этого списка тоже будут удалены.', [('Отмена', 'var(--accent)'), ('Удалить', 'var(--danger)')])
    sw = ''.join(f'<span style="width:40px;height:40px;border-radius:50%;background:var(--list-{n});border:1px solid var(--divider);display:flex;align-items:center;justify-content:center;color:var(--on-accent)">{mi("check", 24) if n == "blue" else ""}</span>' for n in LIST_NAMES)
    color = alert('Цвет списка', f'<div style="display:flex;gap:12px;flex-wrap:wrap">{sw}</div><div style="padding-top:8px">{btn_text("Без цвета", "var(--accent)")}</div>', [('Отмена', 'var(--accent)')])
    icons = ''.join(f'<span style="width:44px;height:44px;border-radius:var(--radius-md);background:var(--list-blue);border:{"2px solid var(--text-primary)" if i == 1 else "1px solid var(--divider)"};display:flex;align-items:center;justify-content:center;color:var(--on-list-square)">{svg("ic_list_" + n, 24)}</span>'
                    for i, n in enumerate(['home', 'work', 'shopping', 'sport', 'study', 'health', 'money', 'travel']))
    icon = alert('Значок списка', f'<div style="display:flex;gap:12px;flex-wrap:wrap">{icons}</div><div style="padding-top:8px">{btn_text("Без значка", "var(--text-primary)")}</div>', [('Отмена', 'var(--accent)')])
    return doc('Окна', 640, 'Стандартные окна Material 3 с цветами из токенов: имя, удаление, цвет, значок', f'<div class="row" style="align-items:flex-start;gap:24px">{name}{delete}{color}{icon}</div>')


def p_checklist():
    cb = lambda on: (f'<span style="width:40px;height:40px;display:flex;align-items:center;justify-content:center;flex-shrink:0"><span style="width:18px;height:18px;border-radius:2px;border:2px solid {"var(--accent)" if on else "var(--text-secondary)"};background:{"var(--accent)" if on else "transparent"};color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("check", 14) if on else ""}</span></span>')
    row = lambda t, on=False: f'<div style="display:flex;align-items:center"><div style="display:flex">{cb(on)}</div><span style="flex:1;font-size:14px;line-height:20px;{"text-decoration:line-through;color:var(--text-secondary)" if on else ""}">{t}</span><span style="width:40px;height:40px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{mi("more", 24)}</span></div>'
    sub = lambda t, pri: f'<div style="display:flex;align-items:center"><div style="display:flex">{cb(False)}</div><span style="flex:1;font-size:14px;line-height:20px">{t}</span>{pri}</div>'
    badge_p = lambda p, bg, fg: f'<span style="font-size:12px;line-height:16px;font-weight:700;color:var(--priority-{p}-text);background:var(--priority-{p}-bg);border-radius:var(--radius-sm);padding:4px 8px">{fg}</span>'
    body = ('<div style="width:380px;background:var(--surface);border-radius:var(--radius-nav);padding:12px 16px">'
            f'{row("Собрать данные", True)}{row("Сверстать таблицу")}'
            '<div style="padding-left:40px;font-size:14px;line-height:20px;color:var(--inactive);padding-top:4px;padding-bottom:4px">Добавить пункт</div>'
            '<div style="padding:12px 0 4px;font-size:12px;font-weight:700;letter-spacing:.04em;color:var(--text-secondary)">ПОДЗАДАЧИ</div>'
            f'{sub("Запросить цифры у бухгалтерии", badge_p("high", "", "P1"))}{sub("Сверить с прошлым кварталом", "")}</div>')
    return doc('Панель и поля', 330, 'Чек-лист и подзадачи в шторке созданной задачи', body)


def p_chips_fab():
    fab = f'<span style="width:56px;height:56px;border-radius:16px;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center;{SHADOW}">{mi("add", 24)}</span>'
    body = (f'<div class="row" style="align-items:center;gap:28px"><div class="col">{fab}<div class="cap">плавающая «+» на экране списка</div></div>'
            f'<div class="col"><div style="display:flex;gap:8px">{chip("Неделя", True)}{chip("Месяц")}</div><div class="cap">чип: выбран — зелёный, нет — белый с рамкой</div></div>'
            f'<div class="col"><div style="display:flex;gap:12px;align-items:center">{material_button("Выбрать файл .csv")}{material_button("Проверить обновление", "outlined")}{material_button("Очистить все данные", "outlined", "var(--danger)")}</div><div class="cap" style="max-width:none">кнопки Material 3: заливка, контур, опасное действие</div></div></div>')
    return doc('Панель и поля', 150, 'Плавающая «+», чипы и кнопки Material', body)


def p_switch_rows():
    sw = lambda t, h, on: (f'<div style="display:flex;align-items:center;gap:12px"><div style="flex:1"><div style="font-size:16px;line-height:24px;font-weight:600">{t}</div><div style="font-size:12px;line-height:16px;color:var(--text-secondary)">{h}</div></div>{switch(on)}</div>')
    body = ('<div style="width:380px;display:flex;flex-direction:column;gap:12px">'
            '<div style="font-size:16px;font-weight:600;line-height:24px">Напоминания</div>'
            f'{sw("Всплывающая полоска", "Напоминание — зелёной полоской снизу поверх любого приложения.", True)}'
            f'{sw("Точное время напоминаний", "Напоминание приходит секунда в секунду.", False)}'
            '<div style="font-size:14px;line-height:20px;color:var(--overdue)">Нужно разрешение «Появление поверх других приложений».</div>'
            f'<div>{material_button("Разрешить")}</div></div>')
    return doc('Панель и поля', 320, 'Строка настройки с переключателем Material 3', body)


def p_headers():
    body = ('<div style="width:412px;display:flex;flex-direction:column;gap:4px">'
            f'{screen_title("Списки")}{section_header("Найдено: 2")}{day_header("ПТ, 16 окт", "Сегодня")}{day_header("ЧТ, 15 окт", None, True)}'
            '<div style="padding:8px 12px"><div style="display:flex;align-items:center;gap:8px"><span style="font-size:14px;font-weight:700">ПТ, 16 ОКТ</span>'
            '<span style="font-size:12px;font-weight:700;background:var(--surface);border-radius:var(--radius-sm);padding:1px 4px">Сегодня</span>'
            '<span style="flex:1;height:4px;border-radius:var(--radius-sm);background:var(--divider);position:relative"><span style="position:absolute;left:0;top:0;bottom:0;width:43%;border-radius:var(--radius-sm);background:var(--accent)"></span></span>'
            '<span style="font-size:12px;font-weight:500;color:var(--text-secondary)">3 из 7</span></div></div></div>')
    return doc('Список и виджет', 300, 'Заголовок экрана, раздела, дня и день в виджете с прогрессом', body)


def p_empty():
    ghost = lambda w: f'<div style="height:44px;border-radius:var(--radius-md);background:var(--surface);display:flex;align-items:center;gap:12px;padding:0 12px"><span style="width:18px;height:18px;border-radius:var(--radius-sm);border:2px solid var(--divider)"></span><span style="width:{w}%;height:8px;border-radius:4px;background:var(--divider)"></span></div>'
    arrow = '<svg width="64" height="64" viewBox="0 0 60 60" fill="none" stroke="var(--accent)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 6C26 10 40 24 44 46"/><path d="M36 40L44 48L50 38"/></svg>'
    body = ('<div style="width:412px;position:relative;padding:8px 12px 72px;display:flex;flex-direction:column;gap:2px">'
            '<div style="border:2px dashed var(--inactive);border-radius:var(--radius-lg);padding:24px 16px;text-align:center;margin-bottom:4px"><div style="font-size:16px;line-height:24px;font-weight:800">Всё сделано</div>'
            '<div style="font-size:14px;line-height:20px;color:var(--text-secondary)">Нажмите «+» внизу, чтобы добавить задачу</div></div>'
            f'{ghost(70)}{ghost(50)}{ghost(62)}<div style="position:absolute;right:24px;bottom:4px">{arrow}</div></div>')
    return doc('Список и виджет', 360, 'Пустой экран: пунктирная подсказка, силуэты плашек, стрелка к «+»', body)


def p_task_row():
    body = ('<div style="width:412px;display:flex;flex-direction:column;gap:2px">'
            + old_row('Позвонить врачу', 'var(--ring-high)', rem='early', trailing=['Сегодня', '12:30'], list_color='var(--list-red)')
            + old_row('Купить продукты', 'var(--ring-low)', rep=True, trailing=['16 окт'])
            + old_row('Отчёт за квартал', 'var(--inactive)', sub='…для руководства: квартальный', mark=None)
            + old_row('Зарядка', 'var(--ring-medium)', done=True, trailing=['08:00']) + '</div>')
    return doc('Список и виджет', 280, 'Плашка TaskRow (поиск и «Выполненные»): квадратная галочка с рамкой цвета приоритета', body)


def p_list_rows():
    lsq = lambda c, ic: f'<span style="width:24px;height:24px;border-radius:var(--radius-md);background:{c};color:var(--on-list-square);display:flex;align-items:center;justify-content:center;flex-shrink:0">{svg(ic, 16)}</span>'
    folder = ('<div style="display:flex;align-items:center;padding-left:8px"><span style="width:24px;display:flex;color:var(--text-secondary)">' + mi('down') + '</span>'
              '<span style="flex:1;padding-left:8px;font-size:16px;line-height:24px;font-weight:600">Дом</span><span style="font-size:14px;color:var(--text-secondary)">7</span>'
              f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{mi("more")}</span></div>')
    proj = lambda name, c, ic, n, ind=False: (f'<div style="display:flex;align-items:center;padding:12px 16px 12px {32 if ind else 16}px">{lsq(c, ic)}<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px">{name}</span>'
                                              f'<span style="font-size:14px;color:var(--text-secondary)">{n}</span></div>')
    done = f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary)">{mi("check")}<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary)">Выполненные</span></div>'
    body = f'<div style="width:412px;background:var(--background)">{done}{folder}{proj("Ремонт", "var(--list-orange)", "ic_list_home", 3, True)}{proj("Работа", "var(--list-blue)", "ic_list_work", 12)}{proj("Спорт", "var(--list-teal)", "ic_list_sport", "")}</div>'
    return doc('Список и виджет', 280, 'Экран «Списки»: «Выполненные», папка, список со счётчиком и без него', body)


def p_search_field():
    hl = '<span style="background:var(--accent-soft);border-radius:2px">отчёт</span>'
    field = lambda inner, trailing='': (f'<div style="height:56px;border-radius:var(--radius-lg);background:var(--surface);display:flex;align-items:center;padding:0 16px;gap:16px;color:var(--text-secondary)">{mi("search")}'
                                        f'<span style="flex:1;font-size:16px;line-height:24px">{inner}</span>{trailing}</div>')
    body = ('<div style="width:412px;display:flex;flex-direction:column;gap:12px;padding:0 12px">'
            + field('<span style="color:var(--text-secondary)">Поиск задач</span>')
            + field('<span style="color:var(--text-primary)">отчёт</span>', mi('close'))
            + f'<div style="font-size:14px;line-height:20px;color:var(--text-secondary)">Найденное в названии подсвечено: Квартальный {hl}</div></div>')
    return doc('Список и виджет', 240, 'Поле поиска: подсказка и введённый запрос с очисткой', body)


def p_bottom_bar():
    body = '<div style="display:flex;flex-direction:column;gap:12px">' + ''.join(frame(f'<div style="position:absolute;inset:0">{nav_bar(i)}</div>', h=96, w=412) for i in (1, 2)) + '</div>'
    return doc('Список и виджет', 260, 'Нижняя панель приложения: пять вкладок и «+»; выбранная вкладка — зелёная', body)


def strip(title, subtitle, undo=None):
    if undo is not None:
        return (f'<div style="height:68px;border-radius:34px;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;gap:12px;padding:0 8px 0 16px;position:relative;overflow:hidden">'
                f'{svg("ic_node_check", 22)}<span style="flex:1;font-size:16px;line-height:24px;font-weight:700">{undo}</span>'
                f'<span style="font-size:14px;font-weight:700;color:var(--accent);background:var(--surface);border-radius:20px;padding:8px 16px">Отменить</span>'
                f'<span style="position:absolute;left:0;bottom:0;height:3px;width:55%;background:color-mix(in srgb,var(--on-accent) 60%,transparent)"></span></div>')
    return (f'<div style="height:68px;border-radius:34px;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;gap:12px;padding:0 8px 0 16px">{svg("ic_tool_reminder", 22)}'
            f'<div style="flex:1;min-width:0"><div style="font-size:16px;line-height:24px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{title}</div>'
            f'<div style="font-size:11px;line-height:16px;opacity:.8">{subtitle}</div></div>'
            f'<span style="width:48px;height:48px;border-radius:50%;background:color-mix(in srgb,var(--on-accent) 18%,transparent);display:flex;align-items:center;justify-content:center">{svg("ic_popup_tomorrow", 22)}</span>'
            f'<span style="width:48px;height:48px;border-radius:50%;background:var(--surface);color:var(--accent);display:flex;align-items:center;justify-content:center">{svg("ic_node_check", 22)}</span></div>')


def p_reminder_strip():
    more = '<div style="align-self:center;font-size:12px;line-height:16px;font-weight:700;color:var(--text-secondary);background:var(--surface);border-radius:var(--radius-md);padding:4px 8px">ещё 2</div>'
    body = (f'<div style="width:412px;padding:8px 12px;display:flex;flex-direction:column;gap:8px">{more}{strip("Созвон с командой", "Сегодня, 18:00 · Дом")}{strip("Оплатить интернет", "Сегодня, 18:30")}'
            f'{strip("", "", undo="Выполнено")}{strip("", "", undo="Завтра, 18:00")}</div>')
    return doc('Напоминания и виджет', 420, 'Полоска напоминания поверх других приложений: стопка, «ещё N» и «Отменить»', body)


def widget_body(pad_bottom=0):
    title = ('<div style="display:flex;align-items:center;gap:4px;padding:2px 6px 2px"><span style="font-size:14px;font-weight:700;color:var(--overdue)">СР, 14 ОКТ</span>'
             '<span style="font-size:12px;font-weight:700;color:var(--overdue);background:var(--surface);border-radius:var(--radius-sm);padding:1px 4px">Вчера</span></div>')
    title2 = ('<div style="display:flex;align-items:center;gap:4px;padding:6px 6px 2px"><span style="font-size:14px;font-weight:700">ЧТ, 15 ОКТ</span>'
              '<span style="font-size:12px;font-weight:700;background:var(--surface);border-radius:var(--radius-sm);padding:1px 4px">Сегодня</span><span style="width:8px"></span>'
              '<span style="flex:1;height:4px;border-radius:var(--radius-sm);background:var(--divider);position:relative"><span style="position:absolute;left:0;top:0;bottom:0;width:43%;border-radius:var(--radius-sm);background:var(--accent)"></span></span>'
              '<span style="font-size:12px;font-weight:500;color:var(--text-secondary);padding-left:8px">3 из 7</span></div>')
    now = ('<div style="display:flex;align-items:center;height:14px;padding-right:8px"><span style="width:38px;text-align:right;font-size:11px;font-weight:700;color:var(--accent)">14:40</span>'
           '<span style="width:26px;display:flex;justify-content:center"><span style="width:7px;height:7px;border-radius:50%;background:var(--accent)"></span></span><span style="flex:1;height:1px;background:var(--accent)"></span></div>')
    rows = (overdue_header(1, widget=True) + title + frow('11:00', 'var(--inactive)', 'Отчёт за неделю', over=True, top=False, bottom=False, lsq=True) + title2
            + frow('09:00', 'var(--ring-high)', 'Зарядка', done=True, top=False) + frow('12:30', 'var(--ring-medium)', 'Созвон с командой', period=True, end='13:30', rep=True, rem='at') + now
            + frow('18:00', 'var(--ring-low)', 'Оплатить интернет', rem='early', bottom=False))
    bar_sw = lambda ic, sel: f'<span style="width:46px;height:42px;border-radius:var(--radius-md);display:flex;align-items:center;justify-content:center;background:{"var(--selected)" if sel else "var(--surface)"};color:{"var(--accent)" if sel else "var(--text-secondary)"}">{svg(ic, 26)}</span>'
    bottom = ('<div style="position:absolute;left:0;right:0;bottom:0;height:104px;background:linear-gradient(to bottom,transparent,var(--background) 60%)"></div>'
              '<div style="position:absolute;left:0;right:0;bottom:0;height:58px;display:flex;align-items:center;padding:0 6px">'
              f'<span style="display:flex;padding:4px;border-radius:var(--radius-lg);background:var(--surface)">{bar_sw("ic_widget_all", True)}{bar_sw("ic_widget_inbox", False)}</span><span style="flex:1"></span>'
              f'<span style="width:44px;height:44px;border-radius:var(--radius-md);background:var(--surface);color:var(--text-secondary);display:flex;align-items:center;justify-content:center">{svg("ic_widget_update", 22)}</span><span style="width:8px"></span>'
              f'<span style="width:46px;height:46px;border-radius:var(--radius-md);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{svg("ic_widget_add", 24)}</span></div>')
    return rows, bottom


def p_widget_bar():
    rows, bottom = widget_body()
    body = (f'<div style="position:relative;width:360px;height:420px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;border:1px solid var(--divider);padding:6px 3px 0">{rows}{bottom}</div>')
    return doc('Напоминания и виджет', 470, 'Виджет «Задачи»: подпись дня с прогрессом, шкала, «сейчас» и нижняя строка', body)


def p_widget_bar_only():
    _, bottom = widget_body()
    body = f'<div style="position:relative;width:360px;height:70px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;border:1px solid var(--divider)">{bottom.split("</div>", 1)[1]}</div>'
    return doc('Напоминания и виджет', 120, 'Нижняя строка виджета: переключатель «Все» / «Входящие», обновление, «+»', body)


# --------------------------------------------------------------- экраны

def all_screen():
    rows = (overdue_header(1) + day_header('СР, 14 окт', 'Вчера', True) + frow('11:00', 'var(--inactive)', 'Отчёт за неделю', over=True, top=False, bottom=False)
            + day_header('ЧТ, 15 окт', 'Сегодня') + frow('09:00', 'var(--ring-high)', 'Зарядка', done=True, top=False) + frow('12:30', 'var(--ring-medium)', 'Созвон с командой', period=True, end='13:30', rep=True, rem='at')
            + '<div style="display:flex;align-items:center;height:14px;padding-right:8px"><span style="width:38px;text-align:right;font-size:11px;font-weight:700;color:var(--accent)">14:40</span><span style="width:26px;display:flex;justify-content:center"><span style="width:7px;height:7px;border-radius:50%;background:var(--accent)"></span></span><span style="flex:1;height:1px;background:var(--accent)"></span></div>'
            + frow('18:00', 'var(--ring-low)', 'Оплатить интернет', rem='early', bottom=False)
            + day_header('ПТ, 16 окт', 'Завтра') + frow('10:00', 'var(--inactive)', 'Купить билеты', top=False, bottom=False))
    return frame(f'<div style="padding-top:8px">{rows}</div>{nav_bar(1)}', 680)


def inbox_screen():
    rows = (frow(['Сегодня', '12:30'], 'var(--ring-medium)', 'Позвонить врачу', top=False, rem='early') + frow(['16 окт'], 'var(--inactive)', 'Купить продукты', rep=True)
            + frow(['18 окт', '10:00'], 'var(--ring-low)', 'Записаться на стрижку', bottom=False))
    return frame(f'<div style="padding-top:8px">{rows}</div>{nav_bar(0)}', 680)


def lists_screen():
    lsq = lambda c, ic: f'<span style="width:24px;height:24px;border-radius:var(--radius-md);background:{c};color:var(--on-list-square);display:flex;align-items:center;justify-content:center;flex-shrink:0">{svg(ic, 16)}</span>'
    proj = lambda name, c, ic, n, ind=False: (f'<div style="display:flex;align-items:center;padding:12px 16px 12px {32 if ind else 16}px">{lsq(c, ic)}<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px">{name}</span><span style="font-size:14px;color:var(--text-secondary)">{n}</span></div>')
    head = f'<div style="display:flex;align-items:center"><div style="flex:1">{screen_title("Списки")}</div>{btn_text("+ Папка")}{btn_text("+ Список")}</div>'
    done = f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary)">{mi("check")}<span style="flex:1;padding-left:12px;font-size:16px;color:var(--text-primary)">Выполненные</span></div>'
    folder = (f'<div style="display:flex;align-items:center;padding-left:8px;color:var(--text-secondary)">{mi("down")}<span style="flex:1;padding-left:8px;font-size:16px;line-height:24px;font-weight:600;color:var(--text-primary)">Дом</span><span style="font-size:14px">7</span>'
              f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("more")}</span></div>')
    trash = f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary)">{TRASH_ICON(24)}<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary)">Корзина</span><span style="font-size:14px">5</span></div>'
    body = f'{head}{done}{trash}{folder}{proj("Ремонт", "var(--list-orange)", "ic_list_home", 3, True)}{proj("Работа", "var(--list-blue)", "ic_list_work", 12)}{proj("Спорт", "var(--list-teal)", "ic_list_sport", "")}{proj("Учёба", "var(--list-purple)", "ic_list_study", 4)}'
    return frame(f'<div style="padding-top:8px">{body}</div>{nav_bar(2)}', 680)


def project_screen():
    top = (f'<div style="display:flex;align-items:center;padding:8px 4px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("back")}</span>'
           f'<span style="width:24px;height:24px;border-radius:var(--radius-md);background:var(--list-blue);color:var(--on-list-square);display:flex;align-items:center;justify-content:center;margin-right:12px">{svg("ic_list_work", 16)}</span>'
           f'<span style="flex:1;font-size:22px;line-height:28px;font-weight:700">Работа</span><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("more")}</span></div>')
    rows = frow(['Сегодня', '12:30'], 'var(--ring-high)', 'Подготовить отчёт', top=False, lsq=False, rem='at') + frow(['16 окт'], 'var(--ring-medium)', 'Согласовать бюджет', lsq=False, rep=True) + frow(['20 окт'], 'var(--inactive)', 'Заказать оборудование', bottom=False, lsq=False)
    fab = f'<span style="position:absolute;right:16px;bottom:16px;width:56px;height:56px;border-radius:16px;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center;{SHADOW}">{mi("add")}</span>'
    return frame(f'{top}<div style="padding-top:4px">{rows}</div>{fab}', 680)


def search_screen():
    field = (f'<div style="margin:12px;height:56px;border-radius:var(--radius-lg);background:var(--surface);display:flex;align-items:center;padding:0 16px;gap:16px;color:var(--text-secondary)">{mi("search")}'
             f'<span style="flex:1;font-size:16px;color:var(--text-primary)">отчёт</span>{mi("close")}</div>')
    rows = (section_header('Найдено: 3') + '<div style="display:flex;flex-direction:column;gap:2px">'
            + old_row('Подготовить отчёт', 'var(--ring-high)', trailing=['Сегодня', '12:30'], mark='отчёт', list_color='var(--list-blue)')
            + old_row('Согласовать бюджет', 'var(--ring-medium)', sub='…квартальный отчёт и план', mark='отчёт', list_color='var(--list-blue)') + '</div>'
            + section_header('Выполненные') + old_row('Сдать отчёт за сентябрь', 'var(--inactive)', done=True, mark='отчёт', list_color='var(--list-blue)'))
    return frame(f'{field}{rows}{nav_bar(3)}', 680)


def completed_screen():
    top = f'<div style="display:flex;align-items:center;padding:8px 4px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("back")}</span><span style="font-size:22px;line-height:28px;font-weight:700">Выполненные</span></div>'
    rows = (day_header('Сегодня') + '<div style="display:flex;flex-direction:column;gap:2px">' + old_row('Зарядка', done=True, trailing=['08:00']) + old_row('Купить продукты', done=True, list_color='var(--list-green)') + '</div>'
            + day_header('Вчера') + '<div style="display:flex;flex-direction:column;gap:2px">' + old_row('Отчёт за неделю', done=True, trailing=['11:00'], list_color='var(--list-blue)') + '</div>')
    return frame(f'{top}{rows}', 680)


def settings_screen():
    title = '<div style="font-size:16px;line-height:24px;font-weight:600;padding-top:8px">'
    sw = lambda t, h, on: (f'<div style="display:flex;align-items:center;gap:12px"><div style="flex:1"><div style="font-size:16px;line-height:24px;font-weight:600">{t}</div><div style="font-size:12px;line-height:16px;color:var(--text-secondary)">{h}</div></div>{switch(on)}</div>')
    divider = '<div style="height:1px;background:var(--divider);margin:4px 0"></div>'
    body = ('<div style="padding:16px;display:flex;flex-direction:column;gap:12px">'
            '<div style="font-size:28px;line-height:36px;font-weight:700">Настройки</div>'
            f'{title}Напоминания</div>{sw("Всплывающая полоска", "Напоминание — зелёной полоской снизу поверх любого приложения.", True)}'
            f'{sw("Точное время напоминаний", "Напоминание приходит секунда в секунду.", True)}{sw("Утренняя сводка", "Каждый день в это время — уведомление с планом на сегодня.", False)}{divider}'
            f'{title}Обновление</div><div style="font-size:14px;line-height:20px">Установлена последняя версия</div><div>{material_button("Проверить обновление", "outlined")}</div>{divider}'
            f'{title}Импорт из TickTick</div><div style="font-size:14px;line-height:20px;color:var(--text-secondary)">Резервную копию (.csv) можно создать в настройках веб-версии TickTick.</div><div>{material_button("Выбрать файл .csv")}</div>{divider}'
            f'{title}Данные</div><div style="font-size:14px;line-height:20px">Задач: 3399 (активных 145)</div><div>{material_button("Очистить все данные", "outlined", "var(--danger)")}</div>'
            '<div style="font-size:12px;line-height:16px;color:var(--text-secondary);padding-top:12px">Версия 0.34.0 (сборка 138)</div></div>')
    return frame(f'{body}{nav_bar(4)}', 760)


def sheet_screen():
    base = all_screen().replace('height:680px', 'height:680px')
    scrim = '<div style="position:absolute;inset:0;background:var(--scrim)"></div>'
    card = f'<div style="position:absolute;left:8px;bottom:16px">{B.sheet_card(False, width=396)}</div>'
    return f'<div style="position:relative;width:412px;height:680px;flex-shrink:0">{base}{scrim}{card}</div>'


def screens(fn, h, subtitle):
    return doc('Экраны', h, subtitle, f'<div class="row" style="align-items:flex-start">{fn()}</div>')


# ---------------------------------------------------------------- итерации 36–37: «Просрочено», подсказка «Отмена», кнопки уведомления

def overdue_header(count=1, collapsed=False, move=True, widget=False):
    """«Просрочено · N» со стрелкой «свернуть» и значком «перенести на сегодня» (итерация 37)."""
    icon = 20 if widget else 24
    text = f'Просрочено · {count}' if count else 'Просрочено'
    arrow = svg('ic_fold_closed' if collapsed else 'ic_fold_open', icon)
    mv = f'<span style="padding:{4 if widget else 8}px;display:flex">{svg("ic_move_today", icon)}</span>' if move else ''
    pad = '6px 0 2px 6px' if widget else '8px 0 0 12px'
    return (f'<div style="display:flex;align-items:center;padding:{pad};color:var(--overdue)"><span style="display:flex;align-items:center;padding:{0 if widget else 8}px 0">'
            f'<span style="font-size:14px;line-height:20px;font-weight:700">{text}</span><span style="display:flex;padding-left:4px">{arrow}</span></span>'
            f'<span style="width:{8 if widget else 0}px"></span>{mv}</div>')


def p_overdue_header():
    cap = lambda t: f'<div style="font-size:12px;line-height:16px;color:var(--text-secondary);padding:8px 12px 0">{t}</div>'
    body = ('<div style="width:412px;display:flex;flex-direction:column">'
            f'{cap("Приложение, раскрыто")}{overdue_header(3)}{day_header("ВТ, 13 окт", None, True)}'
            f'{cap("Свёрнуто: прошедшие дни скрыты, счётчик остаётся")}{overdue_header(3, collapsed=True)}'
            f'{cap("Все просроченные уже выполнены: значка переноса нет")}{overdue_header(0, move=False)}'
            f'{cap("Виджет: 14 sp, значки 20")}{overdue_header(3, widget=True)}</div>')
    return doc('Список и виджет', 470, 'Подпись «Просрочено · N»: стрелка «свернуть» и значок «перенести на сегодня»', body)


def snackbar(text, kind='ic_node_check'):
    """Подсказка «Отмена» (итерация 38): белая полупрозрачная карточка (88%), значок слева, зелёная «Отмена»."""
    return (f'<div style="width:380px;min-height:56px;border-radius:var(--radius-xl);background:color-mix(in srgb,var(--surface) 88%,transparent);border:1px solid var(--divider);'
            f'color:var(--text-primary);display:flex;align-items:center;padding:0 4px 0 16px;gap:12px">'
            f'<span style="display:flex;color:var(--text-secondary)">{svg(kind, 22)}</span>'
            f'<span style="flex:1;font-size:14px;line-height:20px;font-weight:600;padding:8px 0">{text}</span>'
            f'<span style="min-height:40px;padding:0 12px;display:flex;align-items:center;font-size:14px;line-height:20px;font-weight:700;color:var(--accent)">Отмена</span></div>')


def p_snackbar():
    rows = [('Задача выполнена', 'ic_node_check'), ('Следующий повтор: сб, 17 окт', 'ic_tool_repeat'), ('Задача удалена', 'ic_tool_delete'),
            ('Перенесено на сегодня: 3', 'ic_move_today'), ('Задача возвращена', 'ic_snack_restore')]
    body = '<div style="width:412px;padding:12px;display:flex;flex-direction:column;gap:8px">' + ''.join(snackbar(t, k) for t, k in rows) + '</div>'
    return doc('Список и виджет', 340, 'Подсказка внизу экрана с кнопкой «Отмена»: белая полупрозрачная карточка со значком', body)


def recent_row(text):
    return ('<div style="min-height:48px;display:flex;align-items:center;padding:0 4px 0 16px;gap:16px;color:var(--text-secondary)">'
            f'<span style="display:flex">{svg("ic_history", 22)}</span><span style="flex:1;font-size:16px;line-height:24px;font-weight:500;color:var(--text-primary)">{text}</span>'
            f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("close", 20)}</span></div>')


def p_recent_searches():
    head = ('<div style="display:flex;align-items:center;padding:16px 16px 4px"><span style="flex:1;font-size:12px;line-height:16px;font-weight:700;letter-spacing:.04em;color:var(--text-secondary)">НЕДАВНИЕ</span>'
            '<span style="font-size:14px;line-height:20px;font-weight:700;color:var(--accent);padding:8px 0">Очистить</span></div>')
    body = '<div style="width:412px">' + head + ''.join(recent_row(t) for t in ['отчёт', 'купить продукты', 'бюджет', 'врач', 'зарядка']) + '</div>'
    return doc('Список и виджет', 360, 'Недавние запросы в поиске: до 5 строк, нажатие ищет, крестик убирает, «Очистить» стирает все', body)


def draft_sheet():
    card = B.sheet_card(False, width=396)
    sel = '<span style="background:var(--accent-soft);border-radius:2px;padding:0 1px">Купить билеты на конц</span>'
    card = card.replace('color:var(--inactive)">Купить билеты на концерт', f'">{sel}')
    card = card.replace('font-weight:500;color:var(--inactive);margin-top:4px">Описание', 'font-weight:500;color:var(--text-secondary);margin-top:4px">Нужно две штуки, ближе к сцене')
    return card


def p_draft():
    body = f'<div style="width:412px;padding:8px 8px 0">{draft_sheet()}</div>'
    return doc('Окна', 300, 'Шторка с подставленным черновиком: название выделено, описание на месте', body)


def notif_btn(label):
    return f'<span style="min-height:36px;padding:0 12px;display:flex;align-items:center;font-size:14px;font-weight:600;color:var(--accent)">{label}</span>'


def p_notification():
    body = ('<div style="width:412px;padding:12px"><div style="border-radius:var(--radius-xl);background:var(--surface);padding:12px 16px;border:1px solid var(--divider)">'
            '<div style="display:flex;align-items:center;gap:8px;font-size:12px;line-height:16px;color:var(--text-secondary)"><span style="color:var(--accent);display:flex">' + svg('ic_notification', 16) + '</span>Simple Tasks · Напоминания · сейчас</div>'
            '<div style="font-size:16px;line-height:24px;font-weight:700;padding-top:4px">Созвон с командой</div>'
            '<div style="font-size:14px;line-height:20px;color:var(--text-secondary)">Сегодня, 18:00 · Дом</div>'
            f'<div style="display:flex;gap:4px;padding-top:8px;margin-left:-12px">{notif_btn("Выполнить")}{notif_btn("На завтра")}</div></div></div>')
    return doc('Напоминания и виджет', 190, 'Обычное уведомление-напоминание с кнопками «Выполнить» и «На завтра»', body)


# ---------------------------------------------------------------- итерация 39: «Корзина»

TRASH_ICON = lambda size=22: svg('ic_tool_delete', size)


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
    head = f'<div style="display:flex;align-items:center"><div style="flex:1">{screen_title("Списки")}</div>{btn_text("+ Папка")}{btn_text("+ Список")}</div>'
    row = lambda ic, t, n='': (f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary)">{ic}<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary)">{t}</span>'
                              f'<span style="font-size:14px">{n}</span></div>')
    body = (head + row(mi('check'), 'Выполненные') + row(TRASH_ICON(24), 'Корзина', 5) + proj('Работа', 'var(--list-blue)', 'ic_list_work', 12)
            + proj('Спорт', 'var(--list-teal)', 'ic_list_sport', '') + proj('Учёба', 'var(--list-purple)', 'ic_list_study', 4))
    return frame(f'<div style="padding-top:8px">{body}</div>{nav_bar(2)}', 680)


def readonly_task(menu=True):
    card = B.sheet_card(True, width=396)
    card = card.replace('<span class="confirm">' + B.CHECK + '</span>', '')
    card = card.replace('class="tool sel"', 'class="tool dim"').replace('class="tool pri"', 'class="tool pri dim"')
    card = card.replace(f'<span class="clr" style="position:static">{B.X}</span>', '').replace(f'<span class="clr">{B.X}</span>', '')
    banner = (f'<div style="margin:0 0 8px;border-radius:var(--radius-lg);background:var(--selected);color:var(--text-primary);display:flex;align-items:center;gap:10px;padding:10px 14px;font-size:14px;line-height:20px;font-weight:600">'
              f'<span style="display:flex;color:var(--text-secondary)">{TRASH_ICON(20)}</span><span style="flex:1">В корзине · удалена 28 окт</span></div>')
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


def p_danger():
    body = f'<div class="row" style="align-items:flex-start">{danger_dialog("Удалить навсегда?", "Задача «Позвонить врачу» и её подзадачи будут стёрты. Вернуть их будет нельзя.", "Удалить")}{danger_dialog("Удалить все навсегда?", "Задач в корзине: 5. Все они будут стёрты, вернуть их будет нельзя.", "Удалить все")}</div>'
    return doc('Окна', 300, 'Окна необратимых действий: красная кнопка', body)


def danger_dialog(title, text, ok):
    d = dlg(title, f'<div style="padding:0 24px 8px;font-size:14px;line-height:20px;color:var(--text-secondary)">{text}</div>', ('Отмена', ok))
    return d.replace('background:var(--accent);color:var(--on-accent)', 'background:var(--danger);color:var(--on-accent)')




# ---------------------------------------------------------------- итерация 40: массовое редактирование

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



def p_selection():
    body = ('<div style="width:412px;display:flex;flex-direction:column;gap:12px"><div style="position:relative;height:64px">' + select_bar(3) + '</div>'
            + '<div style="position:relative;height:88px">' + ribbon(dim=("Вернуть",)) + '</div>'
            + '<div style="position:relative;height:88px">' + ribbon([("ic_snack_restore", "Восстановить"), ("ic_tool_delete", "Удалить навсегда")]) + '</div>'
            + sel_row('12:30', 'var(--ring-medium)', 'Выбранная задача', selected=True, top=False) + sel_row('15:00', 'var(--ring-low)', 'Невыбранная задача', bottom=False) + '</div>')
    return doc('Список и виджет', 380, 'Режим выбора: «Выбрано: N», лента действий, выбранная и невыбранная плашка', body)


PREVIEWS = {
    'UpdateDialog': p_update_dialog, 'ListPriorityDialogs': p_list_priority_dialogs, 'CustomDialogs': p_custom_dialogs, 'AlertDialogs': p_alert_dialogs,
    'ChecklistRows': p_checklist, 'ChipsAndButtons': p_chips_fab, 'SettingsRow': p_switch_rows,
    'Headers': p_headers, 'EmptyTasks': p_empty, 'TaskRow': p_task_row, 'ListRows': p_list_rows, 'SearchField': p_search_field, 'BottomBar': p_bottom_bar,
    'ReminderStrip': p_reminder_strip, 'SelectionMode': p_selection, 'ScreenSelection': lambda: screens(all_selected, 730, 'Экран «Все» в режиме выбора: сверху «Выбрано: N», снизу лента действий'), 'ScreenTrash': lambda: screens(trash_screen, 730, 'Экран «Корзина»: удалённые задачи как в списке, «Восстановить все» и «Удалить все»'), 'TrashTask': lambda: doc('Окна', 730, 'Задача в корзине: только чтение, «⋮» — восстановить или удалить навсегда', f'<div class="row" style="align-items:flex-start">{readonly_task(True)}</div>'), 'DangerDialog': p_danger, 'OverdueHeader': p_overdue_header, 'UndoSnackbar': p_snackbar, 'RecentSearches': p_recent_searches, 'TaskDraft': p_draft, 'ReminderNotification': p_notification, 'Widget': p_widget_bar, 'WidgetBar': p_widget_bar_only,
    'ScreenAll': lambda: screens(all_screen, 730, 'Экран «Все»: лента по дням со шкалой и нижней панелью'),
    'ScreenInbox': lambda: screens(inbox_screen, 730, 'Экран «Входящие»: плоский список, слева день и время'),
    'ScreenLists': lambda: screens(lists_screen, 730, 'Экран «Списки»'),
    'ScreenProject': lambda: screens(project_screen, 730, 'Экран списка: заголовок со значком, «⋮» и плавающая «+»'),
    'ScreenSearch': lambda: screens(search_screen, 730, 'Экран «Поиск»: запрос, найденное, выполненные'),
    'ScreenCompleted': lambda: screens(completed_screen, 730, 'Экран «Выполненные»: по дням, новые сверху'),
    'ScreenSettings': lambda: screens(settings_screen, 810, 'Экран «Настройки»'),
    'ScreenNewTask': lambda: screens(sheet_screen, 730, 'Окно создания задачи поверх экрана «Все»'),
}

README_MORE = {
    'SelectionMode': ('Режим выбора: верхняя строка «Выбрано: N», лента действий, выбранная плашка (0.40).', 'Включается долгим нажатием на плашку (будущий повтор выбрать нельзя), дальше нажатие на плашку или кружок отмечает и снимает. Сверху строка `surface` высотой `size-selection-bar` (56): «✕» (закрыть режим, как и «Назад»), «Выбрано: N» (`title-large` Bold), «Выбрать все» (`accent`). Внизу на месте панели вкладок лента: высота `size-nav-bar`, радиус `radius-nav`, тень; кнопки не уже `size-ribbon-button` (76) и высотой `size-ribbon-button-height` (56): значок 24 и подпись `label-small` Bold; ряд прокручивается пальцем, у правого края затухание. Действия: «Выполнить», «На завтра», «На другой день» (календарь), «В список», «Приоритет», «Вернуть», «Удалить» (`danger`); неподходящие бледные (35%). В корзине: «Восстановить» и «Удалить навсегда» (с подтверждением `DangerDialog`). Выбранная плашка: фон `selected`, рамка `accent` 2, в кружке зелёная галочка. После действия режим закрывается, внизу `UndoSnackbar` («Выполнено: 3 · Отмена» и т. п.). Источник: `designsystem/components/Selection.kt`, `ui/feature/bulk/SelectionFrame.kt`, `BulkActions.kt`.'),
    'ScreenSelection': ('Экран «Все» в режиме выбора (0.40).', 'Строка «Выбрано: N» поверх верха экрана, лента действий на месте нижней панели («+» и вкладки на это время скрыты). Список прокручивается выше ленты. То же во «Входящих», списках, «Поиске», «Выполненных» и «Корзине»; в виджете режима нет.'),
    'ScreenTrash': ('Экран «Корзина» (0.39).', 'Сверху «назад» и название `title-large` Bold; под ним «ЗАДАЧ: N» (`label-medium` Bold, `text-secondary`), справа «Восстановить все» (`accent`) и «Удалить все» (`danger`). Ниже плашки как в обычном списке (слева срок, справа список) с мелкой подписью «удалена 28 окт» (`body-small`, `text-secondary`); строка выше обычной на `size-trash-row-extra` (14). Новые удалённые сверху. Нажатие открывает задачу только для чтения (`TrashTask`); кружок ничего не отмечает. Пусто: «Корзина пуста» и подсказка по центру. Вход — строка «Корзина» на экране «Списки» под «Выполненными»: значок корзины, название, число задач (при нуле числа нет). Источник: `ui/feature/trash/TrashScreen.kt`.'),
    'TrashTask': ('Задача в корзине: та же шторка, но только для чтения (0.39).', 'Над шторкой плашка `selected` «В корзине · удалена 28 окт» со значком корзины. Название, описание, срок, список, значки повтора и уведомлений, чек-лист и подзадачи показываются как есть, править нельзя: кнопок панели, крестиков у полей и галочки «Готово» нет. «⋮» справа от названия: «Восстановить» и «Удалить навсегда» (`danger`, после подтверждения `DangerDialog`). Затемнение и «Назад» закрывают. Восстановленная задача возвращается в прежний список (если его нет — во «Входящие») вместе с подзадачами и чек-листами, подсказка `UndoSnackbar` «Задача восстановлена · Отмена». Источник: `ui/feature/trash/TrashTaskScreen.kt`.'),
    'DangerDialog': ('Окно необратимого действия: красная кнопка (0.39).', 'Строится на `SheetDialog` с `danger = true`: вторая кнопка залита `danger` вместо `accent`. «Удалить навсегда?» у одной задачи из корзины и «Удалить все навсегда?» у всей корзины; первая кнопка «Отмена». «Восстановить» и «Восстановить все» подтверждения не просят, но дают подсказку «Отмена».'),
    'OverdueHeader': ('Подпись «Просрочено · N» над прошедшими днями: стрелка «свернуть» и значок «перенести на сегодня» (0.37).',
                      '''Стоит над первым прошедшим днём в ленте «Все» приложения и на вкладке «Все» виджета; во «Входящих» и списках её нет. Текст `label-large` Bold (в виджете 14 sp), цвет `overdue`; N — невыполненные задачи прошедших дней (сегодняшние с прошедшим временем не входят).
Справа от текста стрелка 24 (в виджете `size-widget-fold-icon` 20): вниз — раскрыто, вправо — свёрнуто. Нажатие на текст со стрелкой сворачивает все прошедшие дни, счётчик остаётся. Состояние одно на приложение и виджет, запоминается между запусками.
Рядом значок «→|» (`ic_move_today`): переносит все невыполненные просроченные на сегодня, время дня то же. Если невыполненных нет, значка нет. После переноса — подсказка `UndoSnackbar`; из виджета она показывается в прозрачном окне поверх рабочего стола (~4 с).
Источник: `designsystem/components/Common.kt` (`OverdueHeader`), `widget/TasksWidget.kt` (`OverdueTitle`).'''),
    'UndoSnackbar': ('Подсказка внизу экрана с кнопкой «Отмена»: белая полупрозрачная карточка со значком (0.38).',
                     '''Белая карточка `surface` на 88% (сквозь неё виден список или рабочий стол), рамка `divider` 1 (тени нет: сквозь полупрозрачную карточку она просвечивает), радиус `radius-xl` (16), высота от `size-snackbar-min-height` (56), поля слева `space-md`, справа `space-xxs`.
Слева значок `size-snackbar-icon` (22) цвета `text-secondary`: галочка («Задача выполнена»), повтор («Следующий повтор: сб, 17 окт»), корзина («Задача удалена»), стрелка к черте («Перенесено на сегодня: N»), круговая стрелка («Задача возвращена»). Справа зелёная «Отмена» (`accent`, Bold) с зоной нажатия от 40.
Размытия фона нет: на старых телефонах Android оно недоступно. Висит ~4 с, новая заменяет прежнюю. В приложении стоит над нижней панелью с зазором `space-sm`; из виджета и уведомлений появляется в прозрачном окне поверх рабочего стола с отступом от низа `space-lg`; нажатие мимо закрывает окно сразу (отмена тогда уже невозможна).
Тексты: «Задача удалена» без вопроса «Удалить?» (0.36), «Перенесено на сегодня: N» (0.37). «Отмена» возвращает всё как было (удалённое — вместе с подзадачами и чек-листами). Источник: `designsystem/components/UndoSnackbar.kt`, `ui/common/TaskListUi.kt` (`showUndo`).'''),
    'RecentSearches': ('Недавние запросы в поиске: пока поле пустое — до 5 строк (0.38).',
                       '''Блок виден, пока поле поиска пустое и есть запросы; иначе — подсказка «Начните вводить — задачи появятся сразу». Заголовок «НЕДАВНИЕ» (`label-medium` Bold, `text-secondary`), справа «Очистить» (`accent`, Bold) стирает все.
Строка высотой `size-recent-row` (48): слева значок «часы» `size-recent-icon` (22) цвета `text-secondary`, запрос `body-large` Medium, справа крестик в зоне нажатия 48 убирает один запрос. Нажатие на строку ставит запрос в поле и сразу ищет.
Запрос запоминается, когда открыли найденную задачу или нажали «поиск» на клавиатуре (не на каждую букву). Последние 5, новые сверху, без повторов (регистр не важен), хранятся на телефоне. Источник: `ui/feature/search/SearchScreen.kt`, `RecentSearches.kt`.'''),
    'TaskDraft': ('Черновик новой задачи: закрыли карточку, не сохранив, — текст вернётся при следующем «+» (0.38).',
                  '''Если карточку новой задачи закрыли без сохранения (нажатием мимо, «Назад», смахнули окно), а в названии что-то набрано, название и описание запоминаются. Следующее «+» (с любого экрана и из виджета) открывает шторку с этим текстом: название выделено (`accent-soft`), любая буква заменит его, «Удалить» на клавиатуре сотрёт.
Дата, список, приоритет, повтор и чек-лист не запоминаются. Черновик один, без срока годности; стирается, когда задачу сохранили или название очистили. Источник: `ui/feature/task/TaskDraft.kt`, `QuickAddViewModel.kt`.'''),
    'ReminderNotification': ('Обычное уведомление-напоминание: название, срок, кнопки «Выполнить» и «На завтра».',
                             '''Системное уведомление Android (канал «Напоминания»). Название задачи, под ним срок и список. Две кнопки: «Выполнить» и «На завтра» (0.36): срок переносится на завтра, время дня то же; у задачи без времени — только день.
Рисует система, поэтому вид зависит от телефона; значок приложения — `ic_notification`. Источник: `notifications/AndroidReminders.kt`.'''),
    'UpdateDialog': ('Окно обновления поверх рабочего стола: проверка, загрузка, установка, ошибки.',
                     '''Строится на `SheetDialog`. Над заголовком круглый значок `size-dialog-badge` (48): зелёный (`selected`, значок `accent`) для хороших исходов, красный (`danger` 12%) для ошибок.
Состояния: проверка (полоса без процента), «Обновлений нет», «Доступно обновление» («Закрыть» / «Загрузить», версия и размер), загрузка (полоса, «7,2 из 16 МБ», процент, одна кнопка «Отмена»),
«Обновление загружено» («Не сейчас» / «Обновить»), ошибки «Не удалось проверить» и «Не удалось загрузить» («Закрыть» / «Повторить»), «Нужно разрешение» («Закрыть» / «Разрешить»), установка.
Во время загрузки «Назад» и нажатие мимо окна игнорируются: останавливает только «Отмена». Полоса: высота 4, дорожка `divider`, заливка `accent`. Источник: `ui/feature/update/UpdateDialog.kt`.'''),
    'ListPriorityDialogs': ('Окна «Список» и «Приоритет»: один вариант, подсветка выбранного и галочка справа.',
                            '''Окно «Список»: слева квадратик цвета списка со значком (16), у «Входящие» цвет `inbox-list`. Окно «Приоритет»: флажок 24 цвета приоритета (`ring-*`), «Без приоритета» — `inactive`.
Строка не ниже `size-dialog-row` (52), выбранная залита `selected`. Изменение применяется только по «Готово». Источник: `TaskFieldMenus.kt`.'''),
    'CustomDialogs': ('Окна «Вручную» (своё уведомление) и «Произвольно» (свой повтор) с счётчиками.',
                      '''«Вручную»: три строки «Дней до срока / Часов / Минут» (у задачи на весь день: за сколько дней, час, минуты) со счётчиком «− 02 +»; снизу зелёным «Сработает: чт, 29 окт, 15:00».
Минуты идут шагом 5, дни до 30. «Произвольно»: чипы «День / Неделя / Месяц / Год», строка «Каждые − N +» (1–99), у недели ещё чипы дней недели, внизу зелёная расшифровка («Каждые 2 недели: вт, чт»).
Кнопки «Отмена» и «Готово», как у всех окон. Источник: `TaskFieldMenus.kt`, `RepeatDialog.kt`.'''),
    'AlertDialogs': ('Стандартные окна Material 3: имя, удаление, цвет и значок списка.',
                     '''Это `AlertDialog` Material 3 с цветами из токенов: фон `surface`, заголовок `text-primary`, текст `text-secondary`, кнопки текстовые (`accent`, удаление — `danger`).
Имя — поле с зелёной рамкой при вводе (`accent`), у нового списка ещё чипы «Без папки / папка». Цвет — 9 кружков `size-color-swatch` (40) с галочкой у выбранного и «Без цвета».
Значок — плитки `size-icon-tile` (44) цвета списка с белым значком, выбранная в рамке `text-primary`, и «Без значка». Эти окна старого вида; новые окна делай через `SheetDialog`.'''),
    'ChecklistRows': ('Чек-лист и подзадачи в шторке созданной задачи.',
                      '''Пункт: галочка с зоной нажатия `size-checkbox-touch` (40), текст `body-medium`, справа «⋮» (выше, ниже, переименовать, удалить). Отмеченный — зелёная галочка, текст зачёркнут `text-secondary`.
Под пунктами поле «Добавить пункт» (подсказка `inactive`, отступ слева 40). Подзадачи из TickTick — под подписью «ПОДЗАДАЧИ», с плашкой приоритета, если он есть; нажатие открывает подзадачу.'''),
    'ChipsAndButtons': ('Плавающая «+», чипы и кнопки Material 3.',
                        '''Плавающая «+» на экранах списка и «Входящие»: 56, радиус 16, `accent`, значок `on-accent`. Чип: высота 32, радиус `radius-md`; выбранный залит `accent`, текст `on-accent`; невыбранный белый с рамкой `divider`.
Кнопки: заливка `accent` (главное действие), контур `divider` с текстом `accent` (проверить, сохранить), контур с `danger` (очистить). Высота 40, радиус полный.'''),
    'SettingsRow': ('Строка настройки: название, пояснение, переключатель.',
                    '''Название `body-large` SemiBold, пояснение `body-small` `text-secondary`, справа переключатель Material 3 (включён — `accent` с белым бегунком; выключен — рамка `inactive`). Предупреждение — `overdue`, действие под ним — заливной кнопкой.
Секции разделены линией `divider`; заголовок секции — `title-medium` SemiBold.'''),
    'Headers': ('Заголовки: экрана, раздела, дня и день в виджете.',
                '''Экран: `headline-medium` Bold, поля `space-md` и `space-sm`. Раздел: `label-medium` Bold прописными, `text-secondary`, красным для «Просрочено» (с 0.37 над прошедшими днями стоит `OverdueHeader`). День в ленте: `label-large` Bold и белая метка «Сегодня / Завтра / Вчера» (`label-small`, радиус `radius-sm`);
прошедший день с невыполненными задачами красный. В виджете: 14 sp и метка 12 sp, у «Сегодня» справа зелёная линия прогресса (высота 4) и «3 из 7».'''),
    'EmptyTasks': ('Пустой экран: пунктирная подсказка, силуэты плашек и стрелка к «+».',
                   '''Подсказка — рамка из штрихов `inactive` (штрих 6, толщина `size-stroke`), радиус `space-sm`, внутри `title-medium` ExtraBold и `body-medium`. Силуэты плашек высотой `size-ghost-row` (44): рамка галочки и серая полоска `divider` длиной 70%, 50%, 62%.
Стрелка — зелёная кривая к кнопке «+», толщина `size-stroke`. На экране списка, где «+» плавает, стрелку поднимают над кнопкой.'''),
    'TaskRow': ('Плашка TaskRow в поиске и «Выполненные»: квадратная галочка, квадратик списка, название, значки, время.',
                '''Это прежний вид плашки, ленты «Все», «Входящие» и списков им больше не пользуются (там `TimelineRow`). Галочка — квадрат `size-checkbox` (18, радиус `radius-sm`) с рамкой цвета приоритета, зона нажатия 40; отмеченная — заливка `accent` и белая галочка.
Название `body-medium` SemiBold, под ним `body-small` — где найдено (поиск, найденное подсвечено `accent-soft`). Справа повтор 14, колокольчик 18 и время столбиком (`label-medium` Bold). Плашка `surface`, радиус `radius-md`, щель между плашками `size-card-gap` (2).'''),
    'ListRows': ('Экран «Списки»: строка «Выполненные», папка и списки.',
                 '''«Выполненные» — галочка и `body-large`. Папка: стрелка (вниз развёрнута, вправо свёрнута), имя `title-medium` SemiBold, число открытых задач, «⋮» (переименовать, удалить). Список: квадратик `size-list-square-large` (24), имя `body-large`, счётчик `body-medium` (не показывается при нуле).
Списки внутри папки сдвинуты вправо на `space-xl`. «+ Папка» и «+ Список» — текстовые кнопки `accent` справа от заголовка.'''),
    'SearchField': ('Поле поиска: подсказка «Поиск задач», запрос, очистка.',
                    '''Поле Material 3: фон `surface`, радиус `radius-lg`, высота 56, без линии снизу. Слева лупа `text-secondary`; справа крестик, когда есть запрос. Курсор `accent`. При открытии раздела курсор сразу в поле и поднята клавиатура. Найденное в плашках подсвечено `accent-soft`.'''),
    'BottomBar': ('Нижняя панель приложения: «Входящие», «Все», «Списки», «Поиск», «Настройки» и «+».',
                  '''Плавающая панель: высота `size-nav-bar` (64), радиус `radius-nav`, фон `surface`, лёгкая тень, поля от краёв `space-sm`. Вкладки — кнопки `size-nav-tab` (48) со значками 24 Material (почта, календарь, список, лупа, шестерёнка), выбранная `accent`, остальные `text-secondary`.
«+» — `size-nav-add` (52), радиус `radius-xl`, `accent`. Панель стоит над системной панелью навигации.'''),
    'ReminderStrip': ('Полоска напоминания поверх других приложений: стопка, «ещё N», «Отменить».',
                      '''Зелёная капсула `accent` высотой `size-popup-strip` (68), поля от краёв `size-popup-edge` (12), щель между полосками `size-popup-gap` (8). Слева колокольчик 22 `on-accent`; название `body-large` Bold, ниже подпись «Сегодня, 18:00 · Дом» `label-small` 80%.
Справа круглые кнопки `size-popup-button` (48): «на завтра в это же время» (`on-accent` 18%, значок листка) и «выполнено» (`surface`, галочка `accent`). Смахнуть вправо — закрыть. Видно до трёх полосок, остальные — метка «ещё N».
Состояние «Отменить»: полоска «Выполнено» или «Завтра, 18:00» с белой кнопкой «Отменить» и тонкой линией внизу, которая тает за 1,5 секунды. Без вибрации и без кольца «Пульс». Источник: `notifications/ReminderPopup.kt`.'''),
    'Widget': ('Виджет «Задачи»: лента по дням со шкалой, «сейчас» и нижняя строка.',
               '''Фон `background`, радиус `radius-xl`. Список по дням: подпись дня (красная у прошедшего), строки шкалы как в `TimelineRow`, у «Сегодня» линия прогресса. Отметка «сейчас»: зелёное время слева, точка на линии и линия до края (`size-widget-now-row` 14).
Внизу переход от прозрачного к `background` высотой 104 и нижняя строка. В виджете нет градиентов и размытия: фон и переходы — готовыми картинками. Цвета те же, что в приложении: Glance сам выбирает светлую или тёмную тему.'''),
    'ScreenAll': ('Экран «Все».', 'Лента по дням на `background`: подпись дня, шкала со строками `TimelineRow`, отметка «сейчас» у сегодняшнего дня; внизу нижняя панель. Пустая лента — `EmptyTasks`. Окно задачи открывается поверх.'),
    'ScreenInbox': ('Экран «Входящие».', 'Плоский список без подписей дней: слева колонка дня и времени (48 шириной, `label-small`), дальше шкала. Квадратик списка не показывается там, где список один (в «Входящих» он есть: он общий). «+» в нижней панели.'),
    'ScreenLists': ('Экран «Списки».', 'Заголовок и кнопки «+ Папка», «+ Список», строка «Выполненные», папки и списки (`ListRows`). Пусто: «Списков пока нет».'),
    'ScreenProject': ('Экран списка.', 'Сверху «назад», квадратик списка, название `title-large` Bold и «⋮» (переименовать, цвет, значок, удалить; у «Входящих» меню нет). Ниже плоский список без квадратиков списка. Плавающая «+» в правом нижнем углу.'),
    'ScreenSearch': ('Экран «Поиск».', 'Поле поиска, раздел «Найдено: N» с плашками `TaskRow` (найденное подсвечено), ниже «Выполненные» серым; выполненную можно вернуть галочкой.'),
    'ScreenCompleted': ('Экран «Выполненные».', 'Сверху «назад» и заголовок, ниже дни (новые сверху) с плашками `TaskRow` в отмеченном виде; снять галочку — задача вернётся в свой список с кнопкой «Отмена».'),
    'ScreenSettings': ('Экран «Настройки».', 'Заголовок и секции: напоминания (переключатели), обновление, импорт из TickTick, резервная копия, данные; внизу версия. Кнопки Material 3, предупреждения `overdue`, опасные действия `danger`.'),
    'ScreenNewTask': ('Окно создания задачи.', 'Шторка `SheetCard` стоит над клавиатурой поверх экрана под затемнением `scrim`. Подробности — в `SheetCard`, `DateFields`, `ToolButton`.'),
}
