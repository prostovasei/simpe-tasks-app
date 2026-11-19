"""Макет #267. Запуск из корня: python3 design/approved/267/screens.py."""
from calendar import monthrange
from html import escape
from itertools import accumulate
from math import ceil
from pathlib import Path

HERE = Path(__file__).resolve().parent
source = HERE.parent / '259' / 'screens.py'
design = {'__file__': str(source)}
exec(compile(source.read_text().split('\npriority = re.search', 1)[0], str(source), 'exec'), design)
color = design['color']
daily = [900, 1100, 300, 800, 2500, 700, 1200, 0, 1250, 1800, 500, 8400, 400, 1000, 2100, 800, 2000, 1600, 1050]
categories = [('Продукты', '🛒', 12400), ('Транспорт', '🚕', 6400), ('Кафе', '☕', 4200), ('Дом', '🏠', 3500), ('Здоровье', '💊', 1100), ('Связь', '📱', 800)]
places = [('Пятёрочка', 6860), ('Лента', 5270), ('ВкусВилл', 4220), ('Додо Пицца', 3150), ('Аптека', 2100), ('Кофейня', 900)]
total = sum(daily)
unassigned = total - sum(amount for _, amount in places)
assert sum(amount for _, _, amount in categories) == total
assert unassigned == 5900 and places == sorted(places, key=lambda item: -item[1])


def money(amount):
    return f'{amount:,}'.replace(',', '\u00a0') + '\u00a0₽'


def pace(amounts, year, month, month_short):
    last_day = monthrange(year, month)[1]
    as_of = len(amounts)
    assert 1 <= as_of <= last_day
    actual = sum(amounts)
    estimate = actual * last_day / as_of if actual and as_of < last_day else None
    ceiling = max(1000, ceil((estimate if estimate is not None else actual) / 10000) * 10000)

    def x(day):
        return 46 + (day - 1) * 288 / (last_day - 1)

    def y(value):
        return 114 - 94 * value / ceiling

    coords = ' '.join(f'{x(day):.2f},{y(value):.2f}' for day, value in enumerate(accumulate(amounts), start=1))
    axis = [0, ceiling // 2, ceiling]
    grid = ' '.join(f'M46 {y(value):g}H334' for value in axis)
    value_ticks = ''.join(f'<text x="0" y="{y(value)+3:g}">{value/1000:g} тыс.</text>' if value else
                          '<text x="0" y="117">0 ₽</text>' for value in axis)
    day_ticks = {1: ('1 ' + month_short, 'start'), 10: ('10', 'middle'), last_day: (str(last_day) + ' ' + month_short, 'end')}
    if estimate is not None and 4 <= as_of <= last_day - 3:
        day_ticks = {d: tick for d, tick in day_ticks.items() if d in (1, last_day) or abs(d - as_of) >= 4}
        day_ticks[as_of] = (str(as_of), 'middle')
    ticks = ''.join(f'<text x="{x(day):.2f}" y="134" text-anchor="{align}">{label}</text>'
                    for day, (label, align) in sorted(day_ticks.items()))
    forecast = ''
    headline = ''
    description = f'С 1 по {as_of} число потрачено {money(actual)}.'
    if estimate is not None:
        projected = money(round(estimate / 100) * 100)
        description += f' Прогноз на {last_day} число — около {projected}.'
        headline = f'<div class="estimate"><span>К {last_day} {month_short} при текущем темпе</span><b>≈ {projected}</b></div>'
        forecast = f'''<line x1="{x(as_of):.2f}" y1="{y(actual):.2f}" x2="{x(as_of):.2f}" y2="114" class="cutoff"/>
        <line x1="{x(as_of):.2f}" y1="{y(actual):.2f}" x2="{x(last_day):.2f}" y2="{y(estimate):.2f}" class="forecast"/>
        <circle cx="{x(last_day):.2f}" cy="{y(estimate):.2f}" r="3" class="estimate-dot"/>'''
    return f'''<section class="pace" data-days="{last_day}" data-actual-days="{as_of}"><h3>Темп расходов</h3>{headline}
    <svg viewBox="0 0 356 141" role="img" aria-label="{description}">
      <path d="{grid}" class="grid"/>{value_ticks}{forecast}
      <polyline points="{coords}" class="trend"/>
      <circle cx="{x(as_of):.2f}" cy="{y(actual):.2f}" r="3.25" class="actual-dot"/>{ticks}
    </svg></section>'''


def ranking_table(title, items, kind):
    leaders = sorted(items, key=lambda item: -item[2])
    rows = leaders[:4]
    rest = sum(amount for _, _, amount in leaders[4:])
    if rest:
        rows = [*rows, ('Остальное', '…', rest)]
    body = ''.join(
        f'<tr><th scope="row"><span class="row-label"><span class="index" aria-hidden="true">{mark}</span>'
        f'<span class="stat-title">{escape(name)}</span></span></th><td>{money(amount)}</td></tr>'
        for name, mark, amount in rows
    )
    return f'<table class="{kind}"><caption>{title}</caption><tbody>{body}</tbody></table>'


def rankings():
    by_place = [(name, str(rank), amount) for rank, (name, amount) in enumerate(places, start=1)]
    return f'''<section class="rankings" aria-label="Распределение расходов за месяц">
      <div class="tables">
        {ranking_table('Категории', categories, 'categories')}
        {ranking_table('Места', by_place, 'places')}
      </div>
      <div class="unassigned"><span>Расходы без места</span><b>{money(unassigned)}</b></div>
    </section>'''


def without_places():
    return f'''<section class="no-places"><h3>Места</h3>
      <p class="empty">Укажи место в расходе, чтобы увидеть его здесь.</p>
      <div class="unassigned"><span>Расходы без места</span><b>{money(total)}</b></div></section>'''


CSS = design['fonts'] + f":root{{--ink:{color('textPrimary')};--muted:{color('textSecondary')};--divider:{color('divider')};--surface:{color('surface')}}}" + '''
  *{box-sizing:border-box}body{margin:0;background:#eeeef1;color:var(--ink);font:14px/1.4 Manrope,sans-serif}
  main{max-width:960px;margin:auto;padding:24px 16px;display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,320px),1fr));gap:20px;align-items:start}
  .screen{width:100%;max-width:390px;margin:auto;background:var(--surface);padding:16px 18px 14px;border-radius:16px}
  h1,h2,h3,p{margin:0}h1{font-size:16px}h2{font-size:31px;line-height:1.15}h3{font-size:15px;margin-bottom:10px}
  .month{display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid var(--divider);padding-bottom:9px}
  .month span{font-size:23px;color:var(--muted);line-height:1}
  .total{padding-top:18px}.total p,.index,.unassigned,.empty{color:var(--muted)}
  .total p{font-size:12px;margin-bottom:4px}
  section{padding:16px 0;border-bottom:1px solid var(--divider)}section:last-child{border-bottom:none;padding-bottom:0}
  .pace svg{display:block;width:100%;height:auto;fill:var(--ink)}.grid,.trend{fill:none;stroke:var(--divider)}
  .pace svg text{fill:var(--muted);font-size:10px}.trend{stroke:var(--ink);stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}
  .estimate{display:flex;justify-content:space-between;align-items:baseline;gap:8px;margin:0 0 8px;color:var(--muted);font-size:12px}
  .estimate b{color:var(--ink);white-space:nowrap;font-size:15px}
  .forecast{fill:none;stroke:var(--muted);stroke-width:2;stroke-dasharray:4 4;stroke-linecap:round}
  .cutoff{stroke:var(--divider);stroke-dasharray:2 3}.estimate-dot{fill:var(--surface);stroke:var(--muted);stroke-width:1.6}
  .tables{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;align-items:start}
  table{width:100%;border-collapse:collapse;font-size:12px;line-height:1.25}
  caption{text-align:left;font-size:15px;font-weight:700;padding-bottom:10px}
  th,td{height:37px;padding:0;vertical-align:middle}th{text-align:left;font-weight:500;padding-right:4px}
  td{text-align:right;font-weight:700;white-space:nowrap;font-variant-numeric:tabular-nums}
  .row-label{display:grid;grid-template-columns:14px minmax(0,1fr);gap:4px;align-items:center}
  .index{font-size:11px;text-align:center}.stat-title{overflow-wrap:anywhere}
  @media(max-width:360px){main{padding-inline:0}.screen{padding-inline:12px}.tables{gap:12px}table{font-size:11px}}
  .unassigned{display:flex;justify-content:space-between;gap:8px;margin-top:8px;padding-top:5px;font-size:11px}
  .empty{margin-bottom:13px;font-size:13px}.small{max-width:320px;margin-top:0}
'''
document = f'''<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Аналитика месяца · #267</title>
<style>{CSS}</style><main>
<div class="screen" id="overview"><div class="month"><span aria-hidden="true">‹</span><h1>Ноябрь 2026</h1><span aria-hidden="true">›</span></div>
<div class="total"><p>Расходы · 1–19 ноября</p><h2>{money(total)}</h2></div>{pace(daily, 2026, 11, 'ноя')}{rankings()}</div>
<div class="screen small" id="missing">{without_places()}</div>
<div class="screen small" id="completed">{pace([*daily, *([650] * 12)], 2026, 10, 'окт')}</div></main></html>'''
(HERE / 'screens.html').write_text(document)
