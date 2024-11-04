package com.rumit.speech_recognition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.LongDef
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.rumit.speech_recognition.databinding.ActivityMainBinding
import com.rumit.speech_recognition.utility.IS_CONTINUES_LISTEN
import com.rumit.speech_recognition.utility.PERMISSIONS_REQUEST_RECORD_AUDIO
import com.rumit.speech_recognition.utility.RESULTS_LIMIT
import com.rumit.speech_recognition.utility.errorLog
import com.rumit.speech_recognition.utility.getErrorText
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var mContext: Context
    private lateinit var binding: ActivityMainBinding
    lateinit var textTospek:TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null


    private var selectedLanguage = "en" // Default "en selected"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textTospek= TextToSpeech(this,TextToSpeech.OnInitListener {
                status->

            if (status == TextToSpeech.SUCCESS){

                Log.d("TextToSpeech", "Initialization Success")

            }else{
                Log.d("TextToSpeech", "Initialization Failed")
            }
        })
        textTospek.setEngineByPackageName("com.google.android.tts")
        mContext = this
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.progressBar1.visibility=View.GONE
        setContentView(binding.root)
        setListeners()
        checkPermissions()
        resetSpeechRecognizer()
        setRecogniserIntent()
        prepareLocales()
    }

    private fun setListeners() {
        binding.btnStartListen.setOnClickListener {
            startListening()
            binding.progressBar1.visibility=View.VISIBLE
        }

        binding.forcestop.setOnClickListener{
            onStop()
            resetSpeechRecognizer()
            binding.progressBar1.visibility=View.GONE
        }
//        binding.forcestop.setOnClickListener {
//            textToChineseSpeak("你好吗")
//        }

    }
    fun textToChineseSpeak(text:String){
//        textTospek.setEngineByPackageName("com.google.android.tts")
       //textTospek.setLanguage(Locale("zh_CN"))
        //bn_BD
        //zh_CN
        textTospek.speak(text, TextToSpeech.QUEUE_ADD, null )

    }
    fun textToBanglaSpeak(text:String){
        textTospek.speak(text, TextToSpeech.QUEUE_ADD, null)
//        textTospek.setLanguage(Locale("bn_BD"))
        //bn_BD
        //zh_CN
//        if (textTospek.isSpeaking){
//            textTospek.stop()
//            //startOrPauseButton.text = "Start"
//        }else{
//            textTospek.speak(text, TextToSpeech.QUEUE_FLUSH, null)
//            //startOrPauseButton.text = "Pause"
//        }

    }
    private fun checkPermissions() {
        val permissionCheck =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
            return
        }
    }

    private fun resetSpeechRecognizer() {
        if (speechRecognizer != null) speechRecognizer!!.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext)
        errorLog(
            "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(mContext)
        )
        if (SpeechRecognizer.isRecognitionAvailable(mContext))
            speechRecognizer!!.setRecognitionListener(mRecognitionListener)
        else finish()
    }

    private fun setRecogniserIntent() {
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
            selectedLanguage
        )
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            selectedLanguage
        )
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, RESULTS_LIMIT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               //startListening()
            } else {
                Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startListening() {
        speechRecognizer!!.startListening(recognizerIntent)
       // binding.progressBar1.visibility = View.VISIBLE
    }

    public override fun onResume() {
        errorLog("resume")
        super.onResume()
        resetSpeechRecognizer()
        if (IS_CONTINUES_LISTEN) {
            startListening()
        }
    }

    override fun onPause() {
        errorLog("pause")
        super.onPause()
        speechRecognizer!!.stopListening()
    }

    override fun onStop() {
        errorLog("stop")
        super.onStop()
        speechRecognizer!!.stopListening()
       // speechRecognizer!!.destroy()
    }


    private fun prepareLocales() {
        val availableLocales =
            listOf(Locale("bn-BD"),Locale("zh-CN")) //Alternatively you can check https://cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages

        val adapterLocalization: ArrayAdapter<Any?> = ArrayAdapter<Any?>(
            mContext,
            android.R.layout.simple_spinner_item,
            availableLocales
        )
        adapterLocalization.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                selectedLanguage = availableLocales[position].toString()


                setRecogniserIntent()
                binding.progressBar1.visibility=View.GONE
                onStop()
                resetSpeechRecognizer()
                textTospek.stop()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
//                TODO("Not yet implemented")
            }
        }

        binding.spinner1.adapter = adapterLocalization

        // Set "en" as selected language by default
        for (i in availableLocales.indices) {
            val locale = availableLocales[i]
            if (locale.toString().equals("en", true)) {
                binding.spinner1.setSelection(i)
                break
            }
        }
    }

    private val mRecognitionListener = object : RecognitionListener {
        override fun onBeginningOfSpeech() {
            errorLog("onBeginningOfSpeech")
            binding.progressBar1.isIndeterminate = false
            binding.progressBar1.max = 10
        }

        override fun onBufferReceived(buffer: ByteArray) {
            errorLog("onBufferReceived: $buffer")
        }

        override fun onEndOfSpeech() {
            errorLog("onEndOfSpeech")
            binding.progressBar1.isIndeterminate = true
            speechRecognizer!!.stopListening()
        }

        override fun onResults(results: Bundle) {
            errorLog("onResults")
            val matches: ArrayList<String>? = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            var text = ""
            for (result in matches!!) text += result
            binding.textView1.text = text

            //translate by backend service
            Log.d("select languages",selectedLanguage.toString())
            if(selectedLanguage.toString()=="bn-bd"){
                postBanglaToChinese(text.toString()) { response ->
                    if (response != null) {
                        Log.d("Reponse From Api:name: ",response.data.toString())
                        textToChineseSpeak(response.data.toString())
                    } else {
                        println("Failed to get a response from the API.")
                    }
                }
            }else{
                postChineseToBangla(text.toString()) { response ->
                    if (response != null) {
                        Log.d("post chinese to bangla",response.data.toString())
                        textToBanglaSpeak(response.data.toString())
                    } else {
                        println("Failed to get a response from the API.")
                    }
                }
            }


            if (IS_CONTINUES_LISTEN) {
                startListening()
            } else {
                binding.progressBar1.visibility = View.GONE
            }
        }

        override fun onError(errorCode: Int) {
            val errorMessage = getErrorText(errorCode)
            errorLog("FAILED $errorMessage")
           // binding.tvError.text = errorMessage

            // rest voice recogniser
            resetSpeechRecognizer()
            startListening()
        }

        override fun onEvent(arg0: Int, arg1: Bundle) {
            errorLog("onEvent")
        }

        override fun onPartialResults(arg0: Bundle) {
            errorLog("onPartialResults")
        }

        override fun onReadyForSpeech(arg0: Bundle) {
            errorLog("onReadyForSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            binding.progressBar1.progress = rmsdB.toInt()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textTospek.stop()
        textTospek.shutdown()
        speechRecognizer!!.stopListening()
        speechRecognizer!!.destroy()

    }

    fun postChineseToBangla(text: String, callback: (TranslateModel?) -> Unit) {
        // Define the OkHttp client
        val client = OkHttpClient()

        // Create the JSON object for the request body
        val jsonObject = JSONObject()
        jsonObject.put("text", text)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        // Build the request body
        val requestBody = RequestBody.create(
            mediaType,
            jsonObject.toString()
        )

        // Build the request
        val request = Request.Builder()
            .url("https://sabitur.techjus.com/sabiturvoice/chinese_voice_to_bangla")
            .post(requestBody)
            .build()
        val gson = Gson()
        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // Call the callback with null on failure
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val translatemodel:TranslateModel=gson.fromJson(response.body?.string(),TranslateModel::class.java)
//                    val responseData = response.body?.string()
                    // Pass the response data to the callback
                    callback(translatemodel)
                } else {
                    // Handle the case where the server responds with an error
                    callback(null)
                }
            }
        })
    }
    fun postBanglaToChinese(text: String, callback: (TranslateModel?) -> Unit) {
        // Define the OkHttp client
        val client = OkHttpClient()

        // Create the JSON object for the request body
        val jsonObject = JSONObject()
        jsonObject.put("text", text)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        // Build the request body
        val requestBody = RequestBody.create(
            mediaType,
            jsonObject.toString()
        )

        // Build the request
        val request = Request.Builder()
            .url("https://sabitur.techjus.com/sabiturvoice/translate_bangla_to_chinese")
            .post(requestBody)
            .build()
        val gson = Gson()
        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // Call the callback with null on failure
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val translatemodel:TranslateModel=gson.fromJson(response.body?.string(),TranslateModel::class.java)
//                    val responseData = response.body?.string()
                    // Pass the response data to the callback
                    callback(translatemodel)
                } else {
                    // Handle the case where the server responds with an error
                    callback(null)
                }
            }
        })
    }


}