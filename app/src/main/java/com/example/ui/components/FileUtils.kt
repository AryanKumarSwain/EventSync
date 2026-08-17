package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File

fun downloadCsvFile(context: Context, fileName: String, csvContent: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "✅ Sample template saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
            } else {
                saveToFileFallback(context, fileName, csvContent)
            }
        } else {
            saveToFileFallback(context, fileName, csvContent)
        }
    } catch (e: Exception) {
        saveToFileFallback(context, fileName, csvContent)
    }
}

private fun saveToFileFallback(context: Context, fileName: String, csvContent: String) {
    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = File(downloadsDir, fileName)
        file.writeText(csvContent, Charsets.UTF_8)
        Toast.makeText(context, "✅ Sample template saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        try {
            val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(appDir, fileName)
            file.writeText(csvContent, Charsets.UTF_8)
            Toast.makeText(context, "✅ Template saved to ${file.name}", Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Toast.makeText(context, "⚠️ Template pre-filled in dialog!", Toast.LENGTH_SHORT).show()
        }
    }
}
