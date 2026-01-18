package com.example.guru2_android_team04_android.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.MainActivity
import com.example.guru2_android_team04_android.R

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // XML에서 View 찾기
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnSignup = findViewById<Button>(R.id.btn_signup)
        val tvGuestStart = findViewById<TextView>(R.id.tv_guest_start)

        // 로그인 버튼 클릭
        btnLogin.setOnClickListener {
            // TODO: LoginActivity 완성 후 연결
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 회원가입 버튼 클릭
        btnSignup.setOnClickListener {
            // TODO: SignUpActivity 완성 후 연결
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 비회원으로 시작하기 클릭
        tvGuestStart.setOnClickListener {
            // TODO: GuestStartActivity 완성 후 연결
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}