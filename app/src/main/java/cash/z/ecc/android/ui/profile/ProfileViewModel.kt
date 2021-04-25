package cash.z.ecc.android.ui.profile

import android.widget.Toast
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.WalletBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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

    fun wipe() {
        synchronizer.stop()
        Toast.makeText(ZcashWalletApp.instance, "SUCCESS! Wallet data cleared. Please relaunch to rescan!", Toast.LENGTH_LONG).show()
        Initializer.erase(ZcashWalletApp.instance, ZcashWalletApp.instance.defaultNetwork)
    }

    suspend fun fullRescan() {
        rewindTo(synchronizer.latestBirthdayHeight)
    }

    suspend fun quickRescan() {
        rewindTo(synchronizer.latestHeight - 8064)
    }

    private suspend fun rewindTo(targetHeight: Int) {
        twig("TMP: rewinding to targetHeight $targetHeight")
        synchronizer.rewindToNearestHeight(targetHeight, true)
    }

    fun fullScanDistance() =
        (synchronizer.latestHeight - synchronizer.latestBirthdayHeight).coerceAtLeast(0)

    fun quickScanDistance(): Int {
        val latest = synchronizer.latestHeight
        val oneWeek = 60*60*24/75 * 7 // a week's worth of blocks
        var foo = 0
        runBlocking {
            foo = synchronizer.getNearestRewindHeight(latest - oneWeek)
        }
        return latest - foo
    }

    fun blocksToMinutesString(blocks: Int): String {
        val duration = (blocks / bps.toDouble()).toDuration(DurationUnit.SECONDS)
        return duration.toString(DurationUnit.MINUTES).replace("m", " minutes")
    }
}
