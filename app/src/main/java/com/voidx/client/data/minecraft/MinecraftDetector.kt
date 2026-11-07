package com.voidx.client.data.minecraft

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.voidx.client.domain.model.MinecraftInstall

/**
 * Detects the official Minecraft Bedrock Android app and launches it.
 * Only what Android exposes is reported: the installed versionName.
 * We cannot switch the installed Minecraft version from here.
 */
class MinecraftDetector(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    fun detect(): MinecraftInstall {
        return try {
            val info = pm.getPackageInfo(MINECRAFT_PACKAGE, 0)
            MinecraftInstall(
                installed = true,
                versionName = info.versionName ?: "unknown",
                versionCode = info.longVersionCode,
            )
        } catch (_: PackageManager.NameNotFoundException) {
            MinecraftInstall(installed = false, versionName = null, versionCode = null)
        }
    }

    /** Returns an intent to open Minecraft, or null if it is not installed or has no launcher. */
    fun launchIntent(): Intent? =
        pm.getLaunchIntentForPackage(MINECRAFT_PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    companion object {
        const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
    }
}
