package com.example.bookgroundmusic

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
}