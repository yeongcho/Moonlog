package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.MyPageUiBinder

// MyPageActivity : 마이페이지 화면 Activity
// 용도:
// - 사용자 프로필/업적/기록/계정 메뉴를 보여주는 화면(activity_mypage.xml)을 표시한다.
// - 로그인/게스트 상태에 따라 UI 구성을 다르게 보여준다(회원 정보 영역/버튼 노출 여부).
// - 실제 데이터 로딩 및 버튼 이벤트 연결은 MyPageUiBinder에 위임한다.
// 동작 흐름:
// 1) 레이아웃을 세팅한다.
// 2) 하단 네비게이션을 마이페이지 탭으로 바인딩한다.
// 3) binder.bind()로 화면 데이터(프로필/뱃지/버튼 노출)를 세팅한다.
// 4) onResume에서 다시 bind()하여, 다른 화면(뱃지/프로필 편집)에서 돌아왔을 때 최신 상태를 반영한다.
// 설계:
// - Activity는 생명주기와 화면 시작만 담당한다.
// - 화면에 필요한 데이터 조회/조건 분기/클릭 이벤트 연결은 Binder로 분리해 가독성과 유지보수를 높인다.
class MyPageActivity : AppCompatActivity() {

    // appService : 로그인/프로필 조회/로그아웃/회원탈퇴 등 계정 관련 비즈니스 로직을 제공하는 서비스
    private val appService by lazy { (application as MyApp).appService }

    // binder : activity_mypage.xml <-> 데이터(AppService) 연결을 담당하는 UI 바인딩 객체
    private lateinit var binder: MyPageUiBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 마이페이지 레이아웃(activity_mypage.xml)을 화면에 부착한다.
        setContentView(R.layout.activity_mypage)

        // 하단 네비게이션에서 마이페이지 탭을 활성화한다.
        com.example.guru2_android_team04_android.ui.bind.BottomNavBinder.bind(
            this, R.id.navigation_mypage
        )

        // Binder 생성 후 바인딩 실행
        // - UI 요소 findViewById, 데이터 조회, 로그인/게스트 분기, 클릭 이벤트 연결을 한 번에 수행한다.
        binder = MyPageUiBinder(this, appService)
        binder.bind()
    }

    override fun onResume() {
        super.onResume()
        // 다른 화면(뱃지 변경/프로필 편집)에서 돌아온 경우 최신 정보로 다시 갱신한다.
        binder.bind()
    }
}
