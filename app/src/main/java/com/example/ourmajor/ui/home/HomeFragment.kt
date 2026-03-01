package com.example.ourmajor.ui.home

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.ourmajor.MainActivity
import kotlinx.coroutines.withContext
import kotlinx.coroutines.MainScope
import com.example.ourmajor.R
import com.google.android.material.R as MaterialR
import com.example.ourmajor.data.history.SessionHistoryRepository
import com.example.ourmajor.databinding.FragmentHomeBinding
import com.example.ourmajor.focusguard.FocusGuardManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.ourmajor.ui.activities.SmartShuffleViewModelNew
import com.example.ourmajor.ui.activities.SmartShuffleViewModelNewFactory
import com.example.ourmajor.engine.PermissionManager
import com.example.ourmajor.gamification.GamificationManager
import com.example.ourmajor.ui.activities.ActivitiesViewModel
import com.example.ourmajor.ui.breathing.BoxBreathingActivity
import com.example.ourmajor.ui.breathing.ResonantBreathActivity
import com.example.ourmajor.ui.theme.ThemeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var smartShuffleViewModel: SmartShuffleViewModelNew

    // "Psycho Mode" Quotes: These change every time the user opens the app
    private val quotes = listOf(
        "Ready for another mindful day?",
        "Inhale the future, exhale the past.",
        "Peace comes from within.",
        "One step at a time.",
        "Focus on the present moment.",
        "Your calm is your power.",
        "Silence is a source of great strength."
    )

    private val zenQuotes = listOf(
        "The journey of a thousand miles begins with a single step.",
        "Act without expectation.",
        "Wherever you are, be there totally.",
        "Small steps, taken daily, become big changes.",
        "In the stillness, answers arrive.",
        "Breathe in peace. Breathe out worry.",
        "Let go, or be dragged.",
        "A calm mind brings inner strength.",
        "What you seek is seeking you.",
        "Begin again—gently."
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, _savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(_view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(_view, _savedInstanceState)

        applyThemeHeaderBackground()

        // 1. Set Random Quote
        binding.homeTitle.text = "Welcome back!"
        binding.homeSubtitle.text = quotes.random()

        // 2. Prepare Animations (Move everything off-screen or hide it)
        prepareEntryAnimation()

        // 3. Setup Lists & Click Listeners
        setupRecentActivities()
        setupClickListeners()
        setupFocusGuardCard()
        setupSwipeRefresh()
        
        // 4. Setup WindowInsets for status bar collision
        setupWindowInsets()
        
        // 5. Initialize Fragment-scoped Smart Shuffle ViewModel
        smartShuffleViewModel = SmartShuffleViewModelNewFactory().create(SmartShuffleViewModelNew::class.java)
        
        
        // 4. Connect ViewModel & Observe Data
        val appContext = requireContext().applicationContext
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return HomeViewModel(
                        SessionHistoryRepository(appContext),
                        GamificationManager.getInstance(appContext)
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        val vm = ViewModelProvider(this, factory)[HomeViewModel::class.java]
        val recentAdapter = binding.recentActivitiesList.adapter as? RecentActivitiesAdapter

        vm.uiState.observe(viewLifecycleOwner) { state ->
            val goal = state.todayGoalMinutes.coerceAtLeast(1)

            // ANIMATION UPGRADE: Spin the circle and count the numbers
            animateProgress(state.todayMinutes, goal)

            binding.tvDailyGoalStatus.text = "Goal: ${state.todayMinutes} / ${goal} mins"
            val count = state.todaySessionsCount.coerceAtLeast(0)
            binding.chipTodayCount.text = if (count == 1) "1 Activity" else "${count} Activities"

            binding.weekMinutesValue.text = formatMinutes(state.weekMinutes)
            binding.weekSessionsValue.text = state.weekSessions.toString()

            recentAdapter?.submitList(state.recentActivities)

            val empty = state.recentActivities.isEmpty()
            binding.recentActivitiesList.visibility = if (empty) View.GONE else View.VISIBLE
            binding.quoteCard.visibility = if (empty) View.VISIBLE else View.GONE
            if (empty) {
                binding.quoteText.text = zenQuotes.random()
            }
        }

        // 5. Trigger the Entrance (The "Psycho" Slide)
        runEntryAnimation()
    }

    override fun onResume() {
        super.onResume()
        applyThemeHeaderBackground()
        // Refresh Focus Guard card from repo so IG/YT stats update when user returns
        syncFocusGuardUI()
    }

    private fun applyThemeHeaderBackground() {
        val theme = ThemeUtils.currentTheme(requireContext())
        _binding?.headerImage?.setImageResource(theme.backgroundImageRes)
    }

    // --- HELPER FUNCTIONS ---

    private fun formatMinutes(totalMinutes: Int): String {
        val m = totalMinutes.coerceAtLeast(0)
        val h = m / 60
        val rem = m % 60
        return if (h > 0) "${h}h ${rem}m" else "${m}m"
    }

    private fun setupRecentActivities() {
        val adapter = RecentActivitiesAdapter()
        binding.recentActivitiesList.layoutManager = LinearLayoutManager(requireContext())
        binding.recentActivitiesList.adapter = adapter
    }

    private fun setupClickListeners() {
        val activitiesVm = ViewModelProvider(requireActivity())[ActivitiesViewModel::class.java]

        binding.featuredCardMorning.setOnClickListener {
            activitiesVm.selectMainCategory("stretching")
            navigateToActivities()
        }

        binding.featuredCardBreathing.setOnClickListener {
            BoxBreathingActivity.start(requireContext(), 2)
        }

        binding.featuredCardSleep.setOnClickListener {
            ResonantBreathActivity.start(requireContext(), 1)
        }

        binding.startButton.setOnClickListener {
            Log.d("HOME_DEBUG", "Start button clicked")
            
            // Disable button to prevent multiple rapid clicks
            binding.startButton.isEnabled = false
            
            try {
                // Get next activity class from fragment-scoped ViewModel
                val nextActivityClass = smartShuffleViewModel.getNextActivity()
                Log.d("HOME_DEBUG", "Launching activity: ${nextActivityClass.simpleName}")
                
                // Standardized launch using Intent
                val intent = android.content.Intent(requireActivity(), nextActivityClass)
                startActivity(intent)
                
                // Re-enable button after 500ms
                binding.startButton.postDelayed({
                    binding.startButton.isEnabled = true
                }, 500)
                
            } catch (e: Exception) {
                Log.e("HOME_DEBUG", "Failed to launch activity", e)
                // Re-enable button on error
                binding.startButton.isEnabled = true
            }
        }
    }

    private fun setupFocusGuardCard() {
        val cardBinding = binding.focusGuardCardInclude

        // Listener 1: Pill = Power button (quick-toggle)
        cardBinding.focusGuardPill.setOnClickListener {
            if (FocusGuardManager.getDailyLimit(requireContext()) > 0) {
                FocusPauseBottomSheet().apply {
                    onDismissed = { syncFocusGuardUI() }
                }.show(childFragmentManager, "focus_pause")
            } else {
                // First-time setup: open config sheet
                FocusConfigBottomSheet().apply {
                    onDismissed = { syncFocusGuardUI() }
                }.show(childFragmentManager, "focus_config")
            }
        }

        // Listener 2: Card body = Focus Portal (setup / limit / stats)
        cardBinding.root.setOnClickListener {
            FocusConfigBottomSheet().apply {
                onDismissed = { syncFocusGuardUI() }
            }.show(childFragmentManager, "focus_config")
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.hero) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(
                top = statusBarInsets.top
            )
            insets
        }
    }

    private fun setupSwipeRefresh() {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(MaterialR.attr.colorPrimary, typedValue, true)
        binding.swipeRefresh.setColorSchemeColors(typedValue.data)
        binding.swipeRefresh.setOnRefreshListener {
            syncFocusGuardUI()
            _binding?.swipeRefresh?.isRefreshing = false
        }
    }

    /** Force re-calculation of Focus Guard usage and update UI; call from pull-to-refresh. */
    private fun refreshStats() {
        syncFocusGuardUI()
    }

    /** Syncs pill (ON/OFF), subtitle, and stats with FocusGuardManager. Call from onResume and after any toggle. */
    private fun syncFocusGuardUI() {
        // Ensure fragment is attached and view is available
        if (!isAdded || _binding == null) return
        
        val cardBinding = binding.focusGuardCardInclude
        val pill = cardBinding.focusGuardPill
        val subtitle = cardBinding.focusGuardSubtitle
        val instagramMins = cardBinding.focusStatsInstagram
        val youtubeMins = cardBinding.focusStatsYoutube
        
        try {
            // Check permission first
            if (!PermissionManager.hasUsageStatsPermission(requireContext())) {
                pill.text = getString(R.string.focus_guard_pill_activate)
                pill.setBackgroundResource(R.drawable.bg_focus_pill_inactive)
                subtitle.text = getString(R.string.focus_guard_subtitle_permission_needed)
            } else if (FocusGuardManager.getDailyLimit(requireContext()) > 0) {
                pill.text = getString(R.string.focus_guard_pill_active)
                pill.setBackgroundResource(R.drawable.bg_focus_pill_active)
                val limitMinutes = FocusGuardManager.getDailyLimit(requireContext())
                subtitle.text = getString(R.string.focus_guard_subtitle_monitoring_active) + " • Limit: ${limitMinutes}m"
                
                // Get current usage stats
                lifecycleScope.launch {
                    try {
                        val usageStats = FocusGuardManager.getCurrentUsage(requireContext())
                        val instagramUsage = usageStats.packageBreakdown["com.instagram.android"] ?: 0L
                        val youtubeUsage = usageStats.packageBreakdown["com.google.android.youtube"] ?: 0L
                        instagramMins.text = "${(instagramUsage / 60_000).toInt()}m"
                        youtubeMins.text = "${(youtubeUsage / 60_000).toInt()}m"
                    } catch (e: Exception) {
                        instagramMins.text = "0m"
                        youtubeMins.text = "0m"
                    }
                }
            } else {
                pill.text = getString(R.string.focus_guard_pill_inactive)
                pill.setBackgroundResource(R.drawable.bg_focus_pill_inactive)
                subtitle.text = getString(R.string.focus_guard_subtitle_tap_to_configure)
                
                instagramMins.text = "0m"
                youtubeMins.text = "0m"
            }
        } catch (e: Exception) {
            // Silently fail if view is destroyed during UI update
            // This prevents crashes when fragment is destroyed during async operations
        }
    }

    private fun navigateToActivities() {
        // Navigate to activities tab
        requireActivity().let { activity ->
            if (activity is MainActivity) {
                activity.getMainBinding().viewPager.currentItem = 1
            }
        }
    }

    // --- THE "ABSOLUTE LIMIT" ANIMATION ENGINE ---

    private fun prepareEntryAnimation() {
        // Offset is how far down/right elements start (in pixels)
        val offset = 150f

        // Header starts slightly up
        binding.hero.translationY = -offset
        binding.hero.alpha = 0f

        // Cards start down
        binding.progressCard.translationY = offset
        binding.progressCard.alpha = 0f

        binding.statsRow.translationY = offset
        binding.statsRow.alpha = 0f

        // Carousel starts right
        binding.featuredLabel.alpha = 0f
        binding.featuredCarousel.translationX = offset
        binding.featuredCarousel.alpha = 0f

        binding.recentLabel.alpha = 0f
        binding.recentActivitiesList.alpha = 0f
        binding.quoteCard.alpha = 0f
    }

    private fun runEntryAnimation() {
        // "Overshoot" gives it that premium "Bounce" effect
        val interpolator = OvershootInterpolator(1.2f)
        val duration = 700L

        // Cascade the animations with delays
        binding.hero.animate().translationY(0f).alpha(1f).setDuration(duration).setInterpolator(interpolator).start()

        binding.progressCard.animate().translationY(0f).alpha(1f).setDuration(duration).setStartDelay(100).setInterpolator(interpolator).start()

        binding.statsRow.animate().translationY(0f).alpha(1f).setDuration(duration).setStartDelay(200).setInterpolator(interpolator).start()

        binding.featuredLabel.animate().alpha(1f).setDuration(duration).setStartDelay(300).start()
        binding.featuredCarousel.animate().translationX(0f).alpha(1f).setDuration(duration).setStartDelay(300).setInterpolator(interpolator).start()

        binding.recentLabel.animate().alpha(1f).setDuration(duration).setStartDelay(400).start()
        binding.recentActivitiesList.animate().alpha(1f).setDuration(duration).setStartDelay(450).start()
        binding.quoteCard.animate().alpha(1f).setDuration(duration).setStartDelay(450).start()
    }

    private fun animateProgress(current: Int, goal: Int) {
        // 1. Animate the Circle
        binding.circle.setProgress(current.toFloat(), goal.toFloat(), true)

        // 2. Animate the Text (Counting up from 0 to current)
        // Check if we are already at the number to prevent re-animating on config change
        val currentTextVal = binding.todayMinutesValue.text.toString().toIntOrNull() ?: 0
        if (currentTextVal == current) return

        val textAnimator = ValueAnimator.ofInt(0, current)
        textAnimator.duration = 1500 // 1.5 seconds to count up
        textAnimator.interpolator = OvershootInterpolator()
        textAnimator.addUpdateListener { animation ->
            // Safety check: Binding might be null if view is destroyed during animation
            _binding?.todayMinutesValue?.text = animation.animatedValue.toString()
        }
        textAnimator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}