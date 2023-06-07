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
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.fragment.app.Fragment
import com.example.pj4test.ProjectConfiguration
import com.example.pj4test.R
import com.example.pj4test.databinding.FragmentHungryCatBinding

class HungryCatFragment : Fragment() {
    companion object {
        const val TAG = "HungryCatFragment"
    }

    private var fragmentBinding: FragmentHungryCatBinding? = null

    private lateinit var camera: Camera

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
        fragmentBinding = null
        super.onDestroyView()

        requireActivity().unregisterReceiver(hungryCatReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentHungryCatBinding.inflate(inflater, container, false)
        val filter = IntentFilter(getString(R.string.hungry_cat_detected))
        requireActivity().registerReceiver(hungryCatReceiver, filter)
        Log.d(TAG, "Fragment inflated")
        return fragmentBinding!!.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
    }

    private fun handleCatDetection(isCatDetected: Boolean) {
        val catDetectionView = fragmentBinding?.CatDetectionView
        activity?.runOnUiThread {
            // change UI according to the result
            if (isCatDetected) {
                catDetectionView?.apply {
                    setText(R.string.cat_detected)
                    setBackgroundColor(ProjectConfiguration.activeBackgroundColor)
                    setTextColor(ProjectConfiguration.activeTextColor)
                }
            } else {
                catDetectionView?.apply {
                    setText(R.string.cat_not_detected)
                    setBackgroundColor(ProjectConfiguration.idleBackgroundColor)
                    setTextColor(ProjectConfiguration.idleTextColor)
                }
            }
        }
    }

    private fun handleMeowDetection(isMeowDetected: Boolean) {
        val meowView = fragmentBinding?.MeowView
        activity?.runOnUiThread {
            if (isMeowDetected) {
                meowView?.apply {
                    setText(R.string.meow_detected)
                    setBackgroundColor(ProjectConfiguration.activeBackgroundColor)
                    setTextColor(ProjectConfiguration.activeTextColor)
                }
            } else {
                meowView?.apply {
                    setText(R.string.meow_not_detected)
                    setBackgroundColor(ProjectConfiguration.idleBackgroundColor)
                    setTextColor(ProjectConfiguration.idleTextColor)
                }
            }
        }
    }
}
