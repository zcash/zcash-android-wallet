package cash.z.ecc.android.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentHomeBinding
import cash.z.ecc.android.di.viewmodel.activityViewModel
import cash.z.ecc.android.di.viewmodel.viewModel
import cash.z.ecc.android.ext.WalletZecFormmatter
import cash.z.ecc.android.ext.disabledIf
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.ext.invisibleIf
import cash.z.ecc.android.ext.onClickNavTo
import cash.z.ecc.android.ext.showSharedLibraryCriticalError
import cash.z.ecc.android.ext.toColoredSpan
import cash.z.ecc.android.ext.transparentIf
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Tap.HOME_CLEAR_AMOUNT
import cash.z.ecc.android.feedback.Report.Tap.HOME_FUND_NOW
import cash.z.ecc.android.feedback.Report.Tap.HOME_HISTORY
import cash.z.ecc.android.feedback.Report.Tap.HOME_PROFILE
import cash.z.ecc.android.feedback.Report.Tap.HOME_RECEIVE
import cash.z.ecc.android.feedback.Report.Tap.HOME_SEND
import cash.z.ecc.android.sdk.Synchronizer.Status.DISCONNECTED
import cash.z.ecc.android.sdk.Synchronizer.Status.STOPPED
import cash.z.ecc.android.sdk.Synchronizer.Status.SYNCED
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.onFirstWith
import cash.z.ecc.android.sdk.ext.safelyConvertToBigDecimal
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.home.HomeFragment.BannerAction.CANCEL
import cash.z.ecc.android.ui.home.HomeFragment.BannerAction.CLEAR
import cash.z.ecc.android.ui.home.HomeFragment.BannerAction.FUND_NOW
import cash.z.ecc.android.ui.send.SendViewModel
import cash.z.ecc.android.ui.setup.WalletSetupViewModel
import cash.z.ecc.android.ui.setup.WalletSetupViewModel.WalletSetupState.NO_SEED
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    override val screen = Report.Screen.HOME

    private lateinit var numberPad: List<TextView>
    private lateinit var uiModel: HomeViewModel.UiModel

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)
    private val sendViewModel: SendViewModel by activityViewModel()
    private val viewModel: HomeViewModel by viewModel()

    lateinit var snake: MagicSnakeLoader

    override fun inflate(inflater: LayoutInflater): FragmentHomeBinding =
        FragmentHomeBinding.inflate(inflater)

    //
    // LifeCycle
    //

    override fun onAttach(context: Context) {
        twig("HomeFragment.onAttach")
        twig("ZZZ")
        twig("ZZZ")
        twig("ZZZ")
        twig("ZZZ   ===================== HOME FRAGMENT CREATED ==================================")
        super.onAttach(context)

        walletSetup.checkSeed().onFirstWith(lifecycleScope) {
            if (it == NO_SEED) {
                // interact with user to create, backup and verify seed
                // leads to a call to startSync(), later (after accounts are created from seed)
                twig("Previous wallet not found, therefore, launching seed creation flow")
                mainActivity?.setLoading(false)
                mainActivity?.safeNavigate(R.id.action_nav_home_to_create_wallet)
            } else {
                twig("Previous wallet found. Re-opening it.")
                mainActivity?.setLoading(true)
                try {
                    mainActivity?.startSync(walletSetup.openStoredWallet())
                } catch (e: UnsatisfiedLinkError) {
                    mainActivity?.showSharedLibraryCriticalError(e)
                }
                twig("Done reopening wallet.")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        twig("HomeFragment.onViewCreated  uiModel: ${::uiModel.isInitialized}  saved: ${savedInstanceState != null}")
        with(binding) {
            numberPad = arrayListOf(
                buttonNumberPad0.asKey(),
                buttonNumberPad1.asKey(),
                buttonNumberPad2.asKey(),
                buttonNumberPad3.asKey(),
                buttonNumberPad4.asKey(),
                buttonNumberPad5.asKey(),
                buttonNumberPad6.asKey(),
                buttonNumberPad7.asKey(),
                buttonNumberPad8.asKey(),
                buttonNumberPad9.asKey(),
                buttonNumberPadDecimal.asKey(),
                buttonNumberPadBack.asKey()
            )
            hitAreaProfile.onClickNavTo(R.id.action_nav_home_to_nav_profile) { tapped(HOME_PROFILE) }
            textHistory.onClickNavTo(R.id.action_nav_home_to_nav_history) { tapped(HOME_HISTORY) }
            hitAreaReceive.onClickNavTo(R.id.action_nav_home_to_nav_receive) { tapped(HOME_RECEIVE) }

            textBannerAction.setOnClickListener {
                onBannerAction(BannerAction.from((it as? TextView)?.text?.toString()))
            }
            buttonSendAmount.setOnClickListener {
                onSend().also { tapped(HOME_SEND) }
            }
            setSendAmount("0", false)

            snake = MagicSnakeLoader(binding.lottieButtonLoading)
        }

        binding.buttonNumberPadBack.setOnLongClickListener {
            onClearAmount().also { tapped(HOME_CLEAR_AMOUNT) }
            true
        }

        if (::uiModel.isInitialized) {
            twig("uiModel exists! it has pendingSend=${uiModel.pendingSend} ZEC while the sendViewModel=${sendViewModel.zatoshiAmount} zats")
            // if the model already existed, cool but let the sendViewModel be the source of truth for the amount
            onModelUpdated(null, uiModel.copy(pendingSend = WalletZecFormmatter.toZecStringFull(sendViewModel.zatoshiAmount.coerceAtLeast(0))))
        }
    }

    private fun onClearAmount() {
        twig("onClearAmount()")
        if (::uiModel.isInitialized) {
            resumedScope.launch {
                binding.textSendAmount.text.apply {
                    while (uiModel.pendingSend != "0") {
                        viewModel.onChar('<')
                        delay(5)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainActivity?.launchWhenSyncing {
            twig("HomeFragment.onResume  resumeScope.isActive: ${resumedScope.isActive}  $resumedScope")
            val existingAmount = sendViewModel.zatoshiAmount.coerceAtLeast(0)
            viewModel.initializeMaybe(WalletZecFormmatter.toZecStringFull(existingAmount))
            if (existingAmount == 0L) onClearAmount()
            viewModel.uiModels.runningReduce { old, new ->
                onModelUpdated(old, new)
                new
            }.onCompletion {
                twig("uiModel.scanReduce completed.")
            }.catch { e ->
                twig("exception while processing uiModels $e")
                throw e
            }.launchIn(resumedScope)

            // TODO: see if there is a better way to trigger a refresh of the uiModel on resume
            //       the latest one should just be in the viewmodel and we should just "resubscribe"
            //       but for some reason, this doesn't always happen, which kind of defeats the purpose
            //       of having a cold stream in the view model
            resumedScope.launch {
                viewModel.refreshBalance()
            }
            twig("HomeFragment.onResume COMPLETE")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
//        if (::uiModel.isInitialized) {
//            outState.putParcelable("uiModel", uiModel)
//        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let { inState ->
//            onModelUpdated(HomeViewModel.UiModel(), inState.getParcelable("uiModel")!!)
        }
    }

    //
    // Public UI API
    //

    var isSendEnabled = false
    fun setSendEnabled(enabled: Boolean, isSynced: Boolean) {
        isSendEnabled = enabled
        binding.buttonSendAmount.apply {
            if (enabled || !isSynced) {
                isEnabled = true
                isClickable = isSynced
                binding.lottieButtonLoading.alpha = 1.0f
            } else {
                isEnabled = false
                isClickable = false
                binding.lottieButtonLoading.alpha = 0.32f
            }
        }
    }

    fun setProgress(uiModel: HomeViewModel.UiModel) {
        if (!uiModel.processorInfo.hasData && !uiModel.isDisconnected) {
            twig("Warning: ignoring progress update because the processor is still starting.")
            return
        }

        snake.isSynced = uiModel.isSynced
        if (!uiModel.isSynced) {
            snake.downloadProgress = uiModel.downloadProgress
            snake.scanProgress = uiModel.scanProgress
        }

        val sendText = when {
            uiModel.status == DISCONNECTED -> getString(R.string.home_button_send_disconnected)
            uiModel.isSynced -> if (uiModel.hasFunds) getString(R.string.home_button_send_has_funds) else getString(R.string.home_button_send_no_funds)
            uiModel.status == STOPPED -> getString(R.string.home_button_send_idle)
            uiModel.isDownloading -> getString(R.string.home_button_send_downloading, snake.downloadProgress)
            uiModel.isValidating -> getString(R.string.home_button_send_validating)
            uiModel.isScanning -> getString(R.string.home_button_send_scanning, snake.scanProgress)
            else -> getString(R.string.home_button_send_updating)
        }

        binding.buttonSendAmount.text = sendText
        twig("Send button set to: $sendText")

        val resId = if (uiModel.isSynced) R.color.selector_button_text_dark else R.color.selector_button_text_light
        context?.let { binding.buttonSendAmount.setTextColor(AppCompatResources.getColorStateList(it, resId)) }
        binding.lottieButtonLoading.invisibleIf(uiModel.isDisconnected)
    }

    /**
     * @param amount the amount to send represented as ZEC, without the dollar sign.
     */
    fun setSendAmount(amount: String, updateModel: Boolean = true) {
        twig("setSendAmount($amount, $updateModel)")
        binding.textSendAmount.text = "\$$amount".toColoredSpan(R.color.text_light_dimmed, "$")
        if (updateModel) {
            sendViewModel.zatoshiAmount = amount.safelyConvertToBigDecimal().convertZecToZatoshi()
            twig("dBUG: updating model. converting: $amount\tresult: ${sendViewModel.zatoshiAmount}\tprint: ${WalletZecFormmatter.toZecStringFull(sendViewModel.zatoshiAmount)}")
        }
        binding.buttonSendAmount.disabledIf(amount == "0")
    }

    fun setAvailable(availableBalance: Long = -1L, totalBalance: Long = -1L) {
        val missingBalance = availableBalance < 0
        val availableString = if (missingBalance) getString(R.string.home_button_send_updating) else WalletZecFormmatter.toZecStringFull(availableBalance)
        binding.textBalanceAvailable.text = availableString
        binding.textBalanceAvailable.transparentIf(missingBalance)
        binding.labelBalance.transparentIf(missingBalance)
        binding.textBalanceDescription.apply {
            goneIf(missingBalance)
            text = if (availableBalance != -1L && (availableBalance < totalBalance)) {
                val change = WalletZecFormmatter.toZecStringFull(totalBalance - availableBalance)
                "(${getString(R.string.home_banner_expecting)} +$change ZEC)".toColoredSpan(R.color.text_light, "+$change")
            } else {
                getString(R.string.home_instruction_enter_amount)
            }
        }
    }

    fun setBanner(message: String = "", action: BannerAction = CLEAR) {
        with(binding) {
            val hasMessage = !message.isEmpty() || action != CLEAR
            groupBalance.goneIf(hasMessage)
            groupBanner.goneIf(!hasMessage)
            layerLock.goneIf(!hasMessage)

            textBannerMessage.text = message
            textBannerAction.text = action.action
        }
    }

    //
    // Private UI Events
    //

    private fun onModelUpdated(old: HomeViewModel.UiModel?, new: HomeViewModel.UiModel) {
        logUpdate(old, new)
        uiModel = new
        if (old?.pendingSend != new.pendingSend) {
            setSendAmount(new.pendingSend)
        }
        setProgress(new) // TODO: we may not need to separate anymore
//        if (new.status = SYNCING) onSyncing(new) else onSynced(new)
        if (new.status == SYNCED) onSynced(new) else onSyncing(new)
        setSendEnabled(new.isSendEnabled, new.status == SYNCED)
    }

    private fun logUpdate(old: HomeViewModel.UiModel?, new: HomeViewModel.UiModel) {
        var message = ""
        fun maybeComma() = if (message.length > "UiModel(".length) ", " else ""
        message = when {
            old == null -> "$new"
            new == null -> "null"
            else -> {
                buildString {
                    append("UiModel(")
                    if (old.status != new.status) append("status=${new.status}")
                    if (old.processorInfo != new.processorInfo) {
                        append("${maybeComma()}processorInfo=ProcessorInfo(")
                        val startLength = length
                        fun innerComma() = if (length > startLength) ", " else ""
                        if (old.processorInfo.networkBlockHeight != new.processorInfo.networkBlockHeight) append("networkBlockHeight=${new.processorInfo.networkBlockHeight}")
                        if (old.processorInfo.lastScannedHeight != new.processorInfo.lastScannedHeight) append("${innerComma()}lastScannedHeight=${new.processorInfo.lastScannedHeight}")
                        if (old.processorInfo.lastDownloadedHeight != new.processorInfo.lastDownloadedHeight) append("${innerComma()}lastDownloadedHeight=${new.processorInfo.lastDownloadedHeight}")
                        if (old.processorInfo.lastDownloadRange != new.processorInfo.lastDownloadRange) append("${innerComma()}lastDownloadRange=${new.processorInfo.lastDownloadRange}")
                        if (old.processorInfo.lastScanRange != new.processorInfo.lastScanRange) append("${innerComma()}lastScanRange=${new.processorInfo.lastScanRange}")
                        append(")")
                    }
                    if (old.availableBalance != new.availableBalance) append("${maybeComma()}availableBalance=${new.availableBalance}")
                    if (old.totalBalance != new.totalBalance) append("${maybeComma()}totalBalance=${new.totalBalance}")
                    if (old.pendingSend != new.pendingSend) append("${maybeComma()}pendingSend=${new.pendingSend}")
                    append(")")
                }
            }
        }
        twig("onModelUpdated: $message")
    }

    private fun onSyncing(uiModel: HomeViewModel.UiModel) {
        setAvailable()
    }

    private fun onSynced(uiModel: HomeViewModel.UiModel) {
        snake.isSynced = true
        if (!uiModel.hasBalance) {
            onNoFunds()
        } else {
            setBanner("")
            setAvailable(uiModel.availableBalance, uiModel.totalBalance)
        }
    }

    private fun onSend() {
        if (isSendEnabled) mainActivity?.safeNavigate(R.id.action_nav_home_to_send)
    }

    private fun onBannerAction(action: BannerAction) {
        when (action) {
            FUND_NOW -> {
                MaterialAlertDialogBuilder(activity)
                    .setMessage(R.string.home_dialog_no_balance_message)
                    .setTitle(R.string.home_dialog_no_balance_title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.home_dialog_no_balance_button_positive) { dialog, _ ->
                        tapped(HOME_FUND_NOW)
                        dialog.dismiss()
                        mainActivity?.safeNavigate(R.id.action_nav_home_to_nav_receive)
                    }
                    .show()
//                MaterialAlertDialogBuilder(activity)
//                    .setMessage("To make full use of this wallet, deposit funds to your address or tap the faucet to trigger a tiny automatic deposit.\n\nFaucet funds are made available for the community by the community for testing. So please be kind enough to return what you borrow!")
//                    .setTitle("No Balance")
//                    .setCancelable(true)
//                    .setPositiveButton("Tap Faucet") { dialog, _ ->
//                        dialog.dismiss()
//                        setBanner("Tapping faucet...", CANCEL)
//                    }
//                    .setNegativeButton("View Address") { dialog, _ ->
//                        dialog.dismiss()
//                        mainActivity?.safeNavigate(R.id.action_nav_home_to_nav_receive)
//                    }
//                    .show()
            }
            CANCEL -> {
                // TODO: trigger banner / balance update
                onNoFunds()
            }
        }
    }

    private fun onNoFunds() {
        setBanner(getString(R.string.home_no_balance), FUND_NOW)
    }

    //
    // Inner classes and extensions
    //

    enum class BannerAction(val action: String) {
        FUND_NOW(""),
        CANCEL("Cancel"),
        NONE(""),
        CLEAR("clear");

        companion object {
            fun from(action: String?): BannerAction {
                values().forEach {
                    if (it.action == action) return it
                }
                throw IllegalArgumentException("Invalid BannerAction: $action")
            }
        }
    }

    private fun TextView.asKey(): TextView {
        val c = text[0]
        setOnClickListener {
            lifecycleScope.launch {
                viewModel.onChar(c)
            }
        }
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }
    override fun onStart() {
        super.onStart()
        twig("HomeFragment.onStart")
    }
    override fun onPause() {
        super.onPause()
    }
    override fun onStop() {
        super.onStop()
    }
    override fun onDestroyView() {
        super.onDestroyView()
    }
    override fun onDestroy() {
        super.onDestroy()
    }
    override fun onDetach() {
        super.onDetach()
    }
}
