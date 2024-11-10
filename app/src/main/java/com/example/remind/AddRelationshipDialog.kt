package com.example.remind.ui.editpatient

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.remind.R
import com.example.remind.RelationshipQuery

@Composable
fun AddRelationshipDialog(
    onDismiss: () -> Unit,
    onSave: (RelationshipQuery) -> Unit
) {
    val context = LocalContext.current

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var relationshipType by remember { mutableStateOf("") }
    var ageRange by remember { mutableStateOf("") }
    var favoriteMemory by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(text = "Add New Relationship") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Image Picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                        )
                    }
//                    else {
//                        Image(
//                            painter = painterResource(id = R.drawable.placeholder),
//                            contentDescription = "Placeholder Image",
//                            modifier = Modifier
//                                .size(120.dp)
//                                .clip(CircleShape)
//                        )
//                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to select image",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // First Name
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name*") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Last Name
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name*") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Relationship Type
                OutlinedTextField(
                    value = relationshipType,
                    onValueChange = { relationshipType = it },
                    label = { Text("Relationship Type*") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Age Range
                OutlinedTextField(
                    value = ageRange,
                    onValueChange = { ageRange = it },
                    label = { Text("Age Range") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Favorite Memory
                OutlinedTextField(
                    value = favoriteMemory,
                    onValueChange = { favoriteMemory = it },
                    label = { Text("Favorite Memory") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate required fields
                    if (firstName.isBlank() || lastName.isBlank() || relationshipType.isBlank()) {
                        Toast.makeText(context, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    // Create RelationshipQuery object
                    val newRelationship = RelationshipQuery(
                        imageUrl = "", // Will be set after image upload
                        relationshipType = relationshipType,
                        firstName = firstName,
                        lastName = lastName,
                        ageRange = if (ageRange.isBlank()) null else ageRange,
                        favoriteMemory = favoriteMemory
                    )

                    onSave(newRelationship)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}