"""Финансы #237 — макеты доработок на согласование PM (13.11.2026): окно операции, выбор места, эмодзи категорий, значки нижнего бара.
Основа — общие компоненты и стиль макетов 217 и 233 (вид панелей по решению PM 11.11.2026, #227).
Запуск из корня: python3 design/drafts/finance-237/screens.py → screens.html; PNG: node design/drafts/finance-237/render.js (Playwright).
Все суммы и названия вымышленные. Дата «сегодня» в макетах — пт. 13 ноя 2026."""
from pathlib import Path

here = Path(__file__).parent
p233 = here.parent.parent / 'approved' / '233' / 'screens.py'
src233 = p233.read_text()
G = {'__file__': str(p233)}
exec(src233[:src233.index('# ---------- 1. Настройка')], G)  # ico, service, dialog, field, val, fig, top, footer, OPS … (в том числе из 217 и 233)
e0 = src233.index("extra = '''\n.module-top") + len("extra = '''")
extra233 = src233[e0:src233.index("'''", e0)]
g217 = G['g']
ico, service, dialog, field, fig, P = G['ico'], G['service'], G['dialog'], g217['field'], G['fig'], G['P']
OPS = G['OPS'].replace('11 ноября', '12 ноября').replace('12 ноября · сегодня', '13 ноября · сегодня', 1)  # «сегодня» в макетах — 13 ноября
top, keyboard, text = G['top'], g217['keyboard'], g217['text']

P.update({
    'back': '<path d="M19 12H5M11 6l-6 6 6 6"/>',
    'pie': '<circle cx="12" cy="12" r="9"/><path d="M12 3v9h9"/>',
    'receipt': '<path d="M6 3h12v18l-3-2-3 2-3-2-3 2z"/><path d="M9 8h6M9 12h6"/>',
    'trend': '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
    'swap': '<path d="M7 20V4M3 8l4-4 4 4M17 4v16M13 16l4 4 4-4"/>',
    'bars': '<path d="M5 20v-8M12 20V5M19 20v-11"/>',
    'card': '<rect x="4" y="4" width="16" height="16" rx="3"/><path d="M8 9h8M8 13h8M8 17h5"/>',
    'okcheck': '<path d="m5 12 5 5 9-10"/>',
})

TODAY = 'пт. 13 ноя 2026'

# ---------- общие части окна операции ----------
CATS = [('🛒', 'Продукты'), ('☕', 'Кафе'), ('🚕', 'Транспорт'), ('🏠', 'Дом'), ('💊', 'Здоровье')]


def kinds(selected='Расход'):
    return '<div class="tabs kind-tabs">' + ''.join(f'<button class="{"selected" if t == selected else ""}">{t}</button>' for t in ['Расход', 'Доход', 'Перевод']) + '</div>'


def amount(v='850'):
    return f'<div class="money"><span class="amount">{v}</span><span>₽</span></div>'


def emoji_row(active=0, labels=False):
    """Категории иконками: пять последних и «все». В выбранной — чёрная подложка."""
    cells = ''
    for i, (e, n) in enumerate(CATS):
        cells += (f'<div class="em-cell"><button class="em{" active" if i == active else ""}">{e}</button>'
                  + (f'<span class="em-label{" on" if i == active else ""}">{n}</span>' if labels else '') + '</div>')
    cells += '<div class="em-cell"><button class="em more">⋯</button>' + ('<span class="em-label">Все</span>' if labels else '') + '</div>'
    return f'<div class="em-row">{cells}</div>'


def selected_name(active=0):
    return f'<div class="em-name">{CATS[active][1]}</div>'


def pair(date_main=TODAY, date_sub='сегодня', place=None):
    """Дата и место — в одну строку. Место без заголовка: серое «Место платежа» или введённое."""
    d = f'<div class="pair"><span class="pv">{date_main}</span>' + (f'<span class="ps">{date_sub}</span>' if date_sub else '') + '</div>'
    p = f'<div class="pair">' + (f'<span class="pv">{place}</span>' if place else '<span class="pv ph">Место платежа</span>') + '</div>'
    return f'<div class="pair-row">{d}{p}</div>'


def account_field(name='💳 Карта'):
    return field('Счёт', g217['val'](name))


def bar(edit=False, kb=False):
    """Внизу: [удалить — только при правке] [назад] [сохранить ✓]. Все — значки."""
    trash = f'<button class="ib soft del">{ico("trash")}</button>' if edit else ''
    return f'<div class="icon-bar{" kb" if kb else ""}">{trash}<button class="ib soft">{ico("back")}</button><button class="ib dark">{ico("okcheck")}</button></div>'


def sheet(inner, kb=False):
    return (f'<div class="overlay {"has-keyboard" if kb else ""}"><section class="dialog op237">{inner}</section></div>' + (keyboard() if kb else ''))


def under(small=False, overlay=''):
    cls = 'phone small' if small else 'phone'
    return (f'<div class="{cls}"><div class="screen">{top()}<main class="content">{OPS}</main>{G["footer"]("operations")}</div>{overlay}</div>')


def variant_a(kb=False, edit=False, small=False):
    body = ('' if edit else kinds()) + '<div class="form-scroll">' + amount() + account_field() + '<div class="cat-gap"></div>' + emoji_row(0) + selected_name(0) \
        + pair(place='Пятёрочка' if edit else None, date_main='ср. 12 ноя 2026' if edit else TODAY, date_sub='' if edit else 'сегодня') \
        + field('Комментарий', text('')) + '</div>' + bar(edit, kb)
    return under(small, sheet(body, kb))


def variant_b(kb=False, small=False):
    body = kinds() + '<div class="form-scroll">' + amount() + account_field() + '<div class="cat-gap"></div>' + emoji_row(0, labels=True) \
        + pair() + field('Комментарий', text('')) + '</div>' + bar(False, kb)
    return under(small, sheet(body, kb))


def variant_c(kb=False, small=False):
    head = ('<div class="amt-line"><span class="amt-big"><span class="amount">850</span><span>₽</span></span>'
            f'<button class="acc-pill">💳 Карта{ico("down")}</button></div>')
    body = kinds() + '<div class="form-scroll">' + head + emoji_row(0) + selected_name(0) + pair() + field('Комментарий', text('')) + '</div>' + bar(False, kb)
    return under(small, sheet(body, kb))


# ---------- 1–2. Три варианта окна ----------
NA = 'Тип (Расход / Доход / Перевод) остаётся вверху: заголовка «Новая операция» и кнопки «закрыть» нет.'
s1 = [
    fig('1.1 Вариант А — «Спокойный»', 'Сумма, счёт, категории иконками, под ними название выбранной. Дата и место в одной строке (место без заголовка — серым «Место платежа»), '
        'под датой маленькое «сегодня». Внизу значки: назад и ✓. ' + NA, variant_a()),
    fig('1.2 Вариант Б — «С подписями»', 'Под каждой иконкой категории — маленькое название; ничего запоминать не нужно. Выше и крупнее, чем А.', variant_b()),
    fig('1.3 Вариант В — «Плотный»', 'Счёт переехал в одну строку с суммой (таблетка справа). Экономит высоту — лучше всех уживается с клавиатурой (см. лист 2).', variant_c()),
    fig('1.4 Правка операции (вариант А)', 'Без переключателя типа. Слева внизу значок корзины — отдельно от «назад» и ✓, чтобы не нажать случайно. Дата вчерашняя: «ср. 12 ноя 2026».', variant_a(edit=True)),
]

s2 = [
    fig('2.1 А с клавиатурой, 360×640', 'Окно сжимается над клавиатурой, значки внизу всегда видны. «Комментарий» уходит в прокрутку.', variant_a(kb=True, small=True), 360),
    fig('2.2 Б с клавиатурой, 360×640', 'Подписи под иконками съедают высоту — на маленьком экране «Комментарий» тоже уходит в прокрутку.', variant_b(kb=True, small=True), 360),
    fig('2.3 В с клавиатурой, 360×640', 'Помещается всё, включая «Комментарий», и ещё остаётся запас.', variant_c(kb=True, small=True), 360),
]

# ---------- 3. Место и категории ----------
place_body = ('<div class="form-scroll"><div class="place-input"><span class="txt">Пя</span><span class="caret"></span></div>'
              '<div class="place-list"><button>Пятёрочка</button><button>Перекрёсток</button><button>Озон</button></div>'
              '<p class="dialog-note">Нажмите на место — оно подставится. Новое — просто напишите и нажмите ✓.</p></div>' + bar(False))
place = under(False, sheet(place_body))


def crow(emoji, name, arch=False):
    mark = f'<span class="cat-mark">{emoji}</span>' if emoji else f'<span class="cat-mark letter">{name[:1]}</span>'
    return f'<button class="account-row cr{" arch" if arch else ""}">{mark}<span class="name">{name}</span>{ico("restore") if arch else ico("right")}</button>'


cats_list = ('<div class="tabs wide"><button class="selected">Расходы</button><button>Доходы</button></div><div class="card">'
             + crow('🛒', 'Продукты') + crow('☕', 'Кафе') + crow('🚕', 'Транспорт') + crow('🏠', 'Дом') + crow('💊', 'Здоровье') + crow(None, 'Подарки')
             + '</div><div class="group-date">Архив</div><div class="card">' + crow('🚖', 'Такси', True) + '</div>'
             '<p class="dialog-note pad">У категории без эмодзи в окне операции — плитка с первой буквой названия.</p>')
cat_edit = dialog('Категория расходов',
                  '<div class="form-scroll"><div class="field"><span class="field-label">Эмодзи</span><button class="emoji-pick">🛒</button></div>'
                  + field('Название', '<span class="txt">Продукты</span>') + '</div>',
                  '<button class="secondary">В архив</button><button class="primary">Сохранить</button>')
EMOJIS = '🛒☕🚕🏠💊🍔🍕🥗🍺🎁👕📱🎬🏋🚌⛽🧴🐶🎓✈🎮💡🧾💰📚'
picker = dialog('Эмодзи категории', '<div class="form-scroll"><div class="em-grid">' + ''.join(f'<button class="em-pick{" on" if e == "🛒" else ""}">{e}</button>' for e in EMOJIS)
                + '</div></div>', '<button class="secondary">Без эмодзи</button>')
s3 = [
    fig('3.1 Выбор места', 'Нажатие на «Место платежа» открывает выбор: сверху поле ввода, ниже ранее введённые места по набранным буквам. ✓ подставляет место, назад — закрывает.', place),
    fig('3.2 Категории с эмодзи', 'В списке категорий — эмодзи слева от названия.', service('Категории', cats_list, '<button class="icon-button dark">' + ico('plus') + '</button>')),
    fig('3.3 Правка категории', 'Новое поле «Эмодзи» над названием: нажатие открывает выбор.', service('Категории', cats_list, '<button class="icon-button dark">' + ico('plus') + '</button>', cat_edit)),
    fig('3.4 Выбор эмодзи', 'Сетка эмодзи — как в выборе эмодзи списка задач; «Без эмодзи» снимает выбор.', service('Категории', cats_list, '<button class="icon-button dark">' + ico('plus') + '</button>', picker)),
]

# ---------- 4. Значки нижнего бара ----------
SETS = [
    ('А — сейчас', 'Столбцы с осью и линии списка.', 'chart', 'history'),
    ('Б', 'Круговая диаграмма и чек.', 'pie', 'receipt'),
    ('В', 'График роста и две стрелки (движение денег).', 'trend', 'swap'),
    ('Г', 'Столбики и карточка со строками.', 'bars', 'card'),
]


def bar_strip(a, o, active):
    cls = lambda t: 'nav active' if t == active else 'nav'
    return (f'<div class="barbox"><nav class="footer finance-footer"><button class="{cls("analytics")}">{ico(a)}</button><button class="add">{ico("plus")}</button>'
            f'<button class="{cls("operations")}">{ico(o)}</button></nav></div>')


s4 = [f'<figure style="width:390px"><div class="bars">{bar_strip(a, o, "analytics")}{bar_strip(a, o, "operations")}</div><figcaption><b>4.{i} Набор {t}</b>{n} Сверху — активна «Аналитика», снизу — «Операции».</figcaption></figure>'
      for i, (t, n, a, o) in enumerate(SETS, 1)]

extra = '''
.op237{max-height:calc(100% - 24px)}
.op237 .kind-tabs{margin:12px 16px 4px}
.op237 .money{padding:4px 0 10px}
.has-keyboard .op237 .kind-tabs{margin:8px 16px 0}.has-keyboard .op237 .money{padding:2px 0 6px}.has-keyboard .op237 .em{height:40px}
.has-keyboard .op237 .cat-gap{height:4px}.has-keyboard .op237 .em-name{margin:5px 2px 2px}.has-keyboard .op237 .pair{min-height:42px}.has-keyboard .op237 .amt-line{padding:2px 0 6px}
.has-keyboard .op237 .icon-bar{padding:8px 16px 10px}.has-keyboard .op237 .ib{height:42px}
.cat-gap{height:10px}
.em-row{display:flex;gap:8px;justify-content:space-between}
.em-cell{display:flex;flex-direction:column;align-items:center;gap:5px;flex:1;min-width:0}
.em{width:100%;max-width:50px;height:44px;border-radius:12px;background:var(--soft);font-size:22px;display:grid;place-items:center}
.em.active{background:var(--text);box-shadow:inset 0 0 0 2px var(--text)}
.em.more{font-size:20px;color:var(--secondary)}
.em-label{font-size:9px;color:var(--secondary);white-space:nowrap}.em-label.on{color:var(--text);font-weight:700}
.em-name{font-size:10px;color:var(--secondary);margin:7px 2px 4px}
.pair-row{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin:8px 0 2px}
.pair{min-height:48px;background:var(--bg);border-radius:12px;padding:6px 12px;display:flex;flex-direction:column;justify-content:center;gap:1px;min-width:0}
.pv{font-size:12px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.pv.ph{font-weight:400;color:var(--inactive)}
.ps{font-size:9px;color:var(--secondary)}
.icon-bar{display:flex;gap:8px;padding:12px 16px 16px;flex-shrink:0}.icon-bar.kb{padding-top:8px;padding-bottom:10px}
.ib{height:46px;border-radius:12px;display:grid;place-items:center;flex-shrink:0;width:56px}
.ib.soft{background:var(--soft)}.ib.dark{background:var(--text);color:#fff;flex:1;width:auto}.ib svg{width:22px;height:22px}
.ib.del{color:var(--secondary)}
.amt-line{display:flex;align-items:center;justify-content:space-between;padding:6px 0 10px}
.amt-big{display:flex;align-items:baseline;gap:6px}.amt-big .amount{font-size:32px}
.acc-pill{display:flex;align-items:center;gap:4px;background:var(--soft);border-radius:10px;padding:8px 10px;font-size:12px;font-weight:700}.acc-pill svg{width:14px;height:14px;color:var(--inactive)}
.place-input{display:flex;align-items:center;gap:2px;background:var(--bg);border-radius:12px;padding:14px;margin:16px 0 8px;font-size:14px;font-weight:700}
.caret{width:2px;height:18px;background:var(--text)}
.place-list button{display:block;width:100%;text-align:left;padding:14px 4px;font-size:13px;border-bottom:1px solid var(--line)}
.cat-mark{width:29px;font-size:22px;text-align:center}.cat-mark.letter{height:29px;border-radius:9px;background:var(--soft);display:grid;place-items:center;font-size:13px;font-weight:700;color:var(--secondary)}
.emoji-pick{width:44px;height:44px;border-radius:12px;background:var(--soft);font-size:24px}
.em-grid{display:grid;grid-template-columns:repeat(6,1fr);gap:8px;padding:10px 0}
.em-pick{height:46px;border-radius:12px;background:var(--bg);font-size:24px}.em-pick.on{background:var(--text)}
.bars{display:flex;flex-direction:column;gap:12px}.barbox{border-radius:18px;overflow:hidden;background:#fff;box-shadow:0 0 0 1px #d6d6d6}
'''
secs = [('1. Окно операции — три варианта', 'Пункты 3–9 списка #237: без заголовка и «закрыть», категории иконками, дата и место в одну строку, значки внизу.', s1),
        ('2. С клавиатурой на маленьком экране', 'Пункт 10: окно должно помещаться над клавиатурой.', s2),
        ('3. Место и категории', 'Пункты 1, 5, 7: выбор места, эмодзи у категории.', s3),
        ('4. Значки нижнего бара финансов', 'Пункт 2: четыре набора на выбор.', s4)]
html = (f'<!doctype html><html lang="ru"><meta charset="utf-8"><title>Финансы — доработки (#237)</title><style>{g217["style"]}{g217["extra"] if "extra" in g217 else ""}{G["extra217"]}{extra233}{extra}</style>'
        '<div class="board">' + ''.join(f'<section class="sec" id="s{i+1}"><h2>{t}</h2><p>{d}</p><div class="rowp">{"".join(f)}</div></section>' for i, (t, d, f) in enumerate(secs)) + '</div></html>')
(here / 'screens.html').write_text(html)
print('screens.html готов; картинки: node design/drafts/finance-237/render.js')
