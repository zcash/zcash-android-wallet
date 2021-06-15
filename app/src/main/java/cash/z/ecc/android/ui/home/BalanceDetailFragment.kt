package cash.z.ecc.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import cash.z.ecc.android.R
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.databinding.FragmentBalanceDetailBinding
import cash.z.ecc.android.di.viewmodel.viewModel
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.ext.onClickNavBack
import cash.z.ecc.android.ext.toAppColor
import cash.z.ecc.android.ext.toSplitColorSpan
import cash.z.ecc.android.feedback.Report.Tap.RECEIVE_BACK
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.home.BalanceDetailViewModel.StatusModel

class BalanceDetailFragment : BaseFragment<FragmentBalanceDetailBinding>() {

    private val viewModel: BalanceDetailViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentBalanceDetailBinding =
        FragmentBalanceDetailBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack() { tapped(RECEIVE_BACK) }
        binding.textShieldedZecTitle.text = "SHIELDED ${getString(R.string.symbol)}"
        binding.buttonShieldTransaparentFunds.setOnClickListener {
            onAutoShield()
        }

        binding.switchFunds.isChecked = viewModel.showAvailable
        setFundSource(viewModel.showAvailable)
        binding.switchFunds.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.showAvailable = isChecked
            setFundSource(isChecked)
        }
        binding.textSwitchAvailable.setOnClickListener {
            binding.switchFunds.isChecked = true
        }
        binding.textSwitchTotal.setOnClickListener {
            binding.switchFunds.isChecked = false
        }
    }

    private fun setFundSource(isAvailable: Boolean) {
        val selected = R.color.colorPrimary.toAppColor()
        val unselected = R.color.text_light_dimmed.toAppColor()
        binding.textSwitchTotal.setTextColor(if (!isAvailable) selected else unselected)
        binding.textSwitchAvailable.setTextColor(if (isAvailable) selected else unselected)

        viewModel.latestBalance?.let { balance ->
            onBalanceUpdated(balance)
        }
    }

    private fun onAutoShield() {
        if (binding.buttonShieldTransaparentFunds.isActivated) {
            mainActivity?.let { main ->
                main.authenticate(
                    "Shield transparent funds",
                    getString(R.string.biometric_backup_phrase_title)
                ) {
                    main.safeNavigate(R.id.action_nav_balance_detail_to_shield_final)
                }
            }
        } else {
            val toast = when {
                // if funds exist but they're all unconfirmed
                (viewModel.latestBalance?.transparentBalance?.totalZatoshi ?: 0) > 0 -> {
                    "Please wait for more confirmations"
                }
                viewModel.latestBalance?.hasData() == true -> {
                    "No transparent funds"
                }
                else -> {
                    "Please wait until fully synced"
                }
            }
            Toast.makeText(mainActivity, toast, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.balances.collectWith(resumedScope, ::onBalanceUpdated)
        viewModel.statuses.collectWith(resumedScope, ::onStatusUpdated)
    }

    private fun onBalanceUpdated(balanceModel: BalanceDetailViewModel.BalanceModel) {
        balanceModel.apply {
            if (balanceModel.hasData()) {
                setBalances(paddedShielded, paddedTransparent, paddedTotal)
                updateButton(canAutoShield)
            } else {
                setBalances(" --", " --", " --")
                updateButton(false)
            }
        }
    }

    private fun updateButton(canAutoshield: Boolean) {
        binding.buttonShieldTransaparentFunds.apply {
            isActivated = canAutoshield
            refreshDrawableState()
        }
    }

    private fun onStatusUpdated(status: StatusModel) {
        binding.textStatus.text = status.toStatus()
        val height = String.format("%,d", status.latestHeight)
        binding.textBlockHeight.apply {
            // if we got a new block
            if (text.isNotEmpty() && text.toString() != height && isResumed) {
                mainActivity?.vibrate(0, 100, 100, 300)
                Toast.makeText(mainActivity, "New block!", Toast.LENGTH_SHORT).show()
            }
            binding.textBlockHeight.text = height
        }

        status.balances.hasPending.let { hasPending ->
            binding.switchFunds.goneIf(!hasPending)
            binding.textSwitchTotal.goneIf(!hasPending)
            binding.textSwitchAvailable.goneIf(!hasPending)
        }
    }


    fun setBalances(shielded: String, transparent: String, total: String) {
        binding.textShieldAmount.text = shielded.colorize()
        binding.textTransparentAmount.text = transparent.colorize()
        binding.textTotalAmount.text = total.colorize()
    }

    private fun String.colorize(): CharSequence {
        val dotIndex = indexOf('.')
        return if (dotIndex < 0 || length < (dotIndex + 4)) {
            this
        } else {
            toSplitColorSpan(R.color.text_light, R.color.zcashWhite_24, indexOf('.') + 4)
        }
    }

    private fun StatusModel.toStatus(): String {
        fun String.plural(count: Int) = if (count > 1) "${this}s" else this

        if (viewModel.latestBalance?.hasData() == false) {
            return "Balance info is not yet available"
        }

        var status = ""
        if (hasUnmined) {
            val count = pendingUnmined.count()
            status += "Balance excludes $count unmined ${"transaction".plural(count)}. "
        }

        status += when {
            hasPendingTransparentBalance && hasPendingShieldedBalance -> {
                "Awaiting ${pendingShieldedBalance.convertZatoshiToZecString(8)} ${ZcashWalletApp.instance.getString(R.string.symbol)} in shielded funds and ${pendingTransparentBalance.convertZatoshiToZecString(8)} ${ZcashWalletApp.instance.getString(R.string.symbol)} in transparent funds"
            }
            hasPendingShieldedBalance -> {
                "Awaiting ${pendingShieldedBalance.convertZatoshiToZecString(8)} ${ZcashWalletApp.instance.getString(R.string.symbol)} in shielded funds"
            }
            hasPendingTransparentBalance -> {
                "Awaiting ${pendingTransparentBalance.convertZatoshiToZecString(8)} ${ZcashWalletApp.instance.getString(R.string.symbol)} in transparent funds"
            }
            else -> ""
        }

        pendingUnconfirmed.count().takeUnless { it == 0 }?.let { count ->
            if (status.contains("Awaiting")) status += " and "
            status += "$count outbound ${"transaction".plural(count)}"
            remainingConfirmations().firstOrNull()?.let { remaining ->
                status += " with $remaining ${"confirmation".plural(remaining)} remaining"
            }
        }

        return if (status.isEmpty()) "All funds are available!" else status
    }
}
