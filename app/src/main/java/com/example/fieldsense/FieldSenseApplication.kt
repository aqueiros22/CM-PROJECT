package com.example.fieldsense

import android.app.Application
import com.cloudinary.android.MediaManager

class FieldSenseApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        val cloudinaryConfig = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
            "api_key" to BuildConfig.CLOUDINARY_API_KEY,
            "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
        )
        MediaManager.init(this, cloudinaryConfig)
    }
}