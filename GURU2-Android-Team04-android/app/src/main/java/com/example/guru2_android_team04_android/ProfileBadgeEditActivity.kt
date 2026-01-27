package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.ProfileBadgeEditUiBinder

// ProfileBadgeEditActivity : 대표 배지 선택/적용 화면 Activity
// 용도:
// - 사용자가 획득한 배지 중 하나를 선택해 대표 배지로 저장하는 화면(activity_profile_badge_edit.xml)을 제공한다.
// - 배지 목록(그리드) 선택 상태(체크 표시)와 프로필 카드 미리보기(프레임)를 함께 보여준다.
// 동작 흐름:
// 1) 화면 로딩 시 binder.bind()로 프로필/배지 상태를 조회하고 UI를 구성한다.
// 2) 사용자가 배지를 클릭하면 선택 상태(체크) 및 프레임 미리보기가 바뀐다.
// 3) 저장 버튼 클릭 시 선택한 배지를 서버/로컬에 저장(selectBadge)하고 마이페이지로 이동한다.
// 설계:
// - Activity는 레이아웃 세팅과 Binder 연결만 담당한다.
// - 데이터 조회/선택 로직/저장/예외처리는 ProfileBadgeEditUiBinder가 담당한다.
class ProfileBadgeEditActivity : AppCompatActivity() {

    // appService : 프로필 조회 및 대표 배지 저장(selectBadge) 등 계정 관련 비즈니스 로직 제공
    private val appService by lazy { (application as MyApp).appService }

    // binder : activity_profile_badge_edit.xml ↔ AppService 연결 및 배지 선택 로직 담당
    private lateinit var binder: ProfileBadgeEditUiBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 배지 편집 레이아웃(activity_profile_badge_edit.xml)을 화면에 부착한다.
        setContentView(R.layout.activity_profile_badge_edit)

        // Binder 생성 후 바인딩 실행
        // - 프로필 카드 표시 + 배지 그리드 구성 + 저장 버튼 로직 연결
        binder = ProfileBadgeEditUiBinder(this, appService)
        binder.bind()
    }

    override fun onResume() {
        super.onResume()
        // 화면 복귀 시 최신 상태로 다시 바인딩한다.
        binder.bind()
    }
}
