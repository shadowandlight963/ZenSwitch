package com.example.ourmajor.ui.breathing

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SessionRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(2.2f)
        color = 0x80FFFFFF.toInt()
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(2.8f)
        color = 0xCCFFFFFF.toInt()
        maskFilter = BlurMaskFilter(dp(6f), BlurMaskFilter.Blur.NORMAL)
    }

    private val oval = RectF()

    private var progress: Float = 0f

    fun setProgress(fraction: Float) {
        progress = fraction.coerceIn(0f, 1f)
        invalidate()
    }

    fun setAlphaFraction(fraction: Float) {
        val a = (255f * fraction.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        ringPaint.alpha = (a * 0.55f).toInt().coerceIn(0, 255)
        progressPaint.alpha = a
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = maxOf(ringPaint.strokeWidth, progressPaint.strokeWidth) / 2f + dp(2f)
        oval.set(pad, pad, w - pad, h - pad)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawArc(oval, -90f, 360f, false, ringPaint)
        canvas.drawArc(oval, -90f, 360f * progress, false, progressPaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
