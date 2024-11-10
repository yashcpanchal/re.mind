package com.example.remind

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MessageAnalysis(private val messages: List<String>) {
    private val apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"  // Replace this with secure handling of API keys
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currUserID = auth.currentUser?.uid

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = apiKey
    )
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    /**
     * Analyzes the latest conversation and logs valuable information to Firebase if deemed worthy.
     * This function should be called after each message is sent.
     */
    fun analyzeConversation() {
        currUserID?.let { userId ->
            database.child("users").child(userId).child("relationships").get()
                .addOnSuccessListener { snapshot ->
                    val knownPeople = snapshot.children.mapNotNull { it.child("firstName").value.toString() }
                    processConversation(knownPeople)
                }
                .addOnFailureListener { exception ->
                    Log.e("MessageAnalysis", "Error fetching relationships: ${exception.message}")
                }
        }
    }

    /**
     * Processes the conversation and checks if any valuable information should be stored.
     */
    private fun processConversation(knownPeople: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prompt = buildValuableInfoPrompt(messages, knownPeople)
                val valuableInfoResult = generativeModel.generateContent(prompt).text ?: "No additional information."

                // Only save valuable information if it contains a new person, memory, or event
                if (isValuable(valuableInfoResult, knownPeople)) {
                    currUserID?.let { userId ->
                        val insightsRef = database.child("users").child(userId).child("Valuable Insights").push()
                        val insightData = mapOf(
                            "timestamp" to System.currentTimeMillis(),
                            "insight" to valuableInfoResult
                        )
                        insightsRef.setValue(insightData)
                            .addOnSuccessListener {
                                Log.d("MessageAnalysis", "Valuable insight saved successfully in Firebase.")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("MessageAnalysis", "Error saving valuable insight: ${exception.message}")
                            }
                    }
                } else {
                    Log.d("MessageAnalysis", "No new valuable insights to save.")
                }

                withContext(Dispatchers.Main) {
                    Log.d("MessageAnalysis", "Valuable Information for Patient to Remember: $valuableInfoResult")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MessageAnalysis", "Error analyzing conversation: ${e.message}")
                }
            }
        }
    }

    /**
     * Constructs a prompt to identify specific valuable information for the patient to remember.
     * @param messages The complete conversation history.
     * @param knownPeople List of people already known to the user.
     * @return A string prompt for the LLM.
     */
    private fun buildValuableInfoPrompt(messages: List<String>, knownPeople: List<String>): String {
        val conversationHistory = messages.joinToString("\n")

        return """
            From the following conversation history, identify specific information that would be valuable for the user to remember, 
            such as mentions of friends, family members, memorable events, or important details that could aid memory retention. 
            Exclude any details about people already known to the user: ${knownPeople.joinToString(", ")}.
            
            Conversation History:
            $conversationHistory
        """.trimIndent()
    }

    /**
     * Checks if the valuable information result contains a new person, memory, or event.
     * @param valuableInfo The generated text for valuable information.
     * @param knownPeople List of people already known to the user.
     * @return True if the valuable information is meaningful, false otherwise.
     */
    private suspend fun isValuable(valuableInfo: String, knownPeople: List<String>): Boolean {
        // Skip if valuableInfo is empty or contains known people
        if (valuableInfo.isBlank() || knownPeople.any { person -> valuableInfo.contains(person, ignoreCase = true) }) {
            Log.e("debugboy123", "isNOTValuable")  // Log only once for non-valuable cases
            return false
        }

        val prompt = """
        Determine if the following information is valuable or not for memory retention.
        Reply with "Valuable" if it is, or "Not Valuable" if it is not.

        Information:
        $valuableInfo
    """.trimIndent()

        return try {
            // Send prompt to Gemini
            val response = generativeModel.generateContent(prompt).text?.trim() ?: "Not Valuable"

            // Determine the outcome based on response and log once
            val isValuable = response.equals("Valuable", ignoreCase = true)
            if (isValuable) {
                Log.e("debugboy123", "isValuable")
            } else {
                Log.e("debugboy123", "isNOTValuable")
            }
            isValuable
        } catch (e: Exception) {
            Log.e("MessageAnalysis", "Error in isValuable check: ${e.message}")
            false
        }
    }

}
