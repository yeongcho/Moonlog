# GURU2 - 감정 일기 코칭 앱 🌙

> **AI 기반 감정 분석 및 일기 코칭 애플리케이션**  
> 하루의 감정을 기록하고, AI가 분석한 맞춤형 실천 행동을 제안받아보세요.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![API](https://img.shields.io/badge/API-19%2B-brightgreen.svg)](https://android-arsenal.com/api?level=19)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📱 프로젝트 소개

**GURU2**는 사용자가 하루의 감정과 일기를 기록하면, AI(Gemini API)가 감정을 분석하고 오늘 실천 가능한 행동을 제안하는 **감정 코칭 애플리케이션**입니다.

### 🎯 핵심 기능
- 📝 **간편한 일기 작성**: 감정 선택 후 간단한 메모로 하루 기록
- 🤖 **AI 감정 분석**: Gemini API를 활용한 감정 핵심 요약 및 트리거 분석
- 💡 **실천 행동 제안**: AI가 제안하는 구체적이고 실천 가능한 행동 가이드
- 📊 **감정 패턴 분석**: 주간/월간 감정 변화를 그래프로 시각화
- 📅 **캘린더 뷰**: 날짜별 감정 이모지와 일기 목록 확인
- 🖼️ **사진으로 저장**: AI 분석 결과 카드를 이미지로 저장 (아날로그 다꾸용)
- 👤 **비회원 모드**: 로그인 없이도 로컬 저장으로 바로 시작 가능

### 🌟 차별점
- **밤하늘 컨셉**: 차분하고 안정감 있는 녹색 + 밤하늘 디자인
- **치료 목적이 아닌 일상 보조 도구**: 부담 없이 매일 사용 가능한 가벼운 인터페이스
- **비침습적 데이터 수집**: 회원가입 없이도 사용 가능한 프라이버시 친화적 설계

---

## 👥 팀 구성

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

## 🏗️ 기술 스택

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

## 📂 프로젝트 구조
