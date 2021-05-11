package cash.z.ecc.android.ui.profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentAwesomeBinding
import cash.z.ecc.android.di.viewmodel.viewModel
import cash.z.ecc.android.ext.distribute
import cash.z.ecc.android.ext.invisibleIf
import cash.z.ecc.android.ext.onClickNavBack
import cash.z.ecc.android.ext.pending
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Tap.AWESOME_CLOSE
import cash.z.ecc.android.feedback.Report.Tap.AWESOME_SHIELD
import cash.z.ecc.android.feedback.Report.Tap.COPY_TRANSPARENT_ADDRESS
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isCancelled
import cash.z.ecc.android.sdk.db.entity.isCreated
import cash.z.ecc.android.sdk.db.entity.isCreating
import cash.z.ecc.android.sdk.db.entity.isFailedEncoding
import cash.z.ecc.android.sdk.db.entity.isFailure
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.type.WalletBalance
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.util.AddressPartNumberSpan
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AwesomeFragment : BaseFragment<FragmentAwesomeBinding>() {
    override val screen = Report.Screen.AWESOME

    private val viewModel: ProfileViewModel by viewModel()

    private var lastBalance: WalletBalance? = null

    private var initialized: Boolean = false

    override fun inflate(inflater: LayoutInflater): FragmentAwesomeBinding =
        FragmentAwesomeBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack() { tapped(AWESOME_CLOSE) }
        binding.hitAreaAddress.setOnClickListener {
            tapped(COPY_TRANSPARENT_ADDRESS)
            onCopyTransparentAddress()
        }
        binding.buttonAction.setOnClickListener {
            onShieldFundsAction()
        }
        binding.lottieShielding.visibility = View.GONE
        setStatus("Checking balance...")
    }

    private fun onCopyTransparentAddress() {
        resumedScope.launch {
            mainActivity?.copyText(viewModel.getTransparentAddress(), "T-Address")
        }
    }

    override fun onResume() {
        super.onResume()
        if (!initialized) {
            resumedScope.launch {
                onAddressLoaded(viewModel.getTransparentAddress())
                updateBalance()
            }
            initialized = true
        }
    }

    private fun setStatus(status: String) {
        binding.textStatus.text = status
    }

    @SuppressLint("SetTextI18n")
    private fun appendStatus(status: String) {
        binding.textStatus.text = "${binding.textStatus.text}$status"
    }

    private suspend fun updateBalance() {
        val utxoCount = viewModel.fetchUtxos()

        viewModel.getTransparentBalance().let { balance ->
            onBalanceUpdated(balance, utxoCount)
        }

    }

    private fun onAddressLoaded(address: String) {
        twig("t-address loaded:  $address length: ${address.length}")
//        qrecycler.load(address)
//            .withQuietZoneSize(3)
//            .withCorrectionLevel(QRecycler.CorrectionLevel.MEDIUM)
//            .into(binding.receiveQrCode)

        address.distribute(2) { i, part ->
            setAddressPart(i, part)
        }
    }

    private fun setAddressPart(index: Int, addressPart: String) {
        twig("setting t-address for part $index) $addressPart")

        val address = when (index) {
            0 -> binding.textAddressPart1
            1 -> binding.textAddressPart2
            else -> throw IllegalArgumentException(
                "Unexpected address index $index. Unable to split the t-addr into two parts." +
                        " Ensure that the address is valid."
            )
        }

        val thinSpace = "\u2005" // 0.25 em space
        val textSpan = SpannableString("${index + 1}$thinSpace$addressPart")

        textSpan.setSpan(AddressPartNumberSpan(), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        address.text = textSpan
    }

    private fun onShieldFundsAction() {
        if (binding.buttonAction.isActivated) {
            tapped(AWESOME_SHIELD)
            mainActivity?.let { main ->
                main.authenticate(
                    "Shield transparent funds",
                    getString(R.string.biometric_backup_phrase_title)
                ) {
                    onShieldFunds()
                }
            }
        } else {
            Toast.makeText(requireContext(), "No balance to shield!", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun onDoneAction() {
//        mainActivity?.popBackTo(R.id.nav_home)
//        mainActivity?.safeNavigate(R.id.action_nav_send_final_to_nav_history)
//    }

    private fun onDoneAction() {
        viewModel.setEasterEggTriggered()
        mainActivity?.safeNavigate(R.id.action_nav_awesome_to_nav_history)
    }

    private fun onShieldFunds() {
        twig("onShieldFunds")
        lifecycleScope.launchWhenResumed {
            twig("launching shield funds job")
            viewModel.shieldFunds().onEach {
                onPendingTxUpdated(it)
            }.launchIn(lifecycleScope)
        }
    }

    private fun onPendingTxUpdated(tx: PendingTransaction) {
        twig("shielding transaction updated: $tx")
        if (tx == null) return // TODO: maybe log this

        try {
            tx.toUiModel().let { model ->
                binding.apply {
                    lottieShielding.invisibleIf(!model.showProgress)
                    buttonAction.isActivated = !model.showProgress || model.canCancel
                    buttonAction.isEnabled = true
                    buttonAction.refreshDrawableState()
                    setStatus(model.status)
                    appendStatus(model.details.joinToString("\n", "\n\n"))
                    buttonAction.apply {
                        text = model.primaryButtonText
                        setOnClickListener { model.primaryAction() }
                    }
                }
                if (model.updateBalance) {
                    resumedScope.launch {
                        delay(1000L)
                        updateBalance()
                    }
                }
            }
        } catch (t: Throwable) {
            val message = "ERROR: error while handling pending transaction update! $t"
            twig(message)
            mainActivity?.feedback?.report(Report.Error.NonFatal.TxUpdateFailed(t))
            mainActivity?.feedback?.report(t)
        }
    }


    private fun onShieldComplete(isSuccess: Boolean) {
        binding.lottieShielding.visibility = View.GONE

        if (isSuccess) {
            Toast.makeText(mainActivity, "Funds shielded successfully!", Toast.LENGTH_SHORT).show()
            binding.buttonAction.isEnabled = true
            binding.buttonAction.isActivated = true
            binding.buttonAction.text = "See Details"
            binding.textStatus.text = "Success!\n\nIt may take a while to show up."
            binding.buttonAction.setOnClickListener {
                mainActivity?.popBackTo(R.id.nav_home)
            }
        } else {
            Toast.makeText(mainActivity, "Failed to shield funds :(", Toast.LENGTH_SHORT).show()
            binding.buttonAction.isEnabled = true
            binding.buttonAction.text = "Shield Transparent Funds"
            binding.textStatus.text = "Failed!"
            binding.buttonAction.visibility = View.GONE
        }
    }

    private fun onBalanceUpdated(
        balance: WalletBalance = WalletBalance(0, 0),
        utxoCount: Int = 0
    ) {
        lastBalance = balance
        twig("TRANSPARENT BALANCE: ${balance.availableZatoshi} / ${balance.totalZatoshi}")
        binding.textStatus.text = if (balance.availableZatoshi > 0L) {
            binding.buttonAction.isActivated = true
            binding.buttonAction.isEnabled = true
            "Balance: ᙇ${balance.availableZatoshi.convertZatoshiToZecString(8)}"
        } else {
            binding.buttonAction.isActivated = false
            binding.buttonAction.isEnabled = true
            "No available balance found"
        }

        if (utxoCount > 0) {
            appendStatus("\n\nDownloaded $utxoCount ")
            appendStatus(if (utxoCount == 1) "transaction!" else "transactions!")
        }

        balance.pending.takeIf { it > 0 }?.let {
            appendStatus("\n\n(ᙇ${it.convertZatoshiToZecString()} pending confirmation)")
        }
    }




    private fun PendingTransaction.toUiModel() = UiModel().also { model ->
        when {
            isCancelled() -> {
                model.status = "Shielding Cancelled!"
                model.updateBalance = true
                model.primaryAction = { onShieldFundsAction() }
                model.details.add("Cancelled!")
            }
            isSubmitSuccess() -> {
                model.status = "Shielding Success!"
                model.primaryButtonText = "Done"
                model.primaryAction = { onDoneAction() }
            }
            isFailure() -> {
                model.status = if (isFailedEncoding()) {
                    "${getString(R.string.send_final_error_encoding)}\n\nPlease note:\nShielding requires funds\nto have 10 confirmations."
                } else {
                    "${getString(R.string.send_final_error_submitting)}\n\n${this.errorMessage}"
                }

                model.primaryAction = { onShieldFundsAction() }
            }
            else -> {
                model.status = "Shielding ᙇ${lastBalance?.availableZatoshi.convertZatoshiToZecString()}\n\nPlease do not exit this screen!"
                model.showProgress = true
                if (isCreating()) {
                    model.canCancel = true
                    model.details.add("Creating transaction...")
                    model.primaryButtonText = getString(R.string.send_final_button_primary_cancel)
                    model.primaryAction = { onCancel(this) }
                } else {
                    model.primaryButtonText = "Shielding Funds..."
                    if (isCreated()) model.details.add("Submitting transaction...")
                }
            }
        }
    }

    private fun onCancel(tx: PendingTransaction) {
        resumedScope.launch {
            viewModel.cancel(tx.id)
        }
    }

    // fields are ordered, as they appear, top-to-bottom in the UI because that makes it easier to reason about each screen state
    data class UiModel(
        var status: String = "",
        val details: MutableSet<String> = linkedSetOf(),
        var showProgress: Boolean = false,
        var primaryButtonText: String = "Shield Transparent Funds",
        var primaryAction: () -> Unit = {},
        var canCancel: Boolean = false,
        var updateBalance: Boolean = false,
    )
}