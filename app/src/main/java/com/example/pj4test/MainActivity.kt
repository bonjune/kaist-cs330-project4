package com.example.pj4test

import android.Manifest.permission.CAMERA
import android.Manifest.permission.INTERNET
import android.Manifest.permission.RECORD_AUDIO
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity(), HungryCatServiceCallback {
    private val permissions = arrayOf(RECORD_AUDIO, CAMERA, INTERNET)
    private val PERMISSIONS_REQUEST = 0x0000001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        val intent = Intent(this, HungryCatService::class.java)
        intent.action = "Start HungryCatService!"
        startForegroundService(intent)

    }

    private fun checkPermissions() {
        if (permissions.all{ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED}){
            Log.d(TAG, "All Permission Granted")
        }
        else{
            requestPermissions(permissions, PERMISSIONS_REQUEST)
        }
    }

    override fun onHungryCatDetected() {
        Log.d(TAG, "HungryCat!!")
    }

    companion object {
        const val TAG = "MainActivity"
    }
}