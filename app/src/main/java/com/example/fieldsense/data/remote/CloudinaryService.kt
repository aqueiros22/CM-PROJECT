package com.example.fieldsense.data.remote

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CloudinaryService(private val context: Context) {

    suspend fun uploadFile(visitId: Int, fileName: String, fileUri: Uri): String =
        suspendCoroutine { continuation ->
            MediaManager.get().upload(fileUri)
                .option("folder", "visits/$visitId")
                .option("public_id", fileName)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as String
                        continuation.resume(url)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(
                            Exception("Upload failed: ${error.description}")
                        )
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(
                            Exception("Upload rescheduled: ${error.description}")
                        )
                    }
                })
                .dispatch(context)
        }

    suspend fun deleteFile(publicId: String) {
    }
}