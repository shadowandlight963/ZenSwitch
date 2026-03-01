package com.example.ourmajor.ui.breathing

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class ZenBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Style {
        FOCUS,
        SLEEP,
        WAVE
    }

    enum class Phase {
        INHALE,
        HOLD,
        EXHALE
    }

    private var style: Style = Style.FOCUS
    private var phase: Phase = Phase.INHALE

    private var lastFrameNs: Long = 0L
    private var running: Boolean = false

    private var driftT: Float = 0f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgMatrix = Matrix()
    private var bgShader: LinearGradient? = null
    private var bgShaderW: Int = 0
    private var bgShaderH: Int = 0
    private var bgShaderKey: Int = 0

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    private val softParticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
        alpha = 120
        maskFilter = BlurMaskFilter(dp(10f), BlurMaskFilter.Blur.NORMAL)
    }

    private val bubbleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
        color = 0xCCFFFFFF.toInt()
        alpha = 120
    }

    private val diamondPath = Path().apply {
        moveTo(0f, -1f)
        lineTo(1f, 0f)
        lineTo(0f, 1f)
        lineTo(-1f, 0f)
        close()
    }

    private val particleCount = 56
    private val px = FloatArray(particleCount)
    private val py = FloatArray(particleCount)
    private val pvx = FloatArray(particleCount)
    private val pvy = FloatArray(particleCount)
    private val pr = FloatArray(particleCount)
    private val pa = FloatArray(particleCount)
    private val prot = FloatArray(particleCount)

    private var brightness: Float = 0.82f

    fun setStyle(style: Style) {
        if (this.style == style) return
        this.style = style
        resetParticles()
        invalidateShader()
        invalidate()
    }

    fun setPhase(phase: Phase) {
        if (this.phase == phase) return
        this.phase = phase
        invalidateShader()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        lastFrameNs = 0L
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        running = false
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetParticles()
        invalidateShader()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun invalidateShader() {
        bgShaderW = 0
        bgShaderH = 0
        bgShader = null
    }

    private fun resetParticles() {
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        val seedBase = (style.ordinal + 1) * 97 + (phase.ordinal + 1) * 31
        var seed = 0x1234ABCD.toInt() xor seedBase

        for (i in 0 until particleCount) {
            seed = seed * 1664525 + 1013904223
            val rx = ((seed ushr 9) and 0x7FFF) / 32767f
            seed = seed * 1664525 + 1013904223
            val ry = ((seed ushr 9) and 0x7FFF) / 32767f
            seed = seed * 1664525 + 1013904223
            val rr = ((seed ushr 9) and 0x7FFF) / 32767f
            seed = seed * 1664525 + 1013904223
            val ra = ((seed ushr 9) and 0x7FFF) / 32767f
            seed = seed * 1664525 + 1013904223
            val rrot = ((seed ushr 9) and 0x7FFF) / 32767f

            px[i] = rx * w
            py[i] = ry * h

            val baseV = when (style) {
                Style.FOCUS -> dp(8f)
                Style.SLEEP -> dp(5.5f)
                Style.WAVE -> dp(10f)
            }

            pvx[i] = (rr - 0.5f) * baseV * 0.35f
            pvy[i] = -(0.6f + rr * 0.9f) * baseV

            pr[i] = when (style) {
                Style.FOCUS -> dp(1.8f + rr * 1.8f)
                Style.SLEEP -> dp(2.5f + rr * 3.8f)
                Style.WAVE -> dp(2.2f + rr * 4.6f)
            }

            pa[i] = (70 + 120 * ra).coerceIn(60f, 190f)
            prot[i] = rrot * 360f
        }
    }

    private fun ensureShader() {
        if (width <= 0 || height <= 0) return
        if (bgShader != null && bgShaderW == width && bgShaderH == height) return

        bgShaderW = width
        bgShaderH = height

        val inhaleA = when (style) {
            Style.FOCUS -> 0xFF2AF2FF.toInt()
            Style.SLEEP -> 0xFF5D7CFF.toInt()
            Style.WAVE -> 0xFF4DB6AC.toInt()
        }
        val holdA = when (style) {
            Style.FOCUS -> 0xFF1D2E8A.toInt()
            Style.SLEEP -> 0xFF2B1A50.toInt()
            Style.WAVE -> 0xFF193B54.toInt()
        }
        val exhaleA = when (style) {
            Style.FOCUS -> 0xFF0E4A2F.toInt()
            Style.SLEEP -> 0xFF060A14.toInt()
            Style.WAVE -> 0xFF051428.toInt()
        }

        val base = when (phase) {
            Phase.INHALE -> inhaleA
            Phase.HOLD -> holdA
            Phase.EXHALE -> exhaleA
        }

        val c1 = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(base, 0xFF000000.toInt(), 0.15f), 255)
        val c2 = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(base, 0xFFFFFFFF.toInt(), 0.10f), 255)
        val c3 = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(holdA, 0xFF000000.toInt(), 0.40f), 255)

        bgShaderKey = base xor (style.ordinal shl 8)

        bgShader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(c1, c2, c3),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = bgShader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.nanoTime()
        val dt = if (lastFrameNs == 0L) 0f else ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNs = now

        val target = when (phase) {
            Phase.INHALE -> 0.98f
            Phase.HOLD -> 0.88f
            Phase.EXHALE -> 0.72f
        }
        brightness += (target - brightness) * (1f - 0.001f.pow(dt))

        driftT += dt
        ensureShader()

        val cx = width / 2f
        val cy = height / 2f
        val rotDeg = (driftT * 4.5f) % 360f
        val dx = dp(22f) * sin(driftT * 0.25f)
        val dy = dp(16f) * cos(driftT * 0.21f)

        bgMatrix.reset()
        bgMatrix.setRotate(rotDeg, cx, cy)
        bgMatrix.postTranslate(dx, dy)
        bgShader?.setLocalMatrix(bgMatrix)

        val alpha = (255f * brightness).toInt().coerceIn(0, 255)
        bgPaint.alpha = alpha
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        updateAndDrawParticles(canvas, dt)

        if (running) {
            postInvalidateOnAnimation()
        }
    }

    private fun updateAndDrawParticles(canvas: Canvas, dt: Float) {
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        when (style) {
            Style.FOCUS -> {
                particlePaint.alpha = 200
                for (i in 0 until particleCount) {
                    px[i] += pvx[i] * dt
                    py[i] += pvy[i] * dt
                    prot[i] += 24f * dt

                    if (py[i] < -dp(20f)) py[i] = h + dp(20f)
                    if (px[i] < -dp(20f)) px[i] = w + dp(20f)
                    if (px[i] > w + dp(20f)) px[i] = -dp(20f)

                    particlePaint.alpha = (pa[i] * 0.95f).toInt().coerceIn(40, 200)
                    canvas.save()
                    canvas.translate(px[i], py[i])
                    canvas.rotate(prot[i])
                    val s = pr[i]
                    canvas.scale(s, s)
                    canvas.drawPath(diamondPath, particlePaint)
                    canvas.restore()
                }
            }

            Style.SLEEP -> {
                for (i in 0 until particleCount) {
                    px[i] += pvx[i] * dt
                    py[i] += pvy[i] * dt

                    if (py[i] < -dp(30f)) py[i] = h + dp(30f)
                    if (px[i] < -dp(30f)) px[i] = w + dp(30f)
                    if (px[i] > w + dp(30f)) px[i] = -dp(30f)

                    softParticlePaint.alpha = (pa[i] * 0.70f).toInt().coerceIn(20, 120)
                    canvas.drawCircle(px[i], py[i], pr[i] * 1.6f, softParticlePaint)
                }
            }

            Style.WAVE -> {
                for (i in 0 until particleCount) {
                    px[i] += pvx[i] * dt
                    py[i] += pvy[i] * dt

                    if (py[i] < -dp(40f)) {
                        py[i] = h + dp(20f)
                        px[i] = (px[i] + w * 0.37f) % w
                    }

                    val a = (pa[i] * 0.55f).toInt().coerceIn(15, 120)
                    bubbleStrokePaint.alpha = a
                    canvas.drawCircle(px[i], py[i], pr[i] * 1.3f, bubbleStrokePaint)
                }
            }
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
