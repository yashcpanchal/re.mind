package com.example.remind

import android.content.Context
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
import androidx.compose.foundation.layout.*
import com.example.remind.CaregiverUtils
import androidx.compose.foundation.shape.CircleShape
import android.provider.MediaStore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.remind.ui.theme.RemindTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import coil.compose.rememberAsyncImagePainter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    private var isSigningUp = mutableStateOf(false)
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            RemindTheme {
                if (isSigningUp.value) {
                    SignUpScreen(
                        auth = auth,
                        onBackToLogin = { isSigningUp.value = false },
                        navigateToRelationshipSetup = { navigateToRelationshipSetup() }
                    )
                } else {
                    LoginScreen(
                        navigateToSignUp = { isSigningUp.value = true },
                        navigateToHome = { navigateToHome() }
                    )
                }
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, ChatScreenActivity::class.java))
        finish() // Close MainActivity
    }

    private fun navigateToRelationshipSetup() {
        startActivity(Intent(this, RelationshipSetupActivity::class.java))
        finish() // Close MainActivity
    }
}

@Composable
fun LoginScreen(navigateToSignUp: () -> Unit, navigateToHome: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome Back!", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        } else {
                            Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = navigateToSignUp) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(auth: FirebaseAuth, onBackToLogin: () -> Unit, navigateToRelationshipSetup: () -> Unit) {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().reference
    val storage = FirebaseStorage.getInstance().reference // Ensure storage is correctly initialized
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var caretakerEmail by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("Early") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val dementiaLevels = listOf("Early", "Middle", "Late")
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker launcher
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        profileImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Sign Up", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Image
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, Color.Gray, CircleShape)
                .clickable { pickImageLauncher.launch("image/*") }
        ) {
            if (profileImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(profileImageUri),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera_alt),
                    contentDescription = "Add Profile Picture",
                    modifier = Modifier.size(50.dp),
                    tint = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = caretakerEmail,
            onValueChange = { caretakerEmail = it },
            label = { Text("Caretaker Email (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedLevel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Dementia Level") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = isDropdownExpanded
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                dementiaLevels.forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level) },
                        onClick = {
                            selectedLevel = level
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                performSignUp(auth, database, storage, email, password, firstName, lastName, age, caretakerEmail, selectedLevel, profileImageUri, context, navigateToRelationshipSetup)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Already have an account? Log In")
        }
    }
}

fun performSignUp(
    auth: FirebaseAuth,
    database: DatabaseReference,
    storage: StorageReference,
    email: String,
    password: String,
    firstName: String,
    lastName: String,
    age: String,
    caretakerEmail: String,
    selectedLevel: String,
    profileImageUri: Uri?,
    context: Context,
    navigateToRelationshipSetup: () -> Unit
) = CoroutineScope(Dispatchers.IO).launch {
    if (profileImageUri == null) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Please select a profile picture", Toast.LENGTH_SHORT).show()
        }
        return@launch
    }

    try {
        val createUserResult = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

        // Load and compress image
        val bitmap = if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, profileImageUri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, profileImageUri)
            ImageDecoder.decodeBitmap(source)
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream) // Compress quality can be changed as needed
        val imageData = byteArrayOutputStream.toByteArray()

        // Upload compressed image
        val storageRef = storage.child("profile_images/$userId/profile.jpg")
        val uploadTaskSnapshot = storageRef.putBytes(imageData).await()
        val downloadUri = uploadTaskSnapshot.storage.downloadUrl.await()

        val userMap = mutableMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "age" to age,
            "dementiaLevel" to selectedLevel,
            "profileImageUrl" to downloadUri.toString()
        )

        // Add caretaker email only if it is not empty
        if (caretakerEmail.isNotEmpty()) {
            userMap["caretakerEmail"] = caretakerEmail
        }

        // Save the user data in the database
        database.child("users").child(userId).setValue(userMap).await()
        // Link the patient to the caretaker if a valid caretaker email is provided
        if (caretakerEmail.isNotEmpty()) {
            val caretakerQuery = database.child("caretakers").orderByChild("email").equalTo(caretakerEmail).get().await()

            if (caretakerQuery.exists()) {
                CaregiverUtils.linkPatientToCaregiver(database, caretakerEmail, userId, context)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Caretaker email not found. Linking skipped.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
            navigateToRelationshipSetup()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Registration Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
