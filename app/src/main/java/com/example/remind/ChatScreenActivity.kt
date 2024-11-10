package com.example.remind

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import co.rikin.speechtotext.RealSpeechToText
import com.example.remind.ui.theme.RemindTheme
import java.util.*

// Data class for messages
data class Message(val text: String, val isUser: Boolean)

class ChatScreenActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private val responseGenerator = GeminiResponse()  // Replace with actual implementation
    private val conversationHistory = mutableListOf<String>()
    private lateinit var messageAnalysis: MessageAnalysis  // Replace with actual implementation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this, this)
        messageAnalysis = MessageAnalysis(conversationHistory)

        setContent {
            RemindTheme {
                ChatScreen(responseGenerator, textToSpeech, conversationHistory, messageAnalysis)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        analyzeConversation(conversationHistory)
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
        } else {
            Log.e("ChatScreenActivity", "TextToSpeech initialization failed.")
        }
    }

    private fun analyzeConversation(history: List<String>) {
        val notableInfo = history.groupingBy { it }.eachCount().filter { it.value > 1 }
        Log.d("ChatScreenActivity", "Conversation Analysis: Notable Information: $notableInfo")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    responseGenerator: GeminiResponse,
    textToSpeech: TextToSpeech,
    conversationHistory: MutableList<String>,
    messageAnalysis: MessageAnalysis
) {
    val context = LocalContext.current
    val speechToText = remember { RealSpeechToText(context) }
    val messageState = remember { mutableStateOf(TextFieldValue("")) }
    val messages = remember { mutableStateListOf<Message>() }
    val isListening by speechToText.isListening.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            speechToText.start()
        } else {
            Toast.makeText(context, "Microphone permission is required for speech recognition", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(isListening) {
        if (!isListening && messageState.value.text.isNotBlank()) {
            sendMessage(messageState.value.text, responseGenerator, messages, messageState, textToSpeech, conversationHistory, messageAnalysis)
        }
    }

    LaunchedEffect(Unit) {
        speechToText.text.collect { recognizedText ->
            messageState.value = TextFieldValue(recognizedText)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechToText.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with Assistant", fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        content = { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        reverseLayout = true
                    ) {
                        items(messages) { message ->
                            MessageBubble(message)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicTextField(
                            value = messageState.value,
                            onValueChange = { messageState.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = {
                            val userInput = messageState.value.text
                            if (userInput.isNotBlank()) {
                                sendMessage(messageState.value.text, responseGenerator, messages, messageState, textToSpeech, conversationHistory, messageAnalysis)
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_send),
                                contentDescription = "Send Message",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                speechToText.start()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_microphone),
                                contentDescription = "Speech Input",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MessageBubble(message: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 8.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            shadowElevation = 4.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(all = 8.dp),
                color = Color.White
            )
        }
    }
}

private fun sendMessage(
    userInput: String,
    responseGenerator: GeminiResponse,
    messages: MutableList<Message>,
    messageState: MutableState<TextFieldValue>,
    textToSpeech: TextToSpeech,
    conversationHistory: MutableList<String>,
    messageAnalysis: MessageAnalysis  // Add this parameter
) {
    val cleanedInput = userInput.trim().replace(Regex("[^\\w\\s]"), "").lowercase()
    Log.d("ChatScreen", "Processed Input: '$cleanedInput'")

    // Analyze the user's message
    val userAnalysisResult = messageAnalysis.analyzeConversation()  // Assuming analyzeConversation is used for each message
    Log.d("MessageAnalysis", "User Message Analysis: $userAnalysisResult")

    responseGenerator.getResponse(cleanedInput) { responseText ->
        messages.add(0, Message(userInput, true))  // Add user message to the list
        messages.add(0, Message(responseText, false))  // Add assistant message to the list
        messageState.value = TextFieldValue("")  // Clear the input field after sending

        textToSpeech.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, null)

        // Add both messages to conversation history
        conversationHistory.add(userInput)
        conversationHistory.add(responseText)

        // Analyze the assistant's response
        val responseAnalysisResult = messageAnalysis.analyzeConversation()
        Log.d("MessageAnalysis", "Assistant Message Analysis: $responseAnalysisResult")
    }
}
