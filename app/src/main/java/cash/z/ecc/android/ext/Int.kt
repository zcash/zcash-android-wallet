package cash.z.ecc.android.ext

import android.content.res.Resources
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import cash.z.ecc.android.ZcashWalletApp

/**
 * Grab a color out of the application resources, using the default theme
 */
@ColorInt
internal inline fun @receiver:ColorRes Int.toAppColor(): Int {
    return ResourcesCompat.getColor(ZcashWalletApp.instance.resources, this, ZcashWalletApp.instance.theme)
}

/**
 * Grab a string from the application resources
 */
internal inline fun @receiver:StringRes Int.toAppString(lowercase: Boolean = false): String {
    return ZcashWalletApp.instance.getString(this).let {
        if (lowercase) it.toLowerCase() else it
    }
}

/**
 * Grab a formatted string from the application resources
 */
internal inline fun @receiver:StringRes Int.toAppStringFormatted(vararg formatArgs: Any): String {
    return ZcashWalletApp.instance.getString(this, *formatArgs)
}


/**
 * Grab an integer from the application resources
 */
internal inline fun @receiver:IntegerRes Int.toAppInt(): Int {
    return ZcashWalletApp.instance.resources.getInteger(this)}


fun Float.toPx() = this * Resources.getSystem().displayMetrics.density

fun Int.toPx() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

fun Int.toDp() = (this / Resources.getSystem().displayMetrics.density + 0.5f).toInt()