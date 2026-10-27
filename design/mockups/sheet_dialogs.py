"""Макет «Дополнительные окна шторки задачи» (ТЗ PM, итерация 31): единое окно по центру над затемнением,
«Отмена» и «Готово» внизу, клавиатура скрыта. Плюс контраст кнопок панели.

Телефон 412×860 (dp 1:1). Задача — среда 28 октября, 18:00.
Запуск: python3 sheet_dialogs.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_INBOX, P_BELL, P_REP, P_CHECK, P_FLAG, P_LIST
from tasks_polish import A, INK, T2, BG, PRI, SEL, phone, status, row, day

LINE = '#E3E7E4'
DIM = '#C3CBC6'      # иконка недоступной кнопки (новое)
SCRIM = 'rgba(21,22,31,.42)'
SHADOW = 'box-shadow:0 18px 50px rgba(21,22,31,.28);'
P_MINUS, P_PLUS = 'M6 12h12', 'M12 6v12M6 12h12'


def feed():
    """Лента под шторкой (размыта затемнением)."""
    return (status() + '<div style="padding:6px 0;">' + day('Сегодня', 'вт, 27 окт', first=True)
            + row('Позвонить маме', '10:00', ring=PRI['mid']) + row('Оплатить интернет', '12:30')
            + row('Тренировка', '19:00', '20:30', kind='cap', ring=PRI['low'])
            + day('Завтра') + row('Забрать посылку', '18:00', ring=PRI['high'], rem='at') + row('Купить продукты') + '</div>')


def tool(path, state='on'):
    """Кнопка панели. state: on — доступна, sel — выбрана, dim — недоступна (новое: бледная только иконка), old — как сейчас."""
    bg, br, c, op = '#fff', LINE, T2, 1
    if state == 'sel':
        bg, br, c = SEL, SEL, A
    elif state == 'dim':
        c = DIM
    elif state == 'old':
        op = .38
    return (f'<span style="width:50px;height:50px;border-radius:16px;border:1px solid {br};background:{bg};box-sizing:border-box;opacity:{op};'
            f'display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(path, c, 24, 1.9)}</span>')


def list_sq(color='#2E7DD7', size=26):
    return f'<span style="width:{size}px;height:{size}px;border-radius:8px;background:{color};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(P_INBOX, "#fff", size * .6, 2)}</span>'


def sheet(rep='on', bell='sel', date=True):
    """Шторка задачи внизу экрана — клавиатура скрыта."""
    chip = (f'<span style="height:40px;padding:0 14px;border-radius:14px;background:{SEL};display:flex;align-items:center;gap:6px;font-size:15px;font-weight:700;color:{A};">'
            f'ср, 28 окт · 18:00</span>') if date else (f'<span style="height:40px;padding:0 14px;border-radius:14px;border:1px solid {LINE};display:flex;align-items:center;font-size:15px;font-weight:700;color:{T2};">Без даты</span>')
    tools = (f'<span style="width:50px;height:50px;border-radius:16px;border:1px solid {LINE};display:flex;align-items:center;justify-content:center;box-sizing:border-box;">{list_sq()}</span>'
             + tool(P_REP, rep) + tool(P_BELL, bell) + tool(P_FLAG) + tool(P_LIST))
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;background:#fff;border-radius:28px 28px 0 0;padding:10px 16px 26px;box-shadow:0 -6px 24px rgba(0,0,0,.1);">'
            f'<div style="display:flex;justify-content:center;"><span style="width:36px;height:4px;border-radius:2px;background:#D5DAD7;"></span></div>'
            f'<div style="font-size:21px;font-weight:700;color:{INK};margin-top:12px;">Забрать посылку</div>'
            f'<div style="font-size:15px;color:#A3ABA7;margin-top:6px;">Описание</div>'
            f'<div style="display:flex;margin-top:14px;">{chip}</div>'
            f'<div style="display:flex;gap:6px;margin-top:14px;align-items:center;">{tools}<span style="flex:1"></span>'
            f'<span style="width:50px;height:50px;border-radius:16px;background:{A};display:flex;align-items:center;justify-content:center;">{ico(P_CHECK, "#fff", 24, 2.6)}</span></div></div>')


def check(on):
    """Галочка выбора справа (одиночный выбор)."""
    return f'<span style="width:24px;display:flex;justify-content:center;flex-shrink:0;">{ico(P_CHECK, A, 22, 2.6) if on else ""}</span>'


def box(on):
    """Квадрат-галочка (можно выбрать несколько)."""
    return (f'<span style="width:22px;height:22px;border-radius:7px;border:2px solid {A if on else "#B8C2BC"};background:{A if on else "#fff"};box-sizing:border-box;'
            f'display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(P_CHECK, "#fff", 15, 3) if on else ""}</span>')


def item(title, sub='', on=False, lead='', multi=False, hint=''):
    left = box(on) if multi else lead
    right = '' if multi else check(on)
    s = f'<span style="display:block;font-size:13px;color:{T2};margin-top:1px;">{sub}</span>' if sub else ''
    h = f'<span style="font-size:13px;font-weight:600;color:{T2};flex-shrink:0;">{hint}</span>' if hint else ''
    return (f'<div style="min-height:52px;display:flex;align-items:center;gap:14px;padding:6px 22px;box-sizing:border-box;{"background:#F3F8F4;" if on and not multi else ""}">'
            f'{left}<span style="flex:1;"><span style="display:block;font-size:16px;font-weight:{700 if on else 600};color:{INK};">{title}</span>{s}</span>{h}{right}</div>')


def sep():
    return f'<div style="height:1px;background:{LINE};margin:4px 22px;"></div>'


def label(t):
    return f'<div style="font-size:12px;font-weight:700;letter-spacing:.6px;color:{T2};padding:12px 22px 4px;">{t}</div>'


def dialog(title, body, sub='', ok='Готово'):
    """Единое окно: заголовок, содержимое (прокручивается), «Отмена» и «Готово» всегда видны."""
    s = f'<div style="font-size:14px;color:{T2};margin-top:4px;">{sub}</div>' if sub else ''
    btns = (f'<span style="flex:1;height:50px;border-radius:16px;border:1px solid {LINE};display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:700;color:{INK};">Отмена</span>'
            f'<span style="flex:1;height:50px;border-radius:16px;background:{A};display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:700;color:#fff;">{ok}</span>')
    return (f'<div style="position:absolute;inset:0;background:{SCRIM};z-index:5;"></div>'
            f'<div style="position:absolute;inset:34px 0 0;z-index:6;display:flex;align-items:center;justify-content:center;">'
            f'<div style="width:364px;max-height:740px;border-radius:28px;background:#fff;{SHADOW}display:flex;flex-direction:column;overflow:hidden;">'
            f'<div style="padding:22px 22px 8px;"><div style="font-size:21px;font-weight:700;color:{INK};">{title}</div>{s}</div>'
            f'<div style="padding:4px 0 8px;">{body}</div>'
            f'<div style="display:flex;gap:10px;padding:12px 22px 22px;border-top:1px solid {LINE};">{btns}</div></div></div>')


def callout(text, top):
    return (f'<div style="position:absolute;left:16px;right:16px;top:{top}px;z-index:30;font-size:12.5px;line-height:17px;color:#fff;'
            f'background:rgba(21,22,31,.86);border-radius:12px;padding:8px 12px;">{text}</div>')


def under():
    return feed() + sheet(rep='sel')


def scr_repeat():
    body = (item('Нет') + sep()
            + item('Ежедневно') + item('Еженедельно', 'по средам', on=True) + item('Ежемесячно', '28 числа') + item('Ежегодно', '28 октября') + sep()
            + item('Каждый рабочий день', 'пн–пт') + sep()
            + item('Произвольно…', 'каждые N дней, недель, месяцев'))
    return under() + dialog('Повтор', body)


def scr_custom():
    chip = lambda t, on=False: (f'<span style="flex:1;height:40px;border-radius:12px;background:{SEL if on else "#fff"};border:1px solid {SEL if on else LINE};'
                                f'display:flex;align-items:center;justify-content:center;font-size:14px;font-weight:700;color:{A if on else INK};">{t}</span>')
    rnd = lambda p: f'<span style="width:44px;height:44px;border-radius:50%;border:1px solid {LINE};display:flex;align-items:center;justify-content:center;">{ico(p, INK, 20, 2)}</span>'
    wd = lambda t, on=False: (f'<span style="width:40px;height:40px;border-radius:50%;background:{A if on else "#fff"};border:1px solid {A if on else LINE};box-sizing:border-box;'
                              f'display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:700;color:{"#fff" if on else INK};">{t}</span>')
    body = (f'<div style="padding:6px 22px 0;">'
            f'<div style="display:flex;gap:6px;">{chip("День")}{chip("Неделя", True)}{chip("Месяц")}{chip("Год")}</div>'
            f'<div style="display:flex;align-items:center;gap:14px;margin-top:20px;"><span style="font-size:16px;font-weight:600;color:{INK};flex:1;">Каждые</span>'
            f'{rnd(P_MINUS)}<span style="font-size:22px;font-weight:700;color:{INK};width:28px;text-align:center;">2</span>{rnd(P_PLUS)}'
            f'<span style="font-size:16px;font-weight:600;color:{INK};width:62px;">недели</span></div>'
            f'<div style="display:flex;justify-content:space-between;margin-top:20px;">{wd("пн")}{wd("вт")}{wd("ср", True)}{wd("чт")}{wd("пт", True)}{wd("сб")}{wd("вс")}</div>'
            f'<div style="margin-top:18px;padding:12px 14px;border-radius:14px;background:{BG};font-size:14px;font-weight:600;color:{T2};">Каждые 2 недели: ср, пт</div></div>')
    return under() + dialog('Произвольно', body)


def scr_reminders():
    body = (label('ОСНОВНЫЕ')
            + item('В момент срока', multi=True, on=True, hint='18:00') + item('За 30 минут', multi=True, hint='17:30')
            + item('За 1 час', multi=True, on=True, hint='17:00') + item('За 1 день', multi=True, hint='вт 18:00') + item('За 3 дня', multi=True, hint='вс 18:00')
            + sep() + label('СОЗДАННЫЕ ВРУЧНУЮ')
            + item('За 2 часа 15 минут', multi=True, on=True, hint='15:45') + item('За 45 минут', multi=True, hint='17:15') + item('За 2 дня', multi=True, hint='пн 18:00')
            + f'<div style="height:52px;display:flex;align-items:center;gap:14px;padding:0 22px;font-size:16px;font-weight:700;color:{A};">{ico(P_PLUS, A, 22, 2.2)}Вручную…</div>')
    return under() + dialog('Уведомления', body, sub='ср, 28 окт · 18:00')


def scr_manual():
    rnd = lambda p: f'<span style="width:44px;height:44px;border-radius:50%;border:1px solid {LINE};display:flex;align-items:center;justify-content:center;">{ico(p, INK, 20, 2)}</span>'
    step = lambda name, v: (f'<div style="display:flex;align-items:center;gap:14px;padding:8px 22px;"><span style="flex:1;font-size:16px;font-weight:600;color:{INK};">{name}</span>'
                            f'{rnd(P_MINUS)}<span style="font-size:22px;font-weight:700;color:{INK};width:34px;text-align:center;">{v}</span>{rnd(P_PLUS)}</div>')
    body = (step('Дни', 0) + step('Часы', 2) + step('Минуты', 15)
            + f'<div style="margin:12px 22px 4px;padding:12px 14px;border-radius:14px;background:{SEL};font-size:15px;font-weight:700;color:{A};">'
              f'{ico(P_BELL, A, 16, 2)} Сработает в ср, 28 окт, 15:45</div>')
    return under() + dialog('Вручную', body, sub='За сколько до срока напомнить')


def scr_list():
    lists = [('Входящие', '#8A94A0', True), ('Дом', '#2E7DD7', False), ('Работа', '#E08A00', False), ('Покупки', '#15803D', False), ('Здоровье', '#C62828', False)]
    body = ''.join(item(t, lead=list_sq(c, 28), on=on) for t, c, on in lists)
    return under() + dialog('Список', body)


def scr_priority():
    flag = lambda c: f'<span style="width:28px;display:flex;justify-content:center;">{ico(P_FLAG, c, 22, 2.2)}</span>'
    body = (item('Высокий', lead=flag(PRI['high'])) + item('Средний', lead=flag(PRI['mid']), on=True)
            + item('Низкий', lead=flag(PRI['low'])) + item('Нет', lead=flag('#A8B3AB')))
    return under() + dialog('Приоритет', body)


def toolbar_strip():
    """Контраст кнопок: как сейчас и как станет (фон, форма и размер — те же)."""
    def group(name, sub, tools):
        return (f'<div style="background:#fff;border-radius:20px;padding:14px 18px;"><div style="font-size:15px;font-weight:700;color:{INK};">{name}</div>'
                f'<div style="font-size:12.5px;color:{T2};margin:2px 0 10px;">{sub}</div><div style="display:flex;gap:6px;">{tools}</div></div>')
    return (f'<div style="display:flex;gap:20px;margin-bottom:28px;">'
            + group('Сейчас: нет даты', 'Вся кнопка полупрозрачная — сливается с фоном', tool(P_REP, 'old') + tool(P_BELL, 'old'))
            + group('Станет: нет даты', 'Белая кнопка, бледная только иконка', tool(P_REP, 'dim') + tool(P_BELL, 'dim'))
            + group('Станет: доступна', 'Иконка тёмно-серая', tool(P_REP) + tool(P_BELL))
            + group('Станет: выбрано', 'Зелёная иконка на светло-зелёном', tool(P_REP, 'sel') + tool(P_BELL, 'sel'))
            + '</div>')


SCREENS = [
    ('1. Повтор', 'Варианты с датой задачи (ср, 28 окт), выбранный — с галочкой и подсветкой. Нажатие меняет выбор, сохраняет «Готово».', scr_repeat),
    ('2. Произвольно…', 'Вложенное окно поверх «Повтора»: частота, «каждые N», дни недели. «Отмена» — назад в «Повтор» без изменений.', scr_custom),
    ('3. Уведомления', 'Можно несколько. «Основные» и «Созданные вручную» (общая история, по 3 последних). Справа — когда сработает.', scr_reminders),
    ('4. Вручную…', 'За сколько до срока. «Готово» — вариант появляется отмеченным в «Созданных вручную». Без времени — «за N дней в 09:00».', scr_manual),
    ('5. Список', 'Тот же вид: выбор — галочкой, применяется по «Готово».', scr_list),
    ('6. Приоритет', 'Так же. Календарь на месяц получит ту же рамку и кнопки «Отмена» / «Готово».', scr_priority),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    cell = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{phone(BG, f())}</div>'
    rows = ''.join(f'<div style="display:flex;gap:30px;margin-bottom:40px;">{"".join(cell(*s) for s in SCREENS[i:i + 3])}</div>' for i in (0, 3))
    W, H = 3 * 428 + 2 * 30 + 80, 2300
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Окна шторки задачи — единый вид</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Клавиатура прячется, окно по центру над затемнением. Внизу всегда «Отмена» и «Готово»; '
            f'«Назад» и нажатие мимо окна — как «Отмена». Длинное содержимое прокручивается, кнопки остаются на месте.</div>'
            f'{toolbar_strip()}{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Окна шторки задачи</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantD1.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    old = {k: v for k, v in c['boards'].items() if k != 'VariantD1.dc.html'}
    if 'VariantD1.dc.html' not in c['boards']:  # повторный запуск не сдвигает прежние доски ещё раз
        for v in old.values():
            v['y'] += H + 120
    c['boards'] = {'VariantD1.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Окна шторки задачи (итерация 31)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Окна шторки задачи (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
