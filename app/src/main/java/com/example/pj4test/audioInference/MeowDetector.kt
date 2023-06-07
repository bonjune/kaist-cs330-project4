package com.example.pj4test.audioInference

import android.content.Context
import android.media.AudioRecord
import android.util.Log
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.system.measureTimeMillis


class MeowDetector(private val context: Context, private val listener: MeowingListener) {
    // Libraries for audio classification
    lateinit var classifier: AudioClassifier
    lateinit var recorder: AudioRecord
    lateinit var tensor: TensorAudio

    // TimerTask
    private var task: TimerTask? = null

    fun initializeAndStart() {
        classifier = AudioClassifier.createFromFile(context, YAMNET_MODEL)
        Log.d(TAG, "Model loaded from: $YAMNET_MODEL")
        audioInitialize()
        startRecording()

        startInference()
    }

    /**
     * audioInitialize
     *
     * Create the instance of TensorAudio and AudioRecord from the AudioClassifier.
     */
    private fun audioInitialize() {
        tensor = classifier.createInputTensorAudio()

        val format = classifier.requiredTensorAudioFormat
        val recorderSpecs = "Number Of Channels: ${format.channels}\n" +
                "Sample Rate: ${format.sampleRate}"
        Log.d(TAG, recorderSpecs)
        Log.d(TAG, classifier.requiredInputBufferSize.toString())

        recorder = classifier.createAudioRecord()
    }

    /**
     * startRecording
     *
     * This method make recorder start recording.
     * After this function, the microphone is ready for reading.
     */
    private fun startRecording() {
        recorder.startRecording()
        Log.d(TAG, "record started!")
    }

    /**
     * stopRecording
     *
     * This method make recorder stop recording.
     * After this function, the microphone is unavailable for reading.
     */
    private fun stopRecording() {
        recorder.stop()
        Log.d(TAG, "record stopped.")
    }

    private fun inference(): Float {
        // record의 데이터를 tensor로 바로 옮기기 위해 array allocation 1번, data copy 2번 발생
        tensor.load(recorder)
        val output = classifier.classify(tensor)

        val catCategory = output[0].categories.filter {
            it.label == "Cat" || it.label == "Purr" || it.label == "Meow" || it.label == "Hiss"
        }.maxByOrNull {
            it.score
        } ?: return 0F

        Log.d(TAG, "${catCategory.label}, ${catCategory.score}")
        return catCategory.score
    }

    private var inferenceIntervalMs: Long = 1000L
    private fun startInference() {
        if (task == null) {
            task = Timer().scheduleAtFixedRate(0, inferenceIntervalMs) {
                val score = inference()
                listener.onMeowDetectionResult(score)
            }
        }
    }

    fun boostInference() {
        if (inferenceIntervalMs == 50L) {
            return
        }
        inferenceIntervalMs = 50L
        rescheduleInference()
    }

    fun setInferenceIdle() {
        if (inferenceIntervalMs == 1000L) {
            return
        }
        inferenceIntervalMs = 1000L
        rescheduleInference()
    }

    private fun rescheduleInference() {
        task?.cancel()
        task = Timer().scheduleAtFixedRate(0, inferenceIntervalMs) {
            val score = inference()
            listener.onMeowDetectionResult(score)
        }
    }

    /**
     * interface DetectorListener
     *
     * This is an interface for listener.
     * To get result from this classifier, inherit this interface
     * and set itself to this' detector listener
     */
    interface MeowingListener {
        fun onMeowDetectionResult(meowScore: Float)
    }

    /**
     * companion object
     *
     * This includes useful constants for this classifier.
     *
     * @property    TAG                 tag for logging
     * @property    REFRESH_INTERVAL_MS refresh interval of the inference
     * @property    YAMNET_MODEL        file path of the model file
     */
    companion object {
        const val TAG = "MeowDetector"
        const val REFRESH_INTERVAL_MS = 33L
        const val YAMNET_MODEL = "yamnet_classification.tflite"
        const val THRESHOLD = 0.3f
    }
}