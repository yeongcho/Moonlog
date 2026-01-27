package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.MindCardArchiveUiBinder

// MindCardArchiveActivity : 마음 카드 보관함 화면 Activity
// 용도:
// - 즐겨찾기(하트)한 일기의 "마음 카드"들을 리스트로 모아 보여준다.
// - 화면 로직은 UiBinder(MindCardArchiveUiBinder)에 위임하고, Activity는 화면 생명주기만 관리한다.
// 동작 흐름:
// 1) onCreate에서 레이아웃(activity_mindcard_archive.xml)을 세팅한다.
// 2) UiBinder를 생성하고 bind()를 호출하여 RecyclerView 세팅 + 데이터 로딩을 시작한다.
// 3) 상세 화면에서 돌아오거나 즐겨찾기 변경 후에는 갱신이 필요할 수 있어 onResume에서 재로딩 여지가 있다.
// 설계:
// - Activity는 화면 시작과 파라미터 결정만 담당한다.
// - DB 조회/캐시/생성 로직은 Binder로 분리해 책임을 명확히 한다.
class MindCardArchiveActivity : AppCompatActivity() {

    // appService : 앱 전역 서비스(AppService) 접근
    // - MyApp(Application)에서 생성한 단일 인스턴스를 재사용한다.
    // - 즐겨찾기 조회/해제, 프로필 조회 등에 사용된다.
    private val appService by lazy { (application as MyApp).appService }

    // binder : XML(View)과 AppService(데이터/로직)를 연결하는 전담 클래스
    // - Activity가 비대해지지 않도록 UI 바인딩/이벤트/데이터 로딩을 한 곳에 모아둔다.
    private lateinit var binder: MindCardArchiveUiBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mindcard_archive)

        // MindCardArchiveUiBinder 생성 및 최초 바인딩
        // - RecyclerView/Adapter 초기화
        // - 로그인 여부 확인(게스트면 종료)
        // - 마음 카드 목록 로드(refresh) 실행
        binder = MindCardArchiveUiBinder(this, appService)
        binder.bind()
    }

    // 즐겨찾기 해제/상세 보고 돌아왔을 때 갱신
    override fun onResume() {
        super.onResume()
    }
}
