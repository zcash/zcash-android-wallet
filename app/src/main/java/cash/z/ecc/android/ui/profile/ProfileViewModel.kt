package cash.z.ecc.android.ui.profile

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import javax.inject.Inject

class ProfileViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    lateinit var lockBox: LockBox

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    // TODO: track this in the app and then fetch. For now, just estimate the blocks per second.
    val bps = 40

    suspend fun getShieldedAddress(): String = synchronizer.getAddress()

    suspend fun getTransparentAddress(): String {
        return synchronizer.getTransparentAddress()
    }

    override fun onCleared() {
        super.onCleared()
        twig("ProfileViewModel cleared!")
    }

    suspend fun fetchUtxos(): Int {
        val address = getTransparentAddress()
        val height: Int = lockBox[Const.Backup.BIRTHDAY_HEIGHT] ?: synchronizer.network.saplingActivationHeight
        return synchronizer.refreshUtxos(address, height)
    }

    suspend fun getTransparentBalance(): WalletBalance {
        val address = getTransparentAddress()
        return synchronizer.getTransparentBalance(address)
    }

    fun shieldFunds(): Flow<PendingTransaction> {
        return lockBox.getBytes(Const.Backup.SEED)?.let {
            val sk = DerivationTool.deriveSpendingKeys(it, synchronizer.network)[0]
            val tsk = DerivationTool.deriveTransparentSecretKey(it, synchronizer.network)
            val addr = DerivationTool.deriveTransparentAddressFromPrivateKey(tsk, synchronizer.network)
            synchronizer.shieldFunds(sk, tsk, "${ZcashSdk.DEFAULT_SHIELD_FUNDS_MEMO_PREFIX}\nAll UTXOs from $addr").onEach {
                twig("Received shielding txUpdate: ${it?.toString()}")
//                updateMetrics(it)
//                reportFailures(it)
            }
        } ?: throw IllegalStateException("Seed was expected but it was not found!")
    }

    fun setEasterEggTriggered() {
        lockBox.setBoolean(Const.Pref.EASTER_EGG_TRIGGERED_SHIELDING, true)
    }

    fun isEasterEggTriggered(): Boolean {
        return lockBox.getBoolean(Const.Pref.EASTER_EGG_TRIGGERED_SHIELDING)
    }

    suspend fun cancel(id: Long) {
        synchronizer.cancelSpend(id)
    }

}
