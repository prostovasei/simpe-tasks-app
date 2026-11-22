"""Draw alternative Simple Tasks tab icons and show them in the widget panel."""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

SURFACE = "#ffffff"
CANVAS = "#f2f3f6"
TEXT = "#16171d"
SECONDARY = "#62656e"
INACTIVE = "#a5a7ad"
BORDER = "#e1e2e7"
FONT = Path("/usr/share/fonts/truetype/dejavu")

VARIANTS = [
    "Открытая газета",
    "Лист с загибом",
    "Стопка выпусков",
    "Две колонки",
    "Лента",
    "Микрофон",
    "Эфир",
    "Молния",
    "Свисток",
    "Кубок",
]


class Mark:
    size = 192
    density = size / 24

    def __init__(self):
        self.mask = Image.new("L", (self.size, self.size))
        self.draw = ImageDraw.Draw(self.mask)

    def path(self, *points, weight=1.8):
        vertices = [(round(x * self.density), round(y * self.density)) for x, y in points]
        width = round(weight * self.density)
        self.draw.line(vertices, fill=255, width=width, joint="curve")
        radius = (width - 1) / 2
        for x, y in vertices:
            self.draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=255)

    def rounded(self, x1, y1, x2, y2, radius=1.5, weight=1.8):
        offset = weight / 2
        box = tuple(round(v * self.density) for v in (x1 - offset, y1 - offset, x2 + offset, y2 + offset))
        self.draw.rounded_rectangle(box, radius=round((radius + offset) * self.density),
                                    outline=255, width=round(weight * self.density))

    def dot(self, x, y, radius=1):
        box = tuple(round(v * self.density) for v in (x - radius, y - radius, x + radius, y + radius))
        self.draw.ellipse(box, fill=255)

    def arc(self, x1, y1, x2, y2, start, end, weight=1.8):
        from math import cos, radians, sin
        center_x, center_y = (x1 + x2) / 2, (y1 + y2) / 2
        radius_x, radius_y = (x2 - x1) / 2, (y2 - y1) / 2
        angles = [radians(start + (end - start) * i / 30) for i in range(31)]
        self.path(*[(center_x + radius_x * cos(angle), center_y + radius_y * sin(angle))
                    for angle in angles], weight=weight)


def icon(number):
    m = Mark()
    match number:
        case 1:
            m.path((12, 7.8), (10.1, 6.1), (7.6, 5.4), (4.8, 5.5), (4.8, 17.6),
                   (7.6, 17.5), (10.8, 18.5), (12, 19.4), (13.2, 18.5), (16.4, 17.5),
                   (19.2, 17.6), (19.2, 5.5), (16.4, 5.4), (13.9, 6.1), (12, 7.8), (12, 19.4))
            m.path((7.1, 9.7), (9.4, 10.4))
            m.path((14.6, 10.4), (16.9, 9.7))
        case 2:
            m.path((6.7, 4.6), (13.7, 4.6), (18, 9), (18, 18), (16.8, 19.4), (6.7, 19.4),
                   (5.6, 18.3), (5.6, 5.7), (6.7, 4.6))
            m.path((13.7, 4.6), (13.7, 9), (18, 9))
            m.path((8.6, 12.7), (14.8, 12.7))
            m.path((8.6, 16), (13, 16))
        case 3:
            m.path((7.7, 4.7), (17.8, 4.7), (19.3, 6.2), (19.3, 15.8))
            m.rounded(4.7, 7.5, 16.5, 19.3)
            m.path((7.7, 11.2), (13.6, 11.2))
            m.path((7.7, 14.5), (13.6, 14.5))
        case 4:
            m.rounded(4.8, 5.7, 19.2, 18.3)
            m.path((4.8, 9.3), (19.2, 9.3))
            m.path((11.8, 9.3), (11.8, 18.3))
            m.path((7.5, 12.8), (9.1, 12.8))
            m.path((14.7, 12.8), (16.4, 12.8))
            m.path((14.7, 15.6), (16.4, 15.6))
        case 5:
            for y, end in ((5.6, 18.9), (12, 17.6), (18.4, 18.9)):
                m.dot(5.6, y, 1.05)
                m.path((9.5, y), (end, y))
        case 6:
            m.rounded(9.3, 4.5, 14.7, 13.9, radius=2.7)
            m.arc(6.5, 7.5, 17.5, 17.3, 0, 180)
            m.path((6.5, 10.8), (6.5, 12.4))
            m.path((17.5, 10.8), (17.5, 12.4))
            m.path((12, 17.3), (12, 19.5))
            m.path((9.6, 19.5), (14.4, 19.5))
        case 7:
            m.dot(12, 12, 1.15)
            for start, end in ((135, 225), (315, 405)):
                m.arc(7.5, 7.5, 16.5, 16.5, start, end)
                m.arc(4.6, 4.6, 19.4, 19.4, start, end)
        case 8:
            m.path((12.6, 4.6), (6.6, 12.8), (11.6, 12.8), (10.7, 19.4), (17.6, 10.9),
                   (12.7, 10.9), (14, 4.6), (12.6, 4.6))
        case 9:
            m.path((5, 10.1), (13.2, 10.1), (16.2, 7.6), (18.9, 7.6), (18.9, 11.1),
                   (16.9, 12.3), (15.7, 15.3), (13.2, 17.1), (9.3, 17.1), (6.9, 16.1),
                   (5, 14.1), (5, 10.1))
            m.dot(11.4, 13.7, 1.35)
            m.path((6.3, 8), (5.2, 7), weight=1.55)
        case 10:
            m.path((8.1, 4.6), (15.9, 4.6), (15.9, 10.5), (15.3, 12.6), (13.8, 14),
                   (12, 14.5), (10.2, 14), (8.7, 12.6), (8.1, 10.5), (8.1, 4.6))
            m.path((8.1, 6.6), (4.9, 6.6), (4.9, 9.4), (5.6, 10.8), (8.1, 11.5))
            m.path((15.9, 6.6), (19.1, 6.6), (19.1, 9.4), (18.4, 10.8), (15.9, 11.5))
            m.path((12, 14.5), (12, 19.4))
            m.path((8.7, 19.4), (15.3, 19.4))
    return m.mask


def existing(symbol):
    m = Mark()
    match symbol:
        case "tasks":
            m.rounded(5.4, 7.1, 18.6, 18.7)
            m.path((5.4, 10.9), (18.6, 10.9))
            m.path((9, 5.4), (9, 8.2))
            m.path((15, 5.4), (15, 8.2))
        case "finance":
            m.path((9.4, 19), (9.4, 5.1), (13.2, 5.1), (15.6, 5.9),
                   (16.4, 7.6), (16.4, 9.1), (15.3, 10.8), (13.2, 11.4), (7.1, 11.4))
            m.path((7.1, 15.1), (14.4, 15.1))
        case "health":
            m.path((12, 18.9), (9.2, 16.8), (6.2, 13.5), (5.2, 10.9), (5.6, 8.6),
                   (6.9, 7.1), (9, 6.7), (10.8, 7.4), (12, 8.7), (13.2, 7.4),
                   (15, 6.7), (17.1, 7.1), (18.4, 8.6), (18.8, 10.9), (17.8, 13.5),
                   (14.8, 16.8), (12, 18.9))
        case "refresh":
            m.arc(5.3, 5.3, 18.7, 18.7, 35, 322)
            m.path((18.2, 5.7), (18.2, 8.8), (15, 8.8))
    return m.mask


def paste_mark(board, mask, center_x, center_y, pixels, color):
    scaled = mask.resize((pixels, pixels), Image.Resampling.LANCZOS)
    board.paste(color, (center_x - pixels // 2, center_y - pixels // 2,
                        center_x - pixels // 2 + pixels, center_y - pixels // 2 + pixels), scaled)


def render(path):
    board = Image.new("RGB", (1512, 641), CANVAS)
    d = ImageDraw.Draw(board)
    bold = lambda size: ImageFont.truetype(str(FONT / "DejaVuSans-Bold.ttf"), size)
    regular = lambda size: ImageFont.truetype(str(FONT / "DejaVuSans.ttf"), size)

    d.text((40, 27), "Иконка «Новости»", fill=TEXT, font=bold(31))
    d.text((41, 75), "Десять вариантов для приложения и виджета", fill=SECONDARY, font=regular(17))

    for index, label in enumerate(VARIANTS):
        number = index + 1
        row, col = divmod(index, 5)
        x = 40 + col * 290
        y = 116 + row * 247
        d.rounded_rectangle((x, y, x + 272, y + 224), radius=17, fill=SURFACE, outline=BORDER)
        d.text((x + 17, y + 16), f"{number:02d}", fill=SECONDARY, font=bold(15))
        d.text((x + 53, y + 16), label, fill=TEXT, font=bold(17))
        mark = icon(number)
        paste_mark(board, mark, x + 136, y + 97, 88, TEXT)

        bar_x, bar_y = x + 21, y + 163
        d.rounded_rectangle((bar_x, bar_y, bar_x + 230, bar_y + 41), radius=9,
                            fill=SURFACE, outline=BORDER)
        d.rounded_rectangle((bar_x + 8, bar_y + 8, bar_x + 62, bar_y + 33),
                            radius=13, fill=TEXT)
        paste_mark(board, existing("refresh"), bar_x + 35, bar_y + 20, 26, SURFACE)
        for cell, symbol in enumerate(("tasks", "finance", "health")):
            paste_mark(board, existing(symbol), bar_x + 84 + cell * 38, bar_y + 20, 28, INACTIVE)
        paste_mark(board, mark, bar_x + 198, bar_y + 20, 28, TEXT)

    board.save(path, optimize=True)


if __name__ == "__main__":
    render(Path(__file__).with_suffix(".png"))
