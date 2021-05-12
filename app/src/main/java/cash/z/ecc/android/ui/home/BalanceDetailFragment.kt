package cash.z.ecc.android.ui.home

import android.view.LayoutInflater
import cash.z.ecc.android.databinding.FragmentBalanceDetailBinding
import cash.z.ecc.android.ui.base.BaseFragment

class BalanceDetailFragment : BaseFragment<FragmentBalanceDetailBinding>() {

    override fun inflate(inflater: LayoutInflater): FragmentBalanceDetailBinding =
        FragmentBalanceDetailBinding.inflate(inflater)
}
