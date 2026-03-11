package com.example.fieldsense.data

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private fun getUserVisitsCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("users").document(uid).collection("visits")
    }

    suspend fun uploadVisit(visit: Visit) {
        getUserVisitsCollection()?.document(visit.id.toString())?.set(visit)?.await()
    }

    suspend fun deleteVisit(visitId: Int) {
        getUserVisitsCollection()?.document(visitId.toString())?.delete()?.await()
    }
}