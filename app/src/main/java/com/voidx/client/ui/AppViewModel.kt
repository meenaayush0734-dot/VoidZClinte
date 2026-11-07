package com.voidx.client.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voidx.client.VoidXApp
import com.voidx.client.data.packs.ImportException
import com.voidx.client.domain.model.AppPrefs
import com.voidx.client.domain.model.DownloadItem
import com.voidx.client.domain.model.DownloadState
import com.voidx.client.domain.model.LibraryPack
import com.voidx.client.domain.model.MinecraftInstall
import com.voidx.client.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

data class UiMessage(val text: String, val isError: Boolean = true, val id: Long = System.nanoTime())

data class HomeState(
    val minecraft: MinecraftInstall? = null,
    val launchError: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as VoidXApp).container

    val prefs: StateFlow<AppPrefs> = container.preferences.prefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs())

    private val _home = MutableStateFlow(HomeState())
    val home: StateFlow<HomeState> = _home.asStateFlow()

    private val _packs = MutableStateFlow<List<LibraryPack>>(emptyList())
    val packs: StateFlow<List<LibraryPack>> = _packs.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val watchers = mutableMapOf<Long, Job>()

    init {
        refreshHome()
        refreshPacks()
    }

    fun refreshHome() {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { container.minecraft.detect() }
            _home.value = _home.value.copy(minecraft = info)
        }
    }

    fun launchMinecraft() {
        val intent = container.minecraft.launchIntent()
        if (intent == null) {
            _home.value = _home.value.copy(
                launchError = "Minecraft Bedrock is not installed (or has no launchable app). Install it from the Play Store first."
            )
            return
        }
        _home.value = _home.value.copy(launchError = null)
        runCatching { getApplication<Application>().startActivity(intent) }
            .onFailure { _home.value = _home.value.copy(launchError = "Could not open Minecraft: ${it.message}") }
    }

    fun clearLaunchError() {
        _home.value = _home.value.copy(launchError = null)
    }

    fun refreshPacks() {
        viewModelScope.launch {
            _packs.value = withContext(Dispatchers.IO) { container.packs.list() }
        }
    }

    fun importPack(fileName: String, open: () -> InputStream?) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val record = withContext(Dispatchers.IO) { container.packs.importFile(fileName, open) }
                _packs.value = _packs.value + record
                showMessage("Imported \"${record.entries.firstOrNull()?.name ?: record.fileName}\".", isError = false)
            } catch (e: ImportException) {
                showMessage(e.message ?: "Import failed.")
            } catch (e: Exception) {
                showMessage("Import failed: ${e.message ?: "unknown error"}")
            } finally {
                _busy.value = false
            }
        }
    }

    fun deletePack(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { container.packs.delete(id) }
            _packs.value = _packs.value.filterNot { it.id == id }
            showMessage("Removed from VoidX library.", isError = false)
        }
    }

    /** Builds an open intent for a stored pack. Returns null if the file is missing. */
    fun shareIntentFor(pack: LibraryPack): Intent? {
        val file = container.packs.fileFor(pack)
        if (!file.exists()) {
            showMessage("The stored file is missing. Re-import it.")
            return null
        }
        val app = getApplication<Application>()
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/octet-stream")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun startDownload(url: String) {
        viewModelScope.launch {
            val id = try {
                withContext(Dispatchers.IO) { container.downloads.enqueue(url) }
            } catch (e: IllegalArgumentException) {
                showMessage(e.message ?: "Invalid link.")
                return@launch
            } catch (e: Exception) {
                showMessage("Could not start download: ${e.message}")
                return@launch
            }
            watch(id)
        }
    }

    private fun watch(downloadId: Long) {
        watchers[downloadId]?.cancel()
        watchers[downloadId] = viewModelScope.launch {
            container.downloads.observe(downloadId).collect { item ->
                if (item == null) return@collect
                val list = _downloads.value.filterNot { it.downloadId == downloadId } + item
                _downloads.value = list.sortedBy { it.downloadId }
                if (item.state == DownloadState.FAILED) {
                    showMessage("${item.fileName}: ${item.reason ?: "download failed"}")
                }
            }
        }
    }

    /** Imports a finished download into the library. Only succeeds on a real, valid file. */
    fun importDownload(item: DownloadItem) {
        val path = item.localPath
        if (path == null) {
            showMessage("The downloaded file is no longer available.")
            return
        }
        val file = File(path)
        importPack(file.name) { file.inputStream() }
    }

    fun cancelDownload(downloadId: Long) {
        watchers.remove(downloadId)?.cancel()
        container.downloads.cancel(downloadId)
        _downloads.value = _downloads.value.filterNot { it.downloadId == downloadId }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.preferences.setThemeMode(mode) }
    }

    fun setConfirmDelete(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setConfirmDelete(enabled) }
    }

    fun consumeMessage(id: Long) {
        if (_message.value?.id == id) _message.value = null
    }

    private fun showMessage(text: String, isError: Boolean = true) {
        _message.value = UiMessage(text, isError)
    }
}
