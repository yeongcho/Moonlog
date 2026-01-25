package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.core.AppResult
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalysisActionsActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_actions)

        val entryId = intent.getLongExtra("entryId", -1L)

        val tv1 = findViewById<TextView>(R.id.tv_analysis_content1)
        val tv2 = findViewById<TextView>(R.id.tv_analysis_content2)
        val tv3 = findViewById<TextView>(R.id.tv_analysis_content3)
        val tvSummary = findViewById<TextView>(R.id.tv_tags)

        lifecycleScope.launch(Dispatchers.IO) {
            val r = appService.getMindCardDetailByEntryIdSafe(entryId)
            withContext(Dispatchers.Main) {
                when (r) {
                    is AppResult.Success -> {
                        val actions = r.data.missions
                        tv1.text = "1. ${actions.getOrNull(0).orEmpty()}"
                        tv2.text = "2. ${actions.getOrNull(1).orEmpty()}"
                        tv3.text = "3. ${actions.getOrNull(2).orEmpty()}"
                        tvSummary.text = r.data.missionSummary
                    }
                    is AppResult.Failure -> {
                        Toast.makeText(
                            this@AnalysisActionsActivity,
                            r.error.userMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }

        // 이미지 카드로 소장하기(갤러리 저장)
        findViewById<MaterialButton>(R.id.btn_save_card).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val r = appService.exportMindCardToGallerySafe(this@AnalysisActionsActivity, entryId)
                withContext(Dispatchers.Main) {
                    when (r) {
                        is AppResult.Success ->
                            Toast.makeText(this@AnalysisActionsActivity, "갤러리에 저장했어요!", Toast.LENGTH_SHORT).show()
                        is AppResult.Failure ->
                            Toast.makeText(this@AnalysisActionsActivity, r.error.userMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 로그인/일기 모아보기 버튼
        findViewById<MaterialButton>(R.id.btn_login_diary).setOnClickListener {
            val owner = appService.currentOwnerIdOrNull().orEmpty()
            if (owner.startsWith("ANON_") || owner.isBlank()) {
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, DiaryCalendarActivity::class.java))
            }
        }

        // 홈으로
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnTapContinue).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}
