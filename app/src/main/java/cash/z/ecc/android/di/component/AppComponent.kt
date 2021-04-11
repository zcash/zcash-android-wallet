package cash.z.ecc.android.di.component

import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.di.module.AppModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(zcashWalletApp: ZcashWalletApp)

    // Subcomponents
    fun mainActivitySubcomponent(): MainActivitySubcomponent.Factory
    fun synchronizerSubcomponent(): SynchronizerSubcomponent.Factory
    fun initializerSubcomponent(): InitializerSubcomponent.Factory

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: ZcashWalletApp): AppComponent
    }
}
