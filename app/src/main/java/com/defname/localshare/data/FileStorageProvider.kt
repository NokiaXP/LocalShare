package com.defname.localshare.data

import android.net.Uri
import java.io.OutputStream

interface FileStorageProvider {
    suspend fun createOutputFile(fileName: String, mimeType: String): Uri?
    fun openOutputStream(uri: Uri): OutputStream?
    suspend fun deleteFile(uri: Uri)
}
