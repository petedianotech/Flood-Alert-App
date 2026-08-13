package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FloodRepository(private val context: Context) {
    private val firestore by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Throwable) {
                    Log.w(TAG, "FirebaseApp initializeApp failed", e)
                }
            }
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                Log.w(TAG, "FirebaseApp is not initialized, skipping Firestore instance creation")
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore init failed: ${e.message}")
            null
        }
    }

    private val messaging by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Throwable) {
                    Log.w(TAG, "FirebaseApp initializeApp failed", e)
                }
            }
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseMessaging.getInstance()
            } else {
                Log.w(TAG, "FirebaseApp is not initialized, skipping FirebaseMessaging instance creation")
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseMessaging init failed: ${e.message}")
            null
        }
    }

    init {
        subscribeToFloodTopic()
    }

    fun subscribeToFloodTopic() {
        try {
            messaging?.subscribeToTopic("flood_alerts")
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Subscribed to FCM topic: flood_alerts")
                    } else {
                        Log.e(TAG, "Failed to subscribe to flood_alerts topic", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "FCM subscription error", e)
        }
    }

    fun publishAlert(alert: FloodAlert, onComplete: (Boolean) -> Unit = {}) {
        try {
            val fs = firestore
            if (fs != null) {
                val documentRef = fs.collection("flood_alerts").document()
                val alertData = alert.copy(id = documentRef.id)
                documentRef.set(alertData.toFirestoreMap())
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully published flood alert: ${documentRef.id}")
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error publishing flood alert to Firestore", e)
                        onComplete(false)
                    }
            } else {
                onComplete(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating Firestore write", e)
            onComplete(false)
        }
    }

    fun getRecentAlertsFlow(): Flow<List<FloodAlert>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            val fs = firestore
            if (fs != null) {
                listenerRegistration = fs.collection("flood_alerts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(20)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Firestore snapshot listener failed", error)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val alerts = snapshot.documents.mapNotNull { doc ->
                                doc.data?.let { data -> FloodAlert.fromFirestoreMap(doc.id, data) }
                            }
                            trySend(alerts)
                        }
                    }
            } else {
                trySend(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Firestore listener", e)
            trySend(emptyList())
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    companion object {
        private const val TAG = "FloodRepository"
    }
}
