# #329 — хаб «Главная»

24.11.2026. Claude Code. **Финальный вариант, решение PM 24.11.2026** («A, N1 и добавь горизонтальное листание новостей»):
плотность **A · Один экран**, навигация **N1 · Пятая вкладка**, новости — страницы 2 × 2 со свайпом (в виджете — стрелки ‹ ›).
Код — после команды «Код». [Финальное ТЗ и оценка сложности](requirements.md) · [Issue #329](https://github.com/prostovasei/simpe-tasks-app/issues/329).

- [Интерактивный прототип](prototype.html) ([артефакт](https://claude.ai/artifact/LXU2gfSUSFpRacBDzrgjRf), сразу во весь экран — [`#full`](https://claude.ai/artifact/LXU2gfSUSFpRacBDzrgjRf#full)):
  масштаб 1:1, нажимаются кружки задач, «+», вкладки, полоса лимита, новости листаются вбок.
- Масштаб 1:1, экран 412 × 860: [хаб](final-1x1.png) · [вторая страница новостей](final-1x1-news2.png).
- Листы: [живой прототип](sheet-live.png) · [экраны: хаб, листание, «+», пусто, раздел](sheet-screens.png) · [виджет](sheet-widget.png) · [оценка](sheet-estimate.png).
- [Исходник](prototype.template.html) · [сборка](build.py) · [рендер](render.js).

**Отвергнутые варианты — справка, не использовать:** B · Подробная лента и C · Компактно + раскрытие ([лист](rejected-density.png),
1:1 — [B](rejected-real-scale-B.png), [C](rejected-real-scale-C.png)); N2 · Домик вместо ⚙ и N3 · «Назад» в хаб ([лист](rejected-nav.png));
сочетания итерации 4 — [лист](rejected-extras.png).

Значки — векторные ресурсы приложения (`res/drawable`), домик — новый в том же стиле. Цвета — токены приложения, шрифт Manrope.
Данные, суммы и заголовки новостей вымышленные. Воспроизведение из корня: `python3 design/approved/329/build.py && node design/approved/329/render.js`.
