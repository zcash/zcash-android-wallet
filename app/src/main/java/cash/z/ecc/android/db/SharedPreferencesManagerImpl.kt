package cash.z.ecc.android.db

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject

class SharedPreferencesManagerImpl @Inject constructor(private val context: Context) : SharedPreferencesManager {

    private val notEncryptedPreferencesName = "zcash_not_encrypted_preferences"
    private val encryptedPreferencesName = "zcash_encrypted_preferences"

    private var prefs: SharedPreferences

    init {

        val nonEncryptedPreferences: SharedPreferences = context.getSharedPreferences(notEncryptedPreferencesName, Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            prefs = initializeEncryptedSharedPreferencesManager()
            if (nonEncryptedPreferences.all.isNotEmpty()) { // will be useful for user upgraded OS from L to >= M
                // migrate non encrypted shared preferences
                // to encrypted shared preferences and clear them once finished.
                nonEncryptedPreferences.copyTo(prefs)
                nonEncryptedPreferences.clear()
            }
        } else {
            prefs = nonEncryptedPreferences
        }
    }

    private fun initializeEncryptedSharedPreferencesManager(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            encryptedPreferencesName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun <T : Any?> set(key: String, value: T) {
        prefs.set(key, value)
    }

    override fun setBoolean(key: String, value: Boolean) {
        prefs.set(key, value)
    }

    override fun setBytes(key: String, value: ByteArray) {
        set(key, value.toHex())
    }

    override fun setCharsUtf8(key: String, value: CharArray) {
        set(key, String(value))
    }

    override fun getBytes(key: String): ByteArray? {
        return getString(key, "")?.fromHex()
    }

    override fun getCharsUtf8(key: String): CharArray? {
        return getString(key, "")?.toCharArray()
    }

    override fun getString(key: String, defaultValue: String?): String? {
        val value = getValue(key, defaultValue)
        return value as String?
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        val value = getValue(key, defaultValue)
        return value as Int
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val value = getValue(key, defaultValue)
        return value as Boolean
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        val value = getValue(key, defaultValue)
        return value as Long
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        val value = getValue(key, defaultValue)
        return value as Float
    }

    private fun getValue(key: String, defaultValue: Any?): Any? {
        var value = prefs.all[key]
        value = value ?: defaultValue
        return value
    }

    override fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    override fun remove(key: String) {
        prefs.remove(key)
    }

    override fun clear() {
        prefs.clear()
    }
}
