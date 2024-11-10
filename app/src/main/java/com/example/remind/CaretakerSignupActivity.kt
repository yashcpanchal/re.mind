package com.example.remind

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.remind.ui.theme.RemindTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CaretakerSignupActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference
    private var isLoggingIn = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            RemindTheme {
                if (isLoggingIn.value) {
                    CaretakerLoginScreen(
                        auth = auth,
                        onSwitchToSignup = { isLoggingIn.value = false }
                    )
                } else {
                    CaretakerSignupScreen(
                        auth = auth,
                        database = database,
                        onSwitchToLogin = { isLoggingIn.value = true }
                    )
                }
            }
        }
    }
}

@Composable
fun CaretakerLoginScreen(
    auth: FirebaseAuth,
    onSwitchToSignup: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Caretaker Login", style = MaterialTheme.typography.titleLarge)

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
                loginCaretaker(auth, email, password, context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log In")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onSwitchToSignup) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@Composable
fun CaretakerSignupScreen(
    auth: FirebaseAuth,
    database: DatabaseReference,
    onSwitchToLogin: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Caretaker Signup", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                signUpCaretaker(auth, database, name, email, password, context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onSwitchToLogin) {
            Text("Already have an account? Log In")
        }
    }
}

fun signUpCaretaker(
    auth: FirebaseAuth,
    database: DatabaseReference,
    name: String,
    email: String,
    password: String,
    context: android.content.Context
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Register the user in Firebase Authentication
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw IllegalStateException("User ID is null")

            // Save caretaker details in the Firebase Realtime Database
            val caretaker = Caretaker(name, email)
            database.child("caretakers").child(userId).setValue(caretaker).await()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Caretaker registered successfully!", Toast.LENGTH_SHORT).show()
                // Navigate to caretaker dashboard
                val intent = Intent(context, CaretakerDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun loginCaretaker(
    auth: FirebaseAuth,
    email: String,
    password: String,
    context: android.content.Context
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Sign in with Firebase Authentication
            auth.signInWithEmailAndPassword(email, password).await()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                // Navigate to caretaker dashboard
                val intent = Intent(context, CaretakerDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class Caretaker(
    val name: String,
    val email: String
)