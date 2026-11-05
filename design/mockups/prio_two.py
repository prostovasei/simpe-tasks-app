"""Макет «Два приоритета» (итерация 47, доска VariantH1): варианты цвета высокого приоритета.

Решения PM: остаются только высокий и обычный приоритет (средний → высокий, низкий → обычный, миграцией базы и при импорте TickTick);
будущий повтор не тусклый; у повторяющейся задачи вместо кружка — иконка повтора (нажатие = «выполнить»), справа на плашке её нет,
везде — приложение и виджет, списки и «Неделя»; выполненная задача с приоритетом — тусклого цвета своего приоритета; в «Неделе»
просроченное время не красное; строка даты «Недели» — день недели и число одним нежирным начертанием, серые; сегодня — жирное
и чёрное, без кружка (в приложении тоже). Цвет высокого — PM выбирает по этому макету.
Запуск: python3 prio_two.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_INBOX, P_CHECK, P_REP, P_BELL

INK, T2, BG, SURF, LINE, INACT = '#16171D', '#62656E', '#F5F5F5', '#FFFFFF', '#E2E2E2', '#A9A9A9'
NONE_RING, OVD = '#A9A9A9', '#C62828'
# Варианты: (название, кружок, фон строки «Недели», тусклый — выполненная)
VARIANTS = [
    ('А. Оранжевый', '#F76B15', '#FEEBDD', '#F9BE97'),
    ('Б. Красный (как сейчас)', '#E5484D', '#FDECEC', '#F2A9AB'),
    ('В. Фиолетовый', '#8E4EC6', '#F1E8FA', '#CDB3E6'),
    ('Г. Чёрный', '#16171D', '#E6E6E6', '#A9AAAE'),
]


def mark(size, ring, done=False, rep=False, faded=None, stroke=2):
    """Кружок (или иконка повтора); выполненная — залита тусклым цветом приоритета (без приоритета — серым)."""
    if done:
        fill = faded or '#C4C6CA'
        return (f'<span style="width:{size}px;height:{size}px;border-radius:50%;background:{fill};display:flex;align-items:center;justify-content:center;flex-shrink:0;">'
                f'{ico(P_CHECK, "#fff", size - 5, 3)}</span>')
    if rep:
        return f'<span style="width:{size}px;height:{size}px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(P_REP, ring, size + 2, 2.2)}</span>'
    return f'<span style="width:{size}px;height:{size}px;border-radius:50%;border:{stroke}px solid {ring};background:{BG};box-sizing:border-box;flex-shrink:0;"></span>'


def list_row(v, title, time='', high=False, done=False, rep=False, over=False, bell=False, lsq='#3D8BFD'):
    _, ring, _, faded = v
    r = ring if high else NONE_RING
    icons = (ico(P_BELL, '#6B6E76', 17, 1.8) if bell else '')
    icons += f'<span style="width:17px;height:17px;border-radius:5px;background:{lsq};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(P_INBOX, "#fff", 11, 2)}</span>'
    st = 'text-decoration:line-through;color:#8E9096;' if done else ''
    return (f'<div style="display:flex;align-items:center;height:36px;margin-top:5px;">'
            f'<span style="width:40px;text-align:right;font-size:11.5px;font-weight:600;color:{OVD if over else T2};padding-right:6px;box-sizing:border-box;">{time}</span>'
            f'<span style="width:22px;display:flex;justify-content:center;position:relative;z-index:1;">{mark(16, r, done, rep, faded if high else None)}</span>'
            f'<span style="flex:1;height:36px;margin:0 6px 0 6px;border-radius:11px;background:{SURF};display:flex;align-items:center;gap:8px;padding:0 10px;box-sizing:border-box;">'
            f'<span style="flex:1;font-size:13.5px;font-weight:600;color:{INK};{st}white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{title}</span>{icons}</span></div>')


def day_label(t, chip='', red=False):
    c = OVD if red else INK
    ch = f'<span style="font-size:11px;font-weight:700;color:{c};background:{SURF};border-radius:7px;padding:2px 7px;margin-left:7px;">{chip}</span>' if chip else ''
    return f'<div style="display:flex;align-items:center;padding:10px 0 0 10px;font-size:13.5px;font-weight:700;color:{c};">{t}{ch}</div>'


def list_block(v):
    rows = (day_label('Вчера', 'Просрочено', red=True) + list_row(v, 'Отправить отчёт', '18:00', high=True, over=True, lsq='#8E4EC6')
            + day_label('Сегодня', '5 задач') + list_row(v, 'Дейлик', '11:00', high=True, rep=True) + list_row(v, 'Финансы', '14:00', bell=True, lsq='#46A758')
            + list_row(v, 'Зарядка', '08:00', rep=True) + list_row(v, 'Созвон с командой', '12:00', high=True, done=True)
            + list_row(v, 'Купить продукты', '', done=True, lsq='#F76B15')
            + day_label('Завтра') + list_row(v, 'Дейлик', '11:00', high=True, rep=True))
    line = f'<span style="position:absolute;left:52px;top:30px;bottom:14px;width:1.5px;background:{LINE};"></span>'
    return f'<div style="position:relative;background:{BG};border-radius:14px;padding-bottom:8px;">{line}{rows}</div>'


def week_task(v, title, time='', high=False, done=False, rep=False):
    _, ring, tint, faded = v
    bg = tint if high else SURF
    r = ring if high else NONE_RING
    tc = '#8E9096' if done else INK
    st = 'text-decoration:line-through;' if done else ''
    t = f'<span style="font-size:10.5px;font-weight:600;color:{"#A9A9A9" if done else T2};margin-left:5px;">{time}</span>' if time else ''
    return (f'<div style="height:21px;margin-top:3px;border-radius:7px;background:{bg};display:flex;align-items:center;gap:5px;padding:0 6px;box-sizing:border-box;">'
            f'{mark(13, r, done, rep, faded if high else None)}<span style="flex:1;min-width:0;font-size:12px;font-weight:700;color:{tc};white-space:nowrap;overflow:hidden;text-overflow:ellipsis;{st}">{title}</span>{t}</div>')


def week_head(wd, num, today=False):
    w = 800 if today else 500
    c = INK if today else T2
    return f'<div style="display:flex;align-items:center;height:20px;gap:4px;font-size:12.5px;font-weight:{w};color:{c};">{wd} {num}</div>'


def week_block(v):
    def cell(head, tasks, right=True):
        return (f'<div style="flex:1;min-width:0;height:128px;overflow:hidden;box-sizing:border-box;padding:4px 5px;{"border-right:1px solid " + LINE + ";" if right else ""}border-bottom:1px solid {LINE};">'
                f'{head}{"".join(tasks)}</div>')
    r1 = (cell(week_head('пн', 2), [week_task(v, 'Вес', '10:00'), week_task(v, 'Синк', '11:00', high=True, done=True), week_task(v, 'Отчёт', '18:00', high=True)])
          + cell(week_head('вт', 3, today=True), [week_task(v, 'Дейлик', '11:00', high=True, rep=True), week_task(v, 'Финансы', '14:00'), week_task(v, 'Зарядка', '08:00', rep=True)], right=False))
    r2 = (cell(week_head('ср', 4), [week_task(v, 'Дейлик', '11:00', high=True, rep=True), week_task(v, 'Spotify', '10:00'), week_task(v, 'Стирка', '14:30', done=True)])
          + cell(week_head('чт', 5), [week_task(v, 'Дейлик', '11:00', high=True, rep=True), week_task(v, 'Девопсы', '14:30')], right=False))
    return f'<div style="background:{BG};border-radius:14px;overflow:hidden;"><div style="display:flex;">{r1}</div><div style="display:flex;">{r2}</div></div>'


def variant(v):
    cap = lambda s: f'<div style="font-size:12px;font-weight:700;color:{T2};margin:12px 0 5px;">{s}</div>'
    return (f'<div style="width:400px;">{cap("«Все»: высокий, повтор вместо кружка, выполненные — тусклые")}{list_block(v)}'
            f'{cap("«Неделя»: просрочка не красная, понедельник прошёл; «вт 3» — сегодня")}{week_block(v)}</div>')


def detail():
    v = VARIANTS[0]
    _, ring, tint, faded = v
    box = lambda t, inner: (f'<div style="margin-bottom:14px;"><div style="font-size:12px;font-weight:700;color:{T2};margin-bottom:4px;">{t}</div>'
                            f'<div style="width:400px;background:{BG};border:1px solid {LINE};border-radius:14px;padding:12px;box-sizing:border-box;">{inner}</div></div>')
    lab = lambda m, t: f'<span style="display:flex;flex-direction:column;align-items:center;gap:6px;font-size:11px;font-weight:600;color:{T2};width:84px;text-align:center;">{m}{t}</span>'
    marks = (f'<div style="display:flex;justify-content:space-between;">{lab(mark(20, NONE_RING), "Обычный")}{lab(mark(20, ring), "Высокий")}'
             f'{lab(mark(20, NONE_RING, rep=True), "Повтор")}{lab(mark(20, ring, rep=True), "Повтор, высокий")}</div>'
             f'<div style="display:flex;justify-content:space-between;margin-top:12px;">{lab(mark(20, NONE_RING, done=True), "Выполнена")}'
             f'{lab(mark(20, ring, done=True, faded=faded), "Выполнена, высокий")}{lab(mark(20, NONE_RING, rep=True), "Будущий повтор — не тусклый")}<span style="width:84px"></span></div>')
    heads = (f'<div style="display:flex;gap:28px;">{week_head("пн", 2)}{week_head("вт", 3, today=True)}{week_head("ср", 4)}</div>'
             f'<div style="font-size:12px;color:{T2};margin-top:6px;line-height:17px;">Обычный день — день недели и число одним начертанием (Medium), серые. Сегодня — жирное и чёрное, без кружка; так и в приложении, и в виджете.</div>')
    prio = (f'<div style="display:flex;gap:10px;">'
            + ''.join(f'<span style="flex:1;height:44px;border-radius:12px;background:{SURF};border:1px solid {LINE if not sel else ring};display:flex;align-items:center;gap:8px;padding:0 12px;font-size:13px;font-weight:700;color:{INK};">'
                      f'{mark(16, c)}{t}</span>' for t, c, sel in [('Высокий', ring, True), ('Обычный', NONE_RING, False)])
            + '</div>')
    return (box('Кружок задачи — все состояния (вариант А)', marks) + box('Строка даты в «Неделе»', heads)
            + box('Окно приоритета: два варианта вместо четырёх', prio)
            + f'<div style="font-size:13px;color:{T2};line-height:19px;width:400px;">TickTick: при импорте средний (3) → высокий (5), низкий (1) → обычный (0); '
              f'задачи, уже лежащие в приложении, переводятся так же один раз при обновлении. Экспорт выгружает новые значения. '
              f'Просроченное время в списках остаётся красным, в «Неделе» — серое.</div>')


if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    cols = ''.join(f'<div>{lab(v[0])}{variant(v)}</div>' for v in VARIANTS) + f'<div>{lab("Детали")}{detail()}</div>'
    W, H = 5 * 400 + 4 * 30 + 80, 1060
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EFEFEF;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Два приоритета: цвет высокого — итерация 47</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Решения PM: только высокий и обычный; повтор — иконкой вместо кружка; выполненная — тусклым цветом приоритета; '
            f'будущий повтор не тусклый; в «Неделе» просрочка не красная, строка даты — одним нежирным начертанием, сегодня жирным. Выбери цвет высокого: А, Б, В или Г.</div>'
            f'<div style="display:flex;gap:30px;">{cols}</div></div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Два приоритета</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantH1.dc.html'), 'w').write(html)
    cj = os.path.join(OUT, 'canvas.json')
    if os.path.exists(cj):
        c = json.load(open(cj))
        fresh = 'VariantH1.dc.html' not in c['boards']
        old = {k: v for k, v in c['boards'].items() if k != 'VariantH1.dc.html'}
        if fresh:
            for v in old.values():
                v['y'] += H + 120
        c['boards'] = {'VariantH1.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Два приоритета: цвет высокого (итерация 47)'}, **old}
        c['order'] = list(c['boards'])
        c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Два приоритета (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
        json.dump(c, open(cj, 'w'), ensure_ascii=False, indent=2)
