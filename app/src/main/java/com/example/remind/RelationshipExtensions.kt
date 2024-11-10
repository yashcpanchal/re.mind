package com.example.remind

import android.net.Uri

/**
 * Converts a Relationship instance to a RelationshipQuery instance.
 */
fun Relationship.toRelationshipQuery(): RelationshipQuery {
    return RelationshipQuery(
        imageUrl = this.imageUri.toString(), // This will be handled separately
        relationshipType = this.relationshipType,
        firstName = this.firstName,
        lastName = this.lastName,
        ageRange = this.ageRange,
        favoriteMemory = this.favoriteMemory
    )
}

/**
 * Converts a RelationshipQuery instance to a Relationship instance.
 */
fun RelationshipQuery.toRelationship(): Relationship {
    return Relationship(
        imageUri = Uri.EMPTY, // Initialize with empty; actual URI is handled during image selection
        relationshipType = this.relationshipType,
        firstName = this.firstName,
        lastName = this.lastName,
        ageRange = this.ageRange,
        favoriteMemory = this.favoriteMemory
    )
}
