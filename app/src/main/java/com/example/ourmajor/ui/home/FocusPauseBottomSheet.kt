package com.example.ourmajor.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.ourmajor.R
import com.example.ourmajor.focusguard.FocusGuardManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

/**
 * Turn-off confirmation: single action "Turn Off Until Tomorrow".
 * Stops Focus Guard monitoring until tomorrow.
 */
class FocusPauseBottomSheet : BottomSheetDialogFragment() {

    /** Called when sheet is dismissed; use to refresh Home pill (e.g. syncFocusGuardUI). */
    var onDismissed: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        _savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_focus_pause, container, false)
    }

    override fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(view, _savedInstanceState)

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_pause_until_tomorrow)
            .setOnClickListener {
                pauseUntilTomorrow()
                dismiss()
            }
    }

    override fun onDestroyView() {
        onDismissed?.invoke()
        super.onDestroyView()
    }

    /** Turn off Focus Guard until tomorrow. */
    private fun pauseUntilTomorrow() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        // Stop monitoring
        FocusGuardManager.stopMonitoring(requireContext())
    }
}
