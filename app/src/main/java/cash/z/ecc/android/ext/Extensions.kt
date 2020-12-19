package cash.z.ecc.android.ext

import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import cash.z.ecc.android.sdk.ext.Bush
import cash.z.ecc.android.sdk.ext.CompositeTwig
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.twig
import java.util.*

fun Boolean.asString(ifTrue: String = "", ifFalse: String = "") = if (this) ifTrue else ifFalse

inline fun <R> tryWithWarning(message: String = "", block: () -> R): R? {
    return try {
        block()
    } catch (error: Throwable) {
        twig("WARNING: $message")
        null
    }
}

inline fun <E : Throwable, R> failWith(specificErrorType: E, block: () -> R): R {
    return try {
        block()
    } catch (error: Throwable) {
        throw specificErrorType
    }
}

inline fun Fragment.locale(): Locale = context?.locale() ?: Locale.getDefault()

inline fun Context.locale(): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0)
    } else {
        //noinspection deprecation
        resources.configuration.locale
    }
}

// TODO: add this to the SDK and if the trunk is a CompositeTwig, search through there before returning null
inline fun <reified T> Twig.find(): T? {
    return if (Bush.trunk::class.java.isAssignableFrom(T::class.java)) Bush.trunk as T
    else null
}