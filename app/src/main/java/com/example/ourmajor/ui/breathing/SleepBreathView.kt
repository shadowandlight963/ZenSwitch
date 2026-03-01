package com.example.ourmajor.ui.breathing

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.ourmajor.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

class SleepBreathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Phase {
        INHALE,
        HOLD,
        EXHALE
    }

    interface OnPhaseChangeListener {
        fun onPhaseChanged(phase: Phase)
    }

    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.zen_ocean_blue)
    }

    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.zen_indigo)
        alpha = 0
        maskFilter = BlurMaskFilter(dp(22f), BlurMaskFilter.Blur.NORMAL)
    }

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF000000.toInt()
        alpha = 0
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = dp(28f)
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        setShadowLayer(dp(2f), 0f, dp(1f), 0x66000000)
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xCCFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = dp(14f)
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        setShadowLayer(dp(2f), 0f, dp(1f), 0x44000000)
    }

    private var animator: ValueAnimator? = null
    private var cycleFraction: Float = 0f

    private var onPhaseChangeListener: OnPhaseChangeListener? = null
    private var lastPhase: Phase? = null

    private val inhaleMs = 4_000L
    private val holdMs = 7_000L
    private val exhaleMs = 8_000L
    private val cycleMs = inhaleMs + holdMs + exhaleMs

    private val colorInhale by lazy { ContextCompat.getColor(context, R.color.zen_ocean_blue) }
    private val colorHold by lazy { ContextCompat.getColor(context, R.color.zen_indigo) }
    private val colorExhale by lazy { ContextCompat.getColor(context, R.color.zen_lavender) }

    private var orbShader: Shader? = null
    private var orbShaderColor: Int = 0
    private var orbShaderRadius: Float = -1f

    private var bgShader: Shader? = null
    private var bgW: Int = 0
    private var bgH: Int = 0

    fun setOnPhaseChangeListener(listener: OnPhaseChangeListener?) {
        onPhaseChangeListener = listener
    }

    fun start() {
        if (animator?.isRunning == true) return
        cycleFraction = 0f
        lastPhase = null
        notifyPhaseIfNeeded(0L)

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = cycleMs
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                cycleFraction = it.animatedValue as Float
                val tMs = (cycleFraction * cycleMs).toLong().coerceIn(0L, cycleMs)
                notifyPhaseIfNeeded(tMs)
                invalidate()
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    fun fadeOut(durationMs: Long = 600L) {
        animate().alpha(0f).setDuration(durationMs).start()
    }

    fun resetVisual() {
        alpha = 1f
        cycleFraction = 0f
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w <= 0 || h <= 0) return
        if (w == bgW && h == bgH) return
        bgW = w
        bgH = h

        bgShader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(0xFF0D1420.toInt(), 0xFF000000.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    private fun notifyPhaseIfNeeded(tMs: Long) {
        val phase = phaseForTime(tMs)
        if (phase != lastPhase) {
            lastPhase = phase
            onPhaseChangeListener?.onPhaseChanged(phase)
        }
    }

    private fun phaseForTime(tMs: Long): Phase {
        return when {
            tMs < inhaleMs -> Phase.INHALE
            tMs < inhaleMs + holdMs -> Phase.HOLD
            else -> Phase.EXHALE
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val base = min(width, height) * 0.36f
        val minR = base * 0.50f
        val maxR = base * 1.00f

        val tMs = (cycleFraction * cycleMs).toLong().coerceIn(0L, cycleMs)
        val phase = phaseForTime(tMs)

        val radius = when (phase) {
            Phase.INHALE -> {
                val p = (tMs.toFloat() / inhaleMs.toFloat()).coerceIn(0f, 1f)
                lerp(minR, maxR, p)
            }
            Phase.HOLD -> maxR
            Phase.EXHALE -> {
                val p = ((tMs - inhaleMs - holdMs).toFloat() / exhaleMs.toFloat()).coerceIn(0f, 1f)
                lerp(maxR, minR, p)
            }
        }

        val color = when (phase) {
            Phase.INHALE -> colorInhale
            Phase.HOLD -> colorHold
            Phase.EXHALE -> colorExhale
        }
        orbPaint.color = color

        if (phase == Phase.EXHALE) {
            val exT = ((tMs - inhaleMs - holdMs).toFloat() / exhaleMs.toFloat()).coerceIn(0f, 1f)
            dimPaint.alpha = (20 + 90 * exT).toInt().coerceIn(0, 140)
            val s = bgShader
            if (s != null) {
                dimPaint.shader = s
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
                dimPaint.shader = null
            } else {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
            }
        } else {
            dimPaint.alpha = 0
        }

        if (orbShader == null || orbShaderColor != color || abs(orbShaderRadius - radius) > 0.5f) {
            orbShaderColor = color
            orbShaderRadius = radius

            val center = ColorUtils.blendARGB(color, 0xFFFFFFFF.toInt(), 0.62f)
            val edge = ColorUtils.blendARGB(color, 0xFF050A14.toInt(), 0.25f)
            orbShader = RadialGradient(
                cx - radius * 0.28f,
                cy - radius * 0.28f,
                radius,
                intArrayOf(center, color, edge),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        orbPaint.shader = orbShader

        if (phase == Phase.HOLD) {
            val holdT = ((tMs - inhaleMs).toFloat() / holdMs.toFloat()).coerceIn(0f, 1f)
            val pulse = (0.5f + 0.5f * sin(holdT * Math.PI.toFloat() * 5f))
            auraPaint.color = colorHold
            auraPaint.alpha = (34 + 58 * pulse).toInt().coerceIn(0, 140)
            canvas.drawCircle(cx, cy, radius + dp(18f) * pulse, auraPaint)
        } else {
            auraPaint.alpha = 0
        }

        canvas.drawCircle(cx, cy, radius, orbPaint)
        orbPaint.shader = null

        val mainText: String
        val subText: String

        when (phase) {
            Phase.INHALE -> {
                mainText = "Inhale"
                subText = "4 seconds"
            }
            Phase.HOLD -> {
                val remainingMs = (inhaleMs + holdMs - tMs).coerceAtLeast(0L)
                val remainingSec = ((remainingMs + 999L) / 1000L).toInt().coerceIn(1, 7)
                mainText = remainingSec.toString()
                subText = "Hold"
            }
            Phase.EXHALE -> {
                mainText = "Exhale"
                subText = "8 seconds"
            }
        }

        val yMain = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(mainText, cx, yMain, textPaint)

        val ySub = yMain + dp(28f)
        canvas.drawText(subText, cx, ySub, subTextPaint)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
