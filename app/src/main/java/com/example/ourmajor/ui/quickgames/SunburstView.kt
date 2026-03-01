package com.example.ourmajor.ui.quickgames

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SunburstView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        color = 0x66FFD666
        strokeCap = Paint.Cap.ROUND
    }

    private var progress: Float = 0f
    private var rotation: Float = 0f

    private var animator: ValueAnimator? = null

    fun burst() {
        animator?.cancel()
        alpha = 0f
        visibility = VISIBLE
        progress = 0f
        rotation = 0f

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val p = it.animatedValue as Float
                progress = p
                rotation = p * 0.9f
                alpha = (p.coerceIn(0f, 1f) * (1f - (p - 0.65f).coerceAtLeast(0f) / 0.35f)).coerceIn(0f, 1f)
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val r = min(w, h) * (0.15f + 0.55f * progress)
        val cx = w / 2f
        val cy = h * 0.68f

        glowPaint.shader = RadialGradient(
            cx,
            cy,
            r,
            intArrayOf(0x66FFD666, 0x00FFD666),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx, cy, r, glowPaint)

        val rays = 18
        val base = r * 0.65f
        val len = r * (0.65f + 0.55f * progress)

        for (i in 0 until rays) {
            val a = (i.toFloat() / rays.toFloat()) * (Math.PI * 2.0) + rotation
            val x0 = cx + (base * cos(a)).toFloat()
            val y0 = cy + (base * sin(a)).toFloat()
            val x1 = cx + (len * cos(a)).toFloat()
            val y1 = cy + (len * sin(a)).toFloat()
            canvas.drawLine(x0, y0, x1, y1, rayPaint)
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
