package com.example.grindlyapp1
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream



    object FileUtils {
        fun uriToFile(context: Context, uri: Uri): File? {
            val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name = cursor.getString(nameIndex)
            cursor.close()

            val file = File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return file
        }
    }

