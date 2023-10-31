package com.app.vc.customui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.app.vc.R
import org.webrtc.SurfaceViewRenderer

/* created by Naghma 04/10/23*/


class RemotePeerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

     lateinit var micImage:ImageView
     lateinit var videoImage:ImageView
     lateinit var surfaceViewRenderer: SurfaceViewRenderer
     lateinit var streamName:TextView


     var videoActive = false
     var audioActive = false
     var displayText =  ""

    private val attributes: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.RemotePeerView)
    private val view: View = View.inflate(context, R.layout.layout_remote_peer, this)

    init {
        displayText = attributes.getString(R.styleable.RemotePeerView_display_text).toString()
        audioActive = attributes.getBoolean(R.styleable.RemotePeerView_audio_active,false)
        videoActive = attributes.getBoolean(R.styleable.RemotePeerView_video_active,false)
        attributes.recycle()
        micImage = view.findViewById(R.id.mic_img)
        videoImage = view.findViewById(R.id.video_img)
        surfaceViewRenderer = view.findViewById(R.id.surface_view_renderer)
        streamName  = view.findViewById(R.id.stream_name)
        streamName.text = displayText
        surfaceViewRenderer.setOnClickListener {

        }
        if(audioActive){
            micImage.setImageResource(R.drawable.ic_mic_on)
        }else
        {
            micImage.setImageResource(R.drawable.ic_mic_off)
        }

        if(videoActive){
            videoImage.setImageResource(R.drawable.ic_video_on)
        }else
        {
            videoImage.setImageResource(R.drawable.ic_video_off)
        }
        
    }

    fun changeVideoActiveStatus(incomingVideoActiveStatus : Boolean) {
        videoActive = incomingVideoActiveStatus
        if(videoActive){
            videoImage.setImageResource(R.drawable.ic_video_on)
        }else
        {
            videoImage.setImageResource(R.drawable.ic_video_off)
        }
    }

    fun changeAudioActiveStatus(incomingAudioActiveStatus : Boolean){
        audioActive = incomingAudioActiveStatus
        if(audioActive){
            micImage.setImageResource(R.drawable.ic_mic_on)
        }else
        {
            micImage.setImageResource(R.drawable.ic_mic_off)
        }
    }

    fun addCharacterBg(){
        surfaceViewRenderer.background = createLayerDrawable(context,streamName.text.toString().first().toString())
    }
    fun removeCharacterBg(){
        surfaceViewRenderer.background = null
    }

    fun createLayerDrawable(context: Context,character: String): LayerDrawable {
        val rectangleDrawable = ShapeDrawable(RectShape())
        rectangleDrawable.paint.color = ContextCompat.getColor(context, R.color.color_renderer_background)

        val customLayout = LayoutInflater.from(context).inflate(R.layout.layout_renderer_background, null) as ViewGroup
        val customTextView = customLayout.findViewById<TextView>(R.id.tv_first_char)
        customTextView.text = character
        var bitmap = createBitmapFromView(customLayout)

        val iconDrawable = BitmapDrawable(resources, bitmap)
        iconDrawable.gravity = android.view.Gravity.CENTER

        return LayerDrawable(arrayOf(rectangleDrawable, iconDrawable))
    }

    private fun createBitmapFromView(view: View): Bitmap {
        // Measure and layout the view
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // Create a bitmap
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }


}