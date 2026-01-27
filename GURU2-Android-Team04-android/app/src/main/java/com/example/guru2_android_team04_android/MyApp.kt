package com.example.guru2_android_team04_android

import android.app.Application
import com.example.guru2_android_team04_android.llm.GeminiClient

// MyApp : 앱 전체에서 1번만 생성되는 Application 클래스
// 용도:
// - 앱 시작 시점에 전역에서 공유할 객체를 초기화한다.
// - 여기서는 AppService를 싱글톤처럼 만들어, Activity/Fragment 어디서든 동일한 서비스 인스턴스를 사용하게 한다.
// 동작 시점:
// - Android 시스템이 앱 프로세스를 만들고, 가장 먼저 Application.onCreate()를 호출한다.
// 설계:
// - appService를 MyApp이 소유하게 해서 앱 전역 상태를 한 곳에서 관리한다.
// - Activity는 화면/이벤트만 담당하고, 비즈니스 로직은 AppService로 위임한다.
class MyApp : Application() {

    // appService : 앱 기능(회원/일기/AI/보관함/배지 등)을 묶어 제공하는 서비스 계층
    // - lateinit : onCreate()에서 초기화하기 때문에 선언 시점에는 값을 넣지 않는다.
    // - private set : 외부에서 appService 참조는 가능하지만, 다른 인스턴스로 바꿔치기 못하게 막는다.
    //   (전역 서비스의 일관성을 지키기 위한 캡슐화)
    lateinit var appService: AppService
        private set

    override fun onCreate() {
        super.onCreate()

        // GeminiClient : 일기 내용을 AI로 분석하기 위한 네트워크 클라이언트
        // 용도:
        // - Gemini API(generateContent)를 호출해 분석/요약/마음카드 생성 등에 필요한 응답을 받는다.
        // 설계:
        // - apiKey는 BuildConfig에 넣어(gradle/build 설정) 코드에 하드코딩하지 않는다.
        // - endpointUrl은 특정 모델(gemini-2.0-flash)의 generateContent 엔드포인트를 사용한다.
        // 예외처리) apiKey가 비어있거나 잘못되면 실제 호출 시점에 HTTP 실패(401/403 등)가 발생할 수 있다.
        // - 이 앱은 호출부(AppService의 Safe 계열 메서드)가 실패를 AppError/AppResult로 변환해 UI에서 처리하도록 설계한다.
        val geminiClient = GeminiClient(
            apiKey = BuildConfig.GEMINI_API_KEY,
            endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        )

        // AppService 생성
        // - applicationContext를 주입하여 Activity 생명주기와 무관하게 안정적으로 동작하게 한다.
        // - geminiClient를 주입하여 네트워크 분석 기능까지 한 서비스에서 사용 가능하게 한다.
        appService = AppService(applicationContext, geminiClient)

        // 디버그 빌드에서만 테스트용 시드 데이터를 준비한다.
        // - 개발/데모 환경에서 앱 실행 직후 "사용자/과거 일기"가 자동으로 생기도록 해 테스트를 쉽게 한다.
        // 예외처리) SeedData 내부 로직이 실패하면 디버그에서만 문제로 드러나며,
        // - 릴리즈 빌드에서는 실행되지 않으므로 사용자 앱에는 영향을 주지 않는다.
        if (BuildConfig.DEBUG) {
            com.example.guru2_android_team04_android.debug.SeedData.ensureSeedUserAndPastDiaries(
                this
            )
        }
    }
}
