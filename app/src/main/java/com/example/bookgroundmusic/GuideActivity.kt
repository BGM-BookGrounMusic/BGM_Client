package com.example.bookgroundmusic

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bookgroundmusic.databinding.ActivityGuideBinding

// 사용 설명서 화면

class GuideActivity : AppCompatActivity() {
    private var mBinding: ActivityGuideBinding? = null
    private val binding get() = mBinding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 메인 화면으로 이동
        binding.btnBacktomain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
}