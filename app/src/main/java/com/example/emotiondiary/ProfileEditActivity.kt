package com.example.emotiondiary

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.WindowCompat

class ProfileEditActivity : AppCompatActivity() {

    // 현재 선택된 배지 ID를 저장할 변수 (기본값)
    private var selectedBadgeResId: Int = R.drawable.ic_badge_emotion_log

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        window.statusBarColor = android.graphics.Color.parseColor("#CEC8BB")
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnSave = findViewById<AppCompatButton>(R.id.btn_save_profile)
        val etNickname = findViewById<EditText>(R.id.et_nickname)
        val etBio = findViewById<EditText>(R.id.et_bio)
        val ivBadgeFrame = findViewById<ImageView>(R.id.iv_badge_frame)

        // 저장된 데이터 불러와서 화면에 세팅
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedBadge = prefs.getInt("selected_badge", R.drawable.ic_badge_emotion_log)
        ivBadgeFrame.setImageResource(savedBadge)
        selectedBadgeResId = savedBadge

        etNickname.setText(prefs.getString("nickname", "닉네임"))
        etBio.setText(prefs.getString("bio", "감정 로그 수집가"))

        btnBack.setOnClickListener { finish() }

        // 배지 클릭 리스너 정의
        val badgeIcons = mapOf(
            findViewById<ImageView>(R.id.iv_badge_1) to R.drawable.ic_badge_emotion_log,
            findViewById<ImageView>(R.id.iv_badge_2) to R.drawable.ic_badge_wine,
            findViewById<ImageView>(R.id.iv_badge_3) to R.drawable.ic_badge_traveler,
            findViewById<ImageView>(R.id.iv_badge_4) to R.drawable.ic_badge_start,
            findViewById<ImageView>(R.id.iv_badge_5) to R.drawable.ic_badge_month,
            findViewById<ImageView>(R.id.iv_badge_6) to R.drawable.ic_badge_three_days
        )

        badgeIcons.forEach { (view, drawableId) ->
            view.setOnClickListener {
                // 화면 업데이트
                ivBadgeFrame.setImageResource(drawableId)
                // 선택된 ID 변수에 저장
                selectedBadgeResId = drawableId
                Toast.makeText(this, "배지가 선택되었습니다!", Toast.LENGTH_SHORT).show()
            }
        }

        // 저장 버튼 클릭 시 -> SharedPreferences에 저장
        btnSave.setOnClickListener {
            val editor = prefs.edit()
            editor.putString("nickname", etNickname.text.toString())
            editor.putString("bio", etBio.text.toString())
            editor.putInt("selected_badge", selectedBadgeResId) // 배지 ID 저장
            editor.apply()

            Toast.makeText(this, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}