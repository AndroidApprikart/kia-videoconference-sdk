package com.app.vc.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.app.vc.R


class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barWidth = 4f
    private val barGap = 1.5f
    private val totalBarSpace = barWidth + barGap
    private val recordedAmplitudes = mutableListOf<Int>()

    private val amplitudes = IntArray(50) { 0 } // ALWAYS FIXED
    private var progressIndex = 0
    private var playbackAnimating = false
    private var animationFactor = 1f
    private var animator: ValueAnimator? = null
    private val greyPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val blackPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    /* ---------- APIs ---------- */

    /** Used ONLY while recording */
    fun addAmplitude(amp: Int) {

        // shift bars left
        for (i in 0 until amplitudes.size - 1) {
            amplitudes[i] = amplitudes[i + 1]
        }

        amplitudes[amplitudes.lastIndex] = amp

        progressIndex = amplitudes.size

        invalidate()
    }

    /** Used in parent bottom sheet */
    fun setAmplitudes(data: IntArray) {
        require(data.size == amplitudes.size) {
            "Waveform must be exactly ${amplitudes.size} bars"
        }
        data.copyInto(amplitudes)
        progressIndex = 0
        invalidate()
    }

    fun resetProgress() {
        progressIndex = 0
        invalidate()
    }

    fun updateProgress(index: Int) {
        progressIndex = index.coerceIn(0, amplitudes.size)
        invalidate()
    }

    fun clear() {
        amplitudes.fill(10)
        progressIndex = 0
        invalidate()
    }


    fun startPlaybackAnimation() {
        playbackAnimating = true
        animator?.cancel()

        animator = ValueAnimator.ofFloat(0.85f, 1.15f).apply {
            duration = 300
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                animationFactor = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopPlaybackAnimation() {
        playbackAnimating = false
        animator?.cancel()
        animationFactor = 1f
        invalidate()
    }
    /* ---------- Drawing ---------- */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerY = height / 2f
        val minHeight = 6f
        val maxHeight = height / 3f

        val scale = 0.9f

        for (i in amplitudes.indices) {

            val rawHeight = amplitudes[i] * scale

            val lineHeight = rawHeight
                .coerceAtLeast(minHeight)
                .coerceAtMost(maxHeight)

            val x = i * totalBarSpace + barWidth / 2

            val paint = if (i < progressIndex) blackPaint else greyPaint

            canvas.drawLine(
                x,
                centerY - lineHeight,
                x,
                centerY + lineHeight,
                paint
            )
        }
    }
}