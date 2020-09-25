package cash.z.ecc.android.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.FileProvider.getUriForFile
import cash.z.ecc.android.BuildConfig
import cash.z.ecc.android.R
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.databinding.FragmentProfileBinding
import cash.z.ecc.android.di.viewmodel.viewModel
import cash.z.ecc.android.ext.onClick
import cash.z.ecc.android.ext.onClickNavBack
import cash.z.ecc.android.ext.onClickNavTo
import cash.z.ecc.android.feedback.FeedbackFile
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Funnel.UserFeedback
import cash.z.ecc.android.feedback.Report.Tap.*
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import kotlinx.coroutines.launch
import okio.Okio
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException


class ProfileFragment : BaseFragment<FragmentProfileBinding>() {
    override val screen = Report.Screen.PROFILE

    private val viewModel: ProfileViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentProfileBinding =
        FragmentProfileBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaSettings.onClickNavTo(R.id.action_nav_profile_to_nav_settings)
        binding.hitAreaExit.onClickNavBack() { tapped(PROFILE_CLOSE) }
        binding.buttonBackup.onClickNavTo(R.id.action_nav_profile_to_nav_backup) { tapped(PROFILE_BACKUP) }
        binding.buttonFeedback.onClickNavTo(R.id.action_nav_profile_to_nav_feedback) {
            tapped(PROFILE_SEND_FEEDBACK)
            mainActivity?.reportFunnel(UserFeedback.Started)
            Unit
        }
        binding.textVersion.text = BuildConfig.VERSION_NAME
        onClick(binding.buttonLogs) {
            tapped(PROFILE_VIEW_USER_LOGS)
            onViewLogs()
        }
        binding.buttonLogs.setOnLongClickListener {
            tapped(PROFILE_VIEW_DEV_LOGS)
            onViewDevLogs()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        resumedScope.launch {
            binding.textAddress.text = viewModel.getAddress().toAbbreviatedAddress(12, 12)
        }
    }

    private fun onViewLogs() {
        shareFile(userLogFile())
    }

    private fun onViewDevLogs() {
        shareFile(writeLogcat())
    }

    private fun shareFiles(vararg files: File?) {
        val uris = arrayListOf<Uri>().apply {
            files.filterNotNull().mapNotNull {
                getUriForFile(ZcashWalletApp.instance, "${BuildConfig.APPLICATION_ID}.fileprovider", it)
            }.forEach {
                add(it)
            }
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = "text/*"
        }
        startActivity(Intent.createChooser(intent, "Share Log Files"))
    }

    fun shareFile(file: File?) {
        file ?: return
        val uri = getUriForFile(ZcashWalletApp.instance, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Share Log File"))
    }

    private fun userLogFile(): File? {
        return mainActivity?.feedbackCoordinator?.findObserver<FeedbackFile>()?.file
    }

    private fun loadLogFileAsText(): String? {
        val feedbackFile: File = userLogFile() ?: return null
        Okio.buffer(Okio.source(feedbackFile)).use {
            return it.readUtf8()
        }
    }

    private fun writeLogcat(): File? {
        try {
            // Note: the /logs directory has been configured as a file provider under @xml/file_paths which allows the temporary sharing of this file
            val outputFile = File("${ZcashWalletApp.instance.filesDir}/logs", "developer_log.txt").also { it.parentFile.mkdirs() }
            if (!outputFile.parentFile.isDirectory) {
                // addresses security finding in issue #121
                throw IllegalArgumentException("Invalid path: ${outputFile.parentFile}. Verify" +
                        " that the default files directory is not being manipulated.")
            }
            val cmd = arrayOf("/bin/sh", "-c", "logcat -v time -d | grep '@TWIG' > '${outputFile.absolutePath}'")
            Runtime.getRuntime().exec(cmd)
            return outputFile
        } catch (e: IOException) {
            e.printStackTrace()
            twig("Failed to create log")
        }
        return null
    }
}