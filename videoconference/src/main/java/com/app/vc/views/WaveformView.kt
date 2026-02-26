package com.app.vc.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.app.vc.R

/**
 * WhatsApp-style waveform: vertical bars whose height depends on amplitude/frequency.
 * Use [setAmplitudes] to update (e.g. while recording or for playback).
 * Optionally set [playedRatio] (0f..1f) to highlight played portion during playback.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_kia_black)
        style = Paint.Style.FILL
    }
    private val playedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.self_time_color)
        style = Paint.Style.FILL
    }
    private val rect = RectF()

    /** Amplitudes (0f..1f). Number of bars = size. */
    var amplitudes: FloatArray = FloatArray(0)
        set(value) {
            field = value
            invalidate()
        }

    /** For playback: ratio of waveform that is "played" (0f..1f). Drawn with [playedPaint]. */
    var playedRatio: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    /** Add a new amplitude (e.g. each 50ms while recording). Clamps to [maxBars]. */
    fun addAmplitude(amplitude: Float) {
        val maxBars = 80
        val a = amplitude.coerceIn(0.05f, 1f)
        amplitudes = if (amplitudes.size >= maxBars) {
            amplitudes.drop(1).toFloatArray() + a
        } else {
            amplitudes + a
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val count = amplitudes.size
        if (count == 0) return
        val barWidth = (w / count) * 0.6f
        val gap = (w / count) * 0.4f
        val centerY = h / 2f
        for (i in 0 until count) {
            val amp = amplitudes[i]
            val barHeight = (h * 0.4f) * amp
            val left = i * (barWidth + gap)
            rect.set(left, centerY - barHeight / 2f, left + barWidth, centerY + barHeight / 2f)
            val playedBound = playedRatio * count
            if (i < playedBound) canvas.drawRoundRect(rect, 2f, 2f, playedPaint)
            else canvas.drawRoundRect(rect, 2f, 2f, barPaint)
        }
    }
}
