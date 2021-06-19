package cash.z.ecc.android.ui.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentFundsAvailableBinding
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.ui.base.BaseFragment

class FundsAvailableFragment : BaseFragment<FragmentFundsAvailableBinding>() {
    override val screen = Report.Screen.AUTO_SHIELD_AVAILABLE

    override fun inflate(inflater: LayoutInflater): FragmentFundsAvailableBinding =
        FragmentFundsAvailableBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonAction.setOnClickListener {
            onProceedWithAutoshielding()
        }
    }

    /**
     * This function probably serves no purpose other than to click through to the next screen
     */
    private fun onProceedWithAutoshielding() {
        mainActivity?.let { main ->
            main.authenticate(
                "Shield transparent funds",
                getString(R.string.biometric_backup_phrase_title)
            ) {
                main.safeNavigate(R.id.action_nav_funds_available_to_nav_shield_final)
            }
        }
    }
}
