package cash.z.ecc.android.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.db.SharedPreferencesManagerImpl
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.ext.Const.Backup.BIRTHDAY_HEIGHT
import cash.z.ecc.android.ext.Const.Backup.HAS_BACKUP
import cash.z.ecc.android.ext.Const.Backup.HAS_SEED
import cash.z.ecc.android.ext.Const.Backup.HAS_SEED_PHRASE
import cash.z.ecc.android.ext.Const.Backup.PUBLIC_KEY
import cash.z.ecc.android.ext.Const.Backup.SEED
import cash.z.ecc.android.ext.Const.Backup.SEED_PHRASE
import cash.z.ecc.android.ext.Const.Backup.VIEWING_KEY
import cash.z.ecc.android.ext.Const.Pref.EASTER_EGG_TRIGGERED_SHIELDING
import cash.z.ecc.android.ext.Const.Pref.FEEDBACK_ENABLED
import cash.z.ecc.android.ext.Const.Pref.FIRST_USE_VIEW_TX
import cash.z.ecc.android.ext.Const.Pref.IS_LOCK_BOX_MIGRATED_TO_SHARED_PREF
import cash.z.ecc.android.ext.Const.Pref.SERVER_HOST
import cash.z.ecc.android.ext.Const.Pref.SERVER_PORT
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.ext.twig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class MainViewModel @Inject constructor() : ViewModel() {
    private val _loadingMessage = MutableStateFlow<String?>("\u23F3 Loading...")
    private val _syncReady = MutableStateFlow(false)
    val loadingMessage: StateFlow<String?> get() = _loadingMessage
    val isLoading get() = loadingMessage.value != null

    @Inject
    lateinit var sharedPref: SharedPreferencesManagerImpl

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    /**
     * A flow of booleans representing whether or not the synchronizer has been started. This is
     * useful for views that want to monitor the status of the wallet but don't want to access the
     * synchronizer before it is ready to be used. This is also helpful for race conditions where
     * the status of the synchronizer is needed before it is created.
     */
    val syncReady = _syncReady.asStateFlow()

    fun setLoading(isLoading: Boolean = false, message: String? = null) {
        twig("MainViewModel.setLoading: $isLoading")
        _loadingMessage.value = if (!isLoading) {
            null
        } else {
            message ?: "\u23F3 Loading..."
        }
    }

    fun setSyncReady(isReady: Boolean) {
        twig("MainViewModel.setSyncReady: $isReady")
        _syncReady.value = isReady
    }

    /**
     * Check that all LockBox data is migrated to EncryptedSharedPref. If migration didn't happen do that
     * Strategy for migration is to get all data from LockBox and save to sharedPref
     * */
    fun checkLockBoxMigration() = viewModelScope.launch(Dispatchers.IO) {
        if (sharedPref.contains(IS_LOCK_BOX_MIGRATED_TO_SHARED_PREF) && sharedPref.getBoolean(
                IS_LOCK_BOX_MIGRATED_TO_SHARED_PREF,
                false
            )
        ) {
            twig("LockBox migration already done")
        } else {
            val lockBoxKeys = arrayOf(
                FIRST_USE_VIEW_TX,
                SERVER_HOST,
                SERVER_PORT,
                FEEDBACK_ENABLED,
                EASTER_EGG_TRIGGERED_SHIELDING,
                HAS_BACKUP,
                HAS_SEED,
                BIRTHDAY_HEIGHT,
                SEED,
                PUBLIC_KEY,
                SEED_PHRASE,
                VIEWING_KEY,
                HAS_SEED_PHRASE
            )
            Log.d("LockBoxMigration", "Now migration started")
            for (key in lockBoxKeys) {
                when (key) {
                    FIRST_USE_VIEW_TX -> {
                        if (prefs.containsKey(FIRST_USE_VIEW_TX)) {
                            val value = prefs[FIRST_USE_VIEW_TX] ?: false
                            sharedPref.set(FIRST_USE_VIEW_TX, value)
                        }
                    }
                    SERVER_HOST -> {
                        if (prefs.containsKey(SERVER_HOST)) {
                            val value = prefs[SERVER_HOST] ?: Const.Default.Server.HOST
                            sharedPref.set(SERVER_HOST, value)
                        }
                    }
                    SERVER_PORT -> {
                        if (prefs.containsKey(SERVER_PORT)) {
                            val value = prefs[SERVER_PORT] ?: Const.Default.Server.PORT
                            sharedPref.set(SERVER_PORT, value)
                        }
                    }
                    FEEDBACK_ENABLED -> {
                        if (prefs.containsKey(FEEDBACK_ENABLED)) {
                            val value = prefs[FEEDBACK_ENABLED] ?: false
                            sharedPref.set(FEEDBACK_ENABLED, value)
                        }
                    }
                    EASTER_EGG_TRIGGERED_SHIELDING -> {
                        if (prefs.containsKey(EASTER_EGG_TRIGGERED_SHIELDING)) {
                            val value = prefs[EASTER_EGG_TRIGGERED_SHIELDING] ?: false
                            sharedPref.set(EASTER_EGG_TRIGGERED_SHIELDING, value)
                        }
                    }
                    HAS_BACKUP -> {
                        if (prefs.containsKey(HAS_BACKUP)) {
                            val value = prefs[HAS_BACKUP] ?: false
                            sharedPref.set(HAS_BACKUP, value)
                        }
                    }
                    HAS_SEED -> {
                        if (prefs.containsKey(HAS_SEED)) {
                            val value = prefs[HAS_SEED] ?: false
                            sharedPref.set(HAS_SEED, value)
                        }
                    }
                    BIRTHDAY_HEIGHT -> {
                        if (prefs.containsKey(BIRTHDAY_HEIGHT)) {
                            val value = prefs[BIRTHDAY_HEIGHT] ?: -1
                            sharedPref.set(BIRTHDAY_HEIGHT, value)
                        }
                    }
                    SEED -> {
                        if (prefs.containsKey(SEED)) {
                            val value = prefs.getBytes(SEED) ?: "".toByteArray()
                            sharedPref.setBytes(SEED, value)
                        }
                    }
                    PUBLIC_KEY -> {
                        if (prefs.containsKey(PUBLIC_KEY)) {
                            val value = prefs.getCharsUtf8(PUBLIC_KEY) ?: "".toCharArray()
                            sharedPref.setCharsUtf8(PUBLIC_KEY, value)
                        }
                    }
                    SEED_PHRASE -> {
                        if (prefs.containsKey(SEED_PHRASE)) {
                            val value = prefs.getCharsUtf8(SEED_PHRASE) ?: "".toCharArray()
                            sharedPref.setCharsUtf8(SEED_PHRASE, value)
                        }
                    }
                    VIEWING_KEY -> {
                        if (prefs.containsKey(VIEWING_KEY)) {
                            val value = prefs.getCharsUtf8(VIEWING_KEY) ?: "".toCharArray()
                            sharedPref.setCharsUtf8(VIEWING_KEY, value)
                        }
                    }
                    HAS_SEED_PHRASE -> {
                        if (prefs.containsKey(HAS_SEED_PHRASE)) {
                            val value = prefs[HAS_SEED_PHRASE] ?: false
                            sharedPref.set(HAS_SEED_PHRASE, value)
                        }
                    }
                }
            }
            sharedPref.setBoolean(IS_LOCK_BOX_MIGRATED_TO_SHARED_PREF, true)
            twig("LockBox migration done")
            Log.d("LockBoxMigration", "Now migration done")
        }
    }
}
