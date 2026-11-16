"""#257. Альбом фактических Android-снимков раздела «Здоровье» (0.71.0): строится из app/screenshots после ./gradlew recordRoborazziDebug.
Это воспроизведение реального вида, а не новые эскизы; согласованные макеты — screens.html. Запуск из корня: python3 design/approved/257/real.py → real.html."""
from pathlib import Path
import base64

here = Path(__file__).parent
shots = here.parents[2] / 'app' / 'screenshots'
items = [
    ('Вес — главный экран', 'health_main_light', 'Вес, изменение за месяц, цель, переключатель периода, график, история.'),
    ('Подсказка у точки', 'health_tooltip_light', 'Нажатие на точку: «вес · дата», запись подсвечена в истории.'),
    ('Первый вход', 'health_empty_light', 'Записей нет.'),
    ('Одна запись', 'health_one_entry_light', 'Изменение скрыто, цель не задана.'),
    ('Новая запись', 'health_weight_new_light', 'Барабаны, дата, комментарий.'),
    ('Дата занята', 'health_weight_replace_light', 'Предупреждение «заменится».'),
    ('Правка записи', 'health_weight_edit_light', 'В заголовке корзина.'),
    ('Календарь', 'health_calendar_light', 'Точка — день с записью, будущие даты недоступны.'),
    ('Цель и рост', 'health_goal_light', 'Барабаны цели и роста, ИМТ.'),
    ('Настройки: напоминание выключено', 'health_settings_off_light', ''),
    ('Настройки: напоминание включено', 'health_settings_light', ''),
    ('Восстановление копии', 'health_restore_light', 'Сравнение «в копии / сейчас».'),
]
items = [i for i in items if (shots / (i[1] + '.png')).exists()]
assert items, 'Сначала запишите Android-снимки: ./gradlew recordRoborazziDebug'
style = ('body{font-family:Arial,sans-serif;background:#ededee;color:#16171d;margin:0;padding:24px}h1{font-size:26px}p{font-size:13px;color:#62656e}.sec{padding:20px 0}'
         '.rowp{display:flex;gap:24px;align-items:flex-start}figure{margin:0;width:390px}img{width:390px;display:block;box-shadow:0 0 0 1px #d4d4d8}figcaption{font-size:13px;margin-top:12px;line-height:1.6}h2{font-size:18px;margin:0 0 8px}')
sections = []
for start in range(0, len(items), 3):
    cards = []
    for title, name, desc in items[start:start + 3]:
        img = base64.b64encode((shots / (name + '.png')).read_bytes()).decode()
        cards.append(f'<figure><h2>{title}</h2><img src="data:image/png;base64,{img}"><figcaption>{desc}</figcaption></figure>')
    sections.append('<section class="sec"><div class="rowp">' + ''.join(cards) + '</div></section>')
(here / 'real.html').write_text('<!doctype html><html lang="ru"><meta charset="utf-8"><title>Здоровье — реальные снимки (#257)</title><style>' + style + '</style>'
                                '<h1>0.71.0 · Здоровье: вес</h1><p>Реальные тестовые снимки Android с вымышленными данными.</p>' + ''.join(sections) + '</html>')
print('Альбом снимков:', len(items))
