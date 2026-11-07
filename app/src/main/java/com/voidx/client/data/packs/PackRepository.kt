package com.voidx.client.data.packs

import android.content.Context
import com.voidx.client.domain.model.LibraryPack
import com.voidx.client.domain.model.PackDetails
import com.voidx.client.domain.model.PackKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Imports .mcpack / .mcaddon / .mcworld files into app-private storage.
 * A pack is only added to the library after its archive has been read and
 * its manifest parsed. Nothing is written into Minecraft's own data folders.
 */
class PackRepository(private val context: Context) {

    private val root: File = File(context.filesDir, "packs").apply { mkdirs() }
    private val indexFile = File(root, "library.json")

    @Synchronized
    fun list(): List<LibraryPack> {
        if (!indexFile.exists()) return emptyList()
        val arr = runCatching { JSONArray(indexFile.readText()) }.getOrElse { return emptyList() }
        return (0 until arr.length()).map { parseRecord(arr.getJSONObject(it)) }
    }

    /**
     * Copies the file into the library after validating it. Throws [ImportException]
     * with a user-readable message on any failure. Returns the stored record.
     */
    @Synchronized
    fun importFile(fileName: String, open: () -> InputStream?): LibraryPack {
        val kind = PackKind.fromFileName(fileName)
            ?: throw ImportException("Unsupported file. Choose a .mcpack, .mcaddon or .mcworld file.")

        val id = UUID.randomUUID().toString()
        val target = File(root, "$id.${kind.extension}")
        val input = open() ?: throw ImportException("Could not open the selected file.")
        input.use { src -> target.outputStream().use { src.copyTo(it) } }
        if (target.length() == 0L) {
            target.delete()
            throw ImportException("The selected file is empty.")
        }

        val details = try {
            parseArchive(target, kind)
        } catch (e: ImportException) {
            target.delete()
            throw e
        } catch (e: Exception) {
            target.delete()
            throw ImportException("This file is not a valid ${kind.extension} archive.")
        }

        val record = LibraryPack(
            id = id,
            fileName = fileName,
            kind = kind,
            sizeBytes = target.length(),
            importedAt = System.currentTimeMillis(),
            entries = details,
        )
        saveIndex(list() + record)
        return record
    }

    @Synchronized
    fun delete(id: String) {
        val current = list()
        val target = current.firstOrNull { it.id == id } ?: return
        File(root, "${target.id}.${target.kind.extension}").delete()
        saveIndex(current.filterNot { it.id == id })
    }

    /** Stored file for a library entry. The UI shares it through FileProvider. */
    fun fileFor(pack: LibraryPack): File = File(root, "${pack.id}.${pack.kind.extension}")

    // ---- parsing -------------------------------------------------------------

    private fun parseArchive(file: File, kind: PackKind): List<PackDetails> = when (kind) {
        PackKind.MCPACK -> listOf(
            readManifestFromZip(file)
                ?: throw ImportException("No manifest.json found. This is not a valid pack.")
        )
        PackKind.MCADDON -> readAddon(file)
        PackKind.MCWORLD -> listOf(readWorld(file))
    }

    /** Reads manifest.json from the archive root or one folder deep. */
    private fun readManifestFromZip(file: File): PackDetails? {
        file.inputStream().use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val depth = name.count { it == '/' }
                    if (!entry.isDirectory && name.substringAfterLast('/') == "manifest.json" && depth <= 1) {
                        return parseManifest(zis.readBytes().toString(Charsets.UTF_8))
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return null
    }

    /** An .mcaddon is a zip that contains one or more nested .mcpack files. */
    private fun readAddon(file: File): List<PackDetails> {
        val out = mutableListOf<PackDetails>()
        file.inputStream().use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.lowercase().endsWith(".mcpack")) {
                        val nested = ZipInputStream(zis)
                        var inner = nested.nextEntry
                        while (inner != null) {
                            if (!inner.isDirectory && inner.name.substringAfterLast('/') == "manifest.json") {
                                out += parseManifest(nested.readBytes().toString(Charsets.UTF_8))
                                break
                            }
                            inner = nested.nextEntry
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        }
        if (out.isEmpty()) throw ImportException("No packs with a manifest.json were found in this add-on bundle.")
        return out
    }

    /** A .mcworld is a zip containing level.dat and levelname.txt. */
    private fun readWorld(file: File): PackDetails {
        var worldName: String? = null
        var hasLevelDat = false
        file.inputStream().use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val base = entry.name.substringAfterLast('/')
                    if (base == "levelname.txt") worldName = zis.readBytes().toString(Charsets.UTF_8).trim()
                    if (base == "level.dat") hasLevelDat = true
                    entry = zis.nextEntry
                }
            }
        }
        if (!hasLevelDat) throw ImportException("No level.dat found. This is not a valid Minecraft world.")
        return PackDetails(
            name = worldName?.takeIf { it.isNotBlank() } ?: "Imported world",
            description = "Minecraft world",
            uuid = null,
            version = null,
            moduleTypes = emptyList(),
        )
    }

    private fun parseManifest(json: String): PackDetails {
        val root = try { JSONObject(json) } catch (_: Exception) {
            throw ImportException("manifest.json is not valid JSON.")
        }
        val header = root.optJSONObject("header")
            ?: throw ImportException("manifest.json is missing its header.")
        val versionArr = header.optJSONArray("version")
        val version = versionArr?.let { arr ->
            (0 until arr.length()).joinToString(".") { arr.optInt(it).toString() }
        }
        val modules = root.optJSONArray("modules")
        val types = if (modules == null) emptyList() else
            (0 until modules.length()).mapNotNull { modules.optJSONObject(it)?.optString("type")?.takeIf { s -> s.isNotBlank() } }
        return PackDetails(
            name = header.optString("name").ifBlank { "Unnamed pack" },
            description = header.optString("description"),
            uuid = header.optString("uuid").ifBlank { null },
            version = version,
            moduleTypes = types,
        )
    }

    // ---- index persistence ----------------------------------------------------

    private fun saveIndex(records: List<LibraryPack>) {
        val arr = JSONArray()
        records.forEach { arr.put(toJson(it)) }
        val tmp = File(root, "library.json.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(indexFile)) {
            indexFile.delete()
            tmp.renameTo(indexFile)
        }
    }

    private fun toJson(p: LibraryPack): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("fileName", p.fileName)
        put("kind", p.kind.name)
        put("sizeBytes", p.sizeBytes)
        put("importedAt", p.importedAt)
        put("entries", JSONArray().also { arr ->
            p.entries.forEach { d ->
                arr.put(JSONObject().apply {
                    put("name", d.name)
                    put("description", d.description)
                    put("uuid", d.uuid ?: JSONObject.NULL)
                    put("version", d.version ?: JSONObject.NULL)
                    put("moduleTypes", JSONArray(d.moduleTypes))
                })
            }
        })
    }

    private fun parseRecord(o: JSONObject): LibraryPack {
        val entriesArr = o.getJSONArray("entries")
        val entries = (0 until entriesArr.length()).map { i ->
            val e = entriesArr.getJSONObject(i)
            val mods = e.getJSONArray("moduleTypes")
            PackDetails(
                name = e.getString("name"),
                description = e.getString("description"),
                uuid = e.optString("uuid").takeIf { it.isNotBlank() && it != "null" },
                version = e.optString("version").takeIf { it.isNotBlank() && it != "null" },
                moduleTypes = (0 until mods.length()).map { mods.getString(it) },
            )
        }
        return LibraryPack(
            id = o.getString("id"),
            fileName = o.getString("fileName"),
            kind = PackKind.valueOf(o.getString("kind")),
            sizeBytes = o.getLong("sizeBytes"),
            importedAt = o.getLong("importedAt"),
            entries = entries,
        )
    }
}

class ImportException(message: String) : Exception(message)
