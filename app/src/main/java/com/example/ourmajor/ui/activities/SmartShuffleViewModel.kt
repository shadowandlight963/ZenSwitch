package com.example.ourmajor.ui.activities

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel to manage Smart Shuffle state across configuration changes.
 * 
 * Maintains shuffle engine instance and current position persistence.
 */
class SmartShuffleViewModel : ViewModel() {
    
    private val smartShuffleEngine = SmartShuffleEngine()
    
    private val _currentActivity = MutableStateFlow<ActivityInfo?>(null)
    val currentActivity: StateFlow<ActivityInfo?> = _currentActivity.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(1)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()
    
    init {
        // Initialize with first activity
        getNextActivity()
    }
    
    /**
     * Get the next activity from the shuffle engine.
     * Updates state flows for UI observation.
     */
    fun getNextActivity() {
        viewModelScope.launch {
            val activity = smartShuffleEngine.getNextActivity()
            _currentActivity.value = activity
            _currentPosition.value = smartShuffleEngine.getCurrentPosition()
        }
    }
    
    /**
     * Refresh the shuffle and get the next activity.
     */
    fun refreshShuffle() {
        viewModelScope.launch {
            smartShuffleEngine.refresh()
            val activity = smartShuffleEngine.getNextActivity()
            _currentActivity.value = activity
            _currentPosition.value = smartShuffleEngine.getCurrentPosition()
        }
    }
    
    /**
     * Get the current activity for debugging purposes.
     */
    fun getCurrentActivity(): ActivityInfo? = _currentActivity.value
    
    /**
     * Launch the current activity.
     * Called by UI components to trigger navigation.
     */
    fun launchCurrentActivity(context: android.content.Context) {
        _currentActivity.value?.let { activity ->
            activity.launchWithLogging(context)
        } ?: run {
            Log.e("SMART_SHUFFLE", "No current activity available to launch")
        }
    }
    
    /**
     * Get total activities count for UI display.
     */
    fun getTotalActivities(): Int = smartShuffleEngine.getTotalActivities()
    
    /**
     * Check if we're at the start of a new cycle.
     */
    fun isNewCycle(): Boolean = smartShuffleEngine.isNewCycle()
}

/**
 * Factory for creating SmartShuffleViewModel.
 */
class SmartShuffleViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmartShuffleViewModel::class.java)) {
            return SmartShuffleViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
