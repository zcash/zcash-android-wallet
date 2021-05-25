package cash.z.ecc.android.ui.send

import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentSendBinding
import cash.z.ecc.android.di.viewmodel.activityViewModel
import cash.z.ecc.android.ext.WalletZecFormmatter
import cash.z.ecc.android.ext.gone
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.ext.onClickNavUp
import cash.z.ecc.android.ext.toAppColor
import cash.z.ecc.android.ext.visible
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Tap.SEND_ADDRESS_BACK
import cash.z.ecc.android.feedback.Report.Tap.SEND_ADDRESS_PASTE
import cash.z.ecc.android.feedback.Report.Tap.SEND_ADDRESS_REUSE
import cash.z.ecc.android.feedback.Report.Tap.SEND_ADDRESS_SCAN
import cash.z.ecc.android.feedback.Report.Tap.SEND_MEMO_EXCLUDE
import cash.z.ecc.android.feedback.Report.Tap.SEND_MEMO_INCLUDE
import cash.z.ecc.android.feedback.Report.Tap.SEND_SUBMIT
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.onFirstWith
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.type.AddressType
import cash.z.ecc.android.sdk.type.WalletBalance
import cash.z.ecc.android.ui.base.BaseFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SendFragment :
    BaseFragment<FragmentSendBinding>(),
    ClipboardManager.OnPrimaryClipChangedListener {
    override val screen = Report.Screen.SEND_ADDRESS

    private var maxZatoshi: Long? = null
    private var availableZatoshi: Long? = null

    val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendBinding =
        FragmentSendBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply View Model
        applyViewModel(sendViewModel)
        updateAddressUi(false)

        // Apply behaviors

        binding.buttonSend.setOnClickListener {
            onSubmit().also { tapped(SEND_SUBMIT) }
        }

        binding.checkIncludeAddress.setOnCheckedChangeListener { _, _ ->
            onIncludeMemo(binding.checkIncludeAddress.isChecked)
        }

        binding.inputZcashAddress.apply {
            doAfterTextChanged {
                val textStr = text.toString()
                val trim = textStr.trim()
                // bugfix: prevent cursor from moving while backspacing and deleting whitespace
                if (text.toString() != trim) {
                    setText(trim)
                    setSelection(selectionEnd - (textStr.length - trim.length))
                }
                onAddressChanged(trim)
            }
        }

        binding.backButtonHitArea.onClickNavUp { tapped(SEND_ADDRESS_BACK) }
//
//        binding.clearMemo.setOnClickListener {
//            onClearMemo().also { tapped(SEND_MEMO_CLEAR) }
//        }

        binding.inputZcashMemo.doAfterTextChanged {
            sendViewModel.memo = binding.inputZcashMemo.text?.toString() ?: ""
            onMemoUpdated()
        }

        binding.textLayoutAddress.setEndIconOnClickListener {
            mainActivity?.maybeOpenScan().also { tapped(SEND_ADDRESS_SCAN) }
        }

        // banners

        binding.backgroundClipboard.setOnClickListener {
            onPaste().also { tapped(SEND_ADDRESS_PASTE) }
        }
        binding.containerClipboard.setOnClickListener {
            onPaste().also { tapped(SEND_ADDRESS_PASTE) }
        }
        binding.backgroundLastUsed.setOnClickListener {
            onReuse().also { tapped(SEND_ADDRESS_REUSE) }
        }
        binding.containerLastUsed.setOnClickListener {
            onReuse().also { tapped(SEND_ADDRESS_REUSE) }
        }
    }

    private fun applyViewModel(model: SendViewModel) {
        // apply amount
        val roundedAmount =
            WalletZecFormmatter.toZecStringFull(model.zatoshiAmount.coerceAtLeast(0L))
        binding.textSendAmount.text = "\$$roundedAmount"
        // apply address
        binding.inputZcashAddress.setText(model.toAddress)
        // apply memo
        binding.inputZcashMemo.setText(model.memo)
        binding.checkIncludeAddress.isChecked = model.includeFromAddress
        onMemoUpdated()
    }

    private fun onMemoUpdated() {
        val totalLength = sendViewModel.createMemoToSend().length
        binding.textLayoutMemo.helperText = "$totalLength/${ZcashSdk.MAX_MEMO_SIZE} ${getString(R.string.send_memo_chars_abbreviation)}"
        val color = if (totalLength > ZcashSdk.MAX_MEMO_SIZE) R.color.zcashRed else R.color.text_light_dimmed
        binding.textLayoutMemo.setHelperTextColor(ColorStateList.valueOf(color.toAppColor()))
    }

    private fun onClearMemo() {
        binding.inputZcashMemo.setText("")
    }

    private fun onIncludeMemo(checked: Boolean) {
        sendViewModel.afterInitFromAddress {
            sendViewModel.includeFromAddress = checked
            onMemoUpdated()
            tapped(if (checked) SEND_MEMO_INCLUDE else SEND_MEMO_EXCLUDE)
        }
    }

    private fun onAddressChanged(address: String) {
        lifecycleScope.launchWhenResumed {
            val validation = sendViewModel.validateAddress(address)
            binding.buttonSend.isActivated = !validation.isNotValid
            var type = when (validation) {
                is AddressType.Transparent -> R.string.send_validation_address_valid_taddr to R.color.zcashGreen
                is AddressType.Shielded -> R.string.send_validation_address_valid_zaddr to R.color.zcashGreen
                else -> R.string.send_validation_address_invalid to R.color.zcashRed
            }
            updateAddressUi(validation is AddressType.Transparent)
            if (address == sendViewModel.synchronizer.getAddress() || address == sendViewModel.synchronizer.getTransparentAddress()) {
                type = R.string.send_validation_address_self to R.color.zcashRed
            }
            binding.textLayoutAddress.helperText = getString(type.first)
            binding.textLayoutAddress.setHelperTextColor(ColorStateList.valueOf(type.second.toAppColor()))

            // if we have the clipboard address but we're changing it, then clear the selection
            if (binding.imageClipboardAddressSelected.isVisible) {
                loadAddressFromClipboard().let { clipboardAddress ->
                    if (address != clipboardAddress) {
                        updateClipboardBanner(clipboardAddress, false)
                    }
                }
            }
            // if we have the last used address but we're changing it, then clear the selection
            if (binding.imageLastUsedAddressSelected.isVisible) {
                loadLastUsedAddress().let { lastAddress ->
                    if (address != lastAddress) {
                        updateLastUsedBanner(lastAddress, false)
                    }
                }
            }
        }
    }

    /**
     * To hide input Memo and reply-to option for T type address and show a info message about memo option availability */
    private fun updateAddressUi(isMemoHidden: Boolean) {
        if (isMemoHidden) {
            binding.textLayoutMemo.gone()
            binding.checkIncludeAddress.gone()
            binding.textNoZAddress.visible()
        } else {
            binding.textLayoutMemo.visible()
            binding.checkIncludeAddress.visible()
            binding.textNoZAddress.gone()
        }
    }

    private fun onSubmit(unused: EditText? = null) {
        sendViewModel.toAddress = binding.inputZcashAddress.text.toString()
        sendViewModel.validate(requireContext(), availableZatoshi, maxZatoshi).onFirstWith(resumedScope) { errorMessage ->
            if (errorMessage == null) {
                val symbol = getString(R.string.symbol)
                mainActivity?.authenticate("${getString(R.string.send_confirmation_prompt)}\n${WalletZecFormmatter.toZecStringFull(sendViewModel.zatoshiAmount)} $symbol ${getString(R.string.send_final_to)}\n${sendViewModel.toAddress.toAbbreviatedAddress()}") {
//                    sendViewModel.funnel(Send.AddressPageComplete)
                    mainActivity?.safeNavigate(R.id.action_nav_send_to_nav_send_final)
                }
            } else {
                resumedScope.launch {
                    binding.textAddressError.text = errorMessage
                    delay(2500L)
                    binding.textAddressError.text = ""
                }
            }
        }
    }

    private fun onMax() {
        if (maxZatoshi != null) {
//            binding.inputZcashAmount.apply {
//                setText(WalletZecFormmatter.toZecStringFull(maxZatoshi))
//                postDelayed({
//                    requestFocus()
//                    setSelection(text?.length ?: 0)
//                }, 10L)
//            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity?.clipboard?.addPrimaryClipChangedListener(this)
    }

    override fun onDetach() {
        super.onDetach()
        mainActivity?.clipboard?.removePrimaryClipChangedListener(this)
    }

    override fun onResume() {
        super.onResume()
        onPrimaryClipChanged()
        sendViewModel.synchronizer.balances.collectWith(resumedScope) {
            onBalanceUpdated(it)
        }
        binding.inputZcashAddress.text.toString().let {
            if (!it.isNullOrEmpty()) onAddressChanged(it)
        }
    }

    private fun onBalanceUpdated(balance: WalletBalance) {
//        binding.textLayoutAmount.helperText =
//            "You have ${WalletZecFormmatter.toZecStringFull(balance.availableZatoshi.coerceAtLeast(0L))} available"
        maxZatoshi = (balance.availableZatoshi - ZcashSdk.MINERS_FEE_ZATOSHI).coerceAtLeast(0L)
        availableZatoshi = balance.availableZatoshi
    }

    override fun onPrimaryClipChanged() {
        resumedScope.launch {
            updateClipboardBanner(loadAddressFromClipboard())
            updateLastUsedBanner(loadLastUsedAddress())
        }
    }

    private fun updateClipboardBanner(address: String?, selected: Boolean = false) {
        binding.apply {
            updateAddressBanner(
                groupClipboard,
                clipboardAddress,
                imageClipboardAddressSelected,
                imageShield,
                clipboardAddressLabel,
                selected,
                address
            )
        }
    }

    private suspend fun updateLastUsedBanner(
        address: String? = null,
        selected: Boolean = false
    ) {
        val isBoth = address == loadAddressFromClipboard()
        binding.apply {
            updateAddressBanner(
                groupLastUsed,
                lastUsedAddress,
                imageLastUsedAddressSelected,
                imageLastUsedShield,
                lastUsedAddressLabel,
                selected,
                address.takeUnless { isBoth }
            )
        }
        binding.dividerClipboard.setText(if (isBoth) R.string.send_history_last_and_clipboard else R.string.send_history_clipboard)
    }

    private fun updateAddressBanner(
        group: Group,
        addressTextView: TextView,
        checkIcon: ImageView,
        shieldIcon: ImageView,
        addressLabel: TextView,
        selected: Boolean = false,
        address: String? = null
    ) {
        resumedScope.launch {
            if (address == null) {
                group.gone()
            } else {
                val userShieldedAddr = sendViewModel.synchronizer.getAddress()
                val userTransparentAddr = sendViewModel.synchronizer.getTransparentAddress()
                group.visible()
                addressTextView.text = address.toAbbreviatedAddress(16, 16)
                checkIcon.goneIf(!selected)
                ImageViewCompat.setImageTintList(shieldIcon, ColorStateList.valueOf(if (selected) R.color.colorPrimary.toAppColor() else R.color.zcashWhite_12.toAppColor()))
                addressLabel.setText(if (address == userShieldedAddr) R.string.send_banner_address_user else R.string.send_banner_address_unknown)
                if (address == userTransparentAddr) addressLabel.setText("Your Auto-Shielding Address")
                addressLabel.setTextColor(if (selected) R.color.colorPrimary.toAppColor() else R.color.text_light.toAppColor())
                addressTextView.setTextColor(if (selected) R.color.text_light.toAppColor() else R.color.text_light_dimmed.toAppColor())
            }
        }
    }

    private fun onPaste() {
        mainActivity?.clipboard?.let { clipboard ->
            if (clipboard.hasPrimaryClip()) {
                val address = clipboard.text().toString()
                val applyValue = binding.imageClipboardAddressSelected.isGone
                updateClipboardBanner(address, applyValue)
                binding.inputZcashAddress.setText(address.takeUnless { !applyValue })
            }
        }
    }

    private fun onReuse() {
        sendViewModel.viewModelScope.launch {
            val address = loadLastUsedAddress()
            val applyValue = binding.imageLastUsedAddressSelected.isGone
            updateLastUsedBanner(address, applyValue)
            binding.inputZcashAddress.setText(address.takeUnless { !applyValue })
        }
    }

    private suspend fun loadAddressFromClipboard(): String? {
        mainActivity?.clipboard?.apply {
            if (hasPrimaryClip()) {
                text().toString().let { text ->
                    if (sendViewModel.isValidAddress(text)) return@loadAddressFromClipboard text
                }
            }
        }
        return null
    }

    private var lastUsedAddress: String? = null
    private suspend fun loadLastUsedAddress(): String? {
        if (lastUsedAddress == null) {
            lastUsedAddress = sendViewModel.synchronizer.sentTransactions.first().firstOrNull { !it.toAddress.isNullOrEmpty() }?.toAddress
            updateLastUsedBanner(lastUsedAddress, binding.imageLastUsedAddressSelected.isVisible)
        }
        return lastUsedAddress
    }

    private fun ClipboardManager.text(): CharSequence =
        primaryClip!!.getItemAt(0).coerceToText(mainActivity)
}
