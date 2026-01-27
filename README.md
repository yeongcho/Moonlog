# Moonlog - 감정 일기 코칭 앱 🌙

> **AI 기반 감정 분석 및 일기 코칭 애플리케이션**  
> 하루의 감정을 기록하고, AI가 분석한 맞춤형 실천 행동을 제안받아보세요.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/minSdk-23-green)](https://developer.android.com/about/versions/marshmallow)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-brightgreen)](https://developer.android.com/about/versions)
[![JDK](https://img.shields.io/badge/JDK-11-blue)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Build-Gradle-02303A.svg?logo=gradle&logoColor=white)](https://gradle.org/)
[![AGP](https://img.shields.io/badge/AGP-8.12.3-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/build)
[![OkHttp](https://img.shields.io/badge/Network-OkHttp-000000.svg)](https://github.com/square/okhttp)
[![Navigation](https://img.shields.io/badge/Jetpack-Navigation-6F42C1.svg)](https://developer.android.com/guide/navigation)
[![Lifecycle](https://img.shields.io/badge/Jetpack-Lifecycle-6F42C1.svg)](https://developer.android.com/jetpack/androidx/releases/lifecycle)
[![ViewBinding](https://img.shields.io/badge/UI-ViewBinding-orange.svg)](https://developer.android.com/topic/libraries/view-binding)
[![MPAndroidChart](https://img.shields.io/badge/Chart-MPAndroidChart-blueviolet.svg)](https://github.com/PhilJay/MPAndroidChart)
[![Gemini](https://img.shields.io/badge/AI-Gemini_API-4285F4.svg?logo=google&logoColor=white)](https://ai.google.dev/)

---

##  프로젝트 소개

**Moonlog**는 사용자가 하루의 감정과 일기를 기록하면, AI(Gemini API)가 감정을 분석하고 오늘 실천 가능한 감정 완화 솔류션을 제안하는 **감정 코칭 애플리케이션**입니다.

###  핵심 기능
-  **간편한 일기 작성**: 감정 선택 후 간단한 일기로 하루 기록
-  **AI 감정 분석**: Gemini API를 활용한 감정 핵심 요약 및 트리거 분석
-  **실천 행동 제안**: AI가 제안하는 구체적이고 실천 가능한 감정 완화 솔루션 가이드
-  **사진으로 저장**: AI 분석 결과 카드를 이미지로 저장 (아날로그 다이어리 꾸미기 / 비회원의 일기 저장 기능)
-  **감정 패턴 분석**: 월간 감정 변화를 그래프 및 흐름도로 시각화
-  **캘린더 뷰**: 날짜별 감정 이모지와 일기 목록 확인
-  **리스트 뷰**: 주차별 감정 이모지와 일기 목록 확인
-  **월간 요약 뷰**: Gemini API를 활용한 월간 요약 맟 주요 키워드 제시
-  **비회원 모드**: 로그인 없이도 바로 시작 가능

###  차별점
- **밤하늘 테마**: 차분하고 안정감 있는 녹색 + 밤하늘 디자인
- **AI 행동 코칭** : 지금 실행 가능한 구체적인 행동 가이드 제안
- **분석 카드 소장**: AI 분석 결과를 이미지로 저장하여 아날로그 활동과 연계
- **비회원 모드**: 진입 장벽 완화 및 프라이버시 보호
- **비침습적 데이터 수집**: 치료 목적이 아닌 일상 보조 도구로, 부담 없이 매일 사용 가능한 가벼운 설계

---

##  팀 구성

| 역할 | 이름 | 담당 |
|------|------|------|
| 🎨 **UX/UI** | 방세연 | UI/UX 디자인, Figma 목업 |
| 💻 **Frontend** | 김예지 | 홈, 일기, AI 분석, 월간 요약, 마이페이지 화면 |
| 💻 **Frontend** | 최유정 | 온보딩, 인증, 캘린더 화면 |
| ⚙️ **Backend** | 나영초 (팀장) | API와 DB 설계, Gemini API 연동, 프론트엔드와 백엔드 연동 |

---

##  화면 구성

### 1. 온보딩 & 인증 화면
-  스플래시 화면
-  온보딩 메인
-  로그인 화면
-  회원가입 화면
-  시작 안내 화면

### 2. 홈 화면
-  메인 홈 (월간 요약 + 오늘의 일기 + 오늘의 마음카드)

### 3. 오늘의 일기 화면
-  일기 작성 화면
-  일기 보기 화면
    - AI 분석 요약 카드
-  AI 분석 결과 카드 (상세분석)
  - 이미지 카드 형태로 저장

### 4. 캘린더 화면
-  캘린더 메인 (월별 뷰 + 감정 그래프)
-  월간 요약 화면
-  주차별 일기 리스트
-  일기 상세보기

### 5. 마이페이지 화면
- 프로필 메인 (뱃지)
- 업적 (뱃지) 확인하기
- 나의 기록
  - 일기 목록 (캘린더 / 리스트)
  - 마음 카드 보관함 (즐겨찾기)
- 계정 정보
  - 프로필 편집
  - 로그아웃
  - 회원탈퇴
    - 다이얼로그로 재확인

---

##  시작하기

### Prerequisites
```bash
- Android Studio (권장: Hedgehog 이상)
- JDK 11 이상 (프로젝트 설정: Java 11 / Kotlin JVM target 11)
- Android SDK:
  - minSdk 23+ (Android 6.0+)
  - targetSdk 36
  - compileSdk 36
- Kotlin 1.8+
- local.properties에 GEMINI_API_KEY 설정 필요
  예) GEMINI_API_KEY=내 키값
```

##  Dependencies
```gradle
dependencies {
    // Core (AndroidX)
    implementation 'androidx.core:core-ktx:1.17.0'
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'androidx.activity:activity-ktx:1.12.2'

    // UI
    implementation 'com.google.android.material:material:1.13.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.1'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'

    // Lifecycle
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.9.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0'

    // Navigation
    implementation 'androidx.navigation:navigation-fragment-ktx:2.9.6'
    implementation 'androidx.navigation:navigation-ui-ktx:2.9.6'

    // Network
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'

    // Chart
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'

    // Test
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test:core:1.5.0'
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test:rules:1.5.0'
    androidTestImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
}
```

---

##  참고 자료

- [Android Developers Documentation](https://developer.android.com/)
- [Android Build (Gradle/AGP, Kotlin DSL)](https://developer.android.com/build)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Material Design Guidelines](https://m3.material.io/)
- [AndroidX Lifecycle (LiveData / ViewModel)](https://developer.android.com/jetpack/androidx/releases/lifecycle)
- [AndroidX Navigation](https://developer.android.com/guide/navigation)
- [OkHttp GitHub](https://github.com/square/okhttp)
- [View Binding](https://developer.android.com/topic/libraries/view-binding)
- [MPAndroidChart GitHub](https://github.com/PhilJay/MPAndroidChart)
- [Google Gemini API](https://ai.google.dev/)

---

<div align="center">

**Made with ❤️ by Blank**

⭐️ 이 프로젝트가 도움이 되셨다면 별을 눌러주세요! ⭐️

</div>