/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.pj4test.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.pj4test.ProjectConfiguration
import com.example.pj4test.R
import com.example.pj4test.audioInference.MeowDetector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.pj4test.cameraInference.CatDetector
import com.example.pj4test.databinding.FragmentHungryCatBinding
import org.tensorflow.lite.task.vision.detector.Detection

class HungryCatFragment : Fragment(), CatDetector.CatDetectionListener, MeowDetector.MeowingListener {
    private val _tag = "HungryCatFragment"

    private var _fragmentBinding: FragmentHungryCatBinding? = null

    private val fragmentBinding
        get() = _fragmentBinding!!

    private lateinit var catDetectionView: TextView
    
    private lateinit var catDetector: CatDetector
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    private lateinit var meowDetector: MeowDetector
    private lateinit var meowView: TextView

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    private var isCatDetected: Boolean = false
    private var isCatMeowing: Boolean = false

    override fun onDestroyView() {
        _fragmentBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentBinding = FragmentHungryCatBinding.inflate(inflater, container, false)
        Log.d(_tag, "Fragment inflated")
        return fragmentBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catDetector = CatDetector(requireContext(), this)
        catDetector.setUpObjectDetector()

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
         fragmentBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        catDetectionView = fragmentBinding.CatDetectionView
        meowView = fragmentBinding.MeowView

        meowDetector = MeowDetector(requireContext(), this)
        meowDetector.initializeAndStart()

        Log.d(_tag, "Detectors are set up")
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                val cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases(cameraProvider)
            },
            ContextCompat.getMainExecutor(requireContext())
        )
        Log.d(_tag, "Camera is set up")
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation( fragmentBinding.viewFinder.display.rotation)
                .build()
        // Attach the viewfinder's surface provider to preview use case
        preview?.setSurfaceProvider( fragmentBinding.viewFinder.surfaceProvider)


        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation( fragmentBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
        // The analyzer can then be assigned to the instance
        imageAnalyzer!!.setAnalyzer(cameraExecutor) { image -> detectObjects(image) }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e(_tag, "Use case binding failed", exc)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentBinding.viewFinder.display.rotation
    }

    private fun detectObjects(image: ImageProxy) {
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running
            bitmapBuffer = Bitmap.createBitmap(
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
        }
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        val imageRotation = image.imageInfo.rotationDegrees

        // Pass Bitmap and rotation to the object detector helper for processing and detection
        catDetector.detect(bitmapBuffer, imageRotation)
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onCatDetectionResults(
        results: MutableList<Detection>,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        Log.d(_tag, "Cat (object) detection took $inferenceTime")
        isCatDetected = results.find { it.categories[0].label == "cat" } != null
        handleHungryCat()
        activity?.runOnUiThread {
            // Pass necessary information to OverlayView for drawing on the canvas
            fragmentBinding.overlay.setResults(
                results,
                imageHeight,
                imageWidth
            )

            // change UI according to the result
            if (isCatDetected) {
                catDetectionView.setText(R.string.cat_detected)
                catDetectionView.setBackgroundColor(ProjectConfiguration.activeBackgroundColor)
                catDetectionView.setTextColor(ProjectConfiguration.activeTextColor)
            } else {
                catDetectionView.setText(R.string.cat_not_detected)
                catDetectionView.setBackgroundColor(ProjectConfiguration.idleBackgroundColor)
                catDetectionView.setTextColor(ProjectConfiguration.idleTextColor)
            }

            // Force a redraw
            fragmentBinding.overlay.invalidate()
        }
    }

    override fun onCatDetectionError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMeowDetectionResult(meowScore: Float) {
        isCatMeowing = meowScore > MeowDetector.THRESHOLD
        handleHungryCat()
        activity?.runOnUiThread {
            if (isCatMeowing) {
                meowView.setText(R.string.meow_detected)
                meowView.setBackgroundColor(ProjectConfiguration.activeBackgroundColor)
                meowView.setTextColor(ProjectConfiguration.activeTextColor)
            } else {
                meowView.setText(R.string.meow_not_detected)
                meowView.setBackgroundColor(ProjectConfiguration.idleBackgroundColor)
                meowView.setTextColor(ProjectConfiguration.idleTextColor)
            }
        }
    }

    private fun handleHungryCat() {
        if (isCatMeowing && isCatDetected) {
            Log.d(_tag, "Cat is hungry")
        }
    }
}
