package com.voidx.client.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.voidx.client.domain.model.AppPrefs
import com.voidx.client.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voidx_prefs")

class PreferencesRepository(private val context: Context) {

    private val store get() = context.dataStore

    val prefs: Flow<AppPrefs> = store.data.map { p ->
        AppPrefs(
            themeMode = runCatching { ThemeMode.valueOf(p[KEY_THEME] ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = p[KEY_DYNAMIC] ?: false,
            confirmDelete = p[KEY_CONFIRM_DELETE] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[KEY_THEME] = mode.name }
    suspend fun setConfirmDelete(enabled: Boolean) = store.edit { it[KEY_CONFIRM_DELETE] = enabled }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        val KEY_CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
    }
}
