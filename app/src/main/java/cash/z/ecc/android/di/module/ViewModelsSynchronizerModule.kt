package cash.z.ecc.android.di.module


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.z.ecc.android.di.annotation.SynchronizerScope
import cash.z.ecc.android.di.annotation.ViewModelKey
import cash.z.ecc.android.di.viewmodel.ViewModelFactory
import cash.z.ecc.android.ui.detail.WalletDetailViewModel
import cash.z.ecc.android.ui.home.HomeViewModel
import cash.z.ecc.android.ui.receive.ReceiveViewModel
import cash.z.ecc.android.ui.send.SendViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Named

/**
 * View model related objects, scoped to the synchronizer.
 */
@Module
abstract class ViewModelsSynchronizerModule {
    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(implementation: HomeViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(SendViewModel::class)
    abstract fun bindSendViewModel(implementation: SendViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(WalletDetailViewModel::class)
    abstract fun bindWalletDetailViewModel(implementation: WalletDetailViewModel): ViewModel

    @SynchronizerScope
    @Binds
    @IntoMap
    @ViewModelKey(ReceiveViewModel::class)
    abstract fun bindReceiveViewModel(implementation: ReceiveViewModel): ViewModel


    /**
     * Factory for view models that are not created until the Synchronizer exists. Only VMs that
     * require the Synchronizer should wait until it is created. In other words, these are the VMs
     * that live within the scope of the Synchronizer.
     */
    @SynchronizerScope
    @Named("Synchronizer")
    @Binds
    abstract fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory
}