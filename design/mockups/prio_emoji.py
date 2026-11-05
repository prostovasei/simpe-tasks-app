"""Макет «Два приоритета и эмодзи списков» (итерация 47, доска VariantH2).

Решения PM: высокий приоритет — фиолетовый (вариант В доски VariantH1); показать задачу с повтором и длительностью; окна приоритета нет —
кнопка в окне задачи включает и выключает высокий сразу; вместо значков и цветов списков — эмодзи (как в TickTick): сетка популярных
и любой эмодзи с клавиатуры; у старых списков эмодзи по значку; TickTick — эмодзи в начале названия списка становится его эмодзи.
Запуск: python3 prio_emoji.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_CHECK, P_REP, P_BELL, P_FLAG, P_LIST, P_CAL, P_CLOCK, P_X
from tasks_polish import phone, status
import prio_two as pt
from week_black import WALL

INK, T2, BG, SURF, LINE, INACT, OVD = pt.INK, pt.T2, pt.BG, pt.SURF, pt.LINE, pt.INACT, pt.OVD
V = pt.VARIANTS[2]  # фиолетовый
_, RING, TINT, FADED = V
NONE = pt.NONE_RING
EMO = 'font-family:"Noto Color Emoji",sans-serif;'


def emoji(e, size=15):
    return f'<span style="{EMO}font-size:{size}px;line-height:1;flex-shrink:0;">{e}</span>'


def tall_repeat(color, h, w=16):
    """Знак повтора высотой с капсулу периода: две половины капсулы со стрелками (левая — вверх и вправо, правая — вниз и влево)."""
    # Почти замкнутая капсула: левая половина — снизу вверх и вправо (стрелка вправо сверху), правая — сверху вниз и влево
    # (стрелка влево снизу); разрывы короткие — у стрелок.
    sw = 2
    a = (f'M2 {h - 7}V9a6 6 0 0 1 6 -6H10.5'
         f'M8.5 0.8l2.4 2.2-2.4 2.2'
         f'M14 7V{h - 9}a6 6 0 0 1 -6 6H5.5'
         f'M7.5 {h - 5.2}l-2.4 2.2 2.4 2.2')
    return (f'<span style="width:{w}px;height:{h}px;border-radius:{w // 2}px;background:{BG};display:flex;flex-shrink:0;">'
            f'<svg width="{w}" height="{h}" viewBox="0 0 {w} {h}" fill="none" stroke="{color}" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round">'
            f'<path d="{a}"/></svg></span>')


def node(kind, high=False, done=False, rep=False, h=36, option='C'):
    """Кружок (dot) или капсула периода (cap). Повтор: у кружка — иконка вместо кружка; у капсулы — вариант A (иконка внутри
    капсулы сверху) или B (иконка над капсулой, капсула короче)."""
    ring = RING if high else NONE
    if kind == 'dot':
        return pt.mark(16, ring, done, rep, FADED if high else None)
    # капсула
    if done:
        fill = FADED if high else '#C4C6CA'
        return (f'<span style="width:16px;height:{h}px;border-radius:8px;background:{fill};display:flex;align-items:center;justify-content:center;flex-shrink:0;">'
                f'{ico(P_CHECK, "#fff", 11, 3)}</span>')
    if rep and option == 'C':  # решение PM: у повтора с длительностью — знак повтора, вытянутый на высоту капсулы
        return tall_repeat(ring, h)
    if rep and option == 'A':
        return (f'<span style="width:16px;height:{h}px;border-radius:8px;border:2px solid {ring};background:{BG};box-sizing:border-box;display:flex;flex-direction:column;align-items:center;padding-top:2px;flex-shrink:0;">'
                f'{ico(P_REP, ring, 11, 2.6)}</span>')
    if rep and option == 'B':
        return (f'<span style="display:flex;flex-direction:column;align-items:center;gap:2px;height:{h}px;flex-shrink:0;">{ico(P_REP, ring, 16, 2.2)}'
                f'<span style="width:12px;flex:1;border-radius:6px;border:2px solid {ring};background:{BG};box-sizing:border-box;"></span></span>')
    return f'<span style="width:16px;height:{h}px;border-radius:8px;border:2px solid {ring};background:{BG};box-sizing:border-box;flex-shrink:0;"></span>'


def row(title, t1='', t2='', kind='dot', high=False, done=False, rep=False, over=False, bell=False, emo='📥', option='C', ghost=False):
    h = 52 if kind == 'cap' else 36
    times = (f'<span style="display:flex;flex-direction:column;justify-content:space-between;height:{h - 8}px;"><span>{t1}</span><span style="color:#8E9096">{t2}</span></span>'
             if kind == 'cap' else f'<span>{t1}</span>')
    icons = (ico(P_BELL, '#6B6E76', 17, 1.8) if bell else '') + emoji(emo)
    st = 'text-decoration:line-through;color:#8E9096;' if done else ''
    return (f'<div style="display:flex;align-items:center;height:{h}px;margin-top:5px;">'
            f'<span style="width:40px;text-align:right;font-size:11.5px;font-weight:600;color:{OVD if over else T2};padding-right:6px;box-sizing:border-box;">{times}</span>'
            f'<span style="width:22px;display:flex;justify-content:center;position:relative;z-index:1;">{node(kind, high, done, rep, h - 4, option)}</span>'
            f'<span style="flex:1;height:{h - 4 if kind == "cap" else 36}px;margin:0 6px;border-radius:11px;background:{SURF};display:flex;align-items:center;gap:8px;padding:0 10px;box-sizing:border-box;">'
            f'<span style="flex:1;font-size:13.5px;font-weight:600;color:{INK};{st}white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{title}</span>{icons}</span></div>')


def timeline(rows):
    return f'<div style="position:relative;"><span style="position:absolute;left:52px;top:24px;bottom:14px;width:1.5px;background:{LINE};"></span>{rows}</div>'


def screen_all(option='C'):
    d = pt.day_label
    rows = (d('Вчера', 'Просрочено', red=True) + row('Отправить отчёт', '18:00', high=True, over=True, emo='💼')
            + d('Сегодня', '6 задач') + row('Зарядка', '08:00', rep=True, done=True, emo='💪')
            + row('Дейлик', '11:00', high=True, rep=True, emo='💼')
            + row('Созвон с командой', '12:00', '13:00', kind='cap', high=True, rep=True, emo='💼', option=option)
            + row('Финансы', '14:00', bell=True, emo='💰') + row('Тренировка', '18:00', '19:30', kind='cap', rep=True, emo='💪', option=option)
            + row('Купить продукты', '', high=True, done=True, emo='🛒')
            + d('Завтра') + row('Созвон с командой', '12:00', '13:00', kind='cap', high=True, rep=True, emo='💼', option=option, ghost=True)
            + row('Дейлик', '11:00', high=True, rep=True, emo='💼'))
    return phone(BG, status(INK) + timeline(rows))


def sheet(prio_on):
    tool = lambda inner, sel=False, bg=None: (f'<span style="width:48px;height:48px;border-radius:12px;background:{bg or (SURF if not sel else "#ECECEC")};border:1px solid {LINE if not bg else "transparent"};'
                                              f'display:flex;align-items:center;justify-content:center;">{inner}</span>')
    prio = tool(ico(P_FLAG, RING if prio_on else '#8E9096', 23, 2), bg='#F1E8FA' if prio_on else None)
    tools = (tool(emoji('💼', 21)) + prio + tool(ico(P_LIST, '#6B6E76', 23, 2)) + tool(ico(P_REP, '#6B6E76', 23, 2)) + tool(ico(P_BELL, '#6B6E76', 23, 2))
             + f'<span style="flex:1"></span><span style="width:52px;height:52px;border-radius:14px;background:{INK};display:flex;align-items:center;justify-content:center;">{ico(P_CHECK, "#fff", 24, 2.6)}</span>')
    return (f'<div style="background:{SURF};border-radius:22px;padding:14px 14px 14px;box-shadow:0 2px 12px #0001;width:380px;box-sizing:border-box;">'
            f'<div style="font-size:21px;font-weight:800;color:{INK};">Созвон с командой</div>'
            f'<div style="display:flex;gap:16px;margin:12px 0 14px;font-size:14px;font-weight:700;color:{INK};"><span style="display:flex;gap:6px;align-items:center;">{ico(P_CAL, INK, 18, 2)}чт, 5 ноя</span>'
            f'<span style="display:flex;gap:6px;align-items:center;">{ico(P_CLOCK, INK, 18, 2)}12:00–13:00</span></div>'
            f'<div style="display:flex;gap:8px;align-items:center;">{tools}</div></div>')


def screen_sheet():
    cap = lambda s: f'<div style="font-size:12px;font-weight:700;color:{T2};margin:14px 0 6px 16px;">{s}</div>'
    return phone('#8D8D8D', status('#fff') + '<div style="height:160px"></div>'
                 + f'<div style="padding:0 16px;">{cap("<span style=color:#fff>Приоритет выключен — серый флажок</span>")}{sheet(False)}'
                 + f'{cap("<span style=color:#fff>Нажал — сразу высокий: фиолетовый флажок на бледной подложке. Ещё раз — выключен</span>")}{sheet(True)}</div>')


def screen_lists():
    def item(e, name, count, indent=False):
        return (f'<div style="display:flex;align-items:center;height:48px;padding-left:{40 if indent else 18}px;padding-right:20px;gap:14px;">'
                f'<span style="width:30px;height:30px;border-radius:9px;background:{SURF};display:flex;align-items:center;justify-content:center;">{emoji(e, 18)}</span>'
                f'<span style="flex:1;font-size:16px;font-weight:600;color:{INK};">{name}</span><span style="font-size:14px;color:{T2};">{count}</span></div>')
    head = (f'<div style="display:flex;align-items:center;padding:6px 18px 8px;"><span style="font-size:28px;font-weight:800;color:{INK};flex:1;">Списки</span>'
            f'<span style="font-size:14px;font-weight:600;color:{INK};margin-right:14px;">+ Папка</span><span style="font-size:14px;font-weight:600;color:{INK};">+ Список</span></div>')
    plain = lambda t: f'<div style="display:flex;align-items:center;height:48px;padding:0 18px;gap:14px;font-size:16px;font-weight:600;color:{INK};">{ico(P_CHECK, T2, 22, 2)}{t}</div>'
    folder = f'<div style="display:flex;align-items:center;height:46px;padding:0 20px 0 18px;font-size:16px;font-weight:700;color:{INK};">⌄&nbsp;&nbsp;Личное<span style="flex:1"></span><span style="font-weight:500;color:{T2};">5</span></div>'
    body = (head + plain('Выполненные') + plain('Корзина') + item('📥', 'Входящие', 3) + folder + item('🏠', 'Дом', 4, True) + item('❤️', 'Здоровье', 1, True)
            + item('💼', 'Работа', 12) + item('💡', 'Идеи', 0) + item('💪', 'Спорт', 2))
    return phone(BG, status(INK) + body)


POPULAR = [
    ('Дела', '📥 📋 ✅ 📌 ⭐ 🔥 💡 🎯'), ('Работа', '💼 💻 📊 📈 📝 📅 📞 🗂️'), ('Дом', '🏠 🛒 🧹 🍳 🛠️ 🌱 🐶 🧺'),
    ('Здоровье', '❤️ 💪 🏃 🧘 💊 🥗 😴 🦷'), ('Деньги', '💰 💳 🏦 🧾 📉 🪙 🎁 🛍️'), ('Отдых', '✈️ 🎬 🎮 📚 🎵 ⚽ 🏀 🎨'),
]


def screen_picker():
    sel = '💼'
    groups = ''
    for name, es in POPULAR:
        cells = ''.join(f'<span style="width:36px;height:36px;border-radius:10px;display:flex;align-items:center;justify-content:center;'
                        f'{"background:#F1E8FA;box-shadow:inset 0 0 0 2px " + INK + ";" if e == sel else ""}">{emoji(e, 20)}</span>' for e in es.split())
        groups += (f'<div style="font-size:11px;font-weight:700;color:{T2};margin:10px 0 2px;">{name}</div>'
                   f'<div style="display:flex;flex-wrap:wrap;gap:2px;">{cells}</div>')
    field = (f'<div style="display:flex;align-items:center;gap:10px;height:44px;border:1px solid {LINE};border-radius:12px;padding:0 12px;margin-top:12px;">'
             f'<span style="font-size:14px;color:{T2};flex:1;">Свой эмодзи — вставь с клавиатуры</span>{emoji("😀", 20)}</div>')
    btn = lambda t, p: (f'<span style="flex:1;height:46px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:700;'
                        f'{"background:" + INK + ";color:#fff;" if p else "border:1px solid " + LINE + ";color:" + INK + ";"}">{t}</span>')
    dlg = (f'<div style="background:{SURF};border-radius:22px;padding:18px;width:360px;box-sizing:border-box;">'
           f'<div style="display:flex;align-items:center;gap:10px;"><span style="font-size:20px;font-weight:800;color:{INK};flex:1;">Эмодзи списка</span>{emoji(sel, 26)}</div>'
           f'{groups}{field}<div style="display:flex;gap:10px;margin-top:14px;">{btn("Отмена", False)}{btn("Готово", True)}</div></div>')
    return phone('#8D8D8D', status('#fff') + f'<div style="display:flex;justify-content:center;padding-top:70px;">{dlg}</div>')


def screen_widget():
    """Виджет «Все»: те же строки, квадратик списка → эмодзи."""
    d = pt.day_label
    rows = (d('Сегодня', '4 задачи') + row('Дейлик', '11:00', high=True, rep=True, emo='💼')
            + row('Созвон с командой', '12:00', '13:00', kind='cap', high=True, rep=True, emo='💼') + row('Финансы', '14:00', emo='💰')
            + row('Купить продукты', '', high=True, done=True, emo='🛒') + d('Завтра') + row('Тренировка', '18:00', '19:30', kind='cap', rep=True, emo='💪'))
    wg = (f'<div style="position:absolute;left:10px;right:10px;top:60px;height:440px;border-radius:26px;overflow:hidden;background:{BG};padding-top:4px;">'
          f'{timeline(rows)}</div>')
    week = f'<div style="position:absolute;left:10px;right:10px;top:520px;">{pt.week_block(V)}</div>'
    return phone(WALL, status('#fff') + wg + week)


def detail():
    box = lambda t, inner: (f'<div style="margin-bottom:14px;"><div style="font-size:12px;font-weight:700;color:{T2};margin-bottom:4px;">{t}</div>'
                            f'<div style="width:400px;background:{BG};border:1px solid {LINE};border-radius:14px;padding:12px;box-sizing:border-box;">{inner}</div></div>')
    opt = lambda o: timeline(row('Созвон с командой', '12:00', '13:00', kind='cap', high=True, rep=True, emo='💼', option=o)
                             + row('Тренировка', '18:00', '19:30', kind='cap', rep=True, emo='💪', option=o))
    mapping = ''.join(f'<span style="display:flex;align-items:center;gap:6px;width:118px;font-size:12px;color:{INK};">{emoji(e, 16)} {n}</span>'
                      for e, n in [('📥', 'Входящие'), ('💼', 'работа'), ('🏠', 'дом'), ('❤️', 'здоровье'), ('🛒', 'покупки'), ('📚', 'учёба'),
                                   ('💰', 'деньги'), ('✈️', 'поездки'), ('💪', 'спорт'), ('💡', 'идеи'), ('⭐', 'звезда'), ('📁', 'без значка')])
    plain = timeline(row('Обед', '13:00', '14:00', kind='cap', high=True, emo='🏠') + row('Прогулка', '19:00', '20:00', kind='cap', emo='💪'))
    return (box('Повтор + длительность (решение PM): знак повтора высотой с капсулу — две половины со стрелками; конец периода — серым под началом', opt('C'))
            + box('Длительность без повтора — капсула, как сейчас', plain)
            + box('Старые списки: значок → эмодзи (один раз при обновлении)', f'<div style="display:flex;flex-wrap:wrap;gap:6px 4px;">{mapping}</div>')
            + f'<div style="font-size:13px;color:{T2};line-height:19px;width:400px;">Цвет списка больше не выбирается и нигде не показывается — вместо квадратика эмодзи. '
              f'TickTick: «🏠Дом» в файле → список «Дом» с эмодзи 🏠; при экспорте эмодзи возвращается в начало названия. '
              f'Меню списка: «Переименовать», «Эмодзи», «Удалить» (пункты «Цвет» и «Значок» уходят).</div>')


SCREENS = [
    ('1. «Все»', 'Высокий — фиолетовый. Повтор — иконка вместо кружка; у задачи с длительностью знак повтора вытянут на высоту капсулы. Выполненная высокая — тусклый фиолетовый. Справа — эмодзи списка.', lambda: screen_all('C')),
    ('2. Окно задачи', 'Окна приоритета нет: нажатие на флажок сразу включает высокий, повторное — выключает. Кнопка списка — эмодзи.', screen_sheet),
    ('3. «Списки»', 'Вместо цветных квадратиков со значками — эмодзи на белой плитке.', screen_lists),
    ('4. Выбор эмодзи', '«⋮» списка → «Эмодзи»: сетка популярных по группам и поле для любого эмодзи с клавиатуры.', screen_picker),
    ('5. Виджет', 'Лента «Все» и «Неделя» в виджете — те же правила.', screen_widget),
    ('6. Детали', 'Повтор с длительностью — знак повтора высотой с капсулу; без повтора — капсула. Перевод старых значков в эмодзи.', detail),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:74px;width:428px;margin-bottom:10px;">{s}</div>'
    cell_ = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    W, H = 6 * 428 + 5 * 30 + 80, 1230
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EFEFEF;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Два приоритета и эмодзи списков — итерация 47</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Решения PM: высокий — фиолетовый; повтор иконкой вместо кружка; приоритет включается нажатием без окна; '
            f'вместо значков и цветов списков — эмодзи. «Неделя» — как на доске VariantH1 (вариант В).</div>'
            f'<div style="display:flex;gap:30px;">{"".join(cell_(*s) for s in SCREENS)}</div></div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Приоритеты и эмодзи</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantH2.dc.html'), 'w').write(html)
    cj = os.path.join(OUT, 'canvas.json')
    if os.path.exists(cj):
        c = json.load(open(cj))
        fresh = 'VariantH2.dc.html' not in c['boards']
        old = {k: v for k, v in c['boards'].items() if k != 'VariantH2.dc.html'}
        if fresh:
            for v in old.values():
                v['y'] += H + 120
        c['boards'] = {'VariantH2.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Два приоритета и эмодзи списков (итерация 47)'}, **old}
        c['order'] = list(c['boards'])
        c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Два приоритета и эмодзи списков (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
        json.dump(c, open(cj, 'w'), ensure_ascii=False, indent=2)
