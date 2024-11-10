package com.example.remind

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class EditPatientActivity : ComponentActivity() {

    private val viewModel: PatientRelationshipsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val patientId = intent.getStringExtra("patientId") ?: run {
            Toast.makeText(this, "Invalid patient ID.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.loadPatient(patientId, FirebaseDatabase.getInstance().reference)

        setContent {
            MaterialTheme {
                EditPatientScreen(viewModel = viewModel, patientId = patientId)
            }
        }
    }
}

fun updatePatientData(
    database: DatabaseReference,
    patientId: String,
    updatedPatient: Map<String, Any>,
    context: Context
) {
    database.child("users").child(patientId)
        .updateChildren(updatedPatient)
        .addOnSuccessListener {
            Toast.makeText(context, "Patient updated successfully", Toast.LENGTH_SHORT).show()
            (context as? ComponentActivity)?.finish()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to update patient", Toast.LENGTH_SHORT).show()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPatientScreen(
    viewModel: PatientRelationshipsViewModel,
    patientId: String
) {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().reference
    val storage = FirebaseStorage.getInstance()
    val patient by viewModel.patient.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var dementiaLevel by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var newProfileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> newProfileImageUri = uri }

    LaunchedEffect(patient) {
        patient?.let {
            firstName = it.firstName
            lastName = it.lastName
            age = it.age
            dementiaLevel = it.dementiaLevel
            profileImageUrl = it.profileImageUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Patient") },
                actions = {
                    IconButton(onClick = {
                        val updatedPatient = mutableMapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "age" to age,
                            "dementiaLevel" to dementiaLevel
                        )

                        if (newProfileImageUri != null) {
                            val storageRef = storage.reference.child("profile_images/$patientId/profile.jpg")
                            storageRef.putFile(newProfileImageUri!!)
                                .addOnSuccessListener {
                                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                        updatedPatient["profileImageUrl"] = downloadUri.toString()
                                        updatePatientData(database, patientId, updatedPatient, context)
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            updatePatientData(database, patientId, updatedPatient, context)
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProfileSection(profileImageUrl, newProfileImageUri, pickImageLauncher)
                }
                item {
                    PatientInfoSection(firstName, { firstName = it }, lastName, { lastName = it }, age, { age = it }, dementiaLevel, { dementiaLevel = it })
                }
                item {
                    RelationshipSection(
                        relationships = patient?.relationships?.mapValues { it.value.toRelationshipQuery() } ?: emptyMap(),
                        patientId = patientId,
                        showAddDialog = showAddDialog,
                        onShowAddDialogChange = { showAddDialog = it },
                        viewModel = viewModel,
                        storage = storage,
                        database = database,
                        context = context
                    )
                }
            }
        }
    )
}
@Composable
fun ProfileSection(
    profileImageUrl: String,
    newProfileImageUri: Uri?,
    pickImageLauncher: ManagedActivityResultLauncher<String, Uri?>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(newProfileImageUri ?: profileImageUrl),
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable { pickImageLauncher.launch("image/*") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to change profile image",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PatientInfoSection(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    dementiaLevel: String,
    onDementiaLevelChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Patient Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dementiaLevel,
                onValueChange = onDementiaLevelChange,
                label = { Text("Dementia Level") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RelationshipSection(
    relationships: Map<String, RelationshipQuery>,
    patientId: String,
    showAddDialog: Boolean,
    onShowAddDialogChange: (Boolean) -> Unit,
    viewModel: PatientRelationshipsViewModel,
    storage: FirebaseStorage,
    database: DatabaseReference,
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Relationships", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                relationships.forEach { (id, query) ->
                    RelationshipItem(
                        relationshipId = id,
                        relationshipQuery = query,
                        patientId = patientId,
                        onUpdate = { updatedQuery ->
                            viewModel.updateRelationship(
                                patientId, id, updatedQuery, storage, database
                            ) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) viewModel.loadPatient(patientId, database)
                            }
                        },
                        onDelete = {
                            viewModel.deleteRelationship(
                                patientId, id, storage, database
                            ) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) viewModel.loadPatient(patientId, database)
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onShowAddDialogChange(true) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Add Relationship")
            }
        }
    }
}



@Composable
fun RelationshipItem(
    relationshipId: String,
    relationshipQuery: RelationshipQuery,
    patientId: String,
    onUpdate: (RelationshipQuery) -> Unit,
    onDelete: () -> Unit
) {
    var firstName by remember { mutableStateOf(relationshipQuery.firstName) }
    var lastName by remember { mutableStateOf(relationshipQuery.lastName) }
    var relationshipType by remember { mutableStateOf(relationshipQuery.relationshipType) }
    var ageRange by remember { mutableStateOf(relationshipQuery.ageRange ?: "") }
    var favoriteMemory by remember { mutableStateOf(relationshipQuery.favoriteMemory) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf(relationshipQuery.imageUrl) }
    var showDialog by remember { mutableStateOf(false) }
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current

    // Image Picker for Relationship Image
    val relationshipImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        imageUrl = "" // Clear Firebase URL when a new image is selected
    }

    if (showDialog) {
        ConfirmationDialog(
            title = "Delete Relationship",
            message = "Are you sure you want to delete this relationship?",
            onConfirm = {
                onDelete()
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Display profile picture
            Image(
                painter = when {
                    imageUri != null -> rememberAsyncImagePainter(imageUri) // New selected image
                    imageUrl.isNotBlank() -> rememberAsyncImagePainter(imageUrl) // Existing Firebase URL
                    else -> rememberAsyncImagePainter("default_image_url") // Optional: add a placeholder
                },
                contentDescription = "Relationship Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable { relationshipImagePicker.launch("image/*") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Editable fields for relationship details
            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    onUpdate(relationshipQuery.copy(firstName = it))
                },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    onUpdate(relationshipQuery.copy(lastName = it))
                },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = relationshipType,
                onValueChange = {
                    relationshipType = it
                    onUpdate(relationshipQuery.copy(relationshipType = it))
                },
                label = { Text("Relationship Type") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = ageRange,
                onValueChange = {
                    ageRange = it
                    onUpdate(relationshipQuery.copy(ageRange = it))
                },
                label = { Text("Age Range") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = favoriteMemory,
                onValueChange = {
                    favoriteMemory = it
                    onUpdate(relationshipQuery.copy(favoriteMemory = it))
                },
                label = { Text("Favorite Memory") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save and delete buttons
            Button(
                onClick = {
                    if (imageUri != null) {
                        // Upload new image to Firebase and update the URL
                        val storageRef = storage.reference.child("relationship_images/$patientId/$relationshipId.jpg")
                        storageRef.putFile(imageUri!!)
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    imageUrl = downloadUri.toString() // Set image URL from Firebase
                                    onUpdate(
                                        relationshipQuery.copy(
                                            firstName = firstName,
                                            lastName = lastName,
                                            relationshipType = relationshipType,
                                            ageRange = ageRange,
                                            favoriteMemory = favoriteMemory,
                                            imageUrl = imageUrl
                                        )
                                    )
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to upload relationship image", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // No new image, just update other fields
                        onUpdate(
                            relationshipQuery.copy(
                                firstName = firstName,
                                lastName = lastName,
                                relationshipType = relationshipType,
                                ageRange = ageRange,
                                favoriteMemory = favoriteMemory,
                                imageUrl = imageUrl // Keep existing image URL
                            )
                        )
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Delete")
            }
        }
    }
}

@Composable
fun AddRelationshipDialog(
    onDismiss: () -> Unit,
    onSave: (RelationshipQuery) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var relationshipType by remember { mutableStateOf("") }
    var ageRange by remember { mutableStateOf("") }
    var favoriteMemory by remember { mutableStateOf("") }
    var newImageUri by remember { mutableStateOf<Uri?>(null) } // Add this line

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> newImageUri = uri } // Set the URI when an image is picked
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Relationship") },
        text = {
            Column {
                // Image preview and pick button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") } // Launch the image picker
                ) {
                    if (newImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(newImageUri),
                            contentDescription = "Selected Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp)
                        )
                    } else {
                        Text("Tap to select image")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Other relationship fields
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") })
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") })
                OutlinedTextField(value = relationshipType, onValueChange = { relationshipType = it }, label = { Text("Relationship Type") })
                OutlinedTextField(value = ageRange, onValueChange = { ageRange = it }, label = { Text("Age Range") })
                OutlinedTextField(value = favoriteMemory, onValueChange = { favoriteMemory = it }, label = { Text("Favorite Memory") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newRelationship = RelationshipQuery(
                    firstName = firstName,
                    lastName = lastName,
                    relationshipType = relationshipType,
                    ageRange = ageRange,
                    favoriteMemory = favoriteMemory,
                    imageUrl = newImageUri?.toString() ?: "" // Add image URL from URI if available
                )
                onSave(newRelationship)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss() // Close dialog after confirming
            }) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("No")
            }
        }
    )
}
