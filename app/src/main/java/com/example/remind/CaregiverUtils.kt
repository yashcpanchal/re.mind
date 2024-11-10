package com.example.remind

import android.content.Context
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

object CaregiverUtils {

    fun linkPatientToCaregiver(
        database: DatabaseReference,
        caregiverEmail: String,
        patientId: String,
        context: Context
    ) {
        val caregiverQuery = database.child("caretakers").orderByChild("email").equalTo(caregiverEmail)

        caregiverQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (caregiverSnapshot in snapshot.children) {
                        val caregiverId = caregiverSnapshot.key
                        if (caregiverId != null) {
                            // Add the patient's ID under the caregiver's "patients" node
                            database.child("caretakers").child(caregiverId).child("patients").child(patientId).setValue(true)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Linked to caregiver successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to link to caregiver.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    }
                } else {
                    Toast.makeText(context, "Caregiver email not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}