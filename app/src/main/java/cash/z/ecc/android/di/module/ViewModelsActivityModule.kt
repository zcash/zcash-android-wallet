package cash.z.ecc.android.di.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.z.ecc.android.di.annotation.ActivityScope
import cash.z.ecc.android.di.annotation.ViewModelKey
import cash.z.ecc.android.di.viewmodel.ViewModelFactory
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.ui.setup.WalletSetupViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Named

/**
 * View model related objects, scoped to the activity that do not depend on the Synchronizer. These
 * are any VMs that must be created before the Synchronizer.
 */
@Module
abstract class ViewModelsActivityModule {

    @ActivityScope
    @Binds
    @IntoMap
    @ViewModelKey(WalletSetupViewModel::class)
    abstract fun bindWalletSetupViewModel(implementation: WalletSetupViewModel): ViewModel

    /**
     * Factory for view models that are created until before the Synchronizer exists. This is a
     * little tricky because we cannot make them all in one place or else they won't be available
     * to both the parent and the child components. If they all live in the child component, which
     * isn't created until the synchronizer exists, then the parent component will not have the
     * view models yet.
     */
    @ActivityScope
    @Named(Const.Name.BEFORE_SYNCHRONIZER)
    @Binds
    abstract fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory
}
