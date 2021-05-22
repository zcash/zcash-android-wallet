package cash.z.ecc.android.db

import android.content.SharedPreferences
import cash.z.ecc.android.sdk.ext.twig
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.Arrays

fun SharedPreferences.copyTo(dest: SharedPreferences) {
    for (entry in all.entries) {
        val key = entry.key
        val value: Any? = entry.value
        dest.set(key, value)
    }
}

inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
    val editor = this.edit()
    operation(editor)
    editor.apply()
}

fun SharedPreferences.set(key: String, value: Any?) {
    when (value) {
        is String? -> edit { it.putString(key, value) }
        is Int -> edit { it.putInt(key, value.toInt()) }
        is Boolean -> edit { it.putBoolean(key, value) }
        is Float -> edit { it.putFloat(key, value.toFloat()) }
        is Long -> edit { it.putLong(key, value.toLong()) }
        else -> {
            twig("Unsupported Type: $value")
        }
    }
}

fun SharedPreferences.clear() {
    edit { it.clear() }
}

fun SharedPreferences.remove(key: String) {
    edit { it.remove(key) }
}

fun ByteArray.toHex(): String {
    val sb = StringBuilder(size * 2)
    for (b in this)
        sb.append(String.format("%02x", b))
    return sb.toString()
}

fun String.fromHex(): ByteArray {
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

fun CharArray.toBytes(): ByteArray {
    val byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(this))
    return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit())
}

fun ByteArray.fromBytes(): CharArray {
    val charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(this))
    return Arrays.copyOf(charBuffer.array(), charBuffer.limit())
}
