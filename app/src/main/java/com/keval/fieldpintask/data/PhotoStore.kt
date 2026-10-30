package com.keval.fieldpintask.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** App-private storage for inspection photos; wiped on logout with the rest of the local data. */
@Singleton
class PhotoStore @Inject constructor(@ApplicationContext context: Context) {
    private val directory = File(context.filesDir, "photos").apply { mkdirs() }

    fun newPhotoFile(): File = File(directory, "${UUID.randomUUID()}.jpg")

    fun clear() {
        directory.deleteRecursively()
        directory.mkdirs()
    }
}
