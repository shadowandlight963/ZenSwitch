package com.example.ourmajor

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.util.Log
import android.widget.Toast
import android.view.View
import android.view.animation.OvershootInterpolator
import android.animation.ValueAnimator
import android.animation.AnimatorSet
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.content.ContextCompat
import com.example.ourmajor.auth.AuthActivity
import com.example.ourmajor.auth.AuthViewModel
import com.example.ourmajor.databinding.ActivityMainBinding
import com.example.ourmajor.engine.PermissionManager
import com.example.ourmajor.ui.adapter.MainPagerAdapter
import com.example.ourmajor.ui.transformer.SwipePageTransformer
import com.example.ourmajor.R
import com.example.ourmajor.ui.theme.ThemeUtils

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var pagerAdapter: MainPagerAdapter
    private var currentTabIndex = 0
    
    // Public getter for binding access from fragments
    fun getMainBinding(): ActivityMainBinding = binding

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            ThemeUtils.applyTheme(this)

            super.onCreate(savedInstanceState)

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Setup ViewPager2
            setupViewPager()

            // Setup Bottom Navigation
            setupBottomNavigation()

            // Setup Auth ViewModel
            authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
            observeAuthState()
            setupWindowInsets()
            
            // Setup back press handling
            setupBackPressed()
        } catch (e: Exception) {
            Log.e("MainActivity", "Startup Error", e)
            runCatching {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.error_startup_generic),
                    Toast.LENGTH_LONG
                ).show()
            }
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        PermissionManager.checkAndRequestPermissions(this)
    }

    /**
     * Setup ViewPager2 with adapter, page transformer, and performance optimizations
     */
    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        // Performance optimizations
        binding.viewPager.offscreenPageLimit = 1 // Keep only adjacent page ready
        binding.viewPager.setPageTransformer(SwipePageTransformer())
        
        // Disable RecyclerView item animator for smoother swipes
        (binding.viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.let { recyclerView ->
            recyclerView.itemAnimator = null
        }
        
        // Set default page (Home)
        binding.viewPager.setCurrentItem(0, false)
        
        // Register page change callback to sync with bottom navigation
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Update current tab index
                currentTabIndex = position
                
                // Update navigation UI - ONLY place where selection updates happen
                updateNavigationSelection(position)
            }
        })
    }

    /**
     * Setup Custom Navigation Layout with click listeners
     */
    private fun setupBottomNavigation() {
        // Home navigation
        binding.navHome.setOnClickListener {
            navigateToTab(0)
        }
        
        // Activities navigation
        binding.navActivities.setOnClickListener {
            navigateToTab(1)
        }
        
        // Calendar navigation
        binding.navCalendar.setOnClickListener {
            navigateToTab(2)
        }
        
        // Rewards navigation
        binding.navRewards.setOnClickListener {
            navigateToTab(3)
        }
        
        // Profile navigation
        binding.navProfile.setOnClickListener {
            navigateToTab(4)
        }
        
        // Initialize current tab index from ViewPager
        currentTabIndex = binding.viewPager.currentItem
        
        // Set initial selection UI
        updateNavigationSelection(currentTabIndex)
    }
    
    /**
     * Navigate to specific tab - ONLY triggers ViewPager
     */
    private fun navigateToTab(targetIndex: Int) {
        if (currentTabIndex != targetIndex) {
            binding.viewPager.setCurrentItem(targetIndex, false)
        }
        // DO NOT update UI here - ViewPager callback handles it
    }

    /**
     * Update navigation selection UI with icon micro-interactions - ONLY place where selection updates happen
     */
    private fun updateNavigationSelection(position: Int) {
        // Reset all items to unselected state immediately
        resetNavigationItems()
        
        // Animate selected icon with micro-interactions
        animateSelectedIcon(position)
        
        // Set selected item colors (labels update instantly)
        when (position) {
            0 -> {
                binding.navHomeLabel.setTextColor(getSelectedColor())
            }
            1 -> {
                binding.navActivitiesLabel.setTextColor(getSelectedColor())
            }
            2 -> {
                binding.navCalendarLabel.setTextColor(getSelectedColor())
            }
            3 -> {
                binding.navRewardsLabel.setTextColor(getSelectedColor())
            }
            4 -> {
                binding.navProfileLabel.setTextColor(getSelectedColor())
            }
        }
    }
    
    /**
     * Animate selected icon with scale and color transitions
     */
    private fun animateSelectedIcon(position: Int) {
        val selectedIcon = when (position) {
            0 -> binding.navHomeIcon
            1 -> binding.navActivitiesIcon
            2 -> binding.navCalendarIcon
            3 -> binding.navRewardsIcon
            4 -> binding.navProfileIcon
            else -> null
        }
        
        selectedIcon?.let { icon ->
            // Create scale animation (pop effect)
            val scaleX = ValueAnimator.ofFloat(1.0f, 1.12f, 1.0f)
            scaleX.duration = 140
            scaleX.interpolator = OvershootInterpolator(1.2f)
            scaleX.addUpdateListener { animator ->
                icon.scaleX = animator.animatedValue as Float
            }
            
            val scaleY = ValueAnimator.ofFloat(1.0f, 1.12f, 1.0f)
            scaleY.duration = 140
            scaleY.interpolator = OvershootInterpolator(1.2f)
            scaleY.addUpdateListener { animator ->
                icon.scaleY = animator.animatedValue as Float
            }
            
            // Create color transition animation
            val colorAnimator = ValueAnimator.ofArgb(getUnselectedColor(), getSelectedColor())
            colorAnimator.duration = 150
            colorAnimator.addUpdateListener { animator ->
                icon.setColorFilter(animator.animatedValue as Int)
            }
            
            // Play animations together
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(scaleX, scaleY, colorAnimator)
            animatorSet.start()
        }
    }
    
    /**
     * Reset all navigation items to unselected state (safe reset system)
     */
    private fun resetNavigationItems() {
        val unselectedColor = getUnselectedColor()
        
        // Reset Home
        binding.navHomeIcon.setColorFilter(unselectedColor)
        binding.navHomeIcon.scaleX = 1.0f
        binding.navHomeIcon.scaleY = 1.0f
        binding.navHomeLabel.setTextColor(unselectedColor)
        
        // Reset Activities
        binding.navActivitiesIcon.setColorFilter(unselectedColor)
        binding.navActivitiesIcon.scaleX = 1.0f
        binding.navActivitiesIcon.scaleY = 1.0f
        binding.navActivitiesLabel.setTextColor(unselectedColor)
        
        // Reset Calendar
        binding.navCalendarIcon.setColorFilter(unselectedColor)
        binding.navCalendarIcon.scaleX = 1.0f
        binding.navCalendarIcon.scaleY = 1.0f
        binding.navCalendarLabel.setTextColor(unselectedColor)
        
        // Reset Rewards
        binding.navRewardsIcon.setColorFilter(unselectedColor)
        binding.navRewardsIcon.scaleX = 1.0f
        binding.navRewardsIcon.scaleY = 1.0f
        binding.navRewardsLabel.setTextColor(unselectedColor)
        
        // Reset Profile
        binding.navProfileIcon.setColorFilter(unselectedColor)
        binding.navProfileIcon.scaleX = 1.0f
        binding.navProfileIcon.scaleY = 1.0f
        binding.navProfileLabel.setTextColor(unselectedColor)
    }
    
    /**
     * Get selected color (theme primary color)
     */
    private fun getSelectedColor(): Int {
        return TypedValue().let { typedValue ->
            theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            ContextCompat.getColor(this, typedValue.resourceId)
        }
    }
    
    /**
     * Get unselected color (theme-aware)
     */
    private fun getUnselectedColor(): Int {
        return TypedValue().let { typedValue ->
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
            ContextCompat.getColor(this, typedValue.resourceId)
        }
    }

    /**
     * Setup back press handling for ViewPager2 navigation
     */
    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentItem = binding.viewPager.currentItem
                if (currentItem != 0) {
                    // Navigate to Home if not already there
                    binding.viewPager.setCurrentItem(0, true)
                } else {
                    // Exit app if on Home screen
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.floatingNavContainer) { view, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(
                bottom = navInsets.bottom + resources.getDimensionPixelSize(R.dimen.space_2)
            )
            insets
        }
    }

    private fun observeAuthState() {
        authViewModel.uiStateLiveData.observe(this) { state ->
            if (!state.isAuthenticated) {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        }
    }
}