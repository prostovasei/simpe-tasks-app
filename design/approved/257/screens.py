"""#257 «Здоровье»: учёт веса — макеты на согласование PM (16.11.2026).
Стиль — белый плоский фон редизайна #247/#249 и нижняя панель #241. Данные вымышленные.
Запуск из корня: python3 design/approved/257/screens.py → screens.html; PNG: node design/approved/257/render.js."""
from datetime import date, timedelta
from pathlib import Path

here = Path(__file__).parent
TODAY = date(2026, 11, 16)
MONTHS = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня', 'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря']


def d(x):
    return f'{x.day} {MONTHS[x.month - 1]}'


def kg(v):
    return f'{v:.1f}'.replace('.', ',')


# Вымышленный месяц: (сколько дней назад, вес, комментарий)
RAW = [(30, 83.2, ''), (28, 83.4, ''), (27, 83.1, ''), (25, 83.0, ''), (23, 83.3, 'после праздника'), (21, 82.9, ''), (20, 82.9, ''),
       (18, 82.7, ''), (16, 82.8, ''), (14, 82.6, ''), (12, 82.9, ''), (10, 82.5, ''), (9, 82.6, ''), (7, 82.4, 'после тренировки'),
       (5, 82.6, ''), (4, 82.5, ''), (2, 82.7, ''), (1, 82.6, ''), (0, 82.4, '')]
ENTRIES = [(TODAY - timedelta(days=a), w, c) for a, w, c in RAW]
GOAL, HEIGHT = 78.0, 180

ICONS = {
    'plus': '<path d="M12 5v14M5 12h14"/>',
    'tick': '<path d="m5 12 5 5 9-10"/>',
    'ruble': '<path d="M9 20V4h4.5a4 4 0 0 1 0 8H6.5M6.5 16H15"/>',
    'heart': '<path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"/>',
    'scale': '<rect x="4" y="4" width="16" height="16" rx="4"/><path d="M9 9.5a4 4 0 0 1 6 0M12 9.5l1.2-1.6"/>',
    'gear': '<circle cx="12" cy="12" r="3"/><path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M5.6 18.4l2.1-2.1M16.3 7.7l2.1-2.1"/>',
    'trash': '<path d="M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12"/>',
    'x': '<path d="M6 6l12 12M18 6 6 18"/>',
    'cal': '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>',
    'chev': '<path d="m10 6 6 6-6 6"/>',
    'back': '<path d="M15 6l-6 6 6 6"/>',
    'info': '<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5v.5"/>',
    'note': '<path d="M5 5h14v10l-4 4H5z"/><path d="M15 19v-4h4"/>',
}


def ico(n):
    return f'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{ICONS[n]}</svg>'


# ---------- каркас телефона ----------
def navbar(module='health', menu_open=False):
    pages = {'health': [('scale', True)], 'tasks': [('cal', True), ('cal', False), ('cal', False)]}[module]
    cells = ''.join(f'<button class="pg{" on" if on else ""}">{ico(n)}</button>' for n, on in pages)
    m = {'health': 'heart', 'tasks': 'tick', 'finance': 'ruble'}[module]
    return (f'<nav class="navbar"><button class="add">{ico("plus")}</button><div class="pages">{cells}</div>'
            f'<button class="modbtn{" open" if menu_open else ""}">{ico(m)}</button></nav><div class="gesture"><span></span></div>')


def module_menu(cur='health'):
    def row(icon, title, key=None):
        c = key == cur
        return f'<button class="mrow{" cur" if c else ""}">{ico(icon)}<span>{title}</span>{"<i class=cdot></i>" if c else ""}</button>'
    return ('<div class="amenu">' + row('tick', 'Задачи', 'tasks') + row('ruble', 'Финансы', 'finance') + row('heart', 'Здоровье', 'health')
            + '<div class="msep"></div>' + row('gear', 'Настройки') + '</div>')


def phone(body, overlay='', menu_open=False, bar=True, status=True):
    st = '<div class="status"><b>9:41</b><span>●●● ▮</span></div>' if status else ''
    nav = navbar('health', menu_open) if bar else '<div class="gesture"><span></span></div>'
    extra = f'<div class="dim"></div>{module_menu()}' if menu_open else ''
    return f'<div class="phone"><div class="screen">{st}<main class="content">{body}</main>{nav}</div>{extra}{overlay}</div>'


def dialog(title, body, actions, left='', right=''):
    head = f'<div class="dh">{left or "<span></span>"}<b>{title}</b>{right or "<span></span>"}</div>'
    return f'<div class="dim"></div><div class="dialog">{head}{body}<div class="dact">{actions}</div></div>'


# ---------- график ----------
def chart(entries, w=358, h=170, goal=GOAL, tip=None, period_days=30):
    pts = [((TODAY - x).days, v) for x, v, _ in entries if (TODAY - x).days <= period_days]
    vals = [v for _, v in pts] + ([goal] if goal else [])
    lo, hi = min(vals) - 0.4, max(vals) + 0.4
    pl, pr, pt, pb = 8, 40, 10, 22

    def X(a):
        return pl + (period_days - a) / period_days * (w - pl - pr)

    def Y(v):
        return pt + (hi - v) / (hi - lo) * (h - pt - pb)
    grid = ''
    step = 2 if hi - lo > 4 else 1
    v = int(lo) + 1
    while v < hi:
        grid += f'<line x1="{pl}" x2="{w - pr}" y1="{Y(v):.1f}" y2="{Y(v):.1f}" class="gl"/><text x="{w - pr + 6}" y="{Y(v) + 4:.1f}" class="ax">{v}</text>'
        v += step
    gl = (f'<line x1="{pl}" x2="{w - pr}" y1="{Y(goal):.1f}" y2="{Y(goal):.1f}" class="goal"/>'
          f'<text x="{w - pr + 6}" y="{Y(goal) + 4:.1f}" class="ax g">{kg(goal)}</text>') if goal else ''
    path = ' '.join(f'{"M" if i == 0 else "L"}{X(a):.1f},{Y(v):.1f}' for i, (a, v) in enumerate(pts))
    dots = ''.join(f'<circle cx="{X(a):.1f}" cy="{Y(v):.1f}" r="{4.5 if a == 0 else 2.6}" class="{"dot last" if a == 0 else "dot"}"/>' for a, v in pts)
    labels = ''
    if period_days == 30:
        for a in (30, 15, 0):
            day = TODAY - timedelta(days=a)
            labels += f'<text x="{X(a):.1f}" y="{h - 4}" class="ax" text-anchor="{"start" if a == 30 else "end" if a == 0 else "middle"}">{day.day} {MONTHS[day.month - 1][:3]}</text>'
    tipsvg = ''
    if tip is not None:
        a, v = next((a, v) for a, v in pts if a == tip)
        x, y = X(a), Y(v)
        tipsvg = (f'<line x1="{x:.1f}" x2="{x:.1f}" y1="{pt}" y2="{h - pb}" class="cur"/><circle cx="{x:.1f}" cy="{y:.1f}" r="6" class="dot sel"/>'
                  f'<g transform="translate({x - 52:.1f},{y - 46:.1f})"><rect width="104" height="34" rx="9" class="tipbg"/>'
                  f'<text x="52" y="15" text-anchor="middle" class="tipv">{kg(v)} кг</text><text x="52" y="28" text-anchor="middle" class="tipd">{d(TODAY - timedelta(days=a))}</text></g>')
    return f'<svg class="chart" viewBox="0 0 {w} {h}" width="{w}" height="{h}">{grid}{gl}<path d="{path}" class="line"/>{dots}{tipsvg}{labels}</svg>'


def periods(active=0):
    return '<div class="seg">' + ''.join(f'<button class="{"on" if i == active else ""}">{t}</button>' for i, t in enumerate(['Месяц', '3 мес', 'Год', 'Всё'])) + '</div>'


def goal_line(text, sub=None, muted=False):
    s = f'<small>{sub}</small>' if sub else ''
    return f'<button class="goalrow{" muted" if muted else ""}"><span>{text}{s}</span>{ico("chev")}</button>'


GOAL_MAIN = goal_line('Цель 78,0 кг · осталось 4,4 кг', '≈ к 12 марта по текущему темпу · ИМТ 25,4 — выше нормы')


def history(entries, n=8, mark=None):
    rows = ''
    es = sorted(entries, key=lambda e: e[0], reverse=True)
    for i, (day, w, c) in enumerate(es[:n]):
        prev = es[i + 1][1] if i + 1 < len(es) else None
        diff = '' if prev is None else ('±0' if abs(w - prev) < 0.05 else (f'−{kg(prev - w)}' if w < prev else f'+{kg(w - prev)}'))
        cls = 'down' if prev is not None and w < prev - 0.05 else ''
        label = d(day) + (' · сегодня' if day == TODAY else ' · вчера' if day == TODAY - timedelta(days=1) else '')
        cm = f'<small>{c}</small>' if c else ''
        hl = ' hl' if mark == i else ''
        rows += f'<div class="hrow{hl}"><span class="hd">{label}{cm}</span><span class="hdiff {cls}">{diff}</span><b class="hw">{kg(w)}</b></div>'
    return f'<div class="sect">Записи</div>{rows}'


def head(weight, change, period='за месяц'):
    return (f'<div class="hero"><div class="big">{weight}<span>кг</span></div>'
            f'<div class="chg"><b>{change}</b> {period} · последняя запись сегодня</div></div>')


MAIN = head('82,4', '−0,8 кг') + GOAL_MAIN + periods(0) + chart(ENTRIES) + history(ENTRIES)
TIP = head('82,4', '−0,8 кг') + GOAL_MAIN + periods(0) + chart(ENTRIES, tip=7) + history(ENTRIES, 6, mark=4)
EMPTY = ('<div class="empty"><div class="eico">' + ico('scale') + '</div><b>Запишите первый вес</b>'
         '<p>Здесь появятся текущий вес, график и история. Цель и рост можно задать после первой записи.</p>'
         '<button class="primary">Записать вес</button></div>')


# ---------- окна ----------
def drum(values, sel, unit='', wide=False):
    items = ''.join(f'<i class="{"s" if i == sel else "f" + str(abs(i - sel))}">{v}</i>' for i, v in enumerate(values))
    return f'<div class="drum{" w" if wide else ""}"><div class="win"></div>{items}{f"<em>{unit}</em>" if unit else ""}</div>'


def weight_drums(whole=82, tenth=4):
    return ('<div class="drums">' + drum([whole - 2, whole - 1, whole, whole + 1, whole + 2], 2, wide=True)
            + '<div class="comma">,</div>' + drum([(tenth + k) % 10 for k in (-2, -1, 0, 1, 2)], 2) + '<div class="unit">кг</div></div>')


def frow(icon, text, sub='', right=''):
    s = f'<small>{sub}</small>' if sub else ''
    return f'<div class="frow">{ico(icon)}<span>{text}{s}</span>{right}</div>'


NEW = dialog('Вес', weight_drums() + '<p class="hint">Заранее стоит последний вес — 82,6 кг вчера</p>'
             + frow('cal', 'Сегодня, 16 ноября') + frow('note', '<i class="ph">Комментарий</i>'),
             '<button class="ghost">Отмена</button><button class="primary">Сохранить</button>')
REPLACE = dialog('Вес', weight_drums(82, 3) + frow('cal', 'Вчера, 15 ноября') + '<p class="warn">На 15 ноября уже 82,6 кг — заменится</p>'
                 + frow('note', 'после тренировки'),
                 '<button class="ghost">Отмена</button><button class="primary">Сохранить</button>')
EDIT = dialog('Запись · 9 ноября', weight_drums(82, 4) + frow('cal', '9 ноября') + frow('note', 'после тренировки'),
              '<button class="ghost">Отмена</button><button class="primary">Сохранить</button>',
              right=f'<button class="hbtn danger">{ico("trash")}</button>')
UNDO = '<div class="snack"><span>Запись за 9 ноября удалена</span><b>Отменить</b></div>'


def goal_drums():
    return ('<div class="glabel">Цель</div><div class="drums">' + drum([76, 77, 78, 79, 80], 2, wide=True) + '<div class="comma">,</div>'
            + drum([8, 9, 0, 1, 2], 2) + '<div class="unit">кг</div></div>'
            + '<div class="glabel">Рост</div><div class="drums">' + drum([178, 179, 180, 181, 182], 2, wide=True) + '<div class="unit">см</div></div>')


GOAL_DLG = dialog('Цель и рост', goal_drums() + '<p class="hint">ИМТ сейчас 25,4 — выше нормы (норма 18,5–24,9). При 78,0 кг будет 24,1.</p>'
                  + '<button class="link danger-t">Убрать цель</button>',
                  '<button class="ghost">Отмена</button><button class="primary">Сохранить</button>')

CAL_DAYS = ''.join(f'<i class="{"t" if n == 16 else "x" if n > 16 else "w" if n in (1, 2, 4, 5, 7, 9, 10, 12, 14, 15) else ""}">{n}</i>' for n in range(1, 31))
CAL = dialog('Дата записи', '<div class="calm"><b>Ноябрь 2026</b><span>‹ ›</span></div><div class="calg"><u>Пн</u><u>Вт</u><u>Ср</u><u>Чт</u><u>Пт</u><u>Сб</u><u>Вс</u>'
             + '<i></i>' * 6 + CAL_DAYS + '</div><p class="hint">Точка — день с записью. Будущие даты недоступны.</p>',
             '<button class="ghost">Отмена</button><button class="primary">Выбрать</button>')


# ---------- настройки ----------
def service(title, body, overlay=''):
    return (f'<div class="phone"><div class="screen"><div class="status"><b>9:41</b><span>●●● ▮</span></div>'
            f'<div class="svc">{ico("back")}<b>{title}</b></div><main class="content">{body}</main><div class="gesture"><span></span></div></div>{overlay}</div>')


def setrow(icon, title, sub, right=None):
    r = right if right is not None else ico('chev')
    return f'<div class="setrow">{ico(icon)}<span>{title}<small>{sub}</small></span>{r}</div>'


SW_ON, SW_OFF = '<span class="sw on"><i></i></span>', '<span class="sw"><i></i></span>'
SET_HOME = service('Настройки', setrow('tick', 'Задачи', 'Ввод, напоминания, импорт и резервная копия') + setrow('ruble', 'Финансы', 'Категории, места, корзина, копия, банк')
                   + setrow('heart', 'Здоровье', 'Напоминание, резервная копия') + setrow('info', 'Приложение', 'Версия 0.71.0 · обновление'))
SET_HEALTH = service('Здоровье', '<div class="sect">Напоминание</div>' + setrow('note', 'Напоминание взвеситься', 'Только если сегодня записи нет', SW_ON)
                     + setrow('cal', 'Время', '08:00') + '<div class="sect">Данные</div>'
                     + setrow('note', 'Резервная копия', 'Последняя — нет · записей веса: 19'))
RESTORE = ('<div class="dim"></div><div class="dialog"><div class="dh"><span></span><b>Восстановить копию?</b><span></span></div>'
           '<table class="cmp"><tr><th></th><th>В копии</th><th>Сейчас</th></tr><tr><td>Записей веса</td><td>142</td><td>19</td></tr>'
           '<tr><td>Период</td><td>3 янв – 2 ноя</td><td>17 окт – 16 ноя</td></tr><tr><td>Цель / рост</td><td>78,0 / 180</td><td>78,0 / 180</td></tr>'
           '<tr><td>Напоминание</td><td>выкл.</td><td>08:00</td></tr></table>'
           '<p class="warn">Данные «Здоровья» заменятся целиком. Задачи и финансы не меняются.</p>'
           '<div class="dact"><button class="ghost">Отмена</button><button class="primary">Восстановить</button></div></div>')
BACKUP = service('Резервная копия', setrow('note', 'Сохранить в файл', 'JSON: записи веса, цель, рост, напоминание')
                 + setrow('note', 'Восстановить из файла', 'Заменит данные «Здоровья» целиком'), RESTORE)

NOTIF = ('<div class="phone lock"><div class="screen"><div class="lockt">8:00<small>понедельник, 16 ноября</small></div>'
         '<div class="notif"><div class="nh">' + ico('heart') + '<span>Simple Tasks · сейчас</span></div><b>Пора взвеситься</b>'
         '<p>Сегодня записи веса ещё нет. Последняя — 82,6 кг вчера.</p><div class="na"><span>Записать</span></div></div>'
         '<p class="lnote">Нажатие на уведомление или «Записать» открывает окно ввода веса. Если сегодня вес уже записан — уведомления нет.</p></div></div>')


# ---------- состояния строки цели ----------
GOAL_STATES = ('<div class="states">'
               + '<div class="st"><em>Нет цели и роста</em>' + goal_line('Задать цель и рост', 'Для прогноза и ИМТ', muted=True) + '</div>'
               + '<div class="st"><em>Есть цель, мало данных</em>' + goal_line('Цель 78,0 кг · осталось 4,4 кг', 'Прогноз — после 5 записей за 2 недели · ИМТ 25,4 — выше нормы') + '</div>'
               + '<div class="st"><em>Вес идёт от цели</em>' + goal_line('Цель 78,0 кг · осталось 4,4 кг', 'За 30 дней вес растёт — прогноза нет · ИМТ 25,4') + '</div>'
               + '<div class="st"><em>Цель достигнута</em>' + goal_line('Цель 78,0 кг достигнута 🎉', 'Сейчас 77,8 кг · ИМТ 24,0 — норма') + '</div>'
               + '<div class="st"><em>Только рост, без цели</em>' + goal_line('ИМТ 25,4 — выше нормы', 'Задать цель') + '</div>'
               + '</div>')


def fig(title, cap, body, w=390):
    return f'<figure style="width:{w}px">{body}<figcaption><b>{title}</b>{cap}</figcaption></figure>'


s1 = [
    fig('1.1 Вес — главный экран', 'Сверху текущий вес и изменение за выбранный период. Строка цели: остаток, прогнозная дата, ИМТ; нажатие — окно цели. Переключатель периода, график (цель — пунктир), записи: дата, разница с предыдущей (снижение — зелёным), вес; комментарий серым.', phone(MAIN)),
    fig('1.2 Нажатие на точку графика', 'Подсказка «вес · дата», соответствующая запись подсвечена в истории. Повторное нажатие на ту же точку открывает правку.', phone(TIP)),
    fig('1.3 Первый вход', 'Пока записей нет. «Записать вес» и «+» внизу открывают одно окно ввода.', phone(EMPTY)),
    fig('1.4 Меню кнопки модуля', 'Третий модуль «Здоровье» со значком сердца; на нижней панели модуля — одна страница «Вес» (значок весов).', phone(MAIN, menu_open=True)),
]
s2 = [
    fig('2.1 Новая запись', 'Окно по центру. Барабаны «кг» (30–250) и «десятые»; заранее — последний вес. Дата «Сегодня» — нажатие открывает календарь. Комментарий необязателен.', phone(MAIN, NEW)),
    fig('2.2 Дата уже занята', 'Одна запись на дату: если на выбранный день вес есть, видно, что он заменится.', phone(MAIN, REPLACE)),
    fig('2.3 Календарь даты', 'Точка — день с записью. Будущие даты серые и недоступны.', phone(MAIN, CAL)),
    fig('2.4 Правка и удаление', 'Нажатие на запись — то же окно, в заголовке корзина. После удаления — полоска «Отменить» на ~5 секунд.', phone(MAIN, EDIT)),
    fig('2.5 После удаления', 'Полоска над нижней панелью.', phone(MAIN, UNDO)),
]
s3 = [
    fig('3.1 Цель и рост', 'Барабаны цели (0,1 кг) и роста (см). Пересчёт ИМТ сразу. «Убрать цель» — линия и прогноз исчезают, рост остаётся.', phone(MAIN, GOAL_DLG)),
    fig('3.2 Состояния строки цели', 'Тексты строки под весом в разных случаях. Прогноз — по тренду за последние 30 дней, если ≥ 5 записей за ≥ 14 дней и вес движется к цели.', GOAL_STATES),
]
s4 = [
    fig('4.1 Настройки — главная', 'Новый раздел «Здоровье» между «Финансами» и «Приложением».', SET_HOME),
    fig('4.2 Раздел «Здоровье»', 'Напоминание (по умолчанию выключено) и время; резервная копия.', SET_HEALTH),
    fig('4.3 Восстановление копии', 'Как у финансов: сначала сравнение «в копии / сейчас», затем замена данных «Здоровья» целиком. Негодный файл ничего не меняет.', BACKUP),
    fig('4.4 Уведомление', 'Приходит в выбранное время, только если сегодня записи нет.', NOTIF),
]

style = '''
*{box-sizing:border-box}body{margin:0;font-family:Roboto,Arial,sans-serif;background:#ececee;color:#16171d}
:root{--text:#16171d;--sec:#62656e;--inact:#a9a9a9;--line:#e8e8ea;--soft:#f3f3f5;--danger:#c62828;--income:#1f8f4e;--accent:#8e4ec6}
button{font:inherit;border:0;background:none;color:inherit;padding:0}
.board{padding:28px;display:flex;flex-direction:column;gap:28px}.sec{background:#fff;border-radius:24px;padding:24px 28px;width:max-content}
.sec h2{margin:0 0 4px;font-size:22px}.sec>p{margin:0 0 18px;color:var(--sec);font-size:13px;max-width:1100px}
.rowp{display:flex;gap:26px;align-items:flex-start}figure{margin:0}figcaption{font-size:12.5px;line-height:1.5;color:var(--sec);margin-top:12px}figcaption b{display:block;color:var(--text);font-size:14px;margin-bottom:3px}
.phone{width:390px;height:844px;border-radius:36px;overflow:hidden;position:relative;box-shadow:0 0 0 1px #d4d4d8,0 10px 28px #0000001a;background:#fff}
.screen{height:100%;display:flex;flex-direction:column;background:#fff}
.status{height:34px;display:flex;justify-content:space-between;align-items:center;padding:0 22px;font-size:13px;flex-shrink:0}.status span{font-size:9px;letter-spacing:1px}
.content{flex:1;overflow:hidden;padding:0 16px}
.navbar{display:flex;align-items:center;justify-content:space-between;height:44px;padding:5px 14px;border-top:1px solid var(--line);background:#fff;flex-shrink:0}
.navbar .add{width:72px;height:34px;border-radius:12px;background:var(--text);color:#fff;display:grid;place-items:center}.navbar svg{width:22px;height:22px}
.navbar .pages{flex:1;display:flex;justify-content:center}.pg{width:64px;height:34px;display:grid;place-items:center;color:var(--inact)}.pg.on{color:var(--text)}
.modbtn{width:72px;height:34px;border-radius:12px;display:grid;place-items:center;box-shadow:inset 0 0 0 1.6px var(--text)}.modbtn.open{background:var(--text);color:#fff;box-shadow:none}
.gesture{height:17px;display:grid;place-items:center;background:#fff;flex-shrink:0}.gesture span{width:120px;height:4px;border-radius:2px;background:#16171d}
.dim{position:absolute;inset:0;background:#0000003d;z-index:3}
.amenu{position:absolute;right:12px;bottom:68px;width:196px;background:#fff;border-radius:18px;box-shadow:0 12px 32px #0003;padding:6px;z-index:4}
.mrow{display:flex;align-items:center;gap:12px;width:100%;text-align:left;padding:12px;border-radius:12px;font-size:14px}.mrow svg{width:20px;height:20px}.mrow span{flex:1}.mrow.cur{font-weight:700}.cdot{width:8px;height:8px;border-radius:50%;background:var(--text)}.msep{height:1px;background:var(--line);margin:4px 10px}
.hero{padding:14px 2px 6px}.big{font-size:52px;font-weight:700;letter-spacing:-1.5px;line-height:1}.big span{font-size:20px;font-weight:500;margin-left:6px;letter-spacing:0;color:var(--sec)}
.chg{font-size:13px;color:var(--sec);margin-top:8px}.chg b{color:var(--income)}
.goalrow{display:flex;align-items:center;width:100%;text-align:left;gap:8px;padding:10px 0;border-bottom:1px solid var(--line);font-size:14px;font-weight:600}
.goalrow span{flex:1;display:flex;flex-direction:column;gap:3px}.goalrow small{font-size:12px;font-weight:400;color:var(--sec)}.goalrow svg{width:16px;height:16px;color:var(--inact)}.goalrow.muted{color:var(--sec)}
.seg{display:flex;gap:4px;margin:12px 0 6px}.seg button{flex:1;height:30px;border-radius:10px;font-size:13px;color:var(--sec)}.seg .on{background:var(--soft);color:var(--text);font-weight:700}
.chart{display:block;margin:0 -2px}.gl{stroke:var(--line);stroke-width:1}.ax{font-size:10px;fill:var(--inact)}.ax.g{fill:var(--accent);font-weight:700}
.goal{stroke:var(--accent);stroke-width:1.4;stroke-dasharray:4 4}.line{fill:none;stroke:var(--text);stroke-width:2;stroke-linejoin:round}
.dot{fill:#fff;stroke:var(--text);stroke-width:1.6}.dot.last{fill:var(--text)}.dot.sel{fill:var(--text);stroke:#fff;stroke-width:2.5}.cur{stroke:var(--inact);stroke-dasharray:2 3}
.tipbg{fill:var(--text)}.tipv{fill:#fff;font-size:12px;font-weight:700}.tipd{fill:#cfd0d4;font-size:10px}
.sect{font-size:12px;font-weight:700;color:var(--sec);margin:12px 0 2px;text-transform:none}
.hrow{display:flex;align-items:center;gap:10px;padding:9px 0;border-bottom:1px solid var(--line);font-size:14px}.hrow.hl{background:var(--soft);margin:0 -16px;padding:9px 16px}
.hd{flex:1;display:flex;flex-direction:column;gap:2px}.hd small{font-size:12px;color:var(--sec)}.hdiff{font-size:12px;color:var(--sec);width:44px;text-align:right}.hdiff.down{color:var(--income)}.hw{width:46px;text-align:right}
.empty{height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;gap:10px;padding:0 30px}.empty .eico svg{width:44px;height:44px;color:var(--inact)}.empty b{font-size:18px}.empty p{font-size:13px;color:var(--sec);margin:0 0 6px;line-height:1.5}
.primary{background:var(--text);color:#fff;border-radius:12px;height:42px;padding:0 20px;font-weight:700;font-size:14px}.ghost{height:42px;padding:0 16px;font-size:14px;color:var(--sec)}
.dialog{position:absolute;left:20px;right:20px;top:50%;transform:translateY(-50%);background:#fff;border-radius:24px;padding:14px 18px 16px;z-index:5;box-shadow:0 16px 40px #0003}
.dh{display:flex;align-items:center;justify-content:space-between;height:34px;margin-bottom:6px}.dh b{font-size:16px}.dh>span,.hbtn{width:34px;height:34px;display:grid;place-items:center}.hbtn svg{width:20px;height:20px}.danger{color:var(--danger)}
.dact{display:flex;justify-content:flex-end;gap:8px;margin-top:14px}
.drums{display:flex;align-items:center;justify-content:center;gap:4px;margin:4px 0 6px}.comma{font-size:34px;font-weight:700;margin-top:-6px}.unit{font-size:18px;color:var(--sec);margin-left:8px}
.drum{position:relative;width:52px;display:flex;flex-direction:column;align-items:center}.drum.w{width:84px}.drum i{font-style:normal;height:34px;line-height:34px;font-variant-numeric:tabular-nums}
.drum .s{font-size:34px;font-weight:700;height:48px;line-height:48px}.drum .f1{font-size:22px;color:#9a9ca3}.drum .f2{font-size:17px;color:#cfd0d4}
.drum .win{position:absolute;left:0;right:0;top:68px;height:48px;border-radius:12px;background:var(--soft);z-index:-1}.drum{z-index:0}
.hint{font-size:12px;color:var(--sec);margin:4px 2px 8px;line-height:1.45}.warn{font-size:12.5px;color:#9a5b00;background:#fff4e0;border-radius:10px;padding:8px 10px;margin:6px 0}
.frow{display:flex;align-items:center;gap:12px;padding:12px 2px;border-top:1px solid var(--line);font-size:14px}.frow svg{width:19px;height:19px;color:var(--sec)}.frow span{flex:1}.ph{color:var(--inact);font-style:normal}
.glabel{font-size:12px;font-weight:700;color:var(--sec);margin-top:4px}.link{font-size:13px;padding:6px 2px}.danger-t{color:var(--danger)}
.snack{position:absolute;left:12px;right:12px;bottom:72px;background:var(--text);color:#fff;border-radius:14px;padding:14px 16px;display:flex;justify-content:space-between;font-size:14px;z-index:5}.snack b{color:#d4b8f5}
.calm{display:flex;justify-content:space-between;font-size:14px;padding:4px 2px 8px}.calg{display:grid;grid-template-columns:repeat(7,1fr);gap:4px;text-align:center;font-size:13px}
.calg u{text-decoration:none;color:var(--inact);font-size:11px}.calg i{font-style:normal;height:34px;line-height:30px;position:relative}.calg i.w::after{content:"";position:absolute;left:50%;bottom:3px;width:4px;height:4px;margin-left:-2px;border-radius:50%;background:var(--text)}
.calg i.t{background:var(--text);color:#fff;border-radius:10px;font-weight:700}.calg i.t::after{background:#fff}.calg i.x{color:#d0d1d5}
.svc{display:flex;align-items:center;gap:12px;height:48px;padding:0 14px;font-size:17px;flex-shrink:0}.svc svg{width:22px;height:22px}
.setrow{display:flex;align-items:center;gap:12px;padding:14px 2px;border-bottom:1px solid var(--line);font-size:14px}.setrow>svg{width:20px;height:20px}.setrow>svg:last-child{width:16px;height:16px;color:var(--inact)}
.setrow span:not(.sw){flex:1;display:flex;flex-direction:column;gap:3px}.setrow small{font-size:11.5px;color:var(--sec)}
.sw{width:40px;height:24px;border-radius:12px;background:#d6d6da;position:relative;flex-shrink:0}.sw i{position:absolute;top:3px;left:3px;width:18px;height:18px;border-radius:50%;background:#fff}.sw.on{background:var(--text)}.sw.on i{left:19px}
.cmp{width:100%;border-collapse:collapse;font-size:13px}.cmp th{font-size:11px;color:var(--sec);text-align:left;padding:4px}.cmp td{padding:7px 4px;border-top:1px solid var(--line)}.cmp td:first-child{color:var(--sec)}
.lock .screen{background:linear-gradient(#3b3f52,#1d1f2a);color:#fff;padding:0 14px}.lockt{font-size:60px;font-weight:300;text-align:center;margin-top:90px}.lockt small{display:block;font-size:15px;margin-top:4px}
.notif{background:#ffffffeb;color:var(--text);border-radius:22px;padding:14px 16px;margin-top:40px}.nh{display:flex;gap:8px;align-items:center;font-size:11.5px;color:var(--sec)}.nh svg{width:15px;height:15px}
.notif b{display:block;margin-top:8px;font-size:15px}.notif p{font-size:13.5px;margin:4px 0 10px}.na{display:flex;gap:20px;font-weight:700;font-size:13.5px;color:#6b32a8}.lnote{font-size:12px;opacity:.75;margin:18px 6px;line-height:1.5}
.states{width:390px;background:#fff;border-radius:24px;box-shadow:0 0 0 1px #d4d4d8;padding:10px 16px}.st{padding:8px 0}.st em{font-style:normal;font-size:11px;color:var(--accent);font-weight:700}
'''

secs = [('1. Раздел «Здоровье» → «Вес»', 'Третий модуль. Каркас как у «Задач» и «Финансов» (#241), фон и строки — белые плоские, как в редизайне #247.', s1),
        ('2. Ввод, правка, удаление', 'Окно по центру с барабаном. Одна запись на дату; точность 0,1 кг.', s2),
        ('3. Цель, прогноз, ИМТ', 'Всё в одной строке под весом; задаются через окно цели.', s3),
        ('4. Настройки, копия, напоминание', 'Раздел «Здоровье» в единых настройках.', s4)]
html = ('<!doctype html><html lang="ru"><meta charset="utf-8"><title>Здоровье — вес (#257)</title><style>' + style + '</style><div class="board">'
        + ''.join(f'<section class="sec" id="s{i + 1}"><h2>{t}</h2><p>{dsc}</p><div class="rowp">{"".join(f)}</div></section>' for i, (t, dsc, f) in enumerate(secs))
        + '</div></html>')
(here / 'screens.html').write_text(html)
print('screens.html готов; картинки: node design/approved/257/render.js')
