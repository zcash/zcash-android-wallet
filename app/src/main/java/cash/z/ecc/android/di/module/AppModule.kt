package cash.z.ecc.android.di.module

import android.content.ClipboardManager
import android.content.Context
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.db.SharedPreferencesManagerImpl
import cash.z.ecc.android.di.component.MainActivitySubcomponent
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.feedback.Feedback
import cash.z.ecc.android.feedback.FeedbackBugsnag
import cash.z.ecc.android.feedback.FeedbackConsole
import cash.z.ecc.android.feedback.FeedbackCoordinator
import cash.z.ecc.android.feedback.FeedbackFile
import cash.z.ecc.android.feedback.FeedbackMixpanel
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.ext.SilentTwig
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.ui.util.DebugFileTwig
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Named
import javax.inject.Singleton

@Module(subcomponents = [MainActivitySubcomponent::class])
class AppModule {

    @Provides
    @Singleton
    fun provideAppContext(): Context = ZcashWalletApp.instance

    @Provides
    @Singleton
    fun provideClipboard(context: Context) =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    @Named(Const.Name.APP_PREFS)
    fun provideLockbox(appContext: Context): LockBox {
        return LockBox(appContext)
    }

    @Provides
    fun provideSharedPref(appContext: Context): SharedPreferencesManagerImpl {
        return SharedPreferencesManagerImpl(appContext)
    }

    //
    // Feedback
    //

    @Provides
    @Singleton
    fun provideFeedback(): Feedback = Feedback()

    @Provides
    @Singleton
    fun provideFeedbackCoordinator(
        feedback: Feedback,
        @Named(Const.Name.APP_PREFS) prefs: LockBox,
        defaultObservers: Set<@JvmSuppressWildcards FeedbackCoordinator.FeedbackObserver>
    ): FeedbackCoordinator {
        return prefs.getBoolean(Const.Pref.FEEDBACK_ENABLED).let { isEnabled ->
            // observe nothing unless feedback is enabled
            Twig.plant(if (isEnabled) DebugFileTwig() else SilentTwig())
            FeedbackCoordinator(feedback, if (isEnabled) defaultObservers else setOf())
        }
    }

    //
    // Default Feedback Observer Set
    //

    @Provides
    @Singleton
    @IntoSet
    fun provideFeedbackFile(): FeedbackCoordinator.FeedbackObserver = FeedbackFile()

    @Provides
    @Singleton
    @IntoSet
    fun provideFeedbackConsole(): FeedbackCoordinator.FeedbackObserver = FeedbackConsole()

    @Provides
    @Singleton
    @IntoSet
    fun provideFeedbackMixpanel(): FeedbackCoordinator.FeedbackObserver = FeedbackMixpanel()

    @Provides
    @Singleton
    @IntoSet
    fun provideFeedbackBugsnag(): FeedbackCoordinator.FeedbackObserver = FeedbackBugsnag()
}
