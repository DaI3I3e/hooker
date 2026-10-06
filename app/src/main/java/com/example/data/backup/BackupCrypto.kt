package com.example.data.backup

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

sealed class DecryptResult {
    data class Success(val jsonString: String) : DecryptResult()
    data class LegacyPlainJson(val jsonString: String) : DecryptResult()
    data class Error(val message: String) : DecryptResult()
}

object BackupCrypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "jeebban_backup_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    @Synchronized
    fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }

        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts plain JSON string to a binary package: [12-byte IV] + [AES-GCM Ciphertext]
     */
    fun encryptBackup(plainJson: String): ByteArray {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv // Exactly 12 bytes for GCM
        val cipherText = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        val result = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(cipherText, 0, result, iv.size, cipherText.size)
        return result
    }

    /**
     * Decrypts binary package [12-byte IV] + [AES-GCM Ciphertext].
     * If the input is detected as legacy plain JSON (starting with '{'), returns LegacyPlainJson.
     */
    fun decryptBackup(encryptedBytes: ByteArray): DecryptResult {
        if (encryptedBytes.isEmpty()) {
            return DecryptResult.Error("فایل پشتیبان خالی است.")
        }

        // 1. Detect if this is an unencrypted legacy JSON file (starts with '{' or whitespace + '{')
        val preview = String(encryptedBytes.take(64).toByteArray(), Charsets.UTF_8).trimStart()
        if (preview.startsWith("{")) {
            return try {
                val fullText = String(encryptedBytes, Charsets.UTF_8)
                DecryptResult.LegacyPlainJson(fullText)
            } catch (e: Exception) {
                DecryptResult.Error("فایل متنی نامعتبر است: ${e.message}")
            }
        }

        // 2. Validate encrypted file length
        if (encryptedBytes.size <= GCM_IV_LENGTH) {
            return DecryptResult.Error("فایل پشتیبان ناقص یا آسیب‌دیده است.")
        }

        // 3. Attempt AES-GCM Decryption with Keystore
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                return DecryptResult.Error("فایل بکاپ با این دستگاه قابل بازیابی نیست")
            }

            val secretKey = (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedBytes, 0, GCM_IV_LENGTH)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val plainBytes = cipher.doFinal(
                encryptedBytes,
                GCM_IV_LENGTH,
                encryptedBytes.size - GCM_IV_LENGTH
            )
            val plainText = String(plainBytes, Charsets.UTF_8)
            DecryptResult.Success(plainText)
        } catch (e: Exception) {
            DecryptResult.Error("فایل بکاپ با این دستگاه قابل بازیابی نیست")
        }
    }

    /**
     * Converts raw encrypted bytes into a portable Base64 string (useful for clipboard copy/paste).
     */
    fun bytesToBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Decodes a string (which might be Base64 or plain legacy JSON) and decrypts it.
     */
    fun decryptFromString(rawText: String): DecryptResult {
        val trimmed = rawText.trim()
        if (trimmed.startsWith("{")) {
            return DecryptResult.LegacyPlainJson(trimmed)
        }

        return try {
            val bytes = Base64.decode(trimmed, Base64.DEFAULT)
            decryptBackup(bytes)
        } catch (e: Exception) {
            DecryptResult.Error("فرمت متن پشتیبان نامعتبر است.")
        }
    }
}
