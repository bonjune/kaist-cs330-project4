package com.example.pj4test

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.pj4test.audioInference.MeowDetector
import com.example.pj4test.cameraInference.CatDetector
import com.example.pj4test.cameraInference.CatImageAnalyzer
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HungryCatService : LifecycleService(), CatDetector.CatDetectionListener, MeowDetector.MeowingListener {
    private lateinit var camera: Camera

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var catDetector: CatDetector
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var catImageAnalyzer: CatImageAnalyzer

    private lateinit var meowDetector: MeowDetector

    private var isCatDetected = false
    private var isCatMeowing = false

    override fun onCreate() {
        super.onCreate()
        startForegroundService()

        cameraExecutor = Executors.newSingleThreadExecutor()

        catDetector = CatDetector(this, this)
        catDetector.setUpObjectDetector()

        catImageAnalyzer = CatImageAnalyzer(catDetector)

        meowDetector = MeowDetector(this, this)

        Log.d(TAG, "HungryCatService Start")
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        meowDetector.initializeAndStart()
        setUpCamera()

        intent?.action?.let { Log.d(TAG, it) }
        return START_STICKY
    }

    private fun startForegroundService() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "HCSNC",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        manager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle("Hungry Cat Service")
            setContentText("Hungry Cat Service is running")
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentIntent(pendingIntent)
            setWhen(0)
            setOnlyAlertOnce(true)
            setOngoing(true)
        }.build()

        startForeground(FOREGROUND_SERVICE_ID, notification)
    }

    private fun handleHungryCat() {
        val action = getString(R.string.hungry_cat_detected)
        val intent = Intent(action)
        intent.putExtra("meow", isCatMeowing)
        intent.putExtra("cat", isCatDetected)
        sendBroadcast(intent)
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                val cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases(cameraProvider)
            },
            ContextCompat.getMainExecutor(this)
        )
        Log.d(TAG, "Camera is set up")
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        val imageAnalysisBuilder =
            ImageAnalysis.Builder().apply {
                setTargetAspectRatio(AspectRatio.RATIO_4_3)
                setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            }

        imageAnalysis = imageAnalysisBuilder.build()

        // The analyzer can then be assigned to the instance
        imageAnalysis.setAnalyzer(cameraExecutor, catImageAnalyzer)

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    companion object {
        const val TAG = "HungryCatService"
        const val CHANNEL_ID = "HungryCatServiceChannel"
        const val FOREGROUND_SERVICE_ID = 3
    }

    override fun onMeowDetectionResult(meowScore: Float) {
        isCatMeowing = meowScore > MeowDetector.THRESHOLD
        if (isCatMeowing) {
            Log.d(TAG, "Meow detected")
            meowDetector.boostInference()
            catImageAnalyzer.boostInference()
        } else {
            meowDetector.setInferenceIdle()
            catImageAnalyzer.setInferenceIdle()
        }
        handleHungryCat()
    }

    override fun onCatDetectionError(error: String) {
        Log.e(TAG, "Object detector failed...")
    }

    override fun onCatDetectionResults(
        results: MutableList<Detection>,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        isCatDetected = results.find { it.categories[0].label == "cat" } != null
        if (isCatDetected) {
            Log.d(TAG, "Cat detected")
            meowDetector.boostInference()
            catImageAnalyzer.boostInference()
        } else {
            meowDetector.setInferenceIdle()
            catImageAnalyzer.setInferenceIdle()
        }
        handleHungryCat()
    }
}