package com.example.core.ai.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface ApiKeyStorage {
    fun saveKey(providerKeyAlias: String, rawApiKey: String): Boolean
    fun getKey(providerKeyAlias: String): String?
    fun removeKey(providerKeyAlias: String): Boolean
    fun hasKey(providerKeyAlias: String): Boolean
    fun clearAll(): Boolean
}

class EncryptedKeystoreApiKeyStorage(
    private val context: Context
) : ApiKeyStorage {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val keyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    companion object {
        private const val PREFS_NAME = "jarvis_encrypted_byok_store"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "jarvis_byok_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Could not retrieve master key from Keystore")
    }

    override fun saveKey(providerKeyAlias: String, rawApiKey: String): Boolean {
        if (rawApiKey.isBlank()) {
            return removeKey(providerKeyAlias)
        }
        return try {
            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(rawApiKey.toByteArray(StandardCharsets.UTF_8))

            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            val base64Payload = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString(providerKeyAlias, base64Payload).commit()
        } catch (_: Exception) {
            false
        }
    }

    override fun getKey(providerKeyAlias: String): String? {
        val base64Payload = prefs.getString(providerKeyAlias, null) ?: return null
        return try {
            val combined = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return null

            val iv = ByteArray(GCM_IV_LENGTH)
            val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)

            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // Return null on decryption failure, corrupted IV/ciphertext, or invalidated key
            null
        }
    }

    override fun removeKey(providerKeyAlias: String): Boolean {
        return prefs.edit().remove(providerKeyAlias).commit()
    }

    override fun hasKey(providerKeyAlias: String): Boolean {
        val key = getKey(providerKeyAlias)
        return !key.isNullOrBlank()
    }

    override fun clearAll(): Boolean {
        return prefs.edit().clear().commit()
    }
}
