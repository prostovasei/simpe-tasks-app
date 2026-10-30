"""Дизайн-система Simple Tasks для Claude Design (итерация 35): собирает файлы `project/` из кода приложения.

Значения цветов, отступов, радиусов и размеров читаются из Kotlin (`designsystem/tokens`), значки — из векторных
ресурсов, шрифт — из `res/font`; здесь только описания (usage), состав компонентов и их превью.
Запуск: python3 design/system/build_design_system.py <папка вывода>  → <папка>/project/…
"""
import json, re, shutil, sys, datetime
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
MAIN = REPO / 'app/src/main'
TOKENS = MAIN / 'java/com/prostovasei/simpletasks/designsystem/tokens'
RES = MAIN / 'res'
OUT = Path(sys.argv[1]) / 'project'

# ---------------------------------------------------------------- чтение кода


def kebab(name):
    return re.sub(r'(?<!^)(?=[A-Z])', '-', name).lower()


def hexcolor(h):
    h = h.upper().rjust(8, '0')
    a, rgb = h[:2], h[2:]
    return '#' + (rgb if a == 'FF' else rgb + a).lower()


color_src = (TOKENS / 'ColorTokens.kt').read_text()
dim_src = (TOKENS / 'DimensionTokens.kt').read_text()

COLORS = {}  # имя -> (светлая, тёмная)
for m in re.finditer(r'val (\w+) = ThemedColor\(light = 0x(\w+), dark = 0x(\w+)\)', color_src):
    COLORS[kebab(m[1])] = (hexcolor(m[2]), hexcolor(m[3]))
for m in re.finditer(r'val (high|medium|low|none) = PriorityColorTokens\(\s*container = ThemedColor\(light = 0x(\w+), dark = 0x(\w+)\),\s*content = ThemedColor\(light = 0x(\w+), dark = 0x(\w+)\)', color_src):
    COLORS[f'priority-{m[1]}-bg'] = (hexcolor(m[2]), hexcolor(m[3]))
    COLORS[f'priority-{m[1]}-text'] = (hexcolor(m[4]), hexcolor(m[5]))
for m in re.finditer(r'val (high|medium|low|none) = CheckTokens\(ring = ThemedColor\(light = 0x(\w+), dark = 0x(\w+)\), fill = ThemedColor\(light = 0x(\w+), dark = 0x(\w+)\)\)', color_src):
    COLORS[f'ring-{m[1]}'] = (hexcolor(m[2]), hexcolor(m[3]))
    COLORS[f'ring-{m[1]}-fill'] = (hexcolor(m[4]), hexcolor(m[5]))
LIST_PRESETS = re.findall(r'"(#[0-9A-Fa-f]{6})"', color_src.split('presets')[1].split(')')[0])
LIST_NAMES = ['red', 'orange', 'yellow', 'green', 'teal', 'blue', 'purple', 'pink', 'gray']


def consts(obj):
    body = dim_src.split(f'object {obj} {{')[1].split('\n}\n')[0]
    return {m[1]: int(m[2]) for m in re.finditer(r'const val (\w+) = (\d+)', body)}


SPACING, RADIUS, SIZE = consts('SpacingTokens'), consts('RadiusTokens'), consts('SizeTokens')

# ---------------------------------------------------------------- описания (usage)

COLOR_USAGE = {
    'background': 'Фон экранов и виджета. Под белыми плашками и окнами.',
    'surface': 'Плашки задач, окна, шторка, нижняя панель, неактивные кнопки панели.',
    'text-primary': 'Основной текст и тёмные значки на `surface` и `background`.',
    'text-secondary': 'Описания, подписи, значки кнопок панели, колонка времени у выполненных на `surface` и `background`.',
    'accent': 'Главный зелёный: галочка «Готово», даты и выбранные значки на `surface` и `selected`, «сейчас» на шкале. На `selected` в светлой теме 4,3:1 (ниже 4,5): значение из приложения сохранено, для текста на `selected` берут жирное начертание от 14 px.',
    'accent-soft': 'Бледный зелёный: подсветка найденного в поиске, кольцо сегодняшнего дня.',
    'on-accent': 'Текст и значки на заливке `accent`.',
    'on-list-square': 'Значок поверх квадратика цвета списка.',
    'inbox-list': 'Квадратик списка «Входящие»: у него нет своего цвета.',
    'inactive': 'Рамка пустой галочки без приоритета, бледные значки (нет даты) и подсказки полей на `surface`. Контраст 2,2:1 (светлая) и 2,9:1 (тёмная): только декор и подсказки, не основной текст.',
    'overdue': 'Просрочка: красные день и время. Не для кнопок удаления: для них `danger`.',
    'divider': 'Тонкие линии, рамки кнопок и полей, линия шкалы.',
    'selected': 'Мягкая подсветка выбранного: кнопки панели, дни под сеткой, переключатель виджета. Значок на ней зелёный (`accent`).',
    'danger': 'Опасные действия: «Удалить». Совпадает по значению с `overdue`, но роль другая.',
    'scrim': 'Затемнение под окнами и шторкой.',
    'priority-high-bg': 'Плашка приоритета P1: фон.', 'priority-high-text': 'Плашка P1: текст на `priority-high-bg`.',
    'priority-medium-bg': 'Плашка P2: фон.', 'priority-medium-text': 'Плашка P2: текст на `priority-medium-bg`.',
    'priority-low-bg': 'Плашка P3: фон.', 'priority-low-text': 'Плашка P3: текст на `priority-low-bg`.',
    'priority-none-bg': 'Плашка без приоритета: фон.', 'priority-none-text': 'Плашка без приоритета: текст на `priority-none-bg`.',
    'ring-high': 'Цвет приоритета P1: кружок «выполнено», кнопка приоритета, шкала. На `surface` 3,9:1 в светлой теме.', 'ring-high-fill': 'Лёгкая заливка кружка P1 в виджете.',
    'ring-medium': 'Цвет приоритета P2: кружок «выполнено», кнопка приоритета, шкала. На `surface` 2,5:1 в светлой теме (ниже 3:1): значение из приложения сохранено, приоритет не передавай одним цветом там, где важна точность.', 'ring-medium-fill': 'Лёгкая заливка кружка P2 в виджете.',
    'ring-low': 'Цвет приоритета P3: кружок «выполнено», кнопка приоритета, шкала. На `surface` 3,3:1 в светлой теме.', 'ring-low-fill': 'Лёгкая заливка кружка P3 в виджете.',
    'ring-none': 'Кружок без приоритета в виджете.', 'ring-none-fill': 'Без заливки (прозрачный).',
}
LIST_USAGE = 'Цвет списка из набора «Цвет» (хранится в БД как HEX; набор — часть системы).'

SPACING_USAGE = {
    'XXS': 'Поля у значков, между строками чек-листа, у подписей.', 'XS': 'Щель между кнопками панели и полями, внутри кнопок окон.',
    'SM': 'Внутренний отступ плашки, между полями даты и времени.', 'MD': 'Поля шторки и окон по бокам.', 'LG': 'Поля заголовка окна, между блоками.', 'XL': 'Крупные отступы экранов.',
}
RADIUS_USAGE = {
    'SM': 'Квадратики списков, пустая галочка, ручка шторки.', 'MD': 'Плашки задач, большие квадратики списков.',
    'LG': 'Кнопки панели и окон («Отмена», «Готово»).', 'XL': 'Поля и карточки крупных блоков.', 'NAV': 'Шторка задачи, окна по центру, нижняя панель.',
}
SIZE_PICK = {
    'HAIRLINE': ('hairline', 'Тонкие рамки и линии.'),
    'CARD_GAP': ('card-gap', 'Щель между плашками задач.'),
    'LIST_SQUARE': ('list-square', 'Квадратик списка перед названием.'), 'LIST_GLYPH': ('list-glyph', 'Значок в нём.'),
    'LIST_SQUARE_LARGE': ('list-square-large', 'Квадратик на экране «Списки» и в заголовке списка.'), 'LIST_GLYPH_LARGE': ('list-glyph-large', 'Значок в большом квадратике.'),
    'ICON_TILE': ('icon-tile', 'Плитка значка в окне выбора значка.'),
    'NAV_BAR': ('nav-bar', 'Высота плавающей нижней панели.'), 'NAV_TAB': ('nav-tab', 'Кнопка вкладки.'), 'NAV_ADD': ('nav-add', 'Кнопка «+».'),
    'CHECKBOX': ('checkbox', 'Квадратная галочка чек-листа.'), 'CHECKBOX_STROKE': ('stroke', 'Толщина рамки галочек и кружков.'), 'CHECKBOX_TOUCH': ('checkbox-touch', 'Зона нажатия галочки.'),
    'META_ICON': ('meta-icon', 'Значок повтора в плашке.'), 'REMINDER_ICON': ('reminder-icon', 'Колокольчик в плашке приложения.'),
    'COLOR_SWATCH': ('color-swatch', 'Образец цвета списка в выборе.'), 'ICON': ('icon', 'Обычный значок.'),
    'SHEET_HANDLE_WIDTH': ('sheet-handle-width', 'Ручка шторки: ширина.'), 'SHEET_HANDLE_HEIGHT': ('sheet-handle-height', 'Ручка шторки: высота.'),
    'TOOL_BUTTON': ('tool-button', 'Квадратная кнопка панели: не меньше 46, обычно 50.'), 'TOOL_ICON': ('tool-icon', 'Значок в кнопке панели.'),
    'WEEK_HOUR': ('week-hour', 'Высота часа в сетке при обычной прокрутке.'), 'WEEK_HOUR_MIN': ('week-hour-min', 'Наименьшая высота часа в окне даты.'),
    'WEEK_GUTTER': ('week-gutter', 'Колонка часов слева от сетки.'), 'WEEK_HEADER': ('week-header', 'Шапка дней сетки.'), 'WEEK_CLEAR': ('week-clear', 'Крестик «убрать время» (зона нажатия вдвое больше).'),
    'CLEAR_DOT': ('clear-dot', 'Видимый серый крестик.'), 'CLEAR_GLYPH': ('clear-glyph', '× в нём.'),
    'STRIP_HEIGHT': ('field-height', 'Высота строки полей даты и времени в шторке.'), 'STRIP_ICON': ('field-icon', 'Значок поля даты и времени.'),
    'DATE_CELL': ('date-cell', 'Высота ячейки дня в блоке «2 недели».'), 'DATE_DAY': ('date-day', 'Кружок с числом дня.'),
    'DATE_ARROW': ('date-arrow', 'Стрелка листания недель.'), 'DATE_RANGE_MIN': ('date-range-min', 'Место подписи «26 окт – 08 ноя» при обычном шрифте.'),
    'DATE_BUTTON_MIN': ('date-button-min', 'Наименьшая кнопка нижней строки окна даты.'),
    'DONE_CIRCLE': ('done-circle', 'Круг «выполнено» у названия.'), 'DONE_CHECK': ('done-check', 'Галочка в нём.'),
    'DIALOG_MAX_WIDTH': ('dialog-max-width', 'Наибольшая ширина окна по центру.'), 'DIALOG_ROW': ('dialog-row', 'Наименьшая высота строки варианта в окне.'),
    'DIALOG_BADGE': ('dialog-badge', 'Круглый значок над заголовком окна обновления.'),
    'WIDGET_ROW': ('widget-row', 'Высота строки задачи на шкале.'), 'WIDGET_PERIOD_ROW': ('widget-period-row', 'Высота строки периода.'),
    'WIDGET_TIME': ('widget-time', 'Колонка времени слева.'), 'WIDGET_RAIL': ('widget-rail', 'Колонка линии шкалы с кружком.'), 'WIDGET_NODE': ('widget-node', 'Кружок приоритета на шкале.'),
    'WIDGET_BAR': ('widget-bar', 'Нижняя строка виджета.'), 'WIDGET_ADD': ('widget-add', 'Кнопка «+» виджета.'), 'WIDGET_UPDATE': ('widget-update', 'Кнопка обновления виджета.'),
    'SNACKBAR_MIN_HEIGHT': ('snackbar-min-height', 'Подсказка «Отмена»: наименьшая высота.'), 'SNACKBAR_ICON': ('snackbar-icon', 'Значок слева в подсказке «Отмена».'),
    'RIBBON_BUTTON': ('ribbon-button', 'Режим выбора: наименьшая ширина кнопки ленты действий.'), 'RIBBON_BUTTON_HEIGHT': ('ribbon-button-height', 'Режим выбора: высота кнопки ленты действий.'),
    'SELECTION_BAR': ('selection-bar', 'Режим выбора: высота строки «Выбрано: N».'),
    'TIMELINE_DAY_COLUMN': ('timeline-day-column', 'Колонка дня и времени слева от плашки в плоских списках; растёт вместе с системным шрифтом.'),
    'SMART_CHIP': ('smart-chip', 'Чип срока из названия: высота.'), 'SMART_CHIP_ICON': ('smart-chip-icon', 'Значок календаря в чипе.'),
    'SMART_CHIP_CLOSE': ('smart-chip-close', 'Кружок с крестиком в чипе.'),
    'TRASH_ROW_EXTRA': ('trash-row-extra', 'Корзина: строка с подписью «удалена …» выше обычной на столько.'),
    'RECENT_ROW': ('recent-row', 'Строка недавнего запроса в поиске.'), 'RECENT_ICON': ('recent-icon', 'Значок «часы» в строке недавнего запроса.'),
    'WIDGET_FOLD_ICON': ('widget-fold-icon', 'Стрелка «свернуть» и значок «перенести на сегодня» у подписи «Просрочено» в виджете.'),
    'WIDGET_CARD_INSET': ('widget-card-inset', 'Щель плашки внутри строки шкалы (сверху и снизу).'),
    'POPUP_STRIP': ('popup-strip', 'Полоска напоминания поверх других приложений.'), 'POPUP_BUTTON': ('popup-button', 'Круглая кнопка в ней.'),
}


# ---------------------------------------------------------------- tokens.json


def contrast(a, b):
    def lum(h):
        h = h.lstrip('#')[:6]
        c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
        c = [x / 12.92 if x <= .03928 else ((x + .055) / 1.055) ** 2.4 for x in c]
        return .2126 * c[0] + .7152 * c[1] + .0722 * c[2]
    la, lb = sorted([lum(a), lum(b)], reverse=True)
    return (la + .05) / (lb + .05)


def build_tokens():
    color = []
    order = ['background', 'surface', 'text-primary', 'text-secondary', 'accent', 'on-accent', 'accent-soft', 'selected', 'inactive', 'divider',
             'overdue', 'danger', 'scrim', 'inbox-list', 'on-list-square']
    order += [n for n in COLORS if n.startswith('ring-')] + [n for n in COLORS if n.startswith('priority-')]
    for n in order:
        light, dark = COLORS[n]
        color.append({'name': n, 'value': {'light': light, 'dark': dark}, 'usage': COLOR_USAGE[n]})
    for hexv, nm in zip(LIST_PRESETS, LIST_NAMES):
        color.append({'name': f'list-{nm}', 'value': {'light': hexv.lower(), 'dark': hexv.lower()}, 'usage': LIST_USAGE})
    families = {'sans': '"Manrope", system-ui, sans-serif'}
    # Шкала Material 3, шрифт Manrope (AppTypography.kt); начертания — как в экранах приложения.
    scale = [
        ('Заголовки', [('display-large', 57, 64, 400), ('display-medium', 45, 52, 400), ('display-small', 36, 44, 400), ('headline-large', 32, 40, 400),
                       ('headline-medium', 28, 36, 400), ('headline-small', 24, 32, 800)]),
        ('Текст', [('title-large', 22, 28, 700), ('title-medium', 16, 24, 700), ('title-small', 14, 20, 500), ('body-large', 16, 24, 500),
                   ('body-medium', 14, 20, 600), ('body-small', 12, 16, 400)]),
        ('Подписи', [('label-large', 14, 20, 500), ('label-medium', 12, 16, 700), ('label-small', 11, 16, 700)]),
    ]
    use = {
        'headline-small': 'Название задачи в шторке (ExtraBold).', 'title-large': 'Заголовок окна по центру (Bold).', 'title-medium': 'Дата и время в шторке (Bold).',
        'body-large': 'Описание задачи, варианты в окнах, числа дней (Medium).', 'body-medium': 'Название задачи в плашке (Bold в ленте, SemiBold в окнах).',
        'body-small': 'Подписи под вариантом, дата создания.', 'label-medium': 'Время в колонке шкалы, подписи блоков окна (Bold).', 'label-small': 'Дни недели «пн… вс», подпись месяца (Bold).',
        'label-large': 'Подпись недель «26 окт – 08 ноя» (Bold в окне даты).',
    }
    groups = []
    for gname, styles in scale:
        groups.append({'name': gname, 'family': 'sans', 'styles': [
            {'name': n, 'fontSize': f'{s}px', 'lineHeight': f'{lh}px', 'fontWeight': w, **({'usage': use[n]} if n in use else {})} for n, s, lh, w in styles]})
    fonts = [{'family': 'Manrope', 'file': f'fonts/manrope_{w}.ttf', 'weight': str(w)} for w in (400, 500, 600, 700, 800)]
    spacing = [{'name': f'space-{k.lower()}', 'value': f'{v}px', 'usage': SPACING_USAGE[k]} for k, v in SPACING.items()]
    radius = [{'name': f'radius-{k.lower()}', 'value': f'{v}px', 'usage': RADIUS_USAGE[k]} for k, v in RADIUS.items()]
    size = []
    for k, (name, usage) in SIZE_PICK.items():
        if k not in SIZE:
            sys.exit(f'нет размера {k} в SizeTokens')
        size.append({'name': f'size-{name}', 'value': f'{SIZE[k]}px', 'usage': usage})
    return {
        'name': 'Simple Tasks', 'version': 1,
        'color': {'themes': [{'id': 'light', 'name': 'Светлая'}, {'id': 'dark', 'name': 'Тёмная'}], 'tokens': color},
        'type': {'fonts': fonts, 'families': families, 'groups': groups},
        'spacing': {'tokens': spacing}, 'radius': {'tokens': radius}, 'size': {'note': 'Размеры элементов в dp (1 dp = 1 px в превью).', 'tokens': size},
        'meta': {'source': 'code', 'repo': 'prostovasei/simpe-tasks-app', 'paths': {'tokens': ['app/src/main/java/com/prostovasei/simpletasks/designsystem/tokens']}},
    }


# ---------------------------------------------------------------- значки из drawable


def viewport(name):
    xml = (RES / 'drawable' / f'{name}.xml').read_text()
    return float(re.search(r'android:viewportWidth="([\d.]+)"', xml)[1]), float(re.search(r'android:viewportHeight="([\d.]+)"', xml)[1])


def icon_paths(name):
    xml = (RES / 'drawable' / f'{name}.xml').read_text()
    items = []
    for m in re.finditer(r'<path\b(.*?)/>', xml, re.S):
        a = m[1]
        d = re.search(r'android:pathData="([^"]*)"', a)[1]
        sw = re.search(r'android:strokeWidth="([\d.]+)"', a)
        fill = re.search(r'android:fillColor="([^"]*)"', a)
        items.append((d, sw[1] if sw else None, bool(fill and fill[1] == '@android:color/white')))
    return items


def svg(name, size=24, extra=''):
    parts = ''
    for d, sw, filled in icon_paths(name):
        if filled and not sw:
            parts += f'<path d="{d}" fill="currentColor"/>'
        elif filled:
            parts += f'<path d="{d}" fill="currentColor" stroke="currentColor" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round"/>'
        else:
            parts += f'<path d="{d}" fill="none" stroke="currentColor" stroke-width="{sw or 2}" stroke-linecap="round" stroke-linejoin="round"/>'
    vw, vh = viewport(name)
    return f'<svg width="{size}" height="{size}" viewBox="0 0 {vw:g} {vh:g}" aria-hidden="true" {extra}>{parts}</svg>'


_mask = [0]
BELL = 'M6,10a6,6 0 0 1 12,0c0,5 2,7 2,7H4c0,0 2,-2 2,-7 M10,20.5a2,2 0 0 0 4,0'
ARROW = 'M15.4,2.4L13.2,4.6L15.4,6.8 M13.6,4.6H18.8a2.9,2.9 0 0 1 0,5.8H17.6'


def bell(size, kind='at', tint='var(--text-secondary)', mark='var(--accent)'):
    """Колокольчик 1:1 с ReminderIcon: kind — at (в срок), early (заранее, стрелка), multi (стрелка и точка)."""
    _mask[0] += 1
    mid = f'bm{_mask[0]}'
    early, multi = kind in ('early', 'multi'), kind == 'multi'
    cut = ''
    if early:
        cut += f'<path d="{ARROW}" stroke="#000" stroke-width="4.6" stroke-linecap="round" stroke-linejoin="round" fill="none"/><rect x="13.6" y="4.6" width="8.1" height="5.8" fill="#000"/>'
    if multi:
        cut += '<circle cx="19.2" cy="19.2" r="5" fill="#000"/>'
    out = f'<defs><mask id="{mid}"><rect width="24" height="24" fill="#fff"/>{cut}</mask></defs>'
    out += f'<g mask="url(#{mid})"><path d="{BELL}" fill="none" stroke="{tint}" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/></g>'
    if early:
        out += f'<path d="{ARROW}" fill="none" stroke="{mark}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
    if multi:
        out += f'<circle cx="19.2" cy="19.2" r="3.1" fill="{mark}"/>'
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" aria-hidden="true">{out}</svg>'


# ---------------------------------------------------------------- превью


COMMON = '''*{box-sizing:border-box}html,body{margin:0}
body{padding:16px;background:var(--background);color:var(--text-primary);font-family:var(--font-sans);font-size:14px;line-height:20px}
.row{display:flex;gap:20px;flex-wrap:wrap;align-items:flex-end}
.col{display:flex;flex-direction:column;align-items:center;gap:8px}
.cap{font-size:11px;line-height:14px;font-weight:700;color:var(--text-secondary);text-align:center;max-width:120px}
.tool{position:relative;width:50px;height:50px;border-radius:var(--radius-lg);background:var(--surface);border:1px solid var(--divider);display:flex;align-items:center;justify-content:center;color:var(--text-secondary);flex-shrink:0}
.tool svg{width:25px;height:25px}
.tool.sel{background:var(--selected);border-color:var(--selected);color:var(--accent)}
.tool.dim{color:color-mix(in srgb,var(--inactive) 60%,transparent)}
.tool.pri{border-color:transparent}
.confirm{width:50px;height:50px;border-radius:var(--radius-lg);background:var(--accent);color:var(--on-accent);display:flex;align-items:center;justify-content:center;flex-shrink:0}
.confirm svg{width:25px;height:25px}
.clr{position:absolute;top:-7px;right:-7px;width:14px;height:14px;border-radius:50%;background:var(--inactive);color:var(--surface);display:flex;align-items:center;justify-content:center}
.clr svg{width:10px;height:10px}
.field{display:flex;align-items:center;gap:8px;font-size:16px;line-height:24px;font-weight:700;color:var(--accent)}
.field svg{width:22px;height:22px}
.field.empty{color:var(--text-secondary)}
.list-sq{width:16px;height:16px;border-radius:var(--radius-sm);display:flex;align-items:center;justify-content:center;color:var(--on-list-square);flex-shrink:0}
.list-sq svg{width:12px;height:12px}
'''
X = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>'
CHECK = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12l5 5 9-10"/></svg>'


def doc(group, height, subtitle, body, css=''):
    return (f'<!-- @dsCard group="{group}" height={height} subtitle="{subtitle}" -->\n<!doctype html><html lang="ru"><head><meta charset="utf-8">'
            f'<style>{COMMON}{css}</style></head><body>{body}</body></html>\n')


def tool(icon_html, cls='', clear=False):
    return f'<span class="tool {cls}">{icon_html}{f"<span class=clr>{X}</span>" if clear else ""}</span>'


def preview_tool_button():
    flag = svg('ic_tool_priority', 25)
    rep, lst = svg('ic_tool_repeat', 25), svg('ic_tool_checklist', 25)
    pri = lambda c: f'<span class="tool pri" style="background:color-mix(in srgb,var({c}) 16%,var(--surface));color:var({c})">{flag}<span class="clr">{X}</span></span>'
    lsq = '<span class="list-sq" style="background:var(--inbox-list);width:25px;height:25px;border-radius:var(--radius-md)">' + svg('ic_list_inbox', 16) + '</span>'
    body = (f'<div class="row">'
            f'<div class="col">{tool(rep)}<div class="cap">обычная</div></div>'
            f'<div class="col">{tool(rep, "sel")}<div class="cap">выбрана</div></div>'
            f'<div class="col">{tool(rep, "dim")}<div class="cap">нет даты: бледный значок</div></div>'
            f'<div class="col">{tool(lst, "sel", clear=True)}<div class="cap">с крестиком «сбросить»</div></div>'
            f'<div class="col">{tool(lsq, "sel", clear=True)}<div class="cap">список (квадратик цвета)</div></div>'
            f'<div class="col">{pri("--ring-high")}<div class="cap">приоритет P1</div></div>'
            f'<div class="col">{pri("--ring-medium")}<div class="cap">P2</div></div>'
            f'<div class="col">{pri("--ring-low")}<div class="cap">P3</div></div>'
            f'<div class="col"><span class="confirm">{CHECK}</span><div class="cap">«Готово»: зелёная галочка</div></div></div>')
    return doc('Панель и поля', 150, 'Квадратная кнопка панели шторки и галочка «Готово»', body)


def preview_date_fields():
    cal, clock = svg('ic_widget_all', 22), svg('ic_time', 22)
    clr = f'<span class="clr" style="position:static">{X}</span>'

    def f(*parts):
        return '<div style="display:flex;align-items:center;gap:24px;height:54px">' + ''.join(parts) + '</div>'
    date = lambda t: f'<span class="field">{cal}<span>{t}</span>{clr}</span>'
    time = lambda t: f'<span class="field">{clock}<span>{t}</span>{clr}</span>'
    body = ('<div style="display:flex;flex-direction:column;gap:6px">'
            f'{f(f"<span class=\"field empty\">{cal}<span>Без даты и времени</span></span>")}<div class="cap" style="text-align:left;max-width:none">без даты: серая подпись, крестика нет</div>'
            f'{f(date("чт, 29 окт"))}<div class="cap" style="text-align:left;max-width:none">только дата: поля времени нет</div>'
            f'{f(date("чт, 29 окт"), time("17:00"))}<div class="cap" style="text-align:left;max-width:none">дата и время</div>'
            f'{f(date("чт, 29 окт"), time("17:00–18:00"))}<div class="cap" style="text-align:left;max-width:none">период: начало–конец</div></div>')
    return doc('Панель и поля', 360, 'Два поля шторки: дата и время, у каждого свой крестик', body)


def preview_done_circle():
    def circ(color, done=False):
        if done:
            return f'<span style="width:22px;height:22px;border-radius:50%;background:{color};color:var(--on-accent);display:flex;align-items:center;justify-content:center">{CHECK.replace("<svg", "<svg width=14 height=14")}</span>'
        return f'<span style="width:22px;height:22px;border-radius:50%;border:2px solid {color};display:block"></span>'
    cells = ''
    for label, c in [('без приоритета', 'var(--text-secondary)'), ('P3', 'var(--ring-low)'), ('P2', 'var(--ring-medium)'), ('P1', 'var(--ring-high)')]:
        done_c = 'var(--accent)' if label.startswith('без') else c
        cells += f'<div class="col">{circ(c)}<div class="cap">{label}</div></div><div class="col">{circ(done_c, True)}<div class="cap">выполнена</div></div>'
    return doc('Панель и поля', 110, 'Круг «выполнено» слева от названия: цвет приоритета', f'<div class="row">{cells}</div>')


def preview_bell():
    cells = ''
    for kind, label in [('at', 'в момент срока'), ('early', 'заранее: стрелка'), ('multi', 'несколько: стрелка и точка')]:
        cells += (f'<div class="col"><div style="display:flex;gap:20px;align-items:center">{bell(18, kind)}{bell(22, kind)}'
                  f'<span class="tool sel" style="width:50px;height:50px">{bell(25, kind)}</span></div><div class="cap" style="max-width:none">{label}</div></div>')
    return doc('Панель и поля', 120, 'Колокольчик уведомлений: плашка 18, виджет 22, панель 25', f'<div class="row" style="gap:44px">{cells}</div>')


def preview_list_square():
    sw = ''
    for nm in LIST_NAMES:
        sw += f'<div class="col"><span class="list-sq" style="background:var(--list-{nm});width:40px;height:40px;border-radius:var(--radius-md)"></span><div class="cap">{nm}</div></div>'
    icons = ['home', 'work', 'shopping', 'sport', 'study', 'health', 'money', 'travel', 'idea', 'star', 'inbox']
    ic = ''.join(f'<div class="col"><span class="list-sq" style="background:var(--list-blue);width:24px;height:24px;border-radius:var(--radius-md)">{svg(f"ic_list_{i}", 16)}</span><div class="cap">{i}</div></div>' for i in icons)
    body = (f'<div class="row" style="align-items:flex-start;gap:12px">{sw}</div>'
            f'<div style="height:16px"></div><div class="row" style="align-items:flex-start;gap:12px">{ic}</div>')
    return doc('Основы', 220, 'Цвета списков и значки внутри квадратика', body)


def preview_icons():
    names = ['ic_tool_checklist', 'ic_tool_priority', 'ic_tool_reminder', 'ic_tool_repeat', 'ic_tool_delete', 'ic_no_date', 'ic_time', 'ic_widget_all',
             'ic_widget_inbox', 'ic_widget_list', 'ic_widget_add', 'ic_widget_update', 'ic_fold_open', 'ic_fold_closed', 'ic_move_today', 'ic_history', 'ic_snack_restore']
    cells = ''.join(f'<div class="col" style="width:92px"><span style="color:var(--text-primary)">{svg(n, 32)}</span><div class="cap">{n.removeprefix("ic_")}</div></div>' for n in names)
    return doc('Основы', 220, 'Контурные значки 24 dp, линия 2, скругления: цвет задаёт оттенок', f'<div class="row" style="align-items:flex-start;gap:6px">{cells}</div>')


def node(color, done=False, h=20):
    if done:
        return f'<span style="width:20px;height:{h}px;border-radius:10px;background:{color};color:var(--on-accent);display:flex;align-items:center;justify-content:center">{CHECK.replace("<svg", "<svg width=12 height=12")}</span>'
    return f'<span style="width:20px;height:{h}px;border-radius:10px;border:2px solid {color};background:var(--background);display:block"></span>'


def trow(time, color, title, done=False, over=False, period=None, rep=False, rem=None, lsq=True, end=None, top=True, bottom=True):
    h = 64 if period else 46
    tcol = 'var(--overdue)' if over else ('var(--text-secondary)' if done else 'var(--text-primary)')
    t = f'<div style="width:38px;text-align:right;font-size:12px;line-height:16px;font-weight:700;color:{tcol};display:flex;flex-direction:column;justify-content:center">{time}' + (f'<span style="color:{"var(--overdue)" if over else "var(--text-secondary)"}">{end}</span>' if end else '') + '</div>'
    rail = (f'<div style="width:26px;display:flex;flex-direction:column;align-items:center"><span style="width:1px;flex:1;background:{"var(--divider)" if top else "transparent"}"></span>'
            f'{node(color, done, 58 if period else 20)}<span style="width:1px;flex:1;background:{"var(--divider)" if bottom else "transparent"}"></span></div>')
    icons = (f'<span style="color:var(--text-secondary);margin-left:4px">{svg("ic_tool_repeat", 18)}</span>' if rep else '') + (f'<span style="margin-left:4px">{bell(22, rem)}</span>' if rem else '')
    lsqh = f'<span class="list-sq" style="background:var(--inbox-list);margin-left:8px">{svg("ic_list_inbox", 12)}</span>' if lsq else ''
    plaque = (f'<div style="flex:1;margin:3px 0;border-radius:var(--radius-md);background:var(--surface);display:flex;align-items:center;padding:0 12px;opacity:{.7 if done else 1}">'
              f'<span style="flex:1;font-size:14px;font-weight:700;{"text-decoration:line-through;color:var(--text-secondary)" if done else ""}">{title}</span>{icons}{lsqh}</div>')
    return f'<div style="display:flex;height:{h}px;padding-right:8px">{t}{rail}{plaque}</div>'


def preview_timeline_row():
    body = ('<div style="max-width:420px">'
            + trow('08:00', 'var(--ring-high)', 'Позвонить врачу', rem='early', top=False)
            + trow('10:00', 'var(--ring-medium)', 'Созвон с командой', period=True, end='11:30', rep=True, rem='at')
            + trow('12:30', 'var(--ring-low)', 'Оплатить интернет', rem='multi')
            + trow('14:00', 'var(--inactive)', 'Купить продукты')
            + trow('Вчера', 'var(--inactive)', 'Отчёт за неделю', over=True)
            + trow('09:00', 'var(--ring-high)', 'Зарядка', done=True, bottom=False) + '</div>')
    return doc('Список и виджет', 460, 'Строка шкалы: время, кружок приоритета, плашка. Просрочка, выполнено, период', body)


def sheet_card(created, width=388):
    handle = '<div style="display:flex;justify-content:center;padding:8px 0 4px"><span style="width:36px;height:4px;border-radius:var(--radius-sm);background:var(--divider)"></span></div>'
    dots = '<span style="color:var(--text-secondary);display:flex"><svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.4" stroke-linecap="round"><path d="M12 5h.01M12 12h.01M12 19h.01"/></svg></span>'
    circle = f'<span style="width:22px;height:22px;border-radius:50%;border:2px solid var(--ring-high);flex-shrink:0"></span>'
    title = (f'<div style="display:flex;align-items:center;gap:10px;margin-top:10px">{circle}<span style="flex:1;font-size:24px;line-height:32px;font-weight:800">Подготовить отчёт</span>{dots}</div>'
             if created else '<div style="font-size:24px;line-height:32px;font-weight:800;margin-top:10px;color:var(--inactive)">Купить билеты на концерт</div>')
    desc = '<div style="font-size:16px;line-height:24px;font-weight:500;color:var(--text-secondary);margin-top:4px">Квартальный отчёт для руководства</div>' if created else '<div style="font-size:16px;line-height:24px;font-weight:500;color:var(--inactive);margin-top:4px">Описание</div>'
    cal, clock = svg('ic_widget_all', 22), svg('ic_time', 22)
    clr = f'<span class="clr" style="position:static">{X}</span>'
    if created:
        fields = f'<span class="field">{cal}<span>пт, 30 окт</span>{clr}</span><span class="field">{clock}<span>10:00–11:30</span>{clr}</span>'
        tools = (tool('<span class="list-sq" style="background:var(--inbox-list);width:25px;height:25px;border-radius:var(--radius-md)">' + svg('ic_list_inbox', 16) + '</span>', 'sel', True)
                 + f'<span class="tool pri" style="background:color-mix(in srgb,var(--ring-high) 16%,var(--surface));color:var(--ring-high)">{svg("ic_tool_priority", 25)}<span class="clr">{X}</span></span>'
                 + tool(svg('ic_tool_checklist', 25), 'sel') + tool(svg('ic_tool_repeat', 25), 'sel') + tool(bell(25, 'early', 'var(--accent)'), 'sel'))
    else:
        fields = f'<span class="field empty">{cal}<span>Без даты и времени</span></span>'
        tools = (tool('<span class="list-sq" style="background:var(--inbox-list);width:25px;height:25px;border-radius:var(--radius-md)">' + svg('ic_list_inbox', 16) + '</span>')
                 + tool(svg('ic_tool_priority', 25)) + tool(svg('ic_tool_checklist', 25)) + tool(svg('ic_tool_repeat', 25), 'dim') + tool(bell(25, 'at', 'color-mix(in srgb,var(--inactive) 60%,transparent)'), 'dim'))
    return (f'<div style="width:{width}px;border-radius:var(--radius-nav);background:var(--surface);padding:0 16px 14px;box-shadow:0 2px 10px color-mix(in srgb,var(--text-primary) 12%,transparent)">'
            f'{handle}{title}{desc}<div style="display:flex;align-items:center;gap:24px;height:54px;margin-top:8px">{fields}</div>'
            f'<div style="display:flex;gap:8px;align-items:center;margin-top:6px">{tools}<span style="flex:1"></span><span class="confirm">{CHECK}</span></div></div>')


def preview_sheet_card():
    body = f'<div class="row" style="align-items:flex-start;gap:28px">{sheet_card(False)}{sheet_card(True)}</div>'
    return doc('Окна', 360, 'Шторка задачи: новая (повтор и уведомления бледные без даты) и созданная', body)


def preview_dialog():
    def opt(title, sel, sub=None, hint=None, multi=False):
        box = (f'<span style="width:18px;height:18px;border-radius:var(--radius-sm);border:2px solid {"var(--accent)" if sel else "var(--inactive)"};background:{"var(--accent)" if sel else "transparent"};color:var(--on-accent);display:flex;align-items:center;justify-content:center">{CHECK.replace("<svg", "<svg width=12 height=12") if sel else ""}</span>' if multi else '')
        right = f'<span style="color:var(--accent);display:flex">{CHECK.replace("<svg", "<svg width=22 height=22")}</span>' if sel and not multi else ''
        return (f'<div style="min-height:52px;display:flex;align-items:center;gap:12px;padding:4px 24px;background:{"var(--selected)" if sel and not multi else "transparent"}">{box}'
                f'<div style="flex:1"><div style="font-size:16px;line-height:24px;font-weight:{700 if sel else 600}">{title}</div>{f"<div style=font-size:12px;color:var(--text-secondary)>{sub}</div>" if sub else ""}</div>'
                f'{f"<span style=font-size:12px;font-weight:600;color:var(--text-secondary)>{hint}</span>" if hint else ""}{right}</div>')
    sect = lambda t: f'<div style="padding:12px 24px 4px;font-size:12px;line-height:16px;font-weight:700;color:var(--text-secondary);letter-spacing:.04em">{t}</div>'
    btn = lambda t, primary: f'<span style="flex:1;min-height:50px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;font-weight:700;{"background:var(--accent);color:var(--on-accent)" if primary else "border:1px solid var(--divider);color:var(--text-primary)"}">{t}</span>'
    dlg = (f'<div style="width:372px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden">'
           '<div style="padding:24px 24px 8px"><div style="font-size:22px;line-height:28px;font-weight:700">Уведомления</div><div style="font-size:14px;line-height:20px;color:var(--text-secondary)">Можно отметить несколько</div></div>'
           f'{sect("ОСНОВНЫЕ")}{opt("В момент срока", True, multi=True, hint="17:00")}{opt("За 30 минут", True, multi=True, hint="16:30")}{opt("За 1 день", False, multi=True, hint="вчера 17:00")}'
           f'<div style="height:1px;background:var(--divider);margin:4px 24px"></div>{sect("СОЗДАННЫЕ ВРУЧНУЮ")}{opt("За 2 часа", False, multi=True, hint="15:00")}'
           f'<div style="height:1px;background:var(--divider);margin-top:8px"></div><div style="display:flex;gap:12px;padding:16px">{btn("Отмена", False)}{btn("Готово", True)}</div></div>')
    one = (f'<div style="width:372px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden">'
           '<div style="padding:24px 24px 8px"><div style="font-size:22px;line-height:28px;font-weight:700">Повтор</div></div>'
           f'{opt("Не повторять", False)}{opt("Каждый день", False)}{opt("По средам", True)}{opt("Произвольно…", False)}'
           f'<div style="height:1px;background:var(--divider);margin-top:8px"></div><div style="display:flex;gap:12px;padding:16px">{btn("Отмена", False)}{btn("Готово", True)}</div></div>')
    return doc('Окна', 560, 'Окно по центру: несколько вариантов (квадрат-галочка) и один (подсветка и галочка справа)', f'<div class="row" style="align-items:flex-start;gap:28px">{dlg}{one}</div>')


MONTH_RU = ['янв', 'фев', 'мар', 'апр', 'мая', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек']


def day_cell(n, past=False, sel=False, today=False, band=None, month=None):
    r = {'first': '20px 0 0 20px', 'mid': '0', 'last': '0 20px 20px 0'}.get(band, '0')
    bg = 'var(--selected)' if band else 'transparent'
    color = 'var(--inactive)' if past else ('var(--on-accent)' if sel else 'var(--text-primary)')
    inner = ('background:var(--accent);font-weight:800;' if sel else ('box-shadow:inset 0 0 0 2px var(--accent);font-weight:600;' if today else 'font-weight:500;'))
    lab = f'<span style="position:absolute;bottom:0;left:0;right:0;text-align:center;font-size:9px;font-weight:700;color:var(--accent)">{month}</span>' if month else ''
    return (f'<span style="position:relative;flex:1;height:40px;display:flex;align-items:center;justify-content:center;background:{bg};border-radius:{r}">'
            f'<span style="width:34px;height:34px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:16px;line-height:24px;color:{color};{inner}">{n}</span>{lab}</span>')


def week_block():
    head = ''.join(f'<span style="flex:1;text-align:center;font-size:11px;line-height:16px;font-weight:700;color:var(--text-secondary)">{d}</span>' for d in ['пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'])
    r1 = ''.join([day_cell(26, past=True), day_cell(27, past=True), day_cell(28, past=True), day_cell(29, sel=True, band='first'), day_cell(30, band='mid'), day_cell(31, band='last'), day_cell(1, month='ноя')])
    r2 = ''.join(day_cell(n) for n in range(2, 9))
    return f'<div style="padding:4px 12px"><div style="display:flex">{head}</div><div style="display:flex">{r1}</div><div style="display:flex">{r2}</div></div>'


def window_bar():
    def sqb(icon_html, color='var(--text-secondary)'):
        return f'<span class="tool" style="color:{color}">{icon_html}</span>'
    back = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M11 6l-6 6 6 6"/></svg>'
    chev = lambda p, dim=False: f'<span style="color:{"color-mix(in srgb,var(--inactive) 60%,transparent)" if dim else "var(--text-primary)"};display:flex"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="{p}"/></svg></span>'
    mid = f'<span style="flex:1;display:flex;align-items:center;justify-content:center;gap:2px">{chev("M15 6l-6 6 6 6", True)}<span style="font-size:12px;line-height:16px;font-weight:700;white-space:nowrap">26 окт – 08 ноя</span>{chev("M9 6l6 6-6 6")}</span>'
    return (f'<div style="display:flex;gap:8px;align-items:center;padding:8px 12px">{sqb(back)}{sqb(svg("ic_no_date", 25))}{mid}'
            f'{sqb(svg("ic_widget_all", 25), "var(--accent)")}<span class="confirm">{CHECK}</span></div>')


def preview_date_window():
    hours = ''.join(f'<div style="position:absolute;left:0;right:0;top:{(h - 10) * 22}px;height:22px;border-top:1px solid var(--divider)"><span style="position:absolute;left:4px;top:2px;font-size:11px;color:var(--text-secondary)">{h}</span></div>' for h in range(10, 17))
    cols = ''.join(f'<span style="width:{(388 - 26) / 3}px;height:36px;display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:{800 if i == 0 else 600};color:{"var(--accent)" if i == 0 else "var(--text-primary)"};background:{"var(--selected)" if i == 0 else "transparent"}">{t}</span>' for i, t in enumerate(['29 чт', '30 пт', '31 сб']))
    grid = (f'<div style="display:flex;padding-left:26px">{cols}</div><div style="position:relative;height:154px;overflow:hidden"><span style="position:absolute;left:26px;width:{(388 - 26) / 3}px;top:0;bottom:0;background:var(--selected);opacity:.7"></span>{hours}'
            f'<div style="position:absolute;left:28px;width:{(388 - 26) / 3 - 4}px;top:{3 * 22}px;height:42px;border-radius:var(--radius-sm);background:var(--accent);color:var(--on-accent);font-weight:800;font-size:13px;display:flex;align-items:center;justify-content:center">13:00–15:00</div></div>')
    win = (f'<div style="width:388px;border-radius:var(--radius-nav) var(--radius-nav) 0 0;background:var(--surface);overflow:hidden">{grid}'
           f'<div style="border-top:1px solid var(--divider)">{week_block()}</div>{window_bar()}</div>')
    cap = ('<div style="max-width:420px;font-size:12px;line-height:18px;color:var(--text-secondary)">Сетка на 3 дня с часом, подогнанным под высоту (10:00–24:00 видно сразу, час не меньше 32). '
           'Под ней блок «2 недели»: прошедшие дни бледные и недоступны, сегодня в кольце, выбранный зелёный, три дня сетки под светлой подложкой, у 1-го числа подпись месяца. '
           'Внизу: «←» отменяет изменения, «без даты», листание недель (на первой странице «‹» бледная), «Календарь», ✓.</div>')
    return doc('Окна', 420, 'Окно даты и времени на всю высоту (здесь укорочено)', f'<div class="row" style="align-items:flex-start;gap:28px">{win}{cap}</div>')


def preview_calendar():
    def month(title, first_dow, days, past_to=0, sel=None, today=None):
        cells = [''] * first_dow + list(range(1, days + 1))
        rows = ''
        for i in range(0, len(cells), 7):
            chunk = cells[i:i + 7] + [''] * (7 - len(cells[i:i + 7]))
            rows += '<div style="display:flex">' + ''.join(day_cell(c, past=c <= past_to, sel=c == sel, today=c == today) if c != '' else '<span style="flex:1;height:40px"></span>' for c in chunk) + '</div>'
        return f'<div style="font-size:16px;line-height:24px;font-weight:800;margin:10px 4px 2px">{title}</div>{rows}'
    head = ''.join(f'<span style="flex:1;text-align:center;font-size:11px;font-weight:700;color:var(--text-secondary)">{d}</span>' for d in ['пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'])
    body = ('<div style="width:372px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden"><div style="padding:24px 16px 8px">'
            '<div style="font-size:22px;line-height:28px;font-weight:700;padding:0 8px">Календарь</div><div style="font-size:14px;line-height:20px;color:var(--text-secondary);padding:0 8px 8px">Нажмите на день — окно закроется</div>'
            f'<div style="display:flex">{head}</div>{month("Октябрь 2026", 3, 31, past_to=28, sel=29, today=29)}{month("Ноябрь 2026", 6, 30)}</div>'
            '<div style="height:1px;background:var(--divider)"></div><div style="padding:16px"><span style="min-height:50px;border-radius:var(--radius-lg);border:1px solid var(--divider);display:flex;align-items:center;justify-content:center;font-weight:700">Закрыть</span></div></div>')
    return doc('Окна', 720, 'Календарь на 2 месяца: день выбирается сразу, кнопка одна — «Закрыть»', body)


# ---------------------------------------------------------------- обложка

def cover():
    # Блоки и узор — из шкал: радиусы radius-*, шаг 24 (space-lg), кружки — как круги «выполнено».
    discs = ''.join(f'<circle class="ring{i % 3}" cx="{672 + (i % 5) * 48}" cy="{252 + (i // 5) * 48}" r="14"/>' for i in range(10))
    return f'''<!-- @dsCard height=360 -->
<!doctype html><html lang="ru"><head><meta charset="utf-8"><style>
*{{box-sizing:border-box}}html,body{{margin:0}}body{{background:var(--background)}}
svg{{display:block;width:960px;height:360px}}
.ground{{fill:var(--background)}}.brand{{fill:var(--accent)}}.soft{{fill:var(--selected)}}.ink{{fill:var(--text-primary)}}.surf{{fill:var(--surface)}}
.hi{{fill:var(--ring-high)}}.mid{{fill:var(--ring-medium)}}.lo{{fill:var(--ring-low)}}
.big{{rx:var(--radius-nav)}}.tile{{rx:var(--radius-lg)}}
.ring0{{fill:none;stroke:var(--ring-high);stroke-width:4}}.ring1{{fill:none;stroke:var(--ring-medium);stroke-width:4}}.ring2{{fill:none;stroke:var(--ring-low);stroke-width:4}}
.name{{font-family:var(--font-sans);font-weight:800;font-size:112px;fill:var(--text-primary)}}
.tag{{font-family:var(--font-sans);font-weight:500;font-size:14px;fill:var(--text-secondary)}}
</style></head><body>
<svg viewBox="0 0 960 360" role="img" aria-label="Simple Tasks">
<!-- Блоки: accent 288×216 (radius-nav) — главный зелёный; selected 144×216 — подсветка; text-primary 96×96 — ink; surface 144×96 (radius-lg) для кружков.
     Расклад: одна высокая плита зелёного, рядом светлая полоса и тёмный квадрат; узор — кружки цветов приоритетов по шагу 48.
     Узор: «мягко, дружелюбно, круги»: кружок «выполнено» приложения (зелёный/янтарный/голубой/красный контур), шаг 48 = 2 × space-lg.
     Шаги и радиусы: space-lg 24, radius-nav 22, radius-lg 12. -->
<rect class="ground" width="960" height="360"/>
<rect class="brand big" x="504" y="48" width="288" height="216"/>
<rect class="soft big" x="808" y="48" width="104" height="216"/>
<rect class="ink tile" x="504" y="280" width="96" height="48"/>
<rect class="surf tile" x="616" y="280" width="296" height="48"/>
<circle class="hi" cx="744" cy="304" r="12"/><circle class="mid" cx="776" cy="304" r="12"/><circle class="lo" cx="808" cy="304" r="12"/><circle class="brand" cx="840" cy="304" r="12"/>
{''.join(f'<circle cx="{544 + (i % 4) * 48}" cy="{96 + (i // 4) * 48}" r="14" fill="none" stroke="var(--on-accent)" stroke-width="4" opacity="{1 if i in (0, 5, 10) else .55}"/>' for i in range(12))}
<text class="name" x="48" y="204">Simple</text>
<text class="name" x="48" y="304">Tasks</text>
<text class="tag" x="48" y="336">Задачи, дата и время — просто и крупно.</text>
</svg></body></html>
'''


# ---------------------------------------------------------------- README и компоненты

README = '''## Содержание и язык

- Пиши по-русски, коротко и прямо. Подписи кнопок — глаголы или одно слово: «Готово», «Отмена», «Закрыть», «Удалить», «Загрузить».
- Дата в полях и подписях — «чт, 29 окт» (день недели строчными, месяц сокращённо: янв, фев, мар, апр, мая, июн, июл, авг, сен, окт, ноя, дек);
  другой год добавляется числом: «чт, 29 окт 2027». Время — «17:00», период — «17:00–18:00» (длинное тире без пробелов).
- Подпись недель в окне даты — «26 окт – 08 ноя»: число с нулём. Дата создания в меню «⋮» — «создано чт, 01 окт 2026».
- Нет даты — «Без даты и времени», серым и без крестика. Нет времени — поле времени не показывается совсем.
- Подсказка там, где действие недоступно, говорит, что сделать: «Сначала выберите день». Ошибки объясняют причину и что делать дальше.
- Без эмодзи, без вибрации, без восклицательных знаков.

## Основы визуала

- **Цвет.** Фон экрана — `background`, поверх него белые плашки `surface`. Текст — `text-primary`, описания и значки — `text-secondary`.
  Главный цвет — `accent`: галочка «Готово», выбранные значки, даты. Выбранное — не сплошной зелёный, а мягкая заливка `selected` с зелёным значком.
  Просрочка — `overdue` (красные день и время), удаление — `danger`. Приоритет — только цветом кружка и кнопки: `ring-high`, `ring-medium`, `ring-low`.
  Бледные недоступные значки — `inactive` с прозрачностью 60%. Рамки и линии — `divider`, толщина `size-hairline`.
- **Тёмная тема** — те же роли, другие значения; на заливке `accent` текст всегда `on-accent`.
- **Шрифт.** Только Manrope. Название задачи в шторке — `headline-small` (ExtraBold), заголовок окна — `title-large` (Bold), дата и время — `title-medium` (Bold),
  название в плашке — `body-medium` (Bold), время на шкале — `label-medium` (Bold). Не подставляй другие шрифты и не меняй начертания.
- **Отступы и радиусы.** Отступы — `space-xxs`…`space-xl` (4, 8, 12, 16, 24, 32); между кнопками панели — `space-xs`. Радиусы: плашка — `radius-md`,
  кнопка — `radius-lg`, шторка и окна по центру — `radius-nav`.
- **Размеры нажатия.** Квадратная кнопка панели — `size-tool-button` (50), не меньше 46. Зона нажатия галочки — `size-checkbox-touch`. Крестик убирает свойство, но не открывает окно.
- **Тени и анимации.** Тени не используются, кроме подъёма шторки и нижней панели. Окно растёт из плашки виджета, кнопки «желе» при нажатии — это поведение, а не стиль.
- **Правило проекта.** Цвета и dp вне `designsystem/tokens` писать нельзя: `checkDesignTokens` валит сборку. Новое значение сначала добавляется токеном.

## Состояния

- Выбрано: `selected` + зелёный значок. Недоступно (нет даты): значок `inactive` с прозрачностью 60%, фон и рамка прежние; нажатие показывает подсказку.
- Приоритет: кнопка и круг «выполнено» цвета приоритета; без приоритета — серый контур, у выполненной зелёная заливка.
- Просрочено: время и день красные, кружок бледный. Выполнено: плашка 70% непрозрачности, название зачёркнуто, кружок залит цветом приоритета с белой галочкой.
- Уведомления: колокольчик без знаков — в момент срока, со стрелкой — заранее, со стрелкой и зелёной точкой — несколько разных.

## Значки

- Контурные, сетка 24 dp, линия 2, круглые концы и углы. Цвет задаёт оттенок: `text-secondary` по умолчанию, `accent` у выбранного, `inactive` (60%) у недоступного.
- Значки списков — белые на квадратике цвета списка (`list-*`), у «Входящие» — `inbox-list`. Колокольчик со знаками собирается маской: вырез вокруг стрелки и точки.

@@COMPONENTS@@
'''

COMP_README = {
    'ToolButton': ('Квадратная кнопка панели шторки: список, приоритет, чек-лист, повтор, уведомления; рядом зелёная галочка «Готово».',
                   '''Размер 50 (`size-tool-button`), радиус `radius-lg`, рамка `divider` толщиной `size-hairline`, значок 25 (`size-tool-icon`).
Выбранная кнопка залита `selected`, значок `accent`. Кнопка приоритета окрашена в цвет приоритета (`ring-*`), заливка — тот же цвет с прозрачностью 16%.
Без даты повтор и уведомления остаются нажимаемыми: значок `inactive` с прозрачностью 60%, а окно подсказывает «Сначала выберите день».
Крестик справа сверху сбрасывает только это свойство (список, приоритет): серый круг `size-clear-dot` с белым ×, зона нажатия вдвое больше.
Передавай: подпись для доступности, значок, состояние (выбрана / недоступна), действие. Не делай кнопки у́же 46.'''),
    'DateFields': ('Два поля шторки: дата и время; каждое открывает окно даты, у каждого свой крестик.',
                   '''Строка высотой `size-field-height` (54), значок `size-field-icon` (22), текст `title-medium` Bold, цвет `accent`. Промежуток между полями — `space-lg`.
Крестик у даты убирает дату вместе со временем, крестик у времени — только время (день остаётся). Нажатие на текст поля открывает окно даты; крестик стоит рядом и в нажатие не входит.
Без даты — одна серая подпись «Без даты и времени» (`text-secondary`), крестика и поля времени нет. Нет времени — поля времени нет.
Передавай: выбранные день и время (период — «17:00–18:00»). Не показывай время без даты.'''),
    'DoneCircle': ('Круг «выполнено» слева от названия созданной задачи; цвет — приоритет.',
                   '''Диаметр `size-done-circle` (22), контур `size-stroke` (2). Цвет контура — приоритет задачи (`ring-high`, `ring-medium`, `ring-low`); без приоритета — `text-secondary`.
У выполненной — заливка тем же цветом и белая галочка `size-done-check` (14); без приоритета заливка `accent`. Круг вплотную к левому краю текста, зона нажатия шире круга.
У новой задачи круга нет. Не используй для приоритета значок P1 рядом с названием: цвет круга и кнопки — единственный знак приоритета.'''),
    'ReminderBell': ('Колокольчик уведомлений в трёх видах: в момент срока, заранее, несколько.',
                     '''Один рисунок во всех местах (плашка 18, виджет 22, кнопка панели 25). Базовый колокольчик — контур, цвет `text-secondary` (на выбранной кнопке `accent`).
«Заранее» — стрелка назад справа вверху в цвете `accent`, вокруг неё вырез в контуре. «Несколько» — стрелка и зелёная точка `accent` справа внизу (тоже с вырезом).
Вид выбирается так: несколько разных напоминаний — «несколько»; хоть одно раньше срока — «заранее»; иначе — «в момент срока». Одинаковые по моменту считаются одним.'''),
    'ListSquare': ('Квадратики списков: цвет и значок; девять цветов набора «Цвет» и значки внутри.',
                   '''Маленький `size-list-square` (16) с радиусом `radius-sm` и значком `size-list-glyph` (12); большой `size-list-square-large` (24) с радиусом `radius-md` и значком 16.
Значок белый (`on-list-square`). Цвет — один из `list-*`; у «Входящие» — `inbox-list`. Набор цветов хранится в БД как HEX, поэтому новые цвета добавляй в `ListColorTokens`, а не в экраны.'''),
    'Icons': ('Контурные значки приложения: сетка 24, линия 2, круглые концы.',
              '''Бери только значки из превью и ресурсов `ic_*`; цвет задавай оттенком (`text-secondary`, `accent`, `inactive`). Не рисуй новые значки другой толщины линии.
Назначение: `tool_checklist` — чек-лист, `tool_priority` — флаг приоритета, `tool_reminder` — уведомления, `tool_repeat` — повтор, `tool_delete` — удалить, `no_date` — без даты,
`time` — поле времени, `widget_all`/`widget_inbox`/`widget_list` — вкладки и календарь, `widget_add` — «+», `widget_update` — обновление, `fold_open`/`fold_closed` — стрелка «свернуть» у «Просрочено», `move_today` — «перенести на сегодня», `history` — недавний запрос в поиске, `snack_restore` — «задача возвращена» в подсказке.'''),
    'SheetCard': ('Шторка задачи: название, описание, поля даты и времени, панель кнопок и галочка «Готово».',
                  '''Карточка `surface`, радиус `radius-nav`, ручка `size-sheet-handle-width`×`size-sheet-handle-height` цвета `divider`, поля по бокам `space-md`. Над панелью клавиатуры карточка стоит прямо над ней.
Название — `headline-small`, описание — `body-large` (Medium, `text-secondary`), подсказки полей — `inactive`.
Новая задача: круга «выполнено» нет, повтор и уведомления бледные, пока нет даты. Созданная задача: круг цвета приоритета слева, «⋮» справа от названия.
Меню «⋮» содержит «Удалить» (`danger`, без подтверждения: после нажатия окно закрывается, внизу `UndoSnackbar` «Задача удалена · Отмена») и под чертой «создано …»; корзины в панели нет.
Панель: список, приоритет, чек-лист, повтор, уведомления, справа галочка. Кнопки сжимаются на узком экране, но не меньше своего минимума.'''),
    'SheetDialog': ('Окно по центру экрана: заголовок, варианты, «Отмена» и «Готово».',
                    '''Ширина не больше `size-dialog-max-width` (420), радиус `radius-nav`, поля `space-lg`. Заголовок — `title-large`, подзаголовок — `body-medium` (`text-secondary`).
Содержимое прокручивается, кнопки внизу всегда видны; кнопки — одна строка, а на узком экране с крупным шрифтом вторая уходит ниже. «Отмена» — контур `divider`, «Готово» — `accent`.
Несколько вариантов — с квадратом-галочкой слева; один из многих — выбранный подсвечен `selected`, справа зелёная галочка. Справа от варианта — когда сработает (`body-small`).
Окно меняет задачу только по «Готово»; «Назад» и нажатие мимо окна — «Отмена». Если окну нечего менять (нет даты) — подсказка и одна кнопка «Понятно».'''),
    'DateWindow': ('Окно даты и времени на всю высоту: сетка на 3 дня, блок «2 недели», нижняя строка кнопок.',
                   '''Сетка: 3 дня, час подгоняется под высоту так, чтобы 10:00–24:00 были видны сразу, но не меньше `size-week-hour-min` (32). Окно открывается с 10:00 для задачи без времени и с часа времени у созданной.
Блок «2 недели»: ячейка `size-date-cell` (40), кружок дня `size-date-day` (34); прошедшие дни бледные и недоступны, сегодня в кольце `accent`, выбранный залит `accent`,
три дня сетки — под подложкой `selected`, у 1-го числа подпись месяца. День выбирается нажатием сразу, сетка переезжает к нему, время остаётся.
Нижняя строка: «←» (отменить изменения и вернуться), «без даты», «‹ 26 окт – 08 ноя ›» (листание по 2 недели, назад с первой страницы нельзя), «Календарь» значком, галочка (применить).
Кнопки сжимаются на узком экране до `size-date-button-min` (36), место под подпись растёт вместе с крупным шрифтом (`size-date-range-min` × масштаб).'''),
    'CalendarDialog': ('Календарь на ближайшие 2 месяца: день выбирается нажатием, окно закрывается.',
                       '''Окно по центру (`SheetDialog` без «Готово»), единственная кнопка — «Закрыть». Два месяца: текущий и следующий, недели с понедельника.
Прошедшие дни бледные и недоступны, сегодня в кольце, выбранный зелёный. Месяц нигде больше не открывается.'''),
    'TimelineRow': ('Строка шкалы дня: время слева, кружок приоритета на линии, белая плашка с названием.',
                    '''Высота `size-widget-row` (46), у периода `size-widget-period-row` (64). Колонка времени `size-widget-time` (38) справа выровнена, колонка линии `size-widget-rail` (26), кружок `size-widget-node` (20), контур `size-stroke`.
Плашка — `surface`, радиус `radius-md`, щель сверху и снизу `size-widget-card-inset` (3), внутри `space-sm`. Название — `body-medium` Bold; справа повтор, колокольчик, квадратик списка.
Период: кружок растягивается в капсулу на всю высоту плашки, конец периода серым под началом. Просрочка: время и день красные (`overdue`), кружок `inactive`.
Выполнена: кружок залит цветом приоритета с белой галочкой, плашка 70%, название зачёркнуто. Тот же вид в приложении и в виджете.'''),
}


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    (OUT / 'components').mkdir(parents=True)
    (OUT / 'fonts').mkdir()
    for w in (400, 500, 600, 700, 800):
        shutil.copy(RES / 'font' / f'manrope_{w}.ttf', OUT / 'fonts')
    tokens = build_tokens()
    (OUT / 'tokens.json').write_text(json.dumps(tokens, ensure_ascii=False, indent=1))
    previews = {
        'ToolButton': preview_tool_button, 'DateFields': preview_date_fields, 'DoneCircle': preview_done_circle, 'ReminderBell': preview_bell,
        'ListSquare': preview_list_square, 'Icons': preview_icons, 'SheetCard': preview_sheet_card, 'SheetDialog': preview_dialog,
        'DateWindow': preview_date_window, 'CalendarDialog': preview_calendar, 'TimelineRow': preview_timeline_row,
    }
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import ds_more
    previews.update(ds_more.PREVIEWS)
    COMP_README.update(ds_more.README_MORE)
    COMP_README['WidgetBar'] = ('Нижняя строка виджета: переключатель «Все» / «Входящие», кнопка обновления, «+».',
                                '''Высота `size-widget-bar` (58), поля по бокам 6. Слева капсула `surface` (радиус `radius-lg`, поля 4) с двумя кнопками 46×42 (радиус `radius-md`): выбранная залита `selected` со значком `accent` 26, другая `surface` со значком `text-secondary`.
Место между капсулой и кнопками нажимается как «открыть приложение». Справа квадратная кнопка обновления `size-widget-update` (44, радиус `radius-md`, `surface`, значок 22 `text-secondary`), щель 8 и «+» `size-widget-add` (46, радиус `radius-md`, `accent`, значок 24 `on-accent`).''')
    groups = {}
    for name, fn in previews.items():
        html = fn()
        groups.setdefault(re.search(r'group="([^"]+)"', html)[1], []).append(name)
    order = ['Основы', 'Панель и поля', 'Окна', 'Список и виджет', 'Напоминания и виджет', 'Экраны']
    lines = ['## Компоненты', '', 'Компонент рисуется по его превью. Если в превью нет нужного состояния, не придумывай его: добавь в систему. Источник каждого — в README компонента.', '']
    for g in order:
        lines.append(f'**{g}**')
        lines += [f'- `{n}` — {COMP_README[n][0]}' for n in groups.get(g, [])]
        lines.append('')
    (OUT / 'README.md').write_text(README.replace('@@COMPONENTS@@', '\n'.join(lines)))
    for name, fn in previews.items():
        folder = OUT / 'components' / name
        folder.mkdir()
        (folder / 'preview.html').write_text(fn())
        summary, text = COMP_README[name]
        (folder / 'README.md').write_text(f'{summary}\n\n{text}\n')
    (OUT / 'components' / 'Cover').mkdir()
    (OUT / 'components' / 'Cover' / 'preview.html').write_text(cover())
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    index = {
        'v': 3, 'layout': 'files', 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Simple Tasks', 'namespace': 'SimpleTasks', 'libraries': [],
        'sections': {}, 'groups': [], 'assetGroups': {}, 'blobs': {}, 'docs': {'readme': 'project/README.md', 'sections': []},
        'lastChange': {'by': 'Ilya', 'at': now, 'via': 'Claude Code', 'note': 'Обновление до 0.41.1: объём виджета — 7 дней, 10 просроченных, 30 в списках, не больше 40 плашек.'},
    }
    (OUT / 'design-system.json').write_text(json.dumps(index, ensure_ascii=False, indent=1))
    # Контраст пар из описаний (WCAG 2): печатаем таблицу для проверки, не пишем в файлы.
    pairs = [('text-primary', 'surface'), ('text-primary', 'background'), ('text-secondary', 'surface'), ('text-secondary', 'background'), ('accent', 'surface'),
             ('accent', 'selected'), ('on-accent', 'accent'), ('inactive', 'surface'), ('overdue', 'surface'), ('danger', 'surface'), ('ring-high', 'surface'),
             ('ring-medium', 'surface'), ('ring-low', 'surface')]
    for a, b in pairs:
        print(f'{a:15} на {b:11} светлая {contrast(COLORS[a][0], COLORS[b][0]):4.1f}  тёмная {contrast(COLORS[a][1], COLORS[b][1]):4.1f}')
    print(len(tokens['color']['tokens']), 'цветов;', len(tokens['size']['tokens']), 'размеров')


if __name__ == '__main__':
    main()
