package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.ArchiveDiaryDetailUiBinder
import com.example.guru2_android_team04_android.ui.bind.BottomNavBinder

// ArchiveDiaryDetailActivity : 마음 카드 보관함/캘린더/리스트에서 선택한 일기 1개의 상세 화면 Activity
// 용도:
// - entryId(일기 PK)를 전달받아 상세 화면을 보여준다.
// - 화면 구성은 XML(activity_archive_diary_detail.xml)로 하고, 데이터 조회/바인딩/클릭 이벤트 연결은 ArchiveDiaryDetailUiBinder에 위임한다.
// 설계:
// - Activity는 화면 생명주기/화면 시작만 담당하고, 데이터 로딩 + UI 채우기 책임은 Binder로 분리해서 코드 가독성과 테스트/유지보수 높임
class ArchiveDiaryDetailActivity : AppCompatActivity() {

    // appService : API/DB/비즈니스 로직 접근 창구(MyApp에 보관)
    // - 상세 화면에서 마음 카드 조회, 즐겨찾기 토글, 이미지 저장 등에 사용된다.
    private val appService by lazy { (application as MyApp).appService }

    // binder : activity_archive_diary_detail.xml <-> 데이터(AppService/DB) 연결 전담 객체
    private lateinit var binder: ArchiveDiaryDetailUiBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // activity_archive_diary_detail.xml을 화면에 부착한다.
        setContentView(R.layout.activity_archive_diary_detail)

        // 하단 네비게이션을 현재 흐름(캘린더/보관 관련) 탭으로 활성화한다.
        // 다른 화면으로 이동해도 동일한 네비 UX를 유지하기 위함이다.
        BottomNavBinder.bind(this, R.id.navigation_calendar)

        // entryId : 상세로 볼 일기의 PK
        // - 이전 화면에서 putExtra("entryId", ...)로 전달된다.
        // - 기본값 -1L은 "전달 실패"를 의미하고, Binder에서 예외처리한다.
        val entryId = intent.getLongExtra("entryId", -1L)

        // Binder 생성 및 바인딩 시작
        // - 실제 화면 데이터 채우기/버튼 이벤트 연결은 Binder가 처리한다.
        binder = ArchiveDiaryDetailUiBinder(this, appService)
        binder.bind(entryId)
    }
}
