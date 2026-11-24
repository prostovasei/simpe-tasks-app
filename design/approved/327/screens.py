"""Макеты #327 «Бюджет на месяц по категориям». Запуск из корня:
python3 design/approved/327/screens.py && node design/approved/327/render.js
Цвета — токены ColorTokens (warning — новый), шрифт Manrope из app/src/main/res/font. Суммы вымышленные, сегодня 24 ноября 2026."""
from html import escape
from math import ceil, cos, pi, sin
from pathlib import Path

HERE = Path(__file__).resolve().parent
DAY, DAYS = 24, 30  # 24 ноября, в ноябре 30 дней
PACE = DAY / DAYS

expenses = [  # эмодзи, категория, бюджет, потрачено
    ('🏠', 'Аренда', 25_000, 25_000),
    ('🛒', 'Продукты', 20_000, 16_800),
    ('☕', 'Кафе', 6_000, 6_800),
    ('🚕', 'Транспорт', 5_000, 3_100),
    ('📱', 'Связь', 1_000, 650),
]
unplanned = [('🎁', 'Подарки', 2_400), ('💊', 'Здоровье', 0)]
incomes = [('💼', 'Зарплата', 80_000, 65_000), ('💻', 'Подработка', 10_000, 12_000)]
average = {'Аренда': 25_000, 'Продукты': 21_300, 'Кафе': 7_400, 'Транспорт': 3_900, 'Связь': 700, 'Подарки': 2_600,
           'Здоровье': None, 'Зарплата': 80_000, 'Подработка': 8_500}

plan_out = sum(b for *_, b, _ in expenses)
spent = sum(s for *_, s in expenses) + sum(s for *_, s in unplanned)
plan_in = sum(b for *_, b, _ in incomes)
got = sum(s for *_, s in incomes)
assert (plan_out, spent, plan_in, got) == (57_000, 54_750, 90_000, 77_000)


def money(amount, rub=True):
    text = f'{amount:,}'.replace(',', ' ')
    return text + (' ₽' if rub else '')


def state(budget, fact):
    """Состояние расходов по темпу (§3 ТЗ)."""
    if fact > budget:
        return 'over', f'перерасход {money(fact - budget)}'
    if fact == budget:
        return 'done', 'бюджет выбран'
    if fact > budget * PACE:
        return 'ahead', f'осталось {money(budget - fact)}'
    return 'ok', f'осталось {money(budget - fact)}'


def bar(kind, fill, mark=True):
    today = f'<b style="left:{PACE * 100:.1f}%"></b>' if mark else ''
    return f'<div class="bar {kind}"><i style="width:{min(fill, 1) * 100:.1f}%"></i>{today}</div>'


def expense_row(emoji, name, budget, fact, head=False):
    kind, note = state(budget, fact)
    cls = 'row head' if head else 'row'
    icon = '' if head else f'<span class="emoji">{emoji}</span>'
    return f'''<div class="{cls}">
  <div class="line">{icon}<span class="name">{escape(name)}</span><span class="amt">{money(fact, False)} <span class="of">из {money(budget)}</span></span></div>
  {bar(kind, fact / budget)}
  <div class="note {kind}">{note}</div>
</div>'''


def income_row(emoji, name, plan, fact, head=False):
    note = f'сверх плана {money(fact - plan)}' if fact > plan else f'ещё {money(plan - fact)}'
    kind = 'extra' if fact > plan else 'wait'
    cls = 'row head' if head else 'row'
    icon = '' if head else f'<span class="emoji">{emoji}</span>'
    return f'''<div class="{cls}">
  <div class="line">{icon}<span class="name">{escape(name)}</span><span class="amt">{money(fact, False)} <span class="of">из {money(plan)}</span></span></div>
  {bar('income', fact / plan, mark=False)}
  <div class="note {kind}">{note}</div>
</div>'''


def plain_row(emoji, name, right, muted=False):
    cls = ' muted' if muted else ''
    return f'<div class="row plain{cls}"><div class="line"><span class="emoji">{emoji}</span><span class="name">{escape(name)}</span><span class="amt">{right}</span></div></div>'


def window(title, month, body, footer='', top=24, height=None):
    style = f'margin-top:{top}px;' + (f'height:{height}px;' if height else '')
    return f'''<div class="window" style="{style}">
  <header><h1>{title}</h1><span class="month">{month}</span><span class="close">{CLOSE}</span></header>
  <div class="scroll">{body}</div>{footer}
</div>'''


CLOSE = '<svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>'
CHECK = '<svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>'
BACK = '<svg viewBox="0 0 24 24"><path d="M19 12H5m6-6l-6 6 6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>'
RECEIPT = '<svg viewBox="0 0 24 24"><path d="M6 3h12v18l-3-2-3 2-3-2-3 2zM9 8h6M9 12h6M9 16h3" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>'
GAUGE = '<svg viewBox="0 0 24 24"><path d="M3.5,17A8.5,8.5 0,0 1,20.5 17H3.5 M12,15.5L15.5,11.5 M6.7,12.7L7.7,13.7" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>'
CALENDAR = '<svg viewBox="0 0 24 24"><path d="M4 6h16v14H4zM4 10h16M8 3v4M16 3v4M15 16h2" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>'


def budget_window():
    body = f'''<p class="plan">Доходы {money(plan_in, False)} − расходы {money(plan_out, False)} = <strong>отложится {money(plan_in - plan_out)}</strong></p>
<h2>Расходы</h2>
{expense_row('', 'Всего', plan_out, spent, head=True)}
{''.join(expense_row(*item) for item in expenses)}
<h3>Без бюджета</h3>
{plain_row('🎁', 'Подарки', money(2_400))}
{plain_row('💊', 'Здоровье', '—', muted=True)}
<h2>Доходы</h2>
{income_row('', 'Всего', plan_in, got, head=True)}
{''.join(income_row(*item) for item in incomes)}'''
    return window('Бюджет', 'ноя 26', body)


def edit_window():
    body = f'''<div class="field-label">Бюджет на месяц</div>
<div class="big">20&nbsp;000 ₽</div>
<p class="hint">Потрачено {money(16_800)} · осталось 7 дней</p>
<div class="chips"><span class="chip">В среднем {money(21_300)}</span></div>
<p class="hint small">Пустое поле — без бюджета.</p>'''
    footer = '<div class="actions"><button class="primary wide">Сохранить</button></div>'
    return window('🛒 Продукты', 'ноя 26', body, footer, top=210)


def operation(category, amount, spent_before, budget, emoji_order, selected):
    after = spent_before + amount
    if after > budget:
        hint = f'<span class="over">{category} · превысит бюджет на {money(after - budget)}</span>'
    else:
        hint = f'{category} · останется {money(budget - after)} из {money(budget, False)}'
    tiles = ''.join(f'<span class="tile{" on" if e == selected else ""}">{e}</span>' for e in emoji_order) + '<span class="tile more">⋯</span>'
    return f'''<div class="window op" style="margin-top:150px">
  <div class="segmented"><span class="on">Расход</span><span>Доход</span><span>Перевод</span></div>
  <div class="op-amount"><span>{money(amount)}</span><span class="account">💳 Карта ⌄</span></div>
  <div class="tiles">{tiles}</div>
  <div class="op-hint">{hint}</div>
  <div class="pair"><div><strong>вт. 24 ноя 2026</strong><small>сегодня</small></div><div class="placeholder">Место платежа</div></div>
  <div class="comment"><span>Комментарий</span><span class="placeholder">Необязательно</span></div>
  <div class="actions"><button class="ghost">{BACK}</button><button class="primary">{CHECK}</button></div>
</div>'''


def next_window():
    rows = []
    for emoji, name, budget, _ in expenses:
        rows.append(plan_row(emoji, name, budget, average[name]))
    rows.append(plan_row('🎁', 'Подарки', None, average['Подарки']))
    income = ''.join(plan_row(e, n, b, average[n]) for e, n, b, _ in incomes)
    body = f'''<p class="plan">Копия ноября. Под суммой — средние за 3 месяца, нажатие подставляет.</p>
<h2>Расходы <span class="sum">{money(plan_out)}</span></h2>
{''.join(rows)}
<h2>Доходы <span class="sum">{money(plan_in)}</span></h2>
{income}'''
    footer = '<div class="actions"><button class="ghost text">Всё по средним</button><button class="primary">Подтвердить</button></div>'
    return window('Бюджет', 'дек 26', body, footer)


def plan_row(emoji, name, budget, avg):
    value = money(budget) if budget else '<span class="placeholder">—</span>'
    chip = f'<span class="chip small">в среднем {money(avg, False)}</span>' if avg and avg != budget else '<span class="chip small same">как в среднем</span>' if avg else ''
    return f'<div class="row plan-row"><div class="line"><span class="emoji">{emoji}</span><span class="name">{escape(name)}</span><span class="amt">{value}</span></div><div class="avg">{chip}</div></div>'


def empty_window():
    body = '''<p class="empty">Бюджетов пока нет. Задайте сумму у категории или заполните все категории по средним тратам — их можно поправить.</p>
<div class="actions"><button class="primary wide">Заполнить по средним</button></div>'''
    return window('Бюджет', 'ноя 26', body, top=300)


def notifications():
    return f'''<div class="shade">
  <div class="note-card"><div class="app-line"><span class="app-dot"></span>Simple Tasks · Финансы · сейчас</div>
    <strong>Продукты: 84% бюджета</strong><p>Потрачено 16&nbsp;800 из 20&nbsp;000 ₽ · осталось 3&nbsp;200 ₽ на 7 дней</p></div>
  <div class="note-card"><div class="app-line"><span class="app-dot"></span>Simple Tasks · Финансы · 19:00</div>
    <strong>Бюджет на декабрь</strong><p>Оставить как в ноябре — 57&nbsp;000 ₽ расходов — или поправить по средним тратам?</p>
    <div class="note-actions"><span>Оставить</span><span>Изменить</span></div></div>
</div>'''


def entry():
    return f'''<div class="finance">
  <div class="total"><strong>128k ₽</strong><span>+</span></div>
  <div class="accounts"><span>💳 <b>42k</b></span><span>👛 <b>6k</b></span><span>🏦 <b>80k</b></span></div>
  <div class="month-row"><span>2,3k ₽/день</span><span class="switch">‹ &nbsp;ноя 26&nbsp; ›</span></div>
  <div class="summary"><span>{RECEIPT}<b>55k ₽</b></span><span class="tap">{GAUGE}<b>2k ₽</b></span><span>{CALENDAR}<b>68k ₽</b></span></div>
  <svg class="chart" viewBox="0 0 328 120"><path d="M0 1H328M0 60H328M0 119H328" stroke="#E2E2E2"/>
    <polyline points="0,118 8,70 40,66 80,60 120,52 160,46 200,40 238,30 262,26" fill="none" stroke="#16171D" stroke-width="1.6"/>
    <line x1="262" y1="26" x2="328" y2="10" stroke="#16171D" stroke-dasharray="3 3"/></svg>
  <div class="shares tap"><div class="share-line"><i style="width:46%;background:#8858D6"></i><i style="width:18%;background:#438BC9"></i><i style="width:12%;background:#359E8A"></i><i style="width:10%;background:#CC883D"></i><i style="width:14%;background:#8590A7"></i></div>
    <div class="legend"><span>Аренда<b>25k</b></span><span>Продукты<b>17k</b></span><span>Кафе<b>7k</b></span><span>Транспорт<b>3k</b></span></div></div>
  <div class="callout c1">нажатие → окно «Бюджет»</div>
  <div class="callout c2">нажатие → окно «Бюджет»</div>
</div>'''


def frame(name, caption, content, height=760, dim=True):
    cls = 'frame dim' if dim else 'frame'
    return f'<figure class="cell"><div class="{cls}" data-name="{name}" style="min-height:{height}px">{content}</div><figcaption>{caption}</figcaption></figure>'


def limit_variant(kind, widget=False):
    """Варианты полоски лимита вверху «Финансов» (дополнение PM к вопросу 11)."""
    line = bar('ahead', spent / plan_out)
    if kind == 'A':
        return f'''<div class="summary"><span>{RECEIPT}<b>55k ₽</b></span><span>{GAUGE}<b>2k ₽</b></span><span>{CALENDAR}<b>68k ₽</b></span></div>
<div class="limit">{line}<div class="ends"><span>потрачено</span><span>лимит 57k ₽</span></div></div>'''
    if kind == 'B':
        return f'''<div class="summary b"><span>{RECEIPT}<b>55k ₽</b></span><span class="mid"><span class="mid-top">{GAUGE}<b>2k ₽</b></span>{line}<small>из 57k ₽</small></span><span>{CALENDAR}<b>68k ₽</b></span></div>'''
    if kind == 'D':  # Г — толстая полоса с суммой внутри
        fill = spent / plan_out * 100
        return f'''<div class="thick-row"><div class="thick"><i style="width:{fill:.1f}%"><span>55k ₽</span></i><b style="left:{PACE * 100:.1f}%"></b></div><span class="thick-limit">57k</span></div>
<div class="ends"><span class="ahead">осталось 2k ₽</span><span class="forecast">{CALENDAR}прогноз 68k ₽</span></div>'''
    if kind == 'E':  # Д — шкала до прогноза
        top = 68_437
        at = lambda amount: f'{amount / top * 100:.1f}%'
        return f'''<div class="ruler-head"><span>потрачено</span><span class="ahead">осталось 2k ₽</span></div>
<div class="ruler"><span class="bubble" style="left:{at(spent)}">55k</span>
  <div class="track"><i style="width:{at(spent)}"></i><em style="left:{at(plan_out)};width:calc(100% - {at(plan_out)})"></em></div>
  <b class="today" style="left:{at(plan_out * PACE)}"></b><b class="lim" style="left:{at(plan_out)}"></b><span class="dot" style="left:100%"></span>
  <span class="lab" style="left:0">0</span><span class="lab mid" style="left:{at(plan_out)}">лимит 57k</span><span class="lab end" style="left:100%">прогноз 68k</span></div>'''
    if kind == 'F':  # Е — дуга-спидометр в центре
        r, cx, cy = 38, 50, 46
        length = pi * r
        filled = spent / plan_out * length
        theta = pi * (1 - PACE)
        tick = (cx + (r - 7) * cos(theta), cy - (r - 7) * sin(theta), cx + (r + 7) * cos(theta), cy - (r + 7) * sin(theta))
        arc = f'''<svg class="arc" viewBox="0 0 100 52"><path d="M{cx - r} {cy} A{r} {r} 0 0 1 {cx + r} {cy}" fill="none" stroke="#EAEAEA" stroke-width="8" stroke-linecap="round"/>
<path d="M{cx - r} {cy} A{r} {r} 0 0 1 {cx + r} {cy}" fill="none" stroke="#C98500" stroke-width="8" stroke-linecap="round" stroke-dasharray="{filled:.1f} {length:.1f}"/>
<line x1="{tick[0]:.1f}" y1="{tick[1]:.1f}" x2="{tick[2]:.1f}" y2="{tick[3]:.1f}" stroke="#62656E" stroke-width="2"/></svg>'''
        return f'''<div class="summary f"><span>{RECEIPT}<b>55k ₽</b></span><span class="gauge">{arc}<b>2k ₽</b><small>из 57k ₽</small></span><span>{CALENDAR}<b>68k ₽</b></span></div>'''
    return f'''<div class="limit c"><div class="ends top"><span><b>55k</b> из 57k ₽</span><span class="ahead">осталось 2k ₽</span></div>{line}<div class="ends"><span></span><span class="forecast">{CALENDAR}прогноз 68k ₽</span></div></div>'''


def app_top(kind):
    return f'''<div class="finance top">
  <div class="total"><strong>128k ₽</strong><span>+</span></div>
  <div class="accounts"><span>💳 <b>42k</b></span><span>👛 <b>6k</b></span><span>🏦 <b>80k</b></span></div>
  <div class="month-row"><span>2,3k ₽/день</span><span class="switch">‹ &nbsp;ноя 26&nbsp; ›</span></div>
  {limit_variant(kind)}
  <svg class="chart short" viewBox="0 0 328 80"><path d="M0 1H328M0 40H328M0 79H328" stroke="#E2E2E2"/>
    <polyline points="0,78 8,48 40,45 80,40 120,35 160,30 200,26 238,20 262,17" fill="none" stroke="#16171D" stroke-width="1.6"/>
    <line x1="262" y1="17" x2="328" y2="6" stroke="#16171D" stroke-dasharray="3 3"/></svg>
</div>'''


def widget_top(kind):
    return f'''<div class="widget">
  {limit_variant(kind, widget=True)}
  <svg class="chart short" viewBox="0 0 296 70"><path d="M0 1H296M0 35H296M0 69H296" stroke="#E2E2E2"/>
    <polyline points="0,68 8,42 40,39 80,35 120,31 160,26 200,22 236,17" fill="none" stroke="#16171D" stroke-width="1.6"/>
    <line x1="236" y1="17" x2="296" y2="5" stroke="#16171D" stroke-dasharray="3 3"/></svg>
  <div class="widget-bar"><span class="w-add">+</span><span class="w-tabs">✓ &nbsp;&nbsp; <b>₽</b></span><span class="w-month">ноя 26</span></div>
</div>'''


def variant(kind, title, text):
    return f'''<figure class="cell"><div class="variant-title">{title}</div>
<div class="frame" style="min-height:0">{app_top(kind)}</div>
<div class="widget-caption">Виджет 320 dp</div><div class="widget-wrap">{widget_top(kind)}</div>
<figcaption>{text}</figcaption></figure>'''


sheets = [
    ('sheet-1', 'Вход и окно «Бюджет» текущего месяца', [
        frame('entry', '1. Финансы: две точки входа. «Остаток до лимита» = сумма бюджетов (57 000) − потрачено (54 750)', entry(), 470, dim=False),
        frame('budget', '2. Окно «Бюджет», 24 ноября. Чёрточка — план к сегодня (80%). Аренда — «бюджет выбран», Продукты — быстрее плана, Кафе — перерасход', budget_window(), 760),
    ]),
    ('sheet-2', 'Правка бюджета и подсказка в окне операции', [
        frame('edit', '3. Нажатие на «Продукты»: сумма, чип со средним подставляет 21 300', edit_window()),
        frame('operation', '4. Расход 1 200 ₽ в «Продукты»: в бюджете', operation('Продукты', 1_200, 16_800, 20_000, ['🛒', '☕', '🚕', '🏠', '📱'], '🛒')),
        frame('operation-over', '5. Расход 2 500 ₽ в «Транспорт»: выйдет за бюджет', operation('Транспорт', 2_500, 3_100, 5_000, ['🛒', '☕', '🚕', '🏠', '📱'], '🚕')),
    ]),
    ('sheet-3', 'Уведомления и следующий месяц', [
        frame('notifications', '6. Уведомления: 80% бюджета и «Бюджет на декабрь» (24 ноября, 19:00)', notifications(), 470, dim=False),
        frame('next', '7. «Изменить» из уведомления: декабрь, копия ноября + средние', next_window(), 760),
        frame('empty', '8. Первый запуск: бюджетов нет', empty_window(), 760),
    ]),
    ('sheet-4', 'Полоска лимита вверху «Финансов» и виджета — выберите вариант', [
        variant('A', 'А · Полоска под суммами', 'Три суммы как сейчас. Под ними полоска во всю ширину: заполнение — потрачено, чёрточка — план к сегодня, справа «лимит 57k». Осталось — в центре сверху.'),
        variant('B', 'Б · Полоска в центре', 'Строка остаётся одной. В центре «осталось 2k», под ним короткая полоска и «из 57k». Самый компактный вариант.'),
        variant('C', 'В · Как строка бюджета', 'Вместо трёх сумм — строка как «Всего» в окне «Бюджет»: «55k из 57k ₽ · осталось 2k», полоска, под ней прогноз.'),
    ]),
    ('sheet-5', 'Полоска лимита — ещё три варианта', [
        variant('D', 'Г · Толстая полоса с суммой', 'Одна крупная полоса: внутри потрачено «55k ₽», справа лимит «57k». Под ней «осталось 2k» и прогноз. Три суммы убираются.'),
        variant('E', 'Д · Шкала с прогнозом', 'Шкала от 0 до прогноза: заполнено — потрачено (метка «55k»), жирная риска — лимит, пунктир за ней — сколько выйдет сверх лимита по прогнозу.'),
        variant('F', 'Е · Спидометр в центре', 'Значок лимита становится дугой-спидометром: заполнение — потрачено, риска — план к сегодня, внутри «2k» осталось, под ним «из 57k».'),
    ]),
]

CSS = (HERE / 'screens.css').read_text()
html = f'''<!doctype html><html lang="ru"><head><meta charset="utf-8"><title>Бюджет #327</title><style>{CSS}</style></head><body>
{''.join(f'<section class="sheet" id="{sid}"><h1 class="sheet-title">#327 · {title}</h1><div class="cells">{"".join(cells)}</div></section>' for sid, title, cells in sheets)}
</body></html>'''
(HERE / 'screens.html').write_text(html)
print('screens.html', len(html))
