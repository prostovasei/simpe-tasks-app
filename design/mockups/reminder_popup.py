"""Макет «Всплывающее окно напоминания»: 4 варианта карточки снизу поверх другого приложения (как в TickTick).

Телефон 412×860 (dp 1:1). Под карточкой — условный мессенджер, чтобы видно было «поверх любого приложения».
Запуск: python3 reminder_popup.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES
from sheetlib import ico, P_INBOX, P_CHECK, P_BELL, P_CLOCK
from tasks_polish import A, INK, T2, BG, PRI, phone, status, proj, SEL

P_OPEN = 'M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5'
P_CLOSE = 'M6 6l12 12M18 6L6 18'
LINE = '#E3E7E4'


def messenger():
    """Условное чужое приложение под окном."""
    def msg(w, mine=False):
        side = 'flex-end' if mine else 'flex-start'
        bg = '#DCEBFF' if mine else '#FFFFFF'
        return f'<div style="display:flex;justify-content:{side};padding:0 14px;margin-top:10px;"><span style="width:{w}px;height:38px;border-radius:16px;background:{bg};"></span></div>'
    head = ('<div style="height:56px;display:flex;align-items:center;gap:12px;padding:0 16px;background:#fff;">'
            '<span style="width:36px;height:36px;border-radius:50%;background:#C9D6E8;"></span>'
            '<span style="width:140px;height:12px;border-radius:6px;background:#D9DEE5;"></span></div>')
    return head + ''.join(msg(w, m) for w, m in [(220, 0), (160, 1), (260, 0), (120, 1), (200, 0), (240, 1), (180, 0), (140, 1), (230, 0)])


def btn(label, primary=False, w=None, icon=None):
    bg, c, br = (A, '#fff', A) if primary else ('#fff', INK, LINE)
    width = f'width:{w}px;' if w else 'flex:1;'
    ic = ico(icon, c, 18, 2) if icon else ''
    return (f'<span style="{width}height:48px;border-radius:14px;background:{bg};border:1px solid {br};box-sizing:border-box;display:flex;align-items:center;'
            f'justify-content:center;gap:6px;font-size:15px;font-weight:700;color:{c};">{ic}{label}</span>')


def chip(label, on=False):
    return (f'<span style="height:36px;padding:0 14px;border-radius:12px;background:{SEL if on else "#fff"};border:1px solid {SEL if on else LINE};'
            f'display:flex;align-items:center;font-size:14px;font-weight:600;color:{A if on else INK};">{label}</span>')


def ring(color, size=20):
    return f'<span style="width:{size}px;height:{size}px;border-radius:50%;border:2px solid {color};box-sizing:border-box;flex-shrink:0;"></span>'


def handle():
    return '<div style="display:flex;justify-content:center;padding-top:8px;"><span style="width:36px;height:4px;border-radius:2px;background:#D5DAD7;"></span></div>'


def head_line(time='Сегодня, 18:00'):
    return (f'<div style="display:flex;align-items:center;gap:6px;font-size:13px;font-weight:700;color:{A};">'
            f'{ico(P_BELL, A, 16, 2)}Напоминание · {time}</div>')


SHADOW = 'box-shadow:0 12px 40px rgba(21,22,31,.22);'


def card_a():
    """А. Компактная плашка: название, время, «Выполнить» и «Отложить»."""
    return (f'<div style="position:absolute;left:10px;right:10px;bottom:16px;border-radius:26px;background:#fff;{SHADOW}padding:0 16px 16px;">'
            f'{handle()}<div style="display:flex;align-items:center;gap:10px;margin-top:10px;">{head_line()}<span style="flex:1"></span>{ico(P_CLOSE, T2, 20, 2)}</div>'
            f'<div style="font-size:20px;font-weight:700;color:{INK};margin-top:8px;">Забрать посылку</div>'
            f'<div style="display:flex;gap:10px;margin-top:14px;">{btn("Отложить", icon=P_CLOCK)}{btn("Выполнить", True, icon=P_CHECK)}</div></div>')


def card_b():
    """Б. Как в ленте: кружок-галочка слева, плашка задачи, ниже — быстрые варианты «Отложить»."""
    return (f'<div style="position:absolute;left:10px;right:10px;bottom:16px;border-radius:26px;background:{BG};{SHADOW}padding:0 14px 14px;">'
            f'{handle()}<div style="margin-top:10px;">{head_line()}</div>'
            f'<div style="display:flex;align-items:center;gap:10px;margin-top:10px;">{ring(PRI["high"], 26)}'
            f'<span style="flex:1;height:56px;border-radius:14px;background:#fff;display:flex;align-items:center;gap:10px;padding:0 14px;">'
            f'<span style="flex:1;font-size:17px;font-weight:700;color:{INK};">Забрать посылку</span>{proj()}</span></div>'
            f'<div style="font-size:12px;font-weight:700;color:{T2};margin:14px 0 8px;">Отложить</div>'
            f'<div style="display:flex;gap:8px;">{chip("10 мин")}{chip("1 час")}{chip("Вечером")}{chip("Завтра")}</div></div>')


def card_c():
    """В. Тонкая тёмная полоска: одна строка, кнопки-значки."""
    return (f'<div style="position:absolute;left:12px;right:12px;bottom:20px;height:68px;border-radius:34px;background:#15161F;{SHADOW}'
            f'display:flex;align-items:center;gap:12px;padding:0 10px 0 18px;">'
            f'{ico(P_BELL, "#9BE3B4", 22, 2)}<span style="flex:1;min-width:0;"><span style="display:block;font-size:16px;font-weight:700;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">Забрать посылку</span>'
            f'<span style="display:block;font-size:12px;color:#A9AFBC;">Сегодня, 18:00</span></span>'
            f'<span style="width:48px;height:48px;border-radius:50%;background:#2A2C36;display:flex;align-items:center;justify-content:center;">{ico(P_CLOCK, "#fff", 22, 2)}</span>'
            f'<span style="width:48px;height:48px;border-radius:50%;background:{A};display:flex;align-items:center;justify-content:center;">{ico(P_CHECK, "#fff", 24, 2.6)}</span></div>')


def card_d():
    """Г. Большая шторка с затемнением: описание, чек-лист, «Отложить» с вариантами, «Выполнить»."""
    item = lambda t, done=False: (f'<div style="display:flex;align-items:center;gap:10px;margin-top:10px;">'
                                  f'<span style="width:20px;height:20px;border-radius:6px;border:2px solid {A if done else "#B8C2BC"};background:{A if done else "transparent"};box-sizing:border-box;display:flex;align-items:center;justify-content:center;">{ico(P_CHECK, "#fff", 14, 3) if done else ""}</span>'
                                  f'<span style="font-size:15px;color:{T2 if done else INK};{"text-decoration:line-through;" if done else ""}">{t}</span></div>')
    return ('<div style="position:absolute;inset:0;background:rgba(21,22,31,.38);"></div>'
            f'<div style="position:absolute;left:0;right:0;bottom:0;border-radius:28px 28px 0 0;background:#fff;{SHADOW}padding:0 18px 24px;">'
            f'{handle()}<div style="display:flex;align-items:center;margin-top:12px;">{head_line()}<span style="flex:1"></span>'
            f'<span style="display:flex;align-items:center;gap:6px;font-size:13px;font-weight:700;color:{T2};">{ico(P_OPEN, T2, 16, 2)}Открыть</span></div>'
            f'<div style="display:flex;align-items:center;gap:12px;margin-top:12px;">{ring(PRI["high"], 24)}<span style="font-size:22px;font-weight:700;color:{INK};">Забрать посылку</span></div>'
            f'<div style="font-size:15px;color:{T2};margin-top:8px;line-height:21px;">Пункт выдачи на Ленина, 12 — до 21:00, код 4815</div>'
            f'<div style="margin-top:6px;">{item("Взять паспорт", True)}{item("Сумку побольше")}</div>'
            f'<div style="display:flex;gap:8px;margin-top:18px;">{chip("10 мин")}{chip("1 час")}{chip("Вечером")}{chip("Завтра")}</div>'
            f'<div style="display:flex;gap:10px;margin-top:14px;">{btn("Закрыть")}{btn("Выполнить", True, icon=P_CHECK)}</div></div>')


VARIANTS = [
    ('А. Компактная плашка', 'Белая карточка снизу: «Напоминание · время», название, две кнопки. Смахнуть вниз или × — закрыть, нажатие на название — открыть задачу.', card_a),
    ('Б. Как в ленте', 'Тот же вид, что в ленте: кружок приоритета (нажать — выполнено) и плашка со списком. Ниже — быстрые варианты «Отложить».', card_b),
    ('В. Тонкая полоска', 'Минимум места: тёмная полоска в одну строку, кнопки-значки «Отложить» и «Выполнить». Меньше всего мешает.', card_c),
    ('Г. Большая шторка', 'Затемняет экран, видно описание и чек-лист; «Отложить» — сразу вариантами. Для важных напоминаний.', card_d),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    phones = ''.join(f'<div>{lab(t)}{sub(d)}{phone("#EEF1F5", status() + messenger() + c())}</div>' for t, d, c in VARIANTS)
    W, H = 4 * 428 + 3 * 30 + 80, 1170
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Всплывающее окно напоминания</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;">Поднимается снизу поверх любого приложения в момент напоминания (как в TickTick). Обычное уведомление сверху остаётся.</div>'
            f'<div style="display:flex;gap:30px;">{phones}</div></div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Всплывающее окно напоминания</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantR1.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    old = c['boards']
    shift = H + 120
    for v in old.values():
        v['y'] += shift
    c['boards'] = {'VariantR1.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Всплывающее окно напоминания'}, **{k: v for k, v in old.items() if k != 'VariantR1.dc.html'}}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Всплывающее окно напоминания (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)


# ---- Вариант «В» по решениям PM (T-69): зелёная полоска, подпись «время · список», стопка до 3, «ещё N». ----

def strip(title, sub, bottom, depth=0):
    """Зелёная полоска. depth > 0 — полоска сзади в стопке: уже и ниже, видна только краем."""
    inset = 12 + depth * 10
    op = [1, .85, .7][depth]
    return (f'<div style="position:absolute;left:{inset}px;right:{inset}px;bottom:{bottom}px;height:68px;border-radius:34px;background:{A};opacity:{op};{SHADOW}'
            f'display:flex;align-items:center;gap:12px;padding:0 10px 0 18px;z-index:{10 - depth};">'
            f'{ico(P_BELL, "#fff", 22, 2)}<span style="flex:1;min-width:0;"><span style="display:block;font-size:16px;font-weight:700;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{title}</span>'
            f'<span style="display:block;font-size:12px;color:rgba(255,255,255,.8);white-space:nowrap;">{sub}</span></span>'
            f'<span style="width:48px;height:48px;border-radius:50%;background:rgba(255,255,255,.18);display:flex;align-items:center;justify-content:center;">{ico(P_CLOCK, "#fff", 22, 2)}</span>'
            f'<span style="width:48px;height:48px;border-radius:50%;background:#fff;display:flex;align-items:center;justify-content:center;">{ico(P_CHECK, A, 24, 2.6)}</span></div>')


def note(text, bottom):
    return (f'<div style="position:absolute;left:0;right:0;bottom:{bottom}px;display:flex;justify-content:center;z-index:20;">'
            f'<span style="font-size:12px;font-weight:700;color:{T2};background:rgba(255,255,255,.9);padding:4px 10px;border-radius:10px;">{text}</span></div>')


def callout(text, top):
    return (f'<div style="position:absolute;left:16px;right:16px;top:{top}px;z-index:30;font-size:12.5px;line-height:17px;color:#fff;'
            f'background:rgba(21,22,31,.82);border-radius:12px;padding:8px 12px;">{text}</div>')


def screen_one():
    return (status() + messenger() + strip('Забрать посылку', 'Сегодня, 18:00 · Дом', 20)
            + callout('Часы — на завтра в 18:00 · галочка — выполнено · название — окно задачи · смахнуть вниз — закрыть', 640))


def screen_stack():
    return (status() + messenger()
            + strip('Оплатить интернет', 'Сегодня, 18:00 · Дом', 172)
            + strip('Позвонить маме', 'Сегодня, 18:00 · Входящие', 96)
            + strip('Забрать посылку', 'Сегодня, 18:00 · Дом', 20)
            + note('ещё 2', 250)
            + callout('5 напоминаний в 18:00: 3 полоски друг над другом, остальные — «ещё 2». Убрал одну — на её место встаёт следующая.', 520))


def screen_task():
    """Нажали на название: окно задачи поверх (как из виджета), полоска ушла."""
    tool = lambda p, on=False: (f'<span style="width:46px;height:46px;border-radius:12px;border:1px solid {SEL if on else LINE};background:{SEL if on else "#fff"};'
                                f'display:flex;align-items:center;justify-content:center;">{ico(p, A if on else INK, 20, 2)}</span>')
    sheet = (f'<div style="position:absolute;left:10px;right:10px;top:300px;border-radius:26px;background:#fff;{SHADOW}padding:0 18px 18px;">{handle()}'
             f'<div style="display:flex;align-items:center;gap:12px;margin-top:14px;">{ring(PRI["mid"], 22)}<span style="font-size:22px;font-weight:700;color:{INK};">Забрать посылку</span></div>'
             f'<div style="font-size:15px;color:{T2};margin-top:10px;">Пункт выдачи на Ленина, 12</div>'
             f'<div style="display:flex;gap:8px;margin-top:16px;">'
             f'<span style="width:72px;height:72px;border-radius:16px;background:{SEL};display:flex;flex-direction:column;justify-content:center;padding-left:10px;box-sizing:border-box;">'
             f'<b style="font-size:16px;color:{A};">сб</b><b style="font-size:20px;color:{A};">24</b></span>'
             f'<span style="flex:1"></span></div>'
             f'<div style="display:flex;gap:8px;margin-top:16px;">{tool(P_INBOX)}{tool(P_BELL, True)}{tool(P_CLOCK)}<span style="flex:1"></span>'
             f'<span style="width:50px;height:50px;border-radius:14px;background:{A};display:flex;align-items:center;justify-content:center;">{ico(P_CHECK, "#fff", 24, 2.6)}</span></div></div>')
    return (status() + messenger() + '<div style="position:absolute;inset:0;background:rgba(21,22,31,.38);"></div>' + sheet
            + callout('Нажатие на название полоски открывает окно задачи поверх текущего приложения — как из виджета.', 210))


def screen_settings():
    row = lambda t, s, on: (f'<div style="background:#fff;border-radius:16px;padding:14px 16px;display:flex;align-items:center;gap:12px;margin-top:10px;">'
                            f'<span style="flex:1;"><span style="display:block;font-size:16px;font-weight:700;color:{INK};">{t}</span>'
                            f'<span style="display:block;font-size:13px;color:{T2};margin-top:2px;line-height:18px;">{s}</span></span>'
                            f'<span style="width:48px;height:28px;border-radius:14px;background:{A if on else "#C9D0CC"};position:relative;flex-shrink:0;">'
                            f'<span style="position:absolute;top:3px;{"right" if on else "left"}:3px;width:22px;height:22px;border-radius:50%;background:#fff;"></span></span></div>')
    warn = (f'<div style="background:#FDECEA;border-radius:16px;padding:12px 16px;margin-top:10px;font-size:13px;line-height:18px;color:{OVD};">'
            f'Нет разрешения «Появление поверх других приложений» — полоска не появится, придёт обычное уведомление.'
            f'<div style="margin-top:8px;"><span style="display:inline-block;background:{OVD};color:#fff;font-weight:700;padding:8px 14px;border-radius:10px;">Разрешить</span></div></div>')
    body = (f'<div style="padding:8px 16px;"><div style="font-size:24px;font-weight:700;color:{INK};margin:6px 0 4px;">Настройки</div>'
            f'<div style="font-size:12px;font-weight:700;color:{T2};margin-top:14px;">НАПОМИНАНИЯ</div>'
            + row('Всплывающая полоска', 'Снизу поверх любого приложения, вместо уведомления. Вкл. — откроется разрешение телефона.', True)
            + warn + row('Вибрация', 'Два коротких импульса', True) + '</div>')
    return (status() + body + callout('Включил переключатель — сразу открылась настройка Samsung «Появление поверх других приложений». Без разрешения — красная подсказка и кнопка.', 640))


OVD = '#C62828'
VARIANTS_V = [
    ('1. Одно напоминание', 'Зелёная полоска снизу поверх любого приложения: название, «время · список», «на завтра» и «выполнить». Висит, пока не уберёшь.', screen_one, '#EEF1F5'),
    ('2. Несколько сразу', 'Стопкой до 3 полосок, остальные — «ещё N». Обычного уведомления сверху нет — только полоска и вибрация.', screen_stack, '#EEF1F5'),
    ('3. Нажали на название', 'Окно задачи поверх текущего приложения, как из виджета.', screen_task, '#EEF1F5'),
    ('4. Настройки', 'Переключатель «Всплывающая полоска» и подсказка, если нет разрешения.', screen_settings, BG),
]


def build_v(out):
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    phones = ''.join(f'<div>{lab(t)}{sub(d)}{phone(bg, f())}</div>' for t, d, f, bg in VARIANTS_V)
    W, H = 4 * 428 + 3 * 30 + 80, 1170
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">В. Полоска напоминания — по решениям PM</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;">Телефон заблокирован, нет разрешения или переключатель выключен — приходит обычное уведомление, как сейчас.</div>'
            f'<div style="display:flex;gap:30px;">{phones}</div></div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Полоска напоминания</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(out, 'VariantR2.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(out, 'canvas.json')))
    old = {k: v for k, v in c['boards'].items() if k != 'VariantR2.dc.html'}
    for v in old.values():
        v['y'] += H + 120
    c['boards'] = {'VariantR2.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'В. Полоска напоминания (T-69)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Полоска напоминания (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
