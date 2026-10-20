package com.example.ui.common

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.VideoItem
import java.io.File

object VideoLauncher {

    fun openInExternalPlayer(context: Context, filePath: String, title: String): Boolean {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "Video file not found at: $filePath", Toast.LENGTH_LONG).show()
                return false
            }
            if (file.length() == 0L) {
                Toast.makeText(context, "Video file is empty (0 bytes). Please re-download.", Toast.LENGTH_LONG).show()
                return false
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = when {
                file.name.endsWith(".webm", ignoreCase = true) -> "video/webm"
                file.name.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
                file.name.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
                else -> "video/mp4"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Play \"$title\" with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return true
        } catch (e: Exception) {
            // Fallback generic viewer
            try {
                val file = File(filePath)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Open Video"))
                return true
            } catch (fallbackEx: Exception) {
                Toast.makeText(context, "No app found to play video: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                return false
            }
        }
    }

    fun openInExternalPlayer(context: Context, video: VideoItem): Boolean {
        return openInExternalPlayer(context, video.filePath, video.title)
    }
}
