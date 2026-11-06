package com.fiesta.filevault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class VaultStore(private val context: Context) {
    private val root = File(context.filesDir, "vault").apply { mkdirs() }
    private val index = File(root, "index.json")
    private val folderIndex = File(root, "folders.json")

    fun folders(): List<String> {
        val saved = runCatching {
            val a = JSONArray(folderIndex.readText())
            (0 until a.length()).map { a.getString(it) }
        }.getOrDefault(emptyList())
        return (listOf("General") + saved + items().map { it.folder }).distinct().sorted()
    }

    fun createFolder(name: String) {
        val clean = name.trim().take(40)
        require(clean.isNotBlank() && clean != "All files") { "Enter a valid folder name." }
        val all = folders().filterNot { it == "General" }.toMutableSet()
        all.add(clean)
        val a = JSONArray()
        all.sorted().forEach { a.put(it) }
        folderIndex.writeText(a.toString())
    }

    fun items(): List<VaultItem> {
        if (!index.exists()) return emptyList()
        return runCatching {
            val a = JSONArray(index.readText())
            (0 until a.length()).mapNotNull { i ->
                val o = a.getJSONObject(i)
                val f = File(root, o.getString("stored"))
                if (!f.exists()) null else VaultItem(
                    o.getString("id"), o.getString("name"), o.optString("mime", "application/octet-stream"),
                    o.optLong("size"), f, o.optString("folder", "General"), o.optLong("added")
                )
            }.sortedByDescending { it.addedAt }
        }.getOrDefault(emptyList())
    }

    fun import(uri: Uri, folder: String = "General"): VaultItem {
        val resolver = context.contentResolver
        val name = resolver.query(uri, null, null, null, null)?.use { c ->
            val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && col >= 0) c.getString(col) else null
        } ?: "file"
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val id = UUID.randomUUID().toString()
        val plain = File(context.cacheDir, "import-" + id)
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot read selected file" }
            plain.outputStream().use { input.copyTo(it) }
        }
        val encrypted = File(root, id + ".fv")
        try { VaultCrypto.encrypt(plain, encrypted) } finally { plain.delete() }
        val item = VaultItem(id, name, mime, encrypted.length(), encrypted, folder)
        save(items() + item)
        return item
    }

    fun export(item: VaultItem, destination: File) = VaultCrypto.decrypt(item.encryptedFile, destination)
    fun delete(item: VaultItem) { item.encryptedFile.delete(); save(items().filterNot { it.id == item.id }) }

    private fun save(items: List<VaultItem>) {
        val a = JSONArray()
        items.forEach { item -> a.put(JSONObject().apply {
            put("id", item.id); put("name", item.name); put("mime", item.mimeType)
            put("size", item.size); put("stored", item.encryptedFile.name)
            put("folder", item.folder); put("added", item.addedAt)
        }) }
        index.writeText(a.toString())
    }
}