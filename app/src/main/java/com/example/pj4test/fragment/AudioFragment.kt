package com.example.pj4test.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.pj4test.ProjectConfiguration
import com.example.pj4test.audioInference.MeowDetector
import com.example.pj4test.databinding.FragmentAudioBinding

class AudioFragment: Fragment(), MeowDetector.DetectorListener {
    private val TAG = "AudioFragment"

    private var _fragmentAudioBinding: FragmentAudioBinding? = null

    private val fragmentAudioBinding
        get() = _fragmentAudioBinding!!

    // classifiers
    lateinit var meowDetector: MeowDetector

    // views
    lateinit var snapView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentAudioBinding = FragmentAudioBinding.inflate(inflater, container, false)

        return fragmentAudioBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        snapView = fragmentAudioBinding.SnapView

        meowDetector = MeowDetector()
        meowDetector.initialize(requireContext())
        meowDetector.setDetectorListener(this)
    }

    override fun onPause() {
        super.onPause()
        meowDetector.stopInferencing()
    }

    override fun onResume() {
        super.onResume()
        meowDetector.startInferencing()
    }

    override fun onResults(score: Float) {
        activity?.runOnUiThread {
            if (score > MeowDetector.THRESHOLD) {
                snapView.text = "MEOW"
                snapView.setBackgroundColor(ProjectConfiguration.activeBackgroundColor)
                snapView.setTextColor(ProjectConfiguration.activeTextColor)
            } else {
                snapView.text = "NO MEOW"
                snapView.setBackgroundColor(ProjectConfiguration.idleBackgroundColor)
                snapView.setTextColor(ProjectConfiguration.idleTextColor)
            }
        }
    }
}