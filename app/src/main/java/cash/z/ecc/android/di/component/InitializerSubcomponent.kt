package cash.z.ecc.android.di.component

import cash.z.ecc.android.di.annotation.SynchronizerScope
import cash.z.ecc.android.di.module.InitializerModule
import cash.z.ecc.android.sdk.Initializer
import dagger.BindsInstance
import dagger.Subcomponent

@SynchronizerScope
@Subcomponent(modules = [InitializerModule::class])
interface InitializerSubcomponent {

    fun initializer(): Initializer
    fun config(): Initializer.Config

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance config: Initializer.Config): InitializerSubcomponent
    }
}