package com.voidx.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voidx.client.domain.model.ThemeMode
import com.voidx.client.ui.AppViewModel
import com.voidx.client.ui.theme.VoidSurface

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        SectionCard(title = "Theme") {
            listOf(
                ThemeMode.SYSTEM to "Follow system",
                ThemeMode.DARK to "Dark (VoidX)",
                ThemeMode.LIGHT to "Light",
            ).forEach { (mode, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = prefs.themeMode == mode, onClick = { vm.setThemeMode(mode) })
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = prefs.themeMode == mode, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(label)
                }
            }
        }

        SectionCard(title = "Behaviour") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Confirm before removing packs")
                    Text(
                        "Removing only affects VoidX's library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = prefs.confirmDelete, onCheckedChange = { vm.setConfirmDelete(it) })
            }
        }

        SectionCard(title = "About") {
            Text("VoidX Client 1.0.0", fontWeight = FontWeight.Medium)
            Text(
                "VoidX is an independent companion app. It is not affiliated with or endorsed by " +
                    "Mojang or Microsoft. Minecraft is a trademark of Mojang Synergies AB.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Limits: VoidX cannot inject code, switch installed Minecraft versions, or " +
                    "modify Minecraft's own data. Imported packs are stored in VoidX and can be " +
                    "opened in another app from the Packs tab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VoidSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
