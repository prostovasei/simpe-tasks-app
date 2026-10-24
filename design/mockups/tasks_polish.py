"""Макет «Единый вид задач»: список, виджет, уведомление + легенда состояний (решения PM по 20 вопросам).

Размеры в dp 1:1 (телефон 412×860), как в реальном приложении: плашка 40, кружок 14 → 16 (+15%).
Запуск: python3 tasks_polish.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CAL, P_INBOX, P_BELL, P_REP, P_CHECK

A, INK, T2, BG, INACT, OVD, LINE = '#15803D', '#15161F', '#5F6474', '#F6F7F6', '#A8B3AB', '#C62828', '#D5E0D8'
BLUE, SEL = '#3D8BFD', '#E4F2E8'
PRI = {'none': INACT, 'low': '#2E7DD7', 'mid': '#E08A00', 'high': '#C62828'}
CIRC, CIRC_NEW = 14, 16  # было / стало (+15%)
P_GEAR = 'M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6zM19 12l2-1-1-3-2 .5-1.5-1.5L17 5l-3-1-1 2h-2L10 4 7 5l.5 2.5L6 9l-2-.5-1 3 2 1v2l-2 1 1 3 2-.5L7.5 18 7 20l3 1 1-2h2l1 2 3-1-.5-2.5L18 15l2 .5 1-3z'
P_LISTI = 'M8 7h11M8 12h11M8 17h11M4.5 7h.01M4.5 12h.01M4.5 17h.01'
P_SEARCH = 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20 20l-4-4'


def bell(early, c='#6B7280'):
    dot = f'<circle cx="19" cy="5" r="3.6" fill="{A}" stroke="#fff" stroke-width="1.6"/>' if early else ''
    return (f'<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="{c}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'
            f'<path d="{P_BELL}"/>{dot}</svg>')


def proj(color=BLUE):
    return f'<span style="width:18px;height:18px;border-radius:5px;background:{color};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(P_INBOX, "#fff", 12, 2)}</span>'


def mark(kind, ring, done, size, h=None):
    """Кружок или капсула. Выполнено: заливка цветом приоритета + белая галочка."""
    h = size if kind == 'dot' else (h or 52)
    r = size // 2
    if done:
        return (f'<span style="position:relative;z-index:1;width:{size}px;height:{h}px;border-radius:{r}px;background:{ring};display:flex;align-items:center;justify-content:center;flex-shrink:0;">'
                f'{ico(P_CHECK, "#fff", size - 5, 3)}</span>')
    return (f'<span style="position:relative;z-index:1;width:{size}px;height:{h}px;border-radius:{r}px;border:2px solid {ring};background:{BG};box-sizing:border-box;flex-shrink:0;"></span>')


def row(title, t1='', t2='', kind='dot', ring=INACT, done=False, rep=False, rem=None, size=CIRC_NEW, overdue=False, bg=BG):
    h = 56 if kind == 'cap' else 40
    tc = OVD if overdue else INK
    times = (f'<span style="display:flex;flex-direction:column;justify-content:space-between;height:{h - 8}px;">'
             f'<span>{t1}</span><span>{t2}</span></span>') if kind == 'cap' else f'<span>{t1}</span>'
    icons = (ico(P_REP, '#6B7280', 18, 1.8) if rep else '') + (bell(rem == 'early') if rem else '') + proj()
    st = 'text-decoration:line-through;' if done else ''
    op = 'opacity:.7;' if done else ''
    return (f'<div style="display:flex;align-items:center;height:{h}px;margin-top:6px;">'
            f'<span style="width:44px;text-align:right;font-size:12.5px;font-weight:600;color:{OVD if overdue else T2};padding-right:7px;box-sizing:border-box;">{times}</span>'
            f'<span style="width:22px;display:flex;justify-content:center;">{mark(kind, ring, done, size, h - 4)}</span>'
            f'<span style="flex:1;height:{h - 4 if kind == "cap" else 40}px;margin:0 8px 0 7px;border-radius:12px;background:#fff;display:flex;align-items:center;gap:9px;padding:0 11px;box-sizing:border-box;{op}">'
            f'<span style="flex:1;font-size:15px;font-weight:600;color:{INK};{st}white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{title}</span>{icons}</span></div>')


def day(label, chip='', red=False, first=False):
    c = OVD if red else INK
    ch = ''
    if chip:
        ch = (f'<span style="font-size:12px;font-weight:700;color:{c if red else A};background:#fff;border-radius:8px;padding:3px 8px;margin-left:8px;">{chip}</span>')
    return f'<div style="display:flex;align-items:center;padding:{0 if first else 14}px 0 0 12px;font-size:15px;font-weight:700;color:{c};">{label}{ch}</div>'


def timeline(rows_html):
    return f'<div style="position:relative;"><span style="position:absolute;left:58px;top:20px;bottom:20px;width:1.5px;background:{LINE};"></span>{rows_html}</div>'


def phone(bg, inner):
    return (f'<div style="position:relative;width:412px;height:860px;border-radius:44px;overflow:hidden;background:{bg};font-family:Manrope,sans-serif;'
            f'border:8px solid #111;box-sizing:content-box;">{inner}</div>')


def status(c=INK):
    return (f'<div style="height:34px;display:flex;align-items:center;justify-content:space-between;padding:0 24px;font-size:13px;font-weight:700;color:{c};">'
            f'<span>05:52</span><span style="letter-spacing:1px;">●●● ▮</span></div>')


def nav():
    def t(path, on=False):
        c = A if on else '#6B7280'
        return f'<span style="width:44px;height:44px;display:flex;align-items:center;justify-content:center;">{ico(path, c, 22, 2)}</span>'
    return (f'<div style="position:absolute;left:12px;right:12px;bottom:14px;height:62px;border-radius:24px;background:#fff;box-shadow:0 8px 24px rgba(21,22,31,.16);display:flex;align-items:center;padding:0 7px 0 14px;">'
            f'{t(P_INBOX)}{t(P_CAL, True)}{t(P_LISTI)}{t(P_SEARCH)}{t(P_GEAR)}<span style="flex:1"></span>'
            f'<span style="width:50px;height:50px;border-radius:17px;background:{A};display:flex;align-items:center;justify-content:center;">{ico("M12 5v14M5 12h14", "#fff", 24, 2.4)}</span></div>'
            f'<div style="position:absolute;left:0;right:0;bottom:0;height:96px;background:linear-gradient(rgba(246,247,246,0),{BG} 62%);z-index:-1;"></div>')


def tasks_block():
    r = ''
    r += day('ПН, 19 окт', red=True, first=True)
    r += row('Еда', '22:00', ring=OVD, rem='now', overdue=True)
    r += day('СР, 21 окт', red=True)
    r += row('ЛЧ и НБА', '02:00', '04:00', kind='cap', ring=OVD, rem='now', overdue=True)
    r += row('Windsor', ring=OVD, rem='early', overdue=True)
    r += row('Кафе', '18:00', '21:00', kind='cap', ring=OVD, done=True, rem='early', overdue=True)
    r += day('ПТ, 23 окт', 'Вчера', red=True)
    r += row('Rgg', ring=OVD, rem='early', overdue=True)
    r += day('СБ, 24 окт', 'Сегодня')
    r += row('Созвон с командой', '10:00', ring=PRI['low'], done=True, rem='now')
    r += row('Тренировка', '19:00', ring=PRI['mid'], rem='early')
    r += row('Оплатить аренду', ring=PRI['high'], rep=True, rem='early')
    r += day('СР, 04 ноя')
    r += row('Оплатить аренду', ring=INACT, rep=True, rem='early')
    return timeline(r)


def screen_app():
    return phone(BG, status() + f'<div style="padding:8px 0 0 0;">{tasks_block()}</div>' + nav())


def screen_widget():
    wall = 'linear-gradient(165deg,#C5D8F0 0%,#BFD2EE 55%,#9FB8DE 100%)'
    sw = (f'<span style="height:44px;border-radius:16px;background:#fff;display:flex;align-items:center;padding:0 4px;gap:2px;">'
          f'<span style="width:40px;height:36px;border-radius:12px;background:{SEL};display:flex;align-items:center;justify-content:center;">{ico(P_CAL, A, 20, 2)}</span>'
          f'<span style="width:40px;height:36px;display:flex;align-items:center;justify-content:center;">{ico(P_INBOX, "#6B7280", 20, 2)}</span></span>')
    bar = (f'<div style="position:absolute;left:0;right:0;bottom:0;height:70px;background:linear-gradient(rgba(246,247,246,0),{BG} 60%);"></div>'
           f'<div style="position:absolute;left:10px;right:10px;bottom:10px;display:flex;align-items:center;">{sw}<span style="flex:1"></span>'
           f'<span style="width:50px;height:44px;border-radius:15px;background:{A};display:flex;align-items:center;justify-content:center;">{ico("M12 5v14M5 12h14", "#fff", 22, 2.4)}</span></div>')
    wg = (f'<div style="position:absolute;left:10px;right:10px;top:46px;height:598px;border-radius:30px;overflow:hidden;background:{BG};">'
          f'<div style="padding:12px 0 0 0;">{tasks_block()}</div>{bar}</div>')
    apps = ''.join(f'<span style="width:62px;height:62px;border-radius:19px;background:{c};"></span>' for c in ('#F6D90A', '#2AA8E0', '#D6409F', '#3D6FE0'))
    return phone(wall, status('#fff') + wg + f'<div style="position:absolute;left:0;right:0;bottom:52px;display:flex;justify-content:space-around;padding:0 18px;">{apps}</div>')


def notif(title, text, extra=''):
    return (f'<div style="background:#fff;border-radius:22px;padding:14px 16px 8px;margin-top:8px;box-shadow:0 1px 2px rgba(0,0,0,.06);">'
            f'<div style="display:flex;align-items:center;gap:8px;font-size:12px;color:{T2};font-weight:600;">'
            f'<span style="width:18px;height:18px;border-radius:6px;background:{A};display:flex;align-items:center;justify-content:center;">{ico(P_CHECK, "#fff", 12, 3)}</span>Simple Tasks · сейчас</div>'
            f'<div style="font-size:16px;font-weight:700;color:{INK};margin-top:8px;">{title}</div>'
            f'<div style="font-size:14px;color:{T2};margin-top:2px;">{text}</div>{extra}'
            f'<div style="display:flex;justify-content:flex-start;margin-top:8px;"><span style="font-size:14px;font-weight:700;color:{A};padding:8px 12px 10px 0;">Выполнить</span></div></div>')


def screen_notif():
    inner = (status() + '<div style="padding:6px 16px;font-size:40px;font-weight:300;color:#1b1c1f;letter-spacing:-1px;line-height:46px;">05:52</div>'
             + '<div style="padding:0 12px;">' + notif('Кафе', 'Сегодня, 18:00–21:00') + notif('Тренировка', 'Сегодня, 19:00')
             + notif('Оплатить аренду', 'Сегодня') + '</div>')
    return phone('#DCE3EC', inner)


def spec(label, inner):
    return (f'<div style="margin-top:12px;"><div style="font-size:12px;font-weight:700;color:{T2};margin-bottom:2px;">{label}</div>'
            f'<div style="background:{BG};border-radius:16px;padding:2px 0 8px;overflow:hidden;">{inner}</div></div>')


def legend():
    items = [
        ('Колокольчик', 'один значок без цифр: пустой — в момент события, с точкой — заранее. Везде.'),
        ('Повтор', 'иконка только в плашке; слева обычный кружок.'),
        ('Порядок значков', 'справа налево: проект (всегда), колокольчик, повтор.'),
        ('Выполнено', 'кружок и капсула заливаются цветом приоритета, галочка белая; название зачёркнуто, плашка и значки на 70%.'),
        ('Остаётся до 00:00', 'на своём месте, в своём дне; в полночь уходит в «Выполненные».'),
        ('Просрочено в виджете', 'дни, как в приложении; вчера — с плашкой «Вчера».'),
        ('Кружок', '14 → 16 dp (+15%) в виджете и в приложении.'),
        ('Уведомление', 'название, время и кнопка «Выполнить»; без значков.'),
    ]
    li = ''.join(f'<div style="display:flex;gap:10px;margin-top:9px;font-size:13.5px;line-height:19px;color:{INK};"><b style="color:{A};min-width:18px;">{i + 1}</b><span><b>{a}:</b> {b}</span></div>' for i, (a, b) in enumerate(items))
    states = (spec('Обычная / выполнена (приоритет «высокий»)', row('Тренировка', '19:00', ring=PRI['high'], rem='early') + row('Тренировка', '19:00', ring=PRI['high'], done=True, rem='early'))
              + spec('Период: обычная / выполнена', row('Кафе', '18:00', '21:00', kind='cap', ring=PRI['mid'], rem='now') + row('Кафе', '18:00', '21:00', kind='cap', ring=PRI['mid'], done=True, rem='now'))
              + spec('Повтор: иконка только в плашке', row('Оплатить аренду', rep=True, rem='early')))
    return (f'<div style="width:470px;font-family:Manrope,sans-serif;"><div style="font-size:18px;font-weight:700;color:{INK};">Что изменилось</div>{li}'
            f'<div style="font-size:18px;font-weight:700;color:{INK};margin-top:22px;">Состояния</div>{states}</div>')


if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:15px;font-weight:700;color:{INK};margin-bottom:10px;">{s}</div>'
    body = (f'<div style="width:1980px;height:1100px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Единый вид задач: приложение, виджет, уведомление</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;">Все решения по 10 пунктам PM. Сегодня — СБ, 24 окт. Размеры в dp, 1:1.</div>'
            f'<div style="display:flex;gap:30px;"><div>{lab("Список в приложении")}{screen_app()}</div><div>{lab("Виджет")}{screen_widget()}</div>'
            f'<div>{lab("Уведомления")}{screen_notif()}</div>{legend()}</div></div>')
    html = f'''<!doctype html>
    <html lang="ru"><head><meta charset="utf-8"><title>Единый вид задач</title><script src="./support.js"></script></head>
    <body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
    {body}
    </x-dc>
    <script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":1980,"height":1100}}}}'>
    class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
    </script></body></html>'''
    open(os.path.join(OUT, 'VariantT1.dc.html'), 'w').write(html)

    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    old = {k: v for k, v in c['boards'].items() if k != 'VariantT1.dc.html'}
    for i, v in enumerate(old.values()):
        v['y'] = 1220 + i * 1120
    c['boards'] = {'VariantT1.dc.html': {'x': 0, 'y': 0, 'w': 1980, 'h': 1100, 'title': 'Единый вид задач (итерация 27)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Единый вид задач (сверху) · ниже — прежний поиск стиля', 'kind': 'title1', 'maxW': 1400}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
