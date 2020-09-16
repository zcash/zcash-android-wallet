package cash.z.ecc.android.ui.settings

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.di.module.InitializerModule
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import javax.inject.Inject

class SettingsViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    fun updateServer(host: String, port: Int) {
        // TODO: Update the SecurePrefs here
    }

    fun getServerHost(): String {
        return InitializerModule.defaultHost
    }

    fun getServerPort(): Int {
        return InitializerModule.defaultPort
    }

    override fun onCleared() {
        super.onCleared()
        twig("SettingsViewModel cleared!")
    }
}