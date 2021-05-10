package cash.z.ecc.android.db

interface SharedPreferencesManager {

    fun <T : Any?> set(key: String, value: T)

    fun setBoolean(key: String, value: Boolean)

    fun setBytes(key: String, value: ByteArray)

    fun setCharsUtf8(key: String, value: CharArray)

    fun getBytes(key: String): ByteArray?

    fun getCharsUtf8(key: String): CharArray?

    fun getString(key: String, defaultValue: String?): String?

    fun getInt(key: String, defaultValue: Int): Int

    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun getLong(key: String, defaultValue: Long): Long

    fun getFloat(key: String, defaultValue: Float): Float

    fun contains(key: String): Boolean

    fun remove(key: String)

    fun clear()
}
