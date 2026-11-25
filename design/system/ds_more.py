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
    'chevron': 'M8.59 16.59 13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z',
    'expand': 'M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z',
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
    """Окно по центру (SheetDialog): заголовок, содержимое, две кнопки (вторая — чёрная; None — только «Закрыть»)."""
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
    """Круглый значок над заголовком окна обновления: чёрный или красный."""
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


def nav_bar(sel, inbox_count=None, left='upcoming'):
    """Нижняя панель (итерация 55): слева «Предстоящее» или «Неделя» (выбор — удержанием), посередине «+», справа «Списки».
    [sel] — старые номера экранов: 1 «Предстоящее», 2 «Неделя», остальные (Входящие, Списки, Поиск, Настройки) живут в «Списках»."""
    if sel == 2:
        left = 'week'
    lists_on = sel not in (1, 2)
    left_ic = svg('ic_tab_week' if left == 'week' else 'ic_widget_all', 24)
    dot = '<span style="position:absolute;bottom:4px;left:50%;margin-left:-2px;width:4px;height:4px;border-radius:50%;background:currentColor;opacity:.6"></span>'
    tab = lambda ic, on, extra='': (f'<span style="position:relative;width:72px;height:40px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                                   f'background:{"var(--selected)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"}">{ic}{extra}</span>')
    badge = ''
    if inbox_count:
        # Итерация 52 → 55: число неразобранных «Входящих» — на вкладке «Списки»; при нуле его нет.
        badge = (f'<span style="position:absolute;top:-4px;right:6px;min-width:16px;height:16px;padding:0 4px;box-sizing:border-box;border-radius:8px;'
                 f'background:var(--accent);color:var(--on-accent);font-size:10px;line-height:16px;font-weight:700;text-align:center">{inbox_count}</span>')
    add = f'<span style="width:72px;height:44px;border-radius:var(--radius-xl);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("add", 24)}</span>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:57px;background:var(--surface);border-top:1px solid var(--divider);'
            f'display:flex;align-items:center;justify-content:space-around">{tab(left_ic, not lists_on, dot)}{add}{tab(svg("ic_widget_list", 24), lists_on, badge)}</div>')


def day_header(text, relative=None, overdue=False, pad='12px 0 4px 12px'):
    color = 'var(--overdue)' if overdue else 'var(--text-primary)'
    rel = f'<span style="font-size:11px;line-height:16px;font-weight:700;color:{color};background:var(--surface);border-radius:var(--radius-sm);padding:1px 8px">{relative}</span>' if relative else ''
    return f'<div style="display:flex;align-items:center;gap:8px;padding:{pad}"><span style="font-size:14px;line-height:20px;font-weight:700;color:{color}">{text}</span>{rel}</div>'


def section_header(text, color='var(--text-secondary)'):
    return f'<div style="padding:16px 16px 4px;font-size:12px;line-height:16px;font-weight:700;letter-spacing:.04em;color:{color}">{text.upper()}</div>'


def screen_title(text):
    return f'<div style="padding:12px 16px;font-size:28px;line-height:36px;font-weight:700">{text}</div>'


def frow(left, color, title, done=False, over=False, period=False, rep=False, rem=None, lsq=True, end=None, top=True, bottom=True, list_color='var(--list-pin)', list_icon='ic_list_inbox', flat=False):
    """Строка шкалы (TimelineTaskRow). left — 1–2 строки слева: [«Сегодня», «10:00»] в плоских списках или «10:00»."""
    lines = left if isinstance(left, list) else [left]
    wide = len(lines) > 1 or (lines and len(lines[0]) > 5)
    h = 46
    tcol = 'var(--overdue)' if over else ('var(--text-secondary)' if done else 'var(--text-primary)')
    fs = 11 if wide else 12
    t = (f'<div style="width:{56 if wide else 38}px;text-align:right;font-size:{fs}px;line-height:{fs + 4}px;font-weight:700;color:{tcol};display:flex;flex-direction:column;justify-content:center;flex-shrink:0">'
         + ''.join(f'<span>{x}</span>' for x in lines) + (f'<span style="color:{"var(--overdue)" if over else "var(--text-secondary)"}">{end}</span>' if end else '') + '</div>')
    nh = 20
    rail = (f'<div style="width:26px;display:flex;flex-direction:column;align-items:center;flex-shrink:0"><span style="width:1px;flex:1;background:{"var(--divider)" if top else "transparent"}"></span>'
            f'{B.node(color, done, nh, rep)}<span style="width:1px;flex:1;background:{"var(--divider)" if bottom else "transparent"}"></span></div>')
    icons = f'<span style="margin-left:4px;display:flex">{bell(18, rem)}</span>' if rem else ''
    lsqh = f'<span class="list-sq" style="background:{list_color};margin-left:8px">{svg(list_icon, 12)}</span>' if lsq else ''
    plaque = (f'<div style="flex:1;min-width:0;margin:3px 0;border-radius:var(--radius-md);background:{"transparent" if flat else "var(--surface)"};display:flex;align-items:center;padding:0 12px;opacity:{.7 if done else 1}">'
              f'<span style="flex:1;min-width:0;font-size:14px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;{"text-decoration:line-through;color:var(--text-secondary)" if done else ""}">{title}</span>{icons}{lsqh}</div>')
    return f'<div style="display:flex;height:{h}px;padding-right:8px">{t}{rail}{plaque}</div>'


def square_check(ring, done=False):
    if done:
        return f'<span style="width:18px;height:18px;border-radius:var(--radius-sm);background:var(--accent);border:2px solid var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("check", 14)}</span>'
    return f'<span style="width:18px;height:18px;border-radius:var(--radius-sm);background:var(--surface);border:2px solid {ring};display:block"></span>'


def old_row(title, ring='var(--inactive)', done=False, sub=None, trailing=None, rep=False, rem=None, mark=None, list_color='var(--inbox-list)'):
    """Плашка TaskRow (поиск, «Выполненные»): квадратная галочка с рамкой цвета приоритета, эмодзи списка, название, значки, время справа."""
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
    lsq = lambda e: f'<span style="width:16px;height:16px;font-size:15px;line-height:1;display:inline-flex;align-items:center;justify-content:center">{e}</span>'
    lst = dlg('Список', option('Входящие', False, lsq('📥')) + option('Работа', True, lsq('💼')) + option('Дом', False, lsq('🏠')) + option('Покупки', False, lsq('🛒')))
    # 0.52.1: у задачи с датой первый вариант — «📌 Без списка».
    dated = dlg('Список', option('Без списка', True, lsq('📌')) + option('Работа', False, lsq('💼')) + option('Дом', False, lsq('🏠')) + option('Покупки', False, lsq('🛒')))
    flag = lambda c: f'<span style="color:{c};display:flex">{svg("ic_tool_priority", 24)}</span>'
    pri = dlg('Приоритет', option('Высокий', True, flag('var(--ring-high)')) + option('Обычный', False, flag('var(--inactive)')))
    return doc('Окна', 380, 'Окно «Список» (эмодзи) и «Приоритет» (два уровня, только при выборе нескольких задач)', f'<div class="row" style="align-items:flex-start;gap:24px">{lst}{dated}{pri}</div>')


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


def list_edit_dialog(kind='list'):
    """Окно создания списка и папки (0.56, `ListEditDialog`): плашка эмодзи + название, быстрый выбор, у списка — папка."""
    quick = '📁 💼 🏠 🛒 ❤️ 💪 📚 💰 ✈️ 💡 ⭐ 🎮 🏀 🎯 🧩'.split()
    sel = '🛒' if kind == 'list' else '🎯'
    tile = lambda e: (f'<span style="width:38px;height:38px;box-sizing:border-box;border-radius:var(--radius-md);background:{"var(--selected)" if e == sel else "transparent"};'
                      f'border:{"2px solid var(--accent)" if e == sel else "1px solid transparent"};display:flex;align-items:center;justify-content:center">{em(e, 20, 24)}</span>')
    more = ('<span style="width:38px;height:38px;box-sizing:border-box;border-radius:var(--radius-md);border:1px solid var(--divider);display:flex;align-items:center;'
            'justify-content:center;font-weight:800;color:var(--text-secondary)">…</span>')
    lab = lambda t: f'<div style="font-size:12px;font-weight:700;letter-spacing:.04em;color:var(--text-secondary);padding:12px 0 4px">{t}</div>'
    btn = (f'<span style="position:relative;width:56px;height:56px;flex-shrink:0;border-radius:var(--radius-lg);background:var(--selected);display:flex;align-items:center;justify-content:center">'
           f'{em(sel, 28, 32)}<span style="position:absolute;right:-4px;bottom:-4px;width:20px;height:20px;border-radius:10px;background:var(--surface);border:1px solid var(--divider);'
           f'display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{mi("down", 14)}</span></span>')
    name = 'Покупки' if kind == 'list' else 'Проекты'
    field_ = (f'<div style="flex:1;height:56px;box-sizing:border-box;border-radius:var(--radius-lg);border:2px solid var(--accent);background:var(--background);'
              f'display:flex;align-items:center;padding:0 14px;font-size:16px;font-weight:700">{name}</div>')
    body = f'<div style="display:flex;gap:12px;align-items:center">{btn}{field_}</div>' + lab('ЭМОДЗИ') + f'<div style="display:flex;flex-wrap:wrap;gap:4px">{"".join(tile(e) for e in quick)}{more}</div>'
    if kind == 'list':
        body += lab('ПАПКА') + f'<div style="display:flex;gap:8px;flex-wrap:wrap">{chip("Без папки", True)}{chip("🧩 Личное")}</div>'
    return dlg('Новый список' if kind == 'list' else 'Новая папка', f'<div style="padding:8px 24px 12px">{body}</div>', buttons=('Отмена', 'Создать'), width=360)


def p_alert_dialogs():
    field = f'<div style="height:56px;border-radius:4px;border:2px solid var(--accent);display:flex;align-items:center;padding:0 16px;font-size:16px;color:var(--text-primary)">Работа</div>'
    folder = f'<div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:12px">{chip("Без папки", True)}{chip("Дом")}{chip("Учёба")}</div>'
    name = list_edit_dialog()
    delete = alert('Удалить список «Работа»?', 'Задачи и чек-листы этого списка тоже будут удалены.', [('Отмена', 'var(--accent)'), ('Удалить', 'var(--danger)')])
    groups = [('Дела', '📥 📋 ✅ 📌 ⭐ 🔥 💡 🎯'), ('Работа', '💼 💻 📊 📈 📝 📅 📞 🗂️'), ('Дом', '🏠 🛒 🧹 🍳 🛠️ 🌱 🐶 🧺')]
    tile = lambda e, sel: (f'<span style="width:40px;height:40px;border-radius:var(--radius-md);background:{"var(--selected)" if sel else "transparent"};'
                           f'border:{"2px solid var(--accent)" if sel else "1px solid transparent"};box-sizing:border-box;font-size:22px;display:flex;align-items:center;justify-content:center">{e}</span>')
    grid = ''.join(f'<div style="font-size:12px;font-weight:700;color:var(--text-secondary);padding:8px 0 4px">{g.upper()}</div>'
                   f'<div style="display:flex;gap:2px;flex-wrap:wrap">{"".join(tile(e, e == "💼") for e in es.split())}</div>' for g, es in groups)
    field = ('<div style="margin-top:12px;height:48px;border-radius:4px;border:1px solid var(--divider);display:flex;align-items:center;padding:0 12px;'
             'font-size:14px;color:var(--inactive)">Свой эмодзи — вставь с клавиатуры</div>')
    emoji = alert('Эмодзи списка', grid + field, [('Отмена', 'var(--accent)')]).replace('width:300px', 'width:360px')
    return doc('Окна', 640, 'Окна списка: создание с эмодзи (0.56), удаление, все эмодзи', f'<div class="row" style="align-items:flex-start;gap:24px">{name}{delete}{emoji}</div>')


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
            f'<div class="col"><div style="display:flex;gap:8px">{chip("Неделя", True)}{chip("Месяц")}</div><div class="cap">чип: выбран — чёрный, нет — белый с рамкой</div></div>'
            f'<div class="col"><div style="display:flex;gap:12px;align-items:center">{material_button("Выбрать файл .csv")}{material_button("Проверить обновление", "outlined")}{material_button("Очистить все данные", "outlined", "var(--danger)")}</div><div class="cap" style="max-width:none">кнопки Material 3: заливка, контур, опасное действие</div></div></div>')
    return doc('Панель и поля', 150, 'Плавающая «+», чипы и кнопки Material', body)


def p_switch_rows():
    sw = lambda t, h, on: (f'<div style="display:flex;align-items:center;gap:12px"><div style="flex:1"><div style="font-size:16px;line-height:24px;font-weight:600">{t}</div><div style="font-size:12px;line-height:16px;color:var(--text-secondary)">{h}</div></div>{switch(on)}</div>')
    body = ('<div style="width:380px;display:flex;flex-direction:column;gap:12px">'
            '<div style="font-size:16px;font-weight:600;line-height:24px">Напоминания</div>'
            f'{sw("Всплывающая полоска", "Напоминание — чёрной полоской снизу поверх любого приложения.", True)}'
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



# --------------------------------------------------------------- «Списки» 0.56: карточки и строка значков

def em(e, px=20, w=24):
    return f'<span style="font-size:{px}px;line-height:1;display:inline-flex;align-items:center;justify-content:center;flex-shrink:0;width:{w}px;height:{w}px">{e}</span>'


def card_row(lead, name, n=0, last=False, indent=0, trail=''):
    """Строка карточки (0.56): значок/эмодзи 24, название SemiBold, число — только если не 0; линия `divider` под всеми, кроме последней."""
    count = f'<span style="font-size:14px;font-weight:600;color:var(--text-secondary)">{n}</span>' if n else ''
    sep = '' if last else f'<div style="position:absolute;left:{52 + indent}px;right:0;bottom:0;height:1px;background:var(--divider)"></div>'
    return (f'<div style="position:relative;display:flex;align-items:center;gap:12px;min-height:52px;padding:0 16px 0 {16 + indent}px;color:var(--text-secondary)">'
            f'<span style="width:24px;display:flex;justify-content:center;flex-shrink:0">{lead}</span>'
            f'<span style="flex:1;min-width:0;font-size:16px;line-height:22px;font-weight:600;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{name}</span>'
            f'{count}{trail}{sep}</div>')


def folder_card_row(e, name, n, last=False):
    trail = (f'<span style="display:flex;gap:4px;align-items:center;margin-left:4px">{mi("down", 22)}'
             f'<span style="width:36px;height:36px;display:flex;align-items:center;justify-content:center;margin-right:-8px">{mi("more", 22)}</span></span>')
    return card_row(em(e), name, n, last, trail=trail)


def lists_card(rows):
    return f'<div style="margin:0 12px 12px;border-radius:var(--radius-lg);background:var(--surface);overflow:hidden">{rows}</div>'


def lists_toolbar():
    b = lambda ic: (f'<span style="flex:1;height:44px;border-radius:var(--radius-lg);background:var(--surface);color:var(--text-primary);'
                    f'display:flex;align-items:center;justify-content:center">{svg(ic, 24)}</span>')
    return (f'<div style="position:absolute;left:0;right:0;bottom:57px;padding:16px 12px 8px;background:linear-gradient(to bottom,transparent,var(--background) 35%)">'
            f'<div style="display:flex;gap:8px">{b("ic_nav_search")}{b("ic_folder_plus")}{b("ic_list_plus")}{b("ic_nav_settings")}</div></div>')


SYSTEM_CARD = lambda: lists_card(card_row(em('📥'), 'Входящие', 50) + card_row(em('💭'), 'Когда-нибудь', 7) + card_row(em('📌'), 'Без списка', 2, last=True))
ARCHIVE_CARD = lambda: lists_card(card_row(mi('check'), 'Выполненные') + card_row(svg('ic_wont_do', 24), 'Не буду делать', 4) + card_row(svg('ic_tool_delete', 24), 'Корзина', 20, last=True))
MINE_CARD = lambda: lists_card(card_row(em('💼'), 'Работа', 12) + folder_card_row('🧩', 'Личное', 5) + card_row(em('🏠'), 'Дом', 4, indent=24)
                               + card_row(em('❤️'), 'Здоровье', 1, indent=24) + card_row(em('💡'), 'Идеи', last=True))


def p_list_rows():
    body = f'<div style="width:412px;background:var(--background);padding:12px 0 0">{MINE_CARD()}{ARCHIVE_CARD()}</div>'
    return doc('Список и виджет', 560, 'Строки «Списков» в карточках (0.56): папка с эмодзи, списки с числом (при нуле без числа), архив', body)


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
    body = '<div style="display:flex;flex-direction:column;gap:12px">' + ''.join(frame(f'<div style="position:absolute;inset:0">{nav_bar(i, n)}</div>', h=57, w=412) for i, n in ((1, None), (2, None), (3, 3))) + '</div>'
    return doc('Список и виджет', 320, 'Нижняя панель (0.55): «Предстоящее» или «Неделя» · «+» · «Списки» с числом «Входящих»; выбранное место — на светлой подложке', body)


STRIP_BOX = ('background:var(--surface);color:var(--text-primary);border:1px solid var(--divider);'
             'box-shadow:0 6px 18px color-mix(in srgb,var(--text-primary) 18%,transparent);box-sizing:border-box')


def strip_btn(icon, pressed=False):
    """Круглая чёрная кнопка белой полоски (0.53, T-118); нажатая «Отложить» — с кольцом."""
    ring = ';box-shadow:0 0 0 3px var(--surface),0 0 0 5px var(--accent)' if pressed else ''
    return (f'<span style="width:48px;height:48px;border-radius:50%;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;'
            f'justify-content:center;flex-shrink:0{ring}">{svg(icon, 22)}</span>')


def strip(title, subtitle, undo=None, pressed=False, undo_icon='ic_node_check'):
    """Полоска 0.53 (T-118): белая, кнопки чёрные — «Не буду делать», «Отложить» (часы), «Выполнено»."""
    if undo is not None:
        return (f'<div style="height:68px;border-radius:34px;{STRIP_BOX};display:flex;align-items:center;gap:12px;padding:0 10px 0 16px;position:relative;overflow:hidden">'
                f'<span style="display:flex;color:var(--text-secondary)">{svg(undo_icon, 22)}</span><span style="flex:1;font-size:16px;line-height:24px;font-weight:700">{undo}</span>'
                f'<span style="font-size:14px;font-weight:700;color:var(--on-accent);background:var(--accent);border-radius:20px;padding:10px 18px">Отменить</span>'
                f'<span style="position:absolute;left:0;bottom:0;height:3px;width:55%;background:var(--accent)"></span></div>')
    return (f'<div style="height:68px;border-radius:34px;{STRIP_BOX};display:flex;align-items:center;gap:10px;padding:0 10px 0 16px">'
            f'<span style="display:flex;color:var(--text-secondary)">{svg("ic_tool_reminder", 22)}</span>'
            f'<div style="flex:1;min-width:0"><div style="font-size:16px;line-height:24px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{title}</div>'
            f'<div style="font-size:11px;line-height:16px;color:var(--text-secondary)">{subtitle}</div></div>'
            f'{strip_btn("ic_wont_do")}{strip_btn("ic_popup_tomorrow", pressed)}{strip_btn("ic_node_check")}</div>')


def snooze_row(label, when, last=False, icon=None):
    ic = f'<span style="display:flex;color:var(--text-secondary)">{svg(icon, 20)}</span>' if icon else ''
    return (f'<div style="min-height:48px;display:flex;align-items:center;gap:12px;padding:0 20px;{"" if last else "border-bottom:1px solid var(--divider)"}">'
            f'{ic}<span style="flex:1;font-size:16px;line-height:24px;font-weight:600">{label}</span>'
            f'<span style="font-size:14px;line-height:20px;font-weight:600;color:var(--text-secondary)">{when}</span></div>')


SNOOZE_TIMED = [('Через 1 час', '18:00'), ('Через 3 часа', '20:00'), ('Вечером', '19:00'), ('Завтра', 'чт, 12 ноя, 17:00')]
SNOOZE_DAYS = [('Завтра', 'чт, 12 ноя'), ('Послезавтра', 'пт, 13 ноя'), ('В субботу', 'сб, 14 ноя'), ('Через неделю', 'ср, 18 ноя')]


def snooze_menu(title, rows, pick='Выбрать дату и время…'):
    """Меню «Отложить» над полоской (0.53): подпись, варианты с новым сроком справа, «Выбрать дату…»."""
    head = f'<div style="padding:14px 20px 6px;font-size:12px;line-height:16px;font-weight:700;color:var(--text-secondary)">{title}</div>'
    body = ''.join(snooze_row(l, w) for l, w in rows) + snooze_row(pick, '', last=True, icon='ic_widget_all')
    return f'<div style="border-radius:var(--radius-xl);background:var(--surface);overflow:hidden;{STRIP_BOX}">{head}{body}</div>'


def p_snooze():
    timed = (f'<div style="width:388px;display:flex;flex-direction:column;gap:8px">{snooze_menu("ОТЛОЖИТЬ «СОЗВОН С КОМАНДОЙ»", SNOOZE_TIMED)}'
             f'{strip("Созвон с командой", "Сегодня, 17:00 · Работа", pressed=True)}</div>')
    days = (f'<div style="width:388px;display:flex;flex-direction:column;gap:8px">{snooze_menu("ОТЛОЖИТЬ «ОПЛАТИТЬ ИНТЕРНЕТ»", SNOOZE_DAYS, "Выбрать дату…")}'
            f'{strip("Оплатить интернет", "Сегодня · Дом", pressed=True)}</div>')
    window = dlg('Отложить', ''.join(snooze_row(l, w) for l, w in SNOOZE_TIMED) + snooze_row('Выбрать дату и время…', '', last=True, icon='ic_widget_all'),
                 buttons=('Отмена',), subtitle='Созвон с командой · Сегодня, 17:00', width=356)
    body = f'<div class="row" style="align-items:flex-end;gap:24px">{timed}{days}{window}</div>'
    return doc('Напоминания и виджет', 470, '«Отложить»: меню над полоской (со временем и на весь день) и окно из уведомления; справа — новый срок', body)


def p_reminder_strip():
    more = '<div style="align-self:center;font-size:12px;line-height:16px;font-weight:700;color:var(--text-secondary);background:var(--surface);border-radius:var(--radius-md);padding:4px 8px">ещё 2</div>'
    body = (f'<div style="width:412px;padding:8px 12px;display:flex;flex-direction:column;gap:8px">{more}{strip("Созвон с командой", "Сегодня, 18:00 · Дом")}{strip("Оплатить интернет", "Сегодня, 18:30")}'
            f'{strip("", "", undo="Выполнено")}{strip("", "", undo="Отложено на 18:00", undo_icon="ic_popup_tomorrow")}</div>')
    return doc('Напоминания и виджет', 420, 'Полоска напоминания поверх других приложений: стопка, «ещё N» и «Отменить»', body)


def widget_body(pad_bottom=0, sel='all'):
    title = ('<div style="display:flex;align-items:center;gap:4px;padding:2px 6px 2px"><span style="font-size:14px;font-weight:700;color:var(--overdue)">СР, 14 ОКТ</span>'
             '<span style="font-size:12px;font-weight:700;color:var(--overdue);background:var(--surface);border-radius:var(--radius-sm);padding:1px 4px">Вчера</span>'
             '<span style="flex:1"></span><span style="font-size:12px;font-weight:500;color:var(--text-secondary)">2</span></div>')
    title2 = ('<div style="display:flex;align-items:center;gap:4px;padding:6px 6px 2px"><span style="font-size:14px;font-weight:700">ЧТ, 15 ОКТ</span>'
              '<span style="font-size:12px;font-weight:700;background:var(--surface);border-radius:var(--radius-sm);padding:1px 4px">Сегодня</span><span style="width:8px"></span>'
              '<span style="flex:1;height:4px;border-radius:var(--radius-sm);background:var(--divider);position:relative"><span style="position:absolute;left:0;top:0;bottom:0;width:43%;border-radius:var(--radius-sm);background:var(--accent)"></span></span>'
              '<span style="font-size:12px;font-weight:500;color:var(--text-secondary);padding-left:8px">3 из 7</span></div>')
    now = ('<div style="display:flex;align-items:center;height:14px;padding-right:8px"><span style="width:38px;text-align:right;font-size:11px;font-weight:700;color:var(--accent)">14:40</span>'
           '<span style="width:26px;display:flex;justify-content:center"><span style="width:7px;height:7px;border-radius:50%;background:var(--accent)"></span></span><span style="flex:1;height:1px;background:var(--accent)"></span></div>')
    rows = (overdue_header(1, widget=True) + title + frow('11:00', 'var(--inactive)', 'Отчёт за неделю', over=True, top=False, bottom=False, lsq=True) + title2
            + frow('09:00', 'var(--ring-high)', 'Зарядка', done=True, top=False) + frow('12:30', 'var(--ring-medium)', 'Созвон с командой', period=True, end='13:30', rep=True, rem='at') + now
            + frow('18:00', 'var(--ring-low)', 'Оплатить интернет', rem='early', bottom=False))
    # Итерация 57 (решение PM): «+» слева, белый блок вкладок по центру (выбранный — чёрный значок), справа дата «10 ВТ / ноя»; без линии сверху.
    icons = ''.join(f'<span style="width:44px;height:32px;display:flex;align-items:center;justify-content:center;color:{"var(--accent)" if s_ == sel else "var(--inactive)"}">{svg(ic, 22)}</span>'
                    for ic, s_ in (('ic_widget_all', 'all'), ('ic_widget_inbox', 'inbox'), ('ic_tab_week', 'week')))
    tile = lambda inner, bg, color: f'<span style="width:64px;height:32px;border-radius:var(--radius-lg);background:{bg};color:{color};display:flex;align-items:center;justify-content:center;flex-shrink:0">{inner}</span>'
    date = ('<span style="display:flex;align-items:center;gap:3px"><span style="font-size:18px;line-height:20px;font-weight:800">15</span><span style="display:flex;flex-direction:column">'
            '<span style="font-size:9px;line-height:10px;font-weight:800">ЧТ</span><span style="font-size:9px;line-height:10px;font-weight:600;color:var(--text-secondary)">окт</span></span></span>')
    side = 'flex:1;display:flex;align-items:center'
    bottom = ('<div style="position:absolute;left:0;right:0;bottom:0;height:40px;display:flex;align-items:center;padding:0 6px;background:var(--background)">'
              f'<span style="{side}">{tile(svg("ic_widget_add", 22), "var(--accent)", "var(--on-accent)")}</span>'
              f'<span style="display:flex;padding:0 4px;border-radius:var(--radius-lg);background:var(--surface)">{icons}</span>'
              f'<span style="{side};justify-content:flex-end">{tile(date, "var(--surface)", "var(--text-primary)")}</span></div>')
    return rows, bottom


def p_widget_bar():
    rows, bottom = widget_body()
    body = (f'<div style="position:relative;width:360px;height:420px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;border:1px solid var(--divider);padding:6px 3px 0">{rows}{bottom}</div>')
    return doc('Напоминания и виджет', 470, 'Виджет «Задачи»: подпись дня с прогрессом, шкала, «сейчас» и нижняя строка', body)


def p_widget_bar_only():
    _, bottom = widget_body()
    body = f'<div style="position:relative;width:360px;height:70px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;border:1px solid var(--divider)">{bottom}</div>'
    return doc('Напоминания и виджет', 120, 'Нижняя строка виджета (0.57): «+», блок вкладок по центру, дата «15 ЧТ / окт»; свободное место — открыть приложение', body)


def p_widget_week():
    """Вкладка «Неделя» в виджете (0.46.0): строки мельче, чем в приложении, календарь без месяца, плоская нижняя строка.
    Итерация 48 (макет `week_tune.py`, вариант В): эмодзи списка слева от времени, время у края плашки, название 10, строка даты 9 капсом
    по высоте текста, календарь 11."""
    _, bottom = widget_body(sel='week')
    t = lambda *a, e='💼', **k: week_task(*a, h=20, fs=10, gap=2, emoji=e, **k)
    h = 150
    r1 = f'<div style="display:flex">{week_cell("пн", 26, [t("Купить продукты", "10:00", "low", done=True, e="🏠"), t("Тренировка", e="🎾")], h=h, widget=True)}{week_cell("вт", 27, [t("Оплатить интернет", "12:00", "medium", over=True, e="💰")], h=h, widget=True)}</div>'
    r2 = f'<div style="display:flex">{week_cell("ср", 28, [t("Встреча", "15:00", "high", over=True)], h=h, widget=True)}{week_cell("чт", 29, [], h=h, widget=True)}</div>'
    r3 = (f'<div style="display:flex">{week_cell("пт", 30, [t("Отчёт", "17:00", "high"), t("Кино", e="📌"), t("Ужин", "20:00", e="🏠"), t("Зарядка", "08:00", "medium", done=True, e="🎾"), t("Врач", "", "low", e="📌"), t("Билеты", e="📌")], today=True, hidden=1, h=h, widget=True)}'
          f'{week_cell("сб", 31, [t("Уборка", "11:00", e="🏠"), t("Созвон", "10:00", "medium", ghost=True)], h=h, widget=True)}</div>')
    r4 = f'<div style="display:flex">{week_cell("вс", 1, [t("План на неделю", e="📌")], h=h, widget=True)}{week_calendar(h, widget=True)}</div>'
    body = f'<div style="position:relative;width:360px;height:{2 + 4 * h + 41}px;border-radius:var(--radius-xl);background:var(--background);overflow:hidden;border:1px solid var(--divider);padding-top:2px;box-sizing:border-box">{r1}{r2}{r3}{r4}{bottom}</div>'
    return doc('Напоминания и виджет', 720, 'Виджет, вкладка «Неделя»: эмодзи, название, начало справа; без значков выполнения, вся ячейка открывает день, «+N» и календарь сохранены', body)


# --------------------------------------------------------------- экраны

def all_screen():
    rows = (overdue_header(1) + day_header('СР, 14 окт', 'Вчера', True) + frow('11:00', 'var(--inactive)', 'Отчёт за неделю', over=True, top=False, bottom=False)
            + day_header('ЧТ, 15 окт', 'Сегодня') + frow('09:00', 'var(--ring-high)', 'Зарядка', done=True, top=False) + frow('12:30', 'var(--ring-medium)', 'Созвон с командой', period=True, end='13:30', rep=True, rem='at')
            + '<div style="display:flex;align-items:center;height:14px;padding-right:8px"><span style="width:38px;text-align:right;font-size:11px;font-weight:700;color:var(--accent)">14:40</span><span style="width:26px;display:flex;justify-content:center"><span style="width:7px;height:7px;border-radius:50%;background:var(--accent)"></span></span><span style="flex:1;height:1px;background:var(--accent)"></span></div>'
            + frow('18:00', 'var(--ring-low)', 'Оплатить интернет', rem='early', bottom=False)
            + day_header('ПТ, 16 окт', 'Завтра') + frow('10:00', 'var(--inactive)', 'Купить билеты', top=False, bottom=False))
    return frame(f'<div style="padding-top:8px">{screen_title("Предстоящее")}{rows}</div>{nav_bar(1)}', 740)


def inbox_screen():
    """Итерация 52: только неразобранное — без даты и без списка; под заголовком подсказка, на вкладке — счётчик."""
    hint = ('<div style="padding:0 16px 8px;font-size:12px;line-height:16px;color:var(--text-secondary)">Здесь — то, что ещё не разобрано: без даты и без списка. '
            'Поставьте дату или список — задача уйдёт сама.</div>')
    rows = (frow('', 'var(--inactive)', 'Идея подарка маме', top=False, lsq=False) + frow('', 'var(--ring-high)', 'Посмотреть «Дюну 3»', lsq=False)
            + frow('', 'var(--inactive)', 'Разобрать антресоль', bottom=False, lsq=False))
    return frame(f'<div style="padding-top:8px">{screen_title("Входящие")}{hint}{rows}</div>{nav_bar(0, 3)}', 680)


def sorted_head(title, count):
    return (f'<div style="display:flex;align-items:center;padding:8px 16px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("back")}</span>'
            f'<span style="flex:1;font-size:22px;line-height:28px;font-weight:700">{title}</span><span style="font-size:14px;font-weight:700;color:var(--text-secondary)">{count}</span></div>')


def sorted_group(color, icon, name, n):
    return (f'<div style="display:flex;align-items:center;gap:8px;padding:12px 16px 4px"><span class="list-sq" style="background:{color}">{svg(icon, 12)}</span>'
            f'<span style="font-size:14px;line-height:20px;font-weight:700">{name}</span><span style="font-size:12px;font-weight:700;color:var(--text-secondary)">{n}</span></div>')


def sorted_fab():
    return (f'<span style="position:absolute;right:16px;bottom:16px;width:56px;height:56px;border-radius:16px;background:var(--accent);color:var(--on-accent);'
            f'display:flex;align-items:center;justify-content:center;{SHADOW}">{mi("add")}</span>')


def someday_screen():
    """«Когда-нибудь» (итерация 52): задачи со списком, но без даты, группами по спискам в порядке «Списков»."""
    w, h = ('var(--list-blue)', 'ic_list_work'), ('var(--list-orange)', 'ic_list_home')
    r = lambda t, l, top=True, bottom=True: frow('', 'var(--inactive)', t, top=top, bottom=bottom, list_color=l[0], list_icon=l[1])
    rows = (sorted_group(*w, 'Работа', 2) + r('Обновить портфолио', w, top=False) + r('Курс по аналитике', w, bottom=False)
            + sorted_group(*h, 'Дом', 2) + r('Повесить полку', h, top=False) + r('Поменять фильтр воды', h, bottom=False))
    return frame(f'{sorted_head("Когда-нибудь", 4)}{rows}{sorted_fab()}', 680)


def nolist_screen():
    """«Без списка» (итерация 52): задачи без списка, но с датой, по дням, как «Все»."""
    rows = (day_header('СБ, 07 ноя', 'Сегодня') + frow('19:00', 'var(--inactive)', 'Купить подарок', top=False, bottom=False)
            + day_header('ПТ, 13 ноя') + frow('', 'var(--inactive)', 'Забрать посылку', top=False, bottom=False, rem='at'))
    return frame(f'{sorted_head("Без списка", 2)}{rows}{sorted_fab()}', 680)



def lists_screen():
    """Экран «Списки» (0.56): без заголовка, три карточки, строка значков над нижней панелью."""
    body = f'<div style="padding-top:16px">{SYSTEM_CARD()}{MINE_CARD()}{ARCHIVE_CARD()}</div>'
    return frame(f'{body}{lists_toolbar()}{nav_bar(3, 3)}', 800)


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
    return frame(f'{field}{rows}{nav_bar(4)}', 680)


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
            f'{title}Ввод задач</div>{sw("Дата и время в названии", "«Созвон завтра 10:00» — срок ставится сам, фраза уходит из названия.", True)}{divider}'
            f'{title}Напоминания</div>{sw("Всплывающая полоска", "Напоминание — чёрной полоской снизу поверх любого приложения.", True)}'
            f'{sw("Точное время напоминаний", "Напоминание приходит секунда в секунду.", True)}{sw("Утренняя сводка", "Каждый день в это время — уведомление с планом на сегодня.", False)}{divider}'
            f'{title}Обновление</div><div style="font-size:14px;line-height:20px">Установлена последняя версия</div><div>{material_button("Проверить обновление", "outlined")}</div>{divider}'
            f'{title}Импорт из TickTick</div><div style="font-size:14px;line-height:20px;color:var(--text-secondary)">Резервную копию (.csv) можно создать в настройках веб-версии TickTick.</div><div>{material_button("Выбрать файл .csv")}</div>{divider}'
            f'{title}Данные</div><div style="font-size:14px;line-height:20px">Задач: 3399 (активных 145)</div><div>{material_button("Очистить все данные", "outlined", "var(--danger)")}</div>'
            '<div style="font-size:12px;line-height:16px;color:var(--text-secondary);padding-top:12px">Версия 0.34.0 (сборка 138)</div></div>')
    return frame(f'{body}{nav_bar(5)}', 850)


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
    """Подсказка «Отмена» (итерация 38): белая полупрозрачная карточка (88%), значок слева, чёрная «Отмена»."""
    return (f'<div style="width:380px;min-height:56px;border-radius:var(--radius-xl);background:color-mix(in srgb,var(--surface) 88%,transparent);border:1px solid var(--divider);'
            f'color:var(--text-primary);display:flex;align-items:center;padding:0 4px 0 16px;gap:12px">'
            f'<span style="display:flex;color:var(--text-secondary)">{svg(kind, 22)}</span>'
            f'<span style="flex:1;font-size:14px;line-height:20px;font-weight:600;padding:8px 0">{text}</span>'
            f'<span style="min-height:40px;padding:0 12px;display:flex;align-items:center;font-size:14px;line-height:20px;font-weight:700;color:var(--accent)">Отмена</span></div>')


def p_snackbar():
    rows = [('Задача выполнена', 'ic_node_check'), ('Следующий повтор: сб, 17 окт', 'ic_tool_repeat'), ('Задача удалена', 'ic_tool_delete'),
            ('Перенесено на сегодня: 3', 'ic_move_today'), ('Задача возвращена', 'ic_snack_restore'), ('Не буду делать · вс, 08 ноя', 'ic_wont_do'), ('Отложено на 18:00', 'ic_popup_tomorrow')]
    body = '<div style="width:412px;padding:12px;display:flex;flex-direction:column;gap:8px">' + ''.join(snackbar(t, k) for t, k in rows) + '</div>'
    return doc('Список и виджет', 400, 'Подсказка внизу экрана с кнопкой «Отмена»: белая полупрозрачная карточка со значком', body)


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
            '<div style="display:flex;align-items:center;gap:8px;font-size:12px;line-height:16px;color:var(--text-secondary)"><span style="color:var(--accent);display:flex">' + svg('ic_notification_tasks', 16) + '</span>Simple Tasks · Напоминания · сейчас</div>'
            '<div style="font-size:16px;line-height:24px;font-weight:700;padding-top:4px">Созвон с командой</div>'
            '<div style="font-size:14px;line-height:20px;color:var(--text-secondary)">Сегодня, 18:00 · Дом</div>'
            f'<div style="display:flex;gap:4px;padding-top:8px;margin-left:-12px">{notif_btn("Выполнено")}{notif_btn("Не буду делать")}{notif_btn("Отложить")}</div></div></div>')
    return doc('Напоминания и виджет', 190, 'Обычное уведомление-напоминание с кнопками «Выполнено», «Не буду делать» и «Отложить»', body)


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
    """«Корзина» на «Списках» (0.39; с 0.56 — последняя строка карточки архива)."""
    body = f'<div style="padding-top:16px">{SYSTEM_CARD()}{MINE_CARD()}{ARCHIVE_CARD()}</div>'
    return frame(f'{body}{lists_toolbar()}{nav_bar(3)}', 800)


def readonly_task(menu=True, base=None, icon=None, banner='В корзине · удалена 28 окт', items=('Восстановить', 'Удалить навсегда')):
    card = B.sheet_card(True, width=396)
    card = card.replace('<span class="confirm">' + B.CHECK + '</span>', '')
    card = card.replace('class="tool sel"', 'class="tool dim"').replace('class="tool pri"', 'class="tool pri dim"')
    card = card.replace(f'<span class="clr" style="position:static">{B.X}</span>', '').replace(f'<span class="clr">{B.X}</span>', '')
    banner = (f'<div style="margin:0 0 8px;border-radius:var(--radius-lg);background:var(--selected);color:var(--text-primary);display:flex;align-items:center;gap:10px;padding:10px 14px;font-size:14px;line-height:20px;font-weight:600">'
              f'<span style="display:flex;color:var(--text-secondary)">{icon or TRASH_ICON(20)}</span><span style="flex:1">{banner}</span></div>')
    popup = ''
    if menu:
        item = lambda t, c='var(--text-primary)': f'<div style="min-height:48px;display:flex;align-items:center;padding:0 20px;font-size:16px;line-height:24px;font-weight:600;color:{c}">{t}</div>'
        popup = ('<div style="position:absolute;right:20px;top:516px;width:240px;border-radius:var(--radius-lg);background:var(--surface);'
                 'box-shadow:0 4px 16px color-mix(in srgb,var(--text-primary) 18%,transparent);border:1px solid var(--divider);overflow:hidden">'
                 + item(items[0]) + '<div style="height:1px;background:var(--divider)"></div>' + item(items[1], 'var(--danger)') + '</div>')
    scrim = '<div style="position:absolute;inset:0;background:var(--scrim)"></div>'
    base = base or trash_screen()
    return (f'<div style="position:relative;width:412px;height:680px;flex-shrink:0">{base}{scrim}'
            f'<div style="position:absolute;left:8px;right:8px;bottom:16px">{banner}{card}</div>{popup}</div>')


def p_danger():
    body = f'<div class="row" style="align-items:flex-start">{danger_dialog("Удалить навсегда?", "Задача «Позвонить врачу» и её подзадачи будут стёрты. Вернуть их будет нельзя.", "Удалить")}{danger_dialog("Удалить все навсегда?", "Задач в корзине: 5. Все они будут стёрты, вернуть их будет нельзя.", "Удалить все")}</div>'
    return doc('Окна', 300, 'Окна необратимых действий: красная кнопка', body)


def danger_dialog(title, text, ok):
    d = dlg(title, f'<div style="padding:0 24px 8px;font-size:14px;line-height:20px;color:var(--text-secondary)">{text}</div>', ('Отмена', ok))
    return d.replace('background:var(--accent);color:var(--on-accent)', 'background:var(--danger);color:var(--on-accent)')




# ---------------------------------------------------------------- итерация 40: массовое редактирование

def sel_row(left, color, title, selected=False, done=False, ghost=False, rep=False, rem=None, top=True, bottom=True, list_color='var(--list-pin)', list_icon='ic_list_inbox', sub=None):
    """Строка шкалы в режиме выбора: кружок → чёрная галочка, плашка подсвечена; будущий повтор не выбирается."""
    lines = left if isinstance(left, list) else [left]
    wide = len(lines) > 1 or (lines and len(lines[0]) > 5)
    h = 58 if sub else 46
    tcol = 'var(--text-secondary)' if done or ghost else 'var(--text-primary)'
    t = (f'<div style="width:{56 if wide else 38}px;text-align:right;font-size:{11 if wide else 12}px;line-height:{15 if wide else 16}px;font-weight:700;color:{tcol};display:flex;flex-direction:column;justify-content:center;flex-shrink:0">'
         + ''.join(f'<span>{x}</span>' for x in lines) + '</div>')
    if selected:
        node = f'<span style="width:20px;height:20px;border-radius:50%;background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("check", 14)}</span>'
    else:
        node = B.node(color, done, 20, rep and not ghost)
    rail = (f'<div style="width:26px;display:flex;flex-direction:column;align-items:center;flex-shrink:0"><span style="width:1px;flex:1;background:{"var(--divider)" if top else "transparent"}"></span>'
            f'{node}<span style="width:1px;flex:1;background:{"var(--divider)" if bottom else "transparent"}"></span></div>')
    icons = f'<span style="margin-left:4px;display:flex">{bell(18, rem)}</span>' if rem else ''
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


ACTIONS = [('ic_node_check', 'Выполнить'), ('ic_wont_do', 'Не буду делать'), ('ic_popup_tomorrow', 'На завтра'), ('ic_widget_all', 'На другой день'), ('ic_list_inbox', 'В список'),
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


# ---------------------------------------------------------------- итерация 41: дата и время в названии

SMART_HL = 'background:var(--accent-soft);color:var(--accent);border-radius:4px;padding:0 2px'


def smart_chip(text):
    """Чип срока из названия: акцентная капсула со значком календаря и крестиком «не разбирать»."""
    return (f'<span style="display:inline-flex;align-items:center;gap:6px;height:32px;padding:0 4px 0 12px;border-radius:16px;background:var(--accent-soft);'
            f'color:var(--accent);font-size:14px;line-height:20px;font-weight:700"><span style="display:flex">{svg("ic_widget_all", 18)}</span>{text}'
            f'<span style="width:20px;height:20px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:var(--accent-soft)">'
            f'<span style="width:12px;height:12px;display:flex">{B.X}</span></span></span>')


def smart_sheet(before, phrase, after, chip_text, fields):
    """Шторка новой задачи: фраза срока в названии подсвечена, под названием чип, поля даты и времени заполнены."""
    card = B.sheet_card(False, width=396)
    title = f'{before}<span style="{SMART_HL}">{phrase}</span>{after}'
    card = card.replace('color:var(--inactive)">Купить билеты на концерт', f'">{title}')
    card = card.replace('margin-top:4px">Описание</div>', f'margin-top:4px">Описание</div><div style="margin-top:8px">{smart_chip(chip_text)}</div>')
    cal, clock = svg('ic_widget_all', 22), svg('ic_time', 22)
    clr = f'<span class="clr" style="position:static">{B.X}</span>'
    filled = ''.join(f'<span class="field">{ic}<span>{t}</span>{clr}</span>' for ic, t in zip((cal, clock), fields))
    card = card.replace(f'<span class="field empty">{cal}<span>Без даты и времени</span></span>', filled)
    # Срок есть — повтор и уведомления доступны.
    card = card.replace('class="tool dim"', 'class="tool"')
    return card.replace('color-mix(in srgb,var(--inactive) 60%,transparent)', 'var(--text-secondary)')


def p_smart_date():
    body = (f'<div class="row" style="align-items:flex-start;gap:28px">{smart_sheet("Созвон ", "завтра в 10:00", "", "Завтра, 10:00", ("пт, 30 окт", "10:00"))}'
            f'{smart_sheet("Отчёт ", "в пятницу", "", "пт, 30 окт", ("пт, 30 окт",))}</div>')
    return doc('Окна', 330, 'Дата и время в названии: фраза подсвечена, под названием чип срока с крестиком', body)


# ---------------------------------------------------------------- итерация 44: вид «Неделя»

def week_task(title, time='', pri='none', done=False, ghost=False, over=False, h=22, fs=12, gap=3, rep=False, emoji=None):
    """Неделя #263: эмодзи, название, начало справа; вся ячейка открывает день."""
    pri = {'medium': 'high', 'low': 'none'}.get(pri, pri)
    high = pri == 'high'
    color = ('var(--week-row-priority-done)' if high else 'var(--week-row-done)') if done else ('var(--week-row-week-priority)' if high else 'var(--text-primary)')
    weight = 800 if high and not done else 700
    em = f'<span style="font-size:{fs}px;opacity:{.35 if done else 1}">{emoji}</span>' if emoji else ''
    return (f'<div style="height:{h}px;margin-top:{gap}px;display:flex;align-items:center;gap:2px;color:{color}">{em}'
            f'<span style="flex:1;min-width:0;font-size:{fs}px;font-weight:{weight};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{title}</span>'
            f'<span style="flex-shrink:0;font-size:10.5px;font-weight:{weight}">{time}</span></div>')


def week_cell(wd, num, tasks, today=False, hidden=0, h=190, widget=False):
    day_label = f"{wd.upper()} {num}" + ((" Сегодня" if widget else " сегодня") if today else "")
    if widget:  # 0.46.0, проверка PM: строка даты виджета — 12 px, высота 18, сегодня жирным без кружка
        more = f'<span style="margin-left:auto;font-size:9px;font-weight:700;color:var(--text-primary);background:var(--accent-soft);border-radius:12px;padding:0 7px;line-height:11px">+{hidden}</span>' if hidden else ''
        # Итерация 47: день недели и число — одна подпись серым; сегодня — жирным чёрным, без кружка. Итерация 48: 9 sp, капсом,
        # остальные дни обычным начертанием, строка даты по высоте текста (не ниже 12), поле над ней 1.
        w = f'<span style="font-size:9px;font-weight:{800 if today else 400};color:{"var(--text-primary)" if today else "var(--text-secondary)"}">{day_label}</span>'
        return (f'<div style="flex:1;min-width:0;height:{h}px;overflow:hidden;box-sizing:border-box;padding:1px 4px 2px;border-right:1px solid var(--divider);border-bottom:1px solid var(--divider)">'
                f'<div style="display:flex;align-items:center;min-height:12px">{w}{more}</div>{"".join(tasks)}</div>')
    # Итерация 55: подпись как в виджете — «ПТ 30» капсом, сегодня жирным; строка даты 20.
    label = f'<span style="font-size:11px;font-weight:{800 if today else 500};color:{"var(--text-primary)" if today else "var(--text-secondary)"}">{day_label}</span>'
    more = f'<span style="margin-left:auto;font-size:11.5px;font-weight:800;color:var(--text-primary);background:var(--accent-soft);border-radius:12px;padding:0 8px">+{hidden}</span>' if hidden else ''
    return (f'<div style="flex:1;min-width:0;height:{h}px;overflow:hidden;box-sizing:border-box;padding:8px 4px;border-right:1px solid var(--divider);border-bottom:1px solid var(--divider)">'
            f'<div style="display:flex;align-items:center;height:20px">{label}{more}</div>{"".join(tasks)}</div>')


def week_calendar(h=190, widget=False):
    """Мини-календарь (0.46.0): без строки месяца и стрелок — месяц листается выбором недели с днями соседнего месяца.
    В виджете (итерация 48): даты 11, сегодня жирным (#284)."""
    fs, today_d = 12, 22
    weeks = [[28, 29, 30, 1, 2, 3, 4], [5, 6, 7, 8, 9, 10, 11], [12, 13, 14, 15, 16, 17, 18], [19, 20, 21, 22, 23, 24, 25], [26, 27, 28, 29, 30, 31, 1], [2, 3, 4, 5, 6, 7, 8]]
    if widget:
        rows = []
        for i, week in enumerate(weeks):
            dates = []
            for j, day in enumerate(week):
                current = i == 4 and day == 30
                adjacent = (i == 0 and j < 3) or i == 5 or (i == 4 and j == 6)
                color = 'var(--inactive)' if adjacent and not current else 'var(--text-primary)'
                dates.append(f'<span style="flex:1;text-align:center;font-size:11px;font-weight:{700 if current else 400};color:{color}">{day}</span>')
            band = 'background:var(--accent-soft);border-radius:12px;' if i == 4 else ''
            rows.append(f'<div style="flex:1;display:flex;align-items:center;{band}">{"".join(dates)}</div>')
        return (f'<div style="flex:1;min-width:0;height:{h}px;box-sizing:border-box;padding:4px;border-bottom:1px solid var(--divider);display:flex;flex-direction:column">'
                f'{"".join(rows)}</div>')
    rows = ''
    for i, w in enumerate(weeks):
        band = 'background:var(--accent-soft);border-radius:12px;' if i == 4 else ''
        cells = ''.join((f'<span style="flex:1;display:flex;justify-content:center"><span style="width:{today_d}px;height:{today_d}px;border-radius:50%;background:var(--accent);color:var(--on-accent);font-size:{fs}px;font-weight:800;display:flex;align-items:center;justify-content:center">{n}</span></span>'
                         if i == 4 and n == 30 else
                         f'<span style="flex:1;text-align:center;font-size:{fs}px;font-weight:600;color:{"var(--inactive)" if (i == 0 and j < 3) or i == 5 or (i == 4 and j == 6) else "var(--text-primary)"}">{n}</span>')
                        for j, n in enumerate(w))
        rows += f'<div style="display:flex;align-items:center;height:26px;{band}">{cells}</div>'
    wk = ''.join(f'<span style="flex:1;text-align:center;font-size:10px;font-weight:600;color:var(--inactive)">{x}</span>' for x in ('пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'))
    return (f'<div style="flex:1;min-width:0;height:{h}px;overflow:hidden;box-sizing:border-box;padding:4px;border-bottom:1px solid var(--divider);display:flex;flex-direction:column;justify-content:space-evenly">'
            f'<div style="display:flex;height:14px;align-items:center">{wk}</div>{rows}</div>')


def week_screen():
    r1 = (f'<div style="display:flex">{week_cell("пн", 26, [week_task("Купить продукты", "10:00", "low", done=True, emoji="🛒"), week_task("Тренировка", emoji="💪"), week_task("Оплатить интернет", "12:00", "medium", over=True, emoji="🏠")])}'
          f'{week_cell("вт", 27, [week_task("Позвонить маме", emoji="❤️"), week_task("Встреча", "15:00", "high", over=True, emoji="💼")])}</div>')
    r2 = (f'<div style="display:flex">{week_cell("пт", 30, [week_task("Отчёт за неделю", "17:00", "high", emoji="💼"), week_task("Кино", emoji="🎬"), week_task("Ужин с Аней", "20:00", emoji="🏠"), week_task("Зарядка", "08:00", "medium", done=True, rep=True, emoji="💪"), week_task("Позвонить врачу", "", "low", emoji="❤️"), week_task("Купить билеты", emoji="🎬")], today=True, hidden=2)}'
          f'{week_cell("сб", 31, [week_task("Уборка", "11:00", emoji="🏠"), week_task("Созвон с командой", "10:00", "medium", ghost=True, emoji="💼")])}</div>')
    r3 = f'<div style="display:flex">{week_cell("вс", 1, [week_task("План на неделю", emoji="💼")])}{week_calendar()}</div>'
    return frame(f'<div style="border-top:1px solid var(--divider)">{r1}{r2}{r3}</div>{nav_bar(2)}', 700)


def p_week_view():
    return doc('Список и виджет', 740, 'Вид «Неделя»: компактные строки с фоном по приоритету, «+N», мини-календарь без месяца, нижняя панель', f'<div class="row" style="align-items:flex-start">{week_screen()}</div>')


# ---------------------------------------------------------------- итерация 49: «Не буду делать» и удаление повторов

WONT = lambda size=22: svg('ic_wont_do', size)
WONT_ROW = lambda n: (f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary)">{WONT(24)}'
                      f'<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary)">Не буду делать</span><span style="font-size:14px">{n}</span></div>')


def choice_dialog(title, text, buttons):
    """Окно выбора (ChoiceDialog): заголовок, пояснение, кнопки столбиком — red (`danger`), black (`accent`), рамка, просто текст.
    Кнопка — (текст, вид) или (текст, вид, подсказка): с подсказкой она бледная (35%) и не нажимается, подсказка — под ней (итерация 50)."""
    def b(t, kind, hint=None):
        dim = ';opacity:.35' if hint else ''
        note = (f'<span style="font-size:12px;line-height:16px;color:var(--text-secondary);text-align:center;margin:-2px 0 2px">{hint}</span>') if hint else ''
        style = {'danger': 'background:var(--danger);color:var(--on-accent)', 'primary': 'background:var(--accent);color:var(--on-accent)',
                 'outline': 'border:1px solid var(--divider);color:var(--text-primary)', 'plain': 'color:var(--text-primary)'}[kind]
        icon = f'<span style="display:flex;margin-right:8px">{WONT(20)}</span>' if t == 'Не буду делать' else ''
        return f'<span style="min-height:50px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;font-weight:700;{style}{dim}">{icon}{t}</span>{note}'
    return (f'<div style="width:340px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden;flex-shrink:0">'
            f'<div style="padding:24px 24px 4px"><div style="font-size:22px;line-height:28px;font-weight:700">{title}</div>'
            f'<div style="font-size:14px;line-height:20px;color:var(--text-secondary);margin-top:8px">{text}</div></div>'
            f'<div style="display:flex;flex-direction:column;gap:8px;padding:16px">{"".join(b(*x) for x in buttons)}</div></div>')


def p_choice_dialog():
    series = choice_dialog('Удалить серию повторов?', 'Задача «Стирка» повторяется. Удалятся все её будущие повторы, выполненные останутся в истории.<br><br>Чтобы убрать только этот повтор, нажмите «Не буду делать».',
                           [('Удалить серию', 'danger'), ('Не буду делать', 'outline'), ('Отмена', 'plain')])
    unclosed = choice_dialog('Есть незакрытые повторы', 'Перед вс, 22 ноя остались повторы «Стирка»: вс, 08 ноя, вс, 15 ноя. Что с ними сделать?',
                             [('Пропустить все', 'primary'), ('Выполнить все', 'outline'), ('Отмена', 'plain')])
    change = choice_dialog('Изменить повторяющуюся задачу', '«Стирка»: вс, 08 ноя → пн, 09 ноя 18:00. Что перенести?',
                           [('Текущий повтор', 'primary'), ('Все повторы с этого момента', 'outline'), ('Отмена', 'plain')])
    future = choice_dialog('Изменить повторяющуюся задачу', '«Стирка»: вс, 22 ноя → пн, 23 ноя 18:00. Что перенести?',
                           [('Текущий повтор', 'outline', 'Сначала закройте ближайший повтор — вс, 08 ноя'), ('Все повторы с этого момента', 'primary'), ('Отмена', 'plain')])
    return doc('Окна', 880, 'Окно выбора: «Удалить серию повторов?», «Есть незакрытые повторы» (0.49) и «Изменить повторяющуюся задачу» у ближайшего и будущего раза (0.50)',
               f'<div class="row" style="align-items:flex-start;flex-wrap:wrap">{series}{unclosed}{change}{future}</div>')


def wont_row(left, title, sub, rep=False):
    lines = left if isinstance(left, list) else [left]
    t = ('<div style="width:56px;text-align:right;font-size:11px;line-height:15px;font-weight:700;color:var(--text-secondary);display:flex;flex-direction:column;justify-content:center;flex-shrink:0">'
         + ''.join(f'<span>{x}</span>' for x in lines) + '</div>')
    badge = f'<span style="display:flex;color:var(--inactive);margin-left:6px">{svg("ic_tool_repeat", 16)}</span>' if rep else ''
    return (f'<div style="display:flex;height:58px;padding-right:8px;align-items:center;gap:6px">{t}<span style="color:var(--inactive);display:flex">{WONT(20)}</span>'
            f'<div style="flex:1;min-width:0;margin:3px 0;height:52px;border-radius:var(--radius-md);background:var(--surface);display:flex;align-items:center;padding:0 12px;opacity:.85">'
            f'<div style="flex:1;min-width:0"><div style="font-size:14px;font-weight:700;color:var(--text-secondary)">{title}</div>'
            f'<div style="font-size:12px;line-height:14px;color:var(--text-secondary)">{sub}</div></div>{badge}</div></div>')


def wontdo_screen():
    top = (f'<div style="display:flex;align-items:center;padding:8px 4px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("back")}</span>'
           f'<span style="font-size:22px;line-height:28px;font-weight:700">Не буду делать</span></div>')
    rows = (day_header('Сегодня') + wont_row(['вс, 08 ноя', '14:30'], 'Стирка', 'Дом · повтор', rep=True) + wont_row(['—'], 'Записаться к стоматологу', 'Входящие')
            + day_header('Вчера') + wont_row(['чт, 05 ноя', '10:00'], 'Пробежка', 'Спорт · повтор', rep=True))
    return frame(f'{top}{rows}', 680)


def wontdo_task():
    """Задача «Не буду делать»: шторка только для чтения (как TrashTask), плашка с ✕, в «⋮» — «Вернуть в работу» и «Удалить»."""
    return readonly_task(True, base=wontdo_screen(), icon=WONT(20), banner='Не буду делать · отмечено пт, 06 ноя', items=('Вернуть в работу', 'Удалить'))


def day_sheet():
    """Шторка дня #265: компактная дата и вытянутый плюс сверху, без относительных подписей и количества."""
    rows = (frow('10:30', 'var(--inactive)', 'Telegram', done=True, rep=True, top=False, flat=True)
            + frow('19:30', 'var(--inactive)', 'Кальян', period=True, end='21:30', flat=True)
            + frow('21:30', 'var(--inactive)', 'Футбол', period=True, end='23:30', flat=True)
            + frow('23:00', 'var(--inactive)', 'Цветы', rep=True, bottom=False, rem='at', flat=True))
    head = (f'<div style="display:flex;align-items:center;gap:8px;padding:4px 12px;min-height:56px">'
            f'<span style="flex:1;font-size:14px;line-height:20px;font-weight:700">СБ 07 ноя</span>'
            f'<span style="width:64px;height:32px;border-radius:var(--radius-xl);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("add", 22)}</span></div>')
    sheet = (f'<div style="width:396px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden">'
             f'{head}<div style="padding:8px 0 8px 4px;background:var(--surface)">{rows}</div></div>')
    return (f'<div style="padding:24px;border-radius:var(--radius-xl);background:var(--scrim);display:inline-block">{sheet}</div>')


PREVIEWS = {
    'DaySheet': lambda: doc('Напоминания и виджет', 520, 'Шторка дня: компактная дата и плюс сверху, лента дня без нижней кнопки', day_sheet()),
    'WeekView': p_week_view,
    'ChoiceDialog': p_choice_dialog,
    'ScreenWontDo': lambda: screens(wontdo_screen, 730, 'Экран «Не буду делать»: по дням отметки, вместо галочки — ✕'),
    'WontDoTask': lambda: doc('Окна', 730, 'Задача «Не буду делать»: только чтение, «⋮» — вернуть в работу или удалить', f'<div class="row" style="align-items:flex-start">{wontdo_task()}</div>'),
    'SmartDate': p_smart_date,
    'UpdateDialog': p_update_dialog, 'ListPriorityDialogs': p_list_priority_dialogs, 'CustomDialogs': p_custom_dialogs, 'AlertDialogs': p_alert_dialogs,
    'ChecklistRows': p_checklist, 'ChipsAndButtons': p_chips_fab, 'SettingsRow': p_switch_rows,
    'Headers': p_headers, 'EmptyTasks': p_empty, 'TaskRow': p_task_row, 'ListRows': p_list_rows, 'SearchField': p_search_field, 'BottomBar': p_bottom_bar,
    'ReminderStrip': p_reminder_strip, 'Snooze': p_snooze, 'SelectionMode': p_selection, 'ScreenSelection': lambda: screens(all_selected, 730, 'Экран «Все» в режиме выбора: сверху «Выбрано: N», снизу лента действий'), 'ScreenTrash': lambda: screens(trash_screen, 730, 'Экран «Корзина»: удалённые задачи как в списке, «Восстановить все» и «Удалить все»'), 'TrashTask': lambda: doc('Окна', 730, 'Задача в корзине: только чтение, «⋮» — восстановить или удалить навсегда', f'<div class="row" style="align-items:flex-start">{readonly_task(True)}</div>'), 'DangerDialog': p_danger, 'OverdueHeader': p_overdue_header, 'UndoSnackbar': p_snackbar, 'RecentSearches': p_recent_searches, 'TaskDraft': p_draft, 'ReminderNotification': p_notification, 'Widget': p_widget_bar, 'WidgetBar': p_widget_bar_only, 'WidgetWeek': p_widget_week,
    'ScreenAll': lambda: screens(all_screen, 730, 'Экран «Предстоящее» (0.55, бывш. «Все»): заголовок, лента по дням со шкалой и нижней панелью'),
    'ScreenInbox': lambda: screens(inbox_screen, 730, 'Экран «Входящие»: плоский список, слева день и время'),
    'ScreenLists': lambda: screens(lists_screen, 850, 'Экран «Списки» (0.56): карточки групп, строка значков внизу'),
    'ScreenSomeday': lambda: screens(someday_screen, 730, 'Экран «Когда-нибудь»: задачи со списком без даты, группами по спискам (0.52)'),
    'ScreenNoList': lambda: screens(nolist_screen, 730, 'Экран «Без списка»: задачи с датой без списка, по дням (0.52)'),
    'ScreenProject': lambda: screens(project_screen, 730, 'Экран списка: заголовок со значком, «⋮» и плавающая «+»'),
    'ScreenSearch': lambda: screens(search_screen, 730, 'Экран «Поиск»: запрос, найденное, выполненные'),
    'ScreenCompleted': lambda: screens(completed_screen, 730, 'Экран «Выполненные»: по дням, новые сверху'),
    'ScreenSettings': lambda: screens(settings_screen, 900, 'Экран «Настройки»'),
    'ScreenNewTask': lambda: screens(sheet_screen, 730, 'Окно создания задачи поверх экрана «Все»'),
}

README_MORE = {
    'SmartDate': ('Дата и время в названии: подсветка фразы и чип срока (0.41).', 'Пока человек печатает название, явная фраза срока («завтра в 10:00», «в пятницу», «через 3 дня», «15 ноября», «15.11», «в 18», «в 9 утра») подсвечивается: цвет `accent` на фоне `accent-soft`. Под названием чип: высота `size-smart-chip` (32), капсула `accent-soft`, значок календаря `size-smart-chip-icon` (18), текст `body-medium` Bold `accent` («Завтра, 10:00»), справа кружок `size-smart-chip-close` (20) с крестиком — не разбирать дату в названии этой задачи (фраза остаётся обычным текстом, срок возвращается). Поля даты и времени заполняются сразу. При сохранении фраза уходит из названия. Только время — сегодня или завтра, если время прошло; только дата — весь день. Общий выключатель — «Дата и время в названии» в настройках. Источник: `designsystem/components/SmartDateChip.kt`, `domain/smartdate/SmartDate.kt`, `ui/feature/task/SmartTitle.kt`.'),
    'SelectionMode': ('Режим выбора: верхняя строка «Выбрано: N», лента действий, выбранная плашка (0.40).', 'Включается долгим нажатием на плашку (будущий повтор выбрать нельзя), дальше нажатие на плашку или кружок отмечает и снимает. Сверху строка `surface` высотой `size-selection-bar` (56): «✕» (закрыть режим, как и «Назад»), «Выбрано: N» (`title-large` Bold), «Выбрать все» (`accent`). Внизу на месте панели вкладок лента: высота `size-nav-bar`, радиус `radius-nav`, тень; кнопки не уже `size-ribbon-button` (76) и высотой `size-ribbon-button-height` (56): значок 24 и подпись `label-small` Bold; ряд прокручивается пальцем, у правого края затухание. Действия: «Выполнить», «Не буду делать» (итерация 49), «На завтра», «На другой день» (календарь), «В список», «Приоритет», «Вернуть», «Удалить» (`danger`); неподходящие бледные (35%). В корзине: «Восстановить» и «Удалить навсегда» (с подтверждением `DangerDialog`). Выбранная плашка: фон `selected`, рамка `accent` 2, в кружке чёрная галочка. После действия режим закрывается, внизу `UndoSnackbar` («Выполнено: 3 · Отмена» и т. п.). Источник: `designsystem/components/Selection.kt`, `ui/feature/bulk/SelectionFrame.kt`, `BulkActions.kt`. «Удалить», когда среди выбранных есть повторяющиеся, сначала открывает `ChoiceDialog` «Удалить выбранные задачи?» («Удалить» / «Не буду делать» / «Отмена»).'),
    'ScreenSelection': ('Экран «Все» в режиме выбора (0.40).', 'Строка «Выбрано: N» поверх верха экрана, лента действий на месте нижней панели («+» и вкладки на это время скрыты). Список прокручивается выше ленты. То же во «Входящих», списках, «Поиске», «Выполненных» и «Корзине»; в виджете режима нет.'),
    'ScreenTrash': ('Экран «Корзина» (0.39).', 'Сверху «назад» и название `title-large` Bold; под ним «ЗАДАЧ: N» (`label-medium` Bold, `text-secondary`), справа «Восстановить все» (`accent`) и «Удалить все» (`danger`). Ниже плашки как в обычном списке (слева срок, справа список) с мелкой подписью «удалена 28 окт» (`body-small`, `text-secondary`); строка выше обычной на `size-trash-row-extra` (14). Новые удалённые сверху. Нажатие открывает задачу только для чтения (`TrashTask`); кружок ничего не отмечает. Пусто: «Корзина пуста» и подсказка по центру. Вход — строка «Корзина» на экране «Списки» под «Выполненными»: значок корзины, название, число задач (при нуле числа нет). Источник: `ui/feature/trash/TrashScreen.kt`.'),
    'DaySheet': ('Шторка дня поверх рабочего стола (#263, #265).',
                 """Белая плоская шторка без верхнего маркера и указателя текущего времени. В заголовке только дата «СБ 07 ноя» body-medium 14 sp Bold, прошедший день — overdue. Относительных подписей Сегодня/Вчера/Завтра и количества задач нет. Справа вытянутый чёрный плюс WIDGET_ADD_W/H 64×32, область нажатия высотой toolButton 50 dp; подпись для чтения с экрана содержит выбранную дату. Нижней кнопки нет. Плюс доступен в пустом дне. Строки, выполнение/открытие задач, прокрутка и закрытие прежние. Полный DayScreen и недельная сетка не меняются.
Источник: ui/feature/week/DaySheet.kt, TaskPopupActivity."""),
    'ChoiceDialog': ('Окно выбора: заголовок, пояснение и кнопки столбиком на всю ширину (0.49).',
                     '''По центру над затемнением, радиус `radius-nav`, ширина не больше `size-dialog-max-width`. Кнопки высотой не меньше `size-tool-button` (50), радиус `radius-lg`, промежуток `space-xs`: красная (`danger`), чёрная (`accent`), с рамкой `divider` и просто текст. Нажатие сразу выполняет действие, «Назад» и нажатие мимо — как «Отмена».
Где: «Удалить» у повторяющейся задачи — «Удалить серию повторов?» («Удалить серию» / «Не буду делать» / «Отмена»); «Не буду делать» у будущего повтора с незакрытыми повторами перед ним — «Есть незакрытые повторы» («Пропустить все» / «Выполнить все» / «Отмена», как в TickTick); «Удалить» в режиме выбора с повторяющимися. Обычная задача удаляется без окна, с `UndoSnackbar`. Источник: `ui/feature/task/SheetDialog.kt` (`ChoiceDialog`).
Итерация 50 (как в TickTick): новая дата у повторяющейся — «Изменить повторяющуюся задачу» («Текущий повтор» / «Все повторы с этого момента» / «Отмена»). Окно — по «Готово» в окне даты, если дата или время изменились, и по крестику у времени; «←» и «Назад» в окне даты, дата из названия, крестик у даты — без окна. «Текущий повтор» — только у ближайшего раза: раз становится обычной задачей на новую дату, серия едет на следующий раз, внизу `UndoSnackbar` «Перенесено на пн, 09 ноя». У будущего раза эта кнопка бледная (35%, не нажимается), под ней подсказка «Сначала закройте ближайший повтор — вс, 08 ноя» (`body-small`, `text-secondary`); чёрная — «Все повторы с этого момента»: серия делится, до этого раза — по-старому, с него — с новой даты. «Отмена» возвращает прежнюю дату.
Перенос серии (0.51.1): правило с одним днём — одно число месяца, один день недели, одна дата года — меняется под новую дату («каждый месяц, 10-го» → на 4 ноя → «каждый месяц, 4-го»); у правила с несколькими днями («вт, чт») дата встаёт на ближайший подходящий день, сверху `UndoSnackbar` без кнопки: «Повтор «Каждую неделю: вт, чт» — ближайший раз чт, 05 ноя». Так же — дата из названия (без подсказки). «На завтра» и «На другой день» в режиме выбора и «Перенести просроченные» у повторяющейся переносят только этот раз.'''),
    'ScreenWontDo': ('Экран «Не буду делать» (0.49).', 'Вход — строка на «Списках». Как «Выполненные»: «назад» и заголовок, дни отметки (новые сверху), плашки `TaskRow` серые и зачёркнутые, вместо галочки — ✕ в круге (`ic_wont_do`, `inactive`): нажатие на него возвращает задачу в работу («Задача возвращена · Отмена»), нажатие на плашку открывает `WontDoTask`. У раза серии — знак повтора. Здесь же задачи со статусом Won\'t Do из TickTick. Источник: `ui/feature/wontdo/`.'),
    'WontDoTask': ('Задача «Не буду делать»: шторка только для чтения (0.49).', 'Как `TrashTask`: плашка `selected` «Не буду делать · отмечено пт, 06 ноя» со значком ✕, вместо круга «выполнено» — ✕ (`inactive`). «⋮»: «Вернуть в работу» (раз серии возвращается обычной задачей, серия не меняется) и «Удалить» (`danger`, в корзину без подтверждения, с «Отменой»). Источник: `ui/feature/wontdo/WontDoTask.kt`, `ui/feature/trash/TrashTaskScreen.kt` (`ReadonlyKind.WontDo`).'),
    'TrashTask': ('Задача в корзине: та же шторка, но только для чтения (0.39).', 'Над шторкой плашка `selected` «В корзине · удалена 28 окт» со значком корзины. Название, описание, срок, список, значки повтора и уведомлений, чек-лист и подзадачи показываются как есть, править нельзя: кнопок панели, крестиков у полей и галочки «Готово» нет. «⋮» справа от названия: «Восстановить» и «Удалить навсегда» (`danger`, после подтверждения `DangerDialog`). Затемнение и «Назад» закрывают. Восстановленная задача возвращается в прежний список (если его нет — во «Входящие») вместе с подзадачами и чек-листами, подсказка `UndoSnackbar` «Задача восстановлена · Отмена». Источник: `ui/feature/trash/TrashTaskScreen.kt`. Та же шторка показывает задачу «Не буду делать» (`WontDoTask`).'),
    'DangerDialog': ('Окно необратимого действия: красная кнопка (0.39).', 'Строится на `SheetDialog` с `danger = true`: вторая кнопка залита `danger` вместо `accent`. «Удалить навсегда?» у одной задачи из корзины и «Удалить все навсегда?» у всей корзины; первая кнопка «Отмена». «Восстановить» и «Восстановить все» подтверждения не просят, но дают подсказку «Отмена».'),
    'OverdueHeader': ('Подпись «Просрочено · N» над прошедшими днями: стрелка «свернуть» и значок «перенести на сегодня» (0.37).',
                      '''Стоит над первым прошедшим днём в ленте «Все» приложения и на вкладке «Все» виджета; во «Входящих» и списках её нет. Текст `label-large` Bold (в виджете 14 sp), цвет `overdue`; N — невыполненные задачи прошедших дней (сегодняшние с прошедшим временем не входят).
Справа от текста стрелка 24 (в виджете `size-widget-fold-icon` 20): вниз — раскрыто, вправо — свёрнуто. Нажатие на текст со стрелкой сворачивает все прошедшие дни, счётчик остаётся. Состояние одно на приложение и виджет, запоминается между запусками.
Рядом значок «→|» (`ic_move_today`): переносит все невыполненные просроченные на сегодня, время дня то же. Если невыполненных нет, значка нет. После переноса — подсказка `UndoSnackbar`; из виджета она показывается в прозрачном окне поверх рабочего стола (~4 с).
Источник: `designsystem/components/Common.kt` (`OverdueHeader`), `widget/TasksWidget.kt` (`OverdueTitle`).'''),
    'UndoSnackbar': ('Подсказка внизу экрана с кнопкой «Отмена»: белая полупрозрачная карточка со значком (0.38).',
                     '''Белая карточка `surface` на 88% (сквозь неё виден список или рабочий стол), рамка `divider` 1 (тени нет: сквозь полупрозрачную карточку она просвечивает), радиус `radius-xl` (16), высота от `size-snackbar-min-height` (56), поля слева `space-md`, справа `space-xxs`.
Слева значок `size-snackbar-icon` (22) цвета `text-secondary`: галочка («Задача выполнена»), повтор («Следующий повтор: сб, 17 окт»), корзина («Задача удалена»), стрелка к черте («Перенесено на сегодня: N»), круговая стрелка («Задача возвращена»), календарь («Перенесено на пн, 09 ноя» — перенос одного повтора, 0.50). Справа чёрная «Отмена» (`accent`, Bold) с зоной нажатия от 40.
Размытия фона нет: на старых телефонах Android оно недоступно. Висит ~4 с, новая заменяет прежнюю. В приложении стоит над нижней панелью с зазором `space-sm`; из виджета и уведомлений появляется в прозрачном окне поверх рабочего стола с отступом от низа `space-lg`; нажатие мимо закрывает окно сразу (отмена тогда уже невозможна).
Тексты: «Задача удалена» без вопроса «Удалить?» (0.36), «Перенесено на сегодня: N» (0.37), «Повтор «Каждую неделю: вт, чт» — ближайший раз чт, 05 ноя» (0.51.1, без кнопки: серия встала на ближайший день правила). «Отмена» возвращает всё как было (удалённое — вместе с подзадачами и чек-листами). Источник: `designsystem/components/UndoSnackbar.kt`, `ui/common/TaskListUi.kt` (`showUndo`).'''),
    'RecentSearches': ('Недавние запросы в поиске: пока поле пустое — до 5 строк (0.38).',
                       '''Блок виден, пока поле поиска пустое и есть запросы; иначе — подсказка «Начните вводить — задачи появятся сразу». Заголовок «НЕДАВНИЕ» (`label-medium` Bold, `text-secondary`), справа «Очистить» (`accent`, Bold) стирает все.
Строка высотой `size-recent-row` (48): слева значок «часы» `size-recent-icon` (22) цвета `text-secondary`, запрос `body-large` Medium, справа крестик в зоне нажатия 48 убирает один запрос. Нажатие на строку ставит запрос в поле и сразу ищет.
Запрос запоминается, когда открыли найденную задачу или нажали «поиск» на клавиатуре (не на каждую букву). Последние 5, новые сверху, без повторов (регистр не важен), хранятся на телефоне. Источник: `ui/feature/search/SearchScreen.kt`, `RecentSearches.kt`.'''),
    'TaskDraft': ('Черновик новой задачи: закрыли карточку, не сохранив, — текст вернётся при следующем «+» (0.38).',
                  '''Если карточку новой задачи закрыли без сохранения (нажатием мимо, «Назад», смахнули окно), а в названии что-то набрано, название и описание запоминаются. Следующее «+» (с любого экрана и из виджета) открывает шторку с этим текстом: название выделено (`accent-soft`), любая буква заменит его, «Удалить» на клавиатуре сотрёт.
Дата, список, приоритет, повтор и чек-лист не запоминаются. Черновик один, без срока годности; стирается, когда задачу сохранили или название очистили. Источник: `ui/feature/task/TaskDraft.kt`, `QuickAddViewModel.kt`.'''),
    'ReminderNotification': ('Обычное уведомление-напоминание: название, срок, кнопки «Выполнено», «Не буду делать» и «Отложить» (0.53).',
                             '''Системное уведомление Android (канал «Напоминания»). Название задачи, под ним срок и список. Две кнопки: «Выполнить» и «На завтра» (0.36): срок переносится на завтра, время дня то же; у задачи без времени — только день.
Рисует система, поэтому вид зависит от телефона; значок приложения — `ic_notification`. Источник: `notifications/AndroidReminders.kt`. Итерация 49: между «Выполнить» и «На завтра» — «Не буду делать»: у повторяющейся закрывает этот раз, серия едет дальше. Итерация 50: «На завтра» у повторяющейся переносит только этот раз (обычной задачей на завтра в то же время), серия остаётся на своём расписании; окна нет.
Итерация 53 (T-84): кнопки — «Выполнено», «Не буду делать», «Отложить». «На завтра» вошло в «Отложить» вариантом «Завтра». Меню в самом уведомлении Android не показывает, поэтому «Отложить» открывает окно поверх экрана (`Snooze`).'''),
    'UpdateDialog': ('Окно обновления поверх рабочего стола: проверка, загрузка, установка, ошибки.',
                     '''Строится на `SheetDialog`. Над заголовком круглый значок `size-dialog-badge` (48): чёрный (`selected`, значок `accent`) для хороших исходов, красный (`danger` 12%) для ошибок.
Состояния: проверка (полоса без процента), «Обновлений нет», «Доступно обновление» («Закрыть» / «Загрузить», версия и размер), загрузка (полоса, «7,2 из 16 МБ», процент, одна кнопка «Отмена»),
«Обновление загружено» («Не сейчас» / «Обновить»), ошибки «Не удалось проверить» и «Не удалось загрузить» («Закрыть» / «Повторить»), «Нужно разрешение» («Закрыть» / «Разрешить»), установка.
Во время загрузки «Назад» и нажатие мимо окна игнорируются: останавливает только «Отмена». Полоса: высота 4, дорожка `divider`, заливка `accent`. Источник: `ui/feature/update/UpdateDialog.kt`.'''),
    'ListPriorityDialogs': ('Окна «Список» и «Приоритет»: один вариант, подсветка выбранного и галочка справа.',
                            '''Окно «Список»: слева эмодзи списка (16). Первый вариант — «без списка» (итерация 52.1): у задачи без даты «📥 Входящие», у задачи с датой «📌 Без списка»; в новой задаче вариант меняется сразу, как поставили или убрали дату. В режиме выбора — «📌 Без списка», если дата есть у всех выбранных. Окно «Приоритет» (итерация 47) — только при правке нескольких задач: «Высокий» (флажок `ring-high`) и «Обычный» (`inactive`); в шторке одной задачи окна нет, кнопка — переключатель.
Строка не ниже `size-dialog-row` (52), выбранная залита `selected`. Изменение применяется только по «Готово». Источник: `TaskFieldMenus.kt`.'''),
    'CustomDialogs': ('Окна «Вручную» (своё уведомление) и «Произвольно» (свой повтор) с счётчиками.',
                      '''«Вручную»: три строки «Дней до срока / Часов / Минут» (у задачи на весь день: за сколько дней, час, минуты) со счётчиком «− 02 +»; снизу чёрным «Сработает: чт, 29 окт, 15:00».
Минуты идут шагом 5, дни до 30. «Произвольно»: чипы «День / Неделя / Месяц / Год», строка «Каждые − N +» (1–99), у недели ещё чипы дней недели, внизу чёрная расшифровка («Каждые 2 недели: вт, чт»).
Кнопки «Отмена» и «Готово», как у всех окон. Источник: `TaskFieldMenus.kt`, `RepeatDialog.kt`.'''),
    'AlertDialogs': ('Окна списка: создание (0.56 — с эмодзи), удаление, эмодзи.',
                     '''Создание списка и папки и правка папки (итерация 56, T-127, `ListEditDialog` на `SheetDialog`): заголовок «Новый список» / «Новая папка» / «Папка»; слева плашка эмодзи `size-emoji-button` (56, `selected`, радиус `radius-lg`, в углу стрелка `size-emoji-button-mark`) — нажатие открывает все эмодзи; справа поле названия (`title-medium` Bold, фон `background`, рамка `accent`, радиус `radius-lg`). Ниже «ЭМОДЗИ» — быстрый выбор из 15 (`size-emoji-quick` 38, выбранный — рамка на `selected`) и «…» (все эмодзи); у списка — «ПАПКА»: чипы «Без папки» и папки с эмодзи. Кнопки «Отмена» и чёрная «Создать» («Сохранить»), бледная, пока нет названия. 📁 сохраняется как «без эмодзи».
Удаление — `AlertDialog` Material 3 с цветами из токенов (кнопка `danger`). Эмодзи (итерация 47, `EmojiPickerDialog`, окно `SheetDialog`) — сетка популярных по группам (Дела, Работа, Дом, Здоровье, Деньги, Отдых; по 8), выбранное в рамке `accent` на `selected`;
под сеткой поле «Свой эмодзи — вставь с клавиатуры»: берётся первый эмодзи из введённого. Окон «Цвет списка» и «Значок списка» больше нет.'''),
    'ChecklistRows': ('Чек-лист и подзадачи в шторке созданной задачи.',
                      '''Пункт: галочка с зоной нажатия `size-checkbox-touch` (40), текст `body-medium`, справа «⋮» (выше, ниже, переименовать, удалить). Отмеченный — чёрная галочка, текст зачёркнут `text-secondary`.
Под пунктами поле «Добавить пункт» (подсказка `inactive`, отступ слева 40). Подзадачи из TickTick — под подписью «ПОДЗАДАЧИ», с плашкой приоритета, если он есть; нажатие открывает подзадачу.'''),
    'ChipsAndButtons': ('Плавающая «+», чипы и кнопки Material 3.',
                        '''Плавающая «+» на экранах списка и «Входящие»: 56, радиус 16, `accent`, значок `on-accent`. Чип: высота 32, радиус `radius-md`; выбранный залит `accent`, текст `on-accent`; невыбранный белый с рамкой `divider`.
Кнопки: заливка `accent` (главное действие), контур `divider` с текстом `accent` (проверить, сохранить), контур с `danger` (очистить). Высота 40, радиус полный.'''),
    'SettingsRow': ('Строка настройки: название, пояснение, переключатель.',
                    '''Название `body-large` SemiBold, пояснение `body-small` `text-secondary`, справа переключатель Material 3 (включён — `accent` с белым бегунком; выключен — рамка `inactive`). Предупреждение — `overdue`, действие под ним — заливной кнопкой.
Секции разделены линией `divider`; заголовок секции — `title-medium` SemiBold.'''),
    'Headers': ('Заголовки: экрана, раздела, дня и день в виджете.',
                '''Экран: `headline-medium` Bold, поля `space-md` и `space-sm`. Раздел: `label-medium` Bold прописными, `text-secondary`, красным для «Просрочено» (с 0.37 над прошедшими днями стоит `OverdueHeader`). День в ленте: `label-large` Bold и белая метка «Сегодня / Завтра / Вчера» (`label-small`, радиус `radius-sm`);
прошедший день с невыполненными задачами красный. В виджете: 14 sp и метка 12 sp, у «Сегодня» справа чёрная линия прогресса (высота 4) и «3 из 7».'''),
    'EmptyTasks': ('Пустой экран: пунктирная подсказка, силуэты плашек и стрелка к «+».',
                   '''Подсказка — рамка из штрихов `inactive` (штрих 6, толщина `size-stroke`), радиус `space-sm`, внутри `title-medium` ExtraBold и `body-medium`. Силуэты плашек высотой `size-ghost-row` (44): рамка галочки и серая полоска `divider` длиной 70%, 50%, 62%.
Стрелка — чёрная кривая к кнопке «+», толщина `size-stroke`. На экране списка, где «+» плавает, стрелку поднимают над кнопкой.'''),
    'TaskRow': ('Плашка TaskRow в поиске и «Выполненные»: квадратная галочка, эмодзи списка, название, значки, время.',
                '''Это прежний вид плашки, ленты «Все», «Входящие» и списков им больше не пользуются (там `TimelineRow`). Галочка — квадрат `size-checkbox` (18, радиус `radius-sm`) с рамкой цвета приоритета, зона нажатия 40; отмеченная — заливка `accent` и белая галочка.
Название `body-medium` SemiBold, под ним `body-small` — где найдено (поиск, найденное подсвечено `accent-soft`). Справа повтор 14, колокольчик 18 и время столбиком (`label-medium` Bold). Плашка `surface`, радиус `radius-md`, щель между плашками `size-card-gap` (2).'''),
    'ListRows': ('Строки «Списков» в карточках (0.56): папка с эмодзи, списки, архив.',
                 '''Итерация 56 (T-123, T-125, T-126): строки лежат в белых карточках (`surface`, радиус `radius-lg`, поля `space-sm` по бокам, между карточками `space-sm`); скругляются только верх первой и низ последней строки, между строками линия `divider` от начала названия. Строка не ниже `size-list-row` (52): значок или эмодзи в поле `size-list-square-large` (24), название `body-large` SemiBold, справа число открытых задач `body-medium` SemiBold `text-secondary` — только если больше нуля.
Папка: эмодзи (новое — 0.56; по умолчанию 📁), название, сумма задач её списков, стрелка (вниз — развёрнута, вправо — свёрнута) и «⋮» («Изменить» — окно «Папка» с названием и эмодзи, «Удалить»). Списки внутри папки сдвинуты на `space-lg`. Архив: «Выполненные» (галочка), «Не буду делать» (`ic_wont_do`), «Корзина» (`ic_tool_delete`).'''),
    'SearchField': ('Поле поиска: подсказка «Поиск задач», запрос, очистка.',
                    '''Поле Material 3: фон `surface`, радиус `radius-lg`, высота 56, без линии снизу. Слева лупа `text-secondary`; справа крестик, когда есть запрос. Курсор `accent`. При открытии раздела курсор сразу в поле и поднята клавиатура. Найденное в плашках подсвечено `accent-soft`.'''),
    'WeekView': ('Вид «Неделя»: сетка 2×4, семь дней и мини-календарь.',
                 '0.73 / #261: текущая дата содержит «сегодня» тем же начертанием, без цветовой подсветки. Строка без плашки, время слева, два зазора по 2 dp, значок 14 dp, название и эмодзи. Активный высокий приоритет — week-row-week-priority и ExtraBold 800; выполненный бледный без зачёркивания. Пустая область под задачами открывает день, время и значок переключают выполнение, остальная строка открывает задачу; будущий повтор открывает исходную задачу.'),
    'BottomBar': ('Нижняя панель приложения (0.55): «Предстоящее» или «Неделя» · «+» · «Списки».',
                  '''Закреплённая панель (0.44): лежит внизу на всю ширину, без капсулы и тени, сверху линия `divider` `size-hairline`, фон `surface` доходит до низа экрана. Высота `size-nav-bar` (56).
Итерация 55 (T-119, решение PM): три места. Слева — «Предстоящее» (`ic_widget_all`) или «Неделя» (`ic_tab_week`): удержание открывает меню из двух пунктов с галочкой у текущего, выбор запоминается (с ним и стартовый экран), значок меняется; под значком точка `size-nav-hint-dot` (`text-secondary`, 60 %) — подсказка про удержание. Посередине «+»: `size-nav-add-w` × `size-nav-add-h` (72×44), радиус `radius-xl`, `accent`, значок 24 `on-accent`. Справа — «Списки» (`ic_widget_list`): внутри «Входящие», «Поиск» и «Настройки»; нажатие из любого из них возвращает к самим «Спискам».
Место вкладки — `size-nav-tab-w` × `size-nav-tab-h` (72×40), радиус `radius-lg`; выбранное — подложка `selected`, значок `accent`, остальные `text-secondary`. Значки — как в виджете (T-122).
Число неразобранных «Входящих» (итерация 52) — на «Списках», Material Badge: фон `accent`, цифра `on-accent`; при нуле кружка нет.'''),
    'Snooze': ('«Отложить» (итерация 53, T-84; 0.54 — в окне задачи): меню над полоской, окно из уведомления и из окна задачи; переносит срок самой задачи.',
               '''Варианты у задачи со временем: «Через 1 час», «Через 3 часа», «Вечером» (19:00), «Завтра» (в то же время), «Выбрать дату и время…»; на весь день: «Завтра», «Послезавтра», «В субботу», «Через неделю», «Выбрать дату…» — задача остаётся без времени.
Новый срок — от старого срока задачи (у периода — от начала, период едет целиком); если вышло в прошлом — от «сейчас» (часы с округлением до 5 минут, дни — от сегодня). «Вечером» скрыт, если задача уже на 19:00 или позже или 19:00 прошло; «В субботу» — если совпадает с «Завтра» или «Послезавтра». У повторяющейся переносится только этот раз.
Меню над полоской: белая карточка `radius-xl` с тенью `size-popup-shadow`, подпись «ОТЛОЖИТЬ «…»» `label-medium` Bold `text-secondary`, строки `size-popup-menu-row` (48): название `body-large` SemiBold, новый срок справа `body-medium` `text-secondary` («18:00» сегодня, «чт, 12 ноя, 17:00» в другой день), между строками линия `divider`. Окно из уведомления — `SheetDialog` «Отложить» с подписью задачи и одной кнопкой «Отмена»; строки — `DialogOption` с новым сроком справа.
Вариант срабатывает сразу: полоска — «Отложено на 18:00 · Отменить», из уведомления — подсказка «Отложено на … · Отмена». «Выбрать дату…» — окно задачи сразу с окном даты поверх экрана. Источник: `domain/model/Snooze.kt`, `ui/feature/snooze/`, `notifications/ReminderPopup.kt`.
Итерация 54 (T-96): то же окно открывают часы «Отложить» в окне задачи (`SheetCard`) — из виджета, «Недели» виджета и приложения; после выбора шторка закрывается, подсказка «Отложено на … · Отмена» (`UndoSnackbar`), «Выбрать дату и время…» открывает окно даты шторки.'''),
    'ReminderStrip': ('Полоска напоминания поверх других приложений: белая с чёрными кнопками (0.53), стопка, «ещё N», «Отменить».',
                      '''Белая капсула `surface` высотой `size-popup-strip` (68) с рамкой `divider` и тенью `size-popup-shadow` (0.53, T-118: была чёрной). Поля от краёв `size-popup-edge` (12), щель между полосками `size-popup-gap` (8). Слева колокольчик 22 `text-secondary`; название `body-large` Bold `text-primary`, ниже подпись «Сегодня, 18:00 · Дом» `label-small` `text-secondary`.
Справа три круглые чёрные кнопки `size-popup-button` (48): фон `accent`, значок `on-accent` — «Не буду делать», «Отложить» (часы), «Выполнено». Нажатая «Отложить» (меню открыто) — с кольцом `size-popup-pressed-ring` через зазор `size-popup-pressed-gap`. В тёмной теме по токенам наоборот: полоска тёмная, кнопки светлые.
«Отложить» (итерация 53, `Snooze`): меню над полоской, срок задачи переносится. Смахнуть вправо — закрыть. Видно до трёх полосок, остальные — метка «ещё N».
Состояние «Отменить»: белая полоска «Выполнено» (галочка) или «Отложено на 18:00» (часы) с чёрной капсулой «Отменить» и чёрной линией внизу, которая тает за 1,5 секунды. Без вибрации. Источник: `notifications/ReminderPopup.kt`.'''),
    'WidgetWeek': ('Виджет «Неделя», Android 12+.', '0.73 / #261: дата текущего дня содержит «сегодня» тем же начертанием, без цветовой подсветки; сетка дней и календарь; строки и действия соответствуют WeekView. Пустые слоты под задачами открывают день, время и значок переключают выполнение, название открывает задачу. Активный высокий приоритет — week-row-week-priority и начертание 800; выполненный бледный. Размер и число строк сохраняются.'),
    'Widget': ('Виджет «Задачи»: лента по дням со шкалой, «сейчас» и нижняя строка.',
               '''Фон `background`, радиус `radius-xl`. Список по дням: подпись дня (красная у прошедшего), строки шкалы как в `TimelineRow`, у «Сегодня» линия прогресса. Отметка «сейчас»: чёрное время слева, точка на линии и линия до края (`size-widget-now-row` 14).
Внизу нижняя строка (`WidgetBar`; 0.57 — «+» слева, блок вкладок по центру, дата справа, без линии); справа у подписи каждого дня число задач (у «Сегодня» вместо него прогресс), `text-secondary` 12. В виджете нет градиентов и размытия: фон и скругления — готовыми картинками. Цвета те же, что в приложении: Glance сам выбирает светлую или тёмную тему.
Объём (итерация 42): во «Всех» — просроченные, сегодня и ещё 13 дней (с повторами); во «Входящих» и списках — все задачи; всего не больше 150 строк ленты. Строки «Ещё N» нет.
Вид «Неделя» (итерация 45, третья вкладка переключателя; на Android 10–11 вместо неё «Все»): четыре строки одинаковой высоты (0.46.0: не лента, а вертикальная раскладка с долями — строки делят настоящую высоту виджета над нижней строкой, сколько бы ни сообщил рабочий стол) — Пн–Вт, Ср–Чт, Пт–Сб, Вс и мини-календарь; нижняя строка ничего не закрывает. Ячейка и мини-календарь — как в приложении (`WeekView`): строка не ниже `size-widget-week-task` (18; строки делят высоту ячейки поровну, обычно ~20) с промежутком `size-widget-week-task-gap` (2), фон по приоритету (`widget_week_plaque_*`), кружок `size-widget-week-check` (13), справа эмодзи списка и время (итерация 48); строка даты по высоте текста, не ниже `size-widget-week-header` (12; 9 sp капсом — «ПТ 30», `text-secondary` обычным начертанием; сегодня — без кружка, жирные `text-primary`); мини-календарь (#284): цифры 11 sp обычным начертанием, сегодня жирным; выбранная неделя — светлая полоса; шесть строк без подписей дней недели. #313 («Всё листается», вариант PM 5): у каждого дня под неподвижной строкой даты — свой список задач (`ListView`, строка `size-widget-week-item` 22, затухание края `size-widget-week-fade` 8), «+N» нет; под задачами — пустое место, открывающее день. Мини-календарь — лента недель (`size-widget-week-cal-row` 20) с названиями месяцев (`size-widget-week-cal-month` 14, 9 sp жирным `text-secondary`; у другого года — «январь 2027»): с недели за две до текущей на полгода вперёд, неделя — в месяце своего воскресенья. Нажатия: строка даты, задача и пустое место — окно дня; неделя ленты — выбрать её, число выбранной недели — открыть день; выбор недели у каждого виджета свой. Корень `widget_root_week` статический (списки помнят прокрутку), разметка `widget_week_*` генерируется `design/tools/gen_widget_week.py`.
Устройство (итерация 42): на Android 12+ фон и лента — обычные элементы Android прямо в корне виджета (`widget_root`), каждая строка — своя разметка `widget_row_*` (задача, период, у выполненных — `*_done`, подпись дня, «Просрочено», «сейчас», хвост) ~1 КБ; нижняя строка — Glance поверх ленты, фоны плиток нижней строки — картинки со скруглением (`widget_bar_add`, `widget_bar_tile`; 0.57). Размеры и цвета строк — копия токенов в `res/values*/widget_rows.xml` (проверяет тест). Строка всегда задаёт весь свой вид: Android перекрашивает показанные строки другой задачи. Нажатия строк: кружок — «выполнено»/«вернуть», плашка — открыть задачу. Android 10–11 — лента Glance без прокрутки.'''),
    'ScreenAll': ('Экран «Предстоящее» (до 0.55 — «Все»).', 'Итерация 55: сверху заголовок «Предстоящее» (`ScreenTitle`), экран — на левом месте нижней панели. Лента по дням на `background`: подпись дня, шкала со строками `TimelineRow`, отметка «сейчас» у сегодняшнего дня; внизу нижняя панель. Пустая лента — `EmptyTasks`. Окно задачи открывается поверх.'),
    'ScreenInbox': ('Экран «Входящие» (0.52 — только неразобранное).', 'Итерация 52 (решение PM, по справке SingularityApp): «Входящие» — задачи без даты и без списка. Поставили дату — задача уходит в «Без списка» (`ScreenNoList`), выбрали список — в «Когда-нибудь» (`ScreenSomeday`), убрали дату у задачи без списка — возвращается. Под заголовком подсказка `body-small` `text-secondary`: «Здесь — то, что ещё не разобрано…». Плоский список (`TimelineRow`), колонка времени пустая, эмодзи списка нет. Пусто: «Всё разобрано». Открывается строкой «Входящие» на «Списках» (0.55); число неразобранных — там и на вкладке «Списки» (`BottomBar`). «+» — задача без даты и без списка. Вкладка «Входящие» в виджете — по той же логике.'),
    'ScreenSomeday': ('Экран «Когда-нибудь» (0.52).', 'Вход — строка на «Списках» (эмодзи 💭 с 0.52.1, число открытых). «Назад», заголовок `title-large` Bold, справа число открытых задач. Группы по спискам в порядке «Списков» (папки, потом списки без папки): эмодзи `ListBadge`, название `body-medium` Bold, число `label-medium` Bold `text-secondary`; под ними строки `TimelineRow` без колонки времени. Кружок отмечает, плашка открывает задачу; поставили дату — задача уходит в свой список и в ленту по дате. «+» (`AddTaskButton`) — новая задача без даты, сразу открыто окно «Список». Пусто: «Задач со списком, но без даты нет». Источник: `ui/feature/sorted/`.'),
    'ScreenNoList': ('Экран «Без списка» (0.52).', 'Вход — строка на «Списках» (эмодзи 📌 с 0.52.1, число открытых). Задачи без списка, но с датой (у задачи — 📌 вместо 📥): по дням, как на «Все» — подпись дня (`DayHeader`, «Сегодня»/«Завтра», прошедшие — `overdue`), строки `TimelineRow` с временем слева, линия «сейчас» у сегодня. Выбрали список — задача уходит из раздела; убрали дату — возвращается во «Входящие». «+» — задача на сегодня без списка. Пусто: «Задач с датой, но без списка нет». Источник: `ui/feature/sorted/`.'),
    'ScreenLists': ('Экран «Списки» (0.56 — карточки групп и строка значков).', 'Итерация 56 (решение PM 09.11.2026, T-123…T-125): заголовка нет. Три карточки (`ListRows`): системные — 📥 «Входящие», 💭 «Когда-нибудь», 📌 «Без списка» (их нельзя удалить или изменить); мои списки и папки; архив — «Выполненные», «Не буду делать», «Корзина». У каждой строки число задач, если не 0. Внизу, над нижней панелью, закреплена строка значков: поиск (`ic_nav_search`), новая папка (`ic_folder_plus`), новый список (`ic_list_plus`), настройки (`ic_nav_settings`) — четыре кнопки поровну, высота `size-lists-tool` (44), `surface`, радиус `radius-lg`, значок `text-primary`; над строкой плавный переход в `background`. Списки прокручиваются под ней. Окна создания — `AlertDialogs`. Пусто: «Списков пока нет» между системной карточкой и архивом.'),
    'ScreenProject': ('Экран списка.', 'Сверху «назад», плитка эмодзи списка, название `title-large` Bold и «⋮» (переименовать, эмодзи, удалить; у «Входящих» меню нет). Ниже плоский список без эмодзи списка. Плавающая «+» в правом нижнем углу.'),
    'ScreenSearch': ('Экран «Поиск».', 'С 0.55 открывается полем «Поиск задач» на «Списках» (отдельной вкладки нет). Поле поиска, раздел «Найдено: N» с плашками `TaskRow` (найденное подсвечено), ниже «Выполненные» серым; выполненную можно вернуть галочкой.'),
    'ScreenCompleted': ('Экран «Выполненные».', 'Сверху «назад» и заголовок, ниже дни (новые сверху) с плашками `TaskRow` в отмеченном виде; снять галочку — задача вернётся в свой список с кнопкой «Отмена».'),
    'ScreenSettings': ('Экран «Настройки».', 'С 0.55 открывается шестерёнкой в шапке «Списков» (отдельной вкладки нет). Заголовок и секции: напоминания (переключатели), обновление, импорт из TickTick, резервная копия, данные; внизу версия. Кнопки Material 3, предупреждения `overdue`, опасные действия `danger`.'),
    'ScreenNewTask': ('Окно создания задачи.', 'Шторка `SheetCard` стоит над клавиатурой поверх экрана под затемнением `scrim`. Подробности — в `SheetCard`, `DateFields`, `ToolButton`.'),
}


# ---------------------------------------------------------------- финансы, этап 1 (0.58, #221): бар модулей, счета, окно счёта

def module_bar(sel='finance'):
    """Верхний бар модулей (0.58): белая плашка со значками «Задачи» и «₽», активный — на `selected`. Заголовков модулей нет."""
    b = lambda ic, on: (f'<span style="width:44px;height:36px;border-radius:var(--radius-md);display:flex;align-items:center;justify-content:center;'
                        f'background:{"var(--selected)" if on else "transparent"};color:{"var(--text-primary)" if on else "var(--inactive)"}">{svg(ic, 20)}</span>')
    return (f'<div style="height:48px;display:flex;align-items:center;padding:0 12px"><span style="display:flex;gap:4px;padding:4px;border-radius:var(--radius-lg);background:var(--surface)">'
            f'{b("ic_module_tasks", sel == "tasks")}{b("ic_module_finance", sel == "finance")}</span></div>')


def finance_bar(sel='analytics'):
    """Нижняя панель финансов (0.58): аналитика, чёрный «+», операции — только значки, размеры как у панели задач."""
    tab = lambda ic, on: (f'<span style="width:72px;height:40px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;'
                          f'background:{"var(--selected)" if on else "transparent"};color:{"var(--accent)" if on else "var(--text-secondary)"}">{svg(ic, 24)}</span>')
    add = f'<span style="width:72px;height:44px;border-radius:var(--radius-xl);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("add", 24)}</span>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:57px;background:var(--surface);border-top:1px solid var(--divider);display:flex;align-items:center;justify-content:space-around">'
            f'{tab("ic_finance_tab_analytics", sel == "analytics")}{add}{tab("ic_finance_operations", sel == "operations")}</div>')


def primary_action(label, plus=False):
    return (f'<span style="display:flex;align-items:center;justify-content:center;gap:8px;border-radius:var(--radius-lg);background:var(--accent);color:var(--on-accent);'
            f'padding:12px 0;font-size:14px;line-height:20px;font-weight:700">{mi("add", 18) if plus else ""}{label}</span>')


ACCOUNTS = [('💳', 'Карта', '42 300 ₽'), ('👛', 'Наличные', '6 150 ₽'), ('🏦', 'Накопления', '80 000 ₽')]


def account_tiles(items=ACCOUNTS, total='128 450 ₽', wide=True):
    tile = lambda e, n, v: (f'<span style="{"flex:1;min-width:0" if wide else "flex:0 0 108px"};background:var(--surface);border-radius:var(--radius-lg);padding:12px;box-sizing:border-box">'
                            f'<span style="display:flex;gap:4px;align-items:center;font-size:12px;line-height:16px;color:var(--text-secondary);white-space:nowrap"><span style="font-size:14px">{e}</span>{n}</span>'
                            f'<span style="display:block;padding-top:8px;font-size:14px;line-height:20px;font-weight:700;white-space:nowrap">{v}</span></span>')
    head = (f'<div style="display:flex;align-items:center;padding:8px 16px 8px 20px"><span style="font-size:14px;line-height:20px;font-weight:700">Счета</span>'
            f'<span style="flex:1;font-size:14px;line-height:20px;color:var(--text-secondary)">&nbsp;· {total}</span>'
            f'<span style="width:32px;height:32px;border-radius:var(--radius-md);background:var(--surface);display:flex;align-items:center;justify-content:center">{mi("add", 18)}</span></div>')
    return f'{head}<div style="display:flex;gap:8px;padding:0 16px;overflow:hidden">{"".join(tile(*a) for a in items)}</div>'


def finance_empty_screen():
    msg = (f'<div style="position:absolute;left:32px;right:32px;top:250px;text-align:center"><span style="color:var(--inactive)">{svg("ic_finance_wallet", 40)}</span>'
           '<div style="font-size:16px;line-height:24px;font-weight:700;padding-top:16px">Начнём со счёта</div>'
           '<div style="font-size:12px;line-height:16px;color:var(--text-secondary);padding:8px 0 24px">Добавь наличные, карту или накопления<br>и укажи, сколько на них сейчас.</div>'
           f'{primary_action("Создать счёт", plus=True)}</div>')
    return frame(f'{module_bar()}{msg}{finance_bar()}', 730)


def finance_accounts_screen(many=False):
    items = ACCOUNTS + [('💳', 'Т-Банк', '12 480 ₽')] if many else ACCOUNTS
    note = '<div style="padding:16px 20px;font-size:12px;line-height:16px;color:var(--text-secondary)">Аналитика расходов появится, когда будут операции.</div>'
    return frame(f'{module_bar()}{account_tiles(items, "140 930 ₽" if many else "128 450 ₽", wide=not many)}{note}{finance_bar()}', 730)


def account_window(new=True):
    field = lambda label, value, pick=False: (f'<div style="display:flex;align-items:center;min-height:48px;border-bottom:1px solid var(--divider)"><span style="flex:1;font-size:14px;line-height:20px;color:var(--text-secondary)">{label}</span>'
                                              f'<span style="display:flex;align-items:center;font-size:14px;line-height:20px;font-weight:700">{value}{f"""<span style="color:var(--inactive)">{mi("expand", 18)}</span>""" if pick else ""}</span></div>')
    note = '<div style="font-size:12px;line-height:16px;color:var(--text-secondary)">Это деньги на счёте сегодня. Доходом не считается.</div>' if new else ''
    return (f'<div style="width:380px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden;flex-shrink:0">'
            f'<div style="display:flex;align-items:center;padding:12px 8px 0 16px"><span style="flex:1;font-size:22px;line-height:28px;font-weight:700">{"Новый счёт" if new else "Счёт"}</span>'
            f'<span style="width:36px;height:36px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{mi("close")}</span></div>'
            f'<div style="padding:0 16px">{field("Название", "Карта")}{field("Тип", "Карта", True)}{field("Эмодзи", "💳", True)}'
            f'<div style="padding-top:16px;font-size:12px;line-height:16px;color:var(--text-secondary)">Остаток сейчас</div>'
            f'<div style="display:flex;align-items:center;justify-content:center;gap:8px;padding:12px 0"><span style="font-size:32px;line-height:40px;font-weight:700">42300</span><span style="font-size:28px;line-height:36px;font-weight:700">₽</span></div>{note}</div>'
            f'<div style="padding:16px">{primary_action("Создать счёт" if new else "Сохранить")}</div></div>')


PREVIEWS.update({
    'ModuleBar': lambda: doc('Панель и поля', 150, 'Верхний бар модулей и нижняя панель финансов (0.58)',
                             f'<div class="row" style="align-items:flex-start">{frame(module_bar("tasks"), 48)}{frame(module_bar("finance"), 48)}{frame(finance_bar(), 58)}</div>'),
    'FinanceWindow': lambda: doc('Окна', 470, 'Окно счёта по центру экрана: новый счёт и правка (0.58)', f'<div class="row" style="align-items:flex-start">{account_window(True)}{account_window(False)}</div>'),
    'ScreenFinance': lambda: doc('Экраны', 760, 'Финансы, этап 1 (0.58): первый запуск, счета, много счетов',
                                 f'<div class="row" style="align-items:flex-start">{finance_empty_screen()}{finance_accounts_screen()}{finance_accounts_screen(True)}</div>'),
})

README_MORE.update({
    'ModuleBar': ('Верхний бар модулей и нижняя панель финансов (0.58).',
                  'Этап 1 финансов (#221, решения PM 10.11.2026, макет design/approved/217/compact-module-bar.html). Бар модулей — строка `size-module-bar` (48) на фоне `background`, слева белая плашка (`surface`, `radius-lg`, поля 4) с кнопками `size-module-button-w`×`size-module-button-h` (44×36, `radius-md`): «Задачи» (`ic_module_tasks`) и «Финансы» (`ic_module_finance`), значок `size-module-icon` (20). Активная — фон `selected`, значок `text-primary`; остальные — `inactive`. Заголовков модулей нет. Виден на главных экранах задач («Предстоящее», «Неделя», «Списки») и в финансах; на вложенных экранах и в режиме выбора его нет. Новые модули добавляются справа.\n\nНижняя панель финансов повторяет панель задач (`BottomBar`): высота `size-nav-bar`, вкладки 72×40 только значками — «Аналитика» (`ic_finance_analytics`) и «Операции» (`ic_finance_operations`), посередине чёрная «+» 72×44. Без счетов «+» открывает новый счёт.'),
    'FinanceWindow': ('Окно финансов по центру экрана: счёт (0.58).',
                      'Не нижняя шторка (решение PM): окно `radius-nav` на `surface`, поля от краёв 16. Сверху заголовок `title-large` Bold и «×» (`size-finance-close`, `text-secondary`); «Назад» и нажатие мимо — как «×». Строки полей `size-finance-field` (48): подпись `text-secondary` слева, значение Bold справа, у выбора — стрелка `inactive`; под строкой линия `divider`. Сумма — крупно по центру (`headline-large` Bold) со знаком «₽», клавиатура цифровая, копейки через запятую. Внизу одна чёрная кнопка во всю ширину (`accent` / `on-accent`, `radius-lg`); недоступная — `divider` / `text-secondary`. Окно счёта: название (пустое — название типа), тип (наличные / карта / накопления), эмодзи (по умолчанию от типа: 👛 💳 🏦), «Остаток сейчас». Удаления и архива счёта нет.'),
    'ScreenFinance': ('Экран «Финансы», этап 1 (0.58).',
                      'Без заголовка модуля. Счетов нет — по центру значок `ic_finance_wallet` (`size-finance-empty-icon`, `inactive`), «Начнём со счёта», пояснение и чёрная кнопка «+ Создать счёт». Есть счета — блок «Счета · сумма» (сумма `text-secondary`) с маленьким «+» (`size-account-add`, `surface`, `radius-md`) и карточки счетов (`surface`, `radius-lg`, поля 12): эмодзи и название `label-medium` `text-secondary`, остаток `title-small` Bold. До трёх счетов карточки делят ширину поровну; больше — ряд листается вбок, карточка не уже `size-account-tile` (108), следующая выглядывает. Суммы — «42 300 ₽», копейки только если есть. Операции и аналитика — следующие этапы.'),
})


# ---------------------------------------------------------------- финансы, этап 2 (0.59, #225): окно операции, список операций, категории

def segments(options, sel):
    cell = lambda t, on: (f'<span style="flex:1;min-height:36px;border-radius:var(--radius-md);display:flex;align-items:center;justify-content:center;font-size:14px;line-height:20px;'
                          f'background:{"var(--surface)" if on else "transparent"};color:{"var(--text-primary)" if on else "var(--text-secondary)"};font-weight:{700 if on else 500}">{t}</span>')
    return f'<div style="display:flex;gap:4px;padding:4px;border-radius:var(--radius-lg);background:var(--selected)">{"".join(cell(t, t == sel) for t in options)}</div>'


def fin_field(label, value, pick=False, muted=False):
    arrow = f'<span style="color:var(--inactive)">{mi("expand", 18)}</span>' if pick else ''
    style = 'color:var(--inactive);font-weight:400' if muted else 'font-weight:700'
    return (f'<div style="display:flex;align-items:center;min-height:48px;border-bottom:1px solid var(--divider)"><span style="flex:1;font-size:14px;line-height:20px;color:var(--text-secondary)">{label}</span>'
            f'<span style="display:flex;align-items:center;font-size:14px;line-height:20px;{style}">{value}{arrow}</span></div>')


def chips(items, sel=None):
    chip = lambda t: (f'<span style="flex:1;min-width:0;min-height:36px;border-radius:var(--radius-md);display:flex;align-items:center;justify-content:center;font-size:12px;line-height:16px;'
                      f'background:{"var(--accent)" if t == sel else "var(--selected)" if t == "Все категории" else "var(--background)"};'
                      f'color:{"var(--on-accent)" if t == sel else "var(--text-secondary)" if t == "Все категории" else "var(--text-primary)"};font-weight:{700 if t == sel else 600}">{t}</span>')
    rows = [items[i:i + 3] for i in range(0, len(items), 3)]
    return ('<div style="padding-top:12px;font-size:12px;line-height:16px;color:var(--text-secondary)">Категория · необязательно</div>'
            + ''.join(f'<div style="display:flex;gap:4px;padding-top:{8 if i == 0 else 4}px">{"".join(chip(t) for t in r)}</div>' for i, r in enumerate(rows)) + '<div style="height:8px"></div>')


def operation_window(kind='Расход', edit=False):
    head = (f'<div style="display:flex;align-items:center;padding:12px 8px 0 16px"><span style="flex:1;font-size:22px;line-height:28px;font-weight:700">{kind if edit else "Новая операция"}</span>'
            + (f'<span style="width:36px;height:36px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{svg("ic_tool_delete", 24)}</span>' if edit else '')
            + f'<span style="width:36px;height:36px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{mi("close")}</span></div>')
    amount = '<div style="text-align:center;padding:12px 0;font-size:32px;line-height:40px;font-weight:700">850 ₽</div>'
    if kind == 'Перевод':
        body = fin_field('Со счёта', '💳 Карта', True) + fin_field('На счёт', '👛 Наличные', True) + fin_field('Комментарий', 'Необязательно', muted=True)
    else:
        body = (fin_field('Счёт', '💳 Карта', True) + chips(['Продукты', 'Кафе', 'Транспорт', 'Дом', 'Здоровье', 'Все категории'], 'Продукты')
                + fin_field('Место', 'Пятёрочка') + fin_field('Комментарий', 'Необязательно', muted=True))
    body += fin_field('Дата', '10.11.2026 · сегодня', True)
    tabs = '' if edit else f'<div style="padding-top:8px">{segments(["Расход", "Доход", "Перевод"], kind)}</div>'
    return (f'<div style="width:380px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden;flex-shrink:0">{head}<div style="padding:0 16px">{tabs}{amount}{body}</div>'
            f'<div style="padding:16px">{primary_action("Сохранить")}</div></div>')


def operations_screen():
    def row(title, value, transfer=False):
        color = 'var(--text-secondary)' if transfer else 'var(--text-primary)'
        sub = '<div style="font-size:11px;line-height:16px;color:var(--text-secondary)">Перевод</div>' if transfer else ''
        return (f'<div style="display:flex;align-items:center;min-height:52px;padding:0 16px;border-bottom:1px solid var(--divider)"><span style="flex:1;font-size:14px;line-height:20px;font-weight:600;color:{color}">{title}{sub}</span>'
                f'<span style="font-size:14px;line-height:20px;font-weight:{500 if transfer else 700};color:{color}">{value}</span></div>')
    day = lambda t: f'<div style="padding:12px 20px 8px;font-size:14px;line-height:20px;font-weight:700">{t}</div>'
    card = lambda rows: f'<div style="margin:0 16px;border-radius:var(--radius-lg);background:var(--surface);overflow:hidden">{rows}</div>'
    gear = f'<span style="position:absolute;right:12px;top:2px;width:44px;height:44px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{svg("ic_nav_settings", 20)}</span>'
    body = (day('10 ноября · сегодня') + card(row('Продукты', '− 850 ₽') + row('Зарплата', '+ 65 000 ₽') + row('Карта → Наличные', '5 000 ₽', True))
            + day('9 ноября · вчера') + card(row('Кафе', '− 620 ₽') + row('Без категории', '− 1 200 ₽')))
    return frame(f'{module_bar()}{gear}{body}{finance_bar("operations")}', 730)


def categories_screen():
    row = lambda t, arch=False: (f'<div style="display:flex;align-items:center;min-height:52px;padding:0 16px;border-bottom:1px solid var(--divider)"><span style="flex:1;font-size:16px;line-height:24px;font-weight:600;'
                                 f'color:{"var(--text-secondary)" if arch else "var(--text-primary)"}">{t}</span><span style="color:var(--inactive)">{svg("ic_snack_restore", 20) if arch else mi("chevron", 24)}</span></div>')
    top = (f'<div style="display:flex;align-items:center;padding:8px 12px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("back")}</span>'
           f'<span style="flex:1;font-size:22px;line-height:28px;font-weight:700">Категории</span>'
           f'<span style="width:36px;height:36px;border-radius:var(--radius-lg);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("add", 18)}</span></div>')
    card = lambda rows: f'<div style="margin:0 16px;border-radius:var(--radius-lg);background:var(--surface);overflow:hidden">{rows}</div>'
    return frame(f'{top}<div style="padding:8px 16px">{segments(["Расходы", "Доходы"], "Расходы")}</div>{card(row("Продукты") + row("Кафе") + row("Транспорт"))}'
                 f'<div style="padding:16px 20px 8px;font-size:14px;line-height:20px;font-weight:700">Архив</div>{card(row("Такси", True))}', 730)


PREVIEWS.update({
    'OperationWindow': lambda: doc('Окна', 620, 'Окно операции (0.59): расход, перевод и правка',
                                   f'<div class="row" style="align-items:flex-start">{operation_window()}{operation_window("Перевод")}{operation_window(edit=True)}</div>'),
    'ScreenOperations': lambda: doc('Экраны', 760, 'Финансы, этап 2 (0.59): операции и категории',
                                    f'<div class="row" style="align-items:flex-start">{operations_screen()}{categories_screen()}</div>'),
})

README_MORE.update({
    'OperationWindow': ('Окно операции: расход, доход, перевод и правка (0.59).',
                        'Этап 2 финансов (#225, макеты design/approved/217, экраны 1.2–1.6). Окно `FinanceWindow`. Сверху переключатель типа — три равные части на подложке `selected` (`radius-lg`, поля 4), выбранная — `surface` и Bold; высота части `size-finance-chip` (36). Ниже крупная сумма по центру со знаком «₽», строки полей: «Счёт» (эмодзи и название, стрелка — список счетов), категории, «Место», «Комментарий», «Дата» («10.11.2026 · сегодня»). Категории — кнопки по три в ряд высотой `size-finance-chip`, `radius-md`: обычная — `background`, выбранная — `accent` / `on-accent` Bold, «Все категории» — `selected` / `text-secondary`; не больше пяти категорий, недавние первыми. Нет категорий — одна кнопка «+ Создать категорию» и пояснение. У перевода вместо категорий и места — «Со счёта» и «На счёт». Под полем «Место», пока оно в фокусе, — кнопки-подсказки ранее введённых мест. Правка: заголовок — тип операции, переключателя нет, перед «×» значок корзины `ic_tool_delete`. «Сохранить» недоступна без суммы. После сохранения окно остаётся для следующей операции.'),
    'ScreenOperations': ('Экраны «Операции» и «Категории» (0.59).',
                         'Операции: группы по дням — подпись `label-large` Bold («10 ноября · сегодня», в другом году — с годом), карточка `surface` `radius-lg` со строками `size-operation-row` (52): слева категория SemiBold (без категории — «Без категории»), справа сумма Bold: расход «− 850 ₽», доход «+ 65 000 ₽». Перевод — «Карта → Наличные» и сумма без знака цветом `text-secondary`, под названием подпись «Перевод»; новых цветов нет. Справа в баре модулей — шестерёнка финансов (`ic_nav_settings`, `text-secondary`). Категории — вложенный экран без баров: стрелка назад, заголовок, чёрная «+» 36; переключатель «Расходы / Доходы»; карточка строк со стрелкой; «Архив» — серые названия со значком возврата `ic_snack_restore`.'),
})


# ---------------------------------------------------------------- 0.59.1 (#227): панели без плашек, новые значки модулей и «Списков»

def module_bar(sel='finance', gear=True):
    """Верхний бар модулей (0.59.1): белая полоса во всю ширину, по центру галочка и знак рубля; активный чёрный, другой серый, без плашки."""
    b = lambda ic, on: f'<span style="width:44px;height:36px;display:flex;align-items:center;justify-content:center;color:{"var(--accent)" if on else "var(--inactive)"}">{svg(ic, 24)}</span>'
    g = f'<span style="position:absolute;right:12px;top:2px;width:44px;height:44px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{svg("ic_nav_settings", 20)}</span>' if gear and sel == 'finance' else ''
    return (f'<div style="position:relative;height:48px;display:flex;align-items:center;justify-content:center;gap:16px;background:var(--surface)">'
            f'{b("ic_module_tasks", sel == "tasks")}{b("ic_module_finance", sel == "finance")}{g}</div>')


def _bar_tab(ic, on, extra=''):
    return (f'<span style="position:relative;width:72px;height:40px;display:flex;align-items:center;justify-content:center;'
            f'color:{"var(--accent)" if on else "var(--inactive)"}">{svg(ic, 24)}{extra}</span>')


def finance_bar(sel='analytics'):
    """Нижняя панель финансов (0.59.1): без плашки — выбранная вкладка чёрная, другая серая."""
    add = f'<span style="width:72px;height:44px;border-radius:var(--radius-xl);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("add", 24)}</span>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:57px;background:var(--surface);border-top:1px solid var(--divider);display:flex;align-items:center;justify-content:space-around">'
            f'{_bar_tab("ic_finance_tab_analytics", sel == "analytics")}{add}{_bar_tab("ic_finance_operations", sel == "operations")}</div>')


def nav_bar(sel, inbox_count=None, left='upcoming'):
    """Нижняя панель задач (0.59.1): без плашек, выбранная вкладка чёрная, остальные серые; «Списки» — линии без точек."""
    if sel == 2:
        left = 'week'
    lists_on = sel not in (1, 2)
    dot = '<span style="position:absolute;bottom:4px;left:50%;margin-left:-2px;width:4px;height:4px;border-radius:50%;background:currentColor;opacity:.6"></span>'
    badge = (f'<span style="position:absolute;top:-4px;right:6px;min-width:16px;height:16px;padding:0 4px;box-sizing:border-box;border-radius:8px;'
             f'background:var(--accent);color:var(--on-accent);font-size:10px;line-height:16px;font-weight:700;text-align:center">{inbox_count}</span>') if inbox_count else ''
    add = f'<span style="width:72px;height:44px;border-radius:var(--radius-xl);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("add", 24)}</span>'
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:57px;background:var(--surface);border-top:1px solid var(--divider);display:flex;align-items:center;justify-content:space-around">'
            f'{_bar_tab("ic_tab_week" if left == "week" else "ic_widget_all", not lists_on, dot)}{add}{_bar_tab("ic_tab_lists", lists_on, badge)}</div>')


README_MORE['ModuleBar'] = ('Верхний бар модулей и нижние панели (0.59.1).',
    'Решения PM 11.11.2026 (#227), заменяют вид 0.58. Бар модулей — полоса `size-module-bar` (48) на `surface` во всю ширину (фон заходит под строку состояния), по центру значки модулей через 16: «Задачи» — галочка `ic_module_tasks`, «Финансы» — знак рубля `ic_module_finance`, размер `size-icon` (24). Активный — `accent`, остальные — `inactive`; плашки выделения нет. В финансах справа шестерёнка `ic_nav_settings` (`text-secondary`). Виден на главных экранах задач и в финансах; на вложенных экранах и в режиме выбора его нет.\n\nНижние панели задач и финансов: места вкладок 72×40 без плашки — выбранная вкладка `accent`, остальные `inactive`; посередине чёрная «+» 72×44. Задачи: «Предстоящее» / «Неделя» (точка — есть меню), «Списки» — `ic_tab_lists` (три линии без точек) с числом неразобранных «Входящих». Финансы: «Аналитика» и «Операции».')


# ---------------------------------------------------------------- финансы, этап 3 (0.60, #229): аналитика расходов

def analytics_screen(empty=False):
    arrow = lambda ic, dim=False: f'<span style="width:40px;height:40px;border-radius:var(--radius-lg);background:var(--surface);display:flex;align-items:center;justify-content:center;opacity:{.35 if dim else 1}">{mi(ic, 24)}</span>'
    month = f'<div style="display:flex;align-items:center;padding:16px 16px 12px">{arrow("back")}<span style="flex:1;text-align:center;font-size:16px;line-height:24px;font-weight:700">Ноябрь 2026</span>{arrow("chevron", True)}</div>'
    total, delta, versus = ('0 ₽', '−39 200 ₽ · −100,0%', 'К 1–10 октября · было 39 200 ₽') if empty else ('42 800 ₽', '+3 600 ₽ · +9,2%', 'К 1–10 октября · было 39 200 ₽')
    summary = (f'<div style="margin:0 16px;border-radius:var(--radius-xl);background:var(--surface);padding:16px"><div style="font-size:12px;line-height:16px;color:var(--text-secondary)">Расходы · 1–10 ноября</div>'
               f'<div style="font-size:32px;line-height:40px;font-weight:700;padding-top:4px">{total}</div>'
               f'<span style="display:inline-block;margin-top:8px;padding:4px 8px;border-radius:var(--radius-sm);background:var(--selected);font-size:12px;line-height:16px;font-weight:700">{delta}</span>'
               f'<div style="padding-top:8px;font-size:11px;line-height:16px;color:var(--text-secondary)">{versus}</div></div>')
    def cat(name, amount, foot, share):
        return (f'<div style="padding:12px 16px;border-bottom:1px solid var(--divider)"><div style="display:flex;font-size:14px;line-height:20px"><span style="flex:1;font-weight:600">{name}</span><span style="font-weight:700">{amount}</span></div>'
                f'<div style="margin-top:8px;height:6px;border-radius:var(--radius-sm);background:var(--selected)"><div style="width:{share}%;height:100%;border-radius:var(--radius-sm);background:var(--text-secondary)"></div></div>'
                f'<div style="display:flex;padding-top:8px;font-size:11px;line-height:16px;color:var(--text-secondary)"><span style="flex:1">{foot}</span><span>{share}%</span></div></div>')
    if empty:
        body = '<div style="margin:16px 16px 0;border-radius:var(--radius-xl);background:var(--surface);padding:16px;font-size:14px;line-height:20px;color:var(--text-secondary)">В этом месяце расходов пока нет.</div>'
    else:
        body = ('<div style="display:flex;align-items:center;padding:24px 20px 12px"><span style="flex:1;font-size:14px;line-height:20px;font-weight:700">По категориям</span><span style="font-size:11px;line-height:16px;color:var(--text-secondary)">доля расходов</span></div>'
                f'<div style="margin:0 16px;border-radius:var(--radius-xl);background:var(--surface);overflow:hidden">{cat("Продукты", "21 000 ₽", "+3 000 ₽ · +16,7%", 49)}{cat("Кафе", "10 000 ₽", "−1 000 ₽ · −9,1%", 23)}'
                f'{cat("Подарки", "6 800 ₽", "+6 800 ₽ · в октябре не было", 16)}{cat("Без категории", "5 000 ₽", "+1 000 ₽ · +25,0%", 12)}</div>')
    return frame(f'{module_bar()}{account_tiles()}{month}{summary}{body}{finance_bar()}', 860)


PREVIEWS['ScreenAnalytics'] = lambda: doc('Экраны', 890, 'Финансы, этап 3 (0.60): аналитика расходов за месяц и месяц без расходов',
                                          f'<div class="row" style="align-items:flex-start">{analytics_screen()}{analytics_screen(True)}</div>')
README_MORE['ScreenAnalytics'] = ('Экран «Аналитика» финансов (0.60).',
    'С 0.91 (#327, вариант Г, design/approved/327): вместо трёх сумм — полоса лимита `size-limit-bar` (26 dp): внутри потрачено (`label-large` Bold, белым при заполнении от 30 %), справа лимит Bold; отметка «сегодня» `size-budget-mark` на `text-secondary` с белыми краями; заполнение по темпу — `text-primary` (в плане, «бюджет выбран»), `warning` (быстрее плана), `danger` (перерасход); под полосой «осталось/перерасход» и прогноз. Нажатие на полосу или доли «Категории» открывает окно «Бюджет» (`FinanceWindow`): строки категорий с полоской `size-budget-bar` 6 dp, итог «Всего» с линией `size-budget-total-rule`, доходы — `income`. Ниже — прежнее описание. Этап 3 (#229; макеты design/approved/217 — compact-module-bar.html, экраны 4.1, 4.2). Под карточками счетов: переключатель месяца — две кнопки 40×40 (`surface`, `radius-lg`) и название месяца `title-medium` Bold по центру; правая стрелка на текущем месяце бледная (0,35) и не нажимается. Карточка итога (`surface`, `radius-xl`, поля 16): подпись «Расходы · 1–10 ноября» или «Расходы · за месяц» `label-medium` `text-secondary`, сумма `headline-large` Bold, плашка разницы («+3 600 ₽ · +9,2%»; `selected`, `radius-sm`, Bold) и строка сравнения `label-small` («К 1–10 октября · было 39 200 ₽», для целого месяца — «К октябрю · …», при нуле — «… · расходов не было»). «По категориям» — заголовок `title-small` Bold и подпись «доля расходов»; карточка строк: название SemiBold и сумма Bold, полоска высотой `size-progress-height` (подложка `selected`, заполнение `text-secondary`, ширина — доля), под ней разница с прошлым периодом и доля в процентах `label-small` `text-secondary`. Прошлый период 0 ₽ — вместо процента «в октябре не было». Категории без расходов не показываются; месяц без расходов — карточка «В этом месяце расходов пока нет». Цветных графиков, доходов и переводов нет.')


# ---------------------------------------------------------------- финансы, этап 4 (0.61, #231): меню, корзина, сверка, копия

def fin_window(title, body, actions=''):
    return (f'<div style="width:380px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden;flex-shrink:0">'
            f'<div style="display:flex;align-items:center;padding:12px 8px 0 16px"><span style="flex:1;font-size:22px;line-height:28px;font-weight:700">{title}</span>'
            f'<span style="width:36px;height:36px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{mi("close")}</span></div>'
            f'<div style="padding:0 16px">{body}</div><div style="padding:16px;display:flex;flex-direction:column;gap:8px">{actions}</div></div>')


def menu_row(ic, title, sub):
    return (f'<div style="display:flex;align-items:center;gap:12px;min-height:52px;padding:8px 0;border-bottom:1px solid var(--divider)"><span>{svg(ic, 20)}</span>'
            f'<span><div style="font-size:16px;line-height:24px;font-weight:600">{title}</div><div style="font-size:11px;line-height:16px;color:var(--text-secondary)">{sub}</div></span></div>')


def secondary_action(label):
    return f'<span style="display:flex;align-items:center;justify-content:center;border-radius:var(--radius-lg);background:var(--selected);padding:12px 0;font-size:14px;line-height:20px;font-weight:700">{label}</span>'


def win_text(t):
    return f'<div style="padding:8px 0;font-size:12px;line-height:16px;color:var(--text-secondary)">{t}</div>'


def two_actions(a, b):
    return f'<div style="display:flex;gap:8px"><span style="flex:1">{secondary_action(a)}</span><span style="flex:1">{primary_action(b)}</span></div>'


def finance_service():
    menu = fin_window('Финансы', menu_row('ic_finance_categories', 'Категории', 'Действующих: 13') + menu_row('ic_tool_delete', 'Корзина', 'Операций: 3') + menu_row('ic_finance_backup', 'Резервная копия', 'Последняя копия: 09.11.2026, 21:40'))
    account = fin_window('Счёт', fin_field('Название', 'Карта') + fin_field('Тип', 'Карта', True) + fin_field('Эмодзи', '💳', True) + fin_field('Остаток сейчас', '42 300 ₽')
                         + fin_field('Начальный остаток', '<span style="font-weight:400">30 000 ₽ · 01.11.2026</span>'), secondary_action('Остаток не сходится') + primary_action('Сохранить'))
    choose = fin_window('Остаток не сходится', menu_row('ic_finance_operations', 'Найти операцию', 'Открыть операции и исправить пропуск или ошибку')
                        + menu_row('ic_snack_restore', 'Изменить начальный остаток', 'Пересчитает остаток за всё время') + menu_row('ic_finance_wallet', 'Указать реальный остаток', 'Создаст корректировку на разницу')
                        + win_text('Карта · по приложению 42 300 ₽'))
    real = fin_window('Реальный остаток', '<div style="text-align:center;padding-top:12px;font-size:12px;line-height:16px;color:var(--text-secondary)">Сколько на счёте «Карта» на самом деле</div>'
                      '<div style="text-align:center;padding:12px 0;font-size:32px;line-height:40px;font-weight:700">42450 ₽</div>' + fin_field('По приложению', '<span style="font-weight:500">42 300 ₽</span>')
                      + fin_field('Корректировка', '+150 ₽') + fin_field('Дата', '<span style="font-weight:500">10.11.2026 · сегодня</span>') + win_text('Корректировка видна в операциях, но не входит в доходы и расходы.'), primary_action('Сохранить'))
    backup = fin_window('Резервная копия', win_text('В файл попадают счета, начальные остатки, категории, операции и корзина. Задачи — нет.') + win_text('Последняя копия: 09.11.2026, 21:40'),
                        primary_action('Сохранить в файл') + secondary_action('Восстановить из файла'))
    confirm = fin_window('Заменить данные финансов?', fin_field('В копии', '<span style="font-size:12px">счетов 3 · категорий 13 · операций 214</span>')
                         + fin_field('Сейчас', '<span style="font-size:12px;font-weight:400">счетов 3 · категорий 13 · операций 221</span>')
                         + win_text('Все текущие данные финансов будут заменены содержимым копии. Задачи не затрагиваются.'), two_actions('Отмена', 'Заменить'))
    return f'<div class="row" style="align-items:flex-start;flex-wrap:wrap">{menu}{account}{choose}{real}{backup}{confirm}</div>'


def finance_trash_screen():
    mini = lambda ic: f'<span style="width:36px;height:36px;border-radius:var(--radius-md);background:var(--background);display:flex;align-items:center;justify-content:center">{svg(ic, 18)}</span>'
    row = lambda t, sub, v: (f'<div style="display:flex;align-items:center;gap:8px;min-height:52px;padding:8px 16px;border-bottom:1px solid var(--divider)"><span style="flex:1;min-width:0"><div style="font-size:14px;line-height:20px;font-weight:600">{t}</div>'
                             f'<div style="font-size:11px;line-height:16px;color:var(--text-secondary)">{sub}</div></span><span style="font-size:14px;line-height:20px;font-weight:700">{v}</span>{mini("ic_snack_restore")}{mini("ic_tool_delete")}</div>')
    top = (f'<div style="display:flex;align-items:center;padding:8px 12px 0 0"><span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{mi("back")}</span>'
           '<span style="flex:1;font-size:22px;line-height:28px;font-weight:700">Корзина</span><span style="padding:8px;font-size:14px;line-height:20px;font-weight:700">Очистить</span></div>')
    body = (f'<div style="margin:8px 16px 0;border-radius:var(--radius-lg);background:var(--surface);overflow:hidden">{row("Кафе", "9 ноября · удалено сегодня", "− 620 ₽")}'
            f'{row("Карта → Наличные", "7 ноября · удалено вчера", "5 000 ₽")}{row("Продукты", "5 ноября · удалено 6 ноября", "− 1 350 ₽")}</div>'
            '<div style="padding:12px 20px;font-size:12px;line-height:16px;color:var(--text-secondary)">Операции в корзине не влияют на остатки и аналитику. Сами не удаляются — только вручную.</div>')
    return frame(f'{top}{body}', 640)


PREVIEWS['FinanceService'] = lambda: doc('Окна', 980, 'Финансы, этап 4 (0.61): меню шестерёнки, окно счёта, сверка, резервная копия', finance_service())
PREVIEWS['ScreenFinanceTrash'] = lambda: doc('Экраны', 670, 'Финансы, этап 4 (0.61): корзина операций', f'<div class="row" style="align-items:flex-start">{finance_trash_screen()}</div>')
README_MORE['FinanceService'] = ('Служебные окна финансов: меню, счёт, сверка, резервная копия (0.61).',
    'Этап 4 (#231; макеты design/approved/217, экраны 2.5–2.8, 3.1, 3.7–3.8). Все — окна `FinanceWindow`. Меню шестерёнки и выбор способа сверки — строки `size-operation-row`: значок 20, название `body-large` SemiBold, подпись `label-small` `text-secondary`, линия `divider` между строками. Окно счёта: название, тип, эмодзи; «Остаток сейчас» Bold и «Начальный остаток · дата» только показаны; серая кнопка «Остаток не сходится» над чёрной «Сохранить». Окна сверки: подпись-вопрос по центру, крупная сумма, строки «Было указано» / «Остаток сейчас станет» или «По приложению» / «Корректировка» (со знаком, Bold) / «Дата», пояснение `body-small`. Резервная копия: пояснение, строка «Последняя копия», чёрная «Сохранить в файл» и серая «Восстановить из файла». Подтверждение замены и удаления: две кнопки в ряд — серая «Отмена» слева, чёрная справа. Красного цвета в финансах нет.')
README_MORE['ScreenFinanceTrash'] = ('Экран «Корзина» финансов (0.61).',
    'Вложенный экран без баров: стрелка назад, заголовок, справа текстовая кнопка «Очистить» (только если корзина не пуста). Карточка `surface` `radius-lg`: в строке название операции SemiBold, под ним «9 ноября · удалено сегодня» `label-small` `text-secondary`, сумма Bold и две кнопки 36×36 (`background`, `radius-md`, значок 18): «восстановить» `ic_snack_restore` и «удалить навсегда» `ic_tool_delete`. Под карточкой пояснение. Восстановление — сразу; удаление одной операции и очистка — через окно подтверждения. Пустая корзина — «Корзина пуста» по центру.')


# ---------------------------------------------------------------- здоровье (0.71, #257): вес

def health_bar():
    """Нижняя панель «Здоровья» (0.71): «+» слева, одна страница «Вес», справа кнопка модуля с обводкой (сердце)."""
    add = f'<span style="width:72px;height:34px;border-radius:var(--radius-xl);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center">{mi("add", 22)}</span>'
    page = f'<span style="width:64px;height:34px;display:flex;align-items:center;justify-content:center;color:var(--accent)">{svg("ic_health_weight", 22)}</span>'
    mod = (f'<span style="width:72px;height:34px;border-radius:var(--radius-xl);box-shadow:inset 0 0 0 2px var(--accent);display:flex;align-items:center;justify-content:center;'
           f'color:var(--accent)">{svg("ic_module_health", 20)}</span>')
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:44px;background:var(--surface);border-top:1px solid var(--divider);display:flex;align-items:center;'
            f'justify-content:space-between;padding:0 12px">{add}{page}{mod}</div>')


def health_chart():
    pts = [83.2, 83.4, 83.1, 83.0, 83.3, 82.9, 82.9, 82.7, 82.8, 82.6, 82.9, 82.5, 82.6, 82.4, 82.6, 82.5, 82.7, 82.6, 82.4]
    w, h, lo, hi = 330, 150, 77.6, 83.8
    xy = [(8 + i * (w - 48) / (len(pts) - 1), 8 + (hi - v) / (hi - lo) * (h - 24)) for i, v in enumerate(pts)]
    gy = 8 + (hi - 78.0) / (hi - lo) * (h - 24)
    line = ' '.join(f'{"M" if i == 0 else "L"}{x:.1f},{y:.1f}' for i, (x, y) in enumerate(xy))
    dots = ''.join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{5 if i == len(xy) - 1 else 3}" fill="{"var(--text-primary)" if i == len(xy) - 1 else "var(--surface)"}" stroke="var(--text-primary)" stroke-width="1.6"/>' for i, (x, y) in enumerate(xy))
    return (f'<svg width="{w}" height="{h}" viewBox="0 0 {w} {h}" style="display:block;margin:4px 16px 0"><line x1="8" x2="{w - 40}" y1="{gy:.1f}" y2="{gy:.1f}" stroke="var(--goal)" stroke-width="1.4" stroke-dasharray="4 4"/>'
            f'<text x="{w - 34}" y="{gy + 4:.1f}" font-size="10" font-weight="700" fill="var(--goal)">78,0</text><path d="{line}" fill="none" stroke="var(--text-primary)" stroke-width="2" stroke-linejoin="round"/>{dots}</svg>')


def health_screen():
    seg = ''.join(f'<span style="flex:1;height:36px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:{700 if i == 0 else 500};'
                  f'background:{"var(--selected)" if i == 0 else "transparent"};color:{"var(--text-primary)" if i == 0 else "var(--text-secondary)"}">{t}</span>' for i, t in enumerate(['Месяц', '3 мес', 'Год', 'Всё']))

    def row(day, diff, w, comment=''):
        sub = f'<div style="font-size:12px;line-height:16px;color:var(--text-secondary)">{comment}</div>' if comment else ''
        return (f'<div style="display:flex;align-items:center;padding:6px 16px;min-height:52px;box-sizing:border-box;border-bottom:1px solid var(--divider)"><span style="flex:1;font-size:14px;line-height:20px">{day}{sub}</span>'
                f'<span style="width:48px;text-align:right;padding-right:8px;font-size:12px;color:{"var(--income)" if diff.startswith("−") else "var(--text-secondary)"}">{diff}</span><b style="font-size:14px">{w}</b></div>')
    body = (f'<div style="padding:14px 16px 6px"><span style="font-size:45px;line-height:52px;font-weight:700">82,4</span><span style="font-size:16px;color:var(--text-secondary);margin-left:4px">кг</span>'
            f'<div style="font-size:12px;line-height:16px;color:var(--text-secondary)"><b style="color:var(--income)">−0,8 кг</b> за месяц · последняя запись сегодня</div></div>'
            f'<div style="display:flex;align-items:center;margin:0 16px;padding:10px 0;border-bottom:1px solid var(--divider)"><span style="flex:1;font-size:14px;line-height:20px;font-weight:700">Цель 78,0 кг · осталось 4,4 кг'
            f'<div style="font-size:12px;line-height:16px;font-weight:400;color:var(--text-secondary)">≈ к 12 марта по текущему темпу · ИМТ 25,4 — выше нормы</div></span>{mi("right", 20)}</div>'
            f'<div style="display:flex;gap:4px;margin:12px 16px 4px">{seg}</div>{health_chart()}'
            f'<div style="padding:10px 16px 2px;font-size:12px;font-weight:700;color:var(--text-secondary)">Записи</div>'
            f'{row("16 ноября · сегодня", "−0,2", "82,4")}{row("15 ноября · вчера", "−0,1", "82,6")}{row("14 ноября", "+0,2", "82,7")}{row("9 ноября", "−0,2", "82,4", "после тренировки")}')
    return frame(f'{body}{health_bar()}', 760)


def health_weight_dialog():
    drum = lambda vals, big: ''.join(f'<div style="height:40px;line-height:40px;text-align:center;font-size:{34 if i == 2 else 22 if i in (1, 3) else 17}px;font-weight:{700 if i == 2 else 400};'
                                      f'color:{"var(--text-primary)" if i == 2 else "var(--text-secondary)" if i in (1, 3) else "var(--inactive)"};'
                                      f'{"background:var(--selected);border-radius:var(--radius-lg)" if i == 2 else ""}">{v}</div>' for i, v in enumerate(vals))
    drums = (f'<div style="display:flex;justify-content:center;align-items:center;gap:4px;margin:8px 0"><div style="width:84px">{drum([80, 81, 82, 83, 84], 1)}</div><b style="font-size:34px;margin-top:-6px">,</b>'
             f'<div style="width:56px">{drum([2, 3, 4, 5, 6], 1)}</div><span style="margin-left:12px;font-size:16px;color:var(--text-secondary)">кг</span></div>')
    field = lambda l, v: (f'<div style="display:flex;align-items:center;min-height:48px;border-bottom:1px solid var(--divider)"><span style="flex:1;font-size:14px;color:var(--text-secondary)">{l}</span>'
                          f'<b style="font-size:14px">{v}</b></div>')
    return (f'<div style="width:340px;border-radius:var(--radius-nav);background:var(--surface);padding:12px 16px 16px;box-sizing:border-box;{SHADOW}"><div style="display:flex;align-items:center;height:40px"><b style="flex:1;font-size:22px">Вес</b>'
            f'<span style="color:var(--text-secondary)">{mi("close", 24)}</span></div>{drums}<div style="font-size:12px;color:var(--text-secondary);padding:2px 0 8px">Заранее стоит последний вес — 82,6 кг</div>'
            f'{field("Дата", "Сегодня, 16 ноября")}{field("Комментарий", "необязательно")}<div style="display:flex;gap:8px;margin-top:14px"><span style="flex:1">{primary_action("Отмена").replace("var(--accent)", "var(--selected)").replace("var(--on-accent)", "var(--text-primary)")}</span>'
            f'<span style="flex:1">{primary_action("Сохранить")}</span></div></div>')


PREVIEWS['ScreenHealth'] = lambda: doc('Экраны', 800, 'Здоровье, вес (0.71): главный экран с графиком и окно ввода веса барабанами',
                                       f'<div class="row" style="align-items:flex-start">{health_screen()}<div style="padding-top:140px">{health_weight_dialog()}</div></div>')
README_MORE['ScreenHealth'] = ('Экран «Здоровье → Вес» (0.71).',
    'Третий модуль (#257; макеты design/approved/257). Белый плоский экран, как «Неделя»: текущий вес `display-medium` Bold, под ним изменение за период (снижение — `income`), строка цели (название Bold, подпись `text-secondary`: остаток, прогноз даты, ИМТ), переключатель периода «Месяц / 3 мес / Год / Всё» (выбранный на `selected`), график `size-health-chart-height` (линия `size-health-line`, точки `size-health-point`, последняя закрашена `size-health-point-last`, цель — пунктир `goal` с подписью справа), записи: дата и комментарий, разница с прошлой записью, вес Bold; строка не ниже `size-operation-row`. Окно ввода веса — `FinanceWindow` с двумя барабанами: строка `size-health-drum-item`, видно пять строк, выбранная крупная на `selected`. Нижняя панель — общая (#241): «+», одна страница «Вес» (значок весов), кнопка модуля с сердцем.')


# ---------------------------------------------------------------- хаб «Главная» (0.92, #329)

def hub_screen():
    bar = ('<div style="padding:16px 16px 0"><div style="position:relative;height:26px;border-radius:var(--radius-sm);background:var(--selected);overflow:hidden">'
           '<div style="width:68%;height:100%;background:var(--text-primary)"></div>'
           '<span style="position:absolute;left:10px;top:4px;font-size:12px;font-weight:700;color:var(--surface)">57 000 ₽</span>'
           '<span style="position:absolute;left:62%;top:0;bottom:0;width:3px;background:var(--text-secondary);border:1px solid var(--surface)"></span>'
           '<span style="position:absolute;right:10px;top:4px;font-size:12px;font-weight:700">80 000 ₽</span></div>'
           '<div style="display:flex;padding-top:6px;font-size:12px;color:var(--text-secondary)"><span style="flex:1">осталось 23 000 ₽</span><span>прогноз 68 000 ₽</span></div>'
           '<div style="display:flex;gap:6px;padding-top:10px;font-size:12px;font-weight:700"><span style="padding:4px 8px">128k ₽</span>'
           + ''.join(f'<span style="padding:4px 8px;border-radius:var(--radius-sm);background:var(--selected)">{a}</span>' for a in ('💳 42k', '👛 6k', '🏦 80k'))
           + '</div></div>')
    label = lambda t, c='var(--text-secondary)': f'<div style="padding:12px 16px 2px;font-size:13px;font-weight:700;color:{c}">{t}</div>'
    tasks = ('<div style="padding:0 4px">' + label('Просрочено', 'var(--danger)')
             + B.trow('Вчера', 'var(--inactive)', 'Оплатить интернет', over=True, top=False, bottom=False)
             + label('Сегодня') + B.trow('10:00', 'var(--ring-medium)', 'Созвон с командой', top=False, bottom=False)
             + B.trow('14:00', 'var(--inactive)', 'Купить продукты', top=False, bottom=False)
             + label('Завтра') + B.trow('09:30', 'var(--inactive)', 'Стоматолог', top=False, bottom=False) + '</div>')
    card = lambda meta, t, unread=False: (f'<div style="min-height:76px;border-radius:var(--radius-lg);background:var(--selected);padding:8px 10px;box-sizing:border-box">'
                                          f'<div style="font-size:10.5px;color:var(--text-secondary)">{meta}</div><div style="font-size:13px;line-height:17px;font-weight:{700 if unread else 500}">{t}</div></div>')
    news = ('<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;padding:14px 16px 6px">'
            + card('09:05 · Футбол · Новое', 'Бавария обыграла Боруссию в дерби', True) + card('08:40 · Баскетбол · Новое', 'Дончич набрал трипл-дабл', True)
            + card('08:12 · Футбол', 'Расписание тура Бундеслиги') + card('07:55 · Баскетбол', 'Вембаньяма — игрок недели') + '</div>'
            '<div style="display:flex;justify-content:center;gap:6px">' + ''.join(f'<span style="width:6px;height:6px;border-radius:50%;background:var(--{c})"></span>' for c in ('text-primary', 'inactive', 'inactive')) + '</div>')
    return frame(f'{bar}{tasks}{news}', 760)


PREVIEWS['ScreenHub'] = lambda: doc('Экраны', 790, 'Хаб «Главная» (0.92): полоса лимита и счета, задачи, новости 2 × 2', hub_screen())
README_MORE['ScreenHub'] = ('Хаб «Главная» (0.92).',
    'Стартовый экран и первая вкладка панели — домик `ic_module_hub` (#329; макеты design/approved/329, вариант A + N1). Без шапки, заголовков блоков и линий: '
    'сверху полоса лимита Г (`size-limit-bar`) и чипы счетов (до четырёх), посередине задачи «Просрочено» (`danger`) · «Сегодня» · «Завтра» — строки окна дня без линии «сейчас», '
    'блок занимает свободное место и прокручивается внутри; пусто — «На сегодня и завтра задач нет» и «+ Задача». Внизу новости страницами 2 × 2 со свайпом: '
    'карточка `size-hub-news-card` (мин. высота) на `selected`, `radius-lg`, мета 10,5 sp `text-secondary`, заголовок 13 sp до трёх строк, непрочитанные Bold; '
    'точки страниц `size-hub-dot`. «+» на хабе — меню «Задача / Расход / Вес».')


# ---------------------------------------------------------------- плавающая панель и выбор раздела (0.95, #336)

def nav_capsule():
    icon = lambda name: f'<span style="width:30px;height:30px;border-radius:50%;display:grid;place-items:center;color:#fff">{svg(name, 18)}</span>'
    return ('<div style="display:inline-flex;align-items:center;gap:2px;padding:3px;border-radius:999px;background:#16171D;box-shadow:0 4px 14px rgb(0 0 0 / .2)">'
            '<span style="display:flex;align-items:center;gap:3px;padding:0 4px 0 8px;color:#fff"><b style="font-size:16px">25</b>'
            '<span style="display:grid;font-size:9px;line-height:1;font-weight:700;color:#A3A3A3"><span>ср</span><span>ноя</span></span></span>'
            f'<span style="width:30px;height:30px;border-radius:50%;background:#2C2D33;display:grid;place-items:center;color:#fff">{svg("ic_nav_grid", 18)}</span>'
            f'{icon("ic_nav_sliders")}'
            f'<span style="width:30px;height:30px;border-radius:50%;background:#862CE0;display:grid;place-items:center;color:#fff">{svg("ic_nav_plus", 18)}</span></div>')


def picker_tiles():
    names = [('ic_nav_grid', 'Главная'), ('ic_widget_all', 'Предстоящее'), ('ic_tab_week', 'Неделя'), ('ic_tab_lists', 'Списки'),
             ('ic_module_finance', 'Финансы'), ('ic_module_health', 'Здоровье'), ('ic_module_news', 'Новости'), ('ic_nav_sliders', 'Настройки')]
    tile = lambda ic, t, on: (f'<div style="height:76px;border-radius:var(--radius-lg);background:{"var(--priority-high-bg)" if on else "var(--selected)"};'
                              f'color:{"var(--priority-high-text)" if on else "var(--text-primary)"};display:grid;place-items:center;align-content:center;gap:4px;font-size:12px;font-weight:700">{svg(ic, 24)}{t}</div>')
    return ('<div style="width:380px;padding:12px;border-radius:var(--radius-xl);background:var(--surface);display:grid;grid-template-columns:repeat(3,1fr);gap:8px">'
            + ''.join(tile(ic, t, t == 'Неделя') for ic, t in names) + '</div>')


PREVIEWS['NavCapsule'] = lambda: doc('Панель и поля', 360, 'Плавающая панель (0.95): дата, разделы, настройки, «+»; выбор раздела плитками',
                                     f'<div style="display:grid;gap:16px;justify-items:end;width:404px">{picker_tiles()}{nav_capsule()}</div>')
README_MORE['NavCapsule'] = ('Плавающая панель и выбор раздела (0.95).',
    'Решения PM 25.11.2026 (#336, design/approved/336). Вместо нижней панели с вкладками — тёмная капсула (`NavBarTokens.background`) справа внизу, '
    'отступ `size-nav-edge` (12): дата «25» 16 sp и «ср / ноя» 9 sp, хаб — сетка 2 × 2 на подложке `NavBarTokens.selected` (открывает выбор раздела), '
    'настройки — ползунки, «+» — фиолетовый круг `NavBarTokens.plus`. Кнопки `size-nav-button` (30), поля `size-nav-pad` (3) — высота 36, значки `size-nav-icon` (18). '
    'Выбор раздела — плитки снизу по три в ряд, `size-picker-tile` (76): Главная, Предстоящее, Неделя, Списки (в виджете — Входящие), Финансы, Здоровье, Новости, '
    'Настройки; выбранный — `priority-high-bg` с фиолетовым. В виджете — та же капсула (фоны картинками), разделы — окном поверх рабочего стола.')
