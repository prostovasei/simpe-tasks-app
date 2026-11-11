# Финансы #217 — актуальный согласованный макет

PM 10.11.2026: «В целом ок». Согласованы компактный верхний бар и компоновка основных экранов; служебные экраны и отдельные сценарии ещё требуют обсуждения.

- [Общее превью](compact-module-bar-preview.png)
- [Задачи](compact-module-bar-tasks.png) · [Финансы](compact-module-bar-finance.png)
- [Самодостаточный кликабельный HTML / исходник](compact-module-bar.html)
- [Полные актуальные требования](requirements.md)
- [Точка продолжения для Claude](HANDOFF.md)

HTML скачать и открыть в браузере; сетевых зависимостей нет. Изображения — макеты, не скриншоты работающего Android-раздела. Старые материалы в ../../drafts/finance-217 исторические.

Воспроизведение PNG: `python3 design/approved/217/render.py` (нужны Playwright и Chromium /usr/bin/chromium). Сам HTML является редактируемым исходником последнего состояния; прежняя цепочка генераторов для редактирования не нужна.

## Комплект экранов — принят PM 10.11.2026

[service-screens.html](service-screens.html): [1. Операции и формы](service-1.png), [2. Первый запуск и счета](service-2.png),
[3. Категории, корзина, копия](service-3.png), [4. Аналитика и задачи](service-4.png).
Сборка: `python3 service-screens.py`, затем `node service-render.js` (Playwright). Итоги решений — [requirements.md](requirements.md) §10.
