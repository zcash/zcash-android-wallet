package cash.z.ecc.android.di.module

import cash.z.ecc.android.di.component.InitializerSubcomponent
import cash.z.ecc.android.di.component.SynchronizerSubcomponent
import dagger.Module

@Module(
    includes = [ViewModelsActivityModule::class],
    subcomponents = [SynchronizerSubcomponent::class, InitializerSubcomponent::class]
)
class MainActivityModule {

}
