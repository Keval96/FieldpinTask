package com.keval.fieldpintask.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Copies files picked through the system picker into app storage so they survive offline and URI expiry. */
@Singleton
class AttachmentStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val directory = File(context.filesDir, "attachments").apply { mkdirs() }

    fun import(uri: Uri): String? = runCatching {
        val target = File(File(directory, UUID.randomUUID().toString()).apply { mkdirs() }, displayName(uri))
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            ?: return null
        target.absolutePath
    }.getOrNull()

    fun clear() {
        directory.deleteRecursively()
        directory.mkdirs()
    }

    private fun displayName(uri: Uri): String {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        // The name goes into a file path, so strip separators from whatever the provider returns.
        return (name ?: "attachment").replace('/', '_').replace('\\', '_')
    }
}
