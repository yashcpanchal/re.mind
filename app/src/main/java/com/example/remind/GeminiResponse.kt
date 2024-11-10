package com.example.remind

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class GeminiResponse {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currUserID = auth.currentUser?.uid
    private val database = Firebase.database.reference

    private val apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" // Replace with your actual API key
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = apiKey
    )

    private val conversationHistory = mutableListOf<String>()

    /**
     * Generates a response based on the user's input and includes patient information.
     * @param userInput The prompt or question from the user.
     * @param callback A lambda function to handle the response text.
     */
    fun getResponse(userInput: String, callback: (String) -> Unit) {
        conversationHistory.add("User: $userInput")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (currUserID == null) {
                    withContext(Dispatchers.Main) {
                        callback("User is not signed in.")
                    }
                    return@launch
                }

                val userRef = database.child("users").child(currUserID)
                val userDataSnapshot = userRef.get().await()

                val userData = userDataSnapshot.value as? Map<String, Any?>

                // Enhanced age retrieval to handle more data types, including String
                val age = when (val ageValue = userData?.get("age")) {
                    is Long -> ageValue
                    is Int -> ageValue.toLong()
                    is Double -> ageValue.toLong()
                    is String -> ageValue.toIntOrNull()?.toLong() ?: 0L
                    else -> 0L
                }

                val firstName = userData?.get("firstName") as? String ?: ""
                val lastName = userData?.get("lastName") as? String ?: ""
                val dementiaLevel = userData?.get("dementiaLevel") as? String ?: ""

                // Get relationships
                val relationshipsSnapshot = userDataSnapshot.child("relationships")
                val relationshipsList = mutableListOf<Map<String, Any?>>()
                for (relationshipSnapshot in relationshipsSnapshot.children) {
                    val relationshipData = relationshipSnapshot.value as? Map<String, Any?>
                    if (relationshipData != null) {
                        relationshipsList.add(relationshipData)
                    }
                }

                val patientInfo = "Name: $firstName $lastName\nAge: $age\nDementia Level: $dementiaLevel"

                val relationshipsInfo = StringBuilder()
                relationshipsInfo.append("Relationships:\n")
                for (relationship in relationshipsList) {
                    val relFirstName = relationship["firstName"] as? String ?: ""
                    val relLastName = relationship["lastName"] as? String ?: ""
                    val relType = relationship["relationshipType"] as? String ?: ""
                    val relFavoriteMemory = relationship["favoriteMemory"] as? String ?: ""
                    relationshipsInfo.append("- $relType: $relFirstName $relLastName\n  Favorite Memory: $relFavoriteMemory\n")
                }

                // Refined preprompt to guide the LLM more effectively
                val prePrompt = """
You are a nurse designed to help a patient with dementia recall information and memories through gentle prompts and supportive guidance. Your primary goal is to assist the patient in remembering by asking memory-triggering questions and providing affirmations, rather than giving direct answers immediately.

**Guidelines:**

1. **Gentle Memory Prompts:** When the patient asks for information (e.g., name, age, relationships), respond with questions that help jog their memory. For example, instead of saying, "Your name is John Doe," ask, "Do you remember a time when you met John? How did that go?"

2. **Supportive Affirmations:** If the patient provides correct information, offer positive reinforcement. If they provide incorrect information, gently correct them without causing distress. For example:
   - **Correct Information:** "That's wonderful! You're remembering well."
   - **Incorrect Information:** "I believe your name is {PatientInformation.data.firstName} {PatientInformation.data.lastName}. Does that sound right?"

3. **Avoid Direct Answers Initially:** Refrain from giving direct answers to the patient's questions on the first attempt. Instead, use guiding questions to help them recall the information themselves.

4. **Adapt to Dementia Stage:** Tailor your responses based on the patient's dementia stage:
   - **Early:** Use gentle hints and light prompts to encourage engagement.
   - **Medium:** Emphasize patience and provide repetitive cues without rushing.
   - **Late:** Offer clear, simple prompts with minimal new information, maintaining a comforting presence.

5. **Avoid Using Emojis:** Refrain from including emojis in your responses to maintain a professional and comforting tone.
6. **Do NOT fixate on a certain topic, it is especially bad for patients with dementia when trying to form connections. If a patient brings up a topic that seems unrelated gradually shift the topic to what the patient wants to talk about. Later, gently nudge the patient to remember what the previous conversation was about.
7. **At the beginning let the patient initiate the conversation. 
8. **If the patient adamantly asks for a piece of information that you think you know, then you should provide that piece of information. DON'T FRUSTRATE THE PATIENT
**Patient Information:**
- **Name:** {PatientInformation.data.firstName} {PatientInformation.data.lastName}
- **Age:** {PatientInformation.data.age} years old
- **Dementia Level:** {PatientInformation.data.dementiaLevel}-stage

**Relatives' Information:**
- **Name:** {Relative.firstName} {Relative.lastName}
- **Age Range:** {Relative.ageRange}
- **Relationship Type:** {Relative.relationshipType}
- **Favorite Memory:** {Relative.favoriteMemory}

**Response Structure:**
1. **Greeting or Acknowledgment:** Begin with a warm greeting or acknowledgment.
2. **Memory-Triggering Question:** Ask a question that helps the patient recall the information.
3. **Supportive Statement:** End with a supportive statement to encourage further interaction.

**Example Responses:**
- **Question:** "What is my name and age?"
  - **Response:** "Let's see if we can remember together. Do you recall meeting someone named {PatientInformation.data.firstName}? How old do you think you might be?"

- **Question:** "Who am I related to?"
  - **Response:** "Can you think of a special person in your life, perhaps someone you enjoy spending time with? What do you remember about them?"

- **Incorrect Information Provided by User:**
  - **User:** "I am 0 years old."
  - **Assistant:** "I believe you are {PatientInformation.data.age} years old. Does that sound correct?"

**Goal:**
Help the patient feel empowered in recalling memories and information by maintaining a positive and stress-free interaction. Ensure all responses are accurate, personalized, and free from unnecessary embellishments.


                """.trimIndent()

                // Structured prompt to enhance clarity
                val fullContext = """
                    $prePrompt

                    Patient Information:
                    $patientInfo

                    $relationshipsInfo

                    Conversation History:
                    ${conversationHistory.joinToString("\n")}
                    Assistant:
                """.trimIndent()

                // Optionally, log the fullContext for debugging purposes
                // Log.d("GeminiResponse", "Prompt sent to LLM:\n$fullContext")

                val response = generativeModel.generateContent(fullContext)
                val responseText = response.text ?: "I'm here to help you."

                // Clean the response to remove any remaining emojis (optional)
                val cleanedResponse = responseText.replace(Regex("[\\p{So}\\p{C}]"), "")

                // Add the assistant's response to the conversation history
                conversationHistory.add("Assistant: $cleanedResponse")

                withContext(Dispatchers.Main) {
                    callback(cleanedResponse)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback("Error generating response: ${e.message}")
                }
            }
        }
    }
}
