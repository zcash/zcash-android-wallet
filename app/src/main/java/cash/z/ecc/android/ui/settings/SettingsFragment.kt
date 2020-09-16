package cash.z.ecc.android.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.di.viewmodel.viewModel
import cash.z.ecc.android.databinding.FragmentSettingsBinding
import cash.z.ecc.android.ext.onClickNavBack
import cash.z.ecc.android.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCurrentServer()
        binding.hitAreaClose.onClickNavBack()
        binding.buttonUpdate.setOnClickListener(View.OnClickListener {
            validateServerHost(view)
        })
        binding.buttonReset.setOnClickListener(View.OnClickListener {
            resetServer()
            showUpdateServerDialog(view)
        })
    }

    private fun getCurrentServer() {
        binding.inputTextLightwalletdServer.setText(viewModel.getServerHost())
        binding.inputTextLightwalletdPort.setText(viewModel.getServerPort().toString())
    }

    private fun resetServer() {
    }

    private fun validateServerHost(view: View) {
        var isError = false
        if (binding.inputTextLightwalletdServer.text.toString().contains("http")) {
            binding.lightwalletdServer.error = "Please remove http:// or https://"
            isError = true
        } else {
            binding.lightwalletdServer.error = null
        }
        if (Integer.valueOf(binding.inputTextLightwalletdPort.text.toString()) > 65535) {
            binding.lightwalletdPort.error = "Please enter port number below 65535"
            isError = true
        } else {
            binding.lightwalletdPort.error = null
        }
        if (!isError) {
            showUpdateServerDialog(view)
        }
    }

    private fun showUpdateServerDialog(view: View) {
        MaterialAlertDialogBuilder(view.context)
            .setTitle("Modify lightwalletd Server?")
            .setMessage("WARNING: Entering an invalid or compromised lighthttpd server might result in misconfiguration or loss of funds.")
            .setCancelable(false)
            .setPositiveButton("Update") { dialog, _ ->
                dialog.dismiss()
                updateServer()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateServer() {
    }
}
