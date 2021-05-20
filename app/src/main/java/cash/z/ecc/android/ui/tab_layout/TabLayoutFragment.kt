package cash.z.ecc.android.ui.tab_layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentTabLayoutBinding
import cash.z.ecc.android.ext.onClickNavBack
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.profile.AwesomeFragment
import cash.z.ecc.android.ui.receive.ReceiveFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TabLayoutFragment :
    BaseFragment<FragmentTabLayoutBinding>(),
    FragmentCreator,
    TabLayout.OnTabSelectedListener {

    override fun inflate(inflater: LayoutInflater): FragmentTabLayoutBinding =
        FragmentTabLayoutBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack { tapped(Report.Tap.RECEIVE_BACK) }
        binding.viewPager.adapter = ViewPagerAdapter(this, this)
        binding.tabLayout.addOnTabSelectedListener(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Shielded" else "Transparent"
        }.attach()
    }

    //
    // TabLayout.OnTabSelectedListener implementation
    //

    override fun onTabSelected(tab: TabLayout.Tab) {
        when (tab.position) {
            0 -> setSelectedTab(R.color.zcashYellow)
            1 -> setSelectedTab(R.color.zcashBlueDark)
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {}

    override fun onTabReselected(tab: TabLayout.Tab) {}

    private fun setSelectedTab(@ColorRes color: Int) {
        binding.tabLayout.setSelectedTabIndicatorColor(
            ContextCompat.getColor(
                requireContext(),
                color
            )
        )
        binding.tabLayout.setTabTextColors(
            ContextCompat.getColor(
                requireContext(),
                R.color.unselected_tab_grey
            ),
            ContextCompat.getColor(requireContext(), color)
        )
    }

    //
    // FragmentCreator implementation
    //

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ReceiveFragment()
            1 -> AwesomeFragment()
            else -> throw IndexOutOfBoundsException("Cannot create a fragment for index $position")
        }
    }

    override fun getItemCount() = 2
}
