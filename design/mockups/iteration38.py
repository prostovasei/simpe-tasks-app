"""Макеты итерации 38: черновик новой задачи (бэклог №28) и недавние запросы в поиске (№31).

Собирается из компонентов дизайн-системы (`design/system/`), значения — из токенов. Запуск: `python3 design/mockups/iteration38.py <файл.html>`.
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'system'))
OUT_FILE = sys.argv[1] if len(sys.argv) > 1 else 'iteration38.html'
sys.argv = [sys.argv[0], '/tmp/ds_scratch']  # build_design_system читает путь вывода из argv
import build_design_system as B  # noqa: E402
import ds_more as D  # noqa: E402


def tokens_css():
    t = B.build_tokens()
    light, dark = [], []
    for c in t['color']['tokens']:
        light.append(f"--{c['name']}:{c['value']['light']}")
        dark.append(f"--{c['name']}:{c['value']['dark']}")
    common = []
    for group in ('spacing', 'radius', 'size'):
        for s in t[group]['tokens']:
            common.append(f"--{s['name']}:{s['value']}")
    common.append('--font-sans:"Manrope",system-ui,-apple-system,"Segoe UI",sans-serif')
    return ':root{' + ';'.join(light + common) + ';--accent-soft:color-mix(in srgb,var(--accent) 18%,transparent)}' \
        '@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){' + ';'.join(dark) + ';color-scheme:dark}}' \
        ':root[data-theme="dark"]{' + ';'.join(dark) + ';color-scheme:dark}'


def sheet(title_html, desc_html, scrim=True):
    """Шторка новой задачи поверх экрана «Все» (как ScreenNewTask) с подставленным текстом."""
    card = B.sheet_card(False, width=396)
    card = card.replace('color:var(--inactive)">Купить билеты на концерт', f'">{title_html}')
    card = card.replace('font-weight:500;color:var(--inactive);margin-top:4px">Описание', f'font-weight:500;color:{"var(--text-secondary)" if desc_html != "Описание" else "var(--inactive)"};margin-top:4px">{desc_html}')
    base = D.all_screen()
    shade = '<div style="position:absolute;inset:0;background:var(--scrim)"></div>' if scrim else ''
    return f'<div style="position:relative;width:412px;height:680px;flex-shrink:0">{base}{shade}<div style="position:absolute;left:8px;bottom:16px">{card}</div></div>'


CARET = '<span style="display:inline-block;width:2px;height:26px;background:var(--accent);vertical-align:-5px"></span>'
SELECT = lambda t: f'<span style="background:var(--accent-soft);border-radius:2px;padding:0 1px">{t}</span>'


def recent_row(text):
    return ('<div style="min-height:48px;display:flex;align-items:center;padding:0 4px 0 16px;gap:16px;color:var(--text-secondary)">'
            f'<span style="display:flex">{D.mi("range") if False else history_icon()}</span>'
            f'<span style="flex:1;font-size:16px;line-height:24px;font-weight:500;color:var(--text-primary)">{text}</span>'
            f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center">{D.mi("close", 20)}</span></div>')


def history_icon():
    return ('<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">'
            '<path d="M3.5 12a8.5 8.5 0 1 0 2.6-6.1M3.5 4.5v4h4M12 7.5V12l3 2"/></svg>')


def recents_screen():
    field = (f'<div style="margin:12px;height:56px;border-radius:var(--radius-lg);background:var(--surface);display:flex;align-items:center;padding:0 16px;gap:16px;color:var(--text-secondary)">{D.mi("search")}'
             f'<span style="flex:1;font-size:16px;color:var(--inactive)">Поиск{CARET}</span></div>')
    head = ('<div style="display:flex;align-items:center;padding:16px 16px 4px;font-size:12px;line-height:16px;font-weight:700;letter-spacing:.04em">'
            '<span style="flex:1;color:var(--text-secondary)">НЕДАВНИЕ</span><span style="color:var(--accent);padding:8px 0 8px 12px;font-size:13px">Очистить</span></div>')
    rows = ''.join(recent_row(t) for t in ['отчёт', 'купить продукты', 'бюджет', 'врач', 'зарядка'])
    return D.frame(f'{field}{head}{rows}{D.nav_bar(3)}', 680)


def note(t):
    return f'<div class="note">{t}</div>'


CSS = '''
*{box-sizing:border-box}
body{margin:0;background:var(--background);color:var(--text-primary);font-family:var(--font-sans);font-size:14px;line-height:20px;
 padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px));padding-block:28px 56px}
main{max-width:1000px;margin:0 auto;display:flex;flex-direction:column;gap:48px}
h1{font-size:28px;line-height:36px;font-weight:800;margin:0 0 8px}
h2{font-size:22px;line-height:28px;font-weight:700;margin:0}
p{margin:0;color:var(--text-secondary);font-size:15px;line-height:22px;max-width:64ch}
.tag{display:inline-block;font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:var(--accent);background:var(--selected);border-radius:4px;padding:2px 8px;align-self:flex-start}
section{display:flex;flex-direction:column;gap:18px}
.head{display:flex;flex-direction:column;gap:6px}
.screens{display:flex;flex-wrap:wrap;gap:28px;align-items:flex-start}
.screen{display:flex;flex-direction:column;gap:10px;max-width:100%}
.cap{font-size:12px;font-weight:700;letter-spacing:.03em;color:var(--text-secondary);text-transform:uppercase}
.note{width:412px;max-width:100%;font-size:14px;line-height:20px;color:var(--text-primary);background:var(--selected);border-radius:var(--radius-lg);padding:10px 14px}
.rules{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px}
.rules div{background:var(--surface);border:1px solid var(--divider);border-radius:var(--radius-lg);padding:14px 16px;display:flex;flex-direction:column;gap:4px}
.rules b{font-weight:700}.rules span{color:var(--text-secondary)}
.scroll{overflow-x:auto;max-width:100%}
a{color:var(--accent);font-weight:600}
'''


def page():
    t28 = f'''
<section><div class="head"><span class="tag">№28 · Черновик новой задачи</span><h2>Закрыли случайно — текст вернулся</h2>
<p>Если карточку новой задачи закрыли, не сохранив, а в названии что-то набрано, название и описание запоминаются. Следующее «+» открывает карточку с этим текстом.</p></div>
<div class="screens">
 <div class="screen"><div class="cap">1. Набрали и закрыли</div>{sheet('Купить билеты на конц' + CARET, 'Нужно две штуки, ближе к сцене')}{note('Закрыли «Назад» или нажатием мимо карточки: текст запоминается сам, ничего не спрашиваем.')}</div>
 <div class="screen"><div class="cap">2. Следующее «+»</div>{sheet(SELECT('Купить билеты на конц'), 'Нужно две штуки, ближе к сцене')}{note('Текст подставлен молча и выделен: любая буква заменит его, «Удалить» на клавиатуре сотрёт. Дата, список, приоритет и чек-лист не сохраняются.')}</div>
</div>
<div class="rules">
 <div><b>Когда запоминаем</b><span>Карточку закрыли без сохранения, а в названии есть текст.</span></div>
 <div><b>Когда стираем</b><span>Задачу сохранили; название очистили; текст заменили и сохранили.</span></div>
 <div><b>Сколько хранить</b><span>Один черновик, без срока годности.</span></div>
 <div><b>Откуда открыли</b><span>«+» на любом экране и из виджета: черновик общий.</span></div>
</div></section>'''
    t31 = f'''
<section><div class="head"><span class="tag">№31 · Недавние запросы в поиске</span><h2>Пустое поле — последние запросы</h2>
<p>Пока поле поиска пустое, под ним «Недавние»: последние 5 запросов, новые сверху, без повторов. Нажатие подставляет запрос в поле и сразу ищет.</p></div>
<div class="screens">
 <div class="screen"><div class="cap">1. Открыли «Поиск»</div>{recents_screen()}{note('«Очистить» сверху стирает весь список, крестик справа — один запрос. Пока запросов нет, блока нет.')}</div>
 <div class="screen"><div class="cap">2. Нажали «отчёт»</div>{D.search_screen()}{note('В поле «отчёт» и найденное, как сейчас. Запрос запоминается, когда нажали найденную задачу или «поиск» на клавиатуре, а не на каждую букву.')}</div>
</div>
<div class="rules">
 <div><b>Строка запроса</b><span>Высота 48, слева значок «часы» 22, текст 16 Medium, справа крестик в зоне нажатия 48.</span></div>
 <div><b>Заголовок блока</b><span>«НЕДАВНИЕ» мелким Bold, справа «Очистить» зелёным.</span></div>
 <div><b>Хранение</b><span>На телефоне, 5 штук; повтор поднимает запрос наверх.</span></div>
</div></section>'''
    snack = '''
<section><div class="head"><span class="tag">Подсказка «Отмена»</span><h2>Белая полупрозрачная</h2>
<p>Макет уже согласован: <a href="https://claude.ai/artifact/VpFwqTC1w6TdkY4v3Jf4j1">смотреть</a>. Белая карточка на 88%, значок слева, зелёная «Отмена».</p></div></section>'''
    return (f'<title>Макеты итерации 38</title><meta name="claude:viewport-fit-sides" content="cover">'
            f'<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">'
            f'<style>{tokens_css()}{CSS}</style><main>'
            f'<div class="head"><span class="tag">Макеты · итерация 38</span><h1>Черновик задачи и недавние запросы</h1>'
            f'<p>Два экрана в цветах дизайн-системы Simple Tasks. После вашего «ок» они пойдут в код.</p></div>{t28}{t31}{snack}</main>')


Path(OUT_FILE).write_text(page(), encoding='utf-8')
print('ok', OUT_FILE)
