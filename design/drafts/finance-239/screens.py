"""Финансы #239 — макеты на согласование PM (13.11.2026): страница настроек, справочник мест, сортировка по алфавиту, архив и удаление навсегда.
Основа — стиль и компоненты макетов 217/233/237. Запуск из корня: python3 design/drafts/finance-239/screens.py → screens.html;
PNG: node design/drafts/finance-239/render.js. Превью анимаций — python3 design/drafts/finance-239/motion.py. Все названия и суммы вымышленные."""
from pathlib import Path

here = Path(__file__).parent
p233 = here.parent.parent / 'approved' / '233' / 'screens.py'
src233 = p233.read_text()
G = {'__file__': str(p233)}
exec(src233[:src233.index('# ---------- 1. Настройка')], G)
e0 = src233.index("extra = '''\n.module-top") + len("extra = '''")
extra233 = src233[e0:src233.index("'''", e0)]
g217 = G['g']
ico, service, dialog, field, fig, P, OPS = G['ico'], G['service'], G['dialog'], g217['field'], G['fig'], G['P'], G['OPS']
text = g217['text']
OPS = OPS.replace('11 ноября', '12 ноября').replace('12 ноября · сегодня', '13 ноября · сегодня', 1)

P.update({
    'pin': '<path d="M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z"/><circle cx="12" cy="10" r="2.5"/>',
    'chev': '<path d="m10 6 6 6-6 6"/>',
    'back': '<path d="M19 12H5M11 6l-6 6 6 6"/>',
    'okcheck': '<path d="m5 12 5 5 9-10"/>',
    # Значки нижней панели финансов с 0.64.0 (набор «В»): график роста и две стрелки.
    'chart': '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
    'lines': '<path d="M7 20V4M3 8l4-4 4 4M17 4v16M13 16l4 4 4-4"/>',
})
PLUS = '<button class="icon-button dark">' + ico('plus') + '</button>'


def setrow(icon, title, sub):
    return f'<button class="set-row">{ico(icon)}<span class="t">{title}<small>{sub}</small></span>{ico("chev")}</button>'


settings = service('Настройки финансов', '<div class="card">' + setrow('category', 'Категории', 'Действующих: 7') + setrow('pin', 'Места', 'Действующих: 6')
                   + setrow('trash', 'Корзина', 'Операций: 3') + setrow('backup', 'Резервная копия', 'Последняя — 12.11.2026, 21:40')
                   + setrow('bell', 'Уведомления банка', 'Включено · Карта') + '</div>'
                   '<p class="dialog-note pad">Раньше — окно поверх экрана; теперь отдельная страница со стрелкой назад.</p>')


def mark(emoji, name):
    return f'<span class="cat-mark">{emoji}</span>' if emoji else f'<span class="cat-mark letter">{name[:1]}</span>'


def row(name, emoji=None, arch=False, show_mark=True):
    m = mark(emoji, name) if show_mark else ''
    return f'<button class="account-row cr{" arch" if arch else ""}">{m}<span class="name">{name}</span>{ico("chev")}</button>'


CATS = [('🏠', 'Дом'), ('💊', 'Здоровье'), ('☕', 'Кафе'), (None, 'Подарки'), ('🛒', 'Продукты'), ('🚕', 'Транспорт')]
PLACES = ['Азбука вкуса', 'ВкусВилл', 'Кофемания', 'Озон', 'Перекрёсток', 'Пятёрочка']
cats_body = ('<div class="tabs wide"><button class="selected">Расходы</button><button>Доходы</button></div><div class="card">'
             + ''.join(row(n, e) for e, n in CATS) + '</div><div class="group-date">Архив</div><div class="card">' + row('Такси', '🚖', True) + '</div>'
             '<p class="dialog-note pad">По алфавиту. Архивное не предлагается в новых операциях; нажатие — вернуть или удалить навсегда.</p>')
places_body = ('<div class="card">' + ''.join(row(n, show_mark=False) for n in PLACES) + '</div><div class="group-date">Архив</div><div class="card">'
               + row('Кофейня у дома', arch=True, show_mark=False) + '</div>'
               '<p class="dialog-note pad">По алфавиту. Новое место, набранное в окне операции, попадает сюда само. Уже введённые места перенесутся при обновлении.</p>')
cats_screen = lambda overlay='': service('Категории', cats_body, PLUS, overlay)
places_screen = lambda overlay='': service('Места', places_body, PLUS, overlay)

place_edit = dialog('Место', '<div class="form-scroll">' + field('Название', '<span class="txt">Перекрёсток</span>') + '</div>',
                    '<button class="secondary">В архив</button><button class="primary">Сохранить</button>')
two = lambda a, b, danger=False: f'<div class="two"><button class="secondary">{a}</button><button class="primary{" danger" if danger else ""}">{b}</button></div>'
confirm_arch_cat = dialog('Перенести в архив?', '<div class="form-scroll"><p class="dialog-text">Категория «Кафе» не будет предлагаться в новых операциях. '
                          'В старых операциях останется. Вернуть можно из архива.</p></div>', two('Отмена', 'В архив'))
confirm_arch_place = dialog('Перенести в архив?', '<div class="form-scroll"><p class="dialog-text">Место «Перекрёсток» не будет предлагаться при выборе места. '
                            'В старых операциях останется. Вернуть можно из архива.</p></div>', two('Отмена', 'В архив'))
arch_window = dialog('Такси', '<div class="form-scroll"><p class="dialog-text">В архиве. Есть в 14 операциях.</p></div>',
                     '<button class="secondary">Вернуть из архива</button><button class="secondary danger-text">Удалить навсегда</button>')
confirm_del_cat = dialog('Удалить навсегда?', '<div class="form-scroll"><p class="dialog-text">Категория «Такси» будет удалена. У <b>14 операций</b> категория станет '
                         '«Без категории». Сами операции не удаляются. <b>Это нельзя отменить.</b></p></div>', two('Отмена', 'Удалить', danger=True))
confirm_del_place = dialog('Удалить навсегда?', '<div class="form-scroll"><p class="dialog-text">Место «Кофейня у дома» будет удалено. У <b>5 операций</b> место станет '
                           'пустым. Сами операции не удаляются. <b>Это нельзя отменить.</b></p></div>', two('Отмена', 'Удалить', danger=True))

s1 = [
    fig('1.1 Настройки финансов', 'Шестерёнка открывает отдельную страницу со стрелкой назад. Добавился пункт «Места».', settings),
    fig('1.2 Справочник мест', 'Как категории, но без эмодзи и без деления на расходы/доходы. По алфавиту, архив внизу, «+» — новое место.', places_screen()),
    fig('1.3 Категории по алфавиту', 'Порядок по алфавиту (без учёта регистра). В окне операции пять быстрых категорий остаются «недавние первыми».', cats_screen()),
    fig('1.4 Правка места', 'Название, «В архив», «Сохранить». Переименование меняет место во всех операциях.', places_screen(place_edit)),
]
s2 = [
    fig('2.1 Предупреждение об архиве', 'Перед переносом категории в архив — подтверждение.', cats_screen(confirm_arch_cat)),
    fig('2.2 То же для места', 'Текст про выбор места вместо новых операций.', places_screen(confirm_arch_place)),
    fig('2.3 Архивная строка', 'Нажатие на архивную категорию или место: «Вернуть из архива» и «Удалить навсегда». Удалить можно только из архива.', cats_screen(arch_window)),
    fig('2.4 Удаление навсегда', 'Предупреждение с числом затронутых операций; кнопка «Удалить» — красная. Для места — так же («место станет пустым»).', cats_screen(confirm_del_cat)),
]

# ---------- 3. Место в окне операции ----------
place_pick = ('<div class="form-scroll"><div class="place-input"><span class="txt">Пе</span><span class="caret"></span></div>'
              '<div class="place-list"><button>Перекрёсток</button><button>Пятёрочка</button></div>'
              '<p class="dialog-note">Места из справочника, без архивных. Новое — напишите и нажмите ✓: оно появится в справочнике.</p></div>'
              f'<div class="icon-bar"><button class="ib soft">{ico("back")}</button><button class="ib dark">{ico("okcheck")}</button></div>')
op_place = (f'<div class="phone"><div class="screen">{G["top"]()}<main class="content">{OPS}</main>{G["footer"]("operations")}</div>'
            f'<div class="overlay"><section class="dialog op237">{place_pick}</section></div></div>')
s3 = [
    fig('3.1 Выбор места — из справочника', 'Окно выбора то же, что в 0.64.0; подсказки теперь из справочника и без архивных мест.', op_place),
    fig('3.2 Удаление места из справочника', 'Предупреждение для места.', places_screen(confirm_del_place)),
]

extra = '''
.set-row{display:flex;align-items:center;gap:12px;width:100%;text-align:left;padding:14px 16px;border-bottom:1px solid var(--line);font-size:14px}
.set-row:last-child{border:0}.set-row svg{width:20px;height:20px}.set-row svg:last-child{width:16px;height:16px;color:var(--inactive)}
.set-row .t{flex:1;display:flex;flex-direction:column;gap:3px}.set-row small{font-size:10px;color:var(--secondary)}
.cat-mark{width:29px;font-size:22px;text-align:center}.cat-mark.letter{height:29px;border-radius:9px;background:var(--soft);display:grid;place-items:center;font-size:13px;font-weight:700;color:var(--secondary)}
.cr svg{width:16px;height:16px;color:var(--inactive)}.cr.arch .name{color:var(--secondary)}
.primary.danger{background:#d64545}.danger-text{color:#d64545}
.dialog-text b{color:var(--text)}
.op237{max-height:calc(100% - 24px)}
.place-input{display:flex;align-items:center;gap:2px;background:var(--bg);border-radius:12px;padding:14px;margin:16px 0 8px;font-size:14px;font-weight:700}
.caret{width:2px;height:18px;background:var(--text)}
.place-list button{display:block;width:100%;text-align:left;padding:14px 4px;font-size:13px;border-bottom:1px solid var(--line)}
.icon-bar{display:flex;gap:8px;padding:12px 16px 16px;flex-shrink:0}
.ib{height:46px;border-radius:12px;display:grid;place-items:center;flex-shrink:0;width:56px}
.ib.soft{background:var(--soft)}.ib.dark{background:var(--text);color:#fff;flex:1;width:auto}.ib svg{width:22px;height:22px}
'''
secs = [('1. Настройки и справочники', 'Пункты 1–3 и 4 (#239): страница настроек, справочник мест, алфавит.', s1),
        ('2. Архив и удаление навсегда', 'Пункты 5–6: предупреждение при архивации, удаление только из архива с предупреждением.', s2),
        ('3. Место в окне операции', 'Пункт 1: выбор места опирается на справочник.', s3)]
html = (f'<!doctype html><html lang="ru"><meta charset="utf-8"><title>Финансы — справочники (#239)</title><style>{g217["style"]}{G["extra217"]}{extra233}{extra}</style>'
        '<div class="board">' + ''.join(f'<section class="sec" id="s{i+1}"><h2>{t}</h2><p>{d}</p><div class="rowp">{"".join(f)}</div></section>' for i, (t, d, f) in enumerate(secs)) + '</div></html>')
(here / 'screens.html').write_text(html)
print('screens.html готов; картинки: node design/drafts/finance-239/render.js')
