# GURU2 - 감정 일기 코칭 앱 🌙

> **AI 기반 감정 분석 및 일기 코칭 애플리케이션**  
> 하루의 감정을 기록하고, AI가 분석한 맞춤형 실천 행동을 제안받아보세요.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![API](https://img.shields.io/badge/API-19%2B-brightgreen.svg)](https://android-arsenal.com/api?level=19)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

##  프로젝트 소개

**GURU2**는 사용자가 하루의 감정과 일기를 기록하면, AI(Gemini API)가 감정을 분석하고 오늘 실천 가능한 행동을 제안하는 **감정 코칭 애플리케이션**입니다.

###  핵심 기능
-  **간편한 일기 작성**: 감정 선택 후 간단한 메모로 하루 기록
-  **AI 감정 분석**: Gemini API를 활용한 감정 핵심 요약 및 트리거 분석
-  **실천 행동 제안**: AI가 제안하는 구체적이고 실천 가능한 행동 가이드
-  **감정 패턴 분석**: 주간/월간 감정 변화를 그래프로 시각화
-  **캘린더 뷰**: 날짜별 감정 이모지와 일기 목록 확인
-  **사진으로 저장**: AI 분석 결과 카드를 이미지로 저장 (아날로그 다꾸용)
-  **비회원 모드**: 로그인 없이도 로컬 저장으로 바로 시작 가능

###  차별점
- **밤하늘 컨셉**: 차분하고 안정감 있는 녹색 + 밤하늘 디자인
- **치료 목적이 아닌 일상 보조 도구**: 부담 없이 매일 사용 가능한 가벼운 인터페이스
- **비침습적 데이터 수집**: 회원가입 없이도 사용 가능한 프라이버시 친화적 설계

---

##  팀 구성

| 역할 | 이름 | 담당 |
|------|------|------|
| 🎨 **UX/UI** | 방세연 | UI/UX 디자인, Figma 목업 |
| 💻 **Frontend** | 김예지 | 홈, 일기 작성, AI 분석 화면 |
| 💻 **Frontend** | 최유정 | 온보딩, 인증, 마이페이지 화면 |
| ⚙️ **Backend** | 나영초 (팀장) | API 서버, DB 설계, Gemini API 연동 |

---

## 🗓️ 개발 일정

| 주차 | 기간 | 목표 |
|------|------|------|
| **1주차** | 12/21 ~ 12/27 | 주제 선정 & 일정 계획 |
| **2주차** | 12/28 ~ 1/3 | 디자인 목업 완성 (Figma) |
| **3주차** | 1/4 ~ 1/10 | 프론트엔드 화면 구성 & 백엔드 API 개발 |
| **4주차** | 1/11 ~ 1/17 | 통합 테스트 & 버그 수정 |
| **최종** | 1/18 ~ | 최종 발표 준비 |

---

##  기술 스택

### Frontend (Android)
- **Language**: Kotlin
- **UI**: XML Layouts
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**: Retrofit2 (API 통신)
- **Chart Library**: MPAndroidChart (감정 그래프)
- **Image Loading**: Glide
- **Local Storage**: SharedPreferences, Room Database (예정)

### Backend
- **Framework**: FastAPI / Spring Boot (미정)
- **Database**: MySQL / PostgreSQL (미정)
- **AI API**: Google Gemini API
- **Deployment**: AWS EC2, RDS, S3 (예정)

### Design
- **Tool**: Figma
- **Color Palette**: 
  - Primary Green: `#4CAF50`
  - Secondary Pink: `#FF6B9D`
  - Night Sky Blue: `#1A237E`

---

##  프로젝트 구조
```
GURU2-Android-Team04-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/guru2_android_team04_android/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── splash/          # 스플래시 화면
│   │   │   │   │   ├── onboarding/      # 온보딩 & 인증
│   │   │   │   │   ├── home/            # 홈 탭
│   │   │   │   │   ├── diary/           # 일기 작성 탭
│   │   │   │   │   ├── calendar/        # 캘린더 탭
│   │   │   │   │   └── mypage/          # 마이페이지 탭
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/           # 데이터 클래스
│   │   │   │   │   ├── api/             # API 인터페이스
│   │   │   │   │   └── repository/      # 데이터 저장소
│   │   │   │   └── util/                # 유틸리티 클래스
│   │   │   ├── res/
│   │   │   │   ├── layout/              # XML 레이아웃
│   │   │   │   ├── drawable/            # 이미지 리소스
│   │   │   │   ├── values/              # 색상, 문자열
│   │   │   │   └── navigation/          # 네비게이션 그래프
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── README.md
└── settings.gradle.kts
```

---

##  화면 구성

### 1. 온보딩 & 인증
- ✅ 스플래시 화면
- ✅ 온보딩 메인 (마스코트 캐릭터)
- ✅ 로그인 화면
- ✅ 회원가입 화면
- ✅ 비회원 시작 안내 화면

### 2. 홈 탭
- ✅ 메인 홈 (월간 요약 + 오늘의 일기)
- ✅ 연간 요약 화면
- ✅ 비회원 제한 안내

### 3. 일기 작성 탭 
- ✅ 감정 선택 화면
- ✅ 일기 작성 화면
- ✅ AI 분석 중 로딩
- ✅ AI 분석 결과 카드 (상세분석)
- ✅ 사진 저장 완료

### 4. 캘린더 탭
- ✅ 캘린더 메인 (월별 뷰 + 감정 그래프)
- ✅ 날짜별 일기 목록
- ✅ 일기 상세보기

### 5. 마이페이지 탭
- ✅ 프로필 메인 (뱃지)
- ✅ 나의 감정 변화 (그래프)
- ✅ 계정 정보
- ✅ 회원 탈퇴 확인

---

##  시작하기

### Prerequisites
```bash
- Android Studio Arctic Fox 이상
- JDK 11 이상
- Android SDK API 19+ (Android 4.4+)
- Kotlin 1.8+
```

### Installation

1. **저장소 클론**
```bash
git clone https://github.com/yeongcho/GURU2-Android-Team04.git
cd GURU2-Android-Team04/GURU2-Android-Team04-android
```

2. **Android Studio에서 프로젝트 열기**
```
File → Open → GURU2-Android-Team04-android 폴더 선택
```

3. **Gradle Sync**
```
프로젝트 열면 자동으로 Gradle Sync 진행
또는 File → Sync Project with Gradle Files
```

4. **에뮬레이터 또는 실기기에서 실행**
```
Run 버튼 (▶) 클릭 → 기기 선택 → OK
```

---

##  Dependencies
```gradle
dependencies {
    // AndroidX
    implementation 'androidx.core:core-ktx:1.10.1'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.activity:activity-ktx:1.7.2'
    
    // Material Design
    implementation 'com.google.android.material:material:1.4.0'
    
    // ConstraintLayout
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // Navigation (예정)
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'
    
    // Retrofit (API 통신 - 예정)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // MPAndroidChart (그래프 - 예정)
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    
    // Glide (이미지 로딩 - 예정)
    implementation 'com.github.bumptech.glide:glide:4.16.0'
}
```

---

## 🔌 API 명세 (예정)

### 인증 API
```
POST /api/auth/register    # 회원가입
POST /api/auth/login       # 로그인
POST /api/auth/logout      # 로그아웃
```

### 일기 API
```
POST /api/diary/create     # 일기 작성
GET  /api/diary/list       # 일기 목록
GET  /api/diary/{id}       # 일기 상세
DELETE /api/diary/{id}     # 일기 삭제
```

### AI 분석 API
```
POST /api/ai/analyze           # 감정 분석 (Gemini)
GET  /api/ai/summary/weekly    # 주간 요약
GET  /api/ai/summary/monthly   # 월간 요약
```

### 사용자 API
```
GET  /api/user/profile         # 프로필 조회
GET  /api/user/info            # 계정 정보
PUT  /api/user/nickname        # 닉네임 수정
DELETE /api/user/account       # 회원 탈퇴
```

### 통계 API
```
GET /api/stats/emotion?period=week    # 주간 감정 데이터
GET /api/stats/emotion?period=month   # 월간 감정 데이터
```

---

## 🎯 주요 기능 구현 상세

### 1. AI 감정 분석 (Gemini API)
```kotlin
// 감정 분석 요청 예시
POST /api/ai/analyze
Request Body:
{
  "emotion": "우울",
  "content": "오늘은 정말 힘든 하루였다...",
  "emotion_tags": ["불안", "피곤"]
}

Response:
{
  "summary": "오늘은 업무 스트레스와 피로가 겹쳐 힘든 하루를 보내셨네요.",
  "trigger": "업무 과부하, 수면 부족",
  "actions": [
    "지금 이 순간, 3분 동안 심호흡하기",
    "좋아하는 음악 한 곡 듣기"
  ]
}
```

### 2. 감정 그래프 (MPAndroidChart)
- **주간 그래프**: LineChart (7일 추이)
- **월간 그래프**: BarChart (30일 통계)
- **감정 점수**: 1~5점 (매우 우울 ~ 매우 기쁨)

### 3. 사진 저장 기능
- AI 분석 결과 카드를 Bitmap으로 변환
- 갤러리에 저장 (아날로그 다꾸 활용)
- 공유 기능 (Instagram, KakaoTalk 등)

---

## 🌙 디자인 가이드

### 색상 팔레트
```kotlin
// colors.xml
<color name="primary_green">#4CAF50</color>      // 주 색상
<color name="secondary_pink">#FF6B9D</color>     // 보조 색상
<color name="night_sky_blue">#1A237E</color>     // 배경
<color name="white">#FFFFFF</color>
<color name="gray_300">#E0E0E0</color>
<color name="gray_400">#BDBDBD</color>
<color name="gray_600">#757575</color>
<color name="red_500">#F44336</color>            // 경고
```

### 타이포그래피
- **제목**: 24sp, Bold
- **본문**: 16sp, Regular
- **캡션**: 14sp, Regular

### 아이콘
- **크기**: 24dp (기본), 48dp (터치 영역)
- **스타일**: Material Icons

---

##  기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 커밋 메시지 규칙
```
feat: 새로운 기능 추가
fix: 버그 수정
design: UI/레이아웃 변경
refactor: 코드 리팩토링
docs: 문서 수정
test: 테스트 코드 추가
chore: 빌드 설정 변경
```

---

##  라이선스

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

##  문의

**프로젝트 링크**: [https://github.com/yeongcho/GURU2-Android-Team04](https://github.com/yeongcho/GURU2-Android-Team04)

---

##  참고 자료

- [Android Developers Documentation](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Material Design Guidelines](https://material.io/design)
- [MPAndroidChart GitHub](https://github.com/PhilJay/MPAndroidChart)
- [Google Gemini API](https://ai.google.dev/)

---

<div align="center">

**Made with ❤️ by GURU2-Team04**

⭐️ 이 프로젝트가 도움이 되셨다면 Star를 눌러주세요! ⭐️

</div>
