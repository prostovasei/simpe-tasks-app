"""Три направления главного экрана по рефу (А дашборд, Б карточка + лента, В тёмная). Запуск: python3 ref_dashboard.py <папка холста>."""
import os, sys, json
D = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, D)
exec(open(os.path.join(D, 'reflib.py')).read())
OUT = sys.argv[1]

# ---------- A. Дашборд ----------
WARM_TOP = ('radial-gradient(90% 40% at 95% 0%, rgba(243,195,92,.75) 0%, rgba(243,195,92,0) 70%),'
            'radial-gradient(70% 30% at 0% 0%, rgba(110,147,174,.35) 0%, rgba(110,147,174,0) 70%),#F3F1EC')
a = (status('#161616') + header('Сегодня', '#161616', 'rgba(0,0,0,.14)', P_CAL)
     + hero('Сделано сегодня', '3', 'из 7', dots(7, 3))
     + '<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:10px 16px 0;">'
     + tile(P_TRAY, 'Входящие', 'без даты', '12', bars([6, 10, 8, 14, 9, 18, 12], '#161616'))
     + tile(P_WARN, 'Просрочено', '2 дня', '2', wave('#D9534F'), accent='#D9534F')
     + tile(P_SUN, 'Завтра', 'суббота', '5', bars([4, 8, 12, 7, 15], '#161616'))
     + tile(P_LIST, 'Все задачи', 'неделя', '24', wave('#161616'))
     + '</div>'
     + '<div style="margin:10px 16px 0;height:96px;border-radius:30px;background:#FFFFFF;display:flex;align-items:center;gap:12px;padding:0 16px;">'
     + circ(P_CLOCK, 42, 'rgba(0,0,0,.10)', '#161616', isz=19)
     + '<div style="flex:1;"><div style="font-size:13px;color:#77726B;font-weight:500;">Следующая · через 13 мин</div><div style="font-size:18px;font-weight:600;color:#161616;letter-spacing:-.3px;">Стендап команды</div></div>'
     + f'<span style="width:46px;height:46px;border-radius:50%;background:#161616;display:flex;align-items:center;justify-content:center;">{ico(P_ARROW, "#fff", 18, 1.6)}</span></div>'
     + dock())
A = phone(WARM_TOP, a)

# ---------- B. Герой + лента ----------
nxt = ('<div style="color:#fff;font-size:24px;font-weight:600;letter-spacing:-.5px;margin-top:2px;">Стендап команды</div>')
b = (status('#161616') + header('Все задачи', '#161616', 'rgba(0,0,0,.14)')
     + hero('Следующая', '10:00', 'через 13 мин', '', h=232, extra=nxt).replace('Следующая</div>', 'Следующая</div>', 1)
     + '<div style="padding:0 16px;">'
     + day('ПРОСРОЧЕНО', '#D9534F')
     + task_tile('Кафе с Димой', 'Ср, 21 окт · Входящие', '18:00', overdue=True, bell=True)
     + day('СЕГОДНЯ · ПТ, 23 ОКТ', '#8A847C')
     + task_tile('Обед и прогулка', '12:30 – 13:30 · Личное', '12:30')
     + task_tile('Купить хлеб', 'Входящие · каждый день', '')
     + task_tile('Теннис', '19:00 – 21:00 · Спорт', '19:00', bell=True)
     + task_tile('Отправить отчёт', 'Работа', '', done=True)
     + '</div>' + dock())
B = phone(WARM_TOP, b)

# ---------- C. Тёмная ----------
DARK_TOP = ('radial-gradient(90% 38% at 100% 0%, rgba(214,140,60,.55) 0%, rgba(214,140,60,0) 70%),'
            'radial-gradient(70% 30% at 0% 4%, rgba(90,125,150,.35) 0%, rgba(90,125,150,0) 70%),#0F0F0E')
week = ''.join(
    f'<span style="flex:1;height:68px;border-radius:24px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;'
    + ('background:#F2EFEA;' if on else 'background:#1E1D1B;')
    + f'"><span style="font-size:12px;font-weight:500;color:{"#77726B" if on else "rgba(242,239,234,.5)"};">{d}</span><span style="font-size:20px;font-weight:600;color:{"#161616" if on else "#F2EFEA"};">{n}</span></span>'
    for d, n, on in (('Чт', '22', False), ('Пт', '23', True), ('Сб', '24', False), ('Вс', '25', False), ('Пн', '26', False)))
c = (status('#F2EFEA') + header('Сегодня', '#F2EFEA', 'rgba(255,255,255,.18)', P_CAL)
     + hero('Сделано сегодня', '3', 'из 7', dots(7, 3), h=180)
     + f'<div style="display:flex;gap:8px;margin:10px 16px 0;">{week}</div>'
     + '<div style="padding:0 16px;">'
     + day('ДНЁМ', 'rgba(242,239,234,.45)')
     + task_tile('Обед и прогулка', '12:30 – 13:30 · Личное', '12:30', dark=True)
     + task_tile('Купить хлеб', 'Входящие · каждый день', '', dark=True)
     + day('ВЕЧЕРОМ', 'rgba(242,239,234,.45)')
     + task_tile('Теннис', '19:00 – 21:00 · Спорт', '19:00', dark=True, bell=True)
     + '</div>' + dock(dark=True))
C = phone(DARK_TOP, c, True)

V = [
    ('R1', 'А. Дашборд', A, ['Главный экран — сводка дня: большая градиентная карточка «Сделано 3 из 7» с точками прогресса, как «Credit Card Use» на рефе.',
                            'Ниже белые плитки 2×2 (Входящие, Просрочено, Завтра, Все) с тонкими круглыми иконками, крупной цифрой и мини-графиком.',
                            'Внизу — «Следующая задача» с чёрной круглой стрелкой. Тап по плитке открывает список.',
                            'Это меняет структуру: появляется экран-сводка перед списками.']),
    ('R2', 'Б. Герой + лента', B, ['Градиентная карточка показывает ближайшую задачу: крупное время «10:00» и «через 13 мин».',
                                 'Под ней — лента задач крупными белыми плитками (высота 74, круглый чекбокс 30).',
                                 'Структура приложения та же, меняется только внешний вид. Самый простой для внедрения.']),
    ('R3', 'В. Тёмная', C, ['Тот же стиль в тёмной теме: почти чёрный фон, тёплое янтарное свечение сверху.',
                          'Сводка дня + крупные плитки дней недели + задачи по частям дня.',
                          'Нижняя панель становится белой — главный контраст экрана.']),
]
SW = ['#F3C35C', '#6E93AE', '#8A5A37', '#F3F1EC', '#161616']
swatches = ''.join(f'<span style="display:flex;flex-direction:column;gap:4px;align-items:center;"><span style="width:40px;height:40px;border-radius:50%;background:{s};border:1px solid rgba(0,0,0,.08);"></span><span style="font-size:10px;color:#77726B;">{s}</span></span>' for s in SW)

boards = {}
for i, (k, title, ph, notes) in enumerate(V):
    lst = ''.join(f'<li style="margin-bottom:10px;">{n}</li>' for n in notes)
    side = (f'<div style="width:300px;font-family:Manrope,sans-serif;color:#161616;"><div style="font-size:26px;font-weight:600;letter-spacing:-.5px;margin-bottom:14px;">{title}</div>'
            f'<ul style="padding-left:18px;margin:0 0 22px;font-size:14px;line-height:20px;color:#3D3A36;">{lst}</ul>'
            f'<div style="font-size:12px;font-weight:600;letter-spacing:1.2px;color:#77726B;margin-bottom:8px;">ЦВЕТА ИЗ РЕФА</div><div style="display:flex;gap:10px;">{swatches}</div>'
            f'<div style="height:160px;border-radius:30px;margin-top:22px;background:{MESH};position:relative;overflow:hidden;">{grain(.22)}<span style="position:absolute;left:16px;bottom:14px;color:#fff;font-size:13px;font-weight:500;">Градиент карточки</span></div></div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>{title}</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
<div style="width: 800px; height: 940px; box-sizing: border-box; position: relative; overflow: hidden; background: #E9E6E0; display: flex; gap: 40px; padding: 40px;">
{ph}{side}
</div></x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":800,"height":940}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, f'Variant{k}.dc.html'), 'w').write(html)
    boards[f'Variant{k}.dc.html'] = {'x': i * 860, 'y': 0, 'w': 800, 'h': 940, 'title': title}

c = json.load(open(os.path.join(OUT, 'canvas.json')))
c['boards'] = boards
c['order'] = list(boards)
c['notes'] = {'head': {'x': 0, 'y': -300, 'text': 'Стиль по рефу Rondesignlab — 3 направления, главный экран', 'kind': 'title1', 'maxW': 2400}}
c['title'] = 'Реф: Finance App'
json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
