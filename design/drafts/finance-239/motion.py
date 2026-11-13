"""Финансы #239, пункт 7 — превью анимаций перехода на выбор PM (13.11.2026). Кадры собираются из снимков экранов приложения (app/screenshots).
Запуск из корня: python3 design/drafts/finance-239/motion.py → motion-A.gif, motion-B.gif, motion-C.gif и motion-storyboard.png.
Три сцены в каждом превью: «вперёд» (Операции → Категории), «назад жестом» (палец ведёт экран, затем отпускает), «смена модуля» (Финансы → Задачи).
Панели модулей в превью едут вместе с экраном — на телефоне верхняя и нижняя панели стоят на месте, двигается только содержимое."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

here = Path(__file__).parent
shots = here.parent.parent.parent / 'app' / 'screenshots'
W, H = 270, 608
BAND = 44
FPS = 30
FONT = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 15)


def load(name):
    return Image.open(shots / f'{name}.png').convert('RGB').resize((W, H), Image.LANCZOS)


OPS, CATS, TASKS = load('finance_operations_light'), load('finance_categories_emoji_light'), load('week_light')


def ease_out(t):
    return 1 - (1 - t) ** 3


def ease_in_out(t):
    return 4 * t ** 3 if t < .5 else 1 - (-2 * t + 2) ** 3 / 2


def dim(img, k):
    return Image.blend(img, Image.new('RGB', img.size, (0, 0, 0)), k) if k > 0 else img


def shadow_edge(canvas, x):
    """Тень слева у наезжающего экрана — чтобы было видно, что он сверху."""
    d = ImageDraw.Draw(canvas, 'RGBA')
    for i in range(10):
        d.line([(x - i, 0), (x - i, H)], fill=(0, 0, 0, max(0, 40 - i * 4)))


def frame(variant, under, over, p):
    """Кадр перехода «вперёд» с прогрессом p∈[0,1]: [under] — прежний экран, [over] — новый (для «назад» — наоборот, p идёт 1→0)."""
    c = Image.new('RGB', (W, H), 'white')
    if variant == 'A':  # сдвиг целиком: оба экрана едут на полную ширину
        c.paste(under, (round(-W * p), 0))
        c.paste(over, (round(W * (1 - p)), 0))
    elif variant == 'B':  # наезд: новый поверх, прежний чуть сдвигается и темнеет
        c.paste(dim(under, .22 * p), (round(-W * .3 * p), 0))
        x = round(W * (1 - p))
        c.paste(over, (x, 0))
        if 0 < x < W:
            shadow_edge(c, x)
    else:  # C — сдвиг с растворением: небольшой сдвиг на 10% и смена прозрачности
        a = round(-W * .1 * p)
        b = round(W * .1 * (1 - p))
        old = Image.new('RGB', (W, H), 'white')
        old.paste(under, (a, 0))
        new = Image.new('RGB', (W, H), 'white')
        new.paste(over, (b, 0))
        fade_old = min(1, p * 2)  # прежний уходит за первую половину
        fade_new = max(0, p * 2 - 1)  # новый появляется во второй
        base = Image.blend(old, Image.new('RGB', (W, H), 'white'), fade_old)
        c = Image.blend(base, new, fade_new)
    return c


def caption(img, text, finger=None):
    out = Image.new('RGB', (W, H + BAND), (232, 232, 232))
    out.paste(img, (0, 0))
    d = ImageDraw.Draw(out)
    d.text((W // 2, H + BAND // 2), text, fill=(22, 23, 29), font=FONT, anchor='mm')
    if finger is not None:
        dd = ImageDraw.Draw(out, 'RGBA')
        dd.ellipse([finger - 14, H * .45 - 14, finger + 14, H * .45 + 14], fill=(30, 30, 30, 90), outline=(255, 255, 255, 200), width=2)
    return out


def scenes(variant):
    frames = []
    hold = lambda img, text, n=14: frames.extend([caption(img, text)] * n)
    # 1. Вперёд: Операции → Категории (300 мс).
    hold(OPS, 'Вперёд')
    for i in range(10):
        frames.append(caption(frame(variant, OPS, CATS, ease_out((i + 1) / 10)), 'Вперёд'))
    hold(CATS, 'Вперёд')
    # 2. Назад жестом: палец ведёт экран до 40%, держит, отпускает — экран доезжает (как «назад» по стрелке, только под пальцем).
    drag = [i / 14 * .4 for i in range(15)]
    for q in drag:
        frames.append(caption(frame(variant, OPS, CATS, 1 - q), 'Назад жестом', finger=round(W * q)))
    frames.extend([caption(frame(variant, OPS, CATS, .6), 'Назад жестом', finger=round(W * .4))] * 6)
    for i in range(9):
        q = .4 + .6 * ease_out((i + 1) / 9)
        frames.append(caption(frame(variant, OPS, CATS, 1 - q), 'Назад жестом'))
    hold(OPS, 'Назад жестом')
    # 3. Смена модуля: Финансы → Задачи (задачи левее финансов — приходят слева).
    hold(OPS, 'Смена модуля', 8)
    for i in range(10):
        frames.append(caption(frame(variant, TASKS, OPS, 1 - ease_in_out((i + 1) / 10)), 'Смена модуля'))
    hold(TASKS, 'Смена модуля', 24)
    return frames


for v in 'ABC':
    fr = scenes(v)
    fr[0].save(here / f'motion-{v}.gif', save_all=True, append_images=fr[1:], duration=round(1000 / FPS), loop=0, optimize=True)

# Раскадровка «вперёд» для README: строки — варианты, столбцы — 0/25/50/75/100%.
TW, TH = 180, 405
board = Image.new('RGB', (40 + 5 * (TW + 12), 3 * (TH + 34) + 10), (232, 232, 232))
d = ImageDraw.Draw(board)
for r, v in enumerate('ABC'):
    d.text((20, r * (TH + 34) + TH // 2 + 10), v, fill=(22, 23, 29), font=FONT, anchor='mm')
    for k, p in enumerate([0, .25, .5, .75, 1]):
        img = frame(v, OPS, CATS, ease_out(p)).resize((TW, TH), Image.LANCZOS)
        x, y = 40 + k * (TW + 12), r * (TH + 34) + 10
        board.paste(img, (x, y))
        d.text((x + TW // 2, y + TH + 12), f'{round(p * 100)}%', fill=(98, 101, 110), font=FONT, anchor='mm')
board.save(here / 'motion-storyboard.png')
print('motion-A.gif, motion-B.gif, motion-C.gif, motion-storyboard.png готовы')
