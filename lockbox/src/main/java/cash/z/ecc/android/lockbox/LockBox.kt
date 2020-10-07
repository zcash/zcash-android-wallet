package cash.z.ecc.android.lockbox

import android.content.Context
import cash.z.android.plugin.LockBoxPlugin
import de.adorsys.android.securestoragelibrary.SecurePreferences
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

class LockBox @Inject constructor(private val appContext: Context) : LockBoxPlugin {

    private val maxLength: Int = 200

    override fun setBoolean(key: String, value: Boolean) {
        SecurePreferences.setValue(appContext, key, value)
    }

    override fun getBoolean(key: String): Boolean {
        return SecurePreferences.getBooleanValue(appContext, key, false)
    }

    override fun setBytes(key: String, value: ByteArray) {
        // using hex here because this library doesn't really work well for byte arrays
        // but hopefully we can code to arrays and then change the underlying library, later
        setValue(key, value.toHex())
    }

    override fun getBytes(key: String): ByteArray? {
        return getValue(key)?.fromHex()
    }

    override fun setCharsUtf8(key: String, value: CharArray) {
        // Using string here because this library doesn't work well for char arrays
        // but hopefully we can code to arrays and then change the underlying library, later
       setValue(key, String(value))
    }

    override fun getCharsUtf8(key: String): CharArray? {
        return getValue(key)?.toCharArray()
    }

    inline operator fun <reified T> set(key: String, value: T) {
        when (T::class.java) {
            Boolean::class.java, Double::class.java, Float::class.java, Integer::class.java, Long::class.java, String::class.java -> setValue(key, value.toString())
            else -> throw UnsupportedOperationException("Lockbox does not yet support ${T::class.java.simpleName} objects but it can be added")
        }
    }

    inline operator fun <reified T> get(key: String): T? = when (T::class.java) {
        Boolean::class.java -> (getCharsUtf8(key)?.let { String(it).toIntOrNull() }) as T
        Double::class.java -> (getCharsUtf8(key)?.let { String(it).toDoubleOrNull() }) as T
        Float::class.java -> (getCharsUtf8(key)?.let { String(it).toFloatOrNull() }) as T
        Integer::class.java -> (getCharsUtf8(key)?.let { String(it).toIntOrNull() }) as T
        Long::class.java -> (getCharsUtf8(key)?.let { String(it).toLongOrNull() }) as T
        String::class.java -> (getCharsUtf8(key)?.let { String(it) }) as T
        else -> throw UnsupportedOperationException("Lockbox does not yet support  ${T::class.java.simpleName} objects but it can be added")
    }

    /**
     * Splits a string value into smaller pieces so as not to exceed the limit on the length of
     * String that can be stored.
     */
    fun setValue(key: String, value: String) {
        if (value.length > maxLength) {
            SecurePreferences.setValue(appContext, key, value.chunked(maxLength).toSet())
        } else {
            SecurePreferences.setValue(appContext, key, value)
        }
    }

    /**
     * Returns a string value from storage by first fetching the key, directly. If that is missing,
     * it checks for a chunked version of the key. If that exists, it will be merged and returned.
     * If not, then null will be returned.
     *
     * @return the key if found and null otherwise.
     */
    private fun getValue(key: String): String? {
        return SecurePreferences.getStringValue(appContext, key, null)
            ?: SecurePreferences.getStringSetValue(appContext, key, setOf()).let { result ->
                if (result.size == 0) null else result.joinToString("")
            }
    }


    //
    // Extensions (TODO: find library that works better with arrays of bytes and chars)
    //

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this)
            sb.append(String.format("%02x", b))
        return sb.toString()
    }

    private fun String.fromHex(): ByteArray {
        val len = length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] =
                ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun CharArray.toBytes(): ByteArray {
        val byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(this))
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit());
    }

    private fun ByteArray.fromBytes(): CharArray {
        val charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(this))
        return Arrays.copyOf(charBuffer.array(), charBuffer.limit())
    }
}


