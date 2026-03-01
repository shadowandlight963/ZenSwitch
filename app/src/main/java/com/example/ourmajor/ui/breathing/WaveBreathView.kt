package com.example.ourmajor.ui.breathing

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min
import kotlin.math.sin

class WaveBreathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Phase {
        INHALE,
        EXHALE
    }

    interface Listener {
        fun onPhaseChanged(phase: Phase)
        fun onProgress(progress: Float, phase: Phase)
    }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xB3FFFFFF.toInt()
    }

    private val oceanFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cometTrailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    private val cometGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
        alpha = 210
        maskFilter = BlurMaskFilter(dp(14f), BlurMaskFilter.Blur.NORMAL)
    }

    private val cometCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = dp(26f)
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
    private var progress: Float = 0f

    private var lastPhase: Phase? = null
    private var listener: Listener? = null

    private val inhaleMs = 6_000L
    private val exhaleMs = 6_000L
    private val cycleMs = inhaleMs + exhaleMs

    private val wavePath = Path()

    private val oceanFillPath = Path()
    private var oceanShader: Shader? = null

    private val oceanMatrix = Matrix()
    private val twoPi = (2.0 * Math.PI)

    private var geoLeft: Float = 0f
    private var geoRight: Float = 0f
    private var geoTop: Float = 0f
    private var geoBottom: Float = 0f
    private var geoW: Float = 1f
    private var geoH: Float = 1f
    private var geoCy: Float = 0f
    private var geoAmplitude: Float = 0f
    private val startAngle = (-Math.PI / 2.0)

    private var cometX: Float = 0f
    private var cometY: Float = 0f

    private val trailSize = 18
    private val trailX = FloatArray(trailSize)
    private val trailY = FloatArray(trailSize)
    private var trailIndex: Int = 0
    private var trailCount: Int = 0

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun start() {
        if (animator?.isRunning == true) return
        progress = 0f
        lastPhase = null
        notifyPhaseIfNeeded(0f)

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = cycleMs
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                progress = (it.animatedValue as Float).coerceIn(0f, 1f)
                notifyPhaseIfNeeded(progress)
                updateCometPositionAndTrail()
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
        progress = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    private fun notifyPhaseIfNeeded(progress: Float) {
        val phase = if (progress < 0.5f) Phase.INHALE else Phase.EXHALE
        if (phase != lastPhase) {
            lastPhase = phase
            listener?.onPhaseChanged(phase)
        }
        listener?.onProgress(progress, phase)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        geoLeft = paddingLeft.toFloat() + dp(6f)
        geoRight = (w - paddingRight).toFloat() - dp(6f)
        geoTop = paddingTop.toFloat() + dp(6f)
        geoBottom = (h - paddingBottom).toFloat() - dp(6f)

        geoW = (geoRight - geoLeft).coerceAtLeast(1f)
        geoH = (geoBottom - geoTop).coerceAtLeast(1f)

        geoCy = geoTop + geoH / 2f
        geoAmplitude = min(geoH * 0.22f, geoW * 0.10f)

        val steps = 180
        wavePath.reset()
        for (i in 0..steps) {
            val t = i.toFloat() / steps.toFloat()
            val x = geoLeft + geoW * t
            val angle = startAngle + (2.0 * Math.PI * t)
            val y = geoCy - geoAmplitude * sin(angle).toFloat()
            if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
        }

        oceanFillPath.reset()
        oceanFillPath.addPath(wavePath)
        oceanFillPath.lineTo(geoRight, geoBottom)
        oceanFillPath.lineTo(geoLeft, geoBottom)
        oceanFillPath.close()

        oceanShader = LinearGradient(
            0f,
            geoTop,
            0f,
            geoBottom,
            intArrayOf(0x804DB6AC.toInt(), 0x00FFFFFF),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        oceanFillPaint.shader = oceanShader

        setLayerType(LAYER_TYPE_SOFTWARE, null)
        trailIndex = 0
        trailCount = 0
        updateCometPositionAndTrail()
    }

    private fun updateCometPositionAndTrail() {
        if (width <= 0 || height <= 0) return

        val p = progress.coerceIn(0f, 1f)
        val angleBall = startAngle + (2.0 * Math.PI * p)
        cometX = geoLeft + geoW * p
        cometY = geoCy - geoAmplitude * sin(angleBall).toFloat()

        trailX[trailIndex] = cometX
        trailY[trailIndex] = cometY
        trailIndex = (trailIndex + 1) % trailSize
        if (trailCount < trailSize) trailCount++
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val s = oceanShader
        if (s != null) {
            val p = progress.coerceIn(0f, 1f)
            val dx = dp(18f) * sin(twoPi * p).toFloat()
            val dy = dp(10f) * sin(twoPi * (p * 0.7f + 0.15f)).toFloat()
            oceanMatrix.reset()
            oceanMatrix.setTranslate(dx, dy)
            s.setLocalMatrix(oceanMatrix)
        }

        canvas.drawPath(oceanFillPath, oceanFillPaint)
        canvas.drawPath(wavePath, wavePaint)

        val p = progress.coerceIn(0f, 1f)
        val phase = if (p < 0.5f) Phase.INHALE else Phase.EXHALE

        // Comet trail: newest -> oldest
        val baseR = dp(10f)
        for (i in 0 until trailCount) {
            val idx = (trailIndex - 1 - i + trailSize) % trailSize
            val t = i.toFloat() / trailSize.toFloat()
            val a = (220f * (1f - t) * (1f - t)).toInt().coerceIn(0, 220)
            cometTrailPaint.alpha = a
            val r = (baseR * (1f - 0.6f * t)).coerceAtLeast(dp(2.5f))
            canvas.drawCircle(trailX[idx], trailY[idx], r, cometTrailPaint)
        }

        // Comet head
        val glowR = dp(18f)
        val coreR = dp(5.5f)
        canvas.drawCircle(cometX, cometY, glowR, cometGlowPaint)
        canvas.drawCircle(cometX, cometY, coreR, cometCorePaint)

        val mainText = if (phase == Phase.INHALE) "Inhale" else "Exhale"
        val subText = "6 seconds"

        val cx = width / 2f
        val textTopPad = dp(18f)
        val yMainBase = cometY - dp(28f)
        val yMain = yMainBase.coerceIn(textTopPad, height.toFloat() - dp(64f))

        canvas.drawText(mainText, cx, yMain, textPaint)
        val ySub = (yMain + dp(26f)).coerceIn(textTopPad + dp(18f), height.toFloat() - dp(32f))
        canvas.drawText(subText, cx, ySub, subTextPaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
