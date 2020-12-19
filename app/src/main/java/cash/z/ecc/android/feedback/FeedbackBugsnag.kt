package cash.z.ecc.android.feedback

import cash.z.ecc.android.R
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.sdk.ext.twig
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class FeedbackBugsnag : FeedbackCoordinator.FeedbackObserver {

    var isInitialized = false

    override fun initialize(): FeedbackCoordinator.FeedbackObserver = apply {
        ZcashWalletApp.instance.let { appContext ->
            appContext.getString(R.string.bugsnag_api_key)
                .takeUnless { it.isNullOrEmpty() }?.let { apiKey ->
                    twig("starting bugsnag")
                    val config = Configuration(apiKey)
                    Bugsnag.start(appContext, config)
                    isInitialized = true
                } ?: onInitError()
        }
    }

    private fun onInitError() {
        twig("Warning: Failed to load bugsnag because the API key was missing!")
    }

    /**
     * Report non-fatal crashes because fatal ones already get reported by default.
     */
    override fun onAction(action: Feedback.Action) {
        if (!isInitialized) return

        when (action) {
            is Feedback.Crash -> action.exception
            is Feedback.NonFatal -> action.exception
            is Report.Error.NonFatal.Reorg -> ReorgException(
                action.errorHeight,
                action.rewindHeight,
                action.toString()
            )
            is Report.Funnel.Send.Error -> SendException(
                action.errorCode,
                action.errorMessage
            )
            else -> null
        }?.let { exception ->
            // fix: always add details so that we can differentiate a lack of details from a change in the way details should be added
            val details = kotlin.runCatching { action.toMap() }.getOrElse { mapOf("hasDetails" to false) }
            Bugsnag.notify(exception) { event ->
                event.addMetadata("errorDetails", details)
                true
            }
        }
    }

    private class ReorgException(errorHeight: Int, rewindHeight: Int, reorgMesssage: String) :
        Throwable(reorgMesssage)

    private class SendException(errorCode: Int?, errorMessage: String?): RuntimeException(
        "Non-fatal error while sending transaction. code: $errorCode message: $errorMessage"
    )
}