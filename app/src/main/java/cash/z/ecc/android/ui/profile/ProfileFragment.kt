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
import cash.z.ecc.android.ext.find
import cash.z.ecc.android.ext.onClick
import cash.z.ecc.android.ext.onClickNavBack
import cash.z.ecc.android.ext.onClickNavTo
import cash.z.ecc.android.feedback.FeedbackFile
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Funnel.UserFeedback
import cash.z.ecc.android.feedback.Report.Tap.AWESOME_OPEN
import cash.z.ecc.android.sdk.ext.Bush
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.util.DebugFileTwig
import kotlinx.coroutines.launch
import java.io.File


class ProfileFragment : BaseFragment<FragmentProfileBinding>() {
    override val screen = Report.Screen.PROFILE

    private val viewModel: ProfileViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentProfileBinding =
        FragmentProfileBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaSettings.onClickNavTo(R.id.action_nav_profile_to_nav_settings)
        binding.hitAreaExit.onClickNavBack() { tapped(PROFILE_CLOSE) }
        binding.buttonBackup.setOnClickListener {
            tapped(PROFILE_BACKUP)
            mainActivity?.let { main ->
                main.authenticate(
                    getString(R.string.biometric_backup_phrase_description),
                    getString(R.string.biometric_backup_phrase_title)
                ) {
                    main.safeNavigate(R.id.action_nav_profile_to_nav_backup)
                }
            }
        }
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

        binding.iconProfile.setOnLongClickListener {
            tapped(AWESOME_OPEN)
            onEnterAwesomeMode()
            true
        }

        if (viewModel.isEasterEggTriggered()) {
            binding.iconProfile.setImageResource(R.drawable.ic_profile_zebra_02)
        }

    }

    private fun onEnterAwesomeMode() {
        (context as? MainActivity)?.safeNavigate(R.id.action_nav_profile_to_nav_awesome)
            ?: throw IllegalStateException("Cannot navigate from this activity. " +
                    "Expected MainActivity but found ${context?.javaClass?.simpleName}")
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
        developerLogFile().let {
            if (it == null) {
                mainActivity?.showSnackbar("Error: No developer log found!")
            } else {
                shareFile(it)
            }
        }
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
        startActivity(Intent.createChooser(intent, getString(R.string.profile_share_log_title)))
    }

    fun shareFile(file: File?) {
        file ?: return
        val uri = getUriForFile(ZcashWalletApp.instance, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, getString(R.string.profile_share_log_title)))
    }

    private fun userLogFile(): File? {
        return mainActivity?.feedbackCoordinator?.findObserver<FeedbackFile>()?.file
    }

    private fun developerLogFile(): File? {
        return Bush.trunk.find<DebugFileTwig>()?.file
    }
}