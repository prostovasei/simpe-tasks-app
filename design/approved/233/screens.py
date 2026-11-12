"""Уведомления банка #233 — макеты на согласование PM (12.11.2026).
Основа — общий стиль и компоненты макетов финансов 217 (design/approved/217/service-screens.py) с видом панелей по решению PM 11.11.2026 (#227):
бар модулей во всю ширину, иконки без плашек, активная — чёрная, неактивная — серая.
Запуск из корня: python3 design/approved/233/screens.py → screens.html; PNG: node design/approved/233/render.js (Playwright).
Все суммы, магазины и номера карт вымышленные."""
from pathlib import Path

here = Path(__file__).parent
src217 = (here.parent / '217' / 'service-screens.py').read_text()
ns = {'__file__': str(here.parent / '217' / 'service-screens.py')}
exec(src217[:src217.index('# ---------- 1.')], ns)  # общие помощники: ico, screen, service, dialog, form, rows, op, fig…
e0 = src217.index("extra = '''") + len("extra = '''")
extra217 = src217[e0:src217.index("'''", e0)]
g = ns
ico, service, dialog, form, rows, op, fig, P = g['ico'], g['service'], g['dialog'], g['form'], g['rows'], g['op'], g['fig'], g['P']

P.update({
    'bell': '<path d="M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4"/>',
    'lines': '<path d="M5 6h14M5 12h14M5 18h14"/>',
    'check': '<path d="m5 12 5 5 9-10"/>',
    'alert': '<circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16.5v.5"/>',
})
CHECK = '<svg viewBox="0 0 24 24"><path d="m5 12 5 5 9-10"/></svg>'
RUB = '<svg viewBox="0 0 24 24"><path d="M8 20V4h5.5a4 4 0 0 1 0 8H6M6 16h9"/></svg>'


def top(module='finance'):
    s = lambda m: ' class="selected"' if m == module else ''
    return (f'<div class="module-top wide"><nav class="module-bar"><button{s("tasks")}>{CHECK}</button><button{s("finance")}>{RUB}</button></nav>'
            f'<button class="module-settings">{ico("settings")}</button></div>')


def footer(tab):
    a = lambda t: 'nav active' if t == tab else 'nav'
    return (f'<nav class="footer finance-footer"><button class="{a("analytics")}">{ico("chart")}</button><button class="add">{ico("plus")}</button>'
            f'<button class="{a("operations")}">{ico("lines")}</button></nav><div class="gesture"><span></span></div>')


def screen(body, tab='operations', overlay=''):
    return f'<div class="phone"><div class="screen">{top()}<main class="content">{body}</main>{footer(tab)}</div>{overlay}</div>'


ACC = 'Карта'
OPS = ('<div class="group-date">12 ноября · сегодня</div><div class="card">' + op('Без категории', '− 680 ₽') + op('Продукты', '− 1 240 ₽') + '</div>'
       '<div class="group-date">11 ноября · вчера</div><div class="card">' + op('Кафе', '− 620 ₽') + op('Зарплата', '+ 65 000 ₽', 'income')
       + op('Карта → Наличные', '5 000 ₽', 'neutral', 'Перевод') + '</div>')
UNSORTED_ROW = f'<button class="unsorted">{ico("bell")}<span>Не разобрано</span><b>3</b>{ico("right")}</button>'

# ---------- 1. Настройка и наше уведомление ----------
menu = dialog('Финансы', rows([('category', 'Категории', 'Действующих: 7'), ('trash', 'Корзина', 'Операций: 3'),
                               ('backup', 'Резервная копия', 'Последняя — 09.11.2026'), ('bell', 'Уведомления банка', 'Выключено')]))


def switch(on):
    return f'<span class="switch{" on" if on else ""}"><span></span></span>'


def bank_settings(state):
    on = state == 'on'
    if state == 'off':
        access = '<button class="pill dark">Дать доступ</button>'
        acc = g['val']('Выбрать')
        status = ''
    elif state == 'on':
        access = f'<span class="ok">{ico("check")}Есть</span>'
        acc = g['val']('💳 ' + ACC)
        status = '<p class="dialog-note pad">Работает. Новая покупка в Яндекс Банке → наше уведомление с кнопками.</p>'
    else:  # revoked
        access = '<button class="pill dark">Дать доступ</button>'
        acc = g['val']('💳 ' + ACC)
        status = f'<div class="warn">{ico("alert")}<span>Доступ к уведомлениям отключён в настройках Android. Покупки не распознаются.</span></div>'
    body = (f'<div class="card"><div class="field big-row"><span>Распознавать покупки</span>{switch(on)}</div></div>'
            + (status if state != 'revoked' else status)
            + '<div class="group-date">Что нужно</div><div class="card">'
            + f'<div class="field step"><span class="t">1. Доступ к уведомлениям<small>Системное окно Android: найти Simple Tasks и включить</small></span>{access}</div>'
            + f'<div class="field step"><span class="t">2. Счёт для Яндекс Банка<small>Сюда записываются покупки</small></span>{acc}</div></div>'
            + '<p class="dialog-note pad">Читаем только уведомления Яндекса о покупках («Покупка на … RUB»). Текст уведомлений не сохраняется '
              'и никуда не отправляется: остаются только сумма, счёт и время.</p>')
    return service('Уведомления банка', body)


def notif(title, text, mark=''):
    m = f'<span class="mark">{mark}</span>' if mark else ''
    return (f'<div class="notif"><div class="n-head"><span class="n-app">✓</span>Simple Tasks · Покупки по карте · сейчас</div>'
            f'<b class="n-title">{title}</b><span class="n-text">{text}</span>{m}'
            '<div class="n-actions"><button>Записать</button><button>Изменить</button><button>Пропустить</button></div></div>')


shade = ('<div class="phone"><div class="screen shade"><div class="clock">14:32<small>четверг, 12 ноября</small></div>'
         + notif('−680 ₽', 'Яндекс Банк · Карта')
         + notif('−1 240 ₽', 'Яндекс Банк · Карта', 'Похоже, уже записано: «Продукты», −1 240 ₽ сегодня')
         + '<div class="notif dim"><div class="n-head"><span class="n-app y">Я</span>Яндекс Банк · 1 мин</div><b class="n-title">Кофейня</b>'
           '<span class="n-text">Покупка на 680.00 RUB, карта *1234. Доступно 4865.00 RUB</span></div>'
         + '</div></div>')

s1 = [
    fig('1.1 Вход — шестерёнка финансов', 'Четвёртая строка «Уведомления банка». Подпись — текущее состояние: «Выключено», «Включено · Карта», «Нет доступа».', screen(OPS, overlay=menu)),
    fig('1.2 Первая настройка', 'Два шага. «Дать доступ» открывает системный экран Android. Выключатель заработает, когда оба шага сделаны.', bank_settings('off')),
    fig('1.3 Включено', 'Доступ есть, счёт выбран. Здесь же можно выключить или сменить счёт.', bank_settings('on')),
    fig('1.4 Доступ отозван', 'Если доступ выключили в Android — предупреждение здесь и в подписи шестерёнки. Сами ничего не включаем.', bank_settings('revoked')),
]

# ---------- 2. Покупка и «Не разобрано» ----------


def unsorted_item(amount, when, mark=''):
    m = f'<span class="mark">{mark}</span>' if mark else ''
    return (f'<div class="u-item"><div class="row"><span><b>{amount}</b><span class="op-sub">Яндекс Банк · 💳 {ACC} · {when}</span>{m}</span></div>'
            '<div class="u-actions"><button class="secondary">Пропустить</button><button class="secondary">Изменить</button><button class="primary">Записать</button></div></div>')


unsorted_body = ('<div class="card">' + unsorted_item('− 680 ₽', 'сегодня, 14:32')
                 + unsorted_item('− 1 240 ₽', 'сегодня, 12:05', 'Похоже, уже записано: «Продукты», −1 240 ₽ сегодня')
                 + unsorted_item('− 350 ₽', 'вчера, 19:47') + '</div>'
                 '<p class="dialog-note pad">Покупки, на которые ещё не ответил: уведомление смахнули, не нажали или телефон перезагрузился. '
                 'Сами не исчезают.</p>')
edit = form(amount='680', active=None, date='12.11.2026 · сегодня', account='💳 ' + ACC)

s2 = [
    fig('2.1 Наше уведомление', 'Тихий канал «Покупки по карте». «Записать» — расход «Без категории» сразу. Вторая карточка — пометка возможного дубля. Внизу для сравнения — исходное уведомление банка (вымышленное).', shade),
    fig('2.2 «Не разобрано» в операциях', 'Строка над списком, только когда есть неразобранные. Нажатие — отдельный экран.', screen(UNSORTED_ROW + OPS)),
    fig('2.3 Экран «Не разобрано»', 'У каждой покупки — те же три действия. Пометка дубля — серым текстом, ничего не пропускается само.', service('Не разобрано', unsorted_body)),
    fig('2.4 «Изменить»', 'Обычное окно расхода: сумма, счёт и дата подставлены, категория не выбрана. Закрыл без сохранения — покупка остаётся в «Не разобрано».', screen(UNSORTED_ROW + OPS, overlay=edit)),
]

extra = '''
.module-top.wide{background:#fff;height:48px;min-height:48px;justify-content:center;position:relative}
.module-top.wide .module-bar{background:transparent;padding:0;gap:28px}
.module-top.wide .module-bar button{background:transparent;color:#96969c}.module-top.wide .module-bar button.selected{color:#17171c;background:transparent}
.module-top.wide .module-settings{position:absolute;right:10px}
.finance-footer .nav.active svg{background:none;padding:0;height:28px;width:auto}
.unsorted{display:flex;align-items:center;gap:10px;width:100%;background:#fff;border-radius:14px;padding:13px 16px;margin:12px 0 2px;font-size:13px;font-weight:700}
.unsorted svg{width:18px;height:18px}.unsorted span{flex:1;text-align:left}.unsorted b{background:var(--text);color:#fff;border-radius:9px;padding:2px 8px;font-size:11px}
.unsorted svg:last-child{color:var(--inactive);width:16px;height:16px}
.switch{width:40px;height:24px;border-radius:12px;background:#d6d6da;position:relative;flex-shrink:0}.switch span{position:absolute;top:3px;left:3px;width:18px;height:18px;border-radius:50%;background:#fff}
.switch.on{background:var(--text)}.switch.on span{left:19px}
.big-row{font-size:13px;font-weight:700;padding:14px 16px}.step{padding:12px 16px;gap:10px}.step .t{display:flex;flex-direction:column;gap:3px;font-size:13px}.step small{font-size:10px;color:var(--secondary);font-weight:400}
.pill{height:32px;padding:0 12px;border-radius:10px;font-size:11px;font-weight:700;flex-shrink:0}.pill.dark{background:var(--text);color:#fff}
.ok{display:flex;align-items:center;gap:4px;font-size:12px;font-weight:700}.ok svg{width:16px;height:16px}
.warn{display:flex;gap:10px;align-items:flex-start;background:#fff;border-radius:14px;padding:12px 14px;margin:12px 0 0;font-size:12px;line-height:1.5}.warn svg{width:18px;height:18px;flex-shrink:0}
.shade{background:linear-gradient(#3b3d45,#25262b);padding:26px 12px;gap:10px}.clock{color:#fff;font-size:46px;font-weight:300;text-align:center;margin:6px 0 14px;display:flex;flex-direction:column}
.clock small{font-size:12px;opacity:.75;margin-top:2px}
.notif{background:#fff;border-radius:18px;padding:12px 14px;display:flex;flex-direction:column;gap:3px;font-size:12px}.notif.dim{opacity:.92}
.n-head{display:flex;align-items:center;gap:6px;font-size:10px;color:var(--secondary)}.n-app{width:18px;height:18px;border-radius:50%;background:var(--text);color:#fff;display:grid;place-items:center;font-size:10px}
.n-app.y{background:#e6e6e9;color:var(--text);font-weight:700}.n-title{font-size:15px;margin-top:4px}.n-text{color:var(--secondary)}
.mark{font-size:10px;color:var(--secondary);display:block;margin-top:4px}
.n-actions{display:flex;gap:16px;margin-top:8px}.n-actions button{font-size:12px;font-weight:700}
.u-item{padding:12px 16px;border-bottom:1px solid #ececef}.u-item:last-child{border-bottom:0}.u-item b{font-size:15px}
.u-actions{display:flex;gap:6px;margin-top:10px}.u-actions button{height:34px;font-size:11px;border-radius:10px;flex:1}.u-actions .primary{margin:0}
'''
secs = [('1. Настройка', 'Шестерёнка финансов → «Уведомления банка». Решения PM: только Яндекс, один счёт, текст не храним.', s1),
        ('2. Покупка и «Не разобрано»', 'Наше уведомление с тремя кнопками, строка «Не разобрано» и окно «Изменить».', s2)]
html = (f'<!doctype html><html lang="ru"><meta charset="utf-8"><title>Уведомления банка (#233)</title><style>{g["style"]}{extra217}{extra}</style><div class="board">'
        + ''.join(f'<section class="sec" id="s{i+1}"><h2>{t}</h2><p>{d}</p><div class="rowp">{"".join(f)}</div></section>' for i, (t, d, f) in enumerate(secs)) + '</div></html>')
(here / 'screens.html').write_text(html)
print('screens.html готов; картинки: node design/approved/233/render.js')
