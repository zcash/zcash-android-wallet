package cash.z.ecc.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.databinding.FragmentBalanceDetailBinding
import cash.z.ecc.android.ext.onClickNavBack
import cash.z.ecc.android.feedback.Report.Tap.RECEIVE_BACK
import cash.z.ecc.android.ui.base.BaseFragment

class BalanceDetailFragment : BaseFragment<FragmentBalanceDetailBinding>() {

    override fun inflate(inflater: LayoutInflater): FragmentBalanceDetailBinding =
        FragmentBalanceDetailBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack() { tapped(RECEIVE_BACK) }
    }
}
