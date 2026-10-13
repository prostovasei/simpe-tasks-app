package com.prostovasei.simpletasks.core.files

import android.content.Context
import androidx.core.net.toUri
import java.io.FileNotFoundException
import java.io.InputStream

/** Открывает файл, выбранный в системном пикере (Storage Access Framework), по его content:// адресу. */
fun interface DocumentOpener {
    fun open(uri: String): InputStream
}

class ContentResolverDocumentOpener(private val context: Context) : DocumentOpener {
    override fun open(uri: String): InputStream =
        context.contentResolver.openInputStream(uri.toUri()) ?: throw FileNotFoundException(uri)
}
