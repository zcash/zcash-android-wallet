package cash.z.ecc.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentSendConfirmBinding
import cash.z.ecc.android.di.viewmodel.activityViewModel
import cash.z.ecc.android.ext.WalletZecFormmatter
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.ext.onClickNavTo
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Funnel.Send
import cash.z.ecc.android.feedback.Report.Tap.*
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import kotlinx.coroutines.launch

class SendConfirmFragment : BaseFragment<FragmentSendConfirmBinding>() {
    override val screen = Report.Screen.SEND_CONFIRM

    val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendConfirmBinding =
        FragmentSendConfirmBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonNext.setOnClickListener {
            onSend().also { tapped(SEND_CONFIRM_NEXT) }
        }
//        R.id.action_nav_send_confirm_to_nav_send_memo.let {
//            binding.backButtonHitArea.onClickNavTo(it) { tapped(SEND_CONFIRM_BACK) }
//            onBackPressNavTo(it) { tapped(SEND_CONFIRM_BACK) }
//        }
        mainActivity?.lifecycleScope?.launch {
            binding.textConfirmation.text =
                "Send ${WalletZecFormmatter.toZecStringFull(sendViewModel.zatoshiAmount)} ZEC to ${sendViewModel?.toAddress.toAbbreviatedAddress()}?"
        }
        sendViewModel.memo.trim().isNotEmpty().let { hasMemo ->
            binding.radioIncludeAddress.isChecked = hasMemo || sendViewModel.includeFromAddress
            binding.radioIncludeAddress.goneIf(!(hasMemo || sendViewModel.includeFromAddress))
        }
    }

    private fun onSend() {
        sendViewModel.funnel(Send.ConfirmPageComplete)
//        mainActivity?.safeNavigate(R.id.action_nav_send_confirm_to_send_final)
    }
}