package com.example.ourmajor.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ourmajor.R
import com.example.ourmajor.focusguard.FocusGuardManager
import com.example.ourmajor.focusguard.FocusGuardCoreService
import com.example.ourmajor.notifications.NudgeNotificationHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

/**
 * Focus Portal: set/update daily limit (30m, 60m, 90m, or Custom via time picker).
 * Activate/Update saves via FocusRepository.saveDailyLimitMs(); service uses repo.getDailyLimitMs().
 */
class FocusConfigBottomSheet : BottomSheetDialogFragment() {

    var onDismissed: (() -> Unit)? = null

    private var selectedLimitMs: Long = 60L * 60 * 1000 // default 60 min
    private var isCustomSelected: Boolean = false
    private var customLabel: String = "" // e.g. "1h 15m"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        _savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_focus_config, container, false)
    }

    override fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(view, _savedInstanceState)
        selectedLimitMs = FocusGuardManager.getDailyLimit(requireContext()).toLong() * 60_000L
        syncSelectionFromSavedLimit(view)

        val btn30 = view.findViewById<MaterialButton>(R.id.btn_30m)
        val btn60 = view.findViewById<MaterialButton>(R.id.btn_60m)
        val btn90 = view.findViewById<MaterialButton>(R.id.btn_90m)
        val btnCustom = view.findViewById<MaterialButton>(R.id.btn_custom)
        val limitButtons = listOf(btn30, btn60, btn90, btnCustom)

        fun setSelection(selected: MaterialButton, limitMs: Long, custom: Boolean, customText: String? = null) {
            selectedLimitMs = limitMs
            isCustomSelected = custom
            if (custom && customText != null) {
                customLabel = customText
                btnCustom.text = customText
            }
            limitButtons.forEach { btn ->
                updateButtonSelection(btn, btn == selected)
            }
        }

        btn30.setOnClickListener { setSelection(btn30, 30L * 60 * 1000, false) }
        btn60.setOnClickListener { setSelection(btn60, 60L * 60 * 1000, false) }
        btn90.setOnClickListener { setSelection(btn90, 90L * 60 * 1000, false) }

        btnCustom.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour((selectedLimitMs / (3600 * 1000)).toInt().coerceIn(0, 23))
                .setMinute(((selectedLimitMs % (3600 * 1000)) / (60 * 1000)).toInt().coerceIn(0, 59))
                .setTitleText(getString(R.string.focus_config_time_picker_title))
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build()
            picker.addOnPositiveButtonClickListener {
                val hours = picker.hour
                val minutes = picker.minute
                val totalMs = hours * 3600L * 1000 + minutes * 60L * 1000
                val displayText = formatLimitLabel(hours, minutes)
                setSelection(btnCustom, totalMs.coerceAtLeast(60_000), true, displayText)
            }
            picker.show(childFragmentManager, "focus_time_picker")
        }

        val isEnabled = FocusGuardManager.getDailyLimit(requireContext()) > 0
        val activateBtn = view.findViewById<MaterialButton>(R.id.focus_config_activate_btn)
        activateBtn.text = if (isEnabled) getString(R.string.focus_config_update_limit) else getString(R.string.focus_config_activate_guard)
        activateBtn.setOnClickListener {
            FocusGuardManager.setDailyLimit(requireContext(), (selectedLimitMs / 60_000).toInt())
            if (!isEnabled) {
                FocusGuardManager.startMonitoring(requireContext())
            }
            val message = formatLimitForMessage(selectedLimitMs)
            // Use application context to ensure toast survives sheet dismissal
            Toast.makeText(requireContext().applicationContext, getString(R.string.focus_config_limit_updated, message), Toast.LENGTH_SHORT).show()
            
            // Add a small delay to ensure the toast is visible before dismissal
            lifecycleScope.launch {
                kotlinx.coroutines.delay(300)
                dismiss()
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_test_nudge)?.setOnClickListener {
            // Initialize notification helper and show test nudge
            val nudgeHelper = NudgeNotificationHelper(requireContext())
            nudgeHelper.showTestNudge()
        }
    }

    override fun onDestroyView() {
        onDismissed?.invoke()
        super.onDestroyView()
    }

    private fun syncSelectionFromSavedLimit(view: View) {
        val ms = FocusGuardManager.getDailyLimit(requireContext()).toLong() * 60_000L
        val btn30 = view.findViewById<MaterialButton>(R.id.btn_30m)
        val btn60 = view.findViewById<MaterialButton>(R.id.btn_60m)
        val btn90 = view.findViewById<MaterialButton>(R.id.btn_90m)
        val btnCustom = view.findViewById<MaterialButton>(R.id.btn_custom)
        val limitButtons = listOf(btn30, btn60, btn90, btnCustom)
        selectedLimitMs = ms
        when {
            ms == 30L * 60 * 1000 -> { isCustomSelected = false; limitButtons.forEach { updateButtonSelection(it, it == btn30) } }
            ms == 60L * 60 * 1000 -> { isCustomSelected = false; limitButtons.forEach { updateButtonSelection(it, it == btn60) } }
            ms == 90L * 60 * 1000 -> { isCustomSelected = false; limitButtons.forEach { updateButtonSelection(it, it == btn90) } }
            else -> {
                isCustomSelected = true
                val h = (ms / (3600 * 1000)).toInt()
                val m = ((ms % (3600 * 1000)) / (60 * 1000)).toInt()
                customLabel = formatLimitLabel(h, m)
                btnCustom.text = customLabel
                limitButtons.forEach { updateButtonSelection(it, it == btnCustom) }
            }
        }
    }

    private fun formatLimitLabel(hours: Int, minutes: Int): String {
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    /** Format limit for user-facing message: e.g. "1 hour", "1 hour 30 mins", "30 minutes". */
    private fun formatLimitForMessage(ms: Long): String {
        val totalMins = (ms / (60 * 1000)).toInt()
        val hours = totalMins / 60
        val mins = totalMins % 60
        return when {
            hours > 0 && mins > 0 -> if (hours == 1) getString(R.string.focus_config_limit_format_1h_m, mins) else getString(R.string.focus_config_limit_format_h_m, hours, mins)
            hours == 1 -> getString(R.string.focus_config_limit_format_1h)
            hours > 1 -> getString(R.string.focus_config_limit_format_hours, hours)
            mins == 1 -> getString(R.string.focus_config_limit_format_1m)
            else -> getString(R.string.focus_config_limit_format_mins, totalMins)
        }
    }

    private fun updateButtonSelection(button: MaterialButton, selected: Boolean) {
        val primary = ContextCompat.getColor(requireContext(), R.color.sage_primary_fixed)
        val surfaceVariant = ContextCompat.getColor(requireContext(), com.example.ourmajor.R.color.neutral_surface_variant_fixed)
        val onPrimary = ContextCompat.getColor(requireContext(), R.color.white)
        val onSurface = ContextCompat.getColor(requireContext(), com.example.ourmajor.R.color.neutral_text_fixed)
        if (selected) {
            button.setBackgroundColor(primary)
            button.setTextColor(onPrimary)
            button.strokeColor = android.content.res.ColorStateList.valueOf(primary)
        } else {
            button.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            button.setTextColor(onSurface)
            button.strokeColor = android.content.res.ColorStateList.valueOf(surfaceVariant)
        }
    }
}
