package com.nurtaz.dev.inkrecognation

import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Message
import android.util.Log
import android.view.MotionEvent
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.common.annotations.VisibleForTesting
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.Ink.Stroke
import okio.JvmField
import java.util.logging.Handler

class StrokeManager {
    interface ContentChangedListener {
        fun onContentChanged()
    }

    interface StatusChangedListener {
        fun onStatusChanged()
    }

    interface DownLoadedModelsChangedListener {
        fun onDownLoadedModelsChanged(downloadedLanguageTags: Set<String>)
    }

    private var recognitionTask: RecognationTask? = null

    @JvmField
    @VisibleForTesting
    var modelManager =
        ModelManager()

    private val content: MutableList<RecognationTask.RecognizedInk> = ArrayList()

    //managing ink currently drawn
    private var strokeBuilder = Stroke.builder()
    private var inkBuilder = Ink.builder()
    private var stateChangedScinceLastRequest = false
    private var contentChangedListener: ContentChangedListener? = null
    private var statusChangedListener: StatusChangedListener? = null
    private var downLoadedModelsChangedListener: DownLoadedModelsChangedListener? = null
    private var triggerRecognitionAfterInput = true
    private var clearCurrentAfterReconition = true

    var status: String? = ""
        private set(newStatus) {
            field = newStatus
            statusChangedListener!!.onStatusChanged()
        }

    fun setTriggerRecognitionAfterInput(shouldTrigger: Boolean) {
        triggerRecognitionAfterInput = shouldTrigger
    }

    fun setClearCurrentInkRecognition(shouldClear: Boolean) {
        clearCurrentAfterReconition = shouldClear
    }

    private val uiHandler = android.os.Handler(
        android.os.Handler.Callback { msg: Message ->
            if (msg.what == TIMEOUT_TRIGGER) {
                Log.i("TAG", "handling timeout trigger")
                comitResult()
                return@Callback true
            }
            false
        }
    )

    private fun comitResult() {
        recognitionTask!!.result().let {
            content.add(it!!)
            status = "Successfull recognition is: " + it!!.text
            if (clearCurrentAfterReconition) {
                resetCurrentInk()
            }
            contentChangedListener!!.onContentChanged()
        }
    }

    fun reset() {
        Log.i("TAG", "reset")
        resetCurrentInk()
        content.clear()
        recognitionTask!!.cancel()
        status = ""
    }

    private fun resetCurrentInk() {
        inkBuilder = Ink.builder()
        strokeBuilder = Stroke.builder()
        stateChangedScinceLastRequest = false
    }

    val currentInk: Ink
        get() = inkBuilder.build()

    fun addNewTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val x = event.x
        val y = event.y
        val t = System.currentTimeMillis()

        uiHandler.removeMessages(TIMEOUT_TRIGGER)
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> strokeBuilder.addPoint(
                Ink.Point.create(
                    x,
                    y,
                    t
                )
            )

            MotionEvent.ACTION_UP -> {
                strokeBuilder.addPoint(Ink.Point.create(x, y, t))
                inkBuilder.addStroke(strokeBuilder.build())
                strokeBuilder = Stroke.builder()
                stateChangedScinceLastRequest = true
                if (triggerRecognitionAfterInput) {
                    recognize()
                }
            }

            else ->
                return false
        }
        return true
    }

    fun setContentChangedListener(contentChangedListener: ContentChangedListener?) {
        this.contentChangedListener = contentChangedListener
    }

    fun setStatusChangedListener(statusChangedListener: StatusChangedListener?) {
        this.statusChangedListener = statusChangedListener
    }

    fun setDownLoadedModelsChangedListener(
        downLoadedModelsChangedListener: DownLoadedModelsChangedListener?
    ) {
        this.downLoadedModelsChangedListener = downLoadedModelsChangedListener
    }

    fun getContent(): List<RecognationTask.RecognizedInk>{
        return content
    }

    fun setActiveModel(languageTag: String) {
        status = modelManager.setModel(languageTag)
    }


    fun deleteActiveModel(): Task<Nothing?> {
        return modelManager
            .deleteactiveModel()
            .addOnSuccessListener { refreshDownloadedModelStatus() }
            .onSuccessTask(SuccessContinuation { status: String? ->
                this.status = status
                return@SuccessContinuation Tasks.forResult(null)
            }
            )
    }

    fun download(): Task<Nothing?> {
        status = "Download Started"
        return modelManager
            .download()
            .addOnSuccessListener { refreshDownloadedModelStatus() }
            .onSuccessTask(SuccessContinuation { status: String? ->
                this.status = status
                return@SuccessContinuation Tasks.forResult(null)
            }
            )
    }
    fun recognize():Task<String?>{
        if (!stateChangedScinceLastRequest || inkBuilder.isEmpty){
            status = "no recognation ink unchanged or emty"
            return Tasks.forResult(null)
        }
        if (modelManager.recognizer  == null){
            status = "Recognizer no set"
            return Tasks.forResult(null)
        }
        return modelManager
            .checkIsModelDownloded()
            .onSuccessTask { result ->
                if (!result!!){
                    status = "Model not downloaded yet"
                  Tasks.forResult<String?>(null)
                }
                stateChangedScinceLastRequest = false
                recognitionTask =
                    RecognationTask(
                        modelManager.recognizer!!,
                        inkBuilder.build()
                    )

                uiHandler.sendMessageDelayed(
                    uiHandler.obtainMessage(TIMEOUT_TRIGGER),
                    CONVERSATION_TIMEOUT_MS
                )
                recognitionTask!!.run()
            }
    }
    fun refreshDownloadedModelStatus(){
        modelManager.downloadLanguage
            .addOnSuccessListener { downLoadedLanguageTag : Set<String>->
                downLoadedModelsChangedListener!!.onDownLoadedModelsChanged(downLoadedLanguageTag)
            }
    }

    companion object{
        @JvmField
        @VisibleForTesting
        val CONVERSATION_TIMEOUT_MS: Long = 1000
        private const val TIMEOUT_TRIGGER = 1
    }
}