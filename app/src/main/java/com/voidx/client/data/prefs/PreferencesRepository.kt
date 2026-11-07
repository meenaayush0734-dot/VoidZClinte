package com.voidx.client.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.voidx.client.domain.model.AppPrefs
import com.voidx.client.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voidx_prefs")

/** Launch statistics. Only updated after Android confirms Minecraft was actually started. */
data class LaunchStats(
    val launchCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val recentLaunches: List<Long> = emptyList(),
)

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

    val stats: Flow<LaunchStats> = store.data.map { p ->
        LaunchStats(
            launchCount = p[KEY_LAUNCH_COUNT] ?: 0,
            lastPlayedAt = p[KEY_LAST_PLAYED],
            recentLaunches = parseLongs(p[KEY_RECENT]),
        )
    }

    val favoritePackIds: Flow<Set<String>> = store.data.map { it[KEY_FAVORITES] ?: emptySet() }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[KEY_THEME] = mode.name }

    suspend fun setConfirmDelete(enabled: Boolean) = store.edit { it[KEY_CONFIRM_DELETE] = enabled }

    suspend fun recordLaunch(now: Long) {
        store.edit { p ->
            p[KEY_LAUNCH_COUNT] = (p[KEY_LAUNCH_COUNT] ?: 0) + 1
            p[KEY_LAST_PLAYED] = now
            p[KEY_RECENT] = (parseLongs(p[KEY_RECENT]) + now).takeLast(10).joinToString(",")
        }
    }

    suspend fun setPackFavorite(id: String, favorite: Boolean) {
        store.edit { p ->
            val current = p[KEY_FAVORITES] ?: emptySet()
            p[KEY_FAVORITES] = if (favorite) current + id else current - id
        }
    }

    private fun parseLongs(raw: String?): List<Long> =
        raw?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        val KEY_CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val KEY_LAUNCH_COUNT = intPreferencesKey("launch_count")
        val KEY_LAST_PLAYED = longPreferencesKey("last_played_at")
        val KEY_RECENT = stringPreferencesKey("recent_launches")
        val KEY_FAVORITES = stringSetPreferencesKey("favorite_pack_ids")
    }
}
