package com.prostovasei.simpletasks.core.id

import java.util.UUID

/** Генератор первичных ключей: UUIDv4 строкой (SPEC.md §1). */
fun interface IdGenerator {
    fun newId(): String
}

class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
