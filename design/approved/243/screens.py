"""Виджет и приложение #243 — макеты на согласование PM (14.11.2026): окно фильтра вида «Неделя» (по спискам, режимы «показать/скрыть
выбранные», папки, «Без списка»). Основа — стиль 217/233/241. Содержимое — снимки app/screenshots (вымышленные данные).
Запуск из корня: python3 design/approved/243/screens.py → screens.html; PNG: node design/approved/243/render.js."""
from pathlib import Path

here = Path(__file__).parent
p241 = here.parent / "241" / "screens.py"
src = p241.read_text()
ns = {'__file__': str(p241)}
exec(src[:src.index('IMG = {')], ns)
ns['here'] = here  # картинки содержимого — в папку этого макета
exec(src[src.index('def gesture'):src.index('FIN_OPS = ')], ns)
fig, phone_navbar, g217, G, extra233 = ns['fig'], ns['navbar'], ns['g217'], ns['G'], ns['extra233']
e241 = src.index("\nextra = '''\n.v-screen") + len("\nextra = '''")
extra241 = src[e241:src.index("'''", e241)]
week_img = ns['content']('week_light')


def chk(state):
    return f'<span class="chk {state}">{"✓" if state == "on" else "–" if state == "part" else ""}</span>'


def frow(state, emoji, name, sub='', level=0):
    return f'<div class="frow l{level}">{chk(state)}<span class="emo">{emoji}</span><span class="nm">{name}</span>{f"<small>{sub}</small>" if sub else ""}</div>'


def filter_dialog(mode='show', rows=None):
    seg = ('<div class="seg"><button class="{a}">Показать выбранные</button><button class="{b}">Скрыть выбранные</button></div>'
           .format(a='on' if mode == 'show' else '', b='on' if mode == 'hide' else ''))
    body = ''.join(rows)
    return (f'<section class="fdlg"><h3>Фильтр недели</h3>{seg}<div class="flist">{body}</div>'
            '<div class="facts"><button class="ghost">Сбросить</button><button class="primary">Готово</button></div></section>')


ROWS_OFF = [frow('off', '📄', 'Без списка'),
            frow('off', '💼', 'Работа', '', 0), frow('off', '📅', 'Встречи', '', 1), frow('off', '🗂', 'Проекты', '', 1),
            frow('off', '🏠', 'Личное', '', 0), frow('off', '🏡', 'Дом', '', 1), frow('off', '💪', 'Здоровье', '', 1),
            frow('off', '🛒', 'Покупки')]
ROWS_WORK = [frow('off', '📄', 'Без списка'),
             frow('on', '💼', 'Работа', '', 0), frow('on', '📅', 'Встречи', '', 1), frow('on', '🗂', 'Проекты', '', 1),
             frow('off', '🏠', 'Личное', '', 0), frow('off', '🏡', 'Дом', '', 1), frow('off', '💪', 'Здоровье', '', 1),
             frow('off', '🛒', 'Покупки')]
ROWS_PART = [frow('on', '📄', 'Без списка'),
             frow('part', '💼', 'Работа', '', 0), frow('on', '📅', 'Встречи', '', 1), frow('off', '🗂', 'Проекты', '', 1),
             frow('off', '🏠', 'Личное', '', 0), frow('off', '🏡', 'Дом', '', 1), frow('off', '💪', 'Здоровье', '', 1),
             frow('on', '🛒', 'Покупки')]


def ring(html, n=2):
    """Подсвечивает n-ю страницу панели: сюда нажимают (виджет) или удерживают (приложение)."""
    parts = html.split('<button class="pg')
    return parts[0] + ''.join(('<button class="pg ring' if i == n else '<button class="pg') + p for i, p in enumerate(parts[1:], 1))


def app_phone(dialog=False, hotspot=False):
    bar = phone_navbar('tasks', 1, 'O')
    if hotspot:
        bar = bar.replace('<button class="pg on">', '<button class="pg on ring">')
    inner = f'<img class="shot" src="{week_img}">{bar}'
    extra = f'<div class="fdim"></div>{filter_dialog("show", ROWS_WORK)}' if dialog else ''
    return f'<div class="phone"><div class="screen v-screen">{inner}</div>{extra}</div>'


def widget_phone(dialog_html=None, ringed=False):
    ring_html = '<i class="wring"></i>' if ringed else ''
    over = f'<div class="fdim"></div>{dialog_html}' if dialog_html else ''
    return (f'<div class="phone"><div class="home"><img class="wimg" src="widget-base.png">{ring_html}</div>{over}</div>')


s1 = [
    fig('1.1 Виджет «Неделя»', 'Вкладки «Все», «Входящие», «Неделя» и «+» работают как раньше. Решение PM: фильтр открывается только через «Настроить» по долгому нажатию на виджет (нажатие на значок «Неделя» не используется).', widget_phone()),
    fig('1.2 Окно фильтра — показать выбранные', 'Окно поверх рабочего стола, как прежнее. Выбрана папка «Работа» — отмечены все её списки: в «Неделе» остались задачи только из них. «Готово» применяет, «Сбросить» снимает все отметки.', widget_phone(filter_dialog('show', ROWS_WORK))),
    fig('1.3 Режим «Скрыть выбранные» и частичная папка', 'Режим «Скрыть выбранные»: отмеченное из недели убирается. У папки «Работа» стоит «–» — отмечена часть списков. «Без списка» — задачи с датой, у которых нет списка.', widget_phone(filter_dialog('hide', ROWS_PART))),
    fig('1.4 Фильтр выключен', 'Ничего не отмечено — фильтра нет, в «Неделе» всё, как сейчас. Так выглядит окно после «Сбросить» или после обновления приложения.', widget_phone(filter_dialog('show', ROWS_OFF))),
]
s2 = [
    fig('2.1 Страница «Неделя» в приложении', 'Фильтр открывается долгим нажатием на значок «Неделя» в нижней панели. Обычное нажатие открывает страницу, как сейчас. Подсказок нет.', app_phone(hotspot=True)),
    fig('2.2 То же окно фильтра', 'То же окно и те же режимы, но фильтр свой — с виджетом не связан. После «Готово» страница «Неделя» показывает только выбранное.', app_phone(dialog=True)),
]

extra = '''
.home{position:absolute;inset:0;background:#e8e8ea;overflow:hidden}
.wimg{display:block;width:100%;height:auto;margin-top:44px}
.wring{position:absolute;left:226px;top:778px;width:44px;height:44px;border-radius:14px;box-shadow:0 0 0 3px #7b3fe4}
.fdim{position:absolute;inset:0;background:#0000004d;z-index:3}
.fdlg{position:absolute;left:18px;right:18px;top:92px;z-index:4;background:#fff;border-radius:26px;padding:20px 18px 12px;box-shadow:0 12px 32px #0003;max-height:650px;display:flex;flex-direction:column}
.fdlg h3{font-size:19px;font-weight:500;margin:2px 4px 14px}
.seg{display:flex;background:#ececef;border-radius:12px;padding:3px;margin-bottom:10px}
.seg button{flex:1;padding:9px 6px;border-radius:10px;font-size:12px;color:#555}.seg button.on{background:#fff;color:#111;font-weight:600;box-shadow:0 1px 3px #0002}
.flist{overflow:hidden}
.frow{display:flex;align-items:center;gap:12px;padding:11px 6px;font-size:15px;border-bottom:1px solid #f0f0f2}
.frow.l1{padding-left:34px}.frow .nm{flex:1}.frow .emo{font-size:17px}.frow small{font-size:11px;color:#888}
.chk{width:22px;height:22px;border-radius:7px;border:2px solid #b9b9c0;display:grid;place-items:center;font-size:14px;color:#fff;flex-shrink:0}
.chk.on{background:#111;border-color:#111}.chk.part{background:#111;border-color:#111;font-weight:700}
.facts{display:flex;justify-content:space-between;align-items:center;padding:12px 4px 4px}
.facts .ghost{font-size:14px;color:#555;padding:10px 6px}.facts .primary{background:#111;color:#fff;border-radius:14px;padding:11px 26px;font-size:14px}
.navbar .pg.ring{box-shadow:0 0 0 3px #7b3fe4}
'''
secs = [('1. Виджет «Неделя»', 'Как открыть фильтр и как выглядит окно в трёх состояниях.', s1),
        ('2. Страница «Неделя» в приложении', 'Свой фильтр, открывается долгим нажатием на значок в нижней панели.', s2)]
html = (f'<!doctype html><html lang="ru"><meta charset="utf-8"><title>Фильтр недели — виджет и приложение (#243)</title><style>{g217["style"]}{G["extra217"]}{extra233}{extra241}{extra}</style>'
        '<div class="board">' + ''.join(f'<section class="sec" id="s{i+1}"><h2>{t}</h2><p>{d}</p><div class="rowp">{"".join(f)}</div></section>' for i, (t, d, f) in enumerate(secs)) + '</div></html>')
(here / 'screens.html').write_text(html)
print('screens.html готов; картинки: node design/approved/243/render.js')
