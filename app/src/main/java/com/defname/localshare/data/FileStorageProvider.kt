package com.defname.localshare.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

interface FileStorageProvider {
    suspend fun createOutputFile(fileName: String, mimeType: String): Uri?
    fun openOutputStream(uri: Uri): OutputStream?
    suspend fun deleteFile(uri: Uri)
    suspend fun finalizeFile(uri: Uri)
}

class MediaStoreStorageProvider(
    private val context: Context
) : FileStorageProvider {

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    override suspend fun createOutputFile(
        fileName: String,
        mimeType: String
    ): Uri? = withContext(Dispatchers.IO) {

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

            // Datei landet in /Download/LocalShare
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/LocalShare"
            )

            // Android markiert Datei während des Schreibens als "in Bearbeitung"
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        contentResolver.insert(collection, values)
    }

    override fun openOutputStream(uri: Uri): OutputStream? {
        return contentResolver.openOutputStream(uri)
    }

    override suspend fun deleteFile(uri: Uri) {
        withContext(Dispatchers.IO) {
            contentResolver.delete(uri, null, null)
        }
    }

    /**
     * Nach dem Schreiben aufrufen,
     * damit die Datei sichtbar wird.
     */
    override suspend fun finalizeFile(uri: Uri) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }

            contentResolver.update(uri, values, null, null)
        }
    }
}