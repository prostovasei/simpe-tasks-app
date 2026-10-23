"""Общие куски макетов в стиле рефа Rondesignlab: шрифт Manrope из приложения, градиент карточки, кнопки-кружки, плитки."""
import os, sys, json, base64
sys.path.insert(0, os.path.dirname(__file__))
from sheetlib import ico, P_CAL, P_INBOX, P_BELL, P_REP, P_CHECK, P_CLOCK
FONT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'res', 'font')
FACES = ''.join(
    f"@font-face{{font-family:'Manrope';font-weight:{w};src:url(data:font/ttf;base64,{base64.b64encode(open(f'{FONT_DIR}/manrope_{w}.ttf', 'rb').read()).decode()}) format('truetype');}}"
    for w in (400, 500, 600, 700))

P_ARROW = 'M7 17L17 7M9 7h8v8'
P_SEARCH = 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20 20l-4-4'
P_MENU = 'M5 8h14M5 16h9'
P_WARN = 'M12 8v5M12 16.5v.5M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z'
P_SUN = 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4'
P_TRAY = 'M4 13l2.5-7h11L20 13v5H4zM4 13h4.5l1 2h5l1-2H20'
P_LIST = 'M8 7h11M8 12h11M8 17h11M4.5 7h.01M4.5 12h.01M4.5 17h.01'
P_HOME = 'M4 11l8-6 8 6v8H4z'

MESH = ('radial-gradient(120% 95% at 92% 8%, #F3C35C 0%, rgba(243,195,92,0) 55%),'
        'radial-gradient(85% 85% at 6% 30%, #6E93AE 0%, rgba(110,147,174,0) 62%),'
        'radial-gradient(110% 80% at 45% 112%, #8A5A37 0%, rgba(138,90,55,0) 62%),'
        'linear-gradient(135deg,#8FA3B0,#B99062)')
GRAIN = ("url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%22180%22 height=%22180%22><filter id=%22n%22>"
         "<feTurbulence type=%22fractalNoise%22 baseFrequency=%22.9%22 numOctaves=%222%22 stitchTiles=%22stitch%22/><feColorMatrix values=%220 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 .5 0%22/></filter>"
         "<rect width=%22100%25%22 height=%22100%25%22 filter=%22url(%23n)%22/></svg>')")


def grain(op=.18):
    return f'<div style="position:absolute;inset:0;background-image:{GRAIN};opacity:{op};mix-blend-mode:overlay;pointer-events:none;"></div>'


def circ(path, size=44, line='rgba(0,0,0,.12)', color='#1A1A1A', bg='transparent', isz=20):
    return f'<span style="width:{size}px;height:{size}px;border-radius:50%;border:1px solid {line};background:{bg};display:flex;align-items:center;justify-content:center;flex-shrink:0;box-sizing:border-box;">{ico(path, color, isz, 1.5)}</span>'


def status(c):
    return f'<div style="height:44px;display:flex;align-items:center;justify-content:space-between;padding:0 28px;font-size:14px;font-weight:600;color:{c};"><span>9:41</span><span style="letter-spacing:1px;">●●● ▬</span></div>'


def header(title, c, line, left=P_MENU, right=P_SEARCH):
    return (f'<div style="height:60px;display:flex;align-items:center;justify-content:space-between;padding:0 18px;">{circ(left, 46, line, c)}'
            f'<span style="font-size:19px;font-weight:500;color:{c};letter-spacing:-.2px;">{title}</span>{circ(right, 46, line, c)}</div>')


def dock(dark=False):
    bg = '#FFFFFF' if dark else '#161616'
    ic = '#161616' if dark else '#FFFFFF'
    ic2 = 'rgba(22,22,22,.45)' if dark else 'rgba(255,255,255,.5)'
    plus_bg = '#161616' if dark else '#FFFFFF'
    plus_c = '#FFFFFF' if dark else '#161616'
    icons = ''.join(f'<span style="width:48px;height:48px;display:flex;align-items:center;justify-content:center;">{ico(p, ic if i == 0 else ic2, 23, 1.6)}</span>' for i, p in enumerate((P_HOME, P_TRAY, P_LIST, P_SEARCH)))
    return (f'<div style="position:absolute;left:18px;right:18px;bottom:22px;height:72px;border-radius:36px;background:{bg};display:flex;align-items:center;padding:0 10px 0 16px;box-shadow:0 12px 30px rgba(0,0,0,.18);">'
            f'{icons}<span style="flex:1"></span><span style="width:54px;height:54px;border-radius:50%;background:{plus_bg};display:flex;align-items:center;justify-content:center;">{ico("M12 5v14M5 12h14", plus_c, 24, 1.8)}</span></div>')


def dots(n, done, c='#FFFFFF'):
    out = ''
    for i in range(n):
        if i == done - 1:
            out += f'<span style="width:22px;height:22px;border-radius:50%;background:{c};"></span>'
        elif i < done:
            out += f'<span style="width:10px;height:10px;border-radius:50%;background:{c};opacity:.85;"></span>'
        else:
            out += f'<span style="width:6px;height:6px;border-radius:50%;background:{c};opacity:.45;"></span>'
    return f'<span style="display:flex;align-items:center;gap:9px;">{out}</span>'


def hero(label, big, small, right, h=208, extra=''):
    return (f'<div style="position:relative;margin:6px 16px 0;height:{h}px;border-radius:34px;overflow:hidden;background:{MESH};">{grain(.22)}'
            f'<div style="position:absolute;inset:0;padding:18px;display:flex;flex-direction:column;">'
            f'<div style="display:flex;justify-content:space-between;">{circ(P_CHECK, 46, "rgba(255,255,255,.55)", "#FFFFFF")}'
            f'<span style="width:50px;height:50px;border-radius:50%;background:#FFFFFF;display:flex;align-items:center;justify-content:center;">{ico(P_ARROW, "#161616", 20, 1.6)}</span></div>'
            f'<div style="font-size:19px;font-weight:500;color:#FFFFFF;margin-top:16px;">{label}</div>{extra}<span style="flex:1"></span>'
            f'<div style="display:flex;align-items:flex-end;justify-content:space-between;"><span style="color:#FFFFFF;font-weight:400;font-size:58px;line-height:52px;letter-spacing:-2px;">{big}<span style="font-size:18px;letter-spacing:0;opacity:.8;margin-left:4px;">{small}</span></span>{right}</div></div></div>')


def bars(vals, c):
    return '<span style="display:flex;align-items:flex-end;gap:3px;height:22px;">' + ''.join(f'<span style="width:3px;height:{v}px;border-radius:2px;background:{c};"></span>' for v in vals) + '</span>'


def wave(c):
    return f'<svg width="62" height="22" viewBox="0 0 62 22" fill="none"><path d="M1 16c5 0 6-10 11-10s6 12 11 12 6-14 11-14 6 10 11 10 6-6 11-6" stroke="{c}" stroke-width="1.5" stroke-linecap="round"/></svg>'


def tile(path, l1, l2, num, graphic, dark=False, accent=None):
    bg = '#1E1D1B' if dark else '#FFFFFF'
    c = '#F2EFEA' if dark else '#161616'
    c2 = 'rgba(242,239,234,.55)' if dark else '#77726B'
    line = 'rgba(255,255,255,.14)' if dark else 'rgba(0,0,0,.10)'
    dot = f'<span style="width:8px;height:8px;border-radius:50%;background:{accent};margin-left:6px;"></span>' if accent else ''
    return (f'<div style="height:148px;border-radius:30px;background:{bg};padding:16px;box-sizing:border-box;display:flex;flex-direction:column;">'
            f'<div style="display:flex;align-items:center;gap:10px;">{circ(path, 42, line, c, isz=19)}<span style="font-size:13px;line-height:16px;color:{c2};font-weight:500;">{l1}<br>{l2}</span></div><span style="flex:1"></span>'
            f'<div style="display:flex;align-items:flex-end;justify-content:space-between;"><span style="display:flex;align-items:center;font-size:36px;line-height:36px;font-weight:400;color:{c};letter-spacing:-1px;">{num}{dot}</span>{graphic}</div></div>')


def task_tile(title, meta, time='', dark=False, overdue=False, done=False, bell=False):
    bg = '#1E1D1B' if dark else '#FFFFFF'
    c = '#F2EFEA' if dark else '#161616'
    c2 = 'rgba(242,239,234,.55)' if dark else '#8A847C'
    line = 'rgba(255,255,255,.22)' if dark else 'rgba(0,0,0,.18)'
    ring = '#D9534F' if overdue else line
    chk = (f'<span style="width:30px;height:30px;border-radius:50%;background:{c};display:flex;align-items:center;justify-content:center;flex-shrink:0;">{ico(P_CHECK, bg, 16, 2)}</span>' if done else
           f'<span style="width:30px;height:30px;border-radius:50%;border:1.5px solid {ring};box-sizing:border-box;flex-shrink:0;"></span>')
    tc = '#D9534F' if overdue else c
    b = ico(P_BELL, c2, 18, 1.5) if bell else ''
    t = f'<span style="font-size:16px;font-weight:500;color:{tc};letter-spacing:-.3px;">{time}</span>' if time else ''
    st = 'text-decoration:line-through;opacity:.45;' if done else ''
    return (f'<div style="height:74px;border-radius:26px;background:{bg};display:flex;align-items:center;gap:14px;padding:0 18px;margin-top:8px;">{chk}'
            f'<div style="flex:1;min-width:0;"><div style="font-size:17px;font-weight:600;color:{c};letter-spacing:-.3px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;{st}">{title}</div>'
            f'<div style="font-size:13px;color:{c2};margin-top:3px;">{meta}</div></div>{b}{t}</div>')


def day(label, c):
    return f'<div style="font-size:12px;font-weight:600;letter-spacing:1.4px;color:{c};padding:22px 6px 4px;">{label}</div>'


def phone(bg, inner, dark=False):
    return (f'<div style="position:relative;width:390px;height:844px;border-radius:48px;overflow:hidden;background:{bg};font-family:Manrope,sans-serif;border:8px solid #111;box-sizing:content-box;">'
            f'{inner}</div>')


