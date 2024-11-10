package com.example.remind

import android.net.Uri
import com.google.firebase.database.IgnoreExtraProperties

data class Relationship(
    val imageUri: Uri = Uri.EMPTY,
    val relationshipType: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val ageRange: String? = "",
    val favoriteMemory: String = ""
)

@IgnoreExtraProperties
data class RelationshipQuery(
    val imageUrl: String = "",
    val relationshipType: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val ageRange: String? = "",
    val favoriteMemory: String = ""
) {
    @Transient
    var imageUri: Uri? = null
}