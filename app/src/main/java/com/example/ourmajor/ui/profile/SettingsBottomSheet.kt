package com.example.ourmajor.ui.profile

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelProvider
import com.example.ourmajor.R
import com.example.ourmajor.auth.AuthViewModel
import com.example.ourmajor.gamification.GamificationManager
import com.example.ourmajor.ui.theme.ThemeUtils
import com.example.ourmajor.utils.ReminderReceiver
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var profileViewModel: ProfileViewModel2

    private val appPrefs by lazy {
        requireContext().getSharedPreferences(PREFS_APP, android.content.Context.MODE_PRIVATE)
    }

    private var pendingEnablePush: Boolean = false
    private var suppressNotificationListeners: Boolean = false
    private var suppressGoalListener: Boolean = false
    private var lastCheckedGoalChipId: Int = View.NO_ID
    private var customDialogOpen: Boolean = false

    private val requestPostNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingEnablePush) {
            val pushSwitch = view?.findViewById<SwitchMaterial>(R.id.switch_push)
            if (granted) {
                appPrefs.edit().putBoolean(KEY_PUSH_ENABLED, true).apply()
                profileViewModel.setPushNotificationsEnabled(true)
            } else {
                appPrefs.edit().putBoolean(KEY_PUSH_ENABLED, false).apply()
                profileViewModel.setPushNotificationsEnabled(false)
                suppressNotificationListeners = true
                pushSwitch?.isChecked = false
                suppressNotificationListeners = false
            }
        }
        pendingEnablePush = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        _savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_settings, container, false)
    }

    override fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(view, _savedInstanceState)

        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        profileViewModel = ViewModelProvider(
            requireActivity(),
            ProfileViewModelFactory(requireContext())
        )[ProfileViewModel2::class.java]

        val pushSwitch = view.findViewById<SwitchMaterial>(R.id.switch_push)
        val reminderSwitch = view.findViewById<SwitchMaterial>(R.id.switch_reminder)
        val darkModeSwitch = view.findViewById<SwitchMaterial>(R.id.switch_darkmode)
        val reminderSubtitle = view.findViewById<TextView>(R.id.text_reminder_subtitle)

        val pushEnabled = appPrefs.getBoolean(KEY_PUSH_ENABLED, false)
        pushSwitch?.setOnCheckedChangeListener(null)
        pushSwitch?.isChecked = pushEnabled

        val reminderEnabled = appPrefs.getBoolean(KEY_REMINDER_ENABLED, false)
        reminderSwitch?.setOnCheckedChangeListener(null)
        reminderSwitch?.isChecked = reminderEnabled

        val darkEnabled = appPrefs.getBoolean(KEY_DARK_MODE, false)
        darkModeSwitch?.setOnCheckedChangeListener(null)
        darkModeSwitch?.isChecked = darkEnabled
        darkModeSwitch?.setOnCheckedChangeListener { _, isChecked ->
            appPrefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        val gm = GamificationManager.getInstance(requireContext())

        view.findViewById<View>(R.id.theme_sage)?.setOnClickListener {
            val changed = ThemeUtils.saveThemeIfChanged(requireContext(), ThemeUtils.THEME_SAGE)
            profileViewModel.updateTheme("sage")
            if (changed) requireActivity().recreate()
        }
        view.findViewById<View>(R.id.theme_minimal)?.setOnClickListener {
            val unlocked = gm.state.value.unlockedThemes.contains(GamificationManager.ITEM_THEME_MINIMAL_MIST)
            if (unlocked) {
                val changed = ThemeUtils.saveThemeIfChanged(requireContext(), ThemeUtils.THEME_MINIMAL)
                profileViewModel.updateTheme("minimal")
                if (changed) requireActivity().recreate()
            } else {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Locked")
                    .setMessage("Unlock Minimalist Mist in the Zen Store (Rewards) using points.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        view.findViewById<View>(R.id.theme_ocean)?.setOnClickListener {
            val unlocked = gm.state.value.unlockedThemes.contains(GamificationManager.ITEM_THEME_OCEAN)
            if (unlocked) {
                val changed = ThemeUtils.saveThemeIfChanged(requireContext(), ThemeUtils.THEME_OCEAN)
                profileViewModel.updateTheme("ocean")
                if (changed) requireActivity().recreate()
            } else {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Locked")
                    .setMessage("Unlock Ocean Breeze Theme in the Zen Store (Rewards) using points.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        view.findViewById<View?>(R.id.theme_sunset)?.setOnClickListener {
            val unlocked = gm.state.value.unlockedThemes.contains(GamificationManager.ITEM_THEME_SUNSET)
            if (unlocked) {
                val changed = ThemeUtils.saveThemeIfChanged(requireContext(), ThemeUtils.THEME_SUNSET)
                profileViewModel.updateTheme("sunset")
                if (changed) requireActivity().recreate()
            } else {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Locked")
                    .setMessage("Unlock Sunset Serenity Theme in the Zen Store (Rewards) using points.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        val chipGroup = view.findViewById<ChipGroup>(R.id.chipgroup_daily_goal)
        val chip15 = view.findViewById<Chip>(R.id.chip_goal_15)
        val chip30 = view.findViewById<Chip>(R.id.chip_goal_30)
        val chip45 = view.findViewById<Chip>(R.id.chip_goal_45)
        val chip60 = view.findViewById<Chip>(R.id.chip_goal_60)
        val chipCustom = view.findViewById<Chip>(R.id.chip_custom)

        val primary = MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary)
        val onPrimary = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnPrimary)
        val onSurface = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface)
        val uncheckedBg = ColorUtils.setAlphaComponent(onSurface, 18)

        val bgColors = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                primary,
                uncheckedBg
            )
        )

        val textColors = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                onPrimary,
                onSurface
            )
        )

        val strokeColors = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                primary,
                primary
            )
        )

        val strokeWidthPx = (resources.displayMetrics.density * 1f).toInt().coerceAtLeast(1)

        listOf(chip15, chip30, chip45, chip60, chipCustom).forEach { chip ->
            chip?.chipBackgroundColor = bgColors
            chip?.setTextColor(textColors)
            chip?.chipStrokeColor = strokeColors
            chip?.chipStrokeWidth = strokeWidthPx.toFloat()
            chip?.isCheckedIconVisible = false
        }

        fun saveGoal(minutes: Int) {
            appPrefs.edit().putInt(KEY_DAILY_GOAL, minutes).apply()
            Toast.makeText(requireContext(), "Goal saved!", Toast.LENGTH_SHORT).show()
            profileViewModel.updateDailyGoalMinutes(minutes)
        }

        chipCustom?.setOnClickListener {
            if (customDialogOpen) return@setOnClickListener
            if (chipCustom.isChecked) {
                showCustomGoalDialog(
                    chipGroup = chipGroup,
                    chipCustom = chipCustom,
                    onSet = { minutes -> saveGoal(minutes) }
                )
            }
        }

        chipGroup?.setOnCheckedStateChangeListener { group, checkedIds ->
            if (suppressGoalListener) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds.firstOrNull() ?: View.NO_ID

            if (checkedId == R.id.chip_custom) {
                val fallbackId = when (lastCheckedGoalChipId) {
                    View.NO_ID -> R.id.chip_goal_30
                    R.id.chip_custom -> R.id.chip_goal_30
                    else -> lastCheckedGoalChipId
                }

                suppressGoalListener = true
                group.check(fallbackId)
                suppressGoalListener = false

                showCustomGoalDialog(
                    chipGroup = group,
                    chipCustom = chipCustom,
                    onSet = { minutes -> saveGoal(minutes) }
                )
                return@setOnCheckedStateChangeListener
            }

            val minutes = when (checkedId) {
                R.id.chip_goal_15 -> 15
                R.id.chip_goal_30 -> 30
                R.id.chip_goal_45 -> 45
                R.id.chip_goal_60 -> 60
                else -> 30
            }

            if (checkedId != R.id.chip_custom) {
                chipCustom?.text = "Custom"
            }

            if (checkedId != View.NO_ID) {
                lastCheckedGoalChipId = checkedId
            }

            saveGoal(minutes)
        }

        val savedGoal = appPrefs.getInt(KEY_DAILY_GOAL, 30)
        suppressGoalListener = true
        val initialChipId = when (savedGoal) {
            15 -> R.id.chip_goal_15
            30 -> R.id.chip_goal_30
            45 -> R.id.chip_goal_45
            60 -> R.id.chip_goal_60
            else -> {
                chipCustom?.text = "Custom: $savedGoal min"
                R.id.chip_custom
            }
        }
        chipGroup?.check(initialChipId)
        lastCheckedGoalChipId = initialChipId
        suppressGoalListener = false

        setupNotificationLogic(
            pushSwitch = pushSwitch,
            reminderSwitch = reminderSwitch,
            reminderSubtitle = reminderSubtitle
        )

        view.findViewById<View>(R.id.btn_signout)?.setOnClickListener {
            authViewModel.signOut()
            dismiss()
        }

        profileViewModel.uiState.observe(viewLifecycleOwner) { state ->
            view.findViewById<View>(R.id.dot_sage)?.visibility = if (state.preferences.theme == "sage") View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.dot_ocean)?.visibility = if (state.preferences.theme == "ocean") View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.dot_sunset)?.visibility = if (state.preferences.theme == "sunset") View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.dot_minimal)?.visibility = if (state.preferences.theme == "minimal") View.VISIBLE else View.GONE
        }
    }

    private fun showCustomGoalDialog(
        chipGroup: ChipGroup?,
        chipCustom: Chip?,
        onSet: (Int) -> Unit
    ) {
        if (customDialogOpen) return
        customDialogOpen = true

        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Minutes"
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Custom Goal")
            .setView(input)
            .setPositiveButton("Set", null)
            .setNegativeButton("Cancel") { d, _ ->
                d.dismiss()
            }
            .create()

        dialog.setOnDismissListener {
            customDialogOpen = false
        }

        dialog.setOnShowListener {
            val btn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val minutes = input.text?.toString()?.trim()?.toIntOrNull()
                if (minutes == null || minutes <= 0) {
                    input.error = "Enter minutes"
                    return@setOnClickListener
                }
                if (minutes > 180) {
                    input.error = "Max 180"
                    return@setOnClickListener
                }

                chipCustom?.text = "Custom: $minutes min"

                suppressGoalListener = true
                chipGroup?.check(R.id.chip_custom)
                suppressGoalListener = false

                lastCheckedGoalChipId = R.id.chip_custom
                onSet(minutes)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun setupNotificationLogic(
        pushSwitch: SwitchMaterial?,
        reminderSwitch: SwitchMaterial?,
        reminderSubtitle: TextView?
    ) {
        fun formatTime(hour24: Int, minute: Int): String {
            val h12 = when (val h = hour24 % 12) {
                0 -> 12
                else -> h
            }
            val ampm = if (hour24 < 12) "AM" else "PM"
            return String.format("%d:%02d %s", h12, minute, ampm)
        }

        val savedHour = appPrefs.getInt(KEY_REMINDER_HOUR, 19)
        val savedMinute = appPrefs.getInt(KEY_REMINDER_MINUTE, 0)
        val reminderEnabled = appPrefs.getBoolean(KEY_REMINDER_ENABLED, false)

        reminderSubtitle?.text = if (reminderEnabled) {
            "Reminder set for ${formatTime(savedHour, savedMinute)}"
        } else {
            "Gentle daily nudge"
        }

        val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
        val reminderPendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            9001,
            Intent(requireContext(), ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        pushSwitch?.setOnCheckedChangeListener(null)
        reminderSwitch?.setOnCheckedChangeListener(null)

        pushSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (suppressNotificationListeners) return@setOnCheckedChangeListener

            if (!isChecked) {
                appPrefs.edit().putBoolean(KEY_PUSH_ENABLED, false).apply()
                profileViewModel.setPushNotificationsEnabled(false)
                return@setOnCheckedChangeListener
            }

            if (Build.VERSION.SDK_INT >= 33) {
                val granted = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    appPrefs.edit().putBoolean(KEY_PUSH_ENABLED, true).apply()
                    profileViewModel.setPushNotificationsEnabled(true)
                } else {
                    pendingEnablePush = true
                    requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                appPrefs.edit().putBoolean(KEY_PUSH_ENABLED, true).apply()
                profileViewModel.setPushNotificationsEnabled(true)
            }
        }

        reminderSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (suppressNotificationListeners) return@setOnCheckedChangeListener

            if (!isChecked) {
                appPrefs.edit().putBoolean(KEY_REMINDER_ENABLED, false).apply()
                alarmManager.cancel(reminderPendingIntent)
                reminderSubtitle?.text = "Gentle daily nudge"
                return@setOnCheckedChangeListener
            }

            val initialHour = appPrefs.getInt(KEY_REMINDER_HOUR, 19)
            val initialMinute = appPrefs.getInt(KEY_REMINDER_MINUTE, 0)

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(initialHour)
                .setMinute(initialMinute)
                .setTitleText("Set daily reminder")
                .build()

            picker.addOnPositiveButtonClickListener {
                val hour = picker.hour
                val minute = picker.minute

                appPrefs.edit()
                    .putInt(KEY_REMINDER_HOUR, hour)
                    .putInt(KEY_REMINDER_MINUTE, minute)
                    .putBoolean(KEY_REMINDER_ENABLED, true)
                    .apply()

                reminderSubtitle?.text = "Reminder set for ${formatTime(hour, minute)}"

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                try {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        reminderPendingIntent
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Could not set reminder: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    appPrefs.edit().putBoolean(KEY_REMINDER_ENABLED, false).apply()
                    alarmManager.cancel(reminderPendingIntent)
                    reminderSubtitle?.text = "Gentle daily nudge"

                    suppressNotificationListeners = true
                    reminderSwitch.isChecked = false
                    suppressNotificationListeners = false
                    return@addOnPositiveButtonClickListener
                }

                suppressNotificationListeners = true
                reminderSwitch.isChecked = true
                suppressNotificationListeners = false
            }

            picker.addOnNegativeButtonClickListener {
                suppressNotificationListeners = true
                reminderSwitch.isChecked = false
                suppressNotificationListeners = false

                appPrefs.edit().putBoolean(KEY_REMINDER_ENABLED, false).apply()
                alarmManager.cancel(reminderPendingIntent)
                reminderSubtitle?.text = "Gentle daily nudge"
            }

            picker.addOnCancelListener {
                suppressNotificationListeners = true
                reminderSwitch.isChecked = false
                suppressNotificationListeners = false

                appPrefs.edit().putBoolean(KEY_REMINDER_ENABLED, false).apply()
                alarmManager.cancel(reminderPendingIntent)
                reminderSubtitle?.text = "Gentle daily nudge"
            }

            picker.show(parentFragmentManager, "daily_reminder_time_picker")
        }
    }

    companion object {
        private const val PREFS_APP = "app_settings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_PUSH_ENABLED = "push_enabled"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}
