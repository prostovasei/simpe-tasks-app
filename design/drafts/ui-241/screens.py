"""Интерфейс #241 — макеты на согласование PM (13.11.2026): новая структура — без верхнего бара, единый нижний бар
(«+» слева, страницы модуля посередине, аватарка модуля справа), единые настройки.
Основа — стиль макетов 217/233/237/239. Содержимое экранов задач — снимки app/screenshots (вымышленные данные).
Запуск из корня: python3 design/drafts/ui-241/screens.py → screens.html; PNG: node design/drafts/ui-241/render.js."""
from pathlib import Path

from PIL import Image

here = Path(__file__).parent
root = here.parent.parent.parent
shots = root / 'app' / 'screenshots'
p233 = here.parent.parent / 'approved' / '233' / 'screens.py'
src233 = p233.read_text()
G = {'__file__': str(p233)}
exec(src233[:src233.index('# ---------- 1. Настройка')], G)
e0 = src233.index("extra = '''\n.module-top") + len("extra = '''")
extra233 = src233[e0:src233.index("'''", e0)]
g217 = G['g']
ico, service, dialog, field, fig, P = G['ico'], G['service'], G['dialog'], g217['field'], G['fig'], G['P']
analytics = g217['analytics']
OPS = G['OPS'].replace('11 ноября', '12 ноября').replace('12 ноября · сегодня', '13 ноября · сегодня', 1)

P.update({
    'cal': '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>',
    'week': '<path d="M3 4h18v16H3zM9 4v16m6-16v16"/>',
    'lines': '<path d="M5 6h14M5 12h14M5 18h14"/>',
    'trend': '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
    'swap': '<path d="M7 20V4M3 8l4-4 4 4M17 4v16M13 16l4 4 4-4"/>',
    'tick': '<path d="m5 12 5 5 9-10"/>',
    'ruble': '<path d="M8 20V4h5.5a4 4 0 0 1 0 8H6M6 16h9"/>',
    'gear': P['settings'],
    'chev': '<path d="m10 6 6 6-6 6"/>',
    'pin': '<path d="M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z"/><circle cx="12" cy="10" r="2.5"/>',
    'info': '<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5v.5"/>',
})

# ---------- содержимое экранов задач: снимки, вписанные по высоте области над панелью ----------
CONTENT_H = 844 - 72 - 17  # телефон 844 − панель − жестовая полоса


def content(name, bg=(245, 245, 245)):
    im = Image.open(shots / f'{name}.png').convert('RGB')
    if name == 'lists_light':  # шестерёнка настроек из ряда инструментов уходит (настройки — из меню аватарки)
        from PIL import ImageDraw
        ImageDraw.Draw(im).rectangle([592, 1690, 800, 1790], fill=bg)
    h = CONTENT_H * 2
    w = round(im.width * h / im.height)
    im = im.resize((w, h), Image.LANCZOS)
    out = Image.new('RGB', (390 * 2, h), bg)
    out.paste(im, ((390 * 2 - w) // 2, 0))
    path = here / f'content-{name}.png'
    out.save(path)
    return path.name


IMG = {n: content(n) for n in ['all_light', 'week_light', 'lists_light']}


def gesture():
    return '<div class="gesture"><span></span></div>'


# ---------- новая нижняя панель ----------
TASK_PAGES = [('cal', 'Предстоящее'), ('week', 'Неделя'), ('lines', 'Списки')]
FIN_PAGES = [('trend', 'Аналитика'), ('swap', 'Операции')]


def navbar(module='tasks', active=0, variant='A', menu_open=False):
    pages = TASK_PAGES if module == 'tasks' else FIN_PAGES
    cells = ''.join(f'<button class="pg{" on" if i == active else ""}">{ico(n)}</button>' for i, (n, _) in enumerate(pages))
    av_icon = ico('tick') if module == 'tasks' else ico('ruble')
    return (f'<nav class="navbar v{variant}"><button class="add">{ico("plus")}</button><div class="pages">{cells}</div>'
            f'<button class="avatar{" open" if menu_open else ""}">{av_icon}</button></nav>{gesture()}')


def menu(module='tasks'):
    def row(icon, title, current=False):
        return f'<button class="mrow{" cur" if current else ""}">{ico(icon)}<span>{title}</span>{'<i class="cdot"></i>' if current else ""}</button>'
    return ('<div class="amenu">' + row('tick', 'Задачи', module == 'tasks') + row('ruble', 'Финансы', module == 'finance')
            + '<div class="msep"></div>' + row('gear', 'Настройки') + '</div>')


def phone(body, module='tasks', active=0, variant='A', menu_open=False, img=None):
    inner = f'<img class="shot" src="{img}">' if img else f'<main class="content">{body}</main>'
    return (f'<div class="phone"><div class="screen v-screen">{inner}{navbar(module, active, variant, menu_open)}</div>'
            + (f'<div class="dim"></div>{menu(module)}' if menu_open else '') + '</div>')


FIN_OPS = phone(OPS, 'finance', 1)
FIN_AN = phone(analytics(), 'finance', 0)

s1 = [
    fig('1.1 Задачи · «Предстоящее»', 'Верхнего бара нет. Внизу: «+» слева, страницы посередине (три — выбранная чёрная, остальные серые), аватарка модуля справа. Вариант А.', phone('', 'tasks', 0, 'A', img=IMG['all_light'])),
    fig('1.2 Задачи · «Неделя»', 'Страницы «Предстоящее», «Неделя», «Списки» — всегда все три, скрытого выбора удержанием больше нет, точки-подсказки под значком нет.', phone('', 'tasks', 1, 'A', img=IMG['week_light'])),
    fig('1.3 Задачи · «Списки»', 'Счётчика «Входящих» на значке нет. Шестерёнка в ряду инструментов внизу списка уходит (остальные кнопки распределятся поровну): настройки — из меню аватарки.', phone('', 'tasks', 2, 'A', img=IMG['lists_light'])),
    fig('1.4 Финансы · «Операции»', 'Тот же каркас: «+» (новая операция), две страницы «Аналитика» и «Операции», аватарка с ₽.', FIN_OPS),
]
s2 = [
    fig('2.1 Вариант Б — без подложки', 'Страницы без серой капсулы: только значки; аватарка — круг с тонкой обводкой. Остальное то же.', phone('', 'tasks', 0, 'B', img=IMG['all_light'])),
    fig('2.2 Вариант Б · финансы', 'Тот же вариант в финансах.', phone(analytics(), 'finance', 0, 'B')),
    fig('2.3 Меню аватарки · задачи', 'Нажатие на аватарку: меню над панелью — «Задачи» (отмечено) и «Финансы», внизу «Настройки». Смена модуля — одно нажатие.', phone('', 'tasks', 0, 'A', True, img=IMG['all_light'])),
    fig('2.4 Меню аватарки · финансы', 'В финансах отмечены «Финансы», аватарка с ₽. «Настройки» — те же единые настройки.', phone(OPS, 'finance', 1, 'A', True)),
]

# ---------- единые настройки ----------


def setrow(icon, title, sub):
    return f'<button class="set-row">{ico(icon)}<span class="t">{title}<small>{sub}</small></span>{ico("chev")}</button>'


def srow(title, sub, toggle=None):
    right = ''
    if toggle is not None:
        right = f'<span class="switch{" on" if toggle else ""}"><span></span></span>'
    return f'<div class="field big-row"><span class="t">{title}<small>{sub}</small></span>{right}</div>'


home = service('Настройки', '<div class="card">' + setrow('tick', 'Задачи', 'Ввод, напоминания, импорт и резервная копия') + setrow('ruble', 'Финансы', 'Категории, места, корзина, копия, банк')
               + setrow('info', 'Приложение', 'Версия 0.66.0 · обновление') + '</div>'
               '<p class="dialog-note pad">Один вход для всех настроек: из меню аватарки. Стрелка назад возвращает на экран, откуда пришли.</p>')
tasks_settings = service('Задачи', '<div class="group-date">Ввод задач</div><div class="card">' + srow('Дата и время в названии', '«Созвон завтра 10:00» — срок ставится сам', True) + '</div>'
                         '<div class="group-date">Напоминания</div><div class="card">' + srow('Всплывающая полоска', 'Поверх любого приложения', True) + srow('Точное время', 'Будильником', True)
                         + srow('Утренняя сводка', 'Каждый день в 08:00', False) + '</div>'
                         '<div class="group-date">Данные</div><div class="card">' + setrow('lines', 'Импорт из TickTick', 'Файл .csv') + setrow('lines', 'Резервная копия задач', 'В файл формата TickTick')
                         + setrow('lines', 'Очистить все данные', 'Задач в базе: 214') + '</div>')
fin_settings = service('Финансы', '<div class="card">' + setrow('category', 'Категории', 'Действующих: 7') + setrow('pin', 'Места', 'Действующих: 6') + setrow('trash', 'Корзина', 'Операций: 3')
                       + setrow('backup', 'Резервная копия', 'Последняя — 12.11.2026, 21:40') + setrow('bell', 'Уведомления банка', 'Включено · Карта') + '</div>'
                       '<p class="dialog-note pad">Это страница «Настройки финансов» из 0.65.0, теперь — раздел единых настроек.</p>')
app_settings = service('Приложение', '<div class="card">' + setrow('backup', 'Обновление', 'Проверить и установить новую версию') + '<div class="field big-row"><span class="t">Версия<small>0.66.0 (сборка 320)</small></span></div></div>'
                       '<p class="dialog-note pad">Из настроек задач сюда переезжают обновление и версия — они относятся ко всему приложению.</p>')
s3 = [
    fig('3.1 Настройки — главная', 'Три раздела: «Задачи», «Финансы», «Приложение». Открывается из меню аватарки, одинаково из любого модуля.', home),
    fig('3.2 Раздел «Задачи»', 'Прежние настройки задач без обновления и версии (они в «Приложении»).', tasks_settings),
    fig('3.3 Раздел «Финансы»', 'Страница из 0.65.0: категории, места, корзина, копия, банк.', fin_settings),
    fig('3.4 Раздел «Приложение»', 'Обновление и версия.', app_settings),
]

# ---------- что убрано ----------


def old_bar(mode='tasks'):
    if mode == 'tasks':
        return ('<div class="barbox"><nav class="footer finance-footer"><button class="nav active old-left">' + ico('cal') + '<i class="hintdot"></i></button><button class="add">' + ico('plus')
                + '</button><button class="nav old-lists">' + ico('lines') + '<b class="badge">51</b></button></nav></div>')
    return ('<div class="barbox"><nav class="footer finance-footer"><button class="nav active">' + ico('trend') + '</button><button class="add">' + ico('plus') + '</button><button class="nav">' + ico('swap') + '</button></nav></div>')


def new_bar_strip(module, variant='A'):
    return f'<div class="barbox">{navbar(module, 0, variant).replace(gesture(), "")}</div>'


s4 = [
    f'<figure style="width:390px"><div class="bars">{old_bar("tasks")}{new_bar_strip("tasks")}</div><figcaption><b>4.1 Задачи: было и стало</b>'
    'Было: слева «Предстоящее»/«Неделя» с точкой «есть меню» (выбор удержанием), «+» по центру, «Списки» со счётчиком «Входящих». Стало: «+» слева, три страницы, аватарка; точки и счётчика нет (решение PM).</figcaption></figure>',
    f'<figure style="width:390px"><div class="bars">{old_bar("finance")}{new_bar_strip("finance")}</div><figcaption><b>4.2 Финансы: было и стало</b>'
    'Было: «Аналитика», «+», «Операции»; сверху ещё полоса ✓ / ₽ и шестерёнка. Стало: «+» слева, две страницы, аватарка; верхней полосы нет.</figcaption></figure>',
    f'<figure style="width:390px"><div class="bars">{new_bar_strip("tasks", "A")}{new_bar_strip("tasks", "B")}</div><figcaption><b>4.3 Варианты панели</b>'
    'А — страницы в серой капсуле, аватарка — серый круг. Б — страницы без подложки, аватарка — круг с обводкой. Выбранная страница чёрная, остальные серые.</figcaption></figure>',
]

extra = '''
.v-screen{position:relative}
.shot{display:block;width:100%;height:755px;object-fit:cover;object-position:top}
.navbar{display:flex;align-items:center;justify-content:space-between;gap:10px;height:72px;background:#fff;padding:6px 14px;flex-shrink:0}
.navbar .add{width:72px;height:44px;border-radius:12px;background:var(--text);color:#fff;display:grid;place-items:center;flex-shrink:0}
.navbar .pages{display:flex;align-items:center;justify-content:center;gap:4px;flex:1}
.navbar .pg{width:56px;height:40px;display:grid;place-items:center;color:var(--inactive);border-radius:12px}.navbar .pg.on{color:var(--text)}.navbar .pg svg{width:24px;height:24px}
.navbar.vA .pages{background:var(--soft);border-radius:14px;padding:2px 4px;flex:0 1 auto;margin:0 auto}
.navbar .avatar{width:44px;height:44px;border-radius:50%;display:grid;place-items:center;flex-shrink:0}.navbar .avatar svg{width:22px;height:22px}
.navbar.vA .avatar{background:var(--soft);color:var(--text)}
.navbar.vB .avatar{box-shadow:inset 0 0 0 1.6px var(--text);color:var(--text)}
.navbar .avatar.open{background:var(--text);color:#fff;box-shadow:none}
.dim{position:absolute;left:0;right:0;top:0;bottom:89px;background:#0000002e;z-index:3}
.amenu{position:absolute;right:12px;bottom:92px;width:196px;background:#fff;border-radius:18px;box-shadow:0 12px 32px #0003;padding:6px;z-index:4}
.mrow{display:flex;align-items:center;gap:12px;width:100%;text-align:left;padding:12px 12px;border-radius:12px;font-size:14px}.mrow svg{width:20px;height:20px}.mrow span{flex:1}.mrow.cur{font-weight:700}.cdot{width:8px;height:8px;border-radius:50%;background:var(--text)}
.msep{height:1px;background:var(--line);margin:4px 10px}
.set-row{display:flex;align-items:center;gap:12px;width:100%;text-align:left;padding:14px 16px;border-bottom:1px solid var(--line);font-size:14px}
.set-row:last-child{border:0}.set-row svg{width:20px;height:20px}.set-row svg:last-child{width:16px;height:16px;color:var(--inactive)}
.set-row .t{flex:1;display:flex;flex-direction:column;gap:3px}.set-row small,.big-row small{font-size:10px;color:var(--secondary);font-weight:400}
.big-row{font-size:13px;padding:14px 16px;gap:10px}.big-row .t{display:flex;flex-direction:column;gap:3px;flex:1}
.switch{width:40px;height:24px;border-radius:12px;background:#d6d6da;position:relative;flex-shrink:0}.switch span{position:absolute;top:3px;left:3px;width:18px;height:18px;border-radius:50%;background:#fff}
.switch.on{background:var(--text)}.switch.on span{left:19px}
.bars{display:flex;flex-direction:column;gap:12px}.barbox{border-radius:18px;overflow:hidden;background:#fff;box-shadow:0 0 0 1px #d6d6d6;position:relative}
.barbox .navbar{height:72px}
.hintdot{position:absolute;left:50%;bottom:6px;width:4px;height:4px;border-radius:50%;background:var(--text);opacity:.4;transform:translateX(-50%)}
.old-left,.old-lists{position:relative;min-width:72px;height:56px}.hintdot{bottom:2px;width:5px;height:5px;opacity:.55}
.badge{position:absolute;right:14px;top:2px;background:var(--text);color:#fff;border-radius:9px;min-width:18px;height:18px;font-size:10px;display:grid;place-items:center;padding:0 5px}
'''
secs = [('1. Новая нижняя панель', 'Пункты 3–8: верхнего бара нет; «+» слева, страницы модуля посередине, аватарка модуля справа. Вариант А.', s1),
        ('2. Вариант Б и меню аватарки', 'Второй вариант оформления и меню смены модуля с настройками.', s2),
        ('3. Единые настройки', 'Пункт 2: один раздел настроек для задач и финансов.', s3),
        ('4. Что убрано', 'Точка «есть меню» и счётчик «Входящих» (решение PM), верхняя полоса, шестерёнки.', s4)]
html = (f'<!doctype html><html lang="ru"><meta charset="utf-8"><title>Интерфейс — новая структура (#241)</title><style>{g217["style"]}{G["extra217"]}{extra233}{extra}</style>'
        '<div class="board">' + ''.join(f'<section class="sec" id="s{i+1}"><h2>{t}</h2><p>{d}</p><div class="rowp">{"".join(f)}</div></section>' for i, (t, d, f) in enumerate(secs)) + '</div></html>')
(here / 'screens.html').write_text(html)
print('screens.html готов; картинки: node design/drafts/ui-241/render.js')
