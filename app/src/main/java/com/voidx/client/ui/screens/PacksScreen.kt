package com.voidx.client.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voidx.client.domain.model.LibraryPack
import com.voidx.client.ui.AppViewModel
import com.voidx.client.ui.theme.VoidSurface
import com.voidx.client.ui.theme.VoidViolet
import java.text.DecimalFormat

@Composable
fun PacksScreen(vm: AppViewModel) {
    val packs by vm.packs.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selected by remember { mutableStateOf<LibraryPack?>(null) }
    var pendingDelete by remember { mutableStateOf<LibraryPack?>(null) }

    LaunchedEffect(Unit) { vm.refreshPacks() }

    // OpenDocument with "*/*": Android has no reliable MIME type for .mcpack/.mcaddon/.mcworld,
    // so the repository validates the extension and archive contents instead.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = displayName(context, uri)
        vm.importPack(name) { context.contentResolver.openInputStream(uri) }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text("Packs & Add-ons", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Files are stored in VoidX. Open one in another app to use it there.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = { picker.launch(arrayOf("*/*")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Importing…")
            } else {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import .mcpack / .mcaddon / .mcworld")
            }
        }

        if (packs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = VoidSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "No packs yet. Import a file from your device, or download one from the Downloads tab.",
                    modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(packs, key = { it.id }) { pack ->
                    PackRow(
                        pack = pack,
                        onOpen = { selected = pack },
                        onShare = {
                            val intent = vm.shareIntentFor(pack)
                            if (intent != null) startExternal(context, intent)
                        },
                        onDelete = {
                            if (prefs.confirmDelete) pendingDelete = pack else vm.deletePack(pack.id)
                        },
                    )
                }
            }
        }
    }

    selected?.let { pack ->
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } },
            title = { Text(pack.entries.firstOrNull()?.name ?: pack.fileName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("File: ${pack.fileName}")
                    Text("Type: ${pack.kind.label}")
                    Text("Size: ${formatSize(pack.sizeBytes)}")
                    pack.entries.forEach { e ->
                        Text(
                            "• ${e.name}${e.version?.let { " v$it" } ?: ""}",
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (e.description.isNotBlank()) {
                            Text(e.description, style = MaterialTheme.typography.bodySmall)
                        }
                        if (e.moduleTypes.isNotEmpty()) {
                            Text(
                                "Modules: ${e.moduleTypes.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        e.uuid?.let { Text("UUID: $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
        )
    }

    pendingDelete?.let { pack ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove pack?") },
            text = { Text("This deletes \"${pack.fileName}\" from VoidX. Minecraft is not changed.") },
            confirmButton = {
                TextButton(onClick = { vm.deletePack(pack.id); pendingDelete = null }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PackRow(
    pack: LibraryPack,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    pack.entries.firstOrNull()?.name ?: pack.fileName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    "${pack.kind.label} · ${formatSize(pack.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = VoidViolet,
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Open in another app")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove")
            }
        }
    }
}

/** Hands the stored file to an app that can open it. Minecraft may or may not register for this type. */
private fun startExternal(context: Context, intent: Intent) {
    try {
        context.startActivity(Intent.createChooser(intent, "Open pack with"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app on this device can open this pack.", Toast.LENGTH_LONG).show()
    }
}

private fun displayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) return c.getString(0) ?: "" }
    return uri.lastPathSegment?.substringAfterLast('/') ?: ""
}

private fun formatSize(bytes: Long): String {
    val df = DecimalFormat("#.#")
    return when {
        bytes >= 1024L * 1024L -> "${df.format(bytes / 1024.0 / 1024.0)} MB"
        bytes >= 1024L -> "${df.format(bytes / 1024.0)} KB"
        else -> "$bytes B"
    }
}
