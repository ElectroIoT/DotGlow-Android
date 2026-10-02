package com.electroiot.dotglow

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Optional PIN lock for the app itself. Only a salted SHA-256 of the PIN is stored, in
 * app-private preferences; fingerprint / face unlock can be used instead of typing it.
 */
class AppLock(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    val enabled: Boolean get() = prefs.contains(KEY_HASH)
    var biometric: Boolean
        get() = prefs.getBoolean(KEY_BIO, false)
        set(v) = prefs.edit().putBoolean(KEY_BIO, v).apply()

    val biometricAvailable: Boolean
        get() = BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, hash(pin, salt))
            .apply()
    }

    fun check(pin: String): Boolean {
        val salt = Base64.decode(prefs.getString(KEY_SALT, "") ?: "", Base64.NO_WRAP)
        val want = prefs.getString(KEY_HASH, null) ?: return true
        return MessageDigest.isEqual(hash(pin, salt).toByteArray(), want.toByteArray())
    }

    fun remove() {
        prefs.edit().remove(KEY_HASH).remove(KEY_SALT).remove(KEY_BIO).apply()
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return Base64.encodeToString(md.digest(pin.toByteArray()), Base64.NO_WRAP)
    }

    private companion object {
        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
        const val KEY_BIO = "biometric"
    }
}
