# Финансы #239 — макеты и превью анимаций (на согласование)

Статус: **ждут решения PM** (13.11.2026). Список и ответы PM — [Issue #239](https://github.com/prostovasei/simpe-tasks-app/issues/239).
После согласования переносятся в `design/approved/239/`.

- [screens.html](screens.html): [1. Настройки и справочники](screens-1.png) · [2. Архив и удаление](screens-2.png) · [3. Место в окне операции](screens-3.png)
- Превью анимаций (пункт 7): [A — сдвиг целиком](motion-A.gif) · [B — наезд с затемнением](motion-B.gif) · [C — сдвиг с растворением](motion-C.gif) · [раскадровка](motion-storyboard.png)
- Сборка: `python3 design/drafts/finance-239/screens.py` → `node design/drafts/finance-239/render.js`; превью — `python3 design/drafts/finance-239/motion.py` (кадры из `app/screenshots`).
- В превью панели модулей едут вместе с экраном; на телефоне верхняя и нижняя панели стоят на месте.
