package cash.z.ecc.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentSendMemoBinding
import cash.z.ecc.android.di.viewmodel.activityViewModel
import cash.z.ecc.android.ext.gone
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.ext.onEditorActionDone
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Funnel.Send
import cash.z.ecc.android.feedback.Report.Tap.*
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.util.INCLUDE_MEMO_PREFIX_STANDARD

class SendMemoFragment : BaseFragment<FragmentSendMemoBinding>() {
    override val screen = Report.Screen.SEND_MEMO

    val sendViewModel: SendViewModel by activityViewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSendMemoBinding =
        FragmentSendMemoBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonNext.setOnClickListener {
            onTopButton().also { tapped(SEND_MEMO_NEXT) }
        }
        binding.buttonSkip.setOnClickListener {
            onBottomButton().also { tapped(SEND_MEMO_SKIP) }
        }
        binding.clearMemo.setOnClickListener {
            onClearMemo().also { tapped(SEND_MEMO_CLEAR) }
        }

//        R.id.action_nav_send_memo_to_nav_send_address.let {
//            binding.backButtonHitArea.onClickNavTo(it) { tapped(SEND_MEMO_BACK) }
//            onBackPressNavTo(it) { tapped(SEND_MEMO_BACK) }
//        }

        binding.checkIncludeAddress.setOnCheckedChangeListener { _, _->
            onIncludeMemo(binding.checkIncludeAddress.isChecked)
        }

        binding.inputMemo.let { memo ->
            memo.onEditorActionDone {
                onTopButton().also { tapped(SEND_MEMO_NEXT) }
            }
            memo.doAfterTextChanged { 
                binding.clearMemo.goneIf(memo.text.isEmpty())
            }
        }

        sendViewModel.afterInitFromAddress {
            binding.textIncludedAddress.text = "$INCLUDE_MEMO_PREFIX_STANDARD ${sendViewModel.fromAddress}"
        }

        binding.textIncludedAddress.gone()

        applyModel()
    }

    private fun onClearMemo() {
        binding.inputMemo.setText("")
    }

    private fun applyModel() {
        sendViewModel.isShielded.let { isShielded ->
            binding.groupShielded.goneIf(!isShielded)
            binding.groupTransparent.goneIf(isShielded)
            binding.textIncludedAddress.goneIf(!sendViewModel.includeFromAddress)
            if (isShielded) {
                binding.inputMemo.setText(sendViewModel.memo)
                binding.checkIncludeAddress.isChecked = sendViewModel.includeFromAddress
                binding.buttonNext.text = "ADD MEMO"
                binding.buttonSkip.text = "OMIT MEMO"
            } else {
                binding.buttonNext.text = "GO BACK"
                binding.buttonSkip.text = "PROCEED"
            }
        }
    }

    private fun onIncludeMemo(checked: Boolean) {

        binding.textIncludedAddress.goneIf(!checked)
        sendViewModel.includeFromAddress = checked
        binding.textInfoShielded.text = if (checked) {
            tapped(SEND_MEMO_INCLUDE)
            getString(R.string.send_memo_included_message)
        } else {
            tapped(SEND_MEMO_EXCLUDE)
            getString(R.string.send_memo_excluded_message)
        }
    }

    private fun onTopButton() {
        if (sendViewModel.isShielded) {
            sendViewModel.memo = binding.inputMemo.text.toString()
            onNext()
        } else {
//            mainActivity?.safeNavigate(R.id.action_nav_send_memo_to_nav_send_address)
        }
    }

    private fun onBottomButton() {
        binding.inputMemo.setText("")
        sendViewModel.memo = ""
        sendViewModel.includeFromAddress = false
        onNext()
    }

    private fun onNext() {
        sendViewModel.funnel(Send.MemoPageComplete)
//        mainActivity?.safeNavigate(R.id.action_nav_send_memo_to_send_confirm)
    }
}