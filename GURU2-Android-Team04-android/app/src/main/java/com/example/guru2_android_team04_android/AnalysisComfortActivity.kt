package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.core.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// AnalysisComfortActivity : 상세 분석 화면(2/3) - Gemini 위로/격려 메시지 화면 Activity
// 용도:
// - 일기 내용을 기반으로 Gemini가 생성한 위로/격려 메시지를 보여준다.
// - 핵심 키워드를 해시태그 형태로 함께 보여준다.
// - 하단 Tap 버튼 클릭 시 다음 단계(실천안 화면)로 이동한다.
// 동작 흐름:
// 1) Intent로 전달받은 entryId를 사용해 상세 분석 데이터(본문/해시태그)를 조회한다.
// 2) 성공 시 tv_analysis_content(분석 본문) / tv_tags(해시태그)에 바인딩한다.
// 3) 실패 시 에러 메시지를 토스트로 보여주고 화면을 종료한다.
// 설계:
// - 네트워크/DB 접근이 포함될 수 있으므로 IO 스레드에서 조회한다.
// - UI 반영/토스트/화면 종료는 Main 스레드에서 처리한다.
class AnalysisComfortActivity : AppCompatActivity() {

    // appService : 분석 결과 조회/내보내기 등 앱 기능을 담당하는 서비스 레이어
    // - getMindCardDetailByEntryIdSafe()를 통해 MindCardDetail(상세 분석 결과)을 안전하게 가져온다.
    private val appService by lazy { (application as MyApp).appService }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_comfort)

        // entryId : 분석 대상 일기 PK
        // - 이전 화면(AnalysisDiaryActivity)에서 전달받는다.
        val entryId = intent.getLongExtra("entryId", -1L)

        // tv_analysis_content : Gemini가 생성한 위로/격려 "긴 본문" 표시 영역(스크롤 가능)
        val tvContent = findViewById<TextView>(R.id.tv_analysis_content)

        // tv_tags : 분석 핵심 요약 태그(해시태그) 표시 영역
        val tvTags = findViewById<TextView>(R.id.tv_tags)

        // 상세 분석 데이터 조회
        lifecycleScope.launch(Dispatchers.IO) {
            val r = appService.getMindCardDetailByEntryIdSafe(entryId)

            withContext(Dispatchers.Main) {
                when (r) {
                    is AppResult.Success -> {
                        // 분석 본문(위로/격려 메시지)
                        tvContent.text = r.data.fullText

                        // 해시태그: ["시험기간", "마음챙김"] -> "#시험기간 #마음챙김"
                        tvTags.text = r.data.hashtags.map { it.trim().removePrefix("#") }
                            .filter { it.isNotBlank() }.joinToString(" ") { "#$it" }
                    }

                    is AppResult.Failure -> {
                        // 예외처리) 분석 결과를 가져오지 못하면 이 화면은 의미가 없으므로 에러 메시지를 안내하고 종료한다.
                        Toast.makeText(
                            this@AnalysisComfortActivity, r.error.userMessage, Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }

        // 하단 Tap 버튼
        // - 다음 단계(AnalysisActionsActivity)로 이동하며 entryId를 그대로 전달한다.
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnTapContinue).setOnClickListener {
            startActivity(Intent(this, AnalysisActionsActivity::class.java).apply {
                putExtra("entryId", entryId)
            })
        }
    }
}
