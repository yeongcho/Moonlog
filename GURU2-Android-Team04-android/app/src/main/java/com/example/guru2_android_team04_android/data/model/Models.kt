package com.example.guru2_android_team04_android.data.model

// User : users 테이블(회원) 데이터를 앱 내부에서 다루기 위한 모델
// 용도:
// - 회원가입/로그인 시 사용자 정보를 전달/표시할 때 사용한다.
// - DB(UserDao)에서 조회한 결과를 서비스(AppService)와 UI로 넘길 때 사용한다.
// 설계:
// - password_hash는 보안상 노출하면 안 되므로 User 모델에는 포함하지 않는다.
// - createdAt은 가입 시각(Unix ms)로, 서비스 이용일수 계산 등에 사용된다.
data class User(
    // users.user_id (PK)
    val userId: Long,
    // 사용자 닉네임
    val nickname: String,
    // 사용자 이메일(로그인 아이디)
    val email: String,
    // 가입 시각(Unix ms)
    val createdAt: Long
)

// Mood : 일기의 감정 상태를 나타내는 enum
// 용도:
// - diary_entries.mood 컬럼에 정수(dbValue)로 저장된다.
// - UI에서는 Mood 이름/아이콘/색상 등을 결정하는 기준이 된다.
// 설계:
// - SQLite에는 enum을 직접 저장하지 못하므로 Int(dbValue)로 매핑한다.
enum class Mood(val dbValue: Int) {
    JOY(1), CONFIDENCE(2), CALM(3), NORMAL(4), DEPRESSED(5), ANGRY(6), TIRED(7);

    companion object {
        // DB에 저장된 Int 값을 Mood enum으로 복원한다.
        // 예외처리) 알 수 없는 값이 들어오면 앱이 죽지 않도록 NORMAL로 대체한다.
        fun fromDb(v: Int): Mood = values().firstOrNull { it.dbValue == v } ?: NORMAL
    }
}

// DiaryEntry : diary_entries 테이블(일기) 한 건을 나타내는 모델
// 용도:
// - 일기 저장/조회/수정/삭제 등 CRUD의 핵심 데이터 구조로 사용된다.
// - 캘린더/목록/상세 화면에서 동일하게 사용된다.
// 설계:
// - ownerId로 사용자 구분: "USER_xxx"(회원) 또는 "ANON_xxx"(비회원)
// - dateYmd는 "YYYY-MM-DD" 문자열로 저장하여 BETWEEN으로 월/주 범위 조회가 쉽다.
// - tags는 List<String>이므로 DB에는 tags_json(JSON 문자열)로 저장하고,
//   앱에서는 List<String>으로 사용한다(JsonMini 직렬화/역직렬화).
// - isTemporary=1은 비회원 임시 저장 데이터로,
//   목록/캘린더/통계에서 숨기기 위해 사용된다.
data class DiaryEntry(
    // diary_entries.entry_id (PK)
    // - INSERT 후 생성되는 rowId
    val entryId: Long = 0L,

    // 사용자 식별자
    // - 회원: "USER_<userId>"
    // - 비회원: "ANON_<random>"
    val ownerId: String,

    // 작성 날짜 ("YYYY-MM-DD")
    // - 날짜 단위로 한 개의 일기를 관리하기 위한 키로도 활용된다
    //   (UNIQUE(owner_id, date_ymd))
    val dateYmd: String,

    // 일기 제목
    val title: String,

    // 일기 본문
    val content: String,

    // 감정 값
    val mood: Mood,

    // 태그 목록
    // - DB에는 tags_json으로 저장된다.
    val tags: List<String>,

    // 즐겨찾기(하트) 여부
    // - 마음 카드 보관함(archive) 대상 여부를 결정한다.
    val isFavorite: Boolean = false,

    // 비회원 임시 저장 여부
    // - true면 목록/캘린더/통계에서 제외된다.
    val isTemporary: Boolean = false,

    // 생성 시각(Unix ms)
    val createdAt: Long = System.currentTimeMillis(),

    // 수정 시각(Unix ms)
    val updatedAt: Long = System.currentTimeMillis()
)

// AiAnalysis : ai_analysis 테이블(일기 AI 분석 결과) 한 건을 나타내는 모델 (DB 저장용 엔티티)
// 용도:
// - 일기(entryId) 1개에 대한 분석 결과를 캐시 형태로 저장/조회할 때 사용한다.
// - MindCardPreview / MindCardDetail을 구성할 때 핵심 데이터로 사용된다.
// 설계:
// - entryId를 UNIQUE로 두어 "일기 1개당 분석 1개"를 유지한다.
// - actions / hashtags는 List<String>이므로 DB에는 JSON 문자열로 저장한다.
// - createdAt은 분석 생성 시각(Unix ms)로, 캐시 갱신 기준으로도 활용 가능하다.
data class AiAnalysis(
    // ai_analysis.analysis_id (PK)
    val analysisId: Long = 0L,

    // 참조하는 일기 ID (diary_entries.entry_id)
    val entryId: Long,

    // AI가 만든 요약 문장
    val summary: String,

    // 감정 트리거(원인/패턴) 설명
    val triggerPattern: String,

    // 행동 제안(미션) 목록
    // - 보통 1~3개로 제한해서 저장/사용한다.
    val actions: List<String>,

    // 해시태그 목록
    // - 0개 이상
    val hashtags: List<String>,

    // 미션을 아우르는 1줄 요약
    val missionSummary: String,

    // 상세 분석 전체 텍스트
    val fullText: String,

    // 분석 생성 시각(Unix ms)
    val createdAt: Long = System.currentTimeMillis()
)

// MonthlySummary : monthly_summaries 테이블(월간 요약) 한 건을 나타내는 모델
// 용도:
// - 특정 월(YYYY-MM)에 대한 감정 요약 / 대표 태그 / 요약 문장을 저장한다.
// - 월간 요약 화면(activity_monthly_summary.xml)에 필요한 내용을 저장한다.
data class MonthlySummary(
    // 사용자 식별자 ("USER_xxx" / "ANON_xxx")
    val ownerId: String,

    // 대상 월 ("YYYY-MM")
    val yearMonth: String,

    // 월간 대표 감정(최빈 감정)
    val dominantMood: Mood,

    // 한 줄 요약(강조 박스)
    val oneLineSummary: String,

    // 상세 요약 본문(긴 문단)
    val detailSummary: String,

    // 감정 흐름 한 줄(예: "안정 → 지침 → 회복")
    val emotionFlow: String,

    // 주요 키워드(칩) 0~3개
    val keywords: List<String>,

    // 마지막 갱신 시각(Unix ms)
    // - 월간 요약을 재생성/업데이트할 때 변경된다.
    val updatedAt: Long = System.currentTimeMillis()
)

// MoodStat : 감정 통계를 UI에 넘기기 위한 집계 모델
// 용도:
// - 월별 감정 분포 그래프(막대/원 그래프) 등을 그릴 때 사용된다.
data class MoodStat(
    val mood: Mood, val count: Int
)
