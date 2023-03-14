package com.example.bookgroundmusic

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    var dir: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            MediaProjectionController.mediaScreenRecord -> {
                MediaProjectionController.getMediaProjectionRecord(this, resultCode, data)
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


    private fun setListener() {
        // 1. on 버튼 클릭시
        btn_on.setOnClickListener {
            try {
                TimeUnit.SECONDS.sleep(5)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            MediaProjectionController.screenCapture(this) { bitmap ->
                // TODO : You can use the captured image (bitmap)

                var res = bitmaptoText(bitmap)
                // txtview_fortest.text = res (작동 테스트 완료)

                // string to txt file
                val fileName = "text.txt"

                var outputFile : FileOutputStream = openFileOutput(fileName, MODE_PRIVATE)
                outputFile.write(res.toByteArray())
                outputFile.close()

                MainApplication.updateNotification(this, "스크린샷")
                Toast.makeText(this, "스크린캡처 테스트", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 사용 설명서 화면으로 이동
        button_guide.setOnClickListener {
            val intent = Intent(this, GuideActivity::class.java)
            startActivity(intent)
        }
    }

    // Tesseract

    // OCR 인식 위한 파일 존재 확인 메소드
    fun checkFile(dir: File, lang: String) {
        try {
            if (!dir.exists() && dir.mkdirs()) {
                copyFiles(lang)
            }
            if (dir.exists()) {
                val dataFilePath: String = dir.toString() + "/tessdata/" + lang + ".traineddata"
                val dataFile = File(dataFilePath)
                if (!dataFile.exists()) {
                    copyFiles(lang)
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    // OCR 인식 위한 언어 파일 복사 메소드
    fun copyFiles(lang: String) {
        try {
            val filePath: String = dir + "/tessdata/" + lang + ".traineddata"
            val assetManager = assets
            val inputStream: InputStream = assetManager.open("tessdata/$lang.traineddata")
            val outputStream: OutputStream = FileOutputStream(filePath)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    // 비트맵에서 텍스트 추출
    fun bitmaptoText(bitmap: Bitmap) : String {
        var text = ""
        // 테서랙트
        try {
            dir = "$filesDir/tesseract/"
            checkFile(File(dir + "tessdata/"), "kor")
            checkFile(File(dir + "tessdata/"), "eng")
            var tessBaseAPI = TessBaseAPI()
            tessBaseAPI.init(dir, "kor")
            tessBaseAPI.setImage(bitmap)

            text = tessBaseAPI.utF8Text
            tessBaseAPI.end()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return text
    }
}