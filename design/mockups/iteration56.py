"""Макеты итерации 56: раздел «Списки» — карточки групп, строка значков внизу, эмодзи у папки, новое окно создания (T-123…T-127).

Решения PM (09.11.2026):
- Заголовка «Списки» нет. Три группы карточками: системные (Входящие, Когда-нибудь, Без списка — нельзя удалить или изменить),
  мои списки и папки, архив (Выполненные, Не буду делать, Корзина). Названия жирнее, у каждого — число задач, если оно не 0.
- Поиск, «+ папка», «+ список», настройки — значками в строке, закреплённой над нижней панелью.
- У папки есть эмодзи. Окно создания списка и папки — сразу с выбором эмодзи, в стиле окон приложения.

Запуск: python3 design/mockups/iteration56.py <папка project>   → canvas.json, Main.dc.html, Create.dc.html
"""
import datetime
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import iteration49 as I  # noqa: E402
import iteration55 as N  # noqa: E402

D, B, svg, mi = I.D, I.B, I.svg, I.D.mi
H = I.H_PHONE

_LINE = ('<svg width="{s}" height="{s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" '
         'stroke-linejoin="round" aria-hidden="true">{p}</svg>')
# Новые контурные значки (линия 2, как ic_widget_*): «новая папка» и «новый список».
FOLDER_PLUS = ('<path d="M12 10v6M9 13h6"/><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4'
               'a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/>')
LIST_PLUS = '<path d="M11 12H3M16 6H3M16 18H3M18 9v6M21 12h-6"/>'
CHEVRON = '<path d="M6 9l6 6 6-6"/>'
MORE = ('<svg width="{s}" height="{s}" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><circle cx="12" cy="5" r="1.8"/>'
        '<circle cx="12" cy="12" r="1.8"/><circle cx="12" cy="19" r="1.8"/></svg>')


def line(p, s=24):
    return _LINE.format(s=s, p=p)


def emo(e, px=22, w=26):
    return B.EMOJI_SPAN.format(px=px, w=w, e=e)


# ------------------------------------------------------------------ строки и карточки

def row(lead, name, n=0, divider=True, indent=0, trail=''):
    """Строка карточки: значок/эмодзи, название SemiBold (жирнее, чем было), число задач справа — только если не 0."""
    count = f'<span style="font-size:14px;font-weight:600;color:var(--text-secondary)">{n}</span>' if n else ''
    sep = f'<div style="position:absolute;left:{54 + indent}px;right:0;bottom:0;height:1px;background:var(--divider)"></div>' if divider else ''
    return (f'<div style="position:relative;display:flex;align-items:center;gap:12px;min-height:52px;padding:0 16px 0 {16 + indent}px;color:var(--text-secondary)">'
            f'<span style="width:26px;display:flex;justify-content:center;flex-shrink:0">{lead}</span>'
            f'<span style="flex:1;min-width:0;font-size:16px;line-height:22px;font-weight:600;color:var(--text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{name}</span>'
            f'{count}{trail}{sep}</div>')


def folder_row(e, name, n, open_=True, divider=True):
    """Папка: эмодзи (новое — 0.56), название, число, стрелка «свернуть» и «⋮»."""
    arrow = f'<span style="display:flex;color:var(--text-secondary);transform:rotate({0 if open_ else -90}deg)">{line(CHEVRON, 20)}</span>'
    dots = f'<span style="display:flex;color:var(--text-secondary);margin-right:-6px">{MORE.format(s=20)}</span>'
    return row(emo(e), name, n, divider, trail=f'<span style="display:flex;gap:6px;align-items:center;margin-left:4px">{arrow}{dots}</span>')


def card(rows, mark=False):
    ring = 'box-shadow:0 0 0 2px var(--accent);' if mark else ''
    return f'<div style="margin:0 12px 12px;border-radius:var(--radius-lg);background:var(--surface);overflow:hidden;{ring}">{rows}</div>'


SYSTEM = card(row(emo('📥'), 'Входящие', 50) + row(emo('💭'), 'Когда-нибудь', 7) + row(emo('📌'), 'Без списка', 2, divider=False))
MINE = card(row(emo('💡'), 'Планы', 12) + row(emo('🏠'), 'Без даты') + row(emo('💼'), 'Работа', 14) + row(emo('💲'), 'Подписки и услуги', 20)
            + folder_row('🧩', 'Личное', 5) + row(emo('🏠'), 'Дом', 3, indent=24) + row(emo('🏀'), 'Спорт', 1, indent=24)
            + row(emo('🎮'), 'Развлечения', 1, indent=24) + row(emo('🗂️'), 'Другое', divider=False))
ARCHIVE = card(row(mi('check', 24), 'Выполненные') + row(I.wont(24), 'Не буду делать', 4) + row(D.TRASH_ICON(24), 'Корзина', 20, divider=False))


def tools(mark=False):
    """Строка значков над нижней панелью (закреплена): поиск, новая папка, новый список, настройки."""
    def b(ic):
        return (f'<span style="flex:1;height:44px;border-radius:var(--radius-lg);background:var(--surface);color:var(--text-primary);'
                f'display:flex;align-items:center;justify-content:center">{ic}</span>')
    ring = 'box-shadow:0 0 0 2px var(--accent);border-radius:var(--radius-lg);' if mark else ''
    fade = 'background:linear-gradient(to bottom,color-mix(in srgb,var(--background) 0%,transparent),var(--background) 30%)'
    return (f'<div style="position:absolute;left:0;right:0;bottom:57px;padding:14px 12px 8px;{fade}">'
            f'<div style="display:flex;gap:8px;{ring}">{b(N.search_icon(24))}{b(line(FOLDER_PLUS))}{b(line(LIST_PLUS))}{b(N.gear_icon(24))}</div></div>')


def lists_new(scroll=0, mark_tools=False):
    body = f'<div style="padding-top:16px;margin-top:-{scroll}px">{SYSTEM}{MINE}{ARCHIVE}<div style="height:130px"></div></div>'
    return D.frame(f'<div style="position:absolute;inset:0;overflow:hidden">{body}</div>{tools(mark_tools)}{N.nav3("upcoming", lists=True)}', H)


def lists_before():
    """Было (0.55): заголовок, текстовые «+ Папка», «+ Список», шестерёнка, поле поиска, строки без групп."""
    head = (f'<div style="display:flex;align-items:center;padding:12px 4px 4px 16px"><span style="flex:1;font-size:28px;line-height:36px;font-weight:700">Списки</span>'
            f'{D.btn_text("+ Папка")}{D.btn_text("+ Список")}'
            f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{N.gear_icon(24)}</span></div>')
    search = (f'<div style="margin:4px 16px 8px;height:48px;border-radius:var(--radius-lg);background:var(--surface);display:flex;align-items:center;gap:12px;'
              f'padding:0 16px;color:var(--text-secondary)">{N.search_icon(22)}<span style="font-size:16px">Поиск задач</span></div>')
    plain = lambda ic, t, n='': (f'<div style="display:flex;align-items:center;padding:12px 16px;color:var(--text-secondary)">{ic}'
                                 f'<span style="flex:1;padding-left:12px;font-size:16px;line-height:24px;color:var(--text-primary)">{t}</span>'
                                 f'<span style="font-size:14px">{n}</span></div>')
    rows = (plain(emo('📥'), 'Входящие') + plain(emo('💭'), 'Когда-нибудь') + plain(emo('📌'), 'Без списка') + plain(mi('check'), 'Выполненные')
            + plain(I.wont(24), 'Не буду делать', 4) + plain(D.TRASH_ICON(24), 'Корзина', 20) + plain(emo('🗂️'), 'Другое') + plain(emo('💡'), 'Планы', 12)
            + plain(emo('💼'), 'Работа', 14))
    return D.frame(f'{head}{search}{rows}{N.nav3("upcoming", lists=True)}', H)


def phone_lists(scroll=0, mark_tools=False):
    return I.phone(lists_new(scroll, mark_tools))


# ------------------------------------------------------------------ окно создания

QUICK = '📁 💼 🏠 🛒 ❤️ 💪 📚 💰 ✈️ 💡 ⭐ 🎮 🏀 🎯 🧩'.split()


def field(text, focused=True, placeholder=False):
    border = '2px solid var(--accent)' if focused else '1px solid var(--divider)'
    color = 'var(--inactive)' if placeholder else 'var(--text-primary)'
    caret = '<span style="width:2px;height:22px;background:var(--accent);margin-left:1px"></span>' if focused and not placeholder else ''
    return (f'<div style="flex:1;min-width:0;height:56px;box-sizing:border-box;border-radius:var(--radius-lg);border:{border};background:var(--background);'
            f'display:flex;align-items:center;padding:0 14px;font-size:17px;font-weight:700;color:{color}">{text}{caret}</div>')


def emoji_button(e):
    """Крупная плашка выбранного эмодзи слева от названия: нажатие — все эмодзи (окно «Эмодзи списка»)."""
    return (f'<span style="position:relative;width:56px;height:56px;flex-shrink:0;border-radius:var(--radius-lg);background:var(--selected);'
            f'display:flex;align-items:center;justify-content:center">{emo(e, 30, 34)}'
            f'<span style="position:absolute;right:-4px;bottom:-4px;width:20px;height:20px;border-radius:10px;background:var(--surface);border:1px solid var(--divider);'
            f'display:flex;align-items:center;justify-content:center;color:var(--text-secondary)">{line(CHEVRON, 14)}</span></span>')


def quick_grid(sel):
    def tile(e):
        on = e == sel
        return (f'<span style="width:38px;height:38px;box-sizing:border-box;border-radius:var(--radius-md);background:{"var(--selected)" if on else "transparent"};'
                f'border:{"2px solid var(--accent)" if on else "1px solid transparent"};display:flex;align-items:center;justify-content:center">{emo(e, 21, 24)}</span>')
    more = ('<span style="width:38px;height:38px;box-sizing:border-box;border-radius:var(--radius-md);border:1px solid var(--divider);display:flex;align-items:center;'
            'justify-content:center;font-size:18px;font-weight:800;color:var(--text-secondary)">…</span>')
    return f'<div style="display:flex;flex-wrap:wrap;gap:2px">{"".join(tile(e) for e in QUICK)}{more}</div>'


def label(t):
    return f'<div style="font-size:12px;line-height:16px;font-weight:700;letter-spacing:.04em;color:var(--text-secondary);padding:14px 0 6px">{t.upper()}</div>'


def create_dialog(kind='list', name='Покупки', emoji='🛒', folders=True, placeholder=False):
    title = 'Новый список' if kind == 'list' else 'Новая папка'
    top = f'<div style="display:flex;gap:12px;align-items:center">{emoji_button(emoji)}{field(name, placeholder=placeholder)}</div>'
    body = top + label('Эмодзи') + quick_grid(emoji)
    if kind == 'list' and folders:
        body += label('Папка') + f'<div style="display:flex;gap:8px;flex-wrap:wrap">{D.chip("Без папки", True)}{D.chip("🧩 Личное")}{D.chip("🎯 Проекты")}</div>'
    return D.dlg(title, f'<div style="padding:8px 24px 16px">{body}</div>', buttons=('Отмена', 'Создать'), width=372)


def edit_folder_dialog():
    top = f'<div style="display:flex;gap:12px;align-items:center">{emoji_button("🧩")}{field("Личное")}</div>'
    body = top + label('Эмодзи') + quick_grid('🧩')
    return D.dlg('Папка', f'<div style="padding:8px 24px 16px">{body}</div>', buttons=('Отмена', 'Сохранить'), width=372)


def over_lists(dialog):
    return I.phone(lists_new() + I.SCRIM + f'<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center">{dialog}</div>')


def step_list():
    return over_lists(create_dialog('list'))


def step_folder():
    return over_lists(create_dialog('folder', 'Проекты', '🎯'))


def step_folder_edit():
    return over_lists(edit_folder_dialog())


def step_before_dialog():
    """Было: окно Material с полем-рамкой и чипами папок, без эмодзи (эмодзи — только потом, через «⋮» списка)."""
    fieldold = ('<div style="height:56px;border-radius:4px;border:2px solid var(--accent);display:flex;align-items:center;padding:0 16px;'
                'font-size:16px;color:var(--text-primary)">Покупки</div>')
    chips = f'<div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:12px">{D.chip("Без папки", True)}{D.chip("Личное")}</div>'
    dlg = D.alert('Новый список', fieldold + chips, [('Отмена', 'var(--accent)'), ('Создать', 'var(--accent)')])
    return I.phone(lists_before() + I.SCRIM + f'<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center">{dlg}</div>')


# ------------------------------------------------------------------ сборка

BOARDS = [
    ('Main.dc.html', '«Списки»', '«Списки»: группы карточками и значки внизу — итерация 56',
     'Заголовка нет. Три карточки: системные (их нельзя удалить или изменить), мои списки и папки, архив. Названия жирнее, число задач — если не 0. '
     'Поиск, новая папка, новый список и настройки — значками в строке над нижней панелью; строка всегда на месте, списки прокручиваются под ней.',
     [('1. Было (0.55)', 'Заголовок, текстовые «+ Папка» / «+ Список», поле поиска; все строки одним столбиком, у «Входящих» нет числа.',
       lambda: I.phone(lists_before())),
      ('2. Стало', 'Карточки: 📥 💭 📌 · мои списки и папка «🧩 Личное» (эмодзи у папки — новое) · ✓ ⊗ 🗑. Внизу — 🔍, новая папка, новый список, ⚙ (обведены).',
       lambda: phone_lists(mark_tools=True)),
      ('3. Пролистали вниз', 'Строка значков остаётся над панелью; последняя карточка — архив.', lambda: phone_lists(scroll=330))]),
    ('Create.dc.html', 'Окно создания', 'Окно создания списка и папки — итерация 56',
     'Окно в стиле приложения (как «Список», «Приоритет»): название жирным, слева крупная плашка эмодзи — нажатие открывает все эмодзи; '
     'ниже быстрый выбор из 15 и «…». У списка — ещё выбор папки. Кнопки «Отмена» и чёрная «Создать».',
     [('1. Было', 'Окно Material: поле с рамкой и чипы папок, эмодзи выбрать нельзя — только потом через «⋮» списка.', step_before_dialog),
      ('2. Новый список', 'Значок «новый список» внизу → окно: 🛒 + «Покупки», быстрый выбор эмодзи, папка. «…» и плашка эмодзи — полное окно «Эмодзи списка».', step_list),
      ('3. Новая папка', 'Значок «новая папка» → то же окно без выбора папки; по умолчанию 📁.', step_folder),
      ('4. Изменить папку', '«⋮» у папки → «Изменить»: то же окно с «Сохранить» — можно сменить название и эмодзи.', step_folder_edit)]),
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
    canvas = {'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Итерация 56 — Списки', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': boards, 'order': order,
              'notes': {'head': {'x': 0, 'y': -300, 'text': '«Списки»: карточки групп, значки внизу, эмодзи у папки, окно создания (итерация 56)', 'kind': 'title1', 'maxW': 1900}},
              'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', [(n, b['w'], b['h']) for n, b in boards.items()])


if __name__ == '__main__':
    build(sys.argv[1])
