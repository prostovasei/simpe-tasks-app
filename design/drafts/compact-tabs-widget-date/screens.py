from html import escape
from pathlib import Path
from xml.etree import ElementTree

draft = Path(__file__).resolve().parent
repo = draft.parents[2]
drawables = repo / "app/src/main/res/drawable"
android = "{http://schemas.android.com/apk/res/android}"
icons = {
    "tasks": "tab_week",
    "finance": "module_finance",
    "health": "module_health",
    "settings": "nav_settings",
    "add": "widget_add",
    "update": "widget_update",
    "info": "settings_app",
    "settings_tasks": "module_tasks",
    "spent": "finance_receipt",
    "remaining": "finance_limit",
    "forecast": "finance_forecast",
}
labels = {
    "tasks": "Задачи", "finance": "Финансы", "health": "Здоровье",
    "settings": "Настройки", "add": "Добавить", "update": "Обновление",
}


def icon(key):
    vector = ElementTree.parse(drawables / f"ic_{icons[key]}.xml").getroot()
    paths = []
    for path in vector.iter("path"):
        fill = path.get(android + "fillColor")
        attrs = {
            "d": path.attrib[android + "pathData"],
            "fill": "currentColor" if fill and fill != "@android:color/transparent" else "none",
        }
        if path.get(android + "strokeColor"):
            attrs.update({
                "stroke": "currentColor",
                "stroke-width": path.get(android + "strokeWidth", "2"),
                "stroke-linecap": path.get(android + "strokeLineCap", "round"),
                "stroke-linejoin": path.get(android + "strokeLineJoin", "round"),
            })
        paths.append("<path " + " ".join(f'{key}="{escape(value, quote=True)}"' for key, value in attrs.items()) + "/>")
    width = vector.attrib[android + "viewportWidth"]
    height = vector.attrib[android + "viewportHeight"]
    return f'<svg viewBox="0 0 {width} {height}" aria-hidden="true">{"".join(paths)}</svg>'


def source(name, description):
    return f'<img class="source" src="../../../app/screenshots/{name}.png" alt="{description}">'


def button(key, active=False, tab=False):
    state = f' role="tab" aria-selected="{str(active).lower()}"' if tab else ""
    return f'<button class="{key}" aria-label="{labels[key]}"{state}>{icon(key)}</button>'


def date(active):
    date_content = '<b class="day">21</b><span class="date-label"><b>СБ</b><span>ноя</span></span>'
    if active == "tasks":
        return f'<button class="date" aria-label="К сегодняшнему дню, 21 ноября">{date_content}</button>'
    return f'<span class="date" aria-label="21 ноября, суббота">{date_content}</span>'


def bar(active, *, app=False, compact=False):
    keys = ("tasks", "finance", "health")
    tabs = "".join(button(key, key == active, tab=True) for key in keys)
    if app:
        tools = button("settings")
    else:
        tools = "" if compact else f'<div class="end">{date(active)}{button("update")}</div>'
    return f'<nav class="bar" aria-label="Нижняя панель">{button("add")}<div class="tabs" role="tablist">{tabs}</div>{tools}</nav>'


def card(title, content, css_class):
    return f'<figure><figcaption>{title}</figcaption><div class="{css_class}">{content}</div></figure>'


def weight_chart():
    return '''
      <div class="health-content">
        <div class="hero">82,0 <small>кг</small></div>
        <div class="goal">Цель 80,0 кг · осталось 2,0 кг</div>
        <div class="details">Прогноз — после 5 записей за 2 недели · ИМТ 25,3 — выше нормы</div>
        <div class="weight-chart" aria-label="Вес 82 кг, цель 80 кг">
          <div class="level top"><span>82</span><i class="weight-point"></i></div>
          <div class="level middle"><span>81</span></div>
          <div class="level bottom"><span>80,0</span></div>
          <div class="chart-dates"><span>22 окт</span><span>21 ноя</span></div>
        </div>
      </div>'''


def finance_sample():
    summary = "".join(
        f'<b>{icon(key)}{value} ₽</b>'
        for key, value in (("spent", "43k"), ("remaining", "57k"), ("forecast", "128k"))
    )
    return f'''
      <div class="finance-sample">
        <div class="finance-summary">{summary}</div>
        <div class="finance-chart">
          <span>150k</span><span>75k</span>
          <svg viewBox="0 0 280 100" preserveAspectRatio="none" aria-label="График расходов">
            <path d="M0 4H280 M0 48H280 M0 93H280" stroke="#ededee" fill="none"/>
            <path d="M0 93L13 87L29 87L42 77L67 77L79 70L91 70" stroke="#16171d" stroke-width="1.5" fill="none"/>
            <path d="M91 70L280 17" stroke="#787b84" stroke-width="1.5" stroke-dasharray="3 3" fill="none"/>
          </svg>
        </div>
        <div class="finance-days"><span>01</span><span>10</span><span>30</span></div>
        <div class="spending-bar"><i></i><i></i><i></i><i></i><i></i></div>
        <div class="spending-chips"><span>Продукты</span><span>Транспорт</span><span>Кафе</span><span>Дом</span><span>Остальное</span></div>
        <div class="balance">128k ₽</div>
        <div class="accounts"><span>💳 42k</span><span>👛 6k</span><span>🏦 80k</span></div>
      </div>'''


def week_grid():
    days = (
        ("ПН 16", ("Вес", "10:00"), ("Тренировка", "19:00")),
        ("ВТ 17", ("Икра", "19:00"), ("Родители", "20:00")),
        ("СР 18", ("Стирка", "23:00")),
        ("ЧТ 19", ("Встреча", "08:00"), ("Работа", "08:30")),
        ("ПТ 20", ("Финансы", "14:30"), ("Цветы", "15:00")),
        ("СБ 21 Сегодня", ("Футбол", "02:30"), ("Теннис", "20:30")),
        ("ВС 22", ("Стирка", "14:30"), ("Уборка", "16:00")),
    )
    cells = []
    for index, (day, *tasks) in enumerate(days):
        rows = "".join(f'<div class="task"><b>{escape(name)}</b><time>{time}</time></div>' for name, time in tasks)
        state = " today" if index == 5 else ""
        cells.append(f'<div class="day-cell{state}"><div class="day-title">{day}</div>{rows}</div>')
    numbers = [26, 27, 28, 29, 30, 31, *range(1, 31), *range(1, 7)]
    calendar = "".join(f"<em>{day}</em>" for day in ("пн", "вт", "ср", "чт", "пт", "сб", "вс"))
    for index, day in enumerate(numbers):
        style = ' class="in-week"' if 16 <= day <= 22 and index < 36 else ""
        calendar += f"<span{style}>{'<b>21</b>' if index == 26 else day}</span>"
    cells.append(f'<div class="day-cell calendar">{calendar}</div>')
    return f'<div class="week-grid">{"".join(cells)}</div>'


def settings_screen():
    rows = (
        ("settings_tasks", "Задачи", "Ввод, напоминания, импорт и резервная копия"),
        ("finance", "Финансы", "Категории, места, корзина, копия, банк"),
        ("health", "Здоровье", "Напоминание, резервная копия"),
        ("info", "Приложение", "Версия 0.79.3 · обновление"),
    )
    items = "".join(
        f'<div class="settings-row">{icon(key)}<div><b>{title}</b><small>{description}</small></div><span aria-hidden="true">›</span></div>'
        for key, title, description in rows
    )
    return f'<header class="settings-title"><span aria-hidden="true">←</span> Настройки</header><div class="settings-list">{items}</div>'


app = card(
    "Вкладки в приложении",
    week_grid() + bar("tasks", app=True),
    "phone app-tasks",
) + card(
    "Открытые «Настройки»",
    settings_screen(),
    "phone settings-screen",
)

widgets = "".join((
    card("Задачи · 320 dp", week_grid() + bar("tasks"), "widget wide"),
    card("Финансы · 320 dp", finance_sample() + bar("finance"), "widget wide"),
    card("Здоровье · 320 dp", weight_chart() + bar("health"), "widget wide"),
    card("Компактный · 180 × 110 dp", '<div class="compact-content">' + source("widget_health_compact_light", "Вес и цель") + '</div>' + bar("health", compact=True), "widget compact"),
))

(draft / "screens.html").write_text(f"""<!doctype html>
<html lang="ru">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Панель приложения и виджета</title>
<link rel="stylesheet" href="screen.css">
<section class="board" id="app">
  <h1>Приложение</h1>
  <div class="grid">{app}</div>
</section>
<section class="board" id="widget">
  <h1>Виджет</h1>
  <div class="grid">{widgets}</div>
</section>
</html>
""", encoding="utf-8")
