package cash.z.ecc.android.ui.setup

import android.util.Log
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.feedback.Feedback
import cash.z.ecc.android.feedback.Report.MetricType.*
import cash.z.ecc.android.feedback.measure
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.ext.toHex
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import cash.z.ecc.android.ui.setup.WalletSetupViewModel.WalletSetupState.*
import cash.z.ecc.kotlin.mnemonic.Mnemonics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
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
        UNKNOWN, SEED_WITH_BACKUP, SEED_WITHOUT_BACKUP, NO_SEED
    }

    fun checkSeed(): Flow<WalletSetupState> = flow {
        when {
            lockBox.getBoolean(LockBoxKey.HAS_BACKUP) -> emit(SEED_WITH_BACKUP)
            lockBox.getBoolean(LockBoxKey.HAS_SEED) -> emit(SEED_WITHOUT_BACKUP)
            else -> emit(NO_SEED)
        }
    }

    suspend fun buildInitializer() = ZcashWalletApp.component.initializerSubcomponent().create(loadConfig().single()).initializer()

    private fun loadConfig() = flow<Initializer.Builder> {
        emit(
            Initializer.Builder { builder ->
                val vk = lockBox.getCharsUtf8(LockBoxKey.VIEWING_KEY)?.let { String(it) }
                    ?: throw InitializerException.MissingViewingKeyException
                val birthdayHeight = loadBirthdayHeight()
                    ?: throw InitializerException.MissingBirthdayException
                val host = prefs[Const.Pref.SERVER_HOST] ?: Const.Default.Server.HOST
                val port = prefs[Const.Pref.SERVER_PORT] ?: Const.Default.Server.PORT

                builder.import(vk, birthdayHeight, host, port)
            }
        )
    }

    fun loadBirthdayHeight(): Int? {
        val h: Int? = lockBox[LockBoxKey.BIRTHDAY_HEIGHT]
        twigFix("Loaded birthday with key ${LockBoxKey.BIRTHDAY_HEIGHT} and found $h")
        return h
    }

//    /**
//     * Re-open an existing wallet. This is the most common use case, where a user has previously
//     * created or imported their seed and is returning to the wallet. In other words, this is the
//     * non-FTUE case.
//     */
//    fun openWallet(): Initializer {
//        twigFix("Opening existing wallet")
//        return ZcashWalletApp.component.initializerSubcomponent()
//            .create(DefaultBirthdayStore(ZcashWalletApp.instance)).run {
//                initializer().open(birthdayStore().getBirthday())
//            }
//    }

    suspend fun newWallet(): Initializer {
        twigFix("Initializing new wallet")
        createWallet()
        return buildInitializer()
    }

    suspend fun importWallet(seedPhrase: String, birthdayHeight: Int): Initializer {
        twigFix("Importing wallet. Requested birthday: $birthdayHeight")
        val seedPhraseChars = seedPhrase.toCharArray()
        storeSeedPhrase(seedPhraseChars)
        storeSeed(mnemonics.toSeed(seedPhraseChars))
        WalletBirthdayTool.loadNearest(ZcashWalletApp.instance, birthdayHeight).let { birthday ->
            storeBirthday(birthday)
        }
        return buildInitializer()
    }

    private fun twigFix(s: String) {
        Log.e("@TWIG", s)
    }

    /**
     * Take all the steps necessary to create a new wallet and measure how long it takes.
     *
     * @param feedback the object used for measurement.
     */
    private suspend fun createWallet(): ByteArray = withContext(Dispatchers.IO) {
        check(!lockBox.getBoolean(LockBoxKey.HAS_SEED)) {
            "Error! Cannot create a seed when one already exists! This would overwrite the" +
                    " existing seed and could lead to a loss of funds if the user has no backup!"
        }

        feedback.measure(WALLET_CREATED) {
            mnemonics.run {
                feedback.measure(ENTROPY_CREATED) { nextEntropy() }.let { entropy ->
                    feedback.measure(SEED_PHRASE_CREATED) { nextMnemonic(entropy) }
                        .let { seedPhrase ->
                            feedback.measure(SEED_CREATED) { toSeed(seedPhrase) }.let { bip39Seed ->
                                storeSeedPhrase(seedPhrase)
                                storeSeed(bip39Seed)

                                WalletBirthdayTool.loadNearest(ZcashWalletApp.instance).let { birthday ->
                                    storeBirthday(birthday)
                                }
                                bip39Seed
                            }
                        }
                }
            }
        }
    }

    private fun storeBirthday(birthday: WalletBirthdayTool.WalletBirthday) {
        twigFix("Storing birthday ${birthday.height} with and key ${LockBoxKey.BIRTHDAY_HEIGHT}")
        lockBox[LockBoxKey.BIRTHDAY_HEIGHT] = birthday.height
    }

    private fun storeSeed(bip39Seed: ByteArray) {
        twigFix("Storing seed: ${bip39Seed.toHex().length}")
        lockBox.setBytes(LockBoxKey.SEED, bip39Seed)
        lockBox[LockBoxKey.VIEWING_KEY] = DerivationTool.deriveViewingKeys(bip39Seed)[0]
        lockBox[LockBoxKey.HAS_SEED] = true
    }

    private fun storeSeedPhrase(seedPhrase: CharArray) {
        twigFix("Storing seedphrase: ${seedPhrase.size}")
        lockBox[LockBoxKey.SEED_PHRASE] = seedPhrase
        lockBox[LockBoxKey.HAS_SEED_PHRASE] = true
    }

   /**
    * Take all the steps necessary to import a wallet and measure how long it takes.
    *
    * @param feedback the object used for measurement.
    */
   private suspend fun importWallet(
       seedPhrase: CharArray
   ): ByteArray = withContext(Dispatchers.IO) {
       check(!lockBox.getBoolean(LockBoxKey.HAS_SEED)) {
           "Error! Cannot import a seed when one already exists! This would overwrite the" +
                   " existing seed and could lead to a loss of funds if the user has no backup!"
       }

       feedback.measure(WALLET_IMPORTED) {
           mnemonics.run {
               feedback.measure(SEED_IMPORTED) { toSeed(seedPhrase) }.let { bip39Seed ->

                   storeSeedPhrase(seedPhrase)

                   lockBox.setBytes(LockBoxKey.SEED, bip39Seed)
                   lockBox[LockBoxKey.HAS_SEED] = true

                   bip39Seed
               }
           }
       }
   }


    /**
     * Throw an exception if the seed phrase is bad.
     */
    fun validatePhrase(seedPhrase: String) {
        mnemonics.validate(seedPhrase.toCharArray())
    }

    object LockBoxKey {
        const val SEED = "cash.z.ecc.android.SEED"
        const val SEED_PHRASE = "cash.z.ecc.android.SEED_PHRASE"
        const val HAS_SEED = "cash.z.ecc.android.HAS_SEED"
        const val HAS_SEED_PHRASE = "cash.z.ecc.android.HAS_SEED_PHRASE"
        const val HAS_BACKUP = "cash.z.ecc.android.HAS_BACKUP"

        // Config
        const val VIEWING_KEY = "cash.z.ecc.android.VIEWING_KEY"
        const val BIRTHDAY_HEIGHT = "cash.z.ecc.android.BIRTHDAY_HEIGHT"
    }
}