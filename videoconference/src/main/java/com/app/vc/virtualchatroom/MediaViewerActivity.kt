package com.app.vc.virtualchatroom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
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

/**
 * Full-screen media viewer for chat attachments (image, video, PDF/document).
 * Opens like WhatsApp when user taps on a media message.
 */
class MediaViewerActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val okHttp = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vc_activity_media_viewer)

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "TEXT"
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME)

        val imgFullScreen: ImageView = findViewById(R.id.imgFullScreen)
        val layoutVideo: View = findViewById(R.id.layoutVideo)
        val videoView: VideoView = findViewById(R.id.videoView)
        val layoutDocument: View = findViewById(R.id.layoutDocument)
        val txtDocumentName: TextView = findViewById(R.id.txtDocumentName)
        val btnOpenDocument: Button = findViewById(R.id.btnOpenDocument)
        val txtTitle: TextView = findViewById(R.id.txtTitle)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        when (type.uppercase()) {
            "IMAGE" -> {
                imgFullScreen.visibility = View.VISIBLE
                layoutVideo.visibility = View.GONE
                layoutDocument.visibility = View.GONE
                txtTitle.text = fileName ?: "Image"
                loadImageWithAuth(imgFullScreen, url)
            }
            "VIDEO" -> {
                imgFullScreen.visibility = View.GONE
                layoutVideo.visibility = View.VISIBLE
                layoutDocument.visibility = View.GONE
                txtTitle.text = fileName ?: "Video"
                loadVideoWithAuth(videoView, url)
            }
            "FILE" -> {
                imgFullScreen.visibility = View.GONE
                layoutVideo.visibility = View.GONE
                layoutDocument.visibility = View.VISIBLE
                txtDocumentName.text = fileName ?: url.substringAfterLast('/')
                txtTitle.text = fileName ?: "Document"
                btnOpenDocument.setOnClickListener {
                    openDocumentWithAuth(url)
                }
            }
            else -> {
                imgFullScreen.visibility = View.VISIBLE
                layoutVideo.visibility = View.GONE
                layoutDocument.visibility = View.GONE
                txtTitle.text = fileName ?: "Attachment"
                loadImageWithAuth(imgFullScreen, url)
            }
        }
    }

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
        if (url.startsWith("http")) {
            scope.launch {
                val file = withContext(Dispatchers.IO) { downloadToCache(url) }
                if (file != null && !isFinishing) {
                    runOnUiThread {
                        videoView.setVideoURI(Uri.fromFile(file))
                        val mediaController = MediaController(this@MediaViewerActivity)
                        mediaController.setAnchorView(videoView)
                        videoView.setMediaController(mediaController)
                        videoView.start()
                    }
                }
            }
        } else {
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
