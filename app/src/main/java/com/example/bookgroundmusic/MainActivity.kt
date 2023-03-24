package com.example.bookgroundmusic

import android.R
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookgroundmusic.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import org.json.JSONObject
import java.io.*
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import kotlin.random.Random
import java.io.FileInputStream
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap
import kotlin.streams.toList


class MainActivity : AppCompatActivity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

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

    // 음악 재생하도록
    private fun playMusic() {
        Toast.makeText(this, "버튼 클릭됨", Toast.LENGTH_LONG).show()

        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.getReference()

        val pathReference = storageReference.child("감성음악/기쁨/001.mp3")


        // test
        val mediaPlayer : MediaPlayer? = MediaPlayer().apply {
            setDataSource("backgroundmusic-949f0.appspot.com/감성음악/기쁨/008.mp3")
            prepare()
            start()
        }
    }


    private fun setListener() {
        // 1. on 버튼 클릭시
        binding.btnOn.setOnClickListener {
            Toast.makeText(this, "5초 후 캡처 시작", Toast.LENGTH_LONG).show()

            try {
                TimeUnit.SECONDS.sleep(5)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            // 백그라운드 캡처
            MediaProjectionController.screenCapture(this@MainActivity) { bitmap ->
                // TODO : You can use the captured image (bitmap)

                // OCR (ML Kit)
                val options = KoreanTextRecognizerOptions.Builder().build()
                val recognizer = TextRecognition.getClient(options)
                val image = InputImage.fromBitmap(bitmap, 0)

                val result = recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // string to txt file
                        val fileName = "text.txt"

                        var outputFile: FileOutputStream = openFileOutput(fileName, MODE_PRIVATE)
                        outputFile.write(visionText.text.toByteArray())
                        outputFile.close()
                    }
                    .addOnFailureListener { e -> }

//
//                // 보영 코드
//                // firebase 연동 관련 코드
//                val serviceAccount =
//                    FileInputStream("app/src/main/assets/backgroundmusic-949f0-firebase-adminsdk-1xdu2-d57d4620e7.json")
//
//                val option: FirebaseOptions = FirebaseOptions.Builder()
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                    .setDatabaseUrl("https://backgroundmusic-949f0-default-rtdb.firebaseio.com")
//                    .setStorageBucket("backgroundmusic-949f0.appspot.com")
//                    .build()
//
//                FirebaseApp.initializeApp(option)
//
//                //val database: DatabaseReference
//                val bucket = FirebaseStorage.getInstance()
//
//
//                val database = FirebaseDatabase.getInstance().getReference()
//
//
//
//                fun findEmotion(): Query {
//                    val prediction = "슬픔" // 이것은 nlp모델의 결과로 얻은(predict) 값이다.
//                    val mode = "감성음악/"
//                    val modePath = "$mode$prediction"
//
//                    // Get a reference to the Firebase Realtime Database node for 감성음악/슬픔
//                    val emotionalRef = database.ref
//
//                    val emotionalQuery = emotionalRef.orderByKey().limitToFirst(2) // 노래가져오는 갯수
////                    val emotionalData = emotionalQuery.
////                    val playlistEmo = emotionalData.values().shuffled().take()
//
//                    return emotionalQuery
//
//                }


//                MainApplication.updateNotification(this, "스크린샷")
//                Toast.makeText(this, "스크린캡처 테스트", Toast.LENGTH_SHORT).show()

            }

            }

        // 2. 사용 설명서 화면으로 이동
        binding.buttonGuide.setOnClickListener {
            val intent = Intent(this, GuideActivity::class.java)
            startActivity(intent)
        }

        // 3. 음악 재생
        binding.btnPlay.setOnClickListener() {
            playMusic()
        }

    }
}