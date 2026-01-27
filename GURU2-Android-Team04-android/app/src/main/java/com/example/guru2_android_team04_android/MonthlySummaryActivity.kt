package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.BottomNavBinder
import com.example.guru2_android_team04_android.ui.bind.MonthlySummaryUiBinder
import com.example.guru2_android_team04_android.util.DateUtil

// MonthlySummaryActivity : 월간 요약 화면 Activity
// 용도:
// - 선택된 월(yearMonth)의 월간 요약 데이터를 보여준다.
// - 탭(요약/캘린더/리스트)과 월 이동(이전/다음) UX의 시작점이다.
// 동작 흐름:
// 1) yearMonth를 Intent에서 받는다.
// 2) 전달이 없으면 기본값으로 "지난 달"을 선택한다.
// 3) 실제 화면 데이터 로딩/표시는 MonthlySummaryUiBinder에 위임한다.
// 설계:
// - Activity는 화면 시작과 파라미터 결정만 담당한다.
// - DB 조회/캐시/생성 로직은 Binder로 분리해 책임을 명확히 한다.
class MonthlySummaryActivity : AppCompatActivity() {

    // appService : 월간 요약 조회/생성(ensureLastMonthMonthlySummary) 등에 사용되는 서비스 계층
    private val appService by lazy { (application as MyApp).appService }

    // binder : activity_monthly_summary.xml <-> 데이터(AppService/DB 캐시) 연결 전담 객체
    private lateinit var binder: MonthlySummaryUiBinder

    // yearMonth : 현재 화면에서 보여줄 월(yyyy-MM)
    private var yearMonth: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_summary)

        // 하단 네비게이션을 캘린더 탭 기준으로 바인딩한다.
        BottomNavBinder.bind(this, R.id.navigation_calendar)

        // yearMonth : 이전 화면에서 전달받은 월(yyyy-MM)
        // 예외처리) 전달이 없으면 지난 달로 대체한다.
        yearMonth = intent.getStringExtra("yearMonth") ?: DateUtil.previousMonthYm()

        // 월간 요약 화면 데이터 바인딩 시작
        binder = MonthlySummaryUiBinder(this, appService)
        binder.bind(yearMonth)
    }
}
