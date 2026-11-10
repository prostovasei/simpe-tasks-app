# Черновики финансов #217

## Действующий черновик — ждёт согласования PM

- [service-screens.html](service-screens.html) — 25 экранов на основе утверждённого [compact-module-bar.html](../../approved/217/compact-module-bar.html): листы
  [1. Операции и формы](service-1.png), [2. Первый запуск и счета](service-2.png), [3. Категории, корзина, копия](service-3.png), [4. Аналитика и задачи](service-4.png).
- Сборка: `python3 service-screens.py`, затем `node service-render.js` (Playwright). Стили и шрифты берутся из утверждённого макета.
- После согласования перенести в `design/approved/217/`. Требования — [requirements.md](../../approved/217/requirements.md).

## История

`finance-screens.html`, `main-screens.png`, `operation-forms.png`, `screens.py`, `render-screens.py`, `requirements.md` — ранние варианты. Их навигация устарела:
общая нижняя панель «Задачи · + · Финансы» и отдельная вкладка счетов отменены. Не реализовывать по ним.
