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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.fragment.app.Fragment
import com.example.pj4test.ProjectConfiguration
import com.example.pj4test.R
import com.example.pj4test.databinding.FragmentHungryCatBinding

class HungryCatFragment : Fragment() {
    private val _tag = "HungryCatFragment"

    private var _fragmentBinding: FragmentHungryCatBinding? = null

    private val fragmentBinding
        get() = _fragmentBinding!!

    private lateinit var catDetectionView: TextView

    private var imageAnalysis: ImageAnalysis? = null

    private lateinit var meowView: TextView

    private val hungryCatReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null)
                return
            if (intent.action == getString(R.string.hungry_cat_detected)) {
                handleMeowDetection(intent.getBooleanExtra("meow", false))
                handleCatDetection(intent.getBooleanExtra("cat", false))
            }
        }
    }

    override fun onDestroyView() {
        _fragmentBinding = null
        super.onDestroyView()

        requireActivity().unregisterReceiver(hungryCatReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentBinding = FragmentHungryCatBinding.inflate(inflater, container, false)
        val filter = IntentFilter(getString(R.string.hungry_cat_detected))
        requireActivity().registerReceiver(hungryCatReceiver, filter)
        Log.d(_tag, "Fragment inflated")
        return fragmentBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        // Wait for the views to be properly laid out
        catDetectionView = fragmentBinding.CatDetectionView
        meowView = fragmentBinding.MeowView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalysis?.targetRotation = fragmentBinding.viewFinder.display.rotation
    }

    private fun handleCatDetection(isCatDetected: Boolean) {
        activity?.runOnUiThread {
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
        }
    }

    private fun handleMeowDetection(isMeowDetected: Boolean) {
        activity?.runOnUiThread {
            if (isMeowDetected) {
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
}
