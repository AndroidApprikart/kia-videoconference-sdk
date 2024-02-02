package com.app.vc.customui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
     lateinit var micImageTop: ImageView


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
        //Added background color to differentiate between the renderers
        view.setBackgroundColor(resources.getColor(R.color.colorPrimary_kia_kandid))
        micImage = view.findViewById(R.id.mic_img)
        micImageTop = view.findViewById(R.id.mic_img_top)

        videoImage = view.findViewById(R.id.video_img)
        videoImage.visibility = View.GONE
        surfaceViewRenderer = view.findViewById(R.id.surface_view_renderer)
        streamName  = view.findViewById(R.id.stream_name)
        streamName.text = displayText

        var layoutParams  = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        surfaceViewRenderer.layoutParams = layoutParams

        surfaceViewRenderer.setOnClickListener {

        }
        if(audioActive){
            micImage.setImageResource(R.drawable.ic_mic_enabled_without_bg)
            micImage.setColorFilter(Color.parseColor("#FFFFFF"));
            micImageTop.setImageResource(R.drawable.ic_mic_enabled_without_bg)
            micImageTop.setColorFilter(Color.parseColor("#FFFFFF"));
        }else
        {
            micImage.setImageResource(R.drawable.ic_mic_disabled_wighout_bg_tint)
            micImage.setColorFilter(Color.parseColor("#FFFFFF"));
            micImageTop.setImageResource(R.drawable.ic_mic_disabled_wighout_bg_tint)
            micImageTop.setColorFilter(Color.parseColor("#FFFFFF"));
        }

        if(videoActive){
            videoImage.setImageResource(R.drawable.ic_video_enabled_without_bg)
        }else
        {
            videoImage.setImageResource(R.drawable.ic_video_disabled_without_bg)
        }
        videoImage.setColorFilter(Color.parseColor("#FFFFFF"));
        
    }

    fun changeVideoActiveStatus(incomingVideoActiveStatus : Boolean) {
        videoActive = incomingVideoActiveStatus

        if(videoActive){
            videoImage.setImageResource(R.drawable.ic_video_enabled_without_bg)
        }else
        {
            videoImage.setImageResource(R.drawable.ic_video_disabled_without_bg)
        }
        videoImage.setColorFilter(Color.parseColor("#FFFFFF"));
    }

    fun changeAudioActiveStatus(incomingAudioActiveStatus : Boolean){
        audioActive = incomingAudioActiveStatus

        if(audioActive){
            micImage.setImageResource(R.drawable.ic_mic_enabled_without_bg)
            micImageTop.setImageResource(R.drawable.ic_mic_enabled_without_bg)
        }else
        {
            micImage.setImageResource(R.drawable.ic_mic_disabled_wighout_bg_tint)
            micImageTop.setImageResource(R.drawable.ic_mic_disabled_wighout_bg_tint)
        }
        micImage.setColorFilter(Color.parseColor("#FFFFFF"));
        micImageTop.setColorFilter(Color.parseColor("#FFFFFF"));
    }

    fun addCharacterBg(){
        surfaceViewRenderer.background = createLayerDrawable(context,streamName.text.toString().first().toString())
    }
    fun removeCharacterBg(){
        surfaceViewRenderer.background = null
    }
    //function added (19Jan2024)to handle mic image visible when its in either in full screen container or in the small screen container
    fun updateMicForFContainer() {
        micImage.visibility = View.GONE
        micImageTop.visibility = View.VISIBLE
    }

    //function added (19Jan2024) to handle mic image visible when its in either in full screen container or in the small screen container
    fun updateMicForSContainer() {
        micImage.visibility = View.VISIBLE
        micImageTop.visibility = View.GONE
    }

    fun createLayerDrawable(context: Context,character: String): LayerDrawable {
        val rectangleDrawable = ShapeDrawable(RectShape())
        rectangleDrawable.paint.color = ContextCompat.getColor(context, R.color.colorPrimary_kia_kandid)

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

//    fun addCharacterBg(isRemoteRenderer:Boolean){
//        if(isRemoteRenderer) {
//            surfaceViewRenderer.background = createLayerDrawableForSmallcontainerRenderer(context,streamName.text.toString().first().toString())
//        }else {
//            surfaceViewRenderer.background = createLayerDrawableForFullScreenRenderer(context,streamName.text.toString().first().toString())
//        }
//
//
//    }
//    fun createLayerDrawableForFullScreenRenderer(context: Context,character: String): LayerDrawable {
//        val rectangleDrawable = ShapeDrawable(RectShape())
//        rectangleDrawable.paint.color = ContextCompat.getColor(context, R.color.colorPrimary)
//
//
//        val customLayout = LayoutInflater.from(context).inflate(R.layout.layout_renderer_background, null) as ViewGroup
//        val customTextView = customLayout.findViewById<TextView>(R.id.tv_first_char)
//        customTextView.text = character
//        var bitmap = createBitmapFromView(customLayout)
//
//
//        val iconDrawable = BitmapDrawable(resources, bitmap)
//        iconDrawable.gravity = android.view.Gravity.CENTER
//
//
//        return LayerDrawable(arrayOf(rectangleDrawable, iconDrawable))
//    }
//
//
//    fun createLayerDrawableForSmallcontainerRenderer(context: Context, character: String): LayerDrawable {
//        val backgroundDrawable = ShapeDrawable(RectShape())
//        backgroundDrawable.paint.color = ContextCompat.getColor(context, R.color.colorPrimary)
//        backgroundDrawable.paint.style = Paint.Style.FILL
//
//
//        val borderDrawable = ShapeDrawable(RectShape())
//        borderDrawable.paint.color = Color.WHITE // Static white color for border
//        borderDrawable.paint.style = Paint.Style.STROKE
//        borderDrawable.paint.strokeWidth = 2f // Static 2dp width for border
//
//
//        val customLayout = LayoutInflater.from(context).inflate(R.layout.layout_renderer_background, null) as ViewGroup
//        val customTextView = customLayout.findViewById<TextView>(R.id.tv_first_char)
//        customTextView.text = character
//
//
//        val iconDrawable = BitmapDrawable(resources, createBitmapFromView(customLayout))
//        iconDrawable.gravity = Gravity.CENTER
//
//
//        val layers = arrayOf<Drawable>(backgroundDrawable, borderDrawable, iconDrawable)
//        return LayerDrawable(layers)
//    }



}