"""Макеты итерации 54: «Отложить» в окне задачи (T-96, вариант PM Б2, новый холст).

Решения PM (07.11.2026):
- В окне задачи слева от «⋮» — кнопка ⏰ «Отложить»; открывает то же окно вариантов, что и кнопка уведомления (итерация 53).
- Работает везде, где открывается окно задачи: виджет (поверх рабочего стола), «Неделя» виджета, приложение. Вид виджета не меняется.
- После выбора окно задачи закрывается, внизу «Отложено на … · Отмена». У задачи без даты и у закрытой кнопки нет.

Запуск: python3 design/mockups/iteration54.py <папка project>   → canvas.json, Main.dc.html
"""
import datetime
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import iteration49 as I  # noqa: E402
import iteration53 as S  # noqa: E402

D, B, svg = I.D, I.B, I.svg


def desktop():
    """Рабочий стол с виджетом «Задачи» (лента «Все»)."""
    rows, bottom = D.widget_body()
    widget = (f'<div style="position:absolute;left:24px;right:24px;top:96px;height:420px;border-radius:var(--radius-xl);background:var(--background);'
              f'overflow:hidden;border:1px solid var(--divider);padding:6px 3px 0">{rows}{bottom}</div>')
    wall = 'background:linear-gradient(160deg,color-mix(in srgb,var(--text-secondary) 55%,var(--background)),color-mix(in srgb,var(--text-primary) 70%,var(--background)))'
    clock = '<div style="position:absolute;left:28px;top:24px;font-size:40px;line-height:44px;font-weight:300;color:var(--on-accent)">16:58</div>'
    return f'<div style="position:absolute;inset:0;{wall}">{clock}{widget}</div>'


SNOOZE_BTN = (f'<span style="width:40px;height:40px;border-radius:50%;display:flex;align-items:center;justify-content:center;color:var(--text-secondary);'
              f'box-shadow:0 0 0 2px var(--accent)">{svg("ic_popup_tomorrow", 24)}</span>')


def task_window():
    """Окно задачи поверх рабочего стола: справа от названия ⏰ «Отложить» (обведена — новая), затем «⋮»."""
    card = B.sheet_card(True, width=396)
    card = (card.replace('Подготовить отчёт', 'Созвон с командой').replace('Квартальный отчёт для руководства', 'Обсудить план на ноябрь')
            .replace('пт, 30 окт', 'ср, 11 ноя').replace('10:00–11:30', '17:00'))
    dots = '<span style="color:var(--text-secondary);display:flex"><svg width="26" height="26"'
    assert dots in card
    card = card.replace(dots, SNOOZE_BTN + dots, 1)
    return f'<div style="position:absolute;left:8px;right:8px;bottom:16px">{card}</div>'


def step_tap():
    return I.phone(desktop())


def step_window():
    return I.phone(desktop() + I.SCRIM + task_window())


def step_snooze():
    dialog = S.window(S.TIMED, 'Отложить', 'Созвон с командой · сегодня, 17:00')
    return I.phone(desktop() + I.SCRIM + task_window() + f'<div style="position:absolute;inset:0;border-radius:28px;background:var(--scrim)"></div>'
                   f'<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center">{dialog}</div>')


def step_done():
    snack = D.snackbar('Отложено на 18:00', 'ic_popup_tomorrow')
    return I.phone(desktop() + f'<div style="position:absolute;left:16px;right:16px;bottom:28px;display:flex;justify-content:center">{snack}</div>')


BOARD = ('Main.dc.html', '«Отложить» в окне задачи', '«Отложить» в окне задачи — итерация 54',
         'Кнопка ⏰ слева от «⋮» в окне задачи — то же окно вариантов, что у уведомления. Работает из виджета, «Недели» виджета и приложения; '
         'вид виджета не меняется. У задачи без даты и у закрытой кнопки нет.',
         [('1. Виджет', 'Нажатие на плашку — окно задачи поверх рабочего стола (как сейчас).', step_tap),
          ('2. Окно задачи', 'Справа от названия — ⏰ «Отложить» (в макете обведена), за ней «⋮».', step_window),
          ('3. Нажали ⏰', 'Окно «Отложить» поверх окна задачи: варианты с новым сроком справа; «Отмена» — назад к задаче.', step_snooze),
          ('4. После выбора', 'Окно задачи закрылось, внизу «Отложено на 18:00 · Отмена» (~4 с). В приложении — так же, подсказка внизу экрана.', step_done)])


def build(out):
    os.makedirs(out, exist_ok=True)
    name, board_title, title, lead, cols = BOARD
    Wd, H, body = I.board(title, lead, cols)
    Path(out, name).write_text(I.page(board_title, Wd, H, body), encoding='utf-8')
    now = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    canvas = {'v': 3, 'createdOnFiles': {'v': 1, 'at': now}, 'title': 'Итерация 54 — Отложить в окне задачи', 'launch': {'view': 'canvas'}, 'pages': [],
              'boards': {name: {'x': 0, 'y': 0, 'w': Wd, 'h': H, 'title': board_title}}, 'order': [name],
              'notes': {'head': {'x': 0, 'y': -300, 'text': '«Отложить» в окне задачи (итерация 54, T-96)', 'kind': 'title1', 'maxW': 1900}},
              'designSystems': []}
    json.dump(canvas, open(os.path.join(out, 'canvas.json'), 'w'), ensure_ascii=False, indent=2)
    print('ok', name, Wd, H)


if __name__ == '__main__':
    build(sys.argv[1])
