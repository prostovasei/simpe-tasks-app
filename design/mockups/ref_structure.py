"""Наша структура + визуал рефа: список, создание задачи, виджет (А чёрные акценты, Б градиентные). Запуск: python3 ref_structure.py <папка холста>."""
import os, sys, json
D = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, D)
exec(open(os.path.join(D, 'reflib.py')).read())
OUT = sys.argv[1]

INK, INK2, RED = '#161616', '#8A847C', '#D9534F'
BG = '#F3F1EC'
GLOW = ('radial-gradient(90% 36% at 96% 0%, rgba(243,195,92,.72) 0%, rgba(243,195,92,0) 70%),'
        'radial-gradient(70% 28% at 0% 0%, rgba(110,147,174,.32) 0%, rgba(110,147,174,0) 70%),' + BG)
MINI_MESH = ('radial-gradient(120% 120% at 100% 0%, #F3C35C 0%, rgba(243,195,92,0) 60%),'
             'radial-gradient(110% 110% at 0% 40%, #6E93AE 0%, rgba(110,147,174,0) 65%),'
             'radial-gradient(120% 90% at 50% 120%, #8A5A37 0%, rgba(138,90,55,0) 65%),#9A9C96')

TASKS = [
    ('day', 'Просрочено', RED, None),
    ('row', ('21 окт', '18:00'), 'Кафе с Димой', 'over', 'bell', '#3D8BFD'),
    ('day', 'Пятница, 23 окт', None, 'Сегодня'),
    ('now', None),
    ('row', ('10:00', ''), 'Стендап команды', None, 'bell', '#8B5CF6'),
    ('row', ('12:30', '13:30'), 'Обед и прогулка', None, None, '#3D8BFD'),
    ('row', ('', ''), 'Купить хлеб', None, 'rep', '#3D8BFD'),
    ('row', ('19:00', '21:00'), 'Теннис', 'prio', 'bell', '#E09400'),
    ('day', 'Суббота, 24 окт', None, 'Завтра'),
    ('row', ('09:00', ''), 'Бавария — Дортмунд', None, 'bell', '#E11D48'),
    ('row', ('', ''), 'Позвонить маме', None, None, '#3D8BFD'),
]


def chip(text, S, on):
    if on:
        return f'<span style="font-size:12px;font-weight:600;color:#fff;background:{S["sel"]};border-radius:14px;padding:4px 11px;">{text}</span>'
    return f'<span style="font-size:12px;font-weight:600;color:{INK};border:1px solid rgba(0,0,0,.14);border-radius:14px;padding:3px 10px;">{text}</span>'


def timeline(S, compact=False):
    h, tw, rs, fs = (52, 44, 18, 15) if compact else (64, 50, 22, 17)
    out, first = '', True
    for it in TASKS:
        if it[0] == 'day':
            _, title, c, tag = it
            out += (f'<div style="display:flex;align-items:center;gap:8px;padding:{8 if first else 18}px 0 6px 6px;">'
                    f'<span style="font-size:{14 if compact else 15}px;font-weight:700;color:{c or INK};letter-spacing:-.2px;">{title}</span>{chip(tag, S, tag == "Сегодня") if tag else ""}</div>')
            first = False
        elif it[0] == 'now':
            out += (f'<div style="display:flex;align-items:center;height:14px;"><span style="width:{tw}px;text-align:right;font-size:11px;font-weight:600;color:{S["now"]};">11:47</span>'
                    f'<span style="width:30px;display:flex;justify-content:center;"><span style="width:8px;height:8px;border-radius:50%;background:{S["now"]};"></span></span><span style="flex:1;height:1px;background:{S["now"]};"></span></div>')
        else:
            _, (t1, t2), title, kind, meta, lc = it
            tc = RED if kind == 'over' else INK2
            rc = RED if kind == 'over' else ('#E09400' if kind == 'prio' else 'rgba(0,0,0,.22)')
            times = ''.join(f'<span style="font-size:{12 if compact else 13}px;font-weight:500;color:{tc};line-height:{15 if compact else 17}px;">{x}</span>' for x in (t1, t2) if x)
            m = ico(P_BELL, INK2, 19 if compact else 21, 1.5) if meta == 'bell' else (ico(P_REP, INK2, 18 if compact else 20, 1.5) if meta == 'rep' else '')
            sq = f'<span style="width:{20 if compact else 24}px;height:{20 if compact else 24}px;border-radius:50%;border:1px solid rgba(0,0,0,.10);display:flex;align-items:center;justify-content:center;flex-shrink:0;"><span style="width:8px;height:8px;border-radius:50%;background:{lc};"></span></span>'
            out += (f'<div style="display:flex;height:{h}px;margin:5px 0;">'
                    f'<div style="width:{tw}px;flex-shrink:0;display:flex;flex-direction:column;align-items:flex-end;justify-content:center;">{times}</div>'
                    f'<div style="width:30px;flex-shrink:0;display:flex;align-items:center;justify-content:center;position:relative;"><span style="position:absolute;top:-5px;bottom:-5px;width:1px;background:rgba(0,0,0,.10);"></span>'
                    f'<span style="width:{rs}px;height:{rs}px;border-radius:50%;border:1.5px solid {rc};background:{BG};box-sizing:border-box;position:relative;"></span></div>'
                    f'<div style="flex:1;min-width:0;display:flex;align-items:center;gap:10px;padding:0 {12 if compact else 14}px 0 {14 if compact else 18}px;background:#fff;border-radius:{18 if compact else 22}px;">'
                    f'<span style="flex:1;font-size:{fs}px;font-weight:700;color:{INK};letter-spacing:-.3px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{title}</span>{m}{sq}</div></div>')
    return out


def list_inner(S):
    return (status(INK)
            + f'<div style="display:flex;align-items:flex-end;justify-content:space-between;padding:10px 22px 4px;"><div><div style="font-size:34px;font-weight:600;color:{INK};letter-spacing:-1px;line-height:38px;">Все</div>'
              f'<div style="font-size:14px;color:{INK2};margin-top:2px;">Пятница, 23 октября</div></div>{circ(P_SEARCH, 46, "rgba(0,0,0,.14)", INK)}</div>'
            + f'<div style="padding:0 16px 0 2px;">{timeline(S)}</div>' + dock())


def screen_list(S):
    return phone(GLOW, list_inner(S))


def screen_create(S):
    days = [('Сег', '23', True), ('Сб', '24', False), ('Вс', '25', False), ('Пн', '26', False), ('Вт', '27', False)]
    tiles = ''.join(
        f'<span style="width:48px;height:62px;border-radius:22px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;box-sizing:border-box;'
        + (f'background:{S["sel"]};' if on else 'background:#fff;border:1px solid rgba(0,0,0,.12);')
        + f'"><span style="font-size:12px;font-weight:500;color:{"rgba(255,255,255,.8)" if on else INK2};">{d}</span><span style="font-size:19px;font-weight:600;color:{"#fff" if on else INK};">{n}</span></span>'
        for d, n, on in days)
    cal = f'<span style="width:48px;height:62px;border-radius:22px;border:1px solid rgba(0,0,0,.12);display:flex;align-items:center;justify-content:center;box-sizing:border-box;">{ico(P_CAL, INK, 22, 1.5)}</span>'

    def tb(path, state='off'):
        if state == 'on':
            return f'<span style="width:46px;height:46px;border-radius:50%;background:{S["sel"]};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(path, "#fff", 22, 1.6)}</span>'
        op = 'opacity:.35;' if state == 'dim' else ''
        return f'<span style="width:46px;height:46px;border-radius:50%;border:1px solid rgba(0,0,0,.14);display:flex;align-items:center;justify-content:center;flex-shrink:0;box-sizing:border-box;{op}">{ico(path, INK, 22, 1.5)}</span>'
    send = f'<span style="width:56px;height:56px;border-radius:50%;background:{INK};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico("M5 12h14M13 6l6 6-6 6", "#fff", 22, 1.7)}</span>'
    card = (f'<div style="position:absolute;left:10px;right:10px;bottom:292px;padding:20px 18px 16px;border-radius:34px;background:#fff;box-shadow:0 18px 40px rgba(0,0,0,.18);">'
            f'<div style="font-size:23px;font-weight:600;color:{INK};letter-spacing:-.5px;">Позвонить маме<span style="display:inline-block;width:2px;height:24px;background:{INK};vertical-align:-4px;margin-left:2px;"></span></div>'
            f'<div style="font-size:15px;color:{INK2};margin:6px 0 16px;">Описание</div>'
            f'<div style="display:flex;justify-content:space-between;margin-bottom:14px;">{tiles}{cal}</div>'
            f'<div style="display:flex;align-items:center;gap:6px;">{tb(P_CLOCK)}{tb(P_INBOX, "on")}{tb("M5 21V4h11l-2 4 2 4H5")}{tb(P_REP, "dim")}{tb(P_BELL, "dim")}<span style="flex:1"></span>{send}</div></div>')
    rows = [[(c, 30) for c in 'йцукенгшщзх'], [(c, 30) for c in 'фывапролдж'], [('⇧', 44)] + [(c, 30) for c in 'ячсмитьб'] + [('⌫', 44)], [('?123', 58), (',', 34), ('пробел', 168), ('.', 34), ('↵', 58)]]
    kb = (f'<div style="position:absolute;left:0;right:0;bottom:0;height:284px;background:#E4E1DB;padding-top:8px;">'
          + ''.join('<div style="display:flex;gap:5px;justify-content:center;margin-top:10px;">' + ''.join(
              f'<span style="width:{w}px;height:46px;border-radius:9px;background:#fff;display:flex;align-items:center;justify-content:center;font-size:17px;color:{INK};">{ch}</span>' for ch, w in r) + '</div>' for r in rows)
          + '</div>')
    scrim = '<div style="position:absolute;inset:0;background:rgba(22,20,16,.38);"></div>'
    return phone(GLOW, list_inner(S) + scrim + card + kb)


def screen_widget(S):
    sw_sel = f'<span style="width:48px;height:48px;border-radius:50%;background:{S["wsel"]};display:flex;align-items:center;justify-content:center;">{ico(P_CAL, S["wselC"], 23, 1.6)}</span>'
    sw_off = f'<span style="width:48px;height:48px;border-radius:50%;display:flex;align-items:center;justify-content:center;">{ico(P_TRAY, "rgba(255,255,255,.6)", 23, 1.6)}</span>'
    bar = (f'<div style="position:absolute;left:0;right:0;bottom:0;height:140px;background:linear-gradient(rgba(243,241,236,0),rgba(243,241,236,.9) 55%,{BG});"></div>'
           f'<div style="position:absolute;left:10px;right:10px;bottom:10px;height:60px;display:flex;align-items:center;">'
           f'<span style="height:60px;border-radius:30px;background:{INK};display:flex;align-items:center;gap:4px;padding:0 6px;">{sw_sel}{sw_off}</span><span style="flex:1"></span>'
           f'<span style="width:60px;height:60px;border-radius:50%;background:{INK};display:flex;align-items:center;justify-content:center;">{ico("M12 5v14M5 12h14", "#fff", 24, 1.8)}</span></div>')
    wg = (f'<div style="position:absolute;left:14px;right:14px;top:132px;height:560px;border-radius:34px;overflow:hidden;background:{GLOW};">'
          f'<div style="padding:6px 12px 0 8px;">{timeline(S, True)}</div>{bar}</div>')
    apps = ''.join(f'<span style="width:58px;height:58px;border-radius:50%;background:{c};"></span>' for c in ('rgba(255,255,255,.85)', '#5E8AA6', '#C98A3F', '#2B2B2B'))
    wall = ('radial-gradient(80% 40% at 100% 0%, rgba(243,195,92,.55) 0%, rgba(243,195,92,0) 70%),'
            'linear-gradient(165deg,#B8B4AC 0%,#9A9E8C 50%,#5E6A56 100%)')
    inner = (status('#fff') + '<div style="color:#fff;font-size:56px;font-weight:400;letter-spacing:-2px;padding:2px 22px 0;line-height:64px;">11:47</div>'
             + wg + f'<div style="position:absolute;left:0;right:0;bottom:40px;display:flex;justify-content:space-around;padding:0 20px;">{apps}</div>')
    return phone(wall, inner)


V = [
    ('S1', 'А. Чёрные акценты', dict(sel=INK, now=INK, wsel='#fff', wselC=INK),
     'Структура как сейчас, визуал из рефа. Градиент — только мягкое свечение сверху экрана и виджета. Всё выбранное (Сегодня, день, кнопка списка, вкладка в виджете) — чёрное, как кнопки на рефе.'),
    ('S2', 'Б. Градиентные акценты', dict(sel=MINI_MESH, now='#C98A3F', wsel=MINI_MESH, wselC='#fff'),
     'То же самое, но выбранные элементы залиты градиентом из рефа (голубой → янтарь → коричневый): «Сегодня», выбранный день, активная кнопка, вкладка в виджете. Линия «сейчас» — янтарная.'),
]
boards = {}
for i, (k, title, S, desc) in enumerate(V):
    lab = lambda s: f'<div style="font-size:15px;font-weight:600;color:{INK};margin-bottom:10px;">{s}</div>'
    body = (f'<div style="width:1360px;height:1040px;box-sizing:border-box;background:#E9E6E0;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:600;color:{INK};letter-spacing:-.5px;">{title}</div><div style="font-size:15px;color:#3D3A36;margin:6px 0 22px;max-width:1100px;line-height:21px;">{desc}</div>'
            f'<div style="display:flex;gap:30px;"><div>{lab("Список задач")}{screen_list(S)}</div><div>{lab("Создание задачи")}{screen_create(S)}</div><div>{lab("Виджет")}{screen_widget(S)}</div></div></div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>{title}</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":1360,"height":1040}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, f'Variant{k}.dc.html'), 'w').write(html)
    boards[f'Variant{k}.dc.html'] = {'x': 0, 'y': i * 1120, 'w': 1360, 'h': 1040, 'title': title}

c = json.load(open(os.path.join(OUT, 'canvas.json')))
c['boards'] = boards
c['order'] = list(boards)
c['notes'] = {'head': {'x': 0, 'y': -300, 'text': 'Наша структура + визуал из рефа: список, создание задачи, виджет', 'kind': 'title1', 'maxW': 1400}}
c['title'] = 'Структура + реф'
json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
