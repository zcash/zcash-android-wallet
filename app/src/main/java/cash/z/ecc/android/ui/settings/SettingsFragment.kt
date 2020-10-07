package cash.z.ecc.android.ui.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import cash.z.ecc.android.R
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.databinding.FragmentSettingsBinding
import cash.z.ecc.android.di.viewmodel.viewModel
import cash.z.ecc.android.ext.*
import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.ui.base.BaseFragment
import kotlinx.coroutines.launch

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(inflater)

    //
    // Lifecycle
    //

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity?.preventBackPress(this)
        viewModel.init()
        binding.apply {
            groupLoading.gone()
            hitAreaExit.onClickNavBack()
            buttonReset.setOnClickListener(::onResetClicked)
            buttonUpdate.setOnClickListener(::onUpdateClicked)
            buttonUpdate.isActivated = true
            buttonReset.isActivated = true
            inputHost.doAfterTextChanged {
                viewModel.pendingHost = it.toString()
            }
            inputPort.doAfterTextChanged {
                viewModel.pendingPortText = it.toString()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.uiModels.collectWith(resumedScope, ::onUiModelUpdated)
    }


    //
    // Event handlers
    //

    private fun onResetClicked(unused: View?) {
        mainActivity?.hideKeyboard()
        context?.showUpdateServerDialog("Restore Defaults") {
            resumedScope.launch {
                binding.groupLoading.visible()
                binding.loadingView.requestFocus()
                viewModel.resetServer()
            }
        }
    }

    private fun onUpdateClicked(unused: View?) {
        mainActivity?.hideKeyboard()
        context?.showUpdateServerDialog {
            resumedScope.launch {
                binding.groupLoading.visible()
                binding.loadingView.requestFocus()
                viewModel.submit()
            }
        }
    }

    private fun onUiModelUpdated(uiModel: SettingsViewModel.UiModel) {
        twig("onUiModelUpdated:::::$uiModel")
        binding.apply {
            if (handleCompletion(uiModel)) return@onUiModelUpdated

            // avoid moving the cursor on instances where the change originated from the UI
            if (inputHost.text.toString() != uiModel.host) inputHost.setText(uiModel.host)
            if (inputPort.text.toString() != uiModel.portText) inputPort.setText(uiModel.portText)

            buttonReset.isEnabled = uiModel.submitEnabled
            buttonUpdate.isEnabled = uiModel.submitEnabled && !uiModel.hasError

            uiModel.hostErrorMessage.let { it ->
                textInputLayoutHost.helperText = it
                    ?: R.string.settings_host_helper_text.toAppString()
                textInputLayoutHost.setHelperTextColor(it.toHelperTextColor())
            }
            uiModel.portErrorMessage.let { it ->
                textInputLayoutPort.helperText = it
                    ?: R.string.settings_port_helper_text.toAppString()
                textInputLayoutPort.setHelperTextColor(it.toHelperTextColor())
            }
        }
    }

    /**
     * Handle the exit conditions and return true if we're done here.
     */
    private fun handleCompletion(uiModel: SettingsViewModel.UiModel): Boolean {
        return if (uiModel.changeError != null) {
            binding.groupLoading.gone()
            onCriticalError(uiModel.changeError)
            true
        } else {
            if (uiModel.complete) {
                binding.groupLoading.gone()
                mainActivity?.safeNavigate(R.id.nav_home)
                Toast.makeText(ZcashWalletApp.instance, "Successfully changed server!", Toast.LENGTH_SHORT).show()
                true
            }
            false
        }
    }

    private fun onCriticalError(error: Throwable) {
        val details = if (error is LightWalletException.ChangeServerException.StatusException) {
            error.status.description
        } else {
            error.javaClass.simpleName
        }
        val message = "An error occured while changing servers. Please verify the info" +
                " and try again.\n\nError: $details"
        twig(message)
        Toast.makeText(ZcashWalletApp.instance, "Failed to change server!", Toast.LENGTH_SHORT).show()
        context?.showUpdateServerCriticalError(message)
    }


    //
    // Utilities
    //

    private fun String?.toHelperTextColor(): ColorStateList {
        val color =  if (this == null) {
            R.color.text_light_dimmed
        } else {
            R.color.zcashRed
        }
       return ColorStateList.valueOf(color.toAppColor())
    }
}

