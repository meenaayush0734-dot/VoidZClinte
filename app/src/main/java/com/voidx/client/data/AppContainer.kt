package com.voidx.client.data

import android.content.Context
import com.voidx.client.data.download.DownloadRepository
import com.voidx.client.data.minecraft.MinecraftDetector
import com.voidx.client.data.packs.PackRepository
import com.voidx.client.data.prefs.PreferencesRepository

/** Manual dependency container. Small app, so no DI framework is needed. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferences = PreferencesRepository(appContext)
    val minecraft = MinecraftDetector(appContext)
    val packs = PackRepository(appContext)
    val downloads = DownloadRepository(appContext)
}
