package com.fiesta.filevault
import java.io.File
data class VaultItem(val id: String, val name: String, val mimeType: String, val size: Long, val encryptedFile: File, val addedAt: Long = System.currentTimeMillis())