package com.app.vc.virtualchatroom

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.app.vc.R
import com.app.vc.utils.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection

/**
 * Full-screen media viewer for chat attachments (image, video, PDF/document).
 * Opens like WhatsApp when user taps on a media message.
 */
class MediaViewerActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val okHttp = OkHttpClient()

    private var currentUrl: String = ""
    private var currentFileName: String? = null
    private var currentType: String = "TEXT"



    fun isTablet(context: Context): Boolean {
        return (context.resources.configuration.screenLayout
                and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isTablet(this)) {
            requestedOrientation = if (isTablet(
                    context = this
                )) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        setContentView(R.layout.vc_activity_media_viewer)

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }
        currentUrl = url
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "TEXT"
        currentType = type
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
        currentFileName = fileName

        val imgFullScreen: ImageView = findViewById(R.id.imgFullScreen)
        val layoutVideo: View = findViewById(R.id.layoutVideo)
        val videoView: VideoView = findViewById(R.id.videoView)
        val btnVideoReplay: View = findViewById(R.id.btnVideoReplay)
        val layoutDocument: View = findViewById(R.id.layoutDocument)
        val txtDocumentName: TextView = findViewById(R.id.txtDocumentName)
        val btnOpenDocument: Button = findViewById(R.id.btnOpenDocument)
        val txtTitle: TextView = findViewById(R.id.txtTitle)
        val btnOverflow: ImageView = findViewById(R.id.btnOverflow)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        btnOverflow.setOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menu.add(0, 1, 0, "Save")
            popup.setOnMenuItemClickListener {
                if (it.itemId == 1) {
                    saveToDevice()
                    true
                } else false
            }
            popup.show()
        }

        when (type.uppercase()) {
            "IMAGE" -> {
                imgFullScreen.visibility = View.VISIBLE
                layoutVideo.visibility = View.GONE
                layoutDocument.visibility = View.GONE
                txtTitle.text = currentFileName ?: "Image"
                loadImageWithAuth(imgFullScreen, url)
            }
            "VIDEO" -> {
                imgFullScreen.visibility = View.GONE
                layoutVideo.visibility = View.VISIBLE
                layoutDocument.visibility = View.GONE
                txtTitle.text = currentFileName ?: "Video"
                btnVideoReplay.visibility = View.GONE
                loadVideoWithAuth(videoView, url)
            }
            "FILE" -> {
                imgFullScreen.visibility = View.GONE
                layoutVideo.visibility = View.GONE
                layoutDocument.visibility = View.VISIBLE
                txtDocumentName.text = currentFileName ?: url.substringAfterLast('/')
                txtTitle.text = currentFileName ?: "Document"
                btnOpenDocument.setOnClickListener {
                    openDocumentWithAuth(url)
                }
            }
            else -> {
                imgFullScreen.visibility = View.VISIBLE
                layoutVideo.visibility = View.GONE
                layoutDocument.visibility = View.GONE
                txtTitle.text = currentFileName ?: "Attachment"
                loadImageWithAuth(imgFullScreen, url)
            }
        }
    }




    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToDevice() {
        val url = currentUrl
        val fileName = currentFileName?.takeIf { it.isNotBlank() }
            ?: url.substringAfterLast('/').substringBefore('?')
            ?: "download_${System.currentTimeMillis()}"

        scope.launch(Dispatchers.IO) {
            try {
                val token = PreferenceManager.getAccessToken()

                val request = Request.Builder()
                    .url(url)
                    .apply {
                        if (!token.isNullOrBlank()) {
                            addHeader("Authorization", "Bearer $token")
                        }
                    }
                    .build()

                val response = okHttp.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Download failed: ${response.code}")
                }

                val inputStream = response.body?.byteStream()
                    ?: throw IOException("Empty body")

                saveToPublicStorage(inputStream, fileName)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MediaViewerActivity, "Saved to device", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("MediaViewer", "Save failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MediaViewerActivity, "Save failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }





    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToPublicStorage(inputStream: InputStream, fileName: String) {

        val mimeType = URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"

        val collection = when (currentType) {
            "IMAGE" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "VIDEO" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        val relativePath = when (currentType) {
            "IMAGE" -> "${Environment.DIRECTORY_PICTURES}/KiaKandid"
            "VIDEO" -> "${Environment.DIRECTORY_MOVIES}/KiaKandid"
            else -> "${Environment.DIRECTORY_DOWNLOADS}/KiaKandid"
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(collection, values)
            ?: throw IOException("Failed to create file")

        contentResolver.openOutputStream(uri)?.use { output ->
            inputStream.copyTo(output)
        } ?: throw IOException("Failed to open output stream")

        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
    }

//    private fun saveToDevice() {
//        val url = currentUrl
//        val fileName = currentFileName?.takeIf { it.isNotBlank() }
//            ?: url.substringAfterLast('/').takeIf { it.isNotBlank() }
//            ?: "download_${System.currentTimeMillis()}"
//        scope.launch(Dispatchers.IO) {
//            try {
//                val token = PreferenceManager.getAccessToken()
//                val request = Request.Builder()
//                    .url(url)
//                    .apply { if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token") }
//                    .build()
//                val response = okHttp.newCall(request).execute()
//                if (!response.isSuccessful) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@MediaViewerActivity, "Download failed", Toast.LENGTH_SHORT).show()
//                    }
//                    return@launch
//                }
//                val body = response.body ?: return@launch
//                val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
//                } else {
//                    @Suppress("DEPRECATION")
//                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//                }
//                val file = File(dir, fileName)
//                file.outputStream().use { body.byteStream().copyTo(it) }
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@MediaViewerActivity, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                Log.e("MediaViewer", "Save failed: ${e.message}")
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@MediaViewerActivity, "Save failed", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    private fun loadImageWithAuth(imageView: ImageView, url: String) {
        if (url.startsWith("http")) {
            val token = PreferenceManager.getAccessToken()
            val glideUrl = if (!token.isNullOrBlank()) {
                GlideUrl(
                    url,
                    LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                )
            } else {
                GlideUrl(url, LazyHeaders.Builder().build())
            }
            Glide.with(this).load(glideUrl).fitCenter().into(imageView)
        } else {
            Glide.with(this).load(File(url)).fitCenter().into(imageView)
        }
    }

    private fun loadVideoWithAuth(videoView: VideoView, url: String) {
        // Fit video inside the available space on tablets/phones without stretching.
        videoView.setOnPreparedListener { mp ->
            try {
                val videoW = mp.videoWidth
                val videoH = mp.videoHeight
                if (videoW > 0 && videoH > 0) {
                    val container = findViewById<View>(R.id.layoutVideo)
                    container.post {
                        val maxW = container.width.takeIf { it > 0 } ?: return@post
                        val maxH = container.height.takeIf { it > 0 } ?: return@post
                        val videoRatio = videoW.toFloat() / videoH.toFloat()
                        val containerRatio = maxW.toFloat() / maxH.toFloat()

                        val targetW: Int
                        val targetH: Int
                        if (videoRatio > containerRatio) {
                            targetW = maxW
                            targetH = (maxW / videoRatio).toInt()
                        } else {
                            targetH = maxH
                            targetW = (maxH * videoRatio).toInt()
                        }

                        val lp = videoView.layoutParams
                        lp.width = targetW
                        lp.height = targetH
                        videoView.layoutParams = lp
                    }
                }
            } catch (_: Exception) {
            }
        }
        videoView.setOnCompletionListener {
            findViewById<View>(R.id.btnVideoReplay)?.visibility = View.VISIBLE
        }

        if (url.startsWith("http")) {
            scope.launch {
                val file = withContext(Dispatchers.IO) { downloadToCache(url) }
                if (file != null && !isFinishing) {
                    runOnUiThread {
                        findViewById<View>(R.id.btnVideoReplay)?.visibility = View.GONE
                        videoView.setVideoURI(Uri.fromFile(file))
                        val mediaController = MediaController(this@MediaViewerActivity)
                        mediaController.setAnchorView(videoView)
                        videoView.setMediaController(mediaController)
                        videoView.start()
                    }
                }
            }
        } else {
            findViewById<View>(R.id.btnVideoReplay)?.visibility = View.GONE
            videoView.setVideoURI(Uri.fromFile(File(url)))
            val mediaController = MediaController(this)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
            videoView.start()
        }
    }

    private suspend fun downloadToCache(url: String): File? = withContext(Dispatchers.IO) {
        try {
            val token = PreferenceManager.getAccessToken()
            val request = Request.Builder()
                .url(url)
                .apply { if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token") }
                .build()
            val response = okHttp.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            val ext = url.substringAfterLast('.', "").takeIf { it.length in 1..4 } ?: "tmp"
            val file = File(cacheDir, "media_${System.currentTimeMillis()}.$ext")
            file.outputStream().use { body.byteStream().copyTo(it) }
            file
        } catch (_: Exception) {
            null
        }
    }

    private fun openDocumentWithAuth(url: String) {
        if (url.startsWith("http")) {
            scope.launch {
                val file = withContext(Dispatchers.IO) { downloadToCache(url) }
                if (file != null && !isFinishing) {
                    runOnUiThread {
                        try {
                            val uri = FileProvider.getUriForFile(
                                this@MediaViewerActivity,
                                "${packageName}.fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, if (url.contains(".pdf", ignoreCase = true)) "application/pdf" else "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(Intent.createChooser(intent, "Open with"))
                        } catch (_: Exception) {
                            finish()
                        }
                    }
                }
            }
        } else {
            val uri = Uri.fromFile(File(url))
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (url.contains(".pdf", ignoreCase = true)) "application/pdf" else "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(Intent.createChooser(intent, "Open with"))
            } catch (_: Exception) {
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TYPE = "type"
        const val EXTRA_FILE_NAME = "file_name"
    }
}
