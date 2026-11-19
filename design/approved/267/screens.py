"""Макет #267. Запуск из корня: python3 design/approved/267/screens.py."""
from html import escape
from itertools import accumulate
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


def pace():
    x = lambda day: 46 + (day - 1) * 16.1
    y = lambda value: 106 - 89 * value / 30000
    coords = ' '.join(f'{x(day):.2f},{y(value):.2f}' for day, value in enumerate(accumulate(daily), start=1))
    ticks = ''.join(f'<text x="{x(day):.2f}" y="127" text-anchor="{align}">{label}</text>'
                    for day, label, align in [(1, '1 ноя', 'start'), (7, '7', 'middle'), (13, '13', 'middle'), (19, '19 ноя', 'end')])
    return f'''<section class="pace"><h3>Темп расходов</h3>
    <svg viewBox="0 0 356 132" role="img" aria-label="Расходы с 1 по 19 ноября выросли до 28 400 рублей">
      <path d="M46 17H336 M46 61.5H336 M46 106H336" class="grid"/>
      <text x="0" y="20">30 тыс.</text><text x="0" y="64.5">15 тыс.</text><text x="0" y="109">0 ₽</text>
      <polyline points="{coords}" class="trend"/>
      <circle cx="{x(19):.2f}" cy="{y(total):.2f}" r="3.25"/>{ticks}
    </svg></section>'''


def shares():
    items = []
    for title, icon, amount in categories:
        fraction = amount / total
        label = f'{100*fraction:.1f}'.replace('.', ',') + '%'
        items.append(f'<div class="share" style="flex:{amount}" aria-label="{escape(title)}, {label}"><span class="icon">{icon}</span><span class="segment"></span><b>{label}</b></div>')
    return '<section><h3>По категориям</h3><div class="shares">' + ''.join(items) + '</div></section>'


def place_row(position, name, amount):
    return f'''<li class="place">
      <div class="place-text"><span class="index">{position}</span><span class="store">{escape(name)}</span><b>{money(amount)}</b></div>
      <div class="bar" aria-hidden="true"><span style="width:{100*amount/places[0][1]:.3f}%"></span></div></li>'''


def ranking(has_places=True):
    if has_places:
        top = ''.join(place_row(i, *p) for i, p in enumerate(places[:5], start=1))
        extra = ''.join(place_row(i, *p) for i, p in enumerate(places[5:], start=6))
        content = f'<ol class="ranking">{top}</ol><details><summary>Все места</summary><ol class="ranking" start="6">{extra}</ol></details>'
    else:
        content = '<p class="empty">Укажи место в расходе, чтобы увидеть его здесь.</p>'
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
  .shares{display:flex}.share{display:flex;flex-direction:column;align-items:center;min-width:0;gap:5px}
  .share b{font-size:clamp(8px,2.45vw,10px)}.icon{font-size:16px}
  .segment{display:block;width:calc(100% - 3px);height:4px;background:var(--ink);border-radius:2px}
  .ranking{padding:0;margin:0;list-style:none}.place{padding:7px 0 6px}
  .place-text{display:grid;grid-template-columns:17px minmax(0,1fr) auto;gap:7px;align-items:baseline}
  .place-text b,.unassigned b{font-variant-numeric:tabular-nums;white-space:nowrap}.store{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
  .index{font-size:12px}.bar{background:var(--divider);height:3px;margin:5px 0 0 24px;border-radius:2px;overflow:hidden}
  .bar span{height:100%;display:block;background:var(--ink)}
  details{margin-top:5px}summary{padding:6px 0;cursor:pointer;font-weight:700;font-size:12px}
  .unassigned{display:flex;justify-content:space-between;gap:8px;margin-top:6px;padding-top:9px;border-top:1px solid var(--divider);font-size:12px}
  .empty{margin-bottom:13px;font-size:13px}.small{max-width:320px;margin-top:0}
'''
document = f'''<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Аналитика месяца · #267</title>
<style>{CSS}</style><main>
<div class="screen" id="overview"><div class="month"><span aria-hidden="true">‹</span><h1>Ноябрь 2026</h1><span aria-hidden="true">›</span></div>
<div class="total"><p>Расходы · 1–19 ноября</p><h2>{money(total)}</h2></div>{pace()}{shares()}{ranking()}</div>
<div class="screen small" id="missing">{ranking(False)}</div></main></html>'''
(HERE / 'screens.html').write_text(document)
