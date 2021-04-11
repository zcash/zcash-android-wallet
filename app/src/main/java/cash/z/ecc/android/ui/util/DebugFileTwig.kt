package cash.z.ecc.android.ui.util

import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.spiffy
import okio.Okio
import java.io.File

class DebugFileTwig(fileName: String = "developer_log.txt") : TroubleshootingTwig(formatter = spiffy(6)) {
    val file = File("${ZcashWalletApp.instance.filesDir}/logs", fileName)

    override fun twig(logMessage: String) {
        super.twig(logMessage)
        appendToFile(formatter(logMessage))
    }

    private fun appendToFile(message: String) {
        Okio.buffer(Okio.appendingSink(file)).use {
            it.writeUtf8("$message\n")
        }
    }
}
