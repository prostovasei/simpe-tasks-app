package com.prostovasei.simpletasks.ui.navigation

import kotlinx.serialization.Serializable

/** Маршруты приложения (типобезопасная навигация, docs/ARCHITECTURE.md §2.4). */
@Serializable
data object InboxDestination

@Serializable
data object AllDestination

/** Вид «Неделя» (итерация 44). */
@Serializable
data object WeekDestination

/** Полный список дня недели (итерация 44): [date] — день в виде «2026-10-30». Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class DayDestination(val date: String)

@Serializable
data object ListsDestination

@Serializable
data object SearchDestination

@Serializable
data object SettingsDestination

/**
 * Быстрое создание задачи. [projectId] — список по умолчанию (null — «Входящие»);
 * [today] — задача сразу на сегодня (открыто с экрана «Все»).
 */
@Serializable
data class QuickAddDestination(
    val projectId: String? = null,
    val today: Boolean = false,
    val date: String? = null,
    /** Из «Когда-нибудь» (итерация 52): сразу открыть выбор списка — задача без даты со списком. */
    val pickList: Boolean = false,
    /** #95: текст из голосового ввода — подставляется в название, клавиатура не открывается. */
    val voiceText: String? = null,
    /** #310: правка продиктованной задачи [VoiceTask.id] — после сохранения она уходит из списка окна голоса; черновика нет. */
    val voiceTask: Long? = null,
)

/** #95: окно голосового ввода поверх рабочего стола (микрофон в виджете); [today] — как у «+» вида «Все». */
@Serializable
data class VoiceDestination(val today: Boolean = false)

/** Прозрачное окно «Перенести просроченные на сегодня» поверх рабочего стола (итерация 37, из виджета). */
@Serializable
data object MoveOverdueDestination

/** Уведомления банка (#233): настройки распознавания покупок. */
@Serializable
data object FinanceBankDestination

/** «Не разобрано» (#233): покупки из уведомлений банка без ответа. */
@Serializable
data object FinanceUnsortedDestination

/** Не экран: «Изменить» в уведомлении о покупке — финансы с окном нового расхода (#233). */
data class OpenBankPurchase(val purchaseId: String)
data class OpenBankQueue(val purchaseId: String?)

/** Не экран: уведомление бюджета (#327) — финансы с окном «Бюджет» месяца [month] («YYYY-MM»). */
data class OpenBudget(val month: String, val prepare: Boolean)

/** Корзина (итерация 39). */
@Serializable
data object TrashDestination

/** Задача в корзине — шторка только для чтения (итерация 39). Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class TrashTaskDestination(val taskId: String)

/** «Не буду делать» (итерация 49). */
@Serializable
data object WontDoDestination

/** «Когда-нибудь» (итерация 52): задачи со списком, но без даты. */
@Serializable
data object SomedayDestination

/** «Без списка» (итерация 52): задачи без списка, но с датой. */
@Serializable
data object NoListDestination

/** Задача «Не буду делать» — шторка только для чтения (итерация 49). Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class WontDoTaskDestination(val taskId: String)

/** Выполненные задачи (итерация 11). */
@Serializable
data object CompletedDestination

/** Экран задачи; [openDate] — сразу окно даты и времени (итерация 53: «Отложить» → «Выбрать дату и время…»). */
@Serializable
data class TaskDetailDestination(val taskId: String, val openDate: Boolean = false)

/** Окно «Отложить» поверх экрана — из кнопки уведомления (итерация 53). */
@Serializable
data class SnoozeDestination(val taskId: String)

/** Экран задач списка. Имя поля — ключ аргумента в SavedStateHandle. */
@Serializable
data class ProjectDestination(val projectId: String)

/** Хаб «Главная» (#329): стартовый экран, первая вкладка нижней панели (домик). */
@Serializable
data object HubDestination

/** Раздел «Финансы» (#217, этап 1 — #221): открывается значком «₽» верхнего бара модулей. */
@Serializable
data object FinanceDestination

/** Корзина финансов (этап 4, #231): из меню шестерёнки. */
/** Единые настройки (#241): три раздела — «Задачи», «Финансы», «Приложение». Открываются из меню кнопки модуля в нижней панели. */
@Serializable
data object AppSettingsDestination

/** Раздел «Приложение» единых настроек (#241): обновление и версия. */
@Serializable
data object AppInfoDestination

/** Страница «Финансы» единых настроек (#239). */
@Serializable
data object FinanceSettingsDestination

/** Справочник мест (#239). */
@Serializable
data object FinancePlacesDestination

@Serializable
data object FinanceTrashDestination

/** Категории финансов (этап 2, #225): из меню шестерёнки финансов. */
@Serializable
data object FinanceCategoriesDestination

/** Раздел «Здоровье» (#257): вес. Открывается из меню кнопки модуля в нижней панели. */
@Serializable
data object HealthDestination

/** Страница «Здоровье» единых настроек (#257): напоминание взвеситься и резервная копия. */
@Serializable
data object HealthSettingsDestination

/** Резервная копия «Здоровья» (#257). */
@Serializable
data object HealthBackupDestination

/** Не экран: нажатие на «Пора взвеситься» — раздел «Здоровье» с окном ввода веса (#257). */
data object OpenHealthWeight

/** Не экран: нажатие на уведомление «N новых новостей» — вкладка «Новости» (#296). */
data object OpenNews

/** Страница «Новости» единых настроек (#296): уведомления и тишина. */
@Serializable
data object NewsSettingsDestination

@Serializable
data object NewsDestination
