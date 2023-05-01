package com.example.bookgroundmusic

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookgroundmusic.databinding.ActivityMainBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    // 음악 재생 판별용
    var isPaused = true
    val player = MediaPlayer()
    val storage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //permissionCheck()
        startMainService()
        //initializeView()
        setListener()
        checkRemainTime()

        // 아래 코드처럼 모드나 음악 설정 시 Textview 클릭 시 글자색 바뀌는 걸로 하고, 실질적으로 서버에는 boolean 값 넘겨주면 될 것 같음

//        val mode_1 = findViewById<TextView>(R.id.mode_1)
//        var only_bgm = false
//        mode_1.setOnClickListener(View.OnClickListener {
//            if (!only_bgm) {
//                mode_1.setTextColor(Color.parseColor("#FF730D"))
//                only_bgm = true
//            } else {
//                mode_1.setTextColor(Color.parseColor("#000000"))
//                only_bgm = false
//            }
//        })


    }

//    // Root : 슈퍼유저 권한 주기
//    var p: Process? = null
//
//    init {
//        var p = java.lang.Runtime.getRuntime().exec("su")
//    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MediaProjectionController.mediaScreenCapture -> {
                MediaProjectionController.getMediaProjectionCapture(this, resultCode, data)
            }
//            MediaProjectionController.mediaScreenRecord -> {
//                MediaProjectionController.getMediaProjectionRecord(this, resultCode, data)
//            }
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


    // 음악 재생, 일시 정지
    fun musicControl() {
        player.setAudioStreamType(AudioManager.STREAM_MUSIC)

        // 재생 안되고 있는 상태 => play 시키기
        if (isPaused) {
            try {
                storage.reference.child("감성음악/슬픔/050.mp3").downloadUrl.addOnSuccessListener {
                    player.setDataSource(it.toString())
                    player.prepare()
                    player.start()
                }
            } catch (e: Exception) {
                // TODO: handle exception
            }

            isPaused = false
            Toast.makeText(this, "재생 시작함", Toast.LENGTH_LONG).show()
           // binding.btnPlay.setImageResource(R.drawable.ic_stop)
        }

        // 재생 중인 상태 => pause 시키기
        else if (!isPaused) {
            player.pause()
            isPaused = true
            Toast.makeText(this, "일시정지함", Toast.LENGTH_LONG).show()
            //binding.btnPlay.setImageResource(R.drawable.ic_play)
        }
    }


    //
    // Resume 구현해야함
    //


    // 일정 시간 간격 연속 스크린샷
    private fun screenshotSeries() {
        // OCR (ML Kit)
        val options = KoreanTextRecognizerOptions.Builder().build()
        val recognizer = TextRecognition.getClient(options)

        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                var i = 1
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
                    }
                    .addOnFailureListener { e -> }
                    }

                    i++
                    handler.postDelayed(this, 10000)
                }
            }
        }, 10000)
    }


    private fun checkRemainTime() {
        // 재생 중일 동안 남는 시간 계산 -> ??초일 때 연속 스크린샷
        while (player != null && player.isPlaying) {
            var total_duration = player.duration
            var remainingTime = total_duration - player.currentPosition

            // 남은 시간이 ??초일 때 스크린샷
            if (remainingTime == 15000) {
                Toast.makeText(this, "재생시간 15초 남아 스크린샷 시작", Toast.LENGTH_LONG).show()
                screenshotSeries()
            }
        }
    }

    private fun setListener() {
        // 1. on 버튼 클릭시
        binding.btnOn.setOnClickListener {

//            // 최상위 패키지 이름 따오기..?
//            val activityManager = (this.getSystemService(ACTIVITY_SERVICE) as ActivityManager)!!
//            val tasks = activityManager!!.appTasks
//            val topClassName = tasks[0].taskInfo.topActivity!!.className
//
//            Log.d("WJ", topClassName)


//            Toast.makeText(this, "10초 후 캡처 시작", Toast.LENGTH_LONG).show()
//
            screenshotSeries()

            }


        // 2. 사용 설명서 화면으로 이동
        binding.buttonGuide.setOnClickListener {
            val intent = Intent(this, GuideActivity::class.java)
            startActivity(intent)
        }


        // 3. 음악 재생
        binding.btnPlay.setOnClickListener() {
            musicControl()

        }

    }


}