from datetime import date, timedelta
from html import escape
from pathlib import Path

folder = Path(__file__).resolve().parent
today = date(2026, 11, 21)
first = date(2026, 10, 26)
selected = today - timedelta(days=today.weekday())
options = (
    ("band", "1. Светлая полоса", "Мягкая заливка всей выбранной недели."),
    ("outline", "2. Тонкая рамка", "Выбранная неделя обведена тонкой линией."),
    ("marker", "3. Метка слева", "Короткая вертикальная метка у выбранной недели."),
)
sample_tasks = (
    ("🏠", "Стирка", "14:30"),
    ("🔁", "Бритьё", "15:30"),
    ("🏠", "Уборка", "16:00"),
    ("🗂️", "Икра", "16:00"),
)


def week_row(start):
    end = start + timedelta(days=6)
    active = start == selected
    days = []
    for offset in range(7):
        day = start + timedelta(days=offset)
        classes = "date" if day.month == today.month else "date outside"
        current = ' aria-current="date"' if day == today else ""
        days.append(f'<span class="{classes}"{current}>{day.day}</span>')
    return (
        f'<button class="week" type="button" data-sunday="{end.day}"'
        f' aria-label="{start:%d.%m} – {end:%d.%m}" aria-pressed="{str(active).lower()}">'
        f'{"".join(days)}</button>'
    )


def sample(option, title, details):
    rows = "".join(week_row(first + timedelta(weeks=index)) for index in range(6))
    tasks = "".join(
        f'<div class="task"><span class="emoji">{mark}</span><b>{escape(name)}</b><time>{time}</time></div>'
        for mark, name, time in sample_tasks
    )
    return f"""
    <section class="variant {option}">
      <h2>{title}</h2>
      <p>{details}</p>
      <div class="widget">
        <div class="sunday">
          <div class="sunday-title">ВС <span>22</span></div>
          <div class="sunday-tasks">{tasks}</div>
        </div>
        <div class="calendar" role="group" aria-label="Выбор недели">{rows}</div>
      </div>
    </section>
    """


sections = "\n".join(sample(*option).strip() for option in options)
(folder / "screens.html").write_text(f"""<!doctype html>
<html lang="ru">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Выбор недели в виджете</title>
<link rel="stylesheet" href="screen.css">
<main class="board">
  <header><h1>Выбор недели</h1><p>Виджет «Неделя» · ноябрь 2026</p></header>
  {sections}
</main>
<script src="preview.js"></script>
</html>
""", encoding="utf-8")
