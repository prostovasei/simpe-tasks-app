# #329 — хаб «Главная»

24.11.2026. Claude Code. **На согласовании у PM:** ТЗ v1, варианты плотности (A/B/C), навигации (N1/N2/N3) и новостей в виджете.
После решения здесь будет записан выбранный вариант. [ТЗ v1 и оценка сложности](requirements.md) · [Issue #329](https://github.com/prostovasei/simpe-tasks-app/issues/329).

- [Интерактивный прототип](prototype.html) ([артефакт](https://claude.ai/artifact/LXU2gfSUSFpRacBDzrgjRf)): режим «На весь экран» в масштабе 1:1 (ссылка с `#full` открывает его сразу), переключатели плотности, навигации и времени суток; нажимаются кружки задач, «+», заголовки блоков, вкладки, полоса лимита, новости.
- [Живой прототип — лист](sheet-live.png) · [Плотность A/B/C](sheet-density.png) · [Навигация N1/N2/N3](sheet-nav.png) · [«+» и раскрытие](sheet-extras.png) · [Виджет](sheet-widget.png) · [Оценка](sheet-estimate.png).
- [Исходник](prototype.template.html) · [сборка](build.py) · [рендер](render.js).

Значки — векторные ресурсы приложения (`res/drawable`), домик — новый в том же стиле. Цвета — токены приложения, шрифт Manrope.
Данные, суммы и заголовки новостей вымышленные. Воспроизведение из корня: `python3 design/approved/329/build.py && node design/approved/329/render.js`.
