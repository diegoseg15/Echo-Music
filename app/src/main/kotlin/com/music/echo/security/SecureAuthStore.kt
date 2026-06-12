package iad1tya.echo.music.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureAuthStore {
    private const val PREFS_NAME = "secure_auth_store_v1"
    private const val KEY_ALIAS = "echo_music_auth_key_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128

    const val STORED_MARKER = "__SECURE_AUTH_STORED__"

    const val YOUTUBE_COOKIE = "youtube_cookie"
    const val YOUTUBE_VISITOR_DATA = "youtube_visitor_data"
    const val YOUTUBE_DATA_SYNC_ID = "youtube_data_sync_id"

    const val SPOTIFY_SP_DC = "spotify_sp_dc"
    const val SPOTIFY_SP_KEY = "spotify_sp_key"
    const val SPOTIFY_ACCESS_TOKEN = "spotify_access_token"
    const val SPOTIFY_ACCESS_TOKEN_EXPIRES_AT = "spotify_access_token_expires_at"

    fun put(context: Context, key: String, value: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (value.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
            return
        }

        prefs.edit()
            .putString(key, encrypt(value))
            .apply()
    }

    fun putLong(context: Context, key: String, value: Long) {
        put(context, key, value.toString())
    }

    fun get(context: Context, key: String): String? {
        val encrypted = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
            ?: return null

        return runCatching {
            decrypt(encrypted)
        }.getOrNull()
    }

    fun getLong(context: Context, key: String): Long {
        return get(context, key)?.toLongOrNull() ?: 0L
    }

    fun remove(context: Context, key: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }

    fun clearAll(context: Context) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun clearYouTube(context: Context) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(YOUTUBE_COOKIE)
            .remove(YOUTUBE_VISITOR_DATA)
            .remove(YOUTUBE_DATA_SYNC_ID)
            .apply()
    }

    fun clearSpotify(context: Context) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(SPOTIFY_SP_DC)
            .remove(SPOTIFY_SP_KEY)
            .remove(SPOTIFY_ACCESS_TOKEN)
            .remove(SPOTIFY_ACCESS_TOKEN_EXPIRES_AT)
            .apply()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val payload = ByteBuffer
            .allocate(4 + iv.size + cipherText.size)
            .putInt(iv.size)
            .put(iv)
            .put(cipherText)
            .array()

        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedPayload: String): String {
        val payload = Base64.decode(encryptedPayload, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(payload)

        val ivSize = buffer.int
        require(ivSize in 12..16) { "Invalid IV size" }

        val iv = ByteArray(ivSize)
        buffer.get(iv)

        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )

        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}