package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.TodayDiaryDetailUiBinder

// TodayDiaryDetailActivity : 오늘의 일기 보기 화면 Activity
// 용도:
// - 오늘 작성된 일기(날짜/감정/제목/본문)를 보여준다.
// - 수정/삭제/즐겨찾기(하트) 기능을 제공한다.
// - 오늘의 마음 카드(위로 문구 2줄 + 미션 1개)를 함께 보여준다.
// - 상세 분석 보러가기 버튼으로 분석 시작 화면으로 이동한다.
// 설계:
// - 화면의 실제 UI 바인딩/이벤트 처리 로직은 TodayDiaryDetailUiBinder로 분리한다.
// - Activity는 레이아웃 세팅 + bottom nav 바인딩 + entryId 검증 + 바인더 호출만 담당한다.
class TodayDiaryDetailActivity : AppCompatActivity() {

    // appService : 앱의 비즈니스 로직(Service) 진입점
    // - 프로필 조회, 마음 카드 프리뷰 조회, 즐겨찾기/삭제 처리 등에 사용된다.
    private val appService by lazy { (application as MyApp).appService }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_diary_detail)

        // 하단 네비게이션 바 연결(현재 탭: diary)
        com.example.guru2_android_team04_android.ui.bind.BottomNavBinder.bind(
            this, R.id.navigation_diary
        )

        // entryId : 상세로 보여줄 일기 PK
        // - 이전 화면(Home/Editor 등)에서 putExtra("entryId", ...)로 전달받는다.
        val entryId = intent.getLongExtra("entryId", -1L)

        // 예외처리) entryId가 없거나 0 이하이면 보여줄 대상이 없으므로 작성 화면으로 이동시켜 "오늘 일기 작성" 흐름으로 복귀시킨다.
        if (entryId <= 0L) {
            startActivity(Intent(this, DiaryEditorActivity::class.java))
            finish()
            return
        }

        // UI 구성/데이터 로드/버튼 이벤트 처리는 바인더가 전담
        TodayDiaryDetailUiBinder(this, appService).bind(entryId)
    }
}
