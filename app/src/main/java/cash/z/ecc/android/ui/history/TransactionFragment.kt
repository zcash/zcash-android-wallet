package cash.z.ecc.android.ui.history

import android.content.res.ColorStateList
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.transition.*
import cash.z.ecc.android.R
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.databinding.FragmentTransactionBinding
import cash.z.ecc.android.di.viewmodel.activityViewModel
import cash.z.ecc.android.ext.*
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.ui.MainActivity
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.util.toUtf8Memo
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*


class TransactionFragment : BaseFragment<FragmentTransactionBinding>() {
    override val screen = Report.Screen.TRANSACTION
    private val viewModel: HistoryViewModel by activityViewModel()

    var isMemoExpanded: Boolean = false

    override fun inflate(inflater: LayoutInflater): FragmentTransactionBinding =
        FragmentTransactionBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val transition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
//        sharedElementEnterTransition = transition
//        sharedElementReturnTransition = transition

//        sharedElementEnterTransition = createSharedElementTransition()
//        sharedElementReturnTransition = createSharedElementTransition()

//        sharedElementEnterTransition = ChangeBounds().apply { duration = 1500 }
//        sharedElementReturnTransition = ChangeBounds().apply { duration = 1500 }
//        enterTransition = Fade().apply {
//            duration = 1800
////            slideEdge = Gravity.END
//        }
    }

    private fun createSharedElementTransition(duration: Long = 800L): Transition {
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            this.duration = duration
//            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.2f, 1f)
            addTransition(ChangeBounds())
            addTransition(ChangeClipBounds())
            addTransition(ChangeTransform())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            ViewCompat.setTransitionName(topBoxValue, "test_amount_anim_${viewModel.selectedTransaction!!.id}")
            ViewCompat.setTransitionName(topBoxBackground, "test_bg_anim_${viewModel.selectedTransaction!!.id}")
            backButtonHitArea.onClickNavBack { tapped(Report.Tap.TRANSACTION_BACK) }

            lifecycleScope.launch {
                viewModel.selectedTransaction.toUiModel(viewModel.latestHeight).let { uiModel ->
                    topBoxLabel.text = uiModel.topLabel
                    topBoxValue.text = uiModel.topValue
                    bottomBoxLabel.text = uiModel.bottomLabel
                    bottomBoxValue.text = uiModel.bottomValue
                    textBlockHeight.text = uiModel.minedHeight
                    textTimestamp.text = uiModel.timestamp
                    if (uiModel.iconRotation < 0) {
                        topBoxIcon.gone()
                    } else {
                        topBoxIcon.rotation = uiModel.iconRotation
                        topBoxIcon.visible()
                    }

                    if (!uiModel.isMined) {
                        textBlockHeight.invisible()
                        textBlockHeightPrefix.invisible()
                    }

                    val exploreOnClick = View.OnClickListener {
                        uiModel.txId?.let { txId ->
                            mainActivity?.showFirstUseWarning(
                                Const.Pref.FIRST_USE_VIEW_TX,
                                titleResId = R.string.dialog_first_use_view_tx_title,
                                msgResId = R.string.dialog_first_use_view_tx_message,
                                positiveResId = R.string.dialog_first_use_view_tx_positive,
                                negativeResId = R.string.dialog_first_use_view_tx_negative
                            ) {
                                onLaunchUrl(txId.toTransactionUrl())
                            }
                        }
                    }
                    buttonExplore.setOnClickListener(exploreOnClick)
                    textBlockHeight.setOnClickListener(exploreOnClick)

                    uiModel.fee?.let { subwaySpotFee.visible(); subwayLabelFee.visible(); subwayLabelFee.text = it }
                    uiModel.source?.let { subwaySpotSource.visible(); subwayLabelSource.visible(); subwayLabelSource.text = it }
                    uiModel.toAddressLabel()?.let { subwaySpotAddress.visible(); subwayLabelAddress.visible(); subwayLabelAddress.text = it }
                    uiModel.toAddressClickListener()?.let { subwayLabelAddress.setOnClickListener(it) }


                    // TODO: remove logic from sections below and add more fields or extension functions to UiModel
                    uiModel.confirmation?.let {
                        subwaySpotConfirmations.visible(); subwayLabelConfirmations.visible()
                        subwayLabelConfirmations.text = it
                        if (it.equals(getString(R.string.transaction_status_confirmed), true)) {
                            subwayLabelConfirmations.setTextColor(R.color.tx_primary.toAppColor())
                        } else {
                            subwayLabelConfirmations.setTextColor(R.color.tx_text_light_dimmed.toAppColor())
                        }
                    }

                    uiModel.memo?.let {
                        hitAreaMemoSubway.setOnClickListener { _ -> onToggleMemo(!isMemoExpanded, it) }
                        hitAreaMemoIcon.setOnClickListener { _ -> onToggleMemo(!isMemoExpanded, it) }
                        subwayLabelMemo.movementMethod = ScrollingMovementMethod()
                        subwaySpotMemoContent.visible()
                        subwayLabelMemo.visible()
                        hitAreaMemoSubway.visible()
                        onToggleMemo(false)
                    }
                }
            }
        }
    }

    val invertingMatrix = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    private fun onToggleMemo(isExpanded: Boolean, memo: String = "") {
        twig("onToggleMemo($isExpanded, $memo)")
        if (isExpanded) {
            twig("setting memo text to: $memo")
            binding.subwayLabelMemo.setText(memo)
            binding.subwayLabelMemo.invalidate()
            // don't impede the ability to scroll
            binding.groupMemoIcon.gone()
            binding.subwayLabelMemo.backgroundTintList = ColorStateList.valueOf(R.color.tx_text_light_dimmed.toAppColor())
            binding.subwaySpotMemoContent.colorFilter = invertingMatrix
            binding.subwaySpotMemoContent.rotation = 90.0f
        } else {
            binding.subwayLabelMemo.setText(getString(R.string.transaction_with_memo))
            binding.subwayLabelMemo.invalidate()
            twig("setting memo text to: with a memo")
            binding.groupMemoIcon.visible()
            binding.subwayLabelMemo.backgroundTintList = ColorStateList.valueOf(R.color.tx_primary.toAppColor())
            binding.subwaySpotMemoContent.colorFilter = null
            binding.subwaySpotMemoContent.rotation = 0.0f
        }
        isMemoExpanded = isExpanded
    }

    private fun String.toTransactionUrl(): String {
        return "https://explorer.z.cash/tx/$this"
    }

    private fun UiModel?.toAddressClickListener(): View.OnClickListener? {
        return this?.address?.let { addr ->
            View.OnClickListener { mainActivity?.copyText(addr, "Address") }
        }
    }

    private fun UiModel?.toAddressLabel(): CharSequence? {
        if (this == null || this.address == null || this.isInbound == null) return null
        val prefix = getString(
            if (isInbound == true) {
                R.string.transaction_prefix_from
            } else {
                R.string.transaction_prefix_to
            }
        )
        return "$prefix ${address?.toAbbreviatedAddress() ?: "Unknown" }".let {
            it.toColoredSpan(R.color.tx_text_light_dimmed, if (address == null) it else prefix)
        }
    }

    private suspend fun ConfirmedTransaction?.toUiModel(latestHeight: Int? = null): UiModel = UiModel().apply {
        this@toUiModel.let { tx ->
            txId = mainActivity?.toTxId(tx?.rawTransactionId)
            isInbound = when {
                !(tx?.toAddress.isNullOrEmpty()) -> false
                tx != null && tx.toAddress.isNullOrEmpty() && tx.value > 0L && tx.minedHeight > 0 -> true
                else -> null
            }
            isMined = tx?.minedHeight != null && tx.minedHeight > ZcashSdk.SAPLING_ACTIVATION_HEIGHT
            topValue = if (tx == null) "" else "\$${WalletZecFormmatter.toZecStringFull(tx.value)}"
            minedHeight = (tx?.minedHeight ?: 0).toString()
            val flags =
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            timestamp = if (tx == null) getString(R.string.transaction_timestamp_unavailable) else DateUtils.getRelativeDateTimeString(
                ZcashWalletApp.instance,
                tx.blockTimeInSeconds * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                flags
            ).toString()

            // memo logic
            val txMemo = tx?.memo.toUtf8Memo()
            if (!txMemo.isEmpty()) {
                memo = txMemo
            }

            // confirmation logic
            // TODO: clean all of this up and remove/improve reliance on `isSufficientlyOld` function. Also, add a constant for the number of confirmations we expect.
            tx?.let {
                val isMined = it.blockTimeInSeconds != 0L
                if (isMined) {
                    val hasLatestHeight = latestHeight != null && latestHeight > ZcashSdk.SAPLING_ACTIVATION_HEIGHT
                    if (it.minedHeight > 0 && hasLatestHeight) {
                        val confirmations = latestHeight!! - it.minedHeight + 1
                        confirmation = if (confirmations >= 10) getString(R.string.transaction_status_confirmed) else "$confirmations ${getString(R.string.transaction_status_confirming)}"
                    } else {
                        if (!hasLatestHeight && isSufficientlyOld(tx)) {
                            twig("Warning: could not load latestheight from server to determine confirmations but this transaction is mined and old enough to be considered confirmed")
                            confirmation = getString(R.string.transaction_status_confirmed)
                        } else {
                            twig("Warning: could not determine confirmation text value so it will be left null!")
                            confirmation = getString(R.string.transaction_confirmation_count_unavailable)
                        }
                    }
                } else {
                    confirmation = getString(R.string.transaction_status_pending)
                }

            }

            val mainActivity = (context as MainActivity)
            // inbound v. outbound values
            when (isInbound) {
                true -> {
                    topLabel = getString(R.string.transaction_story_inbound)
                    bottomLabel = getString(R.string.transaction_story_inbound_total)
                    bottomValue = "\$${WalletZecFormmatter.toZecStringFull(tx?.value)}"
                    iconRotation = 315f
                    source = getString(R.string.transaction_story_to_shielded)
                    address = mainActivity.extractValidAddress(tx?.memo.toUtf8Memo())
                }
                false -> {
                    topLabel = getString(R.string.transaction_story_outbound)
                    bottomLabel = getString(R.string.transaction_story_outbound_total)
                    bottomValue = "\$${WalletZecFormmatter.toZecStringFull(tx?.value?.plus(ZcashSdk.MINERS_FEE_ZATOSHI))}"
                    iconRotation = 135f
                    fee = getString(R.string.transaction_story_network_fee, WalletZecFormmatter.toZecStringFull(ZcashSdk.MINERS_FEE_ZATOSHI))
                    source = getString(R.string.transaction_story_from_shielded)
                    address = tx?.toAddress
                }
                null -> {
                    twig("Error: transaction appears to be invalid.")
                }
            }
        }
    }

    // TODO: determine this in a more generic and technically correct way. For now, this is good enough.
    //       the goal is just to improve the edge cases where the latest height isn't known but other
    //       information suggests that the TX is confirmed. We can improve this, later.
    private fun isSufficientlyOld(tx: ConfirmedTransaction): Boolean {
        val threshold = 75 * 1000 * 25 //approx 25 blocks
        val delta = System.currentTimeMillis() / 1000L - tx.blockTimeInSeconds
        return tx.minedHeight > ZcashSdk.SAPLING_ACTIVATION_HEIGHT
                && delta < threshold
    }

    data class UiModel(
        var topLabel: String = "",
        var topValue: String = "",
        var bottomLabel: String = "",
        var bottomValue: String = "",
        var minedHeight: String = "",
        var timestamp: String = "",
        var iconRotation: Float = -1f,

        var fee: String? = null,
        var source: String? = null,
        var memo: String? = null,
        var address: String? = null,
        var isInbound: Boolean? = null,
        var isMined: Boolean = false,
        var confirmation: String? = null,
        var txId: String? = null
    )
}




