package com.app.vc.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.File

class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType?,
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
) : RequestBody() {
    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength().coerceAtLeast(1L)
        var written = 0L

        val forwardingSink = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                written += byteCount
                onProgress(written, total)
            }
        }

        file.inputStream().use { input ->
            val buffered = forwardingSink.buffer()
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                buffered.write(buf, 0, read)
            }
            buffered.flush()
        }
    }
}

