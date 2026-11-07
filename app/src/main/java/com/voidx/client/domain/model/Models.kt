package com.voidx.client.domain.model

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class AppPrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val confirmDelete: Boolean = true,
)

data class MinecraftInstall(
    val installed: Boolean,
    val versionName: String?,
    val versionCode: Long?,
)

enum class PackKind(val extension: String, val label: String) {
    MCPACK("mcpack", "Resource / behavior pack"),
    MCADDON("mcaddon", "Add-on bundle"),
    MCWORLD("mcworld", "World");

    companion object {
        fun fromFileName(name: String): PackKind? {
            val ext = name.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.extension == ext }
        }
    }
}

/** One entry parsed from a manifest.json (or a world's level metadata). */
data class PackDetails(
    val name: String,
    val description: String,
    val uuid: String?,
    val version: String?,
    val moduleTypes: List<String>,
)

/** A pack file stored in the VoidX library after a successful import. */
data class LibraryPack(
    val id: String,
    val fileName: String,
    val kind: PackKind,
    val sizeBytes: Long,
    val importedAt: Long,
    val entries: List<PackDetails>,
)

enum class DownloadState { PENDING, RUNNING, PAUSED, SUCCESSFUL, FAILED }

data class DownloadItem(
    val downloadId: Long,
    val fileName: String,
    val url: String,
    val state: DownloadState,
    val bytesDone: Long,
    val bytesTotal: Long,
    val localPath: String?,
    val reason: String?,
)
