"""Макеты итерации 50: перенос одного повтора и зона нажатия кружка в «Неделе» виджета (новый холст).

Решения PM (07.11.2026): при смене даты или времени повторяющейся — окно «Изменить повторяющуюся задачу» (как в TickTick):
«Текущий повтор» / «Все повторы с этого момента» / «Отмена». «Текущий повтор» (вариант А) — только у ближайшего раза: раз становится
обычной задачей на новую дату, серия едет на следующий раз; у будущего раза он бледный с подсказкой. Окно — по «Готово» в окне даты и
при снятии времени крестиком; дата в названии и крестик даты — без окна. «На завтра» в уведомлении и полоске — только этот раз.
Виджет «Неделя»: зона нажатия кружка ~40 dp поверх начала названия, кружок и название не двигаются.

Запуск: python3 design/mockups/iteration50.py <папка project>   → canvas.json, Main.dc.html, Widget.dc.html
"""
import datetime
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import iteration49 as I  # noqa: E402
import week_tune as W  # noqa: E402

D, B, svg = I.D, I.B, I.svg
frow, day_header = D.frow, D.day_header
W_PHONE, H_PHONE = I.W_PHONE, I.H_PHONE


# ------------------------------------------------------------------ перенос повтора

def card_moved(date='пн, 9 ноя', time='18:00', banner=''):
    return I.task_card(date, banner).replace('>14:30<', f'>{time}<')


def change_dialog(future=False):
    """Окно «Изменить повторяющуюся задачу»: у будущего раза «Текущий повтор» бледный, под ним подсказка."""
    def b(t, style, dim=False):
        css = {
            'primary': 'background:var(--accent);color:var(--on-accent)',
            'outline': 'border:1px solid var(--divider);color:var(--text-primary)',
            'plain': 'color:var(--text-primary)',
        }[style]
        return (f'<span style="min-height:50px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;font-size:15px;'
                f'font-weight:700;padding:0 12px;{css};opacity:{.35 if dim else 1}">{t}</span>')
    hint = ('<div style="font-size:12px;line-height:16px;color:var(--text-secondary);text-align:center;margin:-2px 0 2px">'
            'Сначала закройте ближайший повтор — вс, 08 ноя</div>') if future else ''
    when = 'вс, 22 ноя → пн, 23 ноя 18:00' if future else 'вс, 08 ноя → пн, 09 ноя 18:00'
    first = b('Текущий повтор', 'outline' if future else 'primary', dim=future)
    rest = b('Все повторы с этого момента', 'primary' if future else 'outline')
    return (f'<div style="width:356px;border-radius:var(--radius-nav);background:var(--surface);overflow:hidden">'
            f'<div style="padding:24px 24px 8px"><div style="font-size:22px;line-height:28px;font-weight:700">Изменить повторяющуюся задачу</div>'
            f'<div style="font-size:14px;line-height:20px;color:var(--text-secondary);margin-top:8px">«Стирка»: {when}. Что перенести?</div></div>'
            f'<div style="display:flex;flex-direction:column;gap:8px;padding:16px">{first}{hint}{rest}{b("Отмена", "plain")}</div></div>')


def sheet_screen():
    return I.phone(I.feed_before() + I.SCRIM + card_moved())


def dialog_screen():
    return I.centered(I.feed_before() + I.SCRIM + card_moved(), change_dialog())


def after_screen():
    rows = (day_header('ПТ, 6 НОЯ', 'Сегодня') + frow('22:00', 'var(--inactive)', 'Теннис', top=False, bottom=False)
            + day_header('ВС, 8 НОЯ') + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, top=False, bottom=False)
            + day_header('ПН, 9 НОЯ') + frow('18:00', 'var(--inactive)', 'Стирка', top=False, bottom=False)
            + day_header('ВС, 15 НОЯ') + frow('14:30', 'var(--inactive)', 'Стирка', rep=True, top=False) + frow('16:00', 'var(--inactive)', 'Уборка', rep=True, bottom=False))
    return I.phone(I.feed(rows) + I.snack('Перенесено на пн, 09 ноя', svg('ic_widget_all', 22)))


def future_screen():
    banner = I.future_banner().replace('вс, 8 ноя', 'вс, 08 ноя')
    return I.centered(I.feed_before() + I.SCRIM + card_moved('пн, 23 ноя', banner=banner), change_dialog(future=True))


# ------------------------------------------------------------------ зона кружка в виджете

def zone_cell(width_zone, title):
    """Ячейка «СБ 7» крупно: пунктир — зона нажатия «выполнить» у каждой строки."""
    P = W.AFTER
    row_h = 150
    wd, num, tasks = W.WEEK[5]
    head_px = round(W.sp(P['head_fs']) * 1.17, 1)
    slot_h = (row_h - P['pad_t'] - 2 - head_px - W.CAP * P['gap']) / W.CAP
    slots = ''
    for t in tasks:
        s = W.slot(t, P, 'C', slot_h)
        zone = (f'<span style="position:absolute;left:-1px;top:{P["gap"] - 1}px;height:{slot_h + 2}px;width:{width_zone}px;border:1.5px dashed #8E4EC6;'
                f'background:rgba(142,78,198,.14);border-radius:6px;box-sizing:border-box"></span>')
        slots += f'<div style="position:relative">{s}{zone}</div>'
    cell = (f'<div style="width:{W.WIDGET_W // 2}px;height:{row_h}px;box-sizing:border-box;padding:{P["pad_t"]}px 4px 2px 4px;background:{W.BG};'
            f'border-radius:10px;font-family:{W.FONT};overflow:hidden">{W.head(wd, num, P)}{slots}</div>')
    return (f'<div><div style="font-size:15px;font-weight:700;margin-bottom:8px">{title}</div>'
            f'<div style="width:{W.WIDGET_W // 2 * 2}px;height:{row_h * 2}px"><div style="transform:scale(2);transform-origin:0 0">{cell}</div></div></div>')


def widget_body():
    notes = '''<div style="width:460px;display:flex;flex-direction:column;gap:12px;font-size:14px;line-height:20px">
<div style="background:var(--surface);border-radius:var(--radius-lg);padding:14px 16px"><b>Кружок и название — на месте.</b> Поверх начала строки — невидимая зона ~40 dp
на всю высоту плашки: кружок + первые 2–3 буквы. Нажатие на неё — «выполнить» (у выполненной — вернуть), остальная плашка открывает задачу.</div>
<div style="background:var(--surface);border-radius:var(--radius-lg);padding:14px 16px"><b>Случайное нажатие не страшно:</b> выполненная задача остаётся в ячейке
до полуночи серой и зачёркнутой, повторное нажатие на ту же зону возвращает её. У будущего повтора зоны нет — как и кружка.</div>
<div style="background:var(--surface);border-radius:var(--radius-lg);padding:14px 16px"><b>«На завтра» в уведомлении и полоске</b> у повторяющейся задачи теперь
переносит только этот раз (обычной задачей на завтра, в то же время), серия остаётся на своём расписании. Окна нет — в уведомлении спрашивать нельзя.</div>
</div>'''
    return (f'<div style="display:flex;gap:48px;align-items:flex-start">{zone_cell(18, "Было: зона 18 dp — только кружок")}'
            f'{zone_cell(40, "Стало: зона ~40 dp — кружок и начало названия")}{notes}</div>')


BOARDS = [
    ('Main.dc.html', 'Перенос одного повтора', 'Перенос даты повтора — итерация 50',
     'Новая дата в окне даты → «Готово» → окно «Изменить повторяющуюся задачу». «←» в окне даты, как и раньше, отменяет выбор без вопросов.',
     [('1. Выбрал новую дату', 'В окне даты выбран пн, 9 ноя, 18:00 и нажата галочка «Готово» — на поле уже новая дата, но решение ещё не принято.', sheet_screen),
      ('2. Окно выбора', '«Текущий повтор» (чёрная) — перенести только этот раз; «Все повторы с этого момента» — как сейчас, сдвинуть серию; «Отмена» — вернуть дату.', dialog_screen),
      ('3. «Текущий повтор»', 'Раз «Стирка» на пн 9 ноя 18:00 — обычная задача, без знака повтора. Серия — со следующего раза вс 15 ноя. Внизу «Отмена».', after_screen),
      ('4. Будущий повтор', '«Текущий повтор» бледный, под ним подсказка. «Все повторы с этого момента» делит серию: до этого раза — по-старому, с него — по-новому.', future_screen)]),
]


if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    boards, order, y = {}, [], 0
    for name, board_title, title, lead, cols in BOARDS:
        Wd, H, body = I.board(title, lead, cols)
        Path(out, name).write_text(I.page(board_title, Wd, H, body), encoding='utf-8')
        boards[name] = {'x': 0, 'y': y, 'w': Wd, 'h': H, 'title': board_title}
        order.append(name)
        y += H + 120
    Wd, H = 1500, 520
    body = (f'<div style="font-size:28px;font-weight:800;letter-spacing:-.4px">Виджет «Неделя»: зона кружка</div>'
            f'<div style="font-size:15px;color:var(--text-secondary);margin:6px 0 22px;line-height:21px">Пунктир — где нажатие выполняет задачу. Ячейка «СБ 7» в два раза крупнее.</div>'
            + widget_body())
    Path(out, 'Widget.dc.html').write_text(I.page('Зона кружка в виджете', Wd, H, body), encoding='utf-8')
    boards['Widget.dc.html'] = {'x': 0, 'y': y, 'w': Wd, 'h': H, 'title': 'Зона кружка в виджете и «На завтра»'}
    order.append('Widget.dc.html')
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Итерация 50 — перенос повтора', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': boards, 'order': order,
              'notes': {'head': {'x': 0, 'y': -300, 'text': 'Перенос одного повтора и зона кружка (итерация 50)', 'kind': 'title1', 'maxW': 1900}},
              'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', [(n, b['w'], b['h']) for n, b in boards.items()])
