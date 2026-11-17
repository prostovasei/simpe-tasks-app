"""#265: компактный заголовок всплывающего дня, до изменения приложения."""
from pathlib import Path
here=Path(__file__).resolve().parent
source=here.parent/'263'/'screens.py'
s=source.read_text()
P={'__file__':str(source)}
exec(compile(s[:s.index('boards=[]')],str(source),'exec'),P)
CSS=P['CSS']+'''
.compact-day .sheet-header{gap:8px;padding:8px 12px;min-height:56px}
.compact-day .date-line{flex:1;min-width:0;font-size:14px;line-height:20px;font-weight:700}
.compact-day .count{margin-left:0;font-size:14px;line-height:20px;min-width:12px;text-align:right}
.compact-day .add-top{width:44px;height:44px;flex-shrink:0;display:grid;place-items:center;background:none;border-radius:12px}
.compact-day .add-top i{font-style:normal;display:grid;place-items:center;width:36px;height:36px;border-radius:12px;background:#16171D;color:white;font-size:26px;line-height:1;font-weight:500}
.compact-day .timeline{padding-bottom:12px}
.compact-day .empty{padding:16px 12px 28px;color:var(--secondary);font-size:14px}
.large-font .compact-day .date-line,.large-font .compact-day .count{font-size:18.2px;line-height:24px}
'''
ROWS=[('19:00','','Забрать заказ','🛍️',''),('23:00','','Сверить счета','💼','')]
def sheet(rows=ROWS):
 return ('<div class="sheet compact-day"><header class="sheet-header"><div class="date-line">ВТ 17 ноя Сегодня</div>'
 '<button class="add-top" data-action="add" aria-label="Добавить задачу на ВТ 17 ноя"><i>+</i></button>'
 f'<span class="count">{len(rows)}</span></header>'
 +(P['timeline'](rows,False) if rows else '<div class="empty">Нет задач на этот день</div>')+'</div>')
def home(width=390,large=False,rows=ROWS):
 P['WEEK'][1]=[(r[0],r[2],r[3],r[4]) for r in rows]
 return (f'<div class="home-preview {"large-font" if large else ""}" style="width:{width}px;height:780px">'
 +P['week_screen']('widget',width,large)+'<div class="scrim"></div>'+sheet(rows)+'</div>')
def board(id,title,body):
 return f'<section class="sec" id="{id}"><div class="badge">#265 · НА СОГЛАСОВАНИИ</div><h2>{title}</h2>{body}</section>'
main=board('day','Окно дня: дата меньше, «+» перед количеством',P['figure']('390 px · две задачи',home(),'Дата и «Сегодня» — 14 px, как счётчик. Справа кнопка «+», затем общее число 2. Нижней кнопки нет.'))
extra=board('states','Узкий экран и пустой день','<div class="rowp">'+P['figure']('320 px · шрифт 130%',home(320,True),'Дата и количество растут одинаково. «+» остаётся перед счётчиком; перенос заголовка допустим.')+P['figure']('Пустой день',home(rows=[]),'Общее количество — 0. «+» доступен и создаёт задачу на выбранный день.')+'</div>')
js='''
window.layoutReady=document.fonts.ready.then(()=>{for(const d of document.querySelectorAll('.widget.revised-week'))d.style.setProperty('--cell-height',d.querySelector('.day').getBoundingClientRect().height+'px')});
document.addEventListener('click',e=>{if(e.target.closest('[data-action="add"]')){const l=document.querySelector('.log');l.textContent='Создать задачу на ВТ 17 ноя · демонстрация';l.classList.add('on')}});
'''
(here/'screens.html').write_text('<!doctype html><html lang="ru"><meta charset="utf-8"><title>#265 · Заголовок дня</title><style>'+CSS+'</style><header class="intro"><h1>Компактное окно дня</h1><p>HTML-макет по уточнению PM. Код приложения не изменён. Данные вымышлены.</p></header><main class="board">'+main+extra+'</main><output class="log"></output><script>'+js+'</script></html>')
print('Макеты: обычный день, 320 px / 130%, пустой день')
