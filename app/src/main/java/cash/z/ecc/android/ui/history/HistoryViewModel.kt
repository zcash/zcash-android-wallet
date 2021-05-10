package cash.z.ecc.android.ui.history

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.R
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.db.SharedPreferencesManagerImpl
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.ext.WalletZecFormmatter
import cash.z.ecc.android.ext.toAppString
import cash.z.ecc.android.ext.toAppStringFormatted
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.ui.util.MemoUtil
import cash.z.ecc.android.ui.util.toUtf8Memo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class HistoryViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    @Inject
    lateinit var sharedPref: SharedPreferencesManagerImpl

    val selectedTransaction = MutableStateFlow<ConfirmedTransaction?>(null)
    val uiModels = selectedTransaction.map { it.toUiModel() }

    val transactions get() = synchronizer.clearedTransactions
    val balance get() = synchronizer.balances
    val latestHeight get() = synchronizer.latestHeight

    suspend fun getAddress() = synchronizer.getAddress()

    override fun onCleared() {
        super.onCleared()
        twig("HistoryViewModel cleared!")
    }

    //
    // History Item UiModel
    //

    data class UiModel(
        var topLabel: String = "",
        var topValue: String = "",
        var bottomLabel: String = "",
        var bottomValue: String = "",
        var minedHeight: String = "",
        var timestamp: String = "",
        var iconRotation: Float = -1f,

        var fee: String? = null,
        var source: String? = null,
        var memo: String? = null,
        var address: String? = null,
        var isInbound: Boolean? = null,
        var isMined: Boolean = false,
        var confirmation: String? = null,
        var txId: String? = null
    )

    private suspend fun ConfirmedTransaction?.toUiModel(latestHeight: Int? = null): UiModel = UiModel().apply {
        this@toUiModel.let { tx ->
            txId = toTxId(tx?.rawTransactionId)
            isInbound = when {
                !(tx?.toAddress.isNullOrEmpty()) -> false
                tx != null && tx.toAddress.isNullOrEmpty() && tx.value > 0L && tx.minedHeight > 0 -> true
                else -> null
            }
            isMined = tx?.minedHeight != null && tx.minedHeight > synchronizer.network.saplingActivationHeight
            topValue = if (tx == null) "" else "\$${WalletZecFormmatter.toZecStringFull(tx.value)}"
            minedHeight = (tx?.minedHeight ?: 0).toString()
            val flags =
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            timestamp = if (tx == null) getString(R.string.transaction_timestamp_unavailable) else DateUtils.getRelativeDateTimeString(
                ZcashWalletApp.instance,
                tx.blockTimeInSeconds * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                flags
            ).toString()

            // memo logic
            val txMemo = tx?.memo.toUtf8Memo()
            if (!txMemo.isEmpty()) {
                memo = txMemo
            }

            // confirmation logic
            // TODO: clean all of this up and remove/improve reliance on `isSufficientlyOld` function. Also, add a constant for the number of confirmations we expect.
            tx?.let {
                val isMined = it.blockTimeInSeconds != 0L
                if (isMined) {
                    val hasLatestHeight = latestHeight != null && latestHeight > synchronizer.network.saplingActivationHeight
                    if (it.minedHeight > 0 && hasLatestHeight) {
                        val confirmations = latestHeight!! - it.minedHeight + 1
                        confirmation = if (confirmations >= 10) getString(R.string.transaction_status_confirmed) else "$confirmations ${getString(
                            R.string.transaction_status_confirming
                        )}"
                    } else {
                        if (!hasLatestHeight && isSufficientlyOld(tx)) {
                            twig("Warning: could not load latestheight from server to determine confirmations but this transaction is mined and old enough to be considered confirmed")
                            confirmation = getString(R.string.transaction_status_confirmed)
                        } else {
                            twig("Warning: could not determine confirmation text value so it will be left null!")
                            confirmation = getString(R.string.transaction_confirmation_count_unavailable)
                        }
                    }
                } else {
                    confirmation = getString(R.string.transaction_status_pending)
                }
            }

            when (isInbound) {
                true -> {
                    topLabel = getString(R.string.transaction_story_inbound)
                    bottomLabel = getString(R.string.transaction_story_inbound_total)
                    bottomValue = "\$${WalletZecFormmatter.toZecStringFull(tx?.value)}"
                    iconRotation = 315f
                    source = getString(R.string.transaction_story_to_shielded)
                    address = MemoUtil.findAddressInMemo(tx, (synchronizer as SdkSynchronizer)::isValidAddress)
                }
                false -> {
                    topLabel = getString(R.string.transaction_story_outbound)
                    bottomLabel = getString(R.string.transaction_story_outbound_total)
                    bottomValue = "\$${WalletZecFormmatter.toZecStringFull(tx?.value?.plus(ZcashSdk.MINERS_FEE_ZATOSHI))}"
                    iconRotation = 135f
                    fee = "+ 0.00001 network fee"
                    source = getString(R.string.transaction_story_from_shielded)
                    address = tx?.toAddress
                }
                null -> {
                    twig("Error: transaction appears to be invalid.")
                }
            }
        }
    }

    private fun getString(@StringRes id: Int) = id.toAppString()
    private fun getString(@StringRes id: Int, vararg args: Any) = id.toAppStringFormatted(args)

    private fun toTxId(tx: ByteArray?): String? {
        if (tx == null) return null
        val sb = StringBuilder(tx.size * 2)
        for (i in (tx.size - 1) downTo 0) {
            sb.append(String.format("%02x", tx[i]))
        }
        return sb.toString()
    }

    // TODO: determine this in a more generic and technically correct way. For now, this is good enough.
    //       the goal is just to improve the edge cases where the latest height isn't known but other
    //       information suggests that the TX is confirmed. We can improve this, later.
    private fun isSufficientlyOld(tx: ConfirmedTransaction): Boolean {
        val threshold = 75 * 1000 * 25 // approx 25 blocks
        val delta = System.currentTimeMillis() / 1000L - tx.blockTimeInSeconds
        return tx.minedHeight > synchronizer.network.saplingActivationHeight &&
            delta < threshold
    }
}
