package cash.z.ecc.android.ui.send

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentSendFinalBinding
import cash.z.ecc.android.di.viewmodel.activityViewModel
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Tap.SEND_FINAL_CLOSE
import cash.z.ecc.android.feedback.Report.Tap.SEND_FINAL_EXIT
import cash.z.ecc.android.sdk.db.entity.*
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.ui.base.BaseFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SendFinalFragment : BaseFragment<FragmentSendFinalBinding>() {
    override val screen = Report.Screen.SEND_FINAL

    val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendFinalBinding =
        FragmentSendFinalBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonPrimary.setOnClickListener {
            onReturnToSend()
        }
        binding.buttonSecondary.setOnClickListener {
            onExit().also { tapped(SEND_FINAL_EXIT) }
        }
        binding.backButtonHitArea.setOnClickListener {
            onExit().also { tapped(SEND_FINAL_CLOSE) }
        }
        binding.textConfirmation.text =
            "Sending ${sendViewModel.zatoshiAmount.convertZatoshiToZecString(8)} ZEC to\n${sendViewModel.toAddress.toAbbreviatedAddress()}"
        mainActivity?.preventBackPress(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity?.apply {
            sendViewModel.send().onEach {
                onPendingTxUpdated(it)
            }.launchIn(lifecycleScope)
        }
    }

    private fun onPendingTxUpdated(tx: PendingTransaction?) {
        if (tx == null) return // TODO: maybe log this

        try {
            tx.toUiModel().let { model ->
                binding.apply {
                    backButton.goneIf(!model.showCloseIcon)
                    backButtonHitArea.goneIf(!model.showCloseIcon)
                    buttonSecondary.goneIf(!model.showCloseIcon)

                    textConfirmation.text = model.title
                    lottieSending.goneIf(!model.showProgress)
                    if (!model.showProgress) lottieSending.pauseAnimation() else lottieSending.playAnimation()
                    errorMessage.text = model.errorMessage
                    buttonPrimary.apply {
                        text = model.primaryButtonText
                        setOnClickListener { model.primaryAction() }
                    }
                }
            }

            // only hold onto the view model if the transaction failed so that the user can retry
            if (tx.isSubmitSuccess()) {
                sendViewModel.reset()
            }
        } catch (t: Throwable) {
            val message = "ERROR: error while handling pending transaction update! $t"
            twig(message)
            mainActivity?.feedback?.report(Report.Error.NonFatal.TxUpdateFailed(t))
            mainActivity?.feedback?.report(t)
        }
    }

    private fun onExit() {
        sendViewModel.reset()
        mainActivity?.navController?.popBackStack(R.id.nav_home, false)
    }

    private fun onCancel(tx: PendingTransaction) {
        sendViewModel.cancel(tx.id)
    }

    private fun onReturnToSend() {
        mainActivity?.navController?.popBackStack(R.id.nav_send, false)
    }

    private fun onSeeDetails() {
        sendViewModel.reset()
        mainActivity?.safeNavigate(R.id.action_nav_send_final_to_nav_history)
    }

    private fun PendingTransaction.toUiModel() = UiModel().also { model ->
        when {
           isCancelled() -> {
                model.title = "Cancelled."
                model.primaryButtonText = "Go Back"
                model.primaryAction = { onReturnToSend() }
            }
            isSubmitSuccess() -> {
                model.title = "SENT!"
                model.primaryButtonText = "See Details"
                model.primaryAction = { onSeeDetails() }
            }
            isFailure() -> {
                model.title = "Failed."
                model.errorMessage = if (isFailedEncoding()) "The transaction could not be encoded." else "Unable to submit transaction to the network."
                model.primaryButtonText = "Retry"
                model.primaryAction = { onReturnToSend() }
            }
            else -> {
                model.title = "Sending ${value.convertZatoshiToZecString(8)} ZEC to\n${toAddress.toAbbreviatedAddress()}"
                model.showProgress = true
                if (isCreating()) {
                    model.showCloseIcon = false
                    model.primaryButtonText = "Cancel"
                    model.primaryAction = { onCancel(this) }
                } else {
                    model.primaryButtonText = "See Details"
                    model.primaryAction = { onSeeDetails() }
                }
            }
        }
    }

    // fields are ordered, as they appear, top-to-bottom in the UI because that makes it easier to reason about each screen state
    data class UiModel(
        var showCloseIcon: Boolean = true,
        var title: String = "",
        var showProgress: Boolean = false,
        var errorMessage: String = "",
        var primaryButtonText: String = "See Details",
        var primaryAction: () ->  Unit = {}
    )

}
