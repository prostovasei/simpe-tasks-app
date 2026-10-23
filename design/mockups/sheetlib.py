import os, sys, json
A='#15803D'; SOFT='#D3ECDB'; T='#15161F'; T2='#5F6474'; PH='#A3ABA7'; LINE='#D5E0D8'; BG='#EDF5EF'

def page(title, heading, body, h=780):
    return f'''<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<title>{title}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<style>
body{{margin:0;font-family:system-ui,-apple-system,"Segoe UI",sans-serif}}
</style>
</helmet>
<div style="width: 380px; height: {h}px; box-sizing: border-box; position: relative; overflow: hidden; background: #FFFFFF; display: flex; flex-direction: column;">
<div style="font-size: 13px; font-weight: 700; color: {T}; padding: 10px 12px; background: #FFFFFF; z-index: 2;">{heading}</div>
{body}
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":380,"height":{h}}}}}'>
class Component extends DCLogic {{
  renderVals() {{ return {{}}; }}
}}
</script>
</body>
</html>
'''

def ico(path, color, size=22, sw=2):
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke="{color}" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="{path}"/></svg>'
P_CAL='M4 6h16v14H4zM4 10h16M9 3v4M15 3v4'
P_INBOX='M3 13h5l2 3h4l2-3h5M5 5h14l2 8v6H3v-6z'
P_REP='M17 3l3 3-3 3M4 11v-1a4 4 0 0 1 4-4h12M7 21l-3-3 3-3M20 13v1a4 4 0 0 1-4 4H4'
P_BELL='M6 10a6 6 0 0 1 12 0c0 5 2 7 2 7H4s2-2 2-7M10 20.5a2 2 0 0 0 4 0'
P_FLAG='M5 21V4h11l-2 4 2 4H5'
P_LIST='M9 6h11M9 12h11M9 18h11M4 6l1 1 2-2M4 12l1 1 2-2M4 18l1 1 2-2'
P_CHECK='M5 12l5 5 9-10'
P_CHEV='M9 6l6 6-6 6'
P_CLOCK='M12 7v5l3 2M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z'
P_X='M6 6l12 12M18 6L6 18'

def tool(path, active=False):
    bg = A if active else '#FFFFFF'; c = '#FFFFFF' if active else T
    br = A if active else LINE
    return f'<span style="width: 44px; height: 44px; box-sizing: border-box; border-radius: 12px; border: 1.5px solid {br}; background: {bg}; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">{ico(path, c, 20)}</span>'

BACKDROP = f'''<div style="position: absolute; left: 0; right: 0; top: 38px; bottom: 0; background: #7E8794;">
<div style="margin: 10px; border-radius: 18px; background: #949CA6; padding: 10px; display: flex; flex-direction: column; gap: 8px;">
<div style="font-size: 12px; font-weight: 700; color: #2A2E36;">ПН, 19 окт · Сегодня</div>
<div style="height: 40px; border-radius: 10px; background: #A9B0B8;"></div>
<div style="height: 40px; border-radius: 10px; background: #A9B0B8;"></div>
<div style="height: 56px; border-radius: 10px; background: #A9B0B8;"></div>
<div style="height: 40px; border-radius: 10px; background: #A9B0B8;"></div>
</div>
</div>'''

def sheet(middle, toolbar_cal=False, cal_active=False, note=''):
    tools = (tool(P_CAL, cal_active) if toolbar_cal else '') + tool(P_INBOX) + tool(P_REP) + tool(P_BELL, True) + tool(P_FLAG) + tool(P_LIST)
    return f'''{BACKDROP}
<div style="position: absolute; left: 0; right: 0; bottom: 0; background: #FFFFFF; border-radius: 24px 24px 0 0; padding: 10px 16px 14px 16px; display: flex; flex-direction: column; gap: 12px; box-shadow: 0 -4px 20px rgba(0,0,0,0.12);">
<div style="align-self: center; width: 40px; height: 4px; border-radius: 2px; background: {LINE};"></div>
<div style="font-size: 20px; font-weight: 700; color: {T};">Купить билеты</div>
<div style="font-size: 15px; font-weight: 600; color: {PH};">Описание</div>
{middle}
<div style="display: flex; align-items: center; gap: 6px; padding-top: 4px;">
{tools}
<span style="flex-grow: 1;"></span>
<span style="width: 50px; height: 50px; border-radius: 14px; background: {A}; display: flex; align-items: center; justify-content: center;">{ico(P_CHECK, '#FFFFFF', 24, 2.6)}</span>
</div>
{note}
</div>'''

def hint(text):
    return f'<div style="font-size: 11px; color: {T2}; background: #FFF6D6; border-radius: 8px; padding: 6px 8px;">👆 {text}</div>'

