"""Макет «Просрочка и высокий приоритет»: 3 варианта, как развести красный цвет (просрочка vs P1).

Для каждого варианта — список в приложении и виджет на одних и тех же задачах. Сегодня — СБ, 24 окт.
Запуск: python3 prio_overdue.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_REP, P_CHECK
from tasks_polish import A, INK, T2, BG, INACT, OVD, LINE, PRI, CIRC_NEW, bell, proj, mark, day, timeline, phone, status, nav, SEL, P_CAL, P_INBOX

PURPLE = '#8E4EC6'

# Варианты: как красить кружок и есть ли метка просрочки на плашке.
VARIANTS = [
    ('P1', 'А. Кружок = приоритет',
     'Кружок всегда цвета приоритета, в том числе у просроченной. Просрочку показывают красная подпись дня и красное время. Красный кружок — только P1.',
     dict(high=PRI['high'], overdue_ring=False, stripe=False)),
    ('P2', 'Б. P1 — фиолетовый',
     'Красный остаётся только у просрочки (кружок, подпись дня, время). Высокий приоритет — фиолетовый; P2 жёлтый и P3 синий без изменений.',
     dict(high=PURPLE, overdue_ring=True, stripe=False)),
    ('P3', 'В. Приоритет + красная полоска',
     'Кружок цвета приоритета, а у просроченной плашки — тонкая красная полоска слева. Подпись дня и время тоже красные.',
     dict(high=PRI['high'], overdue_ring=False, stripe=True)),
]


def row(title, prio, v, t1='', done=False, rep=False, rem=None, overdue=False):
    ring = {'none': INACT, 'low': PRI['low'], 'mid': PRI['mid'], 'high': v['high']}[prio]
    if overdue and v['overdue_ring']:
        ring = OVD
    icons = (ico(P_REP, '#6B7280', 18, 1.8) if rep else '') + (bell(rem == 'early') if rem else '') + proj()
    st = 'text-decoration:line-through;' if done else ''
    op = 'opacity:.7;' if done else ''
    stripe = f'box-shadow:inset 3px 0 0 {OVD};' if overdue and v['stripe'] else ''
    return (f'<div style="display:flex;align-items:center;height:40px;margin-top:6px;">'
            f'<span style="width:44px;text-align:right;font-size:12.5px;font-weight:600;color:{OVD if overdue else T2};padding-right:7px;box-sizing:border-box;">{t1}</span>'
            f'<span style="width:22px;display:flex;justify-content:center;">{mark("dot", ring, done, CIRC_NEW)}</span>'
            f'<span style="flex:1;height:40px;margin:0 8px 0 7px;border-radius:12px;background:#fff;display:flex;align-items:center;gap:9px;padding:0 11px;box-sizing:border-box;{stripe}{op}">'
            f'<span style="flex:1;font-size:15px;font-weight:600;color:{INK};{st}white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{title}</span>{icons}</span></div>')


def tasks(v):
    r = day('ЧТ, 22 окт', red=True, first=True)
    r += row('Сдать отчёт', 'high', v, '10:00', rem='early', overdue=True)
    r += row('Купить хлеб', 'low', v, overdue=True, rem='now')
    r += day('ПТ, 23 окт', 'Вчера', red=True)
    r += row('Позвонить в банк', 'none', v, '15:00', rem='now', overdue=True)
    r += row('Оплатить свет', 'mid', v, done=True, overdue=True)
    r += row('Записаться к врачу', 'high', v, overdue=True, rep=True, rem='early')
    r += day('СБ, 24 окт', 'Сегодня')
    r += row('Созвон с командой', 'high', v, '12:00', rem='early')
    r += row('Тренировка', 'mid', v, '19:00', rem='now')
    r += row('Разобрать почту', 'none', v)
    r += day('ВС, 25 окт')
    r += row('Подготовить слайды', 'high', v, done=False, rem='early')
    r += row('Полить цветы', 'low', v, rep=True)
    return timeline(r)


def screen_app(v):
    return phone(BG, status() + f'<div style="padding-top:8px;">{tasks(v)}</div>' + nav())


def screen_widget(v):
    wall = 'linear-gradient(165deg,#C5D8F0 0%,#BFD2EE 55%,#9FB8DE 100%)'
    sw = (f'<span style="height:44px;border-radius:16px;background:#fff;display:flex;align-items:center;padding:0 4px;gap:2px;">'
          f'<span style="width:40px;height:36px;border-radius:12px;background:{SEL};display:flex;align-items:center;justify-content:center;">{ico(P_CAL, A, 20, 2)}</span>'
          f'<span style="width:40px;height:36px;display:flex;align-items:center;justify-content:center;">{ico(P_INBOX, "#6B7280", 20, 2)}</span></span>')
    bar = (f'<div style="position:absolute;left:0;right:0;bottom:0;height:70px;background:linear-gradient(rgba(246,247,246,0),{BG} 60%);"></div>'
           f'<div style="position:absolute;left:10px;right:10px;bottom:10px;display:flex;align-items:center;">{sw}<span style="flex:1"></span>'
           f'<span style="width:50px;height:44px;border-radius:15px;background:{A};display:flex;align-items:center;justify-content:center;">{ico("M12 5v14M5 12h14", "#fff", 22, 2.4)}</span></div>')
    wg = (f'<div style="position:absolute;left:10px;right:10px;top:46px;height:640px;border-radius:30px;overflow:hidden;background:{BG};">'
          f'<div style="padding-top:12px;">{tasks(v)}</div>{bar}</div>')
    apps = ''.join(f'<span style="width:62px;height:62px;border-radius:19px;background:{c};"></span>' for c in ('#F6D90A', '#2AA8E0', '#D6409F', '#3D6FE0'))
    return phone(wall, status('#fff') + wg + f'<div style="position:absolute;left:0;right:0;bottom:52px;display:flex;justify-content:space-around;padding:0 18px;">{apps}</div>')


if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:15px;font-weight:700;color:{INK};margin-bottom:10px;">{s}</div>'
    W, H = 1040, 1060
    boards = {}
    for i, (key, title, desc, v) in enumerate(VARIANTS):
        body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
                f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">{title}</div>'
                f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;max-width:940px;line-height:21px;">{desc}</div>'
                f'<div style="display:flex;gap:40px;"><div>{lab("Список в приложении")}{screen_app(v)}</div><div>{lab("Виджет")}{screen_widget(v)}</div></div></div>')
        html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>{title}</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
        open(os.path.join(OUT, f'Variant{key}.dc.html'), 'w').write(html)
        boards[f'Variant{key}.dc.html'] = {'x': i * (W + 80), 'y': 0, 'w': W, 'h': H, 'title': title}

    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    old = {k: v for k, v in c['boards'].items() if k not in boards}
    for j, v in enumerate(old.values()):
        v['x'] = 0
        v['y'] = H + 120 + sum(b['h'] + 120 for b in list(old.values())[:j])
    c['boards'] = {**boards, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Просрочка и высокий приоритет (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 3400}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
