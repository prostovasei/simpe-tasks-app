"""Прототип #329 «Хаб „Главная“». Запуск из корня: python3 design/approved/329/build.py && node design/approved/329/render.js
Иконки — векторные ресурсы приложения (res/drawable), цвета — токены ColorTokens, шрифт Manrope. Данные вымышленные, «сейчас» — вт, 24 ноября 2026."""
import json
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent
DRAWABLE = HERE.parents[2] / 'app/src/main/res/drawable'
ANDROID = '{http://schemas.android.com/apk/res/android}'


def icon(name):
    """Векторный ресурс Android → SVG с цветом от currentColor."""
    vector = ET.parse(DRAWABLE / f'{name}.xml').getroot()
    width = vector.get(ANDROID + 'viewportWidth')
    height = vector.get(ANDROID + 'viewportHeight')
    paths = []
    for path in vector.iter('path'):
        fill = path.get(ANDROID + 'fillColor')
        attrs = {'d': path.get(ANDROID + 'pathData'), 'fill': 'currentColor' if fill and '#00' not in fill else 'none'}
        for source, target in [('strokeWidth', 'stroke-width'), ('strokeLineCap', 'stroke-linecap'), ('strokeLineJoin', 'stroke-linejoin')]:
            if value := path.get(ANDROID + source):
                attrs[target] = value
        if path.get(ANDROID + 'strokeColor'):
            attrs['stroke'] = 'currentColor'
        paths.append('<path ' + ' '.join(f'{k}="{v}"' for k, v in attrs.items()) + '/>')
    return f'<svg viewBox="0 0 {width} {height}" aria-hidden="true">{"".join(paths)}</svg>'


STROKE = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="{d}" fill="none" stroke="currentColor" stroke-width="{w}" stroke-linecap="round" stroke-linejoin="round"/></svg>'
ICONS = {
    'week': icon('ic_tab_week'),
    'finance': icon('ic_module_finance'),
    'health': icon('ic_module_health'),
    'news': icon('ic_module_news'),
    'settings': icon('ic_nav_settings'),
    'add': icon('ic_widget_add'),
    'forecast': icon('ic_finance_forecast'),
    'refresh': icon('ic_news_refresh'),
    # Домик — новый значок в стиле значков панели (линия 2, круглые концы).
    'home': STROKE.format(d='M4.5 10.2 12 4l7.5 6.2V19a1 1 0 0 1-1 1H15v-5.5H9V20H5.5a1 1 0 0 1-1-1z', w='2'),
    'chevron': STROKE.format(d='M9.5 6l6 6-6 6', w='2'),
    'check': STROKE.format(d='M5 12.5l4.5 4.5L19 7.5', w='2.6'),
    'repeat': STROKE.format(d='M17 2.8l3 3-3 3M4 11V9.8a4 4 0 0 1 4-4h12M7 21.2l-3-3 3-3M20 13v1.2a4 4 0 0 1-4 4H4', w='1.8'),
    'task': STROKE.format(d='M4.5 12.5l4 4L19.5 6', w='2.2'),
    'download': STROKE.format(d='M12 4v11m-4.5-4.5L12 15l4.5-4.5M5 20h14', w='2'),
    'left': STROKE.format(d='M14.5 6l-6 6 6 6', w='2.2'),
    'right': STROKE.format(d='M9.5 6l6 6-6 6', w='2.2'),
}

TEMPLATE = (HERE / 'prototype.template.html').read_text()
html = TEMPLATE.replace('/*ICONS*/null', json.dumps(ICONS, ensure_ascii=False))
(HERE / 'prototype.html').write_text(html)
print('prototype.html', len(html))
