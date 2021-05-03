package cash.z.ecc.android.feedback

import cash.z.ecc.android.ZcashWalletApp
import okio.Okio
import java.io.File
import java.text.SimpleDateFormat

class FeedbackFile(fileName: String = "user_log.txt") :
    FeedbackCoordinator.FeedbackObserver {

    val file = File("${ZcashWalletApp.instance.filesDir}/logs", fileName)
    private val format = SimpleDateFormat("MM-dd HH:mm:ss.SSS")

    override fun initialize(): FeedbackCoordinator.FeedbackObserver = apply {
        file.parentFile?.apply {
            if (!exists()) mkdirs()
        }
    }

    override fun onMetric(metric: Feedback.Metric) {
        appendToFile(metric.toString())
    }

    override fun onAction(action: Feedback.Action) {
        appendToFile(action.toString())
    }

    override fun flush() {
        // TODO: be more sophisticated about how we open/close the file. And then flush it here.
    }

    private fun appendToFile(message: String) {
        Okio.buffer(Okio.appendingSink(file)).use {
            it.writeUtf8("${format.format(System.currentTimeMillis())}|\t$message\n")
        }
    }
}
