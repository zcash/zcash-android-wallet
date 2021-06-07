package cash.z.ecc.android.ui.home

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.ext.pending
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isMined
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.type.WalletBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BalanceDetailViewModel @Inject constructor() : ViewModel() {
    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    lateinit var lockBox: LockBox

    var latestBalance: BalanceModel? = null

    val balances: Flow<BalanceModel>
        get() = synchronizer.balances.map { balance ->
            val taddr = synchronizer.getTransparentAddress()
            val transparentBalance = synchronizer.getTransparentBalance(taddr)
            BalanceModel(
                balance,
                transparentBalance
            ).also {
                latestBalance = it
            }
        }

    val statuses: Flow<StatusModel>
        get() = combineTransform(
            balances,
            synchronizer.pendingTransactions,
            synchronizer.networkHeight
        ) { balances, pending, height ->
            emit(StatusModel(balances, pending, height))
        }

    data class BalanceModel(
        val shieldedBalance: WalletBalance = WalletBalance(),
        val transparentBalance: WalletBalance = WalletBalance(),
    ) {
        val balanceShielded: String = shieldedBalance.availableZatoshi.toDisplay()
        val balanceTransparent: String = transparentBalance.availableZatoshi.toDisplay()
        val balanceTotal: String = (shieldedBalance.availableZatoshi + transparentBalance.availableZatoshi).toDisplay()
        val canAutoShield: Boolean = transparentBalance.availableZatoshi > 0L

        val maxLength = maxOf(balanceShielded.length, balanceTransparent.length, balanceTotal.length)
        val paddedShielded = pad(balanceShielded)
        val paddedTransparent = pad(balanceTransparent)
        val paddedTotal = pad(balanceTotal)

        private fun Long.toDisplay(): String {
            return convertZatoshiToZecString(8, 8)
        }

        private fun pad(balance: String): String {
            var diffLength = maxLength - balance.length
            return buildString {
                repeat(diffLength) {
                    append(' ')
                }
                append(balance)
            }
        }

        fun hasData(): Boolean {
            val default = WalletBalance()
            return shieldedBalance.availableZatoshi != default.availableZatoshi ||
                shieldedBalance.totalZatoshi != default.totalZatoshi ||
                shieldedBalance.availableZatoshi != default.availableZatoshi ||
                shieldedBalance.totalZatoshi != default.totalZatoshi
        }
    }

    data class StatusModel(
        val balances: BalanceModel,
        val pending: List<PendingTransaction>,
        val latestHeight: Int,
    ) {
        val pendingUnconfirmed = pending.filter { it.isSubmitSuccess() && it.isMined() && !it.isConfirmed(latestHeight) }
        val pendingUnmined = pending.filter { it.isSubmitSuccess() && !it.isMined() }
        val pendingShieldedBalance = balances.shieldedBalance.pending
        val pendingTransparentBalance = balances.transparentBalance.pending
        val hasUnconfirmed = pendingUnconfirmed.isNotEmpty()
        val hasUnmined = pendingUnmined.isNotEmpty()
        val hasPendingShieldedBalance = pendingShieldedBalance > 0L
        val hasPendingTransparentBalance = pendingTransparentBalance > 0L

        private fun PendingTransaction.isConfirmed(networkBlockHeight: Int): Boolean {
            return isMined() && (networkBlockHeight - minedHeight) > 10
        }

        fun remainingConfirmations(confirmationsRequired: Int = 10) =
            pendingUnconfirmed
                .map { confirmationsRequired - (latestHeight - it.minedHeight) }
                .filter { it > 0 }
                .sortedDescending()
    }
}
