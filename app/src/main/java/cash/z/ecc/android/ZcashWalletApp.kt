package cash.z.ecc.android

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import cash.z.ecc.android.di.component.AppComponent
import cash.z.ecc.android.di.component.DaggerAppComponent
import cash.z.ecc.android.ext.tryWithWarning
import cash.z.ecc.android.feedback.FeedbackCoordinator
import cash.z.ecc.android.sdk.ext.twig
import kotlinx.coroutines.*
import javax.inject.Inject


class ZcashWalletApp : Application(), CameraXConfig.Provider {

    @Inject
    lateinit var coordinator: FeedbackCoordinator

    var creationTime: Long = 0
        private set

    var creationMeasured: Boolean = false

    /**
     * Intentionally private Scope for use with launching Feedback jobs. The feedback object has the
     * longest scope in the app because it needs to be around early in order to measure launch times
     * and stick around late in order to catch crashes. We intentionally don't expose this because
     * application objects can have odd lifecycles, given that there is no clear onDestroy moment in
     * many cases.
     */
    private var feedbackScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(ExceptionReporter(Thread.getDefaultUncaughtExceptionHandler()))
        creationTime = System.currentTimeMillis()
        instance = this
        // Setup handler for uncaught exceptions.
        super.onCreate()

        component = DaggerAppComponent.factory().create(this)
        component.inject(this)
        feedbackScope.launch {
            coordinator.feedback.start()
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
//        MultiDex.install(this)
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    companion object {
        lateinit var instance: ZcashWalletApp
        lateinit var component: AppComponent
    }

    /**
     * @param feedbackCoordinator inject a provider so that if a crash happens before configuration
     * is complete, we can lazily initialize all the feedback objects at this moment so that we
     * don't have to add any time to startup.
     */
    inner class ExceptionReporter(private val ogHandler: Thread.UncaughtExceptionHandler) :
        Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread?, e: Throwable?) {
            twig("Uncaught Exception: $e caused by: ${e?.cause}")
            // Things can get pretty crazy during a fatal exception
            // so be cautious here to avoid freezing the app
            tryWithWarning("Unable to report fatal crash") {
                // note: these are the only reported crashes that set isFatal=true
                coordinator.feedback.report(e, true)
            }
            tryWithWarning("Unable to flush the feedback coordinator") {
                coordinator.flush()
            }

            try {
                // can do this if necessary but first verify that we need it
                runBlocking {
                    coordinator.await()
                    coordinator.feedback.stop()
                }
            } catch (t: Throwable) {
                twig("WARNING: failed to wait for the feedback observers to complete.")
            } finally {
                // it's important that this always runs so we use the finally clause here
                // rather than another tryWithWarning block
                ogHandler.uncaughtException(t, e)
                Thread.sleep(2000L)
            }
        }
    }
}


fun ZcashWalletApp.isEmulator(): Boolean {
    val goldfish = Build.HARDWARE.contains("goldfish");
    val emu = (System.getProperty("ro.kernel.qemu", "")?.length ?: 0) > 0;
    val sdk = Build.MODEL.toLowerCase().contains("sdk")
    return goldfish || emu || sdk;
}
