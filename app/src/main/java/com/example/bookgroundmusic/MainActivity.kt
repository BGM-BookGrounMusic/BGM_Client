package com.example.bookgroundmusic

import android.R
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookgroundmusic.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    var dir: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        // firebase 연동 관련 코드
//        val serviceAccount = FileInputStream("app/src/main/assets/backgroundmusic-949f0-firebase-adminsdk-1xdu2-d57d4620e7.json")
//
//        val options: FirebaseOptions = FirebaseOptions.Builder()
//            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//            .setDatabaseUrl("https://backgroundmusic-949f0-default-rtdb.firebaseio.com")
//            .build()
//
//        FirebaseApp.initializeApp(options)

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

    // Root : 슈퍼유저 권한 주기
    var p: Process? = null
    init {
        var p = java.lang.Runtime.getRuntime().exec("su")
    }

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


    private fun setListener() {
        // 1. on 버튼 클릭시
        btn_on.setOnClickListener {
            Toast.makeText(this, "5초 후 캡처 시작", Toast.LENGTH_LONG).show()

            try {
                TimeUnit.SECONDS.sleep(5)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }





//                MainApplication.updateNotification(this, "스크린샷")
//                Toast.makeText(this, "스크린캡처 테스트", Toast.LENGTH_SHORT).show()



        }

        // 2. 사용 설명서 화면으로 이동
        button_guide.setOnClickListener {
            val intent = Intent(this, GuideActivity::class.java)
            startActivity(intent)
        }
    }

}