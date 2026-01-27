package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.DiaryCalendarUiBinder
import com.example.guru2_android_team04_android.util.DateUtil

// DiaryCalendarActivity : 감정 캘린더 화면 Activity
// 용도:
// - 사용자가 작성한 일기를 달력(그리드) 형태로 한눈에 보여준다.
// - 월 이동(이전/다음), 탭 전환(요약/캘린더/리스트), 월 스티커, 월간 감정 파이차트를 제공한다.
// 설계:
// - 화면(View)과 데이터(AppService) 연결은 DiaryCalendarUiBinder가 담당한다.
// - Activity는 "화면 시작 + 파라미터 준비 + binder 호출"만 담당해서 역할을 단순화한다.
class DiaryCalendarActivity : AppCompatActivity() {

    // appService : 사용자 프로필/일기 데이터 조회 등 앱의 비즈니스 로직 레이어
    private val appService by lazy { (application as MyApp).appService }

    // binder : activity_diary_calendar.xml의 뷰를 찾고 이벤트를 연결하고, 데이터를 화면에 바인딩하는 전담 클래스
    private lateinit var binder: DiaryCalendarUiBinder

    // yearMonth : 현재 보고 있는 월(형식: "yyyy-MM")
    // - 다른 화면에서 넘어올 때 Intent로 전달받거나, 없으면 현재 월로 초기화한다.
    private var yearMonth: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_calendar)

        // 하단 네비게이션 바를 캘린더 탭 상태로 바인딩한다.
        // - 사용자가 다른 탭을 누르면 해당 화면으로 이동하게 된다.
        com.example.guru2_android_team04_android.ui.bind.BottomNavBinder.bind(
            this, R.id.navigation_calendar
        )

        // 화면 진입 시 표시할 월 결정
        // - MonthlySummaryActivity/DiaryListActivity에서 넘어오면 yearMonth를 전달해 같은 달을 유지한다.
        // - 전달값이 없으면 DateUtil.thisMonthYm()로 현재 월("yyyy-MM")을 사용한다.
        yearMonth = intent.getStringExtra("yearMonth") ?: DateUtil.thisMonthYm()

        // UI 바인딩 시작
        // - binder가 month 헤더/탭/그리드/파이차트 등을 설정하고, AppService를 통해 해당 월의 일기 목록을 조회한 뒤 화면에 반영한다.
        binder = DiaryCalendarUiBinder(this, appService)
        binder.bind(yearMonth)
    }
}
