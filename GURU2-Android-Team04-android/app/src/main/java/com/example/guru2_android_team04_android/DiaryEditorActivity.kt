package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.DiaryEditorUiBinder

// DiaryEditorActivity : 오늘 일기 작성/수정 화면 Activity
// 용도:
// - 오늘 날짜 표시 + 감정 선택 + 제목/본문 입력 폼을 제공한다.
// - 일기 작성 완료하기 버튼을 누르면 저장 후 오늘 일기 보기 화면으로 이동한다.
// - entryId가 전달되면(수정 모드) 기존 일기 내용을 불러와 편집할 수 있다.
// 설계:
// - UI 이벤트 처리/데이터 저장 로직을 Activity에 몰아넣지 않고, DiaryEditorUiBinder로 분리해 화면-서비스 연동을 담당하게 한다.
// - Activity는 "레이아웃 세팅 + 바인더 연결 + (옵션) 수정 대상 entryId 전달"만 수행한다.
class DiaryEditorActivity : AppCompatActivity() {

    // appService : 앱의 비즈니스 로직 진입점(Service)
    // - 현재 ownerId 조회/비회원 세션 시작/일기 저장/분석 프리뷰 준비 등을 담당한다.
    private val appService by lazy { (application as MyApp).appService }

    // onCreate : 작성/수정 화면 초기화
    // 동작 흐름:
    // 1) activity_diary_editor 레이아웃을 표시한다.
    // 2) 하단 네비게이션을 diary 탭으로 바인딩한다.
    // 3) Intent로 entryId가 넘어오면 수정 모드로 판단하고 바인더에 전달한다.
    // 4) DiaryEditorUiBinder가 입력 폼 초기화/저장 처리/화면 이동까지 담당한다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_editor)

        // 하단 네비게이션 바 연결(현재 탭: diary)
        com.example.guru2_android_team04_android.ui.bind.BottomNavBinder.bind(
            this, R.id.navigation_diary
        )

        // 수정 모드 진입 여부 판단:
        // - TodayDiaryDetailActivity 등에서 entryId를 전달하면 수정 화면으로 동작한다.
        // 예외처리) entryId가 없거나(-1L) 0 이하이면 "신규 작성"으로 간주한다.
        val entryId = intent.getLongExtra("entryId", -1L).takeIf { it > 0L }

        // UI 바인딩/저장/수정 로직은 바인더가 전담한다.
        DiaryEditorUiBinder(this, appService).bind(entryId)
    }
}
