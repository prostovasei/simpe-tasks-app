"""#259: 10 вариантов Недели, на согласовании; приложение не изменяется.

Каркас листов — approved/241; геометрия — WeekCell, WidgetWeekRows и токены #255.
HTML-прототип воспроизводится без сети; это не Android/Roborazzi-снимок.
Запуск: python3 design/approved/259/screens.py; node design/approved/259/render.js.
"""
from pathlib import Path
import base64
import calendar
import html
import re
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
TOKENS = ROOT / 'app/src/main/java/com/prostovasei/simpletasks/designsystem/tokens'
COLORS = (TOKENS / 'ColorTokens.kt').read_text()
DIMS = (TOKENS / 'DimensionTokens.kt').read_text()
RES = ROOT / 'app/src/main/res'


def dim(name):
    return int(re.search(r'const val ' + name + r' = (\d+)', DIMS)[1])


def color(name):
    return '#' + re.search(r'val ' + name + r' = ThemedColor\(light = 0xFF([A-Fa-f0-9]{6})', COLORS)[1]


def vector(name):
    """Реальные pathData общих значков #255, без генеративной перерисовки."""
    node = ET.parse(RES / 'drawable' / (name + '.xml')).getroot()
    ns = '{http://schemas.android.com/apk/res/android}'
    paths = []
    for p in node:
        fill = p.get(ns + 'fillColor', '')
        attrs = {'d': p.get(ns + 'pathData'), 'fill': 'none' if 'transparent' in fill else 'currentColor'}
        if p.get(ns + 'strokeWidth'):
            attrs.update(stroke='currentColor', **{'stroke-width': p.get(ns + 'strokeWidth')})
        if p.get(ns + 'strokeLineCap'):
            attrs['stroke-linecap'] = p.get(ns + 'strokeLineCap')
        paths.append('<path ' + ' '.join(f'{k}="{v}"' for k, v in attrs.items()) + '/>')
    return f'<svg viewBox="0 0 {node.get(ns + "viewportWidth")} {node.get(ns + "viewportHeight")}" aria-hidden="true">{"".join(paths)}</svg>'


RING = vector('ic_week_node_ring')
REPEAT = vector('ic_task_repeat_round')
DONE = '<svg viewBox="0 0 14 14" aria-hidden="true"><circle cx="7" cy="7" r="7" fill="currentColor"/><path d="m3.5 7 2.3 2.3 4.7-4.8" fill="none" stroke="white" stroke-width="1.5"/></svg>'

# Предлагаемые токены: только эскизы, в Kotlin не внесены.
PROPOSALS = {'deep': '#4C167F', 'soft': '#F0E5FA', 'medium': '#D8BDEB', 'white': '#FFFFFF'}
VARIANTS = [
    ('01', 'Насыщенный текст', 'Тёмный фиолетовый и ExtraBold 800; без подложки.', 'Тихий вариант: заметнее нынешнего, но остаётся полностью плоским.'),
    ('02', 'Лёгкая заливка', 'Светло-фиолетовая полоса на всю строку.', 'Заметен весь контур строки; обычные задачи остаются на белом.'),
    ('03', 'Боковой маркер', 'Фиолетовая полоса справа и насыщенный текст.', 'Маркер не сдвигает время и значок, но находится рядом с эмодзи.'),
    ('04', 'Тонкая рамка', 'Фиолетовый контур вокруг строки.', 'Чёткая граница без заливки; в плотном дне линий станет больше.'),
    ('05', 'Линия под строкой', 'Сплошная фиолетовая линия снизу.', 'Выделяет всю задачу без изменения фона; может напоминать разделитель.'),
    ('06', 'Акцент на времени', 'Время белым на фиолетовом, название — фиолетовым.', 'У задачи без времени пустая колонка тоже получает акцент.'),
    ('07', 'Подложка названия', 'Лавандовый блок только под названием.', 'Время, значок и эмодзи на белом; длина текста не меняется.'),
    ('08', 'Белое на фиолетовом', 'Тёмная фиолетовая строка с белым текстом и значком.', 'Самый сильный контраст; заметно меняет общий спокойный вид недели.'),
    ('09', 'Затухающая заливка', 'Фиолетовая подложка слева постепенно уходит в белый.', 'Акцент на начале строки; виджет потребует отдельного drawable.'),
    ('10', 'Маркер под буквами', 'Лавандовый штрих под нижней частью названия.', 'Выделяется само название, без рамки и заливки всей строки.'),
]

# Только вымышленные данные. Во всех вариантах и обоих представлениях они одинаковые.
TASKS = [
    [('09:30', 'Оплатить интернет', 'high'), ('10:00', 'Созвон с командой', 'high repeat'),
     ('', 'Забрать документы', 'high'), ('12:00', 'Отправить договор', ''),
     ('15:00', 'Готовый отчёт', 'high done'), ('18:00', 'Купить продукты', 'done')],
    [('08:00', 'Зарядка', 'repeat'), ('11:00', 'Записаться к врачу', 'high'), ('', 'Позвонить маме', '')],
    [('10:00', 'Будущий повтор', 'high repeat ghost'), ('14:00', 'Проверить документы', 'high'), ('', 'Прогулка', '')],
    [],
    [('09:30', 'Оплатить интернет', 'high'), ('10:00', 'Созвон с командой', 'high repeat'),
     ('', 'Забрать документы', 'high'), ('12:00', 'Отправить договор', ''),
     ('15:00', 'Готовый отчёт', 'high done'), ('18:00', 'Купить продукты', 'done')],
    [('10:00', 'Очень длинная приоритетная задача с подробностями', 'high'), ('16:00', 'Купить корм', ''),
     ('18:00', 'Выполненный повтор', 'high repeat done')],
    [('', 'План на неделю', 'high'), ('12:00', 'Обед с друзьями', '')],
]
DAYS = ['ПН 16', 'ВТ 17', 'СР 18', 'ЧТ 19', 'ПТ 20', 'СБ 21', 'ВС 22']


def task(data):
    time, title, flags = data
    icon = REPEAT if 'repeat' in flags else DONE if 'done' in flags else RING
    action = 'Открыть исходную задачу' if 'ghost' in flags else 'Вернуть задачу' if 'done' in flags else 'Выполнить задачу'
    return (f'<div class="task {flags}"><button class="toggle" data-action="{action}" aria-label="{action}: {html.escape(title)}">'
            f'<span class="time">{time}</span><span class="node">{icon}</span></button>'
            f'<button class="open" data-action="Открыть задачу" aria-label="Открыть: {html.escape(title)}">'
            f'<span class="title"><span>{html.escape(title)}</span></span><span class="emoji">💼</span></button></div>')


def day(index=0, compact=False):
    rows = TASKS[index]
    if compact:
        rows = [rows[i] for i in [0, 1, 3, 4]]
    more = '<span>+3</span>' if index == 4 else ''
    return (f'<div class="day" data-day="{DAYS[index]}"><button class="day-head {"today" if index == 1 else ""}" data-action="Открыть день">'
            f'<span>{DAYS[index]}</span>{more}</button><div class="rows">' + ''.join(task(t) for t in rows) + '</div>'
            '<button class="blank" data-action="Открыть день" aria-label="Открыть день: пустая область под задачами"></button></div>')


def mini_calendar():
    weekdays = ''.join(f'<span>{d}</span>' for d in ['пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'])
    cells = []
    # Шесть строк, как в WeekCalendar/WidgetWeekRows.
    for week in calendar.Calendar().monthdatescalendar(2026, 11)[:6]:
        cells.append('<button class="cal-week" data-action="Выбрать неделю">' + ''.join(
            f'<span class="{"selected" if 16 <= d.day <= 22 and d.month == 11 else ""} {"muted" if d.month != 11 else ""} {"cal-today" if d.day == 17 and d.month == 11 else ""}">{d.day}</span>' for d in week) + '</button>')
    return '<div class="calendar"><div class="cal-labels">' + weekdays + '</div>' + ''.join(cells) + '</div>'


def nav(widget=False):
    symbols = {'plus': '<path d="M12 4v16M4 12h16"/>', 'cal': '<rect x="4" y="5" width="16" height="15" rx="2"/><path d="M4 10h16M8 3v4M16 3v4"/>',
               'week': '<rect x="3" y="4" width="18" height="16" rx="1"/><path d="M9 4v16M15 4v16"/>',
               'lists': '<path d="M4 6h16M4 12h16M4 18h16"/>', 'check': '<path d="m5 12 5 5 9-10"/>'}
    def icon(name):
        return f'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">{symbols[name]}</svg>'
    center = ''.join(f'<span class="{"active" if n == "week" else ""}">{icon(n)}</span>' for n in ['cal', 'week', 'lists'])
    end = '<span class="date">17 <small>ВТ<br>ноя</small></span>' if widget else f'<span class="module">{icon("check")}</span>'
    return f'<div class="navbar"><span class="add">{icon("plus")}</span><div class="pages">{center}</div>{end}</div>'


def screen(kind='app', variant='00', width=390, large=False):
    return (f'<div class="device {kind} v{variant} {"large" if large else ""}" style="--device-width:{width}px">'
            + ('' if kind == 'widget' else '<div class="status"><b>09:41</b><span>▰ ▰ ▰</span></div>')
            + '<div class="week-grid">' + ''.join(day(i) for i in range(7)) + mini_calendar() + '</div>' + nav(kind == 'widget')
            + ('' if kind == 'widget' else '<div class="gesture"><i></i></div>') + '</div>')


fonts = ''.join('@font-face{font-family:Manrope;font-weight:' + str(w) + ';src:url(data:font/ttf;base64,'
                + base64.b64encode((RES / 'font' / f'manrope_{w}.ttf').read_bytes()).decode() + ')}' for w in [500, 700, 800])
priority = re.search(r'object PriorityTokens.*?content = ThemedColor\(light = 0xFF([A-F0-9]{6})', COLORS, re.S)[1]
CSS = fonts + f'''
:root{{--text:{color('textPrimary')};--secondary:{color('textSecondary')};--done:{color('done')};--priority:#{priority};--priority-done:{color('priorityDone')};--line:#E8E8EA;
--deep:{PROPOSALS['deep']};--soft:{PROPOSALS['soft']};--medium:{PROPOSALS['medium']};--white:{PROPOSALS['white']};
--gap:{dim('WEEK_CONTENT_GAP')}px;--node:{dim('WEEKVIEW_CHECK')}px;--row:{dim('WEEKVIEW_TASK')}px;--row-gap:{dim('WEEKVIEW_TASK_GAP')}px}}
''' + '''
*{box-sizing:border-box}body{margin:0;background:#EBECEF;color:var(--text);font:14px/1.5 Manrope,sans-serif}button{font:inherit;color:inherit;border:0;background:transparent;padding:0;cursor:pointer}a{color:#582695}h1,h2,h3,p{margin:0}h1{font-size:26px}h2{font-size:22px}h3{font-size:15px}p{color:var(--secondary)}.intro{padding:28px 32px 18px;max-width:1100px}.intro p{margin-top:8px}.nav{position:sticky;top:0;z-index:5;background:#ffffffed;padding:10px 32px;display:flex;gap:8px;flex-wrap:wrap;border-bottom:1px solid #DDD}.nav a{font-size:12px;text-decoration:none;padding:4px 10px;background:#F0E5FA;border-radius:6px}.board{padding:20px 32px 50px}.sec{scroll-margin-top:90px;width:1016px;margin:0 0 28px;padding:26px;background:#fff;border:1px solid #DADBE0;border-radius:12px}.sec>p{margin:6px 0 20px;font-size:12px}.badge{font-size:10px;letter-spacing:.5px;font-weight:800;color:#6B32A8}.rowp{display:flex;gap:36px;align-items:flex-start}.rowp figure{margin:0}.label{font-size:12px;color:var(--secondary);margin:0 0 10px}.caption{font-size:11px;color:var(--secondary);margin-top:12px;max-width:390px}.device{width:var(--device-width);height:844px;display:flex;flex-direction:column;overflow:hidden;background:#fff;border:1px solid #C8C9CF;border-radius:10px;font:500 12px/16px Manrope,sans-serif;--time-width:var(--app-time,34px);--cell-height:188px}
.status{height:24px;display:flex;justify-content:space-between;padding:2px 14px;font-size:10px}.status b{font-weight:700}.week-grid{display:grid;grid-template-columns:1fr 1fr;grid-template-rows:repeat(4,minmax(0,1fr));flex:1;min-height:0}.day{min-width:0;min-height:0;border-right:1px solid var(--line);border-bottom:1px solid var(--line);padding:8px 2px;display:flex;flex-direction:column}.day-head{height:20px;flex-shrink:0;display:flex;align-items:center;justify-content:space-between;font-size:11px;line-height:16px;letter-spacing:.5px;color:var(--secondary);font-weight:500;text-align:left}.day-head.today{color:var(--text);font-weight:800}.rows{flex-shrink:0}.task{height:var(--row);margin-top:var(--row-gap);display:flex;align-items:center;position:relative;font-weight:700;color:var(--text);border-radius:4px}.toggle{display:flex;align-items:center;gap:var(--gap);flex-shrink:0;height:100%;padding:0 var(--gap) 0 0}.time{width:var(--time-width);height:100%;display:flex;align-items:center;font-size:11px;letter-spacing:.5px;font-variant-numeric:tabular-nums;line-height:16px;text-align:left}.node{display:flex;width:var(--node);height:var(--node);flex-shrink:0;color:#A9A9A9}.node svg{width:100%;height:100%;overflow:visible}.open{display:flex;align-items:center;flex:1;min-width:0;gap:4px;height:100%;text-align:left}.title{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;line-height:20px;font-size:12px}.emoji{flex-shrink:0;width:16px;font-size:12px}.high{color:var(--priority)}.high .node{color:inherit}.done{color:var(--done)}.done .node{color:inherit}.high.done{color:var(--priority-done)}.done .emoji{opacity:.35}.ghost{opacity:1}.blank{flex:1;min-height:0;width:100%;text-align:left}.calendar{display:flex;flex-direction:column;padding:7px 4px;border-bottom:1px solid var(--line);min-height:0;font-size:11px}.cal-labels,.cal-week{display:grid;grid-template-columns:repeat(7,1fr);text-align:center;align-items:center}.cal-labels{height:18px;color:#A9A9A9}.cal-week{flex:1;min-height:0}.cal-week span{display:grid;place-items:center;min-width:0;height:22px}.cal-week .selected{background:#EAEAEA}.cal-week .muted{color:#A9A9A9}.cal-week .cal-today{border-radius:50%;background:var(--text);color:#fff;font-weight:800}.navbar{height:44px;display:flex;align-items:center;justify-content:space-between;gap:8px;padding:5px 14px;flex-shrink:0}.navbar svg{width:22px;height:22px}.navbar .add,.navbar .module{width:72px;height:34px;border-radius:12px;display:grid;place-items:center;flex-shrink:0}.navbar .add{background:var(--text);color:#fff}.navbar .module{border:2px solid var(--text)}.pages{display:flex;justify-content:center;gap:2px;min-width:0;flex:1}.pages>span{width:64px;display:grid;place-items:center;color:#A9A9A9}.pages>span.active{color:var(--text)}.gesture{height:24px;display:grid;place-items:center;flex-shrink:0}.gesture i{width:100px;height:4px;border-radius:3px;background:#16171D}
.widget{height:650px;border-radius:16px;font-family:Arial,sans-serif;--time-width:var(--widget-time,28px);--row-gap:2px;--cell-height:152px;--row:calc((var(--cell-height) - 16px)/6 - 2px)}.widget .day{padding:1px 4px 2px 2px}.widget .day-head{height:12px;font:400 9px/12px Arial,sans-serif;letter-spacing:0}.widget .day-head.today{font-weight:700}.widget .time,.widget .title,.widget .emoji{font:700 10px/14px Arial,sans-serif;letter-spacing:0}.widget .emoji{width:12px}.widget .navbar{height:40px;padding:4px 6px}.widget .navbar .add{width:64px;height:32px;border-radius:8px}.widget .pages{flex:0 0 auto;padding:0 4px}.widget .pages>span{width:44px}.date{display:flex;gap:4px;align-items:center;font-size:18px;font-weight:700;width:64px;justify-content:center}.date small{font-size:9px;line-height:10px;font-weight:500}.widget .calendar{font-family:Arial,sans-serif}.widget .cal-week span{height:18px}
.v01 .high:not(.done){color:var(--deep);font-weight:800}.v01 .high:not(.done) .time,.v01 .high:not(.done) .title{font-weight:800}
.v02 .high:not(.done){background:var(--soft);color:var(--deep)}
.v03 .high:not(.done){box-shadow:inset -3px 0 var(--priority);color:var(--deep)}
.v04 .high:not(.done){box-shadow:inset 0 0 0 1px var(--priority);color:var(--deep)}
.v05 .high:not(.done){box-shadow:inset 0 -2px var(--priority);color:var(--deep)}
.v06 .high:not(.done) .time{background:var(--deep);color:var(--white);border-radius:3px;align-self:center;height:18px;display:flex;align-items:center}
.v07 .high:not(.done) .title{background:var(--medium);color:var(--deep);border-radius:3px}
.v08 .high:not(.done){color:var(--white);background:var(--deep)}
.v09 .high:not(.done){color:var(--deep);background:linear-gradient(90deg,var(--medium),var(--white))}
.v10 .high:not(.done) .title>span{color:var(--deep);background:linear-gradient(transparent 50%,var(--medium) 50%,var(--medium) 94%,transparent 94%)}
.overview-row{display:grid;grid-template-columns:265px 290px 290px;gap:24px;margin-top:20px;align-items:center;border-top:1px solid #E8E8EA;padding-top:18px}.sample{width:290px;height:159px;overflow:hidden}.sample .day{width:195px;height:106px;transform:scale(1.48);transform-origin:top left;border:0;padding-top:0}.sample .day-head{display:none}.sample .task{--row:22px;--row-gap:3px}.sample .widget .task{--row:18px;--row-gap:2px}.sample .day .blank{display:none}.sample .device{width:195px;height:106px;border:0;border-radius:0;overflow:visible}.sample .widget{--row:18px}.overview-row h3{font-size:17px;margin-bottom:5px}.overview-row p{font-size:11px;max-width:240px}.overview-labels{display:grid;grid-template-columns:265px 290px 290px;gap:24px;color:var(--secondary);font-size:11px;margin-top:18px}
.map .toggle{background:#D5EEDC;box-shadow:inset 0 0 0 1px #3F9360}.map .ghost .toggle{background:#E1E7FD;box-shadow:inset 0 0 0 1px #697BB5}.map .open{background:#E1E7FD;box-shadow:inset 0 0 0 1px #697BB5}.map .blank,.map .day-head{background:#FFF0C7;box-shadow:inset 0 0 0 1px #BD963B}.map .blank{display:grid;place-items:center;font-size:10px;color:#8E691C}.map .blank::after{content:'Открыть день'}.legend{display:flex;gap:18px;font-size:11px;margin-bottom:18px}.legend i{display:inline-block;width:14px;height:14px;vertical-align:middle;margin-right:5px}.legend .green{background:#D5EEDC}.legend .blue{background:#E1E7FD}.legend .yellow{background:#FFF0C7}.map-note{font-size:12px;max-width:390px;margin-top:15px}.action-log{position:fixed;right:16px;bottom:16px;padding:12px 18px;background:#16171D;color:white;border-radius:8px;z-index:10;display:none;max-width:400px;font-size:12px}.action-log.on{display:block}.large{--font-scale:1.3;--row:22px;--row-gap:3px;--time-width:var(--app-time-large,44px)}.large .time{font-size:14.3px}.large .title{font-size:15.6px}.large .day-head{font-size:14.3px}.large .emoji{font-size:15.6px;width:19px}.large .node{width:14px;height:14px}.large.widget{--time-width:var(--widget-time-large,36px)}.large.widget .title,.large.widget .time{font-size:13px}.large.widget .node{width:14px;height:14px}.large.widget .task{--row:calc((var(--cell-height) - 16px)/6 - 2px)}
'''


def section(ident, title, desc, content):
    return f'<section class="sec" id="{ident}"><div class="badge">#259 · НА СОГЛАСОВАНИИ · 17.11.2026</div><h2>{title}</h2><p>{desc}</p>{content}</section>'


boards = []
for start in [0, 5]:
    content = '<div class="overview-labels"><span>Вариант</span><span>Приложение · фрагмент ×1,48</span><span>Виджет · фрагмент ×1,48</span></div>'
    for num, title, desc, note in VARIANTS[start:start + 5]:
        samples = ''.join(f'<div class="sample"><div class="device {kind} v{num}">{day(0,compact=True)}</div></div>' for kind in ['app', 'widget'])
        content += f'<div class="overview-row"><div><h3>{num} · {title}</h3><p>{desc}</p></div>{samples}</div>'
    boards.append(section(f'overview-{start//5+1}', f'10 вариантов · {start+1}–{start+5}', 'Одинаковые задачи и масштаб. Бледная строка внизу — выполненный приоритет; усиление к ней не применяется.', content))

boards.append(section('baseline', '00 · Как сейчас', 'Текущие цвета и начертания. Этот образец не входит в десять вариантов.',
    '<div class="rowp">' + ''.join(f'<figure><div class="label">{label}</div>{screen(kind)}<figcaption class="caption">Источник: main a3d1ed1, #255; HTML-эскиз.</figcaption></figure>' for kind, label in [('app', 'Приложение'), ('widget', 'Виджет')]) + '</div>'))

for num, title, desc, note in VARIANTS:
    body = '<div class="rowp">' + ''.join(f'<figure><div class="label">{label} · {num}</div>{screen(kind,num)}<figcaption class="caption">{note}</figcaption></figure>' for kind, label in [('app', 'Приложение · 390×844'), ('widget', 'Виджет · 390×650')]) + '</div>'
    boards.append(section('variant-' + num, num + ' · ' + title, desc + ' Все варианты сохраняют сетку, шрифтовые размеры и интервалы #255.', body))

legend = '<div class="legend"><span><i class="green"></i>Выполнить / вернуть</span><span><i class="blue"></i>Открыть задачу</span><span><i class="yellow"></i>Открыть день</span></div>'
map_body = '<div class="rowp">' + ''.join(f'<figure><div class="label">{label}</div><div class="map">{screen(kind)}</div><figcaption class="map-note">Заголовок и пустая область ниже задач открывают день. В зелёную зону входит пустая колонка времени. Промежутки между строками не открывают день. У будущего повтора вся строка открывает исходную задачу.</figcaption></figure>' for kind, label in [('app', 'Приложение'), ('widget', 'Виджет')]) + '</div>'
boards.append(section('interactions', 'Зоны нажатий', 'Общие для всех 10 вариантов. Цветные зоны — только пояснение макета, в приложение они не переходят. Можно нажать и увидеть название действия.', legend + map_body))
stress_body = '<div class="rowp">' + ''.join(f'<figure><div class="label">{label}</div>{screen(kind,"02",320,True)}<figcaption class="caption">Длинное название обрезается, время и значок сохраняются. Высоты строк и значки прежние; точное отображение большого шрифта нужно проверить в Android после реализации.</figcaption></figure>' for kind, label in [('app', 'Приложение · 320 px · шрифт 130%'), ('widget', 'Виджет · 320 px · шрифт 130%')]) + '</div>'
boards.append(section('stress', 'Узкий экран и крупный шрифт', 'Пример на варианте 02; это проверка компоновки HTML, не подтверждение работы на Android.', stress_body))

JS = '''
const log=document.querySelector('.action-log');
document.addEventListener('click', e=>{const b=e.target.closest('[data-action]');if(!b)return;
const day=b.closest('.day')?.dataset.day||'';log.textContent=b.dataset.action+(day?' · '+day:'')+' (демонстрация)';log.classList.add('on');});
// Измеренная HH:mm-колонка, как в #255, отдельно для Manrope и системного шрифта виджета.
window.layoutReady=document.fonts.ready.then(()=>{
const canvas=document.createElement('canvas');const ctx=canvas.getContext('2d');
for(const [key,font] of [['app','700 11px Manrope'],['widget','700 10px Arial'],['app-large','700 14.3px Manrope'],['widget-large','700 13px Arial']]){
ctx.font=font;const app=key.startsWith('app');const spacing=app?2.5*(key.endsWith('large')?1.3:1):0;
document.documentElement.style.setProperty('--'+key.replace('-large','')+'-time'+(key.endsWith('large')?'-large':''),Math.ceil(ctx.measureText('00:00').width+spacing)+'px');}
// Сохраняем текущую вместимость: строки, не влезающие по высоте, уходят в +N.
for(const d of document.querySelectorAll('.device:not(.sample .device)')){
const widget=d.classList.contains('widget');const cell=d.querySelector('.day');
if(widget)d.style.setProperty('--cell-height',cell.getBoundingClientRect().height+'px');
for(const day of d.querySelectorAll('.day')){
const rows=day.querySelector('.rows');const tasks=[...rows.children];let hidden=0;
while(rows.getBoundingClientRect().bottom>day.getBoundingClientRect().bottom-2&&tasks.length){tasks.pop().remove();hidden++;}
if(hidden){const head=day.querySelector('.day-head');let more=head.querySelector('span:nth-child(2)');if(!more){more=document.createElement('span');head.appendChild(more);}more.textContent='+'+(hidden+(day.dataset.day==='ПТ 20'?3:0));}
}}
});
'''
links = '<a href="#overview-1">Сравнить 1–5</a><a href="#overview-2">Сравнить 6–10</a><a href="#baseline">Как сейчас</a>' + ''.join(f'<a href="#variant-{n}">{n}</a>' for n, *_ in VARIANTS) + '<a href="#interactions">Нажатия</a><a href="#stress">320 px / 130%</a>'
out = ('<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>#259 · Неделя · 10 вариантов</title>'
       f'<style>{CSS}</style><header class="intro"><h1>Неделя: заметный приоритет и нажатия</h1><p>10 вариантов для приложения и виджета. Ни один ещё не выбран. Код приложения не изменён.</p>'
       '<p>Размеры и базовые цвета — из Kotlin-токенов 0.71.0. Manrope встроен; системный шрифт виджета приближён браузерным Arial. Это HTML-прототип, а не снимки Android.</p></header>'
       f'<nav class="nav">{links}</nav><main class="board">{"".join(boards)}</main><output class="action-log" aria-live="polite"></output><script>{JS}</script></html>')
(HERE / 'screens.html').write_text(out)
print('screens.html: 10 вариантов + исходный вид + зоны нажатий + крайние случаи')
