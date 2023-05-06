package com.example.bookgroundmusic

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookgroundmusic.databinding.ActivityMainBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.FileOutputStream
import java.text.DecimalFormat


class MainActivity : AppCompatActivity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    // 음악 재생 판별용
    var isPaused = true
    val player = MediaPlayer()
    val storage = FirebaseStorage.getInstance()

    private val handler = Handler(Looper.getMainLooper())

    private val checkRemainTimeRunnable = object : Runnable {
        override fun run() {
            val total_duration = player.duration
            val remainingTime = total_duration - player.currentPosition
            val remainingSecs = remainingTime / 1000
            val df = DecimalFormat("#")
            val formattedRemainingTime = df.format(remainingSecs)

            // 남은 시간이 15초일 때 스크린샷
            if (remainingSecs == 15) {
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
            //binding.btnPlay.setImageResource(R.drawable.ic_play)
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
                    }
                    .addOnFailureListener { e -> }
                    }

                    i++
                    handler.postDelayed(this, intervalMillis.toLong())
                }
            }
        }, 10000)
    }


    private fun setListener() {
        // 1. on 버튼 클릭시
        binding.btnOn.setOnClickListener {
            // 처음이면 중립음악 먼저 틀고 분석 시작 하도록 이후 구현

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