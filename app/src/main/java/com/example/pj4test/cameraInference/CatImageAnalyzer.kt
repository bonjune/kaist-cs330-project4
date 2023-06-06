package com.example.pj4test.cameraInference

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class CatImageAnalyzer(private val detector: CatDetector): ImageAnalysis.Analyzer {
    private var lastFrameTimestamp: Long = 0
    private lateinit var buffer: Bitmap

    override fun analyze(image: ImageProxy) {
        if (!::buffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running
            buffer = Bitmap.createBitmap(
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
        }

        val currentTimestamp = System.currentTimeMillis()
        if (lastFrameTimestamp != 0L) {
            val elapsedMillis = currentTimestamp - lastFrameTimestamp
            val fps = 1000 / elapsedMillis.toFloat()
            Log.d("CameraFPS", "FPS: $fps")
        }
        lastFrameTimestamp = currentTimestamp

        image.use {
            buffer.copyPixelsFromBuffer(image.planes[0].buffer)
        }
        detector.detect(buffer, image.imageInfo.rotationDegrees)
    }
}