package com.example.remind

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remind.Patient
import com.example.remind.RelationshipQuery
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PatientRelationshipsViewModel : ViewModel() {

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient

    /**
     * Loads the patient data along with their relationships from Firebase.
     *
     * @param patientId The unique identifier of the patient.
     * @param database The Firebase Realtime Database reference.
     */
    fun loadPatient(patientId: String, database: DatabaseReference) {
        viewModelScope.launch {
            database.child("users").child(patientId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val patientData = snapshot.getValue(Patient::class.java)
                        if (patientData != null) {
                            _patient.value = patientData.copy(id = patientId)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error, e.g., log or notify UI
                        // Consider updating _patient with an error state if necessary
                    }
                })
        }
    }

    /**
     * Adds a new relationship to the specified patient using RelationshipQuery.
     *
     * @param patientId The unique identifier of the patient.
     * @param relationshipQuery The relationship data to add.
     * @param storage The Firebase Storage instance.
     * @param database The Firebase Realtime Database reference.
     * @param callback A callback to notify success or failure.
     */
    fun addRelationship(
        patientId: String,
        relationshipQuery: RelationshipQuery,
        storage: FirebaseStorage,
        database: DatabaseReference,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            // Generate a new relationship ID
            val newRelRef = database.child("users").child(patientId).child("relationships").push()
            val relationshipId = newRelRef.key ?: return@launch

            // Check if an image is selected
            if (relationshipQuery.imageUri != null) {
                val imageRef = storage.reference.child("relationship_images/$patientId/$relationshipId.jpg")
                imageRef.putFile(relationshipQuery.imageUri!!)
                    .addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            val updatedRelationship = relationshipQuery.copy(imageUrl = downloadUri.toString())
                            newRelRef.setValue(updatedRelationship)
                                .addOnSuccessListener {
                                    callback(true, "Relationship added successfully.")
                                }
                                .addOnFailureListener { e ->
                                    callback(false, e.message ?: "Failed to add relationship.")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.message ?: "Failed to upload relationship image.")
                    }
            } else {
                // No image selected; proceed to add relationship without imageUrl
                newRelRef.setValue(relationshipQuery)
                    .addOnSuccessListener {
                        callback(true, "Relationship added successfully.")
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.message ?: "Failed to add relationship.")
                    }
            }
        }
    }

    /**
     * Updates an existing relationship for the specified patient using RelationshipQuery.
     *
     * @param patientId The unique identifier of the patient.
     * @param relationshipId The unique identifier of the relationship.
     * @param updatedRelationshipQuery The updated relationship data.
     * @param storage The Firebase Storage instance.
     * @param database The Firebase Realtime Database reference.
     * @param callback A callback to notify success or failure.
     */
    fun updateRelationship(
        patientId: String,
        relationshipId: String,
        updatedRelationshipQuery: RelationshipQuery,
        storage: FirebaseStorage,
        database: DatabaseReference,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val relRef = database.child("users").child(patientId).child("relationships").child(relationshipId)

            // Check if a new image has been selected
            if (updatedRelationshipQuery.imageUri != null) {
                val imageRef = storage.reference.child("relationship_images/$patientId/$relationshipId.jpg")
                imageRef.putFile(updatedRelationshipQuery.imageUri!!)
                    .addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            val relationshipQuery = updatedRelationshipQuery.copy(imageUrl = downloadUri.toString())
                            relRef.setValue(relationshipQuery)
                                .addOnSuccessListener {
                                    callback(true, "Relationship updated successfully.")
                                }
                                .addOnFailureListener { e ->
                                    callback(false, e.message ?: "Failed to update relationship.")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.message ?: "Failed to upload relationship image.")
                    }
            } else {
                // No new image, retain existing imageUrl
                relRef.setValue(updatedRelationshipQuery)
                    .addOnSuccessListener {
                        callback(true, "Relationship updated successfully.")
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.message ?: "Failed to update relationship.")
                    }
            }
        }
    }

    /**
     * Deletes an existing relationship for the specified patient using RelationshipQuery.
     *
     * @param patientId The unique identifier of the patient.
     * @param relationshipId The unique identifier of the relationship.
     * @param storage The Firebase Storage instance.
     * @param database The Firebase Realtime Database reference.
     * @param callback A callback to notify success or failure.
     */
    fun deleteRelationship(
        patientId: String,
        relationshipId: String,
        storage: FirebaseStorage,
        database: DatabaseReference,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val relRef = database.child("users").child(patientId).child("relationships").child(relationshipId)
            // Delete the image from Firebase Storage
            val imageRef = storage.reference.child("relationship_images/$patientId/$relationshipId.jpg")
            imageRef.delete()
                .addOnSuccessListener {
                    // After deleting the image, delete the relationship entry
                    relRef.removeValue()
                        .addOnSuccessListener {
                            callback(true, "Relationship deleted successfully.")
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message ?: "Failed to delete relationship.")
                        }
                }
                .addOnFailureListener { e ->
                    // Even if image deletion fails, attempt to delete the relationship data
                    relRef.removeValue()
                        .addOnSuccessListener {
                            callback(true, "Relationship deleted successfully (image deletion failed).")
                        }
                        .addOnFailureListener { e2 ->
                            callback(false, e2.message ?: "Failed to delete relationship.")
                        }
                }
        }
    }
}
