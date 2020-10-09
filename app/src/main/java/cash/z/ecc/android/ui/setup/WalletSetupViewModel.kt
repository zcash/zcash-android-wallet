package cash.z.ecc.android.ui.setup

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.feedback.Feedback
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import cash.z.ecc.android.ui.setup.WalletSetupViewModel.WalletSetupState.*
import cash.z.ecc.kotlin.mnemonic.Mnemonics
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


    private fun loadConfig() = Initializer.Builder { builder ->
        val vk = lockBox.getCharsUtf8(Const.Backup.VIEWING_KEY)?.let { String(it) }
            ?: throw InitializerException.MissingViewingKeyException
        val birthdayHeight = loadBirthdayHeight()
            ?: throw InitializerException.MissingBirthdayException
        val host = prefs[Const.Pref.SERVER_HOST] ?: Const.Default.Server.HOST
        val port = prefs[Const.Pref.SERVER_PORT] ?: Const.Default.Server.PORT

        builder.import(vk, birthdayHeight, host, port)
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
    private suspend fun storeWallet(seedPhraseChars: CharArray, birthday: WalletBirthdayTool.WalletBirthday) {
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