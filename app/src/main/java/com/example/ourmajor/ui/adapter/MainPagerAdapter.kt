package com.example.ourmajor.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.ourmajor.R
import com.example.ourmajor.ui.activities.ActivitiesFragment
import com.example.ourmajor.ui.calendar.CalendarFragment
import com.example.ourmajor.ui.home.HomeFragment
import com.example.ourmajor.ui.profile.ProfileFragment
import com.example.ourmajor.ui.rewards.RewardsFragment

/**
 * MainPagerAdapter for ViewPager2 with FragmentStateAdapter.
 * 
 * Provides swipe navigation between main app fragments with proper lifecycle management.
 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        private const val NUM_FRAGMENTS = 5
    }

    override fun getItemCount(): Int = NUM_FRAGMENTS

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> ActivitiesFragment()
            2 -> CalendarFragment()
            3 -> RewardsFragment()
            4 -> ProfileFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    /**
     * Get fragment title for potential tab usage
     */
    fun getFragmentTitle(position: Int): String {
        return when (position) {
            0 -> "Home"
            1 -> "Activities"
            2 -> "Calendar"
            3 -> "Rewards"
            4 -> "Profile"
            else -> "Unknown"
        }
    }
}
