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

// AnalysisActionsActivity : 상세 분석 화면(3/3) - 오늘의 실천안 제공 화면 Activity
// 용도:
// - Gemini가 제안한 오늘의 실천안(미션) 1~3개를 보여준다.
// - 실천안을 아우르는 요약 메시지를 보여준다.
// - 기능 버튼:
//   1) 이미지 카드로 소장하기: 마음 카드를 이미지로 렌더링하여 갤러리에 저장한다.
//   2) 로그인 후 일기 모아보기 / 일기 모아보기:
//      - 비회원(ANON_)이면 로그인 화면으로 이동
//      - 회원이면 캘린더 화면으로 이동
// - 하단 Tap 버튼: 홈 화면으로 돌아간다.
// 설계:
// - 로그인 상태는 ownerId 문자열 규칙으로 판별한다. (ownerId가 비어있거나 "ANON_"으로 시작하면 비회원 세션으로 간주)
// - 분석 데이터 조회/갤러리 저장은 IO 스레드에서 실행한다.
// - UI 갱신/토스트/화면 이동은 Main 스레드에서 실행한다.
class AnalysisActionsActivity : AppCompatActivity() {

    // appService : 분석 상세 조회 / 갤러리 내보내기 / 세션(ownerId) 확인을 담당
    private val appService by lazy { (application as MyApp).appService }

    // btn_login_diary : 로그인 상태에 따라 버튼 문구/동작이 달라지므로 멤버로 들고 갱신한다.
    private lateinit var btnLoginDiary: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_actions)

        // entryId : 분석 대상 일기 PK
        val entryId = intent.getLongExtra("entryId", -1L)

        // tv_analysis_content1~3 : 실천안 1~3번 표시 TextView
        val tv1 = findViewById<TextView>(R.id.tv_analysis_content1)
        val tv2 = findViewById<TextView>(R.id.tv_analysis_content2)
        val tv3 = findViewById<TextView>(R.id.tv_analysis_content3)

        // tv_tags : 실천안 요약 메시지 영역으로 사용됨
        val tvSummary = findViewById<TextView>(R.id.tv_tags)

        // btn_login_diary : 로그인 후 일기 모아보기 / 일기 모아보기 버튼
        btnLoginDiary = findViewById(R.id.btn_login_diary)

        // 최초 진입 시, 현재 세션 상태에 맞춰 버튼 문구를 세팅한다.
        updateLoginDiaryButtonText()

        // 상세 분석 데이터 조회 → 실천안/요약 메시지 표시
        lifecycleScope.launch(Dispatchers.IO) {
            val r = appService.getMindCardDetailByEntryIdSafe(entryId)

            withContext(Dispatchers.Main) {
                when (r) {
                    is AppResult.Success -> {
                        // missions : Gemini가 추천한 행동 리스트(보통 1~3개)
                        val actions = r.data.missions

                        // 예외처리) missions가 3개 미만일 수 있으므로 getOrNull로 안전 접근한다.
                        tv1.text = "1. ${actions.getOrNull(0).orEmpty()}"
                        tv2.text = "2. ${actions.getOrNull(1).orEmpty()}"
                        tv3.text = "3. ${actions.getOrNull(2).orEmpty()}"

                        // missionSummary : 실천안을 아우르는 한 줄 요약 메시지
                        tvSummary.text = r.data.missionSummary
                    }

                    is AppResult.Failure -> {
                        // 예외처리) 분석 상세를 불러오지 못하면 실천안을 보여줄 수 없으므로 에러 안내 후 화면을 종료한다.
                        Toast.makeText(
                            this@AnalysisActionsActivity, r.error.userMessage, Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }

        // 이미지 카드로 소장하기(갤러리 저장)
        // - 화면에서 보이는 분석 카드를 Bitmap으로 렌더링해 기기 갤러리에 저장한다.
        findViewById<MaterialButton>(R.id.btn_save_card).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val r =
                    appService.exportMindCardToGallerySafe(this@AnalysisActionsActivity, entryId)

                withContext(Dispatchers.Main) {
                    when (r) {
                        is AppResult.Success -> Toast.makeText(
                            this@AnalysisActionsActivity, "갤러리에 저장했어요!", Toast.LENGTH_SHORT
                        ).show()

                        is AppResult.Failure -> Toast.makeText(
                            this@AnalysisActionsActivity, r.error.userMessage, Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // 로그인/일기 모아보기 버튼
        // - 비회원이면 로그인 화면으로 유도하고,
        // - 이미 로그인 상태면 일기 캘린더 화면으로 이동한다.
        btnLoginDiary.setOnClickListener {
            val owner = appService.currentOwnerIdOrNull().orEmpty()

            // 예외처리) ownerId가 비어있거나 "ANON_" 세션이면 회원 기능 접근이 불가하므로 로그인 화면으로 보낸다.
            if (owner.startsWith("ANON_") || owner.isBlank()) {
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, DiaryCalendarActivity::class.java))
            }
        }

        // 홈으로 이동 (하단 Tap 버튼)
        // - 분석 흐름을 끝내고 홈으로 복귀한다.
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnTapContinue).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // 로그인 화면으로 갔다가 돌아오면 세션(ownerId)이 바뀔 수 있으므로 버튼 문구를 다시 갱신한다.
        updateLoginDiaryButtonText()
    }

    // updateLoginDiaryButtonText : 현재 로그인 상태에 맞춰 btn_login_diary의 문구를 갱신한다.
    // 판별 규칙: ownerId가 존재하고 "ANON_"으로 시작하지 않으면 로그인 상태로 간주한다.
    private fun updateLoginDiaryButtonText() {
        val owner = appService.currentOwnerIdOrNull().orEmpty()
        val isLoggedIn = owner.isNotBlank() && !owner.startsWith("ANON_")

        btnLoginDiary.text = if (isLoggedIn) {
            "일기 모아보기"
        } else {
            "로그인 후 일기 모아보기"
        }
    }
}
