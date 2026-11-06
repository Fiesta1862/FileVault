package com.fiesta.filevault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

object VaultCrypto {
    private const val ALIAS = "filevault_aes_gcm_v1"
    private const val IV_SIZE = 12
    private fun key(): java.security.Key {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.getKey(ALIAS, null)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        return gen.generateKey()
    }
    fun encrypt(source: File, destination: File) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        FileOutputStream(destination).use { out ->
            out.write(cipher.iv)
            CipherOutputStream(out, cipher).use { encrypted ->
                FileInputStream(source).use { it.copyTo(encrypted) }
            }
        }
    }
    fun decrypt(source: File, destination: File) {
        FileInputStream(source).use { input ->
            val iv = ByteArray(IV_SIZE)
            require(input.read(iv) == IV_SIZE) { "Invalid encrypted file" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            CipherInputStream(input, cipher).use { decrypted ->
                FileOutputStream(destination).use { decrypted.copyTo(it) }
            }
        }
    }
}