package com.app.vc

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.*


/**
 * Created by gopal on 1/3/21.
 */
class FileOperations {
    companion object{

        fun getPath(context: Context?, uri: Uri): String? {
            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }

                } else if (isDownloadsDocument(uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    Log.d("FileOperations::", "isDownloadsDocument: getPath: id: $id")
                    /*val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))*/
                    /*val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), id.toLong())
                    return getDataColumn(context!!, contentUri, null, null)*/

                    /*return try {
                        val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), id.toLong())
                        getDataColumn(context!!, contentUri, null, null)
                    }catch (e: Exception){
                        val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/my_downloads"), id.toLong())
                        getDataColumn(context!!, contentUri, null, null)
                    }*/

                    if (id != null && id.startsWith("raw:")) {
                        return id.substring(4)
                    }

                    val contentUriPrefixesToTry =
                        arrayOf(
                            "content://downloads/public_downloads",
                            "content://downloads/my_downloads"
                        )

                    for (contentUriPrefix in contentUriPrefixesToTry) {
                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse(contentUriPrefix),
                            java.lang.Long.valueOf(id!!)
                        )
                        try {
                            val path =
                                getDataColumn(context!!, contentUri, null, null)
                            if (path != null) {
                                return path
                            }
                        } catch (e: Exception) {
                        }
                    }

                    return null

                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf<String?>(
                            split[1]
                    )
                    return getDataColumn(context!!, contentUri, selection, selectionArgs)
                }
            } else if (isGoogleDriveUri(uri)) {
                return getImagePathFromInputStreamUri(context!!, uri)
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                return getDataColumn(context!!, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        /*fun getPathOld(context: Context?, uri: Uri): String? {
            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }

                } else if (isDownloadsDocument(uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                    return getDataColumn(context!!, contentUri, null, null)
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf<String?>(
                            split[1]
                    )
                    return getDataColumn(context!!, contentUri, selection, selectionArgs)
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                return getDataColumn(context!!, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }*/

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context The context.
         * @param uri The Uri to query.
         * @param selection (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                          selectionArgs: Array<String?>?): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(
                    column
            )
            try {
                cursor = context.getContentResolver().query(uri!!, projection, selection, selectionArgs,
                        null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex: Int = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(columnIndex)
                }
            } finally {
                if (cursor != null) cursor.close()
            }
            return null
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        private fun isGoogleDriveUri(uri: Uri): Boolean {
            return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
        }

        fun getFileExtension(filePath: String?): String{
            return if (filePath!=null && filePath.contains(".")){
                val extensionWithDot = filePath.substring(filePath.lastIndexOf("."))
                if (extensionWithDot.length>1){
                    extensionWithDot.replace(".","")
                }else
                    ""
            }else{
                ""
            }
        }

        fun getImagePathFromInputStreamUri(context: Context?,uri: Uri): String? {
            var inputStream: InputStream? = null
            var filePath: String? = null
            var name: String? = ""
            if (uri.authority != null) {
                try {
                    if (context != null) {
                        inputStream = context.contentResolver.openInputStream(uri)
                        val returnCursor =
                            context!!.contentResolver.query(uri, null, null, null, null)
                        /*
                 * Get the column indexes of the data in the Cursor,
                 *     * move to the first row in the Cursor, get the data,
                 *     * and display it.
                 * */
                        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        returnCursor.moveToFirst()
                        name = returnCursor.getString(nameIndex)
                    } // context needed
                    val photoFile = createTemporalFileFrom(inputStream,context, name)
                    filePath = photoFile!!.path
                } catch (e: FileNotFoundException) {
                    // log
                } catch (e: IOException) {
                    // log
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            return filePath
        }

        /* Creating a temporary file from the URI data  and then sharing its path -> this is implemented for the URI's that raise exception while getting real path*/

        @Throws(IOException::class)
        private fun createTemporalFileFrom(inputStream: InputStream?, context: Context?, fileName: String?): File? {
            var targetFile: File? = null
            if (inputStream != null) {
                var read: Int=0
                val buffer = ByteArray(8 * 1024)
                targetFile = createTemporalFile(context, fileName)
                val outputStream: OutputStream = FileOutputStream(targetFile)
                while (inputStream.read(buffer).also({ read = it }) != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return targetFile
        }
        private fun createTemporalFile(context: Context?, fileName: String?): File? {
            return File(context!!.externalCacheDir, fileName!!) // context needed
//            return File(context.externalCacheDir, "_profile_temp_file.jpg") // context needed
        }
    }
}