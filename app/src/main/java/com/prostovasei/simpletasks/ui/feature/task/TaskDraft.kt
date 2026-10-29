package com.prostovasei.simpletasks.ui.feature.task

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Название и описание не сохранённой новой задачи (итерация 38, бэклог №28). */
data class TaskDraft(val title: String, val content: String)

/**
 * Черновик новой задачи: карточку закрыли, не сохранив, — название и описание запоминаются и подставляются при следующем «+»
 * (с любого экрана и из виджета). Один черновик, без срока годности; дата, список, приоритет и чек-лист не запоминаются.
 */
@Singleton
class TaskDrafts(initial: TaskDraft?, private val persist: (TaskDraft?) -> Unit) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    private constructor(prefs: SharedPreferences) : this(
        initial = prefs.getString(TITLE, null)?.takeIf { it.isNotBlank() }?.let { TaskDraft(it, prefs.getString(CONTENT, "").orEmpty()) },
        persist = { draft ->
            prefs.edit(commit = true) {
                if (draft == null) clear() else putString(TITLE, draft.title).putString(CONTENT, draft.content)
            }
        },
    )

    private var current: TaskDraft? = initial

    fun get(): TaskDraft? = current

    /** Запоминает [title] и [content]; пустое название — черновика нет. */
    fun save(title: String, content: String) {
        current = title.trim().takeIf { it.isNotEmpty() }?.let { TaskDraft(title, content) }
        persist(current)
    }

    fun clear() = save("", "")

    private companion object {
        const val PREFS = "task_draft"
        const val TITLE = "title"
        const val CONTENT = "content"
    }
}
