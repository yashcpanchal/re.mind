package com.example.remind

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

class RelationshipSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RelationshipSetupScreen { navigateToHome() }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}

@Composable
fun RelationshipSetupScreen(onRelationshipsComplete: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()

    // Initialize with one relationship by default
    var relationships by remember {
        mutableStateOf(
            listOf(
                Relationship(
                    imageUri = Uri.EMPTY,
                    relationshipType = "",
                    firstName = "",
                    lastName = "",
                    ageRange = "",
                    favoriteMemory = ""
                )
            )
        )
    }

    // Make the screen scrollable using a LazyColumn
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(text = "Add Relationships", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Iterate through the list of relationships and display each as a card
        items(relationships.size) { index ->
            RelationshipCard(relationship = relationships[index]) { updatedRelationship ->
                relationships = relationships.toMutableList().apply { set(index, updatedRelationship) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Center the "Add" button using a Box
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        relationships = relationships + Relationship(
                            imageUri = Uri.EMPTY,
                            relationshipType = "",
                            firstName = "",
                            lastName = "",
                            ageRange = "",
                            favoriteMemory = ""
                        )
                    },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Relationship")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    saveRelationshipsToBackend(
                        userId,
                        relationships,
                        onSuccess = { onRelationshipsComplete() },
                        onFailure = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Relationships")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RelationshipCard(relationship: Relationship, onRelationshipUpdated: (Relationship) -> Unit) {
    var relationshipType by remember { mutableStateOf(relationship.relationshipType) }
    var firstName by remember { mutableStateOf(relationship.firstName) }
    var lastName by remember { mutableStateOf(relationship.lastName) }
    var ageRange by remember { mutableStateOf(relationship.ageRange ?: "") } // Ensure ageRange is non-null
    var favoriteMemory by remember { mutableStateOf(relationship.favoriteMemory) }
    var imageUri by remember { mutableStateOf(relationship.imageUri) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            onRelationshipUpdated(relationship.copy(imageUri = imageUri))
        }
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RelationshipImagePicker(imageUri) { pickImageLauncher.launch("image/*") }
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = relationshipType,
                onValueChange = {
                    relationshipType = it
                    onRelationshipUpdated(relationship.copy(relationshipType = relationshipType))
                },
                label = { Text("Relationship Type") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    onRelationshipUpdated(relationship.copy(firstName = firstName))
                },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    onRelationshipUpdated(relationship.copy(lastName = lastName))
                },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = ageRange, // ageRange is now guaranteed to be non-null
                onValueChange = {
                    ageRange = it
                    onRelationshipUpdated(relationship.copy(ageRange = ageRange))
                },
                label = { Text("Age Range") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = favoriteMemory,
                onValueChange = {
                    favoriteMemory = it
                    onRelationshipUpdated(relationship.copy(favoriteMemory = favoriteMemory))
                },
                label = { Text("Favorite Memory") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RelationshipImagePicker(imageUri: Uri?, onImagePick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .border(2.dp, Color.Gray, CircleShape)
            .clickable { onImagePick() }
    ) {
        if (imageUri != null && imageUri != Uri.EMPTY) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = "Relationship Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera_alt),
                contentDescription = "Add Picture",
                modifier = Modifier.size(50.dp),
                tint = Color.Gray
            )
        }
    }
}


fun saveRelationshipsToBackend(
    userId: String,
    relationships: List<Relationship>,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val databaseRef = FirebaseDatabase.getInstance().reference.child("users").child(userId).child("relationships")
    val storageRef = FirebaseStorage.getInstance().reference.child("users/$userId/profile_images")

    relationships.forEachIndexed { index, relationship ->
        if (relationship.imageUri != Uri.EMPTY) {
            val imageRef = storageRef.child("relationship_$index.jpg")
            imageRef.putFile(relationship.imageUri).addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val relationshipMap = mapOf(
                        "relationshipType" to relationship.relationshipType,
                        "firstName" to relationship.firstName,
                        "lastName" to relationship.lastName,
                        "ageRange" to relationship.ageRange.orEmpty(),
                        "favoriteMemory" to relationship.favoriteMemory,
                        "imageUrl" to downloadUri.toString()
                    )
                    databaseRef.push().setValue(relationshipMap).addOnCompleteListener { task ->
                        if (task.isSuccessful) onSuccess() else onFailure("Failed to save relationship: ${task.exception?.message}")
                    }
                }.addOnFailureListener { e -> onFailure("Failed to get download URL: ${e.message}") }
            }.addOnFailureListener { e -> onFailure("Failed to upload image: ${e.message}") }
        }
    }
}
