package com.example.remind

import android.content.Intent
import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.remind.ui.theme.RemindTheme

class CaretakerPortalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemindTheme {
                CaretakerPatientPortalScreen(
                    navigateToCaretakerSignup = { navigateToCaretakerSignup() },
                    navigateToPatientPortal = { navigateToPatientPortal() }
                )
            }
        }
    }

    private fun navigateToCaretakerSignup() {
        startActivity(Intent(this, CaretakerSignupActivity::class.java))
    }

    private fun navigateToPatientPortal() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}

@Composable
fun CaretakerPatientPortalScreen(
    navigateToCaretakerSignup: () -> Unit,
    navigateToPatientPortal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Select Your Portal", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = navigateToCaretakerSignup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Caretaker Portal")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = navigateToPatientPortal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Patient Portal")
        }
    }
}