package com.voidx.client.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.voidx.client.domain.model.DownloadItem
import com.voidx.client.domain.model.DownloadState
import com.voidx.client.domain.model.PackKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Downloads pack files from a direct https:// link using Android's DownloadManager.
 * Files land in the app's own Downloads folder, so no storage permission is needed.
 * Only files with a supported pack extension are accepted.
 */
class DownloadRepository(private val context: Context) {

    private val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /** Validates the URL and starts the download. Throws [IllegalArgumentException] with a readable message. */
    fun enqueue(rawUrl: String): Long {
        val url = rawUrl.trim()
        val uri = Uri.parse(url)
        require(uri.scheme.equals("https", ignoreCase = true)) { "Enter a link that starts with https://" }
        val fileName = uri.lastPathSegment?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("The link does not point to a file.")
        require(PackKind.fromFileName(fileName) != null) {
            "Only .mcpack, .mcaddon and .mcworld files can be downloaded."
        }
        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("VoidX Client download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        return dm.enqueue(request)
    }

    /** Emits the current state of [downloadId] every second until it finishes or fails. */
    fun observe(downloadId: Long): Flow<DownloadItem?> = flow {
        while (true) {
            val item = query(downloadId)
            emit(item)
            if (item == null || item.state == DownloadState.SUCCESSFUL || item.state == DownloadState.FAILED) break
            delay(1000)
        }
    }

    fun cancel(downloadId: Long) {
        dm.remove(downloadId)
    }

    fun query(downloadId: Long): DownloadItem? {
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId)) ?: return null
        cursor.use { c ->
            if (!c.moveToFirst()) return null
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reasonCode = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val uriString = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)) ?: ""
            val title = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "download"
            val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            val state = when (status) {
                DownloadManager.STATUS_PENDING -> DownloadState.PENDING
                DownloadManager.STATUS_RUNNING -> DownloadState.RUNNING
                DownloadManager.STATUS_PAUSED -> DownloadState.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> DownloadState.SUCCESSFUL
                else -> DownloadState.FAILED
            }
            val localPath = localUri?.let { Uri.parse(it).path }?.takeIf { File(it).exists() }
            return DownloadItem(
                downloadId = downloadId,
                fileName = title,
                url = uriString,
                state = state,
                bytesDone = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                bytesTotal = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                localPath = localPath,
                reason = if (state == DownloadState.FAILED) failureReason(reasonCode) else null,
            )
        }
    }

    private fun failureReason(code: Int): String = when (code) {
        DownloadManager.ERROR_CANNOT_RESUME -> "The download cannot be resumed."
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No storage device found."
        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "A file with this name already exists in the download folder."
        DownloadManager.ERROR_FILE_ERROR -> "A file error occurred while saving."
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "Data transfer failed."
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space."
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects."
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "The server returned an error."
        DownloadManager.ERROR_UNKNOWN -> "Unknown download error."
        DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network."
        DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for Wi-Fi."
        else -> "Download failed (code $code)."
    }
}
