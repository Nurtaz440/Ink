package com.nurtaz.dev.inkrecognation

import android.util.Log
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionResult
import java.util.concurrent.atomic.AtomicBoolean

class RecognationTask( private val recognizer: DigitalInkRecognizer, private val ink: Ink) {
    private var currentResult: RecognizedInk? = null
    private lateinit var canceled: AtomicBoolean
    private lateinit var done: AtomicBoolean
    fun cancel() {
        canceled.set(true)
    }

    fun done(): Boolean {
        return done.get()
    }

    fun result(): RecognizedInk? {
        return currentResult
    }

     class RecognizedInk internal constructor(val ink: Ink, val text: String?)
        fun run(): Task<String?> {
            Log.i("TAG", "Reco TAsk run")
            return recognizer!!
                .recognize(ink)
                .onSuccessTask(SuccessContinuation { result: RecognitionResult? ->

                    if (canceled.get() || result == null || result.candidates.isEmpty()) {
                        return@SuccessContinuation Tasks.forResult<String>(null)
                    }
                    currentResult =
                        RecognizedInk(
                            ink, result.candidates[0].text
                        )
                    Log.i("TAG", "result:" + currentResult!!.text)

                    done.set(true)
                    return@SuccessContinuation Tasks.forResult<String>(currentResult!!.text)
                }
                )
        }

        init {
            canceled = AtomicBoolean(false)
            done = AtomicBoolean(false)
        }
    }
