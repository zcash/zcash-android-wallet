package cash.z.ecc.android.di.module

import android.content.Context
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.Initializer
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
class InitializerModule {

    companion object {
        const val defaultHost = "lightwalletd.electriccoin.co"
        const val defaultPort = 9067
    }

    // TODO: Read SecurePrefs
//    private val host: String = sharedPreferences.getString(Const.Pref.SERVER_NAME, defaultHost) ?: defaultHost
//    private val port: Int = sharedPreferences.getInt(Const.Pref.SERVER_PORT, defaultPort)

    @Provides
    @Reusable
    fun provideInitializer(appContext: Context) = Initializer(appContext, defaultHost, defaultPort)
}
