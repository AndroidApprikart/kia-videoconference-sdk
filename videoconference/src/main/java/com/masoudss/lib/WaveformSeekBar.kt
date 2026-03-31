package com.masoudss.lib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.AttributeSet

/**
 * Lightweight internal fallback for waveform rendering in SDK/AAR integrations.
 * It keeps the same API surface used by the project.
 */
class WaveformSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : android.view.View(context, attrs, defStyleAttr) {

    var waveWidth: Float = 0f
    var waveGap: Float = 0f
    var waveMinHeight: Float = 0f

    var waveBackgroundColor: Int = Color.LTGRAY
        set(value) {
            field = value
            invalidate()
        }
    var waveProgressColor: Int = Color.DKGRAY
        set(value) {
            field = value
            invalidate()
        }

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate()
        }

    var sample: IntArray? = null
        private set

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun setSampleFrom(uri: Uri) {
        if (sample == null) {
            sample = intArrayOf(1)
        }
        invalidate()
    }

    fun setSampleFrom(values: IntArray) {
        sample = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val w = width.toFloat()
        if (w <= 0f || h <= 0f) return

        paint.color = waveBackgroundColor
        canvas.drawRoundRect(0f, h * 0.35f, w, h * 0.65f, h * 0.25f, h * 0.25f, paint)

        paint.color = waveProgressColor
        val pw = (w * (progress / 100f)).coerceIn(0f, w)
        canvas.drawRoundRect(0f, h * 0.35f, pw, h * 0.65f, h * 0.25f, h * 0.25f, paint)
    }
}
