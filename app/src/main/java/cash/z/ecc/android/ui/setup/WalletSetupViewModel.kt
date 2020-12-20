package cash.z.ecc.android.ui.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.ext.failWith
import cash.z.ecc.android.feedback.Feedback
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import cash.z.ecc.android.ui.setup.WalletSetupViewModel.WalletSetupState.*
import cash.z.ecc.kotlin.mnemonic.Mnemonics
import com.bugsnag.android.Bugsnag
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class WalletSetupViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var mnemonics: Mnemonics

    @Inject
    lateinit var lockBox: LockBox

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    @Inject
    lateinit var feedback: Feedback

    enum class WalletSetupState {
        SEED_WITH_BACKUP, SEED_WITHOUT_BACKUP, NO_SEED
    }

    fun checkSeed(): Flow<WalletSetupState> = flow {
        when {
            lockBox.getBoolean(Const.Backup.HAS_BACKUP) -> emit(SEED_WITH_BACKUP)
            lockBox.getBoolean(Const.Backup.HAS_SEED) -> emit(SEED_WITHOUT_BACKUP)
            else -> emit(NO_SEED)
        }
    }

    /**
     * Throw an exception if the seed phrase is bad.
     */
    fun validatePhrase(seedPhrase: String) {
        mnemonics.validate(seedPhrase.toCharArray())
    }

    fun loadBirthdayHeight(): Int? {
        val h: Int? = lockBox[Const.Backup.BIRTHDAY_HEIGHT]
        twig("Loaded birthday with key ${Const.Backup.BIRTHDAY_HEIGHT} and found $h")
        return h
    }

    suspend fun newWallet(): Initializer {
        twig("Initializing new wallet")
        with(mnemonics) {
            storeWallet(nextMnemonic(nextEntropy()), loadNearestBirthday())
        }
        return openStoredWallet()
    }

    suspend fun importWallet(seedPhrase: String, birthdayHeight: Int): Initializer {
        twig("Importing wallet. Requested birthday: $birthdayHeight")
        storeWallet(seedPhrase.toCharArray(), loadNearestBirthday(birthdayHeight))
        return openStoredWallet()
    }

    suspend fun openStoredWallet(): Initializer {
        val config = loadConfig()
        return ZcashWalletApp.component.initializerSubcomponent().create(config).initializer()
    }

    /**
     * Build a config object by loading in the viewingKey, birthday and server info which is already
     * known by this point.
     */
    private suspend fun loadConfig(): Initializer.Config {
        twig("Loading config variables")
        val vk = lockBox.getCharsUtf8(Const.Backup.VIEWING_KEY)?.let { String(it) }
            ?: onMissingViewingKey()
        val birthdayHeight = loadBirthdayHeight() ?: onMissingBirthday()
        val host = prefs[Const.Pref.SERVER_HOST] ?: Const.Default.Server.HOST
        val port = prefs[Const.Pref.SERVER_PORT] ?: Const.Default.Server.PORT

        twig("Done loading config variables")
        return Initializer.Config { it.importWallet(vk, birthdayHeight, host, port) }
    }

    // TODO: delete this function in the next release
    private suspend fun onMissingViewingKey(): String {
        twig("Recover VK: Viewing key was missing")
        // add some temporary logic to help us troubleshoot this problem.
        ZcashWalletApp.instance.getSharedPreferences("SecurePreferences", Context.MODE_PRIVATE)
            .all.map { it.key }.joinToString().let { keyNames ->
                Const.Backup.VIEWING_KEY.let { missingKey ->
                    // is there a typo or change in how the value is labelled?
                    Bugsnag.leaveBreadcrumb("$missingKey not found in keySet: $keyNames")
                    // for troubleshooting purposes, let's see if we CAN derive the vk from the seed in these situations
                    var recoveryViewingKey: String? = null
                    var ableToLoadSeed = false
                    try {
                        val seed = lockBox.getBytes(Const.Backup.SEED)!!
                        ableToLoadSeed = true
                        twig("Recover VK: Seed found")
                        recoveryViewingKey = DerivationTool.deriveViewingKeys(seed)[0]
                        twig("Recover VK: successfully derived VK from seed")
                    } catch (t: Throwable) {
                        Bugsnag.leaveBreadcrumb("Failed while trying to recover VK due to: $t")
                    }

                    // this will happen during rare upgrade scenarios when the user migrates from a seed-only wallet to this vk-based version
                    if (recoveryViewingKey != null) {
                        storeViewingKey(recoveryViewingKey)
                        return recoveryViewingKey
                    } else {
                        feedback.report(
                            Report.Issue.MissingViewkey(
                                ableToLoadSeed,
                                missingKey,
                                keyNames,
                                lockBox.getCharsUtf8(Const.Backup.VIEWING_KEY) != null
                            )
                        )
                    }
                    throw InitializerException.MissingViewingKeyException
                }
            }
    }

    private fun onMissingBirthday(): Int = failWith(InitializerException.MissingBirthdayException) {
        twig("Recover Birthday: falling back to sapling birthday")
        loadNearestBirthday(ZcashSdk.SAPLING_ACTIVATION_HEIGHT).height
    }

    private fun loadNearestBirthday(birthdayHeight: Int? = null) =
        WalletBirthdayTool.loadNearest(ZcashWalletApp.instance, birthdayHeight)


    //
    // Storage Helpers
    //

    /**
     * Entry point for all storage. Takes a seed phrase and stores all the parts so that we can
     * selectively use them, the next time the app is opened. Although we store everything, we
     * primarily only work with the viewing key and spending key. The seed is only accessed when
     * presenting backup information to the user.
     */
    private suspend fun storeWallet(
        seedPhraseChars: CharArray,
        birthday: WalletBirthdayTool.WalletBirthday
    ) {
        check(!lockBox.getBoolean(Const.Backup.HAS_SEED)) {
            "Error! Cannot store a seed when one already exists! This would overwrite the" +
                    " existing seed and could lead to a loss of funds if the user has no backup!"
        }

        storeBirthday(birthday)

        mnemonics.toSeed(seedPhraseChars).let { bip39Seed ->
            DerivationTool.deriveViewingKeys(bip39Seed)[0].let { viewingKey ->
                storeSeedPhrase(seedPhraseChars)
                storeSeed(bip39Seed)
                storeViewingKey(viewingKey)
            }
        }
    }

    private suspend fun storeBirthday(birthday: WalletBirthdayTool.WalletBirthday) = withContext(IO) {
        twig("Storing birthday ${birthday.height} with and key ${Const.Backup.BIRTHDAY_HEIGHT}")
        lockBox[Const.Backup.BIRTHDAY_HEIGHT] = birthday.height
    }

    private suspend fun storeSeedPhrase(seedPhrase: CharArray) = withContext(IO) {
        twig("Storing seedphrase: ${seedPhrase.size}")
        lockBox[Const.Backup.SEED_PHRASE] = seedPhrase
        lockBox[Const.Backup.HAS_SEED_PHRASE] = true
    }

    private suspend fun storeSeed(bip39Seed: ByteArray) = withContext(IO) {
        twig("Storing seed: ${bip39Seed.size}")
        lockBox.setBytes(Const.Backup.SEED, bip39Seed)
        lockBox[Const.Backup.HAS_SEED] = true
    }

    private suspend fun storeViewingKey(vk: String) = withContext(IO) {
        twig("storeViewingKey vk: ${vk.length}")
        lockBox[Const.Backup.VIEWING_KEY] = vk
    }

}