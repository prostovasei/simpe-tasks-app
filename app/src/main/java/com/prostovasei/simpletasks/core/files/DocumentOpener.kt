package com.prostovasei.simpletasks.core.files

import android.content.Context
import androidx.core.net.toUri
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

/** Открывает файл, выбранный в системном пикере (Storage Access Framework), по его content:// адресу. */
interface DocumentOpener {
    fun openInput(uri: String): InputStream

    /** Открывает файл на запись с заменой содержимого. */
    fun openOutput(uri: String): OutputStream
}

class ContentResolverDocumentOpener(private val context: Context) : DocumentOpener {
    override fun openInput(uri: String): InputStream =
        context.contentResolver.openInputStream(uri.toUri()) ?: throw FileNotFoundException(uri)

    override fun openOutput(uri: String): OutputStream =
        context.contentResolver.openOutputStream(uri.toUri(), "wt") ?: throw FileNotFoundException(uri)
}
