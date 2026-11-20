"""Макеты #269. Запуск: python3 design/approved/269/screens.py."""
from calendar import Calendar
from html import escape
from itertools import accumulate
from pathlib import Path

HERE = Path(__file__).resolve().parent
source = HERE.parent / '267' / 'screens.py'
finance = {'__file__': str(source)}
exec(compile(source.read_text().split('\ndocument = ', 1)[0], str(source), 'exec'), finance)

dim = finance['dim']
color = finance['color']
vector = finance['vector']
daily = finance['daily']
total = finance['total']
money = finance['money']
months = finance['month_label']
MONTH_LIMIT = finance['monthly_limit']
MODES = [('all', 'Все', 'ic_widget_all'), ('inbox', 'Входящие', 'ic_widget_inbox'), ('week', 'Неделя', 'ic_tab_week')]
ACCOUNTS = [
    ('💳', 'Карта', 42_300), ('👛', 'Наличные', 6_150), ('🏦', 'Накопления', 80_000),
    ('🪙', 'Резерв', 19_500), ('✈️', 'Путешествия', 7_400), ('💳', 'Вторая карта', 12_950),
]
WEEK = [
    ('ПН 16', [('📨', 'Проверить почту', '08:00', 'done')]),
    ('ВТ 17', [('💼', 'Подготовить отчёт', '17:00', 'high'), ('🛍️', 'Забрать заказ', '19:00', '')]),
    ('СР 18', []),
    ('ЧТ 19', [('🏋️', 'Тренировка', '08:00', 'high'), ('🏠', 'Купить продукты', '18:30', '')]),
    ('ПТ 20', [('💼', 'Сверить счета', '14:30', ''), ('📦', 'Забрать посылку', '18:00', '')]),
    ('СБ 21', [('⚽', 'Футбол', '11:00', ''), ('🎾', 'Теннис', '20:30', '')]),
    ('ВС 22', [('🏠', 'Домашние дела', '14:30', ''), ('💼', 'Подготовить план', '16:00', '')]),
]


def week_view():
    days = []
    for index, (label, tasks) in enumerate(WEEK):
        today = index == 3
        heading = label + (' Сегодня' if today else '')
        rows = ''.join(f'<div class="week-task {kind}"><span>{icon}</span>'
                       f'<b>{escape(title)}</b><time>{time}</time></div>'
                       for icon, title, time, kind in tasks)
        days.append(f'<div class="week-cell"><p class="week-day{" today" if today else ""}">{heading}</p>{rows}</div>')
    weekdays = ''.join(f'<span>{day}</span>' for day in ('пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'))
    dates = []
    for week in Calendar().monthdatescalendar(2026, 11):
        week_class = ' selected-week' if week[0].day == 16 and week[0].month == 11 else ''
        days_html = ''.join(
            f'<span class="{"calendar-today" if day.day == 19 and day.month == 11 else "other-month" if day.month != 11 else ""}">{day.day}</span>'
            for day in week
        )
        dates.append(f'<div class="calendar-row{week_class}">{days_html}</div>')
    return f'<div class="week-grid">{"".join(days)}<div class="calendar"><div class="calendar-row weekdays">{weekdays}</div>{"".join(dates)}</div></div>'


def task_list(inbox=False):
    groups = [('Без даты', [('💼', 'Подготовить вопросы', '', ''), ('🏠', 'Записаться на приём', '', '')])] if inbox else [
        ('ЧТ, 19 ноя · Сегодня', WEEK[3][1]),
        ('ПТ, 20 ноя · Завтра', WEEK[4][1]),
        ('СБ, 21 ноя', WEEK[5][1]),
    ]
    parts = []
    for label, tasks in groups:
        parts.append(f'<h3>{label}</h3>')
        parts.extend(
            f'<div class="list-task {kind}"><time>{time}</time>'
            f'<span class="ring" aria-hidden="true"></span><b>{escape(title)}</b><span>{icon}</span></div>'
            for icon, title, time, kind in tasks
        )
    return f'<div class="task-list{" inbox" if inbox else ""}">{"".join(parts)}</div>'


def compact_plot():
    """Те же данные и шкала #267 в графике, видимом при высоте 110 dp."""
    estimate = total * 30 / len(daily)
    ceiling = finance['scale_ceiling'](estimate)
    left, right, top, bottom = 2, 162, 4, 43

    def x(day):
        return left + (day - 1) * (right - left) / 29

    def y(amount):
        return bottom - (bottom - top) * amount / ceiling

    actual = ' '.join(f'{x(day):.2f},{y(amount):.2f}' for day, amount in enumerate(accumulate(daily), 1))
    today = x(len(daily))
    guides = ' '.join(f'M{left} {y(amount):g}H{right}' for amount in (0, ceiling // 2, ceiling))
    return f'''<svg viewBox="0 0 164 57" role="img" aria-label="Накопленные расходы и прогноз на ноябрь">
      <path d="{guides}" class="grid"/>
      <line x1="{today:.2f}" x2="{today:.2f}" y1="{top}" y2="{bottom}" class="cutoff"/>
      <polyline points="{actual}" class="trend"/>
      <line x1="{today:.2f}" x2="{right}" y1="{y(total):.2f}" y2="{y(estimate):.2f}" class="forecast"/>
      <circle cx="{today:.2f}" cy="{y(total):.2f}" r="2.3" class="actual-dot"/>
      <circle cx="{right}" cy="{y(estimate):.2f}" r="2" class="estimate-dot"/>
      <text x="5" y="13">{ceiling // 1000}k</text><text x="5" y="33">{ceiling // 2000}k</text>
      <text x="{left}" y="54">01</text><text x="{today:.2f}" y="54" text-anchor="middle" class="current-date">19</text>
      <text x="{right}" y="54" text-anchor="end">30</text>
    </svg>'''


def finance_view(compact):
    chart = finance['pace'](daily, 2026, 11, MONTH_LIMIT)
    if compact:
        summary = chart.split('<svg viewBox="0 0 356 178"', 1)[0]
        chart = summary + compact_plot() + '</section>'
    account_total = sum(amount for _, _, amount in ACCOUNTS)
    rows = ''.join(f'<li><span aria-hidden="true">{icon}</span><span>{escape(name)}</span><b>{money(amount)}</b></li>'
                   for icon, name, amount in ACCOUNTS)
    return f'''{chart}{finance['rankings']()}
    <section class="widget-accounts" aria-label="Все счета">
      <header><h3>Счета</h3><b>{money(account_total)}</b></header>
      <ul>{rows}</ul>
    </section>'''


def widget(name, *, module='tasks', mode='week', compact=False):
    options = ''.join(
        f'<button type="button" data-mode="{key}" aria-pressed="{str(key == mode).lower()}">'
        f'{vector(icon)}<span>{label}</span><b aria-hidden="true">✓</b></button>'
        for key, label, icon in MODES
    )
    tasks_active = module == 'tasks'
    tasks_content = {'all': task_list(), 'inbox': task_list(inbox=True), 'week': week_view()}
    panes = ''.join(f'<div class="task-view" data-view="{key}"{" hidden" if key != mode else ""}>{body}</div>'
                    for key, body in tasks_content.items())
    return f'''<div id="{name}" class="widget-stage{" compact" if compact else ""}" data-module="{module}" data-mode="{mode}">
      <div class="widget-frame">
        <div class="widget-content">
          <div class="tasks-pane"{" hidden" if not tasks_active else ""}>{panes}</div>
          <div class="finance-pane" tabindex="0" aria-label="Финансы, прокручиваемая область"{" hidden" if tasks_active else ""}>
            {finance_view(compact)}
          </div>
        </div>
        <nav class="widget-bar" aria-label="Вкладки виджета">
          <button type="button" class="widget-add" aria-label="{"Новая задача" if tasks_active else "Новая операция"}" data-action="{"task" if tasks_active else "operation"}">
            {vector('ic_widget_add')}
          </button>
          <div class="widget-tabs" role="tablist">
            <button type="button" role="tab" class="widget-tab" data-tab="tasks" aria-label="Задачи" aria-selected="{str(tasks_active).lower()}">
              {vector('ic_module_tasks')}
            </button>
            <button type="button" role="tab" class="widget-tab" data-tab="finance" aria-label="Финансы" aria-selected="{str(not tasks_active).lower()}">
              {vector('ic_module_finance')}
            </button>
          </div>
          <button type="button" class="widget-date" aria-label="Вернуться к сегодня"{" hidden" if not tasks_active else ""}>
            <b>19</b><span><strong>ЧТ</strong><small>ноя</small></span>
          </button>
          <span class="widget-month"{" hidden" if tasks_active else ""}>{months}</span>
        </nav>
      </div>
      <div class="picker-layer" hidden>
        <button type="button" class="picker-backdrop" aria-label="Закрыть выбор вида"></button>
        <div class="view-picker" role="dialog" aria-label="Вид задач">
          <h3>Вид задач</h3>{options}
        </div>
      </div>
    </div>'''


def panel(label, body):
    return f'<figure><figcaption>{label}</figcaption>{body}</figure>'


CSS = finance['CSS'] + f'''
:root {{
  --bar-height:{dim('WIDGET_BAR')}px;--add-width:{dim('WIDGET_ADD_W')}px;
  --add-height:{dim('WIDGET_ADD_H')}px;--tab-icon:{dim('WIDGET_TAB_ICON')}px;
  --week-priority:#862CE0;
}}
''' + '''
[hidden]{display:none!important}
body{background:#edf0f5}
main{max-width:none;width:max-content;display:flex;flex-direction:column;gap:26px;padding:24px;margin:0 auto}
.gallery{display:flex;gap:20px;align-items:flex-start;padding:0;border:0}
figure{margin:0}
figcaption{font:700 14px/20px Manrope,sans-serif;color:var(--ink);margin-bottom:10px}
.widget-stage{position:relative;width:320px}
.widget-frame{display:grid;grid-template-rows:minmax(0,1fr) var(--bar-height);height:640px;overflow:hidden;
  background:var(--surface);border-radius:18px;box-shadow:0 4px 14px #16171d13}
.widget-content,.tasks-pane,.task-view{height:100%;min-height:0;overflow:hidden}
.widget-bar{display:grid;grid-template-columns:var(--add-width) minmax(0,1fr) var(--add-width);align-items:center;
  background:var(--surface);border-top:1px solid var(--divider);padding:0 4px;color:var(--ink);gap:0}
.widget-stage button{font-family:inherit;border:0;background:transparent;color:inherit;padding:0;cursor:pointer}
.widget-stage button:focus-visible{outline:2px solid var(--ink);outline-offset:-2px}
.widget-bar .widget-add{display:grid;place-items:center;width:100%;height:var(--add-height);border-radius:12px;background:var(--ink);color:var(--surface)}
.widget-add svg{width:var(--tab-icon);height:var(--tab-icon)}
.widget-tabs{display:flex;justify-content:center;min-width:0}
.widget-bar .widget-tab{width:44px;height:calc(var(--bar-height) - 1px);display:grid;place-items:center;color:var(--muted)}
.widget-tab svg{width:var(--tab-icon);height:var(--tab-icon)}
.widget-tab svg path[stroke],.view-picker svg path[stroke]{fill:none;stroke-linejoin:round}
.widget-bar .widget-tab[aria-selected="true"]{color:var(--ink)}
.widget-bar .widget-date,.widget-month{display:flex;align-items:center;justify-content:center;gap:3px;height:100%;white-space:nowrap}
.widget-date b{font-size:18px;line-height:18px}
.widget-date strong,.widget-date small{display:block;font-size:9px;line-height:10px;text-align:left}
.widget-date small,.widget-month{color:var(--muted);font-weight:700}
.widget-month{font-size:11px}
.week-grid{height:100%;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));grid-template-rows:repeat(4,minmax(0,1fr))}
.week-cell,.calendar{border-bottom:1px solid var(--divider);overflow:hidden;padding:4px 4px 0}
.week-cell:nth-child(odd){border-right:1px solid var(--divider)}
.week-cell:nth-child(7),.calendar{border-bottom:0}
.week-day{font-size:10px;line-height:15px;color:var(--muted);font-weight:600}
.week-day.today{color:var(--ink);font-weight:800}
.week-task{display:grid;grid-template-columns:13px minmax(0,1fr) auto;gap:2px;align-items:center;height:23px;font-size:10px}
.week-task b{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-weight:800}
.week-task time,.list-task time{font-variant-numeric:tabular-nums;font-weight:800}
.week-task.done{opacity:.34}
.week-task.high,.list-task.high{color:var(--week-priority)}
.calendar{padding:4px 4px 2px;display:flex;flex-direction:column;justify-content:center}
.calendar-row{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));align-items:center;text-align:center;height:18px;font-size:10px;font-weight:700}
.calendar-row.weekdays{font-size:8px;color:var(--muted);font-weight:500}
.calendar-row.selected-week{border-radius:9px;background:#f1f1f3}
.calendar-today{justify-self:center;width:16px;height:16px;display:grid;place-items:center;border-radius:50%;background:var(--ink);color:white}
.other-month{color:var(--inactive)}
.task-list{height:100%;padding:8px;overflow:auto}
.task-list.inbox{background:#f5f5f6}
.task-list h3{font-size:13px;margin:6px 0 4px}
.list-task{min-height:34px;display:grid;grid-template-columns:36px 14px minmax(0,1fr) 20px;align-items:center;gap:4px;font-size:12px}
.list-task b{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.list-task .ring{width:13px;height:13px;border:1.5px solid currentColor;border-radius:50%;opacity:.65}
.finance-pane{height:100%;overflow:auto;overscroll-behavior:contain;scrollbar-width:thin;padding:8px 10px 12px}
.finance-pane .pace{padding:0 0 4px}
.finance-pane .pace-summary{margin:0 0 6px;gap:3px}
.finance-pane .pace-amount{font-size:18px;gap:3px}
.finance-pane .pace-icon svg{width:16px;height:16px}
.finance-pane .pace > svg text{font-size:11px}
.finance-pane .pace > svg .trend,.finance-pane .pace > svg .forecast{stroke-width:2.3}
.finance-pane .rankings{padding:9px 0 0}
.finance-pane .tables{gap:10px}
.finance-pane .tables thead th{font-size:13px;padding-bottom:3px}
.finance-pane .tables tbody th,.finance-pane .tables tbody td{height:25px;font-size:11px}
.finance-pane .row-label{grid-template-columns:10px minmax(0,1fr);gap:3px}
.finance-pane .index{font-size:10px}
.finance-pane .widget-accounts{padding:13px 0 0}
.widget-accounts header{display:flex;align-items:center;justify-content:space-between;font-size:12px}
.widget-accounts h3{margin:0;font-size:13px}
.widget-accounts ul{margin:5px 0 0;padding:0;list-style:none}
.widget-accounts li{display:grid;grid-template-columns:18px minmax(0,1fr) auto;align-items:center;gap:4px;min-height:26px;font-size:11px}
.widget-accounts li b{white-space:nowrap;font-variant-numeric:tabular-nums}
.picker-layer{position:absolute;inset:0;z-index:2}
.picker-layer .picker-backdrop{position:absolute;inset:0;width:100%;height:100%;border-radius:18px;background:#16171d40}
.view-picker{position:absolute;bottom:calc(var(--bar-height) + 6px);left:50%;transform:translateX(-50%);width:204px;
  padding:8px;border-radius:15px;background:var(--surface);box-shadow:0 7px 24px #16171d24}
.view-picker h3{font-size:13px;margin:2px 8px 5px}
.view-picker button{width:100%;height:39px;display:grid;grid-template-columns:19px 1fr auto;gap:9px;align-items:center;text-align:left;
  padding:0 8px;border-radius:9px;font-size:13px}
.view-picker button[aria-pressed="true"]{background:#f1f1f3;font-weight:800}
.view-picker svg{width:18px;height:18px}
.view-picker button b{display:none}
.view-picker button[aria-pressed="true"] b{display:block}
.widget-stage.compact{width:180px}
.compact .widget-frame{height:110px;border-radius:14px}
.compact .widget-bar{grid-template-columns:44px minmax(0,1fr) 40px;padding:0 3px}
.compact .widget-month{font-size:10px}
.compact .widget-add svg{width:19px;height:19px}
.compact .widget-date b{font-size:15px}
.compact .widget-date strong,.compact .widget-date small{font-size:8px}
.compact .view-picker{width:176px}
.compact .finance-pane{padding:5px 8px 8px}
.compact .finance-pane .pace{display:flex;flex-direction:column}
.compact .finance-pane .pace-summary{order:1;margin:6px 0 0}
.compact .pace > svg text{font-size:8px}
.compact .pace > svg .trend,.compact .pace > svg .forecast{stroke-width:1.45px}
.compact .pace > svg .forecast{stroke-dasharray:3 2}
.compact .finance-pane .pace-amount{font-size:11px;gap:2px}
.compact .finance-pane .pace-icon svg{width:11px;height:11px}
.compact .tables{grid-template-columns:minmax(0,1fr);gap:10px}
'''

SCRIPT = '''
for (const widget of document.querySelectorAll('.widget-stage')) {
  const picker = widget.querySelector('.picker-layer');
  const taskTab = widget.querySelector('[data-tab="tasks"]');
  const add = widget.querySelector('.widget-add');
  function show(module) {
    const tasks = module === 'tasks';
    widget.dataset.module = module;
    for (const tab of widget.querySelectorAll('[data-tab]')) {
      tab.setAttribute('aria-selected', String(tab.dataset.tab === module));
    }
    widget.querySelector('.tasks-pane').hidden = !tasks;
    widget.querySelector('.finance-pane').hidden = tasks;
    widget.querySelector('.widget-date').hidden = !tasks;
    widget.querySelector('.widget-month').hidden = tasks;
    add.dataset.action = tasks ? 'task' : 'operation';
    add.setAttribute('aria-label', tasks ? 'Новая задача' : 'Новая операция');
    picker.hidden = true;
  }
  function choose(mode) {
    widget.dataset.mode = mode;
    for (const view of widget.querySelectorAll('[data-view]')) view.hidden = view.dataset.view !== mode;
    for (const button of picker.querySelectorAll('[data-mode]')) {
      button.setAttribute('aria-pressed', String(button.dataset.mode === mode));
    }
    picker.hidden = true;
    taskTab.focus({ preventScroll: true });
  }
  taskTab.addEventListener('click', () => {
    if (widget.dataset.module === 'tasks') {
      picker.hidden = !picker.hidden;
      if (!picker.hidden) picker.querySelector('[aria-pressed="true"]').focus({ preventScroll: true });
    } else show('tasks');
  });
  widget.querySelector('[data-tab="finance"]').addEventListener('click', () => show('finance'));
  picker.querySelector('.picker-backdrop').addEventListener('click', () => {
    picker.hidden = true;
    taskTab.focus({ preventScroll: true });
  });
  for (const button of picker.querySelectorAll('[data-mode]')) button.addEventListener('click', () => choose(button.dataset.mode));
  widget.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !picker.hidden) {
      picker.hidden = true;
      taskTab.focus({ preventScroll: true });
    }
  });
  add.addEventListener('click', () => { widget.dataset.lastAction = add.dataset.action; });
}
'''

standard = (
    panel('Задачи · Неделя', widget('tasks'))
    + panel('Повторное нажатие на «Задачи»', widget('chooser'))
    + panel('Финансы', widget('finance', module='finance'))
)
compact = (
    panel('График', widget('compact-chart', module='finance', compact=True))
    + panel('Категории', widget('compact-ranking', module='finance', compact=True))
    + panel('Места', widget('compact-places', module='finance', compact=True))
    + panel('Счета · конец списка', widget('compact-accounts', module='finance', compact=True))
)
document = f'''<!doctype html><html lang="ru"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1"><title>Виджет · #269</title>
<style>{CSS}</style></head><body><main>
<div class="gallery" id="standard">{standard}</div><div class="gallery" id="compact">{compact}</div>
</main><script>{SCRIPT}</script></body></html>'''
(HERE / 'screens.html').write_text(document, encoding='utf-8')
