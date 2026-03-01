package com.example.ourmajor.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Fragment-scoped Smart Shuffle ViewModel.
 * 
 * Each fragment gets its own instance.
 * No Android dependencies, pure state management.
 */
class SmartShuffleViewModelNew : ViewModel() {
    
    private val smartShuffleEngine = SmartShuffleEngineNew()
    
    // State flows for fragment observation
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()
    
    /**
     * Get next activity class.
     * Returns Class<out AppCompatActivity> for standardized launching.
     */
    fun getNextActivity(): Class<out androidx.appcompat.app.AppCompatActivity> {
        return viewModelScope.launch {
            val activityClass = smartShuffleEngine.getNextActivity()
            _currentPosition.value = smartShuffleEngine.getCurrentPosition()
            activityClass
        }.let { 
            // Suspend function workaround for immediate return
            runCatching { smartShuffleEngine.getNextActivity() }.getOrNull() 
                ?: smartShuffleEngine.getAllActivities().first()
        }
    }
    
    /**
     * Force refresh shuffle and get next activity.
     */
    fun refreshAndGetNext(): Class<out androidx.appcompat.app.AppCompatActivity> {
        return viewModelScope.launch {
            smartShuffleEngine.refresh()
            val activityClass = smartShuffleEngine.getNextActivity()
            _currentPosition.value = smartShuffleEngine.getCurrentPosition()
            activityClass
        }.let { 
            runCatching { smartShuffleEngine.getNextActivity() }.getOrNull() 
                ?: smartShuffleEngine.getAllActivities().first()
        }
    }
    
    /**
     * Get current position for UI display.
     */
    fun getCurrentPositionValue(): Int = _currentPosition.value
    
    /**
     * Get total activities count.
     */
    fun getTotalActivities(): Int = smartShuffleEngine.getTotalActivities()
    
    /**
     * Check if we're at the start of a new cycle.
     */
    fun isNewCycle(): Boolean = smartShuffleEngine.isNewCycle()
}

/**
 * Factory for creating fragment-scoped SmartShuffleViewModel.
 */
class SmartShuffleViewModelNewFactory {
    @Suppress("UNCHECKED_CAST")
    fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmartShuffleViewModelNew::class.java)) {
            return SmartShuffleViewModelNew() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
