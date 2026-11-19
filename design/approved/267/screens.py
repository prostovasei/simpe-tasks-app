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
categories = [('Продукты', '🛒', 12400), ('Транспорт', '🚕', 6400), ('Кафе', '☕', 4200), ('Дом', '🏠', 3500), ('Прочее', '…', 1900)]
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


def share_line(items, label):
    assert sum(amount for _, _, amount in items) == total
    segments = []
    for title, mark, amount in items:
        pct = f'{100*amount/total:.1f}'.replace('.', ',') + '%'
        cls = ' bin-unassigned' if title == 'Без места' else ''
        segments.append(f'<div class="share{cls}" style="flex:{amount}" aria-label="{escape(title)}, {pct}"><span class="mark">{mark}</span><span class="segment"></span><b>{pct}</b></div>')
    return f'<div class="shares" aria-label="{label}">' + ''.join(segments) + '</div>'


def stat_row(mark, title, amount):
    return f'<li class="stat"><span class="index">{mark}</span><span class="stat-title">{escape(title)}</span><b>{money(amount)}</b></li>'


def shares():
    rows = ''.join(stat_row(mark, title, amount) for title, mark, amount in categories)
    return f'''<section class="categories"><h3>По категориям</h3>{share_line(categories, 'Доли расходов по категориям')}
      <details><summary>Все категории</summary><ul class="stats">{rows}</ul></details></section>'''


def ranking(has_places=True):
    if has_places:
        first = [(name, str(i), amount) for i, (name, amount) in enumerate(places[:4], start=1)]
        other = sum(amount for _, amount in places[4:])
        line = share_line([*first, ('Другие места', '…', other), ('Без места', '—', unassigned)], 'Доли расходов по местам')
        top = ''.join(stat_row(mark, name, amount) for name, mark, amount in first)
        extra = ''.join(stat_row(i, *p) for i, p in enumerate(places[4:], start=5))
        content = f'{line}<ol class="stats ranking">{top}</ol><details><summary><span>Другие места</span><b>{money(other)}</b></summary><ol class="stats ranking" start="5">{extra}</ol></details>'
    else:
        line = share_line([('Без места', '—', total)], 'Доли расходов по местам')
        content = line + '<p class="empty">Укажи место в расходе, чтобы увидеть его здесь.</p>'
    return f'''<section class="places"><h3>Где тратили</h3>{content}
      <div class="unassigned"><span>Без места</span><b>{money(unassigned if has_places else total)}</b></div></section>'''


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
  .shares{display:flex;margin-bottom:10px}.share{display:flex;flex-direction:column;align-items:center;min-width:0;gap:5px}
  .share b{font-size:clamp(8px,2.45vw,10px);white-space:nowrap}.mark{font-size:14px;font-weight:700;min-height:22px}
  .segment{display:block;width:calc(100% - 3px);height:4px;background:var(--ink);border-radius:2px}
  .bin-unassigned .segment{background:var(--muted)}
  .stats{padding:0;margin:0;list-style:none}.stat{padding:6px 0;display:grid;grid-template-columns:17px minmax(0,1fr) auto;gap:7px;align-items:baseline}
  .stat b,.unassigned b{font-variant-numeric:tabular-nums;white-space:nowrap}.stat-title{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
  .index{font-size:12px}details{margin-top:5px}summary{padding:6px 0;cursor:pointer;font-weight:700;font-size:12px}
  summary b{float:right}
  .unassigned{display:flex;justify-content:space-between;gap:8px;margin-top:6px;padding-top:9px;border-top:1px solid var(--divider);font-size:12px}
  .empty{margin-bottom:13px;font-size:13px}.small{max-width:320px;margin-top:0}
'''
document = f'''<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Аналитика месяца · #267</title>
<style>{CSS}</style><main>
<div class="screen" id="overview"><div class="month"><span aria-hidden="true">‹</span><h1>Ноябрь 2026</h1><span aria-hidden="true">›</span></div>
<div class="total"><p>Расходы · 1–19 ноября</p><h2>{money(total)}</h2></div>{pace(daily, 2026, 11, 'ноя')}{shares()}{ranking()}</div>
<div class="screen small" id="missing">{ranking(False)}</div>
<div class="screen small" id="completed">{pace([*daily, *([650] * 12)], 2026, 10, 'окт')}</div></main></html>'''
(HERE / 'screens.html').write_text(document)
