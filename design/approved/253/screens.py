"""#253. Альбом фактических снимков; каркас листов approved/241/screens.py.
Перед запуском: ./gradlew recordRoborazziDebug. PNG выбранных эскизов сохранены отдельно.
"""
from pathlib import Path
import base64
here=Path(__file__).parent
root=here.parents[2]
shots=root/'app/screenshots'
items=[('Неделя · приложение','week_light','Белый фон; день и время слева по одной линии, поле 2 dp.'),('Выполненные · приложение','week_priority_states','Без зачёркивания; бледные текст, время, выполнение и эмодзи.'),('Неделя · виджет','widget_week_light','Ширина времени по HH:mm, два зазора по 1 dp, +N без плашки.'),('Выполненные · виджет','widget_week_priority_states','Бледный высокий приоритет и значок списка; исходные клики сохранены.')]

# Имена снимков виджета сверяются по результатам тестов.
items=[(t,n,d) for t,n,d in items if (shots/(n+'.png')).exists()]
assert items, 'Сначала запишите Android-снимки'
style='body{font-family:Arial,sans-serif;background:#ededee;color:#16171d;margin:0;padding:24px}h1{font-size:26px}p{font-size:13px;color:#62656e}.sec{width:1260px;padding:20px 0}.rowp{display:flex;gap:24px}figure{margin:0;width:390px}img{width:390px;max-height:870px;object-fit:contain;object-position:top;display:block}figcaption{font-size:13px;margin-top:12px;line-height:1.6}h2{font-size:20px}'
sections=[]
for start in range(0,len(items),3):
 cards=[]
 for title,name,desc in items[start:start+3]:
  img=base64.b64encode((shots/(name+'.png')).read_bytes()).decode()
  cards.append(f'<figure><h2>{title}</h2><img src="data:image/png;base64,{img}"><figcaption>{desc}</figcaption></figure>')
 sections.append('<section class="sec"><div class="rowp">'+''.join(cards)+'</div></section>')
(here/'screens.html').write_text('<!doctype html><html lang="ru"><meta charset="utf-8"><title>Согласованная часть #247</title><style>'+style+'</style><h1>0.70.2 · Равные интервалы и единые значки</h1><p>Реальные тестовые снимки Android с демонстрационными данными. Основание — макет с двумя зазорами по 1 dp и отдельная команда PM «Делай код».</p>'+''.join(sections)+'</html>')
print('Альбом снимков:',len(items))
