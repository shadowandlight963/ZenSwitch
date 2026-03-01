package com.example.ourmajor.ui.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Premium swipe page transformer for ViewPager2.
 * 
 * Provides subtle, GPU-friendly visual effects:
 * - Scale effect: 0.96f to 1.0f
 * - Fade effect: 0.85f to 1.0f
 * - No heavy 3D transforms or translations
 * - Optimized for performance
 */
class SwipePageTransformer : ViewPager2.PageTransformer {

    companion object {
        private const val MIN_SCALE = 0.96f
        private const val MIN_ALPHA = 0.85f
        private const val SCALE_RANGE = 1.0f - MIN_SCALE
        private const val ALPHA_RANGE = 1.0f - MIN_ALPHA
    }

    override fun transformPage(page: View, position: Float) {
        val absPosition = abs(position)

        when {
            // Page is visible (between -1 and 1)
            absPosition < 1f -> {
                // Calculate scale and alpha based on position
                val scaleFactor = MIN_SCALE + (1f - absPosition) * SCALE_RANGE
                val alphaFactor = MIN_ALPHA + (1f - absPosition) * ALPHA_RANGE

                // Apply GPU-friendly transformations
                page.apply {
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    alpha = alphaFactor
                }
            }
            // Page is outside visible range
            else -> {
                page.alpha = 0f
            }
        }
    }
}
