"""#249. Альбом фактических снимков; каркас листов approved/241/screens.py.
Перед запуском: ./gradlew recordRoborazziDebug. PNG выбранных эскизов сохранены отдельно.
"""
from pathlib import Path
import base64
here=Path(__file__).parent
root=here.parents[2]
shots=root/'app/screenshots'
items=[('Неделя · приложение','week_light','Время слева от выполнения; фиолетовый приоритет, белый фон.'),('Предстоящее · приложение','all_light','Сохраняются времена, интервалы и действия.'),('Приоритет: активный / выполненный / повтор','week_priority_states','Фиолетовые время, значок и название; выполненная — бледная и зачёркнутая.'),('Проект','project_light','Порядок, даты, меню и плавающий плюс прежние.'),('Предстоящее · виджет','widget_light','Тот же стиль; нижняя панель виджета прежняя.'),('Приоритет · виджет','widget_week_priority_states','Высокий приоритет и выполненные состояния.'),('Неделя · виджет','widget_week_light','Календарь и приоритетный индикатор сохранены.')]
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
(here/'screens.html').write_text('<!doctype html><html lang="ru"><meta charset="utf-8"><title>Согласованная часть #247</title><style>'+style+'</style><h1>0.70.0 · Белый фон и приоритеты недели</h1><p>Реальные тестовые снимки Android с демонстрационными данными. Основание — выбранные PM эскизы, ссылки в README.</p>'+''.join(sections)+'</html>')
print('Альбом снимков:',len(items))
