package com.example.remind

import android.Manifest
import android.R.attr.text
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import com.google.firebase.database.*
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.remind.ui.theme.RemindTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import coil.load
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.compose.ui.graphics.Color as uiColor
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.provider.MediaStore
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HomeActivity : ComponentActivity() {

    private lateinit var imageCapture: ImageCapture
    private var hasCameraPermission = false
    private lateinit var interpreter: Interpreter
    private val faceBmp: Bitmap? = null
    private var lastDetectionTime: Long = 0
    private val TF_OD_API_INPUT_SIZE = 112
    private val detectionDelay = 500 // 500 milliseconds in milliseconds
    private var lastSentTime: Long = 0
    private val debounceInterval = 5000 // 1 second in milliseconds

    // Face detection
    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        FaceDetection.getClient(options)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize TensorFlow Lite interpreter
        initTFLiteInterpreter()

        requestCameraPermission()

        setContent {
            RemindTheme {
                var isFaceDetected by remember { mutableStateOf(false) }

                if (hasCameraPermission) {
                    CameraView(isFaceDetected) { detected -> isFaceDetected = detected }
                } else {
                    PermissionDeniedView()
                }
            }
        }
    }

    private fun initTFLiteInterpreter() {
        val TFLITE_MODEL_NAME = "mobile_face_net.tflite"
        val tfLiteOptions = Interpreter.Options() // can be configured to use GPUDelegate
        try {
            val model = FileUtil.loadMappedFile(this, TFLITE_MODEL_NAME)
            interpreter = Interpreter(model, tfLiteOptions)
        } catch (e: IOException) {
            Toast.makeText(this, "Error loading TFLite model: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getInputImage(width: Int, height: Int): ByteBuffer {
        val inputImage =
            ByteBuffer.allocateDirect(1 * width * height * 3 * 4)// input image will be required input shape of tflite model
        inputImage.order(ByteOrder.nativeOrder())
        inputImage.rewind()
        return inputImage
    }

    private fun convertBitmapToByteBuffer(bitmapIn: Bitmap, width: Int, height: Int): ByteBuffer {
        val bitmap = Bitmap.createScaledBitmap(bitmapIn, width, height, false) // convert bitmap into required size
        val mean = arrayOf(127.5f, 127.5f, 127.5f)
        val standard = arrayOf(127.5f, 127.5f, 127.5f)
        val inputImage = ByteBuffer.allocateDirect(4 * width * height * 3).order(ByteOrder.nativeOrder())
        val intValues = IntArray(width * height)
        bitmap.getPixels(intValues, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val px = intValues[y * width + x]
                val r = Color.red(px).toFloat()
                val g = Color.green(px).toFloat()
                val b = Color.blue(px).toFloat()

                // Normalize channel values to [-1.0, 1.0]. Adjust if your model needs different scaling.
                val rf = (r - mean[0]) / standard[0]
                val gf = (g - mean[1]) / standard[1]
                val bf = (b - mean[2]) / standard[2]

                // Putting in BRG order because this model demands input in this order
                inputImage.putFloat(bf)
                inputImage.putFloat(rf)
                inputImage.putFloat(gf)
            }
        }

        return inputImage
    }


    object BitmapUtils {
        fun getBitmapFromMediaImage(image: Image, rotationDegrees: Int): Bitmap {
            val planes = image.planes
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
            val imageBytes = out.toByteArray()
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun requestCameraPermission() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            hasCameraPermission = isGranted
            if (isGranted) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
            setContent {
                RemindTheme {
                    var isFaceDetected by remember { mutableStateOf(false) }

                    if (hasCameraPermission) {
                        CameraView(isFaceDetected = isFaceDetected, onFaceDetected = { detected ->
                            isFaceDetected = detected
                        })
                    } else {
                        PermissionDeniedView()
                    }
                }
            }
        }
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    @Composable
    @androidx.camera.core.ExperimentalGetImage
    fun CameraView(isFaceDetected: Boolean, onFaceDetected: (Boolean) -> Unit) {
        val context = LocalContext.current
        var previewView by remember { mutableStateOf<PreviewView?>(null) }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AndroidView(factory = { ctx ->
                PreviewView(ctx).apply {
                    previewView = this
                    startCamera(this, onFaceDetected) // Start the camera once the PreviewView is available
                }
            }, modifier = Modifier.fillMaxSize())

            // Face Detected Indicator
            if (isFaceDetected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(uiColor.Green),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Face Detected", color = uiColor.White)
                }
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun startCamera(previewView: PreviewView, onFaceDetected: (Boolean) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                    detectFaces(imageProxy, ::onFaceDetectionComplete)                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Update the signature of the callback in the calling context
    private fun onFaceDetectionComplete(faceDetected: Boolean, confidence: Double?) {
        Log.d("FaceDetection", "Face detected with confidence: $faceDetected")
        if (faceDetected) {
            Log.d("FaceDetection", "Face detected with confidence: $confidence")
            // Additional actions based on face detection and confidence level
        } else {
            Log.d("FaceDetection", "No face detected.")
        }
    }

    // Function to calculate cosine similarity between two vectors
    fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        var dotProduct = 0.0
        var normVec1 = 0.0
        var normVec2 = 0.0
        for (i in vec1.indices) {
            dotProduct += (vec1[i] * vec2[i])
            normVec1 += (vec1[i] * vec1[i])
            normVec2 += (vec2[i] * vec2[i])
        }
        val denom = (Math.sqrt(normVec1) * Math.sqrt(normVec2))
        if (denom == 0.0) return 0.0 // Return 0 when dealing with zero vectors to avoid division by zero

        // Ensure the output is within the correct range
        val similarity = (dotProduct / denom).coerceIn(-1.0, 1.0)
        return similarity
    }

    // HashMap to store user data
    val usersMap = HashMap<String, RelationshipQuery>()

    fun fetchUsers() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        val databaseReference =
            userId?.let {
                FirebaseDatabase.getInstance().reference.child("users").child(it).child("relationships")
            }
        if (databaseReference != null) {
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dataSnapshot.children.forEach { childSnapshot ->
                        val user = childSnapshot.getValue(RelationshipQuery::class.java)

                        childSnapshot.key?.let { key ->
                            if (user != null) {
                                usersMap[key] = user
                            }
                        }
                    }
                    // Now `usersMap` contains all the users data
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle possible errors
                    Log.e("fetchUsers", "Error fetching users", databaseError.toException())
                }
            })
        }
    }

    val arr = HashMap<String, Bitmap>()

    suspend fun saveImageToGallery(context: Context, imageUrl: String, key: String) {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false) // Required for compatibility with Android API 28+
            .build()

        val result = imageLoader.execute(request)

        if (result is SuccessResult) {
            val bitmap = (result.drawable as BitmapDrawable).bitmap
            val image = InputImage.fromBitmap(bitmap, 0)
            var resizedBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces.first()
                        val boundingBox = face.boundingBox
                        val rotationDegrees = 0
                        val matrix = Matrix()
                        matrix.postRotate(rotationDegrees.toFloat())

                        // Calculate scaling factors assuming the rotation might change the width and height
                        val scaleX = bitmap.width.toFloat() / if (rotationDegrees % 180 == 0) image.width.toFloat() else image.height.toFloat()
                        val scaleY = bitmap.height.toFloat() / if (rotationDegrees % 180 == 0) image.height.toFloat() else image.width.toFloat()

                        // Adjust bounding box based on scaling
                        val scaledBox = Rect(
                            (boundingBox.left * scaleX).toInt(),
                            (boundingBox.top * scaleY).toInt(),
                            (boundingBox.right * scaleX).toInt(),
                            (boundingBox.bottom * scaleY).toInt()
                        )

                        // Validate and correct the coordinates to prevent out-of-bounds cropping
                        val left = scaledBox.left.coerceIn(0, bitmap.width - scaledBox.width())
                        val top = scaledBox.top.coerceIn(0, bitmap.height - scaledBox.height())
                        val width = scaledBox.width().coerceAtMost(bitmap.width - left)
                        val height = scaledBox.height().coerceAtMost(bitmap.height - top)

                        // Create the cropped bitmap
                        val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
                        resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, TF_OD_API_INPUT_SIZE,
                            TF_OD_API_INPUT_SIZE, true)
                        arr[key] = resizedBitmap
//                        Log.d("Sussy", "${arr.size}")
//                          saveBitmapToGallery(context, resizedBitmap, "GalleryExample", "Image Description")

                    }

                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Face detection from profile picture failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }


        }
    }

    private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String, description: String) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$title.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DESCRIPTION, description)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it).use { outputStream ->
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                }
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun detectFaces(imageProxy: ImageProxy, onFaceDetected: (Boolean, Double?) -> Unit) {
        val mediaImage = imageProxy.image ?: return
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastDetectionTime >= detectionDelay) {
                        lastDetectionTime = currentTime

                        fetchUsers()
                        if (arr.isEmpty()) {
                            usersMap.forEach { (key, value) ->
                                MainScope().launch {
                                    saveImageToGallery(this@HomeActivity, value.imageUrl, key)
                                }
                            }
                        }

                        val face = faces.first()
                        var isReal = false
                        Log.d("FaceDetection", "Processing face, bitmap array size: ${arr.size}")
                        val liveEmbedding = cropAndSaveImage(mediaImage, face.boundingBox, imageProxy.imageInfo.rotationDegrees)
                        val imageEmbedding = Array(1) { FloatArray(192) }
                        var confidence = 0.0
                        var person = ""

                        arr.forEach { (key, map) ->
                            interpreter.run(
                                convertBitmapToByteBuffer(map, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE),
                                imageEmbedding
                            )
                            confidence = cosineSimilarity(liveEmbedding, imageEmbedding[0])
                            if (confidence > 0.45) {
                                isReal = true
                                person = usersMap[key]?.firstName.toString()
                            }
                        }

                        if (isReal) {
                            Log.d("FaceDetection", "Recognized person: $person with confidence: $confidence")
                            sendPersonToChat(person)
                        } else {
                            Log.d("FaceDetection", "Unrecognized person with confidence: $confidence")
                        }
                    }
                } else {
                    onFaceDetected(false, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceDetection", "Face detection failed: ${e.message}", e)
                Toast.makeText(this, "Face detection failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                imageProxy.close() // Ensuring image proxy is closed after processing
            }
    }

    private fun sendPersonToChat(person: String) {
        val currentTime = System.currentTimeMillis()

        // Check if the time difference is less than the debounce interval
        if (currentTime - lastSentTime < debounceInterval) {
            Log.d("FaceDetection", "Debounced duplicate call to ChatScreenActivity")
            return // Skip calling ChatScreenActivity if within debounce interval
        }

        lastSentTime = currentTime // Update last sent time

        val intent = Intent(this, ChatScreenActivity::class.java)
        intent.putExtra("personName", person)
        intent.putExtra(
            "contextMessage",
            "The user has scanned the face of $person, likely intending to discuss or recall specific information, memories, or questions associated with them. Future prompts should reflect this context, focusing on personalized and supportive responses that engage with details related to $person."
        )
        startActivity(intent)
    }

    private fun loadBitmapArrAsset(): ArrayList<Bitmap> {
        val arr = ArrayList<Bitmap>()
        val assetManager = assets

        try {
            // List all files in the "folder" directory within the assets folder
            val files = assetManager.list("folder")
            files?.forEach { fileName ->
                // Load bitmap using the file name from the asset manager
                arr.add(loadBitmapFromAsset2("folder/$fileName"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error reading assets: ${e.message}")
        }

        return arr
    }

    private fun loadBitmapFromAsset2(path: String): Bitmap {
        val inputStream: InputStream = applicationContext.assets.open(path)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        return Bitmap.createScaledBitmap(bitmap, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, false)
    }

    private fun cropAndSaveImage(mediaImage: Image, boundingBox: Rect, rotationDegrees: Int) : FloatArray{
        val originalBitmap = BitmapUtils.getBitmapFromMediaImage(mediaImage, 0) // Get without rotation

        // Rotate the bitmap if necessary
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        val bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)

        // Calculate scaling factors assuming the rotation might change the width and height
        val scaleX = bitmap.width.toFloat() / if (rotationDegrees % 180 == 0) mediaImage.width.toFloat() else mediaImage.height.toFloat()
        val scaleY = bitmap.height.toFloat() / if (rotationDegrees % 180 == 0) mediaImage.height.toFloat() else mediaImage.width.toFloat()
        print(scaleX)
        print(scaleY)

        // Adjust bounding box based on scaling
        val scaledBox = Rect(
            (boundingBox.left * scaleX).toInt(),
            (boundingBox.top * scaleY).toInt(),
            (boundingBox.right * scaleX).toInt(),
            (boundingBox.bottom * scaleY).toInt()
        )

        // Validate and correct the coordinates to prevent out-of-bounds cropping
        val left = scaledBox.left.coerceIn(0, bitmap.width - scaledBox.width())
        val top = scaledBox.top.coerceIn(0, bitmap.height - scaledBox.height())
        val width = scaledBox.width().coerceAtMost(bitmap.width - left)
        val height = scaledBox.height().coerceAtMost(bitmap.height - top)

        // Create the cropped bitmap
        val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
        val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, TF_OD_API_INPUT_SIZE,
            TF_OD_API_INPUT_SIZE, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE)
        val embeddings = Array(1) { FloatArray(192) }
        interpreter.run(byteBuffer, embeddings)
        return embeddings[0];

//         Save the cropped image without compression
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/${getString(R.string.app_name)}")
//        }
//
//        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//        uri?.let {
//            contentResolver.openOutputStream(it)?.use { outputStream ->
//                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)  // JPEG compression set to highest quality
//                Toast.makeText(this, "Face cropped and saved to gallery", Toast.LENGTH_LONG).show()
//            } ?: Toast.makeText(this, "Failed to save image to gallery", Toast.LENGTH_SHORT).show()
//        }
//        return embeddings[0];
    }


    @Composable
    fun PermissionDeniedView() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Camera permission is required to use the app.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        faceDetector.close()
    }
}
