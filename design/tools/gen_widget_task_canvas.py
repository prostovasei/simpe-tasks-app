"""#247: fixed-light RemoteViews from the existing layouts, preserving their geometry.
Run after editing the original widget row layouts or exporting color tokens.
The old layouts remain the Inbox style; *_flat layouts have distinct recycler view types.
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'app/src/main/res'
NAMES = ['widget_row_task', 'widget_row_task_done', 'widget_row_period', 'widget_row_period_done',
         'widget_row_plaque', 'widget_row_plaque_done', 'widget_row_time', 'widget_row_title',
         'widget_row_now', 'widget_row_overdue']
needed = set()
for name in NAMES:
    src = (RES / 'layout' / f'{name}.xml').read_text()
    for inc in NAMES:
        src = src.replace(f'@layout/{inc}"', f'@layout/{inc}_flat"')
    src = re.sub(r'@color/widget_(\w+)', r'@color/widget_flat_\1', src)
    if name in ['widget_row_plaque', 'widget_row_plaque_done']:
        src = src.replace('        android:background="@drawable/widget_row_plaque"\n', '')
    if name == 'widget_row_title':
        src = src.replace('        android:background="@drawable/widget_row_chip"\n', '')
        src = src.replace('@drawable/widget_row_progress', '@drawable/widget_flat_progress')
    if name in ['widget_row_task', 'widget_row_task_done', 'widget_row_period', 'widget_row_period_done']:
        src = src.replace('android:paddingEnd="@dimen/widget_list_edge"', 'android:paddingStart="@dimen/widget_edge" android:paddingEnd="@dimen/widget_list_edge"')
        src = src.replace('android:layout_width="@dimen/widget_rail"', 'android:id="@+id/row_rail" android:layout_marginStart="@dimen/widget_week_content_gap" android:layout_width="@dimen/widget_week_check"')
        src = src.replace('android:layout_width="@dimen/widget_node"', 'android:layout_width="@dimen/widget_week_check"')
        src = src.replace('android:layout_height="@dimen/widget_node"', 'android:layout_height="@dimen/widget_week_check"')
        src = src.replace('@dimen/widget_node_check', '@dimen/widget_week_tick').replace('@color/widget_flat_on_accent', '@color/widget_task_surface')
    if name in ['widget_row_plaque', 'widget_row_plaque_done']:
        src = src.replace('android:paddingStart="@dimen/widget_space_sm"', 'android:paddingStart="@dimen/widget_week_content_gap"')
    if name == 'widget_row_time':
        src = src.replace('android:layout_width="@dimen/widget_time"', 'android:layout_width="wrap_content"')
        src = src.replace('android:textStyle="bold"', 'android:fontFeatureSettings="tnum" android:textStyle="bold"')
    if name == 'widget_row_now':
        src = src.replace('android:paddingEnd="@dimen/widget_list_edge"', 'android:paddingStart="@dimen/widget_edge" android:paddingEnd="@dimen/widget_list_edge"')
        src = src.replace('android:layout_width="@dimen/widget_time"', 'android:id="@+id/now_time" android:layout_width="wrap_content"')
        src = src.replace('android:layout_width="@dimen/widget_rail"', 'android:layout_marginStart="@dimen/widget_week_content_gap" android:layout_marginEnd="@dimen/widget_week_content_gap" android:layout_width="@dimen/widget_week_check"')
    (RES / 'layout' / f'{name}_flat.xml').write_text(src)
    needed.update(re.findall(r'@color/(widget_flat_\w+)', src))
src = (RES / 'drawable/widget_row_progress.xml').read_text()
src = re.sub(r'@color/widget_(\w+)', r'@color/widget_flat_\1', src)
(RES / 'drawable/widget_flat_progress.xml').write_text(src)
needed.update(re.findall(r'@color/(widget_flat_\w+)', src))
colors = {}
for path in (RES / 'values').glob('*.xml'):
    for node in ET.parse(path).getroot():
        name = node.get('name', '')
        if node.tag == 'color' and name.startswith('widget_') and not name.startswith(('widget_flat_', 'widget_task_')):
            if node.text and node.text.startswith('#'):
                colors[name.replace('widget_', 'widget_flat_', 1)] = node.text
canvas = (ROOT / 'app/src/main/java/com/prostovasei/simpletasks/designsystem/tokens/ColorTokens.kt').read_text().split('object TaskCanvasTokens {', 1)[1]
colors['widget_flat_divider'] = '#' + re.search(r'val divider = ThemedColor\(light = 0x([0-9A-F]+)', canvas).group(1)
assert needed <= colors.keys(), needed - colors.keys()
(RES / 'values/widget_flat_palette.xml').write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n<!-- Fixed light copies of ColorTokens / TaskCanvasTokens for compact RemoteViews, #247. -->\n<resources>\n'
    + ''.join(f'    <color name="{name}">{colors[name]}</color>\n' for name in sorted(needed)) + '</resources>\n')
print(f'Updated {len(NAMES)} layouts and {len(needed)} palette entries')
