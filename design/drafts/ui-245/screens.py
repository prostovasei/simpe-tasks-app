"""Плашки операций #245 — макеты на согласование PM (15.11.2026): значок категории, место, счёт, типы операций, итог дня.
Основа — стиль 217/233/241. Данные вымышленные. Запуск из корня: python3 design/drafts/ui-245/screens.py → screens.html; PNG: node design/drafts/ui-245/render.js."""
from pathlib import Path

here = Path(__file__).parent
p241 = here.parent.parent / 'approved' / '241' / 'screens.py'
src = p241.read_text()
ns = {'__file__': str(p241)}
exec(src[:src.index('IMG = {')], ns)
ns['here'] = here
exec(src[src.index('def gesture'):src.index('FIN_OPS = ')], ns)
fig, ico, service, phone, g217, G, extra233 = ns['fig'], ns['ico'], ns['service'], ns['phone'], ns['g217'], ns['G'], ns['extra233']
e241 = src.index("\nextra = '''\n.v-screen") + len("\nextra = '''")
extra241 = src[e241:src.index("'''", e241)]

CARD, CASH = '💳 Карта', '👛 Наличные'


def row(emoji, title, sub, amount, kind='expense', comment=False, q=False):
    """kind: expense — «−» чёрным, income — «+» зелёным, neutral — серым (перевод, корректировка)."""
    icon = f'<span class="oe q">?</span>' if q else f'<span class="oe">{emoji}</span>'
    note = '<i class="cm">💬</i>' if comment else ''
    return f'<div class="op2 {kind}">{icon}<span class="ot">{title}<small>{sub}</small></span><span class="oa">{note}{amount}</span></div>'


def pin(place):
    return f'📍 {place} · '


def day(title, total, rows):
    return f'<div class="group-date dd"><span>{title}</span><span class="sum">{total}</span></div><div class="card">{"".join(rows)}</div>'


MAIN = (day('14 ноября · вчера', '− 4 900 ₽', [
    row('🏃', 'Спорт', pin('Фитнес-клуб') + CARD, '− 3 000 ₽'),
    row('🎭', 'Отдых', CARD, '− 400 ₽', comment=True),
    row('🍽', 'Еда', pin('Пятёрочка') + CARD, '− 400 ₽'),
    row('⇄', f'{CARD} → {CASH}', 'Перевод', '5 000 ₽', 'neutral'),
    row('', 'Без категории', CARD, '− 700 ₽', q=True),
    row('🚕', 'Такси', pin('Яндекс Go') + CARD, '− 400 ₽'),
]) + day('13 ноября', '− 1 400 ₽', [
    row('💼', 'Зарплата', CARD, '+ 65 000 ₽', 'income'),
    row('🍽', 'Еда', pin('Магнит') + CASH, '− 1 400 ₽'),
    row('⚖', CASH, 'Корректировка', '+ 150 ₽', 'neutral'),
]))

CATALOG = ('<div class="card">' + ''.join([
    row('🍽', 'Еда', pin('Пятёрочка') + CARD, '− 850 ₽'),
    row('🍽', 'Еда', CARD, '− 850 ₽'),
    row('🎭', 'Отдых', pin('Кино') + CARD, '− 400 ₽', comment=True),
    row('💼', 'Зарплата', CARD, '+ 65 000 ₽', 'income'),
    row('⇄', f'{CARD} → {CASH}', 'Перевод', '5 000 ₽', 'neutral'),
    row('⚖', CASH, 'Корректировка', '− 150 ₽', 'neutral'),
    row('', 'Без категории', CARD, '− 680 ₽', q=True),
    row('🛒', 'Продукты на неделю для всей семьи', pin('Большой гипермаркет у дома') + CARD, '− 12 480 ₽'),
]) + '</div>')

TRASH = service('Корзина', '<div class="card">' + ''.join([
    row('🎭', 'Отдых', 'удалено сегодня · 9 ноября · ' + CARD, '− 620 ₽'),
    row('⇄', f'{CARD} → {CASH}', 'Перевод · 7 ноября', '5 000 ₽', 'neutral'),
    row('🍽', 'Еда', pin('Пятёрочка') + '5 ноября · ' + CARD, '− 1 350 ₽'),
]).replace('</div>', f'<button class="mini">{ico("restore")}</button><button class="mini">{ico("trash")}</button></div>') + '</div>'
    '<p class="dialog-note pad">Операции в корзине не влияют на остатки и аналитику. Сами не удаляются — только вручную.</p>')


def pending(amount, when, mark=''):
    m = f'<span class="mark">{mark}</span>' if mark else ''
    return ('<div class="u-item">' + row('', 'Без категории', f'Яндекс Банк · {CARD} · {when}', amount, q=True) + m
            + '<div class="u-actions"><button class="secondary">Пропустить</button><button class="secondary">Изменить</button><button class="primary">Записать</button></div></div>')


UNSORTED = service('Не разобрано', '<div class="card">' + pending('− 680 ₽', 'сегодня, 14:32')
                   + pending('− 1 240 ₽', 'сегодня, 12:05', 'Похоже, уже записано: «Продукты», −1 240 ₽ сегодня') + pending('− 350 ₽', 'вчера, 19:47') + '</div>')

s1 = [
    fig('1.1 Операции', 'Слева эмодзи категории, справа сумма; серая строка — «📍 место · 💳 счёт». Расход «−» чёрным, доход «+» зелёным, перевод и корректировка серым. Итог дня — расходы, серым справа.', phone(MAIN, 'finance', 1, 'O')),
    fig('1.2 Все виды плашек', 'Сверху вниз: расход с местом и счётом; без места; с комментарием «💬»; доход; перевод со значком «⇄»; корректировка со значком «⚖»; без категории («?»); длинные названия обрезаются с «…».', f'<div class="phone"><div class="screen v-screen"><main class="content">{CATALOG}</main></div></div>'),
]
s2 = [
    fig('2.1 Корзина', 'Те же плашки, справа «Вернуть» и «Удалить навсегда». Подпись про удаление добавлена в строку с деталями.', TRASH),
    fig('2.2 Не разобрано', 'Покупка из уведомления банка: «?» и «Без категории», в строке — банк, счёт и время. Три кнопки внизу как раньше.', UNSORTED),
]

extra = '''
.op2{display:flex;align-items:center;gap:12px;padding:13px 16px;border-bottom:1px solid #ececef;min-height:56px}
.op2:last-child{border-bottom:0}
.op2 .oe{width:28px;text-align:center;font-size:20px;flex-shrink:0}
.op2 .oe.q{width:26px;height:26px;margin:0 1px;border-radius:50%;background:#e4e4e8;color:#8a8a92;font-size:13px;font-weight:700;display:grid;place-items:center}
.op2 .ot{flex:1;min-width:0;display:flex;flex-direction:column;font-size:15px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.op2 .ot small{font-size:10px;color:var(--secondary);margin-top:3px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.op2 .oa{font-size:15px;font-weight:700;white-space:nowrap;display:flex;align-items:center;gap:5px}
.op2 .cm{font-style:normal;font-size:11px}
.op2.income .oa{color:#1f8f4e}
.op2.neutral .oa,.op2.neutral .ot{color:var(--secondary)}.op2.neutral .oa{font-weight:400}.op2.neutral .oe{opacity:.75}
.group-date.dd{display:flex;justify-content:space-between;align-items:baseline}.group-date .sum{font-size:11px;color:var(--secondary);font-weight:400}
.u-item .op2{padding:0;border:0;min-height:0}.u-item .mark{display:block;font-size:10px;color:var(--secondary);margin:6px 0 0 40px}
.op2 .mini{margin-left:0}
'''
secs = [('1. Операции', 'Главная страница «Операции» и все виды плашек.', s1), ('2. Корзина и «Не разобрано»', 'Те же плашки на других экранах.', s2)]
html = (f'<!doctype html><html lang="ru"><meta charset="utf-8"><title>Плашки операций (#245)</title><style>{g217["style"]}{G["extra217"]}{extra233}{extra241}{extra}</style>'
        '<div class="board">' + ''.join(f'<section class="sec" id="s{i+1}"><h2>{t}</h2><p>{d}</p><div class="rowp">{"".join(f)}</div></section>' for i, (t, d, f) in enumerate(secs)) + '</div></html>')
(here / 'screens.html').write_text(html)
print('screens.html готов; картинки: node design/drafts/ui-245/render.js')
