package com.example.ourmajor.ui.breathing

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.ourmajor.R
import com.google.android.material.color.MaterialColors
import kotlin.math.floor
import kotlin.math.min

class BoxBreathingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Phase {
        INHALE,
        HOLD_1,
        EXHALE,
        HOLD_2
    }

    interface OnPhaseChangeListener {
        fun onPhaseChanged(phase: Phase)
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(10f)
        color = 0x1A000000
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(10f)
        color = ContextCompat.getColor(context, R.color.zen_ocean_blue)
    }

    private val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.zen_ocean_blue)
        maskFilter = BlurMaskFilter(dp(10f), BlurMaskFilter.Blur.NORMAL)
        alpha = 220
    }

    private val dotCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = dp(20f)
        typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
        setShadowLayer(dp(2f), 0f, dp(1f), 0x66000000)
    }

    private val rect = RectF()
    private val path = Path()
    private val progressPath = Path()
    private var pathMeasure: PathMeasure? = null
    private var pathLength: Float = 0f

    private val dotPos = FloatArray(2)

    private var sweepShader: Shader? = null
    private var trackShader: Shader? = null

    private val sweepMatrix = Matrix()
    private var centerX: Float = 0f
    private var centerY: Float = 0f

    private val inhaleAccent: Int = 0xFF4DEBFF.toInt()
    private val holdAccent: Int = 0xFF3A4DFF.toInt()
    private val exhaleAccent: Int by lazy {
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
    }

    private var animator: ValueAnimator? = null
    private var progressFraction: Float = 0f

    private var onPhaseChangeListener: OnPhaseChangeListener? = null
    private var lastPhase: Phase? = null

    private var phaseDurationMs: Long = 4_000L

    private val argbEvaluator = ArgbEvaluator()
    private val phaseColors: IntArray by lazy {
        intArrayOf(
            ContextCompat.getColor(context, R.color.zen_ocean_blue),
            ContextCompat.getColor(context, R.color.zen_ocean_teal),
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary),
            ContextCompat.getColor(context, R.color.zen_ocean_teal)
        )
    }

    fun setOnPhaseChangeListener(listener: OnPhaseChangeListener?) {
        onPhaseChangeListener = listener
    }

    fun start() {
        if (animator?.isRunning == true) return

        progressFraction = 0f
        lastPhase = null
        notifyPhaseIfNeeded()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = phaseDurationMs * 4
            interpolator = android.view.animation.LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                progressFraction = it.animatedValue as Float
                notifyPhaseIfNeeded()
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
        progressFraction = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    private fun notifyPhaseIfNeeded() {
        val phase = phaseForFraction(progressFraction)
        if (phase != lastPhase) {
            lastPhase = phase
            onPhaseChangeListener?.onPhaseChanged(phase)
        }

        val color = currentStrokeColor(progressFraction)
        progressPaint.color = color
        dotGlowPaint.color = color

        textPaint.color = when (phase) {
            Phase.INHALE -> inhaleAccent
            Phase.EXHALE -> exhaleAccent
            Phase.HOLD_1, Phase.HOLD_2 -> holdAccent
        }
    }

    private fun phaseForFraction(fraction: Float): Phase {
        val side = floor((fraction * 4f).toDouble()).toInt().coerceIn(0, 3)
        return when (side) {
            0 -> Phase.INHALE
            1 -> Phase.HOLD_1
            2 -> Phase.EXHALE
            else -> Phase.HOLD_2
        }
    }

    private fun instructionForPhase(phase: Phase): String {
        return when (phase) {
            Phase.INHALE -> "Inhale"
            Phase.HOLD_1 -> "Hold"
            Phase.EXHALE -> "Exhale"
            Phase.HOLD_2 -> "Hold"
        }
    }

    private fun currentStrokeColor(fraction: Float): Int {
        val pos = fraction * 4f
        val side = floor(pos.toDouble()).toInt().coerceIn(0, 3)
        val t = (pos - side).coerceIn(0f, 1f)
        val c1 = phaseColors[side]
        val c2 = phaseColors[(side + 1) % 4]
        return argbEvaluator.evaluate(t, c1, c2) as Int
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val pad = dp(24f)
        val size = min(w, h).toFloat() - pad * 2
        val left = (w - size) / 2f
        val top = (h - size) / 2f
        rect.set(left, top, left + size, top + size)

        val r = dp(32f)
        path.reset()
        path.addRoundRect(rect, r, r, Path.Direction.CW)
        pathMeasure = PathMeasure(path, true)
        pathLength = pathMeasure?.length ?: 0f

        val cx = w / 2f
        val cy = h / 2f
        centerX = cx
        centerY = cy

        val inhaleC = inhaleAccent
        val holdC = holdAccent
        val exhaleC = exhaleAccent
        sweepShader = SweepGradient(
            cx,
            cy,
            intArrayOf(inhaleC, holdC, exhaleC, holdC, inhaleC),
            floatArrayOf(0f, 0.25f, 0.50f, 0.75f, 1f)
        )
        progressPaint.shader = sweepShader

        trackShader = SweepGradient(
            cx,
            cy,
            intArrayOf(
                (inhaleC and 0x00FFFFFF) or 0x55000000,
                (holdC and 0x00FFFFFF) or 0x55000000,
                (exhaleC and 0x00FFFFFF) or 0x55000000,
                (holdC and 0x00FFFFFF) or 0x55000000,
                (inhaleC and 0x00FFFFFF) or 0x55000000
            ),
            floatArrayOf(0f, 0.25f, 0.50f, 0.75f, 1f)
        )
        trackPaint.shader = trackShader
        trackPaint.alpha = 140

        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val sweep = sweepShader
        val track = trackShader
        if (sweep != null || track != null) {
            val angle = progressFraction * 360f

            if (sweep != null) {
                sweepMatrix.reset()
                sweepMatrix.setRotate(angle, centerX, centerY)
                sweep.setLocalMatrix(sweepMatrix)
            }

            if (track != null) {
                sweepMatrix.reset()
                sweepMatrix.setRotate(angle * 0.35f, centerX, centerY)
                track.setLocalMatrix(sweepMatrix)
            }
        }

        canvas.drawPath(path, trackPaint)

        val pm = pathMeasure ?: return
        val stop = (progressFraction * pathLength).coerceIn(0f, pathLength)

        progressPath.reset()
        pm.getSegment(0f, stop, progressPath, true)
        canvas.drawPath(progressPath, progressPaint)

        pm.getPosTan(stop, dotPos, null)
        val glowR = dp(14f)
        val coreR = dp(6.5f)
        canvas.drawCircle(dotPos[0], dotPos[1], glowR, dotGlowPaint)
        canvas.drawCircle(dotPos[0], dotPos[1], coreR, dotCorePaint)

        val phase = lastPhase ?: phaseForFraction(progressFraction)
        val text = instructionForPhase(phase)
        val cx = width / 2f
        val cy = height / 2f
        val y = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, cx, y, textPaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
