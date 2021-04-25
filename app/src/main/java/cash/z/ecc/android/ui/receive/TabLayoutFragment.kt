package cash.z.ecc.android.ui.receive

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentTabLayoutBinding
import cash.z.ecc.android.ui.base.BaseFragment
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_tab_layout.*

abstract class TabLayoutFragment: BaseFragment<FragmentTabLayoutBinding>() {

    lateinit var tabLayout: TabLayout
    lateinit var viewPager: ViewPager


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tabLayout.addTab(tabLayout.newTab().setText("Football"))
        tabLayout.addTab(tabLayout.newTab().setText("Cricket"))
        tabLayout.addTab(tabLayout.newTab().setText("NBA"))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        val adapter = fragmentManager?.let {
            ViewPagerAdapter(requireContext(), it,
                tabLayout.tabCount)
        }
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}