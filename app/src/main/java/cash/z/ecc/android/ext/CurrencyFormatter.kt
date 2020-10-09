package cash.z.ecc.android.ext

import cash.z.ecc.android.ext.ConversionsUniform.FULL_FORMATTER
import cash.z.ecc.android.ext.ConversionsUniform.LONG_SCALE
import cash.z.ecc.android.ext.ConversionsUniform.SHORT_FORMATTER
import cash.z.ecc.android.sdk.ext.Conversions
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.toZec
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*


/**
 * Do the necessary conversions in one place
 *
 * "1.234" -> to zatoshi
 * (zecStringToZatoshi)
 * String.toZatoshi()
 *
 * 123123 -> to "1.2132"
 * (zatoshiToZecString)
 * Long.toZecString()
 *
 */
object ConversionsUniform {
    val ONE_ZEC_IN_ZATOSHI = BigDecimal(ZcashSdk.ZATOSHI_PER_ZEC, MathContext.DECIMAL128)
    val LONG_SCALE = 8
    val SHORT_SCALE = 8
    val SHORT_FORMATTER = from(SHORT_SCALE)
    val FULL_FORMATTER = from(LONG_SCALE)

    val roundingMode = RoundingMode.HALF_EVEN

    private fun from(maxDecimals: Int = 8, minDecimals: Int = 0) = (NumberFormat.getNumberInstance(Locale("en", "USA")) as DecimalFormat).apply {
//        applyPattern("###.##")
        isParseBigDecimal = true
        roundingMode = roundingMode
        maximumFractionDigits = maxDecimals
        minimumFractionDigits = minDecimals
        minimumIntegerDigits = 1
    }
}

object WalletZecFormmatter {
    fun toZatoshi(zecString: String): Long? {
        return toBigDecimal(zecString)?.multiply(Conversions.ONE_ZEC_IN_ZATOSHI, MathContext.DECIMAL128)?.toLong()
    }
    fun toZecStringShort(zatoshi: Long?): String {
        return SHORT_FORMATTER.format((zatoshi ?: 0).toZec())
    }
    fun toZecStringFull(zatoshi: Long?): String {
        return formatFull((zatoshi ?: 0).toZec())
    }
    fun formatFull(zec: BigDecimal): String {
        return FULL_FORMATTER.format(zec)
    }
    fun toBigDecimal(zecString: String?): BigDecimal? {
        if (zecString.isNullOrEmpty()) return BigDecimal.ZERO
        return try {
            // ignore commas and whitespace
            var sanitizedInput = zecString.filter { it.isDigit() or (it == '.') }
            BigDecimal.ZERO.max(FULL_FORMATTER.parse(sanitizedInput) as BigDecimal)
        } catch (t: Throwable) {
            return null
        }
    }

    // convert a zatoshi value to ZEC as a BigDecimal
    private fun Long?.toZec(): BigDecimal =
        BigDecimal(this ?: 0L, MathContext.DECIMAL128)
            .divide(ConversionsUniform.ONE_ZEC_IN_ZATOSHI)
            .setScale(LONG_SCALE, ConversionsUniform.roundingMode)

}