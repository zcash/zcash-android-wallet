package cash.z.ecc.android.ui.tab_layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.viewpager.widget.ViewPager
import cash.z.ecc.android.databinding.FragmentTabLayoutBinding
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.profile.AwesomeFragment
import cash.z.ecc.android.ui.receive.ReceiveFragment
import com.google.android.material.tabs.TabLayout

class TabLayoutFragment: BaseFragment<FragmentTabLayoutBinding>() {

    lateinit var tabLayout: TabLayout
    lateinit var viewPager: ViewPager

    override fun inflate(inflater: LayoutInflater): FragmentTabLayoutBinding =
        FragmentTabLayoutBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabLayout = TabLayout(requireContext())
        viewPager = ViewPager(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter = ViewPagerAdapter(activity?.supportFragmentManager)
        adapter.addFrag(AwesomeFragment(), "Awesome")
        adapter.addFrag(ReceiveFragment(), "Receive")

        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager);

    }
}