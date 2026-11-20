from pathlib import Path
from base64 import b64encode
import xml.etree.ElementTree as ET

repo=Path(__file__).resolve().parents[3]
assets=repo/'app/src/main/res/drawable'
ns='{http://schemas.android.com/apk/res/android}'
icons=dict(week='tab_week', upcoming='widget_all', lists='tab_lists', inbox='widget_inbox', finance='module_finance', health='module_health', settings='nav_settings', update='widget_update', add='widget_add')
def icon(key):
    el=ET.parse(assets/f'ic_{icons[key]}.xml').getroot()
    paths=[]
    for item in el.iter('path'):
        props={'d':item.get(ns+'pathData'),'fill':'currentColor' if item.get(ns+'fillColor') not in (None,'@android:color/transparent') else 'none'}
        if item.get(ns+'strokeColor'):
            props.update({'stroke':'currentColor','stroke-width':item.get(ns+'strokeWidth','2'),'stroke-linecap':item.get(ns+'strokeLineCap','round'),'stroke-linejoin':item.get(ns+'strokeLineJoin','round')})
        paths.append('<path '+' '.join(f'{k}="{v}"' for k,v in props.items())+'/>')
    return '<svg viewBox="0 0 24 24" aria-hidden="true">'+''.join(paths)+'</svg>'

def shot(name):
    p=repo/f'app/screenshots/{name}.png'
    return '<img class="source" src="data:image/png;base64,'+b64encode(p.read_bytes()).decode()+'">'

labels=dict(week='Неделя', upcoming='Предстоящие', lists='Списки', inbox='Входящие', finance='Финансы', health='Здоровье', settings='Настройки', update='Обновить', add='Добавить')
def bar(active, task='week', widget=False, compact=False):
    keys=[task,'finance','health'] + (['settings'] if not widget else [] if compact else ['update'])
    controls=[f'<span class="control {"chosen" if key==active else ""}" aria-label="{labels[key]}">{icon(key)}</span>' for key in keys]
    date=''
    if widget and not compact:
        date='<span class="date"><b>20</b><small>ПТ<br>ноя</small></span>' if active==task else '<span class="date month">ноя 26</span>' if active=='finance' else ''
    add = f'<span class="add">{icon("add")}</span>' if active != "settings" else '<span class="add vacant" aria-hidden="true"></span>'
    return f'<div class="bar {"compact-bar" if compact else ""}">{add}<div class="tabs">{"".join(controls)}</div>{date}</div>'

def modal(content, cls=''):
    return f'<div class="shade"><div class="dialog {cls}">{content}</div></div>'

def picker(widget):
    rows=[]
    for key in (['upcoming','inbox','week'] if widget else ['upcoming','week','lists']):
        label='Все' if widget and key=='upcoming' else labels[key]
        rows.append(f'<div class="option">{icon(key)}<span>{label}</span>{"<b>✓</b>" if key=="week" else ""}</div>')
    return modal('<h3>Раздел задач</h3>'+''.join(rows))

def sample(title, body, caption='', cls=''):
    return f'<figure><figcaption>{title}</figcaption><div class="screen {cls}">{body}</div><p>{caption}</p></figure>'

def weight(compact=False):
    return f'''<div class="health-content {'mini' if compact else ''}"><div class="weight">82,4 <small>кг</small></div>
    <svg class="chart" viewBox="0 0 260 132" preserveAspectRatio="none" aria-label="График веса за месяц"><path d="M0 12H260 M0 62H260 M0 112H260" stroke="#e5e5e8" fill="none"/>
    <path d="M2 28L23 18L43 38L62 28L79 53L102 43L118 58L140 47L157 72L178 61L203 82L224 71L258 90" stroke="#17181d" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none" vector-effect="non-scaling-stroke"/>
    <circle cx="258" cy="90" r="3" fill="#17181d"/></svg><div class="ticks"><span>21 окт</span><span>20 ноя</span></div></div>'''

settings='<div class="settings-body"><h3>Настройки</h3>'+''.join(f'<div class="option">{icon(key)}<span>{labels[key]}</span><b>›</b></div>' for key in ['week','finance','health','settings']).replace('>Неделя<','>Задачи<').replace('<span>Настройки</span>','<span>Приложение</span>')+'</div>'
add_weight=modal('<h3>Вес</h3><div class="drum"><small>81　5</small><b>82,6 кг</b><small>83　7</small></div><div class="option">Дата <b>Сегодня · 20 ноября</b></div><div class="actions"><span>Отмена</span><b>Сохранить</b></div>')
update=modal(f'<div class="update-icon">{icon("update")}</div><h3>Доступно обновление</h3><p class="muted">Установлена 0.78.0</p><div class="actions"><span>Закрыть</span><b>Загрузить</b></div>')
app=''.join([
    sample('Задачи · Неделя',shot('week_light')+bar('week'), 'Удержание активной «Недели» открывает фильтр списков.'),
    sample('Повторный тап по «Задачам»',shot('week_light')+bar('week')+picker(False), 'Выбор меняет раздел и иконку вкладки.'),
    sample('Финансы',shot('finance_analytics_light')+bar('finance',task='lists'), 'Серая иконка задач показывает сохранённые «Списки».'),
    sample('Здоровье',shot('health_main_light')+bar('health',task='upcoming'), '«+» добавляет взвешивание.'),
    sample('Настройки',settings+bar('settings'), 'Четвёртая вкладка приложения.'),
])

wide_health=weight()+bar('health',task='inbox',widget=True)
wide_week=shot('widget_week_light')+bar('week',widget=True)
widget=''.join([
    sample('Широкий · Здоровье',wide_health,'Последний вес и график за месяц.','wide'),
    sample('Широкий · выбор раздела',wide_week+picker(True),'Фильтр списков — удержание всего виджета → «Настроить».','wide'),
    sample('180 × 110 dp',weight(True)+bar('health',widget=True,compact=True),'«+» и три вкладки.','small'),
    sample('«+» · ввод веса',wide_health+add_weight,'Окно поверх рабочего стола.','wide desktop'),
    sample('Иконка обновления',wide_health+update,'Проверка поверх рабочего стола. Установку подтверждает Android.','wide desktop'),
])
css=Path(__file__).with_name('screen.css').read_text()
Path(__file__).with_name('screens.html').write_text(f'<!doctype html><html lang="ru"><meta charset="utf-8"><style>{css}</style><body><main id="app" class="board"><h1>Приложение</h1><div class="gallery">{app}</div></main><main id="widget" class="board"><h1>Виджет</h1><div class="gallery widget-gallery">{widget}</div></main></body></html>')
