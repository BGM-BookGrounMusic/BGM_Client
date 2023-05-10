package com.example.bookgroundmusic

import android.content.Intent
import android.graphics.Color
import android.icu.number.IntegerWidth
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookgroundmusic.DataClass.SentimentAnalysisResponse
import com.example.bookgroundmusic.databinding.ActivityMainBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.FileOutputStream
import java.text.DecimalFormat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private val gson = Gson()

    // 음악 재생 판별용
    var isPaused = true

    var player = MediaPlayer()
    //val storage = FirebaseStorage.getInstance()

    // 모드 확인용
    var only_bgm = false
    var only_asmr = false
    var mode_mix = false

    // 장르 확인용
    var asmr = false
    var lofi = false
    var jazz = false
    var cl = false


    private val handler = Handler(Looper.getMainLooper())

    // 곡 남은 시간 확인
    private val checkRemainTimeRunnable = object : Runnable {
        override fun run() {
            val total_duration = player.duration
            val remainingTime = total_duration - player.currentPosition
            val remainingSecs = remainingTime / 1000
            val df = DecimalFormat("#")
            val formattedRemainingTime = df.format(remainingSecs)

            // 남은 시간이 30초일 때 스크린샷
            if (remainingSecs == 150) {
                Log.d("WJ", formattedRemainingTime + "초 남음")    // 성공
                screenshotSeries()
            }
            handler.postDelayed(this, 1000) // run every second
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //permissionCheck()
        startMainService()
        //initializeView()
        setListener()
        modeCheck()
        genreCheck()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCheckingRemainTime()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MediaProjectionController.mediaScreenCapture -> {
                MediaProjectionController.getMediaProjectionCapture(this, resultCode, data)
            }
        }
    }

    private fun startMainService() {

        val serviceIntent = Intent(this, MainService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

    }

    private fun stopMainService() {
        val serviceIntent = Intent(this, MainService::class.java)
        stopService(serviceIntent)
    }

    // 음악 남은 시간 알아내기
    private fun startCheckingRemainTime() {
        handler.postDelayed(checkRemainTimeRunnable, 1000) // start checking after 1 second
    }

    private fun stopCheckingRemainTime() {
        handler.removeCallbacks(checkRemainTimeRunnable)
    }


    // 음악 재생, 일시 정지
    fun musicControl() {
        // 음악 플레이리스트 초기화
        var playlist: ArrayList<Int> = ArrayList()
        playlist.add(R.raw.neutral_lofi1)

        //player.setAudioStreamType(AudioManager.STREAM_MUSIC)

        // 재생 안되고 있는 상태 => play 시키기
        if (isPaused) {
            player = MediaPlayer.create(this, playlist[0])
            player.start()


//            try {
//                storage.reference.child("감성음악/슬픔/050.mp3").downloadUrl.addOnSuccessListener {
//                    player.setDataSource(it.toString())
//                    player.prepare()
//                    player.start()
//                }
//
//
//            } catch (e: Exception) {
//                // TODO: handle exception
//            }

            isPaused = false
            Toast.makeText(this, "재생 시작함", Toast.LENGTH_LONG).show()

            startCheckingRemainTime()
        }

        // 재생 중인 상태 => pause 시키기
        else if (!isPaused) {
            player.pause()
            isPaused = true

            val total_duration = player.duration
            val remainingTime = total_duration - player.currentPosition
            val remainingSecs = remainingTime / 1000
            val df = DecimalFormat("#")
            val formattedRemainingTime = df.format(remainingSecs)

            Toast.makeText(this, "일시정지함, " + "남은 시간 " + formattedRemainingTime, Toast.LENGTH_LONG).show()
        }
    }


    //
    // 음악 Resume 구현 필요
    //


    // 일정 시간 간격 연속 스크린샷
    private fun screenshotSeries() {
        // OCR (ML Kit)
        val options = KoreanTextRecognizerOptions.Builder().build()
        val recognizer = TextRecognition.getClient(options)
        val intervalMillis = 30000

        var i = 1
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (i in 1..3) {
                    // 백그라운드 캡처
                    MediaProjectionController.screenCapture(this@MainActivity) { bitmap ->

                        val image = InputImage.fromBitmap(bitmap, 0)

                        val result = recognizer.process(image)
                             .addOnSuccessListener { visionText ->
                                 // string to txt file
                                val fileName = "text.txt"

                        var outputFile: FileOutputStream = openFileOutput(fileName, MODE_PRIVATE)
                        outputFile.write(visionText.text.toByteArray())
                        outputFile.close()
                        Log.d("WJ", visionText.text.toString())
                        Log.d("WJ", callSentimentAnalysisAPI(visionText.text.toString()))

                        //Sentiment 받아오기
                        //callSentimentAnalysisAPI(visionText.text.toString())
                    }
                    .addOnFailureListener { e -> }
                    }

                    i++
                    handler.postDelayed(this, intervalMillis.toLong())
                }
            }
        }, 10000)
    }

    //감성분석 Clova sentiment
    private fun callSentimentAnalysisAPI(text: String) : String {
        var sentiment = ""
        val url = "https://naveropenapi.apigw.ntruss.com/sentiment-analysis/v1/analyze"
        val client = OkHttpClient()
        val jsonBody = JSONObject()
            .put("content", text)
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())
        val request = Request.Builder()
            .url(url)
            .addHeader("X-NCP-APIGW-API-KEY-ID", "hzpldolb38")
            .addHeader("X-NCP-APIGW-API-KEY", "xRLUPCPWT8XpNDWXOQOgiSgKxz1XNpgcjMYixloY")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // API 호출 실패 처리
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                // API 호출 성공 처리
                val jsonResult = response.body?.string()
                val responseObject = gson.fromJson(jsonResult, SentimentAnalysisResponse::class.java)
                Log.d("API Response", jsonResult ?: "")

                if (responseObject.document == null){
                    Log.d("ERROR","분석이 제대로 완료되지 않았습니다.")
                }

                if (responseObject.document != null) {
                    // 전체 문장 감정 정보 로그 출력
                    Log.d("MainActivity", "전체 문장 감정: ${responseObject.document.sentiment}")
                    Log.d("MainActivity", "중립 감정 확률: ${responseObject.document.confidence.neutral}")
                    Log.d("MainActivity", "긍정 감정 확률: ${responseObject.document.confidence.positive}")
                    Log.d("MainActivity", "부정 감정 확률: ${responseObject.document.confidence.negative}")
                    sentiment = responseObject.document.sentiment
                }

                /* 나중에 필요하면 쓰깅~~~
                if (responseObject.sentences != null) {
                    for (sentence in responseObject.sentences) {
                        if (sentence.sentiment != null) {
                            // 분류 문장 감정 정보 로그 출력
                            Log.d("MainActivity", "분류 문장: ${sentence.content}")
                            Log.d("MainActivity", "문장 감정: ${sentence.sentiment}")
                            Log.d("MainActivity", "중립 감정 확률: ${sentence.confidence.neutral}")
                            Log.d("MainActivity", "긍정 감정 확률: ${sentence.confidence.positive}")
                            Log.d("MainActivity", "부정 감정 확률: ${sentence.confidence.negative}")
                        }
                    }
                }
                 */
            }
        })

        return sentiment
    }

    private fun setListener() {
        // 1. 시작하기 버튼 클릭시
        binding.btnOn.setOnClickListener {
            // 버튼 클릭 시 텍스트 변환
            if (binding.btnOn.text == "시작하기") {
                // 모드 or 장르 체크 안 했을 경우
                if ((!only_asmr && !only_bgm && !mode_mix) || (!asmr && !lofi && !jazz && !cl)) {
                    Toast.makeText(this, "모드 및 장르 설정을 모두 완료하였는지 다시 한번 확인해주세요.", Toast.LENGTH_LONG).show()
               }
                else {
                    binding.btnOn.setText("중지")
                    Toast.makeText(this, "지금부터 분석을 시작합니다.\n잠시 후, 책의 분위기에 어울리는 노래를 들려드립니다!", Toast.LENGTH_LONG).show()
                    musicControl()
                }
            } else {
                binding.btnOn.setText("시작하기")
                Toast.makeText(this, "분석을 중지합니다.\n'시작하기' 버튼을 다시 누르면, 배경음악이 다시 재생됩니다.", Toast.LENGTH_LONG).show()
                musicControl()
            }


            // 처음이면 중립음악 먼저 틀고 분석 시작 하도록 이후 구현

            }


        // 2. 사용 설명서 화면으로 이동
        binding.buttonGuide.setOnClickListener {
            val intent = Intent(this, GuideActivity::class.java)
            startActivity(intent)
        }

        // 3. 서영 감성분석 테스트
        binding.buttonTest.setOnClickListener {
            val etTest = findViewById<EditText>(R.id.et_test)
            //Sentiment 받아오기
            //callSentimentAnalysisAPI("시간이 약이라는 말이 두 사람에게도 유효한 것처럼 보일 정도였다. 부부는 그러다가도 이야깃거리만 있으면 끝도 없이 아이 이야기를 했다. 예전에는 울기만 했으나, 지금은 웃기만 하다 끝나는 날도 더러 있었다. 두 사람은 아이 이야기를 굳이 피하지 않았다. 처음에는 애써 잊으려고, 잊어야 산다고 생각했으나 결국 잊을 수 없다는 것을 깨닫는 데 얼마 걸리지 않았다. 문득 장난감 광고를 볼 때, 노란 버스가 지나갈 때, 어린이 보호구역 표지판을 볼 때, 어린 아역배우가 잘 자란 모습을 볼 때, 그리고 입학 시즌과 졸업 시즌을 지날 때마다 속수무책으로 무너지게 되는 것이었다. 아내는 아이의 잠든 얼굴이 보고 싶다고 했다. 남편은 목욕하고 나와 푹 안기던 포근한 살 냄새가 그립다고 했다. 두 사람의 목소리 사이에 스며들던 웃음소리. 두 사람을 골고루 빼닮은 우스운 버릇들. 아이는 5살에 멈춰버린 채, 둘만 늙어가는 시간이 너무나도 더디게 느껴졌다. 두 사람 모두 차라리 아이가 너무 외지 않을 때, 늦기 전에 보러 가고 싶다는 생각을 한 적이 있지만 차마 입 밖에는 낼 수 없었다. 그날 밤, 두 사람은 침대에 누워 등을 맞대고 돌아누웠다. 부부는 딱 아이가 누울 만큼의 자리를 습관처럼 남기고 누웠다. 그 공간이 서로의 울음소리가 들리지 않을 만큼 먼 거리는 아니었지만, 둘은 서로가 상대의 울음소리를 못 들은 척해주며 지냈다.")
            callSentimentAnalysisAPI(etTest.toString())
        }
    }

    // 모드 확인
    private fun modeCheck() {
        binding.mode1.setOnClickListener {
            if (!only_bgm) {
                if (only_asmr || mode_mix) {
                    only_asmr = false
                    mode_mix = false
                    binding.mode2.setTextColor(Color.parseColor("#000000"))
                    binding.mode3.setTextColor(Color.parseColor("#000000"))
                }
                binding.mode1.setTextColor(Color.parseColor("#FF730D"))
                only_bgm = true
            } else {
                binding.mode1.setTextColor(Color.parseColor("#000000"))
                only_bgm = false
            }
        }

        binding.mode2.setOnClickListener {
            if (!only_asmr) {
                if (only_bgm || mode_mix) {
                    only_bgm = false
                    mode_mix = false
                    binding.mode1.setTextColor(Color.parseColor("#000000"))
                    binding.mode3.setTextColor(Color.parseColor("#000000"))
                }
                binding.mode2.setTextColor(Color.parseColor("#FF730D"))
                only_asmr = true
            } else {
                binding.mode2.setTextColor(Color.parseColor("#000000"))
                only_asmr = false
            }
        }

        binding.mode3.setOnClickListener {
            if (!mode_mix) {
                if (only_asmr || only_bgm) {
                    only_asmr = false
                    only_bgm = false
                    binding.mode2.setTextColor(Color.parseColor("#000000"))
                    binding.mode1.setTextColor(Color.parseColor("#000000"))
                }
                binding.mode3.setTextColor(Color.parseColor("#FF730D"))
                mode_mix = true
            } else {
                binding.mode3.setTextColor(Color.parseColor("#000000"))
                mode_mix = false
            }
        }

    }

    // 장르 확인
    private fun genreCheck() {
        binding.genre1.setOnClickListener {
            if (!asmr) {
                if (lofi || jazz || cl) {
                    lofi = false
                    jazz = false
                    cl = false
                    binding.genre2.setTextColor(Color.parseColor("#000000"))
                    binding.genre3.setTextColor(Color.parseColor("#000000"))
                    binding.genre4.setTextColor(Color.parseColor("#000000"))
                }
                binding.genre1.setTextColor(Color.parseColor("#FF730D"))
                asmr = true
            } else {
                binding.genre1.setTextColor(Color.parseColor("#000000"))
                asmr = false
            }
        }

        binding.genre2.setOnClickListener {
            if (!lofi) {
                if (asmr || jazz || cl) {
                    asmr = false
                    jazz = false
                    cl = false
                    binding.genre1.setTextColor(Color.parseColor("#000000"))
                    binding.genre3.setTextColor(Color.parseColor("#000000"))
                    binding.genre4.setTextColor(Color.parseColor("#000000"))
                }
                binding.genre2.setTextColor(Color.parseColor("#FF730D"))
                lofi = true
            } else {
                binding.genre2.setTextColor(Color.parseColor("#000000"))
                lofi = false
            }
        }

        binding.genre3.setOnClickListener {
            if (!jazz) {
                if (lofi || asmr || cl) {
                    lofi = false
                    asmr = false
                    cl = false
                    binding.genre2.setTextColor(Color.parseColor("#000000"))
                    binding.genre1.setTextColor(Color.parseColor("#000000"))
                    binding.genre4.setTextColor(Color.parseColor("#000000"))
                }
                binding.genre3.setTextColor(Color.parseColor("#FF730D"))
                jazz = true
            } else {
                binding.genre3.setTextColor(Color.parseColor("#000000"))
                jazz = false
            }
        }

        binding.genre4.setOnClickListener {
            if (!cl) {
                if (lofi || jazz || asmr) {
                    lofi = false
                    jazz = false
                    asmr = false
                    binding.genre2.setTextColor(Color.parseColor("#000000"))
                    binding.genre3.setTextColor(Color.parseColor("#000000"))
                    binding.genre1.setTextColor(Color.parseColor("#000000"))
                }
                binding.genre4.setTextColor(Color.parseColor("#FF730D"))
                cl = true
            } else {
                binding.genre4.setTextColor(Color.parseColor("#000000"))
                cl = false
            }
        }
    }

}