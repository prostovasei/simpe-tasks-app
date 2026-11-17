"""#261: 20 эскизов приоритета Недели. Источник геометрии и каркаса: #259 / #241.
Запуск из корня: python3 design/approved/261/screens.py; node design/approved/261/render.js.
"""
from pathlib import Path
import html

here = Path(__file__).resolve().parent
source = here.parent / '259' / 'screens.py'
code = source.read_text()
G = {'__file__': str(source)}
# До section — общие функции, исходные токены, настоящие векторные значки и CSS;
# без исполнения генерации альбома #259.
exec(compile(code[:code.index('def section(')], str(source), 'exec'), G)

items = [
 ('01','Более яркий текст','Насыщенный фиолетовый, время и название без подложки.'),
 ('02','Спокойная полоса','Едва заметный фиолетовый фон под всей строкой.'),
 ('03','Заметная полоса','Немного более насыщенный фон под всей строкой.'),
 ('04','Левая черта','Тонкая фиолетовая черта перед временем.'),
 ('05','Правая черта','Тонкая вертикальная черта у правого края строки.'),
 ('06','Черта и лёгкий фон','Левая черта вместе с очень светлой подложкой.'),
 ('07','Нижняя линия','Тонкая фиолетовая линия под всей строкой.'),
 ('08','Пунктир под строкой','Короткий пунктир под всей строкой.'),
 ('09','Обводка строки','Тонкая прямоугольная рамка вокруг строки.'),
 ('10','Пунктирная рамка','Лёгкая пунктирная обводка всей строки.'),
 ('11','Выделенное время','Светлая подложка только за временем.'),
 ('12','Время с контуром','Время в тонкой фиолетовой рамке.'),
 ('13','Акцент под временем','Короткая фиолетовая черта под временем.'),
 ('14','Акцент на названии','Светлая подложка только за названием.'),
 ('15','Маркер под текстом','Тонкий светлый штрих непосредственно под буквами.'),
 ('16','Подчёркнутое название','Фиолетовая линия только под буквами названия.'),
 ('17','Черта перед названием','Короткая вертикальная черта перед названием.'),
 ('18','Двойной контур значка','Незалитый кружок с дополнительной внешней обводкой.'),
 ('19','Маленький акцент','Фиолетовая точка после названия.'),
 ('20','Тёмная строка','Белый текст и контур на фиолетовой подложке.'),
]

# Данные вымышлены; «ГТА» — только пример, который PM явно указал в переписке.
G['TASKS'] = [
 [('08:00','ГТА','high'),('08:30','Обычная задача',''),('', 'Задача без времени','high'),('09:30','Выполненная','high done')],
 [('19:00','Позвонить',''),('20:00','Купить продукты','')], [],
 [('08:00','ГТА','high'),('08:30','Обычная задача',''),('', 'Задача без времени','high'),('09:30','Выполненная','high done')],
 [('14:30','Оплатить счёт',''),('15:00','Прогулка',''),('15:30','Забрать посылку','')],
 [('02:30','Футбол',''),('20:30','Теннис','')], [('14:30','Домашние дела',''),('16:00','Покупки','')],
]
G['DAYS']=['ПН 16','ВТ 17','СР 18','ЧТ 19','ПТ 20','СБ 21','ВС 22']
original_task = G['task']
def demo_task(data):
    icon = '🎮' if data[1] == 'ГТА' else '💲' if data[1] == 'Обычная задача' else '💼'
    return original_task(data).replace('>💼</span>', '>'+icon+'</span>')
G['task'] = demo_task
day, screen = G['day'], G['screen']

extra = '''
+ .high:not(.done){color:var(--deep)}
+ .high:not(.done) .time,.high:not(.done) .title{font-weight:800}
+ .vp01 .high:not(.done){color:#862CE0}
+ .vp02 .high:not(.done){background:#F8F3FD}
+ .vp03 .high:not(.done){background:#EDE0FA}
+ .vp04 .high:not(.done)::before{content:'';position:absolute;left:-2px;top:2px;bottom:2px;width:2px;background:#9252CF}
+ .vp05 .high:not(.done){box-shadow:inset -3px 0 #9252CF}
+ .vp06 .high:not(.done){background:#F8F3FD}.vp06 .high:not(.done)::before{content:'';position:absolute;left:-2px;top:2px;bottom:2px;width:2px;background:#9252CF}
+ .vp07 .high:not(.done){box-shadow:inset 0 -1.5px #9252CF}
+ .vp08 .high:not(.done){border-bottom:1px dashed #9252CF}
+ .vp09 .high:not(.done){box-shadow:inset 0 0 0 1px #AD7BDC}
+ .vp10 .high:not(.done){outline:1px dashed #AD7BDC;outline-offset:-1px}
+ .vp11 .high:not(.done) .time{background:#EDE0FA;border-radius:3px}
+ .vp12 .high:not(.done) .time{box-shadow:inset 0 0 0 1px #9252CF;border-radius:3px}
+ .vp13 .high:not(.done) .time{box-shadow:inset 0 -2px #9252CF}
+ .vp14 .high:not(.done) .title{background:#EEE2FA;border-radius:3px}
+ .vp15 .high:not(.done) .title>span{background:linear-gradient(transparent 58%,#D9B8F1 58%,#D9B8F1 94%,transparent 94%)}
+ .vp16 .high:not(.done) .title>span{text-decoration:underline;text-decoration-color:#9252CF;text-decoration-thickness:1px;text-underline-offset:3px}
+ .vp17 .high:not(.done) .title{box-shadow:inset 2px 0 #9252CF;text-indent:4px}
+ .vp18 .high:not(.done) .node{outline:1px solid #AA78D9;outline-offset:1px;border-radius:50%}
+ .vp19 .high:not(.done) .title>span::after{content:' •';color:#8C47CC}
+ .vp20 .high:not(.done){background:#5A248D;color:#fff}
+ .comparison-title{display:grid;grid-template-columns:265px 290px 290px;gap:24px;color:var(--secondary);font-size:11px;margin-top:18px}
+ .sample .day .rows{overflow:visible}
+'''.replace('\n+','\n')

def sec(id, title, description, body):
 return f'<section class="sec" id="{id}"><div class="badge">#261 · НА СОГЛАСОВАНИИ · 17.11.2026</div><h2>{title}</h2><p>{description}</p>{body}</section>'

boards=[]
baseline='<div class="rowp">'+''.join(f'<figure><div class="label">{label}</div>{screen(kind,"p00")}<figcaption class="caption">Текущее оформление 0.72.0 / build-350, HTML-эскиз на тех же вымышленных данных.</figcaption></figure>' for kind,label in [('app','Приложение'),('widget','Виджет')])+'</div>'
boards.append(sec('baseline','00 · Как сейчас','Сравните нынешний приоритет с вариантами ниже. Этот вид не входит в двадцать вариантов.',baseline))
for start in range(0,20,5):
 body='<div class="comparison-title"><span>Вариант</span><span>Приложение · увеличено ×1,48</span><span>Виджет · увеличено ×1,48</span></div>'
 for number,title,desc in items[start:start+5]:
  sample=''.join(f'<div class="sample"><div class="device {kind} vp{number}">{day(0)}</div></div>' for kind in ['app','widget'])
  body+=f'<div class="overview-row"><div><h3>{number} · {title}</h3><p>{desc}</p></div>{sample}</div>'
 boards.append(sec('overview-'+str(start//5+1),f'Варианты {start+1:02}–{start+5:02}',
  'Одинаковые задачи: активный приоритет, обычная задача, приоритет без времени и выполненная приоритетная. Все варианты ещё не выбраны.',body))

for number,title,desc in items:
 body='<div class="rowp">'+''.join(f'<figure><div class="label">{label}</div>{screen(kind,"p"+number)}<figcaption class="caption">{html.escape(desc)} Размеры и поведение строк как в build-350.</figcaption></figure>' for kind,label in [('app','Приложение · 390×844'),('widget','Виджет · 390×650')])+'</div>'
 boards.append(sec('variant-'+number,number+' · '+title,desc+' Это HTML-эскиз, не снимок Android.',body))

links='<a href="#baseline">Как сейчас</a>'+''.join(f'<a href="#overview-{i}">{(i-1)*5+1}–{i*5}</a>' for i in range(1,5))+''.join(f'<a href="#variant-{n}">{n}</a>' for n,_,_ in items)
# Конкретные цвета — предложения макета, не новые Kotlin-токены.
js='''
window.layoutReady=document.fonts.ready.then(()=>{
 const c=document.createElement('canvas').getContext('2d');
 for(const [kind,font,spacing] of [['app','800 11px Manrope',2.5],['widget','800 10px Arial',0]]){
  c.font=font;document.documentElement.style.setProperty('--'+kind+'-time',Math.ceil(c.measureText('00:00').width+spacing)+'px');
 }
 for(const d of document.querySelectorAll('.sec:not([id^="overview-"]) .device.widget'))
  d.style.setProperty('--cell-height',d.querySelector('.day').getBoundingClientRect().height+'px');
});
'''
out=('<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
 '<title>#261 · 20 вариантов приоритета</title><style>'+G['CSS']+extra+'</style>'
 '<header class="intro"><h1>Неделя · 20 способов выделить приоритет</h1><p>Сравнение на одних и тех же демонстрационных задачах. Цвета, линии и подложки — предложения на согласование.</p>'
 '<p>Геометрия основана на 0.72.0 / build-350, Manrope встроен. Шрифт виджета приближён браузерным Arial. Это HTML-эскизы, не Android-снимки.</p></header>'
 f'<nav class="nav">{links}</nav><main class="board">{"".join(boards)}</main><script>{js}</script></html>')
(here/'screens.html').write_text(out)
print('20 вариантов, 4 сравнительных листа и 20 полных экранов')
