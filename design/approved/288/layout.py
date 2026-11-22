from pathlib import Path
import argparse
from io import BytesIO
import runpy
from subprocess import check_output

from PIL import Image, ImageDraw, ImageFont

parser = argparse.ArgumentParser()
parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[3])
parser.add_argument("--output", type=Path, default=Path(__file__).with_name("layout.png"))
args = parser.parse_args()
resources = args.repo / "app/src/main/res/font"
base = "5907a8e38d3d0e30a66c1b6c1b20a20c45c7c6a9"

SCALE = 2
INK = "#16171D"
MUTED = "#62656E"
SURFACE = "#FFFFFF"
BACKGROUND = "#F1F2F5"

board = Image.new("RGB", (2016, 1368), BACKGROUND)
draw = ImageDraw.Draw(board)
fonts = {}


def font(sp, weight=400):
    key = sp, weight
    if key not in fonts:
        fonts[key] = ImageFont.truetype(resources / f"manrope_{weight}.ttf", sp * SCALE)
    return fonts[key]


def point(x, y):
    return round(x * SCALE), round(y * SCALE)


def text(surface, x, y, value, sp, color=INK, weight=400, anchor="lt"):
    ImageDraw.Draw(surface).text(point(x, y), value, fill=color, font=font(sp, weight), anchor=anchor)


def wrap(value, width, sp):
    lines = []
    current = ""
    for word in value.split():
        candidate = f"{current} {word}".strip()
        if current and font(sp).getlength(candidate) > width * SCALE:
            lines.append(current)
            current = word
        else:
            current = candidate
    return [*lines, current]


stories = [
    ("Сегодня", "22:18", "Футбол", "Казанская команда выиграла матч 2:1"),
    ("Сегодня", "21:47", "Хоккей", "Нападающий забросил три шайбы в домашней игре"),
    ("Сегодня", "20:35", "Теннис", "Определились финалисты итогового турнира"),
    ("Сегодня", "19:52", "Баскетбол", "Матч лиги завершился в овертайме"),
    ("Вчера", "23:51", "Лыжи", "Лыжники открыли сезон спринтерской гонкой"),
    ("Вчера", "22:46", "Фигурное катание", "На этапе Гран-при разыграли первые медали"),
]


def bottom_bar(filename, height):
    data = check_output(["git", "show", f"{base}:app/screenshots/{filename}"], cwd=args.repo)
    with Image.open(BytesIO(data)) as screenshot:
        return screenshot.convert("RGB").crop((0, screenshot.height - height * SCALE, screenshot.width, screenshot.height))


app_bar = bottom_bar("news_feed_light.png", 44)
wide_bar = bottom_bar("widget_news_wide_light.png", 40)
tiny_bar = bottom_bar("widget_news_compact_light.png", 40)
medium_bar = Image.new("RGB", (640, 80), SURFACE)
medium_bar.paste(tiny_bar.crop((8, 0, 104, 80)), (8, 0))
for index in range(4):
    center = 392 + (index - 1.5) * 72
    new_center = 640 - 12 - (3.5 - index) * 62
    cell = wide_bar.crop((int(center - 24), 8, int(center + 24), 72))
    medium_bar.paste(cell, (int(new_center - 24), 8))


lightning = runpy.run_path(str(Path(__file__).with_name("icons.py")))["icon"](8).resize((48, 48), Image.Resampling.LANCZOS)
for bar, center in ((app_bar, 504), (wide_bar, 500), (medium_bar, 597), (tiny_bar, 316)):
    top = (bar.height - 48) // 2
    box = (center - 24, top, center + 24, top + 48)
    bar.paste(SURFACE, box)
    bar.paste(INK, box, lightning)


def panel(x, y, width, height, caption, bar):
    text(board, x, y - 21, caption, 13, MUTED, 700)
    screen = Image.new("RGB", point(width, height), SURFACE)
    viewport_height = height - bar.height // SCALE
    feed = Image.new("RGB", point(width, viewport_height), SURFACE)
    inset = 8
    text(feed, inset, 5, "Сегодня", 12, MUTED, 700)
    text(feed, width - inset, 5, "22:34", 11, MUTED, anchor="rt")
    cursor = 23
    previous_day = "Сегодня"
    for day, time, sport, title in stories:
        if cursor >= viewport_height:
            break
        if day != previous_day:
            text(feed, inset, cursor + 4, day, 12, MUTED, 700)
            cursor += 22
        previous_day = day
        text(feed, inset, cursor, f"{time} · {sport}", 11, MUTED)
        cursor += 14
        for row in wrap(title, width - 2 * inset, 14):
            text(feed, inset, cursor, row, 14)
            cursor += 17
        cursor += 8
    screen.paste(feed, (0, 0))
    screen.paste(bar, point(0, viewport_height))
    radius = 14 if width == 180 else 19
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen.width - 1, screen.height - 1), radius * SCALE, fill=255)
    draw.rounded_rectangle((*point(x - 1, y - 1), *point(x + width, y + height)), (radius + 1) * SCALE, fill="#DADCE3")
    board.paste(screen, point(x, y), mask)


text(board, 37, 25, "Лента новостей", 29, weight=800)
panel(38, 97, 360, 550, "Приложение · 360 dp", app_bar)
panel(431, 97, 392, 308, "Широкий виджет · 392 dp", wide_bar)
panel(431, 431, 320, 207, "Виджет · 320 dp", medium_bar)
panel(770, 431, 180, 110, "Минимальный виджет · 180 × 110 dp", tiny_bar)
board.save(args.output, optimize=True)
