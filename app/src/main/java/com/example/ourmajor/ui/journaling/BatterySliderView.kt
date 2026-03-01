package com.example.ourmajor.ui.journaling

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max
import kotlin.math.min

class BatterySliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface OnLevelChangedListener {
        fun onLevelChanged(level: Int, fromUser: Boolean)
    }

    private var listener: OnLevelChangedListener? = null

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = 0x443A2F2A
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF7BC96F.toInt()
    }

    private val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x223A2F2A
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(12f)
        color = 0x993A2F2A.toInt()
    }

    private val rect = RectF()
    private val innerRect = RectF()

    private var level: Int = 60
    private var animatedLevel: Float = level.toFloat()

    private var animator: ValueAnimator? = null

    fun setOnLevelChangedListener(l: OnLevelChangedListener?) {
        listener = l
    }

    fun setLevel(value: Int, fromUser: Boolean = false) {
        val v = value.coerceIn(0, 100)
        if (v == level) return
        val old = animatedLevel
        level = v
        animateLevel(old, level.toFloat())
        listener?.onLevelChanged(level, fromUser)
    }

    fun getLevel(): Int = level

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

        val pad = dp(6f)
        val capH = dp(14f)
        val r = dp(18f)

        rect.set(pad, pad + capH, w - pad, h - pad)

        val capW = (rect.width() * 0.42f).coerceAtLeast(dp(16f))
        val capLeft = rect.centerX() - capW / 2f
        val capTop = pad
        val capRect = RectF(capLeft, capTop, capLeft + capW, capTop + capH)
        canvas.drawRoundRect(capRect, dp(6f), dp(6f), capPaint)

        canvas.drawRoundRect(rect, r, r, outlinePaint)

        val innerPad = dp(6f)
        innerRect.set(rect.left + innerPad, rect.top + innerPad, rect.right - innerPad, rect.bottom - innerPad)

        val frac = (animatedLevel / 100f).coerceIn(0f, 1f)
        val fillHeight = innerRect.height() * frac
        val fillRect = RectF(innerRect.left, innerRect.bottom - fillHeight, innerRect.right, innerRect.bottom)

        fillPaint.color = when {
            frac >= 0.66f -> 0xFF7BC96F.toInt()
            frac >= 0.33f -> 0xFFE8B44A.toInt()
            else -> 0xFFE56A6A.toInt()
        }

        val clip = Path().apply {
            addRoundRect(innerRect, r * 0.7f, r * 0.7f, Path.Direction.CW)
        }
        val save = canvas.save()
        canvas.clipPath(clip)
        canvas.drawRect(fillRect, fillPaint)
        canvas.restoreToCount(save)

        canvas.drawText("${level}%", rect.centerX(), rect.centerY() + dp(4f), textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                updateFromTouch(event.y, true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateFromTouch(event.y, true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateFromTouch(y: Float, fromUser: Boolean) {
        val h = height.toFloat().coerceAtLeast(1f)
        val pad = dp(6f) + dp(14f)
        val bottom = h - dp(6f)
        val top = pad
        val clamped = min(bottom, max(top, y))
        val frac = 1f - ((clamped - top) / (bottom - top)).coerceIn(0f, 1f)
        val newLevel = (frac * 100f).toInt().coerceIn(0, 100)

        val was = level
        setLevel(newLevel, fromUser)
        if (was != level && fromUser && (level % 10 == 0)) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun animateLevel(from: Float, to: Float) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(from, to).apply {
            duration = 220L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animatedLevel = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.density * resources.configuration.fontScale
}
