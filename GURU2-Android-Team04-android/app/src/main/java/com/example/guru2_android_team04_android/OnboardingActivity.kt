package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

// OnboardingActivity : 앱 첫 진입 시 사용자에게 로그인 또는 비회원 시작 선택지를 제공하는 화면 Activity
// 용도:
// - 회원(로그인) 흐름과 비회원(익명 세션) 흐름을 분기하는 진입점 역할
// - 사용자가 어떤 방식으로 앱을 사용할지 결정하면, 다음 Activity로 이동한다.
// 설계:
// - 로그인 버튼: LoginActivity로 이동(인증 진행)
// - 비회원 시작 버튼: SessionManager 기반으로 익명 ownerId를 세팅한 뒤(StartAnonymousSession), 메인 시작 화면(StartActivity)로 이동한다.
// - 비회원 시작은 즉시 세션을 만들어 ownerId가 설정된 상태로 이후 화면들이 동작하도록 한다.
class OnboardingActivity : AppCompatActivity() {

    // onCreate : 화면이 처음 생성될 때 호출
    // 동작 흐름:
    // 1) 온보딩 레이아웃(activity_onboarding)을 표시한다.
    // 2) 로그인/비회원 시작 버튼 클릭 이벤트를 등록한다.
    // 3) 사용자의 선택에 따라 다음 화면으로 이동한다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // 로그인 버튼:
        // - 회원 로그인 화면(LoginActivity)으로 이동한다.
        // - 로그인 성공 시 AppService가 ownerId를 "USER_<id>" 형태로 세팅하게 된다.
        findViewById<Button>(R.id.btn_login).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 비회원 시작 버튼:
        // - 서버 없이 SQLite 기반 앱이므로, 비회원도 ownerId가 반드시 필요하다.
        // - startAnonymousSession()에서 SessionManager가 ANON_xxx를 생성/재사용하고 current ownerId로 저장한다.
        // - 세션이 준비된 상태로 StartActivity로 이동한다.
        findViewById<Button>(R.id.btn_guest_start).setOnClickListener {

            // 비회원 세션 시작 -> ownerId 세팅 완료
            // 예외처리) 이후 화면(일기 저장/조회/배지 등)은 ownerId를 기준으로 데이터를 구분하므로, StartActivity로 이동하기 전에 반드시 세션을 먼저 시작해야 한다.
            appService.startAnonymousSession()

            // 앱 메인 흐름 시작 화면으로 이동
            startActivity(Intent(this, StartActivity::class.java))

            // finish : 온보딩 화면을 백스택에서 제거
            // - 사용자가 뒤로가기를 눌러도 온보딩으로 돌아오지 않게 한다.
            finish()
        }
    }
}
