package com.example.fieldsense.data.remote

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await

class FirebaseStorageService {
    private val storage = Firebase.storage
    private val auth = Firebase.auth

    private fun getUserStorageRef(visitId: Int, fileName: String) =
        auth.currentUser?.uid?.let { uid ->
            storage.reference
                .child("users/$uid/visits/$visitId/$fileName")
        }

    // Faz upload do ficheiro e retorna o URL público
    suspend fun uploadFile(visitId: Int, fileName: String, fileUri: Uri): String {
        val ref = getUserStorageRef(visitId, fileName)
            ?: throw Exception("User not authenticated")

        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteFile(visitId: Int, fileName: String) {
        getUserStorageRef(visitId, fileName)?.delete()?.await()
    }
}