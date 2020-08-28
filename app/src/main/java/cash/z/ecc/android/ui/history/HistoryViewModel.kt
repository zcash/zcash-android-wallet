package cash.z.ecc.android.ui.history

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import javax.inject.Inject

class HistoryViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    val transactions get() = synchronizer.clearedTransactions
    val balance get() = synchronizer.balances

    suspend fun getAddress() = synchronizer.getAddress()

    override fun onCleared() {
        super.onCleared()
        twig("HistoryViewModel cleared!")
    }
}
