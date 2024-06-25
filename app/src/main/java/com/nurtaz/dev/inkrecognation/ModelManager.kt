package com.nurtaz.dev.inkrecognation

import android.util.Log
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions

class ModelManager {
    private var model: DigitalInkRecognitionModel? = null
    var recognizer: DigitalInkRecognizer? = null
    val remoteModelManager = RemoteModelManager.getInstance()
    fun setModel(languageTag : String):String {
        model = null
//        recognizer!!.close()
        recognizer = null
        val modelIdentifier: DigitalInkRecognitionModelIdentifier?
        modelIdentifier = try {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
        } catch (e:MlKitException) {
        Log.e("TAG","Failed to parse language '$languageTag'")
            return ""
        } ?: return "No model for language: $languageTag"

    model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model!!).build()
        )

        Log.i("TAG","Model set for language $languageTag ('${modelIdentifier.languageTag}')")

        return "Model set for language: $languageTag"
    }

    fun checkIsModelDownloded():Task<Boolean>{
        return remoteModelManager.isModelDownloaded(model!!)
    }

    fun deleteactiveModel():Task<String?>{
        if (model == null){
            Log.i("TAG","Model not set")
            return Tasks.forResult("Model not set")
        }
        return checkIsModelDownloded()
            .onSuccessTask { result:Boolean?->
                if (!result!!){
                    return@onSuccessTask Tasks.forResult("Model not downloaded yet")
                }
                remoteModelManager
                    .deleteDownloadedModel(model!!)
                .onSuccessTask { _:Void? ->
                    Log.i("TAG","Model successfully deleted")
                    Tasks.forResult("Model successfully deleted ")
                }
            }.addOnFailureListener { e:Exception->
                Log.i("TAG","Error while model deletion $e")
            }
    }
    val downloadLanguage : Task<Set<String>>
        get() = remoteModelManager
            .getDownloadedModels(DigitalInkRecognitionModel::class.java)
            .onSuccessTask (SuccessContinuation{remoteModels:Set<DigitalInkRecognitionModel>? ->
                    val result : MutableSet<String> = HashSet()
                    for (model in remoteModels!!){
                        result.add(model.modelIdentifier.languageTag)
                    }
                    Log.i("TAG","Downloaded model for language $result")
                    Tasks.forResult(result.toSet())
                }
            )

    fun download():Task<String?>{
        return if (model == null){
            Tasks.forResult("Model not selected")
        }else{
            remoteModelManager.download(model!!, DownloadConditions.Builder().build())
                .onSuccessTask { _:Void ->
                    Log.i("TAG","Model download successfully ")
                    Tasks.forResult("Model download successfully")
                }
                .addOnFailureListener { e:Exception->
                    Log.i("TAG","Error while model downloading $e")
                }
        }
    }

}