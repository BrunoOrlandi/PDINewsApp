package com.example.pdinewsapp.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pdinewsapp.databinding.ActivityOcrCaptureBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OcrCaptureActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private val cameraPermissionCode = 1001
    private lateinit var binding: ActivityOcrCaptureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
        } else {
            startCamera()
        }

        binding.captureButton.setOnClickListener {
            captureImage()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("PNA:OcrCamera", "Error to start camera: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        val photoFile = createFile(application.filesDir, FILENAME, PHOTO_EXTENSION)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("PNA:OcrCamera", "Error to capture the image: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    processImageForOcr(photoFile)
                }
            }
        )
    }

    private fun processImageForOcr(photoFile: File) {
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

        val left = (bitmap.width * 0.25).toInt()
        val top = (bitmap.height * 0.25).toInt()
        val right = (bitmap.width * 0.75).toInt()
        val bottom = (bitmap.height * 0.75).toInt()

        val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)

        runTextRecognition(croppedBitmap)
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                processTextRecognitionResult(visionText)
            }
            .addOnFailureListener { e ->
                Log.e("PNA:OcrCamera", "Error to recognize the text: ${e.message}")
            }
    }

    private fun processTextRecognitionResult(result: Text) {
        val recognizedText = result.text

        val singleLineText = recognizedText.replace("\n", " ").trim()

        Toast.makeText(this, "Recognized text: $singleLineText", Toast.LENGTH_LONG).show()

        val intent = Intent()
        intent.putExtra("recognizedText", singleLineText)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun createFile(baseFolder: File, fileName: String, extension: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val file = File(baseFolder, "$fileName$timestamp$extension")
        file.parentFile?.mkdirs()
        return file
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val FILENAME = "ocr_capture_"
        private const val PHOTO_EXTENSION = ".jpg"
    }
}

