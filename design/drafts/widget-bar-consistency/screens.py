from html import escape
from pathlib import Path
from xml.etree import ElementTree

folder = Path(__file__).resolve().parent
assets = folder.parents[2] / "app/src/main/res/drawable"
ns = "{http://schemas.android.com/apk/res/android}"


def icon(name):
    source = {
        "tasks": "ic_tab_week", "finance": "ic_module_finance",
        "health": "ic_module_health", "refresh": "ic_widget_update",
    }
    if name == "add":
        paths = "M12 5v14 M5 12h14"
    else:
        vector = ElementTree.parse(assets / f"{source[name]}.xml")
        paths = " ".join(p.attrib[ns + "pathData"] for p in vector.iter("path"))
    return (
        '<svg viewBox="0 0 24 24" aria-hidden="true">'
        f'<path d="{escape(paths, quote=True)}" vector-effect="non-scaling-stroke"/>'
        '</svg>'
    )


choices = (
    ("a", "1. Сплошная панель", "Три вкладки на белом фоне, интервал 36 dp"),
    ("b", "2. Тонкие разделители", "Две тонкие линии ограничивают блок вкладок, интервал 34 dp"),
    ("c", "3. Общая подложка", "Одна светлая подложка под тремя вкладками, интервал 32 dp"),
)


def bar(key):
    tabs = "".join(
        f'<button class="tab" role="tab" aria-selected="{str(i == 0).lower()}"'
        f' data-module="{module}" aria-label="{title}">{icon(module)}</button>'
        for i, (module, title) in enumerate(
            (("tasks", "Задачи"), ("finance", "Финансы"), ("health", "Здоровье"))
        )
    )
    return (
        f'<div class="widget variant-{key}"><div class="bar" aria-label="Панель виджета">'
        f'<button class="add" aria-label="Добавить"><span>{icon("add")}</span></button>'
        '<button class="gap left" aria-label="Открыть приложение, левый промежуток"></button>'
        f'<div class="tabs" role="tablist">{tabs}</div>'
        '<button class="gap right" aria-label="Открыть приложение, правый промежуток"></button>'
        '<button class="date" aria-label="Вернуться к сегодня, 21 ноября">'
        '<b class="date-number">21</b><span class="date-caption"><span>СБ</span><span>ноя</span></span>'
        f'</button><button class="refresh" aria-label="Обновление">{icon("refresh")}</button>'
        '</div></div>'
    )


parts = [
    f'<section class="choice"><h2>{name}</h2><p>{desc}</p>{bar(key)}<output aria-live="polite"></output></section>'
    for key, name, desc in choices
]
(folder / "screens.html").write_text(f"""<!doctype html>
<html lang="ru">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Нижняя панель виджета</title>
<link rel="stylesheet" href="screen.css">
<div class="demo-controls">
  <label>Ширина <select id="width"><option value="392">392 dp</option><option value="320">320 dp</option><option value="240">240 dp</option><option value="180">180 dp</option></select></label>
  <label><input type="checkbox" id="zones"> Показать зоны открытия</label>
</div>
<main class="board">
  <header><h1>Нижняя панель виджета</h1>
    <p class="normal-legend">Три варианта на ширине <span class="width-label">392</span> dp</p>
    <p class="zone-legend"><i></i> Открыть приложение</p>
  </header>
  {"".join(parts)}
</main>
<script src="preview.js"></script>
</html>
""", encoding="utf-8")
