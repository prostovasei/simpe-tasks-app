package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.data.local.entity.FinanceAccountEntity
import com.prostovasei.simpletasks.data.local.entity.FinanceCategoryEntity
import com.prostovasei.simpletasks.data.local.entity.FinanceOperationEntity
import com.prostovasei.simpletasks.domain.model.finance.InvalidFinanceBackupException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Содержимое резервной копии финансов: строки трёх таблиц как есть. */
internal data class FinanceBackup(
    val accounts: List<FinanceAccountEntity>,
    val categories: List<FinanceCategoryEntity>,
    val operations: List<FinanceOperationEntity>,
)

/**
 * Файл резервной копии финансов (этап 4, #231) — JSON: пометка [FORMAT], версия [VERSION], время и три массива строк таблиц.
 * Суммы — копейки, дни — числом дней от 1970 года, как в базе: копия воспроизводит остатки точно.
 * Разбор проверяет пометку, версию и ссылки (счета и категории операций должны быть в той же копии).
 */
internal object FinanceBackupCodec {
    const val FORMAT = "simple-tasks-finance"
    const val VERSION = 1

    fun encode(backup: FinanceBackup, exportedAt: Long): String = JSONObject()
        .put("format", FORMAT)
        .put("version", VERSION)
        .put("exported_at", exportedAt)
        .put("accounts", JSONArray(backup.accounts.map { a ->
            JSONObject().put("id", a.id).put("name", a.name).put("type", a.type).putOpt("emoji", a.emoji)
                .put("initial_balance", a.initialBalance).put("sort_order", a.sortOrder).put("created_at", a.createdAt).put("updated_at", a.updatedAt)
        }))
        .put("categories", JSONArray(backup.categories.map { c ->
            JSONObject().put("id", c.id).put("name", c.name).put("kind", c.kind).put("is_archived", c.isArchived)
                .put("sort_order", c.sortOrder).put("created_at", c.createdAt).put("updated_at", c.updatedAt)
        }))
        .put("operations", JSONArray(backup.operations.map { o ->
            JSONObject().put("id", o.id).put("kind", o.kind).put("amount", o.amount).put("account_id", o.accountId)
                .putOpt("to_account_id", o.toAccountId).putOpt("category_id", o.categoryId).putOpt("place", o.place).putOpt("comment", o.comment)
                .put("date", o.date).put("is_deleted", o.isDeleted).putOpt("deleted_at", o.deletedAt).put("created_at", o.createdAt).put("updated_at", o.updatedAt)
        }))
        .toString(2)

    fun decode(json: String): FinanceBackup {
        try {
            val root = JSONObject(json)
            if (root.optString("format") != FORMAT) throw InvalidFinanceBackupException("Это не резервная копия финансов Simple Tasks")
            if (root.optInt("version", -1) != VERSION) throw InvalidFinanceBackupException("Неизвестная версия резервной копии")
            val accounts = root.getJSONArray("accounts").objects().map { a ->
                FinanceAccountEntity(a.getString("id"), a.getString("name"), a.getString("type"), a.text("emoji"), a.getLong("initial_balance"), a.getInt("sort_order"), a.getLong("created_at"), a.getLong("updated_at"))
            }
            val categories = root.getJSONArray("categories").objects().map { c ->
                FinanceCategoryEntity(c.getString("id"), c.getString("name"), c.getString("kind"), c.getBoolean("is_archived"), c.getInt("sort_order"), c.getLong("created_at"), c.getLong("updated_at"))
            }
            val operations = root.getJSONArray("operations").objects().map { o ->
                FinanceOperationEntity(
                    id = o.getString("id"), kind = o.getString("kind"), amount = o.getLong("amount"), accountId = o.getString("account_id"),
                    toAccountId = o.text("to_account_id"), categoryId = o.text("category_id"), place = o.text("place"), comment = o.text("comment"),
                    date = o.getLong("date"), isDeleted = o.getBoolean("is_deleted"), deletedAt = if (o.isNull("deleted_at")) null else o.getLong("deleted_at"),
                    createdAt = o.getLong("created_at"), updatedAt = o.getLong("updated_at"),
                )
            }
            val accountIds = accounts.map { it.id }.toSet()
            val categoryIds = categories.map { it.id }.toSet()
            val broken = operations.any { it.accountId !in accountIds || (it.toAccountId != null && it.toAccountId !in accountIds) || (it.categoryId != null && it.categoryId !in categoryIds) }
            val duplicates = accountIds.size != accounts.size || categoryIds.size != categories.size || operations.map { it.id }.toSet().size != operations.size
            if (broken || duplicates) throw InvalidFinanceBackupException("Резервная копия повреждена")
            return FinanceBackup(accounts, categories, operations)
        } catch (e: JSONException) {
            throw InvalidFinanceBackupException("Файл не читается как резервная копия", e)
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.text(key: String): String? = if (isNull(key)) null else getString(key)
}
