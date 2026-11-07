package com.voidx.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voidx.client.domain.model.DownloadItem
import com.voidx.client.domain.model.DownloadState
import com.voidx.client.ui.AppViewModel
import com.voidx.client.ui.theme.VoidGreen
import com.voidx.client.ui.theme.VoidSurface

@Composable
fun DownloadsScreen(vm: AppViewModel) {
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Downloads", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Paste a direct https:// link to a .mcpack, .mcaddon or .mcworld file. " +
                "When it finishes, import it into your library.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Direct file link (https://…)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                vm.startDownload(url)
                url = ""
            },
            enabled = url.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start download")
        }

        if (downloads.isEmpty()) {
            Text(
                "No downloads yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(downloads, key = { it.downloadId }) { item ->
                    DownloadRow(
                        item = item,
                        onImport = { vm.importDownload(item) },
                        onCancel = { vm.cancelDownload(item.downloadId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onImport: () -> Unit, onCancel: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.fileName, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
                if (item.state != DownloadState.SUCCESSFUL && item.state != DownloadState.FAILED) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                }
            }
            when (item.state) {
                DownloadState.PENDING, DownloadState.RUNNING, DownloadState.PAUSED -> {
                    val total = item.bytesTotal
                    if (total > 0) {
                        LinearProgressIndicator(
                            progress = { (item.bytesDone.toFloat() / total).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = VoidGreen,
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VoidGreen)
                    }
                    Text(
                        "${item.state.label()} · ${item.bytesDone / 1024} KB" +
                            if (total > 0) " of ${total / 1024} KB" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DownloadState.SUCCESSFUL -> {
                    Text("Downloaded", color = VoidGreen, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                        Text("Import into library")
                    }
                }
                DownloadState.FAILED -> {
                    Text(
                        item.reason ?: "Download failed.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun DownloadState.label(): String = when (this) {
    DownloadState.PENDING -> "Queued"
    DownloadState.RUNNING -> "Downloading"
    DownloadState.PAUSED -> "Paused"
    DownloadState.SUCCESSFUL -> "Done"
    DownloadState.FAILED -> "Failed"
}
