package cash.z.ecc.android.di.component

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import cash.z.ecc.android.di.annotation.ActivityScope
import cash.z.ecc.android.di.module.MainActivityModule
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.ui.MainActivity
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@ActivityScope
@Subcomponent(modules = [MainActivityModule::class])
interface MainActivitySubcomponent {

    fun inject(activity: MainActivity)

    @Named(Const.Name.BEFORE_SYNCHRONIZER) fun viewModelFactory(): ViewModelProvider.Factory

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance activity: FragmentActivity): MainActivitySubcomponent
    }
}
