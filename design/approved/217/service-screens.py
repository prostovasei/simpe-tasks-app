"""Финансы #217 — согласованный PM 10.11.2026 комплект экранов на основе compact-module-bar.html.
Запуск: python3 service-screens.py  → service-screens.html; затем node service-render.js → service-N.png (нужен Playwright)."""
import re
from pathlib import Path

root = Path(__file__).parent
approved = (root / 'compact-module-bar.html').read_text()
style = re.search(r'<style>(.*?)</style>', approved, re.S).group(1)

P = {
 'plus': '<path d="M12 4v16M4 12h16"/>', 'left': '<path d="m14 6-6 6 6 6"/>', 'right': '<path d="m10 6 6 6-6 6"/>',
 'down': '<path d="m6 10 6 6 6-6"/>',
 'settings': '<path d="m9 3-.6 2.6-2 .9L4 5.5 2 9l2 1.7v2.6L2 15l2 3.5 2.4-1 2 .9L9 21h6l.6-2.6 2-.9 2.4 1 2-3.5-2-1.7v-2.6L22 9l-2-3.5-2.4 1-2-.9L15 3z"/><circle cx="12" cy="12" r="3"/>',
 'chart': '<path d="M4 20V5M4 20h17M9 16v-5M14 16V7M19 16v-3"/>', 'history': '<path d="M5 4h14v16H5zM8 8h8M8 12h8M8 16h5"/>',
 'category': '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
 'trash': '<path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14M10 11v6M14 11v6"/>',
 'backup': '<path d="M4 16v5h16v-5M12 3v13M7 11l5 5 5-5"/>', 'upload': '<path d="M4 16v5h16v-5M12 16V3M7 8l5-5 5 5"/>',
 'restore': '<path d="M4 12a8 8 0 1 0 2.6-5.9M4 4v5h5"/>', 'archive': '<path d="M4 8h16v13H4zM3 3h18v5H3zM9 12h6"/>',
 'wallet': '<rect x="3" y="6" width="18" height="14" rx="3"/><path d="M3 9V5a2 2 0 0 1 2-2h12v3M16 12h5v5h-5z"/>',
 'search': '<circle cx="10" cy="10" r="7"/><path d="m15 15 6 6"/>', 'folder': '<path d="M3 5h6l2 2h10v14H3zM9 14h6m-3-3v6"/>',
 'week': '<path d="M3 4h18v16H3zM9 4v16m6-16v16"/>',
}
ico = lambda n: f'<svg viewBox="0 0 24 24" aria-hidden="true">{P[n]}</svg>'
TASK = '<svg viewBox="0 0 24 24"><path d="m3 6 1.5 1.5L7 4M10 6h11M3 12l1.5 1.5L7 10M10 12h11M3 18l1.5 1.5L7 16M10 18h11"/></svg>'
COIN = '<svg class="coin" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><text x="12" y="16.6" text-anchor="middle">₽</text></svg>'
money = lambda n: f'{n:,}'.replace(',', ' ') + ' ₽'

def top(module='finance'):
    s = lambda m: ' class="selected"' if m == module else ''
    gear = f'<button class="module-settings">{ico("settings")}</button>' if module == 'finance' else ''
    return f'<div class="module-top"><nav class="module-bar"><button{s("tasks")}>{TASK}</button><button{s("finance")}>{COIN}</button></nav>{gear}</div>'

def footer(tab):
    a = lambda t: 'nav active' if t == tab else 'nav'
    return (f'<nav class="footer finance-footer"><button class="{a("analytics")}">{ico("chart")}</button><button class="add">{ico("plus")}</button>'
            f'<button class="{a("operations")}">{ico("history")}</button></nav><div class="gesture"><span></span></div>')

def screen(body, tab='analytics', overlay='', small=False):
    return f'<div class="phone{" small" if small else ""}"><div class="screen">{top()}<main class="content">{body}</main>{footer(tab)}</div>{overlay}</div>'

def service(title, body, action='', overlay=''):
    """Служебный экран финансов: стрелка назад, название, действие справа; без нижней панели."""
    return (f'<div class="phone"><div class="screen"><div class="svc-head"><button class="icon-button">{ico("left")}</button><h1>{title}</h1>{action}</div>'
            f'<main class="content">{body}</main><div class="gesture plain"><span></span></div></div>{overlay}</div>')

ACC = [('💳', 'Карта', 42300), ('👛', 'Наличные', 6150), ('🏦', 'Накопления', 80000)]
def accounts(items=ACC, total=128450):
    tiles = ''.join(f'<button class="account-tile"><span class="account-title"><span>{e}</span>{n}</span><strong>{money(v)}</strong></button>' for e, n, v in items)
    return (f'<section class="compact-accounts"><div class="accounts-heading"><span>Счета <span class="accounts-total">· {money(total)}</span></span>'
            f'<button>{ico("plus")}</button></div><div class="account-tiles">{tiles}</div></section>')

def month(name='Ноябрь 2026'):
    return f'<div class="month"><button>{ico("left")}</button><strong>{name}</strong><button>{ico("right")}</button></div>'

def cat(n, v, foot, pct):
    return (f'<div class="cat"><div class="row"><span>{n}</span><strong>{money(v)}</strong></div><div class="track"><span style="width:{pct}%"></span></div>'
            f'<div class="cat-foot"><span>{foot}</span><span>{pct}%</span></div></div>')

def analytics(accs=None, zero=False):
    if zero:
        summ = ('<section class="summary"><span class="small muted">Расходы · 1–10 ноября</span><div class="total">42 800 ₽</div>'
                '<span class="delta">+3 600 ₽ · +9,2%</span><div class="summary-note">К 1–10 октября · было 39 200 ₽</div></section>')
        cats = cat('Продукты', 21000, '+3 000 ₽ · +16,7%', 49) + cat('Кафе', 10000, '−1 000 ₽ · −9,1%', 23) + \
               cat('Подарки', 6800, '+6 800 ₽ · в октябре не было', 16) + cat('Без категории', 5000, '+1 000 ₽ · +25,0%', 12)
    else:
        summ = ('<section class="summary"><span class="small muted">Расходы · 1–10 ноября</span><div class="total">42 800 ₽</div>'
                '<span class="delta">+3 600 ₽ · +9,2%</span><div class="summary-note">К 1–10 октября · было 39 200 ₽</div></section>')
        cats = cat('Продукты', 21000, '+3 000 ₽ · +16,7%', 49) + cat('Кафе', 10000, '−1 000 ₽ · −9,1%', 23) + \
               cat('Транспорт', 6800, '+600 ₽ · +9,7%', 16) + cat('Дом', 5000, '+1 000 ₽ · +25,0%', 12)
    return (accs or accounts()) + month() + summ + '<div class="section-title">По категориям <span>доля расходов</span></div><div class="card">' + cats + '</div>'

def op(label, value, kind='expense', sub=''):
    subs = f'<span class="op-sub">{sub}</span>' if sub else ''
    return f'<button class="operation {kind}"><span>{label}{subs}</span><strong>{value}</strong></button>'

OPS = ('<div class="group-date">10 ноября · сегодня</div><div class="card">' + op('Продукты', '− 850 ₽') + op('Зарплата', '+ 65 000 ₽', 'income')
       + op('Карта → Наличные', '5 000 ₽', 'neutral', 'Перевод') + '</div>'
       '<div class="group-date">9 ноября · вчера</div><div class="card">' + op('Кафе', '− 620 ₽') + op('Транспорт', '− 340 ₽')
       + op('Наличные', '+ 150 ₽', 'neutral', 'Корректировка остатка') + op('Без категории', '− 1 200 ₽') + '</div>'
       '<div class="group-date">8 ноября</div><div class="card">' + op('Дом', '− 2 400 ₽') + op('Продукты', '− 1 350 ₽') + op('Возвраты', '+ 990 ₽', 'income') + '</div>')

def val(text):
    return f'<span class="val">{text}{ico("down")}</span>'
def field(label, value, cls=''):
    return f'<div class="field {cls}"><span class="field-label">{label}</span>{value}</div>'
def text(v, placeholder='Необязательно'):
    return f'<span class="txt">{v}</span>' if v else f'<span class="txt ph">{placeholder}</span>'

def dialog(title, body, actions='', head_extra='', cls='', kb=False):
    return (f'<div class="overlay {"has-keyboard" if kb else ""}"><section class="dialog {cls}"><div class="dialog-head"><h2>{title}</h2>'
            f'<span class="head-btns">{head_extra}<button class="close">×</button></span></div>{body}'
            f'{f"""<div class="form-actions">{actions}</div>""" if actions else ""}</section></div>{keyboard() if kb else ""}')

def keyboard():
    keys = ''.join(f'<button class="key">{k}</button>' for k in ['1', '2', '3', '4', '5', '6', '7', '8', '9', ',', '0', '⌫'])
    return f'<div class="keyboard"><div class="key-top"><span>Цифровая клавиатура</span><span>Скрыть ⌄</span></div><div class="keygrid">{keys}</div><div class="gesture"><span></span></div></div>'

def form(kind='expense', amount='', edit=False, kb=False, cats=None, active='Продукты', place='', comment='', date='10.11.2026 · сегодня', account='💳 Карта'):
    tabs = '' if edit else '<div class="tabs kind-tabs">' + ''.join(
        f'<button class="{"selected" if k == kind else ""}">{t}</button>' for k, t in [('expense', 'Расход'), ('income', 'Доход'), ('transfer', 'Перевод')]) + '</div>'
    am = f'<div class="money"><span class="amount {"" if amount else "ph"}">{amount or "0"}</span><span>₽</span></div>'
    if kind == 'transfer':
        body = am + field('Со счёта', val(account)) + field('На счёт', val('👛 Наличные')) + field('Дата', val(date)) + field('Комментарий', text(comment), 'optional')
    else:
        if cats is None:
            cats = ['Зарплата', 'Возвраты', 'Подарки', 'Продажи', 'Другое'] if kind == 'income' else ['Продукты', 'Кафе', 'Транспорт', 'Дом', 'Здоровье']
        if cats:
            chips = ''.join(f'<button class="chip {"active" if c == active else ""}">{c}</button>' for c in cats) + '<button class="chip all-chip">Все категории</button>'
            catblock = f'<div class="category-label">Категория · необязательно</div><div class="chips">{chips}</div>'
        else:
            catblock = ('<div class="category-label">Категория · необязательно</div><div class="chips one"><button class="chip all-chip">＋ Создать категорию</button></div>'
                        '<p class="dialog-note tight">Категорий пока нет. Операцию можно сохранить и без категории.</p>')
        body = am + field('Счёт', val(account)) + catblock + field('Место', text(place), 'optional') + field('Комментарий', text(comment), 'optional') + field('Дата', val(date))
    titles = {'expense': 'Расход', 'income': 'Доход', 'transfer': 'Перевод'}
    title = titles[kind] if edit else 'Новая операция'
    extra = f'<button class="head-ico">{ico("trash")}</button>' if edit else ''
    return dialog(title, tabs + f'<div class="form-scroll">{body}</div>', '<button class="primary">Сохранить</button>', extra, kb=kb)

def rows(items):
    return '<div class="tools">' + ''.join(f'<button>{ico(i)}<span class="t">{t}<small>{s}</small></span></button>' for i, t, s in items) + '</div>'

def fig(title, note, html, w=390):
    return f'<figure style="width:{w}px">{html}<figcaption><b>{title}</b>{note}</figcaption></figure>'

# ---------- 1. Операции и формы ----------
s1 = [
 fig('1.1 Операции', 'Расход — «−», доход — «+». Перевод и корректировка серые, с подписью: в расходы и доходы не входят. Нажатие открывает окно правки.', screen(OPS, 'operations')),
 fig('1.2 Новый расход', '«+» сразу открывает расход. Пять последних категорий и «Все категории». Дата внизу — меняется редко.', screen(analytics(), overlay=form(amount='850', place='Пятёрочка'))),
 fig('1.3 Доход', 'Свой список категорий. При переключении типа сумма, дата и комментарий остаются; категория и место сбрасываются.', screen(analytics(), overlay=form('income', amount='65 000', active='Зарплата'))),
 fig('1.4 Перевод', 'Сумма, со счёта, на счёт, дата, комментарий. Одинаковые счета выбрать нельзя.', screen(analytics(), overlay=form('transfer', amount='5 000'))),
 fig('1.5 Правка операции', 'То же окно без переключателя типа; корзина — вверху. После «Сохранить» окно остаётся и очищается для новой операции.', screen(OPS, 'operations', overlay=form(amount='850', edit=True, place='Пятёрочка', date='09.11.2026 · вчера'))),
 fig('1.6 Клавиатура на маленьком экране (360×640)', 'Окно сжимается над клавиатурой, поля прокручиваются, «Сохранить» всегда видна.', screen(analytics(), overlay=form(amount='850', kb=True), small=True), 360),
]

# ---------- 2. Первый запуск и счета ----------
empty = f'<div class="empty">{ico("wallet")}<h2>Начнём со счёта</h2><p>Добавь наличные, карту или накопления<br>и укажи, сколько на них сейчас.</p><button class="primary">{ico("plus")}Создать счёт</button></div>'
new_acc = dialog('Новый счёт', '<div class="form-scroll">' + field('Название', text('Карта')) + field('Тип', val('Карта')) + field('Эмодзи', val('💳'))
                 + '<div class="category-label">Остаток сейчас</div><div class="money"><span class="amount">42 300</span><span>₽</span></div>'
                 '<p class="dialog-note">Это деньги на счёте сегодня. Доходом не считается.</p></div>', '<button class="primary">Создать счёт</button>')
acc_win = dialog('Счёт', '<div class="form-scroll">' + field('Название', text('Карта')) + field('Тип', val('Карта')) + field('Эмодзи', val('💳'))
                 + field('Остаток сейчас', '<strong class="big">42 300 ₽</strong>') + field('Начальный остаток', '<span class="txt">30 000 ₽ · 01.11.2026</span>')
                 + '</div>', '<button class="secondary">Остаток не сходится</button><button class="primary">Сохранить</button>')
recon = dialog('Остаток не сходится', rows([('history', 'Найти операцию', 'Открыть операции и исправить пропуск или ошибку'),
                                           ('restore', 'Изменить начальный остаток', 'Пересчитает остаток за всё время'),
                                           ('wallet', 'Указать реальный остаток', 'Создаст корректировку на разницу')])
               + '<div class="form-actions"><p class="dialog-note tight">Карта · по приложению 42 300 ₽</p></div>')
corr = dialog('Реальный остаток', '<div class="form-scroll"><div class="category-label center">Сколько на счёте «Карта» на самом деле</div>'
              '<div class="money"><span class="amount">42 450</span><span>₽</span></div>' + field('По приложению', '<span class="txt">42 300 ₽</span>')
              + field('Корректировка', '<strong class="big">+ 150 ₽</strong>') + field('Дата', val('10.11.2026 · сегодня'))
              + '<p class="dialog-note">Корректировка видна в операциях, но не входит в доходы и расходы.</p></div>', '<button class="primary">Сохранить</button>')
init = dialog('Начальный остаток', '<div class="form-scroll"><div class="category-label center">Сколько было на счёте «Карта» 01.11.2026</div>'
              '<div class="money"><span class="amount">30 150</span><span>₽</span></div>' + field('Было указано', '<span class="txt">30 000 ₽</span>')
              + field('Остаток сейчас станет', '<strong class="big">42 450 ₽</strong>')
              + '<p class="dialog-note">Операции не меняются — пересчитывается только остаток.</p></div>', '<button class="primary">Сохранить</button>')
LONG = ACC + [('💳', 'Т-Банк', 12480), ('🏦', 'Вклад', 250000), ('💵', 'Конверт', 15000)]
s2 = [
 fig('2.1 Первый запуск', 'Счетов нет: аналитика пустая, «+» внизу тоже ведёт к созданию счёта. Готовых категорий нет.', screen(empty)),
 fig('2.2 Новый счёт', 'Название, тип, эмодзи (необязательно), остаток сейчас.', screen(empty, overlay=new_acc)),
 fig('2.3 Первая операция без категорий', 'Вместо кнопок категорий — «Создать категорию». Сохранить можно без неё: попадёт в «Без категории».', screen(analytics(), overlay=form(amount='850', cats=[]))),
 fig('2.4 Много счетов', 'Карточки листаются вбок, четвёртая выглядывает — видно, что есть ещё. Сумма в заголовке — по всем счетам.', screen(analytics(accounts(LONG, 405930)))),
 fig('2.5 Окно счёта', 'Нажатие на карточку счёта. Правка названия, типа, эмодзи. Удаления и архива нет (не согласовано).', screen(analytics(), overlay=acc_win)),
 fig('2.6 Сверка: три способа', 'Из окна счёта по «Остаток не сходится».', screen(analytics(), overlay=recon)),
 fig('2.7 Реальный остаток', 'Вводишь, сколько есть на самом деле — разница считается сама.', screen(analytics(), overlay=corr)),
 fig('2.8 Начальный остаток', 'Меняешь стартовую сумму — видно, каким станет остаток сейчас.', screen(analytics(), overlay=init)),
]

# ---------- 3. Категории, корзина, копия ----------
menu = dialog('Финансы', rows([('category', 'Категории', 'Расходы — 8 · доходы — 5'), ('trash', 'Корзина', '3 операции'),
                               ('backup', 'Резервная копия', 'Последняя — 09.11.2026')]))
def crow(n, arch=False):
    return f'<button class="account-row cr{" arch" if arch else ""}"><span class="name">{n}</span>{ico("restore") if arch else ico("right")}</button>'
cats_body = ('<div class="tabs wide"><button class="selected">Расходы</button><button>Доходы</button></div><div class="card">'
             + ''.join(crow(n) for n in ['Продукты', 'Кафе', 'Транспорт', 'Дом', 'Здоровье', 'Подарки', 'Одежда'])
             + '</div><div class="group-date">Архив</div><div class="card">' + crow('Такси', True) + '</div>'
             '<p class="dialog-note pad">Архивная категория остаётся в старых операциях и аналитике, но для новых не предлагается.</p>')
plus_btn = f'<button class="icon-button dark">{ico("plus")}</button>'
cat_edit = dialog('Категория расходов', '<div class="form-scroll">' + field('Название', text('Кафе')) + '</div>',
                  '<button class="secondary">В архив</button><button class="primary">Сохранить</button>')
picker = dialog('Категории расходов', '<div class="form-scroll list">' + ''.join(
    f'<div class="field pick{" on" if n == "Продукты" else ""}"><span>{n}</span>{"<b>✓</b>" if n == "Продукты" else ""}</div>'
    for n in ['Без категории', 'Продукты', 'Кафе', 'Транспорт', 'Дом', 'Здоровье', 'Подарки', 'Одежда']) + '</div>',
    f'<button class="secondary">{ico("plus")}Новая категория</button>')
def trow(n, d, v):
    return f'<div class="operation tr"><span>{n}<span class="op-sub">{d}</span></span><strong>{v}</strong><button class="mini">{ico("restore")}</button><button class="mini">{ico("trash")}</button></div>'
trash_body = ('<div class="card">' + trow('Кафе', '9 ноября · удалено сегодня', '− 620 ₽') + trow('Карта → Наличные', 'Перевод · 7 ноября', '5 000 ₽')
              + trow('Продукты', '5 ноября · удалено 6 ноября', '− 1 350 ₽') + '</div>'
              '<p class="dialog-note pad">Операции в корзине не влияют на остатки и аналитику. Сами не удаляются — только вручную.</p>')
clear = '<button class="text-btn">Очистить</button>'
confirm_clear = dialog('Очистить корзину?', '<div class="form-scroll"><p class="dialog-text">3 операции будут удалены навсегда. Восстановить их будет нельзя.</p></div>',
                       '<div class="two"><button class="secondary">Отмена</button><button class="primary">Удалить</button></div>')
backup = dialog('Резервная копия', '<div class="form-scroll"><p class="dialog-text">В файл попадают счета, начальные остатки, категории, операции и корзина. Задачи — нет.</p>'
                + field('Последняя копия', '<span class="txt">09.11.2026, 21:40</span>') + '</div>',
                f'<button class="primary">{ico("backup")}Сохранить в файл</button><button class="secondary">{ico("upload")}Восстановить из файла</button>')
restore = dialog('Заменить данные финансов?', '<div class="form-scroll">' + field('Файл', '<span class="txt">finance-2026-11-09.json</span>')
                 + field('В копии', '<span class="txt">3 счёта · 13 категорий · 214 операций</span>') + field('Сейчас', '<span class="txt">3 счёта · 13 категорий · 221 операция</span>')
                 + '<p class="dialog-text">Все текущие данные финансов будут заменены содержимым копии. Задачи не затрагиваются.</p></div>',
                 '<div class="two"><button class="secondary">Отмена</button><button class="primary">Заменить</button></div>')
s3 = [
 fig('3.1 Шестерёнка финансов', 'Единственный вход в категории, корзину и копию.', screen(analytics(), overlay=menu)),
 fig('3.2 Категории', 'Отдельный экран: два списка, «+» добавляет в открытый список. Архив — внизу, стрелка возвращает.', service('Категории', cats_body, plus_btn)),
 fig('3.3 Правка категории', 'Только название. «В архив» вместо удаления.', service('Категории', cats_body, plus_btn, cat_edit)),
 fig('3.4 «Все категории» из формы', 'Полный список + «Без категории» + создание новой, не выходя из операции.', screen(analytics(), overlay=picker)),
 fig('3.5 Корзина', 'У каждой операции — восстановить и удалить навсегда. Перевод восстанавливается целиком (обе стороны).', service('Корзина', trash_body, clear)),
 fig('3.6 Подтверждение очистки', 'Так же спрашиваем при удалении одной операции.', service('Корзина', trash_body, clear, confirm_clear)),
 fig('3.7 Резервная копия', 'Сохранение — системное окно выбора места. Восстановление — выбор файла.', screen(analytics(), overlay=backup)),
 fig('3.8 Подтверждение восстановления', 'Видно, что в копии и что будет потеряно.', screen(analytics(), overlay=restore)),
]

# ---------- 4. Аналитика и задачи ----------
def trows(r):
    return '<section class="task-list">' + ''.join(f'<div class="task-row"><span class="emoji">{e}</span><span>{n}</span><small>{c}</small></div>' for e, n, c in r) + '</section>'
tasks = (f'<div class="phone"><div class="screen task-screen">{top("tasks")}<main class="content">'
         + trows([('📥', 'Входящие', 51), ('💭', 'Когда-нибудь', 12), ('📌', 'Без списка', '')])
         + trows([('🗂️', 'Другое', ''), ('💡', 'Планы', 12), ('💼', 'Работа', 13), ('💲', 'Подписки и услуги', 21), ('🏠', 'Дом', 3), ('🏀', 'Спорт', 1), ('🔁', 'Рутина', 4)])
         + trows([('✓', 'Выполненные', '')]) + f'</main><div class="task-tools"><button>{ico("search")}</button><button>{ico("folder")}</button><button>{TASK}</button><button>{ico("settings")}</button></div>'
         f'<nav class="footer finance-footer"><button class="nav">{ico("week")}</button><button class="add">{ico("plus")}</button><button class="nav active">{TASK}</button></nav><div class="gesture"><span></span></div></div></div>')
nodata = accounts() + month() + ('<section class="summary"><span class="small muted">Расходы · 1–10 ноября</span><div class="total">0 ₽</div>'
         '<span class="delta">−39 200 ₽ · −100%</span><div class="summary-note">К 1–10 октября · было 39 200 ₽</div></section>'
         '<div class="card"><p class="dialog-text pad2">В этом месяце расходов пока нет.</p></div>')
s4 = [
 fig('4.1 Сравнивать не с чем', 'Если в прошлом периоде было 0 ₽ — вместо процента пишем «в октябре не было». Категории без расходов в этом месяце не показываем.', screen(analytics(zero=True))),
 fig('4.2 Месяц без расходов', 'Счета и переключатель месяца на месте, список категорий заменён строкой.', screen(nodata)),
 fig('4.3 Задачи без монетки', 'Решение PM: вход в финансы только через верхний бар. В ряду инструментов снова четыре кнопки.', tasks),
]

extra = '''
body{background:#e8e8e8}.board{padding:36px;display:flex;flex-direction:column;gap:48px;width:max-content}.sec>h2{font-size:26px;letter-spacing:-.8px;margin:0 0 6px}.sec>p{font-size:14px;color:#62656e;margin:0 0 22px}
.rowp{display:grid;grid-template-columns:repeat(4,390px);gap:36px 32px;align-items:start}figure{margin:0}figcaption{font-size:13px;line-height:1.55;margin-top:12px;color:#62656e}figcaption b{color:#16171d;display:block;font-size:14px;margin-bottom:2px}
.phone.small{width:360px;height:640px}button{cursor:default}
.val{font-weight:700;font-size:12px;display:flex;align-items:center;gap:3px}.val svg{width:14px;height:14px;color:var(--inactive)}.txt{font-size:12px;text-align:right}.txt.ph,.amount.ph{color:var(--inactive)}
.amount{font-size:36px;font-weight:700;letter-spacing:-1px}.big{font-size:14px}.operation.neutral strong,.operation.neutral{color:var(--secondary)}.operation.neutral strong{font-weight:400}
.op-sub{font-size:10px;color:var(--secondary);display:block;margin-top:3px}.operation{padding:13px 16px}
.head-btns{display:flex;align-items:center;gap:4px}.head-ico{width:32px;height:32px;display:grid;place-items:center;color:var(--secondary)}.head-ico svg{width:19px;height:19px}
.secondary{display:flex;align-items:center;justify-content:center;gap:8px;border-radius:12px;background:var(--soft);height:44px;font-weight:700;font-size:13px;width:100%}.secondary svg{width:18px;height:18px}
.form-actions{display:flex;flex-direction:column;gap:8px}.two{display:flex;gap:8px}.chips.one{grid-template-columns:1fr}.dialog-note.tight{margin:4px 0}.dialog-note.pad{margin:14px 4px}.category-label.center{text-align:center;margin-top:8px}
.dialog-text{font-size:12px;line-height:1.6;color:var(--secondary);margin:8px 0 10px}.dialog-text.pad2{margin:16px}
.tools .t{display:flex;flex-direction:column;gap:3px}.tools small{font-size:10px;color:var(--secondary)}
.svc-head{height:56px;display:flex;align-items:center;gap:6px;padding:0 12px;flex-shrink:0}.svc-head h1{font-size:19px;letter-spacing:-.5px;margin:0;flex:1}.icon-button.dark{background:var(--text);color:#fff;width:36px;height:36px}.icon-button.dark svg{width:18px;height:18px}
.text-btn{font-size:12px;font-weight:700;padding:0 8px;height:36px}.gesture.plain{background:transparent}.tabs.wide{margin:0 0 14px}
.cr{padding:15px 16px;font-size:13px}.cr svg{width:16px;height:16px;color:var(--inactive)}.cr.arch .name{color:var(--secondary)}
.pick{font-size:13px;min-height:44px}.pick.on{font-weight:700}.form-scroll.list{padding-top:4px}
.tr{gap:6px}.tr>span{flex:1}.mini{width:32px;height:32px;border-radius:9px;background:var(--bg);display:grid;place-items:center;flex-shrink:0}.mini svg{width:16px;height:16px}
.account-tile{flex:0 0 108px;min-width:108px}.compact-accounts .account-tiles:has(.account-tile:nth-child(4)){margin-right:-16px}
.account-tiles:not(:has(.account-tile:nth-child(4))) .account-tile{flex:1;min-width:0}
.empty{height:560px}.empty .primary svg{width:19px;height:19px;color:#fff;margin:0}
'''
secs = [('1. Операции и формы', 'Список операций и окно операции поверх экрана с компактным баром.', s1),
        ('2. Первый запуск и счета', 'Пустое состояние, создание и правка счёта, три способа сверки.', s2),
        ('3. Категории, корзина, резервная копия', 'Всё служебное открывается из шестерёнки финансов.', s3),
        ('4. Аналитика и задачи', 'Крайние случаи сравнения и экран задач после удаления монетки.', s4)]
html = (f'<!doctype html><html lang="ru"><meta charset="utf-8"><title>Финансы — недостающие экраны (#217)</title><style>{style}{extra}</style><div class="board">'
        + ''.join(f'<section class="sec" id="s{i+1}"><h2>{t}</h2><p>{d}</p><div class="rowp">{"".join(f)}</div></section>' for i, (t, d, f) in enumerate(secs)) + '</div></html>')
(root / 'service-screens.html').write_text(html)

print('service-screens.html готов; картинки: node service-render.js')
