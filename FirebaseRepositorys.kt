package com.example.clanhub.serviProviderRegisScrn2.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseRepositorys {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private suspend fun ensureAuthenticated(): Boolean {
        return try {
            if (auth.currentUser == null) {
                Log.d("FirebaseRepository", "No authenticated user, signing in anonymously...")
                auth.signInAnonymously().await()
                Log.d("FirebaseRepository", "Anonymous sign-in successful")
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("FirebaseRepository", "User authenticated: ${currentUser.uid}")
                return true
            } else {
                Log.e("FirebaseRepository", "Authentication failed - no user")
                return false
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to authenticate user", e)
            // Try without authentication for development
            Log.w("FirebaseRepository", "Proceeding without authentication for development mode")
            return true
        }
    }

    private suspend fun uploadImageToStorage(
        imageUri: Uri,
        path: String,
        context: Context
    ): String? {
        return try {
            Log.d("FirebaseRepository", "Starting image upload to path: $path")

            val storageRef = storage.reference.child(path)

            // Convert URI to byte array for upload
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                Log.d("FirebaseRepository", "Image size: ${bytes.size} bytes")

                // Upload the image
                val uploadTask = storageRef.putBytes(bytes).await()
                Log.d("FirebaseRepository", "Upload successful, getting download URL...")

                // Get download URL
                val downloadUrl = storageRef.downloadUrl.await()
                Log.d("FirebaseRepository", "Image uploaded successfully: $downloadUrl")
                downloadUrl.toString()
            } else {
                Log.e("FirebaseRepository", "Failed to read image bytes")
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error uploading image to storage: ${e.message}", e)
            null
        }
    }

    suspend fun saveServiceProviderData(
        specializationsWithCharges: List<com.example.clanhub.serviProviderRegisScrn2.screen.fields.SpecializationWithCharge>,
        selectedTimeSlots: Map<String, List<String>>,
        selectedworkstyle: List<String>,
        selectedOptions: List<String>,
        experience: String,
        context: Context,
        onComplete: (Boolean, String?) -> Unit,
    ) {
        try {
            // 🟢 Get current user's phone number - handle null case properly
            val user = FirebaseAuth.getInstance().currentUser
            val phone = user?.phoneNumber?.replace("+91", "") ?: run {
                onComplete(false, "User not authenticated or phone number not available")
                return
            }

            Log.d("FirebaseRepository", "Saving data for phone number: $phone")

            val documentRef = firestore.collection("service_providers").document(phone)

            // First, get existing data to avoid overriding other specializations
            val existingDoc = documentRef.get().await()
            val existingSpecializations = if (existingDoc.exists()) {
                val existingData = existingDoc.data ?: emptyMap()
                existingData["specializations"] as? Map<String, Any> ?: emptyMap()
            } else {
                emptyMap()
            }

            // 🟢 Prepare specializations data as a map with specialization name as key
            val specializationsMap = existingSpecializations.toMutableMap()

            // 🟢 Filter selected time slots
            val filteredTimeSlots = selectedTimeSlots.filterKeys { key ->
                selectedOptions.contains(key) && !selectedTimeSlots[key].isNullOrEmpty()
            }

            specializationsWithCharges.forEach { specialization ->
                val chargesMap = specialization.charges?.toMap() ?: emptyMap()
                val subSpecializations = specialization.subSpecializations

                // 🔥 MODIFIED: Store all fields within each specialization
                specializationsMap[specialization.specialization] = mapOf(
                    "specialization" to specialization.specialization,
                    "charges" to chargesMap,
                    "subSpecializations" to subSpecializations,
                    // 🟢 NEW: Store these fields within each specialization
                    "selectedTimeSlots" to filteredTimeSlots,
                    "selectedWorkStyle" to selectedworkstyle.filter { it.isNotBlank() },
                    "selectedOptions" to selectedOptions.filter { it.isNotBlank() },
                    "experience" to (experience.ifBlank { "0" }),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            }

            // 🟢 Construct final Firestore data - only specializations map and basic info
            val serviceProviderData = hashMapOf(
                "specializations" to specializationsMap,
                "updatedAt" to com.google.firebase.Timestamp.now(),
                "userId" to (auth.currentUser?.uid ?: "anonymous_user")
            )

            // Use merge to update existing fields or create new document
            documentRef.set(serviceProviderData, SetOptions.merge()).await()

            Log.d("FirebaseRepository", "Successfully saved service provider data with phone: $phone")
            Log.d("FirebaseRepository", "Saved specializations: ${specializationsMap.keys}")
            onComplete(true, null)

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error saving service provider data", e)

            val errorMessage = when {
                e.message?.contains("PERMISSION_DENIED") == true ->
                    "Permission denied. Please check Firebase security rules."
                e.message?.contains("UNAUTHENTICATED") == true ->
                    "Authentication failed. Please try again."
                e.message?.contains("network") == true || e.message?.contains("UNAVAILABLE") == true ->
                    "Network error. Please check your internet connection."
                e.message?.contains("administrators only") == true ->
                    "Configuration issue. Please contact support."
                else -> "Failed to save data. Please try again."
            }

            onComplete(false, errorMessage)
        }
    }

    suspend fun getServiceForUser(serviceName: String): Map<String, Any>? {
        val user = FirebaseAuth.getInstance().currentUser
        val phone = user?.phoneNumber?.replace("+91", "") ?: return null

        val docRef = firestore.collection("service_providers").document(phone)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            val data = snapshot.data ?: return null

            // Get the specializations map instead of array
            val allSpecializations = data["specializations"] as? Map<String, Any>
            val specializationData = allSpecializations?.get(serviceName) as? Map<String, Any>

            if (specializationData != null) {
                // 🔥 MODIFIED: Return data from within the specialization
                return mapOf(
                    // Specialization-specific data
                    "specialization" to (specializationData["specialization"] ?: serviceName),
                    "charges" to (specializationData["charges"] ?: mapOf<String, String>()),
                    "subSpecializations" to (specializationData["subSpecializations"] ?: emptyList<String>()),
                    
                    // 🟢 NEW: These are now stored within each specialization
                    "selectedWorkStyle" to (specializationData["selectedWorkStyle"] ?: emptyList<String>()),
                    "selectedTimeSlots" to (specializationData["selectedTimeSlots"] ?: mapOf<String, List<String>>()),
                    "selectedOptions" to (specializationData["selectedOptions"] ?: emptyList<String>()),
                    "experience" to (specializationData["experience"] ?: "")
                )
            }
        }

        return null
    }

    // Method to get all specializations for a user
    suspend fun getAllSpecializationsForUser(): Map<String, Map<String, Any>>? {
        val user = FirebaseAuth.getInstance().currentUser
        val phone = user?.phoneNumber?.replace("+91", "") ?: return null

        val docRef = firestore.collection("service_providers").document(phone)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            val data = snapshot.data ?: return null
            return data["specializations"] as? Map<String, Map<String, Any>>
        }

        return null
    }

    // Method to delete a specific specialization (only used when needed)
    suspend fun deleteSpecialization(serviceName: String): Boolean {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            val phone = user?.phoneNumber?.replace("+91", "") ?: return false

            val docRef = firestore.collection("service_providers").document(phone)

            // Use FieldValue.delete() to remove the specific specialization
            val updates = mapOf(
                "specializations.$serviceName" to FieldValue.delete()
            )

            docRef.update(updates).await()
            Log.d("FirebaseRepository", "Successfully deleted specialization: $serviceName")
            true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error deleting specialization: $serviceName", e)
            false
        }
    }

    // Method to check if a specialization exists
    suspend fun specializationExists(serviceName: String): Boolean {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            val phone = user?.phoneNumber?.replace("+91", "") ?: return false

            val docRef = firestore.collection("service_providers").document(phone)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val data = snapshot.data ?: return false
                val allSpecializations = data["specializations"] as? Map<String, Any>
                return allSpecializations?.containsKey(serviceName) == true
            }
            false
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error checking specialization existence: $serviceName", e)
            false
        }
    }

    // 🔥 MODIFIED: This method is no longer needed since data is stored within specializations
    // But keeping it for backward compatibility - it will return empty data
    suspend fun getUserBasicInfo(): Map<String, Any>? {
        val user = FirebaseAuth.getInstance().currentUser
        val phone = user?.phoneNumber?.replace("+91", "") ?: return null

        val docRef = firestore.collection("service_providers").document(phone)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            val data = snapshot.data ?: return null
            return mapOf(
                "selectedWorkStyle" to emptyList<String>(),
                "selectedTimeSlots" to mapOf<String, List<String>>(),
                "selectedOptions" to emptyList<String>(),
                "experience" to "",
                "userId" to (data["userId"] ?: ""),
                "updatedAt" to (data["updatedAt"] ?: "")
            )
        }

        return null
    }

    // Helper function to convert Charges to Map (if not already available)
    private fun chargesToMap(charges: com.example.clanhub.serviProviderRegisScrn2.screen.fields.Charges?): Map<String, String> {
        return charges?.let {
            mapOf(
                "perHour" to it.perHour,
                "perDay" to it.perDay,
                "perWeek" to it.perWeek,
                "perMonth" to it.perMonth
            )
        } ?: emptyMap()
    }

    // Helper function to get file extension (if needed for future use)
    private fun getFileExtension(uri: Uri, context: Context): String? {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> null
        }
    }
}