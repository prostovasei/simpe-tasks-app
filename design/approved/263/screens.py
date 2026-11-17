"""#263: макеты компактных периодов и новой Недели, до реализации.
Каркас листов approved/241, общие функции/токены/векторы — approved/259.
Запуск: python3 design/approved/263/screens.py; node design/approved/263/render.js.
"""
from pathlib import Path
import html

here = Path(__file__).resolve().parent
source = here.parent / '259' / 'screens.py'
code = source.read_text()
G = {'__file__': str(source)}
exec(compile(code[:code.index('def section(')], str(source), 'exec'), G)
RING, REPEAT, DONE = G['RING'], G['REPEAT'], G['DONE']
dim = G['dim']

DAYS=['ПН 16','ВТ 17','СР 18','ЧТ 19','ПТ 20','СБ 21','ВС 22']
WEEK=[
 [('08:00','Проверить почту','📨','done')],
 [('11:00','Проверить план','💼','done'),('17:00','Подготовить отчёт','💼','high'),('19:00','Забрать заказ','🛍️',''),('20:00','Отправить документы','💼','done')],
 [],
 [('08:00','Тренировка','🏋️','high'),('08:30','Длинная задача для проверки обрезки','🏠','')],
 [('14:30','Сверить счета','💼',''),('15:00','Купить продукты','🛒',''),('15:30','Прогулка','🌿',''),('18:00','Забрать посылку','📦','')],
 [('02:30','Футбол','⚽',''),('20:30','Теннис','🎾','')],
 [('14:30','Домашние дела','🏠',''),('16:00','Подготовить план','💼','')],
]


def week_row(data):
 time,title,emoji,flags=data
 return (f'<div class="task week-plain {flags}"><span class="list-icon">{emoji}</span>'
         f'<span class="title">{html.escape(title)}</span><span class="wtime">{time}</span></div>')


def cell(i,widget=False):
 today=' today' if i==1 else ''
 label=DAYS[i]+((' Сегодня' if widget else ' сегодня') if i==1 else '')
 more='<span class="more">+3</span>' if i==4 else ''
 return (f'<button class="day week-cell" data-action="day" data-day="{DAYS[i]}" aria-label="Открыть день {label}">'
         f'<div class="day-head{today}"><span>{label}</span>{more}</div><div class="week-rows">'
         +''.join(week_row(t) for t in WEEK[i])+'<div class="cell-space"></div></div></button>')


def week_content(widget=False):
 return '<div class="week-grid">'+''.join(cell(i,widget) for i in range(7))+G['mini_calendar']()+'</div>'


def status():
 return '<div class="status"><b>09:41</b><span>▰ ▰ ▰</span></div>'


def gesture():
 return '<div class="gesture"><i></i></div>'


def week_screen(kind='app',width=390,large=False):
 return (f'<div class="device {kind} revised-week {"large-font" if large else ""}" style="--device-width:{width}px">'
         +('' if kind=='widget' else status())+week_content(kind=='widget')+G['nav'](kind=='widget')+('' if kind=='widget' else gesture())+'</div>')

# Начало/конец — вымышленные записи для сравнения обычной и интервальной строки.
ROWS=[
 ('11:00','','Проверить план','💼','repeat done'),
 ('17:00','18:00','Подготовить отчёт','💼',''),
 ('19:00','','Забрать заказ','🛍️','priority'),
 ('20:00','21:00','Отправить документы','💼','done'),
]


def task_row(data):
 start,end,title,emoji,flags=data
 icon=REPEAT if 'repeat' in flags else DONE if 'done' in flags else RING
 label='Вернуть задачу' if 'done' in flags else 'Выполнить задачу'
 times=f'<span>{start}</span>'+(f'<span class="end">{end}</span>' if end else '')
 return (f'<div class="timeline-row {flags}" data-period="{str(bool(end)).lower()}">'
         f'<div class="tstack">{times}</div><div class="rail"><button class="toggle-node" data-action="toggle" aria-label="{label}">{icon}</button></div>'
         f'<button class="row-body" data-action="task"><span class="day-task-title">{html.escape(title)}</span>'
         +('<span class="bell" aria-label="Напоминание"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M6 9a6 6 0 0 1 12 0c0 6 2 6 2 8H4c0-2 2-2 2-8Z"/><path d="M10 20h4"/></svg></span>' if title=='Забрать заказ' else '')
         +f'<span class="day-emoji">{emoji}</span></button></div>')


def timeline(rows=ROWS,now=False):
 content=''.join(task_row(row) for row in rows)
 if now:content+='<div class="now"><span>21:30</span><i></i><b></b></div>'
 return '<div class="timeline">'+content+'</div>'


def day_screen(width=390,large=False):
 return (f'<div class="device app day-page {"large-font" if large else ""}" style="--device-width:{width}px">'+status()
         +'<header class="day-toolbar"><span class="back">‹</span><strong>ВТ 17 ноя</strong><span class="relative">Сегодня</span><span class="count">4</span></header>'
         +timeline()+'<div class="day-fill"></div>'+gesture()+'</div>')


def sheet(width=390,large=False):
 return (f'<div class="sheet {"large-font" if large else ""}"><div class="sheet-top-space"></div>'
         '<header class="sheet-header"><strong>ВТ 17 ноя</strong><span class="relative">Сегодня</span><span class="count">2 из 4</span></header>'
         +timeline()+'<div class="sheet-footer"><button data-action="add">＋ Задача на ВТ, 17 ноя</button></div></div>')


def widget_day_screen(width=390,large=False):
 return (f'<div class="home-preview {"large-font" if large else ""}" style="width:{width}px;height:844px">'
         +week_screen('widget',width,large)+'<div class="scrim"></div>'+sheet(width,large)+'</div>')


CSS=G['CSS']+f'''
:root{{--normal-row:{dim('WIDGET_ROW')}px;--regular-node:{dim('WIDGET_NODE')}px;--compact-node:{dim('WEEKVIEW_CHECK')}px}}
'''+'''
.revised-week .week-cell{text-align:left;align-items:stretch;cursor:pointer;background:#fff;border-radius:0}
.revised-week .week-rows{flex:1;display:flex;flex-direction:column;min-height:0}
.revised-week .cell-space{flex:1;min-height:0}
.revised-week .week-plain{gap:4px;flex-shrink:0;font-weight:700;background:none;border-radius:0}
.revised-week .week-plain.high:not(.done){color:#862CE0;font-weight:800}
.revised-week .list-icon{width:14px;flex-shrink:0;display:grid;place-items:center;font-size:12px}
.revised-week .done .list-icon{opacity:.35}
.revised-week .wtime{font-size:11px;line-height:16px;font-weight:700;letter-spacing:.5px;font-variant-numeric:tabular-nums;flex-shrink:0;white-space:nowrap}
.revised-week .high:not(.done) .wtime{font-weight:800}
.revised-week .title{line-height:20px}
.widget.revised-week .week-plain{height:calc((var(--cell-height) - 16px)/6 - 2px)}
.widget.revised-week .list-icon{font-size:10px;width:12px}
.widget.revised-week .wtime{font:700 10px/14px Arial,sans-serif;letter-spacing:0}
.widget.revised-week .high:not(.done) .wtime,.widget.revised-week .high:not(.done) .title{font-weight:800}
.timeline{background:#fff;flex-shrink:0;padding:8px 4px}
.timeline-row{height:var(--normal-row);display:flex;align-items:center;color:var(--text);font-weight:700;background:transparent;flex-shrink:0}
.tstack{width:38px;flex-shrink:0;display:flex;flex-direction:column;align-items:flex-end;font-size:13px;line-height:16px;font-variant-numeric:tabular-nums;padding-right:2px;font-weight:500}
.tstack .end{color:var(--secondary)}
.rail{width:26px;height:100%;position:relative;display:grid;place-items:center;flex-shrink:0}
.rail::before{content:'';position:absolute;top:0;bottom:0;width:1px;background:var(--line)}
.timeline-row:first-child .rail::before{top:50%}
.toggle-node{display:flex;position:relative;width:var(--regular-node);height:var(--regular-node);background:#fff;border-radius:50%;color:#A9A9A9;z-index:1}
.toggle-node svg{width:100%;height:100%}
.priority .toggle-node{color:#8E4EC6}
.row-body{display:flex;align-items:center;gap:8px;flex:1;min-width:0;height:100%;text-align:left;padding:0 12px;background:none;border-radius:0}
.day-task-title{font-size:14px;line-height:20px;flex:1;min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-weight:700}
.timeline-row.done .day-task-title{text-decoration:line-through;color:#A6A6AC}
.timeline-row.done .toggle-node{color:#BDBDC3}
.timeline-row.done .tstack{color:#A6A6AC}.timeline-row.done .tstack .end{color:#A6A6AC}
.timeline-row.done .day-emoji{opacity:.35}
.day-emoji{font-size:16px;flex-shrink:0;width:18px}
.bell{font-size:22px;font-weight:500;color:var(--secondary)}
.now{height:14px;display:flex;align-items:center;font-size:13px;font-variant-numeric:tabular-nums}
.now>span{width:38px;text-align:right;padding-right:2px}.now>i{width:7px;height:7px;background:#16171D;border-radius:50%;margin:0 9.5px}.now>b{flex:1;height:1px;background:#16171D;margin-left:4px}
.day-page,.sheet{background:#fff}
.day-toolbar{display:flex;align-items:center;gap:8px;height:56px;padding-right:12px;flex-shrink:0}
.back{width:44px;font-size:34px;text-align:center;flex-shrink:0;font-weight:400}
.day-toolbar strong,.sheet-header strong{font-size:22px;line-height:28px;white-space:nowrap}
.relative{font-size:22px;line-height:28px;font-weight:700;white-space:nowrap;background:none;padding:0}
.count{margin-left:auto;color:var(--secondary);font-size:14px;font-weight:700;white-space:nowrap}
.day-fill{flex:1}
.home-preview{position:relative;border-radius:16px;background:#D5E2F2;border:1px solid #CCD1DB;overflow:hidden;padding:30px 6px;font-family:Manrope,sans-serif}
.home-preview>.device{width:100%;height:710px;border-radius:16px;--cell-height:167px}
.scrim{position:absolute;inset:0;background:#0006}
.sheet{position:absolute;left:6px;right:6px;bottom:6px;border-radius:22px;overflow:hidden;color:var(--text);box-shadow:0 -2px 12px #00000008}
.sheet-top-space{height:18px}
.task-edit{padding:20px 18px;font-family:Manrope,sans-serif}.task-edit h3{font-size:24px;margin:0 0 18px}.task-edit p{color:var(--secondary);font-size:16px;margin:0 0 24px}.edit-date{font-size:16px;margin-bottom:24px}.edit-actions{display:flex;align-items:center;justify-content:space-between;font-size:24px}.edit-actions span{border:1px solid #ddd;border-radius:12px;padding:8px 12px}.edit-actions .save{background:#16171D;color:white}
.sheet-header{display:flex;align-items:center;gap:8px;padding:4px 14px 14px;min-height:48px}
.sheet-footer{padding:16px 12px;display:flex;justify-content:flex-end;background:#fff}
.sheet-footer button{padding:12px 16px;font-size:14px;font-weight:700;background:var(--text);color:#fff;border-radius:18px}
.sheet .timeline{padding:6px 4px 0}
.sample-panel{width:290px;border:1px solid #DDDDE3;border-radius:8px;overflow:hidden;font:500 14px/20px Manrope,sans-serif;background:white}
.panel-title{font-size:13px;font-weight:800;padding:16px 12px 8px}
.sample-panel .timeline{padding:0 2px 12px}
.sample-panel.compact .toggle-node{width:var(--compact-node);height:var(--compact-node)}
.sample-panel.compact .done .day-task-title{text-decoration:none}
.sample-panel.compact .rail{width:18px}
.sample-panel.compact .tstack{font-weight:700}
.sample-panel.inbox{background:#F5F5F5}.sample-panel.inbox .timeline{background:#F5F5F5}
.sample-panel.inbox .row-body{background:#fff;border-radius:8px;height:40px}
.sample-panel.listwidget{font-family:Arial,sans-serif}
.samples{display:flex;gap:24px;align-items:flex-start}
.small-note{font-size:11px;color:var(--secondary);margin-top:10px;max-width:290px}
.rule-note{padding:14px 18px;background:#F4F1FA;border-radius:8px;font-size:12px;margin-bottom:20px;color:#4C167F}
.log{position:fixed;right:16px;bottom:16px;background:#16171D;color:#fff;z-index:20;padding:12px 18px;border-radius:8px;max-width:450px;font-size:12px;display:none}.log.on{display:block}
.large-font.revised-week .title{font-size:15.6px}.large-font.revised-week .wtime{font-size:14.3px}.large-font.revised-week .day-head{font-size:14.3px}
.large-font.widget.revised-week .title,.large-font.widget.revised-week .wtime{font-size:13px}.large-font.widget.revised-week .day-head{font-size:11.7px}
.large-font .tstack{font-size:16.9px;line-height:18px;width:48px}.large-font .day-task-title{font-size:18.2px;line-height:24px}
.large-font .sheet-header strong{font-size:20px}.large-font .sheet-header{gap:6px;padding-left:10px;padding-right:10px}
.large-font .sheet-footer button{font-size:13px}.large-font .relative{font-size:20px}
'''


def figure(label,view,caption):
 return f'<figure><div class="label">{label}</div>{view}<figcaption class="caption">{caption}</figcaption></figure>'


def section(id,title,desc,body):
 return f'<section class="sec" id="{id}"><div class="badge">#263 · НА СОГЛАСОВАНИИ · 17.11.2026</div><h2>{title}</h2><p>{desc}</p>{body}</section>'

boards=[]
boards.append(section('week','1 · Неделя: эмодзи, название, время справа',
 'В ячейках нет значков выполнения и повтора. У периода видно только начало. Вся ячейка открывает день.',
 '<div class="rule-note">Одинаковое действие по дате, задаче, времени, эмодзи и пустому месту: открыть соответствующий день.</div><div class="rowp">'
 +figure('Приложение · 390×844',week_screen('app'),'Яркий приоритет и бледные выполненные сохранены. Сегодня отмечено текстом. Нижняя панель и календарь прежние.')
 +figure('Виджет · 390×650',week_screen('widget'),'Недельный виджет Android 12+. Время начала справа; эмодзи списка перед названием.')+'</div>'))
boards.append(section('day','2 · День: белый фон и компактные периоды',
 'В открытом дне выполнение остаётся. Обычная и интервальная задача одинаковой высоты, начало и конец периода слева друг под другом.',
 '<div class="rowp">'+figure('День в приложении',day_screen(),'Прежний вложенный экран с «Назад». Белая лента без фоновых карточек задач; круг выполняет, название открывает задачу.')
 +figure('Окно дня из виджета',widget_day_screen(),'Прежняя шторка поверх рабочего стола. Серый фон ленты и карточки убраны; закрытие, счётчик и кнопка добавления сохраняются.')+'</div>'))
normal=('09:00','','Обычная задача','💼','')
period=('10:00','11:00','Задача с периодом','💼','')
repeat=('12:00','13:00','Повтор с периодом','💼','repeat')
done=('14:00','15:00','Выполненный период','💼','done')
body='<div class="samples">'
for klass,title,note in [
 ('inbox','Списки с карточками','В остальных экранах оформление обычных задач сохраняется. Период становится той же высоты, со стандартным кругом или повтором.'),
 ('compact','Плоский список / Предстоящие','Обычная и интервальная строки по 46 dp. Значок 14 dp в существующем компактном стиле, оба времени сохранены.'),
 ('compact listwidget','Список виджета / Glance','Для недельной сетки показывается только начало. В списке виджета остаются начало и конец; интервал больше не вытягивает значок.')]:
 body+=f'<figure style="margin:0;width:290px"><div class="sample-panel {klass}"><div class="panel-title">{title}</div>{timeline([normal,period,repeat,done],False)}</div><figcaption class="small-note">{note}</figcaption></figure>'
body+='</div>'
boards.append(section('periods','3 · Интервал равен обычной строке',
 'Изменяется высота интервальной строки и форма значка, а не длительность задачи. Сравнивайте первые две строки каждого примера.',body))
boards.append(section('narrow','4 · Узкий экран и крупный шрифт',
 '320 px и шрифт 130% в HTML-прототипе. Длинные названия обрезаются, времена остаются видимыми.',
 '<div class="rowp">'+figure('Неделя приложения',week_screen('app',320,True),'Эмодзи слева, время справа. Вся ячейка открывает день, включая пустой день.')
 +figure('Окно дня виджета',widget_day_screen(320,True),'Оба времени периода в обычной строке. Точное поведение Android и доступность проверяются после реализации.')+'</div>'))
boards.append(section('task-window','5 · Окно задачи без верхней чёрточки',
 'В окне создания и редактирования задачи верхний маркер убран. Поля и действия сохраняются.',
 '<div class="rowp">'+figure('Окно задачи · приложение и виджет',
 '<div class="home-preview" style="width:390px;height:500px">'+week_screen('widget')+'<div class="scrim"></div><div class="sheet"><div class="task-edit"><h3>Подготовить отчёт</h3><p>Проверить данные перед встречей</p><div class="edit-date">▢ ВТ, 17 ноя · 17:00–18:00</div><div class="edit-actions"><span>💼</span><span>⚑</span><span>☷</span><span>↻</span><span><svg width="20" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M6 9a6 6 0 0 1 12 0c0 6 2 6 2 8H4c0-2 2-2 2-8Z"/><path d="M10 20h4"/></svg></span><span class="save">✓</span></div></div></div></div>',
 'Эскиз показывает удаление верхнего маркера. Клавиатура не показана; жест закрытия сохраняется.')+'</div>'))
links=''.join(f'<a href="#{id}">{text}</a>' for id,text in [('week','Неделя'),('day','Окно дня'),('periods','Компактные периоды'),('narrow','320 px / 130%'),('task-window','Окно задачи')])
JS='''
const log=document.querySelector('.log');
document.addEventListener('click',event=>{
 const el=event.target.closest('[data-action]');if(!el)return;
 const names={day:'Открыть день: '+(el.dataset.day||''),toggle:el.getAttribute('aria-label')||'Выполнить / вернуть задачу',task:'Открыть задачу',add:'Создать задачу на выбранный день'};
 log.textContent=(names[el.dataset.action]||'Выбрать неделю')+' · демонстрация';log.classList.add('on');
});
window.layoutReady=document.fonts.ready.then(()=>{
 for(const d of document.querySelectorAll('.widget.revised-week'))d.style.setProperty('--cell-height',d.querySelector('.day').getBoundingClientRect().height+'px');
});
'''
out=('<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
 '<title>#263 · Компактные периоды и новая Неделя</title><style>'+CSS+'</style>'
 '<header class="intro"><h1>Компактные задачи и плоский день</h1><p>Эскизы по ответам PM. Код приложения не изменён; перед реализацией нужно согласование и отдельное «Делай код».</p>'
 '<p>Данные вымышлены, шрифт Manrope встроен. Это HTML-макеты, не Android-снимки.</p></header>'
 f'<nav class="nav">{links}</nav><main class="board">{"".join(boards)}</main><output class="log" aria-live="polite"></output><script>{JS}</script></html>')
(here/'screens.html').write_text(out)
print('5 листов: неделя, день, интервалы, узкий экран, окно задачи')
