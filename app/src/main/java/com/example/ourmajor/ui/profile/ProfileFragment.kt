package com.example.ourmajor.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.CircularProgressIndicator
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ourmajor.auth.AuthViewModel
import com.example.ourmajor.R
import com.google.firebase.auth.FirebaseAuth
import com.example.ourmajor.gamification.GamificationManager
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private lateinit var authViewModel: AuthViewModel
    private lateinit var profileViewModel: ProfileViewModel2

    private fun valueOrNotSet(value: String?, notSet: String): String {
        return value?.trim()?.takeIf { it.isNotBlank() } ?: notSet
    }

    private fun ZenIdCard(
        root: View,
        userName: String,
        archetype: String,
        northStar: String,
        age: String,
        gender: String,
        phoneNumber: String
    ) {
        val notSet = getString(R.string.zen_id_not_set)

        root.findViewById<TextView>(R.id.zen_identity_name)
            ?.text = valueOrNotSet(userName, notSet)
        root.findViewById<TextView>(R.id.zen_identity_archetype_value)
            ?.text = valueOrNotSet(archetype, notSet)
        root.findViewById<TextView>(R.id.zen_identity_north_star_value)
            ?.text = valueOrNotSet(northStar, notSet)
        root.findViewById<TextView>(R.id.zen_identity_age_value)
            ?.text = valueOrNotSet(age, notSet)
        root.findViewById<TextView>(R.id.zen_identity_gender_value)
            ?.text = valueOrNotSet(gender, notSet)
        root.findViewById<TextView>(R.id.zen_identity_contact_value)
            ?.text = valueOrNotSet(phoneNumber, notSet)
    }

    private fun initials(value: String): String {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return ""
        val parts = cleaned.split(" ").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> (parts[0].take(1) + parts[1].take(1)).uppercase()
            cleaned.contains("@") -> cleaned.take(1).uppercase()
            else -> cleaned.take(2).uppercase()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, _savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(view, _savedInstanceState)

        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        profileViewModel = ViewModelProvider(
            requireActivity(),
            ProfileViewModelFactory(requireContext())
        )[ProfileViewModel2::class.java]

        val auth = FirebaseAuth.getInstance()

        // Email from AuthViewModel
        val emailTextHeader = view.findViewById<TextView>(R.id.email)
        val nameTextHeader = view.findViewById<TextView>(R.id.name)
        authViewModel.uiStateLiveData.observe(viewLifecycleOwner) { state ->
            val user = auth.currentUser
            val displayName = user?.displayName?.takeIf { it.isNotBlank() }
            nameTextHeader?.text = displayName ?: ""
            emailTextHeader?.text = state.email ?: user?.email.orEmpty()
        }

        view.findViewById<View>(R.id.btn_edit_profile)?.setOnClickListener {
            EditProfileBottomSheet().show(childFragmentManager, "edit_profile")
        }

        view.findViewById<View>(R.id.avatar_container)?.setOnClickListener {
            EditProfileBottomSheet().show(childFragmentManager, "edit_profile")
        }

        view.findViewById<View>(R.id.btn_open_settings)?.setOnClickListener {
            SettingsBottomSheet().show(childFragmentManager, "settings")
        }

        val toolbar = view.findViewById<MaterialToolbar>(R.id.profile_toolbar)
        val appBar = view.findViewById<AppBarLayout>(R.id.profile_appbar)
        val heroContainer = view.findViewById<View>(R.id.hero_container)
        val avatarContainer = view.findViewById<View>(R.id.avatar_container)
        val bioText = view.findViewById<TextView>(R.id.bio)
        val nameText = view.findViewById<TextView>(R.id.name)
        val completenessRing = view.findViewById<CircularProgressIndicator>(R.id.profile_completeness)

        var latestDisplayName: String = ""

        val streakChip = view.findViewById<TextView>(R.id.streak_chip)
        val pointsChip = view.findViewById<TextView>(R.id.points_chip)
        val activitiesChip = view.findViewById<TextView>(R.id.activities_chip)

        val avatarInitials = view.findViewById<TextView>(R.id.avatar_initials)

        profileViewModel.stats.observe(viewLifecycleOwner) { st ->
            val streak = st.currentStreakDays
            streakChip?.text = if (streak <= 0) "Streak: —" else "Streak: $streak"
            pointsChip?.text = "Minutes: ${st.totalMinutes}"
            activitiesChip?.text = "Level: ${st.currentLevel}"
        }

        profileViewModel.profileCompleteness.observe(viewLifecycleOwner) { pct ->
            completenessRing?.setProgressCompat(pct.coerceIn(0, 100), true)
        }
        
        // Observe ProfileViewModel2 state
        profileViewModel.uiState.observe(viewLifecycleOwner) { state ->
            // Update display name (add an edit field later if needed)
            val user = auth.currentUser
            val displayName = user?.displayName?.takeIf { it.isNotBlank() }
                ?: state.profile.displayName.takeIf { it.isNotBlank() }
            nameText?.text = displayName ?: ""

            ZenIdCard(
                root = view,
                userName = displayName ?: "",
                archetype = state.profile.mindfulnessLevel,
                northStar = state.profile.primaryGoal,
                age = state.profile.age?.toString() ?: "",
                gender = state.profile.gender,
                phoneNumber = state.profile.mobileNumber
            )

            // Toolbar shows the name only when collapsed
            latestDisplayName = displayName ?: ""

            val email = user?.email?.takeIf { it.isNotBlank() }
                ?: state.profile.email.takeIf { it.isNotBlank() }
            view.findViewById<TextView>(R.id.email)?.text = email ?: ""

            val initialsSource = (displayName ?: email ?: "").trim()
            avatarInitials?.text = initials(initialsSource)

            val bio = state.profile.bio.trim()
            bioText?.text = if (bio.isBlank()) "One breath at a time" else bio

        }

        // Set avatar initials immediately
        avatarInitials?.text = initials(auth.currentUser?.displayName ?: auth.currentUser?.email ?: "")

        // Collapsing fade/scale behavior
        appBar?.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val range = appBarLayout.totalScrollRange.coerceAtLeast(1)
            val fraction = kotlin.math.abs(verticalOffset).toFloat() / range.toFloat()

            toolbar?.title = if (fraction >= 0.65f) latestDisplayName else ""

            val heroAlpha = (1f - (fraction * 1.25f)).coerceIn(0f, 1f)
            heroContainer?.alpha = heroAlpha

            val avatarScale = (1f - (fraction * 0.15f)).coerceIn(0.85f, 1f)
            avatarContainer?.scaleX = avatarScale
            avatarContainer?.scaleY = avatarScale

            val bioAlpha = (1f - (fraction * 2f)).coerceIn(0f, 1f)
            bioText?.alpha = bioAlpha
            nameText?.alpha = (1f - (fraction * 1.8f)).coerceIn(0f, 1f)
        })

        val gm = GamificationManager.getInstance(requireContext())

        // Developer cheat: long-press avatar awards +5000 points
        view.findViewById<View>(R.id.avatar_container)?.setOnLongClickListener {
            lifecycleScope.launch {
                gm.grantPoints(5000)
                Toast.makeText(requireContext(), "Dev Mode: +5000 Points!", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
}
