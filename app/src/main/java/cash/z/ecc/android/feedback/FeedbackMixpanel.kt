package cash.z.ecc.android.feedback

import cash.z.ecc.android.R
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.ext.toAppString
import com.mixpanel.android.mpmetrics.MixpanelAPI

class FeedbackMixpanel : FeedbackCoordinator.FeedbackObserver {

    private lateinit var mixpanel: MixpanelAPI

    override fun initialize(): FeedbackCoordinator.FeedbackObserver = apply {
        mixpanel =
            MixpanelAPI.getInstance(ZcashWalletApp.instance, R.string.mixpanel_project.toAppString())
    }

    override fun onMetric(metric: Feedback.Metric) {
        track(metric.key, metric.toMap())
    }

    override fun onAction(action: Feedback.Action) {
        track(action.key, action.toMap())
    }

    override fun flush() {
        mixpanel.flush()
    }

    private fun track(eventName: String, properties: Map<String, Any>) {
        mixpanel.trackMap(eventName, properties)
    }
}
