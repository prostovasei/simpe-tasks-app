"""Макет #267. Запуск из корня: python3 design/approved/267/screens.py."""
from calendar import monthrange
from html import escape
from itertools import accumulate
from math import floor, log10
from pathlib import Path

HERE = Path(__file__).resolve().parent
source = HERE.parent / '259' / 'screens.py'
design = {'__file__': str(source)}
exec(compile(source.read_text().split('\npriority = re.search', 1)[0], str(source), 'exec'), design)
color = design['color']
dim = design['dim']
vector = design['vector']
daily = [900, 1100, 300, 800, 2500, 700, 1200, 0, 1250, 1800, 500, 8400, 400, 1000, 2100, 800, 2000, 1600, 1050]
categories = [('Продукты', '🛒', 12400), ('Транспорт', '🚕', 6400), ('Кафе', '☕', 4200), ('Дом', '🏠', 3500), ('Здоровье', '💊', 1100), ('Связь', '📱', 800)]
places = [('Пятёрочка', 6860), ('Лента', 5270), ('ВкусВилл', 4220), ('Додо Пицца', 3150), ('Аптека', 2100), ('Кофейня', 900)]
total = sum(daily)
unassigned = total - sum(amount for _, amount in places)
assert sum(amount for _, _, amount in categories) == total
assert unassigned == 5900 and places == sorted(places, key=lambda item: -item[1])


def digits(amount):
    return f'{amount:,}'.replace(',', '\u00a0')


def money(amount):
    return digits(amount) + '\u00a0₽'


def scale_ceiling(amount):
    amount = max(1000, amount)
    magnitude = 10 ** floor(log10(amount))
    steps = (1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10)
    return next(int(step * magnitude) for step in steps if step * magnitude >= amount)


def pace(amounts, year, month, month_short):
    last_day = monthrange(year, month)[1]
    as_of = len(amounts)
    assert 1 <= as_of <= last_day
    actual = sum(amounts)
    partial = as_of < last_day
    estimate = actual * last_day / as_of if actual and partial else None
    ceiling = scale_ceiling(estimate if estimate is not None else actual)
    left, right = 4, 352
    top, bottom = 30, 168

    def x(day):
        return left + (day - 1) * (right - left) / (last_day - 1)

    def y(value):
        return bottom - (bottom - top) * value / ceiling

    coords = ' '.join(f'{x(day):.2f},{y(value):.2f}' for day, value in enumerate(accumulate(amounts), start=1))
    axis = [ceiling, ceiling // 2, 0]
    grid = ' '.join(f'M{left} {y(value):g}H{right}' for value in axis)
    value_ticks = ''.join(f'<text class="value-tick" x="{left + 4}" y="{y(value) + offset:g}">{digits(value)}</text>'
                          for value, offset in zip(axis, [12, -5, -5]))
    date_ticks = {1: ('start', 12), last_day: ('end', 12)}
    if partial and as_of != 1:
        # У краёв дата факта переносится ниже, чтобы подписи не пересекались.
        align = 'start' if as_of <= 4 else 'end' if as_of >= last_day - 3 else 'middle'
        date_ticks[as_of] = (align, 12 if align == 'middle' else 24)
    dates = ''.join(
        f'<text class="{"current-date" if partial and day == as_of else "date-tick"}" '
        f'x="{x(day):.2f}" y="{row}" text-anchor="{align}">{day} {month_short}</text>'
        for day, (align, row) in sorted(date_ticks.items())
    )
    cutoff = ''
    if partial:
        cutoff = f'<line x1="{x(as_of):.2f}" y1="{top}" x2="{x(as_of):.2f}" y2="{bottom}" class="cutoff"/>'
    forecast = ''
    headline = ''
    description = f'С 1 по {as_of} число потрачено {money(actual)}.'
    if estimate is not None:
        projected = money(round(estimate / 100) * 100)
        description += f' Прогноз на {last_day} число — около {projected}.'
        headline = f'<div class="estimate"><span>К {last_day} {month_short} при текущем темпе</span><b>≈ {projected}</b></div>'
        forecast = f'''<line x1="{x(as_of):.2f}" y1="{y(actual):.2f}" x2="{x(last_day):.2f}" y2="{y(estimate):.2f}" class="forecast"/>
        <circle cx="{x(last_day):.2f}" cy="{y(estimate):.2f}" r="3" class="estimate-dot"/>'''
    return f'''<section class="pace" data-days="{last_day}" data-actual-days="{as_of}"><h3>Темп расходов</h3>{headline}
    <svg viewBox="0 0 356 176" role="img" aria-label="{description}">
      <path d="{grid}" class="grid"/>{cutoff}{forecast}
      <polyline points="{coords}" class="trend"/>
      <circle cx="{x(as_of):.2f}" cy="{y(actual):.2f}" r="3.25" class="actual-dot"/>{value_ticks}{dates}
    </svg></section>'''


def ranking_table(title, items, kind, without_place=0):
    leaders = sorted(items, key=lambda item: -item[2])
    rows = [(str(rank), *item) for rank, item in enumerate(leaders[:4], start=1)]
    rest = sum(amount for _, _, amount in leaders[4:]) + without_place
    if rest:
        rows.append(('…', 'Остальное', '', rest))
    body = ''.join(
        f'<tr><th scope="row"><span class="row-label"><span class="index" aria-hidden="true">{rank}</span>'
        f'<span class="stat-title"><span class="stat-icon" aria-hidden="true">{escape(icon)}</span>{escape(name)}</span>'
        f'</span></th><td>{digits(amount)}</td></tr>'
        for rank, name, icon, amount in rows
    )
    return f'''<table class="{kind}">
        <thead><tr><th scope="col">{title}</th><th scope="col" class="currency" aria-label="Сумма в рублях">₽</th></tr></thead>
        <tbody>{body}</tbody>
    </table>'''


def rankings(named_places=places):
    by_place = [(name, '', amount) for name, amount in named_places]
    without_place = total - sum(amount for _, amount in named_places)
    return f'''<section class="rankings" aria-label="Распределение расходов за месяц">
      <div class="tables">
        {ranking_table('Категории', categories, 'categories')}
        {ranking_table('Места', by_place, 'places', without_place=without_place)}
      </div>
    </section>'''


def month_content():
    return f'''<div class="total"><p>Расходы · 1–19 ноября</p><h2>{money(total)}</h2></div>
    {pace(daily, 2026, 11, 'ноя')}{rankings()}'''


def finance_home():
    # Компоновка финансов по снимку finance_analytics_light.png, фон как в «Неделе».
    accounts = [('💳', 'Карта', 42300), ('👛', 'Наличные', 6150), ('🏦', 'Накопления', 80000)]
    balances = ''.join(f'<div><p>{icon} {name}</p><b>{money(balance)}</b></div>' for icon, name, balance in accounts)
    plus = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>'
    prev = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m14 7-5 5 5 5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>'
    return f'''<div class="finance" id="finance">
      <div class="finance-body">
        <div class="accounts-heading"><span><b>Счета</b> · {money(sum(balance for _, _, balance in accounts))}</span>
          <button class="account-add" aria-label="Создать счёт">{plus}</button></div>
        <div class="accounts">{balances}</div>
        <div class="finance-month"><button aria-label="Предыдущий месяц">{prev}</button>
          <h1>Ноябрь 2026</h1><button aria-label="Следующий месяц" disabled>{prev}</button></div>
        {month_content()}
      </div>
      <nav class="finance-bar" aria-label="Финансы">
        <button class="add-operation" aria-label="Добавить операцию">{plus}</button>
        <div class="finance-tabs"><button aria-current="page" aria-label="Аналитика">{vector('ic_finance_tab_analytics')}</button>
          <button aria-label="Операции">{vector('ic_finance_tab_operations')}</button></div>
        <button class="finance-module" aria-label="Открыть меню модулей">{vector('ic_module_finance')}</button>
      </nav>
    </div>'''


CSS = design['fonts'] + f''':root{{--ink:{color('textPrimary')};--muted:{color('textSecondary')};--divider:{color('divider')};
  --surface:{color('surface')};--inactive:{color('inactive')};
  --bar-height:{dim('APP_BAR')}px;--bar-button:{dim('APP_BAR_BUTTON')}px;--page-width:{dim('APP_BAR_PAGE_W')}px}}''' + '''
  *{box-sizing:border-box}body{margin:0;background:#eeeef1;color:var(--ink);font:14px/1.4 Manrope,sans-serif}
  main{max-width:960px;margin:auto;padding:24px 16px;display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,320px),1fr));gap:20px;align-items:start}
  .screen{width:100%;max-width:390px;margin:auto;background:var(--surface);padding:16px 18px 14px;border-radius:16px}
  h1,h2,h3,p{margin:0}h1{font-size:16px}h2{font-size:31px;line-height:1.15}h3{font-size:15px;margin-bottom:10px}
  .month{display:flex;align-items:center;justify-content:space-between;padding-bottom:10px}
  .month span{font-size:23px;color:var(--muted);line-height:1}
  .total{padding-top:18px}.total p,.index,.currency{color:var(--muted)}
  .total p{font-size:12px;margin-bottom:4px}
  section{padding:16px 0}section:last-child{padding-bottom:0}
  .pace svg{display:block;width:100%;height:auto;fill:var(--ink)}.grid,.trend{fill:none;stroke:var(--divider)}
  .pace svg text{fill:var(--muted);font-size:10px}.trend{stroke:var(--ink);stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}
  .pace .current-date{fill:var(--ink);font-weight:700}.value-tick{paint-order:stroke;stroke:var(--surface);stroke-width:2.5px}
  .estimate{display:flex;justify-content:space-between;align-items:baseline;gap:8px;margin:0 0 8px;color:var(--muted);font-size:12px}
  .estimate b{color:var(--ink);white-space:nowrap;font-size:15px}
  .forecast{fill:none;stroke:var(--muted);stroke-width:2;stroke-dasharray:4 4;stroke-linecap:round}
  .cutoff{stroke:var(--divider);stroke-dasharray:2 3}.estimate-dot{fill:var(--surface);stroke:var(--muted);stroke-width:1.6}
  .tables{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;align-items:start}
  table{width:100%;border-collapse:collapse;font-size:12px;line-height:1.25}
  thead th{font-size:15px;font-weight:700;height:auto;padding:0 0 10px}.currency{text-align:right}
  tbody th,td{height:37px;padding:0;vertical-align:middle}th{text-align:left}tbody th{font-weight:500;padding-right:4px}
  td{text-align:right;font-weight:700;white-space:nowrap;font-variant-numeric:tabular-nums}
  .row-label{display:grid;grid-template-columns:14px minmax(0,1fr);gap:4px;align-items:center}
  .index{font-size:11px;text-align:center}.stat-title{overflow-wrap:anywhere}
  .stat-icon:not(:empty){margin-inline-end:4px}
  @media(max-width:360px){main{padding-inline:0}.screen{padding-inline:12px}.tables{gap:12px}table{font-size:11px}}
  .small{max-width:320px;margin-top:0}
  .finance{width:100%;max-width:400px;min-height:900px;margin:auto;display:flex;flex-direction:column;background:var(--surface)}
  .finance-body{padding:0 16px 16px}
  .finance button{display:grid;place-items:center;color:var(--ink);background:transparent;border:0;padding:0}
  .finance button svg{width:24px;height:24px}
  .accounts-heading,.finance-month{display:flex;align-items:center;justify-content:space-between}
  .accounts-heading{height:48px;color:var(--muted)}.accounts-heading b{color:var(--ink)}
  .finance .account-add{width:32px;height:32px}.finance .account-add svg{width:18px;height:18px}
  .accounts{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;padding:2px 0 16px}
  .accounts>div{min-width:0}
  .accounts p{color:var(--muted);font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-bottom:4px}
  .accounts b{white-space:nowrap}
  .finance-month{margin:6px 0 18px}
  .finance-month button{width:40px;height:40px}
  .finance-month button:last-child svg{transform:scaleX(-1)}.finance-month button:disabled{color:var(--inactive)}
  .finance .total{padding-top:0}
  .finance-bar{margin-top:auto;min-height:var(--bar-height);border-top:1px solid var(--divider);padding:0 12px;display:flex;align-items:center}
  .finance-bar svg path{fill:none;stroke-linejoin:round}
  .finance-bar .add-operation,.finance-bar .finance-module{width:72px;height:var(--bar-button);border-radius:16px}
  .finance-bar .add-operation{background:var(--ink);color:var(--surface)}.finance-bar .finance-module{border:2px solid var(--ink)}
  .finance-bar .finance-module svg{width:20px;height:20px}
  .finance-tabs{display:flex;flex:1;justify-content:center}
  .finance-tabs button{width:var(--page-width);height:var(--bar-height);color:var(--inactive)}
  .finance-tabs button[aria-current]{color:var(--ink)}
  @media(max-width:360px){.finance-body{padding-inline:12px}}
'''
document = f'''<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Аналитика месяца · #267</title>
<style>{CSS}</style><main>
<div class="screen" id="overview"><div class="month"><span aria-hidden="true">‹</span><h1>Ноябрь 2026</h1><span aria-hidden="true">›</span></div>
{month_content()}</div>
<div class="screen small" id="missing">{rankings(named_places=[])}</div>
<div class="screen small" id="completed">{pace([*daily, *([650] * 12)], 2026, 10, 'окт')}</div>
<div class="screen" id="scale-examples">{''.join(pace([amount * factor for amount in daily], 2026, 11, 'ноя') for factor in (2, 3))}</div>
{finance_home()}</main></html>'''
(HERE / 'screens.html').write_text(document)
