# Переход на общий бэклог

Дата: 10.11.2026. [Задача переноса #92](https://github.com/prostovasei/simpe-tasks-app/issues/92). Исполнитель: Claude Code.
**Перенос выполнен 10.11.2026.** Источник задач — только GitHub Issues; прежние артефакты Claude — архив, не редактировать.

## Что перенесено

| Данные | Было (архив) | Теперь |
|---|---|---|
| Карточки T-N | [Claude backlog](https://claude.ai/artifact/EoNYmmboC6gG13ju3MnEid) | GitHub Issues `[T-N] …` (#94–#215), экспорт [imports/claude-backlog.json](imports/claude-backlog.json) |
| Полный чек-лист | [Claude checklist](https://claude.ai/artifact/LfCQHWnhLob8u75CP2vaMS) | [TESTING-CHECKLIST.md](TESTING-CHECKLIST.md), экспорт [imports/claude-checklist.json](imports/claude-checklist.json) |
| Прогоны (`runs`) | тот же артефакт | [testing-runs/README.md](testing-runs/README.md), экспорт [imports/claude-checklist-runs.json](imports/claude-checklist-runs.json) |
| Макеты и дизайн-система | приватные холсты Claude | `design/mockups`, `design/system` — см. [design/README.md](../design/README.md) |

## Сверка

- Карточек в артефакте: **122**, номера T-1…T-128. Номеров T-25, T-36, T-74, T-76, T-77, T-78 в артефакте нет
  (карточки с такими номерами не существуют; не восстанавливать по догадкам).
- Статусы в источнике: «Готово» — 97, «Бэклог» — 19, «Приостановлен» — 6, «В работе» — 0.
  В GitHub: закрыто 97, открыто 25 (сверено списком открытых Issues 10.11.2026). Номера T-N уникальны.
- Все поля карточек (`c, e, i, r, num, iteration, section, source, status, title, why, createdAt, updatedAt` и ключ документа)
  сохранены в JSON; в теле Issue — описание, статус, версия, раздел, источник, оценки и исходные даты.
  Дата создания Issue в GitHub — дата переноса; исторические даты — в теле и JSON.
- Статус в теле Issue: `Бэклог` / `Приостановлен` / `Готово`. Приостановленные карточки открыты — искать по тексту «Статус: Приостановлен».
- Чек-лист: **51 раздел, 364 пункта**, ID уникальны и не менялись. Прогонов: **52** (отметок: прошло 201, ошибка 1, пропущено 19; 28 прогонов без отметок).
  В старых прогонах есть 11 ID пунктов, которых уже нет в чек-листе (перечислены в файле прогонов) — сохранены как есть.
- Личных задач PM, токенов и секретов в экспортированных данных нет: в карточках и пунктах — только описание приложения и примеры.
- Тексты Issues набирались по экспорту вручную через API; эталон формулировок — JSON в `docs/imports`.

## Правила после переноса

- Новые задачи — GitHub #N по шаблону Issue. Старую карточку вести в её Issue `[T-N]`; T-N и #N — разные номера.
- Артефакты бэклога и чек-листа не обновлять и не заводить новые параллельные списки в Claude.
- Чек-лист дополнять в [TESTING-CHECKLIST.md](TESTING-CHECKLIST.md) (новый пункт — новый уникальный ID), результаты — отдельным файлом в `docs/testing-runs/`.

## Реестр переноса

| Старый ID | GitHub Issue | Исходный статус | Проверено | Название |
|---|---|---|---|---|
| T-1 | [#94](https://github.com/prostovasei/simpe-tasks-app/issues/94) | Приостановлен (открыта) | да | Теги |
| T-2 | [#95](https://github.com/prostovasei/simpe-tasks-app/issues/95) | Приостановлен (открыта) | да | Голосовой ввод |
| T-3 | [#96](https://github.com/prostovasei/simpe-tasks-app/issues/96) | Приостановлен (открыта) | да | Облако и веб-версия |
| T-4 | [#97](https://github.com/prostovasei/simpe-tasks-app/issues/97) | Готово (закрыта) | да | Неделя в виджете и приложении |
| T-5 | [#98](https://github.com/prostovasei/simpe-tasks-app/issues/98) | Готово (закрыта) | да | Основа приложения: проект, база данных, тема |
| T-6 | [#99](https://github.com/prostovasei/simpe-tasks-app/issues/99) | Готово (закрыта) | да | Импорт из TickTick |
| T-7 | [#100](https://github.com/prostovasei/simpe-tasks-app/issues/100) | Готово (закрыта) | да | Резервная копия в формате TickTick |
| T-8 | [#101](https://github.com/prostovasei/simpe-tasks-app/issues/101) | Готово (закрыта) | да | Навигация и экраны «Все», «Входящие», «Списки» |
| T-9 | [#102](https://github.com/prostovasei/simpe-tasks-app/issues/102) | Готово (закрыта) | да | Создание задачи и экран задачи |
| T-10 | [#103](https://github.com/prostovasei/simpe-tasks-app/issues/103) | Готово (закрыта) | да | Повторы и напоминания |
| T-11 | [#104](https://github.com/prostovasei/simpe-tasks-app/issues/104) | Готово (закрыта) | да | Будущие повторы и вибрация |
| T-12 | [#105](https://github.com/prostovasei/simpe-tasks-app/issues/105) | Готово (закрыта) | да | Виджет на рабочем столе |
| T-13 | [#106](https://github.com/prostovasei/simpe-tasks-app/issues/106) | Готово (закрыта) | да | Задача поверх рабочего стола |
| T-14 | [#107](https://github.com/prostovasei/simpe-tasks-app/issues/107) | Готово (закрыта) | да | Редизайн виджета |
| T-15 | [#108](https://github.com/prostovasei/simpe-tasks-app/issues/108) | Готово (закрыта) | да | Нижняя строка виджета |
| T-16 | [#109](https://github.com/prostovasei/simpe-tasks-app/issues/109) | Готово (закрыта) | да | Стабильный виджет и журнал |
| T-17 | [#110](https://github.com/prostovasei/simpe-tasks-app/issues/110) | Готово (закрыта) | да | Обновление по кнопке |
| T-18 | [#111](https://github.com/prostovasei/simpe-tasks-app/issues/111) | Готово (закрыта) | да | Экран «Выполненные» |
| T-19 | [#112](https://github.com/prostovasei/simpe-tasks-app/issues/112) | Готово (закрыта) | да | Плашки на голубом фоне |
| T-20 | [#113](https://github.com/prostovasei/simpe-tasks-app/issues/113) | Готово (закрыта) | да | Новый дизайн приложения |
| T-21 | [#114](https://github.com/prostovasei/simpe-tasks-app/issues/114) | Готово (закрыта) | да | Значки списков |
| T-22 | [#115](https://github.com/prostovasei/simpe-tasks-app/issues/115) | Готово (закрыта) | да | Виджет в новом дизайне |
| T-23 | [#116](https://github.com/prostovasei/simpe-tasks-app/issues/116) | Готово (закрыта) | да | Анимации: пыль, карточка из «+», сдвиг экранов, пустые экраны |
| T-24 | [#117](https://github.com/prostovasei/simpe-tasks-app/issues/117) | Готово (закрыта) | да | Поиск |
| T-26 | [#118](https://github.com/prostovasei/simpe-tasks-app/issues/118) | Готово (закрыта) | да | «Перенести на сегодня» для просроченных |
| T-27 | [#119](https://github.com/prostovasei/simpe-tasks-app/issues/119) | Готово (закрыта) | да | Меню по долгому нажатию на задачу |
| T-28 | [#120](https://github.com/prostovasei/simpe-tasks-app/issues/120) | Готово (закрыта) | да | Черновик новой задачи |
| T-29 | [#121](https://github.com/prostovasei/simpe-tasks-app/issues/121) | Готово (закрыта) | да | Дата и время прямо в названии |
| T-30 | [#122](https://github.com/prostovasei/simpe-tasks-app/issues/122) | Готово (закрыта) | да | Свернуть «Просрочено» |
| T-31 | [#123](https://github.com/prostovasei/simpe-tasks-app/issues/123) | Готово (закрыта) | да | Недавние запросы в поиске |
| T-32 | [#124](https://github.com/prostovasei/simpe-tasks-app/issues/124) | Приостановлен (открыта) | да | Выбор темы в «Настройках» |
| T-33 | [#125](https://github.com/prostovasei/simpe-tasks-app/issues/125) | Приостановлен (открыта) | да | Ярлыки на иконке приложения |
| T-34 | [#126](https://github.com/prostovasei/simpe-tasks-app/issues/126) | Готово (закрыта) | да | Удаление с «Отменой» |
| T-35 | [#127](https://github.com/prostovasei/simpe-tasks-app/issues/127) | Готово (закрыта) | да | Корзина |
| T-37 | [#128](https://github.com/prostovasei/simpe-tasks-app/issues/128) | Готово (закрыта) | да | Массовое редактирование |
| T-38 | [#129](https://github.com/prostovasei/simpe-tasks-app/issues/129) | Готово (закрыта) | да | Новое окно создания задачи с сеткой недели |
| T-39 | [#130](https://github.com/prostovasei/simpe-tasks-app/issues/130) | Готово (закрыта) | да | То же окно для уже созданных задач |
| T-40 | [#131](https://github.com/prostovasei/simpe-tasks-app/issues/131) | Готово (закрыта) | да | Шкала дня в виджете |
| T-41 | [#132](https://github.com/prostovasei/simpe-tasks-app/issues/132) | Готово (закрыта) | да | Неделя в шторке и таймлайн на 3 дня |
| T-42 | [#133](https://github.com/prostovasei/simpe-tasks-app/issues/133) | Готово (закрыта) | да | Новая шторка: плитки дней |
| T-43 | [#134](https://github.com/prostovasei/simpe-tasks-app/issues/134) | Готово (закрыта) | да | Уведомления и их значки |
| T-44 | [#135](https://github.com/prostovasei/simpe-tasks-app/issues/135) | Готово (закрыта) | да | «ОК» на клавиатуре не сохраняет задачу |
| T-45 | [#136](https://github.com/prostovasei/simpe-tasks-app/issues/136) | Готово (закрыта) | да | Тоньше обводка у кнопок внизу окна задачи |
| T-46 | [#137](https://github.com/prostovasei/simpe-tasks-app/issues/137) | Готово (закрыта) | да | Крестик на плитке дня — сверху справа, как у кнопок |
| T-47 | [#138](https://github.com/prostovasei/simpe-tasks-app/issues/138) | Готово (закрыта) | да | Кнопки внизу ×1,25 и скругление как у плиток |
| T-48 | [#139](https://github.com/prostovasei/simpe-tasks-app/issues/139) | Готово (закрыта) | да | Повтор без даты — бледный |
| T-49 | [#140](https://github.com/prostovasei/simpe-tasks-app/issues/140) | Готово (закрыта) | да | Нижняя панель виджета: градиент и минимум кнопок |
| T-50 | [#141](https://github.com/prostovasei/simpe-tasks-app/issues/141) | Готово (закрыта) | да | Виджет: плотнее названия, крупнее иконки и даты |
| T-51 | [#142](https://github.com/prostovasei/simpe-tasks-app/issues/142) | Готово (закрыта) | да | Мягкая подсветка выбранного и серый крестик |
| T-52 | [#143](https://github.com/prostovasei/simpe-tasks-app/issues/143) | Готово (закрыта) | да | Белый фон вместо светло-зелёного |
| T-53 | [#144](https://github.com/prostovasei/simpe-tasks-app/issues/144) | Готово (закрыта) | да | Задачи в приложении как в виджете |
| T-54 | [#145](https://github.com/prostovasei/simpe-tasks-app/issues/145) | Готово (закрыта) | да | Нестабильный тест «пустое название» |
| T-55 | [#146](https://github.com/prostovasei/simpe-tasks-app/issues/146) | Готово (закрыта) | да | Один вид списка в приложении и виджете |
| T-56 | [#147](https://github.com/prostovasei/simpe-tasks-app/issues/147) | Готово (закрыта) | да | Колокольчик без цифр |
| T-57 | [#148](https://github.com/prostovasei/simpe-tasks-app/issues/148) | Готово (закрыта) | да | Повтор: иконка только в плашке |
| T-58 | [#149](https://github.com/prostovasei/simpe-tasks-app/issues/149) | Готово (закрыта) | да | Порядок значков в плашке |
| T-59 | [#150](https://github.com/prostovasei/simpe-tasks-app/issues/150) | Готово (закрыта) | да | Галочка выполненной: цвет приоритета, капсула |
| T-60 | [#151](https://github.com/prostovasei/simpe-tasks-app/issues/151) | Готово (закрыта) | да | Выполненная остаётся в списке до 00:00 |
| T-61 | [#152](https://github.com/prostovasei/simpe-tasks-app/issues/152) | Готово (закрыта) | да | Вид выполненной: зачёркнута и потухла |
| T-62 | [#153](https://github.com/prostovasei/simpe-tasks-app/issues/153) | Готово (закрыта) | да | Кружок-галочка крупнее на 15% |
| T-63 | [#154](https://github.com/prostovasei/simpe-tasks-app/issues/154) | Готово (закрыта) | да | Уведомление: название, время, «Выполнить» |
| T-64 | [#155](https://github.com/prostovasei/simpe-tasks-app/issues/155) | Готово (закрыта) | да | Повторяющаяся задача при выполнении |
| T-65 | [#156](https://github.com/prostovasei/simpe-tasks-app/issues/156) | Готово (закрыта) | да | Напоминание заранее — колокольчик со стрелкой назад |
| T-66 | [#157](https://github.com/prostovasei/simpe-tasks-app/issues/157) | Готово (закрыта) | да | Плитки дней: дата дальше двух недель |
| T-67 | [#158](https://github.com/prostovasei/simpe-tasks-app/issues/158) | Готово (закрыта) | да | Просрочка и P1 не путаются: кружок всегда цвета приоритета |
| T-68 | [#159](https://github.com/prostovasei/simpe-tasks-app/issues/159) | Готово (закрыта) | да | Кружок выполнения 20 dp |
| T-69 | [#160](https://github.com/prostovasei/simpe-tasks-app/issues/160) | Готово (закрыта) | да | Всплывающая полоска напоминания снизу |
| T-70 | [#161](https://github.com/prostovasei/simpe-tasks-app/issues/161) | Готово (закрыта) | да | Анимации полоски напоминания: П10, В9, О5 |
| T-71 | [#162](https://github.com/prostovasei/simpe-tasks-app/issues/162) | Готово (закрыта) | да | Точные напоминания (режим «будильник») |
| T-72 | [#163](https://github.com/prostovasei/simpe-tasks-app/issues/163) | Готово (закрыта) | да | «Отменить» на полоске напоминания |
| T-73 | [#164](https://github.com/prostovasei/simpe-tasks-app/issues/164) | Готово (закрыта) | да | Быстрее открытие и закрытие окна задачи |
| T-75 | [#165](https://github.com/prostovasei/simpe-tasks-app/issues/165) | Готово (закрыта) | да | Кнопка «На завтра» в уведомлении |
| T-79 | [#166](https://github.com/prostovasei/simpe-tasks-app/issues/166) | Готово (закрыта) | да | Прогресс дня в виджете |
| T-80 | [#167](https://github.com/prostovasei/simpe-tasks-app/issues/167) | Готово (закрыта) | да | Утренняя сводка |
| T-81 | [#168](https://github.com/prostovasei/simpe-tasks-app/issues/168) | Готово (закрыта) | да | Озвучка плана дня |
| T-82 | [#169](https://github.com/prostovasei/simpe-tasks-app/issues/169) | Готово (закрыта) | да | Нестабильный тест будильника напоминаний |
| T-83 | [#170](https://github.com/prostovasei/simpe-tasks-app/issues/170) | Бэклог (открыта) | да | Поделиться в Simple Tasks |
| T-84 | [#171](https://github.com/prostovasei/simpe-tasks-app/issues/171) | Готово (закрыта) | да | Отложить задачу из уведомления и полоски |
| T-85 | [#172](https://github.com/prostovasei/simpe-tasks-app/issues/172) | Бэклог (открыта) | да | Повтор «после выполнения» |
| T-86 | [#173](https://github.com/prostovasei/simpe-tasks-app/issues/173) | Бэклог (открыта) | да | Итог дня вечером |
| T-87 | [#174](https://github.com/prostovasei/simpe-tasks-app/issues/174) | Бэклог (открыта) | да | Авто-копия данных |
| T-88 | [#175](https://github.com/prostovasei/simpe-tasks-app/issues/175) | Бэклог (открыта) | да | Список и приоритет в названии |
| T-89 | [#176](https://github.com/prostovasei/simpe-tasks-app/issues/176) | Бэклог (открыта) | да | Кликабельные ссылки в описании |
| T-90 | [#177](https://github.com/prostovasei/simpe-tasks-app/issues/177) | Бэклог (открыта) | да | Прогресс подзадач на плашке |
| T-91 | [#178](https://github.com/prostovasei/simpe-tasks-app/issues/178) | Бэклог (открыта) | да | Малый виджет 1×1 |
| T-92 | [#179](https://github.com/prostovasei/simpe-tasks-app/issues/179) | Бэклог (открыта) | да | Статистика |
| T-93 | [#180](https://github.com/prostovasei/simpe-tasks-app/issues/180) | Бэклог (открыта) | да | Тихие часы |
| T-94 | [#181](https://github.com/prostovasei/simpe-tasks-app/issues/181) | Бэклог (открыта) | да | Подсказка названия |
| T-95 | [#182](https://github.com/prostovasei/simpe-tasks-app/issues/182) | Бэклог (открыта) | да | Копия задачи |
| T-96 | [#183](https://github.com/prostovasei/simpe-tasks-app/issues/183) | Готово (закрыта) | да | «Отложить» в окне задачи (виджет и приложение) |
| T-97 | [#184](https://github.com/prostovasei/simpe-tasks-app/issues/184) | Бэклог (открыта) | да | Напоминание по месту |
| T-98 | [#185](https://github.com/prostovasei/simpe-tasks-app/issues/185) | Бэклог (открыта) | да | Календарь телефона |
| T-99 | [#186](https://github.com/prostovasei/simpe-tasks-app/issues/186) | Бэклог (открыта) | да | Закрепить задачу |
| T-100 | [#187](https://github.com/prostovasei/simpe-tasks-app/issues/187) | Бэклог (открыта) | да | Сортировка списка |
| T-101 | [#188](https://github.com/prostovasei/simpe-tasks-app/issues/188) | Бэклог (открыта) | да | Журнал изменений задачи |
| T-102 | [#189](https://github.com/prostovasei/simpe-tasks-app/issues/189) | Бэклог (открыта) | да | Быстрый шаблон |
| T-103 | [#190](https://github.com/prostovasei/simpe-tasks-app/issues/190) | Приостановлен (открыта) | да | Повторное нажатие на выполненный повтор = «Отмена» |
| T-104 | [#191](https://github.com/prostovasei/simpe-tasks-app/issues/191) | Готово (закрыта) | да | Чек-лист проверки: догнать 0.29–0.46 |
| T-105 | [#192](https://github.com/prostovasei/simpe-tasks-app/issues/192) | Бэклог (открыта) | да | «Пропустить этот повтор» (Skip, как в TickTick) |
| T-106 | [#193](https://github.com/prostovasei/simpe-tasks-app/issues/193) | Готово (закрыта) | да | Лента виджета на 100+ задачах |
| T-107 | [#194](https://github.com/prostovasei/simpe-tasks-app/issues/194) | Готово (закрыта) | да | Новая нижняя строка виджета |
| T-108 | [#195](https://github.com/prostovasei/simpe-tasks-app/issues/195) | Готово (закрыта) | да | «Неделя» по рефу и чёрный акцент |
| T-109 | [#196](https://github.com/prostovasei/simpe-tasks-app/issues/196) | Готово (закрыта) | да | Два приоритета и эмодзи списков |
| T-110 | [#197](https://github.com/prostovasei/simpe-tasks-app/issues/197) | Готово (закрыта) | да | Поправки «Недели» в виджете |
| T-111 | [#198](https://github.com/prostovasei/simpe-tasks-app/issues/198) | Готово (закрыта) | да | «Не буду делать» |
| T-112 | [#199](https://github.com/prostovasei/simpe-tasks-app/issues/199) | Готово (закрыта) | да | Перенос одного повтора |
| T-113 | [#200](https://github.com/prostovasei/simpe-tasks-app/issues/200) | Готово (закрыта) | да | Пересекающиеся задачи на сетке окна даты |
| T-114 | [#201](https://github.com/prostovasei/simpe-tasks-app/issues/201) | Готово (закрыта) | да | Дата в нижней строке и шторка дня |
| T-115 | [#202](https://github.com/prostovasei/simpe-tasks-app/issues/202) | Готово (закрыта) | да | Перенос серии ровно на выбранную дату |
| T-116 | [#203](https://github.com/prostovasei/simpe-tasks-app/issues/203) | Готово (закрыта) | да | «Входящие», «Когда-нибудь», «Без списка» |
| T-117 | [#204](https://github.com/prostovasei/simpe-tasks-app/issues/204) | Готово (закрыта) | да | Эмодзи 📌 «Без списка» и 💭 «Когда-нибудь» |
| T-118 | [#205](https://github.com/prostovasei/simpe-tasks-app/issues/205) | Готово (закрыта) | да | Белая полоска напоминания с чёрными кнопками |
| T-119 | [#206](https://github.com/prostovasei/simpe-tasks-app/issues/206) | Готово (закрыта) | да | Новый навбар: «Предстоящее/Неделя», «+» посередине, «Списки» |
| T-120 | [#207](https://github.com/prostovasei/simpe-tasks-app/issues/207) | Готово (закрыта) | да | «Входящие», «Поиск» и «Настройки» — в разделе «Списки» |
| T-121 | [#208](https://github.com/prostovasei/simpe-tasks-app/issues/208) | Готово (закрыта) | да | «Неделя» в приложении — как в виджете |
| T-122 | [#209](https://github.com/prostovasei/simpe-tasks-app/issues/209) | Готово (закрыта) | да | Значки приложения — как в виджете |
| T-123 | [#210](https://github.com/prostovasei/simpe-tasks-app/issues/210) | Готово (закрыта) | да | «Списки»: три группы, без заголовка, названия жирнее |
| T-124 | [#211](https://github.com/prostovasei/simpe-tasks-app/issues/211) | Готово (закрыта) | да | «Списки»: поиск, «+ папка», «+ список» и настройки — значками внизу |
| T-125 | [#212](https://github.com/prostovasei/simpe-tasks-app/issues/212) | Готово (закрыта) | да | Число задач у каждого списка, если не 0 |
| T-126 | [#213](https://github.com/prostovasei/simpe-tasks-app/issues/213) | Готово (закрыта) | да | Эмодзи у папки |
| T-127 | [#214](https://github.com/prostovasei/simpe-tasks-app/issues/214) | Готово (закрыта) | да | Новое окно создания списка и папки: сразу выбор эмодзи |
| T-128 | [#215](https://github.com/prostovasei/simpe-tasks-app/issues/215) | Готово (закрыта) | да | Новый вид нижней строки виджета (10 вариантов на выбор) |
