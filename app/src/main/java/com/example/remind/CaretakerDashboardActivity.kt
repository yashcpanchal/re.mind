package com.example.remind

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaretakerDashboardActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setContent {
            MaterialTheme {
                CaretakerDashboard()
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.getDefault())
    return format.format(date)
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CaretakerDashboard() {
    val context = LocalContext.current
    val patients = remember { mutableStateListOf<Patient>() }
    val caretakerId = FirebaseAuth.getInstance().currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference

    DisposableEffect(caretakerId) {
        if (caretakerId != null) {
            val caretakerPatientsRef = database.child("caretakers").child(caretakerId).child("patients")

            val caretakerListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentPatientIds = snapshot.children.mapNotNull { it.key }
                    currentPatientIds.forEach { patientId ->
                        val userRef = database.child("users").child(patientId)
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val patient = userSnapshot.getValue(Patient::class.java)
                                if (patient != null) {
                                    patients.add(patient.copy(id = patientId))
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(context, "Error fetching patient $patientId: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error listening for patients: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }

            caretakerPatientsRef.addListenerForSingleValueEvent(caretakerListener)

            // Return onDispose to satisfy DisposableEffectResult
            onDispose {
                caretakerPatientsRef.removeEventListener(caretakerListener)
            }
        } else {
            onDispose { } // Return empty onDispose if caretakerId is null
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caretaker Dashboard") }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (patients.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No patients found", fontSize = 18.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    PatientList(patients)
                }
            }
        }
    )
}

@Composable
fun PatientList(patients: List<Patient>) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(patients) { patient ->
            PatientCard(patient) {
                // Navigate to ValuableInsightsActivity
                val intent = Intent(context, ValuableInsightsActivity::class.java)
                intent.putExtra("patientId", patient.id) // Pass patient ID
                context.startActivity(intent)
            }
        }
    }
}

@Composable
fun PatientCard(patient: Patient, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(patient.profileImageUrl),
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )
            Text(
                text = "${patient.firstName.capitalizeFirstLetter()} ${patient.lastName.capitalizeFirstLetter()}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(text = "Age: ${patient.age}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Text(text = "Dementia Level: ${patient.dementiaLevel}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(8.dp))

            // "Add Relationship" Button
            androidx.compose.material3.Button(
                onClick = {
                    val intent = Intent(context, EditPatientActivity::class.java)
                    intent.putExtra("patientId", patient.id) // Pass patient ID
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = "Modify Relationship")
            }
        }
    }
}


class ValuableInsightsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val patientId = intent.getStringExtra("patientId") ?: return

        setContent {
            MaterialTheme {
                ValuableInsightsScreen(patientId)
            }
        }
    }
}

@Composable
fun ValuableInsightsScreen(patientId: String) {
    val context = LocalContext.current
    val insights = remember { mutableStateListOf<ValuableInsight>() }
    val database = FirebaseDatabase.getInstance().reference.child("users").child(patientId).child("Valuable Insights")

    DisposableEffect(patientId) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                insights.clear()
                snapshot.children.mapNotNullTo(insights) { it.getValue(ValuableInsight::class.java) }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error fetching insights: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        database.addValueEventListener(listener)

        onDispose { database.removeEventListener(listener) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(insights) { insight ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = insight.insight,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Timestamp: ${formatTimestamp(insight.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@IgnoreExtraProperties
data class ValuableInsight(
    val insight: String = "",
    val timestamp: Long = 0L
)

fun String.capitalizeFirstLetter(): String {
    return if (this.isNotEmpty()) {
        this.substring(0, 1).uppercase() + this.substring(1)
    } else {
        this
    }
}

@IgnoreExtraProperties
data class Patient(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val age: String = "",
    val dementiaLevel: String = "",
    val profileImageUrl: String = "",
    val relationships: Map<String, Relationship> = emptyMap()
)
