"""Макет «Кнопка обновления на виджете» (итерация 32): кнопка слева от «+» и окно по центру экрана с шагами
проверки, загрузки и установки.

Телефон 412×860 (dp 1:1). Окно открывается над рабочим столом, как окно задачи из виджета.
Запуск: python3 update_button.py <папка с canvas.json>
"""
import os, sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from reflib import FACES, P_WARN
from sheetlib import ico, P_CHECK, P_CAL, P_INBOX
from tasks_polish import A, INK, T2, BG, SEL, phone, status, tasks_block

LINE = '#E3E7E4'
SCRIM = 'rgba(21,22,31,.42)'
SHADOW = 'box-shadow:0 18px 50px rgba(21,22,31,.28);'
WALL = 'linear-gradient(165deg,#C5D8F0 0%,#BFD2EE 55%,#9FB8DE 100%)'
P_DL = 'M12 4v10M8 10l4 4 4-4M5 19h14'
P_KEY = 'M14 8a4 4 0 1 1-1.2 2.8L5 18.5V21h2.5v-2H10v-2.5h2.2'


def widget_inner(highlight=False):
    """Виджет на рабочем столе: слева переключатель, справа кнопка обновления и «+»."""
    sw = (f'<span style="height:44px;border-radius:16px;background:#fff;display:flex;align-items:center;padding:0 4px;gap:2px;">'
          f'<span style="width:40px;height:36px;border-radius:12px;background:{SEL};display:flex;align-items:center;justify-content:center;">{ico(P_CAL, A, 20, 2)}</span>'
          f'<span style="width:40px;height:36px;display:flex;align-items:center;justify-content:center;">{ico(P_INBOX, "#6B7280", 20, 2)}</span></span>')
    ring = 'box-shadow:0 0 0 3px rgba(21,128,61,.45);' if highlight else ''
    upd = (f'<span style="width:44px;height:44px;border-radius:15px;background:#fff;{ring}display:flex;align-items:center;justify-content:center;margin-right:8px;">'
           f'{ico(P_DL, "#6B7280", 22, 2)}</span>')
    plus = (f'<span style="width:50px;height:44px;border-radius:15px;background:{A};display:flex;align-items:center;justify-content:center;">'
            f'{ico("M12 5v14M5 12h14", "#fff", 22, 2.4)}</span>')
    bar = (f'<div style="position:absolute;left:0;right:0;bottom:0;height:70px;background:linear-gradient(rgba(246,247,246,0),{BG} 60%);"></div>'
           f'<div style="position:absolute;left:10px;right:10px;bottom:10px;display:flex;align-items:center;">{sw}<span style="flex:1"></span>{upd}{plus}</div>')
    wg = (f'<div style="position:absolute;left:10px;right:10px;top:46px;height:598px;border-radius:30px;overflow:hidden;background:{BG};">'
          f'<div style="padding:12px 0 0 0;">{tasks_block()}</div>{bar}</div>')
    apps = ''.join(f'<span style="width:62px;height:62px;border-radius:19px;background:{c};"></span>' for c in ('#F6D90A', '#2AA8E0', '#D6409F', '#3D6FE0'))
    return status('#fff') + wg + f'<div style="position:absolute;left:0;right:0;bottom:52px;display:flex;justify-content:space-around;padding:0 18px;">{apps}</div>'


def callout(text, right, top, tip):
    return (f'<div style="position:absolute;right:{right}px;top:{top}px;width:236px;z-index:30;font-size:13px;line-height:18px;color:#fff;background:rgba(21,22,31,.88);'
            f'border-radius:12px;padding:9px 12px;">{text}<span style="position:absolute;top:-6px;right:{tip}px;width:12px;height:12px;background:rgba(21,22,31,.88);transform:rotate(45deg);"></span></div>')


def bar(pct=None, indeterminate=False):
    """Полоса хода: pct — доля загрузки, indeterminate — идёт проверка."""
    fill = (f'<span style="position:absolute;left:34%;top:0;bottom:0;width:32%;border-radius:4px;background:{A};"></span>' if indeterminate
            else f'<span style="position:absolute;left:0;top:0;bottom:0;width:{pct}%;border-radius:4px;background:{A};"></span>')
    return f'<div style="position:relative;height:8px;border-radius:4px;background:{LINE};margin-top:16px;">{fill}</div>'


def badge(path, color, bg):
    return f'<span style="width:48px;height:48px;border-radius:50%;background:{bg};display:flex;align-items:center;justify-content:center;margin-bottom:12px;">{ico(path, color, 26, 2.2)}</span>'


def btn(label, primary=False):
    bg, c, br = (A, '#fff', A) if primary else ('#fff', INK, LINE)
    return (f'<span style="flex:1;height:50px;border-radius:16px;background:{bg};border:1px solid {br};box-sizing:border-box;display:flex;align-items:center;'
            f'justify-content:center;font-size:16px;font-weight:700;color:{c};">{label}</span>')


def dialog(title, body, buttons, icon=''):
    return (f'<div style="position:absolute;inset:0;background:{SCRIM};z-index:5;"></div>'
            f'<div style="position:absolute;inset:34px 0 0;z-index:6;display:flex;align-items:center;justify-content:center;">'
            f'<div style="width:364px;border-radius:28px;background:#fff;{SHADOW}overflow:hidden;">'
            f'<div style="padding:24px 24px 8px;">{icon}<div style="font-size:21px;font-weight:700;color:{INK};">{title}</div>{body}</div>'
            f'<div style="display:flex;gap:10px;padding:16px 24px 24px;">{buttons}</div></div></div>')


def txt(s, strong=False, size=15, mt=8):
    return f'<div style="font-size:{size}px;line-height:21px;color:{INK if strong else T2};font-weight:{700 if strong else 500};margin-top:{mt}px;">{s}</div>'


def over(inner_dialog):
    return phone(WALL, widget_inner() + inner_dialog)


def s_button():
    return phone(WALL, widget_inner(highlight=True) + callout('Новая кнопка слева от «+»: нажал — открывается окно обновления', 38, 662, 62))


def s_checking():
    return over(dialog('Обновление', txt('Проверяю наличие обновления…') + bar(indeterminate=True), btn('Закрыть')))


def s_none():
    return over(dialog('Обновлений нет', txt('Установлена последняя версия') + txt('0.31.2 (сборка 127)', True, mt=2), btn('Закрыть'), badge(P_CHECK, A, SEL)))


def s_found():
    return over(dialog('Доступно обновление', txt('Новая версия', mt=10) + txt('0.32.0 (сборка 130)', True, 18, 2)
                       + txt('Сейчас: 0.31.2 (сборка 127) · около 16 МБ', size=13, mt=10), btn('Закрыть') + btn('Загрузить', True), badge(P_DL, A, SEL)))


def s_downloading():
    return over(dialog('Загружаю обновление', txt('Версия 0.32.0 (сборка 130)', mt=8) + bar(45)
                       + f'<div style="display:flex;justify-content:space-between;font-size:13px;color:{T2};font-weight:600;margin-top:8px;"><span>7,2 из 16 МБ</span><span>45%</span></div>', btn('Отмена')))


def s_ready():
    return over(dialog('Обновление загружено', txt('Установить версию 0.32.0 сейчас? Приложение перезапустится, задачи и настройки сохранятся.', mt=10),
                       btn('Не сейчас') + btn('Обновить', True), badge(P_CHECK, A, SEL)))


def s_error():
    return over(dialog('Не удалось проверить', txt('Проверьте интернет и попробуйте ещё раз.', mt=10), btn('Закрыть') + btn('Повторить', True), badge(P_WARN, '#C62828', '#FDECEA')))


def s_permission():
    return over(dialog('Нужно разрешение', txt('Чтобы установить обновление, разрешите приложению установку. Это один раз: откроется настройка телефона.', mt=10),
                       btn('Закрыть') + btn('Разрешить', True), badge(P_KEY, A, SEL)))


SCREENS = [
    ('1. Кнопка на виджете', 'Маленькая тихая кнопка в нижней строке, слева от «+». Остальной виджет без изменений.', s_button),
    ('2. Проверка', 'Нажал кнопку — над рабочим столом открывается окно по центру. Идёт проверка, «Закрыть» отменяет её.', s_checking),
    ('3. Обновления нет', 'Установлена последняя версия, показан её номер. Одна кнопка «Закрыть».', s_none),
    ('4. Обновление найдено', 'Номер новой версии и размер. «Загрузить» — начать, «Закрыть» — отложить.', s_found),
    ('5. Загрузка', 'Полоса и проценты прямо в окне. «Отмена» останавливает загрузку.', s_downloading),
    ('6. Загружено', 'Обновить сейчас или не сейчас. После «Обновить» Android может показать своё окно «Обновить приложение?» — ещё одно нажатие.', s_ready),
    ('7. Ошибка', 'Нет интернета или GitHub не ответил: «Повторить» запускает проверку заново.', s_error),
    ('8. Нужно разрешение', 'Если телефон ещё не разрешил установку из приложения: «Разрешить» открывает настройку, это один раз.', s_permission),
]

if __name__ == '__main__':
    OUT = sys.argv[1]
    lab = lambda s: f'<div style="font-size:17px;font-weight:700;color:{INK};margin-bottom:4px;">{s}</div>'
    sub = lambda s: f'<div style="font-size:13px;color:{T2};line-height:18px;height:56px;width:428px;margin-bottom:10px;">{s}</div>'
    cell = lambda t, d, f: f'<div>{lab(t)}{sub(d)}{f()}</div>'
    rows = ''.join(f'<div style="display:flex;gap:30px;margin-bottom:34px;">{"".join(cell(*s) for s in SCREENS[i:i + 4])}</div>' for i in (0, 4))
    W, H = 4 * 428 + 3 * 30 + 80, 2130
    body = (f'<div style="width:{W}px;height:{H}px;box-sizing:border-box;background:#EEF0EE;padding:36px 40px;font-family:Manrope,sans-serif;">'
            f'<div style="font-size:28px;font-weight:700;color:{INK};letter-spacing:-.5px;">Кнопка обновления на виджете</div>'
            f'<div style="font-size:15px;color:{T2};margin:6px 0 22px;line-height:21px;">Нажал кнопку — окно по центру: проверка → найдено → загрузка → «Обновить». '
            f'Окно открывается поверх рабочего стола, приложение запускать не нужно.</div>{rows}</div>')
    html = f'''<!doctype html>
<html lang="ru"><head><meta charset="utf-8"><title>Кнопка обновления</title><script src="./support.js"></script></head>
<body><x-dc><helmet><style>{FACES}body{{margin:0}}</style></helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{W},"height":{H}}}}}'>
class Component extends DCLogic {{ renderVals() {{ return {{}}; }} }}
</script></body></html>'''
    open(os.path.join(OUT, 'VariantU1.dc.html'), 'w').write(html)
    c = json.load(open(os.path.join(OUT, 'canvas.json')))
    fresh = 'VariantU1.dc.html' not in c['boards']
    old = {k: v for k, v in c['boards'].items() if k != 'VariantU1.dc.html'}
    if fresh:  # повторный запуск не сдвигает прежние доски ещё раз
        for v in old.values():
            v['y'] += H + 120
    c['boards'] = {'VariantU1.dc.html': {'x': 0, 'y': 0, 'w': W, 'h': H, 'title': 'Кнопка обновления на виджете (итерация 32)'}, **old}
    c['order'] = list(c['boards'])
    c['notes']['head'] = {'x': 0, 'y': -300, 'text': 'Кнопка обновления на виджете (сверху) · ниже — прежние макеты', 'kind': 'title1', 'maxW': 1900}
    json.dump(c, open(os.path.join(OUT, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
